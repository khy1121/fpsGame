package com.fpsgame.common.character.types.ghost.ability;

import com.fpsgame.common.Vec2;
import com.fpsgame.common.character.Character;
import com.fpsgame.common.character.ability.TacticalAbility;
import com.fpsgame.common.character.sound.SkillSound;
import com.fpsgame.common.character.sound.SoundManager;
import com.fpsgame.common.character.visual.ParticleManager;

/**
 * Ghost의 전술 스킬: 은신
 * 일시적으로 투명화되어 적의 시야에서 숨습니다.
 */
public class GhostInvisibility extends TacticalAbility {
    private Character activeCharacter;       // 효과가 적용된 캐릭터
    
    /**
     * Ghost 은신 스킬 생성자
     * @param cooldown 쿨다운 시간 (초)
     * @param duration 지속 시간 (초)
     */
    public GhostInvisibility(float cooldown, float duration) {
        super(cooldown, duration);
    }
    
    @Override
    public void activate(Character source, Vec2 direction) {
        if (!isReady()) return;
        
        // 은신 상태 적용
        this.activeCharacter = source;
        setInvisible(true);
        
        // 사운드 효과 재생
        SoundManager.getInstance().playSkillSound(
            SkillSound.INVISIBILITY_START,
            source.getPosition()
        );
        
        // 투명화 파티클 생성
        ParticleManager.getInstance().createInvisibility(source.getPosition());
        
        // 쿨다운 및 지속시간 시작
        startCooldown();
        this.source = source;
    }
    
    @Override
    protected void updateActiveEffect(float deltaTime) {
        if (!isActive || activeCharacter == null) return;
        
        // 지속시간 종료 체크
        duration -= deltaTime;
        if (duration <= 0) {
            // 은신 해제
            setInvisible(false);
            isActive = false;
            activeCharacter = null;
        }
    }
    
    /**
     * 은신 상태 설정
     * @param invisible true면 은신 상태로, false면 해제
     */
    private void setInvisible(boolean invisible) {
        if (activeCharacter != null) {
            if (invisible) {
                // TODO: 캐릭터의 투명도를 낮게 설정
                // activeCharacter.setAlpha(0.2f);
                // TODO: 발소리 제거
                // activeCharacter.setFootstepVolume(0.0f);
            } else {
                // TODO: 캐릭터의 투명도를 원래대로
                // activeCharacter.setAlpha(1.0f);
                // TODO: 발소리 원래대로
                // activeCharacter.setFootstepVolume(1.0f);
            }
        }
    }
    
    /**
     * 피격 시 은신 해제
     */
    public void onDamaged() {
        if (!isActive) return;
        
        // 은신 즉시 해제
        setInvisible(false);
        isActive = false;
        activeCharacter = null;
    }
}
