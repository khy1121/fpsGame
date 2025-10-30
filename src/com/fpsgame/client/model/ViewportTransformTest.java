package com.fpsgame.client.model;
import org.junit.jupiter.api.Test;
import java.awt.Point;
import static org.junit.jupiter.api.Assertions.*;
class ViewportTransformTest {
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
}
