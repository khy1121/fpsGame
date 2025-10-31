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
    private GamePanel gamePanel; // GAME 카드 렌더러
    
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

    // 카드 전환용 상수 및 컨테이너
    private static final String CARD_LOBBY = "CARD_LOBBY";
    private static final String CARD_VOTE  = "CARD_VOTE";
    private static final String CARD_GAME  = "CARD_GAME";
    private JPanel centerCards;

    private void buildUI() {
        JPanel mainPanel = new JPanel(new BorderLayout(12, 12));
        mainPanel.setBackground(new Color(0x1a1d24));
        mainPanel.setBorder(new EmptyBorder(12, 12, 12, 12));

        // 상단: 연결 정보
        mainPanel.add(buildTopPanel(), BorderLayout.NORTH);

    // 중앙: 카드 레이아웃(LOBBY/VOTE/GAME)
    mainPanel.add(buildCardsPanel(), BorderLayout.CENTER);

        // 우측: 채팅
        mainPanel.add(buildChatPanel(), BorderLayout.EAST);

        setContentPane(mainPanel);
    }

    private JPanel buildCardsPanel() {
        centerCards = new JPanel(new java.awt.CardLayout());
        centerCards.setOpaque(false);
        centerCards.add(buildCenterPanel(), CARD_LOBBY);
        centerCards.add(buildVotePanel(), CARD_VOTE);
        centerCards.add(buildGamePanel(), CARD_GAME);
        showCenterCard(CARD_LOBBY);
        return centerCards;
    }

    private void showCenterCard(String name) {
        java.awt.CardLayout cl = (java.awt.CardLayout) centerCards.getLayout();
        cl.show(centerCards, name);
    }

    private JPanel buildVotePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        JLabel lbl = new JLabel("🗳️ Voting in progress... Click a map on the left.", SwingConstants.CENTER);
        lbl.setForeground(Color.WHITE);
        lbl.setFont(new Font("맑은 고딕", Font.BOLD, 18));
        panel.add(lbl, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildGamePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        gamePanel = new GamePanel();
        // Bind input sender to controller
        gamePanel.setInputSender((mask, aim) -> {
            try {
                controller.sendInputMask(mask, aim);
            } catch (Exception ex) {
                // ignore transient send errors; connection state will handle
            }
        });
        // Bind action sender for attacks/skills
        gamePanel.setActionSender(actionType -> {
            try {
                controller.sendAction(actionType);
            } catch (Exception ex) {
                // ignore transient send errors
            }
        });
        panel.add(gamePanel, BorderLayout.CENTER);
        return panel;
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
        
        // PHASE 5: 캐릭터 선택 콜백 설정
        charSelectPanel.onSendSelection(charId -> {
            try {
                int charIdInt = charIdToInt(charId);
                controller.sendCharacterSelection(charIdInt);
                chatPanel.appendSystemMessage("캐릭터 선택: " + charId + " (ID: " + charIdInt + ")");
            } catch (Exception ex) {
                chatPanel.appendSystemMessage("캐릭터 선택 실패: " + ex.getMessage());
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
        toggleChatBtn.setForeground(Color.BLACK); // 검정 글씨
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
                // 팀 채팅 모드이면 [TEAM] 접두사 추가
                String prefix = isTeamChatMode ? "[TEAM]" : "[ALL]";
                String formattedMsg = prefix + "[" + nickname + "] : " + msg;
                controller.sendChat(formattedMsg);
                // 서버에서 에코백될 때 표시되도록 여기서는 표시하지 않음
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
        
        // UI 상태만 업데이트 (버튼 색상/텍스트)
        if (isReady) {
            readyBtn.setBackground(Color.RED);
            readyBtn.setText("CANCEL");
        } else {
            readyBtn.setBackground(new Color(0x00ff00));
            readyBtn.setText("READY");
        }

        // 서버에 ready 상태 전송
        // → 서버가 broadcastReadyStatus() 호출
        // → 모든 클라이언트가 onReadyStatusWithPlayers() 수신
        // → 팀 슬롯 UI가 서버 데이터로 자동 동기화됨
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
    
    /**
     * 캐릭터 ID 문자열을 정수로 변환 (PHASE 5)
     * @param charId 캐릭터 ID 문자열 (예: "Sage", "Piper", ...)
     * @return 캐릭터 ID 정수 (0-9)
     */
    private int charIdToInt(String charId) {
        return switch (charId) {
            case "Sage" -> 0;
            case "Piper" -> 1;
            case "Technician" -> 2;
            case "General" -> 3;
            case "Bulldog" -> 4;
            case "Wildcat" -> 5;
            case "Raven" -> 6;
            case "Ghost" -> 7;
            case "Skull" -> 8;
            case "Steam" -> 9;
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
                chatPanel.appendMessage(text);
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
            if (gamePanel != null) {
                gamePanel.setWorldSize(welcome.worldW, welcome.worldH);
                gamePanel.setMyId(welcome.myId);
                if (welcome.mapId >= 0) {
                    gamePanel.setMapId(welcome.mapId);
                }
            }
        });
    }

    @Override
    public void onPhaseUpdate(int phaseCode) {
        SwingUtilities.invokeLater(() -> {
            // PHASE 6: Phase에 따라 탭 자동 전환 및 UI 업데이트
            switch (phaseCode) {
                case 0: // LOBBY
                    showCenterCard(CARD_LOBBY);
                    tabbedPane.setSelectedIndex(0); // Map Info 탭
                    chatPanel.appendSystemMessage("=============================");
                    chatPanel.appendSystemMessage("  📋 로비 단계");
                    chatPanel.appendSystemMessage("  팀을 선택하고 READY 버튼을 눌러주세요");
                    chatPanel.appendSystemMessage("=============================");
                    break;
                    
                case 1: // VOTE
                    showCenterCard(CARD_VOTE);
                    tabbedPane.setSelectedIndex(0); // Map Info 탭
                    chatPanel.appendSystemMessage("=============================");
                    chatPanel.appendSystemMessage("  🗳️ 맵 투표 시작!");
                    chatPanel.appendSystemMessage("  원하는 맵을 클릭하여 투표하세요");
                    chatPanel.appendSystemMessage("=============================");
                    break;
                    
                case 2: // CHARACTER_SELECT
                    showCenterCard(CARD_LOBBY); // 캐릭터 선택은 기존 로비 탭에서 처리
                    tabbedPane.setSelectedIndex(1); // Character Select 탭
                    chatPanel.appendSystemMessage("=============================");
                    chatPanel.appendSystemMessage("  👤 캐릭터 선택 시작!");
                    chatPanel.appendSystemMessage("  캐릭터를 선택하고 Choose 버튼을 눌러주세요");
                    chatPanel.appendSystemMessage("=============================");
                    break;
                    
                case 3: // COUNTDOWN
                    showCenterCard(CARD_GAME); // 게임 화면 준비
                    if (gamePanel != null) gamePanel.requestFocusInWindow();
                    chatPanel.appendSystemMessage("=============================");
                    chatPanel.appendSystemMessage("  ⏰ 게임 곧 시작!");
                    chatPanel.appendSystemMessage("  준비하세요...");
                    chatPanel.appendSystemMessage("=============================");
                    break;
                    
                case 4: // PLAYING
                    showCenterCard(CARD_GAME);
                    if (gamePanel != null) gamePanel.requestFocusInWindow();
                    chatPanel.appendSystemMessage("=============================");
                    chatPanel.appendSystemMessage("  🎮 게임 시작!");
                    chatPanel.appendSystemMessage("=============================");
                    break;
                    
                case 5: // ROUND_END
                    showCenterCard(CARD_LOBBY);
                    chatPanel.appendSystemMessage("=============================");
                    chatPanel.appendSystemMessage("  🏁 라운드 종료");
                    chatPanel.appendSystemMessage("=============================");
                    break;
                    
                default:
                    chatPanel.appendSystemMessage("PHASE=" + phaseCode);
            }
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
            // 기본 메시지 출력
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
    
    /**
     * ClientController.Ui 확장: 플레이어 리스트 포함 READY_STATUS 수신 시 팀 슬롯을 동기화한다.
     */
    @Override
    public void onReadyStatusDetailed(Protocol.ReadyStatus status) {
        // 기존 구현과의 호환을 위해 세부 처리 메서드로 위임
        onReadyStatusWithPlayers(status);
    }
    
    /**
     * READY_STATUS 새 버전 - 플레이어 리스트 포함
     * 서버에서 전체 플레이어 정보를 받아서 팀 슬롯 UI를 동기화
     */
    public void onReadyStatusWithPlayers(Protocol.ReadyStatus status) {
        SwingUtilities.invokeLater(() -> {
            // 디버그 로그
            System.out.println("[LobbyFrame] onReadyStatusWithPlayers called:");
            System.out.println("  ready=" + status.ready + " total=" + status.total);
            System.out.println("  players.size=" + (status.players != null ? status.players.size() : "null"));
            if (status.players != null) {
                for (Protocol.PlayerInfo p : status.players) {
                    System.out.println("    - sid=" + p.sessionId + " nick=" + p.nickname + 
                                     " team=" + p.team + " char=" + p.character + " ready=" + p.ready);
                }
            }
            
            // 1. 모든 슬롯 초기화
            for (int i = 0; i < 5; i++) {
                redSlots[i].setText("Empty");
                redSlots[i].setForeground(Color.GRAY);
                blueSlots[i].setText("Empty");
                blueSlots[i].setForeground(Color.GRAY);
            }
            
            // 2. 플레이어 리스트로 슬롯 채우기
            int[] redCount = {0};
            int[] blueCount = {0};
            
            if (status.players != null) {
                for (Protocol.PlayerInfo player : status.players) {
                    String displayName = player.nickname + (player.ready ? " ✓" : "");
                    Color nameColor = player.ready ? Color.GREEN : Color.WHITE;
                    
                    if (player.team == 0 && redCount[0] < 5) { // RED 팀
                        redSlots[redCount[0]].setText(displayName);
                        redSlots[redCount[0]].setForeground(nameColor);
                        redCount[0]++;
                    } else if (player.team == 1 && blueCount[0] < 5) { // BLUE 팀
                        blueSlots[blueCount[0]].setText(displayName);
                        blueSlots[blueCount[0]].setForeground(nameColor);
                        blueCount[0]++;
                    }
                }
            }
            
            // 3. 채팅 메시지
            chatPanel.appendSystemMessage("=============================");
            chatPanel.appendSystemMessage("  준비 완료: " + status.ready + " / " + status.total + " 명");
            chatPanel.appendSystemMessage("  RED: " + redCount[0] + "명 | BLUE: " + blueCount[0] + "명");
            if (status.ready == status.total && status.total > 0) {
                chatPanel.appendSystemMessage("  🎮 모든 플레이어 준비 완료!");
            } else if (status.ready > 0) {
                chatPanel.appendSystemMessage("  ⏳ " + (status.total - status.ready) + "명 대기 중...");
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

    // === Gameplay snapshot/projectile updates ===
    @Override
    public void onSnapshotV2(java.util.List<com.fpsgame.common.SnapshotV2.Entry> list) {
        if (gamePanel == null) return;
        SwingUtilities.invokeLater(() -> gamePanel.applySnapshot(list));
    }

    @Override
    public void onProjectilesV2(java.util.List<com.fpsgame.common.ProjectilesV2.Entry> list) {
        if (gamePanel == null) return;
        SwingUtilities.invokeLater(() -> gamePanel.applyProjectiles(list));
    }
}
