package com.fpsgame.client.model;

import com.fpsgame.common.PlayerNet;
import com.fpsgame.common.Vec2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * 서버 권위 스냅샷 보간 버퍼.
 *
 * <p>역할</p>
 * <ul>
 *   <li>서버가 주기적으로 보낸 {@link PlayerNet.Snapshot} 페이로드를 수신/저장</li>
 *   <li>렌더 타임스텝에서 이전/현재 스냅샷을 선형 보간하여 화면용 좌표를 제공</li>
 *   <li>클라 예측 없이도 부드러운 움직임을 보장(기본 보간)</li>
 * </ul>
 *
 * <p>사용 가이드</p>
 * <pre>
 *   SnapshotBuffer buf = new SnapshotBuffer();
 *   // NetClient 수신부에서:
 *   if (frame.opcode == Protocol.Opcode.SNAPSHOT) {
 *       buf.onSnapshot(frame.payload, System.nanoTime());
 *   }
 *   // 렌더 단계에서:
 *   List<SnapshotBuffer.RenderPlayer> players = buf.getRenderPlayers(alpha);
 * </pre>
 *
 * <p>보간 파라미터 {@code alpha}</p>
 * <ul>
 *   <li>0.0 = 이전 스냅샷, 1.0 = 현재 스냅샷</li>
 *   <li>고정틱(서버 20Hz 등)과 렌더 주기 사이의 시간 비율로 산출</li>
 * </ul>
 */
public class SnapshotBuffer {

    /** 렌더에 필요한 최소 정보 */
    public static final class RenderPlayer {
        public final int id, team, hp;
        public final Vec2 pos, vel;

        public RenderPlayer(int id, int team, int hp, Vec2 pos, Vec2 vel) {
            this.id = id; this.team = team; this.hp = hp; this.pos = pos; this.vel = vel;
        }
    }

    /** 내부 저장용 엔티티 스냅샷 */
    private static final class P {
        int id, team, hp;
        float x, y, vx, vy;

        static P from(PlayerNet.Snapshot.Player sp) {
            P p = new P();
            p.id = sp.id; p.team = sp.team; p.hp = sp.hp;
            p.x = sp.x; p.y = sp.y; p.vx = sp.vx; p.vy = sp.vy;
            return p;
        }
    }

    /** 하나의 스냅샷(틱/엔티티 모음) */
    private static final class S {
        final int tick;
        final long recvNanos; // 수신 시각(보간 진단용)
        final HashMap<Integer, P> map = new HashMap<>();

        S(int tick, long recvNanos) { this.tick = tick; this.recvNanos = recvNanos; }
    }

    private volatile S prev;   // 이전 스냅샷
    private volatile S curr;   // 최신 스냅샷

    /** 새 스냅샷 수신 시 호출. payload는 {@link PlayerNet#buildSnapshotPayload(PlayerNet.Snapshot)} 포맷. */
    public synchronized void onSnapshot(byte[] payload, long recvNanos) {
        PlayerNet.Snapshot s = PlayerNet.parseSnapshot(payload);
        S next = new S(s.tick, recvNanos);
        for (PlayerNet.Snapshot.Player sp : s.players) {
            next.map.put(sp.id, P.from(sp));
        }
        // 현재를 이전으로 밀고 저장
        this.prev = this.curr;
        this.curr = next;
    }

    /** 보간된 플레이어 배열을 반환. alpha∈[0,1] */
    public synchronized ArrayList<RenderPlayer> getRenderPlayers(float alpha) {
        S a = prev, b = curr;
        ArrayList<RenderPlayer> out = new ArrayList<>();
        if (b == null) return out;

        alpha = clamp01(alpha);

        // 두 스냅샷 모두 있는 경우 보간, 아니면 현재 값 그대로
        if (a != null) {
            // 모든 id의 합집합을 순회
            for (Map.Entry<Integer, P> e : b.map.entrySet()) {
                int id = e.getKey();
                P pb = e.getValue();
                P pa = a.map.get(id);
                if (pa == null) {
                    // 새로 등장: 현재 값 그대로
                    out.add(new RenderPlayer(id, pb.team, pb.hp, new Vec2(pb.x, pb.y), new Vec2(pb.vx, pb.vy)));
                } else {
                    float x = lerp(pa.x, pb.x, alpha);
                    float y = lerp(pa.y, pb.y, alpha);
                    float vx = lerp(pa.vx, pb.vx, alpha);
                    float vy = lerp(pa.vy, pb.vy, alpha);
                    out.add(new RenderPlayer(id, pb.team, pb.hp, new Vec2(x, y), new Vec2(vx, vy)));
                }
            }
        } else {
            for (P p : b.map.values()) {
                out.add(new RenderPlayer(p.id, p.team, p.hp, new Vec2(p.x, p.y), new Vec2(p.vx, p.vy)));
            }
        }
        return out;
    }

    /** 최신 틱 번호(없으면 -1) */
    public int currentTick() {
        S c = curr;
        return c == null ? -1 : c.tick;
    }

    /** 최근 두 스냅샷 수신 간격(ns). 수신이 1회 뿐이면 -1. */
    public long lastDeltaNanos() {
        S a = prev, b = curr;
        if (a == null || b == null) return -1L;
        return Math.max(0L, b.recvNanos - a.recvNanos);
    }

    // ===== 유틸 =====

    private static float lerp(float a, float b, float t) { return a + (b - a) * t; }

    private static float clamp01(float v) {
        if (v < 0f) return 0f; if (v > 1f) return 1f; return v;
    }
}
