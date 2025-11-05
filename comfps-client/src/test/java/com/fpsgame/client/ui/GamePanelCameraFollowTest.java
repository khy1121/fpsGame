// 파일 위치: comfps-client/src/test/java/com/fpsgame/client/ui/GamePanelCameraFollowTest.java
package com.fpsgame.client.ui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.awt.Point;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fpsgame.common.SnapshotV2;
import com.fpsgame.common.Vec2;
import com.fpsgame.client.model.Viewport;

class GamePanelCameraFollowTest {

    /**
     * 테스트 전용으로 GamePanel 내부(Viewport 포함)에 접근하기 위한 헬퍼.
     * (프로덕션 코드 수정 없이 리플렉션만 사용합니다.)
     */
    private static final class TestableGamePanel extends GamePanel {
        private final Method updateCameraMethod;
        private final Field viewportField;

        TestableGamePanel() {
            try {
                updateCameraMethod = GamePanel.class.getDeclaredMethod("updateCamera");
                updateCameraMethod.setAccessible(true);
                viewportField = GamePanel.class.getDeclaredField("viewport");
                viewportField.setAccessible(true);
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

        void setCameraZoom(float zoom) {
            viewport().setScale(zoom);
        }
    }

    @Test
    void cameraFollowsPlayerAndAppliesZoom() {
        TestableGamePanel panel = new TestableGamePanel();
        panel.setWorldSize(6000, 4000);
        panel.setMyId(7);
        panel.setCameraZoom(3.0f); // 원하는 줌 레벨
        panel.setInputEnabled(false); // 테스트 안정화를 위해 입력 타이머 차단(선택 사항)

        // 첫 번째 위치: (1500, 1000)
        SnapshotV2.Entry firstPose = new SnapshotV2.Entry(
                7, 1500f, 1000f, 0f, 0, 0, 100, 0f, 0f);
        panel.applySnapshot(List.of(firstPose));
        panel.tickCamera();

        Vec2 camCenter1 = panel.viewport().getCenter(new Vec2());
        assertThat(camCenter1.x).isCloseTo(1500f, within(0.01f));
        assertThat(camCenter1.y).isCloseTo(1000f, within(0.01f));
        assertThat(panel.viewport().getScale()).isEqualTo(3.0f);

        Point originProjected1 = panel.viewport().worldToScreen(0f, 0f);

        // 두 번째 위치: (2000, 1200) - 플레이어가 이동
        SnapshotV2.Entry movedPose = new SnapshotV2.Entry(
                7, 2000f, 1200f, 0f, 0, 0, 100, 0f, 0f);
        panel.applySnapshot(List.of(movedPose));
        panel.tickCamera();

        Vec2 camCenter2 = panel.viewport().getCenter(new Vec2());
        assertThat(camCenter2.x).isCloseTo(2000f, within(0.01f));
        assertThat(camCenter2.y).isCloseTo(1200f, within(0.01f));

        // 플레이어가 오른쪽 아래로 이동했으므로, 월드 원점(0,0)의 스크린 좌표는 왼쪽 위로 이동해야 함
        Point originProjected2 = panel.viewport().worldToScreen(0f, 0f);
        assertThat(originProjected2.x).isLessThan(originProjected1.x);
        assertThat(originProjected2.y).isLessThan(originProjected1.y);
    }
}
