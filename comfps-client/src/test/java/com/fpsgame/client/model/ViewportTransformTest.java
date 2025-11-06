package com.fpsgame.client.model;
import java.awt.Point;
import com.fpsgame.common.Rect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

/**
 * Viewport 변환 테스트
 * - 월드 중심 좌표가 스크린 중심으로 매핑되는지, 줌 변경 후에도 유지되는지 확인
 */
class ViewportTransformTest {
    // 월드 중심 → 스크린 중심 매핑 검증 (줌 변화 후에도 중심 고정)
    @Test
    void worldCenter_mapsTo_screenCenter() {
        Viewport vp = new Viewport(3000f, 2000f);
        vp.resize(800, 600);
        vp.setCenter(1000f, 500f);
        int cx = 400, cy = 300;
        Point p = vp.worldToScreen(1000f, 500f);
        assertEquals(cx, p.x, 2);
        assertEquals(cy, p.y, 2);
        vp.zoomBy(1.25f);
        Point p2 = vp.worldToScreen(1000f, 500f);
        assertEquals(cx, p2.x, 2);
        assertEquals(cy, p2.y, 2);
    }
    @Test
    void fitToWorldKeepsEntireWorldVisible() {
        Viewport vp = new Viewport(6000f, 4000f);
        vp.resize(1200, 800);
        vp.fitToWorld();
        float expectedScale = Math.min(1200f / 6000f, 800f / 4000f);
        assertEquals(expectedScale, vp.getScale(), 1e-4f);
        Rect view = vp.getViewBounds(null);
        assertEquals(0f, view.x, 1e-3f);
        assertEquals(0f, view.y, 1e-3f);
        assertEquals(6000f, view.w, 1e-2f);
        assertEquals(4000f, view.h, 1e-2f);
    }
}