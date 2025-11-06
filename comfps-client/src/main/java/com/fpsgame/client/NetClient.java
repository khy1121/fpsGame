package com.fpsgame.client;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.SwingUtilities;

import com.fpsgame.common.Protocol;

/** NetClient 네트워크 코어. (주석 한글화) */
public class NetClient implements Closeable {

    // 내부 이벤트 리스너 정의

    /** 네트워크 이벤트 콜백 인터페이스 */
    public interface Listener {
        // 텍스트 채팅 수신
        default void onChat(String text) {}
        // 서버 환영 패킷 수신
        default void onWelcome(Protocol.Welcome welcome) {}
        // 핑/퐁 교환
        default void onPing(long nonce) {}
        default void onPong(long nonce) {}
        // 게임 페이즈 및 카운트다운/라운드 결과
        default void onPhaseUpdate(int phaseCode) {}
        default void onCountdown(int seconds) {}
        default void onRoundResult(Protocol.RoundResult rr) {}
        // 대기 인원 수 갱신
        default void onReadyStatus(int ready, int total) {}
        // 상세 READY_STATUS (플레이어 리스트 포함)
        default void onReadyStatusFull(Protocol.ReadyStatus rs) {}
        // 연결 끊김
        default void onDisconnected(String message) {}
        // 스냅샷/투사체 업데이트
        default void onSnapshotV2(java.util.List<com.fpsgame.common.SnapshotV2.Entry> list) {}
        default void onProjectilesV2(java.util.List<com.fpsgame.common.ProjectilesV2.Entry> list) {}

        // 연결 상태 알림
        default void onOpen(NetClient client) {}
        default void onClosed(NetClient client, String reason) {}
        default void onFrame(NetClient client, Protocol.Frame frame) {}
    }

    // 동작 옵션 및 상태

    private final boolean dispatchOnEdt;
    private final Listener listener;

    private final AtomicBoolean running = new AtomicBoolean(false);

    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private Thread rxThread;

    // 소켓/스트림/수신 스레드

    /** 리스너와 EDT 디스패치 여부를 지정하는 생성자 */
    public NetClient(Listener listener, boolean dispatchOnEdt) {
        this.listener = Objects.requireNonNull(listener, "listener");
        this.dispatchOnEdt = dispatchOnEdt;
    }

    /** 호스트/포트로 즉시 연결하는 생성자 */
    public NetClient(String host, int port, int timeoutMs, Listener listener) throws IOException {
        this(listener, true);
        connect(host, port, timeoutMs);
    }

    /** 서버에 연결한다. connectTimeoutMs는 밀리초 단위. */
    public synchronized void connect(String host, int port, int connectTimeoutMs) throws IOException {
        if (running.get()) return;

        this.socket = new Socket();
        this.socket.setTcpNoDelay(true);
        this.socket.setKeepAlive(true);
        // 읽기 타임아웃 없음(블로킹 I/O)
        this.socket.setSoTimeout(0);

        this.socket.connect(new InetSocketAddress(host, port), Math.max(0, connectTimeoutMs));

        this.in = new DataInputStream(socket.getInputStream());
        this.out = new DataOutputStream(socket.getOutputStream());

        running.set(true);
        rxThread = new Thread(this::rxLoop, "NetClient-RX");
        rxThread.setDaemon(true);
        rxThread.start();

        // 연결 완료 알림
        dispatch(() -> listener.onOpen(this));
    }

    /** 기본 타임아웃(3초)으로 연결한다. */
    public void connect(String host, int port) throws IOException {
        connect(host, port, 3000);
    }

    /** 자원 정리 및 연결 종료 */
    @Override
    public synchronized void close() {
        if (!running.getAndSet(false)) return;

        // 소켓/스트림 종료
        try { if (socket != null) socket.close(); } catch (IOException ignore) {}
        try { if (in != null) in.close(); } catch (IOException ignore) {}
        try { if (out != null) out.close(); } catch (IOException ignore) {}

        if (rxThread != null && rxThread != Thread.currentThread()) {
            try { rxThread.join(1000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
        rxThread = null;
        in = null;
        out = null;

        // 종료 콜백 알림
        dispatch(() -> {
            listener.onClosed(this, "closed");
            listener.onDisconnected("closed");
        });

        socket = null;
    }

    // 전송 API ---------------------------------------------------------

    /** 채팅 전송 */
    public void sendChat(String text) throws IOException {
        DataOutputStream o = ensureOut();
        synchronized (o) {
            Protocol.sendChat(o, text);
        }
    }

    /** 종료(작별) 전송 */
    public void sendBye() throws IOException {
        DataOutputStream o = ensureOut();
        synchronized (o) {
            Protocol.sendBye(o);
        }
    }

    /** 핑 전송 */
    public void sendPing(long nonce) throws IOException {
        DataOutputStream o = ensureOut();
        synchronized (o) {
            Protocol.sendPing(o, nonce);
        }
    }

    /** READY 상태 전송 */
    public void sendReadyToggle(boolean ready) throws IOException {
        DataOutputStream o = ensureOut();
        synchronized (o) {
            Protocol.sendReadyToggle(o, ready);
        }
    }

    /** 팀/캐릭터 선택 전송 */
    public void sendSetSelection(int team, int character) throws IOException {
        DataOutputStream o = ensureOut();
        synchronized (o) {
            Protocol.sendSetSelection(o, team, character);
        }
    }

    /** 맵 투표 전송 */
    public void sendMapVote(int mapId) throws IOException {
        DataOutputStream o = ensureOut();
        synchronized (o) {
            Protocol.sendMapVote(o, mapId);
        }
    }

    /** 임의 프레임 전송 */
    public void send(byte opcode, byte[] payload) throws IOException {
        DataOutputStream o = ensureOut();
        synchronized (o) {
            Protocol.writeFrame(o, opcode, payload);
        }
    }

    /** Send simple input mask + optional aim. */
    public void sendInputMask(byte mask, java.lang.Float aimNullable) throws IOException {
        DataOutputStream o = ensureOut();
        byte[] payload;
        if (aimNullable == null) {
            payload = new byte[]{ mask };
        } else {
            int bits = Float.floatToIntBits(aimNullable);
            payload = new byte[]{
                mask,
                (byte)((bits>>>24)&0xFF), (byte)((bits>>>16)&0xFF), (byte)((bits>>>8)&0xFF), (byte)(bits&0xFF)
            };
        }
        synchronized (o) { Protocol.writeFrame(o, Protocol.Opcode.INPUT, payload); }
    }

    /** Send action (0=BasicAttack, 1=TacticalAbility, 2=UltimateAbility) */
    public void sendAction(int actionType) throws IOException {
        DataOutputStream o = ensureOut();
        synchronized (o) { Protocol.sendAction(o, actionType); }
    }

    // 수신 루프 ---------------------------------------------------------

    private void rxLoop() {
        try {
            while (running.get()) {
                Protocol.Frame f = Protocol.readFrame(in);
                // 원시 프레임 콜백 후 처리
                dispatch(() -> listener.onFrame(this, f));
                handleFrame(f);
            }
        } catch (EOFException | SocketException eof) {
            fireDisconnected("remote closed: " + eof.getMessage());
        } catch (IOException io) {
            fireDisconnected("I/O error: " + io.getMessage());
        } catch (Throwable t) {
            fireDisconnected("rxLoop error: " + t);
        } finally {
            // 예외/종료 시 안전 종료
            try { close(); } catch (Exception ignore) {}
        }
    }

    // 프레임 디스패치 ---------------------------------------------------

    private void handleFrame(Protocol.Frame f) {
        switch (f.opcode) {
            case Protocol.Opcode.CHAT -> fireChat(Protocol.parseChat(f.payload));
            case Protocol.Opcode.WELCOME -> fireWelcome(Protocol.parseWelcome(f.payload));
            case Protocol.Opcode.PING -> firePing(Protocol.parseNonce(f.payload));
            case Protocol.Opcode.PONG -> firePong(Protocol.parseNonce(f.payload));
            case Protocol.Opcode.PHASE_UPDATE -> firePhase(Protocol.parsePhaseUpdate(f.payload));
            case Protocol.Opcode.COUNTDOWN -> fireCountdown(Protocol.parseCountdown(f.payload));
            case Protocol.Opcode.ROUND_RESULT -> fireRoundResult(Protocol.parseRoundResult(f.payload));
            case Protocol.Opcode.READY_STATUS -> {
                var rs = Protocol.parseReadyStatus(f.payload);
                fireReadyStatus(rs);
            }
            case Protocol.Opcode.SNAPSHOT -> fireSnapshotV2Safe(f.payload);
            case Protocol.PROJECTILES -> fireProjectilesV2Safe(f.payload);
            default -> { }
        }
    }

    // 개별 이벤트 디스패치 헬퍼

    private void fireChat(String text) {
        dispatch(() -> listener.onChat(text));
    }

    private void fireWelcome(Protocol.Welcome w) {
        System.out.println("[NetClient] ★ WELCOME 수신: myId=" + w.myId + " team=" + w.team + " character=" + w.character);
        dispatch(() -> listener.onWelcome(w));
    }

    private void firePing(long nonce) {
        dispatch(() -> listener.onPing(nonce));
    }

    private void firePong(long nonce) {
        dispatch(() -> listener.onPong(nonce));
    }

    private void firePhase(int code) {
        dispatch(() -> listener.onPhaseUpdate(code));
    }

    private void fireCountdown(int sec) {
        dispatch(() -> listener.onCountdown(sec));
    }

    private void fireRoundResult(Protocol.RoundResult rr) {
        dispatch(() -> listener.onRoundResult(rr));
    }

    private void fireReadyStatus(Protocol.ReadyStatus rs) {
        dispatch(() -> {
            listener.onReadyStatus(rs.ready, rs.total);
            // 타입 안전한 직접 콜백 호출
            listener.onReadyStatusFull(rs);
        });
    }

    private void fireSnapshotV2Safe(byte[] payload) {
        try {
            var list = com.fpsgame.common.SnapshotV2.parse(payload);
            // DEBUG: 스냅샷 수신 로그 (처음 5개 ID만)
            if (!list.isEmpty()) {
                StringBuilder sb = new StringBuilder("[NetClient] ★ SNAPSHOT 수신: ");
                int limit = Math.min(5, list.size());
                for (int i = 0; i < limit; i++) {
                    var e = list.get(i);
                    sb.append(String.format("id=%d(%.0f,%.0f) ", e.id, e.x, e.y));
                }
                if (list.size() > 5) sb.append("...");
                System.out.println(sb.toString());
            }
            dispatch(() -> listener.onSnapshotV2(list));
        } catch (Exception e) {
            // ignore malformed payload
        }
    }

    private void fireProjectilesV2Safe(byte[] payload) {
        try {
            var list = com.fpsgame.common.ProjectilesV2.parse(payload);
            dispatch(() -> listener.onProjectilesV2(list));
        } catch (Exception e) {
            // ignore malformed payload
        }
    }

    private void fireDisconnected(String msg) {
        dispatch(() -> {
            listener.onDisconnected(msg);
            listener.onClosed(this, msg); // 연결 종료 알림
        });
    }

    private void dispatch(Runnable r) {
        if (dispatchOnEdt && !SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(r);
        } else {
            r.run();
        }
    }

    // 유틸리티 ----------------------------------------------------------

    private DataOutputStream ensureOut() throws SocketException {
        DataOutputStream o;
        synchronized (this) { o = this.out; }
        if (o == null) throw new SocketException("Not connected.");
        return o;
    }

    /** 연결되어 있는지 여부 */
    public boolean isConnected() {
        return running.get() && socket != null && socket.isConnected() && !socket.isClosed();
    }

    /** open 상태 여부(동치) */
    public boolean isOpen() { return isConnected(); }

    /** 원격 주소 문자열 */
    public String remoteAddress() {
        try { return socket == null ? "(null)" : String.valueOf(socket.getRemoteSocketAddress()); }
        catch (Exception e) { return "(unknown)"; }
    }

    /** 원격 주소 별칭 */
    public String remote() { return remoteAddress(); }
}



