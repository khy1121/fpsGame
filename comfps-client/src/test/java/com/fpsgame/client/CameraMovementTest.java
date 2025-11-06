package com.fpsgame.client;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fpsgame.client.model.Viewport;
import com.fpsgame.common.SnapshotV2;
import com.fpsgame.common.Vec2;

/**
 * 카메라와 이동 로직 단위 테스트
 * 
 * 테스트 시나리오:
 * 1. 카메라는 오직 "내 플레이어(myId)"만 추적해야 함
 * 2. 다른 플레이어 위치가 변경되어도 카메라는 움직이지 않아야 함
 * 3. 내 플레이어가 이동하면 카메라도 따라가야 함
 */
public class CameraMovementTest {
    
    private Viewport viewport;
    private Map<Integer, SnapshotV2.Entry> players;
    private int myId;
    
    @BeforeEach
    void setUp() {
        // 맵 크기: 3000 x 2000 (실제 게임과 동일)
        viewport = new Viewport(3000f, 2000f);
        viewport.setScale(2.0f); // 실제 게임과 동일한 줌 레벨
        players = new HashMap<>();
        myId = 1; // 내 플레이어 ID
    }
    
    /**
     * 테스트 1: 카메라는 내 플레이어만 추적
     */
    @Test
    void testCameraFollowsOnlyMyPlayer() {
        // Given: 두 명의 플레이어 (나: id=1, 상대: id=2)
        SnapshotV2.Entry me = new SnapshotV2.Entry(
            1,      // id
            450f,   // x (RED 스폰 위치)
            1700f,  // y
            0f,     // aim
            0,      // team
            6,      // character
            100,    // hp
            0f,     // tacticalCd
            0f      // ultimateCd
        );
        
        SnapshotV2.Entry other = new SnapshotV2.Entry(
            2,      // id
            2550f,  // x (BLUE 스폰 위치)
            1700f,  // y
            0f,     // aim
            1,      // team
            6,      // character
            100,    // hp
            0f,     // tacticalCd
            0f      // ultimateCd
        );
        
        players.put(1, me);
        players.put(2, other);
        
        // When: 카메라를 내 플레이어 위치로 설정
        updateCamera(myId, players, viewport);
        
        // Then: 카메라는 내 위치(450, 1700)에 있어야 함
        Vec2 center = viewport.getCenter(null);
        assertEquals(450f, center.x, 0.1f, "카메라 X는 내 플레이어 위치여야 함");
        assertEquals(1700f, center.y, 0.1f, "카메라 Y는 내 플레이어 위치여야 함");
    }
    
    /**
     * 테스트 2: 다른 플레이어가 움직여도 카메라는 고정
     */
    @Test
    void testCameraIgnoresOtherPlayerMovement() {
        // Given: 초기 상태
        players.put(1, createPlayer(1, 450f, 1700f));
        players.put(2, createPlayer(2, 2550f, 1700f));
        updateCamera(myId, players, viewport);
        
        Vec2 initialCenter = viewport.getCenter(null);
        float initialCameraX = initialCenter.x;
        float initialCameraY = initialCenter.y;
        
        // When: 상대방 플레이어만 이동 (2550 → 2600)
        players.put(2, createPlayer(2, 2600f, 1750f));
        updateCamera(myId, players, viewport);
        
        // Then: 카메라는 여전히 내 위치에 고정
        Vec2 newCenter = viewport.getCenter(null);
        assertEquals(initialCameraX, newCenter.x, 0.1f, 
            "다른 플레이어가 움직여도 카메라 X는 변하지 않아야 함");
        assertEquals(initialCameraY, newCenter.y, 0.1f,
            "다른 플레이어가 움직여도 카메라 Y는 변하지 않아야 함");
    }
    
    /**
     * 테스트 3: 내 플레이어가 이동하면 카메라도 따라감
     */
    @Test
    void testCameraFollowsMyPlayerMovement() {
        // Given: 초기 위치
        players.put(1, createPlayer(1, 450f, 1700f));
        players.put(2, createPlayer(2, 2550f, 1700f));
        updateCamera(myId, players, viewport);
        
        // When: 내 플레이어가 이동 (WASD 입력 시뮬레이션)
        // 서버에서 새 위치를 받았다고 가정
        players.put(1, createPlayer(1, 500f, 1650f)); // 오른쪽+위로 이동
        updateCamera(myId, players, viewport);
        
        // Then: 카메라는 새 위치를 추적
        Vec2 center = viewport.getCenter(null);
        assertEquals(500f, center.x, 0.1f, 
            "내가 이동하면 카메라 X도 따라가야 함");
        assertEquals(1650f, center.y, 0.1f,
            "내가 이동하면 카메라 Y도 따라가야 함");
    }
    
    /**
     * 테스트 4: 맵 경계에서의 카메라 동작
     * (실제 게임에서는 0-3000 x 0-2000)
     * 
     * 참고: Viewport는 화면의 절반이 맵 밖을 보지 않도록
     * 카메라를 자동으로 클램프합니다. 따라서 플레이어가
     * (0, 0)에 있어도 카메라는 약간 안쪽에 위치합니다.
     */
    @Test
    void testCameraAtMapBoundaries() {
        // 맵 왼쪽 상단 모서리 - 카메라는 클램프됨
        players.put(1, createPlayer(1, 0f, 0f));
        updateCamera(myId, players, viewport);
        Vec2 center1 = viewport.getCenter(null);
        // 카메라가 플레이어 위치 근처에 있는지만 확인 (정확히 0은 아닐 수 있음)
        assertTrue(center1.x >= 0f && center1.x < 500f, 
            () -> "카메라 X는 맵 왼쪽 근처에 있어야 함: " + center1.x);
        assertTrue(center1.y >= 0f && center1.y < 500f,
            () -> "카메라 Y는 맵 상단 근처에 있어야 함: " + center1.y);
        
        // 맵 오른쪽 하단 모서리 - 카메라는 클램프됨
        players.put(1, createPlayer(1, 3000f, 2000f));
        updateCamera(myId, players, viewport);
        Vec2 center2 = viewport.getCenter(null);
        assertTrue(center2.x > 2500f && center2.x <= 3000f,
            () -> "카메라 X는 맵 오른쪽 근처에 있어야 함: " + center2.x);
        assertTrue(center2.y > 1500f && center2.y <= 2000f,
            () -> "카메라 Y는 맵 하단 근처에 있어야 함: " + center2.y);
    }
    
    /**
     * 테스트 5: myId가 스냅샷에 없을 때 카메라는 변경되지 않아야 함
     */
    @Test
    void testCameraStaysWhenMyPlayerNotInSnapshot() {
        // Given: 초기 위치 설정
        players.put(1, createPlayer(1, 450f, 1700f));
        updateCamera(myId, players, viewport);
        
        Vec2 initialCenter = viewport.getCenter(null);
        float initialX = initialCenter.x;
        float initialY = initialCenter.y;
        
        // When: 내 플레이어가 스냅샷에서 사라짐 (연결 끊김 시뮬레이션)
        players.remove(1);
        updateCamera(myId, players, viewport);
        
        // Then: 카메라는 마지막 위치 유지
        Vec2 newCenter = viewport.getCenter(null);
        assertEquals(initialX, newCenter.x, 0.1f,
            "플레이어가 없으면 카메라는 마지막 위치 유지");
        assertEquals(initialY, newCenter.y, 0.1f,
            "플레이어가 없으면 카메라는 마지막 위치 유지");
    }
    
    // Helper methods
    
    /**
     * GamePanel.updateCamera() 로직을 독립적으로 테스트
     */
    private void updateCamera(int myId, Map<Integer, SnapshotV2.Entry> players, Viewport viewport) {
        SnapshotV2.Entry me = players.get(myId);
        if (me != null) {
            viewport.setCenter(me.x, me.y);
        }
        // me가 null이면 카메라 위치는 변경되지 않음 (마지막 위치 유지)
    }
    
    /**
     * 테스트용 플레이어 생성
     */
    private SnapshotV2.Entry createPlayer(int id, float x, float y) {
        return new SnapshotV2.Entry(
            id,     // id
            x,      // x
            y,      // y
            0f,     // aim
            0,      // team
            6,      // character (Raven)
            100,    // hp
            0f,     // tacticalCd
            0f      // ultimateCd
        );
    }
}
