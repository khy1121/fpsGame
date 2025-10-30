package com.fpsgame.common;

import java.time.Duration;

/**
 * Cooldown (reuse delay) utility.
 * - Use for ability reuse logic (Primary/E/Q)
 * - Start with start(Duration), query with isReady()/remaining()
 * - Uses System.nanoTime() for drift-resistant timing
 */
public final class CooldownTimer {

    /** Total cooldown length (ns). */
    private long cooldownNs;

    /** Start time (ns). 0 means inactive/not started. */
    private long startNs;

    /** Force-ready flag to make remaining time zero. */
    private boolean forcedReady;

    /** Default constructor: no cooldown. */
    public CooldownTimer() {}

    /** Start cooldown. 0 or negative means ready immediately. */
    public synchronized void start(Duration d) {
        long ns = d == null ? 0L : Math.max(0L, d.toNanos());
        this.cooldownNs = ns;
        this.startNs = (ns == 0L) ? 0L : System.nanoTime();
        this.forcedReady = (ns == 0L);
    }

    /** Change cooldown length while keeping remaining proportion. */
    public synchronized void setDuration(Duration d) {
        long ns = d == null ? 0L : Math.max(0L, d.toNanos());
        long rem = remainingNanosLocked();
        this.cooldownNs = ns;
        if (ns == 0L) {
            // Ready immediately
            this.startNs = 0L;
            this.forcedReady = true;
        } else if (rem > ns) {
            // If remaining is longer than new total, clamp to new total
            this.startNs = System.nanoTime();
        } else {
            // Keep remaining ratio by shifting start time
            this.startNs = System.nanoTime() - (ns - rem);
        }
    }

    /** Force cooldown to ready state immediately. */
    public synchronized void forceReady() {
        this.forcedReady = true;
        this.startNs = 0L;
    }

    /** Whether ready (remaining time <= 0). */
    public synchronized boolean isReady() {
        return forcedReady || remainingNanosLocked() <= 0L;
    }

    /** Remaining time as Duration (or Duration.ZERO if ready). */
    public synchronized Duration remaining() {
        long ns = remainingNanosLocked();
        if (ns <= 0L) return Duration.ZERO;
        return Duration.ofNanos(ns);
    }

    /** Progress ratio (0.0~1.0). Ready=1.0, before start=0.0. */
    public synchronized float progress01() {
        if (cooldownNs <= 0L) return 1f;
        if (forcedReady) return 1f;
        if (startNs == 0L) return 0f;
        long now = System.nanoTime();
        long passed = Math.max(0L, now - startNs);
        return Math.max(0f, Math.min(1f, (float) passed / (float) cooldownNs));
    }

    /** Current total cooldown length. */
    public synchronized Duration getDuration() {
        return Duration.ofNanos(cooldownNs);
    }

    // Internal: compute remaining (ns). Callers hold lock.
    private long remainingNanosLocked() {
        if (forcedReady || cooldownNs <= 0L || startNs == 0L) return 0L;
        long now = System.nanoTime();
        long end = startNs + cooldownNs;
        return end - now;
    }

    @Override
    public synchronized String toString() {
        Duration rem = remaining();
        return "CooldownTimer{ready=" + isReady() + ", remaining=" + rem.toMillis() + "ms}";
    }

    // Simple demo
    public static void main(String[] args) throws InterruptedException {
        CooldownTimer cd = new CooldownTimer();
        cd.start(Duration.ofMillis(200));
        while (!cd.isReady()) {
            System.out.println("progress=" + cd.progress01() + " rem=" + cd.remaining().toMillis() + "ms");
            Thread.sleep(50);
        }
        System.out.println("ready!");
    }
}

