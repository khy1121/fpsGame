package com.fpsgame.common.character.types;

import com.fpsgame.common.GameEnums.CharacterId;
import com.fpsgame.common.GameEnums.Team;
import com.fpsgame.common.Vec2;
import com.fpsgame.common.character.Character;
import com.fpsgame.common.character.types.raven.ability.RavenBasicAttack;

/** SKULL (Mercenary) – placeholder implementation. */
public class Skull extends Character {
    public Skull(Team team, Vec2 startPosition) {
        super(CharacterId.SKULL, team, startPosition);
        initialize();
    }
    @Override protected void initializeStats() {
        setMaxHealth(120f); setCurrentHealth(120f);
        setArmor(12f); setMoveSpeed(5.0f); setJumpForce(7f);
    }
    @Override protected void initializeAbilities() {
        this.basicAbility = new RavenBasicAttack(0.45f, 20f); // harder-hitting carbine feel
        this.tacticalAbility = null; // TODO: adrenaline
        this.ultimateAbility = null; // TODO: ammo drop
    }
}
