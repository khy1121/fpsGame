package com.fpsgame.client;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import com.fpsgame.common.Protocol;

/** ClientController (ASCII comments). */
public class ClientController {

    /** ClientController (ASCII comments). */
    public interface Ui {
        /** ClientController (ASCII comments). */
        void onChat(String text);
        /** ClientController (ASCII comments). */
        void onWelcome(Protocol.Welcome welcome);
        /** ClientController (ASCII comments). */
        void onPhaseUpdate(int phaseCode);
        /** ClientController (ASCII comments). */
        void onCountdown(int seconds);
        /** ClientController (ASCII comments). */
        void onRoundResult(Protocol.RoundResult rr);
        /** ClientController (ASCII comments). */
        void onDisconnected(String message);
        /** ClientController (ASCII comments). */
        default void onPingPong(long rttMillis) {}
        /** Ready status update (ready/total). */
        default void onReadyStatus(int ready, int total) {}
    }

    //

    private final Ui ui;                 //
    private final boolean dispatchOnEdt; //

    private NetClient net;               //
    private volatile long lastPingNonce; //
    private volatile long lastPingSentAt;

    // Legacy UI system removed (PHASE 1)
    // private com.fpsgame.client.PhaseIntegration phaseIntegration;   //
    // private com.fpsgame.client.ClientLobbyBootstrap lobbyBootstrap; //

    //

    /** ClientController (ASCII comments). */
    public ClientController(Ui ui, boolean dispatchOnEdt) {
        this.ui = Objects.requireNonNull(ui, "ui");
        this.dispatchOnEdt = dispatchOnEdt;
    }

    /** ClientController (ASCII comments). */
    public synchronized void connect(String host, int port, int timeoutMs) throws IOException {
        if (net != null) return;
        NetClient.Listener listener = buildListener();
        net = new NetClient(listener, dispatchOnEdt);
        net.connect(host, port, timeoutMs);
        //
        sendPingOnce();
    }

    /** ClientController (ASCII comments). */
    public void connect(String host, int port) throws IOException {
        connect(host, port, 3000);
    }

    /** ClientController (ASCII comments). */
    public synchronized void disconnect() {
        //
        uninstallUi();
        if (net != null) {
            try {
                //
                net.sendBye();
            } catch (IOException ignore) {}
            try { net.close(); } catch (Exception ignore) {}
            net = null;
        }
    }

    /** ClientController (ASCII comments). */
    public void close() {
        disconnect();
    }

    /** ClientController (ASCII comments). */
    public boolean isConnected() {
        NetClient n = net;
        return n != null && n.isConnected();
    }

    //

    /** 
     * Legacy UI attachment method - DEPRECATED (PHASE 1)
     * Use new LobbyFrame instead.
     */
    @Deprecated
    public synchronized void attachUi(JFrame gameFrame) {
        // Legacy UI system removed
        // New LobbyFrame should be used directly
    }

    /** 
     * Legacy UI uninstall method - DEPRECATED (PHASE 1)
     */
    @Deprecated
    public synchronized void uninstallUi() {
        // Legacy UI system removed
    }

    //

    /** ClientController (ASCII comments). */
    public void sendChat(String text) {
        NetClient n = ensureConnected();
        try {
            n.sendChat(text == null ? "" : text);
        } catch (IOException e) {
            notifyDisconnected("send error: " + e.getMessage());
        }
    }

    /** ClientController (ASCII comments). */
    public void sendReadyToggle(boolean ready) {
        NetClient n = ensureConnected();
        try {
            n.sendReadyToggle(ready);
        } catch (IOException e) {
            notifyDisconnected("send error: " + e.getMessage());
        }
    }

    /** ClientController (ASCII comments). */
    public void sendSetSelection(int team, int character) {
        NetClient n = ensureConnected();
        try {
            n.sendSetSelection(team, character);
        } catch (IOException e) {
            notifyDisconnected("send error: " + e.getMessage());
        }
    }

    /** ClientController (ASCII comments). */
    public void sendMapVote(int mapId) {
        NetClient n = ensureConnected();
        try {
            n.sendMapVote(mapId);
        } catch (IOException e) {
            notifyDisconnected("send error: " + e.getMessage());
        }
    }

    /** ClientController (ASCII comments). */
    public void sendPingOnce() {
        NetClient n = ensureConnected();
        try {
            long nonce = ThreadLocalRandom.current().nextLong();
            lastPingNonce = nonce;
            lastPingSentAt = System.nanoTime();
            n.sendPing(nonce);
        } catch (IOException e) {
            notifyDisconnected("send error: " + e.getMessage());
        }
    }

    //

    private NetClient.Listener buildListener() {
        return new NetClient.Listener() {
            @Override
            public void onChat(String text) {
                dispatchEdt(() -> ui.onChat(text));
            }

            @Override
            public void onWelcome(Protocol.Welcome welcome) {
                dispatchEdt(() -> ui.onWelcome(welcome));
            }

            @Override
            public void onPing(long nonce) {
                //
            }

            @Override
            public void onPong(long nonce) {
                if (nonce == lastPingNonce) {
                    long rttMs = Math.max(0L, (System.nanoTime() - lastPingSentAt) / 1_000_000L);
                    dispatchEdt(() -> ui.onPingPong(rttMs));
                }
            }

            @Override
            public void onPhaseUpdate(int phaseCode) {
                dispatchEdt(() -> ui.onPhaseUpdate(phaseCode));
            }

            @Override
            public void onCountdown(int seconds) {
                dispatchEdt(() -> ui.onCountdown(seconds));
            }

            @Override
            public void onRoundResult(Protocol.RoundResult rr) {
                dispatchEdt(() -> ui.onRoundResult(rr));
            }

            @Override
            public void onReadyStatus(int ready, int total) {
                dispatchEdt(() -> ui.onReadyStatus(ready, total));
            }

            @Override
            public void onDisconnected(String message) {
                //
                dispatchEdt(() -> ui.onDisconnected(message));
            }
        };
    }

    //

    private NetClient ensureConnected() {
        NetClient n = net;
        if (n == null || !n.isConnected()) {
            throw new IllegalStateException("Server not connected.");
        }
        return n;
    }

    /** ClientController (ASCII comments). */
    private static void dispatchEdt(Runnable r) {
        if (SwingUtilities.isEventDispatchThread()) {
            r.run();
        } else {
            SwingUtilities.invokeLater(r);
        }
    }

    /** ClientController (ASCII comments). */
    public void notifyDisconnected(String message) {
        //
        dispatchEdt(() -> ui.onDisconnected(message == null ? "" : message));
        disconnect();
    }

    //

    /** ClientController (ASCII comments). */
    public static void main(String[] args) throws Exception {
        String host = args.length > 0 ? args[0] : "127.0.0.1";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 7777;

        ClientController controller = new ClientController(new Ui() {
            @Override public void onChat(String text) { System.out.println("[CHAT] " + text); }
            @Override public void onWelcome(Protocol.Welcome welcome) {
                System.out.println("[WELCOME] id=" + welcome.myId + " world=" + welcome.worldW + "x" + welcome.worldH);
            }
            @Override public void onPhaseUpdate(int phaseCode) { System.out.println("[PHASE] " + phaseCode); }
            @Override public void onCountdown(int seconds) { System.out.println("[COUNTDOWN] " + seconds); }
            @Override public void onRoundResult(Protocol.RoundResult rr) {
                System.out.println("[ROUND] winner=" + rr.winnerTeam + " score " + rr.blueRounds + ":" + rr.redRounds + " end=" + rr.matchEnded);
            }
            @Override public void onDisconnected(String message) { System.out.println("[DISCONNECTED] " + message); }
            @Override public void onPingPong(long rttMillis) { System.out.println("[RTT] " + rttMillis + " ms"); }
        }, true);

        controller.connect(host, port, 3000);
        System.out.println("Connected: " + controller.isConnected());

        //
        javax.swing.JFrame f = new javax.swing.JFrame("ClientController Demo");
        f.setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        f.setSize(960, 600);
        f.setLocationRelativeTo(null);
        f.setVisible(true);
        controller.attachUi(f);

        controller.sendChat("hello");
        Thread.sleep(300);
        controller.sendReadyToggle(true);
        Thread.sleep(300);
        controller.sendSetSelection(1, 3);
        Thread.sleep(300);
        controller.sendMapVote(0);
        Thread.sleep(300);
        controller.sendPingOnce();

        //
        Thread.sleep(5000);
        controller.close();
        f.dispose();
    }
}





