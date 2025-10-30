package com.fpsgame.common.character.types.ghost;

import com.fpsgame.common.GameEnums.CharacterId;
import com.fpsgame.common.GameEnums.Team;
import com.fpsgame.common.Vec2;
import com.fpsgame.common.character.Character;
import com.fpsgame.common.character.types.ghost.ability.GhostBasicAttack;
import com.fpsgame.common.character.types.ghost.ability.GhostInvisibility;
import com.fpsgame.common.character.types.ghost.ability.GhostUltimate;

/**
 * Ghost 캐릭터
 * 은신과 위장 능력을 가진 스텔스 전문가입니다.
 */
public class Ghost extends Character {
    
    // 기본 스탯
    private static final float BASE_HEALTH = 120;     // 기본 체력
    private static final float BASE_SPEED = 6.0f;     // 기본 이동속도
    private static final float BASE_ARMOR = 1.0f;     // 기본 방어력
    
    /**
     * Ghost 캐릭터 생성자
     * @param team 소속 팀
     * @param startPosition 초기 위치
     */
    public Ghost(Team team, Vec2 startPosition) {
        super(CharacterId.GHOST, team, startPosition);
        initialize();  // 능력치와 스킬 초기화
    }
    
    @Override
    protected void initializeStats() {
        setMaxHealth(BASE_HEALTH);
        setCurrentHealth(BASE_HEALTH);
        setMoveSpeed(BASE_SPEED);
        setArmor(BASE_ARMOR);
        setJumpForce(10f);  // 기본 점프력 사용
    }
    
    @Override
    protected void initializeAbilities() {
        this.basicAbility = new GhostBasicAttack(
            0.08f,      // 0.08초당 한 발 (높은 연사 속도)
            8.0f        // 낮은 피해량
        );
        
        this.tacticalAbility = new GhostInvisibility(
            12.0f,      // 쿨다운 12초
            5.0f        // 지속시간 5초
        );
        
        this.ultimateAbility = new GhostUltimate(
            40.0f,      // 쿨다운 40초
            15.0f       // 지속시간 15초
        );
    }
}
