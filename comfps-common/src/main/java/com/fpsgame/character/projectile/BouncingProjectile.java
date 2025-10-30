package com.fpsgame.common.character.projectile;

import com.fpsgame.common.Vec2;
import com.fpsgame.common.character.Character;

/** Bouncing projectile: reflects on wall up to N times. */
public class BouncingProjectile extends Projectile {
    private final int maxBounceCount;   // maximum bounces
    private int bounceCount;            // current bounces
    private float bounceMultiplier;     // velocity retain factor (0..1)

    public BouncingProjectile(Vec2 position, Vec2 velocity, float damage, float lifetime,
                              Character owner, int maxBounceCount, float bounceMultiplier) {
        super(position, velocity, damage, lifetime, owner);
        this.maxBounceCount = maxBounceCount;
        this.bounceCount = 0;
        this.bounceMultiplier = Math.max(0, Math.min(1, bounceMultiplier));
    }

    @Override
    protected void onWallCollision() {
        if (bounceCount >= maxBounceCount) {
            deactivate();
            return;
        }
        // Simple reflection: invert velocity; production should use wall normal.
        velocity = velocity.mul(-bounceMultiplier);
        bounceCount++;
    }

    @Override
    public void onHit(Character target) {
        // Apply damage (with possible headshot multiplier)
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

        deactivate();
    }
}

