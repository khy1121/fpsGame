package com.fpsgame.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 서버 권위 플레이어 동기화(입력 수집 → 물리 업데이트 → 스냅샷 직렬화)를 담당하는 경량 서비스.
 *
 * <p>역할</p>
 * <ul>
 *   <li>{@link #addPlayer(int)} / {@link #removePlayer(int)} 로 세션별 플레이어 엔티티를 등록/해제</li>
 *   <li>{@link #onInputFrame(int, byte[])} 로 수신 INPUT 페이로드를 파싱하여 입력 상태를 반영</li>
 *   <li>{@link #tick(float)} 에서 고정 틱마다 위치/시야각을 갱신</li>
 *   <li>{@link #buildSnapshotFramePayload()} 로 {@link com.fpsgame.common.Protocol.Opcode#SNAPSHOT}
 *       페이로드를 생성</li>
 * </ul>
 *
 * <p>주의(포맷 유연성)</p>
 * 현재 클라이언트 입력 포맷이 고정되지 않았으므로, 서버는 다음의 <b>관대한</b> 파서를 사용한다.
 * <pre>
 *   case A) [byte mask][float aim?]
 *     - mask 비트: 0=Up(W), 1=Down(S), 2=Left(A), 3=Right(D)
 *     - aim(float)는 존재할 수도, 없을 수도 있음(없으면 기존 시야 유지)
 *
 *   case B) [float ax][float ay][float aim?]
 *     - ax,ay ∈ [-1..+1]의 입력 벡터, 정규화는 서버가 수행
 * </pre>
 * 정의되지 않은 레이아웃이 들어오면 입력을 무시한다(서버 안정성 우선).
 */
public final class PlayerSyncService {

    // ================= 내부 상태 =================

    /** 한 플레이어의 서버 권위 상태 */
    private static final class Player {
        // 위치/각도
        float x, y;
        float aim;

        // 직전 틱의 입력(서버가 수집/해석)
        float inAx, inAy; // -1..+1 방향 벡터(정규화됨)
        boolean dirty;    // 최근 입력 수신 플래그(디버그/추후 최적화용)

        Player(float x, float y) { this.x = x; this.y = y; }
    }

    /** 세션ID → 플레이어 */
    private final Map<Integer, Player> players = new ConcurrentHashMap<>();

    /** 이동 속도(초당 유닛) */
    private volatile float moveSpeed = 6.5f;

    /** 맵 경계(간단 보호용). 필요 시 외부에서 주입 가능. */
    private volatile float minX = -100f, maxX = 100f, minY = -100f, maxY = 100f;

    // ---- External character provider (sessionId -> Character) ----
    public interface CharacterProvider { com.fpsgame.common.character.Character get(int sessionId); }
    private volatile CharacterProvider characterProvider;
    public void setCharacterProvider(CharacterProvider provider) { this.characterProvider = provider; }

    /** Set a player's position explicitly (e.g., when selection/spawn changes). */
    public void setPosition(int sessionId, float x, float y) {
        Player p = players.get(sessionId);
        if (p != null) { p.x = x; p.y = y; p.dirty = true; }
    }

    // ================= 생명주기 =================

    /** 세션 오픈 시 플레이어 생성(스폰 위치는 간단히 원점 주변 랜덤) */
    public void addPlayer(int sessionId) {
        float px, py;
        CharacterProvider cp = this.characterProvider;
        if (cp != null) {
            try {
                var ch = cp.get(sessionId);
                if (ch != null && ch.getPosition() != null) {
                    px = ch.getPosition().x;
                    py = ch.getPosition().y;
                } else {
                    px = (float) (Math.random() * 4.0 - 2.0);
                    py = (float) (Math.random() * 4.0 - 2.0);
                }
            } catch (Throwable t) {
                px = (float) (Math.random() * 4.0 - 2.0);
                py = (float) (Math.random() * 4.0 - 2.0);
            }
        } else {
            px = (float) (Math.random() * 4.0 - 2.0);
            py = (float) (Math.random() * 4.0 - 2.0);
        }
        Player p = new Player(px, py);
        p.aim = 0f;
        players.put(sessionId, p);
    }

    /** 세션 종료 시 제거 */
    public void removePlayer(int sessionId) {
        players.remove(sessionId);
    }

    // ================= 입력 처리 =================

    /**
     * INPUT 페이로드를 파싱하여 플레이어 입력 상태로 반영한다.
     * 포맷은 상단 "주의(포맷 유연성)" 참고.
     */
    public void onInputFrame(int sessionId, byte[] payload) {
        Player p = players.get(sessionId);
        if (p == null || payload == null) return;

        try {
            if (payload.length == 1 || payload.length == 5) {
                // case A: [byte mask][float aim?]
                int mask = payload[0] & 0xFF;
                float ax = 0f, ay = 0f;
                if ((mask & 0x01) != 0) ay -= 1f; // Up
                if ((mask & 0x02) != 0) ay += 1f; // Down
                if ((mask & 0x04) != 0) ax -= 1f; // Left
                if ((mask & 0x08) != 0) ax += 1f; // Right
                normalize2(p, ax, ay);

                if (payload.length == 5) {
                    // float aim 읽기(빅엔디안). 자바는 big-endian이므로 직접 조립.
                    int b1 = (payload[1] & 0xFF);
                    int b2 = (payload[2] & 0xFF);
                    int b3 = (payload[3] & 0xFF);
                    int b4 = (payload[4] & 0xFF);
                    int bits = (b1 << 24) | (b2 << 16) | (b3 << 8) | b4;
                    p.aim = Float.intBitsToFloat(bits);
                }
                p.dirty = true;
                return;
            }

            if (payload.length == 8 || payload.length == 12) {
                // case B: [float ax][float ay][float aim?]
                float ax = toFloat(payload, 0);
                float ay = toFloat(payload, 4);
                normalize2(p, ax, ay);
                if (payload.length == 12) {
                    p.aim = toFloat(payload, 8);
                }
                p.dirty = true;
                return;
            }

            // 알 수 없는 레이아웃 → 무시
        } catch (Throwable ignored) {
            // 불량 페이로드 → 무시(서버 생존 우선)
        }
    }

    // ================= 틱 업데이트 =================

    /**
     * 고정 틱마다 호출하여 위치/각도를 갱신.
     * @param dt 델타 타임(초)
     */
    public void tick(float dt) {
        if (dt <= 0f) return;
        final float speed = Math.max(0f, moveSpeed);
        for (Map.Entry<Integer, Player> e : players.entrySet()) {
            Player p = e.getValue();
            p.x += p.inAx * speed * dt;
            p.y += p.inAy * speed * dt;
            // 간단 경계 보정
            if (p.x < minX) p.x = minX; else if (p.x > maxX) p.x = maxX;
            if (p.y < minY) p.y = minY; else if (p.y > maxY) p.y = maxY;
            // 캐릭터 객체로 반영(있다면)
            CharacterProvider cp = this.characterProvider;
            if (cp != null) {
                try {
                    var ch = cp.get(e.getKey());
                    if (ch != null) ch.setPosition(new com.fpsgame.common.Vec2(p.x, p.y));
                } catch (Throwable ignore) {}
            }
        }
    }

    // ================= 스냅샷 직렬화 =================

    /**
     * 현재 모든 플레이어의 스냅샷을 v2 포맷으로 직렬화하여 반환.
     * (클라이언트는 payload[0] 버전 바이트로 v1/v2를 구분해 파싱해야 함)
     */
    public byte[] buildSnapshotFramePayload() throws IOException {
        Collection<com.fpsgame.common.SnapshotV2.Entry> list = new ArrayList<>(players.size());
        CharacterProvider cp = this.characterProvider;
        for (Map.Entry<Integer, Player> e : players.entrySet()) {
            int id = e.getKey();
            Player p = e.getValue();
            int team = 0;
            int chr = 0;
            if (cp != null) {
                try {
                    var ch = cp.get(id);
                    if (ch != null) {
                        team = (ch.getTeam() == com.fpsgame.common.GameEnums.Team.BLUE) ? 1 : 0;
                        chr = ch.getId().ordinal();
                    }
                } catch (Throwable ignore) {}
            }
            list.add(new com.fpsgame.common.SnapshotV2.Entry(id, p.x, p.y, p.aim, team, chr));
            p.dirty = false;
        }
        return com.fpsgame.common.SnapshotV2.build(list);
    }

    // ================= 설정/유틸 =================

    /** 이동 속도(초당 유닛) 설정 */
    public void setMoveSpeed(float unitsPerSec) {
        this.moveSpeed = Math.max(0f, unitsPerSec);
    }

    /** 월드 경계 설정 */
    public void setBounds(float minX, float maxX, float minY, float maxY) {
        this.minX = Math.min(minX, maxX);
        this.maxX = Math.max(minX, maxX);
        this.minY = Math.min(minY, maxY);
        this.maxY = Math.max(minY, maxY);
    }

    /** 현재 세션의 조준 각도(라디안)를 반환. 없으면 0. */
    public float getAimAngle(int sessionId) {
        Player p = players.get(sessionId);
        return (p != null) ? p.aim : 0f;
    }

    // ---- 내부 보조 ----

    /** 8바이트 배열 오프셋에서 big-endian float 읽기 */
    private static float toFloat(byte[] a, int off) {
        int b1 = (a[off] & 0xFF);
        int b2 = (a[off + 1] & 0xFF);
        int b3 = (a[off + 2] & 0xFF);
        int b4 = (a[off + 3] & 0xFF);
        int bits = (b1 << 24) | (b2 << 16) | (b3 << 8) | b4;
        return Float.intBitsToFloat(bits);
    }

    /** 입력 벡터 정규화 → 플레이어에 반영 */
    private static void normalize2(Player p, float ax, float ay) {
        float len2 = ax * ax + ay * ay;
        if (len2 > 1e-6f) {
            float inv = (float) (1.0 / Math.sqrt(len2));
            p.inAx = ax * inv;
            p.inAy = ay * inv;
        } else {
            p.inAx = 0f;
            p.inAy = 0f;
        }
    }
}
