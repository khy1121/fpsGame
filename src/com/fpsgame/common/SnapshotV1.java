package com.fpsgame.common;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.*;

/**
 * 서버 스냅샷 바이너리 v1의 파서/빌더 유틸리티.
 *
 * <p>레이아웃(v=1)</p>
 * <pre>
 * byte  version (=1)
 * int   count
 * // 반복(count):
 * int   id
 * float x
 * float y
 * float aim
 * </pre>
 *
 * <p>용도</p>
 * <ul>
 *   <li>클라이언트: 수신한 SNAPSHOT 페이로드를 구조화된 객체로 파싱</li>
 *   <li>서버: 임시로 리스트를 받아 페이로드 생성(테스트/도구용)</li>
 * </ul>
 *
 * <p>주의</p>
 * <ul>
 *   <li>파서는 방어적으로 작성되어, 잘못된 페이로드는 {@link IOException} 으로 표시</li>
 *   <li>이 클래스는 순수 데이터 직렬화만 담당(보간/예측/머지 로직은 별도)</li>
 * </ul>
 */
public final class SnapshotV1 {

    /** 단일 플레이어 상태 DTO(경량) */
    public static final class Entry {
        public final int id;
        public final float x, y, aim;

        public Entry(int id, float x, float y, float aim) {
            this.id = id;
            this.x = x;
            this.y = y;
            this.aim = aim;
        }

        @Override public String toString() {
            return "Entry{id=" + id + ", x=" + x + ", y=" + y + ", aim=" + aim + '}';
        }
    }

    private SnapshotV1() { /* no instance */ }

    // =====================================================================================
    // 파싱
    // =====================================================================================

    /**
     * 페이로드를 파싱하여 플레이어 엔트리 목록을 반환한다.
     * @throws IOException 포맷 불일치/바이트 부족 등
     */
    public static List<Entry> parse(byte[] payload) throws IOException {
        if (payload == null || payload.length == 0) {
            return Collections.emptyList();
        }
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(payload))) {
            int ver = in.readUnsignedByte();
            if (ver != 1) throw new IOException("unsupported snapshot version: " + ver);
            int count = in.readInt();
            if (count < 0 || count > 100_000) { // 비정상 보호
                throw new IOException("invalid count: " + count);
            }
            List<Entry> list = new ArrayList<>(Math.min(count, 1024));
            for (int i = 0; i < count; i++) {
                int id = in.readInt();
                float x = in.readFloat();
                float y = in.readFloat();
                float aim = in.readFloat();
                list.add(new Entry(id, x, y, aim));
            }
            // 잔여 바이트가 있어도 무시(확장 필드 대비)
            return list;
        } catch (EOFException eof) {
            throw new IOException("truncated snapshot payload", eof);
        }
    }

    /**
     * 파싱 결과를 ID→엔트리 맵으로 반환(조회 편의용).
     * 충돌 시 마지막 항목이 유지된다.
     */
    public static Map<Integer, Entry> parseToMap(byte[] payload) throws IOException {
        List<Entry> list = parse(payload);
        Map<Integer, Entry> map = new HashMap<>(Math.max(16, list.size() * 2));
        for (Entry e : list) map.put(e.id, e);
        return map;
    }

    // =====================================================================================
    // 빌드(도구/테스트용)
    // =====================================================================================

    /**
     * 엔트리 목록을 스냅샷 v1 페이로드로 직렬화한다.
     * <p>서버의 실제 구현은 {@code PlayerSyncService} 가 담당하지만,
     * 클라이언트/테스트 도구에서 페이로드를 손쉽게 생성할 수 있도록 제공한다.</p>
     */
    public static byte[] build(Collection<Entry> entries) throws IOException {
        if (entries == null) entries = Collections.emptyList();
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream(1 + 4 + entries.size() * 16);
        try (java.io.DataOutputStream out = new java.io.DataOutputStream(baos)) {
            out.writeByte(1);              // version
            out.writeInt(entries.size());  // count
            for (Entry e : entries) {
                out.writeInt(e.id);
                out.writeFloat(e.x);
                out.writeFloat(e.y);
                out.writeFloat(e.aim);
            }
        }
        return baos.toByteArray();
    }

    // =====================================================================================
    // 간단 테스트
    // =====================================================================================

    public static void main(String[] args) throws Exception {
        List<Entry> src = List.of(new Entry(1, 1.0f, 2.0f, 0.3f),
                                  new Entry(2, -3.5f, 0.1f, -1.2f));
        byte[] bin = build(src);
        List<Entry> dst = parse(bin);
        System.out.println("parsed: " + dst);
    }
}
