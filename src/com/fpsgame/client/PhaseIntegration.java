package com.fpsgame.client;

import java.util.Objects;

import javax.swing.JFrame;

import com.fpsgame.client.ui.PhaseUiInstaller;

/**
 * PhaseIntegration
 * ------------------------------------------------------------
 * Net ↔(Bus/Model)↔ UI(HUD/오버레이) 연결의 경량 통합 설치자.
 *
 * <p>역할</p>
 * <ul>
 *   <li>프레임에 상단 HUD와 중앙 오버레이를 설치한다.</li>
 *   <li>필요 시(확장 포인트) NetClient 수신 이벤트를 ClientPhaseBus/Model로 중계할 수 있다.</li>
 *   <li>중복 설치/해제에 안전하다.</li>
 * </ul>
 *
 * <p>사용</p>
 * <pre>
 *   PhaseIntegration pi = PhaseIntegration.install(frame, netClient);
 *   ...
 *   pi.uninstall();
 * </pre>
 *
 * <p>설계 메모</p>
 * - 현재 버전은 HUD/오버레이 설치에 집중한다. 네트 이벤트 중계는
 *   이미 상위 {@link ClientController}에서 UI 콜백으로 전달되고 있으므로
 *   필요 시 추후 ClientPhaseBus/Model 브리지 코드를 여기에 추가한다.
 */
public final class PhaseIntegration {

    private final JFrame frame;
    @SuppressWarnings("unused")
    private final NetClient net; // 확장 포인트: 필요 시 버스/모델로 이벤트 브리지

    // UI 일괄 설치자(HUD + Center Overlay)
    private PhaseUiInstaller uiInstaller;

    private PhaseIntegration(JFrame frame, NetClient net) {
        this.frame = Objects.requireNonNull(frame, "frame");
        this.net   = Objects.requireNonNull(net,   "netClient");
    }

    /** 프레임에 HUD/오버레이를 설치하고 인스턴스를 반환한다. */
    public static PhaseIntegration install(JFrame frame, NetClient net) {
        PhaseIntegration i = new PhaseIntegration(frame, net);
        i.doInstall();
        return i;
    }

    /** 해제(중복 호출 안전) */
    public void uninstall() {
        try {
            if (uiInstaller != null) {
                uiInstaller.uninstall();
            }
        } catch (Throwable ignore) {
        } finally {
            uiInstaller = null;
        }
    }

    // ================= 내부 구현 =================

    private void doInstall() {
        try {
            // HUD + 중앙 오버레이 일괄 설치
            uiInstaller = PhaseUiInstaller.installTo(frame, net);
        } catch (Throwable t) {
            // 설치 실패 시에도 전체 앱은 계속 동작해야 한다.
            uiInstaller = null;
        }
    }
}
