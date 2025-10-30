package com.fpsgame.client;

import com.fpsgame.client.ui.LobbyCenterCoordinator;
import com.fpsgame.client.ui.LobbyUiInstaller;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * ClientLobbyBootstrap
 * ------------------------------------------------------------
 * 프레임에 "로비 UI"를 한 번에 장착/해제하는 상위 퍼사드.
 * - EAST : READY 리스트/버튼
 * - CENTER : 맵 투표 / 캐릭터 선택 (페이즈에 따라 자동 전환)
 *
 * 사용 예:
 *   ClientLobbyBootstrap b = ClientLobbyBootstrap
 *       .install(frame, netClient)
 *       .withMaps(List.of("terminal","neonCity","ForestOutpost"))
 *       .withCharacters(List.of("Sage","Piper","Technician","General","Bulldog",
 *                               "Wildcat","Raven","Ghost","Skull","Steam"));
 *   ...
 *   b.uninstall();
 *
 * 설계:
 * - CENTER 영역은 {@link LobbyCenterCoordinator} 가 전담(맵/캐릭 전환 및 바인딩).
 * - EAST READY 패널은 {@link LobbyUiInstaller} 가 담당.
 * - netClient 파라미터는 NetClient 또는 ClientController 등 송신 메서드를 갖는 객체면 충분하다.
 */
public final class ClientLobbyBootstrap {

    private final JFrame frame;
    private final Object netLike; // NetClient 또는 ClientController 등의 송신 대상

    // EAST 영역 (READY 리스트/버튼)
    private LobbyUiInstaller lobbyEast;

    // CENTER 전환 코디네이터 (맵 투표/캐릭 선택 + 바인더)
    private LobbyCenterCoordinator center;

    // 초기 주입용 후보 리스트 (선택)
    private List<String> maps = new ArrayList<>();
    private List<String> characters = new ArrayList<>();

    private boolean installed;

    private ClientLobbyBootstrap(JFrame frame, Object netLike) {
        this.frame = Objects.requireNonNull(frame, "frame");
        this.netLike = Objects.requireNonNull(netLike, "netLike");
    }

    /** 프레임에 로비 UI를 설치하고 퍼사드 인스턴스를 반환한다. */
    public static ClientLobbyBootstrap install(JFrame frame, Object netLike) {
        ClientLobbyBootstrap b = new ClientLobbyBootstrap(frame, netLike);
        b.doInstall();
        return b;
    }

    // ---------------------- 플루언트 설정 ----------------------

    /** 맵 후보를 주입(설치 후에는 즉시 UI에 반영됨). */
    public ClientLobbyBootstrap withMaps(List<String> mapIds) {
        this.maps = (mapIds == null) ? new ArrayList<>() : new ArrayList<>(mapIds);
        // 설치가 끝난 상태라면 곧바로 반영
        if (installed && center != null) {
            try { center.updateMaps(this.maps); } catch (Throwable ignore) {}
        }
        return this;
    }

    /** 캐릭터 후보를 주입(설치 후에는 즉시 UI에 반영됨). */
    public ClientLobbyBootstrap withCharacters(List<String> ids) {
        this.characters = (ids == null) ? new ArrayList<>() : new ArrayList<>(ids);
        if (installed && center != null) {
            try { center.updateCharacters(this.characters); } catch (Throwable ignore) {}
        }
        return this;
    }

    // ---------------------- 해제 ----------------------

    /** 로비 UI 제거(중복 호출 안전). */
    public void uninstall() {
        if (!installed) return;
        SwingUtilities.invokeLater(() -> {
            try {
                if (center != null) {
                    try { center.uninstall(); } catch (Throwable ignore) {}
                    center = null;
                }
                if (lobbyEast != null) {
                    try { lobbyEast.uninstall(); } catch (Throwable ignore) {}
                    lobbyEast = null;
                }
                frame.revalidate();
                frame.repaint();
            } finally {
                installed = false;
            }
        });
    }

    // ---------------------- 내부 구현 ----------------------

    private void doInstall() {
        SwingUtilities.invokeLater(() -> {
            ensureBorderLayout(frame.getContentPane());

            // EAST : READY 리스트/버튼 설치
            try {
                lobbyEast = LobbyUiInstaller.installTo(frame, netLike, BorderLayout.EAST);
            } catch (Throwable t) {
                lobbyEast = null; // 클래스 부재/시그니처 상이 시 조용히 스킵
            }

            // CENTER : 맵 투표 / 캐릭 선택 (자동 전환 코디네이터)
            try {
                center = LobbyCenterCoordinator.install(frame.getContentPane(), netLike);
            } catch (Throwable t) {
                center = null;
            }

            // 초기 후보 반영
            try {
                if (center != null && !maps.isEmpty()) {
                    center.updateMaps(maps);
                }
            } catch (Throwable ignore) {}
            try {
                if (center != null && !characters.isEmpty()) {
                    center.updateCharacters(characters);
                }
            } catch (Throwable ignore) {}

            frame.revalidate();
            frame.repaint();
            installed = true;
        });
    }

    private static void ensureBorderLayout(Container content) {
        if (!(content.getLayout() instanceof BorderLayout)) {
            content.setLayout(new BorderLayout());
        }
    }

    // ---------------------- 선택적 접근자 ----------------------

    public JFrame getFrame() { return frame; }
    public Object getNetLike() { return netLike; }
    public LobbyUiInstaller getLobbyEast() { return lobbyEast; }
    public LobbyCenterCoordinator getCenter() { return center; }
    public List<String> getMaps() { return List.copyOf(maps); }
    public List<String> getCharacters() { return List.copyOf(characters); }
}
