package com.fpsgame.common;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * 클라이언트→서버 INPUT 페이로드 빌더 유틸리티
 * 지원 형식:
 * - 형식 A: 키 마스크 + 조준각 [byte mask][float aim]
 *   · mask 비트: 0=Up(W), 1=Down(S), 2=Left(A), 3=Right(D)
 *   · aim: 라디안(오른쪽=0, 위=+pi/2)
 * - 형식 B: 이동 축 + 선택적 조준각 [float ax][float ay][float aim?]
 *   · ax/ay는 [-1..+1] 범위, 정규화는 서버에서 처리
 */
public final class InputPackets {

    private InputPackets() { /* no instance */ }

    // 형식 A: 키 마스크 + 조준각 -------------------------------------------------

    /** W/A/S/D 상태로 마스크 비트를 구성 */
    public static int toMask(boolean w, boolean a, boolean s, boolean d) {
        int m = 0;
        if (w) m |= 0x01; // Up
        if (s) m |= 0x02; // Down
        if (a) m |= 0x04; // Left
        if (d) m |= 0x08; // Right
        return m;
    }

    /** 형식 A 페이로드 생성: [byte mask][float aim] */
    public static byte[] maskWithAim(boolean w, boolean a, boolean s, boolean d, float aimRad) {
        int mask = toMask(w, a, s, d);
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(5);
            Binary.putByte(baos, mask);
            Binary.putFloat(baos, aimRad);
            return baos.toByteArray();
        } catch (IOException e) {
            // ByteArrayOutputStream 예외는 드묾; 최소 페이로드로 폴백
            return new byte[] { (byte) (mask & 0xFF) };
        }
    }

    // 형식 B: 이동 축 + 선택적 조준각 -------------------------------------------

    /** 형식 B 페이로드 생성: [float ax][float ay][float aim?] */
    public static byte[] axisWithAim(float ax, float ay, float aimRad) {
        try {
            boolean includeAim = !Float.isNaN(aimRad);
            ByteArrayOutputStream baos = new ByteArrayOutputStream(includeAim ? 12 : 8);
            Binary.putFloat(baos, ax);
            Binary.putFloat(baos, ay);
            if (includeAim) Binary.putFloat(baos, aimRad);
            return baos.toByteArray();
        } catch (IOException e) {
            // 비정상 I/O 시 빈 페이로드 반환
            return new byte[0];
        }
    }
}

