package com.fpsgame.common;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.LongConsumer;

/**
 * 고정 틱 루프 유틸리티.
 * <p>
 * - 초당 {@code hz} 회수로 {@link LongConsumer} 콜백을 호출
 * - 스레드 하나에서 실행되며, 시작/중지/조인을 제공
 * - 드리프트를 줄이기 위해 {@link System#nanoTime()} 기반 스케줄링
 *
 * 사용 예:
 * <pre>
 *   FixedTickRunner loop = new FixedTickRunner(30, deltaMs -> { ... });
 *   loop.start();
 *   ...
 *   loop.stopAndJoin(1000);
 * </pre>
 */
public final class FixedTickRunner implements Runnable {

    /** 최소/최대 허용 Hz (수동 방어) */
    private static final int MIN_HZ = 1;
    private static final int MAX_HZ = 240;

    private final int hz;
    private final long tickNanos;            // 틱 기간(ns)
    private final LongConsumer onTick;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread thread;

    /**
     * @param hz      초당 틱 수(1~240)
     * @param onTick  매 틱마다 호출되는 콜백(인자는 deltaMillis)
     */
    public FixedTickRunner(int hz, LongConsumer onTick) {
        if (hz < MIN_HZ || hz > MAX_HZ) {
            throw new IllegalArgumentException("hz out of range: " + hz + " (allowed " + MIN_HZ + "~" + MAX_HZ + ")");
        }
        this.hz = hz;
        this.tickNanos = 1_000_000_000L / hz;
        this.onTick = Objects.requireNonNull(onTick, "onTick");
    }

    /** 현재 Hz 반환 */
    public int getHz() { return hz; }

    /** 루프 시작(중복 호출 시 무시) */
    public synchronized void start() {
        if (running.get()) return;
        running.set(true);
        thread = new Thread(this, "fixed-tick-" + hz + "hz");
        thread.setDaemon(true);
        thread.start();
    }

    /** 중지 요청 후 지정 ms 내 조인 */
    public synchronized void stopAndJoin(long joinTimeoutMs) {
        running.set(false);
        if (thread != null) {
            try {
                thread.join(Math.max(0, joinTimeoutMs));
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            } finally {
                thread = null;
            }
        }
    }

    @Override
    public void run() {
        long next = System.nanoTime();
        long last = next;

        while (running.get()) {
            long now = System.nanoTime();
            long deltaNs = now - last;
            last = now;

            // 콜백 실행(단위: ms, 0 이하 방지)
            long deltaMs = Math.max(0L, deltaNs / 1_000_000L);
            try {
                onTick.accept(deltaMs);
            } catch (Throwable t) {
                // 콜백 예외는 루프를 죽이지 않는다(로그는 상위에서)
            }

            // 다음 틱까지 슬립
            next += tickNanos;
            long sleepNs = next - System.nanoTime();

            if (sleepNs > 0) {
                // ns 단위 대기 — 정확도가 낮으므로 남은 일부는 busy-wait로 소진
                try {
                    long ms = sleepNs / 1_000_000L;
                    int ns = (int) (sleepNs % 1_000_000L);
                    if (ms > 0) Thread.sleep(ms, ns);
                    else if (ns > 50_000) Thread.sleep(0, ns); // 짧은 나노슬립
                    // 남은 수십 마이크로초는 자연 소모
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    // 인터럽트 시 즉시 중단
                    running.set(false);
                }
            } else {
                // 오버런: 다음 기준점을 현재로 재동기화(런어웨이 방지)
                next = System.nanoTime();
            }
        }
    }
}
