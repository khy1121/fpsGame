package com.fpsgame.server;

import com.fpsgame.common.Protocol;

/**
 * 서버 라우터 인터페이스
 * - TCP 세션에서 수신한 프레임을 게임/채팅/로비 처리기로 라우팅
 * - 수명 이벤트(onOpen/onClosed), 프레임 이벤트(onFrame) 제공
 * - 구현체는 상태 비보유를 권장(필요 시 상위에서 관리)
 */
public interface ServerRouter {

    /** 세션 오픈 알림 */
    default void onOpen(SessionHandle session) {}

    /** 프레임 수신 알림 */
    void onFrame(SessionHandle session, Protocol.Frame frame);

    /** 세션 종료 알림 */
    default void onClosed(SessionHandle session, String reason) {}

    // 세션 송수신 최소 핸들(서버 네트 계층 제공)
    interface SessionHandle {
        /** 원격 표시 문자열(ip:port) */
        String remote();
        /** 프레임 송신 */
        void send(Protocol.Opcode opcode, byte[] payload);
        /** UTF-8 문자열 송신 헬퍼 */
        default void sendText(Protocol.Opcode opcode, String text) { send(opcode, Protocol.utf8(text)); }
        /** 세션 종료 요청 */
        void close(String reason);
    }

    // 기본 구현(no-op)
    final class Noop implements ServerRouter {
        @Override public void onFrame(SessionHandle session, Protocol.Frame frame) { /* no-op */ }
    }
}

