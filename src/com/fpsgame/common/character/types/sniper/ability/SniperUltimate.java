package com.fpsgame.common.character.types.sniper.ability;

import com.fpsgame.common.Vec2;
import com.fpsgame.common.character.Character;
import com.fpsgame.common.character.ability.UltimateAbility;

/**
 * 스나이퍼 궁극기 - 헤드허터(예시)
 * 일정 시간 동안 헤드샷 피해 배율을 증가시키는 자기 버프형 궁극기.
 */
public class SniperUltimate extends UltimateAbility {

    private static final float HEADSHOT_MULT = 2.5f; // 헤드샷 피해 배수
    private final float baseDuration;                // 초기 지속시간 보관

    public SniperUltimate(float cooldown, float duration) {
        super(cooldown, duration, 0f);  // 자기 버프형(범위 0)
        this.baseDuration = duration;
    }

    @Override
    public void activate(Character user, Vec2 direction) {
        if (!isReady()) return;
        // 지속시간 리셋 후 활성화
        this.duration = baseDuration;
        startCooldown(); // isActive=true, cooldown=maxCooldown 설정
    }

    @Override
    protected void updateUltimateEffect(float deltaTime) {
        // 남은 지속시간 감소 후 종료
        duration -= deltaTime;
        if (duration <= 0f) {
            isActive = false;
        }
    }

    /** 현재 헤드샷 피해 배율 반환 */
    public float getHeadshotMultiplier() {
        return isActive ? HEADSHOT_MULT : 1.0f;
    }
}