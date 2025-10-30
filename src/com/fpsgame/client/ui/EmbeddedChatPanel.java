package com.fpsgame.client.ui;

import com.fpsgame.client.NetClient;
import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/**
 * 임베드 가능한 경량 채팅 패널.
 *
 * <p>특징</p>
 * <ul>
 *   <li>프레임이 아닌 {@link JPanel} 기반이라 어디든 포함 가능</li>
 *   <li>{@link NetClient}와 느슨 결합: 외부에서 setClient(...)로 주입하면 즉시 사용</li>
 *   <li>레거시/신규 프로토콜 호환: CHAT 프레임 수신을 onChat 또는 onFrame 경로 모두 처리</li>
 *   <li>EDT 안전: 외부 스레드에서 호출해도 내부에서 EDT로 디스패치</li>
 * </ul>
 *
 * <p>연동 예시</p>
 * <pre>
 *   EmbeddedChatPanel chat = new EmbeddedChatPanel();
 *   NetClient client = new NetClient(new NetClient.Listener() {
 *       @Override public void onChat(String text) { chat.appendRemote(text); }
 *       @Override public void onFrame(NetClient c, Protocol.Frame f) {
 *           if (f.opcode == Protocol.CHAT) chat.appendRemote(Protocol.parseChat(f.payload));
 *       }
 *   }, true);
 *   client.connect("127.0.0.1", 7777, 3000);
 *   chat.setClient(client);
 * </pre>
 */
public class EmbeddedChatPanel extends JPanel {

    private final JTextArea chatArea = new JTextArea();
    private final JTextField input = new JTextField();
    private final JButton sendBtn = new JButton("Send");

    /** 선택 주입: 연결된 NetClient. null이면 로컬 echo 모드 */
    private volatile NetClient client;

    public EmbeddedChatPanel() {
        setLayout(new BorderLayout(6, 6));
        setBorder(new EmptyBorder(6, 6, 6, 6));

        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        chatArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane scroll = new JScrollPane(chatArea);

        JPanel bottom = new JPanel(new BorderLayout(6, 6));
        bottom.add(input, BorderLayout.CENTER);
        bottom.add(sendBtn, BorderLayout.EAST);

        add(scroll, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);

        input.addActionListener(e -> doSend());
        sendBtn.addActionListener(e -> doSend());
    }

    /** 외부에서 NetClient를 주입(교체)한다. */
    public void setClient(NetClient client) {
        this.client = client;
    }

    /** 채팅 한 줄을 전송한다(연결 없으면 로컬 echo). */
    public void doSend() {
        String s = input.getText().trim();
        if (s.isEmpty()) return;

        NetClient c = client;
        if (c == null || !c.isConnected()) {
            appendLocal(s);
            input.setText("");
            return;
        }
        try {
            c.sendChat(s);
            appendLocal(s);
        } catch (Exception ex) {
            appendSystem("전송 실패: " + ex.getMessage());
        } finally {
            input.setText("");
            input.requestFocusInWindow();
        }
    }

    // ================= 출력 헬퍼 =================

    /** 서버에서 수신한 채팅을 표시 */
    public void appendRemote(String line) {
        append("srv", line);
    }

    /** 로컬 사용자가 보낸 채팅을 표시 */
    public void appendLocal(String line) {
        append("me", line);
    }

    /** 시스템 메시지 표시 */
    public void appendSystem(String line) {
        append("sys", line);
    }

    private void append(String who, String line) {
        SwingUtilities.invokeLater(() -> {
            chatArea.append("[" + who + "] " + line + "\n");
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
        });
    }

    // ================= 단독 테스트 =================

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("EmbeddedChatPanel Demo");
            EmbeddedChatPanel p = new EmbeddedChatPanel();
            f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            f.setSize(480, 320);
            f.setLocationRelativeTo(null);
            f.setContentPane(p);
            f.setVisible(true);
        });
    }
}
