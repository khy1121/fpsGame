package com.fpsgame.client.ui;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

/**
 * PhaseHudInstaller (경량)
 * ------------------------------------------------------------
 * 프레임 상단에 {@link PhaseHudPanel}만 간단히 설치/해제하는 유틸.
 * - 외부 바인더/브릿지 없이 HUD 자체만 다룬다.
 * - 모든 UI 갱신은 EDT에서 수행한다.
 *
 * 사용 예:
 *   PhaseHudInstaller i = PhaseHudInstaller.installTo(frame);
 *   ...
 *   i.show();      // 필요 시 보이기
 *   i.hide();      // 필요 시 숨기기
 *   i.uninstall(); // 제거
 */
public final class PhaseHudInstaller {

    private final JFrame frame;
    private PhaseHudPanel hud;
    private boolean installed;

    private PhaseHudInstaller(JFrame frame) {
        this.frame = Objects.requireNonNull(frame, "frame");
    }

    /** 프레임에 HUD를 설치하고 인스턴스를 반환한다. (중복 설치 안전) */
    public static PhaseHudInstaller installTo(JFrame frame) {
        PhaseHudInstaller i = new PhaseHudInstaller(frame);
        i.doInstall();
        return i;
    }

    /** 표시 */
    public void show() {
        runEdt(() -> {
            if (hud != null && !hud.isVisible()) {
                hud.setVisible(true);
                frame.revalidate();
                frame.repaint();
            }
        });
    }

    /** 숨김 */
    public void hide() {
        runEdt(() -> {
            if (hud != null && hud.isVisible()) {
                hud.setVisible(false);
                frame.revalidate();
                frame.repaint();
            }
        });
    }

    /** 해제(중복 호출 안전) */
    public void uninstall() {
        if (!installed) return;
        runEdt(() -> {
            try {
                if (hud != null) {
                    try { frame.getContentPane().remove(hud); } catch (Throwable ignore) {}
                    hud = null;
                }
                frame.revalidate();
                frame.repaint();
            } finally {
                installed = false;
            }
        });
    }

    // ================ 내부 구현 ================

    private void doInstall() {
        runEdt(() -> {
            Container content = frame.getContentPane();
            ensureBorderLayout(content);

            if (hud == null) {
                try {
                    hud = new PhaseHudPanel();
                    hud.setVisible(true);
                } catch (Throwable t) {
                    // HUD 클래스가 없거나 생성 실패 시 조용히 스킵
                    hud = null;
                }
            }

            if (hud != null) {
                content.add(hud, BorderLayout.NORTH);
            }

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

    private static void runEdt(Runnable r) {
        if (SwingUtilities.isEventDispatchThread()) r.run();
        else SwingUtilities.invokeLater(r);
    }
}
