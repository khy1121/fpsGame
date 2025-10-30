package com.fpsgame.client.model;

import com.fpsgame.common.SnapshotV1;
import com.fpsgame.common.SnapshotV2;

import java.util.*;

/**
 * 플레이어 스냅샷 버퍼
 * - 서버에서 주기적으로 수신한 SNAPSHOT(v1/v2)을 보관
 * - 보간 지연(interpDelayNs)을 적용해 특정 시점의 위치/에임을 샘플링
 * - v2일 경우 팀/캐릭터 메타데이터를 함께 유지
 */
public final class PlayerSnapshotBuffer {

    /** 단일 샘플 엔트리(보간 결과) */
    public static final class Entry {
        public final int id;
        public final float x, y, aim;
        public Entry(int id, float x, float y, float aim) {
            this.id = id; this.x = x; this.y = y; this.aim = aim;
        }
        @Override public String toString() { return "Entry{id="+id+", x="+x+", y="+y+", aim="+aim+"}"; }
    }

    /** 원시 스냅샷 1프레임 + 수신 시각 */
    private static final class Shot {
        final long t; // ns (수신 시각)
        final Map<Integer, SnapshotV1.Entry> map;
        Shot(long t, Map<Integer, SnapshotV1.Entry> map) { this.t = t; this.map = map; }
    }

    /** 보간 지연(ns). 예: 100ms = 100_000_000ns */
    private final long interpDelayNs;

    /** 보관 가능한 스냅샷 최대 개수 */
    private final int capacity;

    /** 수신 스냅샷 큐(시간순) */
    private final Deque<Shot> shots = new ArrayDeque<>();

    // 선택적 메타데이터 맵(Snapshot v2 수신 시 채워짐)
    private final Map<Integer, Integer> teamById = new HashMap<>();       // 0=RED,1=BLUE
    private final Map<Integer, Integer> characterById = new HashMap<>();  // CharacterId ordinal

    /**
     * @param interpDelayNs 보간 지연(ns)
     * @param capacity      버퍼 용량(최소 64 권장)
     */
    public PlayerSnapshotBuffer(long interpDelayNs, int capacity) {
        this.interpDelayNs = Math.max(0, interpDelayNs);
        this.capacity = Math.max(8, capacity);
    }

    /** 기본 용량(64) 사용. */
    public PlayerSnapshotBuffer(long interpDelayNs) {
        this(interpDelayNs, 64);
    }

    /** 버퍼 초기화. */
    public synchronized void clear() { shots.clear(); }

    /**
     * SNAPSHOT(v1/v2) 페이로드를 파싱하여 버퍼에 추가한다.
     * @param payload Protocol.Opcode.SNAPSHOT의 payload
     * @param recvNs  System.nanoTime() 기준 수신 시각(ns)
     */
    public void push(byte[] payload, long recvNs) {
        if (payload == null || payload.length == 0) return;
        Map<Integer, SnapshotV1.Entry> map;
        // v2 메타데이터(옵션). 동기화 블록에서 맵에 병합
        Map<Integer, Integer> metaTeams = null;
        Map<Integer, Integer> metaChars = null;
        int ver = payload[0] & 0xFF;
        try {
            if (ver == 1) {
                map = SnapshotV1.parseToMap(payload);
            } else if (ver == 2) {
                Map<Integer, SnapshotV2.Entry> m2 = SnapshotV2.parseToMap(payload);
                map = new HashMap<>(m2.size());
                metaTeams = new HashMap<>(m2.size());
                metaChars = new HashMap<>(m2.size());
                for (Map.Entry<Integer, SnapshotV2.Entry> e : m2.entrySet()) {
                    SnapshotV2.Entry v = e.getValue();
                    int id = e.getKey();
                    map.put(id, new SnapshotV1.Entry(v.id, v.x, v.y, v.aim));
                    metaTeams.put(id, (v.team & 1));
                    metaChars.put(id, Math.max(0, v.characterId));
                }
            } else {
                return;
            }
        } catch (Exception e) {
            return;
        }
        synchronized (this) {
            // 메타데이터 병합(v2 전용)
            if (metaTeams != null) teamById.putAll(metaTeams);
            if (metaChars != null) characterById.putAll(metaChars);

            long t = recvNs;
            if (!shots.isEmpty() && t <= shots.peekLast().t) {
                t = shots.peekLast().t + 1;
            }
            shots.addLast(new Shot(t, map));
            while (shots.size() > capacity) shots.removeFirst();
        }
    }

    /** 팀/캐릭터 메타데이터를 포함한 확장 샘플 엔트리 목록을 반환한다. */
    public static final class EntryEx {
        public final int id;
        public final float x, y, aim;
        public final int team;         // 0=RED,1=BLUE (default 0)
        public final int characterId;  // ordinal (default -1 if unknown)
        public EntryEx(int id, float x, float y, float aim, int team, int characterId) {
            this.id = id; this.x = x; this.y = y; this.aim = aim; this.team = team; this.characterId = characterId;
        }
    }

    /**
     * 가능하면 메타데이터(팀/캐릭터)를 포함해 샘플링한다.
     * v1만 있을 경우 기본값으로 대체한다.
     */
    public List<EntryEx> sampleEx(long nowNs) {
        List<Entry> base = sample(nowNs);
        List<EntryEx> out = new ArrayList<>(base.size());
        synchronized (this) {
            for (Entry e : base) {
                int team = teamById.getOrDefault(e.id, 0);
                int chr  = characterById.getOrDefault(e.id, -1);
                out.add(new EntryEx(e.id, e.x, e.y, e.aim, team, chr));
            }
        }
        return out;
    }

    /** 플레이어 id의 마지막 팀(0=RED/1=BLUE)을 조회한다. */
    public synchronized int getTeam(int id) { return teamById.getOrDefault(id, 0); }
    /** 플레이어 id의 마지막 캐릭터 ordinal을 조회(-1이면 미상). */
    public synchronized int getCharacterId(int id) { return characterById.getOrDefault(id, -1); }

    /**
     * 보간된 스냅샷 샘플 리스트를 생성한다.
     * @param nowNs System.nanoTime() 기준 현재 시각
     */
    public List<Entry> sample(long nowNs) {
        Shot a, b;
        long target = nowNs - interpDelayNs;

        synchronized (this) {
            if (shots.isEmpty()) return List.of();
            // target을 둘러싸는 a(이전), b(다음) 프레임 찾기
            a = null; b = null;
            for (Shot s : shots) {
                if (s.t <= target) a = s;
                if (s.t >= target) { b = s; break; }
            }
            if (a == null) {
                // 과거 프레임이 없는 경우: 가장 이른 프레임을 사용
                b = shots.peekFirst();
                a = b;
            } else if (b == null) {
                // 미래 프레임이 없는 경우: 가장 마지막 프레임을 사용
                a = shots.peekLast();
                b = a;
            }
        }

        // 보간 계수 t(0..1)
        float t;
        if (a == b || a.t == b.t) t = 0f;
        else t = (float) ((target - a.t) / (double) (b.t - a.t));
        t = Math.max(0f, Math.min(1f, t));

        // a, b에서 동일 id를 기준으로 보간
        Map<Integer, SnapshotV1.Entry> am = a.map;
        Map<Integer, SnapshotV1.Entry> bm = b.map;

        // id 집합 생성
        Set<Integer> ids = new HashSet<>(Math.max(am.size(), bm.size()));
        ids.addAll(am.keySet());
        ids.addAll(bm.keySet());

        List<Entry> out = new ArrayList<>(ids.size());
        for (int id : ids) {
            SnapshotV1.Entry ea = am.get(id);
            SnapshotV1.Entry eb = bm.get(id);
            if (ea == null) ea = eb;
            if (eb == null) eb = ea;
            if (ea == null) continue; // 둘 다 null이면 건너뜀

            float x = lerp(ea.x, eb.x, t);
            float y = lerp(ea.y, eb.y, t);
            // aim은 단순 선형 보간(범위 래핑이 필요하면 별도 처리)
            float aim = lerp(ea.aim, eb.aim, t);

            out.add(new Entry(id, x, y, aim));
        }
        return out;
    }

    // =================== 유틸리티 ===================

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }
}

