package com.fpsgame.client.ui;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * MapVoteUiInstaller
 * ------------------------------------------------------------
 * 프레임에 {@link MapVotePanel}을 장착/해제하고,
 * 표시/숨김 및 맵 후보 갱신, 전송 콜백 연결까지 맡는 경량 설치자.
 *
 * <p>사용 예</p>
 * <pre>
 *   MapVoteUiInstaller i =
 *       MapVoteUiInstaller.installTo(frame, netClient, BorderLayout.CENTER);
 *   i.updateMaps(List.of("terminal","neonCity","ForestOutpost"));
 *   i.show();   // 표시
 *   i.hide();   // 숨김
 *   i.uninstall(); // 제거
 * </pre>
 *
 * <p>전송 연결</p>
 * - 패널에서 투표 이벤트가 발생하면, 존재한다면
 *   {@code com.fpsgame.client.net.MapVoteSender#send(Object,String)}
 *   를 리플렉션으로 호출한다(없으면 조용히 무시).
 *
 * <p>EDT 안전성</p>
 * - 모든 UI 변경은 EDT에서 수행한다.
 */
public final class MapVoteUiInstaller {

    private final JFrame frame;
    private final Object netClient;
    private final String position;

    private MapVotePanel panel;
    private boolean installed;

    private MapVoteUiInstaller(JFrame frame, Object netClient, String position) {
        this.frame = Objects.requireNonNull(frame, "frame");
        this.netClient = netClient; // null 허용(전송 라인 미연결)
        this.position = (position == null ? BorderLayout.CENTER : position);
    }

    /** 프레임에 MapVotePanel을 장착하고 인스턴스를 반환한다. */
    public static MapVoteUiInstaller installTo(JFrame frame, Object netClient, String position) {
        MapVoteUiInstaller i = new MapVoteUiInstaller(frame, netClient, position);
        i.doInstall();
        return i;
    }

    // ======================= 외부 API =======================

    /** 표시 */
    public void show() {
        runEdt(() -> {
            if (panel != null) {
                panel.setVisible(true);
                frame.revalidate();
                frame.repaint();
            }
        });
    }

    /** 숨김 */
    public void hide() {
        runEdt(() -> {
            if (panel != null) {
                panel.setVisible(false);
                frame.revalidate();
                frame.repaint();
            }
        });
    }

    /** 맵 후보 목록 갱신(빈/NULL 허용) */
    public void updateMaps(List<String> mapIds) {
        runEdt(() -> {
            if (panel == null) return;
            try {
                panel.setMaps(mapIds);
            } catch (Throwable ignore) {
                // 패널 인터페이스가 다르면 조용히 무시
            }
        });
    }

    /** 해제(중복 호출 안전) */
    public void uninstall() {
        runEdt(() -> {
            if (!installed) return;
            try {
                if (panel != null) {
                    frame.getContentPane().remove(panel);
                    panel = null;
                }
                frame.revalidate();
                frame.repaint();
            } finally {
                installed = false;
            }
        });
    }

    // ======================= 내부 구현 =======================

    private void doInstall() {
        runEdt(() -> {
            Container content = frame.getContentPane();
            ensureBorderLayout(content);

            if (panel == null) {
                panel = new MapVotePanel();
                panel.setVisible(false); // 기본은 숨김
                wireSenders(panel);
            }

            content.add(panel, position);
            frame.revalidate();
            frame.repaint();
            installed = true;
        });
    }

    /** 패널의 전송 콜백을 netClient로 연결(리플렉션; 실패 시 조용히 무시) */
    private void wireSenders(MapVotePanel p) {
        if (p == null) return;
        Consumer<String> sender = mapId -> {
            if (netClient == null) return;
            try {
                Class<?> cls = Class.forName("com.fpsgame.client.net.MapVoteSender");
                Method m = cls.getMethod("send", Object.class, String.class);
                m.invoke(null, netClient, mapId);
            } catch (Throwable ignore) {
                // 전송 유틸이 없거나 시그니처가 다르면 무시(데모/테스트 환경 지원)
            }
        };
        try {
            p.onSendVote(sender);
        } catch (Throwable ignore) {
            // 패널 구현이 다르면 조용히 무시
        }
    }

    private static void ensureBorderLayout(Container content) {
        if (!(content.getLayout() instanceof BorderLayout)) {
            content.setLayout(new BorderLayout());
        }
    }

    private static void runEdt(Runnable r) {
        if (SwingUtilities.isEventDispatchThread()) r.run();
        else SwingUtilities.invokeLater(r);
    }
}
