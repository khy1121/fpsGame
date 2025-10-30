package com.fpsgame.common.character.types.sniper.ability;

import com.fpsgame.common.Vec2;
import com.fpsgame.common.character.Character;
import com.fpsgame.common.character.ability.Ability;
import com.fpsgame.common.character.ability.TacticalAbility;

/** Sniper scope (clean ASCII). Reduces movement speed and marks basic attack as scoped. */
public class SniperScope extends TacticalAbility {

    private Float originalMoveSpeed = null;
    private static final float MOVEMENT_PENALTY = 0.5f; // movement penalty while scoped

    public SniperScope(float cooldown, float duration) {
        super(cooldown, duration);
    }

    @Override
    public void activate(Character user, Vec2 direction) {
        if (!isReady()) return;

        source = user;
        isActive = true;

        // Cache and apply movement penalty
        originalMoveSpeed = user.getMoveSpeed();
        user.overrideMoveSpeed(originalMoveSpeed * MOVEMENT_PENALTY);

        // Toggle scoped mode on basic attack
        Ability basicAbility = user.getBasicAbility();
        if (basicAbility instanceof SniperBasicAttack sniperAttack) {
            sniperAttack.setScoped(true);
        }

        startCooldown();
    }

    @Override
    protected void updateActiveEffect(float deltaTime) {
        duration -= deltaTime;
        if (duration <= 0f) {
            deactivate();
        }
    }

    public void deactivate() {
        if (!isActive || source == null) return;

        isActive = false;

        // Restore move speed
        if (originalMoveSpeed != null) {
            source.overrideMoveSpeed(originalMoveSpeed);
        }
        originalMoveSpeed = null;

        // Toggle off scoped mode
        Ability basicAbility = source.getBasicAbility();
        if (basicAbility instanceof SniperBasicAttack sniperAttack) {
            sniperAttack.setScoped(false);
        }

        source = null;
    }
}

