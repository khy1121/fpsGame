package com.fpsgame.common;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 * CooldownTimer 단위 테스트
 */
public class CooldownTimerTest {

    // 초기 상태 검증: 생성 직후에는 즉시 사용 가능(ready)이며 남은 시간이 0이어야 한다
    @Test
    void testInitialState() {
        CooldownTimer timer = new CooldownTimer();
        assertTrue(timer.isReady());
        assertEquals(Duration.ZERO, timer.remaining());
    }

    // start로 쿨다운 시작 시 ready=false가 되고 남은 시간이 0보다 커야 한다
    @Test
    void testStartCooldown() {
        CooldownTimer timer = new CooldownTimer();
        timer.start(Duration.ofMillis(1000));

        assertFalse(timer.isReady());
        assertTrue(timer.remaining().toMillis() > 0);
    }

    // 시간 경과에 따라 남은 시간이 감소하고 지정 시간 이후에는 ready=true가 된다
    @Test
    void testCooldownProgression() throws InterruptedException {
        CooldownTimer timer = new CooldownTimer();
        timer.start(Duration.ofMillis(500));

        assertFalse(timer.isReady());
        long remainingMs = timer.remaining().toMillis();
        assertTrue(remainingMs >= 400 && remainingMs <= 500);

        Thread.sleep(600);

        assertTrue(timer.isReady());
    }

    // forceReady() 호출 시 즉시 쿨다운이 완료되고 남은 시간이 0으로 고정된다
    @Test
    void testForceReady() {
        CooldownTimer timer = new CooldownTimer();
        timer.start(Duration.ofMillis(10000));

        assertFalse(timer.isReady());
        
        timer.forceReady();
        
        assertTrue(timer.isReady());
        assertEquals(Duration.ZERO, timer.remaining());
    }

    // 0ms 쿨다운은 즉시 완료 상태여야 한다
    @Test
    void testZeroDuration() {
        CooldownTimer timer = new CooldownTimer();
        timer.start(Duration.ZERO);

        assertTrue(timer.isReady());
        assertEquals(Duration.ZERO, timer.remaining());
    }

    // 음수(duration<0)로 시작하더라도 방어적으로 완료 상태로 간주한다
    @Test
    void testNegativeDuration() {
        CooldownTimer timer = new CooldownTimer();
        timer.start(Duration.ofMillis(-100));

        assertTrue(timer.isReady());
    }

    // setDuration으로 남은 시간을 갱신할 수 있으며 내부 duration 필드가 정상적으로 유지된다
    @Test
    void testSetDuration() {
        CooldownTimer timer = new CooldownTimer();
        timer.start(Duration.ofMillis(1000));

        assertFalse(timer.isReady());
        
        timer.setDuration(Duration.ofMillis(100));
        
        assertNotNull(timer.getDuration());
    }

    // progress01()은 0..1 범위의 진행률을 반환해야 한다(시작 직후엔 0에 가깝다)
    @Test
    void testProgressCalculation() {
        CooldownTimer timer = new CooldownTimer();
        timer.start(Duration.ofMillis(1000));

        float progress = timer.progress01();
        assertTrue(progress >= 0.0f && progress <= 0.1f);
    }

    // getDuration()은 start에 전달된 duration 값을 그대로 반환해야 한다
    @Test
    void testGetDuration() {
        CooldownTimer timer = new CooldownTimer();
        Duration expected = Duration.ofMillis(1500);
        timer.start(expected);

        Duration actual = timer.getDuration();
        assertEquals(expected.toMillis(), actual.toMillis());
    }

    // 매우 짧은 쿨다운은 짧은 대기 후 완료 상태가 되어야 한다
    @Test
    void testVeryShortCooldown() throws InterruptedException {
        CooldownTimer timer = new CooldownTimer();
        timer.start(Duration.ofMillis(10));

        Thread.sleep(50);

        assertTrue(timer.isReady());
    }

    // 여러 번 start를 호출해도 마지막 설정이 유효하며 즉시 완료되지는 않는다
    @Test
    void testMultipleStarts() {
        CooldownTimer timer = new CooldownTimer();
        timer.start(Duration.ofMillis(1000));

        assertFalse(timer.isReady());
        
        timer.start(Duration.ofMillis(500));
        
        assertFalse(timer.isReady());
    }

    // 멀티스레드 접근에서도 NPE 등 예외 없이 안전하게 동작해야 한다
    @Test
    void testConcurrentAccess() throws InterruptedException {
        CooldownTimer timer = new CooldownTimer();
        timer.start(Duration.ofMillis(1000));

        Thread t1 = new Thread(() -> {
            for (int i = 0; i < 100; i++) {
                timer.isReady();
                timer.remaining();
            }
        });

        Thread t2 = new Thread(() -> {
            for (int i = 0; i < 100; i++) {
                timer.isReady();
                timer.progress01();
            }
        });

        t1.start();
        t2.start();
        t1.join();
        t2.join();

        assertNotNull(timer.remaining());
    }
}
