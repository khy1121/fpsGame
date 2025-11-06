package com.fpsgame.client;

import com.fpsgame.client.ui.LobbyUiInstaller;
import com.fpsgame.client.ui.PhaseUiInstaller;

import javax.swing.*;
import java.util.Objects;

/**
 * ClientUiBootstrap
 * ------------------------------------------------------------
 * 클라이언트 UI 요소들을 한 번에 장착/해제하는 통합 부트스트랩.
 *
 * 포함 구성:
 *  - PhaseUiInstaller : 상단 HUD + 중앙 메시지 오버레이
 *  - LobbyUiInstaller : 로비의 READY 리스트 + READY 버튼 (EAST 기본)
 *
 * 사용 예시:
 *   JFrame frame = gameFrame;
 *   NetClient netClient = ...; // 연결된 인스턴스
 *   ClientUiBootstrap ui = ClientUiBootstrap.attach(frame, netClient);
 *
 *   // 프레임 종료/화면 전환 시
 *   ui.detach();
 */
public final class ClientUiBootstrap {

    private final JFrame frame;
    private final Object netClient;

    private PhaseUiInstaller phaseUi;
    private LobbyUiInstaller lobbyUi;

    private java.awt.event.WindowListener disposer;

    private ClientUiBootstrap(JFrame frame, Object netClient) {
        this.frame = Objects.requireNonNull(frame, "frame");
        this.netClient = Objects.requireNonNull(netClient, "netClient");
    }

    /** HUD/오버레이 + 로비 READY UI를 모두 장착하고, 프레임 종료 시 자동 해제한다. */
    public static ClientUiBootstrap attach(JFrame frame, Object netClient) {
        ClientUiBootstrap b = new ClientUiBootstrap(frame, netClient);
        b.install();
        b.hookDispose();
        return b;
    }

    /** 수동 해제(중복 호출 안전) */
    public synchronized void detach() {
        try {
            if (lobbyUi != null) {
                lobbyUi.uninstall();
                lobbyUi = null;
            }
        } catch (Throwable ignore) {}

        try {
            if (phaseUi != null) {
                phaseUi.uninstall();
                phaseUi = null;
            }
        } catch (Throwable ignore) {}

        if (disposer != null) {
            try { frame.removeWindowListener(disposer); } catch (Throwable ignore) {}
            disposer = null;
        }
    }

    // ---------------- 내부 구현 ----------------

    private void install() {
        // 1) 상단 HUD + 중앙 메시지
        this.phaseUi = com.fpsgame.client.ui.PhaseUiInstaller.installTo(frame, (com.fpsgame.client.NetClient) netClient);

        // 2) 로비 READY 패널(EAST)
        this.lobbyUi = LobbyUiInstaller.installTo(frame, netClient, java.awt.BorderLayout.EAST);
    }

    private void hookDispose() {
        this.disposer = new java.awt.event.WindowAdapter() {
            @Override public void windowClosing(java.awt.event.WindowEvent e) { detach(); }
            @Override public void windowClosed (java.awt.event.WindowEvent e) { detach(); }
        };
        frame.addWindowListener(disposer);
    }

    // 선택적 접근자
    public JFrame getFrame() { return frame; }
    public Object getNetClient() { return netClient; }
    public PhaseUiInstaller getPhaseUi() { return phaseUi; }
    public LobbyUiInstaller getLobbyUi() { return lobbyUi; }
}
