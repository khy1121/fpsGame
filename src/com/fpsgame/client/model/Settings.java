package com.fpsgame.client.model;


import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Properties;

/**
 * FPS 게임의 지속적인 클라이언트 설정.
 * <p>
 * - 마우스 감도, Y축 반전, 마스터 볼륨, 네트워크 통계 표시, 프레임 제한
 * - 키 바인딩 ({@link Keybinds}를 통해 내장)
 * - 표준 Java Properties 파일로 저장/로드
 *
 * 스레드 안전성: 개별 setter/getter는 단순함; 스레드 간 공유 시 외부에서 동기화 필요.
 */
public final class Settings {

    private float mouseSensitivity = 1.0f;  // 0.1 ~ 2.0
    private boolean invertY = false;
    private int masterVolume = 70;          // 0 ~ 100
    private boolean showNetStats = false;   // 핑/패킷 손실 HUD
    private int frameLimit = 60;            // 0=무제한, 이외 30/60/120...

    private final Keybinds keybinds = Keybinds.defaults();

    /** 기본값이 적용된 새로운 Settings를 반환. */
    public static Settings defaults() {
        return new Settings();
    }

    // --- getters/setters ---

    public float getMouseSensitivity() { return mouseSensitivity; }

    public void setMouseSensitivity(float mouseSensitivity) {
        this.mouseSensitivity = clamp(mouseSensitivity, 0.1f, 2.0f);
    }

    public boolean isInvertY() { return invertY; }

    public void setInvertY(boolean invertY) { this.invertY = invertY; }

    public int getMasterVolume() { return masterVolume; }

    public void setMasterVolume(int masterVolume) { this.masterVolume = clamp(masterVolume, 0, 100); }

    public boolean isShowNetStats() { return showNetStats; }

    public void setShowNetStats(boolean showNetStats) { this.showNetStats = showNetStats; }

    public int getFrameLimit() { return frameLimit; }

    public void setFrameLimit(int frameLimit) {
        if (frameLimit < 0) frameLimit = 0;
        this.frameLimit = frameLimit;
    }

    public Keybinds getKeybinds() { return keybinds; }

    // --- persistence ---

    /** Loads settings from a .properties file (missing file is OK; defaults are kept). */
    public void load(Path file) throws IOException {
        Objects.requireNonNull(file, "file");
        if (!Files.exists(file)) return;
        try (InputStream in = Files.newInputStream(file)) {
            load(in);
        }
    }

    /** Loads settings from an InputStream (java.util.Properties format). */
    public void load(InputStream in) throws IOException {
        Properties p = new Properties();
        p.load(Objects.requireNonNull(in, "in"));

        this.mouseSensitivity = clamp(parseFloat(p.getProperty("mouseSensitivity"), mouseSensitivity), 0.1f, 2.0f);
        this.invertY = parseBoolean(p.getProperty("invertY"), invertY);
        this.masterVolume = clamp(parseInt(p.getProperty("masterVolume"), masterVolume), 0, 100);
        this.showNetStats = parseBoolean(p.getProperty("showNetStats"), showNetStats);
        this.frameLimit = Math.max(0, parseInt(p.getProperty("frameLimit"), frameLimit));

        // Keybinds: everything under "kb."
        Properties kb = new Properties();
        for (String name : p.stringPropertyNames()) {
            if (name.startsWith("kb.")) {
                kb.setProperty(name.substring(3), p.getProperty(name));
            }
        }
        keybinds.load(kb);
    }

    /** Saves settings to a .properties file (creates/overwrites). */
    public void save(Path file, String comment) throws IOException {
        Objects.requireNonNull(file, "file");
        Files.createDirectories(file.getParent());
        try (OutputStream out = Files.newOutputStream(file)) {
            save(out, comment);
        }
    }

    /** Saves settings to an OutputStream as java.util.Properties. */
    public void save(OutputStream out, String comment) throws IOException {
        Properties p = new Properties();
        p.setProperty("mouseSensitivity", Float.toString(mouseSensitivity));
        p.setProperty("invertY", Boolean.toString(invertY));
        p.setProperty("masterVolume", Integer.toString(masterVolume));
        p.setProperty("showNetStats", Boolean.toString(showNetStats));
        p.setProperty("frameLimit", Integer.toString(frameLimit));

        // Keybinds under "kb."
        Properties kb = keybinds.toProperties();
        for (String name : kb.stringPropertyNames()) {
            p.setProperty("kb." + name, kb.getProperty(name));
        }

        p.store(new OutputStreamWriter(out, StandardCharsets.UTF_8), comment);
    }

    // --- small helpers ---

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }

    private static int parseInt(String s, int def) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException | NullPointerException e) {
            return def;
        }
    }

    private static float parseFloat(String s, float def) {
        try {
            return Float.parseFloat(s.trim());
        } catch (NumberFormatException | NullPointerException e) {
            return def;
        }
    }

    private static boolean parseBoolean(String s, boolean def) {
        if (s == null) return def;
        String t = s.trim().toLowerCase();
        if (t.equals("true") || t.equals("yes") || t.equals("1")) return true;
        if (t.equals("false") || t.equals("no") || t.equals("0")) return false;
        return def;
    }

    // --- simple demo ---

    public static void main(String[] args) {
        try {
            Settings s = Settings.defaults();
            s.setMouseSensitivity(1.25f);
            s.setInvertY(true);
            s.setMasterVolume(80);
            s.setShowNetStats(true);
            s.setFrameLimit(120);

            Path tmp = Files.createTempFile("fps-settings", ".properties");
            s.save(tmp, "FPS Client Settings");
            System.out.println("Saved settings to: " + tmp);

            Settings loaded = Settings.defaults();
            loaded.load(tmp);
            System.out.println("Loaded sens=" + loaded.getMouseSensitivity()
                    + ", invertY=" + loaded.isInvertY()
                    + ", vol=" + loaded.getMasterVolume()
                    + ", net=" + loaded.isShowNetStats()
                    + ", cap=" + loaded.getFrameLimit());

            // Cleanup
            Files.deleteIfExists(tmp);
        } catch (IOException e) {
            System.err.println("Failed to run settings test: " + e.getMessage());
        }
    }
}
