package com.fpsgame.common.character.types.ghost.ability;

import com.fpsgame.common.Vec2;
import com.fpsgame.common.character.Character;
import com.fpsgame.common.character.ability.BasicAbility;
import com.fpsgame.common.character.projectile.Bullet;
import com.fpsgame.common.character.projectile.ProjectileManager;

/**
 * Ghost의 기본 공격: 소음기 기관단총
 * 빠른 연사 속도와 낮은 탄 퍼짐이 특징입니다.
 */
public class GhostBasicAttack extends BasicAbility {
    
    private static final float BULLET_SPEED = 25f;     // 발사체 속도
    private static final float SPREAD_ANGLE = 1.5f;    // 탄 퍼짐 각도 (도)
    
    /**
     * Ghost 기본 공격 생성자
     * @param cooldown 공격 쿨다운 (초)
     * @param damage 피해량
     */
    public GhostBasicAttack(float cooldown, float damage) {
        super(cooldown, damage);
    }
    
    @Override
    public void activate(Character source, Vec2 direction) {
        if (!isReady()) {
            return;
        }

    // 탄 퍼짐 계산
    float spread = (float) (Math.random() * SPREAD_ANGLE - SPREAD_ANGLE / 2);
    float angle = (float) Math.atan2(direction.y, direction.x) + (float) Math.toRadians(spread);

    // 발사 방향 계산 (새 Vec2로 생성)
    Vec2 bulletDir = new Vec2((float) Math.cos(angle), (float) Math.sin(angle));

    // 발사체 시작 위치(캐릭터 중심에서 약간 앞)
    Vec2 spawnPos = source.getPosition().copy().add(bulletDir.copy().scale(1.0f));

    // 속도 벡터 계산
    Vec2 velocity = bulletDir.copy().scale(BULLET_SPEED);

    // 발사체 생성 및 매니저에 추가 (position, velocity, damage, lifetime, owner)
    Bullet bullet = new Bullet(spawnPos, velocity, getDamage(), 5.0f, source);
    ProjectileManager.getInstance().addProjectile(bullet);

    super.startCooldown();
    }
}
