package com.fpsgame.common;

/**
 * 지수이동평균(EMA) 기반의 간단 스무더
 * - 프레임/RTT/입력 등 노이즈 많은 값의 안정화에 유용
 * - 알파(0..1) 혹은 시간상수(tau)로 구성 가능
 * 수식: y_t = y_{t-1} + alpha * (x_t - y_{t-1})
 */
public final class ExpSmoother {

    private double value;
    private boolean initialized = false;

    /** 0..1, 최근값 반영 강도 */
    private final double alpha;

    /** @param alpha 0..1, 값이 클수록 반응이 빠름 */
    public ExpSmoother(double alpha) {
        if (alpha < 0.0 || alpha > 1.0) throw new IllegalArgumentException("alpha must be in [0,1]");
        this.alpha = alpha;
    }

    /**
     * 시간상수(tau)와 샘플 간격(dt)로 alpha를 유도하는 팩토리
     * 일반식: alpha = 1 - exp(-dt / tau)
     */
    public static ExpSmoother withTimeConstant(double tauSeconds, double dtSeconds) {
        if (tauSeconds <= 0.0 || dtSeconds <= 0.0) throw new IllegalArgumentException("tauSeconds and dtSeconds must be > 0");
        double a = 1.0 - Math.exp(-dtSeconds / tauSeconds);
        if (a < 0.0) a = 0.0; if (a > 1.0) a = 1.0;
        return new ExpSmoother(a);
    }

    /** 현재 스무딩된 최신 출력(초기엔 0) */
    public double get() { return value; }

    /** 초기화 여부 */
    public boolean isInitialized() { return initialized; }

    /** 입력 샘플을 추가하고 스무딩 결과를 반환 */
    public double add(double sample) {
        if (!initialized) { value = sample; initialized = true; }
        else { value = value + alpha * (sample - value); }
        return value;
    }

    /** 값을 지정 값으로 설정(초기화 상태 진입) */
    public void resetTo(double v) { value = v; initialized = true; }

    /** 초기 상태로 되돌림(다음 add 시 샘플로 설정) */
    public void clear() { value = 0.0; initialized = false; }
}

