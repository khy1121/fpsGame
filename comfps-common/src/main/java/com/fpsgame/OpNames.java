package com.fpsgame.common;

/**
 * Opcode 바이트 값을 사람이 읽기 쉬운 문자열로 변환하는 헬퍼.
 *
 * <p>로그/디버깅/UI 표시용으로만 사용하며, 프로토콜 스펙의 단일 출처는
 * {@link Protocol} 이어야 한다.</p>
 */
public final class OpNames {

    private OpNames() {}

    /**
     * opcode 값을 사람이 읽기 쉬운 이름으로 반환한다.
     * 알 수 없는 값은 "0xHH" 형태의 16진수로 반환.
     */
    public static String name(byte opcode) {
        // 기존/확장 opcode를 모두 케이스로 나열
        if (opcode == Protocol.Opcode.WELCOME)       return "WELCOME";
        if (opcode == Protocol.Opcode.CHAT)          return "CHAT";
        if (opcode == Protocol.Opcode.PING)          return "PING";
        if (opcode == Protocol.Opcode.PONG)          return "PONG";
        if (opcode == Protocol.Opcode.BYE)           return "BYE";

        // 확장(로비/라운드/선택/스냅샷/입력 등)
        if (opcode == Protocol.Opcode.READY_TOGGLE)  return "READY_TOGGLE";
        if (opcode == Protocol.Opcode.SET_SELECTION) return "SET_SELECTION";
        if (opcode == Protocol.Opcode.MAP_VOTE)      return "MAP_VOTE";
        if (opcode == Protocol.Opcode.PHASE_UPDATE)  return "PHASE_UPDATE";
        if (opcode == Protocol.Opcode.COUNTDOWN)     return "COUNTDOWN";
        if (opcode == Protocol.Opcode.ROUND_RESULT)  return "ROUND_RESULT";
        if (opcode == Protocol.Opcode.SNAPSHOT)      return "SNAPSHOT";
        if (opcode == Protocol.Opcode.INPUT)         return "INPUT";

        // 모르는 값 → 16진수
        return String.format("0x%02X", opcode);
    }

    /** 알려진 opcode 인지 여부. */
    public static boolean isKnown(byte opcode) {
        return !name(opcode).startsWith("0x");
    }
}
