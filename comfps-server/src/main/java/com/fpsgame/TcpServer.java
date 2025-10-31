package com.fpsgame.server;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * TCP server (accept loop): binds, accepts, creates sessions.
 */
public class TcpServer implements Closeable {

    private final int port;
    private final DefaultServerRouter.Hooks hooks;
    private final SessionRegistry registry;

    private ServerSocket serverSocket;
    private Thread acceptThread;
    private final AtomicBoolean running = new AtomicBoolean(false);

    private final int backlog;
    private final String bindAddress;

    /** Constructor with external SessionRegistry (recommended). */
    public TcpServer(int port, DefaultServerRouter.Hooks hooks, SessionRegistry registry, int backlog, String bindAddress) {
        this.port = port;
        this.hooks = Objects.requireNonNull(hooks, "hooks");
        this.registry = Objects.requireNonNull(registry, "registry");
        this.backlog = backlog;
        this.bindAddress = bindAddress;
    }

    /** Legacy constructor: creates its own SessionRegistry. */
    public TcpServer(int port, DefaultServerRouter.Hooks hooks, int backlog, String bindAddress) {
        this.port = port;
        this.hooks = Objects.requireNonNull(hooks, "hooks");
        this.backlog = backlog;
        this.bindAddress = bindAddress;

        DefaultServerRouter router = new DefaultServerRouter(hooks);
        this.registry = new SessionRegistry(router);
    }

    /** Convenience: ANY bind, default backlog. */
    public TcpServer(int port, DefaultServerRouter.Hooks hooks) { this(port, hooks, 0, null); }

    // ==== Lifecycle ====

    /** Start: bind ServerSocket and start accept loop. */
    public synchronized void start() throws IOException {
        if (running.get()) return;

        this.serverSocket = new ServerSocket();
        try { this.serverSocket.setReuseAddress(true); } catch (Exception ignore) {}

        InetSocketAddress addr = new InetSocketAddress(
                bindAddress == null || bindAddress.isEmpty() ? "0.0.0.0" : bindAddress,
                port
        );
        if (backlog > 0) this.serverSocket.bind(addr, backlog); else this.serverSocket.bind(addr);

        running.set(true);
        acceptThread = new Thread(this::acceptLoop, "TcpServer-Accept-" + port);
        acceptThread.setDaemon(true);
        acceptThread.start();

        registry.log("TCP server started on " + addr);
    }

    /** Stop: stop accept loop and close sessions. */
    @Override
    public synchronized void close() {
        if (!running.getAndSet(false)) return;

        try { if (serverSocket != null) serverSocket.close(); } catch (IOException ignore) {}

        if (acceptThread != null && acceptThread != Thread.currentThread()) {
            try { acceptThread.join(1000); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
        }

        closeAllSessionsSafely();
        registry.log("TCP server stopped.");
    }

    // ==== Accept loop ====

    private void acceptLoop() {
        try {
            while (running.get()) {
                Socket sock = null;
                try {
                    sock = serverSocket.accept();
                    configureSocket(sock);

                    ServerSession s = registry.createAndStartSession(sock);
                    registry.log("client connected: sid=" + s.getSessionId() + " from " + safeRemote(sock));
                } catch (SocketException se) {
                    if (running.get()) registry.log("Accept socket error: " + se.getMessage());
                    break;
                } catch (IOException ioe) {
                    registry.log("Accept failed: " + ioe.getMessage());
                    safeClose(sock);
                } catch (Throwable t) {
                    registry.log("Accept loop error: " + t);
                    safeClose(sock);
                }
            }
        } finally {
            // accept loop finished
        }
    }

    // ==== Utils ====

    private static void configureSocket(Socket sock) {
        try { sock.setTcpNoDelay(true); } catch (Exception ignore) {}
        try { sock.setKeepAlive(true); } catch (Exception ignore) {}
        try { sock.setSoTimeout(0); } catch (Exception ignore) {} // non-blocking read
    }

    private static void safeClose(Socket s) { if (s != null) { try { s.close(); } catch (IOException ignore) {} } }
    private static String safeRemote(Socket s) { try { return s.getRemoteSocketAddress().toString(); } catch (Exception e) { return "(unknown)"; } }

    /** Close all sessions (best-effort). */
    private void closeAllSessionsSafely() {
        try { registry.broadcastSystemChat("[SYSTEM] Server is shutting down"); } catch (Exception ignore) {}
        // individual sessions will be closed by ServerSession/registry paths
    }

    // ==== Accessors ====
    public SessionRegistry getRegistry() { return registry; }
    public boolean isRunning() { return running.get(); }
    public int getPort() { return port; }
    public String getBindAddress() { return bindAddress; }
}

