package com.fpsgame.common.character;

import com.fpsgame.common.GameEnums.CharacterId;
import com.fpsgame.common.GameEnums.Team;
import com.fpsgame.common.Vec2;
import com.fpsgame.common.World;
import com.fpsgame.common.character.ability.BasicAbility;
import com.fpsgame.common.character.ability.TacticalAbility;
import com.fpsgame.common.character.ability.UltimateAbility;
import com.fpsgame.common.character.projectile.ProjectileManager;
import com.fpsgame.common.character.sound.FootstepManager;
import com.fpsgame.common.character.state.AliveState;
import com.fpsgame.common.character.state.CharacterState;
import com.fpsgame.common.character.visual.VisualEffectManager;

/**
 * Base character entity (clean ASCII).
 * Subclasses provide stats and ability loadout.
 */
public abstract class Character {

    // Identity/visual
    private final CharacterId id;
    private final String displayName;
    private final FootstepManager footstepManager;
    private final VisualEffectManager visualEffectManager;

    // Spatial
    private Team team;
    private Vec2 position;
    private Vec2 velocity;
    private float rotation;
    private World world;

    // State
    protected CharacterState state;
    protected float currentHealth;
    protected float maxHealth;
    protected float armor;
    protected float moveSpeed;
    protected float jumpForce;
    protected float ultimateCharge; // 0..100

    protected boolean isMoving;
    protected boolean isCrouching;
    protected boolean isAirborne;
    protected boolean isInvulnerable;

    // Abilities
    protected BasicAbility basicAbility;
    protected TacticalAbility tacticalAbility;
    protected UltimateAbility ultimateAbility;

    // Damage adjustment (e.g., cover stance). 1.0 = normal, 0.5 = 50% taken
    private float incomingDamageMultiplier = 1.0f;
    // Vision/marking helpers
    private boolean thermalVisionActive = false;
    private float markedRemaining = 0f;
    // Head hitbox (relative to position). Top-down: Y- is up
    private float headOffsetY = -0.5f;
    private float headRadius = 0.3f;

    protected Character(CharacterId id, Team team, Vec2 startPosition) {
        this.id = id;
        this.displayName = id.displayName();
        this.footstepManager = new FootstepManager();
        this.visualEffectManager = new VisualEffectManager();

        this.team = team;
        this.position = startPosition;
        this.velocity = new Vec2(0, 0);
        this.rotation = 0f;
        this.world = null;

        this.maxHealth = 100f;
        this.currentHealth = 100f;
        this.armor = 0f;
        this.moveSpeed = 5f;
        this.jumpForce = 10f;
        this.ultimateCharge = 0f;

        this.state = new AliveState();

        this.isMoving = false;
        this.isCrouching = false;
        this.isAirborne = false;
        this.isInvulnerable = false;

        this.basicAbility = null;
        this.tacticalAbility = null;
        this.ultimateAbility = null;
    }

    // Initialization
    public final void initialize() {
        initializeStats();
        initializeAbilities();
    }

    protected abstract void initializeStats();
    protected abstract void initializeAbilities();

    // Per-frame update hook (abilities cooldown/progress)
    public void update(float deltaTime) {
        if (basicAbility != null) basicAbility.update(deltaTime);
        if (tacticalAbility != null) tacticalAbility.update(deltaTime);
        if (ultimateAbility != null) ultimateAbility.update(deltaTime);
        if (markedRemaining > 0f) markedRemaining = Math.max(0f, markedRemaining - deltaTime);
    }

    // Ability helpers
    public boolean canMove() { return state != null && state.canMove(); }
    public boolean canUseAbilities() { return state != null && state.canUseAbilities(); }

    public void useBasicAbility(Vec2 direction) {
        if (canUseAbilities() && basicAbility != null && basicAbility.isReady()) {
            basicAbility.activate(this, direction);
        }
    }

    public void useTacticalAbility(Vec2 direction) {
        if (canUseAbilities() && tacticalAbility != null && tacticalAbility.isReady()) {
            tacticalAbility.activate(this, direction);
        }
    }

    public void useUltimateAbility(Vec2 direction) {
        if (canUseAbilities() && ultimateAbility != null && ultimateAbility.isReady() && ultimateCharge >= 100f) {
            ultimateAbility.activate(this, direction);
            ultimateCharge = 0f;
        }
    }

    // Damage/heal
    public void takeDamage(float damage, Character source) {
        if (state != null && !isInvulnerable) {
            state.handleDamage(this, calculateDamage(damage));
            if (source != null) {
                source.addUltimateCharge(damage * 0.1f);
            }
        }
    }

    private float calculateDamage(float rawDamage) {
        float mult = Math.max(0f, incomingDamageMultiplier);
        return rawDamage * mult * (100f / (100f + Math.max(0f, armor)));
    }

    public void heal(float amount) {
        if (state != null) {
            state.handleHealing(this, Math.max(0f, amount));
        }
    }

    public void addUltimateCharge(float amount) {
        ultimateCharge = Math.min(ultimateCharge + Math.max(0f, amount), 100f);
    }

    // Visual/sound
    public void setAlpha(float alpha) {
        if (visualEffectManager != null) visualEffectManager.setAlpha(alpha);
    }
    public float getAlpha() {
        return visualEffectManager != null ? visualEffectManager.getCurrentAlpha() : 1.0f;
    }
    public void setFootstepVolume(float volume) {
        if (footstepManager != null) footstepManager.setVolume(volume);
    }
    public void muteFootsteps(boolean muted) {
        if (footstepManager != null) footstepManager.setMuted(muted);
    }

    // Invulnerability
    public void setInvulnerable(boolean invulnerable) { this.isInvulnerable = invulnerable; }
    public boolean isInvulnerable() { return isInvulnerable; }

    // World binding
    public World getWorld() { return world; }
    public void setWorld(World world) {
        this.world = world;
        try {
            if (world != null) {
                ProjectileManager.getInstance().addCharacter(this);
            } else {
                ProjectileManager.getInstance().removeCharacter(this);
            }
        } catch (Throwable ignore) {}
    }

    // Getters/setters
    public CharacterId getId() { return id; }
    public String getDisplayName() { return displayName; }
    public Team getTeam() { return team; }
    public void setTeam(Team team) { this.team = team; }
    public Vec2 getPosition() { return position; }
    public void setPosition(Vec2 position) { this.position = (world != null) ? world.clampToBounds(position) : position; }
    public Vec2 getVelocity() { return velocity; }
    public void setVelocity(Vec2 velocity) { this.velocity = velocity; }
    public float getRotation() { return rotation; }
    public void setRotation(float rotation) { this.rotation = rotation; }
    public float getCurrentHealth() { return currentHealth; }
    public void setCurrentHealth(float health) { this.currentHealth = Math.min(health, maxHealth); }
    public float getMaxHealth() { return maxHealth; }
    protected void setMaxHealth(float maxHealth) { this.maxHealth = maxHealth; }
    public float getArmor() { return armor; }
    protected void setArmor(float armor) { this.armor = armor; }
    public float getMoveSpeed() { return moveSpeed; }
    protected void setMoveSpeed(float moveSpeed) { this.moveSpeed = moveSpeed; }
    public float getJumpForce() { return jumpForce; }
    protected void setJumpForce(float jumpForce) { this.jumpForce = jumpForce; }
    public CharacterState getState() { return state; }
    public void setState(CharacterState state) { this.state = state; }
    public float getUltimateCharge() { return ultimateCharge; }
    protected void setUltimateCharge(float charge) { this.ultimateCharge = Math.min(Math.max(charge, 0f), 100f); }

    public BasicAbility getBasicAbility() { return this.basicAbility; }
    public TacticalAbility getTacticalAbility() { return this.tacticalAbility; }
    public UltimateAbility getUltimateAbility() { return this.ultimateAbility; }

    // Movement helpers
    public void overrideMoveSpeed(float speed) { setMoveSpeed(speed); }
    public void applyMoveSpeedMultiplier(float multiplier) { setMoveSpeed(this.moveSpeed * multiplier); }

    // Damage multiplier hooks (e.g., cover stance)
    public void setDamageTakenMultiplier(float multiplier) { this.incomingDamageMultiplier = Math.max(0f, multiplier); }
    public void resetDamageTakenMultiplier() { this.incomingDamageMultiplier = 1.0f; }

    // Headshot helpers
    public boolean isHeadHit(Vec2 hitPoint) {
        if (hitPoint == null || position == null) return false;
        Vec2 headCenter = new Vec2(position.x, position.y + headOffsetY);
        return headCenter.distance(hitPoint) <= headRadius;
    }
    public float getHeadRadius() { return headRadius; }
    public float getHeadOffsetY() { return headOffsetY; }
    public void setHeadHitbox(float offsetY, float radius) { this.headOffsetY = offsetY; this.headRadius = Math.max(0f, radius); }

    // Marking/vision
    public void setThermalVision(boolean active) { this.thermalVisionActive = active; }
    public boolean isThermalVisionActive() { return thermalVisionActive; }
    public void mark(float durationSec) { this.markedRemaining = Math.max(this.markedRemaining, Math.max(0f, durationSec)); }
    public boolean isMarked() { return markedRemaining > 0f; }
}
