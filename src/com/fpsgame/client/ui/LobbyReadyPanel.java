package com.fpsgame.client.ui;

import com.fpsgame.client.model.ClientPhaseBus;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * LobbyReadyPanel
 * ------------------------------------------------------------
 * Description: READY toggle + READY state management.
 *
 * Features:
 * - ClientPhaseBus READY_TOGGLE handling.
 * - "Ready" toggle button (integrates with network client)
 *   Interface: sendReady(bool), ready(bool),
 *   send(opcode, payload).
 * - Customizable callback (onSendReady).
 *
 * Usage:
 *   LobbyReadyPanel panel = new LobbyReadyPanel(netClient);
 *   frame.getContentPane().add(panel, BorderLayout.EAST);
 */
public final class LobbyReadyPanel extends JPanel implements ClientPhaseBus.PhaseListener {

    /** Network client (type: Object) */
    private Object netClient;

    /** Ready toggle callback (optional) */
    private java.util.function.Consumer<Boolean> onSendReady;

    /** READY state map: sessionId -> ready */
    private final Map<Integer, Boolean> readyMap = new ConcurrentHashMap<>();

    /** UI components */
    private final DefaultListModel<String> listModel = new DefaultListModel<>();
    private final JList<String> readyList = new JList<>(listModel);
    private final JToggleButton btnReady = new JToggleButton("Ready");

    /** Phase bus */
    private final ClientPhaseBus bus = ClientPhaseBus.get();

    /** My session id() */
    private int mySessionId = -1;

    public LobbyReadyPanel(Object netClient) {
        super(new BorderLayout(8, 8));
        this.netClient = Objects.requireNonNull(netClient, "netClient");
        buildUi();
        wireEvents();
        bus.addListener(this);
    }

    /**  */
    public LobbyReadyPanel setNetClient(Object netClient) {
        this.netClient = netClient;
        return this;
    }

    /**    id  */
    public LobbyReadyPanel setMySessionId(int sessionId) {
        this.mySessionId = sessionId;
        refreshList();
        return this;
    }

    /**   ().      */
    public LobbyReadyPanel onSendReady(java.util.function.Consumer<Boolean> cb) {
        this.onSendReady = cb;
        return this;
    }

    private void buildUi() {
        setBorder(new EmptyBorder(8, 8, 8, 8));
        setPreferredSize(new Dimension(260, 0));
        setBackground(new Color(28, 34, 46));

        JLabel title = new JLabel(" ");
        title.setForeground(new Color(230, 238, 246));
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));

        readyList.setBorder(BorderFactory.createLineBorder(new Color(62, 70, 86)));
        readyList.setBackground(new Color(36, 42, 56));
        readyList.setForeground(new Color(210, 220, 236));
        readyList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        btnReady.setFocusPainted(false);
        btnReady.setBackground(new Color(48, 76, 96));
        btnReady.setForeground(Color.WHITE);

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(title, BorderLayout.WEST);

        add(top, BorderLayout.NORTH);
        add(new JScrollPane(readyList), BorderLayout.CENTER);
        add(btnReady, BorderLayout.SOUTH);
    }

    private void wireEvents() {
        btnReady.addActionListener(e -> {
            boolean ready = btnReady.isSelected();
            btnReady.setText(ready  "Ready  : "Ready");
            //   
            if (onSendReady != null) {
                try { onSendReady.accept(ready); } catch (Throwable t) { t.printStackTrace(); }
                return;
            }
            /
            sendReadyToggle(ready);
        });
    }

    // ---------------- ClientPhaseBus.PhaseListener ----------------

    @Override
    public void onReadyToggle(int sessionId, boolean ready) {
        readyMap.put(sessionId, ready);
        refreshList();
        //      )
        if (sessionId == mySessionId) {
            SwingUtilities.invokeLater(() -> {
                if (btnReady.isSelected() != ready) {
                    btnReady.setSelected(ready);
                    btnReady.setText(ready ? "Ready" : "Not Ready");
                }
            });
        }
    }

    @Override public void onPhaseUpdate(ClientPhaseBus.PhaseState s) { /* ignore for now */ }
    @Override public void onCountdown(int sec) { /* ignore */ }
    @Override public void onRoundResult(ClientPhaseBus.RoundResult r) { /* ignore */ }

    // ---------------- Private methods ----------------

    private void refreshList() {
        SwingUtilities.invokeLater(() -> {
            listModel.clear();
            // Display: [Ready/Not Ready] sessionId
            readyMap.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(e -> {
                        boolean r = Boolean.TRUE.equals(e.getValue());
                        String self = (e.getKey() != null && e.getKey() == mySessionId) ? " (me)" : "";
                        listModel.addElement(String.format("[%s] %d%s",
                                r ? "Ready" : "Not Ready", e.getKey(), self));
                    });
        });
    }

    // ---------------- Internal helpers ----------------

    /**
     * Send ready toggle to server:
     *  - sendReady(boolean)
     *  - ready(boolean)
     *  - send(byte opcode, byte[] payload) / write(...)
     *
     * Protocol.Opcode.READY_TOGGLE = 0xFE
     * Custom opcode for READY_TOGGLE
     * (or use onSendReady callback)
     */
    private void sendReadyToggle(boolean ready) {
        Object nc = this.netClient;
        if (nc == null) return;

        // 1) Try standard methods
        if (invokeBool(nc, "sendReady", ready)) return;
        if (invokeBool(nc, "ready", ready)) return;

        // 2) Send frame (opcode, payload)
        byte[] payload = new byte[]{ (byte)(ready ? 1 : 0) };
        if (invokeSendFrame(nc, (byte) 0xFE, payload)) return; // custom opcode

        // Fallback: (manual send, if needed)
    }

    private static boolean invokeBool(Object target, String name, boolean arg) {
        try {
            Method m = target.getClass().getMethod(name, boolean.class);
            m.invoke(target, arg);
            return true;
        } catch (Throwable ignore) { /* fallthrough */ }
        try {
            Method m = target.getClass().getMethod(name, Boolean.class);
            m.invoke(target, Boolean.valueOf(arg));
            return true;
        } catch (Throwable ignore) { /* fallthrough */ }
        return false;
    }

    private static boolean invokeSendFrame(Object target, byte opcode, byte[] payload) {
        String[] names = { "send", "write" };
        for (String n : names) {
            if (invoke2(target, n, byte.class, byte[].class, opcode, payload)) return true;
            if (invoke2(target, n, int.class,  byte[].class, (int)(opcode & 0xFF), payload)) return true;
            // (len+opcode+payload) write(byte[]) :
            if (invoke1(target, n, byte[].class, frame(opcode, payload))) return true;
        }
        return false;
    }

    private static boolean invoke1(Object target, String name, Class<?> p0, Object a0) {
        try {
            Method m = target.getClass().getMethod(name, p0);
            m.invoke(target, a0);
            return true;
        } catch (Throwable ignore) {
            return false;
        }
    }

    private static boolean invoke2(Object target, String name, Class<?> p0, Class<?> p1, Object a0, Object a1) {
        try {
            Method m = target.getClass().getMethod(name, p0, p1);
            m.invoke(target, a0, a1);
            return true;
        } catch (Throwable ignore) {
            return false;
        }
    }

    /** Frame: length(int) + opcode(byte) + payload */
    private static byte[] frame(byte opcode, byte[] payload) {
        int len = 1 + (payload == null ? 0 : payload.length);
        ByteBuffer bb = ByteBuffer.allocate(4 + len).order(ByteOrder.BIG_ENDIAN);
        bb.putInt(len).put(opcode);
        if (payload != null && payload.length > 0) bb.put(payload);
        return bb.array();
    }

    // ---------------- Cleanup ----------------

    /** Dispose resources (cleanup) */
    public void dispose() {
        try { bus.removeListener(this); } catch (Throwable ignore) {}
    }
}
