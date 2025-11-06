package com.fpsgame.client.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.JPanel;
import javax.swing.Timer;

import com.fpsgame.client.model.Viewport;
import com.fpsgame.common.ProjectilesV2;
import com.fpsgame.common.Rect;
import com.fpsgame.common.SnapshotV2;

/**
 * 게임 패널 v2 (전면 개편)
 * - Viewport 카메라 클래스로 렌더링 추적
 * - Timer 기반 60fps 렌더 루프
 * - 맵 배경 이미지 렌더링
 * - 캐릭터 스프라이트(방향 회전)
 * - 투사체 & 미니맵 표시
 */
public class GamePanel extends JPanel {
    private static final int TARGET_FPS = 60;

    public enum CameraMode {
        MAP_OVERVIEW,
        PLAYER_FOLLOW
    }

    
    // 뷰 & 카메라
    private final Viewport viewport;
    private volatile CameraMode cameraMode = CameraMode.MAP_OVERVIEW;
    private volatile boolean autoFitViewport = true;
    private volatile float followModeScale = 2.0f;
    private volatile float worldW = 3000f;
    private volatile float worldH = 2000f;
    private volatile int myId = -1;
    // TODO: Use currentMapId for map-specific rendering
    // private volatile int currentMapId = -1;
    
    // 부드러운 카메라 이동을 위한 필드
    private volatile float smoothCameraX = 0f;
    private volatile float smoothCameraY = 0f;
    private static final float CAMERA_LERP_FACTOR = 0.15f;

    private final Map<Integer, SnapshotV2.Entry> players = new ConcurrentHashMap<>();
    private volatile List<ProjectilesV2.Entry> projectiles = java.util.Collections.emptyList();

    // 맵 배경 이미지 캐시
    private BufferedImage mapBackground = null;
    
    // 캐릭터 이미지 캐시 (characterId -> 이미지)
    private final Map<Integer, BufferedImage> characterImages = new HashMap<>();

    // 미니맵 설정
    private static final int MINIMAP_MAX_W = 220;
    private static final int MINIMAP_MAX_H = 160;
    private static final int MINIMAP_MARGIN = 12;
    private static final int MINIMAP_DOT_PLAYER = 4;
    private static final int MINIMAP_DOT_PROJECTILE = 3;
    private volatile boolean minimapEnabled = true;

    // HUD 폰트
    private final Font hudFont = new Font(Font.MONOSPACED, Font.PLAIN, 12);
    private final Font hudFontBold = new Font(Font.MONOSPACED, Font.BOLD, 14);
    private final Font hudFontLarge = new Font(Font.MONOSPACED, Font.BOLD, 16);

    // HUD 상태 데이터
    private volatile String phaseText = "LOBBY";
    private volatile int countdownSeconds = -1;
    private volatile int blueScore = 0;
    private volatile int redScore = 0;
    private volatile int readyCount = 0;
    private volatile int totalPlayers = 0;
    private volatile String systemMessage = "";
    private volatile boolean isConnected = false;
    
    // 플레이어 HP/스킬 상태 (myId 기준)
    private volatile int myHp = 100;
    private volatile int myMaxHp = 100;
    private volatile float tacticalCooldown = 0f; // 0~1 (0=사용가능)
    private volatile float ultimateCooldown = 0f;

    // Input state (WASD for movement)
    private volatile boolean keyW, keyA, keyS, keyD;
    // TODO: keyE, keyQ for abilities (not yet implemented)
    private volatile Float aimRad = null; // nullable until computed

    // Sender binding
    public interface InputSender { void send(byte mask, java.lang.Float aimNullable); }
    public interface ActionSender { void sendAction(int actionType); }
    private volatile InputSender inputSender;
    private volatile ActionSender actionSender;

    public GamePanel() {
        // Viewport defaults to a 3000x2000 world configuration.
        viewport = new Viewport(3000f, 2000f);
        viewport.setZoomLimits(0.01f, 6.0f);

        setOpaque(false);
        setDoubleBuffered(true);
        setFocusable(true);
        setBackground(new Color(0x101418));

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                Dimension d = getSize();
                viewport.resize(Math.max(1, d.width), Math.max(1, d.height));
                maybeFitViewportToWorld();
                repaint();
            }
        });

        // Key handling (WASD + E, Q, M, C)
        addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_W -> keyW = true;
                    case KeyEvent.VK_A -> keyA = true;
                    case KeyEvent.VK_S -> keyS = true;
                    case KeyEvent.VK_D -> keyD = true;
                    case KeyEvent.VK_M -> minimapEnabled = !minimapEnabled;
                    case KeyEvent.VK_C -> toggleCameraMode();
                    case KeyEvent.VK_E -> {
                        ActionSender as = actionSender;
                        if (as != null) as.sendAction(1);
                    }
                    case KeyEvent.VK_Q -> {
                        ActionSender as = actionSender;
                        if (as != null) as.sendAction(2);
                    }
                    default -> {}
                }
            }
            @Override public void keyReleased(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_W -> keyW = false;
                    case KeyEvent.VK_A -> keyA = false;
                    case KeyEvent.VK_S -> keyS = false;
                    case KeyEvent.VK_D -> keyD = false;
                    default -> {}
                }
            }
        });

        MouseAdapter mouse = new MouseAdapter() {
            @Override public void mouseMoved(MouseEvent e) { updateAimFromMouse(e.getPoint()); }
            @Override public void mouseDragged(MouseEvent e) { updateAimFromMouse(e.getPoint()); }
            @Override public void mouseEntered(MouseEvent e) { requestFocusInWindow(); }
            @Override public void mouseClicked(MouseEvent e) {
                requestFocusInWindow();
                if (e.getButton() == MouseEvent.BUTTON1) {
                    ActionSender as = actionSender;
                    if (as != null) as.sendAction(0); // BasicAttack
                }
            }
        };
        addMouseMotionListener(mouse);
        addMouseListener(mouse);

        // Input dispatch timer (~30 Hz)
        Timer inputTimer = new Timer(33, ev -> flushInput());
        inputTimer.setRepeats(true);
        inputTimer.start();

        // Render timer (60 FPS)
        int delay = Math.max(1, 1000 / TARGET_FPS);
        new Timer(delay, e -> {
            updateCamera();
            repaint();
        }).start();
    }

    public void setWorldSize(int w, int h) {
        worldW = Math.max(1, w);
        worldH = Math.max(1, h);
        viewport.setWorldSize(worldW, worldH);

        int panelW = Math.max(1, getWidth());
        int panelH = Math.max(1, getHeight());
        viewport.resize(panelW, panelH);
        maybeFitViewportToWorld();
        repaint();
    }
    public void setCameraMode(CameraMode mode) {
        if (mode == null) return;
        cameraMode = mode;
        if (mode == CameraMode.MAP_OVERVIEW) {
            autoFitViewport = true;
            maybeFitViewportToWorld();
        } else {
            autoFitViewport = false;
        }
    }

    public CameraMode getCameraMode() {
        return cameraMode;
    }

    public void setFollowCameraScale(float scale) {
        if (!Float.isFinite(scale) || scale <= 0f) return;
        followModeScale = scale;
    }

    public void setAutoFitViewport(boolean enabled) {
        autoFitViewport = enabled;
        if (enabled) {
            maybeFitViewportToWorld();
        }
    }

    private void toggleCameraMode() {
        CameraMode next = (cameraMode == CameraMode.MAP_OVERVIEW)
                ? CameraMode.PLAYER_FOLLOW : CameraMode.MAP_OVERVIEW;
        setCameraMode(next);
        System.out.println("[GamePanel] Camera mode switched to " + next);
    }

    private void maybeFitViewportToWorld() {
        if (!autoFitViewport) return;
        viewport.fitToWorld();
    }
    
    public void setMyId(int id) { 
        this.myId = id;
        // 카메라 위치 초기화
        smoothCameraX = 0f;
        smoothCameraY = 0f;
    }

    /** Bind a sender that transmits input to server. */
    public void setInputSender(InputSender sender) { this.inputSender = sender; }

    /** Bind action sender for attacks/skills. */
    public void setActionSender(ActionSender sender) { this.actionSender = sender; }
    
    /** Set map ID and load background image. */
    public void setMapId(int mapId) {
        // TODO: Use mapId for map-specific rendering
        // this.currentMapId = mapId;
        loadMapBackground(mapId);
        repaint();
    }
    
    // ===== HUD 업데이트 메서드 =====
    
    public void updatePhase(int phaseCode) {
        this.phaseText = switch (phaseCode) {
            case 0 -> "LOBBY";
            case 1 -> "VOTE";
            case 2 -> "COUNTDOWN";
            case 3 -> "ROUND_RUNNING";
            case 4 -> "ROUND_RESULT";
            case 5 -> "MATCH_END";
            default -> "UNKNOWN";
        };
        repaint();
    }
    
    public void updateCountdown(int seconds) {
        this.countdownSeconds = seconds;
        repaint();
    }
    
    public void updateScore(int blue, int red) {
        this.blueScore = blue;
        this.redScore = red;
        repaint();
    }
    
    public void updateReadyCount(int ready, int total) {
        this.readyCount = ready;
        this.totalPlayers = total;
        repaint();
    }
    
    public void setSystemMessage(String msg) {
        this.systemMessage = (msg == null ? "" : msg);
        repaint();
    }
    
    public void setConnected(boolean connected) {
        this.isConnected = connected;
        repaint();
    }
    
    public void updatePlayerStats(int hp, int maxHp, float tacticalCd, float ultimateCd) {
        this.myHp = Math.max(0, hp);
        this.myMaxHp = Math.max(1, maxHp);
        this.tacticalCooldown = Math.max(0f, Math.min(1f, tacticalCd));
        this.ultimateCooldown = Math.max(0f, Math.min(1f, ultimateCd));
        repaint();
    }
    
    // ===== 카메라 & 렌더 =====
    
    // 카메라 업데이트 (플레이어 추적)
    private void updateCamera() {
        if (cameraMode == CameraMode.MAP_OVERVIEW) {
            viewport.setCenter(worldW * 0.5f, worldH * 0.5f);
            maybeFitViewportToWorld();
            return;
        }

        SnapshotV2.Entry me = players.get(myId);
        if (me != null) {
            viewport.setScale(followModeScale);
            
            // 부드러운 카메라 이동 (Lerp)
            if (smoothCameraX == 0f && smoothCameraY == 0f) {
                // 첫 프레임: 즉시 플레이어 위치로 이동
                smoothCameraX = me.x;
                smoothCameraY = me.y;
            } else {
                // 부드럽게 플레이어를 따라감
                smoothCameraX += (me.x - smoothCameraX) * CAMERA_LERP_FACTOR;
                smoothCameraY += (me.y - smoothCameraY) * CAMERA_LERP_FACTOR;
            }
            
            viewport.setCenter(smoothCameraX, smoothCameraY);
        } else {
            // 플레이어가 없으면 카메라 초기화
            if (myId >= 0 && smoothCameraX == 0f && smoothCameraY == 0f) {
                smoothCameraX = worldW * 0.5f;
                smoothCameraY = worldH * 0.5f;
            }
        }
    }
    private void loadMapBackground(int mapId) {
        String mapName = switch (mapId) {
            case 0 -> "terminal";
            case 1 -> "neonCity";
            case 2 -> "forestOutpost";
            default -> null;
        };
        
        if (mapName == null) {
            mapBackground = null;
            System.err.println("[GamePanel] Unknown map ID: " + mapId);
            return;
        }
        
        System.out.println("[GamePanel] Loading map background: " + mapName + " (mapId=" + mapId + ")");
        
        // 리소스에서 맵 이미지 로드 (복수의 방식 참고)
        BufferedImage img = ImageUtil.loadResource(GamePanel.class, "/assets/maps/" + mapName + ".png");
        if (img == null) img = ImageUtil.loadResource(GamePanel.class, "/assets/maps/" + mapName + ".jpg");
        if (img == null) img = ImageUtil.loadFile("assets/maps/" + mapName + ".png");
        if (img == null) img = ImageUtil.loadFile("assets/maps/" + mapName + ".jpg");
        
        mapBackground = img;
        if (img == null) {
            System.err.println("[GamePanel] Failed to load map background: " + mapName);
        } else {
            System.out.println("[GamePanel] Map loaded: " + mapName + " (" + img.getWidth() + "x" + img.getHeight() + ")");
        }
    }
    
    private BufferedImage getCharacterImage(int characterId) {
        if (characterImages.containsKey(characterId)) {
            return characterImages.get(characterId);
        }
        
        String charName = switch (characterId) {
            case 0 -> "Sage";
            case 1 -> "Piper";
            case 2 -> "Technician";
            case 3 -> "General";
            case 4 -> "Bulldog";
            case 5 -> "Wildcat";
            case 6 -> "Raven";
            case 7 -> "Ghost";
            case 8 -> "Skull";
            case 9 -> "Steam";
            default -> null;
        };
        
        if (charName == null) {
            System.err.println("[GamePanel] Unknown character ID: " + characterId);
            return null;
        }
        
        System.out.println("[GamePanel] Loading character: " + charName + " (id=" + characterId + ")");
        
        // 복수의 방식 참고: 리소스 우선, 파일 대안
        BufferedImage img = ImageUtil.loadResource(GamePanel.class, "/assets/characters/" + charName + ".png");
        if (img == null) img = ImageUtil.loadResource(GamePanel.class, "/assets/characters/" + charName + ".jpg");
        if (img == null) img = ImageUtil.loadFile("assets/characters/" + charName + ".png");
        if (img == null) img = ImageUtil.loadFile("assets/characters/" + charName + ".jpg");
        
        if (img != null) {
            // 흰색 배경 제거
            img = ImageUtil.whiteToTransparent(img, 20);
            characterImages.put(characterId, img);
            System.out.println("[GamePanel] Character loaded: " + charName + " (" + img.getWidth() + "x" + img.getHeight() + ")");
        } else {
            System.err.println("[GamePanel] Failed to load character: " + charName);
        }
        
        return img;
    }

    public void applySnapshot(List<SnapshotV2.Entry> list) {
        if (list == null) return;
        Map<Integer, SnapshotV2.Entry> map = new ConcurrentHashMap<>(Math.max(16, list.size()*2));
        for (SnapshotV2.Entry e : list) map.put(e.id, e);
        
        players.clear();
        players.putAll(map);
    }

    public void applyProjectiles(List<ProjectilesV2.Entry> list) {
        this.projectiles = (list == null) ? java.util.Collections.emptyList() : list;
    }

    private void updateAimFromMouse(Point p) {
        SnapshotV2.Entry me = players.get(myId);
        if (me == null || p == null) { aimRad = null; return; }
        
        // 스크린 좌표를 월드 좌표로 변환
        com.fpsgame.common.Vec2 worldPos = viewport.screenToWorld(p.x, p.y, null);
        double dx = worldPos.x - me.x;
        double dy = worldPos.y - me.y;
        aimRad = (float) Math.atan2(dy, dx);
    }

    private void flushInput() {
        InputSender sender = this.inputSender;
        if (sender == null) return;
        
        // 자신의 캐릭터가 게임에 있을 때만 입력 전송
        SnapshotV2.Entry me = players.get(myId);
        if (me == null && myId >= 0) {
            // 자신의 캐릭터가 아직 스폰되지 않았거나 죽은 상태
            return;
        }
        
        int mask = 0;
        if (keyW) mask |= 0x01;
        if (keyS) mask |= 0x02;
        if (keyA) mask |= 0x04;
        if (keyD) mask |= 0x08;
        
        sender.send((byte)(mask & 0xFF), aimRad);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        int w = getWidth(), h = getHeight();
        
        // Background color
        g2.setColor(new Color(0x0f1115));
        g2.fillRect(0, 0, w, h);

        // 맵 배경 렌더링 (전체 월드 크기에 맞춤)
        if (mapBackground != null) {
            // 월드 (0, 0)과 (worldW, worldH)의 스크린 좌표 계산
            Point mapTopLeft = viewport.worldToScreen(0, 0);
            Point mapBottomRight = viewport.worldToScreen(worldW, worldH);
            
            int mapW = mapBottomRight.x - mapTopLeft.x;
            int mapH = mapBottomRight.y - mapTopLeft.y;
            
            // 전체 맵을 월드 크기에 맞춰 렌더링
            g2.drawImage(mapBackground, mapTopLeft.x, mapTopLeft.y, mapW, mapH, null);
        } else {
            // 맵 이미지가 없으면 뷰포트 영역만 기본 배경으로 채우기
            Rect viewBounds = viewport.getViewBounds(null);
            Point vTopLeft = viewport.worldToScreen(viewBounds.x, viewBounds.y);
            Point vBottomRight = viewport.worldToScreen(viewBounds.x + viewBounds.w, viewBounds.y + viewBounds.h);
            int viewW = vBottomRight.x - vTopLeft.x;
            int viewH = vBottomRight.y - vTopLeft.y;
            
            g2.setColor(new Color(0x1e232b));
            g2.fillRect(vTopLeft.x, vTopLeft.y, viewW, viewH);
        }

        // 투사체 렌더링(맵 위에 차곡차곡)
        for (ProjectilesV2.Entry p : projectiles) {
            if (!p.active) continue;
            Point pp = viewport.worldToScreen(p.x, p.y);
            
            Color bulletColor;
            int r;
            switch (p.type) {
                case ProjectilesV2.TYPE_BULLET:
                default:
                    bulletColor = new Color(0xffd54f); // Yellow color
                    r = 4;
                    break;
            }
            
            g2.setColor(new Color(bulletColor.getRed(), bulletColor.getGreen(), bulletColor.getBlue(), 100));
            g2.fillOval(pp.x - r - 2, pp.y - r - 2, (r + 2) * 2, (r + 2) * 2);
            g2.setColor(bulletColor);
            g2.fillOval(pp.x - r, pp.y - r, r * 2, r * 2);
            
          
            g2.setColor(new Color(255, 255, 255, 180));
            int hr = Math.max(1, r / 2);
            g2.fillOval(pp.x - hr, pp.y - hr, hr * 2, hr * 2);
        }

        // ========================================
        // Player Character Rendering
        // ========================================
        for (SnapshotV2.Entry e : players.values()) {
            // Convert world coordinates to screen coordinates
            Point screenPos = viewport.worldToScreen(e.x, e.y);
            
            // 1. Draw character image (rotated based on aim direction)
            BufferedImage charImg = getCharacterImage(e.characterId);
            if (charImg != null) {
                int imgSize = 64;
                
                // Save current transform
                AffineTransform oldTransform = g2.getTransform();
                
                // Create rotation transform centered at player position
                AffineTransform transform = new AffineTransform();
                transform.translate(screenPos.x, screenPos.y);  // Move to player screen position
                transform.rotate(e.aim);                         // Rotate by aim angle
                transform.translate(-imgSize / 2.0, -imgSize / 2.0);  // Center the image
                
                g2.setTransform(transform);
                g2.drawImage(charImg, 0, 0, imgSize, imgSize, null);
                g2.setTransform(oldTransform);  // Restore original transform
            } else {
                // Fallback: Draw colored circle if image not available
                int radius = 16;
                Color bodyColor = (e.team == 1) ? new Color(0x4f8cff) : new Color(0xff6f61);
                g2.setColor(bodyColor);
                g2.fillOval(screenPos.x - radius, screenPos.y - radius, radius * 2, radius * 2);
            }

            // 2. Draw health bar (above player)
            int healthBarWidth = 40;
            int healthBarHeight = 6;
            int healthBarX = screenPos.x - healthBarWidth / 2;
            int healthBarY = screenPos.y - 45;
            
            int maxHealth = 100;  // Default max HP
            
            // Background (red)
            g2.setColor(new Color(200, 0, 0));
            g2.fillRect(healthBarX, healthBarY, healthBarWidth, healthBarHeight);
            
            // Foreground (green based on current HP)
            if (e.hp > 0) {
                g2.setColor(new Color(0, 200, 0));
                int currentHealthWidth = (int) (healthBarWidth * Math.min(e.hp, maxHealth) / (float)maxHealth);
                g2.fillRect(healthBarX, healthBarY, currentHealthWidth, healthBarHeight);
            }
            
            // Health bar border
            g2.setColor(Color.BLACK);
            g2.setStroke(new BasicStroke(1f));
            g2.drawRect(healthBarX, healthBarY, healthBarWidth, healthBarHeight);

            // 3. Draw team indicator (circle above health bar)
            Color teamColor = (e.team == 1) ? new Color(0x4f8cff) : new Color(0xff6f61);
            g2.setColor(teamColor);
            int teamIndicatorRadius = 6;
            int teamIndicatorY = healthBarY - 12;
            g2.fillOval(screenPos.x - teamIndicatorRadius, teamIndicatorY, 
                       teamIndicatorRadius * 2, teamIndicatorRadius * 2);

            // 4. Draw player ID (above team indicator)
            g2.setFont(hudFont);
            g2.setColor(Color.WHITE);
            String idText = "#" + e.id;
            int idWidth = g2.getFontMetrics().stringWidth(idText);
            g2.drawString(idText, screenPos.x - idWidth / 2, teamIndicatorY - 2);
        }

        // HUD 정보 표시
        g2.setFont(hudFont);
        g2.setColor(Color.YELLOW);
        g2.drawString("Players: " + players.size(), 10, 20);
        g2.drawString(String.format("Scale: %.2f | Cam: (%.0f, %.0f)", 
            viewport.getScale(), 
            viewport.getCenter(null).x, 
            viewport.getCenter(null).y), 10, 35);

        // 미니맵 그리기
        drawMinimap(g2, w, h);

        // HUD 정보 그리기
        drawHUD(g2, w, h);

        // 조준선 그리기
        drawCrosshair(g2, w, h);

        g2.dispose();
    }

    // Top-left minimap rendering (position adjusted - right-bottom corner)
    private void drawMinimap(Graphics2D g2, int panelW, int panelH) {
        if (!minimapEnabled) return;
        
        // Get world bounds
        float ww = this.worldW;
        float wh = this.worldH;
        
        if (ww <= 0 || wh <= 0) return;

        double msx = MINIMAP_MAX_W / (double) ww;
        double msy = MINIMAP_MAX_H / (double) wh;
        double ms = Math.min(msx, msy);
        int mmW = Math.max(40, (int) Math.round(ww * ms));
        int mmH = Math.max(40, (int) Math.round(wh * ms));

        int x0 = panelW - MINIMAP_MARGIN - mmW;
        int y0 = panelH - MINIMAP_MARGIN - mmH;

        g2.setColor(new Color(0x0b0d12, true));
        g2.fillRect(x0 - 2, y0 - 2, mmW + 4, mmH + 4);
        g2.setColor(new Color(0x20252e));
        g2.fillRect(x0, y0, mmW, mmH);
        g2.setColor(new Color(0x445062));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRect(x0, y0, mmW, mmH);

        // 미니맵 경계 (내부 여백)
        g2.setColor(new Color(80, 80, 80, 100));
        g2.drawRect(x0 + 2, y0 + 2, mmW - 4, mmH - 4);

        // 플레이어 위치 표시
        g2.setColor(new Color(0xffd54f));
        for (ProjectilesV2.Entry p : projectiles) {
            if (!p.active) continue;
            int mx = x0 + (int) Math.round(p.x * ms);
            int my = y0 + (int) Math.round(p.y * ms);
            g2.fillOval(mx - MINIMAP_DOT_PROJECTILE/2, my - MINIMAP_DOT_PROJECTILE/2, MINIMAP_DOT_PROJECTILE, MINIMAP_DOT_PROJECTILE);
        }

        // 플레이어 팀 표시 (아래 팀 아이콘)
        SnapshotV2.Entry me = players.get(myId);
        int myTeam = (me != null) ? me.team : -1;
        
        for (SnapshotV2.Entry e : players.values()) {
            // 팀이 다르면 표시하지 않음
            if (myTeam >= 0 && e.team != myTeam) continue;
            
            int mx = x0 + (int) Math.round(e.x * ms);
            int my = y0 + (int) Math.round(e.y * ms);
            Color teamColor = (e.team == 1) ? new Color(0x4f8cff) : new Color(0xff6f61);
            g2.setColor(teamColor);
            g2.fillOval(mx - MINIMAP_DOT_PLAYER/2, my - MINIMAP_DOT_PLAYER/2, MINIMAP_DOT_PLAYER, MINIMAP_DOT_PLAYER);
            
            // 플레이어 ID 표시
            if (e.id == myId) {
                g2.setColor(Color.WHITE);
                g2.drawOval(mx - MINIMAP_DOT_PLAYER/2 - 2, my - MINIMAP_DOT_PLAYER/2 - 2, MINIMAP_DOT_PLAYER + 4, MINIMAP_DOT_PLAYER + 4);
            }
        }
    }

    // HUD 정보 표시 (상단: 타이머, 하단: HP/스킬)
    private void drawHUD(Graphics2D g2, int w, int h) {
        // 상단 HUD: Phase, Countdown, Score
        g2.setFont(hudFontBold);
        int y = 15;
        
        // Phase
        g2.setColor(new Color(180, 220, 255));
        g2.drawString("Phase: " + phaseText, 10, y);

        // Countdown (타이머)
        if (countdownSeconds >= 0) {
            g2.setColor(new Color(255, 220, 120));
            g2.drawString("Countdown: " + countdownSeconds + "s", 150, y);
        }

        // Score (점수)
        g2.setColor(new Color(200, 255, 200));
        g2.drawString("Score " + blueScore + " : " + redScore, 320, y);
        
        // Ready count
        g2.setColor(new Color(200, 220, 255));
        g2.drawString("Ready " + readyCount + "/" + totalPlayers, 480, y);

        // 연결 상태 표시: 연결됨 / 연결 끊김
        String connText = isConnected ? "Connected" : "Disconnected";
        Color connColor = isConnected ? new Color(100, 255, 100) : new Color(255, 100, 100);
        g2.setColor(connColor);
        g2.setFont(hudFont);
        int connW = g2.getFontMetrics().stringWidth(connText);
        g2.drawString(connText, w - connW - 15, 15);
        
        // 시스템 메시지 표시 (가운데 정렬)
        if (!systemMessage.isEmpty()) {
            g2.setFont(hudFontLarge);
            g2.setColor(new Color(255, 190, 190));
            int msgW = g2.getFontMetrics().stringWidth(systemMessage);
            g2.drawString(systemMessage, (w - msgW) / 2, 50);
        }
        
        // Bottom left: HP bar
        drawHealthBar(g2, 20, h - 80);
        
        // Bottom center: Skill cooldowns
        drawSkillCooldowns(g2, w / 2 - 100, h - 80);
    }
    
    // HP bar rendering
    private void drawHealthBar(Graphics2D g2, int x, int y) {
        int barW = 200;
        int barH = 20;
        
        // Background (dark red)
        g2.setColor(new Color(60, 20, 20));
        g2.fillRect(x, y, barW, barH);
        
        // HP (bright red)
        float hpRatio = (float) myHp / myMaxHp;
        int hpW = Math.round(barW * hpRatio);
        g2.setColor(new Color(220, 50, 50));
        g2.fillRect(x, y, hpW, barH);
        
        // Border
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(2f));
        g2.drawRect(x, y, barW, barH);
        
        // HP text
        g2.setFont(hudFont);
        String hpText = myHp + " / " + myMaxHp;
        int textW = g2.getFontMetrics().stringWidth(hpText);
        g2.setColor(Color.WHITE);
        g2.drawString(hpText, x + (barW - textW) / 2, y + barH - 5);
    }
    
    // Skill cooldown rendering (E: Tactical, Q: Ultimate)
    private void drawSkillCooldowns(Graphics2D g2, int x, int y) {
        int skillSize = 50;
        int gap = 10;
        
        // Tactical (E)
        drawSkillBox(g2, x, y, skillSize, "E", tacticalCooldown, new Color(100, 150, 255));
        
        // Ultimate (Q)
        drawSkillBox(g2, x + skillSize + gap, y, skillSize, "Q", ultimateCooldown, new Color(255, 150, 100));
    }
    
    private void drawSkillBox(Graphics2D g2, int x, int y, int size, String key, float cooldown, Color color) {
        // Background
        g2.setColor(new Color(30, 30, 30));
        g2.fillRect(x, y, size, size);
        
        // Cooldown overlay (dark semi-transparent)
        if (cooldown > 0.01f) {
            int cdHeight = Math.round(size * cooldown);
            g2.setColor(new Color(0, 0, 0, 180));
            g2.fillRect(x, y, size, cdHeight);
        }
        
        // Border
        g2.setColor(cooldown > 0.01f ? Color.GRAY : color);
        g2.setStroke(new BasicStroke(2f));
        g2.drawRect(x, y, size, size);
        
        // Key display
        g2.setFont(hudFontBold);
        g2.setColor(Color.WHITE);
        int textW = g2.getFontMetrics().stringWidth(key);
        g2.drawString(key, x + (size - textW) / 2, y + size / 2 + 5);
        
        // Cooldown percentage
        if (cooldown > 0.01f) {
            String cdText = Math.round(cooldown * 100) + "%";
            g2.setFont(hudFont);
            int cdW = g2.getFontMetrics().stringWidth(cdText);
            g2.drawString(cdText, x + (size - cdW) / 2, y + size - 5);
        }
    }
    
    private void drawCrosshair(Graphics2D g2, int w, int h) {
        SnapshotV2.Entry me = players.get(myId);
        if (me == null) return;

        // Get player screen position
        Point myScreenPos = viewport.worldToScreen(me.x, me.y);
        
        // Draw crosshair at screen center (basic crosshair)
        int centerX = w / 2;
        int centerY = h / 2;
        
        g2.setColor(new Color(255, 255, 255, 200));
        g2.setStroke(new BasicStroke(2f));
        int len = 15;
        int gap = 5;
        
        // Basic crosshair (center of screen)
        g2.drawLine(centerX - len, centerY, centerX - gap, centerY); // Left
        g2.drawLine(centerX + gap, centerY, centerX + len, centerY); // Right
        g2.drawLine(centerX, centerY - len, centerX, centerY - gap); // Top
        g2.drawLine(centerX, centerY + gap, centerX, centerY + len); // Bottom
        
        // Center dot
        g2.fillOval(centerX - 2, centerY - 2, 4, 4);

        // Draw aim direction line from player position
        g2.setColor(new Color(255, 100, 100, 120));
        g2.setStroke(new BasicStroke(2f));
        
        // Calculate aim endpoint in world coordinates
        int aimLen = 100;
        int aimX = myScreenPos.x + (int)(Math.cos(me.aim) * aimLen);
        int aimY = myScreenPos.y + (int)(Math.sin(me.aim) * aimLen);
        
        // Draw aim line from player position
        g2.drawLine(myScreenPos.x, myScreenPos.y, aimX, aimY);
        
        // Draw aim endpoint indicator
        g2.fillOval(aimX - 4, aimY - 4, 8, 8);
    }
}

