package com.fpsgame.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 * Mathf 단위 테스트
 */
public class MathfTest {

    private static final float EPSILON = 0.0001f;

    // clamp: 최소/최대 범위로 값이 잘 제한되는지 확인
    @Test
    void testClamp() {
        assertEquals(5f, Mathf.clamp(5f, 0f, 10f), EPSILON);
        assertEquals(0f, Mathf.clamp(-5f, 0f, 10f), EPSILON);
        assertEquals(10f, Mathf.clamp(15f, 0f, 10f), EPSILON);
        assertEquals(5f, Mathf.clamp(5f, 10f, 0f), EPSILON); // min > max 케이스
    }

    // saturate: [0,1] 범위로 값이 잘 잘리는지 확인
    @Test
    void testSaturate() {
        assertEquals(0.5f, Mathf.saturate(0.5f), EPSILON);
        assertEquals(0f, Mathf.saturate(-0.5f), EPSILON);
        assertEquals(1f, Mathf.saturate(1.5f), EPSILON);
    }

    // lerp: 선형보간이 기대대로 동작하는지 확인
    @Test
    void testLerp() {
        assertEquals(5f, Mathf.lerp(0f, 10f, 0.5f), EPSILON);
        assertEquals(0f, Mathf.lerp(0f, 10f, 0f), EPSILON);
        assertEquals(10f, Mathf.lerp(0f, 10f, 1f), EPSILON);
        assertEquals(15f, Mathf.lerp(10f, 20f, 0.5f), EPSILON);
    }

    // unlerp: 역보간이 기대대로 동작하는지 확인 (경계/특수 케이스 포함)
    @Test
    void testUnlerp() {
        assertEquals(0.5f, Mathf.unlerp(0f, 10f, 5f), EPSILON);
        assertEquals(0f, Mathf.unlerp(0f, 10f, 0f), EPSILON);
        assertEquals(1f, Mathf.unlerp(0f, 10f, 10f), EPSILON);
        assertEquals(0f, Mathf.unlerp(5f, 5f, 5f), EPSILON); // a == b 케이스
    }

    // remap: 한 구간에서 다른 구간으로 값이 올바르게 변환되는지 확인
    @Test
    void testRemap() {
        // 0..10 -> 0..100
        assertEquals(50f, Mathf.remap(5f, 0f, 10f, 0f, 100f), EPSILON);
        // 0..100 -> 0..1
        assertEquals(0.5f, Mathf.remap(50f, 0f, 100f, 0f, 1f), EPSILON);
    }

    // smoothstep: [0,1]에서 S-curve 형태를 만족하는지 확인
    @Test
    void testSmoothstep() {
        assertEquals(0f, Mathf.smoothstep(0f), EPSILON);
        assertEquals(1f, Mathf.smoothstep(1f), EPSILON);
        float mid = Mathf.smoothstep(0.5f);
        assertTrue(mid > 0f && mid < 1f);
    }

    // smootherstep: 더 완만한 S-curve 특성을 만족하는지 확인
    @Test
    void testSmootherstep() {
        assertEquals(0f, Mathf.smootherstep(0f), EPSILON);
        assertEquals(1f, Mathf.smootherstep(1f), EPSILON);
        float mid = Mathf.smootherstep(0.5f);
        assertTrue(mid > 0f && mid < 1f);
    }

    // wrapAngle: 각도를 [-PI, PI] 구간으로 래핑하는지 확인 (±PI 경계 포함)
    @Test
    void testWrapAngle() {
        float PI = (float) Math.PI;
        
        assertEquals(0f, Mathf.wrapAngle(0f), EPSILON);
            // PI와 -PI는 같은 각도 (360도 = 0도)
            assertTrue(Math.abs(Mathf.wrapAngle(PI)) - PI < EPSILON);
        assertTrue(Mathf.wrapAngle(3 * PI) >= -PI && Mathf.wrapAngle(3 * PI) <= PI);
    }

    // approach: 목표값으로 델타만큼 다가가는 로직 확인 (상/하향, 초과, 동일 케이스)
    @Test
    void testApproach() {
        // 목표보다 작을 때
        assertEquals(5f, Mathf.approach(0f, 10f, 5f), EPSILON);
        // 목표보다 클 때
        assertEquals(5f, Mathf.approach(10f, 0f, 5f), EPSILON);
        // 델타가 거리보다 클 때
        assertEquals(10f, Mathf.approach(0f, 10f, 20f), EPSILON);
        // 이미 목표에 도달
        assertEquals(10f, Mathf.approach(10f, 10f, 5f), EPSILON);
    }

    // nearlyEqual: 주어진 epsilon 내에서 거의 같은지 확인
    @Test
    void testNearlyEqual() {
        assertTrue(Mathf.nearlyEqual(1.0f, 1.0001f, 0.001f));
        assertFalse(Mathf.nearlyEqual(1.0f, 1.1f, 0.01f));
        assertTrue(Mathf.nearlyEqual(0f, 0f, 0.0001f));
    }

    // nearlyEqual: 기본 epsilon 시나리오 검증
    @Test
    void testNearlyEqualWithDefaultEpsilon() {
        assertTrue(Mathf.nearlyEqual(1.0f, 1.00001f, 0.0001f));
        assertFalse(Mathf.nearlyEqual(1.0f, 1.001f, 0.0001f));
    }

    // clamp: 특수 케이스(동일 범위, 음수 범위) 확인
    @Test
    void testClampEdgeCases() {
        // 같은 범위
        assertEquals(5f, Mathf.clamp(5f, 5f, 5f), EPSILON);
        // 음수 범위
        assertEquals(-5f, Mathf.clamp(-10f, -5f, 0f), EPSILON);
    }

    // lerp: 보간 범위를 벗어난 외삽 시나리오(t<0, t>1) 확인
    @Test
    void testLerpExtrapolation() {
        // t > 1.0
        assertEquals(15f, Mathf.lerp(0f, 10f, 1.5f), EPSILON);
        // t < 0.0
        assertEquals(-5f, Mathf.lerp(0f, 10f, -0.5f), EPSILON);
    }
}
