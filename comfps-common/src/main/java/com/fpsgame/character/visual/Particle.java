package com.fpsgame.common.character.visual;

import com.fpsgame.common.Vec2;

/**
 * 게임의 시각적 효과(파티클)를 표현하는 클래스
 */
public class Particle {
    private Vec2 position;      // 파티클 위치
    private Vec2 velocity;      // 파티클 속도
    private float lifeTime;     // 남은 생존 시간
    private float alpha;        // 투명도
    private float scale;        // 크기 배율
    private ParticleType type;  // 파티클 종류
    
    public Particle(Vec2 position, Vec2 velocity, float lifeTime, float alpha, float scale, ParticleType type) {
        this.position = position;
        this.velocity = velocity;
        this.lifeTime = lifeTime;
        this.alpha = alpha;
        this.scale = scale;
        this.type = type;
    }
    
    /**
     * 파티클 상태 업데이트
     * @param deltaTime 프레임 간 시간 간격
     * @return 파티클이 여전히 살아있으면 true
     */
    public boolean update(float deltaTime) {
        lifeTime -= deltaTime;
        if (lifeTime <= 0) return false;
        
        position = position.add(velocity.scale(deltaTime));
        
        // 시간에 따른 투명도 감소
        float lifeFactor = lifeTime / getTotalLifeTime();
        alpha *= lifeFactor;
        
        return true;
    }
    
    /**
     * 파티클의 초기 생존시간 반환
     */
    public float getTotalLifeTime() {
        return switch (type) {
            case FOOTSTEP -> 2.0f;        // 발자국은 2초 유지
            case TELEPORT -> 0.5f;        // 순간이동 흔적은 0.5초 유지
            case INVISIBILITY -> 0.3f;    // 투명화 흔적은 0.3초 유지
        };
    }
    
    // 게터
    public Vec2 getPosition() { return position; }
    public float getAlpha() { return alpha; }
    public float getScale() { return scale; }
    public ParticleType getType() { return type; }
}