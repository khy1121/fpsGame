package com.fpsgame.server;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.fpsgame.common.GameEnums;

/**
 * 맵 투표 집계기
 * - 플레이어 세션ID → 선택한 맵 기록
 * - 다수결 승자 계산(동률이면 기본 맵 폴백)
 * - ConcurrentHashMap 기반 동시성 안전
 */
public final class MapVoteManager {

    /** 플레이어ID → 투표 맵 */
    private final ConcurrentHashMap<Integer, GameEnums.MapId> votes = new ConcurrentHashMap<>();

    /** 모든 투표 제거(새 라운드 시작 등) */
    public void reset() { votes.clear(); }

    /** 투표 등록/변경(같은 플레이어가 다시 투표하면 교체) */
    public void vote(int playerSessionId, GameEnums.MapId map) {
        if (playerSessionId <= 0 || map == null) return;
        votes.put(playerSessionId, map);
    }

    /** 투표 철회(플레이어 퇴장 등) */
    public void revoke(int playerSessionId) { if (playerSessionId > 0) votes.remove(playerSessionId); }

    /** 현재 투표 인원 수 */
    public int voterCount() { return votes.size(); }

    /** 현재 집계 스냅샷(맵별 득표수) */
    public EnumMap<GameEnums.MapId, Integer> countsSnapshot() {
        EnumMap<GameEnums.MapId, Integer> out = new EnumMap<>(GameEnums.MapId.class);
        for (GameEnums.MapId m : GameEnums.MapId.values()) out.put(m, 0);
        for (GameEnums.MapId m : votes.values()) out.put(m, out.get(m) + 1);
        return out;
    }

    /** 승자 계산(다수결). 동률이면 "투표된 맵들 중에서 랜덤" 선택, 아무도 없으면 기본 맵 */
    public GameEnums.MapId winnerOrDefault() {
        EnumMap<GameEnums.MapId, Integer> c = countsSnapshot();
        int max = 0;
        for (Integer v : c.values()) max = Math.max(max, v == null ? 0 : v);

        // 투표가 전혀 없으면 기본 맵
        if (max <= 0) return GameEnums.MapId.defaultMap();

        // 최다 득표 맵들을 수집 후 랜덤 선택
        java.util.ArrayList<GameEnums.MapId> top = new java.util.ArrayList<>();
        Integer maxI = Integer.valueOf(max);
        for (Map.Entry<GameEnums.MapId, Integer> e : c.entrySet()) {
            Integer vv = e.getValue();
            if (vv != null && vv.equals(maxI)) top.add(e.getKey());
        }
        if (top.isEmpty()) return GameEnums.MapId.defaultMap();

        int idx = java.util.concurrent.ThreadLocalRandom.current().nextInt(top.size());
        return top.get(idx);
    }
}

