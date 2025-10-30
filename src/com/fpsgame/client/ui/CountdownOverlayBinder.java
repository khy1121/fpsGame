package com.fpsgame.client.ui;

import com.fpsgame.client.model.ClientPhaseBus;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.Objects;

/**
 * CountdownOverlayBinder (경량)
 * ------------------------------------------------------------
 * ClientPhaseBus 의 카운트다운 이벤트를 중앙 오버레이(CenterMessageOverlayPanel)에 바인딩한다.
 *
 * 특징
 * - 프레임 위 JLayeredPane 에 이미 오버레이가 있으면 재사용하고,
 *   없으면 최소 구현을 직접 추가한다(해제 시 본인이 추가한 것만 제거).
 * - 모든 UI 갱신은 EDT에서 수행.
 *
 * 사용
 * <pre>
 *   CountdownOverlayBinder b = CountdownOverlayBinder.install(frame);
 *   ...
 *   b.uninstall(); // 리스너/오버레이 정리
 * </pre>
 */
public final class CountdownOverlayBinder implements ClientPhaseBus.PhaseListener {

    private final JFrame frame;
    private CenterMessageOverlayPanel overlay; // 재사용 또는 생성
    private boolean ownOverlay;                // 우리가 추가했는지 여부
    private ComponentAdapter resizeSync;       // 오버레이 사이즈 동기화

    private boolean installed;

    private CountdownOverlayBinder(JFrame frame) {
        this.frame = Objects.requireNonNull(frame, "frame");
    }

    /** 권장: 간편 설치 (버스 구독 + 오버레이 확보) */
    public static CountdownOverlayBinder install(JFrame frame) {
        CountdownOverlayBinder b = new CountdownOverlayBinder(frame);
        b.bind();
        return b;
    }

    /** 버스 구독 시작(체이닝 가능) */
    public CountdownOverlayBinder bind() {
        if (installed) return this;
        SwingUtilities.invokeLater(() -> {
            ensureOverlay();
            ClientPhaseBus.get().addListener(this);
            installed = true;
        });
        return this;
    }

    /** 버스 구독 해제 별칭 */
    public void unbind() {
        uninstall();
    }

    /** 해제(중복 호출 안전) */
    public void uninstall() {
        if (!installed) return;
        SwingUtilities.invokeLater(() -> {
            try {
                ClientPhaseBus.get().removeListener(this);
            } catch (Throwable ignore) {}

            if (overlay != null && ownOverlay) {
                try { frame.getLayeredPane().remove(overlay); } catch (Throwable ignore) {}
                overlay = null;
            }
            if (resizeSync != null) {
                try { frame.removeComponentListener(resizeSync); } catch (Throwable ignore) {}
                resizeSync = null;
            }
            frame.revalidate();
            frame.repaint();
            installed = false;
        });
    }

    // ========================= ClientPhaseBus 리스너 =========================

    @Override
    public void onPhaseUpdate(ClientPhaseBus.PhaseState state) {
        // 사용 안 함
    }

    @Override
    public void onCountdown(int seconds) {
        SwingUtilities.invokeLater(() -> {
            ensureOverlay();
            if (overlay == null) return;
            if (seconds <= 0) {
                overlay.hideOverlay();
            } else {
                overlay.showCountdown(seconds);
            }
        });
    }

    @Override
    public void onRoundResult(ClientPhaseBus.RoundResult rr) {
        // 사용 안 함
    }

    @Override
    public void onReadyToggle(int sessionId, boolean ready) {
        // 사용 안 함
    }

    // ========================= 내부 구현 =========================

    /** 프레임 레이어드 페인에서 오버레이를 찾아 재사용하거나, 없으면 생성/추가 */
    private void ensureOverlay() {
        if (overlay != null) return;

        // 1) 이미 존재하는 CenterMessageOverlayPanel 재사용 시도
        JLayeredPane lp = frame.getLayeredPane();
        for (Component c : lp.getComponentsInLayer(JLayeredPane.PALETTE_LAYER)) {
            if (c instanceof CenterMessageOverlayPanel) {
                overlay = (CenterMessageOverlayPanel) c;
                ownOverlay = false;
                return;
            }
        }

        // 2) 없으면 우리가 생성/추가 (경량)
        overlay = new CenterMessageOverlayPanel();
        overlay.setVisible(false);
        lp.add(overlay, JLayeredPane.PALETTE_LAYER);
        ownOverlay = true;

        // 크기 동기화
        resizeSync = new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) { syncOverlayBounds(); }
            @Override public void componentShown  (ComponentEvent e) { syncOverlayBounds(); }
        };
        frame.addComponentListener(resizeSync);
        syncOverlayBounds();
    }

    /** 오버레이를 프레임 콘텐츠 크기에 맞게 확장 */
    private void syncOverlayBounds() {
        if (overlay == null) return;
        int w = Math.max(1, frame.getContentPane().getWidth());
        int h = Math.max(1, frame.getContentPane().getHeight());
        overlay.setBounds(0, 0, w, h);
        overlay.revalidate();
        overlay.repaint();
    }
}
