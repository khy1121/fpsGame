package com.fpsgame.client.model;

import com.fpsgame.common.PropsFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Properties;

/**
 * 게임 클라이언트의 사용자 설정(오디오/마우스/FOV/최근 서버)을
 * Properties 파일로 저장/로드하는 경량 스토어.
 *
 * <p>특징</p>
 * <ul>
 *   <li>기본값을 내장하여 파일이 없어도 안전하게 동작</li>
 *   <li>{@link #load()} / {@link #save()} 제공</li>
 *   <li>설정 항목은 단순한 필드 + getter/setter 로 구성</li>
 *   <li>저장 경로: 사용자 홈의 {@code ~/.fpsgame/settings.properties}</li>
 * </ul>
 */
public final class SettingsStore {

    // ===== 파일 경로 =====
    private static final Path FILE = PropsFile.userHome(".fpsgame/settings.properties");

    // ===== 프로퍼티 키 =====
    private static final String K_MASTER = "audio.master";
    private static final String K_SFX    = "audio.sfx";
    private static final String K_SENS   = "mouse.sens";
    private static final String K_FOV    = "video.fov";
    private static final String K_HOST   = "net.lastHost";
    private static final String K_PORT   = "net.lastPort";

    // ===== 기본값 =====
    private static final double DEF_MASTER = 0.80; // 0..1
    private static final double DEF_SFX    = 0.90; // 0..1
    private static final double DEF_SENS   = 1.00; // 0.1..10.0
    private static final int    DEF_FOV    = 90;   // 60..120
    private static final String DEF_HOST   = "127.0.0.1";
    private static final int    DEF_PORT   = 7777;

    // ===== 상태(메모리) =====
    private double masterVolume = DEF_MASTER;
    private double sfxVolume    = DEF_SFX;
    private double mouseSensitivity = DEF_SENS;
    private int fov = DEF_FOV;
    private String lastHost = DEF_HOST;
    private int lastPort = DEF_PORT;

    /** 설정 파일 경로를 반환(디버그/표시용). */
    public Path getFile() { return FILE; }

    // ===================== 로드/세이브 =====================

    /** 디스크에서 로드(파일이 없거나 오류여도 기본값 유지). */
    public void load() {
        Properties p = PropsFile.load(FILE);
        masterVolume = clamp01(PropsFile.getDouble(p, K_MASTER, DEF_MASTER));
        sfxVolume    = clamp01(PropsFile.getDouble(p, K_SFX,    DEF_SFX));
        mouseSensitivity = clamp(PropsFile.getDouble(p, K_SENS, DEF_SENS), 0.1, 10.0);
        fov = clampInt(parseInt(p.getProperty(K_FOV), DEF_FOV), 60, 120);
        lastHost = defaultIfBlank(p.getProperty(K_HOST), DEF_HOST);
        lastPort = clampInt(parseInt(p.getProperty(K_PORT), DEF_PORT), 1, 65535);
    }

    /** 디스크에 저장(부모 디렉터리를 자동 생성). */
    public void save() throws IOException {
        Properties p = new Properties();
        p.setProperty(K_MASTER, String.valueOf(masterVolume));
        p.setProperty(K_SFX,    String.valueOf(sfxVolume));
        p.setProperty(K_SENS,   String.valueOf(mouseSensitivity));
        p.setProperty(K_FOV,    String.valueOf(fov));
        p.setProperty(K_HOST,   Objects.toString(lastHost, DEF_HOST));
        p.setProperty(K_PORT,   String.valueOf(lastPort));
        PropsFile.save(FILE, p, "FPS Game Settings");
    }

    // ===================== 게터/세터 =====================

    public double getMasterVolume() { return masterVolume; }
    public void setMasterVolume(double v) { masterVolume = clamp01(v); }

    public double getSfxVolume() { return sfxVolume; }
    public void setSfxVolume(double v) { sfxVolume = clamp01(v); }

    public double getMouseSensitivity() { return mouseSensitivity; }
    public void setMouseSensitivity(double v) { mouseSensitivity = clamp(v, 0.1, 10.0); }

    public int getFov() { return fov; }
    public void setFov(int v) { fov = clampInt(v, 60, 120); }

    public String getLastHost() { return lastHost; }
    public void setLastHost(String host) { lastHost = defaultIfBlank(host, DEF_HOST); }

    public int getLastPort() { return lastPort; }
    public void setLastPort(int port) { lastPort = clampInt(port, 1, 65535); }

    // ===================== 유틸 =====================

    private static double clamp01(double v) {
        return v < 0.0 ? 0.0 : (v > 1.0 ? 1.0 : v);
    }

    private static double clamp(double v, double min, double max) {
        return v < min ? min : (v > max ? max : v);
    }

    private static int clampInt(int v, int min, int max) {
        return v < min ? min : (v > max ? max : v);
    }

    private static int parseInt(String s, int def) {
        if (s == null) return def;
        try { return Integer.parseInt(s.trim()); } catch (NumberFormatException ignored) { return def; }
    }

    private static String defaultIfBlank(String s, String def) {
        return (s == null || s.isBlank()) ? def : s.trim();
    }
}
