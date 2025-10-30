package com.fpsgame.common.character.projectile;

import com.fpsgame.common.Vec2;
import com.fpsgame.common.character.Character;

/** Explosive projectile: direct damage + radius AOE. */
public class ExplosiveProjectile extends Projectile {
    private final float explosionRadius;
    private final float explosionDamage;

    public ExplosiveProjectile(Vec2 position, Vec2 velocity, float damage, float lifetime,
                               Character owner, float explosionRadius, float explosionDamage) {
        super(position, velocity, damage, lifetime, owner);
        this.explosionRadius = explosionRadius;
        this.explosionDamage = explosionDamage;
    }

    @Override
    protected void onWallCollision() { explode(); }

    @Override
    protected void onLifetimeEnd() { explode(); }

    @Override
    public void onHit(Character target) {
        // Direct damage (with headshot check like other projectiles)
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

        explode();
    }

    /** Handle explosion AOE. */
    private void explode() {
        for (Character character : ProjectileManager.getInstance().getCharactersInRange(position, explosionRadius)) {
            if (character == owner) continue;
            float distance = position.distance(character.getPosition());
            float damageMultiplier = Math.max(0f, 1 - (distance / explosionRadius));
            float actualDamage = explosionDamage * damageMultiplier;
            character.takeDamage(actualDamage, owner);
        }
        deactivate();
    }
}

