package com.fpsgame.common.character.state;

import com.fpsgame.common.character.Character;

/**
 * 캐릭터의 기본 생존 상태를 구현하는 클래스.
 * 모든 행동이 가능한 정상 상태를 나타낸다.
 */
public class AliveState implements CharacterState {
    
    @Override
    public void update(Character character) {
        // 생존 상태에서는 특별한 업데이트가 필요 없음
    }

    @Override
    public void handleDamage(Character character, float damage) {
        float currentHealth = character.getCurrentHealth();
        float newHealth = currentHealth - damage;
        
        if (newHealth <= 0) {
            // 체력이 0 이하가 되면 사망 상태로 전환
            character.setCurrentHealth(0);
            character.setState(new DeadState());
        } else {
            character.setCurrentHealth(newHealth);
        }
    }

    @Override
    public void handleHealing(Character character, float amount) {
        float currentHealth = character.getCurrentHealth();
        float maxHealth = character.getMaxHealth();
        
        // 최대 체력을 초과하지 않도록 회복
        character.setCurrentHealth(Math.min(currentHealth + amount, maxHealth));
    }

    @Override
    public boolean canMove() {
        return true;  // 생존 상태에서는 항상 이동 가능
    }

    @Override
    public boolean canUseAbilities() {
        return true;  // 생존 상태에서는 항상 스킬 사용 가능
    }
}