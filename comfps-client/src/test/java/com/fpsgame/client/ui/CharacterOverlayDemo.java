package com.fpsgame.client.ui;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * 캐릭터 스프라이트가 빨간점 위에 올바르게 렌더링되는지 시각적으로 확인하는 데모
 * 
 * 조작법:
 * - SPACE: 스프라이트 표시/숨김 토글
 * - +/-: 확대/축소
 * - ESC: 종료
 */
public class CharacterOverlayDemo extends JFrame {
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            CharacterOverlayDemo demo = new CharacterOverlayDemo();
            demo.setVisible(true);
        });
    }
    
    public CharacterOverlayDemo() {
        setTitle("캐릭터 스프라이트 오버레이 데모");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);
        
        DemoPanel panel = new DemoPanel();
        add(panel);
        
        // 하단 정보 패널
        JPanel infoPanel = new JPanel();
        infoPanel.setBackground(new Color(0x2a2a2a));
        infoPanel.setPreferredSize(new Dimension(800, 50));
        JLabel infoLabel = new JLabel();
        infoLabel.setForeground(Color.WHITE);
        infoLabel.setFont(new Font("Monospaced", Font.PLAIN, 12));
        infoPanel.add(infoLabel);
        add(infoPanel, BorderLayout.SOUTH);
        
        // 정보 업데이트
        Timer infoTimer = new Timer(100, e -> {
            String info = String.format(
                "스프라이트: %s | 확대: %.1fx | 조작: SPACE=토글, +/-=줌, ESC=종료",
                panel.showSprite ? "ON" : "OFF",
                panel.scale
            );
            infoLabel.setText(info);
        });
        infoTimer.start();
    }
    
    static class DemoPanel extends JPanel {
        private boolean showSprite = true;
        private float scale = 1.5f;
        
        // 플레이어 위치들 (여러 개)
        private final Point[] playerPositions = {
            new Point(200, 150),
            new Point(400, 200),
            new Point(600, 350),
            new Point(300, 450)
        };
        
        // 캐릭터 스프라이트 캐시
        private BufferedImage characterSprite;
        
        public DemoPanel() {
            setBackground(new Color(0x1e232b));
            setFocusable(true);
            
            // 캐릭터 스프라이트 생성 (초록색 원)
            createCharacterSprite();
            
            // 키보드 입력
            addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    switch (e.getKeyCode()) {
                        case KeyEvent.VK_SPACE -> {
                            showSprite = !showSprite;
                            repaint();
                        }
                        case KeyEvent.VK_PLUS, KeyEvent.VK_EQUALS -> {
                            scale = Math.min(scale * 1.2f, 5.0f);
                            repaint();
                        }
                        case KeyEvent.VK_MINUS -> {
                            scale = Math.max(scale / 1.2f, 0.5f);
                            repaint();
                        }
                        case KeyEvent.VK_ESCAPE -> {
                            System.exit(0);
                        }
                    }
                }
            });
            
            // 렌더 루프
            Timer renderTimer = new Timer(16, e -> repaint());
            renderTimer.start();
        }
        
        private void createCharacterSprite() {
            int size = 48;
            characterSprite = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = characterSprite.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // 투명 배경은 기본값
            
            // 초록색 원형 바디
            int radius = 18;
            g.setColor(new Color(0x24D05A));
            g.fillOval(size/2 - radius, size/2 - radius, radius * 2, radius * 2);
            
            // 테두리
            g.setColor(new Color(0x0F8A3E));
            g.setStroke(new BasicStroke(2f));
            g.drawOval(size/2 - radius, size/2 - radius, radius * 2, radius * 2);
            
            // 방향 표시 (작은 흰색 점)
            g.setColor(Color.WHITE);
            g.fillOval(size/2 + radius - 6, size/2 - 3, 6, 6);
            
            g.dispose();
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // 제목
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("SansSerif", Font.BOLD, 16));
            g2.drawString("빨간점(플레이어) 위에 캐릭터 스프라이트 렌더링", 20, 30);
            
            // 설명
            g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
            g2.setColor(new Color(0xcccccc));
            g2.drawString("→ 빨간점이 먼저 그려지고, 그 위에 초록색 스프라이트가 렌더링됩니다", 20, 50);
            
            // 각 플레이어 렌더링
            for (Point pos : playerPositions) {
                // 1단계: 빨간점 그리기 (배경)
                int dotRadius = (int)(14 * scale);
                g2.setColor(new Color(0xFF3A3A));
                g2.fillOval(pos.x - dotRadius, pos.y - dotRadius, dotRadius * 2, dotRadius * 2);
                
                // 2단계: 캐릭터 스프라이트 그리기 (전경)
                if (showSprite && characterSprite != null) {
                    int spriteSize = (int)(48 * scale);
                    int sx = pos.x - spriteSize / 2;
                    int sy = pos.y - spriteSize / 2;
                    g2.drawImage(characterSprite, sx, sy, spriteSize, spriteSize, null);
                }
                
                // 플레이어 ID 표시
                g2.setColor(Color.YELLOW);
                g2.setFont(new Font("Monospaced", Font.BOLD, 11));
                String label = showSprite ? "P" : "P (스프라이트 OFF)";
                g2.drawString(label, pos.x + (int)(20 * scale), pos.y);
            }
            
            // 범례
            g2.setColor(new Color(0x444444));
            g2.fillRect(20, getHeight() - 120, 350, 70);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("SansSerif", Font.BOLD, 13));
            g2.drawString("렌더 순서:", 30, getHeight() - 100);
            
            // 1단계
            g2.setColor(new Color(0xFF3A3A));
            g2.fillOval(40, getHeight() - 85, 20, 20);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
            g2.drawString("1. 빨간점 (플레이어 위치)", 70, getHeight() - 70);
            
            // 2단계
            if (showSprite) {
                g2.drawImage(characterSprite, 40, getHeight() - 60, 20, 20, null);
                g2.drawString("2. 캐릭터 스프라이트 (전경)", 70, getHeight() - 45);
            } else {
                g2.setColor(new Color(0x888888));
                g2.drawString("2. 캐릭터 스프라이트 (OFF)", 70, getHeight() - 45);
            }
            
            g2.dispose();
        }
    }
}
