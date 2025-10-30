package com.fpsgame.common.character.ability;

import com.fpsgame.common.Vec2;
import com.fpsgame.common.character.Character;

/**
 * 캐릭터의 능력(스킬)을 정의하는 인터페이스.
 * 모든 캐릭터의 기본 공격, 전술 스킬, 궁극기는 이 인터페이스를 구현한다.
 */
public interface Ability {
    /**
     * 능력 사용
     * @param source 능력을 사용하는 캐릭터
     * @param direction 능력 사용 방향
     */
    void activate(Character source, Vec2 direction);
    
    /**
     * 능력 사용 가능 여부 확인
     * @return 사용 가능 여부
     */
    boolean isReady();
    
    /**
     * 남은 쿨다운 시간 반환
     * @return 쿨다운 (초)
     */
    float getCooldown();
    
    /**
     * 쿨다운 업데이트
     * @param deltaTime 프레임 간 시간 간격
     */
    void update(float deltaTime);
}