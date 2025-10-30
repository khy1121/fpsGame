package com.fpsgame.client.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * RTT(핑) 지표를 색상/숫자로 보여주는 경량 컴포넌트.
 *
 * <p>특징</p>
 * <ul>
 *   <li>{@link #setRtt(long)} 로 RTT(ms)를 갱신하면 색상과 텍스트가 자동 업데이트</li>
 *   <li>임계값(양호/보통/나쁨)을 커스터마이즈 가능</li>
 *   <li>EDT 안전: 외부 스레드에서 호출해도 내부에서 EDT 디스패치</li>
 * </ul>
 *
 * <p>연동 예</p>
 * <pre>
 *   LatencyIndicator li = new LatencyIndicator();
 *   hudBar.add(li);
 *   // RttMonitor 콜백에서:
 *   rttMonitor = new RttMonitor(net, li::setRtt);
 *   rttMonitor.start(1000);
 * </pre>
 */
public class LatencyIndicator extends JPanel {

    /** ms 기준 임계값(이 값들은 기본값이며 필요 시 생성자/세터로 조정 가능) */
    private volatile long goodThreshold = 50;   // 0..good → 초록
    private volatile long warnThreshold = 120;  // good..warn → 노랑, warn↑ → 빨강

    private final JLabel label = new JLabel("RTT — ms");
    private final Dot dot = new Dot();

    public LatencyIndicator() {
        this(50, 120);
    }

    public LatencyIndicator(long goodThreshold, long warnThreshold) {
        setOpaque(false);
        setLayout(new GridBagLayout());
        setBorder(new EmptyBorder(2, 6, 2, 6));

        this.goodThreshold = Math.max(1, goodThreshold);
        this.warnThreshold = Math.max(this.goodThreshold + 1, warnThreshold);

        label.setFont(label.getFont().deriveFont(Font.BOLD, 12f));
        label.setForeground(new Color(230, 230, 230));

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(0, 0, 0, 6);
        c.gridx = 0; c.gridy = 0; add(dot, c);
        c.gridx = 1; add(label, c);
    }

    /** RTT 임계값 수정(즉시 적용). */
    public void setThresholds(long good, long warn) {
        this.goodThreshold = Math.max(1, good);
        this.warnThreshold = Math.max(this.goodThreshold + 1, warn);
        updateUIByRtt(lastRtt);
    }

    private volatile long lastRtt = -1;

    /** 외부에서 RTT(ms) 갱신 시 호출. */
    public void setRtt(long millis) {
        long v = Math.max(0, millis);
        if (SwingUtilities.isEventDispatchThread()) {
            applyRtt(v);
        } else {
            SwingUtilities.invokeLater(() -> applyRtt(v));
        }
    }

    private void applyRtt(long v) {
        this.lastRtt = v;
        updateUIByRtt(v);
    }

    private void updateUIByRtt(long v) {
        label.setText("RTT " + (v >= 0 ? v : 0) + " ms");
        Color col = colorFor(v);
        dot.setColor(col);
        repaint();
    }

    /** RTT 값에 따른 색상 결정 */
    private Color colorFor(long v) {
        if (v <= goodThreshold) return new Color(40, 180, 60);       // 초록
        if (v <= warnThreshold) return new Color(230, 200, 40);      // 노랑
        return new Color(200, 70, 70);                               // 빨강
    }

    /** 작고 동그란 지시등 */
    private static final class Dot extends JComponent {
        private Color color = new Color(90, 90, 90);
        private static final int SIZE = 12;
        @Override public Dimension getPreferredSize() { return new Dimension(SIZE, SIZE); }
        void setColor(Color c) { this.color = c; repaint(); }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                int d = Math.min(w, h);
                int x = (w - d) / 2, y = (h - d) / 2;
                // 외곽 그림자
                g2.setColor(new Color(0, 0, 0, 90));
                g2.fillOval(x, y + 1, d, d);
                // 본체
                g2.setColor(color);
                g2.fillOval(x, y, d, d);
                // 하이라이트
                g2.setColor(new Color(255, 255, 255, 120));
                g2.fillOval(x + d/4, y + d/4, d/2, d/3);
                g2.setColor(new Color(255, 255, 255, 80));
                g2.drawOval(x, y, d, d);
            } finally { g2.dispose(); }
        }
    }

    // ================= 단독 실행 데모 =================
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("LatencyIndicator Demo");
            f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            f.setSize(360, 120);
            f.setLocationRelativeTo(null);

            LatencyIndicator li = new LatencyIndicator();

            JPanel root = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 12));
            root.setBackground(new Color(30, 30, 30));
            root.add(li);

            // 데모: RTT 값을 주기적으로 변동
            Timer t = new Timer(600, e -> {
                long base = System.currentTimeMillis() / 1000 % 6; // 0..5
                long rtt = switch ((int) base) {
                    case 0 -> 35;
                    case 1 -> 60;
                    case 2 -> 95;
                    case 3 -> 130;
                    case 4 -> 180;
                    default -> 240;
                };
                li.setRtt(rtt);
            });
            t.start();

            f.setContentPane(root);
            f.setVisible(true);
        });
    }
}
