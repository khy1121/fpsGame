package com.fpsgame.client.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

/**
 * 채팅 패널 (JPanel 기반)
 * - LobbyFrame에 임베드 가능
 * - 채팅 메시지 표시 및 입력
 */
public class ChatPanel extends JPanel {

    private final JTextArea chatArea = new JTextArea();
    private final JTextField inputField = new JTextField();
    private final JButton sendBtn = new JButton("Send");
    
    private Consumer<String> onSendChat;
    
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("HH:mm:ss");

    public ChatPanel() {
        setLayout(new BorderLayout(0, 8));
        setBackground(new Color(0x1a1d24));
        setOpaque(true);
        
        buildUI();
    }

    private void buildUI() {
        // 채팅 영역
        chatArea.setEditable(false);
        chatArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        chatArea.setBackground(new Color(0x0d0f13));
        chatArea.setForeground(Color.WHITE);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        
        JScrollPane chatScroll = new JScrollPane(chatArea);
        chatScroll.setBorder(BorderFactory.createLineBorder(new Color(0x2a2d34), 1));
        chatScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        // 입력 영역
        JPanel inputPanel = new JPanel(new BorderLayout(4, 0));
        inputPanel.setOpaque(false);
        
        inputField.setFont(new Font("Arial", Font.PLAIN, 12));
        inputField.setBackground(new Color(0x0d0f13));
        inputField.setForeground(Color.WHITE);
        inputField.setCaretColor(Color.WHITE);
        inputField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0x2a2d34), 1),
            new EmptyBorder(4, 8, 4, 8)
        ));

        sendBtn.setBackground(new Color(0x4a90e2));
        sendBtn.setForeground(Color.WHITE);
        sendBtn.setFocusPainted(false);
        sendBtn.setBorderPainted(false);
        sendBtn.setFont(new Font("Arial", Font.BOLD, 12));
        sendBtn.setPreferredSize(new Dimension(70, 32));

        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendBtn, BorderLayout.EAST);

        add(chatScroll, BorderLayout.CENTER);
        add(inputPanel, BorderLayout.SOUTH);

        // 이벤트 바인딩
        Runnable sendAction = this::doSend;
        inputField.addActionListener(e -> sendAction.run());
        sendBtn.addActionListener(e -> sendAction.run());
    }

    private void doSend() {
        String msg = inputField.getText().trim();
        if (msg.isEmpty()) return;
        
        if (onSendChat != null) {
            onSendChat.accept(msg);
        }
        
        inputField.setText("");
        inputField.requestFocusInWindow();
    }

    // ---------------------------------------------------------------------
    // Public API
    // ---------------------------------------------------------------------

    /**
     * 채팅 전송 콜백 설정
     */
    public void setOnSendChat(Consumer<String> callback) {
        this.onSendChat = callback;
    }

    /**
     * 시스템 메시지 추가
     */
    public void appendSystemMessage(String msg) {
        appendLine("[" + now() + "][System] " + msg);
    }

    /**
     * 다른 플레이어 메시지 추가
     */
    public void appendOtherMessage(String msg) {
        appendLine("[" + now() + "][Chat] " + msg);
    }

    /**
     * 내 메시지 추가
     */
    public void appendMyMessage(String msg) {
        appendLine("[" + now() + "][Me] " + msg);
    }

    /**
     * 일반 메시지 추가
     */
    public void appendMessage(String msg) {
        appendLine(msg);
    }

    private void appendLine(String line) {
        SwingUtilities.invokeLater(() -> {
            chatArea.append(line + "\n");
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
        });
    }

    private static String now() {
        return TS.format(LocalTime.now());
    }
}
