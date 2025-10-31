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
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

/**
 * 채팅 패널 (JPanel 기반)
 * - LobbyFrame에 임베드 가능
 * - 채팅 메시지 표시 및 입력
 * - 색상별 메시지 구분
 */
public class ChatPanel extends JPanel {

    private final JTextPane chatPane = new JTextPane();
    private final StyledDocument doc;
    private final JTextField inputField = new JTextField();
    private final JButton sendBtn = new JButton("Send");
    
    private final Style myStyle;
    private final Style otherStyle;
    private final Style systemStyle;
    
    private Consumer<String> onSendChat;
    
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("HH:mm:ss");

    public ChatPanel() {
        setLayout(new BorderLayout(0, 8));
        setBackground(new Color(0x1a1d24));
        setOpaque(true);
        
        // 스타일 초기화
        doc = chatPane.getStyledDocument();
        
        myStyle = chatPane.addStyle("MyStyle", null);
        StyleConstants.setForeground(myStyle, new Color(0xFFD700)); // 노란색
        StyleConstants.setFontFamily(myStyle, "맑은 고딕");
        StyleConstants.setFontSize(myStyle, 12);
        
        otherStyle = chatPane.addStyle("OtherStyle", null);
        StyleConstants.setForeground(otherStyle, Color.WHITE);
        StyleConstants.setFontFamily(otherStyle, "맑은 고딕");
        StyleConstants.setFontSize(otherStyle, 12);
        
        systemStyle = chatPane.addStyle("SystemStyle", null);
        StyleConstants.setForeground(systemStyle, new Color(0x808080)); // 회색
        StyleConstants.setFontFamily(systemStyle, "맑은 고딕");
        StyleConstants.setFontSize(systemStyle, 11);
        
        buildUI();
    }

    private void buildUI() {
        // 채팅 영역
        chatPane.setEditable(false);
        chatPane.setBackground(new Color(0x0d0f13));
        chatPane.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        
        JScrollPane chatScroll = new JScrollPane(chatPane);
        chatScroll.setBorder(BorderFactory.createLineBorder(new Color(0x2a2d34), 1));
        chatScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        // 입력 영역
        JPanel inputPanel = new JPanel(new BorderLayout(4, 0));
        inputPanel.setOpaque(false);
        
        inputField.setFont(new Font("맑은 고딕", Font.PLAIN, 13));
        inputField.setBackground(new Color(0x0d0f13));
        inputField.setForeground(Color.WHITE);
        inputField.setCaretColor(Color.WHITE);
        inputField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0x2a2d34), 1),
            new EmptyBorder(6, 10, 6, 10)
        ));

        sendBtn.setBackground(new Color(0x4a90e2));
        sendBtn.setForeground(Color.WHITE);
        sendBtn.setFocusPainted(false);
        sendBtn.setBorderPainted(false);
        sendBtn.setFont(new Font("맑은 고딕", Font.BOLD, 12));
        sendBtn.setPreferredSize(new Dimension(70, 36));

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
        appendStyledLine("[" + now() + "][System] " + msg, systemStyle);
    }

    /**
     * 다른 플레이어 메시지 추가
     */
    public void appendOtherMessage(String msg) {
        appendStyledLine("[" + now() + "][Chat] " + msg, otherStyle);
    }

    /**
     * 내 메시지 추가 (노란색)
     */
    public void appendMyMessage(String msg) {
        appendStyledLine("[" + now() + "][Me] " + msg, myStyle);
    }

    /**
     * 일반 메시지 추가
     */
    public void appendMessage(String msg) {
        appendStyledLine(msg, otherStyle);
    }

    private void appendStyledLine(String line, Style style) {
        SwingUtilities.invokeLater(() -> {
            try {
                doc.insertString(doc.getLength(), line + "\n", style);
                chatPane.setCaretPosition(doc.getLength());
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        });
    }

    private void appendLine(String line) {
        appendStyledLine(line, otherStyle);
    }

    private static String now() {
        return TS.format(LocalTime.now());
    }
}
