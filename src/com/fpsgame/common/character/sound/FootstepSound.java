package com.fpsgame.common.character.sound;

/**
 * 발소리 종류를 정의하는 열거형
 */
public enum FootstepSound {
    WALK("walk"),      // 걷기 발소리
    RUN("run"),        // 달리기 발소리
    JUMP("jump"),      // 점프 시 발소리
    LAND("land");      // 착지 시 발소리
    
    private final String soundId;
    
    FootstepSound(String soundId) {
        this.soundId = soundId;
    }
    
    public String getSoundId() {
        return soundId;
    }
}