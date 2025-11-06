package com.fpsgame.server;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fpsgame.common.GameEnums;

/**
 * GameServer 단위 테스트
 */
class GameServerTest {

    private GameServer gameServer;

    @BeforeEach
    void setUp() {
        gameServer = new GameServer(30); // 30 Hz
    }

    // 초기화: 생성 직후 실행 상태가 아님
    @Test
    void testGameServerInitialization() {
        assertNotNull(gameServer);
        assertFalse(gameServer.isRunning());
    }

    // 시작/정지: start/stop 호출로 상태 전환 확인
    @Test
    void testGameServerStartStop() {
        // Start
        gameServer.start();
        assertTrue(gameServer.isRunning());
        
        // Stop
        gameServer.stop(1000);
        assertFalse(gameServer.isRunning());
    }

    // 플레이어 입장/퇴장: 존재하지 않는 ID 퇴장 시 예외 없음
    @Test
    void testPlayerJoinLeave() {
        // Join
        gameServer.onPlayerJoin(1);
        gameServer.onPlayerJoin(2);
        
        // Leave
        gameServer.onPlayerLeave(1);
        // Should not throw exception
        assertDoesNotThrow(() -> gameServer.onPlayerLeave(999)); // Non-existent
    }

    // 맵 투표: 과반 또는 최다 득표 맵이 승자에 반영되는지 확인
    @Test
    void testMapVoting() {
        gameServer.onPlayerJoin(1);
        gameServer.onPlayerJoin(2);
        
        gameServer.voteMap(1, GameEnums.MapId.NEON_CITY);
        gameServer.voteMap(2, GameEnums.MapId.NEON_CITY);
        
        assertEquals(GameEnums.MapId.NEON_CITY, gameServer.currentVoteWinner());
        assertEquals(2, gameServer.voterCount());
    }

    // 투표 초기화: resetVotes 후 투표자 수 0
    @Test
    void testVoteReset() {
        gameServer.onPlayerJoin(1);
        gameServer.voteMap(1, GameEnums.MapId.FOREST_OUTPOST);
        
        gameServer.resetVotes();
        
        assertEquals(0, gameServer.voterCount());
    }

    // 단계 추적: 서버 시작 후 Phase가 유효한지 확인
    @Test
    void testPhaseTracking() {
        gameServer.start();
        GameEnums.Phase phase = gameServer.getPhase();
        assertNotNull(phase);
        gameServer.stop(500);
    }
}
