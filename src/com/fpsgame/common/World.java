package com.fpsgame.common;

import com.fpsgame.common.GameEnums.MapId;
import com.fpsgame.common.character.Character;
import java.util.ArrayList;
import java.util.List;

/**
 * 게임 월드의 물리적 정보를 관리하는 클래스
 */
public class World {
    private final MapId mapId;                   // 맵 ID
    private final List<Wall> walls;              // 맵의 벽 목록
    private final Vec2 bounds;                   // 맵의 크기 (너비, 높이)
    private final List<Character> characters;    // 게임에 참여한 캐릭터들
    
    /**
     * 월드 생성자
     * @param mapId 맵 ID
     * @param bounds 맵 크기
     */
    public World(MapId mapId, Vec2 bounds) {
        this.mapId = mapId;
        this.bounds = bounds;
        this.walls = new ArrayList<>();
        this.characters = new ArrayList<>();
        initializeWalls();
    }
    
    /**
     * 맵별 벽 초기화
     * 각 맵의 레이아웃에 따라 벽을 추가
     */
    private void initializeWalls() {
        switch (mapId) {
            case TERMINAL:
                initializeTerminalWalls();
                addDefaultTerminalInterior();
                break;
            case NEON_CITY:
                initializeNeonCityWalls();
                addDefaultNeonCityInterior();
                break;
            case FOREST_OUTPOST:
                initializeForestOutpostWalls();
                addDefaultForestOutpostInterior();
                break;
        }
    }

    // 내부 구조(예시) 추가 헬퍼들
    private void addDefaultTerminalInterior() {
        float w = bounds.x, h = bounds.y;
        walls.add(new Wall(new Vec2(w * 0.5f, h * 0.15f), new Vec2(w * 0.5f, h * 0.85f)));
        walls.add(new Wall(new Vec2(w * 0.15f, h * 0.3f), new Vec2(w * 0.4f, h * 0.3f)));
        walls.add(new Wall(new Vec2(w * 0.6f, h * 0.7f), new Vec2(w * 0.85f, h * 0.7f)));
        walls.add(new Wall(new Vec2(w * 0.25f, h * 0.65f), new Vec2(w * 0.25f, h * 0.85f)));
    }

    private void addDefaultNeonCityInterior() {
        float w = bounds.x, h = bounds.y;
        walls.add(new Wall(new Vec2(w * 0.2f, h * 0.2f), new Vec2(w * 0.4f, h * 0.4f)));
        walls.add(new Wall(new Vec2(w * 0.8f, h * 0.2f), new Vec2(w * 0.6f, h * 0.4f)));
        walls.add(new Wall(new Vec2(w * 0.35f, h * 0.5f), new Vec2(w * 0.65f, h * 0.5f)));
        walls.add(new Wall(new Vec2(w * 0.65f, h * 0.5f), new Vec2(w * 0.65f, h * 0.75f)));
    }

    private void addDefaultForestOutpostInterior() {
        float w = bounds.x, h = bounds.y;
        walls.add(new Wall(new Vec2(w * 0.2f, h * 0.8f), new Vec2(w * 0.4f, h * 0.8f)));
        walls.add(new Wall(new Vec2(w * 0.6f, h * 0.2f), new Vec2(w * 0.8f, h * 0.2f)));
        walls.add(new Wall(new Vec2(w * 0.15f, h * 0.5f), new Vec2(w * 0.35f, h * 0.65f)));
    }
    
    /**
     * Terminal 맵의 벽 초기화
     */
    private void initializeTerminalWalls() {
        // 맵 외곽 벽
        walls.add(new Wall(new Vec2(0, 0), new Vec2(bounds.x, 0)));          // 상단 벽
        walls.add(new Wall(new Vec2(bounds.x, 0), new Vec2(bounds.x, bounds.y)));  // 우측 벽
        walls.add(new Wall(new Vec2(bounds.x, bounds.y), new Vec2(0, bounds.y)));  // 하단 벽
        walls.add(new Wall(new Vec2(0, bounds.y), new Vec2(0, 0)));          // 좌측 벽
        
        // TODO: 내부 벽 추가
    }
    
    /**
     * Neon City 맵의 벽 초기화
     */
    private void initializeNeonCityWalls() {
        // 맵 외곽 벽
        walls.add(new Wall(new Vec2(0, 0), new Vec2(bounds.x, 0)));
        walls.add(new Wall(new Vec2(bounds.x, 0), new Vec2(bounds.x, bounds.y)));
        walls.add(new Wall(new Vec2(bounds.x, bounds.y), new Vec2(0, bounds.y)));
        walls.add(new Wall(new Vec2(0, bounds.y), new Vec2(0, 0)));
        
        // TODO: 내부 벽 추가
    }
    
    /**
     * Forest Outpost 맵의 벽 초기화
     */
    private void initializeForestOutpostWalls() {
        // 맵 외곽 벽
        walls.add(new Wall(new Vec2(0, 0), new Vec2(bounds.x, 0)));
        walls.add(new Wall(new Vec2(bounds.x, 0), new Vec2(bounds.x, bounds.y)));
        walls.add(new Wall(new Vec2(bounds.x, bounds.y), new Vec2(0, bounds.y)));
        walls.add(new Wall(new Vec2(0, bounds.y), new Vec2(0, 0)));
        
        // TODO: 내부 벽 추가
    }
    
    /**
     * 주어진 선분이 벽과 충돌하는지 확인
     * @param start 선분의 시작점
     * @param end 선분의 끝점
     * @return 충돌하는 경우 true
     */
    public boolean checkWallCollision(Vec2 start, Vec2 end) {
        for (Wall wall : walls) {
            if (wall.intersects(start, end)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 주어진 위치가 맵 경계 안에 있는지 확인
     * @param position 확인할 위치
     * @return 경계 안에 있으면 true
     */
    public boolean isInBounds(Vec2 position) {
        return position.x >= 0 && position.x <= bounds.x 
            && position.y >= 0 && position.y <= bounds.y;
    }
    
    /**
     * 경계 안으로 위치를 조정
     * @param position 조정할 위치
     * @return 경계 안으로 조정된 위치
     */
    public Vec2 clampToBounds(Vec2 position) {
        float x = Math.max(0, Math.min(bounds.x, position.x));
        float y = Math.max(0, Math.min(bounds.y, position.y));
        return new Vec2(x, y);
    }

    /**
     * 게임에 참여한 모든 캐릭터 목록을 반환
     */
    public List<Character> getCharacters() {
        return characters;
    }

    /**
     * 게임에 캐릭터 추가
     */
    public void addCharacter(Character character) {
        if (character != null) {
            characters.add(character);
        }
    }

    /**
     * 게임에서 캐릭터 제거
     */
    public void removeCharacter(Character character) {
        characters.remove(character);
    }
}
