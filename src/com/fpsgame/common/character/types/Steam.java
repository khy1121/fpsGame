package com.fpsgame.common.character.types;

import com.fpsgame.common.GameEnums.CharacterId;
import com.fpsgame.common.GameEnums.Team;
import com.fpsgame.common.Vec2;
import com.fpsgame.common.character.Character;
import com.fpsgame.common.character.types.raven.ability.RavenBasicAttack;

/** STEAM (Special forces) – placeholder implementation. */
public class Steam extends Character {
    public Steam(Team team, Vec2 startPosition) {
        super(CharacterId.STEAM, team, startPosition);
        initialize();
    }
    @Override protected void initializeStats() {
        setMaxHealth(110f); setCurrentHealth(110f);
        setArmor(10f); setMoveSpeed(5.4f); setJumpForce(7f);
    }
    @Override protected void initializeAbilities() {
        this.basicAbility = new RavenBasicAttack(0.4f, 16f); // balanced AR
        this.tacticalAbility = null; // TODO: EMP grenade
        this.ultimateAbility = null; // TODO: tactical reset
    }
}
