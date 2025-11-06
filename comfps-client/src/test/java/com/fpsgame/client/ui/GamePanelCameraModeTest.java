package com.fpsgame.client.ui;

import java.awt.Point;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fpsgame.client.model.Viewport;
import com.fpsgame.common.SnapshotV2;
import com.fpsgame.common.Vec2;

/**
 * GamePanel 카메라 모드 테스트
 * - MAP_OVERVIEW: 맵 전체 보기, 카메라 맵 중앙 고정
 * - PLAYER_FOLLOW: 플레이어 추적, 카메라가 플레이어를 따라감
 */
class GamePanelCameraModeTest {

    private GamePanel gamePanel;
    private Viewport viewport;

    @BeforeEach
    void setUp() throws Exception {
        gamePanel = new GamePanel();
        gamePanel.setSize(800, 600);
        gamePanel.setWorldSize(3000, 2000);
        
        // Reflection을 통해 viewport 접근
        Field viewportField = GamePanel.class.getDeclaredField("viewport");
        viewportField.setAccessible(true);
        viewport = (Viewport) viewportField.get(gamePanel);
    }

    @Test
    void testDefaultCameraMode() {
        // Given: 기본 생성된 GamePanel
        
        // When: 카메라 모드 확인
        GamePanel.CameraMode mode = gamePanel.getCameraMode();
        
        // Then: 기본값은 MAP_OVERVIEW
        assertThat(mode).isEqualTo(GamePanel.CameraMode.MAP_OVERVIEW);
    }

    @Test
    void testMapOverviewModeFixesCameraAtMapCenter() throws Exception {
        // Given: MAP_OVERVIEW 모드
        gamePanel.setCameraMode(GamePanel.CameraMode.MAP_OVERVIEW);
        gamePanel.setMyId(1);
        
        // 플레이어 위치 설정 (맵 한쪽 구석)
        List<SnapshotV2.Entry> snapshot = new ArrayList<>();
        snapshot.add(new SnapshotV2.Entry(1, 500f, 300f, 0f, 1, 0, 100, 0f, 0f));
        gamePanel.applySnapshot(snapshot);
        
        // When: updateCamera 호출
        Method updateCamera = GamePanel.class.getDeclaredMethod("updateCamera");
        updateCamera.setAccessible(true);
        updateCamera.invoke(gamePanel);
        
        // Then: 카메라는 맵 중앙 (1500, 1000)에 고정
        Vec2 camCenter = viewport.getCenter(null);
        assertThat(camCenter.x).isEqualTo(1500f);
        assertThat(camCenter.y).isEqualTo(1000f);
    }

    @Test
    void testPlayerFollowModeTracksPlayer() throws Exception {
        // Given: PLAYER_FOLLOW 모드
        gamePanel.setCameraMode(GamePanel.CameraMode.PLAYER_FOLLOW);
        gamePanel.setMyId(1);
        gamePanel.setFollowCameraScale(2.0f);
        
        // 플레이어 위치 설정
        List<SnapshotV2.Entry> snapshot = new ArrayList<>();
        snapshot.add(new SnapshotV2.Entry(1, 800f, 600f, 0f, 1, 0, 100, 0f, 0f));
        gamePanel.applySnapshot(snapshot);
        
        // When: updateCamera 호출
        Method updateCamera = GamePanel.class.getDeclaredMethod("updateCamera");
        updateCamera.setAccessible(true);
        updateCamera.invoke(gamePanel);
        
        // Then: 카메라가 플레이어 위치 (800, 600)를 추적
        Vec2 camCenter = viewport.getCenter(null);
        assertThat(camCenter.x).isEqualTo(800f);
        assertThat(camCenter.y).isEqualTo(600f);
        
        // And: 줌 레벨 확인
        assertThat(viewport.getScale()).isEqualTo(2.0f);
    }

    @Test
    void testCameraModeToggle() {
        // Given: MAP_OVERVIEW 모드
        gamePanel.setCameraMode(GamePanel.CameraMode.MAP_OVERVIEW);
        
        // When: PLAYER_FOLLOW로 전환
        gamePanel.setCameraMode(GamePanel.CameraMode.PLAYER_FOLLOW);
        
        // Then: 모드가 변경됨
        assertThat(gamePanel.getCameraMode()).isEqualTo(GamePanel.CameraMode.PLAYER_FOLLOW);
        
        // When: 다시 MAP_OVERVIEW로 전환
        gamePanel.setCameraMode(GamePanel.CameraMode.MAP_OVERVIEW);
        
        // Then: 모드가 변경됨
        assertThat(gamePanel.getCameraMode()).isEqualTo(GamePanel.CameraMode.MAP_OVERVIEW);
    }

    @Test
    void testMapOverviewAutoFitsViewport() throws Exception {
        // Given: MAP_OVERVIEW 모드
        gamePanel.setCameraMode(GamePanel.CameraMode.MAP_OVERVIEW);
        
        // When: 화면 크기 변경 시뮬레이션
        gamePanel.setSize(1200, 800);
        Field autoFitField = GamePanel.class.getDeclaredField("autoFitViewport");
        autoFitField.setAccessible(true);
        boolean autoFit = (boolean) autoFitField.get(gamePanel);
        
        // Then: autoFitViewport가 true
        assertThat(autoFit).isTrue();
    }

    @Test
    void testPlayerFollowDisablesAutoFit() throws Exception {
        // Given: PLAYER_FOLLOW 모드
        gamePanel.setCameraMode(GamePanel.CameraMode.PLAYER_FOLLOW);
        
        // When: autoFitViewport 확인
        Field autoFitField = GamePanel.class.getDeclaredField("autoFitViewport");
        autoFitField.setAccessible(true);
        boolean autoFit = (boolean) autoFitField.get(gamePanel);
        
        // Then: autoFitViewport가 false
        assertThat(autoFit).isFalse();
    }

    @Test
    void testWorldToScreenTransformInMapOverview() {
        // Given: MAP_OVERVIEW 모드, 3000x2000 월드, 800x600 화면
        gamePanel.setCameraMode(GamePanel.CameraMode.MAP_OVERVIEW);
        gamePanel.setSize(800, 600);
        gamePanel.setWorldSize(3000, 2000);
        
        // When: 월드 원점 (0,0)을 스크린 좌표로 변환
        Point screenPos = viewport.worldToScreen(0f, 0f);
        
        // Then: 변환이 정상 작동 (화면 왼쪽 상단 근처)
        assertThat(screenPos.x).isLessThanOrEqualTo(100);
        assertThat(screenPos.y).isLessThanOrEqualTo(100);
    }

    @Test
    void testSanitizeEntryClampsToBounds() throws Exception {
        // Given: 월드 크기 3000x2000
        gamePanel.setWorldSize(3000, 2000);
        
        // When: 경계 밖 플레이어 위치
        Method sanitize = GamePanel.class.getDeclaredMethod("sanitizeEntry", SnapshotV2.Entry.class);
        sanitize.setAccessible(true);
        
        SnapshotV2.Entry outOfBounds = new SnapshotV2.Entry(1, 3500f, 2500f, 0f, 1, 0, 100, 0f, 0f);
        SnapshotV2.Entry sanitized = (SnapshotV2.Entry) sanitize.invoke(gamePanel, outOfBounds);
        
        // Then: 월드 경계 내로 클램핑됨
        assertThat(sanitized.x).isLessThanOrEqualTo(3000f);
        assertThat(sanitized.y).isLessThanOrEqualTo(2000f);
    }

    @Test
    void testMultiplePlayersIndependentMovement() throws Exception {
        // Given: 여러 플레이어
        gamePanel.setMyId(1);
        
        List<SnapshotV2.Entry> snapshot = new ArrayList<>();
        snapshot.add(new SnapshotV2.Entry(1, 100f, 100f, 0f, 1, 0, 100, 0f, 0f));
        snapshot.add(new SnapshotV2.Entry(2, 200f, 200f, 0f, 2, 1, 100, 0f, 0f));
        snapshot.add(new SnapshotV2.Entry(3, 300f, 300f, 0f, 1, 2, 100, 0f, 0f));
        
        // When: 스냅샷 적용
        gamePanel.applySnapshot(snapshot);
        
        // Then: 각 플레이어가 독립적으로 저장됨
        Field playersField = GamePanel.class.getDeclaredField("players");
        playersField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<Integer, SnapshotV2.Entry> players = (Map<Integer, SnapshotV2.Entry>) playersField.get(gamePanel);
        
        assertThat(players).hasSize(3);
        assertThat(players.get(1).x).isEqualTo(100f);
        assertThat(players.get(2).x).isEqualTo(200f);
        assertThat(players.get(3).x).isEqualTo(300f);
    }

    @Test
    void testFollowModeScaleConfiguration() {
        // Given: 기본 GamePanel
        
        // When: 팔로우 모드 스케일 설정
        gamePanel.setFollowCameraScale(3.5f);
        gamePanel.setCameraMode(GamePanel.CameraMode.PLAYER_FOLLOW);
        gamePanel.setMyId(1);
        
        List<SnapshotV2.Entry> snapshot = new ArrayList<>();
        snapshot.add(new SnapshotV2.Entry(1, 500f, 500f, 0f, 1, 0, 100, 0f, 0f));
        gamePanel.applySnapshot(snapshot);
        
        // Then: 스케일이 적용됨
        try {
            Method updateCamera = GamePanel.class.getDeclaredMethod("updateCamera");
            updateCamera.setAccessible(true);
            updateCamera.invoke(gamePanel);
            
            assertThat(viewport.getScale()).isEqualTo(3.5f);
        } catch (Exception e) {
            fail("Failed to invoke updateCamera", e);
        }
    }
}
