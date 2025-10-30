package com.fpsgame.common.character.visual;

/**
 * 캐릭터의 시각적 효과를 관리하는 클래스
 */
public class VisualEffectManager {
    private static final float DEFAULT_ALPHA = 1.0f;
    private static final float MIN_ALPHA = 0.0f;
    private static final float MAX_ALPHA = 1.0f;
    
    private float currentAlpha;
    private boolean isVisible;
    
    public VisualEffectManager() {
        this.currentAlpha = DEFAULT_ALPHA;
        this.isVisible = true;
    }
    
    /**
     * 투명도 설정 (0: 완전 투명, 1: 완전 불투명)
     * @param alpha 0.0 ~ 1.0 사이의 투명도 값
     */
    public void setAlpha(float alpha) {
        this.currentAlpha = Math.max(MIN_ALPHA, Math.min(alpha, MAX_ALPHA));
    }
    
    /**
     * 캐릭터 표시 여부 설정
     * @param visible true면 표시, false면 숨김
     */
    public void setVisible(boolean visible) {
        this.isVisible = visible;
    }
    
    /**
     * 현재 투명도 값 반환
     */
    public float getCurrentAlpha() {
        return currentAlpha;
    }
    
    /**
     * 표시 상태 반환
     */
    public boolean isVisible() {
        return isVisible;
    }
}