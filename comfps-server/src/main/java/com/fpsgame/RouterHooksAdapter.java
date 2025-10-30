package com.fpsgame.server;

import java.io.IOException;

/**
 * {@link DefaultServerRouter.Hooks} 의 최소 구현체.
 *
 * <p>용도</p>
 * <ul>
 *   <li>서버 라우터를 빠르게 구동하기 위한 기본 어댑터</li>
 *   <li>로비/맵투표/라운드 FSM과 아직 직접 연동하지 않을 때 사용</li>
 *   <li>필요 시 각 메서드를 오버라이드하여 실제 게임 로직에 연결</li>
 * </ul>
 *
 * <p>기본 동작</p>
 * <ul>
 *   <li>채팅: 들어온 메시지를 그대로 브로드캐스트</li>
 *   <li>READY/선택/맵투표: 서버 로그만 남김</li>
 *   <li>현재 페이즈: 0(LOBBY) 고정 반환</li>
 * </ul>
 */
public class RouterHooksAdapter implements DefaultServerRouter.Hooks {

    private final DefaultServerRouter.ServerContext context;

    public RouterHooksAdapter(DefaultServerRouter.ServerContext context) {
        this.context = context;
    }

    @Override
    public void onChat(int fromSessionId, String text, DefaultServerRouter.Broadcaster bc) throws IOException {
        // 기본은 서버 프리픽스만 붙여 모든 클라에 전송
        String line = "[S](" + fromSessionId + ") " + (text == null ? "" : text);
        bc.broadcastChat(line);
        context.log("chat<" + fromSessionId + ">: " + text);
    }

    @Override
    public void setReady(int sessionId, boolean ready) {
        context.log("ready[" + sessionId + "] = " + ready);
        // TODO: 로비 상태와 연동 시 LobbyState#setReady(...) 호출
    }

    @Override
    public void setSelection(int sessionId, int team, int character) {
        context.log("select[" + sessionId + "] team=" + team + " ch=" + character);
        // TODO: 선택 상태를 서버 모델에 반영
    }

    @Override
    public void registerMapVote(int sessionId, int mapId) {
        context.log("mapVote[" + sessionId + "] map=" + mapId);
        // TODO: MapVoteManager와 연동하여 집계
    }

    @Override
    public void onClientBye(int sessionId) {
        context.log("client BYE: " + sessionId);
    }

    @Override
    public int currentPhase() {
        // 0=LOBBY (필요 시 RoundController와 연동하여 실제 페이즈 반환)
        return 0;
    }
}
