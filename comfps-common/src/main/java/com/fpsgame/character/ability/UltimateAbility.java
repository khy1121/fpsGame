package com.fpsgame.common.character.ability;

import com.fpsgame.common.Vec2;
import com.fpsgame.common.character.Character;

/**
 * 캐릭터의 궁극기를 구현하는 추상 클래스.
 * 각 캐릭터의 가장 강력한 특수 능력을 구현한다.
 */
public abstract class UltimateAbility implements Ability {
    protected float cooldown;        // 현재 쿨다운
    protected float maxCooldown;     // 최대 쿨다운
    protected float duration;        // 지속 시간
    protected boolean isActive;      // 스킬 지속 중 여부
    protected float effectRadius;    // 효과 범위
    
    protected UltimateAbility(float maxCooldown, float duration, float effectRadius) {
        this.maxCooldown = maxCooldown;
        this.duration = duration;
        this.effectRadius = effectRadius;
        this.cooldown = 0;
        this.isActive = false;
    }
    
    @Override
    public boolean isReady() {
        return cooldown <= 0 && !isActive;
    }
    
    @Override
    public float getCooldown() {
        return cooldown;
    }
    
    @Override
    public void update(float deltaTime) {
        if (cooldown > 0) {
            cooldown = Math.max(0, cooldown - deltaTime);
        }
        
        if (isActive) {
            updateUltimateEffect(deltaTime);
        }
    }
    
    /**
     * 궁극기 사용 시 쿨다운 적용
     */
    protected void startCooldown() {
        this.cooldown = this.maxCooldown;
        this.isActive = true;
    }
    
    /**
     * 지속 효과가 있는 궁극기의 업데이트 처리
     * @param deltaTime 프레임 간 시간 간격
     */
    protected abstract void updateUltimateEffect(float deltaTime);
    
    /**
     * 궁극기 효과 범위 내의 대상 확인
     * @param position 효과 중심점
     * @param target 대상 위치
     * @return 범위 내 포함 여부
     */
    protected boolean isInEffectRange(Vec2 position, Vec2 target) {
        return position.distance(target) <= effectRadius;
    }
}