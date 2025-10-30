package com.fpsgame.common.character.types;

import com.fpsgame.common.GameEnums.CharacterId;
import com.fpsgame.common.GameEnums.Team;
import com.fpsgame.common.Vec2;
import com.fpsgame.common.character.Character;
import com.fpsgame.common.character.types.raven.ability.RavenBasicAttack;

/** WILDCAT (Shotgun specialist) – placeholder implementation. */
public class Wildcat extends Character {
    public Wildcat(Team team, Vec2 startPosition) {
        super(CharacterId.WILDCAT, team, startPosition);
        initialize();
    }
    @Override protected void initializeStats() {
        setMaxHealth(110f); setCurrentHealth(110f);
        setArmor(10f); setMoveSpeed(5.2f); setJumpForce(7f);
    }
    @Override protected void initializeAbilities() {
        // Placeholder: fast, close-range friendly basic attack
        this.basicAbility = new RavenBasicAttack(0.35f, 18f);
        this.tacticalAbility = null; // TODO: breach shot
        this.ultimateAbility = null; // TODO: berserk
    }
}
