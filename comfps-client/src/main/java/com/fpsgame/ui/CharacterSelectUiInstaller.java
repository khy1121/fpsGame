package com.fpsgame.client.ui;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * CharacterSelectUiInstaller
 * ------------------------------------------------------------
 * Installs/uninstalls CharacterSelectPanel into a frame and provides simple
 * show/hide helpers and character list updates. Network sending is delegated
 * to the panel and/or a loose-coupled sender (via reflection).
 *
 * Usage:
 *   CharacterSelectUiInstaller i =
 *       CharacterSelectUiInstaller.installTo(frame, netClient, BorderLayout.CENTER);
 *   i.updateCharacters(List.of("Sage","Piper","Technician","General","Bulldog",
 *                              "Wildcat","Raven","Ghost","Skull","Steam"));
 *   i.show();
 *   i.hide();
 *   i.uninstall();
 */
public final class CharacterSelectUiInstaller {

    private final JFrame frame;
    private final Object netClient;
    private final String position;

    private CharacterSelectPanel panel;
    private boolean installed;

    private CharacterSelectUiInstaller(JFrame frame, Object netClient, String position) {
        this.frame = Objects.requireNonNull(frame, "frame");
        this.netClient = netClient; // null ????(???? ???? ???????
        this.position = (position == null ? BorderLayout.CENTER : position);
    }

    /** ???????? CharacterSelectPanel????????????????????????????. */
    public static CharacterSelectUiInstaller installTo(JFrame frame, Object netClient, String position) {
        CharacterSelectUiInstaller i = new CharacterSelectUiInstaller(frame, netClient, position);
        i.doInstall();
        return i;
    }

    // ======================= ??? API =======================

    /** ???? */
    public void show() {
        runEdt(() -> {
            if (panel != null) {
                panel.setVisible(true);
                frame.revalidate();
                frame.repaint();
            }
        });
    }

    /** ??? */
    public void hide() {
        runEdt(() -> {
            if (panel != null) {
                panel.setVisible(false);
                frame.revalidate();
                frame.repaint();
            }
        });
    }

    /** ??????????????????NULL ????) */
    public void updateCharacters(List<String> ids) {
        runEdt(() -> {
            if (panel == null) return;
            try {
                if (ids == null || ids.isEmpty()) {
                    panel.withDefaultCharacters();
                } else {
                    panel.setCharacters(ids);
                }
            } catch (Throwable ignore) {
                // ???? ??????????? ?????????????????
            }
        });
    }

    /** ????(????????? ????) */
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

    // ======================= ???? ???? =======================

    private void doInstall() {
        runEdt(() -> {
            Container content = frame.getContentPane();
            ensureBorderLayout(content);

            if (panel == null) {
                panel = new CharacterSelectPanel();
                panel.setVisible(false); // ?????? ???
                wireSenders(panel);
            }

            content.add(panel, position);
            frame.revalidate();
            frame.repaint();
            installed = true;
        });
    }

    /** ?????????? ??????netClient??????(????????? ???? ????????????? */
    private void wireSenders(CharacterSelectPanel p) {
        if (p == null) return;
        Consumer<String> sender = chId -> {
            if (netClient == null) return;
            // 1) ?????????/Net ????? sendSetSelection(int,int) ????
            try {
                Method m = netClient.getClass().getMethod("sendSetSelection", int.class, int.class);
                int team = 0; // ???????(?????UI???? ???? ???????????????????
                int chIdx = safeIndex(chId);
                m.invoke(netClient, team, chIdx);
                return;
            } catch (Throwable ignore) {
                // ???? ?????????
            }
            // 2) ???? ????? SetSelectionSender.send(Object, String)
            try {
                Class<?> cls = Class.forName("com.fpsgame.client.net.SetSelectionSender");
                Method m = cls.getMethod("send", Object.class, String.class);
                m.invoke(null, netClient, String.valueOf(chId));
            } catch (Throwable ignore) {
                // ???? ??????????????????????? ???????????????/?????????? ?????
            }
        };
        try {
            p.onSendSelection(sender);
        } catch (Throwable ignore) {
            // ???? ???????????????????????
        }
    }

    private static int safeIndex(String id) {
        if (id == null) return 0;
        try { return Integer.parseInt(id.trim()); }
        catch (Exception ignore) { return 0; }
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

