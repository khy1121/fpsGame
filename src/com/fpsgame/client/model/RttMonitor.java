package com.fpsgame.client.model;

import com.fpsgame.client.NetClient;
import com.fpsgame.common.Binary;
import com.fpsgame.common.Protocol;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.LongConsumer;

/**
 * PING/PONG 기반 RTT 측정기.
 *
 * <p>역할</p>
 * <ul>
 *   <li>{@link #start(int)} 호출 시 주기적으로 PING 프레임을 송신</li>
 *   <li>서버에서 동일 페이로드로 PONG 이 돌아오면 왕복 시간(ms)을 계산</li>
 *   <li>콜백으로 최신 RTT 값을 전달</li>
 * </ul>
 *
 * <p>연동</p>
 * <pre>
 *   RttMonitor rtt = new RttMonitor(netClient, ms -> SwingUtilities.invokeLater(() -> label.setText("RTT " + ms + "ms")));
 *   rtt.start(1000);
 *   // NetClient.Listener.onFrame(...) 에서 rtt.onFrame(frame) 호출
 * </pre>
 *
 * <p>스레드 안전</p>
 * - 송수신/타이머 스레드와 EDT가 섞여도 안전하도록 최소한의 동기화만 사용
 */
public final class RttMonitor implements AutoCloseable {

    /** 측정 대상 네트 클라이언트 */
    private final NetClient net;

    /** 샘플 콜백(ms) */
    private final LongConsumer onSample;

    /** 주기 타이머(데몬) */
    private Timer timer;

    /** 현재 outstanding ping 여부(중복 측정 방지) */
    private final AtomicBoolean pingInFlight = new AtomicBoolean(false);

    /** 마지막으로 보낸 ping 시간(ns, System.nanoTime 기준) */
    private volatile long lastPingNs = 0L;

    public RttMonitor(NetClient net, LongConsumer onSample) {
        this.net = Objects.requireNonNull(net, "net");
        this.onSample = Objects.requireNonNull(onSample, "onSample");
    }

    /**
     * 주기적 PING 전송 시작.
     * @param intervalMs 전송 주기(ms, 최소 200ms 권장)
     */
    public synchronized void start(int intervalMs) {
        stopTimerIfAny();
        int period = Math.max(200, intervalMs);
        timer = new Timer("RttMonitor", true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override public void run() { pingOnce(); }
        }, period, period);
    }

    /** 즉시 PING을 한 번 전송. 전송 시도에 성공하면 true. */
    public boolean pingOnce() {
        // 이미 대기 중인 핑이 있으면 스킵(너무 잦은 중복 방지)
        if (!net.isConnected()) return false;
        if (!pingInFlight.compareAndSet(false, true)) return false;

        lastPingNs = System.nanoTime();

        try {
            // payload: [long sentNanoTime] (big-endian)
            ByteArrayOutputStream baos = new ByteArrayOutputStream(8);
            Binary.putLong(baos, lastPingNs);
            net.send(Protocol.Opcode.PING, baos.toByteArray());
            return true;
        } catch (IOException e) {
            // 실패 시 플래그 원복
            pingInFlight.set(false);
            return false;
        }
    }

    /**
     * 수신 프레임을 전달받아 RTT를 계산한다.
     * NetClient.Listener.onFrame(...) 에서 호출해야 한다.
     */
    public void onFrame(Protocol.Frame frame) {
        if (frame == null || frame.opcode != Protocol.Opcode.PONG) return;

        // 페이로드에서 보낸 시각(ns)을 복구(없으면 현재 값으로 대체)
        long sentNs;
        try {
            if (frame.payload != null && frame.payload.length >= 8) {
                sentNs = Binary.getLong(frame.payload, 0);
            } else {
                sentNs = lastPingNs; // 호환용(서버가 빈 페이로드로 응답하는 경우)
            }
        } catch (Throwable t) {
            sentNs = lastPingNs;
        }

        long now = System.nanoTime();
        long rttNs = Math.max(0L, now - sentNs);
        long rttMs = Math.max(0L, rttNs / 1_000_000L);

        // 다음 측정을 위해 플래그 해제
        pingInFlight.set(false);

        // 콜백 통지
        try {
            onSample.accept(rttMs);
        } catch (Throwable ignored) {
            // 콜백 예외는 무시
        }
    }

    /** 타이머 중단(재시작 가능). */
    public synchronized void stop() {
        stopTimerIfAny();
    }

    private void stopTimerIfAny() {
        if (timer != null) {
            try { timer.cancel(); } catch (Throwable ignored) {}
            timer = null;
        }
    }

    /** 종료(타이머 중단). */
    @Override
    public void close() {
        stop();
    }
}
