package com.fpsgame.client.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.util.Objects;

/**
 * PhaseUiInstaller (경량화 버전)
 * ------------------------------------------------------------
 * 프레임에 "상단 HUD + 중앙 오버레이"만 최소로 설치/해제하는 유틸.
 * - 외부 의존/브릿지/바인더를 붙이지 않는다.
 * - 존재하는 패널(PhaseHudPanel, CenterMessageOverlayPanel)을 그대로 사용한다.
 * - 모든 UI 갱신은 EDT에서 수행한다.
 *
 * 사용 예:
 *   PhaseUiInstaller i = PhaseUiInstaller.installTo(frame, netLike);
 *   ...
 *   i.uninstall();
 */
public final class PhaseUiInstaller {

    private final JFrame frame;
    @SuppressWarnings("unused")
    private final Object netLike; // 인터페이스 단순화를 위해 보관만(미사용)

    private JComponent hud;          // PhaseHudPanel
    private JComponent overlay;      // CenterMessageOverlayPanel
    private ComponentAdapter resizeSync;
    private boolean installed;

    private PhaseUiInstaller(JFrame frame, Object netLike) {
        this.frame   = Objects.requireNonNull(frame, "frame");
        this.netLike = netLike; // (경량화 버전에서는 미사용)
    }

    /** 프레임에 HUD+오버레이를 설치하고 인스턴스를 반환한다. */
    public static PhaseUiInstaller installTo(JFrame frame, Object netLike) {
        PhaseUiInstaller i = new PhaseUiInstaller(frame, netLike);
        i.doInstall();
        return i;
    }

    /** 해제(중복 호출 안전) */
    public void uninstall() {
        if (!installed) return;
        SwingUtilities.invokeLater(() -> {
            try {
                // 오버레이 제거
                if (overlay != null) {
                    try { frame.getLayeredPane().remove(overlay); } catch (Throwable ignore) {}
                    overlay = null;
                }
                // HUD 제거
                if (hud != null) {
                    try { frame.getContentPane().remove(hud); } catch (Throwable ignore) {}
                    hud = null;
                }
                // 리사이즈 리스너 해제
                if (resizeSync != null) {
                    try { frame.removeComponentListener(resizeSync); } catch (Throwable ignore) {}
                    resizeSync = null;
                }
                frame.revalidate();
                frame.repaint();
            } finally {
                installed = false;
            }
        });
    }

    // ====================== 내부 구현 ======================

    private void doInstall() {
        SwingUtilities.invokeLater(() -> {
            ensureBorderLayout(frame.getContentPane());

            // 1) 상단 HUD
            hud = createHud();
            if (hud != null) {
                frame.getContentPane().add(hud, BorderLayout.NORTH);
            }

            // 2) 중앙 오버레이(레이어드 페인 위에 풀스크린 부착)
            overlay = createOverlay();
            if (overlay != null) {
                JLayeredPane lp = frame.getLayeredPane();
                lp.add(overlay, JLayeredPane.PALETTE_LAYER);
                overlay.setOpaque(false);

                resizeSync = new ComponentAdapter() {
                    @Override public void componentResized(java.awt.event.ComponentEvent e) { syncOverlayBounds(); }
                    @Override public void componentShown  (java.awt.event.ComponentEvent e) { syncOverlayBounds(); }
                };
                frame.addComponentListener(resizeSync);
                syncOverlayBounds();
            }

            frame.revalidate();
            frame.repaint();
            installed = true;
        });
    }

    /** BorderLayout이 아니면 교체 */
    private static void ensureBorderLayout(Container content) {
        if (!(content.getLayout() instanceof BorderLayout)) {
            content.setLayout(new BorderLayout());
        }
    }

    /** 오버레이를 프레임 콘텐츠 영역 크기에 맞춰 확장 */
    private void syncOverlayBounds() {
        if (overlay == null) return;
        int w = Math.max(1, frame.getContentPane().getWidth());
        int h = Math.max(1, frame.getContentPane().getHeight());
        overlay.setBounds(0, 0, w, h);
        overlay.revalidate();
        overlay.repaint();
    }

    // -------------------- 생성 헬퍼(경량) --------------------

    private JComponent createHud() {
        try {
            return new PhaseHudPanel();
        } catch (Throwable ignore) {
            // HUD가 없으면 조용히 스킵(경량화 방침)
            return null;
        }
    }

    private JComponent createOverlay() {
        try {
            return new CenterMessageOverlayPanel();
        } catch (Throwable ignore) {
            // 오버레이가 없으면 조용히 스킵
            return null;
        }
    }
}
