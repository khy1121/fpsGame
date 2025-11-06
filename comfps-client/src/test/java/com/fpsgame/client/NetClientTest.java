package com.fpsgame.client;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;

import com.fpsgame.common.Protocol;

/**
 * NetClient 단위 테스트
 */
public class NetClientTest {

    // 클라이언트 생성: 리스너 주입 시 정상 생성되는지 확인
    @Test
    void testClientCreation() {
        NetClient.Listener listener = new NetClient.Listener() {
            @Override
            public void onChat(String text) {}
        };
        
        NetClient client = new NetClient(listener, false);
        
        assertNotNull(client);
    }

    // 리스너 콜백: onChat 등 콜백 필드가 정상적으로 연결되는지 확인(호출 여부는 별도 시뮬레이션 필요)
    @Test
    void testListenerCallbacks() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean chatReceived = new AtomicBoolean(false);
        
        NetClient.Listener listener = new NetClient.Listener() {
            @Override
            public void onChat(String text) {
                chatReceived.set(true);
                latch.countDown();
            }
            
            @Override
            public void onOpen(NetClient client) {
                // 연결 성공
            }
        };
        
        NetClient client = new NetClient(listener, false);
        
        assertNotNull(client);
        assertFalse(chatReceived.get());
    }

    // onWelcome 콜백: 환영 메시지 콜백 경로가 준비되어 있는지 확인
    @Test
    void testWelcomeListener() {
        AtomicBoolean welcomeReceived = new AtomicBoolean(false);
        
        NetClient.Listener listener = new NetClient.Listener() {
            @Override
            public void onWelcome(Protocol.Welcome welcome) {
                welcomeReceived.set(true);
                assertNotNull(welcome);
            }
        };
        
        NetClient client = new NetClient(listener, false);
        
        assertNotNull(client);
        assertFalse(welcomeReceived.get());
    }

    // onPing/onPong 콜백: 핑퐁 콜백 경로가 준비되어 있는지 확인
    @Test
    void testPingPongListener() {
        AtomicBoolean pingReceived = new AtomicBoolean(false);
        AtomicBoolean pongReceived = new AtomicBoolean(false);
        
        NetClient.Listener listener = new NetClient.Listener() {
            @Override
            public void onPing(long nonce) {
                pingReceived.set(true);
            }
            
            @Override
            public void onPong(long nonce) {
                pongReceived.set(true);
            }
        };
        
        NetClient client = new NetClient(listener, false);
        
        assertNotNull(client);
        assertFalse(pingReceived.get());
        assertFalse(pongReceived.get());
    }

    // onDisconnected 콜백: 연결 종료 콜백 경로 확인
    @Test
    void testDisconnectListener() {
        AtomicBoolean disconnected = new AtomicBoolean(false);
        
        NetClient.Listener listener = new NetClient.Listener() {
            @Override
            public void onDisconnected(String message) {
                disconnected.set(true);
            }
        };
        
        NetClient client = new NetClient(listener, false);
        
        assertNotNull(client);
        assertFalse(disconnected.get());
    }

    // onPhaseUpdate 콜백: 단계 변경 이벤트 콜백 경로 확인
    @Test
    void testPhaseUpdateListener() {
        AtomicBoolean phaseUpdated = new AtomicBoolean(false);
        
        NetClient.Listener listener = new NetClient.Listener() {
            @Override
            public void onPhaseUpdate(int phaseCode) {
                phaseUpdated.set(true);
            }
        };
        
        NetClient client = new NetClient(listener, false);
        
        assertNotNull(client);
        assertFalse(phaseUpdated.get());
    }

    // onSnapshotV2 콜백: 스냅샷 수신 콜백 경로 확인
    @Test
    void testSnapshotListener() {
        AtomicBoolean snapshotReceived = new AtomicBoolean(false);
        
        NetClient.Listener listener = new NetClient.Listener() {
            @Override
            public void onSnapshotV2(java.util.List<com.fpsgame.common.SnapshotV2.Entry> list) {
                snapshotReceived.set(true);
            }
        };
        
        NetClient client = new NetClient(listener, false);
        
        assertNotNull(client);
        assertFalse(snapshotReceived.get());
    }

    // 다중 콜백: 여러 콜백 메서드가 같은 리스너에서 함께 구성되는지 확인
    @Test
    void testMultipleListeners() {
        int[] callCount = {0};
        
        NetClient.Listener listener = new NetClient.Listener() {
            @Override
            public void onChat(String text) { callCount[0]++; }
            
            @Override
            public void onWelcome(Protocol.Welcome welcome) { callCount[0]++; }
            
            @Override
            public void onPing(long nonce) { callCount[0]++; }
        };
        
        NetClient client = new NetClient(listener, false);
        
        assertNotNull(client);
        assertEquals(0, callCount[0]);
    }
}
