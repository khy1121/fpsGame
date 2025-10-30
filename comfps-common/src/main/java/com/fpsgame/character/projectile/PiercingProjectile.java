package com.fpsgame.common.character.projectile;

import com.fpsgame.common.Vec2;
import com.fpsgame.common.character.Character;

/** Piercing projectile: passes through up to N characters. */
public class PiercingProjectile extends Projectile {
    private final int maxPierceCount;
    private int pierceCount;

    public PiercingProjectile(Vec2 position, Vec2 velocity, float damage, float lifetime,
                              Character owner, int maxPierceCount) {
        super(position, velocity, damage, lifetime, owner);
        this.maxPierceCount = maxPierceCount;
        this.pierceCount = 0;
    }

    @Override
    public void onHit(Character target) {
        float finalDamage = damage;
        try {
            if (target != null && owner != null && target.isHeadHit(this.position)) {
                if (owner.getBasicAbility() instanceof com.fpsgame.common.character.types.sniper.ability.SniperBasicAttack sba && sba.isScoped()) {
                    if (owner.getUltimateAbility() instanceof com.fpsgame.common.character.types.sniper.ability.SniperUltimate ult) {
                        finalDamage *= Math.max(1.0f, ult.getHeadshotMultiplier());
                    }
                }
            }
        } catch (Throwable ignore) {}
        target.takeDamage(finalDamage, owner);

        pierceCount++;
        if (pierceCount >= maxPierceCount) {
            deactivate();
        }
    }
}

