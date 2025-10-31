package com.fpsgame.client.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;

import com.fpsgame.client.ClientController;
import com.fpsgame.client.model.Settings;
import com.fpsgame.common.Protocol;

/**
 * 로비 화면 프레임
 * - Map Info / Character Select 탭
 * - RED TEAM / BLUE TEAM 선택
 * - 팀별 5명 슬롯
 * - READY 버튼
 * - 채팅 패널
 */
public class LobbyFrame extends JFrame implements ClientController.Ui {

    private final String nickname;
    private final Settings settings;
    private final ClientController controller;

    // 탭 패널
    private final JTabbedPane tabbedPane = new JTabbedPane();
    private final MapInfoPanel mapInfoPanel;
    private final CharacterSelectPanel charSelectPanel;

    // 팀 선택
    private final JButton redTeamBtn = new JButton("RED TEAM");
    private final JButton blueTeamBtn = new JButton("BLUE TEAM");
    private int selectedTeam = 0; // 0=RED, 1=BLUE

    // 팀 슬롯
    private final JLabel[] redSlots = new JLabel[5];
    private final JLabel[] blueSlots = new JLabel[5];

    // READY 버튼
    private final JButton readyBtn = new JButton("READY");
    private boolean isReady = false;

    // 채팅 패널
    private final ChatPanel chatPanel = new ChatPanel();

    // 연결 정보
    private final JLabel hostLabel = new JLabel("Host:");
    private final JTextField hostField = new JTextField("127.0.0.1");
    private final JLabel portLabel = new JLabel("Port:");
    private final JTextField portField = new JTextField("7,777");

    public LobbyFrame(String nickname, Settings settings) {
        super("FPS Client");
        this.nickname = nickname;
        this.settings = settings;
        this.controller = new ClientController(this, true);

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setSize(1400, 800);
        setLocationRelativeTo(null);

        // 패널 초기화
        mapInfoPanel = new MapInfoPanel();
        charSelectPanel = new CharacterSelectPanel();
        charSelectPanel.withDefaultCharacters();

        buildUI();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                closeLobby();
            }
        });

        // 자동 서버 연결 시도
        SwingUtilities.invokeLater(this::autoConnect);
    }

    private void buildUI() {
        JPanel mainPanel = new JPanel(new BorderLayout(8, 8));
        mainPanel.setBackground(new Color(0x1a1d24));
        mainPanel.setBorder(new EmptyBorder(8, 8, 8, 8));

        // 상단: 연결 정보
        mainPanel.add(buildTopPanel(), BorderLayout.NORTH);

        // 중앙: 탭 + 팀 슬롯
        mainPanel.add(buildCenterPanel(), BorderLayout.CENTER);

        // 우측: 채팅
        mainPanel.add(buildChatPanel(), BorderLayout.EAST);

        setContentPane(mainPanel);
    }

    private JPanel buildTopPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        panel.setOpaque(false);

        hostLabel.setForeground(Color.WHITE);
        portLabel.setForeground(Color.WHITE);

        hostField.setPreferredSize(new Dimension(150, 30));
        portField.setPreferredSize(new Dimension(80, 30));

        panel.add(hostLabel);
        panel.add(hostField);
        panel.add(portLabel);
        panel.add(portField);

        return panel;
    }

    private JPanel buildCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setOpaque(false);

        // 탭 구성
        tabbedPane.setBackground(new Color(0x2a2f38));
        tabbedPane.setForeground(Color.WHITE);
        tabbedPane.addTab("Map Info", mapInfoPanel);
        tabbedPane.addTab("Character Select", charSelectPanel);

        panel.add(tabbedPane, BorderLayout.NORTH);

        // 팀 선택 + 슬롯 + READY
        panel.add(buildTeamPanel(), BorderLayout.CENTER);

        return panel;
    }

    private JPanel buildTeamPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setOpaque(false);

        // 팀 선택 버튼
        JPanel teamBtnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 8));
        teamBtnPanel.setOpaque(false);

        redTeamBtn.setPreferredSize(new Dimension(150, 40));
        redTeamBtn.setBackground(new Color(0xd32f2f));
        redTeamBtn.setForeground(Color.WHITE);
        redTeamBtn.setFont(new Font("Arial", Font.BOLD, 14));
        redTeamBtn.setBorder(BorderFactory.createLineBorder(new Color(0xffd700), 2));
        redTeamBtn.addActionListener(e -> selectTeam(0));

        blueTeamBtn.setPreferredSize(new Dimension(150, 40));
        blueTeamBtn.setBackground(new Color(0x1976d2));
        blueTeamBtn.setForeground(Color.WHITE);
        blueTeamBtn.setFont(new Font("Arial", Font.BOLD, 14));
        blueTeamBtn.addActionListener(e -> selectTeam(1));

        teamBtnPanel.add(redTeamBtn);
        teamBtnPanel.add(blueTeamBtn);

        panel.add(teamBtnPanel, BorderLayout.NORTH);

        // 팀 슬롯
        JPanel slotsPanel = new JPanel(new GridLayout(1, 2, 16, 0));
        slotsPanel.setOpaque(false);

        slotsPanel.add(buildTeamSlotsPanel("RED TEAM", redSlots, new Color(0x8b0000)));
        slotsPanel.add(buildTeamSlotsPanel("BLUE TEAM", blueSlots, new Color(0x000080)));

        panel.add(slotsPanel, BorderLayout.CENTER);

        // READY 버튼
        JPanel readyPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 12));
        readyPanel.setOpaque(false);

        readyBtn.setPreferredSize(new Dimension(200, 50));
        readyBtn.setBackground(new Color(0x4caf50));
        readyBtn.setForeground(Color.WHITE);
        readyBtn.setFont(new Font("Arial", Font.BOLD, 18));
        readyBtn.addActionListener(e -> toggleReady());

        readyPanel.add(readyBtn);
        panel.add(readyPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel buildTeamSlotsPanel(String teamName, JLabel[] slots, Color borderColor) {
        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setBackground(new Color(0x2a2f38));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(borderColor, 3),
            new EmptyBorder(8, 8, 8, 8)
        ));

        JLabel title = new JLabel(teamName, SwingConstants.CENTER);
        title.setForeground(borderColor.brighter());
        title.setFont(new Font("Arial", Font.BOLD, 16));
        panel.add(title, BorderLayout.NORTH);

        JPanel slotsGrid = new JPanel(new GridLayout(5, 1, 4, 4));
        slotsGrid.setOpaque(false);

        for (int i = 0; i < 5; i++) {
            JLabel slot = new JLabel("Empty", SwingConstants.CENTER);
            slot.setPreferredSize(new Dimension(0, 40));
            slot.setOpaque(true);
            slot.setBackground(new Color(0x3a3f47));
            slot.setForeground(new Color(0x999999));
            slot.setFont(new Font("Arial", Font.PLAIN, 14));
            slot.setBorder(BorderFactory.createLineBorder(new Color(0x555555)));
            slots[i] = slot;
            slotsGrid.add(slot);
        }

        panel.add(slotsGrid, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildChatPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setPreferredSize(new Dimension(360, 0));
        panel.setOpaque(false);

        JLabel chatTitle = new JLabel("Chat", SwingConstants.CENTER);
        chatTitle.setForeground(Color.WHITE);
        chatTitle.setFont(new Font("Arial", Font.BOLD, 14));
        chatTitle.setBorder(new EmptyBorder(4, 0, 4, 0));

        panel.add(chatTitle, BorderLayout.NORTH);
        panel.add(chatPanel, BorderLayout.CENTER);

        // 채팅 전송 콜백 설정
        chatPanel.setOnSendChat(msg -> {
            try {
                controller.sendChat(msg);
                chatPanel.appendMyMessage(msg);
            } catch (Exception ex) {
                chatPanel.appendSystemMessage("Failed to send: " + ex.getMessage());
            }
        });

        return panel;
    }

    private void selectTeam(int team) {
        selectedTeam = team;
        if (team == 0) {
            redTeamBtn.setBorder(BorderFactory.createLineBorder(new Color(0xffd700), 2));
            blueTeamBtn.setBorder(null);
        } else {
            blueTeamBtn.setBorder(BorderFactory.createLineBorder(new Color(0xffd700), 2));
            redTeamBtn.setBorder(null);
        }
    }

    private void toggleReady() {
        isReady = !isReady;
        readyBtn.setBackground(isReady ? new Color(0x9e9e9e) : new Color(0x4caf50));
        readyBtn.setText(isReady ? "READY ✓" : "READY");

        try {
            controller.sendReadyToggle(isReady);
        } catch (Exception ex) {
            chatPanel.appendSystemMessage("Failed to send ready: " + ex.getMessage());
        }
    }

    private void autoConnect() {
        try {
            String host = hostField.getText().trim();
            int port = Integer.parseInt(portField.getText().replace(",", "").trim());
            controller.connect(host, port, 3000);
            controller.attachUi(this);
            chatPanel.appendSystemMessage("서버 연결 중...");
        } catch (Exception ex) {
            chatPanel.appendSystemMessage("연결 실패: " + ex.getMessage());
        }
    }

    private void closeLobby() {
        try {
            controller.uninstallUi();
            controller.disconnect();
            controller.close();
        } catch (Exception ignore) {}

        dispose();
    }

    // ClientController.Ui 구현
    @Override
    public void onChat(String text) {
        SwingUtilities.invokeLater(() -> chatPanel.appendOtherMessage(text));
    }

    @Override
    public void onWelcome(Protocol.Welcome welcome) {
        SwingUtilities.invokeLater(() -> {
            chatPanel.appendSystemMessage("WELCOME id=" + welcome.myId + 
                " world=" + welcome.worldW + "x" + welcome.worldH +
                (welcome.mapId >= 0 ? (" map=" + welcome.mapId) : "") +
                (welcome.team >= 0 ? (" team=" + welcome.team) : "") +
                (welcome.character >= 0 ? (" char=" + welcome.character) : ""));
        });
    }

    @Override
    public void onPhaseUpdate(int phaseCode) {
        SwingUtilities.invokeLater(() -> {
            chatPanel.appendSystemMessage("PHASE=" + phaseCode);
            // TODO: Phase에 따라 UI 변경
        });
    }

    @Override
    public void onCountdown(int seconds) {
        SwingUtilities.invokeLater(() -> {
            chatPanel.appendSystemMessage("COUNTDOWN=" + seconds);
        });
    }

    @Override
    public void onRoundResult(Protocol.RoundResult rr) {
        SwingUtilities.invokeLater(() -> {
            chatPanel.appendSystemMessage("ROUND winner=" + rr.winnerTeam + 
                " score " + rr.blueRounds + ":" + rr.redRounds + 
                (rr.matchEnded ? " GAME OVER" : ""));
        });
    }

    @Override
    public void onReadyStatus(int ready, int total) {
        SwingUtilities.invokeLater(() -> {
            chatPanel.appendSystemMessage("READY_STATUS " + ready + "/" + total);
        });
    }

    @Override
    public void onDisconnected(String message) {
        SwingUtilities.invokeLater(() -> {
            chatPanel.appendSystemMessage("Disconnected: " + message);
            JOptionPane.showMessageDialog(this, "서버 연결이 끊어졌습니다.", "알림", JOptionPane.WARNING_MESSAGE);
        });
    }
}
