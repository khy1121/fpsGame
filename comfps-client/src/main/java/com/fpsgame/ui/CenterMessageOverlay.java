package com.fpsgame.client.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.geom.RoundRectangle2D;

/**
 * 화면 중앙에 라운드 시작/알림/타이틀 등의 텍스트를 잠시 표시하는 오버레이 컴포넌트.
 *
 * 특징:
 * - 투명 GlassPane 위에 올려 사용하기 좋은 경량 컴포넌트
 * - {@link #showMessage(String, int)} 한 메서드로 간단 사용(내부 타이머 애니메이션)
 * - 여러 메시지가 연속 호출되면 이전 애니메이션을 정리하고 교체
 * - EDT 안전: 내부에서 EDT로 디스패치
 *
 * 사용 예:
 * <pre>
 *   // 1) 독립 컴포넌트로 직접 사용(JLayeredPane 위에 add)
 *   CenterMessageOverlay overlay = new CenterMessageOverlay();
 *   layeredPane.add(overlay, Integer.valueOf(30));
 *   overlay.setBounds(0, 0, width, height);
 *   overlay.showMessage("ROUND START", 1500);
 *
 *   // 2) GlassPane에 설치하여 사용
 *   CenterMessageOverlay.install(frame).showMessage("ROUND START", 1500);
 * </pre>
 */
public final class CenterMessageOverlay extends JComponent {

    /** 현재 표시할 텍스트 */
    private volatile String text = "";

    /** 알파(0..1) */
    private volatile float alpha = 0f;

    /** 애니메이션 타이머 */
    private Timer anim;

    /**
     * 기본 생성자(public 유지). HudOverlayLayer 등에서 독립 컴포넌트로 직접 생성해 사용 가능.
     */
    public CenterMessageOverlay() {
        setOpaque(false);
        setFocusable(false);
    }

    /**
     * 주어진 윈도우의 GlassPane 위에 오버레이를 설치하고 반환한다.
     * 이미 설치되어 있으면 기존 인스턴스를 반환.
     */
    public static CenterMessageOverlay install(Window owner) {
        if (!(owner instanceof RootPaneContainer rpc)) {
            throw new IllegalArgumentException("owner must be a RootPaneContainer");
        }
        JComponent gp = (JComponent) rpc.getGlassPane();
        gp.setVisible(true);
        gp.setLayout(null);

        // 이미 설치되어 있으면 반환
        for (Component c : gp.getComponents()) {
            if (c instanceof CenterMessageOverlay o) return o;
        }

        CenterMessageOverlay overlay = new CenterMessageOverlay();
        gp.add(overlay);
        overlay.setBounds(0, 0, gp.getWidth(), gp.getHeight());
        gp.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override public void componentResized(java.awt.event.ComponentEvent e) {
                overlay.setBounds(0, 0, gp.getWidth(), gp.getHeight());
                overlay.revalidate();
                overlay.repaint();
            }
        });
        return overlay;
    }

    /** 메시지를 주어진 시간(ms) 동안 표시(자동 페이드 인/아웃 포함). */
    public void showMessage(String msg, int durationMs) {
        if (msg == null || msg.isBlank()) return;
        int dur = Math.max(600, durationMs);
        Runnable r = () -> play(msg, dur);
        if (SwingUtilities.isEventDispatchThread()) r.run();
        else SwingUtilities.invokeLater(r);
    }

    private void play(String msg, int durationMs) {
        this.text = msg;
        if (anim != null) anim.stop();

        final int fps = 60;
        final int inMs = 250;
        final int outMs = 350;
        final int holdMs = Math.max(200, durationMs - inMs - outMs);

        final int inSteps = Math.max(1, inMs * fps / 1000);
        final int holdSteps = Math.max(1, holdMs * fps / 1000);
        final int outSteps = Math.max(1, outMs * fps / 1000);

        anim = new Timer(1000 / fps, null);
        anim.addActionListener(new AbstractAction() {
            int step = 0;
            @Override public void actionPerformed(ActionEvent e) {
                step++;
                if (step <= inSteps) {
                    alpha = step / (float) inSteps;
                } else if (step <= inSteps + holdSteps) {
                    alpha = 1f;
                } else {
                    int k = step - inSteps - holdSteps;
                    alpha = 1f - k / (float) outSteps;
                    if (k >= outSteps) {
                        anim.stop();
                        alpha = 0f;
                    }
                }
                repaint();
            }
        });
        alpha = 0f;
        anim.start();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (alpha <= 0f || text.isEmpty()) return;
        Graphics2D gg = (Graphics2D) g.create();
        try {
            gg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth(), h = getHeight();

            // 반투명 블랙 배경(살짝 어둡게)
            gg.setComposite(AlphaComposite.SrcOver.derive(Math.min(0.35f, alpha * 0.35f)));
            gg.setColor(Color.BLACK);
            gg.fillRect(0, 0, w, h);

            // 메시지 박스
            gg.setComposite(AlphaComposite.SrcOver.derive(alpha));
            String[] lines = text.split("\n");
            Font base = getFont().deriveFont(Font.BOLD, 36f);
            gg.setFont(base);

            int maxWidth = 0;
            int totalHeight = 0;
            for (String line : lines) {
                FontMetrics fm = gg.getFontMetrics();
                maxWidth = Math.max(maxWidth, fm.stringWidth(line));
                totalHeight += fm.getHeight();
            }
            int padX = 28, padY = 18;
            int bw = maxWidth + padX * 2;
            int bh = totalHeight + padY * 2;

            int bx = (w - bw) / 2;
            int by = (h - bh) / 2;

            Shape box = new RoundRectangle2D.Float(bx, by, bw, bh, 22, 22);
            gg.setColor(new Color(20, 20, 20, 220));
            gg.fill(box);
            gg.setColor(new Color(240, 240, 240, 60));
            gg.draw(box);

            // 본문 텍스트
            gg.setColor(new Color(240, 240, 240));
            int y = by + padY;
            for (String line : lines) {
                FontMetrics fm = gg.getFontMetrics();
                int x = bx + (bw - fm.stringWidth(line)) / 2;
                y += fm.getAscent();
                gg.drawString(line, x, y);
                y += fm.getDescent();
            }
        } finally {
            gg.dispose();
        }
    }

    // ===== 독립 실행 데모 =====
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("CenterMessageOverlay Demo");
            f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            f.setSize(800, 480);
            f.setLocationRelativeTo(null);
            f.setLayout(new BorderLayout());
            f.getContentPane().setBackground(new Color(36, 36, 36));

            // 독립 컴포넌트를 직접 추가
            CenterMessageOverlay overlay = new CenterMessageOverlay();
            JLayeredPane lp = new JLayeredPane();
            lp.setLayout(null);
            f.setContentPane(lp);
            lp.add(overlay, Integer.valueOf(10));
            lp.addComponentListener(new java.awt.event.ComponentAdapter() {
                @Override public void componentResized(java.awt.event.ComponentEvent e) {
                    overlay.setBounds(0, 0, lp.getWidth(), lp.getHeight());
                }
            });

            JButton btn = new JButton("Show message");
            btn.addActionListener(e -> overlay.showMessage("ROUND START", 1500));
            JPanel center = new JPanel(new GridBagLayout());
            center.add(btn);
            lp.add(center, Integer.valueOf(0));
            lp.addComponentListener(new java.awt.event.ComponentAdapter() {
                @Override public void componentResized(java.awt.event.ComponentEvent e) {
                    center.setBounds(0, 0, lp.getWidth(), lp.getHeight());
                }
            });

            f.setVisible(true);
            overlay.showMessage("WELCOME", 1200);
        });
    }
}

