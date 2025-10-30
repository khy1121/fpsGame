package com.fpsgame.common;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * 투사체 목록 전송용 경량 포맷 v2.
 *
 * 포맷:
 *   byte  version (=2)
 *   int   count
 *   // 반복(count):
 *   int   id
 *   byte  type     (0=unknown, 1=bullet, reserve others)
 *   byte  active   (0/1)
 *   float x
 *   float y
 */
public final class ProjectilesV2 {

    // Simple type constants to avoid magic numbers in callers
    public static final int TYPE_UNKNOWN = 0;
    public static final int TYPE_BULLET  = 1;

    public static final class Entry {
        public final int id; public final int type; public final boolean active; public final float x, y;
        public Entry(int id, int type, boolean active, float x, float y) { this.id = id; this.type = type; this.active = active; this.x = x; this.y = y; }
    }

    private ProjectilesV2() {}

    public static byte[] build(Collection<Entry> entries) throws IOException {
        if (entries == null) entries = Collections.emptyList();
        ByteArrayOutputStream baos = new ByteArrayOutputStream(1 + 4 + entries.size() * (4 + 1 + 1 + 4 + 4));
        try (DataOutputStream out = new DataOutputStream(baos)) {
            out.writeByte(2);
            out.writeInt(entries.size());
            for (Entry e : entries) {
                out.writeInt(e.id);
                out.writeByte(e.type & 0xFF);
                out.writeByte(e.active ? 1 : 0);
                out.writeFloat(e.x);
                out.writeFloat(e.y);
            }
        }
        return baos.toByteArray();
    }

    public static List<Entry> parse(byte[] payload) throws IOException {
        if (payload == null || payload.length == 0) return Collections.emptyList();
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(payload))) {
            int ver = in.readUnsignedByte();
            if (ver != 2) throw new IOException("unsupported projectiles version: " + ver);
            int count = in.readInt();
            if (count < 0 || count > 100_000) throw new IOException("invalid count: " + count);
            List<Entry> list = new ArrayList<>(Math.min(count, 1024));
            for (int i = 0; i < count; i++) {
                int id = in.readInt();
                int type = in.readUnsignedByte();
                boolean active = in.readUnsignedByte() != 0;
                float x = in.readFloat();
                float y = in.readFloat();
                list.add(new Entry(id, type, active, x, y));
            }
            return list;
        } catch (EOFException eof) {
            throw new IOException("truncated projectiles payload", eof);
        }
    }

    // Helper: map runtime projectile instance to wire type id
    public static int typeOf(com.fpsgame.common.character.projectile.Projectile p) {
        if (p instanceof com.fpsgame.common.character.projectile.Bullet) return TYPE_BULLET;
        return TYPE_UNKNOWN;
    }
}
