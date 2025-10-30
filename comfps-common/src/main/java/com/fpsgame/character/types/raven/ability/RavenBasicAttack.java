package com.fpsgame.common.character.types.raven.ability;

import com.fpsgame.common.Vec2;
import com.fpsgame.common.character.Character;
import com.fpsgame.common.character.ability.BasicAbility;
import com.fpsgame.common.character.projectile.Bullet;
import com.fpsgame.common.character.projectile.ProjectileManager;

/**
 * 레이븐의 기본 공격: 고속 연사 권총
 * 낮은 피해량이지만 빠른 발사 속도가 특징입니다.
 */
public class RavenBasicAttack extends BasicAbility {
    
    private static final float BULLET_SPEED = 20f;     // 발사체 속도
    private static final float SPREAD_ANGLE = 2f;      // 탄 퍼짐 각도 (도)
    
    /**
     * 레이븐 기본 공격 생성자
     * @param cooldown 공격 쿨다운 (초)
     * @param damage 피해량
     */
    public RavenBasicAttack(float cooldown, float damage) {
        super(cooldown, damage);
    }
    
    @Override
    public void activate(Character source, Vec2 direction) {
        if (!isReady()) return;
        
        // 탄 퍼짐 계산
        float spread = (float) (Math.random() * SPREAD_ANGLE - SPREAD_ANGLE / 2);
        float angle = (float) Math.atan2(direction.y, direction.x) + (float) Math.toRadians(spread);
        
        // 발사 방향 계산
        Vec2 bulletDir = new Vec2(
            (float) Math.cos(angle),
            (float) Math.sin(angle)
        );
        
        // 발사체 생성 위치 (캐릭터 중심에서 약간 앞)
        Vec2 spawnPos = source.getPosition().copy().add(bulletDir.copy().mul(1.0f));
        
        // 발사체 생성 및 발사
        Bullet bullet = new Bullet(
            spawnPos,
            bulletDir.mul(BULLET_SPEED),
            getDamage(),
            3.0f,  // 3초 후 소멸 (빠른 탄환이므로 수명 감소)
            source
        );
        ProjectileManager.getInstance().addProjectile(bullet);
        
        // 쿨다운 시작
        startCooldown();
    }
}
