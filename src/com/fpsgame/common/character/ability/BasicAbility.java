package com.fpsgame.common.character.ability;

/**
 * 캐릭터의 기본 공격을 구현하는 추상 클래스.
 * 대부분의 캐릭터는 발사체를 생성하는 기본 공격을 가진다.
 */
public abstract class BasicAbility implements Ability {
    protected float cooldown;        // 현재 쿨다운
    protected float maxCooldown;     // 최대 쿨다운
    protected float damage;          // 기본 피해량
    protected float fireRateMultiplier = 1.0f;  // 발사 속도 배율
    
    protected BasicAbility(float maxCooldown, float damage) {
        this.maxCooldown = maxCooldown;
        this.damage = damage;
        this.cooldown = 0;
    }
    
    @Override
    public boolean isReady() {
        return cooldown <= 0;
    }
    
    @Override
    public float getCooldown() {
        return cooldown;
    }
    
    @Override
    public void update(float deltaTime) {
        if (cooldown > 0) {
            cooldown = Math.max(0, cooldown - deltaTime);
        }
    }
    
    /**
     * 기본 공격 사용 시 쿨다운 적용
     */
    protected void startCooldown() {
        // 발사 속도 배율 적용
        this.cooldown = this.maxCooldown / fireRateMultiplier;
    }
    
    /**
     * 기본 공격의 피해량 반환
     * @return 기본 피해량
     */
    protected float getDamage() {
        return damage;
    }
    
    /**
     * 발사 속도 배율 설정
     * @param multiplier 발사 속도 배율 (1.0이 기본)
     */
    public void setFireRateMultiplier(float multiplier) {
        this.fireRateMultiplier = Math.max(0.1f, multiplier);
    }
    
    /**
     * 현재 발사 속도 배율 반환
     * @return 발사 속도 배율
     */
    public float getFireRateMultiplier() {
        return fireRateMultiplier;
    }
}