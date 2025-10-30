package com.fpsgame.common.character.types.sniper.ability;

import com.fpsgame.common.Vec2;
import com.fpsgame.common.character.Character;
import com.fpsgame.common.character.ability.BasicAbility;
import com.fpsgame.common.character.projectile.Bullet;
import com.fpsgame.common.character.projectile.ProjectileManager;

/** Sniper basic attack (clean ASCII). */
public class SniperBasicAttack extends BasicAbility {

    // Base damage, projectile speed, and scope multiplier
    private static final float DAMAGE = 75f;               // base damage
    private static final float PROJECTILE_SPEED = 25f;     // units/sec
    private static final float SCOPE_DAMAGE_MULT = 1.5f;   // damage multiplier when scoped

    private boolean isScoped = false;

    public SniperBasicAttack(float cooldown, float damage) {
        super(cooldown, DAMAGE);
    }

    @Override
    public void activate(Character source, Vec2 direction) {
        if (!isReady() || source == null || direction == null) return;

        // Compute spawn position and normalized direction
        Vec2 position = source.getPosition().copy();
        Vec2 dir = direction.copy().normalize();

        // Scoped baseline damage (headshot multiplier is applied at hit-time in Projectile)
        float finalDamage = isScoped ? DAMAGE * SCOPE_DAMAGE_MULT : DAMAGE;

        // Spawn projectile (velocity = direction * speed, lifetime 3s)
        Bullet bullet = new Bullet(position, dir.mul(PROJECTILE_SPEED), finalDamage, 3.0f, source);
        ProjectileManager.getInstance().addProjectile(bullet);

        // Start cooldown (respect current fireRateMultiplier)
        cooldown = maxCooldown / Math.max(0.1f, fireRateMultiplier);
    }

    /** Set scoped state (affects damage and fire rate). */
    public void setScoped(boolean scoped) {
        this.isScoped = scoped;
        // Scoped: slow fire rate a bit for balance
        fireRateMultiplier = scoped ? 0.7f : 1.0f;
    }

    /** Whether scoped right now (used for headshot gating). */
    public boolean isScoped() { return isScoped; }
}
