package com.fpsgame.client;

import com.fpsgame.common.OpNames;
import com.fpsgame.common.Protocol;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * NetClient 이벤트/프레임을 사람이 읽기 쉬운 형태로 로깅하는 리스너.
 *
 * <p>용도</p>
 * <ul>
 *   <li>네트워크 디버깅 시 콘솔/파일 등에 간단히 출력</li>
 *   <li>다른 리스너와 체인으로 묶을 수 있도록 delegate를 지원</li>
 * </ul>
 *
 * <p>예시</p>
 * <pre>
 *   NetLogger logger = new NetLogger(System.out::println, existingListener);
 *   net = new NetClient(host, port, 10_000, logger);
 * </pre>
 */
public final class NetLogger implements NetClient.Listener {

    /** 로그 출력 대상(기본은 System.out::println) */
    private final Consumer<String> out;

    /** 하위 위임 리스너(선택) */
    private final NetClient.Listener delegate;

    public NetLogger() {
        this(System.out::println, null);
    }

    public NetLogger(Consumer<String> out) {
        this(out, null);
    }

    public NetLogger(Consumer<String> out, NetClient.Listener delegate) {
        this.out = Objects.requireNonNull(out, "out");
        this.delegate = delegate;
    }

    private void log(String s) {
        try { out.accept(s); } catch (Throwable ignored) {}
    }

    // ================= NetClient.Listener 구현 =================

    @Override
    public void onOpen(NetClient c) {
        log("[net] OPEN " + safe(c));
        if (delegate != null) delegate.onOpen(c);
    }

    @Override
    public void onClosed(NetClient c, String reason) {
        log("[net] CLOSED " + safe(c) + " reason=" + reason);
        if (delegate != null) delegate.onClosed(c, reason);
    }

    @Override
    public void onDisconnected(String reason) {
        log("[net] DISCONNECTED reason=" + reason);
        if (delegate != null) delegate.onDisconnected(reason);
    }

    @Override
    public void onChat(String text) {
        log("[chat] " + text);
        if (delegate != null) delegate.onChat(text);
    }

    @Override
    public void onFrame(NetClient c, Protocol.Frame frame) {
        if (frame != null) {
            String name = OpNames.name(frame.opcode);
            log("[frame] " + name + " (" + (frame.payload == null ? 0 : frame.payload.length) + " bytes)");
        } else {
            log("[frame] null");
        }
        if (delegate != null) delegate.onFrame(c, frame);
    }

    // ================= 유틸 =================

    private static String safe(NetClient c) {
        try {
            return c == null ? "-" : String.valueOf(c.remoteAddress());
        } catch (Throwable ignored) {
            return "-";
        }
    }
}
