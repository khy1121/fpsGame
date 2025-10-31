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
    
    // 내 플레이어 ID (서버에서 받음)
    private int myId = -1;

    // 탭 패널
    private final JTabbedPane tabbedPane = new JTabbedPane();
    private final MapInfoPanel mapInfoPanel;
    private final CharacterSelectPanel charSelectPanel;
    
    // 레디 상태 및 팀 채팅 토글
    private boolean isReady = false;
    private boolean isTeamChatMode = false; // false=전체, true=팀

    // 팀 선택
    private final JButton redTeamBtn = new JButton("RED TEAM");
    private final JButton blueTeamBtn = new JButton("BLUE TEAM");
    private int selectedTeam = 0; // 0=RED, 1=BLUE

    // 팀 슬롯
    private final JLabel[] redSlots = new JLabel[5];
    private final JLabel[] blueSlots = new JLabel[5];

    // READY 버튼
    private final JButton readyBtn = new JButton("READY");

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
        setSize(1600, 900); // 화면 크기 증가
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
        JPanel mainPanel = new JPanel(new BorderLayout(12, 12));
        mainPanel.setBackground(new Color(0x1a1d24));
        mainPanel.setBorder(new EmptyBorder(12, 12, 12, 12));

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

        // 탭 구성 - 노란 배경에 검정 글씨
        tabbedPane.setBackground(new Color(0xffd700));
        tabbedPane.setForeground(Color.BLACK);
        tabbedPane.setFont(new Font("맑은 고딕", Font.BOLD, 14));
        tabbedPane.addTab("Map Info", mapInfoPanel);
        tabbedPane.addTab("Character Select", charSelectPanel);
        
        // PHASE 4: 맵 투표 콜백 설정
        mapInfoPanel.setOnMapSelected(mapId -> {
            try {
                int mapIdInt = mapIdToInt(mapId);
                controller.sendMapVote(mapIdInt);
                chatPanel.appendSystemMessage("맵 투표: " + mapId + " (ID: " + mapIdInt + ")");
            } catch (Exception ex) {
                chatPanel.appendSystemMessage("맵 투표 실패: " + ex.getMessage());
            }
        });

        panel.add(tabbedPane, BorderLayout.NORTH);

        // 팀 선택 + 슬롯 + READY
        panel.add(buildTeamPanel(), BorderLayout.CENTER);

        return panel;
    }

    private JPanel buildTeamPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setOpaque(false);

        // 팀 선택 버튼
        JPanel teamBtnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 12));
        teamBtnPanel.setOpaque(false);

        // RED 팀 버튼 - 빨강 배경, 검정 글씨
        redTeamBtn.setPreferredSize(new Dimension(180, 50));
        redTeamBtn.setBackground(Color.RED);
        redTeamBtn.setForeground(Color.BLACK);
        redTeamBtn.setFont(new Font("맑은 고딕", Font.BOLD, 16));
        redTeamBtn.setFocusPainted(false);
        redTeamBtn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0x8b0000), 2),
            new EmptyBorder(8, 16, 8, 16)
        ));
        redTeamBtn.addActionListener(e -> selectTeam(0));

        // BLUE 팀 버튼 - 파랑 배경, 검정 글씨
        blueTeamBtn.setPreferredSize(new Dimension(180, 50));
        blueTeamBtn.setBackground(Color.BLUE);
        blueTeamBtn.setForeground(Color.BLACK);
        blueTeamBtn.setFont(new Font("맑은 고딕", Font.BOLD, 16));
        blueTeamBtn.setFocusPainted(false);
        blueTeamBtn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0x000080), 2),
            new EmptyBorder(8, 16, 8, 16)
        ));
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
        JPanel readyPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 16));
        readyPanel.setOpaque(false);

        // READY 버튼 - 초록 배경, 검정 글씨
        readyBtn.setPreferredSize(new Dimension(240, 60));
        readyBtn.setBackground(new Color(0x00ff00)); // 초록
        readyBtn.setForeground(Color.BLACK);
        readyBtn.setFont(new Font("맑은 고딕", Font.BOLD, 20));
        readyBtn.setFocusPainted(false);
        readyBtn.setBorderPainted(false);
        readyBtn.addActionListener(e -> toggleReady());

        readyPanel.add(readyBtn);
        panel.add(readyPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel buildTeamSlotsPanel(String teamName, JLabel[] slots, Color borderColor) {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBackground(new Color(0x2a2f38));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(borderColor, 3),
            new EmptyBorder(12, 12, 12, 12)
        ));

        JLabel title = new JLabel(teamName, SwingConstants.CENTER);
        title.setForeground(borderColor.brighter());
        title.setFont(new Font("맑은 고딕", Font.BOLD, 18));
        panel.add(title, BorderLayout.NORTH);

        JPanel slotsGrid = new JPanel(new GridLayout(5, 1, 6, 6));
        slotsGrid.setOpaque(false);

        for (int i = 0; i < 5; i++) {
            JLabel slot = new JLabel("Empty", SwingConstants.CENTER);
            slot.setPreferredSize(new Dimension(0, 50));
            slot.setOpaque(true);
            slot.setBackground(new Color(0x3a3f47));
            slot.setForeground(new Color(0x999999));
            slot.setFont(new Font("맑은 고딕", Font.PLAIN, 15));
            slot.setBorder(BorderFactory.createLineBorder(new Color(0x555555), 2));
            slots[i] = slot;
            slotsGrid.add(slot);
        }

        panel.add(slotsGrid, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildChatPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setPreferredSize(new Dimension(400, 0));
        panel.setOpaque(false);

        // 상단: 제목 + 팀/전체 토글
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        
        JLabel chatTitle = new JLabel("Chat", SwingConstants.CENTER);
        chatTitle.setForeground(Color.WHITE);
        chatTitle.setFont(new Font("맑은 고딕", Font.BOLD, 16));
        
        JButton toggleChatBtn = new JButton("전체");
        toggleChatBtn.setFocusPainted(false);
        toggleChatBtn.setFont(new Font("맑은 고딕", Font.BOLD, 12));
        toggleChatBtn.setBackground(new Color(0x4caf50));
        toggleChatBtn.setForeground(Color.WHITE);
        toggleChatBtn.addActionListener(e -> {
            isTeamChatMode = !isTeamChatMode;
            toggleChatBtn.setText(isTeamChatMode ? "팀" : "전체");
            toggleChatBtn.setBackground(isTeamChatMode ? new Color(0xff9800) : new Color(0x4caf50));
        });
        
        headerPanel.add(chatTitle, BorderLayout.CENTER);
        headerPanel.add(toggleChatBtn, BorderLayout.EAST);

        panel.add(headerPanel, BorderLayout.NORTH);
        panel.add(chatPanel, BorderLayout.CENTER);

        // 채팅 전송 콜백 설정
        chatPanel.setOnSendChat(msg -> {
            try {
                // [닉네임] : 메시지 형식으로 전송
                String formattedMsg = "[" + nickname + "] : " + msg;
                controller.sendChat(formattedMsg);
            } catch (Exception ex) {
                chatPanel.appendSystemMessage("Failed to send: " + ex.getMessage());
            }
        });

        return panel;
    }

    private void selectTeam(int team) {
        // READY 상태일 때는 팀 변경 불가
        if (isReady) {
            chatPanel.appendSystemMessage("READY 상태에서는 팀 변경이 불가능합니다. CANCEL을 먼저 누르세요.");
            return;
        }
        
        selectedTeam = team;
        
        // 서버로 팀 선택 전송 (PHASE 3.2)
        try {
            controller.sendTeamSelection(team);
            chatPanel.appendSystemMessage("팀 선택: " + (team == 0 ? "RED" : "BLUE"));
        } catch (Exception ex) {
            chatPanel.appendSystemMessage("팀 선택 전송 실패: " + ex.getMessage());
        }
        
        if (team == 0) {
            // RED 선택: 금색 굵은 테두리, 빨강 배경 유지
            redTeamBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xffd700), 4),
                new EmptyBorder(8, 16, 8, 16)
            ));
            // BLUE 기본: 얇은 테두리
            blueTeamBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0x000080), 2),
                new EmptyBorder(8, 16, 8, 16)
            ));
        } else {
            // BLUE 선택: 금색 굵은 테두리, 파랑 배경 유지
            blueTeamBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xffd700), 4),
                new EmptyBorder(8, 16, 8, 16)
            ));
            // RED 기본: 얇은 테두리
            redTeamBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0x8b0000), 2),
                new EmptyBorder(8, 16, 8, 16)
            ));
        }
    }

    private void toggleReady() {
        if (selectedTeam < 0) {
            chatPanel.appendSystemMessage("팀을 먼저 선택하세요!");
            return;
        }
        
        isReady = !isReady;
        
        if (isReady) {
            // READY 상태: 빨강 배경으로 CANCEL 표시
            readyBtn.setBackground(Color.RED);
            readyBtn.setText("CANCEL");
            
            // 해당 팀 슬롯에 닉네임 추가
            JLabel[] teamSlots = (selectedTeam == 0) ? redSlots : blueSlots;
            for (int i = 0; i < teamSlots.length; i++) {
                if (teamSlots[i].getText().equals("Empty")) {
                    teamSlots[i].setText(nickname);
                    teamSlots[i].setForeground(Color.WHITE);
                    break;
                }
            }
        } else {
            // CANCEL 상태: 초록 배경으로 READY 표시
            readyBtn.setBackground(new Color(0x00ff00));
            readyBtn.setText("READY");
            
            // 해당 팀 슬롯에서 닉네임 제거
            JLabel[] teamSlots = (selectedTeam == 0) ? redSlots : blueSlots;
            for (int i = 0; i < teamSlots.length; i++) {
                if (teamSlots[i].getText().equals(nickname)) {
                    teamSlots[i].setText("Empty");
                    teamSlots[i].setForeground(new Color(0x999999));
                    break;
                }
            }
        }

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
            // controller.attachUi(this) 제거 - 레거시 UI 중복 방지
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
    
    /**
     * 맵 ID 문자열을 정수로 변환 (PHASE 4)
     * @param mapId 맵 ID 문자열 (예: "terminal", "neonCity", "forestOutpost")
     * @return 맵 ID 정수 (0, 1, 2)
     */
    private int mapIdToInt(String mapId) {
        return switch (mapId.toLowerCase()) {
            case "terminal" -> 0;
            case "neoncity" -> 1;
            case "forestoutpost" -> 2;
            default -> 0;
        };
    }

    // ClientController.Ui 구현
    @Override
    public void onChat(String text) {
        SwingUtilities.invokeLater(() -> {
            // [닉네임] : 메시지 형식으로 이미 포맷되어 옴
            // 내 닉네임으로 시작하면 노란색, 아니면 흰색
            if (text.startsWith("[" + nickname + "]")) {
                // 내 메시지: 노란색
                chatPanel.appendMyMessage(text);
            } else {
                // 다른 사람 메시지: 흰색
                chatPanel.appendOtherMessage(text);
            }
        });
    }

    @Override
    public void onWelcome(Protocol.Welcome welcome) {
        myId = welcome.myId; // 내 ID 저장
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
            // PHASE 3.1: 더 상세한 READY 상태 표시
            chatPanel.appendSystemMessage("=============================");
            chatPanel.appendSystemMessage("  준비 완료: " + ready + " / " + total + " 명");
            if (ready == total && total > 0) {
                chatPanel.appendSystemMessage("  🎮 모든 플레이어 준비 완료!");
            } else if (ready > 0) {
                chatPanel.appendSystemMessage("  ⏳ " + (total - ready) + "명 대기 중...");
            }
            chatPanel.appendSystemMessage("=============================");
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
