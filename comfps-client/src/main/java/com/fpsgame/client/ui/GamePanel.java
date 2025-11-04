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
 * 게임 렌더 패널 v2 (완전 재구축)
 * - Viewport 카메라 시스템으로 플레이어 추적
 * - Timer 기반 60fps 렌더 루프
 * - 맵 배경 이미지 렌더링
 * - 캐릭터 스프라이트 (방향 회전)
 * - 투사체 & 미니맵 오버레이
 */
public class GamePanel extends JPanel {
    private static final int TARGET_FPS = 60;
    
    // 월드 & 카메라
    private final Viewport viewport;
    private volatile float worldW = 3000f;
    private volatile float worldH = 2000f;
    private volatile int myId = -1;
    // TODO: Use currentMapId for map-specific rendering
    // private volatile int currentMapId = -1;

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
        // Viewport 초기화 (기본 3000x2000 월드, 줌 2.0 적용)
        viewport = new Viewport(3000f, 2000f);
        viewport.setScale(2.0f); // 맵의 절반만 보이도록 줌
        
        setOpaque(false);
        setDoubleBuffered(true);
        setFocusable(true);
        setBackground(new Color(0x101418));

        // 리사이즈 훅
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                Dimension d = getSize();
                viewport.resize(Math.max(1, d.width), Math.max(1, d.height));
                repaint();
            }
        });

        // Key handling (WASD + E, Q, M)
        addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_W -> keyW = true;
                    case KeyEvent.VK_A -> keyA = true;
                    case KeyEvent.VK_S -> keyS = true;
                    case KeyEvent.VK_D -> keyD = true;
                    case KeyEvent.VK_M -> minimapEnabled = !minimapEnabled; // 토글
                    case KeyEvent.VK_E -> {
                        // TODO: Implement tactical ability key
                        ActionSender as = actionSender;
                        if (as != null) as.sendAction(1); // Tactical
                    }
                    case KeyEvent.VK_Q -> {
                        // TODO: Implement ultimate ability key
                        ActionSender as = actionSender;
                        if (as != null) as.sendAction(2); // Ultimate
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
                    // E and Q keys are handled in keyPressed
                    default -> {}
                }
            }
        });
        
        // Resize listener
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                int w = Math.max(1, getWidth());
                int h = Math.max(1, getHeight());
                viewport.resize(w, h);
                // resize 후 다시 원하는 스케일 설정 (2.0배 카메라 줌)
                viewport.setScale(2.0f);
            }
        });

        // Mouse aim & attack
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

        // 입력 전송 타이머 (~30 Hz)
        Timer inputTimer = new Timer(33, ev -> flushInput());
        inputTimer.setRepeats(true);
        inputTimer.start();

        // 렌더 타이머 (60 FPS)
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
        // 화면 크기를 기준으로 적절한 스케일 설정
        int panelW = Math.max(1, getWidth());
        int panelH = Math.max(1, getHeight());
        viewport.resize(panelW, panelH);
        
        // 카메라 줌: 스케일을 2.0으로 설정하여 맵의 절반 크기만 보이도록
        // 즉, 3000x2000 맵에서 1500x1000 영역만 보임
        float targetScale = 2.0f;
        viewport.setScale(targetScale);
        repaint();
    }

    public void setMyId(int id) { 
        this.myId = id; 
        System.out.println("[GamePanel] ★★ myId 설정됨: " + id + " (이전 myId=" + this.myId + ")");
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
    
    // ===== 카메라 & 입력 =====
    
    // 카메라 업데이트 (내 플레이어 추적)
    private void updateCamera() {
        SnapshotV2.Entry me = players.get(myId);
        if (me != null) {
            // 카메라가 플레이어를 중심으로 따라가도록 설정
            // 이렇게 하면 플레이어가 이동할 때 맵이 이동하는 효과가 생김
            viewport.setCenter(me.x, me.y);
        } else {
            // DEBUG: 내 캐릭터를 찾을 수 없음
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
        
        // 리소스에서 맵 이미지 로드 (레거시 방식 참고)
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
        
        // 레거시 방식 참고: 리소스 우선, 파일 폴백
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
        
        // DEBUG: 스냅샷 적용 로그
        if (myId >= 0 && !map.containsKey(myId)) {
            System.out.println("[GamePanel] ★★ WARNING: 스냅샷에 myId=" + myId + " 없음! 받은 IDs: " + map.keySet());
        }
        
        // DEBUG: 스냅샷 위치 확인 (myId와 다른 플레이어 비교)
        if (list != null && list.size() > 0) {
            StringBuilder sb = new StringBuilder("[GamePanel] ★ myId=" + myId + " 스냅샷 받음: ");
            for (SnapshotV2.Entry e : list) {
                String mark = (e.id == myId) ? "★MY★" : "";
                sb.append(String.format("id=%d%s pos=(%.1f,%.1f) ", e.id, mark, e.x, e.y));
            }
            System.out.println(sb.toString());
        }
        
        players.clear();
        players.putAll(map);
        // Repaint는 Timer가 처리
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
        int mask = 0;
        if (keyW) mask |= 0x01;
        if (keyS) mask |= 0x02;
        if (keyA) mask |= 0x04;
        if (keyD) mask |= 0x08;
        
        // DEBUG: 입력 전송 로그 (입력이 있을 때만)
        if (mask != 0) {
            System.out.println("[GamePanel] ★ myId=" + myId + " 입력 전송: mask=" + mask + " W=" + keyW + " S=" + keyS + " A=" + keyA + " D=" + keyD);
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
        
        // 배경색
        g2.setColor(new Color(0x0f1115));
        g2.fillRect(0, 0, w, h);

        // Viewport 영역 계산 (월드 좌표계를 화면에 매핑)
        Rect viewBounds = viewport.getViewBounds(null);
        Point topLeft = viewport.worldToScreen(viewBounds.x, viewBounds.y);
        Point bottomRight = viewport.worldToScreen(viewBounds.x + viewBounds.w, viewBounds.y + viewBounds.h);
        int viewW = bottomRight.x - topLeft.x;
        int viewH = bottomRight.y - topLeft.y;

        // 맵 배경 렌더링 (Viewport 영역에 맞춤)
        if (mapBackground != null) {
            g2.drawImage(mapBackground, topLeft.x, topLeft.y, viewW, viewH, null);
        } else {
            // 맵 이미지가 없으면 기본 배경
            g2.setColor(new Color(0x1e232b));
            g2.fillRect(topLeft.x, topLeft.y, viewW, viewH);
        }

        // 투사체 렌더링 (타입별 차별화)
        for (ProjectilesV2.Entry p : projectiles) {
            if (!p.active) continue;
            Point pp = viewport.worldToScreen(p.x, p.y);
            
            // 타입별 색상 및 크기 (고정 크기)
            Color bulletColor;
            int r;
            switch (p.type) {
                case ProjectilesV2.TYPE_BULLET:
                default:
                    bulletColor = new Color(0xffd54f); // 노란색
                    r = 4;
                    break;
            }
            
            // 외곽선 (빛나는 효과)
            g2.setColor(new Color(bulletColor.getRed(), bulletColor.getGreen(), bulletColor.getBlue(), 100));
            g2.fillOval(pp.x - r - 2, pp.y - r - 2, (r + 2) * 2, (r + 2) * 2);
            
            // 메인 투사체
            g2.setColor(bulletColor);
            g2.fillOval(pp.x - r, pp.y - r, r * 2, r * 2);
            
            // 중앙 하이라이트 (반짝임)
            g2.setColor(new Color(255, 255, 255, 180));
            int hr = Math.max(1, r / 2);
            g2.fillOval(pp.x - hr, pp.y - hr, hr * 2, hr * 2);
        }

        // 플레이어 렌더링 (캐릭터 이미지 + 방향 회전)
        for (SnapshotV2.Entry e : players.values()) {
            Point pp = viewport.worldToScreen(e.x, e.y);
            
            BufferedImage charImg = getCharacterImage(e.characterId);
            if (charImg != null) {
                    // 캐릭터 이미지를 aim 방향으로 회전 (고정 크기 64px)
                    int imgSize = 64;
                
                AffineTransform oldTx = g2.getTransform();
                AffineTransform tx = new AffineTransform();
                tx.translate(pp.x, pp.y);
                tx.rotate(e.aim); // aim 라디안으로 회전
                tx.translate(-imgSize / 2.0, -imgSize / 2.0);
                
                g2.setTransform(tx);
                g2.drawImage(charImg, 0, 0, imgSize, imgSize, null);
                g2.setTransform(oldTx);
            } else {
                // 이미지가 없으면 원으로 표시 (고정 크기)
                int r = 16;
                Color body = (e.team == 1) ? new Color(0x4f8cff) : new Color(0xff6f61);
                g2.setColor(body);
                g2.fillOval(pp.x - r, pp.y - r, r * 2, r * 2);
            }
            
            // 내 플레이어 강조는 크로스헤어로 대체 (흰색 테두리 제거)
            
            // 팀 색상 표시 (작은 원 - 고정 크기)
            Color teamColor = (e.team == 1) ? new Color(0x4f8cff) : new Color(0xff6f61);
            g2.setColor(teamColor);
            int tr = 5;
            int ty = pp.y - 40;
            g2.fillOval(pp.x - tr, ty, tr * 2, tr * 2);
            
            // 플레이어 ID 표시 (닉네임 대신 임시)
            g2.setFont(hudFont);
            g2.setColor(Color.WHITE);
            String idText = "#" + e.id;
            int idW = g2.getFontMetrics().stringWidth(idText);
            g2.drawString(idText, pp.x - idW / 2, ty - 8);
        }

        // HUD 정보 (좌상단)
        g2.setFont(hudFont);
        g2.setColor(Color.YELLOW);
        g2.drawString("Players: " + players.size(), 10, 20);
        g2.drawString(String.format("Scale: %.2f | Cam: (%.0f, %.0f)", 
            viewport.getScale(), 
            viewport.getCenter(null).x, 
            viewport.getCenter(null).y), 10, 35);

        // 미니맵 오버레이
        drawMinimap(g2, w, h);
        
        // HUD 오버레이
        drawHUD(g2, w, h);
        
        // 크로스헤어
        drawCrosshair(g2, w, h);

        g2.dispose();
    }

    // 우상단 오버레이 미니맵 렌더링 (위치 조정 - 맵을 가리지 않게)
    private void drawMinimap(Graphics2D g2, int panelW, int panelH) {
        if (!minimapEnabled) return;
        
        // 월드 전체 영역 가져오기
        float ww = this.worldW;
        float wh = this.worldH;
        
        if (ww <= 0 || wh <= 0) return;

        // 월드 비율 유지하면서 최대 크기 안에 맞춤
        double msx = MINIMAP_MAX_W / (double) ww;
        double msy = MINIMAP_MAX_H / (double) wh;
        double ms = Math.min(msx, msy);
        int mmW = Math.max(40, (int) Math.round(ww * ms));
        int mmH = Math.max(40, (int) Math.round(wh * ms));

        // 위치 조정: 우하단으로 이동 (맵을 가리지 않게)
        int x0 = panelW - MINIMAP_MARGIN - mmW;
        int y0 = panelH - MINIMAP_MARGIN - mmH;

        // 배경 (투명도)
        g2.setColor(new Color(0x0b0d12, true));
        g2.fillRect(x0 - 2, y0 - 2, mmW + 4, mmH + 4);
        g2.setColor(new Color(0x20252e));
        g2.fillRect(x0, y0, mmW, mmH);
        g2.setColor(new Color(0x445062));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRect(x0, y0, mmW, mmH);
        
        // 맵 구조 표시 (간단한 경계선)
        g2.setColor(new Color(80, 80, 80, 100));
        g2.drawRect(x0 + 2, y0 + 2, mmW - 4, mmH - 4);

        // 투사체
        g2.setColor(new Color(0xffd54f));
        for (ProjectilesV2.Entry p : projectiles) {
            if (!p.active) continue;
            int mx = x0 + (int) Math.round(p.x * ms);
            int my = y0 + (int) Math.round(p.y * ms);
            g2.fillOval(mx - MINIMAP_DOT_PROJECTILE/2, my - MINIMAP_DOT_PROJECTILE/2, MINIMAP_DOT_PROJECTILE, MINIMAP_DOT_PROJECTILE);
        }

        // 플레이어 (같은 팀만 표시)
        SnapshotV2.Entry me = players.get(myId);
        int myTeam = (me != null) ? me.team : -1;
        
        for (SnapshotV2.Entry e : players.values()) {
            // 같은 팀만 미니맵에 표시
            if (myTeam >= 0 && e.team != myTeam) continue;
            
            int mx = x0 + (int) Math.round(e.x * ms);
            int my = y0 + (int) Math.round(e.y * ms);
            Color teamColor = (e.team == 1) ? new Color(0x4f8cff) : new Color(0xff6f61);
            g2.setColor(teamColor);
            g2.fillOval(mx - MINIMAP_DOT_PLAYER/2, my - MINIMAP_DOT_PLAYER/2, MINIMAP_DOT_PLAYER, MINIMAP_DOT_PLAYER);
            
            // 내 캐릭터 강조
            if (e.id == myId) {
                g2.setColor(Color.WHITE);
                g2.drawOval(mx - MINIMAP_DOT_PLAYER/2 - 2, my - MINIMAP_DOT_PLAYER/2 - 2, MINIMAP_DOT_PLAYER + 4, MINIMAP_DOT_PLAYER + 4);
            }
        }
    }
    
    // HUD 렌더링 (상단: 게임 상태, 하단: HP/스킬)
    private void drawHUD(Graphics2D g2, int w, int h) {
        // 상단 좌측: Phase, Countdown, Score
        g2.setFont(hudFontBold);
        int y = 15;
        
        // Phase
        g2.setColor(new Color(180, 220, 255));
        g2.drawString("Phase: " + phaseText, 10, y);
        
        // Countdown (노란색)
        if (countdownSeconds >= 0) {
            g2.setColor(new Color(255, 220, 120));
            g2.drawString("Countdown: " + countdownSeconds + "s", 150, y);
        }
        
        // Score (녹색)
        g2.setColor(new Color(200, 255, 200));
        g2.drawString("Score " + blueScore + " : " + redScore, 320, y);
        
        // Ready count
        g2.setColor(new Color(200, 220, 255));
        g2.drawString("Ready " + readyCount + "/" + totalPlayers, 480, y);
        
        // 상단 우측: 연결 상태
        String connText = isConnected ? "● Connected" : "● Disconnected";
        Color connColor = isConnected ? new Color(100, 255, 100) : new Color(255, 100, 100);
        g2.setColor(connColor);
        g2.setFont(hudFont);
        int connW = g2.getFontMetrics().stringWidth(connText);
        g2.drawString(connText, w - connW - 15, 15);
        
        // 시스템 메시지 (중앙 상단)
        if (!systemMessage.isEmpty()) {
            g2.setFont(hudFontLarge);
            g2.setColor(new Color(255, 190, 190));
            int msgW = g2.getFontMetrics().stringWidth(systemMessage);
            g2.drawString(systemMessage, (w - msgW) / 2, 50);
        }
        
        // 하단 좌측: HP 바
        drawHealthBar(g2, 20, h - 80);
        
        // 하단 중앙: 스킬 쿨다운
        drawSkillCooldowns(g2, w / 2 - 100, h - 80);
    }
    
    // HP 바 렌더링
    private void drawHealthBar(Graphics2D g2, int x, int y) {
        int barW = 200;
        int barH = 20;
        
        // 배경 (어두운 빨강)
        g2.setColor(new Color(60, 20, 20));
        g2.fillRect(x, y, barW, barH);
        
        // HP (밝은 빨강)
        float hpRatio = (float) myHp / myMaxHp;
        int hpW = Math.round(barW * hpRatio);
        g2.setColor(new Color(220, 50, 50));
        g2.fillRect(x, y, hpW, barH);
        
        // 테두리
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(2f));
        g2.drawRect(x, y, barW, barH);
        
        // HP 텍스트
        g2.setFont(hudFont);
        String hpText = myHp + " / " + myMaxHp;
        int textW = g2.getFontMetrics().stringWidth(hpText);
        g2.setColor(Color.WHITE);
        g2.drawString(hpText, x + (barW - textW) / 2, y + barH - 5);
    }
    
    // 스킬 쿨다운 렌더링 (E: Tactical, Q: Ultimate)
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
        
        // 쿨다운 오버레이 (어두운 반투명)
        if (cooldown > 0.01f) {
            int cdHeight = Math.round(size * cooldown);
            g2.setColor(new Color(0, 0, 0, 180));
            g2.fillRect(x, y, size, cdHeight);
        }
        
        // 테두리
        g2.setColor(cooldown > 0.01f ? Color.GRAY : color);
        g2.setStroke(new BasicStroke(2f));
        g2.drawRect(x, y, size, size);
        
        // 키 표시
        g2.setFont(hudFontBold);
        g2.setColor(Color.WHITE);
        int textW = g2.getFontMetrics().stringWidth(key);
        g2.drawString(key, x + (size - textW) / 2, y + size / 2 + 5);
        
        // 쿨다운 퍼센트
        if (cooldown > 0.01f) {
            String cdText = Math.round(cooldown * 100) + "%";
            g2.setFont(hudFont);
            int cdW = g2.getFontMetrics().stringWidth(cdText);
            g2.drawString(cdText, x + (size - cdW) / 2, y + size - 5);
        }
    }
    
    // 크로스헤어 렌더링 (내 캐릭터 위치에 표시 + 조준 방향)
    private void drawCrosshair(Graphics2D g2, int w, int h) {
        // 내 캐릭터 찾기
        SnapshotV2.Entry me = players.get(myId);
        if (me == null) return;
        
        // 내 캐릭터의 화면 좌표
        Point myScreenPos = viewport.worldToScreen(me.x, me.y);
        int cx = myScreenPos.x;
        int cy = myScreenPos.y;
        
        // 기본 십자선 (내 캐릭터 위치)
        int len = 12;
        int gap = 5;
        
        g2.setColor(new Color(255, 255, 255, 220));
        g2.setStroke(new BasicStroke(2f));
        
        // 상하좌우 라인
        g2.drawLine(cx - len, cy, cx - gap, cy); // 좌
        g2.drawLine(cx + gap, cy, cx + len, cy); // 우
        g2.drawLine(cx, cy - len, cx, cy - gap); // 상
        g2.drawLine(cx, cy + gap, cx, cy + len); // 하
        
        // 중앙 점
        g2.fillOval(cx - 2, cy - 2, 4, 4);
        
        // 조준 방향 표시 (aim 각도 - 내 캐릭터의 aim 사용)
        g2.setColor(new Color(255, 100, 100, 180));
        g2.setStroke(new BasicStroke(3f));
        int aimLen = 40;
        int aimX = cx + (int)(Math.cos(me.aim) * aimLen);
        int aimY = cy + (int)(Math.sin(me.aim) * aimLen);
        g2.drawLine(cx, cy, aimX, aimY);
        
        // 조준선 끝에 작은 원
        g2.fillOval(aimX - 3, aimY - 3, 6, 6);
    }
}

