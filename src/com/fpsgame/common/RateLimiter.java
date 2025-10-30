package com.fpsgame.common;

import java.util.concurrent.TimeUnit;

/**
 * 간단한 토큰 버킷 기반 레이트 리미터.
 *
 * <p>특징</p>
 * <ul>
 *   <li>초당 N회 허용 같은 형태를 손쉽게 구현</li>
 *   <li>스레드 안전: 동기화 블록으로 보호</li>
 *   <li>{@link #tryAcquire()} 는 현재 시점에서 1개의 토큰을 소모 가능하면 true 반환</li>
 *   <li>{@link #tryAcquire(int)} 로 여러 개의 토큰을 한 번에 요청 가능</li>
 * </ul>
 *
 * <p>사용 예</p>
 * <pre>{@code
 * RateLimiter rl = RateLimiter.perSecond(10); // 초당 10회
 * if (rl.tryAcquire()) {
 *     // 허용
 * } else {
 *     // 거부(다음 틱/주기까지 대기 또는 드랍)
 * }
 * }</pre>
 */
public final class RateLimiter {

    /** 버킷 최대 용량(토큰 수) */
    private final double capacity;

    /** 초당 보충되는 토큰 수 */
    private final double refillPerSec;

    /** 현재 토큰 수(실수 누적) */
    private double tokens;

    /** 마지막 갱신 시각(ns, 모노토닉) */
    private long lastNs;

    private RateLimiter(double capacity, double refillPerSec) {
        if (capacity <= 0.0) throw new IllegalArgumentException("capacity must be > 0");
        if (refillPerSec <= 0.0) throw new IllegalArgumentException("refillPerSec must be > 0");
        this.capacity = capacity;
        this.refillPerSec = refillPerSec;
        this.tokens = capacity; // 시작 시 풀 버킷
        this.lastNs = System.nanoTime();
    }

    /** 초당 {@code permitsPerSec} 회 허용(버킷 용량은 초당량과 동일). */
    public static RateLimiter perSecond(double permitsPerSec) {
        return new RateLimiter(permitsPerSec, permitsPerSec);
    }

    /** 커스텀 버킷(용량/초당 보충)을 구성. */
    public static RateLimiter of(double capacity, double refillPerSec) {
        return new RateLimiter(capacity, refillPerSec);
    }

    /** 현재 남은 토큰을 반환(디버그용). */
    public synchronized double availableTokens() {
        refillIfNeeded();
        return tokens;
    }

    /** 1개의 토큰을 즉시 획득 시도(성공 시 true). */
    public boolean tryAcquire() { return tryAcquire(1); }

    /**
     * n개의 토큰을 즉시 획득 시도.
     * @param n 필요한 토큰 수(양수)
     */
    public synchronized boolean tryAcquire(int n) {
        if (n <= 0) return true;
        refillIfNeeded();
        if (tokens >= n) {
            tokens -= n;
            return true;
        }
        return false;
    }

    /** 다음 획득까지 예상 대기 시간(ns)을 대략적으로 계산(0이 될 수 있음). */
    public synchronized long estimateWaitNanos(int n) {
        if (n <= 0) return 0L;
        refillIfNeeded();
        if (tokens >= n) return 0L;
        double deficit = n - tokens;
        double sec = deficit / refillPerSec;
        return (long) (sec * 1_000_000_000L);
    }

    /** 밀리초 단위 버전. */
    public long estimateWaitMillis(int n) {
        return TimeUnit.NANOSECONDS.toMillis(estimateWaitNanos(n));
    }

    // ============ 내부 구현 ============

    private void refillIfNeeded() {
        long now = System.nanoTime();
        long elapsedNs = now - lastNs;
        if (elapsedNs <= 0) return;
        lastNs = now;

        double elapsedSec = elapsedNs / 1_000_000_000.0;
        tokens = Math.min(capacity, tokens + elapsedSec * refillPerSec);
    }
}
