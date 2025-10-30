package com.fpsgame.common.character.types.raven.ability;

import com.fpsgame.common.Vec2;
import com.fpsgame.common.character.Character;
import com.fpsgame.common.character.ability.UltimateAbility;

/**
 * 레이븐의 궁극기: 과충전
 * 일시적으로 발사 속도와 이동 속도가 크게 증가합니다.
 */
public class RavenUltimate extends UltimateAbility {
    
    private static final float SPEED_BOOST = 1.5f;     // 이동 속도 증가율
    private static final float FIRE_RATE_BOOST = 2.0f; // 발사 속도 증가율
    
    private float originalMoveSpeed;    // 원래 이동 속도
    private float originalFireRate;     // 원래 발사 속도
    private Character activeCharacter;  // 효과가 적용된 캐릭터
    
    /**
     * 레이븐 궁극기 생성자
     * @param cooldown 쿨다운 (초)
     * @param duration 지속 시간 (초)
     * @param radius 효과 범위 (자기 자신만 해당하므로 0)
     */
    public RavenUltimate(float cooldown, float duration, float radius) {
        super(cooldown, duration, radius);
    }
    
    @Override
    public void activate(Character source, Vec2 direction) {
        if (!isReady()) return;
        
        // 현재 상태 저장
        this.activeCharacter = source;
        this.originalMoveSpeed = source.getMoveSpeed();
        this.originalFireRate = source.getBasicAbility().getFireRateMultiplier();
        
    // 강화 효과 적용
    source.overrideMoveSpeed(originalMoveSpeed * SPEED_BOOST);
        source.getBasicAbility().setFireRateMultiplier(originalFireRate * FIRE_RATE_BOOST);
        
        // 이펙트 시작
        startCooldown();
    }
    
    @Override
    protected void updateUltimateEffect(float deltaTime) {
        if (!isActive || activeCharacter == null) return;
        
        // 지속 시간 종료 체크
        if (duration <= 0) {
            // 원래 상태로 복구
            activeCharacter.overrideMoveSpeed(originalMoveSpeed);
            activeCharacter.getBasicAbility().setFireRateMultiplier(originalFireRate);
            
            isActive = false;
            activeCharacter = null;
        }
    }
}
