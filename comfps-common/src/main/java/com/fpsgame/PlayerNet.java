package com.fpsgame.common;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 서버 권위 이동/스냅샷 동기화를 위한 경량 네트 모델 유틸.
 *
 * <p>역할</p>
 * <ul>
 *   <li>클라이언트 입력(Input) → payload 직렬화/역직렬화</li>
 *   <li>서버 스냅샷(Snapshot) → payload 직렬화/역직렬화</li>
 *   <li>{@link Protocol} 의 opcode( {@link Protocol.Opcode#INPUT},
 *       {@link Protocol.Opcode#SNAPSHOT} )와 함께 사용</li>
 * </ul>
 *
 * <p>설계 원칙</p>
 * <ul>
 *   <li>고정 크기 필드 우선(네트 대역 최소화)</li>
 *   <li>float 사용 시 IEEE754 비트로 직렬화(엔디언 고정, 상호운용성)</li>
 *   <li>추후 필드 확장 대비하여 버전 필드 포함</li>
 * </ul>
 */
public final class PlayerNet {

    private PlayerNet() {}

    // =====================================================================================
    // 입력 패킷
    // =====================================================================================

    /**
     * 클라이언트 입력 상태.
     *
     * <p>시퀀스 번호(seq)는 클라 예측/보정에 사용(서버가 echo 하여 스냅샷과 매칭).</p>
     */
    public static final class Input {
        public final int version;     // 포맷 버전(현재 1)
        public final int seq;         // 클라이언트 로컬 입력 시퀀스
        public final int buttons;     // 비트마스크(UP/DOWN/LEFT/RIGHT/JUMP/FIRE/ALT_FIRE/RELOAD)
        public final float aimX;      // 조준 벡터 또는 마우스 정규화 x
        public final float aimY;      // 조준 벡터 또는 마우스 정규화 y
        public final float moveX;     // 이동 입력(-1..1)
        public final float moveY;     // 이동 입력(-1..1)

        public Input(int version, int seq, int buttons, float aimX, float aimY, float moveX, float moveY) {
            this.version = version;
            this.seq = seq;
            this.buttons = buttons;
            this.aimX = aimX;
            this.aimY = aimY;
            this.moveX = moveX;
            this.moveY = moveY;
        }

        @Override public String toString() {
            return "Input{v=" + version + ",seq=" + seq + ",btn=" + buttons +
                    ",aim=(" + aimX + "," + aimY + "),move=(" + moveX + "," + moveY + ")}";
        }
    }

    // 버튼 비트 정의(자유롭게 확장 가능)
    public static final class Btn {
        public static final int UP       = 1 << 0;
        public static final int DOWN     = 1 << 1;
        public static final int LEFT     = 1 << 2;
        public static final int RIGHT    = 1 << 3;
        public static final int JUMP     = 1 << 4;
        public static final int FIRE     = 1 << 5;
        public static final int ALT_FIRE = 1 << 6;
        public static final int RELOAD   = 1 << 7;
    }

    /** Input → payload 직렬화 */
    public static byte[] buildInputPayload(Input in) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(32);
        Protocol.putByte(baos, in.version);         // [u8]  version
        Protocol.putInt(baos, in.seq);              // [i32] seq
        Protocol.putInt(baos, in.buttons);          // [i32] buttons
        putFloat(baos, in.aimX);                    // [f32] aimX
        putFloat(baos, in.aimY);                    // [f32] aimY
        putFloat(baos, in.moveX);                   // [f32] moveX
        putFloat(baos, in.moveY);                   // [f32] moveY
        return baos.toByteArray();
    }

    /** payload → Input 역직렬화 */
    public static Input parseInput(byte[] p) {
        int off = 0;
        int version = Protocol.getU8(p, off); off += 1;
        int seq     = Protocol.getInt(p, off); off += 4;
        int buttons = Protocol.getInt(p, off); off += 4;
        float aimX  = getFloat(p, off); off += 4;
        float aimY  = getFloat(p, off); off += 4;
        float moveX = getFloat(p, off); off += 4;
        float moveY = getFloat(p, off); // off += 4;
        return new Input(version, seq, buttons, aimX, aimY, moveX, moveY);
    }

    // =====================================================================================
    // 스냅샷 패킷
    // =====================================================================================

    /**
     * 서버가 주기적으로 브로드캐스트하는 스냅샷.
     * <p>간단히 모든 플레이어의 위치/속도/팀/체력 등을 포함한다.</p>
     */
    public static final class Snapshot {
        public final int version;       // 포맷 버전(현재 1)
        public final int tick;          // 서버 권위 틱 번호
        public final ArrayList<Player> players;

        public Snapshot(int version, int tick, ArrayList<Player> players) {
            this.version = version;
            this.tick = tick;
            this.players = players;
        }

        /** 플레이어 단위 스냅샷 */
        public static final class Player {
            public final int id;      // 세션/플레이어 ID
            public final float x, y;  // 위치
            public final float vx, vy;// 속도
            public final int team;    // 팀 코드
            public final int hp;      // 체력(0..255)

            public Player(int id, float x, float y, float vx, float vy, int team, int hp) {
                this.id = id; this.x = x; this.y = y; this.vx = vx; this.vy = vy; this.team = team; this.hp = hp;
            }
        }
    }

    /** Snapshot → payload 직렬화 */
    public static byte[] buildSnapshotPayload(Snapshot s) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(64 + s.players.size() * 24);
        Protocol.putByte(baos, s.version);         // [u8]  version
        Protocol.putInt(baos, s.tick);             // [i32] tick
        Protocol.putInt(baos, s.players.size());   // [i32] count
        for (Snapshot.Player pl : s.players) {
            Protocol.putInt(baos, pl.id);          // [i32]
            putFloat(baos, pl.x);                  // [f32]
            putFloat(baos, pl.y);                  // [f32]
            putFloat(baos, pl.vx);                 // [f32]
            putFloat(baos, pl.vy);                 // [f32]
            Protocol.putByte(baos, pl.team);       // [u8]
            Protocol.putByte(baos, pl.hp);         // [u8]
        }
        return baos.toByteArray();
    }

    /** payload → Snapshot 역직렬화 */
    public static Snapshot parseSnapshot(byte[] p) {
        int off = 0;
        int version = Protocol.getU8(p, off); off += 1;
        int tick    = Protocol.getInt(p, off); off += 4;
        int count   = Protocol.getInt(p, off); off += 4;
        ArrayList<Snapshot.Player> players = new ArrayList<>(Math.max(0, count));
        for (int i = 0; i < count; i++) {
            int id   = Protocol.getInt(p, off); off += 4;
            float x  = getFloat(p, off); off += 4;
            float y  = getFloat(p, off); off += 4;
            float vx = getFloat(p, off); off += 4;
            float vy = getFloat(p, off); off += 4;
            int team = Protocol.getU8(p, off); off += 1;
            int hp   = Protocol.getU8(p, off); off += 1;
            players.add(new Snapshot.Player(id, x, y, vx, vy, team, hp));
        }
        return new Snapshot(version, tick, players);
    }

    // =====================================================================================
    // 내부 헬퍼: float <-> bytes
    // =====================================================================================

    /** IEEE754 float → big-endian 4바이트 */
    private static void putFloat(ByteArrayOutputStream baos, float f) {
        int bits = Float.floatToIntBits(f);
        baos.write((bits >>> 24) & 0xFF);
        baos.write((bits >>> 16) & 0xFF);
        baos.write((bits >>> 8) & 0xFF);
        baos.write(bits & 0xFF);
    }

    /** big-endian 4바이트 → IEEE754 float */
    private static float getFloat(byte[] buf, int off) {
        int bits = ((buf[off] & 0xFF) << 24)
                | ((buf[off + 1] & 0xFF) << 16)
                | ((buf[off + 2] & 0xFF) << 8)
                | (buf[off + 3] & 0xFF);
        return Float.intBitsToFloat(bits);
    }

    // =====================================================================================
    // 간편 빌더
    // =====================================================================================

    /** 입력 빌더 헬퍼 */
    public static Input input(int seq, int buttons, float aimX, float aimY, float moveX, float moveY) {
        return new Input(1, seq, buttons, aimX, aimY, moveX, moveY);
    }

    /** 스냅샷 빌더 헬퍼 */
    public static Snapshot snapshot(int tick, List<Snapshot.Player> players) {
        return new Snapshot(1, tick, new ArrayList<>(players));
    }
}
