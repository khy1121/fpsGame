package com.fpsgame.common;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * 단순 길이 선행 프로토콜.
 * 프레임: [int 길이][byte 명령코드][페이로드 바이트...]
 * 길이 = 1 + 페이로드 길이
 */
public final class Protocol {
    private Protocol() {}

    // ==== 명령 코드 ====
    public static final class Opcode {
        // 핵심
        public static final byte WELCOME       = 0x01;
        public static final byte CHAT          = 0x02;
        public static final byte PING          = 0x03;
        public static final byte PONG          = 0x04;
        public static final byte BYE           = 0x05;

        // 로비
        public static final byte READY_TOGGLE  = 0x10;
        public static final byte SET_SELECTION = 0x11;
        public static final byte MAP_VOTE      = 0x12;

        // 매치 진행
        public static final byte PHASE_UPDATE  = 0x20;
        public static final byte COUNTDOWN     = 0x21;
        public static final byte ROUND_RESULT  = 0x22;
        public static final byte READY_STATUS  = 0x23; // 선택사항

        // 게임플레이
        public static final byte INPUT         = 0x30;
        public static final byte SNAPSHOT      = 0x31;
    }

    // 공개 별칭 (레거시 코드에서 직접 접근)
    public static final byte BYE           = Opcode.BYE;
    public static final byte CHAT          = Opcode.CHAT;
    public static final byte PING          = Opcode.PING;
    public static final byte PONG          = Opcode.PONG;
    public static final byte WELCOME       = Opcode.WELCOME;
    public static final byte READY_TOGGLE  = Opcode.READY_TOGGLE;
    public static final byte SET_SELECTION = Opcode.SET_SELECTION;
    public static final byte MAP_VOTE      = Opcode.MAP_VOTE;
    public static final byte PHASE_UPDATE  = Opcode.PHASE_UPDATE;
    public static final byte COUNTDOWN     = Opcode.COUNTDOWN;
    public static final byte ROUND_RESULT  = Opcode.ROUND_RESULT;
    public static final byte INPUT         = Opcode.INPUT;
    public static final byte SNAPSHOT      = Opcode.SNAPSHOT;
    public static final byte READY_STATUS  = Opcode.READY_STATUS;
    // extension channel (server->client broadcast of projectiles)
    public static final byte PROJECTILES   = (byte)0x32;

    // ==== Frame container ====
    public static final class Frame {
        public final byte opcode;
        public final byte[] payload;
        public Frame(byte opcode, byte[] payload) { this.opcode = opcode; this.payload = payload; }
        public String asUtf8() { return getUtf8(payload, 0).a; }
    }

    // ==== I/O primitives ====
    public static void writeFrame(DataOutput out, byte opcode, byte[] payload) throws IOException {
        int len = 1 + (payload == null ? 0 : payload.length);
        out.writeInt(len);
        out.writeByte(opcode);
        if (payload != null && payload.length > 0) out.write(payload);
    }
    public static Frame readFrame(DataInput in) throws IOException {
        int length = in.readInt();
        if (length < 1) throw new IOException("Invalid frame length: " + length);
        byte opcode = in.readByte();
        int payloadLen = length - 1;
        byte[] p = new byte[payloadLen];
        if (payloadLen > 0) in.readFully(p);
        return new Frame(opcode, p);
    }

    public static void putInt(ByteArrayOutputStream baos, int v){ baos.write((v>>>24)&0xFF); baos.write((v>>>16)&0xFF); baos.write((v>>>8)&0xFF); baos.write(v&0xFF); }
    public static void putLong(ByteArrayOutputStream baos, long v){
        baos.write((int)((v>>>56)&0xFF)); baos.write((int)((v>>>48)&0xFF)); baos.write((int)((v>>>40)&0xFF)); baos.write((int)((v>>>32)&0xFF));
        baos.write((int)((v>>>24)&0xFF)); baos.write((int)((v>>>16)&0xFF)); baos.write((int)((v>>>8)&0xFF)); baos.write((int)(v&0xFF));
    }
    public static void putByte(ByteArrayOutputStream baos, int v){ baos.write(v & 0xFF); }
    public static void putUtf8(ByteArrayOutputStream baos, String s){
        if (s == null) s = ""; byte[] b = s.getBytes(StandardCharsets.UTF_8);
        if (b.length > 0xFFFF) throw new IllegalArgumentException("String too long: "+b.length);
        baos.write((b.length>>>8)&0xFF); baos.write(b.length&0xFF); baos.write(b,0,b.length);
    }
    public static int getInt(byte[] buf,int off){ return ((buf[off]&0xFF)<<24)|((buf[off+1]&0xFF)<<16)|((buf[off+2]&0xFF)<<8)|(buf[off+3]&0xFF); }
    public static long getLong(byte[] buf,int off){
        return ((long)(buf[off]&0xFF)<<56)|((long)(buf[off+1]&0xFF)<<48)|((long)(buf[off+2]&0xFF)<<40)|((long)(buf[off+3]&0xFF)<<32)
             | ((long)(buf[off+4]&0xFF)<<24)|((long)(buf[off+5]&0xFF)<<16)|((long)(buf[off+6]&0xFF)<<8)|((long)(buf[off+7]&0xFF));
    }
    public static int getU8(byte[] buf,int off){ return buf[off] & 0xFF; }
    public static Pair<String,Integer> getUtf8(byte[] buf,int off){ int len=((buf[off]&0xFF)<<8)|(buf[off+1]&0xFF); int next=off+2+len; String s=new String(buf,off+2,len,StandardCharsets.UTF_8); return new Pair<>(s,next); }
    public static final class Pair<A,B>{ public final A a; public final B b; public Pair(A a,B b){this.a=a;this.b=b;} }

    // ==== High level builders/parsers ====
    // Chat
    public static void sendChat(DataOutput out, String text) throws IOException { ByteArrayOutputStream baos=new ByteArrayOutputStream(64); putUtf8(baos,text); writeFrame(out, Opcode.CHAT, baos.toByteArray()); }
    public static String parseChat(byte[] p){ return getUtf8(p,0).a; }

    // Ping/Pong
    public static void sendPing(DataOutput out, long nonce) throws IOException { ByteArrayOutputStream baos=new ByteArrayOutputStream(8); putLong(baos, nonce); writeFrame(out, Opcode.PING, baos.toByteArray()); }
    public static void sendPong(DataOutput out, long nonce) throws IOException { ByteArrayOutputStream baos=new ByteArrayOutputStream(8); putLong(baos, nonce); writeFrame(out, Opcode.PONG, baos.toByteArray()); }
    public static long parseNonce(byte[] p){ return getLong(p,0); }

    // Bye
    public static void sendBye(DataOutput out) throws IOException { writeFrame(out, Opcode.BYE, null); }

    // Welcome (v1/v2)
    public static void sendWelcome(DataOutput out, int myId, int team, int character, int worldW, int worldH) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(16);
        putInt(baos, myId); putByte(baos, team); putByte(baos, character); putInt(baos, worldW); putInt(baos, worldH);
        writeFrame(out, Opcode.WELCOME, baos.toByteArray());
    }
    public static void sendWelcome(DataOutput out, int myId, int team, int character, int worldW, int worldH, int mapId) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(17);
        putInt(baos, myId); putByte(baos, team); putByte(baos, character); putInt(baos, worldW); putInt(baos, worldH); putByte(baos, mapId);
        writeFrame(out, Opcode.WELCOME, baos.toByteArray());
    }
    public static class Welcome { public final int myId, team, character, worldW, worldH, mapId; public Welcome(int myId,int team,int ch,int w,int h){ this(myId,team,ch,w,h,0);} public Welcome(int myId,int team,int ch,int w,int h,int mapId){ this.myId=myId; this.team=team; this.character=ch; this.worldW=w; this.worldH=h; this.mapId=mapId; } }
    public static Welcome parseWelcome(byte[] p){ int myId=getInt(p,0); int team=getU8(p,4); int ch=getU8(p,5); int w=getInt(p,6); int h=getInt(p,10); int map=(p!=null && p.length>=15)? getU8(p,14):0; return new Welcome(myId,team,ch,w,h,map); }

    // Lobby
    public static void sendReadyToggle(DataOutput out, boolean ready) throws IOException { ByteArrayOutputStream baos=new ByteArrayOutputStream(1); putByte(baos, ready?1:0); writeFrame(out, Opcode.READY_TOGGLE, baos.toByteArray()); }
    public static boolean parseReadyToggle(byte[] p){ return getU8(p,0)!=0; }
    public static void sendSetSelection(DataOutput out, int team, int character) throws IOException { ByteArrayOutputStream baos=new ByteArrayOutputStream(2); putByte(baos, team); putByte(baos, character); writeFrame(out, Opcode.SET_SELECTION, baos.toByteArray()); }
    public static class Selection { public final int team, character; public Selection(int t,int c){ team=t; character=c; } }
    public static Selection parseSetSelection(byte[] p){ return new Selection(getU8(p,0), getU8(p,1)); }
    public static void sendMapVote(DataOutput out, int mapId) throws IOException { ByteArrayOutputStream baos=new ByteArrayOutputStream(1); putByte(baos, mapId); writeFrame(out, Opcode.MAP_VOTE, baos.toByteArray()); }
    public static int parseMapVote(byte[] p){ return getU8(p,0); }

    // Progress
    public static void sendPhaseUpdate(DataOutput out, int phase) throws IOException { ByteArrayOutputStream baos=new ByteArrayOutputStream(1); putByte(baos, phase); writeFrame(out, Opcode.PHASE_UPDATE, baos.toByteArray()); }
    public static int parsePhaseUpdate(byte[] p){ return getU8(p,0); }
    public static void sendCountdown(DataOutput out, int seconds) throws IOException { ByteArrayOutputStream baos=new ByteArrayOutputStream(4); putInt(baos, seconds); writeFrame(out, Opcode.COUNTDOWN, baos.toByteArray()); }
    public static int parseCountdown(byte[] p){ return getInt(p,0); }
    public static void sendRoundResult(DataOutput out, int winnerTeam, int blueRounds, int redRounds, boolean matchEnded) throws IOException { ByteArrayOutputStream baos=new ByteArrayOutputStream(4); putByte(baos, winnerTeam); putByte(baos, blueRounds); putByte(baos, redRounds); putByte(baos, matchEnded?1:0); writeFrame(out, Opcode.ROUND_RESULT, baos.toByteArray()); }
    public static final class RoundResult { public final int winnerTeam, blueRounds, redRounds; public final boolean matchEnded; public RoundResult(int w,int b,int r,boolean m){ winnerTeam=w; blueRounds=b; redRounds=r; matchEnded=m; } }
    public static RoundResult parseRoundResult(byte[] p){ return new RoundResult(getU8(p,0), getU8(p,1), getU8(p,2), getU8(p,3)!=0); }

    // Optional READY_STATUS helpers
    public static byte[] buildReadyStatusPayload(int ready, int total){ ByteArrayOutputStream baos=new ByteArrayOutputStream(2); putByte(baos, Math.max(0, ready)); putByte(baos, Math.max(0, total)); return baos.toByteArray(); }
    public static final class ReadyStatus { public final int ready, total; public ReadyStatus(int r,int t){ ready=r; total=t; } }
    public static ReadyStatus parseReadyStatus(byte[] p){ return new ReadyStatus(getU8(p,0), getU8(p,1)); }

    // Safe sender
    public static final class SafeSender {
        private final DataOutput out; private final Object lock = new Object();
        public SafeSender(DataOutput out) { this.out = out; }
        public void send(byte opcode, byte[] payload) throws IOException { synchronized (lock) { writeFrame(out, opcode, payload); } }
        public void chat(String text) throws IOException { send(Opcode.CHAT, utf8(text)); }
        public void bye() throws IOException { send(Opcode.BYE, null); }
        public void ping(long nonce) throws IOException { ByteArrayOutputStream baos=new ByteArrayOutputStream(8); putLong(baos, nonce); send(Opcode.PING, baos.toByteArray()); }
        public void pong(long nonce) throws IOException { ByteArrayOutputStream baos=new ByteArrayOutputStream(8); putLong(baos, nonce); send(Opcode.PONG, baos.toByteArray()); }
    }

    // Legacy wrappers
    public static void write(DataOutputStream out, byte opcode, byte[] payload) throws IOException { writeFrame(out, opcode, payload); }
    public static Frame read(DataInputStream in) throws IOException { return readFrame(in); }
    public static byte[] utf8(String s){ ByteArrayOutputStream baos=new ByteArrayOutputStream(32); putUtf8(baos, s==null?"":s); return baos.toByteArray(); }
}

