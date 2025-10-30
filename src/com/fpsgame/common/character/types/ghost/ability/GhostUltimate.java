package com.fpsgame.common.character.types.ghost.ability;

import com.fpsgame.common.Vec2;
import com.fpsgame.common.character.Character;
import com.fpsgame.common.character.ability.UltimateAbility;

/**
 * Ghost의 궁극기: 영혼 탈취
 * 대상의 위치로 순간이동하며 일시적으로 이동 속도가 증가합니다.
 */
public class GhostUltimate extends UltimateAbility {
    private static final float SPEED_BOOST = 1.5f;     // 이동 속도 증가 배율
    private static final float TELEPORT_RANGE = 15.0f; // 순간이동 최대 거리
    private static final float ULTIMATE_POINTS = 100.0f; // 궁극기 포인트 요구량
    
    private Character activeCharacter;
    private float originalSpeed;
    
    public GhostUltimate(float cooldown, float duration) {
        super(cooldown, duration, ULTIMATE_POINTS);
    }
    
    @Override
    public void activate(Character source, Vec2 direction) {
        if (!isReady()) return;
        
        this.activeCharacter = source;
        // 대상 위치로 순간이동 (원본 direction을 변경하지 않도록 복사)
        Vec2 targetPosition = source.getPosition().copy().add(direction.copy().scale(TELEPORT_RANGE));
        source.setPosition(targetPosition);

        // 이동 속도 증가 효과 적용 (공개 래퍼 사용)
        originalSpeed = source.getMoveSpeed();
        source.overrideMoveSpeed(originalSpeed * SPEED_BOOST);

        // 쿨다운 및 지속시간 시작
        startCooldown();
    }
    
    @Override
    protected void updateUltimateEffect(float deltaTime) {
        if (!isActive || activeCharacter == null) return;
        
        // 지속시간 체크
        duration -= deltaTime;
        if (duration <= 0) {
            // 효과 종료
            activeCharacter.overrideMoveSpeed(originalSpeed);
            isActive = false;
            activeCharacter = null;
        }
    }
    
    /**
     * 사망 시 효과 즉시 종료
     */
    public void onDeath() {
        if (!isActive) return;
        
        if (activeCharacter != null) {
            activeCharacter.overrideMoveSpeed(originalSpeed);
        }
        isActive = false;
        activeCharacter = null;
    }
}
