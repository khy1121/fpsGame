package com.fpsgame.client.model;

import java.awt.Point;

import com.fpsgame.common.Rect;
import com.fpsgame.common.Vec2;

/**
 * 뷰포트(월드↔스크린 변환 및 카메라 관리).
 * <p>
 * - 스크린 크기(px), 월드 크기(wu), 스케일(px/ wu)을 보유
 * - 카메라 중심 좌표(camX, camY) 기반으로 가시 영역을 계산
 * - 줌/패닝 지원(월드 경계 내 클램프)
 *
 * 용어:
 * - wu(World Unit): 논리 좌표 단위
 * - scale: 1 wu가 스크린에서 몇 px인가
 */
public final class Viewport {

    // ---- 스크린/월드 상태 ----
    private int screenW = 1;   // px
    private int screenH = 1;   // px

    private float worldW;      // wu
    private float worldH;      // wu
    private final Rect worldBounds = new Rect(); // (0,0,w,h)

    // 카메라 중심(월드 좌표)
    private float camX = 0f;
    private float camY = 0f;

    // 스케일(px / wu)
    private float scale = 1.0f;  // 기본 스케일 1.0으로 복원

    // 줌 한계
    private float minScale = 0.05f;
    private float maxScale = 3.0f;

    /** @param worldW 초기 월드 너비(wu), @param worldH 높이(wu) */
    public Viewport(float worldW, float worldH) {
        setWorldSize(worldW, worldH);
        setCenter(worldW * 0.5f, worldH * 0.5f);
        recomputeScale();
    }

    // ---------------------------------------------------------------------
    // 크기/스케일
    // ---------------------------------------------------------------------

    /** 스크린(픽셀) 크기 갱신 — 컴포넌트 리사이즈 시 호출 */
    public void resize(int w, int h) {
        this.screenW = Math.max(1, w);
        this.screenH = Math.max(1, h);
        recomputeScale();
        clampCamera();
    }

    /** 월드 크기(wu) 갱신 — 맵 전환 등에 사용 */
    public void setWorldSize(float w, float h) {
        this.worldW = Math.max(1f, w);
        this.worldH = Math.max(1f, h);
        // 기존 논의에 따라 Rect#set(x,y,w,h)를 정확히 사용
        this.worldBounds.set(0f, 0f, this.worldW, this.worldH);
        clampCamera();
        recomputeScale();
    }

    /** 스케일 재계산(월드가 스크린보다 작으면 최소로 꽉 채우도록) */
    private void recomputeScale() {
        // 최소 스케일은 고정값으로 설정 (자동 계산하지 않음)
        // 이렇게 하면 사용자가 설정한 스케일(0.8 등)이 유지됨
        // minScale은 생성자에서 0.1로 이미 설정되어 있음
        scale = clamp(scale, minScale, maxScale);
    }

    /** Fit the entire world inside the current screen dimensions. */
    public void fitToWorld() {
        if (worldW <= 0f || worldH <= 0f) return;
        if (screenW <= 0 || screenH <= 0) return;
        float scaleX = screenW / worldW;
        float scaleY = screenH / worldH;
        float target = Math.min(scaleX, scaleY);
        if (!Float.isFinite(target) || target <= 0f) return;
        scale = clamp(target, minScale, maxScale);
        clampCamera();
    }

    /** 줌 배율 설정(절대값) */
    public void setScale(float pxPerWu) {
        this.scale = clamp(pxPerWu, minScale, maxScale);
        clampCamera();
    }

    /** 줌 배율 변경(상대값) */
    public void zoomBy(float factor) {
        if (!Float.isFinite(factor) || factor <= 0f) return;
        setScale(scale * factor);
    }

    /** 현재 스케일(px/ wu) */
    public float getScale() { return scale; }

    /** 줌 한계 설정 */
    public void setZoomLimits(float min, float max) {
        if (min > 0 && max >= min) {
            this.minScale = min;
            this.maxScale = max;
            setScale(scale); // 재클램프
        }
    }

    // ---------------------------------------------------------------------
    // 카메라
    // ---------------------------------------------------------------------

    /** 카메라 중심 좌표를 설정(월드 경계로 클램프) */
    public void setCenter(float x, float y) {
        this.camX = x;
        this.camY = y;
        clampCamera();
    }

    /** 카메라 중심 반환(출력 벡터 재사용 가능) */
    public Vec2 getCenter(Vec2 out) {
        if (out == null) out = new Vec2();
        return out.set(camX, camY);
    }

    /** 카메라를 월드 경계에 맞춰 클램프 */
    private void clampCamera() {
        float halfWwu = screenW / (2f * scale);
        float halfHwu = screenH / (2f * scale);

        float minX = worldBounds.x + halfWwu;
        float maxX = worldBounds.right() - halfWwu;
        float minY = worldBounds.y + halfHwu;
        float maxY = worldBounds.bottom() - halfHwu;

        if (minX > maxX) {
            // 화면이 월드보다 더 클 때: 중앙 고정
            camX = worldW * 0.5f;
        } else {
            camX = clamp(camX, minX, maxX);
        }

        if (minY > maxY) {
            camY = worldH * 0.5f;
        } else {
            camY = clamp(camY, minY, maxY);
        }
    }

    // ---------------------------------------------------------------------
    // 좌표 변환
    // ---------------------------------------------------------------------

    /** 월드 좌표 → 스크린 좌표 */
    public Point worldToScreen(float wx, float wy) {
        float halfWpx = screenW * 0.5f;
        float halfHpx = screenH * 0.5f;

        int sx = Math.round((wx - camX) * scale + halfWpx);
        int sy = Math.round((wy - camY) * scale + halfHpx);
        return new Point(sx, sy);
    }

    /** 스크린 좌표 → 월드 좌표(부동소수) */
    public Vec2 screenToWorld(int sx, int sy, Vec2 out) {
        if (out == null) out = new Vec2();
        float halfWpx = screenW * 0.5f;
        float halfHpx = screenH * 0.5f;

        float wx = (sx - halfWpx) / scale + camX;
        float wy = (sy - halfHpx) / scale + camY;
        return out.set(wx, wy);
    }

    /** 현재 가시 월드 사각형을 반환 */
    public Rect getViewBounds(Rect out) {
        if (out == null) out = new Rect();
        float halfWwu = screenW / (2f * scale);
        float halfHwu = screenH / (2f * scale);
        // 기존 논의에 따라 Rect#set(x, y, w, h)를 정확히 사용
        out.set(camX - halfWwu, camY - halfHwu, halfWwu * 2f, halfHwu * 2f);
        return out;
    }

    // ---------------------------------------------------------------------
    // 유틸/클램프
    // ---------------------------------------------------------------------

    private static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }
}
