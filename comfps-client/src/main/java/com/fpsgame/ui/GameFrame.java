package com.fpsgame.client.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;

import com.fpsgame.client.ClientController;
import com.fpsgame.common.GameEnums;
import com.fpsgame.common.Protocol;

/**
 * 메인 게임 창 - 전반적인 게임 UI와 컨트롤을 처리합니다.
 * 
 * 특징:
 * - {@link ClientController.Ui}를 구현하는 주요 게임 UI 컨테이너
 * - HUD 컴포넌트와 네트워크 상태 표시를 포함
 * - 입력 처리와 창 상태 관리
 * 
 * 컨트롤:
 * - 네트워크: 호스트/포트 + 연결/연결해제 버튼
 * - 채팅: 메시지 기록이 있는 통합 채팅창
 * - HUD: 플레이어 상태, 팀 정보, RTT 표시
 */
public class GameFrame extends JFrame implements ClientController.Ui {

    // 네트워크 컨트롤러(콜백은 EDT에서 처리)
    private final ClientController controller = new ClientController(this, true);

    // 호스트/포트 설정
    private final JTextField hostField = new JTextField("127.0.0.1", 12);
    private final JSpinner portSpinner = new JSpinner(new SpinnerNumberModel(7777, 1, 65535, 1));
    private final JButton connectBtn = new JButton("Connect");
    private final JButton disconnectBtn = new JButton("Disconnect");

    // 게임 화면 레이아웃 / 메인 HUD
    // 중앙: 좌측 채팅 임베드, 우측 HUD
    private final ChatWindow chatWindowEmbed = new ChatWindow(); // 채팅창 임베드
    private final HudPanel hud = new HudPanel();

    // 하단: READY/팀/캐릭터/맵 선택
    private final JToggleButton readyToggle = new JToggleButton("READY");
    private final JComboBox<String> teamCombo = new JComboBox<>(new String[]{"Team 0","Team 1","Team 2"});
    private final JComboBox<String> charCombo = new JComboBox<>(new String[]{"Char 0","Char 1","Char 2","Char 3"});
    private final JComboBox<String> mapCombo  = new JComboBox<>(new String[]{"Map 0","Map 1","Map 2"});
    private final JButton selectBtn = new JButton("Select");
    private final JButton voteBtn = new JButton("Vote");
    private final JButton pingBtn = new JButton("Ping");

    public GameFrame() {
        super("FPS Client - GameFrame");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setSize(1600, 900);
        setMinimumSize(new Dimension(1280, 720));
        setLocationRelativeTo(null);
        ((JComponent)getContentPane()).setBorder(new EmptyBorder(8,8,8,8));
        setLayout(new BorderLayout(8,8));

        // 상단 패널: 네트워크 연결 컨트롤
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        ((JSpinner.DefaultEditor)portSpinner.getEditor()).getTextField().setColumns(5);
        top.add(new JLabel("Host")); top.add(hostField);
        top.add(new JLabel("Port")); top.add(portSpinner);
        top.add(connectBtn); top.add(disconnectBtn); top.add(pingBtn);
        add(top, BorderLayout.NORTH);

        // 중앙 분할 패널: 메인 게임/HUD 영역(좌) / 채팅창(우)
        JSplitPane center = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        center.setResizeWeight(0.8);  // 좌측(게임/HUD)에 80%, 우측(채팅)에 20%
        center.setOneTouchExpandable(true);
        center.setContinuousLayout(true);
        
        // HUD를 메인 영역으로 설정 (게임 화면 + 정보)
        hud.setMinimumSize(new Dimension(600, 400));
        hud.setPreferredSize(new Dimension(1000, 600));
        
        // 채팅창을 오른쪽에 배치 (최소 크기 설정)
        JComponent chatPanel = wrap(chatWindowEmbed.getContentPane());
        chatPanel.setMinimumSize(new Dimension(250, 400));
        chatPanel.setPreferredSize(new Dimension(320, 600));
        
        center.setLeftComponent(hud);
        center.setRightComponent(chatPanel);
        add(center, BorderLayout.CENTER);

        // 하단 패널: READY/팀/캐릭터/맵 선택
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 4));
        bottom.add(readyToggle);
        bottom.add(new JLabel("Team")); bottom.add(teamCombo);
        bottom.add(new JLabel("Char")); bottom.add(charCombo);
        bottom.add(selectBtn);
        bottom.add(new JLabel("Map")); bottom.add(mapCombo);
        bottom.add(voteBtn);
        add(bottom, BorderLayout.SOUTH);

        // 이벤트 리스너 등록
        connectBtn.addActionListener(e -> doConnect());
        disconnectBtn.addActionListener(e -> doDisconnect());
        pingBtn.addActionListener(e -> safe(() -> controller.sendPingOnce()));

        readyToggle.addActionListener(e -> safe(() -> controller.sendReadyToggle(readyToggle.isSelected())));
        selectBtn.addActionListener(e -> safe(() -> controller.sendSetSelection(teamCombo.getSelectedIndex(), charCombo.getSelectedIndex())));
        voteBtn.addActionListener(e -> safe(() -> controller.sendMapVote(mapCombo.getSelectedIndex())));

        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { doDisconnect(); }
        });

        updateUiState(false);
        // 맵 콤보를 실제 Enum 이름으로 갱신
        try { updateMapComboFromEnums(); } catch (Throwable ignore) {}
    }

    /** GameEnums.MapId 값을 기반으로 맵 콤보 내용을 갱신 */
    private void updateMapComboFromEnums() {
        DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
        GameEnums.MapId[] maps = GameEnums.MapId.values();
        for (GameEnums.MapId m : maps) {
            model.addElement(m.displayName());
        }
        mapCombo.setModel(model);
        mapCombo.setSelectedIndex(0);
    }

    private static JComponent wrap(Container c) {
        JPanel p = new JPanel(new BorderLayout());
        p.add((Component)c, BorderLayout.CENTER);
        p.setBorder(BorderFactory.createTitledBorder("Chat"));
        return p;
    }

    // ================= 네트워크 연결 처리 =================

    private void doConnect() {
        String host = hostField.getText().trim();
        int port = (int) portSpinner.getValue();
        try {
            controller.connect(host, port, 3000);
            hud.log("connecting to " + host + ":" + port + " ...");
            updateUiState(true);
        } catch (Exception ex) {
            hud.log("connect failed: " + ex.getMessage());
            updateUiState(false);
        }
    }

    private void doDisconnect() {
        try { controller.disconnect(); } catch (Exception ignore) {}
        updateUiState(false);
    }

    private void updateUiState(boolean connected) {
        hostField.setEnabled(!connected);
        portSpinner.setEnabled(!connected);
        connectBtn.setEnabled(!connected);
        disconnectBtn.setEnabled(connected);
        readyToggle.setEnabled(connected);
        selectBtn.setEnabled(connected);
        voteBtn.setEnabled(connected);
        pingBtn.setEnabled(connected);
    }

    private static void safe(RunnableEx r) {
        try { r.run(); } catch (Exception e) {
            JOptionPane.showMessageDialog(null, e.getMessage(), "네트워크 오류", JOptionPane.ERROR_MESSAGE);
        }
    }
    @FunctionalInterface private interface RunnableEx { void run() throws Exception; }

    // ================= ClientController.Ui 콜백 구현 =================

    @Override public void onChat(String text) {
        // ChatWindow는 별도 UI이므로 여기서는 채팅 내용을 파싱하여 HUD에 반영
        try {
            if (text != null && text.contains("World=")) {
                java.util.regex.Pattern p = java.util.regex.Pattern.compile(".*?([0-9]+),\\s*World=([0-9]+)x([0-9]+).*");
                java.util.regex.Matcher m = p.matcher(text);
                if (m.matches()) {
                    int mapId = Integer.parseInt(m.group(1));
                    int w = Integer.parseInt(m.group(2));
                    int h = Integer.parseInt(m.group(3));
                    String mapName = "Unknown";
                    try {
                        com.fpsgame.common.GameEnums.MapId[] maps = com.fpsgame.common.GameEnums.MapId.values();
                        if (mapId >= 0 && mapId < maps.length) mapName = maps[mapId].displayName();
                    } catch (Throwable ignore) {}
                    hud.setWorldInfo(w, h);
                    hud.setMapInfo(mapName, mapId);
                    try { setTitle("FPS Client - " + mapName + " [" + w + "x" + h + "]"); } catch (Throwable ignore) {}
                    try { if (mapId >= 0 && mapId < mapCombo.getItemCount()) mapCombo.setSelectedIndex(mapId); } catch (Throwable ignore) {}
                }
            }
            if (text != null && text.startsWith("[READY]")) {
                java.util.regex.Pattern rp = java.util.regex.Pattern.compile(".*?ready=([0-9]+)\\s+total=([0-9]+).*");
                java.util.regex.Matcher rm = rp.matcher(text);
                if (rm.matches()) {
                    int ready = Integer.parseInt(rm.group(1));
                    int total = Integer.parseInt(rm.group(2));
                    hud.setReadyInfo(ready, total);
                }
            }
            // Optional ability/status tags: [STANCE] on|off, [SCOPE] on|off, [ULT] on|off [sec]
            if (text != null && text.startsWith("[STANCE]")) {
                boolean on = text.toLowerCase().contains("on");
                hud.setCoverStance(on);
            }
            if (text != null && text.startsWith("[SCOPE]")) {
                boolean on = text.toLowerCase().contains("on");
                hud.setScopeActive(on);
            }
            if (text != null && text.startsWith("[ULT]")) {
                boolean on = text.toLowerCase().contains("on");
                java.util.regex.Pattern up = java.util.regex.Pattern.compile(".*?(on|off)\\s*(\\d+)?");
                java.util.regex.Matcher um = up.matcher(text.toLowerCase());
                Integer remain = null;
                if (um.matches() && um.group(2) != null) {
                    try { remain = Integer.parseInt(um.group(2)); } catch (Exception ignore) {}
                }
                hud.setUltimateActive(on, remain);
            }
        } catch (Throwable ignore) {}
        hud.log("[chat] " + (text == null ? "" : text));
    }

    @Override public void onWelcome(Protocol.Welcome welcome) {
        String mapName = "Unknown";
        try {
            com.fpsgame.common.GameEnums.MapId[] maps = com.fpsgame.common.GameEnums.MapId.values();
            if (welcome.mapId >= 0 && welcome.mapId < maps.length) {
                mapName = maps[welcome.mapId].displayName();
            }
        } catch (Throwable ignore) {}
        // HUD 라벨에도 지속적으로 표기
        hud.setWorldInfo(welcome.worldW, welcome.worldH);
        hud.setMapInfo(mapName, welcome.mapId);
        // 콤보박스 선택 상태를 서버 값으로 동기화(안전 범위 체크)
        try {
            if (welcome.team >= 0 && welcome.team < teamCombo.getItemCount()) {
                teamCombo.setSelectedIndex(welcome.team);
            }
            if (welcome.character >= 0 && welcome.character < charCombo.getItemCount()) {
                charCombo.setSelectedIndex(welcome.character);
            }
            if (welcome.mapId >= 0 && welcome.mapId < mapCombo.getItemCount()) {
                mapCombo.setSelectedIndex(welcome.mapId);
            }
        } catch (Throwable ignore) {}
        hud.log("[welcome] id=" + welcome.myId + " team=" + welcome.team + " ch=" + welcome.character +
                " world=" + welcome.worldW + "x" + welcome.worldH + " map=" + mapName + "(" + welcome.mapId + ")");
        // 프레임 타이틀에도 반영
        try { setTitle("FPS Client - " + mapName + " [" + welcome.worldW + "x" + welcome.worldH + "]"); } catch (Throwable ignore) {}
    }

    @Override public void onPhaseUpdate(int phaseCode) {
        hud.updatePhase(phaseCode);
    }

    @Override public void onCountdown(int seconds) {
        hud.updateCountdown(seconds);
    }

    @Override public void onRoundResult(Protocol.RoundResult rr) {
        hud.updateRoundResult(rr);
    }

    @Override public void onReadyStatus(int ready, int total) {
        try { hud.setReadyInfo(ready, total); } catch (Throwable ignore) {}
    }

    @Override public void onDisconnected(String message) {
        hud.log("[disconnected] " + (message == null ? "" : message));
        updateUiState(false);
    }

    @Override public void onPingPong(long rttMillis) {
        hud.updateRtt(rttMillis);
    }

    // ================= 메인 진입점 =================

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new GameFrame().setVisible(true));
    }
}










