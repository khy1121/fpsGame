package com.fpsgame.server;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fpsgame.common.SnapshotV2;

/**
 * 다중 세션 입력 처리 테스트: 두 플레이어가 서로 다른 입력을 보낼 때
 * 각자의 위치만 업데이트되고 다른 플레이어에게 영향을 주지 않는지 검증.
 * 
 * 문제 시나리오: "내 캐릭터를 조종하면 다른 캐릭터도 같이 조종된다"
 * 
 * 테스트 순서:
 * 1. 두 세션(session1=100, session2=200) 추가
 * 2. session1은 오른쪽(D), session2는 위쪽(W) 입력 전송
 * 3. tick 실행 후 스냅샷 생성
 * 4. session1의 x좌표만 증가, session2의 y좌표만 감소하는지 확인
 */
@DisplayName("다중 세션 입력 처리 테스트 - 독립적 움직임 검증")
class PlayerSyncMultiSessionTest {

    private PlayerSyncService sync;

    @BeforeEach
    void setUp() {
        sync = new PlayerSyncService();
        sync.setBounds(0f, 3000f, 0f, 2000f);
        sync.setMoveSpeed(100f); // 초당 100 유닛
    }

    @Test
    @DisplayName("두 세션이 각각 다른 입력을 보낼 때 독립적으로 움직임")
    void testIndependentMovement() throws IOException {
        // Given: 두 플레이어 추가
        int session1 = 100;
        int session2 = 200;
        
        sync.addPlayer(session1);
        sync.addPlayer(session2);
        
        // 초기 위치 설정 (다르게 배치)
        sync.setPosition(session1, 1000f, 1000f);
        sync.setPosition(session2, 2000f, 1000f);
        
        // When: session1은 오른쪽(D)으로, session2는 위쪽(W)으로 입력
        byte[] input1 = new byte[]{ 0x08 }; // D키 (Right)
        byte[] input2 = new byte[]{ 0x01 }; // W키 (Up)
        
        sync.onInputFrame(session1, input1);
        sync.onInputFrame(session2, input2);
        
        // 0.1초 틱 실행 (100 units/s * 0.1s = 10 units)
        sync.tick(0.1f);
        
        // Then: 스냅샷에서 각 플레이어 위치 확인
        byte[] snapshot = sync.buildSnapshotFramePayload();
        List<SnapshotV2.Entry> entries = SnapshotV2.parse(snapshot);
        
        // 두 플레이어 모두 스냅샷에 포함되어야 함
        assertEquals(2, entries.size(), "스냅샷에 두 플레이어 모두 포함되어야 함");
        
        // 각 플레이어 정보 추출
        SnapshotV2.Entry p1 = entries.stream().filter(e -> e.id == session1).findFirst()
                .orElseThrow(() -> new AssertionError("session1이 스냅샷에 없음"));
        SnapshotV2.Entry p2 = entries.stream().filter(e -> e.id == session2).findFirst()
                .orElseThrow(() -> new AssertionError("session2가 스냅샷에 없음"));
        
        // session1은 오른쪽으로 이동 (x 증가, y 불변)
        assertEquals(1000f + 10f, p1.x, 0.5f, "session1의 x좌표는 오른쪽으로 10 증가해야 함");
        assertEquals(1000f, p1.y, 0.5f, "session1의 y좌표는 변경되지 않아야 함");
        
        // session2는 위쪽으로 이동 (y 감소, x 불변)
        assertEquals(2000f, p2.x, 0.5f, "session2의 x좌표는 변경되지 않아야 함");
        assertEquals(1000f - 10f, p2.y, 0.5f, "session2의 y좌표는 위쪽으로 10 감소해야 함");
    }

    @Test
    @DisplayName("한 세션만 입력을 보낼 때 다른 세션은 정지 상태 유지")
    void testOnePlayerMoving() throws IOException {
        // Given
        int session1 = 100;
        int session2 = 200;
        
        sync.addPlayer(session1);
        sync.addPlayer(session2);
        
        sync.setPosition(session1, 1500f, 1000f);
        sync.setPosition(session2, 1500f, 1500f);
        
        // When: session1만 입력 (오른쪽)
        byte[] input = new byte[]{ 0x08 }; // D키
        sync.onInputFrame(session1, input);
        // session2는 입력 없음
        
        sync.tick(0.1f);
        
        // Then
        byte[] snapshot = sync.buildSnapshotFramePayload();
        List<SnapshotV2.Entry> entries = SnapshotV2.parse(snapshot);
        
        SnapshotV2.Entry p1 = entries.stream().filter(e -> e.id == session1).findFirst()
                .orElseThrow(() -> new AssertionError("session1 missing"));
        SnapshotV2.Entry p2 = entries.stream().filter(e -> e.id == session2).findFirst()
                .orElseThrow(() -> new AssertionError("session2 missing"));
        
        // session1은 이동
        assertEquals(1500f + 10f, p1.x, 0.5f, "session1은 오른쪽으로 이동해야 함");
        assertEquals(1000f, p1.y, 0.5f);
        
        // session2는 정지
        assertEquals(1500f, p2.x, 0.5f, "session2의 x좌표는 변경되지 않아야 함");
        assertEquals(1500f, p2.y, 0.5f, "session2의 y좌표는 변경되지 않아야 함");
    }

    @Test
    @DisplayName("두 세션이 동시에 같은 방향으로 움직일 때 독립적으로 같은 거리 이동")
    void testBothMovingSameDirection() throws IOException {
        // Given
        int session1 = 100;
        int session2 = 200;
        
        sync.addPlayer(session1);
        sync.addPlayer(session2);
        
        sync.setPosition(session1, 500f, 500f);
        sync.setPosition(session2, 1500f, 1500f);
        
        // When: 둘 다 오른쪽 입력
        byte[] input = new byte[]{ 0x08 }; // D키
        sync.onInputFrame(session1, input);
        sync.onInputFrame(session2, input);
        
        sync.tick(0.1f);
        
        // Then
        byte[] snapshot = sync.buildSnapshotFramePayload();
        List<SnapshotV2.Entry> entries = SnapshotV2.parse(snapshot);
        
        SnapshotV2.Entry p1 = entries.stream().filter(e -> e.id == session1).findFirst().orElseThrow();
        SnapshotV2.Entry p2 = entries.stream().filter(e -> e.id == session2).findFirst().orElseThrow();
        
        // 둘 다 같은 거리 이동하지만 절대 위치는 다름
        assertEquals(500f + 10f, p1.x, 0.5f, "session1은 초기 위치(500)에서 10 이동");
        assertEquals(500f, p1.y, 0.5f);
        
        assertEquals(1500f + 10f, p2.x, 0.5f, "session2는 초기 위치(1500)에서 10 이동");
        assertEquals(1500f, p2.y, 0.5f);
    }

    @Test
    @DisplayName("세션 추가/제거 후에도 나머지 세션은 정상 동작")
    void testAddRemoveSessions() throws IOException {
        // Given
        sync.addPlayer(1);
        sync.addPlayer(2);
        sync.addPlayer(3);
        
        sync.setPosition(1, 100f, 100f);
        sync.setPosition(2, 200f, 200f);
        sync.setPosition(3, 300f, 300f);
        
        // When: session 2 제거 후, session 1과 3만 입력
        sync.removePlayer(2);
        
        sync.onInputFrame(1, new byte[]{ 0x08 }); // session 1: 오른쪽
        sync.onInputFrame(3, new byte[]{ 0x04 }); // session 3: 왼쪽
        
        sync.tick(0.1f);
        
        // Then
        byte[] snapshot = sync.buildSnapshotFramePayload();
        List<SnapshotV2.Entry> entries = SnapshotV2.parse(snapshot);
        
        assertEquals(2, entries.size(), "session 2가 제거되어 2개만 남아야 함");
        
        SnapshotV2.Entry p1 = entries.stream().filter(e -> e.id == 1).findFirst().orElseThrow();
        SnapshotV2.Entry p3 = entries.stream().filter(e -> e.id == 3).findFirst().orElseThrow();
        
        assertEquals(100f + 10f, p1.x, 0.5f, "session 1은 오른쪽 이동");
        assertEquals(300f - 10f, p3.x, 0.5f, "session 3은 왼쪽 이동");
    }
}
