package com.fpsgame.client.ui;

import com.fpsgame.client.NetClient;
import com.fpsgame.common.Protocol;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * 간단 채팅 전용 창(Opcode 호환 + 예외 처리 보강판).
 *
 * <p>변경 사항</p>
 * <ul>
 *   <li>{@link NetClient} 생성자가 {@link IOException}을 던지므로
 *       {@code doConnect()}에서 try/catch로 처리하여
 *       <b>"Unhandled exception type IOException"</b> 컴파일 오류 제거</li>
 *   <li>프레임 수신 시 PING→PONG 에코는 그대로 유지</li>
 * </ul>
 */
public class ChatWindow extends JFrame {

    // 상단 연결 바 구성 요소
    private final JTextField hostField = new JTextField("127.0.0.1", 12);
    private final JSpinner portField = new JSpinner(new SpinnerNumberModel(7777, 1, 65535, 1));
    private final JButton connectBtn = new JButton("연결");
    private final JButton disconnectBtn = new JButton("끊기");

    // 채팅 영역
    private final JTextArea chatArea = new JTextArea();
    private final JTextField inputField = new JTextField();

    // 네트워크 핸들
    private volatile NetClient client; // null 이면 오프라인

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("HH:mm:ss");

    public ChatWindow() {
        super("채팅");

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setSize(640, 480);
        setLocationRelativeTo(null);

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
        top.add(new JLabel("Host"));
        top.add(hostField);
        top.add(new JLabel("Port"));
        ((JSpinner.DefaultEditor) portField.getEditor()).getTextField().setColumns(5);
        top.add(portField);
        top.add(connectBtn);
        top.add(disconnectBtn);
        disconnectBtn.setEnabled(false);

        chatArea.setEditable(false);
        chatArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        JScrollPane scroll = new JScrollPane(chatArea);

        JPanel bottom = new JPanel(new BorderLayout(6, 6));
        bottom.setBorder(new EmptyBorder(6, 0, 0, 0));
        bottom.add(inputField, BorderLayout.CENTER);
        JButton sendBtn = new JButton("보내기");
        bottom.add(sendBtn, BorderLayout.EAST);

        JPanel root = new JPanel(new BorderLayout(6, 6));
        root.setBorder(new EmptyBorder(8, 8, 8, 8));
        root.add(top, BorderLayout.NORTH);
        root.add(scroll, BorderLayout.CENTER);
        root.add(bottom, BorderLayout.SOUTH);
        setContentPane(root);

        // 이벤트 바인딩
        connectBtn.addActionListener(e -> doConnect());
        disconnectBtn.addActionListener(e -> doDisconnect());
        inputField.addActionListener(e -> doSend());
        sendBtn.addActionListener(e -> doSend());

        addWindowListener(new WindowAdapter() {
            @Override public void windowClosed(WindowEvent e) { doDisconnect(); }
        });

        logLocal("채팅 창 준비 완료.");
    }

    // ---------------------------------------------------------------------
    // 네트워크
    // ---------------------------------------------------------------------

    /** 서버 연결 시도(이미 연결되어 있으면 무시). */
    private void doConnect() {
        if (client != null && client.isOpen()) return;
        String host = hostField.getText().trim();
        int port = (int) portField.getValue();

        try {
            // NetClient(host,port,timeout,listener) 생성자는 내부에서 connect까지 수행
            client = new NetClient(host, port, 20_000, new NetClient.Listener() {
                @Override public void onOpen(NetClient c) {
                    SwingUtilities.invokeLater(() -> {
                        logSys("서버에 연결되었습니다: " + c.remote());
                        setConnected(true);
                    });
                }

                @Override public void onFrame(NetClient c, Protocol.Frame frame) {
                    if (frame == null) return;
                    switch (frame.opcode) {
                        case Protocol.Opcode.CHAT: {
                            String msg = Protocol.parseChat(frame.payload);
                            logRemote(msg);
                            break;
                        }
                        case Protocol.Opcode.WELCOME: {
                            logSys("WELCOME (len=" + frame.payload.length + ")");
                            break;
                        }
                        case Protocol.Opcode.PING: {
                            try {
                                // 그대로 PONG 에코
                                c.send(Protocol.Opcode.PONG, frame.payload);
                            } catch (Exception ex) {
                                logSys("PONG 전송 실패: " + ex.getMessage());
                            }
                            break;
                        }
                        default: /* ignore */ break;
                    }
                }

                @Override public void onChat(String text) {
                    SwingUtilities.invokeLater(() -> logRemote(text));
                }

                @Override public void onDisconnected(String reason) {
                    SwingUtilities.invokeLater(() -> {
                        logSys("연결 종료: " + reason);
                        setConnected(false);
                    });
                }

                @Override public void onClosed(NetClient c, String reason) {
                    SwingUtilities.invokeLater(() -> {
                        logSys("연결 종료: " + reason);
                        setConnected(false);
                    });
                }
            });
        } catch (IOException ex) {
            // ★ 여기서 IOException을 처리하여 컴파일 오류 제거
            logSys("연결 실패: " + ex.getMessage());
            JOptionPane.showMessageDialog(this, "연결 실패: " + ex.getMessage(),
                    "Connect Error", JOptionPane.ERROR_MESSAGE);
            setConnected(false);
        }
    }

    /** 연결 종료. */
    private void doDisconnect() {
        NetClient c = client;
        client = null;
        if (c != null) {
            try { c.close(); } catch (Exception ignored) {}
        }
        setConnected(false);
    }

    /** 채팅 전송. */
    private void doSend() {
        String s = inputField.getText().trim();
        if (s.isEmpty()) return;

        NetClient c = client;
        if (c == null || !c.isOpen()) {
            // 오프라인 echo
            logLocal(s);
            inputField.setText("");
            return;
        }

        try {
            c.sendChat(s);
            logLocal(s);
        } catch (Exception ex) {
            logSys("전송 실패: " + ex.getMessage());
        } finally {
            inputField.setText("");
            inputField.requestFocusInWindow();
        }
    }

    /** 연결/입력 상태 UI 토글. */
    private void setConnected(boolean connected) {
        hostField.setEnabled(!connected);
        portField.setEnabled(!connected);
        connectBtn.setEnabled(!connected);
        disconnectBtn.setEnabled(connected);
        inputField.setEnabled(connected);
    }

    // ---------------------------------------------------------------------
    // 출력 헬퍼
    // ---------------------------------------------------------------------

    private void logLocal(String msg) { appendLine("[" + now() + "][me] " + msg); }
    private void logRemote(String msg){ appendLine("[" + now() + "][srv] " + msg); }
    private void logSys(String msg)   { appendLine("[" + now() + "][sys] " + msg); }

    private void appendLine(String line) {
        chatArea.append(line + "\n");
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
    }

    private static String now() { return TS.format(LocalTime.now()); }

    // 단독 실행
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ChatWindow().setVisible(true));
    }
}
