package com.fpsgame.client.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * 네트워크 상태/진단을 표시하는 경량 패널.
 *
 * <p>수정 내용(컴파일 오류 해결):</p>
 * <ul>
 *   <li>호출부에서 사용하는 오버로드 대응을 위해 {@link #setConnected(boolean)} 추가</li>
 *   <li>{@link #setConnected(boolean, String)} 는 기존 그대로 유지</li>
 *   <li>null 주소 전달 시에도 안전하게 처리</li>
 * </ul>
 */
public class NetStatusPanel extends JPanel {

    private final JLabel connectedLabel = new JLabel("Disconnected");
    private final JLabel remoteLabel    = new JLabel("-");
    private final JLabel rttLabel       = new JLabel("RTT: - ms");

    public NetStatusPanel() {
        setLayout(new GridBagLayout());
        setBorder(new EmptyBorder(6, 6, 6, 6));

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(2, 4, 2, 4);
        c.gridy = 0;

        connectedLabel.setFont(connectedLabel.getFont().deriveFont(Font.BOLD));
        connectedLabel.setForeground(new Color(180, 0, 0));
        remoteLabel.setForeground(new Color(60, 60, 60));

        c.gridx = 0; add(new JLabel("State:"), c);
        c.gridx = 1; add(connectedLabel, c);
        c.gridx = 2; add(new JSeparator(SwingConstants.VERTICAL), c);

        c.gridy = 1; c.gridx = 0; add(new JLabel("Remote:"), c);
        c.gridx = 1; c.gridwidth = 2; add(remoteLabel, c); c.gridwidth = 1;

        c.gridy = 2; c.gridx = 0; add(new JLabel("Latency:"), c);
        c.gridx = 1; add(rttLabel, c);
    }

    /** 연결 상태/원격 주소를 갱신 */
    public void setConnected(boolean connected, String remote) {
        dispatchEdt(() -> {
            connectedLabel.setText(connected ? "Connected" : "Disconnected");
            connectedLabel.setForeground(connected ? new Color(0, 130, 0) : new Color(180, 0, 0));
            remoteLabel.setText(remote == null || remote.isBlank() ? "-" : remote);
        });
    }

    /** 레거시 호출부 호환: 주소 없이 연결 상태만 갱신 */
    public void setConnected(boolean connected) {
        setConnected(connected, null);
    }

    /** RTT(ms) 갱신 */
    public void setRtt(long rttMillis) {
        dispatchEdt(() -> rttLabel.setText("RTT: " + Math.max(0, rttMillis) + " ms"));
    }

    /** 텍스트를 그대로 상태 라벨에 표시(임시 메시지) */
    public void setStatusText(String text) {
        dispatchEdt(() -> connectedLabel.setText(text == null ? "" : text));
    }

    private static void dispatchEdt(Runnable r) {
        if (SwingUtilities.isEventDispatchThread()) r.run();
        else SwingUtilities.invokeLater(r);
    }

    // 단독 실행 데모
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("NetStatus Demo");
            NetStatusPanel p = new NetStatusPanel();
            f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            f.setSize(360, 160);
            f.setLocationRelativeTo(null);
            f.setContentPane(p);
            f.setVisible(true);

            // 데모 타이머
            new Timer(800, e -> {
                long now = System.currentTimeMillis() / 800;
                boolean conn = (now % 6) >= 2;
                p.setConnected(conn);
                p.setConnected(conn, conn ? "127.0.0.1:7777" : null);
                if (conn) p.setRtt(12 + (now % 7) * 7L);
            }).start();
        });
    }
}
