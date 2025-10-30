package com.fpsgame.client.net;

import java.lang.reflect.Method;

/**
 * SetSelectionSender (경량 호환 유틸)
 * ------------------------------------------------------------
 * 캐릭터 선택 전송을 다양한 호출부와 느슨하게 연결하기 위한 헬퍼.
 *
 * 목표
 * - 컨트롤러/넷 객체의 공개 메서드에 우선 위임(존재 시)
 *   1) sendSetSelection(int team, int character)
 *   2) sendSetSelection(int character)           // team 기본 0 처리
 * - 위 메서드가 없으면 조용히 무시(no-op)하여 테스트/샌드박스에서도 안전.
 *
 * 사용
 *   SetSelectionSender.send(netLike, "3");  // "3" → character index 3
 *   SetSelectionSender.send(netLike, "Raven"); // 파싱 실패 시 0으로 폴백
 */
public final class SetSelectionSender {

    private SetSelectionSender() {}

    /**
     * 캐릭터 선택 전송(문자열 id를 정수 인덱스로 관용 파싱).
     * @param netLike NetClient, ClientController 등 전송 메서드를 가진 객체
     * @param characterId "3" 같이 숫자가 들어오면 그 값, 그 외엔 0으로 폴백
     */
    public static void send(Object netLike, String characterId) {
        if (netLike == null) return;

        final int teamIdx = 0; // 기본 팀(별도의 팀 선택 UI가 있다면 그쪽에서 호출부 조정)
        final int charIdx = toIndex(characterId);

        // 1) sendSetSelection(int,int)
        try {
            Method m = netLike.getClass().getMethod("sendSetSelection", int.class, int.class);
            m.invoke(netLike, teamIdx, charIdx);
            return;
        } catch (Throwable ignore) {
            // 다음 경로 시도
        }

        // 2) sendSetSelection(int)
        try {
            Method m = netLike.getClass().getMethod("sendSetSelection", int.class);
            m.invoke(netLike, charIdx);
            return;
        } catch (Throwable ignore) {
            // no-op: 최종 폴백(테스트 환경/더미 넷일 수 있음)
        }
    }

    /** 문자열을 정수 인덱스로 관용 파싱(실패 시 0). 공백/Null 허용. */
    private static int toIndex(String s) {
        if (s == null) return 0;
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception ignore) {
            return 0;
        }
    }
}
