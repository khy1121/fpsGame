package com.fpsgame.client.net;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

/** 길이접두 + UTF-8 페이로드 생성 유틸 (데모 전용) */
public final class DemoPayloads {

    private DemoPayloads() {}

    public static byte[] phaseUpdate(String phase, int round,
                                     int scoreA, int scoreB,
                                     int aliveA, int aliveB,
                                     int remainSec) {
        byte[] p = str(phase);
        int cap = p.length + 4 + 2 + 2 + 2 + 2 + 4;
        ByteBuffer bb = ByteBuffer.allocate(cap).order(ByteOrder.BIG_ENDIAN);
        bb.put(p);
        bb.putInt(round);
        bb.putShort((short) scoreA);
        bb.putShort((short) scoreB);
        bb.putShort((short) aliveA);
        bb.putShort((short) aliveB);
        bb.putInt(remainSec);
        return bb.array();
    }

    public static byte[] countdown(int sec) {
        ByteBuffer bb = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN);
        bb.putInt(sec);
        return bb.array();
    }

    public static byte[] roundResult(String winnerTeam, String reason) {
        byte[] a = str(winnerTeam);
        byte[] b = str(reason);
        ByteBuffer bb = ByteBuffer.allocate(a.length + b.length).order(ByteOrder.BIG_ENDIAN);
        bb.put(a).put(b);
        return bb.array();
    }

    public static byte[] readyToggle(int sessionId, boolean ready) {
        ByteBuffer bb = ByteBuffer.allocate(4 + 1).order(ByteOrder.BIG_ENDIAN);
        bb.putInt(sessionId).put((byte) (ready ? 1 : 0));
        return bb.array();
    }

    private static byte[] str(String s) {
        if (s == null) s = "";
        byte[] data = s.getBytes(StandardCharsets.UTF_8);
        ByteBuffer bb = ByteBuffer.allocate(4 + data.length).order(ByteOrder.BIG_ENDIAN);
        bb.putInt(data.length).put(data);
        return bb.array();
    }
}
