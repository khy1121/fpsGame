package com.fpsgame.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fpsgame.common.GameEnums;
import com.fpsgame.common.character.Character;

/**
 * SessionRegistry 단위 테스트 (현재 API 기준)
 */
public class SessionRegistryTest {

    private SessionRegistry registry;

    @BeforeEach
    void setUp() {
        // Provide a router with no-op hooks; we'll not exercise network I/O here.
        DefaultServerRouter router = new DefaultServerRouter(new DefaultServerRouter.Hooks(){});
        registry = new SessionRegistry(router);
    }

    // 닉네임: 기본 닉네임 생성 및 명시적 설정/조회 확인
    @Test
    void nicknameRegistrationAndDefaults() {
        assertTrue(registry.getNickname(1234).startsWith("Player"));
        registry.setNickname(42, "Alice");
        assertEquals("Alice", registry.getNickname(42));
    }

    // setMapId: 월드가 생성되는지 확인
    @Test
    void worldIsCreatedOnMapIdSet() {
        assertNull(registry.getWorld());
        registry.setMapId(0); // TERMINAL
        assertNotNull(registry.getWorld());
    }

    // 캐릭터 생성: 팀 배정(인덱스 기반 RED/BLUE) 확인
    @Test
    void createCharacterAssignsTeamFromIndex() {
        // Ensure world exists (spawn uses world size as hint)
        registry.setMapId(0);
        Character cRed = registry.createOrUpdateCharacter(1, 0, 0);
        assertNotNull(cRed);
        assertEquals(GameEnums.Team.RED, cRed.getTeam());

        Character cBlue = registry.createOrUpdateCharacter(2, 1, 0);
        assertNotNull(cBlue);
        assertEquals(GameEnums.Team.BLUE, cBlue.getTeam());
    }

    // 조준각: 동기화 서비스가 없을 때 기본값 0f 확인
    @Test
    void aimAngleDefaultsToZeroWithoutSyncService() {
        assertEquals(0f, registry.getAimAngle(999));
    }
}
