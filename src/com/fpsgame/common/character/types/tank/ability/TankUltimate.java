package com.fpsgame.common.character.types.tank.ability;

import java.util.ArrayList;
import java.util.List;
import com.fpsgame.common.Vec2;
import com.fpsgame.common.World;
import com.fpsgame.common.GameEnums.Team;
import com.fpsgame.common.character.Character;
import com.fpsgame.common.character.ability.UltimateAbility;

/**
 * 탱크의 궁극기 - 자기장
 * 주변의 적을 끌어당기고 지속적인 피해를 입힌다
 */
public class TankUltimate extends UltimateAbility {
    
    private static final float PULL_FORCE = 10f;       // 끌어당기는 힘
    private static final float DAMAGE_PER_SEC = 30f;   // 초당 피해량
    private float accumulatedDamage;                   // 누적 피해량 계산용
    private Character source;                          // 스킬 사용자
    private List<Character> affectedEnemies;           // 영향을 받는 적 캐릭터들
    
    public TankUltimate(float cooldown, float duration, float radius) {
        super(cooldown, duration, radius);
        this.accumulatedDamage = 0f;
        this.source = null;
        this.affectedEnemies = new ArrayList<>();
    }
    
    @Override
    public void activate(Character user, Vec2 direction) {
        if (!isReady()) return;
        
        source = user;
        isActive = true;
        accumulatedDamage = 0f;
        affectedEnemies.clear();
        duration = 5f;  // 5초 지속
        
        // 범위 내의 모든 캐릭터 중 적 팀 캐릭터만 수집
        World world = user.getWorld();
        if (world != null) {
            List<Character> characters = world.getCharacters();
            if (characters != null) {
                Vec2 userPos = user.getPosition();
                for (Character character : characters) {
                    if (character == user) continue;
                    
                    if (character.getTeam() != user.getTeam() && 
                        character.getPosition().distance(userPos) <= effectRadius) {
                        affectedEnemies.add(character);
                    }
                }
            }
        }
        
        // 쿨다운 시작
        cooldown = maxCooldown;
    }
    
    @Override
    public void updateUltimateEffect(float deltaTime) {
        if (isActive && source != null) {
            // 누적 피해량 계산
            accumulatedDamage += DAMAGE_PER_SEC * deltaTime;
            
            // 모든 영향을 받는 적들에 대해
            World world = source.getWorld();
            if (world != null) {
                for (Character character : affectedEnemies) {
                    // 계산된 피해량 적용
                    character.takeDamage(DAMAGE_PER_SEC * deltaTime, source);
                    
                    // 적을 끌어당김
                    Vec2 dirToSource = source.getPosition().sub(character.getPosition()).normalize();
                    Vec2 pullVelocity = dirToSource.mul(PULL_FORCE);
                    character.setVelocity(pullVelocity);
                }
            }
        }
    }
    
    @Override
    public void update(float deltaTime) {
        if (cooldown > 0) {
            cooldown -= deltaTime;
        }
        
        if (isActive) {
            duration -= deltaTime;
            if (duration <= 0) {
                deactivate();
            } else {
                updateUltimateEffect(deltaTime);
            }
        }
    }
    
    public void deactivate() {
        if (!isActive || source == null) return;
        
        isActive = false;
        accumulatedDamage = 0f;
        
        // TODO: 자기장 효과 종료
        
        source = null;
    }
    
    /**
     * 자기장이 적을 끌어당기는 힘을 반환
     */
    public float getPullForce() {
        return isActive ? PULL_FORCE : 0f;
    }
    
    /**
     * 현재까지 누적된 피해량을 반환
     */
    public float getAccumulatedDamage() {
        return accumulatedDamage;
    }
}