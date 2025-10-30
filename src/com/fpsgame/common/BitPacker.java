package com.fpsgame.common;

/**
 * 비트 연산 헬퍼 유틸리티.
 *
 * <p>용도</p>
 * <ul>
 *   <li>여러 개의 불리언/작은 정수 값을 하나의 정수 비트마스크에 압축/해제</li>
 *   <li>입력 버튼 플래그(예: {@code PlayerNet.Btn}) 처리에 유용</li>
 *   <li>순수 정적 메서드로 구성되어 외부 의존성이 전혀 없음</li>
 * </ul>
 *
 * <p>예시</p>
 * <pre>{@code
 * int mask = 0;
 * mask = BitPacker.set(mask, 0, true);  // 0번째 비트 ON
 * mask = BitPacker.set(mask, 3, true);  // 3번째 비트 ON
 * boolean isOn = BitPacker.test(mask, 3); // true
 * mask = BitPacker.toggle(mask, 0);       // 0번째 비트 토글
 *
 * // 부분 필드(예: 3비트 값)를 5번째 위치부터 저장/읽기
 * mask = BitPacker.put(mask, 5, 3, 0b101); // 5~7번째 비트에 0b101 저장
 * int v = BitPacker.get(mask, 5, 3);       // 0b101
 * }</pre>
 */
public final class BitPacker {

    private BitPacker() {}

    // ========================= 단일 비트 =========================

    /** index번째 비트를 1 또는 0으로 설정하여 새 마스크를 반환. */
    public static int set(int mask, int index, boolean on) {
        checkIndex(index);
        if (on) return mask | (1 << index);
        else    return mask & ~(1 << index);
    }

    /** index번째 비트를 토글하여 새 마스크를 반환. */
    public static int toggle(int mask, int index) {
        checkIndex(index);
        return mask ^ (1 << index);
    }

    /** index번째 비트가 1인지 확인. */
    public static boolean test(int mask, int index) {
        checkIndex(index);
        return (mask & (1 << index)) != 0;
    }

    // ========================= 부분 필드 =========================

    /**
     * 시작 위치 {@code offset}부터 {@code width}비트 길이의 필드에 값을 저장.
     * @param mask   기존 비트마스크
     * @param offset 시작 비트 위치(0이 LSB)
     * @param width  필드 폭(1~31)
     * @param value  저장할 값(폭을 초과하는 상위 비트는 잘림)
     * @return 갱신된 마스크
     */
    public static int put(int mask, int offset, int width, int value) {
        checkRange(offset, width);
        int m = ((1 << width) - 1) << offset;   // 대상 필드 마스크
        int v = (value & ((1 << width) - 1)) << offset;
        return (mask & ~m) | v;
    }

    /**
     * 시작 위치 {@code offset}부터 {@code width}비트 길이의 필드 값을 읽음.
     * @return 추출된 값(0 이상, 최대 {@code (1<<width)-1})
     */
    public static int get(int mask, int offset, int width) {
        checkRange(offset, width);
        int m = ((1 << width) - 1);
        return (mask >> offset) & m;
    }

    // ========================= 유틸 =========================

    /** 모든 비트를 0으로. */
    public static int clear() { return 0; }

    /** 모든 비트를 1로(32비트). */
    public static int allOnes() { return ~0; }

    private static void checkIndex(int index) {
        if (index < 0 || index >= 32) {
            throw new IllegalArgumentException("bit index out of range: " + index);
        }
    }

    private static void checkRange(int offset, int width) {
        if (width <= 0 || width >= 32) {
            throw new IllegalArgumentException("width must be 1..31: " + width);
        }
        if (offset < 0 || offset + width > 32) {
            throw new IllegalArgumentException("offset/width out of range: " + offset + "+" + width);
        }
    }
}
