package com.fpsgame.client.model;

import com.fpsgame.client.NetClient;
import com.fpsgame.common.PlayerNet;
import com.fpsgame.common.Protocol;

import java.io.Closeable;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 클라이언트 입력을 주기적으로 수집하여 서버로 전송하는 경량 펌프.
 *
 * <p>특징</p>
 * <ul>
 *   <li>고정 주기(Hz)로 {@link StateProvider}에서 현재 입력 상태를 읽어
 *       {@link PlayerNet.Input} 페이로드를 생성 후 {@link Protocol.Opcode#INPUT} 프레임으로 송신</li>
 *   <li>시퀀스 번호(seq)를 자동 증가시켜 서버 스냅샷 보정과 매칭 가능</li>
 *   <li>UI 스레드(EDT)와 독립된 전용 송신 스레드를 사용</li>
 *   <li>네트워크 끊김 상태에서는 자동 대기하며, 재연결되면 즉시 재개</li>
 * </ul>
 *
 * <p>사용 예</p>
 * <pre>
 *   ClientInputPump.StateProvider provider = new ClientInputPump.StateProvider() {
 *       public int   buttons() { return btnMask; }
 *       public float aimX()    { return crosshairX; }
 *       public float aimY()    { return crosshairY; }
 *       public float moveX()   { return moveAxisX; }
 *       public float moveY()   { return moveAxisY; }
 *   };
 *   ClientInputPump pump = new ClientInputPump(netClient, provider, 30.0); // 30Hz
 *   pump.start();
 *   ...
 *   pump.close(); // 종료
 * </pre>
 */
public final class ClientInputPump implements Closeable {

    /** 입력 상태를 제공하는 인터페이스(호출 측 UI/게임캔버스 등에서 구현). */
    public interface StateProvider {
        /** 버튼 비트마스크 (예: {@link PlayerNet.Btn} 조합) */
        int buttons();
        /** 조준 또는 마우스 정규화 X */
        float aimX();
        /** 조준 또는 마우스 정규화 Y */
        float aimY();
        /** 이동 입력 X(-1..1) */
        float moveX();
        /** 이동 입력 Y(-1..1) */
        float moveY();
    }

    private final NetClient net;
    private final StateProvider provider;
    private final long periodNanos; // 틱 주기(ns)
    private final AtomicBoolean running = new AtomicBoolean(false);

    private Thread thread;
    private volatile int seq = 0;

    /**
     * @param net       연결된 {@link NetClient}
     * @param provider  현재 입력 상태를 알려줄 공급자
     * @param hz        전송 주파수(Hz). 10~120 권장.
     */
    public ClientInputPump(NetClient net, StateProvider provider, double hz) {
        this.net = Objects.requireNonNull(net, "net");
        this.provider = Objects.requireNonNull(provider, "provider");
        if (hz <= 0) throw new IllegalArgumentException("hz must be > 0");
        this.periodNanos = (long) (1_000_000_000L / hz);
    }

    /** 펌프 스레드를 시작(이미 시작되어 있으면 무시). */
    public synchronized void start() {
        if (running.get()) return;
        running.set(true);
        thread = new Thread(this::loop, "ClientInputPump");
        thread.setDaemon(true);
        thread.start();
    }

    /** 펌프를 중지하고 스레드를 종료. */
    @Override
    public synchronized void close() {
        if (!running.getAndSet(false)) return;
        if (thread != null) {
            try { thread.join(1000); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
        }
        thread = null;
    }

    // =================== 내부 루프 ===================

    private void loop() {
        long next = System.nanoTime();
        while (running.get()) {
            long now = System.nanoTime();
            if (now >= next) {
                tickSend();
                next += periodNanos;
                // 드리프트가 크게 쌓였을 때 보정
                if (now - next > 5 * periodNanos) {
                    next = now + periodNanos;
                }
            } else {
                try {
                    long sleepNs = Math.max(100_000L, next - now); // 최소 0.1ms
                    Thread.sleep(sleepNs / 1_000_000L, (int) (sleepNs % 1_000_000L));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    /** 한 틱 분량의 입력을 수집하여 서버로 전송. */
    private void tickSend() {
        // 네트워크가 연결되어 있지 않으면 건너뜀
        if (net == null || !net.isConnected()) return;

        // 공급자로부터 현재 상태를 읽는다.
        int buttons   = safeButtons(provider);
        float aimX    = safe(provider::aimX);
        float aimY    = safe(provider::aimY);
        float moveX   = clampNegPos1(safe(provider::moveX));
        float moveY   = clampNegPos1(safe(provider::moveY));

        // 입력 패킷 생성(버전=1, 시퀀스 증가)
        PlayerNet.Input in = PlayerNet.input(nextSeq(), buttons, aimX, aimY, moveX, moveY);
        byte[] payload = PlayerNet.buildInputPayload(in);

        // 전송(동기화는 NetClient 내부에서 out 스트림 기준으로 보장)
        try {
            net.send(Protocol.Opcode.INPUT, payload);
        } catch (IOException ioe) {
            // 네트워크 오류는 조용히 무시(다음 틱에서 재시도)
        }
    }

    private int nextSeq() {
        int s = seq + 1;
        if (s == Integer.MAX_VALUE) s = 0;
        seq = s;
        return s;
    }

    // =================== 유틸 ===================

    private static float clampNegPos1(float v) {
        if (v < -1f) return -1f;
        if (v > 1f) return 1f;
        return v;
    }

    private static int safeButtons(StateProvider p) {
        try { return p.buttons(); } catch (Throwable t) { return 0; }
    }

    private interface FloatSupplier { float get(); }
    private static float safe(FloatSupplier s) {
        try { return s.get(); } catch (Throwable t) { return 0f; }
    }
}
