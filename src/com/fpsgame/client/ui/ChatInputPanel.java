package com.fpsgame.client.ui;

import com.fpsgame.client.ChatCommandHandler;
import com.fpsgame.client.NetClient;
import java.awt.*;
import java.io.IOException;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/**
 * 메시지 전송을 위한 채팅 입력 패널.
 *
 * 기능:
 * - 텍스트 영역 + 전송 버튼
 * - Enter=전송, Shift+Enter=줄바꿈
 * - 슬래시 명령어는 {@link ChatCommandHandler}가 처리
 * - 네트워크 오프라인 시 로거를 통해 로컬로 에코
 *
 * 사용법:
 * <pre>
 *   ChatInputPanel chat = new ChatInputPanel();
 *   chat.setNetClient(netClient, console::println);
 *   frame.add(chat, BorderLayout.SOUTH);
 * </pre>
 */
public class ChatInputPanel extends JPanel {

    private final JTextArea input = new JTextArea(2, 32);
    private final JButton sendBtn = new JButton("Send");

    private volatile NetClient net;                // null: offline
    private volatile Consumer<String> logger = s -> {}; // console/log output

    private ChatCommandHandler cmdHandler; // initialized when net set

    public ChatInputPanel() {
        super(new BorderLayout(6, 6));
        setBorder(new EmptyBorder(6, 6, 6, 6));

        input.setLineWrap(true);
        input.setWrapStyleWord(true);
        input.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));

        JScrollPane scroll = new JScrollPane(input,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        add(scroll, BorderLayout.CENTER);
        add(sendBtn, BorderLayout.EAST);

        // Key bindings: Enter=send, Shift+Enter=newline
        InputMap im = input.getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap am = input.getActionMap();
        im.put(KeyStroke.getKeyStroke("ENTER"), "send");
        im.put(KeyStroke.getKeyStroke("shift ENTER"), "newline");
        am.put("send", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { doSend(); }
        });
        am.put("newline", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { input.replaceSelection("\n"); }
        });

        sendBtn.addActionListener(e -> doSend());
    }

    /**
     * 네트워크와 로거 설정.
     * @param client  NetClient (null 허용: 오프라인 에코 모드)
     * @param logger  출력 대상 (예: 콘솔/채팅)
     */
    public void setNetClient(NetClient client, Consumer<String> logger) {
        this.net = client;
        this.logger = logger == null ? (s -> {}) : logger;
        this.cmdHandler = new ChatCommandHandler(client, this.logger);
        updateUiEnabled(client != null);
    }

    /** 온라인 상태에 따라 UI 활성화/비활성화. */
    public void updateUiEnabled(boolean online) {
        // Always allow input; change button label when offline
        input.setEnabled(true);
        sendBtn.setEnabled(true);
        sendBtn.setText(online ? "Send" : "Echo");
    }

    /** Focus the input field. */
    public void focusInput() {
        SwingUtilities.invokeLater(() -> input.requestFocusInWindow());
    }

    // ================= impl =================

    private void doSend() {
        String text = input.getText();
        if (text == null) text = "";
        String s = text.strip();
        if (s.isEmpty()) return;

        // Handle multi-line input safely
        for (String line : s.split("\\R")) {
            if (line.isBlank()) continue;
            if (cmdHandler != null && cmdHandler.handleInput(line)) {
                continue; // handled as command
            }
            NetClient c = net;
            if (c != null && c.isConnected()) {
                try {
                    c.sendChat(line);
                    // Also print locally for better UX
                    logger.accept("[me] " + line);
                } catch (IOException ex) {
                    logger.accept("Chat send failed: " + ex.getMessage());
                }
            } else {
                // Offline echo
                logger.accept("[local] " + line);
            }
        }

        input.setText("");
        focusInput();
    }

    // ================= standalone demo =================

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("ChatInputPanel Demo");
            f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            f.setSize(640, 180);
            f.setLocationRelativeTo(null);

            JTextArea logArea = new JTextArea();
            logArea.setEditable(false);
            logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
            JScrollPane logScroll = new JScrollPane(logArea);

            ChatInputPanel chat = new ChatInputPanel();
            chat.setNetClient(null, s -> {
                logArea.append(s + "\n");
                logArea.setCaretPosition(logArea.getDocument().getLength());
            });

            f.setLayout(new BorderLayout());
            f.add(logScroll, BorderLayout.CENTER);
            f.add(chat, BorderLayout.SOUTH);
            f.setVisible(true);

            chat.focusInput();
        });
    }
}

