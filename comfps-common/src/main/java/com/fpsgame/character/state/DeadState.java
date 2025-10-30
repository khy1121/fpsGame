package com.fpsgame.common.character.state;

import com.fpsgame.common.character.Character;

/**
 * 캐릭터의 사망 상태를 구현하는 클래스.
 * 이동 및 스킬 사용이 불가능한 상태를 나타낸다.
 */
public class DeadState implements CharacterState {
    
    @Override
    public void update(Character character) {
        // 사망 상태에서는 특별한 업데이트가 필요 없음
    }

    @Override
    public void handleDamage(Character character, float damage) {
        // 사망 상태에서는 추가 피해를 받지 않음
    }

    @Override
    public void handleHealing(Character character, float amount) {
        float newHealth = amount;
        
        // 부활 처리 (체력이 0보다 커지면 생존 상태로 전환)
        if (newHealth > 0) {
            character.setCurrentHealth(newHealth);
            character.setState(new AliveState());
        }
    }

    @Override
    public boolean canMove() {
        return false;  // 사망 상태에서는 이동 불가
    }

    @Override
    public boolean canUseAbilities() {
        return false;  // 사망 상태에서는 스킬 사용 불가
    }
}