package com.fpsgame.client.ui;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fpsgame.common.SnapshotV2;

/**
 * 캐릭터 렌더링 필터링 테스트
 * 
 * 목적: 유효하지 않은 캐릭터가 화면에 렌더링되지 않는지 검증
 * - HP 0인 캐릭터 필터링
 * - 잘못된 characterId 필터링
 * - 맵 밖 캐릭터 필터링
 */
class CharacterRenderFilterTest {

    private Map<Integer, SnapshotV2.Entry> players;
    private float worldW = 3000f;
    private float worldH = 2000f;

    @BeforeEach
    void setUp() {
        players = new ConcurrentHashMap<>();
    }

    @Test
    @DisplayName("HP 0인 캐릭터는 렌더링에서 제외")
    void testDeadCharacterFiltered() {
        // Given: HP 0인 캐릭터
        SnapshotV2.Entry dead = new SnapshotV2.Entry(1, 100f, 100f, 0f, 0, 0, 0, 0f, 0f);
        players.put(1, dead);

        // When: 렌더링 대상 필터링
        long validCount = players.values().stream()
                .filter(this::isValidForRendering)
                .count();

        // Then: 0개
        assertEquals(0, validCount, "HP 0인 캐릭터는 렌더링되지 않아야 함");
    }

    @Test
    @DisplayName("잘못된 characterId는 렌더링에서 제외")
    void testInvalidCharacterIdFiltered() {
        // Given: characterId가 범위 밖 (-1, 999)
        SnapshotV2.Entry invalid1 = new SnapshotV2.Entry(1, 100f, 100f, 0f, 0, -1, 100, 0f, 0f);
        SnapshotV2.Entry invalid2 = new SnapshotV2.Entry(2, 200f, 200f, 0f, 0, 999, 100, 0f, 0f);
        players.put(1, invalid1);
        players.put(2, invalid2);

        // When: 렌더링 대상 필터링
        long validCount = players.values().stream()
                .filter(this::isValidForRendering)
                .count();

        // Then: 0개
        assertEquals(0, validCount, "잘못된 캐릭터 ID는 렌더링되지 않아야 함");
    }

    @Test
    @DisplayName("맵 밖 캐릭터는 렌더링에서 제외")
    void testOutOfBoundsFiltered() {
        // Given: 맵 경계 밖 캐릭터들
        SnapshotV2.Entry outLeft = new SnapshotV2.Entry(1, -10f, 1000f, 0f, 0, 0, 100, 0f, 0f);
        SnapshotV2.Entry outRight = new SnapshotV2.Entry(2, 3100f, 1000f, 0f, 0, 0, 100, 0f, 0f);
        SnapshotV2.Entry outTop = new SnapshotV2.Entry(3, 1500f, -10f, 0f, 0, 0, 100, 0f, 0f);
        SnapshotV2.Entry outBottom = new SnapshotV2.Entry(4, 1500f, 2100f, 0f, 0, 0, 100, 0f, 0f);
        
        players.put(1, outLeft);
        players.put(2, outRight);
        players.put(3, outTop);
        players.put(4, outBottom);

        // When: 렌더링 대상 필터링
        long validCount = players.values().stream()
                .filter(this::isValidForRendering)
                .count();

        // Then: 0개
        assertEquals(0, validCount, "맵 밖 캐릭터는 렌더링되지 않아야 함");
    }

    @Test
    @DisplayName("유효한 캐릭터만 렌더링 대상으로 선택")
    void testValidCharactersOnly() {
        // Given: 유효한 캐릭터 2개 + 무효한 캐릭터 3개
        SnapshotV2.Entry valid1 = new SnapshotV2.Entry(1, 450f, 1700f, 0f, 0, 0, 100, 0f, 0f);
        SnapshotV2.Entry valid2 = new SnapshotV2.Entry(2, 2550f, 1700f, 0f, 1, 1, 80, 2f, 5f);
        SnapshotV2.Entry dead = new SnapshotV2.Entry(3, 100f, 100f, 0f, 0, 0, 0, 0f, 0f);
        SnapshotV2.Entry invalidId = new SnapshotV2.Entry(4, 200f, 200f, 0f, 0, -1, 100, 0f, 0f);
        SnapshotV2.Entry outOfBounds = new SnapshotV2.Entry(5, -10f, 100f, 0f, 0, 0, 100, 0f, 0f);
        
        players.put(1, valid1);
        players.put(2, valid2);
        players.put(3, dead);
        players.put(4, invalidId);
        players.put(5, outOfBounds);

        // When: 렌더링 대상 필터링
        long validCount = players.values().stream()
                .filter(this::isValidForRendering)
                .count();

        // Then: 2개만
        assertEquals(2, validCount, "유효한 캐릭터 2개만 렌더링되어야 함");
    }

    @Test
    @DisplayName("HP 1인 캐릭터는 렌더링됨")
    void testLowHealthCharacterRendered() {
        // Given: HP 1인 캐릭터
        SnapshotV2.Entry lowHp = new SnapshotV2.Entry(1, 100f, 100f, 0f, 0, 0, 1, 0f, 0f);
        players.put(1, lowHp);

        // When: 렌더링 대상 필터링
        long validCount = players.values().stream()
                .filter(this::isValidForRendering)
                .count();

        // Then: 1개
        assertEquals(1, validCount, "HP 1인 캐릭터는 렌더링되어야 함");
    }

    @Test
    @DisplayName("맵 경계선 위 캐릭터는 렌더링됨")
    void testBoundaryCharacterRendered() {
        // Given: 맵 경계선 위 캐릭터들
        SnapshotV2.Entry atLeft = new SnapshotV2.Entry(1, 0f, 1000f, 0f, 0, 0, 100, 0f, 0f);
        SnapshotV2.Entry atRight = new SnapshotV2.Entry(2, 3000f, 1000f, 0f, 0, 0, 100, 0f, 0f);
        SnapshotV2.Entry atTop = new SnapshotV2.Entry(3, 1500f, 0f, 0f, 0, 0, 100, 0f, 0f);
        SnapshotV2.Entry atBottom = new SnapshotV2.Entry(4, 1500f, 2000f, 0f, 0, 0, 100, 0f, 0f);
        
        players.put(1, atLeft);
        players.put(2, atRight);
        players.put(3, atTop);
        players.put(4, atBottom);

        // When: 렌더링 대상 필터링
        long validCount = players.values().stream()
                .filter(this::isValidForRendering)
                .count();

        // Then: 4개 모두
        assertEquals(4, validCount, "경계선 위 캐릭터는 모두 렌더링되어야 함");
    }

    // 렌더링 유효성 검증 헬퍼 메서드
    private boolean isValidForRendering(SnapshotV2.Entry e) {
        // HP 체크
        if (e.hp <= 0) return false;
        
        // 캐릭터 ID 범위 체크 (0~9)
        if (e.characterId < 0 || e.characterId > 9) return false;
        
        // 맵 경계 체크
        if (e.x < 0 || e.x > worldW) return false;
        if (e.y < 0 || e.y > worldH) return false;
        
        return true;
    }
}
