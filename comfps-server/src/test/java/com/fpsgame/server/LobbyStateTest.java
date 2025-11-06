package com.fpsgame.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * LobbyState 단위 테스트 (현재 API 기준)
 */
public class LobbyStateTest {

    private LobbyState lobby;

    @BeforeEach
    void setUp() {
        lobby = new LobbyState();
    }

    // join/leave가 로비 인원 수(size)에 반영되는지 확인
    @Test
    void joinsAndLeavesAffectSize() {
        assertEquals(0, lobby.size());
        lobby.join(1);
        lobby.join(2);
        assertEquals(2, lobby.size());
        lobby.leave(1);
        assertEquals(1, lobby.size());
    }

    // setReady/isReady: 플레이어별 준비 상태 토글 확인
    @Test
    void readyFlagsPerPlayer() {
        lobby.join(10);
        assertFalse(lobby.isReady(10));
        lobby.setReady(10, true);
        assertTrue(lobby.isReady(10));
        lobby.setReady(10, false);
        assertFalse(lobby.isReady(10));
    }

    // isEveryoneReady: 최소 1명 이상, 전원이 true일 때만 true
    @Test
    void everyoneReadyRequiresAllTrueAndAtLeastOne() {
        assertFalse(lobby.isEveryoneReady());
        lobby.join(1);
        lobby.join(2);
        lobby.setReady(1, true);
        assertFalse(lobby.isEveryoneReady());
        lobby.setReady(2, true);
        assertTrue(lobby.isEveryoneReady());
    }

    // resetReady: 모든 준비 플래그 초기화
    @Test
    void resetReadyClearsAllFlags() {
        lobby.join(1);
        lobby.join(2);
        lobby.setReady(1, true);
        lobby.setReady(2, true);
        assertTrue(lobby.isReady(1));
        assertTrue(lobby.isReady(2));
        lobby.resetReady();
        assertFalse(lobby.isReady(1));
        assertFalse(lobby.isReady(2));
    }

    // resetAll: 로비 인원 전체 초기화 및 준비 상태 초기화
    @Test
    void resetAllClearsLobby() {
        lobby.join(1);
        lobby.join(2);
        assertEquals(2, lobby.size());
        lobby.resetAll();
        assertEquals(0, lobby.size());
        assertFalse(lobby.isEveryoneReady());
    }
}
