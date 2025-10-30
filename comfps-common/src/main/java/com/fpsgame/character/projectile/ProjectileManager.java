package com.fpsgame.common.character.projectile;

import com.fpsgame.common.Vec2;
import com.fpsgame.common.character.Character;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 게임 내의 모든 투사체를 관리하는 매니저 클래스
 * 싱글톤 패턴 사용
 */
public class ProjectileManager {
    private static ProjectileManager instance;
    private final List<Projectile> projectiles;
    private final List<Character> characters;
    
    private ProjectileManager() {
        projectiles = new ArrayList<>();
        characters = new ArrayList<>();
    }
    
    /**
     * 싱글톤 인스턴스 반환
     */
    public static ProjectileManager getInstance() {
        if (instance == null) {
            instance = new ProjectileManager();
        }
        return instance;
    }
    
    /**
     * 투사체 추가
     * @param projectile 추가할 투사체
     */
    public void addProjectile(Projectile projectile) {
        projectiles.add(projectile);
    }
    
    /**
     * 추적할 캐릭터 추가
     * @param character 추가할 캐릭터
     */
    public void addCharacter(Character character) {
        characters.add(character);
    }
    
    /**
     * 추적 중인 캐릭터 제거
     * @param character 제거할 캐릭터
     */
    public void removeCharacter(Character character) {
        characters.remove(character);
    }
    
    /**
     * 모든 투사체 업데이트
     * @param deltaTime 프레임 간 시간 간격
     */
    public void update(float deltaTime) {
        Iterator<Projectile> it = projectiles.iterator();
        while (it.hasNext()) {
            Projectile projectile = it.next();
            
            // 투사체 업데이트
            projectile.update(deltaTime);
            
            // 비활성 투사체 제거
            if (!projectile.isActive()) {
                it.remove();
                continue;
            }
            
            // 캐릭터와의 충돌 체크
            for (Character character : characters) {
                if (projectile.checkCollision(character)) {
                    projectile.onHit(character);
                    break;
                }
            }
        }
    }
    
    /**
     * 현재 활성화된 모든 투사체 반환
     * @return 투사체 리스트
     */
    public List<Projectile> getProjectiles() {
        return new ArrayList<>(projectiles);
    }
    
    /**
     * 특정 범위 내의 모든 캐릭터 반환
     * @param center 범위의 중심점
     * @param radius 범위의 반경
     * @return 범위 내의 캐릭터 리스트
     */
    public List<Character> getCharactersInRange(Vec2 center, float radius) {
        List<Character> inRange = new ArrayList<>();
        for (Character character : characters) {
            if (center.distance(character.getPosition()) <= radius) {
                inRange.add(character);
            }
        }
        return inRange;
    }
    
    /**
     * 모든 투사체 제거
     */
    public void clearProjectiles() {
        projectiles.clear();
    }
}