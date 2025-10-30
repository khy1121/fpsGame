package com.fpsgame.client.model;

import java.awt.event.KeyEvent;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

/**
 * 키 바인딩 모델.
 * <p>
 * - 액션별 키 코드 저장/수정
 * - 기본값 복원, 직렬화/역직렬화(Properties) 지원
 * - 스레드 안전: 동기화 블록으로 보호(EDT/게임 루프 혼용 대비)
 */
public final class Keybinds {

    /** 바인딩 가능한 액션 열거형 */
    public enum Action {
        MOVE_UP,
        MOVE_DOWN,
        MOVE_LEFT,
        MOVE_RIGHT,
        ATTACK_PRIMARY,
        SKILL_TACTICAL,
        SKILL_ULTIMATE,
        OPEN_MENU,
        CHAT_SEND,
        READY_TOGGLE,
        CHANGE_CHARACTER
    }

    /** 액션 → 키코드 매핑 */
    private final EnumMap<Action, Integer> map = new EnumMap<>(Action.class);

    private Keybinds() {
        resetDefaults();
    }

    /** 기본값으로 채워진 인스턴스 생성 */
    public static Keybinds defaults() {
        return new Keybinds();
    }

    // ---------------------------------------------------------------------
    // 조회/설정
    // ---------------------------------------------------------------------

    /** 액션의 현재 키 코드 반환(없으면 VK_UNDEFINED) */
    public synchronized int get(Action a) {
        Integer v = map.get(a);
        return v == null ? KeyEvent.VK_UNDEFINED : v;
    }

    /** 액션에 키 코드 설정(null 무시) */
    public synchronized void set(Action a, Integer keyCode) {
        Objects.requireNonNull(a, "action");
        if (keyCode == null) return;
        map.put(a, keyCode);
    }

    /** 액션의 현재 키 이름(표시용) */
    public synchronized String keyName(Action a) {
        return keyName(get(a));
    }

    /** 키 코드의 표시 이름 */
    public static String keyName(int keyCode) {
        if (keyCode == KeyEvent.VK_UNDEFINED || keyCode == 0) return "(없음)";
        return KeyEvent.getKeyText(keyCode);
    }

    /** 모든 바인딩을 기본값으로 복원 */
    public synchronized void resetDefaults() {
        map.clear();
        map.put(Action.MOVE_UP, KeyEvent.VK_W);
        map.put(Action.MOVE_DOWN, KeyEvent.VK_S);
        map.put(Action.MOVE_LEFT, KeyEvent.VK_A);
        map.put(Action.MOVE_RIGHT, KeyEvent.VK_D);

        map.put(Action.ATTACK_PRIMARY, MouseEventProxy.VK_MOUSE_LEFT);   // 마우스 대체 키 코드(프록시)
        map.put(Action.SKILL_TACTICAL, KeyEvent.VK_E);
        map.put(Action.SKILL_ULTIMATE, KeyEvent.VK_Q);

        map.put(Action.OPEN_MENU, KeyEvent.VK_ESCAPE);
        map.put(Action.CHAT_SEND, KeyEvent.VK_ENTER);
        map.put(Action.READY_TOGGLE, KeyEvent.VK_R);
        map.put(Action.CHANGE_CHARACTER, KeyEvent.VK_B);
    }

    // ---------------------------------------------------------------------
    // 직렬화/역직렬화(Properties)
    // ---------------------------------------------------------------------

    /** Properties로 저장(키: action.name, 값: 정수 키코드) */
    public synchronized Properties toProperties() {
        Properties p = new Properties();
        for (Map.Entry<Action, Integer> e : map.entrySet()) {
            p.setProperty(e.getKey().name(), String.valueOf(e.getValue()));
        }
        return p;
    }

    /** Properties에서 로드(해당 키만 갱신, 유효하지 않으면 무시) */
    public synchronized void load(Properties p) {
        if (p == null) return;
        for (Action a : Action.values()) {
            String v = p.getProperty(a.name());
            if (v == null) continue;
            try {
                int code = Integer.parseInt(v.trim());
                if (code > 0) map.put(a, code);
            } catch (NumberFormatException ignore) {
                // 무시
            }
        }
    }

    // ---------------------------------------------------------------------
    // 내부: 마우스 버튼을 키코드처럼 표현하기 위한 프록시
    // ---------------------------------------------------------------------

    /**
     * Swing KeyStroke로 마우스 버튼을 표현하기는 번거롭기 때문에
     * 내부적으로 특별한 키코드 값을 예약해둔다.
     * <p>
     * 실제 입력 시스템에서 마우스 버튼과 매핑할 때만 사용(표시용/저장 가능).
     */
    public static final class MouseEventProxy {
        private MouseEventProxy() {}
        /** 좌클릭 프록시 코드(일반 키코드와 겹치지 않도록 높은 값 사용) */
        public static final int VK_MOUSE_LEFT = 0xF001;
        public static final int VK_MOUSE_RIGHT = 0xF002;
        public static final int VK_MOUSE_MIDDLE = 0xF003;

        /** 프록시 코드 여부 */
        public static boolean isProxy(int code) {
            return code == VK_MOUSE_LEFT || code == VK_MOUSE_RIGHT || code == VK_MOUSE_MIDDLE;
        }

        /** 프록시 코드의 표시 이름 */
        public static String name(int code) {
            return switch (code) {
                case VK_MOUSE_LEFT -> "Mouse Left";
                case VK_MOUSE_RIGHT -> "Mouse Right";
                case VK_MOUSE_MIDDLE -> "Mouse Middle";
                default -> Keybinds.keyName(code);
            };
        }
    }

    // ---------------------------------------------------------------------
    // 디버그
    // ---------------------------------------------------------------------

    @Override
    public synchronized String toString() {
        StringBuilder sb = new StringBuilder("Keybinds{");
        boolean first = true;
        for (Action a : Action.values()) {
            if (!first) sb.append(", ");
            first = false;
            sb.append(a.name()).append('=').append(keyName(a));
        }
        sb.append('}');
        return sb.toString();
    }
}
