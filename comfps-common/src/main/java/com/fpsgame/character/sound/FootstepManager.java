package com.fpsgame.common.character.sound;

import com.fpsgame.common.Vec2;

/**
 * 캐릭터의 발소리를 관리하는 클래스
 */
public class FootstepManager {
    private static final float DEFAULT_VOLUME = 1.0f;
    private static final float MIN_VOLUME = 0.0f;
    private static final float MAX_VOLUME = 1.0f;
    
    private float currentVolume;
    private boolean isMuted;
    
    public FootstepManager() {
        this.currentVolume = DEFAULT_VOLUME;
        this.isMuted = false;
    }
    
    /**
     * 발소리 볼륨 설정
     * @param volume 0.0 ~ 1.0 사이의 볼륨값
     */
    public void setVolume(float volume) {
        this.currentVolume = Math.max(MIN_VOLUME, Math.min(volume, MAX_VOLUME));
    }
    
    /**
     * 발소리 음소거 설정
     * @param muted true면 음소거, false면 해제
     */
    public void setMuted(boolean muted) {
        this.isMuted = muted;
    }
    
    /**
     * 발소리 재생
     * @param sound 재생할 발소리 종류
     * @param position 발소리가 나는 위치
     */
    public void playFootstep(FootstepSound sound, Vec2 position) {
        if (isMuted || currentVolume <= MIN_VOLUME) return;
        
        // TODO: 실제 사운드 시스템과 연동
        // SoundSystem.play(sound.getSoundId(), position, currentVolume);
    }
    
    /**
     * 현재 볼륨 값 반환
     */
    public float getCurrentVolume() {
        return currentVolume;
    }
    
    /**
     * 음소거 상태 반환
     */
    public boolean isMuted() {
        return isMuted;
    }
}