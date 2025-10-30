package com.fpsgame.common;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.*;
import java.util.Objects;
import java.util.Properties;

/**
 * 간단한 .properties 파일 I/O 유틸리티.
 *
 * <p>특징</p>
 * <ul>
 *   <li>사용자 홈 기준 경로 생성 헬퍼({@link #userHome(String)})</li>
 *   <li>존재하지 않는 경우에도 안전하게 로드(기본값 유지)</li>
 *   <li>부모 디렉터리를 자동 생성 후 저장</li>
 *   <li>형 변환 헬퍼(getDouble 등) 제공</li>
 * </ul>
 */
public final class PropsFile {

    private PropsFile() { /* no instance */ }

    // =====================================================================================
    // 경로 유틸
    // =====================================================================================

    /**
     * 사용자 홈 디렉터리 하위에 상대 경로를 붙인 {@link Path} 를 반환한다.
     * <p>예) {@code userHome(".fpsgame/settings.properties")}</p>
     */
    public static Path userHome(String relative) {
        String home = System.getProperty("user.home", ".");
        if (relative == null || relative.isBlank()) return Paths.get(home);
        return Paths.get(home).resolve(relative.replace('\\', '/'));
    }

    // =====================================================================================
    // 로드/세이브
    // =====================================================================================

    /**
     * 지정 경로의 properties 파일을 로드한다.
     * 파일이 없거나 읽기 실패 시 빈 Properties 를 반환한다.
     */
    public static Properties load(Path path) {
        Objects.requireNonNull(path, "path");
        Properties p = new Properties();
        if (!Files.exists(path)) return p;

        try (InputStream in = Files.newInputStream(path)) {
            p.load(in);
        } catch (IOException ignored) {
            // 손상/권한 문제 등은 빈 프로퍼티로 대체
        }
        return p;
    }

    /**
     * 지정 경로로 properties 를 저장한다. 부모 폴더가 없으면 생성한다.
     * @param path     저장 경로
     * @param props    저장할 Properties(필수)
     * @param comments 주석(파일 헤더에 기록), null 허용
     */
    public static void save(Path path, Properties props, String comments) throws IOException {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(props, "props");

        Path parent = path.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }
        try (OutputStream out = Files.newOutputStream(path,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
            props.store(out, comments == null ? "" : comments);
        }
    }

    // =====================================================================================
    // 형 변환 헬퍼
    // =====================================================================================

    /** double 값을 파싱(실패 시 기본값). */
    public static double getDouble(Properties p, String key, double def) {
        String s = p.getProperty(key);
        if (s == null) return def;
        try { return Double.parseDouble(s.trim()); }
        catch (NumberFormatException ignored) { return def; }
    }

    /** int 값을 파싱(실패 시 기본값). */
    public static int getInt(Properties p, String key, int def) {
        String s = p.getProperty(key);
        if (s == null) return def;
        try { return Integer.parseInt(s.trim()); }
        catch (NumberFormatException ignored) { return def; }
    }

    /** boolean 값을 파싱(실패 시 기본값). */
    public static boolean getBoolean(Properties p, String key, boolean def) {
        String s = p.getProperty(key);
        if (s == null) return def;
        s = s.trim().toLowerCase();
        if ("true".equals(s) || "1".equals(s) || "yes".equals(s) || "y".equals(s)) return true;
        if ("false".equals(s) || "0".equals(s) || "no".equals(s) || "n".equals(s)) return false;
        return def;
    }
}
