package com.fpsgame.client.net;

import com.fpsgame.client.NetClient;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;

/**
 * READY 토글 전송 유틸(호환 확장판).
 *
 * <p>변경 사항</p>
 * <ul>
 *   <li>UI 쪽에서 NetClient를 <b>Object</b>로 보관하거나 READY 값을 <b>String/Boolean</b> 등
 *       다양한 타입으로 넘겨도 수용하도록 <b>오버로드</b> 추가.</li>
 *   <li>기존 시그니처 <code>send(NetClient, boolean)</code>는 그대로 유지(호환).</li>
 * </ul>
 *
 * <p>전송 우선순위</p>
 * <ol>
 *   <li><code>NetClient#sendReadyToggle(boolean)</code></li>
 *   <li><code>NetClient#sendRaw(byte, byte[])</code> 또는 <code>send(byte, byte[])</code>
 *       (opcode는 Protocol.Opcode에서 리플렉션으로 탐색)</li>
 *   <li><code>NetClient#sendFramed(byte[])</code> (length(4,BE)+opcode(1)+payload)</li>
 * </ol>
 */
public final class ReadyToggleSender {

    private ReadyToggleSender() {}

    // ===================== 공개 API (호환 오버로드) =====================

    /** 기존 사용처 호환: 강타입 NetClient + boolean */
    public static boolean send(NetClient net, boolean ready) {
        return sendAny(net, ready);
    }

    /** UI 일부가 Object로 들고 있을 때 호환: Object + boolean */
    public static boolean send(Object net, boolean ready) {
        return sendAny(net, ready);
    }

    /** READY 값을 박싱 Boolean으로 주는 경우 */
    public static boolean send(Object net, Boolean ready) {
        if (ready == null) return false;
        return sendAny(net, ready.booleanValue());
    }

    /** READY 값을 문자열("true"/"false"/"1"/"0"/"y"/"n")로 주는 경우 */
    public static boolean send(Object net, String ready) {
        Boolean b = toBool(ready);
        return (b != null) && sendAny(net, b);
    }

    /** READY 값을 Object로 주는 경우(문자열/숫자/불리언 모두 수용) */
    public static boolean send(Object net, Object ready) {
        Boolean b = toBool(ready);
        return (b != null) && sendAny(net, b);
    }

    // ===================== 내부 구현 =====================

    private static boolean sendAny(Object net, boolean ready) {
        if (net == null) return false;

        // 경로 1: 고수준 API가 있으면 사용
        if (invokeIfExists(net, "sendReadyToggle", ready)) return true;

        // opcode 후보 탐색
        Byte opcode = findOpcode("READY_TOGGLE", "READY", "SET_READY", "READY_SET", "TOGGLE_READY");
        if (opcode == null) return false;

        // 경로 2: sendRaw / send (1바이트 → 4바이트 순으로 시도)
        byte[] payload1 = new byte[]{ ready ? (byte)1 : (byte)0 };
        byte[] payload4 = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(ready ? 1 : 0).array();

        if (invokeIfExists(net, "sendRaw", opcode, payload1)) return true;
        if (invokeIfExists(net, "send",    opcode, payload1)) return true;
        if (invokeIfExists(net, "sendRaw", opcode, payload4)) return true;
        if (invokeIfExists(net, "send",    opcode, payload4)) return true;

        // 경로 3: 프레이밍 후 전송
        if (invokeIfExists(net, "sendFramed", frame(opcode, payload1))) return true;
        if (invokeIfExists(net, "sendFramed", frame(opcode, payload4))) return true;

        return false;
    }

    /** Protocol.Opcode.{names} 중 먼저 발견되는 것을 byte로 반환. 없으면 null. */
    private static Byte findOpcode(String... names) {
        try {
            Class<?> proto = Class.forName("com.fpsgame.common.Protocol$Opcode");
            for (String name : names) {
                try {
                    Field f = proto.getField(name);
                    Object v = f.get(null);
                    if (v instanceof Byte) return (Byte) v;
                    if (v instanceof Number) return (byte)((Number) v).intValue();
                } catch (NoSuchFieldException ignoreOne) {
                    // 다음 후보 계속
                }
            }
        } catch (Throwable ignore) {
            // 클래스가 없거나 접근 불가
        }
        return null;
    }

    /** length(4,BE) + opcode(1) + payload 구성 */
    private static byte[] frame(byte opcode, byte[] payload) {
        int plen = (payload == null) ? 0 : payload.length;
        ByteBuffer bb = ByteBuffer.allocate(4 + 1 + plen).order(ByteOrder.BIG_ENDIAN);
        bb.putInt(1 + plen);
        bb.put(opcode);
        if (plen > 0) bb.put(payload);
        return bb.array();
    }

    /** 리플렉션으로 동명 메서드가 있으면 호출. 성공 시 true. */
    private static boolean invokeIfExists(Object target, String name, Object... args) {
        if (target == null) return false;
        try {
            Method m = findMethod(target.getClass(), name, args);
            if (m == null) return false;
            m.invoke(target, args);
            return true;
        } catch (Throwable ignore) {
            return false;
        }
    }

    /** 인자 타입에 맞는 공개 메서드 탐색(박싱/기본형 호환 포함) */
    private static Method findMethod(Class<?> cls, String name, Object... args) {
        outer:
        for (Method m : cls.getMethods()) {
            if (!m.getName().equals(name)) continue;
            Class<?>[] pt = m.getParameterTypes();
            if (pt.length != args.length) continue;
            for (int i = 0; i < pt.length; i++) {
                Object a = args[i];
                if (a == null) continue;
                if (!wrap(pt[i]).isInstance(a) && !isPrimitiveMatch(pt[i], a)) {
                    continue outer;
                }
            }
            return m;
        }
        return null;
    }

    private static boolean isPrimitiveMatch(Class<?> param, Object arg) {
        if (!param.isPrimitive()) return false;
        return wrap(param).isInstance(arg);
    }

    private static Class<?> wrap(Class<?> t) {
        if (!t.isPrimitive()) return t;
        if (t == int.class)    return Integer.class;
        if (t == long.class)   return Long.class;
        if (t == short.class)  return Short.class;
        if (t == byte.class)   return Byte.class;
        if (t == boolean.class)return Boolean.class;
        if (t == char.class)   return Character.class;
        if (t == float.class)  return Float.class;
        if (t == double.class) return Double.class;
        return t;
    }

    // -------------------- 파싱 유틸 --------------------

    private static Boolean toBool(Object v) {
        if (v == null) return null;
        if (v instanceof Boolean) return (Boolean) v;
        if (v instanceof Number)  return ((Number) v).intValue() != 0;
        if (v instanceof CharSequence) {
            String s = v.toString().trim().toLowerCase();
            if ("true".equals(s) || "t".equals(s) || "yes".equals(s) || "y".equals(s)) return true;
            if ("false".equals(s) || "f".equals(s) || "no".equals(s) || "n".equals(s)) return false;
            try { return Integer.parseInt(s) != 0; } catch (NumberFormatException ignore) {}
        }
        return null;
    }
}
