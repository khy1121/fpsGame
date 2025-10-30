package com.fpsgame.client.ui;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.DefaultCaret;

/**
 * 간단한 채팅 UI 패널.
 * <p>
 * - 상단: 스크롤 가능한 로그(읽기 전용)
 * - 하단: 입력 필드 + 전송 버튼
 * - Enter 키로 전송, Shift+Enter 줄바꿈
 * - 외부에서 {@link #setSendListener(Consumer)} 로 전송 콜백 주입
 */
public final class ChatPanel extends JPanel {

    /** 메시지 로그 영역(읽기 전용) */
    private final JTextArea logArea = new JTextArea();

    /** 입력 필드 */
    private final JTextArea inputArea = new JTextArea(2, 20);

    /** 전송 버튼 */
    private final JButton sendBtn = new JButton("Send");

    /** 메시지 전송 콜백(널 허용) */
    private volatile Consumer<String> sendListener;

    public ChatPanel() {
        super(new BorderLayout(8, 8));
        setBorder(new EmptyBorder(8, 8, 8, 8));

        // 로그 영역 설정
        logArea.setEditable(false);
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        logArea.setBackground(new Color(0xFAFAFA));
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        // 새 메시지 도착 시 자동 스크롤
        ((DefaultCaret) logArea.getCaret()).setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

        // 입력 영역 설정
        inputArea.setLineWrap(true);
        inputArea.setWrapStyleWord(true);
        inputArea.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        JScrollPane inputScroll = new JScrollPane(inputArea);
        inputScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

        // 하단 바: 입력 + 전송 버튼
        JPanel bottom = new JPanel(new BorderLayout(8, 0));
        bottom.add(inputScroll, BorderLayout.CENTER);
        bottom.add(sendBtn, BorderLayout.EAST);

        add(logScroll, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);

        // 액션 바인딩
        installActions();
    }

    /** 외부에서 전송 콜백을 주입(널 허용). */
    public void setSendListener(Consumer<String> sendListener) {
        this.sendListener = sendListener;
    }

    /** 로그에 한 줄 추가(자동 줄바꿈 포함). */
    public void appendMessage(String line) {
        if (line == null) return;
        if (!line.endsWith("\n")) line += "\n";
        logArea.append(line);
    }

    // ---------------------------------------------------------------------
    // 내부: 단축키/버튼 액션
    // ---------------------------------------------------------------------

    private void installActions() {
        // Enter → send, Shift+Enter → 줄바꿈
        InputMap im = inputArea.getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap am = inputArea.getActionMap();

        im.put(KeyStroke.getKeyStroke("ENTER"), "sendOrNewline");
        am.put("sendOrNewline", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                if (isShiftDown(e)) {
                    inputArea.insert("\n", inputArea.getCaretPosition());
                } else {
                    triggerSend();
                }
            }
        });

        sendBtn.addActionListener(e -> triggerSend());
    }

    private boolean isShiftDown(ActionEvent e) {
        return (e.getModifiers() & ActionEvent.SHIFT_MASK) == ActionEvent.SHIFT_MASK;
    }

    private void triggerSend() {
        String text = inputArea.getText();
        if (text == null) text = "";
        text = text.trim();
        if (text.isEmpty()) return;

        Consumer<String> cb = this.sendListener;
        if (cb != null) {
            try {
                cb.accept(text);
            } catch (Throwable t) {
                appendMessage("[error] send failed: " + t.getMessage());
            }
        } else {
            appendMessage("[warn] no send listener bound");
        }
        inputArea.setText("");
        inputArea.requestFocusInWindow();
    }

    // 간단 데모 실행
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("ChatPanel Demo");
            f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            ChatPanel p = new ChatPanel();
            p.setSendListener(msg -> p.appendMessage("[echo] " + msg));
            f.setContentPane(p);
            f.setSize(640, 420);
            f.setLocationRelativeTo(null);
            f.setVisible(true);

            p.appendMessage("[system] 이 패널은 네트 없이 로컬 에코로 동작합니다.");
        });
    }
}
