package com.fpsgame.common.character.types.bulldog.ability;

import com.fpsgame.common.Vec2;
import com.fpsgame.common.character.Character;
import com.fpsgame.common.character.ability.UltimateAbility;
import com.fpsgame.common.character.projectile.ExplosiveProjectile;
import com.fpsgame.common.character.projectile.ProjectileManager;

/** Bulldog ultimate: fire a wide cone of explosive rounds instantly. */
public class ShatterBurst extends UltimateAbility {

    private Character source;
    private Vec2 fireDir;

    private final int pellets;
    private final float halfAngleDeg;
    private final float projectileSpeed;
    private final float projectileLifetime;
    private final float directDamage;
    private final float explosionRadius;
    private final float explosionDamage;

    public ShatterBurst(float cooldown, float duration, float effectRadius,
                        int pellets, float halfAngleDeg,
                        float projectileSpeed, float projectileLifetime,
                        float directDamage, float explosionRadius, float explosionDamage) {
        super(cooldown, duration, effectRadius);
        this.pellets = Math.max(1, pellets);
        this.halfAngleDeg = Math.max(0f, halfAngleDeg);
        this.projectileSpeed = projectileSpeed;
        this.projectileLifetime = projectileLifetime;
        this.directDamage = directDamage;
        this.explosionRadius = explosionRadius;
        this.explosionDamage = explosionDamage;
    }

    @Override
    public void activate(Character user, Vec2 direction) {
        if (!isReady()) return;
        this.source = user;
        this.fireDir = direction == null ? new Vec2(1, 0) : direction.copy().normalize();
        volley();
        startCooldown();
    }

    private void volley() {
        float base = (float) Math.atan2(fireDir.y, fireDir.x);
        float spread = (float) Math.toRadians(halfAngleDeg);
        for (int i = 0; i < pellets; i++) {
            float t = (pellets == 1) ? 0f : (i / (float) (pellets - 1));
            float angle = base - spread + t * (2f * spread);
            Vec2 dir = new Vec2((float) Math.cos(angle), (float) Math.sin(angle));
            Vec2 pos = source.getPosition().copy().add(dir.copy().mul(1.2f));
            ExplosiveProjectile p = new ExplosiveProjectile(pos, dir.mul(projectileSpeed),
                    directDamage, projectileLifetime, source, explosionRadius, explosionDamage);
            ProjectileManager.getInstance().addProjectile(p);
        }
    }

    @Override
    protected void updateUltimateEffect(float deltaTime) {
        // One-shot volley; immediately end.
        isActive = false;
    }
}

