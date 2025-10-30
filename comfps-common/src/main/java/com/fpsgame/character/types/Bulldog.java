package com.fpsgame.common.character.types;

import com.fpsgame.common.GameEnums.CharacterId;
import com.fpsgame.common.GameEnums.Team;
import com.fpsgame.common.Vec2;
import com.fpsgame.common.character.Character;
import com.fpsgame.common.character.types.raven.ability.RavenBasicAttack;
import com.fpsgame.common.character.types.bulldog.ability.BulldogMinigun;
import com.fpsgame.common.character.types.bulldog.ability.CoverStance;
import com.fpsgame.common.character.types.bulldog.ability.ShatterBurst;

/**
 * BULLDOG (Heavy)
 * - High survivability and team protection.
 */
public class Bulldog extends Character {

    private static final float BASE_HEALTH = 200f;
    private static final float BASE_ARMOR = 40f;
    private static final float BASE_SPEED = 4.5f;
    private static final float BASE_JUMP = 6f;

    private static final float BASIC_COOLDOWN = 1.0f;
    private static final float ULTIMATE_COOLDOWN = 0f;

    public Bulldog(Team team, Vec2 startPosition) {
        super(CharacterId.BULLDOG, team, startPosition);
        initialize();
    }

    @Override
    protected void initializeStats() {
        setMaxHealth(BASE_HEALTH);
        setCurrentHealth(BASE_HEALTH);
        setArmor(BASE_ARMOR);
        setMoveSpeed(BASE_SPEED);
        setJumpForce(BASE_JUMP);
    }

    @Override
    protected void initializeAbilities() {
        // Basic: placeholder AR-like fire (can be replaced with hold-to-fire minigun later)
        basicAbility = new RavenBasicAttack(BASIC_COOLDOWN, 14f);
        // Tactical: Cover stance (immobile, damage reduction)
        tacticalAbility = new com.fpsgame.common.character.types.bulldog.ability.CoverStance(
                10f,  // cooldown
                4f,   // duration
                0.5f  // damage taken multiplier
        );
        // Ultimate: ShatterBurst (wide cone explosive volley)
        ultimateAbility = new ShatterBurst(
                ULTIMATE_COOLDOWN,
                0.1f, // duration (one-shot)
                0f,   // effectRadius (unused)
                12,   // pellets
                30f,  // half-angle degrees
                20f,  // projectile speed
                2.5f, // lifetime
                8f,   // direct damage
                2.5f, // explosion radius
                10f   // explosion damage
        );
    }
}



