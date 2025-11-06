package com.fpsgame.client;

import com.fpsgame.client.ui.NetPlaygroundFrame;

import javax.swing.*;

/**
 * 네트워크 샌드박스 실행 엔트리.
 *
 * <p>이 클래스를 실행하면 {@link NetPlaygroundFrame} 이 열립니다.
 * 내부에는 간단한 월드 렌더/입력 전송/RTT 측정 UI가 포함되어 있어,
 * 서버 측 {@code PlayerSyncService + PlayerServerRouter + NetTickLoop} 조합을
 * 손쉽게 검증할 수 있습니다.</p>
 */
public final class NetPlaygroundMain {

    private NetPlaygroundMain() {}

    public static void main(String[] args) {
        // 시스템 Look&Feel 적용(가능한 경우)
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> {
            NetPlaygroundFrame f = new NetPlaygroundFrame();
            f.setVisible(true);
        });
    }
}
