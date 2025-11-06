package com.fpsgame.server;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fpsgame.common.GameEnums;
import com.fpsgame.common.SnapshotV2;
import com.fpsgame.common.Vec2;
import com.fpsgame.common.character.Character;

/**
 * PlayerSyncService 테스트
 * 
 * 목적: 캐릭터 위치 판단 및 스냅샷 생성의 정확성 검증
 * - 유효한 플레이어만 스냅샷에 포함되는지 확인
 * - 유령 세션(연결 해제된 세션)이 제거되는지 확인
 * - 캐릭터 정보가 올바르게 동기화되는지 확인
 */
class PlayerSyncServiceTest {

    private PlayerSyncService service;
    private MockCharacterProvider characterProvider;

    @BeforeEach
    void setUp() {
        service = new PlayerSyncService();
        characterProvider = new MockCharacterProvider();
        service.setCharacterProvider(characterProvider);
        service.setBounds(0f, 3000f, 0f, 2000f);
        service.setMoveSpeed(150f);
    }

    @Test
    @DisplayName("플레이어 추가 시 캐릭터가 있으면 해당 위치로 스폰")
    void testAddPlayerWithCharacter() throws IOException {
        // Given: 캐릭터가 특정 위치에 있음
        Character mockChar = new MockCharacter(1, GameEnums.Team.RED, new Vec2(450f, 1700f));
        characterProvider.addCharacter(1, mockChar);

        // When: 플레이어 추가
        service.addPlayer(1);

        // Then: 스냅샷에 캐릭터 위치가 반영됨
        byte[] snapshot = service.buildSnapshotFramePayload();
        List<SnapshotV2.Entry> entries = SnapshotV2.parse(snapshot);
        
        assertEquals(1, entries.size(), "플레이어 1명이어야 함");
        SnapshotV2.Entry entry = entries.get(0);
        assertEquals(1, entry.id, "플레이어 ID가 1이어야 함");
        assertEquals(450f, entry.x, 0.1f, "X 좌표가 450이어야 함");
        assertEquals(1700f, entry.y, 0.1f, "Y 좌표가 1700이어야 함");
        assertEquals(0, entry.team, "RED 팀(0)이어야 함");
    }

    @Test
    @DisplayName("플레이어 추가 시 캐릭터가 없으면 맵 중앙 근처 스폰")
    void testAddPlayerWithoutCharacter() throws IOException {
        // Given: 캐릭터 없음
        // When: 플레이어 추가
        service.addPlayer(1);

        // Then: 맵 중앙(1500, 1000) 근처에 스폰
        byte[] snapshot = service.buildSnapshotFramePayload();
        List<SnapshotV2.Entry> entries = SnapshotV2.parse(snapshot);
        
        assertEquals(1, entries.size());
        SnapshotV2.Entry entry = entries.get(0);
        assertTrue(entry.x >= 1400f && entry.x <= 1600f, "X가 1500 ±100 범위여야 함");
        assertTrue(entry.y >= 900f && entry.y <= 1100f, "Y가 1000 ±100 범위여야 함");
    }

    @Test
    @DisplayName("플레이어 제거 시 스냅샷에서 사라짐")
    void testRemovePlayer() throws IOException {
        // Given: 플레이어 2명 추가
        service.addPlayer(1);
        service.addPlayer(2);

        // When: 1번 플레이어 제거
        service.removePlayer(1);

        // Then: 스냅샷에 2번만 존재
        byte[] snapshot = service.buildSnapshotFramePayload();
        List<SnapshotV2.Entry> entries = SnapshotV2.parse(snapshot);
        
        assertEquals(1, entries.size(), "플레이어 1명만 남아야 함");
        assertEquals(2, entries.get(0).id, "2번 플레이어만 존재해야 함");
    }

    @Test
    @DisplayName("존재하지 않는 플레이어 제거 시 예외 없이 무시")
    void testRemoveNonexistentPlayer() {
        // When & Then: 예외 발생하지 않음
        assertDoesNotThrow(() -> service.removePlayer(999));
    }

    @Test
    @DisplayName("스냅샷에 HP와 스킬 쿨다운 정보 포함")
    void testSnapshotIncludesHealthAndCooldowns() throws IOException {
        // Given: HP 50, 스킬 쿨다운 있는 캐릭터
        MockCharacter mockChar = new MockCharacter(1, GameEnums.Team.RED, new Vec2(100f, 200f));
        mockChar.setHealth(50f);
        mockChar.setTacticalCooldown(3.5f);
        mockChar.setUltimateCooldown(10.0f);
        characterProvider.addCharacter(1, mockChar);
        service.addPlayer(1);

        // When: 스냅샷 생성
        byte[] snapshot = service.buildSnapshotFramePayload();
        List<SnapshotV2.Entry> entries = SnapshotV2.parse(snapshot);

        // Then: HP와 쿨다운 정보 확인
        SnapshotV2.Entry entry = entries.get(0);
        assertEquals(50, entry.hp, "HP가 50이어야 함");
        assertEquals(3.5f, entry.tacticalCd, 0.01f, "전술 스킬 쿨다운이 3.5초여야 함");
        assertEquals(10.0f, entry.ultimateCd, 0.01f, "궁극기 쿨다운이 10초여야 함");
    }

    @Test
    @DisplayName("틱 업데이트 시 플레이어 위치 이동")
    void testTickUpdatesPosition() throws IOException {
        // Given: 플레이어 추가 및 입력 설정
        service.addPlayer(1);
        byte[] input = new byte[] { 0x01 }; // W키 (위로)
        service.onInputFrame(1, input);

        // When: 1초 경과
        service.tick(1.0f);

        // Then: Y 좌표가 감소 (위로 이동, moveSpeed=150)
        byte[] snapshot = service.buildSnapshotFramePayload();
        List<SnapshotV2.Entry> entries = SnapshotV2.parse(snapshot);
        SnapshotV2.Entry entry = entries.get(0);
        
        // 초기 위치(1500,1000)에서 위로 150 이동 → y=850
        assertTrue(entry.y < 1000f, "Y 좌표가 감소해야 함 (위로 이동)");
    }

    @Test
    @DisplayName("맵 경계를 벗어나면 클램핑")
    void testBoundaryClamp() throws IOException {
        // Given: 플레이어를 맵 경계 근처에 배치
        Character mockChar = new MockCharacter(1, GameEnums.Team.RED, new Vec2(2990f, 10f));
        characterProvider.addCharacter(1, mockChar);
        service.addPlayer(1);
        service.setPosition(1, 2990f, 10f);

        // When: 오른쪽 위로 이동 (D키 + W키)
        byte[] input = new byte[] { (byte)(0x01 | 0x08) }; // W + D
        service.onInputFrame(1, input);
        service.tick(1.0f); // 150 유닛 이동

        // Then: 맵 경계에 클램핑
        byte[] snapshot = service.buildSnapshotFramePayload();
        List<SnapshotV2.Entry> entries = SnapshotV2.parse(snapshot);
        SnapshotV2.Entry entry = entries.get(0);
        
        assertTrue(entry.x <= 3000f, "X가 맵 경계(3000)를 넘지 않아야 함");
        assertTrue(entry.y >= 0f, "Y가 맵 경계(0)보다 작지 않아야 함");
    }

    @Test
    @DisplayName("여러 플레이어 동시 관리")
    void testMultiplePlayers() throws IOException {
        // Given: 3명의 플레이어 (각각 다른 팀)
        Character char1 = new MockCharacter(1, GameEnums.Team.RED, new Vec2(100f, 100f));
        Character char2 = new MockCharacter(2, GameEnums.Team.BLUE, new Vec2(200f, 200f));
        Character char3 = new MockCharacter(3, GameEnums.Team.RED, new Vec2(300f, 300f));
        
        characterProvider.addCharacter(1, char1);
        characterProvider.addCharacter(2, char2);
        characterProvider.addCharacter(3, char3);
        
        service.addPlayer(1);
        service.addPlayer(2);
        service.addPlayer(3);

        // When: 스냅샷 생성
        byte[] snapshot = service.buildSnapshotFramePayload();
        Map<Integer, SnapshotV2.Entry> entries = SnapshotV2.parseToMap(snapshot);

        // Then: 3명 모두 포함, 각자 팀 정보 유지
        assertEquals(3, entries.size(), "3명의 플레이어가 있어야 함");
        assertEquals(0, entries.get(1).team, "1번은 RED(0)");
        assertEquals(1, entries.get(2).team, "2번은 BLUE(1)");
        assertEquals(0, entries.get(3).team, "3번은 RED(0)");
    }

    @Test
    @DisplayName("빈 스냅샷 생성 (플레이어 없음)")
    void testEmptySnapshot() throws IOException {
        // When: 플레이어 없이 스냅샷 생성
        byte[] snapshot = service.buildSnapshotFramePayload();
        List<SnapshotV2.Entry> entries = SnapshotV2.parse(snapshot);

        // Then: 빈 리스트
        assertEquals(0, entries.size(), "플레이어가 없어야 함");
    }

    // ===== Mock 클래스 =====

    private static class MockCharacterProvider implements PlayerSyncService.CharacterProvider {
        private final java.util.Map<Integer, Character> characters = new java.util.HashMap<>();

        void addCharacter(int sessionId, Character character) {
            characters.put(sessionId, character);
        }

        @Override
        public Character get(int sessionId) {
            return characters.get(sessionId);
        }
    }

    private static class MockCharacter extends Character {
        private float tacticalCd = 0f;
        private float ultimateCd = 0f;

        MockCharacter(int id, GameEnums.Team team, Vec2 position) {
            super(GameEnums.CharacterId.values()[id % GameEnums.CharacterId.values().length], team, position);
            initialize();
        }

        @Override
        protected void initializeStats() {
            // 기본 스탯
        }

        @Override
        protected void initializeAbilities() {
            // 기본 능력
        }

        void setHealth(float hp) {
            this.currentHealth = hp;
        }

        void setTacticalCooldown(float cd) {
            this.tacticalCd = cd;
        }

        void setUltimateCooldown(float cd) {
            this.ultimateCd = cd;
        }

        @Override
        public com.fpsgame.common.character.ability.TacticalAbility getTacticalAbility() {
            return new MockTacticalAbility(tacticalCd);
        }

        @Override
        public com.fpsgame.common.character.ability.UltimateAbility getUltimateAbility() {
            return new MockUltimateAbility(ultimateCd);
        }
    }

    private static class MockTacticalAbility extends com.fpsgame.common.character.ability.TacticalAbility {
        private final float cd;

        MockTacticalAbility(float cooldown) {
            super(10f, 1f);
            this.cd = cooldown;
        }

        @Override
        public float getCooldown() {
            return cd;
        }

        @Override
        public void activate(Character source, Vec2 direction) {}

        @Override
        protected void updateActiveEffect(float deltaTime) {}
    }

    private static class MockUltimateAbility extends com.fpsgame.common.character.ability.UltimateAbility {
        private final float cd;

        MockUltimateAbility(float cooldown) {
            super(10f, 1f, 5f);
            this.cd = cooldown;
        }

        @Override
        public float getCooldown() {
            return cd;
        }

        @Override
        public void activate(Character source, Vec2 direction) {}

        @Override
        protected void updateUltimateEffect(float deltaTime) {}
    }
}
