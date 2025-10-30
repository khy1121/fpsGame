package com.fpsgame.common.character.sound;

import com.fpsgame.common.Vec2;

/**
 * 게임 내 사운드 효과를 관리하는 매니저 클래스
 */
public class SoundManager {
    private static SoundManager instance;
    private float masterVolume;
    private float effectVolume;
    private boolean isMuted;
    
    private SoundManager() {
        this.masterVolume = 1.0f;
        this.effectVolume = 1.0f;
        this.isMuted = false;
    }
    
    public static SoundManager getInstance() {
        if (instance == null) {
            instance = new SoundManager();
        }
        return instance;
    }
    
    /**
     * 발소리 재생
     */
    public void playFootstep(FootstepSound sound, Vec2 position, float volume) {
        if (isMuted) return;
        
        float finalVolume = masterVolume * effectVolume * volume;
        // TODO: 실제 사운드 시스템과 연동
        // playSound(sound.getSoundId(), position, finalVolume);
    }
    
    /**
     * 스킬 사운드 재생
     */
    public void playSkillSound(SkillSound sound, Vec2 position) {
        if (isMuted) return;
        
        float finalVolume = masterVolume * effectVolume;
        // TODO: 실제 사운드 시스템과 연동
        // playSound(sound.getSoundId(), position, finalVolume);
    }
    
    // 볼륨 제어 메서드
    public void setMasterVolume(float volume) {
        this.masterVolume = Math.max(0.0f, Math.min(volume, 1.0f));
    }
    
    public void setEffectVolume(float volume) {
        this.effectVolume = Math.max(0.0f, Math.min(volume, 1.0f));
    }
    
    public void setMuted(boolean muted) {
        this.isMuted = muted;
    }
    
    // 게터
    public float getMasterVolume() { return masterVolume; }
    public float getEffectVolume() { return effectVolume; }
    public boolean isMuted() { return isMuted; }
}