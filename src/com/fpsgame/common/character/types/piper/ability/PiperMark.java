package com.fpsgame.common.character.types.piper.ability;

import com.fpsgame.common.Vec2;
import com.fpsgame.common.character.Character;
import com.fpsgame.common.character.ability.TacticalAbility;
import com.fpsgame.common.character.projectile.ProjectileManager;

/** Target Marking: highlights a targeted enemy briefly (no wall check for now). */
public class PiperMark extends TacticalAbility {

    private final float range;
    private final float coneCos; // cosine of half-cone angle

    public PiperMark(float cooldown, float duration, float range, float coneDegrees) {
        super(cooldown, duration);
        this.range = Math.max(0f, range);
        double rad = Math.toRadians(Math.max(0.1, coneDegrees));
        this.coneCos = (float) Math.cos(rad);
    }

    @Override
    public void activate(Character user, Vec2 direction) {
        if (!isReady()) return;
        if (user == null || direction == null) return;
        source = user;

        // Find best target in cone within range
        Vec2 dir = direction.copy().normalize();
        Character best = null;
        float bestDot = coneCos;
        for (Character ch : ProjectileManager.getInstance().getCharactersInRange(user.getPosition(), range)) {
            if (ch == user) continue;
            if (ch.getTeam() == user.getTeam()) continue;
            Vec2 to = ch.getPosition().copy().sub(user.getPosition());
            float dist = to.len();
            if (dist <= 0.0001f) continue;
            float dot = dir.dot(to.normalize());
            if (dot >= bestDot) {
                bestDot = dot;
                best = ch;
            }
        }

        if (best != null) {
            best.mark(duration);
        }

        startCooldown();
    }

    @Override
    protected void updateActiveEffect(float deltaTime) {
        // one-shot effect; no persistent active state
        isActive = false;
    }
}
