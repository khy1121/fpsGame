package com.fpsgame.client.ui;

import com.fpsgame.common.MovingAverage;

import javax.swing.*;
import java.awt.*;

/**
 * 좌상단에 초당 프레임(FPS)을 표시하는 경량 오버레이 패널.
 *
 * <p>특징</p>
 * <ul>
 *   <li>{@link #frameArrived()} 를 매 렌더링 틱마다 호출하면 FPS를 계산</li>
 *   <li>{@link MovingAverage} 로 최근 표본의 평균을 사용하여 수치 안정화</li>
 *   <li>불투명한 라운드 박스 형태로 가독성 확보</li>
 *   <li>다른 UI 위에 얹기 쉽도록 고정 크기의 컴포넌트</li>
 * </ul>
 *
 * <p>사용 예</p>
 * <pre>
 *   FpsOverlayPanel fps = new FpsOverlayPanel();
 *   new Timer(16, e -> fps.frameArrived()).start(); // 60fps 근사 계산용 타이머
 * </pre>
 */
public final class FpsOverlayPanel extends JComponent {

    /** 최근 프레임 간격(ms)의 이동 평균(표본 120개 ≒ 2초 at 60fps) */
    private final MovingAverage avgMs = new MovingAverage(120);

    /** 마지막 프레임 타임스탬프(ns) */
    private long lastNs = 0L;

    /** 최근 계산된 FPS(표시용 캐시) */
    private volatile double fps = 0.0;

    /** 배경/테두리 색상 */
    private static final Color BG = new Color(20, 22, 26, 220);
    private static final Color FG = new Color(220, 230, 240);
    private static final Color EDGE = new Color(240, 240, 240, 60);

    public FpsOverlayPanel() {
        setOpaque(false);
        setFocusable(false);
        setToolTipText("Frames per second");
    }

    /** 하나의 프레임이 렌더링되었음을 알린다(계산 및 리페인트 트리거). */
    public void frameArrived() {
        long now = System.nanoTime();
        if (lastNs != 0L) {
            long dtNs = now - lastNs;
            // 비정상 값 보호(1초 이상 끊기면 무시)
            if (dtNs > 0 && dtNs < 1_000_000_000L) {
                double ms = dtNs / 1_000_000.0;
                avgMs.addSample(ms);
                double avg = avgMs.getAverage();
                fps = (avg <= 0.0) ? 0.0 : (1000.0 / avg);
            }
        }
        lastNs = now;
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(110, 44);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D gg = (Graphics2D) g.create();
        try {
            gg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            int arc = 14;

            // 배경 박스
            gg.setColor(BG);
            gg.fillRoundRect(0, 0, w - 1, h - 1, arc, arc);
            gg.setColor(EDGE);
            gg.drawRoundRect(0, 0, w - 1, h - 1, arc, arc);

            // 텍스트
            gg.setColor(FG);
            Font f1 = getFont().deriveFont(Font.BOLD, 13f);
            Font f2 = getFont().deriveFont(Font.BOLD, 18f);

            String label = "FPS";
            String value = (fps <= 0.0 || Double.isNaN(fps)) ? "—" : String.format("%.0f", fps);

            // 배치
            gg.setFont(f1);
            FontMetrics fm1 = gg.getFontMetrics();
            int lx = 10;
            int ly = 12 + fm1.getAscent();
            gg.drawString(label, lx, ly);

            gg.setFont(f2);
            FontMetrics fm2 = gg.getFontMetrics();
            int vx = w - 10 - fm2.stringWidth(value);
            int vy = h - 10;
            gg.drawString(value, vx, vy);
        } finally {
            gg.dispose();
        }
    }
}
