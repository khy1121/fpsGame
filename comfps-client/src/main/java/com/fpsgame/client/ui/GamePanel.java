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
 * 게임 ?�더 ?�널 v2 (?�전 ?�구�?
 * - Viewport 카메???�스?�으�??�레?�어 추적
 * - Timer 기반 60fps ?�더 루프
 * - �?배경 ?��?지 ?�더�? * - 캐릭???�프?�이??(방향 ?�전)
 * - ?�사�?& 미니�??�버?�이
 */
public class GamePanel extends JPanel {
    private static final int TARGET_FPS = 60;
\n    public enum CameraMode {\n        MAP_OVERVIEW,\n        PLAYER_FOLLOW\n    }\n\n    
    // ?�드 & 카메??    private final Viewport viewport;
    private volatile CameraMode cameraMode = CameraMode.MAP_OVERVIEW;\n    private volatile boolean autoFitViewport = true;\n    private volatile float followModeScale = 2.0f;\n    private volatile float worldW = 3000f;
    private volatile float worldH = 2000f;
    private volatile int myId = -1;
    // TODO: Use currentMapId for map-specific rendering
    // private volatile int currentMapId = -1;

    private final Map<Integer, SnapshotV2.Entry> players = new ConcurrentHashMap<>();
    private volatile List<ProjectilesV2.Entry> projectiles = java.util.Collections.emptyList();

    // �?배경 ?��?지 캐시
    private BufferedImage mapBackground = null;
    
    // 캐릭???��?지 캐시 (characterId -> ?��?지)
    private final Map<Integer, BufferedImage> characterImages = new HashMap<>();

    // 미니�??�정
    private static final int MINIMAP_MAX_W = 220;
    private static final int MINIMAP_MAX_H = 160;
    private static final int MINIMAP_MARGIN = 12;
    private static final int MINIMAP_DOT_PLAYER = 4;
    private static final int MINIMAP_DOT_PROJECTILE = 3;
    private volatile boolean minimapEnabled = true;

    // HUD ?�트
    private final Font hudFont = new Font(Font.MONOSPACED, Font.PLAIN, 12);
    private final Font hudFontBold = new Font(Font.MONOSPACED, Font.BOLD, 14);
    private final Font hudFontLarge = new Font(Font.MONOSPACED, Font.BOLD, 16);

    // HUD ?�태 ?�이??    private volatile String phaseText = "LOBBY";
    private volatile int countdownSeconds = -1;
    private volatile int blueScore = 0;
    private volatile int redScore = 0;
    private volatile int readyCount = 0;
    private volatile int totalPlayers = 0;
    private volatile String systemMessage = "";
    private volatile boolean isConnected = false;
    
    // ?�레?�어 HP/?�킬 ?�태 (myId 기�?)
    private volatile int myHp = 100;
    private volatile int myMaxHp = 100;
    private volatile float tacticalCooldown = 0f; // 0~1 (0=?�용가??
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
        System.out.println("[GamePanel] ?�★ myId ?�정?? " + id + " (?�전 myId=" + this.myId + ")");
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
    
    // ===== HUD ?�데?�트 메서??=====
    
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
    
    // ===== 카메??& ?�력 =====
    
    // 카메???�데?�트 (???�레?�어 추적)
    private void updateCamera() {
        if (cameraMode == CameraMode.MAP_OVERVIEW) {
            viewport.setCenter(worldW * 0.5f, worldH * 0.5f);
            maybeFitViewportToWorld();
            return;
        }

        SnapshotV2.Entry me = players.get(myId);
        if (me != null) {
            viewport.setScale(followModeScale);
            viewport.setCenter(me.x, me.y);
        } else {
            if (myId >= 0) {
                System.out.println("[GamePanel] WARNING: myId=" + myId + " not found in players. players.size=" + players.size() + ", keys=" + players.keySet());
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
        
        // 리소?�에??�??��?지 로드 (?�거??방식 참고)
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
        
        // ?�거??방식 참고: 리소???�선, ?�일 ?�백
        BufferedImage img = ImageUtil.loadResource(GamePanel.class, "/assets/characters/" + charName + ".png");
        if (img == null) img = ImageUtil.loadResource(GamePanel.class, "/assets/characters/" + charName + ".jpg");
        if (img == null) img = ImageUtil.loadFile("assets/characters/" + charName + ".png");
        if (img == null) img = ImageUtil.loadFile("assets/characters/" + charName + ".jpg");
        
        if (img != null) {
            // ?�색 배경 ?�거
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
        
        // DEBUG: ?�냅???�용 로그
        if (myId >= 0 && !map.containsKey(myId)) {
            System.out.println("[GamePanel] ?�★ WARNING: ?�냅?�에 myId=" + myId + " ?�음! 받�? IDs: " + map.keySet());
        }
        
        // DEBUG: ?�냅???�치 ?�인 (myId?� ?�른 ?�레?�어 비교)
        if (list != null && list.size() > 0) {
            StringBuilder sb = new StringBuilder("[GamePanel] ??myId=" + myId + " ?�냅??받음: ");
            for (SnapshotV2.Entry e : list) {
                String mark = (e.id == myId) ? "?�MY?? : "";
                sb.append(String.format("id=%d%s pos=(%.1f,%.1f) ", e.id, mark, e.x, e.y));
            }
            System.out.println(sb.toString());
        }
        
        players.clear();
        players.putAll(map);
        // Repaint??Timer가 처리
    }

    public void applyProjectiles(List<ProjectilesV2.Entry> list) {
        this.projectiles = (list == null) ? java.util.Collections.emptyList() : list;
    }

    private void updateAimFromMouse(Point p) {
        SnapshotV2.Entry me = players.get(myId);
        if (me == null || p == null) { aimRad = null; return; }
        
        // ?�크�?좌표�??�드 좌표�?변??        com.fpsgame.common.Vec2 worldPos = viewport.screenToWorld(p.x, p.y, null);
        double dx = worldPos.x - me.x;
        double dy = worldPos.y - me.y;
        aimRad = (float) Math.atan2(dy, dx);
    }

    private void flushInput() {
        InputSender sender = this.inputSender;
        if (sender == null) return;
        int mask = 0;
        if (keyW) mask |= 0x01;
        if (keyS) mask |= 0x02;
        if (keyA) mask |= 0x04;
        if (keyD) mask |= 0x08;
        
        // DEBUG: ?�력 ?�송 로그 (?�력???�을 ?�만)
        if (mask != 0) {
            System.out.println("[GamePanel] ??myId=" + myId + " ?�력 ?�송: mask=" + mask + " W=" + keyW + " S=" + keyS + " A=" + keyA + " D=" + keyD);
        }
        
        sender.send((byte)(mask & 0xFF), aimRad);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        int w = getWidth(), h = getHeight();
        
        // 배경??        g2.setColor(new Color(0x0f1115));
        g2.fillRect(0, 0, w, h);

        // Viewport ?�역 계산 (?�드 좌표계�? ?�면??매핑)
        Rect viewBounds = viewport.getViewBounds(null);
        Point topLeft = viewport.worldToScreen(viewBounds.x, viewBounds.y);
        Point bottomRight = viewport.worldToScreen(viewBounds.x + viewBounds.w, viewBounds.y + viewBounds.h);
        int viewW = bottomRight.x - topLeft.x;
        int viewH = bottomRight.y - topLeft.y;

        // �?배경 ?�더�?(Viewport ?�역??맞춤)
        if (mapBackground != null) {
            g2.drawImage(mapBackground, topLeft.x, topLeft.y, viewW, viewH, null);
        } else {
            // �??��?지가 ?�으�?기본 배경
            g2.setColor(new Color(0x1e232b));
            g2.fillRect(topLeft.x, topLeft.y, viewW, viewH);
        }

        // ?�사�??�더�?(?�?�별 차별??
        for (ProjectilesV2.Entry p : projectiles) {
            if (!p.active) continue;
            Point pp = viewport.worldToScreen(p.x, p.y);
            
            // ?�?�별 ?�상 �??�기 (고정 ?�기)
            Color bulletColor;
            int r;
            switch (p.type) {
                case ProjectilesV2.TYPE_BULLET:
                default:
                    bulletColor = new Color(0xffd54f); // ?��???                    r = 4;
                    break;
            }
            
            // ?�곽??(빛나???�과)
            g2.setColor(new Color(bulletColor.getRed(), bulletColor.getGreen(), bulletColor.getBlue(), 100));
            g2.fillOval(pp.x - r - 2, pp.y - r - 2, (r + 2) * 2, (r + 2) * 2);
            
            // 메인 ?�사�?            g2.setColor(bulletColor);
            g2.fillOval(pp.x - r, pp.y - r, r * 2, r * 2);
            
            // 중앙 ?�이?�이??(반짝??
            g2.setColor(new Color(255, 255, 255, 180));
            int hr = Math.max(1, r / 2);
            g2.fillOval(pp.x - hr, pp.y - hr, hr * 2, hr * 2);
        }

        // ?�레?�어 ?�더�?(캐릭???��?지 + 방향 ?�전)
        for (SnapshotV2.Entry e : players.values()) {
            Point pp = viewport.worldToScreen(e.x, e.y);
            
            BufferedImage charImg = getCharacterImage(e.characterId);
            if (charImg != null) {
                    // 캐릭???��?지�?aim 방향?�로 ?�전 (고정 ?�기 64px)
                    int imgSize = 64;
                
                AffineTransform oldTx = g2.getTransform();
                AffineTransform tx = new AffineTransform();
                tx.translate(pp.x, pp.y);
                tx.rotate(e.aim); // aim ?�디?�으�??�전
                tx.translate(-imgSize / 2.0, -imgSize / 2.0);
                
                g2.setTransform(tx);
                g2.drawImage(charImg, 0, 0, imgSize, imgSize, null);
                g2.setTransform(oldTx);
            } else {
                // ?��?지가 ?�으�??�으�??�시 (고정 ?�기)
                int r = 16;
                Color body = (e.team == 1) ? new Color(0x4f8cff) : new Color(0xff6f61);
                g2.setColor(body);
                g2.fillOval(pp.x - r, pp.y - r, r * 2, r * 2);
            }
            
            // ???�레?�어 강조???�로?�헤?�로 ?��?(?�색 ?�두�??�거)
            
            // ?� ?�상 ?�시 (?��? ??- 고정 ?�기)
            Color teamColor = (e.team == 1) ? new Color(0x4f8cff) : new Color(0xff6f61);
            g2.setColor(teamColor);
            int tr = 5;
            int ty = pp.y - 40;
            g2.fillOval(pp.x - tr, ty, tr * 2, tr * 2);
            
            // ?�레?�어 ID ?�시 (?�네???�???�시)
            g2.setFont(hudFont);
            g2.setColor(Color.WHITE);
            String idText = "#" + e.id;
            int idW = g2.getFontMetrics().stringWidth(idText);
            g2.drawString(idText, pp.x - idW / 2, ty - 8);
        }

        // HUD ?�보 (좌상??
        g2.setFont(hudFont);
        g2.setColor(Color.YELLOW);
        g2.drawString("Players: " + players.size(), 10, 20);
        g2.drawString(String.format("Scale: %.2f | Cam: (%.0f, %.0f)", 
            viewport.getScale(), 
            viewport.getCenter(null).x, 
            viewport.getCenter(null).y), 10, 35);

        // 미니�??�버?�이
        drawMinimap(g2, w, h);
        
        // HUD ?�버?�이
        drawHUD(g2, w, h);
        
        // ?�로?�헤??        drawCrosshair(g2, w, h);

        g2.dispose();
    }

    // ?�상???�버?�이 미니�??�더�?(?�치 조정 - 맵을 가리�? ?�게)
    private void drawMinimap(Graphics2D g2, int panelW, int panelH) {
        if (!minimapEnabled) return;
        
        // ?�드 ?�체 ?�역 가?�오�?        float ww = this.worldW;
        float wh = this.worldH;
        
        if (ww <= 0 || wh <= 0) return;

        // ?�드 비율 ?��??�면??최�? ?�기 ?�에 맞춤
        double msx = MINIMAP_MAX_W / (double) ww;
        double msy = MINIMAP_MAX_H / (double) wh;
        double ms = Math.min(msx, msy);
        int mmW = Math.max(40, (int) Math.round(ww * ms));
        int mmH = Math.max(40, (int) Math.round(wh * ms));

        // ?�치 조정: ?�하?�으�??�동 (맵을 가리�? ?�게)
        int x0 = panelW - MINIMAP_MARGIN - mmW;
        int y0 = panelH - MINIMAP_MARGIN - mmH;

        // 배경 (?�명??
        g2.setColor(new Color(0x0b0d12, true));
        g2.fillRect(x0 - 2, y0 - 2, mmW + 4, mmH + 4);
        g2.setColor(new Color(0x20252e));
        g2.fillRect(x0, y0, mmW, mmH);
        g2.setColor(new Color(0x445062));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRect(x0, y0, mmW, mmH);
        
        // �?구조 ?�시 (간단??경계??
        g2.setColor(new Color(80, 80, 80, 100));
        g2.drawRect(x0 + 2, y0 + 2, mmW - 4, mmH - 4);

        // ?�사�?        g2.setColor(new Color(0xffd54f));
        for (ProjectilesV2.Entry p : projectiles) {
            if (!p.active) continue;
            int mx = x0 + (int) Math.round(p.x * ms);
            int my = y0 + (int) Math.round(p.y * ms);
            g2.fillOval(mx - MINIMAP_DOT_PROJECTILE/2, my - MINIMAP_DOT_PROJECTILE/2, MINIMAP_DOT_PROJECTILE, MINIMAP_DOT_PROJECTILE);
        }

        // ?�레?�어 (같�? ?��??�시)
        SnapshotV2.Entry me = players.get(myId);
        int myTeam = (me != null) ? me.team : -1;
        
        for (SnapshotV2.Entry e : players.values()) {
            // 같�? ?��?미니맵에 ?�시
            if (myTeam >= 0 && e.team != myTeam) continue;
            
            int mx = x0 + (int) Math.round(e.x * ms);
            int my = y0 + (int) Math.round(e.y * ms);
            Color teamColor = (e.team == 1) ? new Color(0x4f8cff) : new Color(0xff6f61);
            g2.setColor(teamColor);
            g2.fillOval(mx - MINIMAP_DOT_PLAYER/2, my - MINIMAP_DOT_PLAYER/2, MINIMAP_DOT_PLAYER, MINIMAP_DOT_PLAYER);
            
            // ??캐릭??강조
            if (e.id == myId) {
                g2.setColor(Color.WHITE);
                g2.drawOval(mx - MINIMAP_DOT_PLAYER/2 - 2, my - MINIMAP_DOT_PLAYER/2 - 2, MINIMAP_DOT_PLAYER + 4, MINIMAP_DOT_PLAYER + 4);
            }
        }
    }
    
    // HUD ?�더�?(?�단: 게임 ?�태, ?�단: HP/?�킬)
    private void drawHUD(Graphics2D g2, int w, int h) {
        // ?�단 좌측: Phase, Countdown, Score
        g2.setFont(hudFontBold);
        int y = 15;
        
        // Phase
        g2.setColor(new Color(180, 220, 255));
        g2.drawString("Phase: " + phaseText, 10, y);
        
        // Countdown (?��???
        if (countdownSeconds >= 0) {
            g2.setColor(new Color(255, 220, 120));
            g2.drawString("Countdown: " + countdownSeconds + "s", 150, y);
        }
        
        // Score (?�색)
        g2.setColor(new Color(200, 255, 200));
        g2.drawString("Score " + blueScore + " : " + redScore, 320, y);
        
        // Ready count
        g2.setColor(new Color(200, 220, 255));
        g2.drawString("Ready " + readyCount + "/" + totalPlayers, 480, y);
        
        // ?�단 ?�측: ?�결 ?�태
        String connText = isConnected ? "??Connected" : "??Disconnected";
        Color connColor = isConnected ? new Color(100, 255, 100) : new Color(255, 100, 100);
        g2.setColor(connColor);
        g2.setFont(hudFont);
        int connW = g2.getFontMetrics().stringWidth(connText);
        g2.drawString(connText, w - connW - 15, 15);
        
        // ?�스??메시지 (중앙 ?�단)
        if (!systemMessage.isEmpty()) {
            g2.setFont(hudFontLarge);
            g2.setColor(new Color(255, 190, 190));
            int msgW = g2.getFontMetrics().stringWidth(systemMessage);
            g2.drawString(systemMessage, (w - msgW) / 2, 50);
        }
        
        // ?�단 좌측: HP �?        drawHealthBar(g2, 20, h - 80);
        
        // ?�단 중앙: ?�킬 쿨다??        drawSkillCooldowns(g2, w / 2 - 100, h - 80);
    }
    
    // HP �??�더�?    private void drawHealthBar(Graphics2D g2, int x, int y) {
        int barW = 200;
        int barH = 20;
        
        // 배경 (?�두??빨강)
        g2.setColor(new Color(60, 20, 20));
        g2.fillRect(x, y, barW, barH);
        
        // HP (밝�? 빨강)
        float hpRatio = (float) myHp / myMaxHp;
        int hpW = Math.round(barW * hpRatio);
        g2.setColor(new Color(220, 50, 50));
        g2.fillRect(x, y, hpW, barH);
        
        // ?�두�?        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(2f));
        g2.drawRect(x, y, barW, barH);
        
        // HP ?�스??        g2.setFont(hudFont);
        String hpText = myHp + " / " + myMaxHp;
        int textW = g2.getFontMetrics().stringWidth(hpText);
        g2.setColor(Color.WHITE);
        g2.drawString(hpText, x + (barW - textW) / 2, y + barH - 5);
    }
    
    // ?�킬 쿨다???�더�?(E: Tactical, Q: Ultimate)
    private void drawSkillCooldowns(Graphics2D g2, int x, int y) {
        int skillSize = 50;
        int gap = 10;
        
        // Tactical (E)
        drawSkillBox(g2, x, y, skillSize, "E", tacticalCooldown, new Color(100, 150, 255));
        
        // Ultimate (Q)
        drawSkillBox(g2, x + skillSize + gap, y, skillSize, "Q", ultimateCooldown, new Color(255, 150, 100));
    }
    
    private void drawSkillBox(Graphics2D g2, int x, int y, int size, String key, float cooldown, Color color) {
        // 배경
        g2.setColor(new Color(30, 30, 30));
        g2.fillRect(x, y, size, size);
        
        // 쿨다???�버?�이 (?�두??반투�?
        if (cooldown > 0.01f) {
            int cdHeight = Math.round(size * cooldown);
            g2.setColor(new Color(0, 0, 0, 180));
            g2.fillRect(x, y, size, cdHeight);
        }
        
        // ?�두�?        g2.setColor(cooldown > 0.01f ? Color.GRAY : color);
        g2.setStroke(new BasicStroke(2f));
        g2.drawRect(x, y, size, size);
        
        // ???�시
        g2.setFont(hudFontBold);
        g2.setColor(Color.WHITE);
        int textW = g2.getFontMetrics().stringWidth(key);
        g2.drawString(key, x + (size - textW) / 2, y + size / 2 + 5);
        
        // 쿨다???�센??        if (cooldown > 0.01f) {
            String cdText = Math.round(cooldown * 100) + "%";
            g2.setFont(hudFont);
            int cdW = g2.getFontMetrics().stringWidth(cdText);
            g2.drawString(cdText, x + (size - cdW) / 2, y + size - 5);
        }
    }
    
    // ?�로?�헤???�더�?(??캐릭???�치???�시 + 조�? 방향)
    private void drawCrosshair(Graphics2D g2, int w, int h) {
        // ??캐릭??찾기
        SnapshotV2.Entry me = players.get(myId);
        if (me == null) return;
        
        // ??캐릭?�의 ?�면 좌표
        Point myScreenPos = viewport.worldToScreen(me.x, me.y);
        int cx = myScreenPos.x;
        int cy = myScreenPos.y;
        
        // 기본 ??��??(??캐릭???�치)
        int len = 12;
        int gap = 5;
        
        g2.setColor(new Color(255, 255, 255, 220));
        g2.setStroke(new BasicStroke(2f));
        
        // ?�하좌우 ?�인
        g2.drawLine(cx - len, cy, cx - gap, cy); // �?        g2.drawLine(cx + gap, cy, cx + len, cy); // ??        g2.drawLine(cx, cy - len, cx, cy - gap); // ??        g2.drawLine(cx, cy + gap, cx, cy + len); // ??        
        // 중앙 ??        g2.fillOval(cx - 2, cy - 2, 4, 4);
        
        // 조�? 방향 ?�시 (aim 각도 - ??캐릭?�의 aim ?�용)
        g2.setColor(new Color(255, 100, 100, 180));
        g2.setStroke(new BasicStroke(3f));
        int aimLen = 40;
        int aimX = cx + (int)(Math.cos(me.aim) * aimLen);
        int aimY = cy + (int)(Math.sin(me.aim) * aimLen);
        g2.drawLine(cx, cy, aimX, aimY);
        
        // 조�????�에 ?��? ??        g2.fillOval(aimX - 3, aimY - 3, 6, 6);
    }
}

