package com.fpsgame.common.character.types;

import com.fpsgame.common.GameEnums.CharacterId;
import com.fpsgame.common.GameEnums.Team;
import com.fpsgame.common.Vec2;
import com.fpsgame.common.character.Character;
import com.fpsgame.common.character.types.raven.ability.RavenBasicAttack;

/** GENERAL (Commander) – placeholder implementation. */
public class General extends Character {
    public General(Team team, Vec2 startPosition) {
        super(CharacterId.GENERAL, team, startPosition);
        initialize();
    }
    @Override protected void initializeStats() {
        setMaxHealth(120f); setCurrentHealth(120f);
        setArmor(12f); setMoveSpeed(5.0f); setJumpForce(7f);
    }
    @Override protected void initializeAbilities() {
        this.basicAbility = new RavenBasicAttack(0.5f, 15f); // tactical rifle feel
        this.tacticalAbility = null; // TODO: command aura
        this.ultimateAbility = null; // TODO: airstrike
    }
}
