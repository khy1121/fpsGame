package com.fpsgame.client;

import javax.swing.*;
import java.util.List;

/**
 * PhaseAutoRegister
 * ------------------------------------------------------------
 * 런처/엔트리에서 한 줄로 HUD/오버레이/로비 UI를 장착/해제하기 위한 얇은 퍼사드.
 * <p>
 * - 가능하면 {@link ClientController#attachUi(JFrame)} / {@link ClientController#detachUi()}를 사용하고,
 *   그게 없거나 다른 호출자가 넘어오면 내부적으로 {@link PhaseIntegration}, {@link ClientLobbyBootstrap}을 직접 사용한다.
 * - 모든 동작은 중복 호출에 안전하다.
 */
public final class PhaseAutoRegister {

    private PhaseAutoRegister() {}

    // 장착 상태(간단 캐시): 동일 프레임에 대해 마지막으로 장착한 인스턴스를 보관
    private static PhaseIntegration lastIntegration;
    private static ClientLobbyBootstrap lastLobby;

    /**
     * 프레임에 HUD/오버레이/로비 UI를 장착한다.
     *
     * @param frame    대상 프레임(필수)
     * @param netLike  NetClient 또는 ClientController(둘 다 허용)
     */
    public static void install(JFrame frame, Object netLike) {
        if (frame == null || netLike == null) return;

        // 1) ClientController를 넘긴 경우 최우선: attachUi 호출
        if (netLike instanceof ClientController) {
            try {
                ((ClientController) netLike).attachUi(frame);
                return;
            } catch (Throwable ignore) {
                // attachUi 미구현 등 → 내부 경로로 폴백
            }
        }

        // 2) 내부 경로: PhaseIntegration + ClientLobbyBootstrap
        // NetClient가 필요한 PhaseIntegration은 ClientController에서만 안전하므로,
        // 단독 NetClient가 온 경우에만 직접 장착. (그 외엔 로비만 장착)
        if (netLike instanceof NetClient) {
            try {
                lastIntegration = PhaseIntegration.install(frame, (NetClient) netLike);
            } catch (Throwable ignore) {
                lastIntegration = null;
            }
        }
        try {
            lastLobby = ClientLobbyBootstrap
                    .install(frame, netLike)
                    .withMaps(List.of("terminal","neonCity","ForestOutpost"))
                    .withCharacters(List.of("Sage","Piper","Technician","General","Bulldog",
                                            "Wildcat","Raven","Ghost","Skull","Steam"));
        } catch (Throwable ignore) {
            lastLobby = null;
        }
    }

    /**
     * 프레임에서 HUD/오버레이/로비 UI를 해제한다.
     * - ClientController가 넘어온 경우 {@link ClientController#detachUi()} 사용.
     * - 그렇지 않으면 마지막 장착 캐시를 기반으로 해제한다.
     */
    public static void uninstall(Object maybeController) {
        if (maybeController instanceof ClientController) {
            try {
                ((ClientController) maybeController).uninstallUi();
            } catch (Throwable ignore) {
                // 폴백으로 계속
            }
        }
        // 캐시로 폴백 해제
        try {
            if (lastLobby != null) { lastLobby.uninstall(); }
        } catch (Throwable ignore) {}
        try {
            if (lastIntegration != null) { lastIntegration.uninstall(); }
        } catch (Throwable ignore) {}
        lastLobby = null;
        lastIntegration = null;
    }
}
