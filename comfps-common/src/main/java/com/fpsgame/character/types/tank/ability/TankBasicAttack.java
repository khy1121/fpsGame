package com.fpsgame.common.character.types.tank.ability;

import com.fpsgame.common.Vec2;
import com.fpsgame.common.character.Character;
import com.fpsgame.common.character.ability.BasicAbility;
import com.fpsgame.common.character.projectile.Bullet;
import com.fpsgame.common.character.projectile.ProjectileManager;

/**
 * ?  ??        - ?  
 *        ?      ???      ??  ?      
 */
public class TankBasicAttack extends BasicAbility {
    
    private static final float DAMAGE_PER_PELLET = 15f;    // ?   ?  ???  ??
    private static final float PELLET_SPEED = 20f;         // ?   ?  
    private static final int PELLET_COUNT = 8;             //    ?   ?   ??
    private static final float SPREAD_ANGLE = 30f;         // ???       (??
    
    public TankBasicAttack(float cooldown) {
        super(cooldown, DAMAGE_PER_PELLET);
    }
    
    @Override
    public void activate(Character source, Vec2 direction) {
        if (!isReady()) return;
        
        Vec2 position = source.getPosition().copy();
        Vec2 normalizedDir = direction.copy().normalize();
        
        // ?       ?  ??      ?   
        float angleStep = (float) Math.toRadians(SPREAD_ANGLE) / (PELLET_COUNT - 1);
        float baseAngle = (float) Math.atan2(normalizedDir.y, normalizedDir.x);
        float startAngle = (float)(baseAngle - Math.toRadians(SPREAD_ANGLE) / 2);
        
        for (int i = 0; i < PELLET_COUNT; i++) {
            float currentAngle = startAngle + angleStep * i;
            Vec2 pelletDir = new Vec2(
                (float) Math.cos(currentAngle),
                (float) Math.sin(currentAngle)
            ).normalize();
            // ?   ?    ??   1.5           
            Bullet pellet = new Bullet(position.copy(), pelletDir.mul(PELLET_SPEED), DAMAGE_PER_PELLET, 1.5f, source);
            ProjectileManager.getInstance().addProjectile(pellet);
        }
        
        //    ???  
        cooldown = maxCooldown / fireRateMultiplier;
    }
}

