package com.fpsgame.common.character.types.piper.ability;

import com.fpsgame.common.Vec2;
import com.fpsgame.common.character.Character;
import com.fpsgame.common.character.ability.UltimateAbility;

/** Thermal scope: for a short time, reveal enemies (client can render through walls). */
public class PiperThermalScope extends UltimateAbility {

    private Character owner;

    public PiperThermalScope(float cooldown, float duration) {
        super(cooldown, duration, 0f);
    }

    @Override
    public void activate(Character user, Vec2 direction) {
        if (!isReady()) return;
        owner = user;
        isActive = true;
        try { owner.setThermalVision(true); } catch (Throwable ignore) {}
        startCooldown();
    }

    @Override
    protected void updateUltimateEffect(float deltaTime) {
        duration -= deltaTime;
        if (duration <= 0f) {
            isActive = false;
            try { if (owner != null) owner.setThermalVision(false); } catch (Throwable ignore) {}
            owner = null;
        }
    }
}
