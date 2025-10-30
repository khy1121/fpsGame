package com.fpsgame.server;

/**
 * 서버 권위 스냅샷 틱 루프
 * - 고정 주파수(Hz)로 PlayerServerRouter#tickAndBroadcast 호출
 * - 간단한 캐치업 보정(최대 N회 보정 틱 수행)
 * - 데몬 스레드로 동작; start()/close() 제공
 */
public final class NetTickLoop implements Runnable, AutoCloseable {

    /** 라우터/컨텍스트 */
    private final PlayerServerRouter router;
    private final DefaultServerRouter.ServerContext ctx;

    /** 주파수(Hz)와 고정 델타시간(초) */
    private final double hz;
    private final float dtSec;

    /** 루프 제어 */
    private volatile boolean running = false;
    private Thread thread;

    /** 루프 내 최대 캐치업 틱 수(무한 반복 방지) */
    private final int maxCatchUpTicks;

    /**
     * @param router 스냅샷 브로드캐스터
     * @param ctx    브로드캐스트/로그 컨텍스트
     * @param hz     목표 주파수(예: 20.0). 1 이상 권장
     */
    public NetTickLoop(PlayerServerRouter router, DefaultServerRouter.ServerContext ctx, double hz) {
        if (router == null) throw new IllegalArgumentException("router");
        if (ctx == null) throw new IllegalArgumentException("ctx");
        if (hz <= 0.1) throw new IllegalArgumentException("hz must be > 0.1");
        this.router = router;
        this.ctx = ctx;
        this.hz = hz;
        this.dtSec = (float) (1.0 / hz);
        this.maxCatchUpTicks = 5; // 프레임당 최대 5틱 보정
    }

    /** 시작(이미 시작된 경우 무시) */
    public synchronized void start() {
        if (running) return;
        running = true;
        thread = new Thread(this, "NetTickLoop-" + (int) Math.round(hz) + "Hz");
        thread.setDaemon(true);
        thread.start();
        ctx.log("NetTickLoop started at " + hz + " Hz");
    }

    /** 종료(정상 종료 대기) */
    @Override
    public synchronized void close() {
        running = false;
        Thread t = thread;
        if (t != null) {
            try { t.join(2000); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
            thread = null;
            ctx.log("NetTickLoop stopped");
        }
    }

    @Override
    public void run() {
        final long nanosPerTick = (long) Math.max(1, (1_000_000_000L / hz));
        long next = System.nanoTime();

        while (running) {
            long now = System.nanoTime();
            long sleepNs = next - now;

            if (sleepNs > 0) {
                // 다음 목표까지 슬립
                sleepNanos(sleepNs);
                continue;
            }

            // 지연 발생: 하나 이상 틱 수행(최대 보정 틱 수 제한)
            int performed = 0;
            while (running && now >= next && performed < maxCatchUpTicks) {
                tickOnce();
                next += nanosPerTick;
                performed++;
                now = System.nanoTime();
            }

            // 과도한 지연이면 시간 맞춤
            if (now - next > nanosPerTick * maxCatchUpTicks) {
                next = now + nanosPerTick;
            }
        }
    }

    /** 한 틱 수행(예외는 로그 후 루프 유지) */
    private void tickOnce() {
        try {
            router.tickAndBroadcast(ctx, dtSec);
        } catch (Throwable t) {
            try { ctx.log("NetTickLoop tick error: " + t); } catch (Throwable ignored) {}
        }
    }

    /** 나노 단위 슬립; 인터럽트 시 플래그 복구 */
    private static void sleepNanos(long ns) {
        if (ns <= 0) return;
        long ms = ns / 1_000_000L;
        int extraNs = (int) (ns % 1_000_000L);
        try {
            if (ms > 0) Thread.sleep(ms, extraNs);
            else Thread.sleep(0, Math.max(0, extraNs));
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}

