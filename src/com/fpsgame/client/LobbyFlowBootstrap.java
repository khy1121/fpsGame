package com.fpsgame.client;

import com.fpsgame.client.model.ClientPhaseModel;
import com.fpsgame.client.ui.*;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Objects;

/**
 * 로비 흐름 부트스트랩
 * - 로비 화면의 Ready/맵 투표/캐릭터 선택을 한 번에 설치/해제
 * - 페이즈 변환에 따라 VOTE/SELECT를 자동 show/hide
 */
public final class LobbyFlowBootstrap {

    private final JFrame frame;
    private final Object netClient;

    // Installers
    private LobbyUiInstaller lobbyUi;
    private MapVoteUiInstaller voteUi;
    private CharacterSelectUiInstaller selectUi;

    // Binders
    private MapVotePhaseBinder voteBinder;
    private CharacterSelectPhaseBinder selectBinder;

    // 공유 모델
    private final ClientPhaseModel model;

    private java.awt.event.WindowListener disposer;

    private LobbyFlowBootstrap(JFrame frame, Object netClient) {
        this.frame = Objects.requireNonNull(frame, "frame");
        this.netClient = Objects.requireNonNull(netClient, "netClient");
        this.model = ClientPhaseModel.installDefault();
    }

    /** 로비 흐름을 설치하고, 프레임 종료 시 자동 해제 */
    public static LobbyFlowBootstrap attach(JFrame frame, Object netClient) {
        LobbyFlowBootstrap f = new LobbyFlowBootstrap(frame, netClient);
        f.install();
        f.hookDispose();
        return f;
    }

    /** 자동 해제(중복 호출 안전) */
    public synchronized void detach() {
        try { if (voteBinder   != null) voteBinder.unbind(); } catch (Throwable ignore) {}
        try { if (selectBinder != null) selectBinder.unbind(); } catch (Throwable ignore) {}
        try { if (voteUi   != null) voteUi.uninstall(); } catch (Throwable ignore) {}
        try { if (selectUi != null) selectUi.uninstall(); } catch (Throwable ignore) {}
        try { if (lobbyUi  != null) lobbyUi.uninstall(); } catch (Throwable ignore) {}
        voteBinder = null; selectBinder = null; voteUi = null; selectUi = null; lobbyUi = null;
        if (disposer != null) { try { frame.removeWindowListener(disposer); } catch (Throwable ignore) {} disposer = null; }
    }

    // 초기 설치 ---------------------------------------------------------
    private void install() {
        // 1) READY 리스너 + 버튼(EAST)
        lobbyUi = LobbyUiInstaller.installTo(frame, netClient, BorderLayout.EAST);
        // 2) 맵 투표 패널(CENTER)
        voteUi = MapVoteUiInstaller.installTo(frame, netClient, BorderLayout.CENTER);
        if (voteUi != null) voteUi.hide();
        // 3) 캐릭터 선택 패널(CENTER)
        selectUi = CharacterSelectUiInstaller.installTo(frame, netClient, BorderLayout.CENTER);
        if (selectUi != null) selectUi.hide();

        // 4) 페이즈 바인딩: CENTER 공간에서 VOTE/SELECT 전환
        voteBinder = new MapVotePhaseBinder(model).bind();
        voteBinder.onShow(() -> { if (selectUi != null) selectUi.hide(); if (voteUi != null) voteUi.show(); });
        voteBinder.onHide(() -> { if (voteUi != null) voteUi.hide(); });

        selectBinder = new CharacterSelectPhaseBinder(model).bind();
        selectBinder.onShow(() -> { if (voteUi != null) voteUi.hide(); if (selectUi != null) selectUi.show(); });
        selectBinder.onHide(() -> { if (selectUi != null) selectUi.hide(); });

        frame.revalidate();
        frame.repaint();
    }

    private void hookDispose() {
        this.disposer = new java.awt.event.WindowAdapter() {
            @Override public void windowClosing(java.awt.event.WindowEvent e) { detach(); }
            @Override public void windowClosed (java.awt.event.WindowEvent e) { detach(); }
        };
        frame.addWindowListener(disposer);
    }

    // 동적 업데이트 ------------------------------------------------------
    /** 로비 맵 후보 갱신(서버 브로드캐스트에 맞춰 호출) */
    public LobbyFlowBootstrap setMaps(List<String> mapIds) { if (voteUi != null) voteUi.updateMaps(mapIds); return this; }
    /** 로비 캐릭터 후보 갱신(서버 브로드캐스트에 맞춰 호출) */
    public LobbyFlowBootstrap setCharacters(List<String> ids) { if (selectUi != null) selectUi.updateCharacters(ids); return this; }

    // 접근자 ------------------------------------------------------------
    public JFrame getFrame() { return frame; }
    public Object getNetClient() { return netClient; }
    public LobbyUiInstaller getLobbyUi() { return lobbyUi; }
    public MapVoteUiInstaller getVoteUi() { return voteUi; }
    public CharacterSelectUiInstaller getSelectUi() { return selectUi; }
    public MapVotePhaseBinder getVoteBinder() { return voteBinder; }
    public CharacterSelectPhaseBinder getSelectBinder() { return selectBinder; }
}

