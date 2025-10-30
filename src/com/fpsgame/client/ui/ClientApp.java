package com.fpsgame.client.ui;

import javax.swing.*;

/**
 * 클라이언트 UI 런처.
 *
 * <p>
 * 프로젝트 내 여러 엔트리 파일이 혼재할 수 있어 충돌이 날 때,
 * 이 클래스를 표준 진입점으로 사용하면 안전합니다.
 * 시스템 L&F를 적용하고 {@link GameFrame}을 열어줍니다.
 * </p>
 */
public final class ClientApp {

    private ClientApp() {}

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            setSystemLookAndFeel();
            GameFrame frame = new GameFrame();
            frame.setVisible(true);
        });
    }

    /** 시스템 룩앤필 적용(실패 시 무시). */
    private static void setSystemLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignore) {
            // 일부 환경에서 지원하지 않을 수 있으므로 조용히 무시
        }
    }
}
