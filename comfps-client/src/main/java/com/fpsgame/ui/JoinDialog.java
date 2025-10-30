package com.fpsgame.client.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * 서버 접속 정보를 입력받는 간단한 모달 다이얼로그.
 *
 * <p>사용 예</p>
 * <pre>
 *   JoinDialog.Result r = JoinDialog.show(frame, "127.0.0.1", 7777, "player");
 *   if (r != null) {
 *       // r.host / r.port / r.name 사용
 *   }
 * </pre>
 */
public final class JoinDialog extends JDialog {

    /** 결과 DTO */
    public static final class Result {
        public final String host;
        public final int port;
        public final String name;
        public Result(String host, int port, String name) {
            this.host = host;
            this.port = port;
            this.name = name;
        }
        @Override public String toString() { return host + ":" + port + " (" + name + ")"; }
    }

    private final JTextField tfHost = new JTextField(16);
    private final JSpinner spPort   = new JSpinner(new SpinnerNumberModel(7777, 1, 65535, 1));
    private final JTextField tfName = new JTextField(16);

    private Result result; // OK 시에만 설정

    private JoinDialog(Window owner, String defHost, int defPort, String defName) {
        super(owner, "Join Server", ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setSize(380, 210);
        setLocationRelativeTo(owner);

        tfHost.setText(defHost == null || defHost.isBlank() ? "127.0.0.1" : defHost.trim());
        ((JSpinner.DefaultEditor) spPort.getEditor()).getTextField().setColumns(6);
        spPort.setValue(Math.max(1, Math.min(65535, defPort)));
        tfName.setText(defName == null || defName.isBlank() ? System.getProperty("user.name", "player") : defName.trim());

        setContentPane(buildUi());
        getRootPane().setDefaultButton(okButton);
    }

    private JButton okButton; // 기본 버튼 등록용

    private JPanel buildUi() {
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 6, 4, 6);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;

        // Host
        c.gridx = 0; c.gridy = 0; c.weightx = 0; form.add(new JLabel("Host"), c);
        c.gridx = 1; c.weightx = 1.0; form.add(tfHost, c);

        // Port
        c.gridx = 0; c.gridy = 1; c.weightx = 0; form.add(new JLabel("Port"), c);
        c.gridx = 1; c.weightx = 1.0; form.add(spPort, c);

        // Name
        c.gridx = 0; c.gridy = 2; c.weightx = 0; form.add(new JLabel("Name"), c);
        c.gridx = 1; c.weightx = 1.0; form.add(tfName, c);

        root.add(form, BorderLayout.CENTER);

        // Buttons
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton btnCancel = new JButton("Cancel");
        okButton = new JButton("OK");
        buttons.add(btnCancel);
        buttons.add(okButton);
        root.add(buttons, BorderLayout.SOUTH);

        btnCancel.addActionListener(e -> { result = null; dispose(); });
        okButton.addActionListener(e -> onOk());

        // ESC로 닫기
        root.registerKeyboardAction(e -> { result = null; dispose(); },
                KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        return root;
    }

    /** OK 버튼 처리: 값 검증 후 결과 설정 */
    private void onOk() {
        String host = tfHost.getText().trim();
        if (host.isEmpty()) host = "127.0.0.1";

        int port = (Integer) spPort.getValue();
        if (port < 1 || port > 65535) {
            JOptionPane.showMessageDialog(this, "Port 범위는 1~65535 입니다.", "입력 오류", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String name = tfName.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "이름을 입력하세요.", "입력 오류", JOptionPane.WARNING_MESSAGE);
            return;
        }

        result = new Result(host, port, name);
        dispose();
    }

    // ================== 정적 헬퍼 ==================

    /**
     * 모달 다이얼로그를 띄워 접속 정보를 입력받는다.
     * @return 사용자가 OK를 누르면 Result, 그렇지 않으면 null
     */
    public static Result show(Window owner, String defHost, int defPort, String defName) {
        JoinDialog d = new JoinDialog(owner, defHost, defPort, defName);
        d.setVisible(true); // 모달
        return d.result;
    }
}
