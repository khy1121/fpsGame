package com.fpsgame.client.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * RTT/핑 값을 시계열 그래프로 보여주는 경량 패널.
 *
 * <p>특징</p>
 * <ul>
 *   <li>{@link #addSample(long)} 로 ms 단위 샘플을 추가</li>
 *   <li>최근 N개(기본 120개) 표본을 보존하여 스파크라인 형태로 렌더</li>
 *   <li>최댓값 자동 스케일링, 마우스 오버 시 툴팁에 마지막 값 표시</li>
 *   <li>EDT 안전: 외부 스레드에서 호출해도 내부에서 EDT로 리페인트</li>
 * </ul>
 *
 * <p>연동 예</p>
 * <pre>
 *   LatencyGraphPanel g = new LatencyGraphPanel();
 *   RttMonitor rtt = new RttMonitor(net, ms -> SwingUtilities.invokeLater(() -> {
 *       g.addSample(ms);
 *   }));
 * </pre>
 */
public final class LatencyGraphPanel extends JPanel {

    /** 보존할 최대 표본 수 */
    private int capacity = 120;

    /** ms 표본(고정 길이 링버퍼처럼 사용) */
    private final Deque<Long> samples = new ArrayDeque<>();

    /** 최근 표시용 라벨(옵션) */
    private final JLabel label = new JLabel("RTT —");

    /** 그래프 패딩 */
    private static final int PAD_X = 8;
    private static final int PAD_Y = 6;

    public LatencyGraphPanel() {
        super(new BorderLayout());
        setBorder(new EmptyBorder(4, 6, 4, 6));
        setBackground(new Color(30, 30, 30));

        label.setForeground(new Color(180, 200, 255));
        label.setHorizontalAlignment(SwingConstants.RIGHT);
        add(label, BorderLayout.SOUTH);

        setToolTipText("RTT graph (ms) — last " + capacity + " samples");
    }

    /** 링 버퍼 용량 설정(기본 120). 30 이상 권장. */
    public void setCapacity(int capacity) {
        this.capacity = Math.max(30, capacity);
        SwingUtilities.invokeLater(() -> {
            while (samples.size() > this.capacity) samples.removeFirst();
            repaint();
        });
    }

    /** RTT(ms) 표본 추가. */
    public void addSample(long ms) {
        if (SwingUtilities.isEventDispatchThread()) {
            push(ms);
        } else {
            SwingUtilities.invokeLater(() -> push(ms));
        }
    }

    private void push(long ms) {
        samples.addLast(Math.max(0, ms));
        while (samples.size() > capacity) samples.removeFirst();
        label.setText("RTT " + ms + " ms");
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(280, 100);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (samples.isEmpty()) return;

        Graphics2D gg = (Graphics2D) g.create();
        try {
            gg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight() - label.getHeight();
            int gx = PAD_X, gy = PAD_Y, gw = w - PAD_X * 2, gh = h - PAD_Y * 2;
            if (gw <= 2 || gh <= 2) return;

            // 배경/그리드
            gg.setColor(new Color(40, 42, 48));
            gg.fillRect(gx, gy, gw, gh);
            gg.setColor(new Color(55, 58, 66));
            gg.drawRect(gx, gy, gw, gh);

            // 최대값 계산(자동 스케일)
            long max = 1;
            for (long v : samples) max = Math.max(max, v);
            // 상단 여유(10%)
            double scale = (max * 1.1) / gh;
            if (scale <= 0) scale = 1;

            // 축 약선(중간선)
            gg.setColor(new Color(70, 75, 85));
            gg.drawLine(gx, gy + gh / 2, gx + gw, gy + gh / 2);

            // 스파크라인
            gg.setStroke(new BasicStroke(2f));
            gg.setColor(new Color(120, 200, 255));

            int n = samples.size();
            double step = (n <= 1) ? gw : (gw / (double) (n - 1));
            int i = 0;
            int prevX = gx, prevY = gy + gh - (int) Math.round(samples.peekFirst() / scale);

            for (long v : samples) {
                int x = gx + (int) Math.round(i * step);
                int y = gy + gh - (int) Math.round(v / scale);
                if (i > 0) gg.drawLine(prevX, prevY, x, y);
                prevX = x; prevY = y; i++;
            }

            // 마지막 점 강조
            gg.setColor(new Color(200, 230, 255));
            gg.fillOval(prevX - 3, prevY - 3, 6, 6);

            // 우측 눈금(최대값)
            gg.setFont(getFont().deriveFont(10f));
            gg.setColor(new Color(170, 170, 170));
            String maxLabel = max + " ms";
            int tw = gg.getFontMetrics().stringWidth(maxLabel);
            gg.drawString(maxLabel, gx + gw - tw, gy + 12);
        } finally {
            gg.dispose();
        }
    }

    // ===== 단독 실행 데모 =====
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("LatencyGraphPanel Demo");
            f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            f.setSize(360, 200);
            f.setLocationRelativeTo(null);

            LatencyGraphPanel g = new LatencyGraphPanel();
            f.add(g, BorderLayout.CENTER);

            // 가짜 샘플(랜덤 스파이크 포함)
            new Timer(200, e -> {
                long base = 30 + (long) (Math.random() * 20);
                if (Math.random() < 0.1) base += (long) (Math.random() * 120); // 스파이크
                g.addSample(base);
            }).start();

            f.setVisible(true);
        });
    }
}
