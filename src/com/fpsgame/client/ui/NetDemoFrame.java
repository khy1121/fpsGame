package com.fpsgame.client.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.IOException;

/**
 * {@link GameCanvasNetDemo} 를 담아 간단히 접속/해제를 테스트할 수 있는
 * 경량 런처 프레임.
 *
 * <p>특징</p>
 * <ul>
 *   <li>Host/Port 입력 + Connect/Disconnect 버튼 제공</li>
 *   <li>연결되면 {@link GameCanvasNetDemo}가 서버 권위 스냅샷을 렌더</li>
 *   <li>WASD/마우스 입력 전송은 {@link GameCanvasNetDemo} 내부 펌프가 담당</li>
 * </ul>
 */
public class NetDemoFrame extends JFrame {

    private final JTextField hostField = new JTextField("127.0.0.1", 12);
    private final JSpinner portSpinner = new JSpinner(new SpinnerNumberModel(7777, 1, 65535, 1));
    private final JButton connectBtn = new JButton("Connect");
    private final JButton disconnectBtn = new JButton("Disconnect");

    private final GameCanvasNetDemo canvas = new GameCanvasNetDemo();

    public NetDemoFrame() {
        super("FPS Net Demo");

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(1024, 640);
        setLocationRelativeTo(null);

        JPanel top = new JPanel(new GridBagLayout());
        top.setBorder(new EmptyBorder(6, 6, 6, 6));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(2, 4, 2, 4);
        c.gridy = 0;

        ((JSpinner.DefaultEditor) portSpinner.getEditor()).getTextField().setColumns(5);

        c.gridx = 0; top.add(new JLabel("Host"), c);
        c.gridx = 1; c.weightx = 1.0; c.fill = GridBagConstraints.HORIZONTAL; top.add(hostField, c);
        c.gridx = 2; c.weightx = 0; c.fill = GridBagConstraints.NONE; top.add(new JLabel("Port"), c);
        c.gridx = 3; top.add(portSpinner, c);
        c.gridx = 4; top.add(connectBtn, c);
        c.gridx = 5; top.add(disconnectBtn, c);

        connectBtn.addActionListener(e -> doConnect());
        disconnectBtn.addActionListener(e -> doDisconnect());
        updateButtons(false);

        setLayout(new BorderLayout());
        add(top, BorderLayout.NORTH);
        add(canvas, BorderLayout.CENTER);
    }

    private void doConnect() {
        String host = hostField.getText().trim();
        int port = (int) portSpinner.getValue();
        try {
            canvas.connect(host, port);
            updateButtons(true);
            canvas.requestFocusInWindow();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "연결 실패: " + ex.getMessage(),
                    "Connect Error", JOptionPane.ERROR_MESSAGE);
            updateButtons(false);
        }
    }

    private void doDisconnect() {
        canvas.disconnect();
        updateButtons(false);
    }

    private void updateButtons(boolean connected) {
        hostField.setEnabled(!connected);
        portSpinner.setEnabled(!connected);
        connectBtn.setEnabled(!connected);
        disconnectBtn.setEnabled(connected);
    }

    /** 단독 실행 엔트리 */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new NetDemoFrame().setVisible(true));
    }
}
