package com.fpsgame.client.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * 화면 오른쪽 하단에 잠깐 표시되는 토스트 메시지 유틸리티
 * - 모든 호출은 EDT에서 안전하게 처리됨
 * - 간단한 페이드 인/홀드/아웃 애니메이션(약 60fps)
 * - 소유 윈도우가 있으면 그 하단 중앙, 없으면 화면 하단 중앙에 표시
 */
public final class Toast {

    private Toast() { }

    // 공개 API ---------------------------------------------------------

    /** 일반 토스트 표시(기본 색상). */
    public static void show(Component owner, String text, int durationMs) {
        Window w = owner == null ? null : SwingUtilities.getWindowAncestor(owner);
        show(w, text, durationMs, new Color(36, 36, 36, 230), new Color(235, 235, 235));
    }

    /** 성공 토스트(녹색 계열). */
    public static void success(Component owner, String text) {
        Window w = owner == null ? null : SwingUtilities.getWindowAncestor(owner);
        show(w, text, 1200, new Color(22, 80, 22, 235), new Color(230, 255, 230));
    }

    /** 오류 토스트(빨간색 계열). */
    public static void error(Component owner, String text) {
        Window w = owner == null ? null : SwingUtilities.getWindowAncestor(owner);
        show(w, text, 1500, new Color(110, 22, 22, 235), new Color(255, 230, 230));
    }

    /** 내부 색/시간을 직접 지정하는 토스트 표시. */
    public static void show(Window owner, String text, int durationMs,
                            Color bg, Color fg) {
        if (text == null || text.isBlank()) return;
        int dur = Math.max(500, durationMs);

        Runnable run = () -> {
            JWindow toast = new JWindow(owner);
            toast.setBackground(new Color(0, 0, 0, 0)); // 투명 배경

            ToastPanel panel = new ToastPanel(text, bg, fg);
            toast.setContentPane(panel);
            toast.pack();

            // 소유 윈도우가 있으면 그 기준, 없으면 화면 기준
            Rectangle bounds = (owner != null) ? owner.getBounds() : GraphicsEnvironment
                    .getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration().getBounds();

            int x = bounds.x + (bounds.width - toast.getWidth()) / 2;
            int y = bounds.y + bounds.height - toast.getHeight() - 60;
            toast.setLocation(x, y);

            // 간단한 페이드 인/홀드/아웃 타임라인(약 60fps)
            final int fps = 60;
            final int inMs = 200;
            final int outMs = 300;
            final int holdMs = Math.max(200, dur - inMs - outMs);
            final int inSteps = Math.max(1, inMs * fps / 1000);
            final int holdSteps = Math.max(1, holdMs * fps / 1000);
            final int outSteps = Math.max(1, outMs * fps / 1000);

            Timer timer = new Timer(1000 / fps, null);
            timer.addActionListener(e -> {
                int step = panel.step + 1;
                panel.step = step;

                if (step <= inSteps) {
                    panel.alpha = step / (float) inSteps;
                } else if (step <= inSteps + holdSteps) {
                    panel.alpha = 1f;
                } else {
                    int k = step - inSteps - holdSteps;
                    panel.alpha = 1f - k / (float) outSteps;
                    if (k >= outSteps) {
                        timer.stop();
                        toast.setVisible(false);
                        toast.dispose();
                        return;
                    }
                }
                toast.repaint();
            });

            toast.setAlwaysOnTop(true);
            toast.setVisible(true);
            timer.start();
        };

        if (SwingUtilities.isEventDispatchThread()) run.run();
        else SwingUtilities.invokeLater(run);
    }

    // 내부 구현 --------------------------------------------------------
    private static final class ToastPanel extends JComponent {
        private final String text;
        private final Color bg;
        private final Color fg;

        volatile float alpha = 0f; // 0..1
        int step = 0;

        ToastPanel(String text, Color bg, Color fg) {
            this.text = text;
            this.bg = bg;
            this.fg = fg;
            setOpaque(false);
            setFocusable(false);
        }

        @Override
        public Dimension getPreferredSize() {
            Font f = getFont().deriveFont(Font.BOLD, 13f);
            FontMetrics fm = getFontMetrics(f);
            int w = fm.stringWidth(text) + 28;
            int h = fm.getHeight() + 14;
            // 최소/최대 폭 제한
            w = Math.min(Math.max(w, 120), 560);
            return new Dimension(w, h);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D gg = (Graphics2D) g.create();
            try {
                gg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth(), h = getHeight();
                float a = Math.max(0f, Math.min(1f, alpha));

                // 반투명 배경 박스
                gg.setComposite(AlphaComposite.SrcOver.derive(a));
                Shape box = new RoundRectangle2D.Float(0, 0, w - 1, h - 1, 14, 14);
                gg.setColor(bg);
                gg.fill(box);
                gg.setColor(new Color(255, 255, 255, 60));
                gg.draw(box);

                // 텍스트
                gg.setColor(fg);
                Font f = getFont().deriveFont(Font.BOLD, 13f);
                gg.setFont(f);
                FontMetrics fm = gg.getFontMetrics();
                int tx = (w - fm.stringWidth(text)) / 2;
                int ty = (h - fm.getHeight()) / 2 + fm.getAscent();
                gg.drawString(text, tx, ty);
            } finally {
                gg.dispose();
            }
        }
    }
}

