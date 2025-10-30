package com.fpsgame.client.ui;

import com.fpsgame.client.net.ReadyToggleSender;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Objects;

/**
 * 로비 레이아웃 설치 도우미
 * - 한 호출로 컨테이너에 로비 화면을 구성
 * - 배치 구성:
 *   - Center: LobbyCenterCoordinator (맵 투표 + 캐릭터 선택)
 *   - East  : LobbyReadyPanel (플레이어 목록 + 준비 토글)
 * - 네트워크 전송:
 *   - 맵/캐릭터: LobbyCenterCoordinator 내부에서 바인딩
 *   - 준비 토글: ReadyToggleSender로 이 클래스에서 바인딩
 * - 사용 예시:
 *   LobbyLayoutInstaller inst = LobbyLayoutInstaller.installTo(frame, netClient)
 *       .withMaps(List.of("terminal","neonCity","ForestOutpost"))
 *       .withCharacters(List.of("Sage","Piper","Technician","General","Bulldog",
 *                               "Wildcat","Raven","Ghost","Skull","Steam"));
 *   inst.uninstall(); // 해제 시
 */
public final class LobbyLayoutInstaller {

    private final Container container;
    private final Object netClient;

    private final JPanel root = new JPanel(new BorderLayout(8, 8));

    private LobbyCenterCoordinator center; // CENTER
    private LobbyReadyPanel ready;         // EAST

    private LobbyLayoutInstaller(Container container, Object netClient) {
        this.container = Objects.requireNonNull(container, "container");
        this.netClient = Objects.requireNonNull(netClient, "netClient");
        installInternal();
    }

    /** JFrame에 설치(컨텐트페인 BorderLayout 보장). */
    public static LobbyLayoutInstaller installTo(JFrame frame, Object netClient) {
        Objects.requireNonNull(frame, "frame");
        ensureBorderLayout(frame.getContentPane());
        LobbyLayoutInstaller i = new LobbyLayoutInstaller(frame.getContentPane(), netClient);
        frame.getContentPane().add(i.root, BorderLayout.CENTER);
        frame.getContentPane().revalidate();
        frame.getContentPane().repaint();
        return i;
    }

    /** 임의 컨테이너에 설치(BorderLayout 보장). */
    public static LobbyLayoutInstaller installTo(Container container, Object netClient) {
        ensureBorderLayout(container);
        LobbyLayoutInstaller i = new LobbyLayoutInstaller(container, netClient);
        container.add(i.root, BorderLayout.CENTER);
        container.revalidate();
        container.repaint();
        return i;
    }

    /** 맵 후보 갱신(체이닝 가능). */
    public LobbyLayoutInstaller withMaps(List<String> maps) {
        if (center != null) center.updateMaps(maps);
        return this;
    }

    /** 캐릭터 후보 갱신(체이닝 가능). */
    public LobbyLayoutInstaller withCharacters(List<String> ids) {
        if (center != null) center.updateCharacters(ids);
        return this;
    }

    /** 해제(idempotent). */
    public void uninstall() {
        try { if (center != null) center.uninstall(); } catch (Throwable ignore) {}
        try { if (ready  != null) ready.dispose();    } catch (Throwable ignore) {}

        try { container.remove(root); } catch (Throwable ignore) {}
        container.revalidate();
        container.repaint();

        center = null;
        ready = null;
    }

    // 내부 구현 ---------------------------------------------------------

    private void installInternal() {
        root.setOpaque(false);

        // CENTER: 로비 센터(맵 투표 / 캐릭터 선택)
        center = LobbyCenterCoordinator.installTo(root, netClient);

        // EAST  : 준비 패널(목록 + 토글 버튼)
        ready = new LobbyReadyPanel(netClient)
                .onSendReady(ready -> ReadyToggleSender.send(netClient, ready));
        root.add(ready, BorderLayout.EAST);

        // 센터 전송자는 LobbyCenterCoordinator 내부에서 바인딩됨(MapVote/SetSelection)
    }

    private static void ensureBorderLayout(Container c) {
        if (!(c.getLayout() instanceof BorderLayout)) {
            Component[] comps = c.getComponents();
            c.setLayout(new BorderLayout());
            for (Component comp : comps) c.add(comp, BorderLayout.CENTER);
        }
    }
}
