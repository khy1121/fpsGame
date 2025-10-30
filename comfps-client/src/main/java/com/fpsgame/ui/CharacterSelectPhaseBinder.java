package com.fpsgame.client.ui;

import com.fpsgame.client.model.ClientPhaseBus;
import com.fpsgame.client.model.ClientPhaseModel;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Objects;

/**
 * 캐릭터 선택(SELECT) 페이즈일 때 캐릭터 선택 패널을 표시/숨김하는 바인더.
 *
 * <p>레거시 호환 강화</p>
 * <ul>
 *   <li>new CharacterSelectPhaseBinder(ClientPhaseModel) 생성자 제공</li>
 *   <li>install(frame) / installDefault() / bind() / unbind() / uninstall() 제공</li>
 *   <li>onShow(), onShow(Runnable), onHide(), onHide(Runnable) 체이닝 지원</li>
 *   <li>프레임이 없으면(UI 토글 불가) 버스 구독만 수행</li>
 * </ul>
 */
public final class CharacterSelectPhaseBinder implements ClientPhaseBus.PhaseListener {

    /** 표시/숨김을 적용할 대상 프레임(없을 수 있음) */
    private final JFrame frame;

    // ===================== 생성/설치 =====================

    private CharacterSelectPhaseBinder(JFrame frame) {
        this.frame = frame; // null 허용
    }

    /** 권장: 프레임을 알고 있을 때 설치 */
    public static CharacterSelectPhaseBinder install(JFrame frame) {
        CharacterSelectPhaseBinder b = new CharacterSelectPhaseBinder(Objects.requireNonNull(frame, "frame"));
        ClientPhaseBus.get().addListener(b);
        return b;
    }

    /** 레거시: 모델만 전달하는 생성자(프레임 정보 없음 → 표시 토글 생략, 구독만 수행) */
    public CharacterSelectPhaseBinder(ClientPhaseModel model) {
        this.frame = null;
        ClientPhaseBus.get().addListener(this);
    }

    /** 레거시: 아무 정보 없이 기본 설치(버스 구독만) */
    public static CharacterSelectPhaseBinder installDefault() {
        CharacterSelectPhaseBinder b = new CharacterSelectPhaseBinder((JFrame) null);
        ClientPhaseBus.get().addListener(b);
        return b;
    }

    /** 레거시: 체이닝을 위한 바인드 API */
    public CharacterSelectPhaseBinder bind() {
        ClientPhaseBus.get().addListener(this);
        return this;
    }

    /** 레거시: 해제 별칭 */
    public void unbind() { uninstall(); }

    /** 해제(중복 호출 안전) */
    public void uninstall() {
        ClientPhaseBus.get().removeListener(this);
    }

    // -------- 레거시 훅(onShow/onHide) — 다양한 시그니처/체이닝 지원 --------

    /** no-op: 화면 표시 시점 훅(체이닝 유지) */
    public CharacterSelectPhaseBinder onShow() { return this; }

    /** Runnable 콜백 형태도 허용(체이닝) */
    public CharacterSelectPhaseBinder onShow(Runnable callback) {
        if (callback != null) {
            if (SwingUtilities.isEventDispatchThread()) callback.run();
            else SwingUtilities.invokeLater(callback);
        }
        return this;
    }

    /** no-op: 화면 숨김 시점 훅(체이닝 유지) */
    public CharacterSelectPhaseBinder onHide() { return this; }

    /** Runnable 콜백 형태도 허용(체이닝) — 호출부: .onHide(this::hideAll) 등 */
    public CharacterSelectPhaseBinder onHide(Runnable callback) {
        if (callback != null) {
            if (SwingUtilities.isEventDispatchThread()) callback.run();
            else SwingUtilities.invokeLater(callback);
        }
        return this;
    }

    // ======================= 버스 리스너 =======================

    @Override
    public void onPhaseUpdate(ClientPhaseBus.PhaseState state) {
        final boolean select = isSelectPhase(state);
        if (frame == null) return; // 프레임 정보가 없으면 UI 토글은 생략
        SwingUtilities.invokeLater(() -> setCharacterSelectVisible(select));
    }

    @Override public void onCountdown(int sec) { /* 사용 안 함 */ }
    @Override public void onRoundResult(ClientPhaseBus.RoundResult rr) { /* 사용 안 함 */ }
    @Override public void onReadyToggle(int sessionId, boolean ready) { /* 사용 안 함 */ }

    // ======================= 내부 구현 =======================

    /** PhaseState에서 "SELECT" 페이즈인지 유연하게 판정 */
    private static boolean isSelectPhase(Object state) {
        if (state == null) return false;
        // 1) enum/문자열 name()
        try {
            Method m = state.getClass().getMethod("name");
            Object v = m.invoke(state);
            if (isSelectString(v)) return true;
        } catch (Throwable ignore) {}
        // 2) getCode()/code 필드
        Object code = readAny(state, "code", "phase", "id", "value");
        if (isSelectString(code)) return true;
        // 3) getDisplay()/getLabel()
        Object label = readAny(state, "display", "label", "text");
        return isSelectString(label);
    }

    private static boolean isSelectString(Object o) {
        if (o == null) return false;
        String s = o.toString().trim().toUpperCase();
        // SELECT, CHAR, PICK 등 키워드 허용
        return s.contains("SELECT") || s.contains("CHAR") || s.contains("PICK");
    }

    private static Object readAny(Object obj, String... names) {
        for (String n : names) {
            // getter 우선
            try {
                Method m = obj.getClass().getMethod("get" + up(n));
                return m.invoke(obj);
            } catch (Throwable ignore) {}
            try {
                Method m = obj.getClass().getMethod(n);
                return m.invoke(obj);
            } catch (Throwable ignore) {}
            // public 필드
            try {
                Field f = obj.getClass().getField(n);
                return f.get(obj);
            } catch (Throwable ignore) {}
        }
        return null;
    }

    private static String up(String n) {
        if (n == null || n.isEmpty()) return n;
        return Character.toUpperCase(n.charAt(0)) + n.substring(1);
    }

    /** 프레임 내 CharacterSelectPanel을 찾아 표시/숨김 */
    private void setCharacterSelectVisible(boolean vis) {
        Component panel = findCharacterSelectPanel();
        if (panel == null) return;
        boolean current = panel.isVisible();
        if (current == vis) return;
        panel.setVisible(vis);
        panel.revalidate();
        panel.repaint();
    }

    private Component findCharacterSelectPanel() {
        if (frame == null) return null;
        // 1) contentPane 트리에서 탐색
        Component c = findInTree(frame.getContentPane());
        if (c != null) return c;
        // 2) 레이어드 페인에서도 탐색
        return findInTree(frame.getLayeredPane());
    }

    private Component findInTree(Container root) {
        if (root == null) return null;
        for (Component comp : root.getComponents()) {
            if (comp == null) continue;
            if ("com.fpsgame.client.ui.CharacterSelectPanel".equals(comp.getClass().getName())) {
                return comp;
            }
            if (comp instanceof Container) {
                Component found = findInTree((Container) comp);
                if (found != null) return found;
            }
        }
        return null;
    }
}
