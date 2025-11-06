package com.fpsgame.common;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.Test;

/**
 * Protocol 단위 테스트
 */
public class ProtocolTest {

    // 프레임 쓰기/읽기: opcode와 payload가 보존되는지 확인
    @Test
    void testWriteReadFrame() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(baos);
        
        byte[] payload = "test".getBytes();
        Protocol.writeFrame(out, Protocol.CHAT, payload);
        
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        DataInputStream in = new DataInputStream(bais);
        
        Protocol.Frame frame = Protocol.readFrame(in);
        
        assertEquals(Protocol.CHAT, frame.opcode);
        assertArrayEquals(payload, frame.payload);
    }

    // 채팅 메시지: 전송/파싱이 일치하는지 확인
    @Test
    void testChatMessage() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(baos);
        
        String message = "Hello, World!";
        Protocol.sendChat(out, message);
        
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        DataInputStream in = new DataInputStream(bais);
        
        Protocol.Frame frame = Protocol.readFrame(in);
        
        assertEquals(Protocol.CHAT, frame.opcode);
        assertEquals(message, Protocol.parseChat(frame.payload));
    }

    // 핑/퐁: nonce가 일관되게 전송/파싱되는지 확인
    @Test
    void testPingPong() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(baos);
        
        long nonce = 12345678L;
        Protocol.sendPing(out, nonce);
        
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        DataInputStream in = new DataInputStream(bais);
        
        Protocol.Frame frame = Protocol.readFrame(in);
        
        assertEquals(Protocol.PING, frame.opcode);
        assertEquals(nonce, Protocol.parseNonce(frame.payload));
    }

    // 환영 메시지: 모든 필드가 전송/파싱 시 일치하는지 확인
    @Test
    void testWelcomeMessage() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(baos);
        
        int myId = 1;
        int team = 0;
        int character = 2;
        int worldW = 1000;
        int worldH = 800;
        int mapId = 3;
        
        Protocol.sendWelcome(out, myId, team, character, worldW, worldH, mapId);
        
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        DataInputStream in = new DataInputStream(bais);
        
        Protocol.Frame frame = Protocol.readFrame(in);
        
        assertEquals(Protocol.WELCOME, frame.opcode);
        Protocol.Welcome welcome = Protocol.parseWelcome(frame.payload);
        
        assertEquals(myId, welcome.myId);
        assertEquals(team, welcome.team);
        assertEquals(character, welcome.character);
        assertEquals(worldW, welcome.worldW);
        assertEquals(worldH, welcome.worldH);
        assertEquals(mapId, welcome.mapId);
    }

    // 준비 토글: 불리언 플래그 전송/파싱 확인
    @Test
    void testReadyToggle() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(baos);
        
        Protocol.sendReadyToggle(out, true);
        
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        DataInputStream in = new DataInputStream(bais);
        
        Protocol.Frame frame = Protocol.readFrame(in);
        
        assertEquals(Protocol.READY_TOGGLE, frame.opcode);
        assertTrue(Protocol.parseReadyToggle(frame.payload));
    }

    // 선택 설정: 팀/캐릭터 선택 전송/파싱 확인
    @Test
    void testSetSelection() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(baos);
        
        int team = 1;
        int character = 3;
        
        Protocol.sendSetSelection(out, team, character);
        
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        DataInputStream in = new DataInputStream(bais);
        
        Protocol.Frame frame = Protocol.readFrame(in);
        
        assertEquals(Protocol.SET_SELECTION, frame.opcode);
        Protocol.Selection selection = Protocol.parseSetSelection(frame.payload);
        
        assertEquals(team, selection.team);
        assertEquals(character, selection.character);
    }

    // 맵 투표: 맵 ID 전송/파싱 확인
    @Test
    void testMapVote() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(baos);
        
        int mapId = 5;
        
        Protocol.sendMapVote(out, mapId);
        
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        DataInputStream in = new DataInputStream(bais);
        
        Protocol.Frame frame = Protocol.readFrame(in);
        
        assertEquals(Protocol.MAP_VOTE, frame.opcode);
        assertEquals(mapId, Protocol.parseMapVote(frame.payload));
    }

    // 단계 업데이트: 현재 게임 단계 전송/파싱 확인
    @Test
    void testPhaseUpdate() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(baos);
        
        int phase = 2;
        
        Protocol.sendPhaseUpdate(out, phase);
        
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        DataInputStream in = new DataInputStream(bais);
        
        Protocol.Frame frame = Protocol.readFrame(in);
        
        assertEquals(Protocol.PHASE_UPDATE, frame.opcode);
        assertEquals(phase, Protocol.parsePhaseUpdate(frame.payload));
    }

    // 카운트다운: 남은 초 전송/파싱 확인
    @Test
    void testCountdown() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(baos);
        
        int seconds = 30;
        
        Protocol.sendCountdown(out, seconds);
        
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        DataInputStream in = new DataInputStream(bais);
        
        Protocol.Frame frame = Protocol.readFrame(in);
        
        assertEquals(Protocol.COUNTDOWN, frame.opcode);
        assertEquals(seconds, Protocol.parseCountdown(frame.payload));
    }

    // 종료(bye): opcode와 빈 payload 확인
    @Test
    void testByeMessage() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(baos);
        
        Protocol.sendBye(out);
        
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        DataInputStream in = new DataInputStream(bais);
        
        Protocol.Frame frame = Protocol.readFrame(in);
        
        assertEquals(Protocol.BYE, frame.opcode);
        assertEquals(0, frame.payload.length);
    }

    // put/get Int: 바이트 배열로 직렬화/역직렬화 확인
    @Test
    void testPutGetInt() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        int value = 0x12345678;
        Protocol.putInt(baos, value);
        
        byte[] bytes = baos.toByteArray();
        int retrieved = Protocol.getInt(bytes, 0);
        
        assertEquals(value, retrieved);
    }

    // put/get Long: 바이트 배열로 직렬화/역직렬화 확인
    @Test
    void testPutGetLong() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        long value = 0x123456789ABCDEFL;
        Protocol.putLong(baos, value);
        
        byte[] bytes = baos.toByteArray();
        long retrieved = Protocol.getLong(bytes, 0);
        
        assertEquals(value, retrieved);
    }

    // put/get Byte: 부호 없는 8비트 저장/로드 확인
    @Test
    void testPutGetByte() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        int value = 0xFF;
        Protocol.putByte(baos, value);
        
        byte[] bytes = baos.toByteArray();
        int retrieved = Protocol.getU8(bytes, 0);
        
        assertEquals(value, retrieved);
    }

    // put/get UTF-8: 문자열 직렬화/역직렬화 확인 (한글 포함)
    @Test
    void testPutGetUtf8() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        String value = "안녕하세요 Hello 123";
        Protocol.putUtf8(baos, value);
        
        byte[] bytes = baos.toByteArray();
        Protocol.Pair<String, Integer> result = Protocol.getUtf8(bytes, 0);
        
        assertEquals(value, result.a);
    }

    // 빈 payload 프레임: null 입력 시 길이 0으로 처리되는지 확인
    @Test
    void testEmptyPayload() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(baos);
        
        Protocol.writeFrame(out, Protocol.BYE, null);
        
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        DataInputStream in = new DataInputStream(bais);
        
        Protocol.Frame frame = Protocol.readFrame(in);
        
        assertEquals(Protocol.BYE, frame.opcode);
        assertEquals(0, frame.payload.length);
    }

    // 큰 payload 프레임: 대용량 데이터도 손실 없이 왕복되는지 확인
    @Test
    void testLargePayload() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(baos);
        
        byte[] largePayload = new byte[10000];
        for (int i = 0; i < largePayload.length; i++) {
            largePayload[i] = (byte)(i % 256);
        }
        
        Protocol.writeFrame(out, Protocol.SNAPSHOT, largePayload);
        
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        DataInputStream in = new DataInputStream(bais);
        
        Protocol.Frame frame = Protocol.readFrame(in);
        
        assertEquals(Protocol.SNAPSHOT, frame.opcode);
        assertArrayEquals(largePayload, frame.payload);
    }

    // 잘못된 프레임 길이: 0 길이 등 비정상 입력 시 예외 처리 확인
    @Test
    void testInvalidFrameLength() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(baos);
        
        try {
            out.writeInt(0); // Invalid length
            out.flush();
            
            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            DataInputStream in = new DataInputStream(bais);
            
            assertNotNull(assertThrows(IOException.class, () -> Protocol.readFrame(in)));
        } catch (IOException e) {
            fail("Should not throw during setup");
        }
    }

    // opcode 상수 값 검증: 프로토콜 호환성 보장
    @Test
    void testOpcodeConstants() {
        assertEquals(0x01, Protocol.WELCOME);
        assertEquals(0x02, Protocol.CHAT);
        assertEquals(0x03, Protocol.PING);
        assertEquals(0x04, Protocol.PONG);
        assertEquals(0x05, Protocol.BYE);
        assertEquals(0x10, Protocol.READY_TOGGLE);
        assertEquals(0x11, Protocol.SET_SELECTION);
        assertEquals(0x12, Protocol.MAP_VOTE);
        assertEquals(0x20, Protocol.PHASE_UPDATE);
        assertEquals(0x21, Protocol.COUNTDOWN);
        assertEquals(0x22, Protocol.ROUND_RESULT);
        assertEquals(0x30, Protocol.INPUT);
        assertEquals(0x31, Protocol.SNAPSHOT);
        assertEquals(0x33, Protocol.ACTION);
    }
}
