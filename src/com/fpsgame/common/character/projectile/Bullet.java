package com.fpsgame.common.character.projectile;

import com.fpsgame.common.Vec2;
import com.fpsgame.common.character.Character;

/**
 * 기본 총알 투사체
 * 직선으로 날아가며 캐릭터나 벽에 맞으면 소멸합니다.
 */
public class Bullet extends Projectile {
    
    /**
     * 총알 생성자
     * @param position 초기 위치
     * @param velocity 속도(방향과 속력)
     * @param damage 피해량
     * @param lifetime 수명(초)
     * @param owner 발사한 캐릭터
     */
    public Bullet(Vec2 position, Vec2 velocity, float damage, float lifetime, Character owner) {
        super(position, velocity, damage, lifetime, owner);
    }
    
    @Override
    protected void onWallCollision() {
        // 벽에 맞으면 바로 소멸
        deactivate();
    }
    
    @Override
    protected void onLifetimeEnd() {
        // 수명이 다하면 조용히 소멸
        deactivate();
    }
}