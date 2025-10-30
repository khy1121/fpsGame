package com.fpsgame.common;

import java.util.Arrays;

/**
 * 고정 크기 윈도우 기반의 단순 이동 평균(Simple Moving Average).
 *
 * <p>특징</p>
 * <ul>
 *   <li>스레드 안전: 모든 퍼블릭 메서드는 동기화됨</li>
 *   <li>값이 아직 충분치 않을 때는 채워진 표본만으로 평균을 계산</li>
 *   <li>최근 값으로 덮어쓰는 환형 버퍼 구현</li>
 * </ul>
 *
 * <p>사용</p>
 * <pre>
 *   MovingAverage avg = new MovingAverage(120);
 *   avg.addSample(60.0);
 *   double fps = avg.getAverage();
 * </pre>
 */
public final class MovingAverage {

    private final double[] ring;
    private int index = 0;      // 다음에 쓸 위치
    private int filled = 0;     // 채워진 표본 수(최대 window)
    private double sum = 0.0;   // 현재 합계

    /**
     * @param window 표본 개수(윈도우 크기). 2 이상 권장.
     */
    public MovingAverage(int window) {
        if (window <= 0) throw new IllegalArgumentException("window must be > 0");
        this.ring = new double[window];
    }

    /** 윈도우 크기 반환. */
    public int window() { return ring.length; }

    /** 내부 상태 초기화(모든 표본 삭제). */
    public synchronized void clear() {
        Arrays.fill(ring, 0.0);
        index = 0;
        filled = 0;
        sum = 0.0;
    }

    /** 표본을 1개 추가. O(1). */
    public synchronized void addSample(double v) {
        if (filled < ring.length) {
            ring[index] = v;
            sum += v;
            filled++;
            index = (index + 1) % ring.length;
        } else {
            // 오래된 값 제거 후 추가
            double old = ring[index];
            sum -= old;
            ring[index] = v;
            sum += v;
            index = (index + 1) % ring.length;
        }
    }

    /** 현재 평균을 반환(표본이 하나도 없으면 0). */
    public synchronized double getAverage() {
        return filled == 0 ? 0.0 : (sum / filled);
    }
}
