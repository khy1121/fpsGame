package com.fpsgame.common;

import java.time.Duration;

/**
 * 경과 시간 측정용 간단 스톱워치.
 * <p>
 * - {@link System#nanoTime()} 기반(시스템 시간 변경 영향 없음)
 * - start()/stop()/reset() 제공
 * - 누적 시간 측정(여러 번 start/stop 가능)
 */
public final class Stopwatch {

    /** 마지막 시작 시각(ns). 0이면 정지 상태 */
    private long startNs;

    /** 누적 경과(ns) */
    private long accNs;

    /** 스톱워치 시작(이미 실행 중이면 무시) */
    public synchronized Stopwatch start() {
        if (startNs == 0L) {
            startNs = System.nanoTime();
        }
        return this;
    }

    /** 스톱워치 정지(이미 정지 상태면 무시) */
    public synchronized Stopwatch stop() {
        if (startNs != 0L) {
            long now = System.nanoTime();
            accNs += Math.max(0L, now - startNs);
            startNs = 0L;
        }
        return this;
    }

    /** 누적/상태 초기화(정지 상태로 전환) */
    public synchronized Stopwatch reset() {
        startNs = 0L;
        accNs = 0L;
        return this;
    }

    /** 현재 실행 중인지 여부 */
    public synchronized boolean isRunning() {
        return startNs != 0L;
    }

    /** 경과 시간(ns) — 실행 중이면 현재 시각을 포함해 계산 */
    public synchronized long elapsedNanos() {
        long base = accNs;
        if (startNs != 0L) {
            long now = System.nanoTime();
            base += Math.max(0L, now - startNs);
        }
        return base;
    }

    /** 경과 시간(ms) */
    public long elapsedMillis() {
        return elapsedNanos() / 1_000_000L;
    }

    /** 경과 시간(Duration) */
    public Duration elapsed() {
        return Duration.ofNanos(elapsedNanos());
    }

    @Override
    public synchronized String toString() {
        return "Stopwatch{running=" + isRunning() + ", elapsedMs=" + elapsedMillis() + "}";
    }
}
