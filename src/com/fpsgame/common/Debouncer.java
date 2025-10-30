package com.fpsgame.common;

import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 짧은 시간에 연속 발생하는 이벤트를 일정 시간 모아 한 번만 실행하는 디바운서
 * - submit 호출 시 타이머를 리셋하여 마지막 호출만 실행되도록 함
 * - AutoCloseable 지원으로 종료 시 타이머 정리 가능
 */
public final class Debouncer implements AutoCloseable {

    /** 지연 시간(ms) */
    private final long delayMillis;
    /** 데몬 타이머(프로세스 종료를 막지 않음) */
    private final Timer timer;
    /** 현재 예약 작업(취소/교체) */
    private final AtomicReference<TimerTask> scheduled = new AtomicReference<>();

    /** @param delayMillis 대기 시간(ms). 대기 중 재호출 시 타이머 리셋 */
    public Debouncer(long delayMillis) {
        if (delayMillis < 0) throw new IllegalArgumentException("delayMillis must be >= 0");
        this.delayMillis = delayMillis;
        this.timer = new Timer("Debouncer", true);
    }

    /** 작업을 제출(기존 예약이 있으면 취소하고 다시 예약) */
    public void submit(Runnable action) {
        Objects.requireNonNull(action, "action");
        // 기존 예약 취소
        TimerTask prev = scheduled.getAndSet(null);
        if (prev != null) prev.cancel();
        // 새 작업 예약
        TimerTask task = new TimerTask() {
            @Override public void run() {
                // 실행 직전 자신을 비우기
                scheduled.compareAndSet(this, null);
                try { action.run(); } catch (Throwable ignored) {}
            }
        };
        scheduled.set(task);
        try { timer.schedule(task, delayMillis); } catch (IllegalStateException closed) { /* 이미 close() 된 경우 무시 */ }
    }

    /** 예약 작업을 취소하고 비움 */
    public void cancel() {
        TimerTask t = scheduled.getAndSet(null);
        if (t != null) t.cancel();
    }

    /** 타이머 스레드를 종료하고 예약 작업을 취소 */
    @Override
    public void close() {
        cancel();
        try { timer.cancel(); } catch (Throwable ignored) {}
    }
}

