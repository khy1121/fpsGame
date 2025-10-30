package com.fpsgame.client;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * AutoReconnect: manages reconnect loop for NetClient.
 * - Creates NetClient with given listener and reconnects with backoff
 * - start()/close() control lifecycle; current() returns latest client
 */
public final class AutoReconnect implements AutoCloseable {

    private final String host;
    private final int port;
    private final int connectTimeoutMillis;
    private final NetClient.Listener listener;

    private final long initialDelayMs;
    private final long maxDelayMs;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread loopThread;

    private volatile NetClient client;

    /**
     * @param host server host
     * @param port server port
     * @param connectTimeoutMillis connect timeout (ms)
     * @param listener NetClient listener to attach
     * @param initialDelayMs initial backoff delay (ms)
     * @param maxDelayMs     max backoff delay (ms)
     */
    public AutoReconnect(String host,
                         int port,
                         int connectTimeoutMillis,
                         NetClient.Listener listener,
                         long initialDelayMs,
                         long maxDelayMs) {
        this.host = Objects.requireNonNull(host, "host");
        this.port = port;
        this.connectTimeoutMillis = Math.max(1000, connectTimeoutMillis);
        this.listener = listener;
        this.initialDelayMs = Math.max(250L, initialDelayMs);
        this.maxDelayMs = Math.max(this.initialDelayMs, maxDelayMs);
    }

    /** Convenience defaults: initial=1000ms, max=10000ms */
    public AutoReconnect(String host, int port, int connectTimeoutMillis, NetClient.Listener listener) {
        this(host, port, connectTimeoutMillis, listener, 1000L, 10_000L);
    }

    /** Current NetClient (may be null). */
    public NetClient current() { return client; }

    /** Start loop (no-op if already running). */
    public synchronized void start() {
        if (running.get()) return;
        running.set(true);
        loopThread = new Thread(this::loop, "AutoReconnect");
        loopThread.setDaemon(true);
        loopThread.start();
    }

    /** Stop loop and close current NetClient. */
    @Override
    public synchronized void close() {
        running.set(false);
        if (loopThread != null) {
            loopThread.interrupt();
            try { loopThread.join(1000); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
        }
        loopThread = null;

        NetClient c = client;
        client = null;
        if (c != null) {
            try { c.close(); } catch (Exception ignored) {}
        }
    }

    // ================= Impl =================

    private void loop() {
        long delay = 0L; // immediate first attempt
        while (running.get()) {
            if (delay > 0) {
                try { Thread.sleep(delay); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
            }

            if (!running.get()) break;

            try {
                // Close previous
                NetClient prev = client;
                if (prev != null) {
                    try { prev.close(); } catch (Exception ignored) {}
                }

                // Attempt connect
                client = new NetClient(host, port, connectTimeoutMillis, listener);

                // Connected: reset backoff
                delay = 0L;

                // Keep alive while connected
                while (running.get() && client != null && client.isConnected()) {
                    try { Thread.sleep(300); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
                }

                // If still running, compute next backoff
                if (!running.get()) break;
                delay = Math.max(initialDelayMs, delay == 0 ? initialDelayMs : Math.min(maxDelayMs, delay * 2));
            } catch (IOException connectFail) {
                // On connect failure: backoff
                delay = Math.max(initialDelayMs, delay == 0 ? initialDelayMs : Math.min(maxDelayMs, delay * 2));
            }
        }
    }
}

