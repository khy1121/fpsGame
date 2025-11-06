package com.fpsgame.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fpsgame.common.GameEnums.Team;
import com.fpsgame.common.SpawnManager.SpawnPattern;

/**
 * SpawnManager 테스트 케이스
 * 
 * 검증 항목:
 * 1. 4가지 스폰 패턴별 위치 정확성
 * 2. 팀별 정반대 위치 배치
 * 3. 맵 경계 내 위치 (10-20% 마진)
 * 4. 랜덤 패턴 선택
 */
class SpawnManagerTest {
    
    private static final float WORLD_W = 3000f;
    private static final float WORLD_H = 2000f;
    private static final float EPSILON = 0.01f;  // 부동소수점 비교 오차
    
    @Test
    @DisplayName("VERTICAL 패턴: RED(12시/북쪽) vs BLUE(6시/남쪽)")
    void testVerticalPattern() {
        SpawnManager manager = new SpawnManager(WORLD_W, WORLD_H, SpawnPattern.VERTICAL, 42);
        
        Vec2 redSpawn = manager.getRedSpawn();
        Vec2 blueSpawn = manager.getBlueSpawn();
        
        // RED: X는 중앙(40-60%), Y는 상단(10-20%)
        assertTrue(redSpawn.x >= WORLD_W * 0.4f && redSpawn.x <= WORLD_W * 0.6f, 
            "RED X should be in center: " + redSpawn.x);
        assertTrue(redSpawn.y >= WORLD_H * 0.10f && redSpawn.y <= WORLD_H * 0.20f,
            "RED Y should be at top: " + redSpawn.y);
        
        // BLUE: X는 중앙(40-60%), Y는 하단(80-90%)
        assertTrue(blueSpawn.x >= WORLD_W * 0.4f && blueSpawn.x <= WORLD_W * 0.6f,
            "BLUE X should be in center: " + blueSpawn.x);
        assertTrue(blueSpawn.y >= WORLD_H * 0.80f && blueSpawn.y <= WORLD_H * 0.90f,
            "BLUE Y should be at bottom: " + blueSpawn.y);
        
        // 패턴 확인
        assertEquals(SpawnPattern.VERTICAL, manager.getPattern());
        assertTrue(manager.getPatternDescription().contains("VERTICAL"));
    }
    
    @Test
    @DisplayName("HORIZONTAL 패턴: RED(9시/서쪽) vs BLUE(3시/동쪽)")
    void testHorizontalPattern() {
        SpawnManager manager = new SpawnManager(WORLD_W, WORLD_H, SpawnPattern.HORIZONTAL, 123);
        
        Vec2 redSpawn = manager.getRedSpawn();
        Vec2 blueSpawn = manager.getBlueSpawn();
        
        // RED: X는 좌측(10-20%), Y는 중앙(40-60%)
        assertTrue(redSpawn.x >= WORLD_W * 0.10f && redSpawn.x <= WORLD_W * 0.20f,
            "RED X should be at left: " + redSpawn.x);
        assertTrue(redSpawn.y >= WORLD_H * 0.4f && redSpawn.y <= WORLD_H * 0.6f,
            "RED Y should be in center: " + redSpawn.y);
        
        // BLUE: X는 우측(80-90%), Y는 중앙(40-60%)
        assertTrue(blueSpawn.x >= WORLD_W * 0.80f && blueSpawn.x <= WORLD_W * 0.90f,
            "BLUE X should be at right: " + blueSpawn.x);
        assertTrue(blueSpawn.y >= WORLD_H * 0.4f && blueSpawn.y <= WORLD_H * 0.6f,
            "BLUE Y should be in center: " + blueSpawn.y);
        
        assertEquals(SpawnPattern.HORIZONTAL, manager.getPattern());
    }
    
    @Test
    @DisplayName("DIAGONAL_NE_SW 패턴: RED(2시/북동) vs BLUE(8시/남서)")
    void testDiagonalNESWPattern() {
        SpawnManager manager = new SpawnManager(WORLD_W, WORLD_H, SpawnPattern.DIAGONAL_NE_SW, 456);
        
        Vec2 redSpawn = manager.getRedSpawn();
        Vec2 blueSpawn = manager.getBlueSpawn();
        
        // RED: X는 우측 상단(75-90%), Y는 상단(10-20%)
        assertTrue(redSpawn.x >= WORLD_W * 0.75f && redSpawn.x <= WORLD_W * 0.90f,
            "RED X should be at right: " + redSpawn.x);
        assertTrue(redSpawn.y >= WORLD_H * 0.10f && redSpawn.y <= WORLD_H * 0.20f,
            "RED Y should be at top: " + redSpawn.y);
        
        // BLUE: X는 좌측(10-20%), Y는 하단(80-90%)
        assertTrue(blueSpawn.x >= WORLD_W * 0.10f && blueSpawn.x <= WORLD_W * 0.20f,
            "BLUE X should be at left: " + blueSpawn.x);
        assertTrue(blueSpawn.y >= WORLD_H * 0.80f && blueSpawn.y <= WORLD_H * 0.90f,
            "BLUE Y should be at bottom: " + blueSpawn.y);
        
        assertEquals(SpawnPattern.DIAGONAL_NE_SW, manager.getPattern());
    }
    
    @Test
    @DisplayName("DIAGONAL_NW_SE 패턴: RED(10시/북서) vs BLUE(4시/남동)")
    void testDiagonalNWSEPattern() {
        SpawnManager manager = new SpawnManager(WORLD_W, WORLD_H, SpawnPattern.DIAGONAL_NW_SE, 789);
        
        Vec2 redSpawn = manager.getRedSpawn();
        Vec2 blueSpawn = manager.getBlueSpawn();
        
        // RED: X는 좌측(10-20%), Y는 상단(10-20%)
        assertTrue(redSpawn.x >= WORLD_W * 0.10f && redSpawn.x <= WORLD_W * 0.20f,
            "RED X should be at left: " + redSpawn.x);
        assertTrue(redSpawn.y >= WORLD_H * 0.10f && redSpawn.y <= WORLD_H * 0.20f,
            "RED Y should be at top: " + redSpawn.y);
        
        // BLUE: X는 우측(80-90%), Y는 하단(80-90%)
        assertTrue(blueSpawn.x >= WORLD_W * 0.80f && blueSpawn.x <= WORLD_W * 0.90f,
            "BLUE X should be at right: " + blueSpawn.x);
        assertTrue(blueSpawn.y >= WORLD_H * 0.80f && blueSpawn.y <= WORLD_H * 0.90f,
            "BLUE Y should be at bottom: " + blueSpawn.y);
        
        assertEquals(SpawnPattern.DIAGONAL_NW_SE, manager.getPattern());
    }
    
    @Test
    @DisplayName("getSpawn(Team) 메서드로 팀별 스폰 위치 가져오기")
    void testGetSpawnByTeam() {
        SpawnManager manager = new SpawnManager(WORLD_W, WORLD_H, SpawnPattern.VERTICAL, 999);
        
        Vec2 redSpawn1 = manager.getRedSpawn();
        Vec2 redSpawn2 = manager.getSpawn(Team.RED);
        Vec2 blueSpawn1 = manager.getBlueSpawn();
        Vec2 blueSpawn2 = manager.getSpawn(Team.BLUE);
        
        // RED 팀 위치는 다를 수 있음 (랜덤)
        // BLUE 팀 위치도 다를 수 있음 (랜덤)
        // 하지만 범위는 동일해야 함
        assertTrue(redSpawn2.y >= WORLD_H * 0.10f && redSpawn2.y <= WORLD_H * 0.20f);
        assertTrue(blueSpawn2.y >= WORLD_H * 0.80f && blueSpawn2.y <= WORLD_H * 0.90f);
    }
    
    @Test
    @DisplayName("랜덤 패턴 선택 (null 전달 시)")
    void testRandomPatternSelection() {
        SpawnManager manager = new SpawnManager(WORLD_W, WORLD_H, null);
        
        // 패턴이 4가지 중 하나여야 함
        SpawnPattern pattern = manager.getPattern();
        assertNotNull(pattern);
        assertTrue(
            pattern == SpawnPattern.VERTICAL ||
            pattern == SpawnPattern.HORIZONTAL ||
            pattern == SpawnPattern.DIAGONAL_NE_SW ||
            pattern == SpawnPattern.DIAGONAL_NW_SE
        );
    }
    
    @Test
    @DisplayName("랜덤 패턴 선택 (기본 생성자)")
    void testDefaultConstructorRandomPattern() {
        SpawnManager manager = new SpawnManager(WORLD_W, WORLD_H);
        
        SpawnPattern pattern = manager.getPattern();
        assertNotNull(pattern);
        
        // 스폰 위치가 맵 경계 내에 있는지 확인
        Vec2 redSpawn = manager.getRedSpawn();
        Vec2 blueSpawn = manager.getBlueSpawn();
        
        assertTrue(redSpawn.x >= 0 && redSpawn.x <= WORLD_W);
        assertTrue(redSpawn.y >= 0 && redSpawn.y <= WORLD_H);
        assertTrue(blueSpawn.x >= 0 && blueSpawn.x <= WORLD_W);
        assertTrue(blueSpawn.y >= 0 && blueSpawn.y <= WORLD_H);
    }
    
    @Test
    @DisplayName("스폰 위치가 맵 경계를 벗어나지 않음")
    void testSpawnWithinBounds() {
        for (SpawnPattern pattern : SpawnPattern.values()) {
            SpawnManager manager = new SpawnManager(WORLD_W, WORLD_H, pattern, 12345);
            
            Vec2 redSpawn = manager.getRedSpawn();
            Vec2 blueSpawn = manager.getBlueSpawn();
            
            // 경계 검증
            assertTrue(redSpawn.x >= 0 && redSpawn.x <= WORLD_W,
                "RED X out of bounds in " + pattern + ": " + redSpawn.x);
            assertTrue(redSpawn.y >= 0 && redSpawn.y <= WORLD_H,
                "RED Y out of bounds in " + pattern + ": " + redSpawn.y);
            assertTrue(blueSpawn.x >= 0 && blueSpawn.x <= WORLD_W,
                "BLUE X out of bounds in " + pattern + ": " + blueSpawn.x);
            assertTrue(blueSpawn.y >= 0 && blueSpawn.y <= WORLD_H,
                "BLUE Y out of bounds in " + pattern + ": " + blueSpawn.y);
        }
    }
    
    @Test
    @DisplayName("팀별 스폰 위치가 충분히 멀리 떨어져 있음")
    void testSpawnDistanceIsSufficient() {
        for (SpawnPattern pattern : SpawnPattern.values()) {
            SpawnManager manager = new SpawnManager(WORLD_W, WORLD_H, pattern, 54321);
            
            Vec2 redSpawn = manager.getRedSpawn();
            Vec2 blueSpawn = manager.getBlueSpawn();
            
            // 거리 계산
            float dx = blueSpawn.x - redSpawn.x;
            float dy = blueSpawn.y - redSpawn.y;
            float distance = (float) Math.sqrt(dx * dx + dy * dy);
            
            // 패턴별 최소 거리 검증
            // VERTICAL/HORIZONTAL: Y or X 축 거리가 최소 1200 이상
            // DIAGONAL: 대각선 거리가 최소 1200 이상
            float minDistance;
            switch (pattern) {
                case VERTICAL:
                    // Y축 거리: 80-90% - 10-20% = 60-80% → 최소 1200 (worldH * 0.6)
                    minDistance = WORLD_H * 0.6f;
                    assertTrue(Math.abs(dy) >= minDistance,
                        "Vertical distance too small in " + pattern + ": " + Math.abs(dy));
                    break;
                case HORIZONTAL:
                    // X축 거리: 80-90% - 10-20% = 60-80% → 최소 1800 (worldW * 0.6)
                    minDistance = WORLD_W * 0.6f;
                    assertTrue(Math.abs(dx) >= minDistance,
                        "Horizontal distance too small in " + pattern + ": " + Math.abs(dx));
                    break;
                default:
                    // 대각선: 전체 거리가 최소 1200 이상
                    minDistance = 1200f;
                    assertTrue(distance >= minDistance,
                        "Distance too small in " + pattern + ": " + distance + " < " + minDistance);
                    break;
            }
        }
    }
    
    @Test
    @DisplayName("toString() 및 getPatternDescription() 정보 출력")
    void testToStringAndDescription() {
        SpawnManager manager = new SpawnManager(WORLD_W, WORLD_H, SpawnPattern.VERTICAL);
        
        String str = manager.toString();
        assertTrue(str.contains("SpawnManager"));
        assertTrue(str.contains("VERTICAL"));
        assertTrue(str.contains("3000"));
        assertTrue(str.contains("2000"));
        
        String desc = manager.getPatternDescription();
        assertTrue(desc.contains("VERTICAL"));
        assertTrue(desc.contains("12시"));
        assertTrue(desc.contains("6시"));
    }
    
    @Test
    @DisplayName("동일 시드로 생성 시 동일한 스폰 위치 (재현성)")
    void testReproducibilityWithSeed() {
        long seed = 99999;
        
        SpawnManager manager1 = new SpawnManager(WORLD_W, WORLD_H, SpawnPattern.HORIZONTAL, seed);
        Vec2 red1 = manager1.getRedSpawn();
        Vec2 blue1 = manager1.getBlueSpawn();
        
        SpawnManager manager2 = new SpawnManager(WORLD_W, WORLD_H, SpawnPattern.HORIZONTAL, seed);
        Vec2 red2 = manager2.getRedSpawn();
        Vec2 blue2 = manager2.getBlueSpawn();
        
        // 동일 시드 → 동일 위치
        assertEquals(red1.x, red2.x, EPSILON);
        assertEquals(red1.y, red2.y, EPSILON);
        assertEquals(blue1.x, blue2.x, EPSILON);
        assertEquals(blue1.y, blue2.y, EPSILON);
    }
}
