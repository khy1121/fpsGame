package com.fpsgame.common;

import java.io.*;
import java.util.*;

/**
 * Snapshot binary v2 utilities.
 *
 * Layout (v=2):
 * byte  version (=2)
 * int   count
 * // repeated 'count' times
 * int   id
 * float x
 * float y
 * float aim
 * byte  team   (0=RED,1=BLUE)
 * byte  chrId  (CharacterId ordinal)
 */
public final class SnapshotV2 {

    /** One player entry (augmented). */
    public static final class Entry {
        public final int id;
        public final float x, y, aim;
        public final int team;
        public final int characterId;

        public Entry(int id, float x, float y, float aim, int team, int characterId) {
            this.id = id; this.x = x; this.y = y; this.aim = aim; this.team = team; this.characterId = characterId;
        }
    }

    private SnapshotV2() {}

    // Parsing
    public static List<Entry> parse(byte[] payload) throws IOException {
        if (payload == null || payload.length == 0) return List.of();
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(payload))) {
            int ver = in.readUnsignedByte();
            if (ver != 2) throw new IOException("unsupported snapshot version: " + ver);
            int count = in.readInt();
            if (count < 0 || count > 100_000) throw new IOException("invalid count: " + count);
            List<Entry> list = new ArrayList<>(Math.min(count, 1024));
            for (int i = 0; i < count; i++) {
                int id = in.readInt();
                float x = in.readFloat();
                float y = in.readFloat();
                float aim = in.readFloat();
                int team = in.readUnsignedByte();
                int chr = in.readUnsignedByte();
                list.add(new Entry(id, x, y, aim, team, chr));
            }
            return list;
        } catch (EOFException eof) {
            throw new IOException("truncated snapshot payload", eof);
        }
    }

    public static Map<Integer, Entry> parseToMap(byte[] payload) throws IOException {
        List<Entry> list = parse(payload);
        Map<Integer, Entry> map = new HashMap<>(Math.max(16, list.size() * 2));
        for (Entry e : list) map.put(e.id, e);
        return map;
    }

    // Build
    public static byte[] build(Collection<Entry> entries) throws IOException {
        if (entries == null) entries = List.of();
        ByteArrayOutputStream baos = new ByteArrayOutputStream(1 + 4 + entries.size() * (4 + 4 + 4 + 4 + 1 + 1));
        try (DataOutputStream out = new DataOutputStream(baos)) {
            out.writeByte(2);             // version
            out.writeInt(entries.size()); // count
            for (Entry e : entries) {
                out.writeInt(e.id);
                out.writeFloat(e.x);
                out.writeFloat(e.y);
                out.writeFloat(e.aim);
                out.writeByte(e.team & 0xFF);
                out.writeByte(e.characterId & 0xFF);
            }
        }
        return baos.toByteArray();
    }
}

