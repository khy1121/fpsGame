package com.fpsgame.common.character;

import com.fpsgame.common.GameEnums.CharacterId;
import com.fpsgame.common.GameEnums.Team;
import com.fpsgame.common.Vec2;
import com.fpsgame.common.character.types.*;

/**
 * CharacterFactory: central place to instantiate concrete Character subclasses
 * from a CharacterId (or index) with given team and spawn position.
 */
public final class CharacterFactory {
    private CharacterFactory() {}

    /** Create by enum id. Falls back to Raven on error. */
    public static Character create(CharacterId id, Team team, Vec2 spawn) {
        if (id == null) id = CharacterId.RAVEN;
        if (team == null) team = Team.RED;
        if (spawn == null) spawn = new Vec2(0, 0);
        try {
            switch (id) {
                case RAVEN:       return new Raven(team, spawn);
                case PIPER:       return new Piper(team, spawn);
                case BULLDOG:     return new Bulldog(team, spawn);
                case SAGE:        return new Sage(team, spawn);
                case GHOST:       return new com.fpsgame.common.character.types.ghost.Ghost(team, spawn);
                case GENERAL:     return new General(team, spawn);
                case TECHNICIAN:  return new Technician(team, spawn);
                case WILDCAT:     return new Wildcat(team, spawn);
                case SKULL:       return new Skull(team, spawn);
                case STEAM:       return new Steam(team, spawn);
                case SNIPER:      return new Sniper(team, spawn);
                case TANK:        return new Tank(team, spawn);
                default:          return new Raven(team, spawn);
            }
        } catch (Throwable t) {
            // In case any constructor throws, ensure we still return a playable character
            return new Raven(team, spawn);
        }
    }

    /** Create by ordinal index (as used over the wire). */
    public static Character create(int characterIndex, Team team, Vec2 spawn) {
        CharacterId[] ids = CharacterId.values();
        CharacterId id = ids[(characterIndex < 0 ? 0 : (characterIndex >= ids.length ? 0 : characterIndex))];
        return create(id, team, spawn);
    }

    /** Convenience: from string name (case-insensitive). */
    public static Character create(String name, Team team, Vec2 spawn) {
        CharacterId id = com.fpsgame.common.GameEnums.CharacterId.fromName(name);
        return create(id, team, spawn);
    }
}

