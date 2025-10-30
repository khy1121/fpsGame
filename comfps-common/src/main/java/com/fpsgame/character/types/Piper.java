package com.fpsgame.common.character.types;

import com.fpsgame.common.GameEnums.CharacterId;
import com.fpsgame.common.GameEnums.Team;
import com.fpsgame.common.Vec2;
import com.fpsgame.common.character.Character;
import com.fpsgame.common.character.types.sniper.ability.SniperBasicAttack;
import com.fpsgame.common.character.types.piper.ability.PiperMark;
import com.fpsgame.common.character.types.piper.ability.PiperThermalScope;

/**
 * PIPER (Sniper archetype)
 * - Long-range focused; scoped shots and headshot-oriented ultimate.
 */
public class Piper extends Character {

    private static final float BASE_HEALTH = 80f;
    private static final float BASE_ARMOR = 15f;
    private static final float BASE_SPEED = 5.5f;
    private static final float BASE_JUMP = 7f;

    private static final float BASIC_COOLDOWN = 1.5f;
    private static final float MARK_COOLDOWN = 8f;
    private static final float MARK_DURATION = 4f;
    private static final float MARK_RANGE = 12f;
    private static final float MARK_CONE_DEG = 15f;

    private static final float ULTIMATE_COOLDOWN = 0f;
    private static final float THERMAL_DURATION = 6f;

    public Piper(Team team, Vec2 startPosition) {
        super(CharacterId.PIPER, team, startPosition);
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
        this.basicAbility = new SniperBasicAttack(BASIC_COOLDOWN, 75f);
        this.tacticalAbility = new PiperMark(MARK_COOLDOWN, MARK_DURATION, MARK_RANGE, MARK_CONE_DEG);
        this.ultimateAbility = new PiperThermalScope(ULTIMATE_COOLDOWN, THERMAL_DURATION);
    }
}
