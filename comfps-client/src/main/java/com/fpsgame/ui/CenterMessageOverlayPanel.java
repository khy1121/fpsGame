package com.fpsgame.client.ui;

import javax.swing.*;
import java.awt.*;
import java.text.DecimalFormat;

/**
 * 중앙 메시지/카운트다운 오버레이 패널(경량)
 *
 * 특징:
 * - 투명 패널 위 중앙 정렬 라벨 1개로 텍스트 표시
 * - EDT 안전: 모든 공개 API는 내부에서 EDT 디스패치
 * - showMessage / showBigText / showCountdown / showBigNumber / setMessage 제공
 *
 * 사용 예:
 * <pre>
 *   CenterMessageOverlayPanel p = new CenterMessageOverlayPanel();
 *   layeredPane.add(p, JLayeredPane.PALETTE_LAYER);
 *   p.showCountdown(3); // 3, 2, 1 표시(타이머로 갱신 호출)
 * </pre>
 */
public class CenterMessageOverlayPanel extends JComponent {

    /** 중앙에 그릴 텍스트 라벨 */
    private final JLabel centerLabel = new JLabel("", SwingConstants.CENTER);

    /** 글꼴 기본 크기 */
    private volatile float baseFontSize = 72f;

    public CenterMessageOverlayPanel() {
        setOpaque(false);
        setLayout(new BorderLayout());

        centerLabel.setForeground(new Color(255, 255, 255, 230));
        centerLabel.setFont(centerLabel.getFont().deriveFont(Font.BOLD, baseFontSize));
        centerLabel.setHorizontalAlignment(SwingConstants.CENTER);
        centerLabel.setVerticalAlignment(SwingConstants.CENTER);
        centerLabel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        // 라벨에 간단한 그림자 UI 적용
        centerLabel.setUI(new ShadowLabelUI());

        add(centerLabel, BorderLayout.CENTER);
        setVisible(false);
    }

    // ====================== 공개 API (EDT 안전) ======================

    /** 일반 메시지 표시(보통 크기) */
    public void showMessage(String message) {
        runEdt(() -> {
            centerLabel.setText(safe(message));
            centerLabel.setFont(centerLabel.getFont().deriveFont(Font.BOLD, baseFontSize));
            setVisible(true);
            repaint();
        });
    }

    /** 대형 텍스트 표시(예: "READY", "GO!") */
    public void showBigText(String message) {
        runEdt(() -> {
            centerLabel.setText(safe(message));
            centerLabel.setFont(centerLabel.getFont().deriveFont(Font.BOLD, baseFontSize * 1.25f));
            setVisible(true);
            repaint();
        });
    }

    /** 카운트다운 숫자 표시(초). 0 이하이면 숨김. */
    public void showCountdown(int seconds) {
        runEdt(() -> {
            if (seconds <= 0) {
                setVisible(false);
                centerLabel.setText("");
            } else {
                centerLabel.setText(Integer.toString(seconds));
                centerLabel.setFont(centerLabel.getFont().deriveFont(Font.BOLD, baseFontSize * 1.35f));
                setVisible(true);
            }
            repaint();
        });
    }

    /** 큰 숫자 표시(실수 지원) */
    public void showBigNumber(double number) {
        runEdt(() -> {
            String txt = new DecimalFormat("0.##").format(number);
            centerLabel.setText(txt);
            centerLabel.setFont(centerLabel.getFont().deriveFont(Font.BOLD, baseFontSize * 1.5f));
            setVisible(true);
            repaint();
        });
    }

    /** 현재 메시지를 직접 설정(표시는 별도 호출) */
    public void setMessage(String message) {
        runEdt(() -> {
            centerLabel.setText(safe(message));
            repaint();
        });
    }

    /** 오버레이 숨김 */
    public void hideOverlay() {
        runEdt(() -> {
            setVisible(false);
            centerLabel.setText("");
            repaint();
        });
    }

    /** 기본 글꼴 크기(포인트)를 조정한다. */
    public void setBaseFontSize(float pt) {
        if (pt <= 0) return;
        runEdt(() -> {
            baseFontSize = pt;
            centerLabel.setFont(centerLabel.getFont().deriveFont(Font.BOLD, baseFontSize));
            repaint();
        });
    }

    // ====================== 내부 유틸 ======================

    private static String safe(String s) {
        return (s == null ? "" : s);
    }

    private static void runEdt(Runnable r) {
        if (SwingUtilities.isEventDispatchThread()) r.run();
        else SwingUtilities.invokeLater(r);
    }

    // ====================== 배경 그라데이션(상단 약간) ======================

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        // 화면 상단에 반투명 배경을 얹어 시야 방해 최소화
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            int w = getWidth(), h = getHeight();
            Color c1 = new Color(0, 0, 0, 90);
            Color c2 = new Color(0, 0, 0, 0);
            GradientPaint gp = new GradientPaint(0, h / 3f, c1, 0, h, c2);
            g2.setPaint(gp);
            g2.fillRect(0, 0, w, h);
        } finally {
            g2.dispose();
        }
    }

    // ====================== 간단한 그림자 라벨 UI ======================

    /**
     * JLabel 텍스트에 그림자를 주어 가독성을 높이는 간단 UI.
     * (LookAndFeel 무관)
     */
    private static final class ShadowLabelUI extends javax.swing.plaf.basic.BasicLabelUI {
        @Override
        public void paint(Graphics g, JComponent c) {
            JLabel l = (JLabel) c;
            String text = l.getText();
            if (text == null || text.isEmpty()) {
                super.paint(g, c);
                return;
            }
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setFont(l.getFont());
                FontMetrics fm = g2.getFontMetrics();
                Rectangle r = new Rectangle(0, 0, c.getWidth(), c.getHeight());
                Point p = layoutTextCenter(fm, text, r);

                // 그림자
                g2.setColor(new Color(0, 0, 0, 140));
                g2.drawString(text, p.x + 2, p.y + 2);

                // 본문
                g2.setColor(l.getForeground());
                g2.drawString(text, p.x, p.y);
            } finally {
                g2.dispose();
            }
        }

        private Point layoutTextCenter(FontMetrics fm, String text, Rectangle r) {
            int textW = fm.stringWidth(text);
            int textH = fm.getAscent();
            int x = r.x + (r.width - textW) / 2;
            int y = r.y + (r.height + textH) / 2 - fm.getDescent();
            return new Point(Math.max(x, 0), Math.max(y, 0));
        }
    }
}

