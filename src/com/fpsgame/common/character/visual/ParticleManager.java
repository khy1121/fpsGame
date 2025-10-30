package com.fpsgame.common.character.visual;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import com.fpsgame.common.Vec2;

/**
 * 파티클 효과를 관리하는 매니저 클래스
 */
public class ParticleManager {
    private static ParticleManager instance;
    private List<Particle> particles;
    
    private ParticleManager() {
        particles = new ArrayList<>();
    }
    
    public static ParticleManager getInstance() {
        if (instance == null) {
            instance = new ParticleManager();
        }
        return instance;
    }
    
    /**
     * 새로운 파티클 생성
     * @param position 시작 위치
     * @param velocity 이동 속도
     * @param alpha 초기 투명도
     * @param scale 크기 배율
     * @param type 파티클 종류
     */
    public void createParticle(Vec2 position, Vec2 velocity, float alpha, float scale, ParticleType type) {
        Particle particle = new Particle(position, velocity, 0f, alpha, scale, type);
        particles.add(particle);
    }
    
    /**
     * 발자국 파티클 생성
     */
    public void createFootstep(Vec2 position) {
        createParticle(position, new Vec2(0, 0), 0.7f, 1.0f, ParticleType.FOOTSTEP);
    }
    
    /**
     * 순간이동 파티클 생성
     */
    public void createTeleport(Vec2 position, Vec2 velocity) {
        createParticle(position, velocity, 0.8f, 1.2f, ParticleType.TELEPORT);
    }
    
    /**
     * 투명화 파티클 생성
     */
    public void createInvisibility(Vec2 position) {
        createParticle(position, new Vec2(0, 0), 0.5f, 0.8f, ParticleType.INVISIBILITY);
    }
    
    /**
     * 모든 파티클 업데이트
     * @param deltaTime 프레임 간 시간 간격
     */
    public void update(float deltaTime) {
        Iterator<Particle> it = particles.iterator();
        while (it.hasNext()) {
            Particle particle = it.next();
            if (!particle.update(deltaTime)) {
                it.remove(); // 수명이 다한 파티클 제거
            }
        }
    }
    
    /**
     * 현재 활성화된 파티클 목록 반환
     */
    public List<Particle> getParticles() {
        return particles;
    }
}