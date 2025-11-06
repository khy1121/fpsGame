package com.fpsgame.server;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fpsgame.common.Protocol;

/**
 * DefaultServerRouter 라우팅 동작 단위 테스트 (가짜 ServerContext/Hooks 사용)
 */
class DefaultServerRouterTest {

    private static class CapturedBroadcast {
        final byte opcode; final byte[] payload;
        CapturedBroadcast(byte opcode, byte[] payload){ this.opcode=opcode; this.payload=payload; }
    }

    private static class FakeCtx implements DefaultServerRouter.ServerContext {
        final ConcurrentHashMap<Integer, ByteArrayOutputStream> outs = new ConcurrentHashMap<>();
        final List<CapturedBroadcast> broadcasts = new ArrayList<>();
        final List<Integer> closed = new ArrayList<>();
        final List<String> logs = new ArrayList<>();

        @Override public DataOutput getSender(int sessionId) {
            ByteArrayOutputStream baos = outs.computeIfAbsent(sessionId, k -> new ByteArrayOutputStream());
            return new DataOutputStream(baos);
        }
        @Override public void broadcast(byte opcode, byte[] payload) {
            broadcasts.add(new CapturedBroadcast(opcode, payload));
        }
        @Override public void closeSession(int sessionId) { closed.add(sessionId); }
        @Override public void log(String message) { logs.add(message); }

        byte[] getOutBytes(int sid){
            ByteArrayOutputStream baos = outs.get(sid);
            return baos != null ? baos.toByteArray() : new byte[0];
        }
    }

    private static class HookProbe implements DefaultServerRouter.Hooks {
        int lastChatFrom = -1; String lastChatText = null;
        Integer setReadySid = null; Boolean setReadyFlag = null;
        Integer selSid = null; Integer selTeam = null; Integer selChar = null;
        Integer voteSid = null; Integer voteMap = null;
        Integer byeSid = null;
        Integer lastActionSid = null; Integer lastActionType = null;
        Integer lastInputSid = null; byte[] lastInputPayload = null;

        @Override public void onChat(int fromSessionId, String text, DefaultServerRouter.Broadcaster bc) throws IOException {
            lastChatFrom = fromSessionId; lastChatText = text; bc.broadcastChat(text);
        }
        @Override public void setReady(int sessionId, boolean ready) { setReadySid = sessionId; setReadyFlag = ready; }
        @Override public void setSelection(int sessionId, int team, int character) { selSid=sessionId; selTeam=team; selChar=character; }
        @Override public void registerMapVote(int sessionId, int mapId) { voteSid=sessionId; voteMap=mapId; }
        @Override public void onClientBye(int sessionId) { byeSid= sessionId; }
        @Override public void onInputFrame(int sessionId, byte[] payload) { lastInputSid=sessionId; lastInputPayload=payload; }
        @Override public void onAction(int sessionId, int actionType) { lastActionSid=sessionId; lastActionType=actionType; }
    }

    private DefaultServerRouter router;
    private HookProbe hooks;
    private FakeCtx ctx;

    @BeforeEach
    void setup() {
        hooks = new HookProbe();
        router = new DefaultServerRouter(hooks);
        ctx = new FakeCtx();
    }

    // CHAT 라우팅: Hook 호출 및 브로드캐스트 페이로드 검증
    @Test
    void routesChatAndBroadcasts() {
        int sid = 7;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Protocol.putUtf8(baos, "hello");
        Protocol.Frame f = new Protocol.Frame(Protocol.Opcode.CHAT, baos.toByteArray());
        router.route(ctx, sid, f);

        assertEquals(7, hooks.lastChatFrom);
        assertEquals("hello", hooks.lastChatText);
        assertFalse(ctx.broadcasts.isEmpty());
        CapturedBroadcast b = ctx.broadcasts.get(0);
        assertEquals(Protocol.Opcode.CHAT, b.opcode);
        assertEquals("hello", Protocol.parseChat(b.payload));
    }

    // PING 처리: 동일 nonce로 PONG 응답을 전송하는지 확인
    @Test
    void repliesPongToPing() throws IOException {
        int sid = 3; long nonce = 123456789L;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Protocol.putLong(baos, nonce);
        Protocol.Frame f = new Protocol.Frame(Protocol.Opcode.PING, baos.toByteArray());
        router.route(ctx, sid, f);

        byte[] out = ctx.getOutBytes(sid);
        assertTrue(out.length > 0);
        DataInputStream din = new DataInputStream(new ByteArrayInputStream(out));
        Protocol.Frame sent = Protocol.read(din);
        assertEquals(Protocol.Opcode.PONG, sent.opcode);
        assertEquals(nonce, Protocol.parseNonce(sent.payload));
    }

    // READY_TOGGLE: 준비 상태 토글 훅 호출 확인
    @Test
    void togglesReadyViaHook() {
        int sid = 10;
        Protocol.Frame f = new Protocol.Frame(Protocol.Opcode.READY_TOGGLE, new byte[]{1});
        router.route(ctx, sid, f);
        assertEquals(10, hooks.setReadySid);
        assertEquals(Boolean.TRUE, hooks.setReadyFlag);
    }

    // SET_SELECTION / MAP_VOTE: 페이로드 파싱 및 훅 인자 확인
    @Test
    void parsesSelectionAndMapVote() {
        int sid = 2;
        Protocol.Frame sel = new Protocol.Frame(Protocol.Opcode.SET_SELECTION, new byte[]{1, 5});
        router.route(ctx, sid, sel);
        assertEquals(2, hooks.selSid);
        assertEquals(1, hooks.selTeam);
        assertEquals(5, hooks.selChar);

        Protocol.Frame vote = new Protocol.Frame(Protocol.Opcode.MAP_VOTE, new byte[]{2});
        router.route(ctx, sid, vote);
        assertEquals(2, hooks.voteSid);
        assertEquals(2, hooks.voteMap);
    }

    // BYE/ACTION/INPUT: 세션 종료, 행동 타입, 입력 프레임 전달 확인
    @Test
    void handlesByeAndActionAndInput() {
        int sid = 9;
        router.route(ctx, sid, new Protocol.Frame(Protocol.Opcode.BYE, new byte[0]));
        assertEquals(9, hooks.byeSid);
        assertTrue(ctx.closed.contains(9));

        router.route(ctx, sid, new Protocol.Frame(Protocol.Opcode.ACTION, new byte[]{2}));
        assertEquals(9, hooks.lastActionSid);
        assertEquals(2, hooks.lastActionType);

        byte[] payload = new byte[]{1,2,3};
        router.route(ctx, sid, new Protocol.Frame(Protocol.Opcode.INPUT, payload));
        assertEquals(9, hooks.lastInputSid);
        assertArrayEquals(payload, hooks.lastInputPayload);
    }
}
