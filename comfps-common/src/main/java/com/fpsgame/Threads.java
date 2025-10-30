package com.fpsgame.common;

import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 스레드/스레드풀 유틸리티 모음.
 * <p>
 * - 명명 가능한 ThreadFactory (데몬/비데몬 지정)
 * - 고정 스레드풀 생성 헬퍼
 * - 그레이스풀 셧다운(shutdown → await → shutdownNow → await)
 */
public final class Threads {

    private Threads() {}

    /* ------------------------------------------------------------
     * ThreadFactory
     * ------------------------------------------------------------ */
    public static final class NamedThreadFactory implements ThreadFactory {
        private final String baseName;
        private final boolean daemon;
        private final ThreadGroup group;
        private final AtomicInteger idx = new AtomicInteger(1);
        private final Thread.UncaughtExceptionHandler handler;

        /**
         * @param baseName 스레드 이름 접두사 (예: "net-rx")
         * @param daemon   데몬 스레드 여부
         */
        public NamedThreadFactory(String baseName, boolean daemon) {
            this(baseName, daemon, null);
        }

        /**
         * @param baseName 스레드 이름 접두사
         * @param daemon   데몬 스레드 여부
         * @param handler  UncaughtExceptionHandler (null 허용)
         */
        public NamedThreadFactory(String baseName, boolean daemon, Thread.UncaughtExceptionHandler handler) {
            this.baseName = Objects.requireNonNull(baseName, "baseName");
            this.daemon = daemon;
            this.group = Thread.currentThread().getThreadGroup();
            this.handler = handler;
        }

        @Override
        public Thread newThread(Runnable r) {
            String name = baseName + "-" + idx.getAndIncrement();
            Thread t = new Thread(group, r, name, 0);
            t.setDaemon(daemon);
            if (handler != null) t.setUncaughtExceptionHandler(handler);
            return t;
        }
    }

    /* ------------------------------------------------------------
     * Executors helpers
     * ------------------------------------------------------------ */

    /**
     * 고정 크기 스레드풀을 생성한다.
     * @param baseName 스레드 이름 접두사
     * @param nThreads 스레드 수(>=1)
     * @param daemon   데몬 여부
     */
    public static ExecutorService newFixedPool(String baseName, int nThreads, boolean daemon) {
        int n = Math.max(1, nThreads);
        return Executors.newFixedThreadPool(n, new NamedThreadFactory(baseName, daemon));
    }

    /**
     * 캐시형 스레드풀 생성(필요 시). 현재 프로젝트에서는 주로 newFixedPool 사용.
     */
    public static ExecutorService newCachedPool(String baseName, boolean daemon) {
        return Executors.newCachedThreadPool(new NamedThreadFactory(baseName, daemon));
    }

    /* ------------------------------------------------------------
     * Graceful shutdown
     * ------------------------------------------------------------ */

    /**
     * 스레드풀을 우아하게 종료한다.
     * <ol>
     *     <li>shutdown()</li>
     *     <li>timeoutMs까지 awaitTermination</li>
     *     <li>미종료 시 shutdownNow()</li>
     *     <li>forceTimeoutMs까지 재대기</li>
     * </ol>
     */
    public static void shutdownGracefully(ExecutorService pool, long timeoutMs, long forceTimeoutMs) {
        if (pool == null) return;
        pool.shutdown();
        try {
            if (!pool.awaitTermination(Math.max(0, timeoutMs), TimeUnit.MILLISECONDS)) {
                pool.shutdownNow();
                pool.awaitTermination(Math.max(0, forceTimeoutMs), TimeUnit.MILLISECONDS);
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            pool.shutdownNow();
        }
    }

    /* ------------------------------------------------------------
     * 기타 유틸
     * ------------------------------------------------------------ */

    /** 조용히 슬립(InterruptedException 발생 시 인터럽트 플래그 복원) */
    public static void sleepQuietly(long millis) {
        try {
            Thread.sleep(Math.max(0, millis));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
