package com.fpsgame.common.character.types;

import com.fpsgame.common.GameEnums.CharacterId;
import com.fpsgame.common.GameEnums.Team;
import com.fpsgame.common.Vec2;
import com.fpsgame.common.character.Character;
import com.fpsgame.common.character.types.raven.ability.RavenBasicAttack;

/** TECHNICIAN (Engineer) – placeholder implementation. */
public class Technician extends Character {
    public Technician(Team team, Vec2 startPosition) {
        super(CharacterId.TECHNICIAN, team, startPosition);
        initialize();
    }
    @Override protected void initializeStats() {
        setMaxHealth(100f); setCurrentHealth(100f);
        setArmor(8f); setMoveSpeed(5.0f); setJumpForce(7f);
    }
    @Override protected void initializeAbilities() {
        this.basicAbility = new RavenBasicAttack(0.5f, 14f); // placeholder plasma-like
        this.tacticalAbility = null; // TODO: mine
        this.ultimateAbility = null; // TODO: turret
    }
}
