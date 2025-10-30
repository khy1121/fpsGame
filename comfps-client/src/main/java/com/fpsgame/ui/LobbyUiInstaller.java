package com.fpsgame.client.ui;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * LobbyUiInstaller
 * ------------------------------------------------------------
 * 프레임에 "READY 리스트 + READY 토글 버튼" 로비 UI를 장착/해제하는 경량 설치자.
 *
 * <p>정책</p>
 * - 우선 기존 프로젝트의 {@code com.fpsgame.client.ui.LobbyReadyPanel}이 있으면 이를 생성해서 사용한다.
 * - 해당 클래스가 없거나 시그니처가 다르면, 내부 Fallback 패널을 사용한다.
 * - 전송 라인은 다음 우선순위로 호출한다.
 *   1) netLike 객체의 {@code sendReadyToggle(boolean)}
 *   2) {@code com.fpsgame.client.net.ReadyToggleSender#send(Object, boolean)}
 *
 * <p>사용 예</p>
 * <pre>
 *   LobbyUiInstaller east = LobbyUiInstaller.installTo(frame, netClient, BorderLayout.EAST);
 *   // ...
 *   east.uninstall();
 * </pre>
 */
public final class LobbyUiInstaller {

    private final JFrame frame;
    private final Object netLike; // NetClient 또는 ClientController 등 송신 대상
    private final String position;

    private JComponent panel;     // LobbyReadyPanel 또는 Fallback
    private boolean installed;

    private LobbyUiInstaller(JFrame frame, Object netLike, String position) {
        this.frame = Objects.requireNonNull(frame, "frame");
        this.netLike = netLike; // null 허용: 송신 라인 없이 UI만
        this.position = (position == null ? BorderLayout.EAST : position);
    }

    /** 프레임에 로비 Ready UI를 장착하고 인스턴스를 반환한다. */
    public static LobbyUiInstaller installTo(JFrame frame, Object netLike, String position) {
        LobbyUiInstaller i = new LobbyUiInstaller(frame, netLike, position);
        i.doInstall();
        return i;
    }

    /** 해제(중복 호출 안전). */
    public void uninstall() {
        if (!installed) return;
        SwingUtilities.invokeLater(() -> {
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

    // ================ 내부 구현 ================

    private void doInstall() {
        SwingUtilities.invokeLater(() -> {
            Container content = frame.getContentPane();
            ensureBorderLayout(content);

            // 1) 우선 기존 LobbyReadyPanel을 시도
            panel = tryCreateReadyPanelViaReflection();
            if (panel == null) {
                // 2) 실패하면 Fallback 패널 사용
                panel = buildFallbackReadyPanel();
            }

            content.add(panel, position);
            frame.revalidate();
            frame.repaint();
            installed = true;
        });
    }

    /** 기존 구현(있다면) 사용: com.fpsgame.client.ui.LobbyReadyPanel */
    private JComponent tryCreateReadyPanelViaReflection() {
        try {
            Class<?> cls = Class.forName("com.fpsgame.client.ui.LobbyReadyPanel");
            Object instance = cls.getDeclaredConstructor().newInstance();
            if (!(instance instanceof JComponent)) return null;

            // 콜백 배선: onToggleReady(Consumer<Boolean>) 또는 setOnToggleReady(Consumer<Boolean>)
            Method setCb = null;
            try {
                setCb = cls.getMethod("onToggleReady", java.util.function.Consumer.class);
            } catch (NoSuchMethodException ignore) {
                try { setCb = cls.getMethod("setOnToggleReady", java.util.function.Consumer.class); }
                catch (NoSuchMethodException ignore2) {}
            }
            if (setCb != null) {
                java.util.function.Consumer<Boolean> sender = this::sendReadyToggleSafe;
                setCb.invoke(instance, sender);
            }

            return (JComponent) instance;
        } catch (Throwable ignore) {
            return null;
        }
    }

    /** 간단 Fallback: 플레이어 리스트 + READY 토글 버튼 */
    private JComponent buildFallbackReadyPanel() {
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JLabel title = new JLabel("Lobby Ready");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 14f));
        root.add(title, BorderLayout.NORTH);

        // 간단 리스트(데모용)
        DefaultListModel<String> model = new DefaultListModel<>();
        JList<String> list = new JList<>(model);
        list.setVisibleRowCount(10);
        list.setBorder(BorderFactory.createLineBorder(new Color(210,210,210)));
        root.add(new JScrollPane(list), BorderLayout.CENTER);

        // READY 토글
        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        JToggleButton readyBtn = new JToggleButton("I'm READY");
        south.add(readyBtn);
        root.add(south, BorderLayout.SOUTH);

        // 토글 → 전송
        AtomicBoolean last = new AtomicBoolean(false);
        readyBtn.addActionListener(e -> {
            boolean ready = readyBtn.isSelected();
            if (ready != last.getAndSet(ready)) {
                sendReadyToggleSafe(ready);
            }
        });

        return root;
        }

    /** 안전 송신: 컨트롤러 우선, 없으면 Sender 유틸 리플렉션 사용 */
    private void sendReadyToggleSafe(Boolean ready) {
        if (ready == null) return;
        // 1) netLike가 컨트롤러/넷 객체면서 메서드가 있으면 직접 호출
        if (netLike != null) {
            try {
                Method m = netLike.getClass().getMethod("sendReadyToggle", boolean.class);
                m.invoke(netLike, ready.booleanValue());
                return;
            } catch (Throwable ignore) { /* 다음 경로 시도 */ }
        }
        // 2) ReadyToggleSender.send(Object, boolean)
        try {
            Class<?> cls = Class.forName("com.fpsgame.client.net.ReadyToggleSender");
            Method m = cls.getMethod("send", Object.class, boolean.class);
            m.invoke(null, netLike, ready.booleanValue());
        } catch (Throwable ignore) {
            // 유틸이 없거나 시그니처가 다르면 실패를 조용히 무시
        }
    }

    private static void ensureBorderLayout(Container content) {
        if (!(content.getLayout() instanceof BorderLayout)) {
            content.setLayout(new BorderLayout());
        }
    }
}
