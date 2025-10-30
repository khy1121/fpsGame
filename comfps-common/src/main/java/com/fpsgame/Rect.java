package com.fpsgame.common;

import java.io.Serializable;

/**
 * 축 정렬 사각형(월드 단위) 유틸리티.
 * <p>
 * - 필드: x, y, w, h (모두 float)
 * - 좌표계: (x,y)는 좌상단, w/h는 폭/높이
 * - Viewport, 충돌 판정, 화면 변환 등에서 공용으로 사용
 */
public final class Rect implements Serializable, Cloneable {

    private static final long serialVersionUID = 1L;

    /** 좌상단 X */
    public float x;
    /** 좌상단 Y */
    public float y;
    /** 폭 */
    public float w;
    /** 높이 */
    public float h;

    /** (0,0,0,0) 기본 생성 */
    public Rect() { this(0f, 0f, 0f, 0f); }

    /** (x,y,w,h)로 초기화 */
    public Rect(float x, float y, float w, float h) {
        set(x, y, w, h);
    }

    /** 다른 사각형으로부터 복사 생성 */
    public Rect(Rect r) {
        if (r == null) { set(0, 0, 0, 0); }
        else { set(r.x, r.y, r.w, r.h); }
    }

    // ---------------------------------------------------------------------
    // 설정/쿼리
    // ---------------------------------------------------------------------

    /** 값을 설정한다. (x, y, w, h) */
    public Rect set(float x, float y, float w, float h) {
        this.x = x;
        this.y = y;
        this.w = Math.max(0f, w);
        this.h = Math.max(0f, h);
        return this;
    }

    /** 다른 사각형과 동일하게 설정 */
    public Rect set(Rect r) {
        if (r == null) return this;
        return set(r.x, r.y, r.w, r.h);
    }

    /** 우측 경계 x 값 반환: x + w */
    public float right() { return x + w; }

    /** 하단 경계 y 값 반환: y + h */
    public float bottom() { return y + h; }

    /** 중심 X */
    public float centerX() { return x + w * 0.5f; }

    /** 중심 Y */
    public float centerY() { return y + h * 0.5f; }

    // ---------------------------------------------------------------------
    // 기하 유틸
    // ---------------------------------------------------------------------

    /** 점 포함 여부 */
    public boolean contains(float px, float py) {
        return px >= x && px <= right() && py >= y && py <= bottom();
    }

    /** 사각형 포함 여부 */
    public boolean contains(Rect r) {
        if (r == null) return false;
        return r.x >= x && r.right() <= right() && r.y >= y && r.bottom() <= bottom();
    }

    /** 교차 여부(경계 접촉 포함) */
    public boolean intersects(Rect r) {
        if (r == null) return false;
        return !(r.right() < x || r.x > right() || r.bottom() < y || r.y > bottom());
    }

    /** 이동(translate) */
    public Rect translate(float dx, float dy) {
        this.x += dx; this.y += dy;
        return this;
    }

    /** 패딩(사방 동일하게 확장; 음수면 축소) */
    public Rect inflate(float pad) {
        this.x -= pad;
        this.y -= pad;
        this.w += pad * 2f;
        this.h += pad * 2f;
        if (w < 0f) w = 0f;
        if (h < 0f) h = 0f;
        return this;
    }

    /** 다른 사각형을 포함하도록 확장 */
    public Rect expandToInclude(Rect r) {
        if (r == null) return this;
        float nx = Math.min(this.x, r.x);
        float ny = Math.min(this.y, r.y);
        float nr = Math.max(this.right(), r.right());
        float nb = Math.max(this.bottom(), r.bottom());
        this.x = nx;
        this.y = ny;
        this.w = Math.max(0f, nr - nx);
        this.h = Math.max(0f, nb - ny);
        return this;
    }

    /**
     * 주어진 경계 안에 현재 사각형을 클램프한다.
     * 대상보다 크면 좌상단 정렬로 맞춘다.
     */
    public Rect clampWithin(Rect bounds) {
        if (bounds == null) return this;
        // 폭/높이가 더 크면 잘라낸다
        if (this.w > bounds.w) { this.x = bounds.x; this.w = bounds.w; }
        if (this.h > bounds.h) { this.y = bounds.y; this.h = bounds.h; }
        // 좌표 클램프
        if (this.x < bounds.x) this.x = bounds.x;
        if (this.y < bounds.y) this.y = bounds.y;
        if (this.right() > bounds.right()) this.x = bounds.right() - this.w;
        if (this.bottom() > bounds.bottom()) this.y = bounds.bottom() - this.h;
        return this;
    }

    // ---------------------------------------------------------------------
    // 표준 메서드
    // ---------------------------------------------------------------------

    @Override public Rect clone() { return new Rect(this); }

    @Override
    public String toString() {
        return "Rect{x=" + x + ", y=" + y + ", w=" + w + ", h=" + h + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Rect)) return false;
        Rect r = (Rect) o;
        return Float.compare(r.x, x) == 0 &&
               Float.compare(r.y, y) == 0 &&
               Float.compare(r.w, w) == 0 &&
               Float.compare(r.h, h) == 0;
    }

    @Override
    public int hashCode() {
        int result = Float.hashCode(x);
        result = 31 * result + Float.hashCode(y);
        result = 31 * result + Float.hashCode(w);
        result = 31 * result + Float.hashCode(h);
        return result;
    }
}
