package com.fpsgame.common.character.state;

import com.fpsgame.common.character.Character;

/**
 * 캐릭터의 상태를 정의하는 인터페이스.
 * 상태 패턴을 사용하여 캐릭터의 다양한 상태(살아있음, 죽음, 스턴 등)를 관리한다.
 */
public interface CharacterState {
    /**
     * 상태 업데이트 처리
     * @param character 대상 캐릭터
     */
    void update(Character character);

    /**
     * 피해 처리
     * @param character 대상 캐릭터
     * @param damage 피해량
     */
    void handleDamage(Character character, float damage);

    /**
     * 회복 처리
     * @param character 대상 캐릭터
     * @param amount 회복량
     */
    void handleHealing(Character character, float amount);

    /**
     * 이동 가능 여부 확인
     * @return 이동 가능 여부
     */
    boolean canMove();

    /**
     * 스킬 사용 가능 여부 확인
     * @return 스킬 사용 가능 여부
     */
    boolean canUseAbilities();
}