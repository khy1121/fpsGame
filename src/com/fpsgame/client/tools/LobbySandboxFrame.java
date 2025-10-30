package com.fpsgame.client.tools;

import com.fpsgame.client.model.ClientPhaseBus;
import com.fpsgame.client.model.PhaseEventLogger;
import com.fpsgame.client.ui.*;
import java.awt.*;
import java.util.List;
import javax.swing.*;

/**
 * LobbySandboxFrame
 * ------------------------------------------------------------
 * 서버 없이도 로비 플로우 UI를 빠르게 검증하기 위한 개발용 샌드박스.
 *
 * 구성
 * - 상단: PhaseHudPanel (페이즈/스코어/생존/카운트다운 표시)
 * - 중앙: LobbyCenterCoordinator (맵 투표 / 캐릭터 선택 스위치)
 * - 우측: LobbyReadyPanel (플레이어 READY 리스트 + READY 버튼)
 * - 하단: Dev 패널(모의 이벤트 버튼들)
 *
 * 목적
 * - ClientPhaseBus 이벤트만으로 전체 UI 바인딩/표시가 올바르게 동작하는지 검증.
 * - 서버 연동 전에 화면/바인더 배선 문제를 조기에 발견.
 *
 * 실행:
 *   java com.fpsgame.client.tools.LobbySandboxFrame
 */
public final class LobbySandboxFrame extends JFrame {

    private final ClientPhaseBus bus = ClientPhaseBus.get();
    // 네트워크 없이도 빌드되도록 Object로 더미 보관(송신 유틸은 리플렉션 기반).
    private final Object dummyNetClient = new Object();

    private final PhaseHudPanel hud = new PhaseHudPanel();
    private final LobbyCenterCoordinator center;
    private final LobbyReadyPanel ready = new LobbyReadyPanel(dummyNetClient);

    public LobbySandboxFrame() {
        super("Lobby Sandbox");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        // 이벤트 로거 부착(하단 textarea 없이 콘솔 출력)
        PhaseEventLogger.attachConsole();

        // 상단 HUD
        add(hud, BorderLayout.NORTH);

        // 중앙 스택(맵 투표/캐릭터 선택)을 초기화하고 설정
        center = createCenterCoordinator();

        // 우측 READY 패널 추가
        add(ready, BorderLayout.EAST);

        // 하단 개발 버튼들
        add(devPanel(), BorderLayout.SOUTH);

        // 창 크기 및 위치 설정
        setSize(1100, 720);
        setLocationRelativeTo(null);
    }

    private LobbyCenterCoordinator createCenterCoordinator() {
        // Create and install center coordinator
        LobbyCenterCoordinator coordinator = LobbyCenterCoordinator.installTo(this, dummyNetClient);
        // Initialize with initial state
        resetState();
        return coordinator;
    }

    private JComponent devPanel() {
        JPanel p = new JPanel(new GridLayout(2, 1));
        p.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));

        JButton btnLobby     = new JButton("PHASE: LOBBY");
        JButton btnVote      = new JButton("PHASE: VOTE");
        JButton btnSelect    = new JButton("PHASE: SELECT");
        JButton btnCount3    = new JButton("COUNTDOWN 3→1");
        JButton btnRunning   = new JButton("PHASE: RUNNING");
        JButton btnResultR   = new JButton("RESULT: RED");
        JButton btnResultB   = new JButton("RESULT: BLUE");
        JButton btnReadyOn   = new JButton("READY id=1001 ON");
        JButton btnReadyOff  = new JButton("READY id=1001 OFF");

        btnLobby.addActionListener(e ->
                bus.publishPhase(new ClientPhaseBus.PhaseState("LOBBY", 0, 0, 0, 5, 5, -1)));

        btnVote.addActionListener(e ->
                bus.publishPhase(new ClientPhaseBus.PhaseState("VOTE", 0, 0, 0, 5, 5, 15)));

        btnSelect.addActionListener(e ->
                bus.publishPhase(new ClientPhaseBus.PhaseState("SELECT", 0, 0, 0, 5, 5, 10)));

        btnCount3.addActionListener(e -> new Thread(() -> {
            try {
                bus.publishCountdown(3); Thread.sleep(350);
                bus.publishCountdown(2); Thread.sleep(350);
                bus.publishCountdown(1); Thread.sleep(350);
                bus.publishCountdown(0);
            } catch (InterruptedException ignored) {}
        }).start());

        btnRunning.addActionListener(e ->
                bus.publishPhase(new ClientPhaseBus.PhaseState("RUNNING", 1, 0, 0, 5, 5, 90)));

        btnResultR.addActionListener(e ->
                bus.publishRoundResult(new ClientPhaseBus.RoundResult("RED", "TEAM_WIPE")));

        btnResultB.addActionListener(e ->
                bus.publishRoundResult(new ClientPhaseBus.RoundResult("BLUE", "TIMEOUT")));

        btnReadyOn.addActionListener(e -> bus.publishReadyToggle(1001, true));
        btnReadyOff.addActionListener(e -> bus.publishReadyToggle(1001, false));

        row1.add(btnLobby);
        row1.add(btnVote);
        row1.add(btnSelect);
        row1.add(btnCount3);

        row2.add(btnRunning);
        row2.add(btnResultR);
        row2.add(btnResultB);
        row2.add(btnReadyOn);
        row2.add(btnReadyOff);

        p.add(row1);
        p.add(row2);
        return p;
    }

    // ---------------- main ----------------
    // Reset to initial state
    public void resetState() {
        SwingUtilities.invokeLater(() -> {
            center.updateMaps(List.of("terminal", "neonCity", "ForestOutpost"));
            center.updateCharacters(List.of(
                    "Sage","Piper","Technician","General","Bulldog",
                    "Wildcat","Raven","Ghost","Skull","Steam"
            ));
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LobbySandboxFrame().setVisible(true));
    }
}
