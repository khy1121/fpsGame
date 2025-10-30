package com.fpsgame.common.character.types.raven.ability;

import com.fpsgame.common.Vec2;
import com.fpsgame.common.World;
import com.fpsgame.common.character.Character;
import com.fpsgame.common.character.ability.TacticalAbility;

/**
 * 레이븐의 전술 스킬: 대쉬
 * 짧은 거리를 순간이동하며 해당 시간 동안 피해를 무시합니다.
 */
public class RavenDash extends TacticalAbility {
    
    private final float dashDistance;       // 대쉬 거리
    private Vec2 dashDirection;             // 대쉬 방향
    private float currentDashTime;          // 현재 대쉬 진행 시간
    private boolean isDashing;              // 대쉬 중 여부
    
    /**
     * 레이븐 대쉬 스킬 생성자
     * @param cooldown 쿨다운 시간 (초)
     * @param duration 대쉬 지속 시간 (초)
     * @param distance 대쉬 이동 거리
     */
    public RavenDash(float cooldown, float duration, float distance) {
        super(cooldown, duration);
        this.dashDistance = distance;
        this.isDashing = false;
        this.currentDashTime = 0f;
    }
    
    @Override
    public void activate(Character source, Vec2 direction) {
        if (!isReady()) return;
        
        // 스킬 사용 캐릭터 저장
        this.source = source;
        
        // 대쉬 방향 정규화
        this.dashDirection = direction.copy().normalize();
        
        // 대쉬 시작
        this.isDashing = true;
        this.currentDashTime = 0f;
        
        // 대쉬 중 무적 상태 적용
        source.setInvulnerable(true);
        
        // 쿨다운 시작
        startCooldown();
    }
    
    @Override
    protected void updateActiveEffect(float deltaTime) {
        if (!isDashing) return;
        
        currentDashTime += deltaTime;
        
        // 대쉬 진행률 계산 (0-1)
        float progress = Math.min(currentDashTime / duration, 1.0f);
        
        // 대쉬 이동 거리 계산 (시작은 빠르고 끝은 느리게)
        float currentSpeed = dashDistance * (1 - progress);
        
        // 캐릭터 위치 업데이트
        Vec2 movement = dashDirection.copy().mul(currentSpeed * deltaTime);
        
        // 이동 전 위치
        Vec2 currentPos = source.getPosition();
        // 이동 후 예상 위치
        Vec2 nextPos = currentPos.add(movement);
        
        // 월드가 있는 경우 벽 충돌 체크
        World world = source.getWorld();
        if (world != null && world.checkWallCollision(currentPos, nextPos)) {
            // 벽과 충돌하면 이동 취소
            isDashing = false;
            source.setInvulnerable(false);
            return;
        }
        
        // 이동 적용
        source.setPosition(nextPos);
        
        // 대쉬 종료 체크
        if (currentDashTime >= duration) {
            isDashing = false;
            // 대쉬 종료 시 무적 해제
            source.setInvulnerable(false);
        }
    }
    
    /**
     * 현재 대쉬 중인지 여부 반환
     * @return 대쉬 중 여부
     */
    public boolean isDashing() {
        return isDashing;
    }
}
