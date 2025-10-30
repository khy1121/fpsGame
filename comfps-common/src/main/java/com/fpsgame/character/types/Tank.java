package com.fpsgame.common.character.types;

import com.fpsgame.common.GameEnums.CharacterId;
import com.fpsgame.common.GameEnums.Team;
import com.fpsgame.common.Vec2;
import com.fpsgame.common.character.Character;
import com.fpsgame.common.character.types.tank.ability.TankBasicAttack;
import com.fpsgame.common.character.types.tank.ability.TankShield;
import com.fpsgame.common.character.types.tank.ability.TankUltimate;

/**
 * TANK (방어형 캐릭터)
 * ------------------------------------
 * 높은 방어력과 체력을 가진 캐릭터. 아군을 보호하고 적의 공격을 견딜 수 있다.
 * 
 * 기본 공격: 샷건 (근접 범위 공격, 넓은 탄 퍼짐)
 * 전술 스킬: 방어막 (자신 또는 아군을 보호하는 방어막 생성)
 * 궁극기: 자기장 (주변의 모든 적을 끌어당기고 피해를 입힘)
 */
public class Tank extends Character {
    
    // 탱크 기본 스펙
    private static final float BASE_HEALTH = 200f;      // 기본 체력 (높음)
    private static final float BASE_ARMOR = 40f;        // 기본 방어력 (높음)
    private static final float BASE_SPEED = 4.5f;       // 기본 이동 속도 (느림)
    private static final float BASE_JUMP = 6f;          // 기본 점프력
    
    // 스킬 쿨다운
    private static final float BASIC_COOLDOWN = 1.0f;    // 기본 공격 쿨다운
    private static final float SHIELD_COOLDOWN = 8f;     // 방어막 쿨다운
    private static final float ULTIMATE_COOLDOWN = 0f;   // 궁극기는 차지 방식
    
    // 스킬 속성
    private static final float SHIELD_DURATION = 4f;      // 방어막 지속시간
    private static final float ULTIMATE_DURATION = 5f;    // 궁극기 지속시간
    private static final float ULTIMATE_RADIUS = 10f;     // 궁극기 효과 범위
    
    /**
     * 탱크 캐릭터 생성
     * @param team 소속 팀
     * @param startPosition 초기 위치
     */
    public Tank(Team team, Vec2 startPosition) {
        super(CharacterId.TANK, team, startPosition);
        initialize();  // 능력치와 스킬 초기화
    }
    
    @Override
    protected void initializeStats() {
        setMaxHealth(BASE_HEALTH);
        setCurrentHealth(BASE_HEALTH);
        setArmor(BASE_ARMOR);
        setMoveSpeed(BASE_SPEED);
        setJumpForce(BASE_JUMP);
    }
    
    @Override
    protected void initializeAbilities() {
        basicAbility = new TankBasicAttack(BASIC_COOLDOWN);
        tacticalAbility = new TankShield(SHIELD_COOLDOWN, SHIELD_DURATION);
        ultimateAbility = new TankUltimate(ULTIMATE_COOLDOWN, ULTIMATE_DURATION, ULTIMATE_RADIUS);
    }
}