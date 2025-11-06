package com.fpsgame.client.test;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * 카메라 팔로우 시스템 테스트 클래스
 * 
 * 목적:
 * 1. 맵 확대 + 캐릭터 중심 카메라 시스템 검증
 * 2. 각 클라이언트가 자신의 캐릭터만 조작하는지 확인
 * 3. 다른 캐릭터는 서버 동기화 위치만 표시되는지 확인
 * 
 * 조작법:
 * - WASD: 현재 플레이어 이동
 * - 1~3: 조작할 플레이어 전환 (다중 플레이어 시뮬레이션)
 * - +/-: 카메라 줌 인/아웃
 * - Space: 자동 이동 시뮬레이션 토글
 */
public class CameraFollowTest extends JFrame {
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            CameraFollowTest test = new CameraFollowTest();
            test.setVisible(true);
        });
    }
    
    public CameraFollowTest() {
        setTitle("카메라 팔로우 시스템 테스트");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);
        
        TestPanel panel = new TestPanel();
        add(panel);
        
        // 정보 패널
        JPanel infoPanel = new JPanel();
        infoPanel.setBackground(new Color(0x2a2a2a));
        infoPanel.setPreferredSize(new Dimension(1200, 60));
        JLabel infoLabel = new JLabel();
        infoLabel.setForeground(Color.WHITE);
        infoLabel.setFont(new Font("Monospaced", Font.PLAIN, 12));
        infoPanel.add(infoLabel);
        add(infoPanel, BorderLayout.SOUTH);
        
        // 정보 업데이트 타이머
        javax.swing.Timer infoTimer = new javax.swing.Timer(100, e -> {
            Player current = panel.getCurrentPlayer();
            if (current != null) {
                String info = String.format(
                    "현재 플레이어: %d | 위치: (%.1f, %.1f) | 카메라: (%.1f, %.1f) | 줌: %.2fx | " +
                    "조작: WASD=이동, 1-3=플레이어전환, +/-=줌, Space=자동이동",
                    current.id, current.x, current.y, 
                    panel.cameraX, panel.cameraY, panel.cameraScale
                );
                infoLabel.setText(info);
            }
        });
        infoTimer.start();
    }
    
    /**
     * 테스트용 플레이어 클래스
     */
    static class Player {
        int id;
        float x, y;
        float vx, vy;
        int team; // 1=Blue, 2=Red
        Color color;
        boolean controlled; // 현재 조작 중인 플레이어인가?
        
        // 자동 이동용
        float targetX, targetY;
        boolean autoMove = false;
        
        Player(int id, float x, float y, int team) {
            this.id = id;
            this.x = x;
            this.y = y;
            this.team = team;
            this.color = (team == 1) ? new Color(0x4f8cff) : new Color(0xff6f61);
            this.vx = 0;
            this.vy = 0;
            this.controlled = false;
        }
        
        void update(float deltaTime, float worldW, float worldH) {
            if (autoMove) {
                // 자동 이동: 목표 지점으로 이동
                float dx = targetX - x;
                float dy = targetY - y;
                float dist = (float) Math.sqrt(dx * dx + dy * dy);
                
                if (dist > 5) {
                    vx = (dx / dist) * 200; // 속도
                    vy = (dy / dist) * 200;
                } else {
                    // 새로운 랜덤 목표 설정
                    targetX = (float) (Math.random() * worldW);
                    targetY = (float) (Math.random() * worldH);
                }
            }
            
            // 위치 업데이트
            x += vx * deltaTime;
            y += vy * deltaTime;
            
            // 월드 경계 체크
            if (x < 0) { x = 0; vx = 0; }
            if (x > worldW) { x = worldW; vx = 0; }
            if (y < 0) { y = 0; vy = 0; }
            if (y > worldH) { y = worldH; vy = 0; }
        }
    }
    
    /**
     * 테스트용 렌더링 패널
     */
    static class TestPanel extends JPanel {
        // 월드 설정
        private static final float WORLD_WIDTH = 3000f;
        private static final float WORLD_HEIGHT = 2000f;
        
        // 카메라 설정
        float cameraX, cameraY;
        float cameraScale = 0.5f; // 맵 확대 (2배)
        
        // 플레이어들
        private final Map<Integer, Player> players = new ConcurrentHashMap<>();
        private int currentPlayerId = 1;
        
        // 입력 상태
        private boolean keyW, keyA, keyS, keyD;
        private boolean autoMoveAll = false;
        
        // 맵 이미지 (시뮬레이션용)
        private BufferedImage mapImage;
        
        public TestPanel() {
            setBackground(new Color(0x0f1115));
            setFocusable(true);
            
            // 테스트용 플레이어 3명 생성
            players.put(1, new Player(1, WORLD_WIDTH * 0.3f, WORLD_HEIGHT * 0.3f, 1));
            players.put(2, new Player(2, WORLD_WIDTH * 0.7f, WORLD_HEIGHT * 0.3f, 2));
            players.put(3, new Player(3, WORLD_WIDTH * 0.5f, WORLD_HEIGHT * 0.7f, 1));
            
            // 초기 조작 플레이어 설정
            players.get(currentPlayerId).controlled = true;
            
            // 카메라 초기 위치
            Player current = players.get(currentPlayerId);
            cameraX = current.x;
            cameraY = current.y;
            
            // 맵 이미지 생성 (그리드 패턴)
            createMapImage();
            
            // 키보드 입력
            addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    switch (e.getKeyCode()) {
                        case KeyEvent.VK_W -> keyW = true;
                        case KeyEvent.VK_A -> keyA = true;
                        case KeyEvent.VK_S -> keyS = true;
                        case KeyEvent.VK_D -> keyD = true;
                        case KeyEvent.VK_1 -> switchPlayer(1);
                        case KeyEvent.VK_2 -> switchPlayer(2);
                        case KeyEvent.VK_3 -> switchPlayer(3);
                        case KeyEvent.VK_PLUS, KeyEvent.VK_EQUALS -> {
                            cameraScale = Math.min(cameraScale * 1.2f, 3.0f);
                        }
                        case KeyEvent.VK_MINUS -> {
                            cameraScale = Math.max(cameraScale / 1.2f, 0.1f);
                        }
                        case KeyEvent.VK_SPACE -> toggleAutoMove();
                    }
                }
                
                @Override
                public void keyReleased(KeyEvent e) {
                    switch (e.getKeyCode()) {
                        case KeyEvent.VK_W -> keyW = false;
                        case KeyEvent.VK_A -> keyA = false;
                        case KeyEvent.VK_S -> keyS = false;
                        case KeyEvent.VK_D -> keyD = false;
                    }
                }
            });
            
            // 게임 루프 (60 FPS)
            javax.swing.Timer gameLoop = new javax.swing.Timer(16, e -> {
                updateGame(0.016f);
                repaint();
            });
            gameLoop.start();
        }
        
        private void createMapImage() {
            mapImage = new BufferedImage((int)WORLD_WIDTH, (int)WORLD_HEIGHT, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = mapImage.createGraphics();
            
            // 배경
            g.setColor(new Color(0x1e232b));
            g.fillRect(0, 0, (int)WORLD_WIDTH, (int)WORLD_HEIGHT);
            
            // 그리드
            g.setColor(new Color(0x2a3038));
            g.setStroke(new BasicStroke(2));
            for (int x = 0; x < WORLD_WIDTH; x += 100) {
                g.drawLine(x, 0, x, (int)WORLD_HEIGHT);
            }
            for (int y = 0; y < WORLD_HEIGHT; y += 100) {
                g.drawLine(0, y, (int)WORLD_WIDTH, y);
            }
            
            // 중앙선 (더 굵게)
            g.setColor(new Color(0x4a5058));
            g.setStroke(new BasicStroke(4));
            g.drawLine((int)(WORLD_WIDTH / 2), 0, (int)(WORLD_WIDTH / 2), (int)WORLD_HEIGHT);
            g.drawLine(0, (int)(WORLD_HEIGHT / 2), (int)WORLD_WIDTH, (int)(WORLD_HEIGHT / 2));
            
            // 코너 마커
            g.setColor(Color.YELLOW);
            g.fillOval(0, 0, 50, 50);
            g.fillOval((int)WORLD_WIDTH - 50, 0, 50, 50);
            g.fillOval(0, (int)WORLD_HEIGHT - 50, 50, 50);
            g.fillOval((int)WORLD_WIDTH - 50, (int)WORLD_HEIGHT - 50, 50, 50);
            
            g.dispose();
        }
        
        private void switchPlayer(int playerId) {
            if (!players.containsKey(playerId)) return;
            
            // 이전 플레이어 제어 해제
            Player prev = players.get(currentPlayerId);
            if (prev != null) {
                prev.controlled = false;
                prev.vx = 0;
                prev.vy = 0;
            }
            
            // 새 플레이어 제어
            currentPlayerId = playerId;
            Player current = players.get(currentPlayerId);
            current.controlled = true;
            
            System.out.println("플레이어 전환: " + playerId);
        }
        
        private void toggleAutoMove() {
            autoMoveAll = !autoMoveAll;
            for (Player p : players.values()) {
                if (!p.controlled) {
                    p.autoMove = autoMoveAll;
                    if (autoMoveAll) {
                        p.targetX = (float) (Math.random() * WORLD_WIDTH);
                        p.targetY = (float) (Math.random() * WORLD_HEIGHT);
                    } else {
                        p.vx = 0;
                        p.vy = 0;
                    }
                }
            }
            System.out.println("자동 이동: " + (autoMoveAll ? "ON" : "OFF"));
        }
        
        private void updateGame(float deltaTime) {
            Player current = players.get(currentPlayerId);
            if (current == null) return;
            
            // 현재 플레이어만 키보드로 조작
            if (current.controlled) {
                float speed = 300f; // 픽셀/초
                current.vx = 0;
                current.vy = 0;
                
                if (keyW) current.vy -= speed;
                if (keyS) current.vy += speed;
                if (keyA) current.vx -= speed;
                if (keyD) current.vx += speed;
                
                // 대각선 이동 보정
                if (current.vx != 0 && current.vy != 0) {
                    float factor = (float) (1.0 / Math.sqrt(2));
                    current.vx *= factor;
                    current.vy *= factor;
                }
            }
            
            // 모든 플레이어 업데이트
            for (Player p : players.values()) {
                p.update(deltaTime, WORLD_WIDTH, WORLD_HEIGHT);
            }
            
            // 카메라를 현재 플레이어를 따라가도록 업데이트 (부드러운 이동)
            float lerpFactor = 0.1f;
            cameraX += (current.x - cameraX) * lerpFactor;
            cameraY += (current.y - cameraY) * lerpFactor;
            
            // 카메라 경계 제한
            float halfViewW = getWidth() / (2f * cameraScale);
            float halfViewH = getHeight() / (2f * cameraScale);
            
            cameraX = Math.max(halfViewW, Math.min(cameraX, WORLD_WIDTH - halfViewW));
            cameraY = Math.max(halfViewH, Math.min(cameraY, WORLD_HEIGHT - halfViewH));
        }
        
        public Player getCurrentPlayer() {
            return players.get(currentPlayerId);
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            int screenW = getWidth();
            int screenH = getHeight();
            
            // 월드 -> 스크린 변환 계산
            // 카메라 중심을 화면 중앙에 배치
            float offsetX = screenW / 2f - cameraX * cameraScale;
            float offsetY = screenH / 2f - cameraY * cameraScale;
            
            // 맵 배경 렌더링 (확대됨)
            if (mapImage != null) {
                int mapW = (int) (WORLD_WIDTH * cameraScale);
                int mapH = (int) (WORLD_HEIGHT * cameraScale);
                g2.drawImage(mapImage, (int)offsetX, (int)offsetY, mapW, mapH, null);
            }
            
            // 플레이어 렌더링
            for (Player p : players.values()) {
                int screenX = (int) (p.x * cameraScale + offsetX);
                int screenY = (int) (p.y * cameraScale + offsetY);
                
                // 캐릭터 원
                int radius = (int) (20 * cameraScale);
                if (radius < 5) radius = 5;
                
                // 조작 중인 플레이어는 테두리 표시
                if (p.controlled) {
                    g2.setColor(Color.YELLOW);
                    g2.setStroke(new BasicStroke(3));
                    g2.drawOval(screenX - radius - 5, screenY - radius - 5, 
                               (radius + 5) * 2, (radius + 5) * 2);
                }
                
                // 캐릭터 본체
                g2.setColor(p.color);
                g2.fillOval(screenX - radius, screenY - radius, radius * 2, radius * 2);
                
                // ID 표시
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Arial", Font.BOLD, 14));
                String idText = "P" + p.id;
                FontMetrics fm = g2.getFontMetrics();
                int textW = fm.stringWidth(idText);
                g2.drawString(idText, screenX - textW / 2, screenY + 5);
                
                // 속도 벡터 표시 (이동 중일 때)
                if (p.vx != 0 || p.vy != 0) {
                    g2.setColor(Color.GREEN);
                    g2.setStroke(new BasicStroke(2));
                    int endX = (int) (screenX + p.vx * cameraScale * 0.1f);
                    int endY = (int) (screenY + p.vy * cameraScale * 0.1f);
                    g2.drawLine(screenX, screenY, endX, endY);
                }
            }
            
            // 화면 중앙에 십자선 표시 (카메라 중심)
            g2.setColor(new Color(255, 255, 255, 100));
            g2.setStroke(new BasicStroke(1));
            g2.drawLine(screenW / 2 - 10, screenH / 2, screenW / 2 + 10, screenH / 2);
            g2.drawLine(screenW / 2, screenH / 2 - 10, screenW / 2, screenH / 2 + 10);
            
            g2.dispose();
        }
    }
}
