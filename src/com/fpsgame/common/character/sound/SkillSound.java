package com.fpsgame.common.character.sound;

/**
 * 캐릭터 스킬 사운드 효과 정의
 */
public enum SkillSound {
    INVISIBILITY_START("ghost_invisible_start"),  // 투명화 시작
    INVISIBILITY_END("ghost_invisible_end"),      // 투명화 해제
    TELEPORT("ghost_teleport"),                   // 순간이동
    BASIC_ATTACK("ghost_basic_attack");          // 기본 공격
    
    private final String soundId;
    
    SkillSound(String soundId) {
        this.soundId = soundId;
    }
    
    public String getSoundId() {
        return soundId;
    }
}