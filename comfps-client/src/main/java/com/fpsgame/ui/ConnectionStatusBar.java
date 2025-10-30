package com.fpsgame.client.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Objects;

/**
 * 하단 상태바 컴포넌트(연결 상태/RTT 표시에 특화).
 *
 * <p>용도</p>
 * <ul>
 *   <li>프레임의 SOUTH(또는 PAGE_END)에 부착하여 연결 상태와 RTT(ms)를 표시</li>
 *   <li>프로젝트 어디서나 재사용 가능한 경량 컴포넌트</li>
 * </ul>
 *
 * <p>특징</p>
 * <ul>
 *   <li>EDT 안전 업데이트: 모든 setter는 EDT에서 실행되도록 보장</li>
 *   <li>시각적 강조: 연결/해제에 따라 아이콘과 색상이 달라짐</li>
 * </ul>
 *
 * <p>예시</p>
 * <pre>
 *   ConnectionStatusBar bar = new ConnectionStatusBar();
 *   frame.add(bar, BorderLayout.PAGE_END);
 *   bar.setConnected(true, "127.0.0.1:7777");
 *   bar.setRttMillis(23);
 * </pre>
 */
public class ConnectionStatusBar extends JPanel {

    private final JLabel statusDot = new JLabel("●");
    private final JLabel statusText = new JLabel("Disconnected");
    private final JLabel rttText = new JLabel("RTT: - ms");

    public ConnectionStatusBar() {
        super(new FlowLayout(FlowLayout.LEFT, 8, 2));
        setBorder(new EmptyBorder(4, 8, 4, 8));
        setOpaque(true);
        setBackground(new Color(245, 245, 245));

        statusDot.setForeground(new Color(180, 0, 0)); // 초기: 끊김(빨강)
        statusText.setForeground(new Color(60, 60, 60));
        rttText.setForeground(new Color(60, 60, 60));

        add(statusDot);
        add(statusText);
        add(new JSeparator(SwingConstants.VERTICAL));
        add(rttText);
    }

    /** 연결 상태/호스트 정보를 설정 */
    public void setConnected(boolean connected, String endpoint) {
        runEdt(() -> {
            statusDot.setForeground(connected ? new Color(0, 140, 0) : new Color(180, 0, 0));
            String ep = (endpoint == null || endpoint.isEmpty()) ? "" : "  " + endpoint;
            statusText.setText((connected ? "Connected" : "Disconnected") + ep);
            repaint();
        });
    }

    /** RTT(ms) 텍스트를 갱신 */
    public void setRttMillis(long rttMillis) {
        runEdt(() -> {
            long v = Math.max(0L, rttMillis);
            rttText.setText("RTT: " + v + " ms");
        });
    }

    // ============== 유틸 (EDT 안전) ==============

    private static void runEdt(Runnable r) {
        if (SwingUtilities.isEventDispatchThread()) r.run();
        else SwingUtilities.invokeLater(r);
    }
}
