package com.fpsgame.common;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * 네트워크 바이너리 직렬화/역직렬화 경량 유틸리티
 * - 고정 길이 값 put/get(Big-endian)
 * - 길이-서두(ushort/int) UTF-8 문자열 put/get
 * - 길이-서두 바이너리 배열 put/get
 * 주의: 네트워크 바이트 오더(Big-endian) 고정, 문자열 길이 검증 필요
 */
public final class Binary {

    private static final Charset UTF8 = StandardCharsets.UTF_8;
    private Binary() { }

    // ============ 기본 값 put/get ============
    public static void putByte(OutputStream out, int v) throws IOException { out.write(v & 0xFF); }
    public static void putShort(OutputStream out, int v) throws IOException { out.write((v>>>8)&0xFF); out.write(v&0xFF); }
    public static void putUShort(OutputStream out, int v) throws IOException { putShort(out, v & 0xFFFF); }
    public static void putInt(OutputStream out, int v) throws IOException { out.write((v>>>24)&0xFF); out.write((v>>>16)&0xFF); out.write((v>>>8)&0xFF); out.write(v&0xFF); }
    public static void putLong(OutputStream out, long v) throws IOException {
        out.write((int)((v>>>56)&0xFF)); out.write((int)((v>>>48)&0xFF)); out.write((int)((v>>>40)&0xFF)); out.write((int)((v>>>32)&0xFF));
        out.write((int)((v>>>24)&0xFF)); out.write((int)((v>>>16)&0xFF)); out.write((int)((v>>>8)&0xFF)); out.write((int)(v&0xFF));
    }
    public static void putFloat(OutputStream out, float f) throws IOException { putInt(out, Float.floatToIntBits(f)); }
    public static void putDouble(OutputStream out, double d) throws IOException { putLong(out, Double.doubleToLongBits(d)); }

    public static int getUByte(DataInput in) throws IOException { return in.readUnsignedByte(); }
    public static short getShort(DataInput in) throws IOException { return in.readShort(); }
    public static int getUShort(DataInput in) throws IOException { return in.readUnsignedShort(); }
    public static int getInt(DataInput in) throws IOException { return in.readInt(); }
    public static long getLong(DataInput in) throws IOException { return in.readLong(); }
    public static float getFloat(DataInput in) throws IOException { return in.readFloat(); }
    public static double getDouble(DataInput in) throws IOException { return in.readDouble(); }

    // ============ 문자열/바이너리 ============
    /** 길이-서두 UTF-8 문자열(ushort 길이) 작성. null은 빈 문자열 */
    public static void putUtf16Len(OutputStream out, String s) throws IOException {
        byte[] bytes = s == null ? new byte[0] : s.getBytes(UTF8);
        if (bytes.length > 0xFFFF) throw new IOException("string too long: " + bytes.length);
        putUShort(out, bytes.length); out.write(bytes);
    }
    /** 길이-서두 UTF-8 문자열(ushort 길이) 읽기 */
    public static String readUtf16Len(DataInputStream in) throws IOException {
        int len = in.readUnsignedShort(); if (len == 0) return "";
        byte[] buf = in.readNBytes(len); if (buf.length != len) throw new EOFException("short read");
        return new String(buf, UTF8);
    }
    /** 길이-서두 UTF-8 문자열(int 길이) 작성 */
    public static void putUtf32Len(OutputStream out, String s) throws IOException {
        byte[] bytes = s == null ? new byte[0] : s.getBytes(UTF8);
        putInt(out, bytes.length); out.write(bytes);
    }
    /** 길이-서두 UTF-8 문자열(int 길이) 읽기 */
    public static String readUtf32Len(DataInputStream in) throws IOException {
        int len = in.readInt(); if (len < 0) throw new IOException("negative length"); if (len == 0) return "";
        byte[] buf = in.readNBytes(len); if (buf.length != len) throw new EOFException("short read");
        return new String(buf, UTF8);
    }
    /** 길이-서두 바이너리 배열(int 길이) 작성. null이면 길이 0 */
    public static void putBytes(OutputStream out, byte[] bytes) throws IOException { if (bytes == null) { putInt(out, 0); return; } putInt(out, bytes.length); out.write(bytes); }
    /** 길이-서두 바이너리 배열(int 길이) 읽기 */
    public static byte[] readBytes(DataInputStream in) throws IOException {
        int len = in.readInt(); if (len < 0) throw new IOException("negative length"); if (len == 0) return new byte[0];
        byte[] buf = in.readNBytes(len); if (buf.length != len) throw new EOFException("short read");
        return buf;
    }

    // ============ 부가: ByteBuffer 기반 ============
    public static long getLong(byte[] arr, int off) { return ByteBuffer.wrap(arr, off, 8).getLong(); }
    public static int getInt(byte[] arr, int off) { return ByteBuffer.wrap(arr, off, 4).getInt(); }
    public static float getFloat(byte[] arr, int off) { return ByteBuffer.wrap(arr, off, 4).getFloat(); }
}

