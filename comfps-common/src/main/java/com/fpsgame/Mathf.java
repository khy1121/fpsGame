package com.fpsgame.common;

/**
 * 경량 수학 유틸리티 모음.
 *
 * <p>게임 개발에서 자주 쓰는 보간/클램프/각도 유틸을 제공한다.</p>
 *
 * <p>특징</p>
 * <ul>
 *   <li>정적 메서드만 제공(인스턴스 생성 불가)</li>
 *   <li>float 중심(필요 시 double 변환하여 사용)</li>
 *   <li>부동 소수점 연산에서 발생하기 쉬운 경계값을 안전하게 처리</li>
 * </ul>
 */
public final class Mathf {

    private Mathf() { /* no instance */ }

    /** {@code v} 를 {@code [min,max]} 범위로 자른다. */
    public static float clamp(float v, float min, float max) {
        if (min > max) { float t = min; min = max; max = t; }
        return v < min ? min : (v > max ? max : v);
    }

    /** {@code v} 를 {@code [0,1]} 범위로 자른다. */
    public static float saturate(float v) { return clamp(v, 0f, 1f); }

    /** 선형 보간: {@code a + (b-a) * t}. */
    public static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    /** 역보간: {@code a..b} 구간에서 {@code v} 가 차지하는 비율 t(0..1). */
    public static float unlerp(float a, float b, float v) {
        if (a == b) return 0f;
        return (v - a) / (b - a);
    }

    /** 구간 재매핑: {@code v} (inMin..inMax) → (outMin..outMax) 선형 변환. */
    public static float remap(float v, float inMin, float inMax, float outMin, float outMax) {
        return lerp(outMin, outMax, unlerp(inMin, inMax, v));
    }

    /** Smoothstep(0..1): {@code t*t*(3-2*t)}. 입력은 자동으로 0..1로 포화(saturate). */
    public static float smoothstep(float t) {
        t = saturate(t);
        return t * t * (3f - 2f * t);
    }

    /** Smootherstep(0..1): {@code t^3( t(6t-15)+10 ) }. */
    public static float smootherstep(float t) {
        t = saturate(t);
        return t * t * t * (t * (t * 6f - 15f) + 10f);
    }

    /** 각도(라디안)를 {@code -π..+π} 범위로 감싼다. */
    public static float wrapAngle(float rad) {
        final float PI = (float) Math.PI;
        final float TAU = (float) (Math.PI * 2.0);
        rad = (rad + PI) % TAU;
        if (rad < 0) rad += TAU;
        return rad - PI;
    }

    /**
     * 값을 일정 속도로 목표에 접근시킨다(오버슈트 없음).
     * <p>예) 카메라 추적: {@code x = approach(x, target, 5f * dt);}
     * @param current 현재 값
     * @param target  목표 값
     * @param delta   이번 프레임에 움직일 수 있는 최대 변화량(절대값)
     */
    public static float approach(float current, float target, float delta) {
        float diff = target - current;
        if (Math.abs(diff) <= delta) return target;
        return current + Math.copySign(delta, diff);
    }

    /** 부동소수점 비교: {@code |a-b| <= eps} 이면 동일로 간주. */
    public static boolean nearlyEqual(float a, float b, float eps) {
        return Math.abs(a - b) <= eps;
    }

    /** 부호 함수: 음수 -1, 0은 0, 양수 1. */
    public static int sign(float v) {
        return (v > 0f) ? 1 : (v < 0f ? -1 : 0);
    }

    // ===== 간단한 자체 테스트 =====
    public static void main(String[] args) {
        System.out.println("clamp(1.5,0,1) = " + clamp(1.5f, 0f, 1f)); // 1.0
        System.out.println("smoothstep(0.5) = " + smoothstep(0.5f));   // 0.5 부근
        System.out.println("wrapAngle(4π)   = " + wrapAngle((float)(4*Math.PI))); // 0
        System.out.println("remap(5,0,10,0,1) = " + remap(5f, 0f, 10f, 0f, 1f)); // 0.5
        float v = 0f;
        for (int i = 0; i < 5; i++) {
            v = approach(v, 10f, 2.5f);
            System.out.println("approach step " + i + " -> " + v);
        }
    }
}
