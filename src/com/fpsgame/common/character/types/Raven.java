package com.fpsgame.common.character.types;

import com.fpsgame.common.GameEnums.CharacterId;
import com.fpsgame.common.GameEnums.Team;
import com.fpsgame.common.Vec2;
import com.fpsgame.common.character.Character;
import com.fpsgame.common.character.types.raven.ability.RavenBasicAttack;
import com.fpsgame.common.character.types.raven.ability.RavenDash;
import com.fpsgame.common.character.types.raven.ability.RavenUltimate;

/**
 * RAVEN (공격형 캐릭터)
 * ------------------------------------
 * 빠른 기동성과 높은 화력을 가진 공격형 캐릭터.
 * 
 * 기본 공격: 고속 연사 권총 (낮은 피해량, 빠른 발사 속도)
 * 전술 스킬: 대쉬 (짧은 거리를 순간이동하며 피해를 무시)
 * 궁극기: 과충전 (일시적으로 발사 속도와 이동 속도 증가)
 */
public class Raven extends Character {
    
    // 레이븐 기본 스펙
    private static final float BASE_HEALTH = 100f;      // 기본 체력
    private static final float BASE_ARMOR = 20f;        // 기본 방어력
    private static final float BASE_SPEED = 6.5f;       // 기본 이동 속도 (일반 캐릭터보다 빠름)
    private static final float BASE_JUMP = 8f;          // 기본 점프력
    
    // 스킬 쿨다운
    private static final float BASIC_COOLDOWN = 0.1f;    // 기본 공격 쿨다운
    private static final float DASH_COOLDOWN = 6f;       // 대쉬 쿨다운
    private static final float ULTIMATE_COOLDOWN = 0f;    // 궁극기는 차지 방식
    
    // 스킬 속성
    private static final float DASH_DISTANCE = 5f;       // 대쉬 거리
    private static final float DASH_DURATION = 0.2f;     // 대쉬 지속시간
    private static final float ULTIMATE_DURATION = 8f;   // 궁극기 지속시간
    
    /**
     * 레이븐 캐릭터 생성
     * @param team 소속 팀
     * @param startPosition 초기 위치
     */
    public Raven(Team team, Vec2 startPosition) {
        super(CharacterId.RAVEN, team, startPosition);
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
        // 기본 공격: 고속 연사 권총
        this.basicAbility = new RavenBasicAttack(
            BASIC_COOLDOWN,  // 쿨다운
            12f             // 피해량
        );
        
        // 전술 스킬: 대쉬
        this.tacticalAbility = new RavenDash(
            DASH_COOLDOWN,   // 쿨다운
            DASH_DURATION,   // 지속시간
            DASH_DISTANCE    // 이동 거리
        );
        
        // 궁극기: 과충전
        this.ultimateAbility = new RavenUltimate(
            ULTIMATE_COOLDOWN,  // 쿨다운
            ULTIMATE_DURATION,  // 지속시간
            0f                  // 범위 (자기 자신만 적용)
        );
    }
    
    /**
     * 레이븐 특수 능력: 은밀한 이동
     * 움직이지 않을 때 소음이 감소합니다.
     */
    @Override
    public void update(float deltaTime) {
        super.update(deltaTime);
        
        // 움직이지 않을 때 소음 감소 처리
        if (getVelocity().x == 0 && getVelocity().y == 0) {
            // 소음 감소 로직 구현
        }
    }
}
