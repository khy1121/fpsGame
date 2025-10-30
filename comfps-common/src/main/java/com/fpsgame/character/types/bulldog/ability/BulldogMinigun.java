package com.fpsgame.common.character.types.bulldog.ability;

import com.fpsgame.common.Vec2;
import com.fpsgame.common.character.Character;
import com.fpsgame.common.character.ability.TacticalAbility;
import com.fpsgame.common.character.projectile.Bullet;
import com.fpsgame.common.character.projectile.ProjectileManager;

/** Bulldog minigun: spin-up, then sustained rapid fire while active. */
public class BulldogMinigun extends TacticalAbility {

    private final float spinUp;
    private final float fireInterval;
    private final float perShotDamage;
    private final float projectileSpeed;
    private final float projectileLifetime;

    private float elapsed;
    private float fireTimer;
    private Vec2 fireDir;

    public BulldogMinigun(float cooldown, float duration,
                          float spinUp, float fireInterval,
                          float perShotDamage, float projectileSpeed,
                          float projectileLifetime) {
        super(cooldown, duration);
        this.spinUp = Math.max(0f, spinUp);
        this.fireInterval = Math.max(0.02f, fireInterval);
        this.perShotDamage = Math.max(0f, perShotDamage);
        this.projectileSpeed = projectileSpeed;
        this.projectileLifetime = projectileLifetime;
    }

    @Override
    public void activate(Character user, Vec2 direction) {
        if (!isReady()) return;
        this.source = user;
        this.fireDir = direction == null ? new Vec2(1, 0) : direction.copy().normalize();
        this.elapsed = 0f;
        this.fireTimer = 0f;
        startCooldown();
    }

    @Override
    protected void updateActiveEffect(float deltaTime) {
        elapsed += deltaTime;
        duration -= deltaTime;
        if (duration <= 0f) {
            isActive = false;
            return;
        }
        if (elapsed < spinUp || source == null) return;

        fireTimer += deltaTime;
        while (fireTimer >= fireInterval) {
            fireTimer -= fireInterval;
            spawnBullet();
        }
    }

    private void spawnBullet() {
        // Slight jitter for spread
        float jitter = (float) Math.toRadians(2.0 * (Math.random() - 0.5));
        float a = (float) Math.atan2(fireDir.y, fireDir.x) + jitter;
        Vec2 dir = new Vec2((float) Math.cos(a), (float) Math.sin(a));
        Vec2 pos = source.getPosition().copy().add(dir.copy().mul(1.0f));
        Bullet b = new Bullet(pos, dir.mul(projectileSpeed), perShotDamage, projectileLifetime, source);
        ProjectileManager.getInstance().addProjectile(b);
    }
}

