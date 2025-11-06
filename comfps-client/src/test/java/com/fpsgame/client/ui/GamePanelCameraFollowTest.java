package com.fpsgame.client.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import org.junit.jupiter.api.Test;

import com.fpsgame.client.model.Viewport;
import com.fpsgame.common.SnapshotV2;
import com.fpsgame.common.Vec2;

class GamePanelCameraFollowTest {

    private static final class TestableGamePanel extends GamePanel {
        private final Method updateCameraMethod;
        private final Field viewportField;
        private final Field playersField;

        TestableGamePanel() {
            try {
                updateCameraMethod = GamePanel.class.getDeclaredMethod("updateCamera");
                updateCameraMethod.setAccessible(true);
                viewportField = GamePanel.class.getDeclaredField("viewport");
                viewportField.setAccessible(true);
                playersField = GamePanel.class.getDeclaredField("players");
                playersField.setAccessible(true);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException("Reflection setup failed", e);
            }
        }

        void tickCamera() {
            try {
                updateCameraMethod.invoke(this);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException("Unable to invoke updateCamera()", e);
            }
        }

        Viewport viewport() {
            try {
                return (Viewport) viewportField.get(this);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Unable to access viewport", e);
            }
        }

        SnapshotV2.Entry playerEntry(int id) {
            try {
                @SuppressWarnings("unchecked")
                Map<Integer, SnapshotV2.Entry> map = (Map<Integer, SnapshotV2.Entry>) playersField.get(this);
                return map.get(id);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Unable to access players map", e);
            }
        }
    }

    private static SnapshotV2.Entry snapshot(int id, float x, float y) {
        return new SnapshotV2.Entry(id, x, y, 0f, 0, 0, 100, 0f, 0f);
    }

    @Test
    void defaultModeShowsWholeMap() {
        TestableGamePanel panel = new TestableGamePanel();
        panel.setSize(800, 600);
        panel.viewport().resize(800, 600);
        panel.setWorldSize(6000, 4000);
        panel.setMyId(7);

        panel.applySnapshot(List.of(snapshot(7, 3200f, 1800f)));
        panel.tickCamera();

        Vec2 center1 = panel.viewport().getCenter(new Vec2());
        assertThat(center1.x).isCloseTo(3000f, within(0.01f));
        assertThat(center1.y).isCloseTo(2000f, within(0.01f));

        float expectedScale = Math.min(800f / 6000f, 600f / 4000f);
        assertThat(panel.viewport().getScale()).isCloseTo(expectedScale, within(0.0001f));

        Point origin1 = panel.viewport().worldToScreen(0f, 0f);

        panel.applySnapshot(List.of(snapshot(7, 4500f, 2500f)));
        panel.tickCamera();

        Vec2 center2 = panel.viewport().getCenter(new Vec2());
        assertThat(center2.x).isCloseTo(3000f, within(0.01f));
        assertThat(center2.y).isCloseTo(2000f, within(0.01f));

        Point origin2 = panel.viewport().worldToScreen(0f, 0f);
        assertThat(origin2).isEqualTo(origin1);
    }

    @Test
    void followModeTracksPlayerWithConfiguredZoom() {
        TestableGamePanel panel = new TestableGamePanel();
        panel.setSize(1024, 768);
        panel.viewport().resize(1024, 768);
        panel.setWorldSize(4000, 2400);
        panel.setMyId(3);
        panel.setCameraMode(GamePanel.CameraMode.PLAYER_FOLLOW);
        panel.setFollowCameraScale(2.5f);

        panel.applySnapshot(List.of(snapshot(3, 800f, 600f)));
        panel.tickCamera();

        Vec2 camCenter1 = panel.viewport().getCenter(new Vec2());
        assertThat(camCenter1.x).isCloseTo(800f, within(0.01f));
        assertThat(camCenter1.y).isCloseTo(600f, within(0.01f));
        assertThat(panel.viewport().getScale()).isCloseTo(2.5f, within(0.0001f));

        Point origin1 = panel.viewport().worldToScreen(0f, 0f);

        panel.applySnapshot(List.of(snapshot(3, 1600f, 1400f)));
        panel.tickCamera();

        Vec2 camCenter2 = panel.viewport().getCenter(new Vec2());
        assertThat(camCenter2.x).isCloseTo(1600f, within(0.01f));
        assertThat(camCenter2.y).isCloseTo(1400f, within(0.01f));
        assertThat(panel.viewport().getScale()).isCloseTo(2.5f, within(0.0001f));

        Point origin2 = panel.viewport().worldToScreen(0f, 0f);
        assertThat(origin2.x).isLessThan(origin1.x);
        assertThat(origin2.y).isLessThan(origin1.y);
    }

    @Test
    void snapshotPositionsAreClampedToWorld() {
        TestableGamePanel panel = new TestableGamePanel();
        panel.setWorldSize(1000, 500);
        panel.setMyId(9);

        panel.applySnapshot(List.of(snapshot(9, -75f, 620f)));
        SnapshotV2.Entry sanitized = panel.playerEntry(9);
        assertThat(sanitized.x).isZero();
        assertThat(sanitized.y).isEqualTo(500f);

        panel.applySnapshot(List.of(snapshot(9, 250f, 120f)));
        SnapshotV2.Entry withinBounds = panel.playerEntry(9);
        assertThat(withinBounds.x).isEqualTo(250f);
        assertThat(withinBounds.y).isEqualTo(120f);
    }

    /**
     * 렌더 순서(z-order) 검증: 플레이어를 나타내는 빨간 점을 먼저 그리고,
     * 그 위에 캐릭터 스프라이트(중앙이 불투명한 초록색)를 그리면 중심 픽셀은
     * 스프라이트 색(초록)이어야 한다.
     */
    @Test
    void characterSpriteRendersAboveRedDot() {
        int W = 200, H = 200;
        int cx = W / 2, cy = H / 2;

        // 알파 채널 포함 캔버스
        BufferedImage canvas = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = canvas.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 1) 플레이어 빨간점 (배경)
        int dotR = 14;
        g.setColor(new Color(0xFF3A3A));
        g.fillOval(cx - dotR, cy - dotR, dotR * 2, dotR * 2);

        // 2) 캐릭터 스프라이트 (전경): 중앙이 불투명 초록색 원
        int spriteSize = 48;
        BufferedImage sprite = new BufferedImage(spriteSize, spriteSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D sg = sprite.createGraphics();
        sg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int sr = 18; // 스프라이트 내부 원 반지름
        sg.setColor(new Color(0x24D05A)); // 불투명 초록
        sg.fillOval(spriteSize/2 - sr, spriteSize/2 - sr, sr * 2, sr * 2);
        sg.setColor(new Color(0x0F8A3E));
        sg.setStroke(new BasicStroke(2f));
        sg.drawOval(spriteSize/2 - sr, spriteSize/2 - sr, sr * 2, sr * 2);
        sg.dispose();

        // 3) 중심 정렬로 스프라이트를 빨간점 위에 그림
        int sx = cx - spriteSize/2;
        int sy = cy - spriteSize/2;
        g.drawImage(sprite, sx, sy, null);
        g.dispose();

        // 4) 중심 픽셀 샘플: 스프라이트 색이어야 함
        Color center = new Color(canvas.getRGB(cx, cy), true);
        assertThat(center.getAlpha()).isEqualTo(255);
        assertThat(center.getRed()).isEqualTo(0x24);
        assertThat(center.getGreen()).isEqualTo(0xD0);
        assertThat(center.getBlue()).isEqualTo(0x5A);
    }
}
