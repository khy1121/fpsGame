package com.fpsgame.client.net;

import java.lang.reflect.Method;

/**
 * MapVoteSender (경량 호환 유틸)
 * ------------------------------------------------------------
 * 맵 투표 전송을 다양한 호출부와 느슨하게 연결하기 위한 헬퍼.
 *
 * 목표
 * - 컨트롤러/넷 객체의 공개 메서드에 우선 위임(존재 시)
 *   1) sendMapVote(int mapIndex)
 * - 위 메서드가 없으면 조용히 무시(no-op)하여 테스트/샌드박스에서도 안전.
 *
 * 사용
 *   MapVoteSender.send(netLike, "2");      // "2" → map index 2
 *   MapVoteSender.send(netLike, "neonCity"); // 파싱 실패 시 0으로 폴백
 */
public final class MapVoteSender {

    private MapVoteSender() {}

    /**
     * 맵 투표 전송(문자열 id를 정수 인덱스로 관용 파싱).
     * @param netLike NetClient, ClientController 등 전송 메서드를 가진 객체
     * @param mapId 문자열 id. 정수 파싱되면 해당 값, 실패 시 0으로 폴백
     */
    public static void send(Object netLike, String mapId) {
        if (netLike == null) return;

        final int mapIndex = toIndex(mapId);

        // 1) sendMapVote(int)
        try {
            Method m = netLike.getClass().getMethod("sendMapVote", int.class);
            m.invoke(netLike, mapIndex);
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
