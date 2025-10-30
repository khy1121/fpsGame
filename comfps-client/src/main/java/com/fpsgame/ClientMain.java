package com.fpsgame.client;

import com.fpsgame.common.Protocol;
import java.awt.*;
import java.awt.event.ActionEvent;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

/**
 * 데모/테스트용 최소 Swing 클라이언트 시작점.
 *
 * 기능:
 * - 서버 접속/해제, 채팅 송수신
 * - READY 토글, 팀/캐릭터 선택, 맵 투표
 * - 페이즈/카운트다운/라운드 결과 표시
 */
public final class ClientMain {

    private ClientMain() {}

    public static void main(String[] args) {
        try { com.fpsgame.common.Utf8.installConsoleUtf8(); } catch (Throwable ignore) {}
        SwingUtilities.invokeLater(() -> {
            setSystemLookAndFeel();
            new MainFrame().setVisible(true);
        });
    }

    private static void setSystemLookAndFeel() {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (Exception ignore) {}
    }

    static final class MainFrame extends JFrame implements ClientController.Ui {

        // 네트워킹 컨트롤러(EDT에서 디스패치)
        private final ClientController controller = new ClientController(this, true);

        // 상단: 접속 바
        private final JTextField hostField = new JTextField("127.0.0.1");
        private final JSpinner   portSpinner = new JSpinner(new SpinnerNumberModel(7777, 1, 65535, 1));
        private final JButton    connectBtn = new JButton("Connect");
        private final JButton    disconnectBtn = new JButton("Disconnect");
        private final JButton    pingBtn = new JButton("Ping");

        // 중앙: 채팅 로그
        private final JTextPane chatPane = new JTextPane();
        private final StyledDocument chatDoc = chatPane.getStyledDocument();

        // 하단: 입력 + 액션 버튼들
        private final JTextField chatInput = new JTextField();
        private final JButton    sendBtn = new JButton("Send");
        private final JToggleButton readyToggle = new JToggleButton("READY");
        private final JComboBox<String> teamCombo = new JComboBox<>(new String[]{"Team0","Team1","Team2","Team3"});
        private final JComboBox<String> charCombo = new JComboBox<>(new String[]{"Sage","Piper","Technician","General","Bulldog","Wildcat","Raven","Ghost","Skull","Steam"});
        private final JButton    selectBtn = new JButton("Select");
        private final JComboBox<String> mapCombo = new JComboBox<>(new String[]{"terminal","neonCity","ForestOutpost"});
        private final JButton    voteBtn = new JButton("Vote");

        private final JLabel statusLabel = new JLabel("Disconnected");

        MainFrame() {
            super("FPS Client");
            setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            setSize(960, 600);
            setLocationRelativeTo(null);

            chatPane.setEditable(false);
            chatPane.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

            JPanel root = new JPanel(new BorderLayout(12, 8));
            root.setBorder(new EmptyBorder(8, 8, 8, 8));
            setContentPane(root);

            root.add(buildTopBar(), BorderLayout.NORTH);
            root.add(new JScrollPane(chatPane), BorderLayout.CENTER);
            root.add(buildBottomBar(), BorderLayout.SOUTH);
            root.add(statusLabel, BorderLayout.WEST);

            bindHandlers();

            addWindowListener(new java.awt.event.WindowAdapter() {
                @Override public void windowClosing(java.awt.event.WindowEvent e) {
                    try { controller.uninstallUi(); } catch (Throwable ignore) {}
                    try { controller.close(); } catch (Throwable ignore) {}
                }
            });

            updateUiState(false);
        }

        private JPanel buildTopBar() {
            JPanel p = new JPanel(new GridBagLayout());
            GridBagConstraints c = new GridBagConstraints();
            c.gridy = 0; c.insets = new Insets(0,0,0,6);
            c.gridx = 0; p.add(new JLabel("Host:"), c);
            c.gridx = 1; c.fill = GridBagConstraints.HORIZONTAL; c.weightx = 1.0; p.add(hostField, c);
            c.gridx = 2; c.fill = GridBagConstraints.NONE; c.weightx = 0; p.add(new JLabel("Port:"), c);
            c.gridx = 3; p.add(portSpinner, c);
            c.gridx = 4; p.add(connectBtn, c);
            c.gridx = 5; p.add(disconnectBtn, c);
            c.gridx = 6; p.add(pingBtn, c);
            return p;
        }

        private JPanel buildBottomBar() {
            JPanel left = new JPanel(new BorderLayout(6, 6));
            left.add(chatInput, BorderLayout.CENTER);
            left.add(sendBtn, BorderLayout.EAST);

            JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
            right.add(readyToggle);
            right.add(new JLabel("Team:"));
            right.add(teamCombo);
            right.add(new JLabel("Char:"));
            right.add(charCombo);
            right.add(selectBtn);
            right.add(new JLabel("Map:"));
            right.add(mapCombo);
            right.add(voteBtn);

            JPanel wrap = new JPanel(new BorderLayout(12, 6));
            wrap.add(left, BorderLayout.CENTER);
            wrap.add(right, BorderLayout.EAST);
            return wrap;
        }

        private void bindHandlers() {
            // Connect
            connectBtn.addActionListener(e -> {
                try {
                    controller.connect(hostField.getText().trim(), (Integer) portSpinner.getValue(), 3000);
                    controller.attachUi(this);
                    appendSystem("Connecting...");
                    updateUiState(true);
                    statusLabel.setText("Connected: " + hostField.getText() + ":" + portSpinner.getValue());
                } catch (Exception ex) {
                    appendError("Connect failed: " + ex.getMessage());
                    updateUiState(false);
                    statusLabel.setText("Disconnected");
                }
            });

            // Disconnect
            disconnectBtn.addActionListener(e -> {
                try { controller.uninstallUi(); } catch (Throwable ignore) {}
                safeDisconnect();
                appendSystem("Disconnect requested");
            });

            // Send chat
            sendBtn.addActionListener(e -> sendChatFromInput());
            chatInput.addActionListener(e -> sendChatFromInput());

            // READY 토글
            readyToggle.addActionListener(e -> {
                boolean ready = readyToggle.isSelected();
                try {
                    controller.sendReadyToggle(ready);
                    appendSystem("READY=" + ready);
                } catch (Exception ex) {
                    appendError("Send READY failed: " + ex.getMessage());
                }
            });

            // 팀/캐릭터 선택 전송
            selectBtn.addActionListener(e -> {
                int team = Math.max(0, teamCombo.getSelectedIndex());
                int ch   = Math.max(0, charCombo.getSelectedIndex());
                try {
                    controller.sendSetSelection(team, ch);
                    appendSystem("Selection sent: team=" + team + ", char=" + ch);
                } catch (Exception ex) {
                    appendError("Selection failed: " + ex.getMessage());
                }
            });

            // 맵 투표 전송
            voteBtn.addActionListener(e -> {
                int mapId = Math.max(0, mapCombo.getSelectedIndex());
                try {
                    controller.sendMapVote(mapId);
                    appendSystem("Map vote sent: map=" + mapId);
                } catch (Exception ex) {
                    appendError("Map vote failed: " + ex.getMessage());
                }
            });

            // 핑 전송
            pingBtn.addActionListener((ActionEvent e) -> {
                try {
                    controller.sendPingOnce();
                } catch (Exception ex) {
                    appendError("Ping failed: " + ex.getMessage());
                }
            });
        }

        private void sendChatFromInput() {
            String msg = chatInput.getText().trim();
            if (msg.isEmpty()) return;
            chatInput.setText("");
            try {
                controller.sendChat(msg);
                // 로컬 에코는 제거하고 서버 브로드캐스트만 표시
            } catch (Exception ex) {
                appendError("Chat send failed: " + ex.getMessage());
            }
        }

        private void safeDisconnect() {
            try { controller.disconnect(); } catch (Exception ignore) {}
            updateUiState(false);
            statusLabel.setText("Disconnected");
        }

        private void updateUiState(boolean connected) {
            hostField.setEnabled(!connected);
            portSpinner.setEnabled(!connected);
            connectBtn.setEnabled(!connected);
            disconnectBtn.setEnabled(connected);
            pingBtn.setEnabled(connected);
            sendBtn.setEnabled(true);
            chatInput.setEnabled(true);
            readyToggle.setEnabled(connected);
            selectBtn.setEnabled(connected);
            voteBtn.setEnabled(connected);
            teamCombo.setEnabled(connected);
            charCombo.setEnabled(connected);
            mapCombo.setEnabled(connected);
        }

        // =================== UI 콜백 (ClientController.Ui) ===================
        @Override public void onChat(String text) { appendChat(formatChat(text)); }
        @Override public void onWelcome(Protocol.Welcome welcome) {
            appendSystem("WELCOME id=" + welcome.myId + " world=" + welcome.worldW + "x" + welcome.worldH + 
                        (welcome.mapId >= 0 ? (" map=" + welcome.mapId) : "") +
                        (welcome.team >= 0 ? (" team=" + welcome.team) : "") +
                        (welcome.character >= 0 ? (" char=" + welcome.character) : ""));
            
            // 팀/캐릭터 선택 UI 업데이트
            try {
                if (welcome.team >= 0 && welcome.team < teamCombo.getItemCount()) {
                    teamCombo.setSelectedIndex(welcome.team);
                }
                if (welcome.character >= 0 && welcome.character < charCombo.getItemCount()) {
                    charCombo.setSelectedIndex(welcome.character);
                }
            } catch (Exception ignore) {}
        }
        @Override public void onPhaseUpdate(int phaseCode) { appendSystem("PHASE=" + phaseCode); }
        @Override public void onCountdown(int seconds) { appendSystem("COUNTDOWN=" + seconds); }
        @Override public void onRoundResult(Protocol.RoundResult rr) {
            appendSystem("ROUND winner=" + rr.winnerTeam + " score " + rr.blueRounds + ":" + rr.redRounds + (rr.matchEnded ? " end" : ""));
        }
        @Override public void onReadyStatus(int ready, int total) { appendSystem("READY_STATUS ready=" + ready + " total=" + total); }
        @Override public void onDisconnected(String message) { appendError("Disconnected: " + message); }

        // =================== 채팅 표시 도우미 ===================
        private void appendChat(String s) { append(s + "\n", new Color(0,128,255)); }
        private void appendLocal(String s) { append(s + "\n", new Color(120,120,120)); }
        private void appendSystem(String s) { append("[SYS] " + s + "\n", new Color(0,160,0)); }
        private void appendError(String s) { append("[ERR] " + s + "\n", new Color(200,40,40)); }

        private void append(String s, Color color) {
            try {
                SimpleAttributeSet a = new SimpleAttributeSet();
                StyleConstants.setForeground(a, color);
                chatDoc.insertString(chatDoc.getLength(), s, a);
                chatPane.setCaretPosition(chatDoc.getLength());
            } catch (Exception ignore) {}
        }

        // 수신 채팅을 "닉네임 - 메시지" 형식으로 정규화
        private String formatChat(String raw) {
            if (raw == null) return "";
            String s = raw.trim();
            // 서버가 "[id] message" 형태로 보낼 때 id 추출
            if (s.startsWith("[") && s.contains("]")) {
                int r = s.indexOf(']');
                String id = s.substring(1, r).trim();
                String msg = s.substring(r + 1).trim();
                if (!id.isEmpty() && !msg.isEmpty()) return id + " - " + msg;
            }
            return s;
        }
    }
}
