package com.fpsgame.server;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.Objects;

import com.fpsgame.common.Protocol;

/**
 * Manages a single client connection (Session).
 *
 * Responsibilities:
 * - Socket lifecycle: creating/closing I/O streams, thread termination
 * - Frame parsing via Protocol.readFrame in a dedicated RX thread
 * - Delegating parsed frames to DefaultServerRouter.route(...)
 * - Transmission (DataOutput) synchronized via send(op,payload)
 *
 * Threading:
 * - One dedicated RX thread, safely interrupted/joined on shutdown
 * - TX: Upper layer (ServerContext) can share DataOutput from getSender(...)
 */
public class ServerSession implements Closeable {

    // Session identity
    private final int sessionId;              // unique ID assigned by registry

    // Socket and router/context
    private final Socket socket;              // client socket
    private final DefaultServerRouter router; // opcode dispatcher
    private final DefaultServerRouter.ServerContext serverContext; // callbacks

    // I/O
    private DataInputStream in;
    private DataOutputStream out;

    // RX thread
    private Thread rxThread;
    private volatile boolean running = false;

    public ServerSession(int sessionId,
                         Socket socket,
                         DefaultServerRouter router,
                         DefaultServerRouter.ServerContext serverContext) {
        this.sessionId = sessionId;
        this.socket = Objects.requireNonNull(socket, "socket");
        this.router = Objects.requireNonNull(router, "router");
        this.serverContext = Objects.requireNonNull(serverContext, "serverContext");
    }

    // ===================== Lifecycle =====================

    /** Start session: create I/O streams and start RX thread. */
    public synchronized void start() throws IOException {
        if (running) return;
        try {
            socket.setTcpNoDelay(true);
            // Set SO_TIMEOUT for read operations (60 seconds idle timeout)
            // This prevents hanging forever if client becomes unresponsive
            socket.setSoTimeout(60000);
        } catch (Exception ignore) { /* May fail on some platforms - ignore */ }

        this.in = new DataInputStream(socket.getInputStream());
        this.out = new DataOutputStream(socket.getOutputStream());

        running = true;
        rxThread = new Thread(this::rxLoop, "ServerSession-RX-" + sessionId);
        rxThread.setDaemon(true);
        rxThread.start();
    }

    /** End session: stop thread and release socket/stream resources. */
    @Override
    public synchronized void close() {
        if (!running) return;
        running = false;
        // Close socket first to release any blocked reads
        try { socket.close(); } catch (IOException ignore) {}
        // Clean up streams
        try { if (in != null) in.close(); } catch (IOException ignore) {}
        try { if (out != null) out.close(); } catch (IOException ignore) {}
        // Join RX thread (max 1 second)
        if (rxThread != null && rxThread != Thread.currentThread()) {
            try { rxThread.join(1000); } catch (InterruptedException ignore) { Thread.currentThread().interrupt(); }
        }
    }

    // ===================== Accessors/Transmission =====================

    /** DataOutput returned by registry in getSender(...). */
    public DataOutput getDataOutput() { return out; }

    /**
     * Safe transmission helper: send frame with internal synchronization.
     * Use when multiple threads may send from higher levels.
     */
    public void send(byte opcode, byte[] payload) throws IOException {
        DataOutputStream localOut;
        synchronized (this) { localOut = this.out; }
        if (localOut == null) throw new SocketException("Session not started or already closed.");
        synchronized (localOut) {
            Protocol.writeFrame(localOut, opcode, payload);
            // localOut.flush(); // optional
        }
    }

    /** Return session ID. */
    public int getSessionId() { return sessionId; }

    // ===================== Receive Loop =====================

    /** RX thread entry point. */
    private void rxLoop() {
        try {
            while (running) {
                Protocol.Frame frame = Protocol.readFrame(in);
                // INPUT frames are delegated directly to SyncService (if present)
                if (frame != null && frame.opcode == Protocol.Opcode.INPUT) {
                    try {
                        if (serverContext instanceof SessionRegistry reg) {
                            reg.handleInput(sessionId, frame.payload);
                        }
                    } catch (Throwable ignore) {}
                }
                router.route(serverContext, sessionId, frame);
            }
        } catch (java.net.SocketTimeoutException timeout) {
            // Client idle for 60 seconds - close connection
            serverContext.log("Session " + sessionId + " timed out (60s idle)");
        } catch (EOFException | SocketException eof) {
            // Remote disconnect or socket error - treat as normal termination
            serverContext.log("Session " + sessionId + " disconnected: " + eof.getMessage());
        } catch (IOException io) {
            serverContext.log("Session " + sessionId + " I/O error: " + io.getMessage());
        } catch (Throwable t) {
            serverContext.log("Session " + sessionId + " loop error: " + t);
        } finally {
            // Clean up server-side even if no BYE received
            try { serverContext.closeSession(sessionId); } catch (Throwable ignore) {}
        }
    }

    // ===================== WELCOME helpers =====================

    /** Send legacy WELCOME without mapId. */
    public void sendWelcome(int myId, int team, int character, int worldW, int worldH) {
        try {
            sendWelcomeUnsafe(myId, team, character, worldW, worldH);
        } catch (IOException e) {
            serverContext.log("sendWelcome failed for " + sessionId + ": " + e.getMessage());
            try { close(); } catch (Exception ignore) {}
        }
    }

    private void sendWelcomeUnsafe(int myId, int team, int character, int worldW, int worldH) throws IOException {
        DataOutputStream localOut;
        synchronized (this) { localOut = this.out; }
        if (localOut == null) throw new SocketException("Session closed.");
        synchronized (localOut) {
            serverContext.log("Sending WELCOME(v1) to sid=" + sessionId + " myId=" + myId + " team=" + team + " char=" + character + " world=" + worldW + "x" + worldH);
            Protocol.sendWelcome(localOut, myId, team, character, worldW, worldH);
        }
    }

    // v2: version including mapId
    public void sendWelcome(int myId, int team, int character, int worldW, int worldH, int mapId) {
        try {
            sendWelcomeUnsafe(myId, team, character, worldW, worldH, mapId);
        } catch (IOException e) {
            serverContext.log("sendWelcome(v2) failed for " + sessionId + ": " + e.getMessage());
            try { close(); } catch (Exception ignore) {}
        }
    }

    private void sendWelcomeUnsafe(int myId, int team, int character, int worldW, int worldH, int mapId) throws IOException {
        DataOutputStream localOut;
        synchronized (this) { localOut = this.out; }
        if (localOut == null) throw new SocketException("Session closed.");
        synchronized (localOut) {
            serverContext.log("Sending WELCOME(v2) to sid=" + sessionId + " myId=" + myId + " team=" + team + " char=" + character + " world=" + worldW + "x" + worldH + " mapId=" + mapId);
            Protocol.sendWelcome(localOut, myId, team, character, worldW, worldH, mapId);
        }
    }
}

