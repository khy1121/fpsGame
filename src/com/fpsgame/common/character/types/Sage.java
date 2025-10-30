package com.fpsgame.common.character.types;

import com.fpsgame.common.GameEnums.CharacterId;
import com.fpsgame.common.GameEnums.Team;
import com.fpsgame.common.Vec2;
import com.fpsgame.common.character.Character;
import com.fpsgame.common.character.types.raven.ability.RavenBasicAttack;

/** SAGE (Support) – placeholder implementation. */
public class Sage extends Character {
    public Sage(Team team, Vec2 startPosition) {
        super(CharacterId.SAGE, team, startPosition);
        initialize();
    }
    @Override protected void initializeStats() {
        setMaxHealth(100f); setCurrentHealth(100f);
        setArmor(8f); setMoveSpeed(5.3f); setJumpForce(7f);
    }
    @Override protected void initializeAbilities() {
        this.basicAbility = new RavenBasicAttack(0.33f, 12f); // SMG-like
        this.tacticalAbility = null; // TODO: self/ally heal kit
        this.ultimateAbility = null; // TODO: revive drone
    }
}
