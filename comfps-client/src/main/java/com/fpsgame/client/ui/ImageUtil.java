package com.fpsgame.client.ui;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

/** 간단 이미지 유틸리티(리소스/파일 로드, 스케일, 흰색 투명 처리). */
public final class ImageUtil {
    private ImageUtil() {}

    public static BufferedImage loadFile(String path) {
        if (path == null || path.isBlank()) return null;
        try { return ImageIO.read(new File(path)); } catch (IOException ignore) { return null; }
    }

    public static BufferedImage loadResource(Class<?> base, String resourcePath) {
        if (resourcePath == null || resourcePath.isBlank()) return null;
        // 1) base 클래스 기준으로 시도
        try (InputStream in = base.getResourceAsStream(resourcePath)) {
            if (in != null) {
                BufferedImage img = ImageIO.read(in);
                return img;
            }
        } catch (IOException ignore) {}

        // 2) 선행 슬래시 제거 후 재시도
        if (resourcePath.startsWith("/")) {
            String noSlash = resourcePath.substring(1);
            try (InputStream in = base.getResourceAsStream(noSlash)) {
                if (in != null) {
                    BufferedImage img = ImageIO.read(in);
                    return img;
                }
            } catch (IOException ignore) {}
        }

        // 3) ContextClassLoader 기준으로 시도
        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath.startsWith("/") ? resourcePath.substring(1) : resourcePath)) {
            if (in != null) {
                BufferedImage img = ImageIO.read(in);
                return img;
            }
        } catch (IOException ignore) {}

        System.err.println("Resource not found: " + resourcePath + " (base=" + base.getName() + ")");
        return null;
    }

    public static BufferedImage scale(BufferedImage src, int w, int h) {
        if (src == null || w <= 0 || h <= 0) return src;
        BufferedImage dst = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = dst.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(src, 0, 0, w, h, null);
        g.dispose();
        return dst;
    }

    /** 흰색(또는 거의 흰색)을 투명 처리하는 간단한 마스크. tolerance=예: 15 */
    public static BufferedImage whiteToTransparent(BufferedImage src, int tolerance) {
        if (src == null) return null;
        int w = src.getWidth(), h = src.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        int thr = Math.max(0, Math.min(255, tolerance));
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = src.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = (rgb) & 0xFF;
                int a = 0xFF;
                if (r >= 255 - thr && g >= 255 - thr && b >= 255 - thr) a = 0; // 거의 흰색 → 투명
                int argb = (a << 24) | (r << 16) | (g << 8) | b;
                out.setRGB(x, y, argb);
            }
        }
        return out;
    }
}

