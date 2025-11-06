package com.fpsgame.client;

import com.fpsgame.client.ui.PhaseUiInstaller;

import javax.swing.*;
import java.util.Objects;

/**
 * 클라이언트 페이즈 부트스트랩
 * - 초기화 코드(ClientMain/GameFrame)에서 UI/HUD/바인딩을 설치/해제
 * - PhaseUiInstaller를 감싸 간단한 attach/detach 제공
 */
public final class ClientPhaseBootstrap {

    private final JFrame frame;
    private final NetClient client;
    private PhaseUiInstaller installer;
    private java.awt.event.WindowListener disposer;

    private ClientPhaseBootstrap(JFrame frame, NetClient client) {
        this.frame = Objects.requireNonNull(frame, "frame");
        this.client = Objects.requireNonNull(client, "client");
    }

    /** HUD/바인딩 설치 후 프레임 종료 시 자동 해제 연결 */
    public static ClientPhaseBootstrap attach(JFrame frame, NetClient client) {
        ClientPhaseBootstrap b = new ClientPhaseBootstrap(frame, client);
        b.install();
        b.hookDispose();
        return b;
    }

    /** 자동 해제(리스너 제거 + UI 해제) */
    public synchronized void detach() {
        try {
            if (installer != null) { installer.uninstall(); installer = null; }
        } finally {
            if (disposer != null) { frame.removeWindowListener(disposer); disposer = null; }
        }
    }

    // 내부 구현 ----------------------------------------------------------
    private void install() { this.installer = PhaseUiInstaller.installTo(frame, client); }

    private void hookDispose() {
        // 프레임 종료 시 정리
        this.disposer = new java.awt.event.WindowAdapter() {
            @Override public void windowClosed (java.awt.event.WindowEvent e) { detach(); }
            @Override public void windowClosing(java.awt.event.WindowEvent e) { detach(); }
        };
        frame.addWindowListener(disposer);
    }

    // 접근자 ------------------------------------------------------------
    public JFrame getFrame() { return frame; }
    public NetClient getClient() { return client; }
    public PhaseUiInstaller getInstaller() { return installer; }
}

