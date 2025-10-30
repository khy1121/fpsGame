package com.fpsgame.common.character.projectile;

import com.fpsgame.common.Vec2;
import com.fpsgame.common.character.Character;

/** Base projectile with simple movement/collision hooks. */
public abstract class Projectile {
    protected Vec2 position;
    protected Vec2 velocity;
    protected float damage;
    protected float lifetime;
    protected Character owner;
    protected boolean active;

    public Projectile(Vec2 position, Vec2 velocity, float damage, float lifetime, Character owner) {
        this.position = position;
        this.velocity = velocity;
        this.damage = damage;
        this.lifetime = lifetime;
        this.owner = owner;
        this.active = true;
    }

    /** Update projectile. */
    public void update(float deltaTime) {
        if (!active) return;

        lifetime -= deltaTime;
        if (lifetime <= 0) { onLifetimeEnd(); return; }

        if (owner != null && owner.getWorld() != null) {
            Vec2 nextPos = position.copy().add(velocity.mul(deltaTime));
            if (owner.getWorld().checkWallCollision(position, nextPos)) {
                onWallCollision();
                return;
            }
        }

        updatePosition(deltaTime);
    }

    /** Default position integrator. */
    protected void updatePosition(float deltaTime) { position = position.add(velocity.mul(deltaTime)); }

    /** Called on wall collision. */
    protected void onWallCollision() { deactivate(); }

    /** Called when lifetime ends. */
    protected void onLifetimeEnd() { deactivate(); }

    /** Simple circle collision against a character center. */
    public boolean checkCollision(Character other) {
        if (!active || other == owner) return false;
        float collisionRadius = 1.0f;
        return position.distance(other.getPosition()) <= collisionRadius;
    }

    /** Apply hit to target (headshot check included). */
    public void onHit(Character target) {
        if (!active) return;

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

    public void deactivate() { this.active = false; }
    public boolean isActive() { return active; }
    public Vec2 getPosition() { return position; }
}

