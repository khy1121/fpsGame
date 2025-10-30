package com.fpsgame.client.model;

import com.fpsgame.client.NetClient;
import com.fpsgame.common.Protocol;

/**
 * NetClient 수신 프레임 중 SNAPSHOT(opcode)을 가로채
 * {@link SnapshotBuffer}에 적재해 주는 어댑터.
 *
 * <p>용도</p>
 * <ul>
 *   <li>{@link NetClient.Listener}를 구현하여 기존 리스너 체인에 끼워 넣는다.</li>
 *   <li>SNAPSHOT 프레임은 {@link SnapshotBuffer#onSnapshot(byte[], long)}으로 저장하고,
 *       그 외 프레임은 하위(delegate) 리스너로 넘긴다.</li>
 *   <li>게임 렌더러는 {@link #buffer()}를 통해 보간된 좌표를 얻는다.</li>
 * </ul>
 *
 * <p>연동 예시</p>
 * <pre>
 *   SnapshotBridge bridge = new SnapshotBridge(existingListener);
 *   net = new NetClient(host, port, 20_000, bridge);
 *   // 렌더 루프
 *   var players = bridge.buffer().getRenderPlayers(alpha);
 * </pre>
 */
public class SnapshotBridge implements NetClient.Listener {

    private final SnapshotBuffer buffer = new SnapshotBuffer();
    private final NetClient.Listener delegate; // 하위 리스너로 이벤트 전달(선택)

    public SnapshotBridge() { this(null); }

    public SnapshotBridge(NetClient.Listener delegate) {
        this.delegate = delegate;
    }

    /** 외부에서 보간 결과를 조회할 때 사용 */
    public SnapshotBuffer buffer() { return buffer; }

    // ================= NetClient.Listener 구현 =================

    @Override
    public void onOpen(NetClient c) {
        if (delegate != null) delegate.onOpen(c);
    }

    @Override
    public void onClosed(NetClient c, String reason) {
        if (delegate != null) delegate.onClosed(c, reason);
    }

    @Override
    public void onDisconnected(String reason) {
        if (delegate != null) delegate.onDisconnected(reason);
    }

    @Override
    public void onChat(String text) {
        if (delegate != null) delegate.onChat(text);
    }

    @Override
    public void onFrame(NetClient c, Protocol.Frame frame) {
        // SNAPSHOT 프레임만 가로채어 버퍼에 저장, 나머지는 하위 리스너로 위임
        if (frame != null && frame.opcode == Protocol.Opcode.SNAPSHOT) {
            buffer.onSnapshot(frame.payload, System.nanoTime());
            return;
        }
        if (delegate != null) delegate.onFrame(c, frame);
    }
}
