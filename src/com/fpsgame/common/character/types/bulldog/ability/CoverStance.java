package com.fpsgame.common.character.types.bulldog.ability;

import com.fpsgame.common.Vec2;
import com.fpsgame.common.character.Character;
import com.fpsgame.common.character.ability.TacticalAbility;

/**
 * Bulldog Cover Stance: temporarily reduces incoming damage and locks movement.
 */
public class CoverStance extends TacticalAbility {

    private final float damageMultiplier; // e.g., 0.5f = 50% damage taken
    private Float originalMoveSpeed = null;

    public CoverStance(float cooldown, float duration, float damageMultiplier) {
        super(cooldown, duration);
        this.damageMultiplier = Math.max(0f, Math.min(1f, damageMultiplier));
    }

    @Override
    public void activate(Character user, Vec2 direction) {
        if (!isReady()) return;
        source = user;
        isActive = true;

        // Apply damage reduction
        user.setDamageTakenMultiplier(damageMultiplier);

        // Lock movement by setting speed to 0 (cache original)
        originalMoveSpeed = user.getMoveSpeed();
        user.overrideMoveSpeed(0f);

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

        // Restore movement and damage multiplier
        try {
            source.resetDamageTakenMultiplier();
            if (originalMoveSpeed != null) source.overrideMoveSpeed(originalMoveSpeed);
        } catch (Throwable ignore) {}
        originalMoveSpeed = null;
        source = null;
    }
}

