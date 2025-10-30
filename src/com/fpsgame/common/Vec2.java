package com.fpsgame.common;

/**
 * 2D 벡터(부동소수) 유틸 클래스.
 * <p>
 * - 불필요한 할당을 줄이기 위해 가변(mutating) 연산 위주로 제공
 * - set/add/sub/scale/normalize 등 기본 벡터 연산 지원
 * - 렌더/물리/좌표 변환에서 임시 객체로 재사용하기 쉬움
 */
public final class Vec2 {

    /** x, y 좌표(공개 필드: 핫패스에서 접근 비용 최소화) */
    public float x;
    public float y;

    /** (0,0) 기본 생성자 */
    public Vec2() { this(0f, 0f); }

    /** 초기값 지정 생성자 */
    public Vec2(float x, float y) { this.x = x; this.y = y; }

    // ---------------------------------------------------------------------
    // 설정/복사
    // ---------------------------------------------------------------------

    /** 값을 설정하고 this 반환(체이닝) */
    public Vec2 set(float x, float y) { this.x = x; this.y = y; return this; }

    /** 다른 벡터를 복사하여 설정 */
    public Vec2 set(Vec2 v) {
        if (v == null) return this;
        this.x = v.x; this.y = v.y; return this;
    }

    /** 새 복사본 반환(불변 객체가 필요할 때) */
    public Vec2 copy() { return new Vec2(x, y); }

    // ---------------------------------------------------------------------
    // 산술 연산(가변)
    // ---------------------------------------------------------------------

    /** 더하기(컴포넌트별) */
    public Vec2 add(float dx, float dy) { this.x += dx; this.y += dy; return this; }

    /** 더하기(벡터) */
    public Vec2 add(Vec2 v) {
        if (v != null) { this.x += v.x; this.y += v.y; }
        return this;
    }

    /** 두 벡터 간의 거리 계산 */
    public float distance(Vec2 v) {
        float dx = this.x - v.x;
        float dy = this.y - v.y;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    /** 빼기(컴포넌트별) */
    public Vec2 sub(float sx, float sy) { this.x -= sx; this.y -= sy; return this; }

    /** 빼기(벡터) */
    public Vec2 sub(Vec2 v) {
        if (v != null) { this.x -= v.x; this.y -= v.y; }
        return this;
    }

    /** 스칼라 배 */
    public Vec2 scale(float s) { this.x *= s; this.y *= s; return this; }

    /** 컴포넌트별 배 */
    public Vec2 scale(float sx, float sy) { this.x *= sx; this.y *= sy; return this; }

    // ---------------------------------------------------------------------
    // 길이/정규화
    // ---------------------------------------------------------------------

    /** 길이 제곱(루트 연산 없이 빠르게 비교 시 사용) */
    public float len2() { return x * x + y * y; }

    /** 길이 */
    public float len() { return (float) Math.sqrt(len2()); }

    /** 정규화(0 벡터면 변화 없음) */
    public Vec2 normalize() {
        float l2 = len2();
        if (l2 > 0f) {
            float inv = (float) (1.0 / Math.sqrt(l2));
            x *= inv; y *= inv;
        }
        return this;
    }

    // ---------------------------------------------------------------------
    // 내적/외적(2D 의사 외적: 스칼라)
    // ---------------------------------------------------------------------

    /** 내적 */
    public float dot(Vec2 v) { return (v == null) ? 0f : (x * v.x + y * v.y); }

    /** 2D 의사 외적( x1*y2 - y1*x2 ) */
    public float cross(Vec2 v) { return (v == null) ? 0f : (x * v.y - y * v.x); }

    // ---------------------------------------------------------------------
    // 보조
    // ---------------------------------------------------------------------

    /** 근사적 비교(허용 오차 epsilon) */
    public boolean epsilonEquals(Vec2 v, float eps) {
        if (v == null) return false;
        return Math.abs(x - v.x) <= eps && Math.abs(y - v.y) <= eps;
    }

    @Override
    public String toString() {
        return "Vec2{" + "x=" + x + ", y=" + y + '}';
    }

    public Vec2 mul(float deltaTime) {
        // Non-mutating scalar multiply: returns a new vector
        return new Vec2(this.x * deltaTime, this.y * deltaTime);
    }
}
