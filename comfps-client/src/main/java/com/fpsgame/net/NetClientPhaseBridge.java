package com.fpsgame.client.net;

import com.fpsgame.client.model.ClientPhaseBus;
import com.fpsgame.common.Protocol;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;

/**
 * NetClientPhaseBridge
 * ------------------------------------------------------------
 * 당신의 NetClient 수신부에 {@link PhaseFrameAdapter}를 "비침투적"으로 연결하는 브리지.
 *
 * 목적
 * - NetClient 구현이 서로 달라도(리스너 타입/메서드명이 다름) 최대한 호환되게 붙는다.
 * - 붙인 뒤에는 서버에서 오는 PHASE_UPDATE / COUNTDOWN / ROUND_RESULT / READY_TOGGLE
 *   프레임이 자동으로 {@link ClientPhaseBus}로 브로드캐스트된다.
 *
 * 사용 방법 A) NetClient가 addListener(...)를 지원하는 경우
 *   NetClientPhaseBridge b = NetClientPhaseBridge.attach(netClient, ClientPhaseBus.get());
 *   // 종료 시
 *   b.detach();
 *
 * 사용 방법 B) 직접 수신부에서 호출할 수 있는 경우
 *   NetClientPhaseBridge b = new NetClientPhaseBridge(ClientPhaseBus.get());
 *   // NetClient가 onFrame(op, payload) 시점에:
 *   b.handle(op, payload); // 내부에서 PhaseFrameAdapter 처리
 *
 * 구현 노트
 * - 리스너 객체는 onFrame(int, byte[]), onFrame(byte, byte[]), onPacket(...), onFrameReceived(...)
 *   등의 여러 이름/시그니처를 모두 제공하므로 NetClient가 어느 것을 호출하더라도 동작한다.
 * - 자동 부착은 다음 메서드들을 순차 시도한다:
 *     addListener(Object), addListener(NetClient.Listener),
 *     addFrameListener(Object), setListener(Object), setRouter(Object)
 * - 분리 시에는 removeListener(...) 또는 setListener(null)/setRouter(null)을 시도한다.
 */
public final class NetClientPhaseBridge {

    private final PhaseFrameAdapter adapter;
    private final Object listenerProxy; // NetClient 가 호출할 리스너 객체
    private Object netClient;           // 부착된 클라이언트 (detach에 사용)
    private Method removeMethod;        // removeListener 등 분리 시 사용할 메서드
    private boolean attached;

    /** 버스 지정 생성자 */
    public NetClientPhaseBridge(ClientPhaseBus bus) {
        this.adapter = new PhaseFrameAdapter(Objects.requireNonNull(bus, "bus"));
        this.listenerProxy = new MultiSigListenerProxy(this.adapter);
    }

    /** 디폴트 버스(ClientPhaseBus.get()) 사용 */
    public NetClientPhaseBridge() {
        this(ClientPhaseBus.get());
    }

    /** 수동 처리: NetClient의 수신 스레드에서 직접 호출 가능 */
    public void handle(int opcode, byte[] payload) {
        adapter.handle(opcode, payload);
    }

    /** 수동 처리: byte opcode 버전 */
    public void handle(byte opcode, byte[] payload) {
        adapter.handle(opcode, payload);
    }

    // -------------------- 자동 부착/해제 --------------------

    /**
     * NetClient 인스턴스에 최대한 호환되게 리스너를 부착한다.
     * 성공 시 이 객체를 반환(체이닝 가능), 실패 시 IllegalStateException.
     */
    public static NetClientPhaseBridge attach(Object netClient, ClientPhaseBus bus) {
        NetClientPhaseBridge b = new NetClientPhaseBridge(bus);
        if (!b.tryAttach(netClient)) {
            throw new IllegalStateException("NetClientPhaseBridge: attach failed for " + netClient);
        }
        return b;
    }

    /** 디폴트 버스로 부착 */
    public static NetClientPhaseBridge attach(Object netClient) {
        return attach(netClient, ClientPhaseBus.get());
    }

    /** 분리(중복 호출 안전) */
    public synchronized void detach() {
        if (!attached || netClient == null) return;
        try {
            if (removeMethod != null) {
                // removeListener(Object) 형태가 있는 경우
                removeMethod.invoke(netClient, listenerProxy);
            } else {
                // setListener(null) 또는 setRouter(null) 시도
                safeNullSetter(netClient, "setListener");
                safeNullSetter(netClient, "setRouter");
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            // 분리에 실패해도 안정성에는 영향 없음
            System.err.println("Failed to detach listener: " + e.getMessage());
        } finally {
            attached = false;
            netClient = null;
            removeMethod = null;
        }
    }

    // -------------------- 내부 구현 --------------------

    private synchronized boolean tryAttach(Object nc) {
        if (attached) return true;
        if (nc == null) return false;
        this.netClient = nc;

        // 1) addListener(Object/Listener) 우선
        if (tryAdd(nc, "addListener")) return success(nc, "removeListener");

        // 2) addFrameListener(Object) 시도
        if (tryAdd(nc, "addFrameListener")) return success(nc, "removeFrameListener");

    // 3) setListener(Object) 시도 (remove는 setListener(null)로)
    if (trySet(nc, "setListener")) return successWithoutRemove();

    // 4) setRouter(Object) 시도 (remove는 setRouter(null)로)
    if (trySet(nc, "setRouter")) return successWithoutRemove();

        // 실패
        this.netClient = null;
        return false;
    }

    private boolean tryAdd(Object nc, String methodName) {
        try {
            Method m = findMethod(nc.getClass(), methodName, Object.class);
            if (m != null) {
                m.invoke(nc, listenerProxy);
                return true;
            }
            // 인터페이스형 매개변수(예: NetClient.Listener)를 요구할 수도 있으므로,
            // 파라미터가 1개인 any-Object형 메서드를 찾아 시도한다.
            for (Method mm : nc.getClass().getMethods()) {
                if (!mm.getName().equals(methodName)) continue;
                if (mm.getParameterCount() == 1) {
                    if (mm.getParameterTypes()[0].isInstance(listenerProxy) ||
                        mm.getParameterTypes()[0].isAssignableFrom(listenerProxy.getClass())) {
                        mm.invoke(nc, listenerProxy);
                        return true;
                    }
                    // 파라미터가 인터페이스이고 listenerProxy가 해당 인터페이스를 구현하지 않아도,
                    // Java는 동적 프록시가 아니면 캐스팅 불가 → 건너뛴다.
                }
            }
            return false;
        } catch (IllegalAccessException | InvocationTargetException | SecurityException e) {
            System.err.println("Failed to invoke method: " + e.getMessage());
            return false;
        }
    }

    private boolean trySet(Object nc, String methodName) {
        try {
            Method m = findMethod(nc.getClass(), methodName, Object.class);
            if (m != null) {
                m.invoke(nc, listenerProxy);
                return true;
            }
            return false;
        } catch (IllegalAccessException | InvocationTargetException | SecurityException e) {
            System.err.println("Failed to set method: " + e.getMessage());
            return false;
        }
    }

    private boolean success(Object nc, String removeName) {
        this.attached = true;
        // 분리용 remove 메서드를 캐시(있으면)
        this.removeMethod = findMethod(nc.getClass(), removeName, Object.class);
        return true;
    }

    private boolean successWithoutRemove() {
        this.attached = true;
        this.removeMethod = null; // setListener(null)/setRouter(null) 경로 사용
        return true;
    }

    private static Method findMethod(Class<?> cls, String name, Class<?> paramType) {
        try {
            return cls.getMethod(name, paramType);
        } catch (NoSuchMethodException e) {
            // 시그니처가 정확히 일치하지 않을 수 있음 → null
            return null;
        }
    }

    private static void safeNullSetter(Object target, String method) {
        try {
            Method m = target.getClass().getMethod(method, Object.class);
            m.invoke(target, new Object[]{ null });
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            System.err.println("Failed to set null: " + e.getMessage());
        }
    }

    // ----------------------------------------------------------------------
    // 내부 리스너 프록시: NetClient가 어떤 시그니처로 콜백하든 최대한 수신해 Adapter로 전달
    // ----------------------------------------------------------------------
    @SuppressWarnings("unused")
    private static final class MultiSigListenerProxy {
        private final PhaseFrameAdapter adapter;
        MultiSigListenerProxy(PhaseFrameAdapter adapter) { this.adapter = adapter; }

        // 가장 일반적인 시그니처
        public void onFrame(int opcode, byte[] payload) { adapter.handle(opcode, payload); }
        public void onFrame(byte opcode, byte[] payload) { adapter.handle(opcode, payload); }

        // 다른 네이밍들
        public void onPacket(int opcode, byte[] payload) { adapter.handle(opcode, payload); }
        public void onPacket(byte opcode, byte[] payload) { adapter.handle(opcode, payload); }
        public void onFrameReceived(int opcode, byte[] payload) { adapter.handle(opcode, payload); }
        public void onFrameReceived(byte opcode, byte[] payload) { adapter.handle(opcode, payload); }

        // 혹시 (opcode, buffer, offset, len) 같은 형태를 쓰는 구현을 위해 약식 제공
        public void onFrame(int opcode, byte[] payload, int off, int len) {
            if (payload == null) { adapter.handle(opcode, null); return; }
            if (off == 0 && len == payload.length) { adapter.handle(opcode, payload); return; }
            byte[] slice = new byte[Math.max(0, Math.min(len, payload.length - Math.max(0, off)))];
            if (slice.length > 0) System.arraycopy(payload, Math.max(0, off), slice, 0, slice.length);
            adapter.handle(opcode, slice);
        }

        // PING/PONG 같은 기타 이벤트를 무시하더라도 NetClient가 호출만 하도록 더미 메서드 마련
        public void onOpen() {}
        public void onClose() {}
        public void onError(Throwable t) {
            System.err.println("[NetClientPhaseBridge] onError: " + t);
        }
    }

    // ---------------------- 데모 ----------------------
    // ---------------------- 데모 ----------------------
    public static void main(String[] args) {
        NetClientPhaseBridge b = new NetClientPhaseBridge(ClientPhaseBus.get());
        // PHASE_UPDATE
        byte[] phase = DemoPayloads.phaseUpdate("LOBBY", 0, 0, 0, 5, 5, -1);
        b.handle(Protocol.Opcode.PHASE_UPDATE, phase);
        // COUNTDOWN
        b.handle(Protocol.Opcode.COUNTDOWN, DemoPayloads.countdown(3));
        // ROUND_RESULT
        b.handle(Protocol.Opcode.ROUND_RESULT, DemoPayloads.roundResult("RED", "TEAM_WIPE"));
        // READY_TOGGLE
        b.handle(Protocol.Opcode.READY_TOGGLE, DemoPayloads.readyToggle(1001, true));
    }

}
