package com.fpsgame.common.character.ability;

import com.fpsgame.common.character.Character;

/**
 * 캐릭터의 전술 스킬을 구현하는 추상 클래스.
 * 각 캐릭터의 특징적인 전술 능력을 구현한다.
 */
public abstract class TacticalAbility implements Ability {
    protected float cooldown;        // 현재 쿨다운
    protected float maxCooldown;     // 최대 쿨다운
    protected float duration;        // 지속 시간 (있는 경우)
    protected boolean isActive;      // 스킬 지속 중 여부
    protected Character source;      // 스킬을 사용하는 캐릭터
    
    protected TacticalAbility(float maxCooldown, float duration) {
        this.maxCooldown = maxCooldown;
        this.duration = duration;
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
            updateActiveEffect(deltaTime);
        }
    }
    
    /**
     * 전술 스킬 사용 시 쿨다운 적용
     */
    protected void startCooldown() {
        this.cooldown = this.maxCooldown;
        this.isActive = true;
    }
    
    /**
     * 지속 효과가 있는 스킬의 업데이트 처리
     * @param deltaTime 프레임 간 시간 간격
     */
    protected abstract void updateActiveEffect(float deltaTime);
}