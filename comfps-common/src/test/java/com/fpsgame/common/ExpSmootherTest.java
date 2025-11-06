package com.fpsgame.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 * ExpSmoother 단위 테스트
 */
public class ExpSmootherTest {

    // 초깃값: 초기화 전 상태와 기본 출력값(0.0)을 확인한다
    @Test
    void testInitialState() {
        ExpSmoother smoother = new ExpSmoother(0.5);
        assertFalse(smoother.isInitialized());
        assertEquals(0.0, smoother.get(), 0.0001);
    }

    // 첫 샘플 입력 시 내부 상태가 초기화되고 그 값이 그대로 반영된다
    @Test
    void testFirstSample() {
        ExpSmoother smoother = new ExpSmoother(0.5);
        double result = smoother.add(10.0);
        assertTrue(smoother.isInitialized());
        assertEquals(10.0, result, 0.0001);
        assertEquals(10.0, smoother.get(), 0.0001);
    }

    // 일반적 평활: y = y + alpha*(x - y) 공식을 통해 기대값을 검증한다
    @Test
    void testSmoothing() {
        ExpSmoother smoother = new ExpSmoother(0.5);
        smoother.add(10.0);
        double result = smoother.add(20.0);
        // y = 10.0 + 0.5 * (20.0 - 10.0) = 15.0
        assertEquals(15.0, result, 0.0001);
        assertEquals(15.0, smoother.get(), 0.0001);
    }

    // 작은 alpha(0.1)에서는 변화 폭이 작아진다
    @Test
    void testLowAlpha() {
        ExpSmoother smoother = new ExpSmoother(0.1);
        smoother.add(10.0);
        double result = smoother.add(20.0);
        // y = 10.0 + 0.1 * (20.0 - 10.0) = 11.0
        assertEquals(11.0, result, 0.0001);
    }

    // 큰 alpha(0.9)에서는 새 입력값에 가깝게 빠르게 수렴한다
    @Test
    void testHighAlpha() {
        ExpSmoother smoother = new ExpSmoother(0.9);
        smoother.add(10.0);
        double result = smoother.add(20.0);
        // y = 10.0 + 0.9 * (20.0 - 10.0) = 19.0
        assertEquals(19.0, result, 0.0001);
    }

    // resetTo로 내부 현재값을 임의로 재설정할 수 있다
    @Test
    void testResetTo() {
        ExpSmoother smoother = new ExpSmoother(0.5);
        smoother.add(10.0);
        smoother.resetTo(25.0);
        assertTrue(smoother.isInitialized());
        assertEquals(25.0, smoother.get(), 0.0001);
    }

    // clear 후 첫 입력은 다시 초기화 동작을 수행해야 한다
    @Test
    void testResetAfterReset() {
        ExpSmoother smoother = new ExpSmoother(0.5);
        smoother.add(10.0);
        smoother.clear();
        assertFalse(smoother.isInitialized());
        double result = smoother.add(30.0);
        assertTrue(smoother.isInitialized());
        assertEquals(30.0, result, 0.0001);
    }

    // 시간상수 기반 팩토리 메서드가 유효한 객체를 반환하는지 확인한다
    @Test
    void testWithTimeConstant() {
        ExpSmoother smoother = ExpSmoother.withTimeConstant(1.0, 0.1);
        assertNotNull(smoother);
        assertFalse(smoother.isInitialized());
    }

    // 경계값 alpha=0: 이전 값 유지(변화 없음)
    @Test
    void testAlphaBoundaryZero() {
        ExpSmoother smoother = new ExpSmoother(0.0);
        smoother.add(10.0);
        double result = smoother.add(20.0);
        // alpha=0 means no change
        assertEquals(10.0, result, 0.0001);
    }

    // 경계값 alpha=1: 새 입력값으로 즉시 이동
    @Test
    void testAlphaBoundaryOne() {
        ExpSmoother smoother = new ExpSmoother(1.0);
        smoother.add(10.0);
        double result = smoother.add(20.0);
        // alpha=1 means immediate jump to new value
        assertEquals(20.0, result, 0.0001);
    }

    // 잘못된 alpha(<0) 생성자는 예외를 던져야 한다
    @Test
    void testInvalidAlphaNegative() {
        Throwable ex = assertThrows(IllegalArgumentException.class, () -> new ExpSmoother(-0.1));
        assertNotNull(ex);
    }

    // 잘못된 alpha(>1) 생성자는 예외를 던져야 한다
    @Test
    void testInvalidAlphaAboveOne() {
        Throwable ex = assertThrows(IllegalArgumentException.class, () -> new ExpSmoother(1.1));
        assertNotNull(ex);
    }

    // 시간상수 tc<=0 은 허용되지 않는다
    @Test
    void testInvalidTimeConstantZero() {
        Throwable ex = assertThrows(IllegalArgumentException.class, () -> ExpSmoother.withTimeConstant(0.0, 0.1));
        assertNotNull(ex);
    }

    // 음수 시간상수도 예외 발생
    @Test
    void testInvalidTimeConstantNegative() {
        Throwable ex = assertThrows(IllegalArgumentException.class, () -> ExpSmoother.withTimeConstant(-1.0, 0.1));
        assertNotNull(ex);
    }

    // 연속 입력에 대해 기대되는 누적 평활 결과를 검증한다
    @Test
    void testSequentialSmoothing() {
        ExpSmoother smoother = new ExpSmoother(0.3);
        smoother.add(100.0);
        smoother.add(110.0); // 100 + 0.3 * 10 = 103
        double result = smoother.add(120.0); // 103 + 0.3 * 17 = 108.1
        assertEquals(108.1, result, 0.0001);
    }

    // 노이즈가 포함된 데이터에서 출력이 입력 범위 내로 평탄화되는지 확인한다
    @Test
    void testNoisyDataSmoothing() {
        ExpSmoother smoother = new ExpSmoother(0.2);
        double[] noisyData = {10.0, 15.0, 8.0, 20.0, 12.0};
        double lastValue = 0.0;
        for (double data : noisyData) {
            lastValue = smoother.add(data);
        }
        // 최종값은 노이즈가 평탄화되어야 함
        assertTrue(lastValue > 8.0 && lastValue < 20.0);
    }
}
