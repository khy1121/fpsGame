package com.fpsgame.client.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * 간단한 서버 연결 패널.
 *
 * <p>구성</p>
 * <ul>
 *   <li>Host / Port 입력</li>
 *   <li>Connect / Disconnect 버튼</li>
 *   <li>상태 표시 라벨</li>
 * </ul>
 *
 * <p>이 패널은 {@link ChatConsoleFrame}, {@link NetPlaygroundFrame} 등에서
 * 상단 연결 바(UI)로 재사용하기 위해 설계되었다.</p>
 *
 * <p>호출측은 {@link #setListener(Listener)} 로 콜백을 주입하고,
 * 연결 상태에 따라 {@link #setConnected(boolean)} 를 호출해 버튼 활성화/비활성화를
 * 갱신하면 된다.</p>
 */
public final class ConnectPanel extends JPanel {

    /** 외부 콜백 인터페이스 */
    public interface Listener {
        /** 사용자가 Connect 버튼을 눌렀을 때 호출 */
        void onConnect(String host, int port);
        /** 사용자가 Disconnect 버튼을 눌렀을 때 호출 */
        void onDisconnect();
    }

    private Listener listener;

    private final JTextField tfHost = new JTextField("127.0.0.1", 16);
    private final JSpinner spPort   = new JSpinner(new SpinnerNumberModel(7777, 1, 65535, 1));

    private final JButton btnConnect    = new JButton("Connect");
    private final JButton btnDisconnect = new JButton("Disconnect");

    private final JLabel status = new JLabel("Disconnected");

    public ConnectPanel() {
        super(new BorderLayout(8, 4));
        setBorder(new EmptyBorder(6, 8, 6, 8));

        // 좌측: 입력 영역
        JPanel left = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(2, 4, 2, 4);
        c.gridy = 0;

        c.gridx = 0; left.add(new JLabel("Host"), c);
        c.gridx = 1; c.weightx = 1.0; c.fill = GridBagConstraints.HORIZONTAL; left.add(tfHost, c);

        c.gridx = 2; c.weightx = 0; c.fill = GridBagConstraints.NONE; left.add(new JLabel("Port"), c);
        ((JSpinner.DefaultEditor) spPort.getEditor()).getTextField().setColumns(6);
        c.gridx = 3; left.add(spPort, c);

        add(left, BorderLayout.CENTER);

        // 우측: 버튼 영역
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        right.add(btnConnect);
        right.add(btnDisconnect);
        add(right, BorderLayout.EAST);

        // 하단: 상태
        JPanel bottom = new JPanel(new BorderLayout());
        status.setForeground(new Color(180, 0, 0));
        bottom.add(status, BorderLayout.WEST);
        add(bottom, BorderLayout.SOUTH);

        // 기본 상태: 연결 안 됨
        setConnected(false);

        // 액션
        btnConnect.addActionListener(e -> {
            if (listener == null) return;
            String host = tfHost.getText().trim();
            int port = (Integer) spPort.getValue();
            if (host.isEmpty()) host = "127.0.0.1";
            if (port < 1 || port > 65535) {
                JOptionPane.showMessageDialog(this, "Port 범위는 1~65535 입니다.", "입력 오류", JOptionPane.WARNING_MESSAGE);
                return;
            }
            listener.onConnect(host, port);
        });

        btnDisconnect.addActionListener(e -> {
            if (listener != null) listener.onDisconnect();
        });
    }

    /** 콜백 리스너 설정 */
    public void setListener(Listener l) { this.listener = l; }

    /**
     * 연결 상태를 UI에 반영.
     * @param connected true면 Connect 비활성/Disconnect 활성, false는 반대로
     */
    public void setConnected(boolean connected) {
        btnConnect.setEnabled(!connected);
        btnDisconnect.setEnabled(connected);
        status.setForeground(connected ? new Color(0, 130, 0) : new Color(180, 0, 0));
        if (connected && status.getText().equals("Disconnected")) {
            status.setText("Connected");
        } else if (!connected && status.getText().startsWith("Connected")) {
            status.setText("Disconnected");
        }
    }

    /**
     * 상태 텍스트/색상 설정(예: "Connecting...", 노랑색 등).
     * 호출측에서 연결 진행 상황을 알릴 때 사용.
     */
    public void setStatusText(String text, Color color) {
        status.setText(text == null ? "" : text);
        if (color != null) status.setForeground(color);
    }

    /** 현재 Host 문자열 */
    public String getHost() { return tfHost.getText().trim(); }

    /** 현재 Port 값 */
    public int getPort() { return (Integer) spPort.getValue(); }
}
