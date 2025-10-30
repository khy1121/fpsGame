package com.fpsgame.client.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Objects;

/**
 * HUD/오버레이 토글 키 바인딩 유틸.
 *
 * <p>역할</p>
 * <ul>
 *   <li>지정한 프레임의 {@link JLayeredPane}에서 {@link CenterMessageOverlayPanel}을 찾아 보이기/숨김을 토글</li>
 *   <li>기본 키는 <b>F9</b> 이며, 필요 시 다른 KeyStroke 로도 등록 가능</li>
 *   <li>프로젝트 외부 의존 없이 독립적으로 사용 가능(없으면 조용히 무시)</li>
 * </ul>
 *
 * <p>사용</p>
 * <pre>
 *   // 프레임 가시화 직후
 *   HudOverlayToggleKeybind.install(frame);           // 기본 F9
 *   // 또는
 *   HudOverlayToggleKeybind.install(frame, KeyStroke.getKeyStroke("F10"));
 * </pre>
 *
 * <p>주의</p>
 * <ul>
 *   <li>EDT에서 호출할 것을 권장</li>
 *   <li>오버레이가 아직 생성되지 않았다면(레이어드 페인에 없음) 토글 시 아무 동작도 하지 않음</li>
 * </ul>
 */
public final class HudOverlayToggleKeybind {

    private HudOverlayToggleKeybind() {}

    /** 기본 키(F9)로 설치 */
    public static void install(JFrame frame) {
        install(frame, KeyStroke.getKeyStroke("F9"));
    }

    /** 사용자 지정 키로 설치 */
    public static void install(JFrame frame, KeyStroke key) {
        Objects.requireNonNull(frame, "frame");
        Objects.requireNonNull(key, "key");

        JRootPane root = frame.getRootPane();
        if (root == null) return;

        String actionKey = "toggle-center-overlay";

        // InputMap
        InputMap im = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        im.put(key, actionKey);

        // ActionMap
        ActionMap am = root.getActionMap();
        am.put(actionKey, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                toggleOverlay(frame);
            }
        });
    }

    // ========================= 내부 구현 =========================

    /** 레이어드 페인에서 CenterMessageOverlayPanel을 찾아 보이기/숨김 토글 */
    private static void toggleOverlay(JFrame frame) {
        JLayeredPane lp = frame.getLayeredPane();
        if (lp == null) return;

        Component target = null;
        for (Component c : lp.getComponents()) {
            if (c == null) continue;
            if ("com.fpsgame.client.ui.CenterMessageOverlayPanel".equals(c.getClass().getName())) {
                target = c;
                break;
            }
        }
        if (target == null) return;

        boolean vis = target.isVisible();
        target.setVisible(!vis);
        lp.revalidate();
        lp.repaint();
    }
}
