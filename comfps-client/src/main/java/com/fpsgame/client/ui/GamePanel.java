package com.fpsgame.client.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.JPanel;
import javax.swing.Timer;

import com.fpsgame.common.ProjectilesV2;
import com.fpsgame.common.SnapshotV2;

/**
 * 간단한 게임 렌더 패널(v1)
 * - 서버 SNAPSHOT v2, PROJECTILES v2를 수신하여 2D로 그린다.
 * - 월드 좌표계(worldW x worldH)를 화면 크기에 맞게 스케일링
 */
public class GamePanel extends JPanel {
    private volatile int worldW = 3000;
    private volatile int worldH = 2000;
    private volatile int myId = -1;
    private volatile int currentMapId = -1; // 현재 맵 ID

    private final Map<Integer, SnapshotV2.Entry> players = new ConcurrentHashMap<>();
    private volatile List<ProjectilesV2.Entry> projectiles = java.util.Collections.emptyList();

    // 맵 배경 이미지 캐시
    private BufferedImage mapBackground = null;
    
    // 캐릭터 이미지 캐시 (characterId -> 이미지)
    private final Map<Integer, BufferedImage> characterImages = new HashMap<>();

    // Input state
    private volatile boolean keyW, keyA, keyS, keyD, keyE, keyQ;
    private volatile Float aimRad = null; // nullable until computed

    // Sender binding
    public interface InputSender { void send(byte mask, java.lang.Float aimNullable); }
    public interface ActionSender { void sendAction(int actionType); }
    private volatile InputSender inputSender;
    private volatile ActionSender actionSender;
    private Timer inputTimer;

    public GamePanel() {
        setOpaque(false);
        setFocusable(true);
        setBackground(new Color(0x101418));

        // Key handling (WASD + E, Q)
        addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_W -> keyW = true;
                    case KeyEvent.VK_A -> keyA = true;
                    case KeyEvent.VK_S -> keyS = true;
                    case KeyEvent.VK_D -> keyD = true;
                    case KeyEvent.VK_E -> {
                        keyE = true;
                        ActionSender as = actionSender;
                        if (as != null) as.sendAction(1); // Tactical
                    }
                    case KeyEvent.VK_Q -> {
                        keyQ = true;
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
                    case KeyEvent.VK_E -> keyE = false;
                    case KeyEvent.VK_Q -> keyQ = false;
                    default -> {}
                }
            }
        });

        // Mouse aim handling
        MouseAdapter mouse = new MouseAdapter() {
            @Override public void mouseMoved(MouseEvent e) { updateAimFromMouse(e.getPoint()); }
            @Override public void mouseDragged(MouseEvent e) { updateAimFromMouse(e.getPoint()); }
            @Override public void mouseEntered(MouseEvent e) { requestFocusInWindow(); }
            @Override public void mouseClicked(MouseEvent e) {
                requestFocusInWindow();
                // Left click = Basic Attack
                if (e.getButton() == MouseEvent.BUTTON1) {
                    ActionSender as = actionSender;
                    if (as != null) as.sendAction(0); // BasicAttack
                }
            }
        };
        addMouseMotionListener(mouse);
        addMouseListener(mouse);

        // Send inputs at ~30 Hz if bound
        inputTimer = new Timer(33, ev -> flushInput());
        inputTimer.setRepeats(true);
        inputTimer.start();
    }

    public void setWorldSize(int w, int h) {
        this.worldW = Math.max(1, w);
        this.worldH = Math.max(1, h);
        repaint();
    }

    public void setMyId(int id) { this.myId = id; }

    /** Bind a sender that transmits input to server. */
    public void setInputSender(InputSender sender) { this.inputSender = sender; }

    /** Bind action sender for attacks/skills. */
    public void setActionSender(ActionSender sender) { this.actionSender = sender; }
    
    /** Set map ID and load background image. */
    public void setMapId(int mapId) {
        this.currentMapId = mapId;
        loadMapBackground(mapId);
        repaint();
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
            return;
        }
        
        // 리소스에서 맵 이미지 로드
        BufferedImage img = ImageUtil.loadResource(GamePanel.class, "/assets/maps/" + mapName + ".png");
        if (img == null) img = ImageUtil.loadResource(GamePanel.class, "/assets/maps/" + mapName + ".jpg");
        if (img == null) img = ImageUtil.loadFile("assets/maps/" + mapName + ".png");
        if (img == null) img = ImageUtil.loadFile("assets/maps/" + mapName + ".jpg");
        
        mapBackground = img;
        if (img == null) {
            System.err.println("Failed to load map background: " + mapName);
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
        
        if (charName == null) return null;
        
        BufferedImage img = ImageUtil.loadResource(GamePanel.class, "/assets/characters/" + charName + ".png");
        if (img == null) img = ImageUtil.loadResource(GamePanel.class, "/assets/characters/" + charName + ".jpg");
        if (img == null) img = ImageUtil.loadFile("assets/characters/" + charName + ".png");
        if (img == null) img = ImageUtil.loadFile("assets/characters/" + charName + ".jpg");
        
        if (img != null) {
            // 흰색 배경 제거
            img = ImageUtil.whiteToTransparent(img, 20);
            characterImages.put(characterId, img);
        }
        
        return img;
    }

    public void applySnapshot(List<SnapshotV2.Entry> list) {
        if (list == null) return;
        Map<Integer, SnapshotV2.Entry> map = new ConcurrentHashMap<>(Math.max(16, list.size()*2));
        for (SnapshotV2.Entry e : list) map.put(e.id, e);
        players.clear();
        players.putAll(map);
        repaint();
    }

    public void applyProjectiles(List<ProjectilesV2.Entry> list) {
        this.projectiles = (list == null) ? java.util.Collections.emptyList() : list;
        repaint();
    }

    private void updateAimFromMouse(Point p) {
        SnapshotV2.Entry me = players.get(myId);
        if (me == null || p == null) { aimRad = null; return; }
        int w = getWidth(), h = getHeight();
        double sx = (w - 64) / (double) worldW;
        double sy = (h - 64) / (double) worldH;
        double s = Math.min(sx, sy);
        double ox = (w - worldW * s) * 0.5;
        double oy = (h - worldH * s) * 0.5;
        double worldX = (p.x - ox) / s;
        double worldY = (p.y - oy) / s;
        double dx = worldX - me.x;
        double dy = worldY - me.y;
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
        sender.send((byte)(mask & 0xFF), aimRad);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth(), h = getHeight();
        
        // 배경색
        g2.setColor(new Color(0x0f1115));
        g2.fillRect(0, 0, w, h);

        // 월드 좌표계 스케일 계산
        double sx = (w - 64) / (double) worldW;
        double sy = (h - 64) / (double) worldH;
        double s = Math.min(sx, sy);
        double ox = (w - worldW * s) * 0.5;
        double oy = (h - worldH * s) * 0.5;

        // 맵 배경 그리기
        if (mapBackground != null) {
            int mapW = (int) (worldW * s);
            int mapH = (int) (worldH * s);
            g2.drawImage(mapBackground, (int)ox, (int)oy, mapW, mapH, null);
        } else {
            // 맵 이미지가 없으면 기본 배경
            g2.setColor(new Color(0x1e232b));
            g2.fillRect((int)ox, (int)oy, (int)(worldW * s), (int)(worldH * s));
        }

        // 투사체 렌더링
        g2.setStroke(new BasicStroke(2f));
        g2.setColor(new Color(0xffd54f));
        for (ProjectilesV2.Entry p : projectiles) {
            if (!p.active) continue;
            int px = (int) Math.round(ox + p.x * s);
            int py = (int) Math.round(oy + p.y * s);
            g2.fillOval(px-3, py-3, 6, 6);
        }

        // 플레이어 렌더링 (캐릭터 이미지 사용)
        for (SnapshotV2.Entry e : players.values()) {
            int px = (int) Math.round(ox + e.x * s);
            int py = (int) Math.round(oy + e.y * s);
            
            BufferedImage charImg = getCharacterImage(e.characterId);
            if (charImg != null) {
                // 캐릭터 이미지를 적절한 크기로 렌더링
                int imgSize = 32; // 기본 크기
                g2.drawImage(charImg, px - imgSize/2, py - imgSize/2, imgSize, imgSize, null);
            } else {
                // 이미지가 없으면 원으로 표시
                Color body = (e.team == 1) ? new Color(0x4f8cff) : new Color(0xff6f61);
                g2.setColor(body);
                g2.fillOval(px-8, py-8, 16, 16);
            }
            
            // 내 플레이어 강조 (흰색 테두리)
            if (e.id == myId) {
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(2f));
                g2.drawOval(px-12, py-12, 24, 24);
            }
            
            // 팀 색상 표시 (작은 원)
            Color teamColor = (e.team == 1) ? new Color(0x4f8cff) : new Color(0xff6f61);
            g2.setColor(teamColor);
            g2.fillOval(px - 3, py - 15, 6, 6);
        }

        g2.dispose();
    }
}
