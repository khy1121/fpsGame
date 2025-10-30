package com.fpsgame.common.character.types.tank.ability;

import com.fpsgame.common.Vec2;
import com.fpsgame.common.character.Character;
import com.fpsgame.common.character.ability.TacticalAbility;

/**
 * 탱크의 전술 능력 - 방어막
 * 자신이나 아군을 보호하는 방어막을 생성한다
 */
public class TankShield extends TacticalAbility {
    
    private static final float SHIELD_AMOUNT = 50f;     // 방어막이 흡수하는 피해량
    private static final float SHIELD_RADIUS = 3f;      // 방어막 범위
    
    public TankShield(float cooldown, float duration) {
        super(cooldown, duration);
    }
    
    @Override
    public void activate(Character user, Vec2 direction) {
        if (!isReady()) return;
        
        source = user;
        isActive = true;
        
        // 방어막 효과 적용
        source.setInvulnerable(true);
        
        // TODO: 아군에게도 방어막 효과 적용 (범위 내에 있는 경우)
        
        // 쿨다운 시작
        startCooldown();
    }
    
    @Override
    protected void updateActiveEffect(float deltaTime) {
        duration -= deltaTime;
        if (duration <= 0) {
            deactivate();
        }
    }
    
    public void deactivate() {
        if (!isActive || source == null) return;
        
        isActive = false;
        source.setInvulnerable(false);
        
        source = null;
    }
    
    /**
     * 방어막의 현재 유효 범위를 반환
     */
    public float getShieldRadius() {
        return isActive ? SHIELD_RADIUS : 0f;
    }
    
    /**
     * 방어막이 흡수할 수 있는 피해량을 반환
     */
    public float getShieldAmount() {
        return isActive ? SHIELD_AMOUNT : 0f;
    }
}