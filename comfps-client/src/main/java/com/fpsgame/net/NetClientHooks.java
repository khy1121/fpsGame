package com.fpsgame.client.net;

import java.lang.reflect.Method;

/**
 * NetClientHooks
 * ------------------------------------------------------------
 * NetClient의 리스너 등록/해제를 런타임 리플렉션으로 시도하는 호환 레이어.
 * - addListener/removeListener, registerListener/unregisterListener, setListener(null) 등
 *   다양한 메서드명을 탐색해 호출한다.
 * - 컴파일타임 의존을 제거하여 시그니처 차이로 인한 컴파일 에러를 방지.
 */
public final class NetClientHooks {

    private NetClientHooks() {}

    /** 등록 성공 시 true 반환. */
    public static boolean tryAttach(Object netClient, Object listener) {
        if (netClient == null || listener == null) return false;
        // 우선순위대로 메서드 탐색
        String[] addNames = { "addListener", "registerListener", "addNetListener", "setListener" };
        for (String name : addNames) {
            Object ok = invokeBest(netClient, name, listener);
            if (ok != null) return (Boolean) ok;
        }
        return false;
    }

    /** 해제 성공 시 true 반환. setListener(null) 형태도 지원. */
    public static boolean tryDetach(Object netClient, Object listener) {
        if (netClient == null) return false;
        String[] removeNames = { "removeListener", "unregisterListener", "removeNetListener" };
        for (String name : removeNames) {
            Object ok = invokeBest(netClient, name, listener);
            if (ok != null) return (Boolean) ok;
        }
        // setListener(null) 백업 경로
        try {
            Method m = netClient.getClass().getMethod("setListener", Object.class);
            m.invoke(netClient, new Object[]{ null });
            return true;
        } catch (Throwable ignore) {
            return false;
        }
    }

    // 내부 유틸: 다양한 파라미터 시그니처를 관대하게 시도
    private static Object invokeBest(Object target, String name, Object arg) {
        Class<?> c = target.getClass();
        try {
            // 1) (Object) 한 개
            Method m = c.getMethod(name, Object.class);
            m.invoke(target, arg);
            return true;
        } catch (Throwable ignore) { /* fallthrough */ }
        try {
            // 2) 파라미터 타입 한 개짜리 아무거나
            for (Method m : c.getMethods()) {
                if (!m.getName().equals(name)) continue;
                Class<?>[] p = m.getParameterTypes();
                if (p.length == 1 && p[0].isInstance(arg)) {
                    m.invoke(target, arg);
                    return true;
                }
            }
        } catch (Throwable ignore) { /* fallthrough */ }
        return null;
    }
}
