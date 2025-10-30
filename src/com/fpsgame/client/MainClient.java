package com.fpsgame.client;

import java.awt.*;
import java.awt.event.*;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/**
 * Minimal production-ready Swing client that connects to MainServer.
 * - Length-prefixed framing compatible with server (int length, byte opcode, payload)
 * - Separate RX thread; synchronized send; graceful shutdown
 * - Simple chat UI (Enter to send), PING button to measure RTT
 * - No external dependencies on other project files
 *
 * You can evolve this bootstrap to integrate game rendering panels later.
 */
public final class MainClient {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ClientConfig cfg = ClientConfig.fromArgs(args);
            try (ClientWindow win = new ClientWindow(cfg)) {
                win.setVisible(true);
                
                // 윈도우가 닫힐 때까지 대기
                win.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosing(WindowEvent e) {
                        win.dispose();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(1);
            }
        });
    }

    /* ------------------------------------------------------------------------
     * Configuration
     * --------------------------------------------------------------------- */
    public static final class ClientConfig {
        public final String host;
        public final int port;
        public final int soTimeoutMs;

        private ClientConfig(String host, int port, int soTimeoutMs) {
            this.host = host;
            this.port = port;
            this.soTimeoutMs = soTimeoutMs;
        }

        public static ClientConfig defaults() {
            return new ClientConfig("127.0.0.1", 7777, 20_000);
        }

        public static ClientConfig fromArgs(String[] args) {
            ClientConfig d = defaults();
            String host = d.host;
            int port = d.port;
            int timeout = d.soTimeoutMs;
            for (String a : args) {
                if (a.startsWith("--host=")) host = a.substring("--host=".length());
                else if (a.startsWith("--port=")) port = parseIntOr(a.substring("--port=".length()), d.port);
                else if (a.startsWith("--timeoutMs=")) timeout = parseIntOr(a.substring("--timeoutMs=".length()), d.soTimeoutMs);
            }
            return new ClientConfig(host, port, timeout);
        }

        private static int parseIntOr(String s, int def) {
            try { return Integer.parseInt(s.trim()); } catch (NumberFormatException e) { return def; }
        }
    }

    /* ------------------------------------------------------------------------
     * Wire protocol opcodes (keep in sync with server bootstrap)
     * --------------------------------------------------------------------- */
    private enum Opcode {
        WELCOME((byte)0x01),
        CHAT((byte)0x50),
        PING((byte)0x7E),
        PONG((byte)0x7F),
        BYE((byte)0x5F);

        final byte code;
        Opcode(byte c) { this.code = c; }
        static Opcode from(byte b) {
            for (Opcode o : values()) if (o.code == b) return o;
            return null;
        }
    }

    /* ------------------------------------------------------------------------
     * UI Window
     * --------------------------------------------------------------------- */
    private static final class ClientWindow extends JFrame implements Closeable {
        private final JTextArea outputArea = new JTextArea();
        private final JTextField inputField = new JTextField();
        private final JButton sendBtn = new JButton("Send");
        private final JButton pingBtn = new JButton("Ping");
        private final JButton connectBtn = new JButton("Connect");
        private final JLabel statusLbl = new JLabel("Disconnected");
        private final JComboBox<String> scopeBox = new JComboBox<>(new String[]{"All"});
        private final ClientConfig config;
        private final NetClient net;

        ClientWindow(ClientConfig cfg) {
            super("FPS Client (Chat Bootstrap)");
            this.config = Objects.requireNonNull(cfg, "cfg");
            this.net = new NetClient();

            setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            setSize(760, 520);
            setLocationRelativeTo(null);

            outputArea.setEditable(false);
            outputArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
            outputArea.setLineWrap(true);
            outputArea.setWrapStyleWord(true);

            JPanel topBar = new JPanel(new GridBagLayout());
            topBar.setBorder(new EmptyBorder(8, 8, 8, 8));
            GridBagConstraints gc = new GridBagConstraints();
            gc.insets = new Insets(0, 4, 0, 4);
            gc.gridy = 0;

            gc.gridx = 0; topBar.add(new JLabel("Scope:"), gc);
            gc.gridx = 1; topBar.add(scopeBox, gc);

            gc.gridx = 2; topBar.add(connectBtn, gc);
            gc.gridx = 3; topBar.add(pingBtn, gc);
            gc.gridx = 4; topBar.add(statusLbl, gc);

            JPanel bottom = new JPanel(new BorderLayout(6, 6));
            bottom.setBorder(new EmptyBorder(8, 8, 8, 8));
            bottom.add(inputField, BorderLayout.CENTER);
            bottom.add(sendBtn, BorderLayout.EAST);

            JScrollPane scroll = new JScrollPane(outputArea);
            scroll.setBorder(new EmptyBorder(8, 8, 8, 8));

            setLayout(new BorderLayout());
            add(topBar, BorderLayout.NORTH);
            add(scroll, BorderLayout.CENTER);
            add(bottom, BorderLayout.SOUTH);

            // Actions
            sendBtn.addActionListener(e -> sendChat());
            inputField.addActionListener(e -> sendChat());
            pingBtn.addActionListener(e -> doPing());
            connectBtn.addActionListener(e -> doConnectDialog());

            addWindowListener(new WindowAdapter() {
                @Override public void windowOpened(WindowEvent e) {
                    SwingUtilities.invokeLater(() -> outputArea.requestFocusInWindow());
                }
                @Override public void windowClosing(WindowEvent e) {
                    try { close(); } catch (IOException ignored) {}
                }
            });

            // Keyboard shortcut: Ctrl+L to clear log
            getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                    .put(KeyStroke.getKeyStroke(KeyEvent.VK_L, InputEvent.CTRL_DOWN_MASK), "clear");
            getRootPane().getActionMap().put("clear", new AbstractAction() {
                @Override public void actionPerformed(ActionEvent e) {
                    outputArea.setText("");
                }
            });

            setDisconnectedUI();
        }

        private void doConnectDialog() {
            JPanel p = new JPanel(new GridLayout(0, 2, 6, 6));
            JTextField hostF = new JTextField(config.host);
            JTextField portF = new JTextField(Integer.toString(config.port));
            p.add(new JLabel("Host:")); p.add(hostF);
            p.add(new JLabel("Port:")); p.add(portF);

            int res = JOptionPane.showConfirmDialog(this, p, "Connect to Server",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (res != JOptionPane.OK_OPTION) return;

            String host = hostF.getText().trim();
            int port;
            try { port = Integer.parseInt(portF.getText().trim()); }
            catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Invalid port", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            connect(host, port);
        }

        private void connect(String host, int port) {
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            appendLine("Connecting to " + host + ":" + port + " ...");
            CompletableFuture.runAsync(() -> {
                try {
                    net.connect(host, port, 20_000);
                    net.setListener(new NetClient.Listener() {
                        @Override public void onWelcome(String text) {
                            appendLine("[SERVER] " + text);
                            SwingUtilities.invokeLater(ClientWindow.this::setConnectedUI);
                        }
                        @Override public void onChat(String text) {
                            appendLine(text);
                        }
                        @Override public void onPong(long sentAtNanos) {
                            long rttMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - sentAtNanos);
                            appendLine(String.format("PONG (RTT ~ %d ms)", rttMs));
                        }
                        @Override public void onClosed(String reason) {
                            appendLine("[INFO] Disconnected: " + reason);
                            SwingUtilities.invokeLater(ClientWindow.this::setDisconnectedUI);
                        }
                    });
                } catch (IOException e) {
                    appendLine("[ERROR] " + e.getMessage());
                    SwingUtilities.invokeLater(ClientWindow.this::setDisconnectedUI);
                } finally {
                    SwingUtilities.invokeLater(() -> setCursor(Cursor.getDefaultCursor()));
                }
            });
        }

        private void sendChat() {
            String text = inputField.getText().trim();
            if (text.isEmpty()) return;
            inputField.setText("");
            if (!net.isConnected()) {
                appendLine("[WARN] Not connected.");
                return;
            }
            net.send(Opcode.CHAT.code, text.getBytes(StandardCharsets.UTF_8));
        }

        private void doPing() {
            if (!net.isConnected()) {
                appendLine("[WARN] Not connected.");
                return;
            }
            long now = System.nanoTime();
            byte[] payload = Long.toString(now).getBytes(StandardCharsets.UTF_8);
            net.send(Opcode.PING.code, payload);
        }

        private void appendLine(String s) {
            SwingUtilities.invokeLater(() -> {
                outputArea.append(s);
                outputArea.append("\n");
                outputArea.setCaretPosition(outputArea.getDocument().getLength());
            });
        }

        private void setConnectedUI() {
            statusLbl.setText("Connected");
            statusLbl.setForeground(new Color(0x1B5E20));
            connectBtn.setText("Disconnect");
            for (ActionListener al : connectBtn.getActionListeners()) {
                connectBtn.removeActionListener(al);
            }
            connectBtn.addActionListener(e -> {
                try { close(); } catch (IOException ignored) {}
                setDisconnectedUI();
            });
        }

        private void setDisconnectedUI() {
            statusLbl.setText("Disconnected");
            statusLbl.setForeground(new Color(0xB71C1C));
            for (ActionListener al : connectBtn.getActionListeners()) {
                connectBtn.removeActionListener(al);
            }
            connectBtn.setText("Connect");
            connectBtn.addActionListener(e -> doConnectDialog());
        }

        @Override
        public void close() throws IOException {
            net.close();
        }
    }

    /* ------------------------------------------------------------------------
     * Networking client
     * --------------------------------------------------------------------- */
    private static final class NetClient implements Closeable {
        interface Listener {
            void onWelcome(String text);
            void onChat(String text);
            void onPong(long sentAtNanos);
            void onClosed(String reason);
        }

        private final AtomicBoolean connected = new AtomicBoolean(false);
        private final ExecutorService rxPool = Executors.newSingleThreadExecutor(new NamedFactory("net-rx"));
        private volatile Listener listener;
        private Socket socket;
        private DataInputStream in;
        private DataOutputStream out;

        void setListener(Listener l) { this.listener = l; }

        boolean isConnected() { return connected.get(); }

        void connect(String host, int port, int timeoutMs) throws IOException {
            if (connected.get()) return;
            Socket s = new Socket();
            s.setTcpNoDelay(true);
            s.setSoTimeout(timeoutMs);
            s.connect(new InetSocketAddress(host, port), timeoutMs);

            this.socket = s;
            this.in = new DataInputStream(s.getInputStream());
            this.out = new DataOutputStream(s.getOutputStream());
            this.connected.set(true);

            rxPool.execute(this::rxLoop);
        }

        private void rxLoop() {
            try {
                while (connected.get()) {
                    int frameLen = in.readInt(); // includes (1 + payload)
                    byte opcode = in.readByte();
                    int payloadLen = frameLen - 1;
                    if (payloadLen < 0 || payloadLen > (16 * 1024 * 1024)) {
                        throw new IOException("Invalid frame length: " + frameLen);
                    }
                    byte[] payload = new byte[payloadLen];
                    in.readFully(payload);

                    Opcode op = Opcode.from(opcode);
                    if (op == null) continue;

                    switch (op) {
                        case WELCOME: {
                            Listener l = listener;
                            if (l != null) l.onWelcome(new String(payload, StandardCharsets.UTF_8));
                            break;
                        }
                        case CHAT: {
                            Listener l = listener;
                            if (l != null) l.onChat(new String(payload, StandardCharsets.UTF_8));
                            break;
                        }
                        case PONG: {
                            try {
                                long sentAt = Long.parseLong(new String(payload, StandardCharsets.UTF_8).trim());
                                Listener l = listener;
                                if (l != null) l.onPong(sentAt);
                            } catch (NumberFormatException ignored) { /* ignore */ }
                            break;
                        }
                        case BYE: {
                            closeWithReason("Server requested close");
                            return;
                        }
                        default:
                            // ignore others for bootstrap
                            break;
                    }
                }
            } catch (EOFException eof) {
                closeWithReason("Server closed");
            } catch (IOException e) {
                closeWithReason("I/O error: " + e.getMessage());
            }
        }

        void send(byte opcode, byte[] payload) {
            if (!connected.get()) return;
            synchronized (this) {
                try {
                    out.writeInt(1 + payload.length);
                    out.writeByte(opcode);
                    out.write(payload);
                    out.flush();
                } catch (IOException e) {
                    closeWithReason("Send failed: " + e.getMessage());
                }
            }
        }

        private void closeWithReason(String reason) {
            if (!connected.getAndSet(false)) return;
            safeClose(in);
            safeClose(out);
            safeClose(socket);
            Listener l = listener;
            if (l != null) l.onClosed(reason);
        }

        @Override
        public void close() {
            if (!connected.getAndSet(false)) return;
            try {
                // Try to notify server we are leaving
                byte[] why = "client-exit".getBytes(StandardCharsets.UTF_8);
                synchronized (this) {
                    if (out != null) {
                        out.writeInt(1 + why.length);
                        out.writeByte(Opcode.BYE.code);
                        out.write(why);
                        out.flush();
                    }
                }
            } catch (IOException ignored) {
            } finally {
                safeClose(in);
                safeClose(out);
                safeClose(socket);
            }
            rxPool.shutdownNow();
            try { rxPool.awaitTermination(Duration.ofSeconds(1).toMillis(), TimeUnit.MILLISECONDS); }
            catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
            Listener l = listener;
            if (l != null) l.onClosed("Client closed");
        }
    }

    /* ------------------------------------------------------------------------
     * Utilities
     * --------------------------------------------------------------------- */
    private static void safeClose(Closeable c) {
        if (c == null) return;
        try { c.close(); } catch (IOException ignored) {}
    }
    private static void safeClose(Socket s) {
        if (s == null) return;
        try { s.close(); } catch (IOException ignored) {}
    }
    private static final class NamedFactory implements ThreadFactory {
        private final String base; private int idx = 1;
        NamedFactory(String b) { base = b; }
        @Override public Thread newThread(Runnable r) {
            Thread t = new Thread(r, base + "-" + (idx++));
            t.setDaemon(true);
            return t;
        }
    }
}
