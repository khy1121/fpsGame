package com.fpsgame.common;

import java.util.Random;

/**
 * SpawnManager: 팀별 스폰 위치를 시계 방향 정반대 위치에 랜덤으로 배치하는 유틸리티 클래스
 * 
 * 지원하는 스폰 패턴:
 * - VERTICAL: RED 북쪽(12시), BLUE 남쪽(6시)
 * - HORIZONTAL: RED 서쪽(9시), BLUE 동쪽(3시)
 * - DIAGONAL_NE_SW: RED 북동쪽(2시), BLUE 남서쪽(8시)
 * - DIAGONAL_NW_SE: RED 북서쪽(10시), BLUE 남동쪽(4시)
 * 
 * 각 팀의 스폰 위치는 맵 경계에서 일정 거리 안쪽(10-20% 범위)에 랜덤하게 배치됩니다.
 */
public class SpawnManager {
    
    /** 스폰 패턴 열거형 */
    public enum SpawnPattern {
        VERTICAL,        // RED: 12시 (북쪽), BLUE: 6시 (남쪽)
        HORIZONTAL,      // RED: 9시 (서쪽), BLUE: 3시 (동쪽)
        DIAGONAL_NE_SW,  // RED: 2시 (북동), BLUE: 8시 (남서)
        DIAGONAL_NW_SE   // RED: 10시 (북서), BLUE: 4시 (남동)
    }
    
    private final float worldW;
    private final float worldH;
    private final SpawnPattern pattern;
    private final Random random;
    
    // 스폰 위치 안전 마진 (맵 경계에서 10-20% 거리)
    private static final float MIN_MARGIN = 0.10f;  // 10%
    private static final float MAX_MARGIN = 0.20f;  // 20%
    
    /**
     * SpawnManager 생성자
     * 
     * @param worldW 맵 너비
     * @param worldH 맵 높이
     * @param pattern 스폰 패턴 (null인 경우 랜덤 선택)
     */
    public SpawnManager(float worldW, float worldH, SpawnPattern pattern) {
        this.worldW = worldW;
        this.worldH = worldH;
        this.random = new Random();
        
        // 패턴이 null이면 랜덤 선택
        if (pattern == null) {
            SpawnPattern[] patterns = SpawnPattern.values();
            this.pattern = patterns[random.nextInt(patterns.length)];
        } else {
            this.pattern = pattern;
        }
    }
    
    /**
     * 랜덤 패턴으로 SpawnManager 생성 (기본 생성자)
     */
    public SpawnManager(float worldW, float worldH) {
        this(worldW, worldH, null);
    }
    
    /**
     * 시드값을 지정한 SpawnManager 생성 (테스트용)
     */
    public SpawnManager(float worldW, float worldH, SpawnPattern pattern, long seed) {
        this.worldW = worldW;
        this.worldH = worldH;
        this.pattern = (pattern != null) ? pattern : SpawnPattern.values()[new Random(seed).nextInt(4)];
        this.random = new Random(seed);
    }
    
    /**
     * RED 팀의 스폰 위치 계산
     * 
     * @return Vec2 스폰 좌표
     */
    public Vec2 getRedSpawn() {
        switch (pattern) {
            case VERTICAL:
                // 12시 방향 (북쪽 상단)
                return new Vec2(
                    worldW * (0.4f + random.nextFloat() * 0.2f),  // X: 40-60% (중앙)
                    worldH * (MIN_MARGIN + random.nextFloat() * (MAX_MARGIN - MIN_MARGIN))  // Y: 10-20%
                );
                
            case HORIZONTAL:
                // 9시 방향 (서쪽 좌측)
                return new Vec2(
                    worldW * (MIN_MARGIN + random.nextFloat() * (MAX_MARGIN - MIN_MARGIN)),  // X: 10-20%
                    worldH * (0.4f + random.nextFloat() * 0.2f)  // Y: 40-60% (중앙)
                );
                
            case DIAGONAL_NE_SW:
                // 2시 방향 (북동쪽)
                return new Vec2(
                    worldW * (0.75f + random.nextFloat() * 0.15f),  // X: 75-90%
                    worldH * (MIN_MARGIN + random.nextFloat() * (MAX_MARGIN - MIN_MARGIN))  // Y: 10-20%
                );
                
            case DIAGONAL_NW_SE:
                // 10시 방향 (북서쪽)
                return new Vec2(
                    worldW * (MIN_MARGIN + random.nextFloat() * (MAX_MARGIN - MIN_MARGIN)),  // X: 10-20%
                    worldH * (MIN_MARGIN + random.nextFloat() * (MAX_MARGIN - MIN_MARGIN))  // Y: 10-20%
                );
                
            default:
                // 기본값: VERTICAL 패턴
                return new Vec2(worldW * 0.5f, worldH * 0.15f);
        }
    }
    
    /**
     * BLUE 팀의 스폰 위치 계산 (RED 팀의 정반대 위치)
     * 
     * @return Vec2 스폰 좌표
     */
    public Vec2 getBlueSpawn() {
        switch (pattern) {
            case VERTICAL:
                // 6시 방향 (남쪽 하단) - 12시의 반대
                return new Vec2(
                    worldW * (0.4f + random.nextFloat() * 0.2f),  // X: 40-60% (중앙)
                    worldH * (1.0f - MIN_MARGIN - random.nextFloat() * (MAX_MARGIN - MIN_MARGIN))  // Y: 80-90%
                );
                
            case HORIZONTAL:
                // 3시 방향 (동쪽 우측) - 9시의 반대
                return new Vec2(
                    worldW * (1.0f - MIN_MARGIN - random.nextFloat() * (MAX_MARGIN - MIN_MARGIN)),  // X: 80-90%
                    worldH * (0.4f + random.nextFloat() * 0.2f)  // Y: 40-60% (중앙)
                );
                
            case DIAGONAL_NE_SW:
                // 8시 방향 (남서쪽) - 2시의 반대
                return new Vec2(
                    worldW * (MIN_MARGIN + random.nextFloat() * (MAX_MARGIN - MIN_MARGIN)),  // X: 10-20%
                    worldH * (1.0f - MIN_MARGIN - random.nextFloat() * (MAX_MARGIN - MIN_MARGIN))  // Y: 80-90%
                );
                
            case DIAGONAL_NW_SE:
                // 4시 방향 (남동쪽) - 10시의 반대
                return new Vec2(
                    worldW * (1.0f - MIN_MARGIN - random.nextFloat() * (MAX_MARGIN - MIN_MARGIN)),  // X: 80-90%
                    worldH * (1.0f - MIN_MARGIN - random.nextFloat() * (MAX_MARGIN - MIN_MARGIN))  // Y: 80-90%
                );
                
            default:
                // 기본값: VERTICAL 패턴
                return new Vec2(worldW * 0.5f, worldH * 0.85f);
        }
    }
    
    /**
     * 팀에 따른 스폰 위치 반환
     * 
     * @param team 팀 (RED 또는 BLUE)
     * @return Vec2 스폰 좌표
     */
    public Vec2 getSpawn(GameEnums.Team team) {
        if (team == GameEnums.Team.BLUE) {
            return getBlueSpawn();
        } else {
            return getRedSpawn();
        }
    }
    
    /**
     * 현재 스폰 패턴 반환
     */
    public SpawnPattern getPattern() {
        return pattern;
    }
    
    /**
     * 스폰 패턴의 설명 반환
     */
    public String getPatternDescription() {
        switch (pattern) {
            case VERTICAL:
                return "VERTICAL: RED(12시/북쪽) vs BLUE(6시/남쪽)";
            case HORIZONTAL:
                return "HORIZONTAL: RED(9시/서쪽) vs BLUE(3시/동쪽)";
            case DIAGONAL_NE_SW:
                return "DIAGONAL_NE_SW: RED(2시/북동) vs BLUE(8시/남서)";
            case DIAGONAL_NW_SE:
                return "DIAGONAL_NW_SE: RED(10시/북서) vs BLUE(4시/남동)";
            default:
                return "UNKNOWN";
        }
    }
    
    /**
     * 디버깅용 정보 출력
     */
    @Override
    public String toString() {
        return String.format("SpawnManager[pattern=%s, worldSize=(%.0f, %.0f)]", 
            pattern, worldW, worldH);
    }
}
