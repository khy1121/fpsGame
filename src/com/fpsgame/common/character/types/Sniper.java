package com.fpsgame.common.character.types;

import com.fpsgame.common.GameEnums.CharacterId;
import com.fpsgame.common.GameEnums.Team;
import com.fpsgame.common.Vec2;
import com.fpsgame.common.character.Character;
import com.fpsgame.common.character.types.sniper.ability.SniperBasicAttack;
import com.fpsgame.common.character.types.sniper.ability.SniperScope;
import com.fpsgame.common.character.types.sniper.ability.SniperUltimate;

/**
 * SNIPER (장거리 특화 캐릭터)
 * ------------------------------------
 * 높은 단일 타겟 피해량과 장거리 교전에 특화된 캐릭터.
 * 
 * 기본 공격: 저격소총 (높은 피해량, 낮은 발사 속도, 높은 정확도)
 * 전술 스킬: 조준경 (정확도 증가, 이동속도 감소)
 * 궁극기: 헤드헌터 (일정 시간 동안 헤드샷 피해량 증가)
 */
public class Sniper extends Character {
    
    // 스나이퍼 기본 스펙
    private static final float BASE_HEALTH = 80f;       // 기본 체력 (낮음)
    private static final float BASE_ARMOR = 15f;        // 기본 방어력 (낮음)
    private static final float BASE_SPEED = 5.5f;       // 기본 이동 속도
    private static final float BASE_JUMP = 7f;          // 기본 점프력
    
    // 스킬 쿨다운
    private static final float BASIC_COOLDOWN = 1.5f;    // 기본 공격 쿨다운 (높음)
    private static final float SCOPE_COOLDOWN = 1f;      // 조준경 쿨다운
    private static final float ULTIMATE_COOLDOWN = 0f;   // 궁극기는 차지 방식
    
    // 스킬 속성
    private static final float SCOPE_DURATION = 5f;      // 조준경 지속시간
    private static final float ULTIMATE_DURATION = 10f;  // 궁극기 지속시간
    
    /**
     * 스나이퍼 캐릭터 생성
     * @param team 소속 팀
     * @param startPosition 초기 위치
     */
    public Sniper(Team team, Vec2 startPosition) {
        super(CharacterId.SNIPER, team, startPosition);
        initialize();  // 능력치와 스킬 초기화
    }
    
    @Override
    protected void initializeStats() {
        // 기본 능력치 세팅
        setMaxHealth(BASE_HEALTH);
        setCurrentHealth(BASE_HEALTH);
        setArmor(BASE_ARMOR);
        setMoveSpeed(BASE_SPEED);
        setJumpForce(BASE_JUMP);
    }
    
    @Override
    protected void initializeAbilities() {
        // 스나이퍼 전용 스킬 장착
        this.basicAbility = new com.fpsgame.common.character.types.sniper.ability.SniperBasicAttack(BASIC_COOLDOWN, 75f);
        this.tacticalAbility = new SniperScope(SCOPE_COOLDOWN, SCOPE_DURATION);
        this.ultimateAbility = new SniperUltimate(ULTIMATE_COOLDOWN, ULTIMATE_DURATION);
    }
}
