package com.fpsgame.client;

import com.fpsgame.client.ui.NetDemoFrame;

import javax.swing.*;

/**
 * 네트워크 동기화 데모 런처.
 *
 * <p>역할</p>
 * <ul>
 *   <li>{@link NetDemoFrame} 를 실행하여 서버와의 연결/해제를 테스트</li>
 *   <li>다른 프로젝트 파일과의 의존성을 최소화하여 컴파일 오류 가능성을 줄임</li>
 * </ul>
 */
public final class ClientDemoMain {

    private ClientDemoMain() {}

    public static void main(String[] args) {
        // EDT에서 안전하게 UI 실행
        SwingUtilities.invokeLater(() -> {
            try {
                // 시스템 룩앤필 적용(실패해도 무시)
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}

            new NetDemoFrame().setVisible(true);
        });
    }
}
