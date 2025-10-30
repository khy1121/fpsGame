package com.fpsgame.server;

import com.fpsgame.common.Protocol;
import java.io.ByteArrayOutputStream;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Objects;

/**
 * Default server-side router. Normalizes opcode usage to Protocol.Opcode.*.
 *
 * Motivation:
 * - Some Protocol versions in this project may not expose top-level alias
 *   constants (e.g., Protocol.MAP_VOTE). This router keeps references under
 *   Protocol.Opcode.* to avoid resolution errors.
 */
public final class DefaultServerRouter {

    /** Hooks for game logic/lobby integration. */
    public interface Hooks {
        default void onChat(int fromSessionId, String text, Broadcaster bc) throws IOException {
            bc.broadcastChat("[" + fromSessionId + "] " + text);
        }
        default void setReady(int sessionId, boolean ready) {}
        default void setSelection(int sessionId, int team, int character) {}
        default void registerMapVote(int sessionId, int mapId) {}
        default void onClientBye(int sessionId) {}
        default int currentPhase() { return 0; }
    }

    /** Server context used by router (send/broadcast/close/log). */
    public interface ServerContext {
        DataOutput getSender(int sessionId);
        void broadcast(byte opcode, byte[] payload) throws IOException;
        void closeSession(int sessionId);
        void log(String message);
    }

    /** Broadcaster helper for various outbound messages. */
    public static final class Broadcaster {
        private final ServerContext context;

        public Broadcaster(ServerContext context) {
            this.context = Objects.requireNonNull(context, "context");
        }

        /** Broadcast system chat. */
        public void broadcastChat(String text) throws IOException {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(64);
            Protocol.putUtf8(baos, text == null ? "" : text);
            context.broadcast(Protocol.Opcode.CHAT, baos.toByteArray());
        }

        /** Broadcast phase update. */
        public void broadcastPhase(int phaseCode) throws IOException {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(4);
            Protocol.putByte(baos, phaseCode);
            context.broadcast(Protocol.Opcode.PHASE_UPDATE, baos.toByteArray());
        }

        /** Broadcast countdown (seconds). */
        public void broadcastCountdown(int seconds) throws IOException {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(4);
            Protocol.putInt(baos, seconds);
            context.broadcast(Protocol.Opcode.COUNTDOWN, baos.toByteArray());
        }

        /** Broadcast round result. */
        public void broadcastRoundResult(int winnerTeam, int blueRounds, int redRounds, boolean matchEnded) throws IOException {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(8);
            Protocol.putByte(baos, winnerTeam);
            Protocol.putByte(baos, blueRounds);
            Protocol.putByte(baos, redRounds);
            Protocol.putByte(baos, matchEnded ? 1 : 0);
            context.broadcast(Protocol.Opcode.ROUND_RESULT, baos.toByteArray());
        }
    }

    private final Hooks hooks;

    public DefaultServerRouter(Hooks hooks) {
        this.hooks = Objects.requireNonNull(hooks, "hooks");
    }

    /** Router entry invoked from RX thread. */
    public void route(ServerContext context, int sessionId, Protocol.Frame frame) {
        try {
            switch (frame.opcode) {
                case Protocol.Opcode.CHAT -> onChat(context, sessionId, frame);
                case Protocol.Opcode.PING -> onPing(context, sessionId, frame);
                case Protocol.Opcode.PONG -> onPong(context, sessionId, frame);
                case Protocol.Opcode.BYE  -> onBye(context, sessionId);
                case Protocol.Opcode.READY_TOGGLE -> onReadyToggle(context, sessionId, frame);
                case Protocol.Opcode.SET_SELECTION -> onSetSelection(context, sessionId, frame);
                case Protocol.Opcode.MAP_VOTE -> onMapVote(context, sessionId, frame);
                // Ignore server-to-client only opcodes on server RX
                case Protocol.Opcode.WELCOME,
                     Protocol.Opcode.PHASE_UPDATE,
                     Protocol.Opcode.COUNTDOWN,
                     Protocol.Opcode.ROUND_RESULT,
                     Protocol.Opcode.INPUT,
                     Protocol.Opcode.SNAPSHOT -> { /* ignore on server RX */ }
                default -> context.log("Unknown opcode from " + sessionId + ": " + (frame.opcode & 0xFF));
            }
        } catch (Throwable t) {
            context.log("route error from " + sessionId + ": " + t);
        }
    }

    // Handlers

    private void onChat(ServerContext ctx, int sid, Protocol.Frame f) throws IOException {
        String text = Protocol.parseChat(f.payload);
        hooks.onChat(sid, text, new Broadcaster(ctx));
    }

    /** Reply PONG immediately to client PING. */
    private void onPing(ServerContext ctx, int sid, Protocol.Frame f) throws IOException {
        long nonce = Protocol.parseNonce(f.payload);
        DataOutput out = ctx.getSender(sid);
        Protocol.sendPong(out, nonce);
    }

    /** Client PONG currently only logged (for diagnostics). */
    private void onPong(ServerContext ctx, int sid, Protocol.Frame f) {
        long nonce = Protocol.parseNonce(f.payload);
        ctx.log("PONG from " + sid + " nonce=" + nonce);
    }

    private void onBye(ServerContext ctx, int sid) {
        hooks.onClientBye(sid);
        ctx.closeSession(sid);
    }

    private void onReadyToggle(ServerContext ctx, int sid, Protocol.Frame f) {
        boolean ready = Protocol.parseReadyToggle(f.payload);
        ctx.log("[RX] READY_TOGGLE sid=" + sid + " ready=" + ready);
        hooks.setReady(sid, ready);
        ctx.log("[HOOK] setReady called for sid=" + sid);
    }

    private void onSetSelection(ServerContext ctx, int sid, Protocol.Frame f) {
        Protocol.Selection sel = Protocol.parseSetSelection(f.payload);
        ctx.log("[RX] SET_SELECTION sid=" + sid + " team=" + sel.team + " char=" + sel.character);
        hooks.setSelection(sid, sel.team, sel.character);
        ctx.log("[HOOK] setSelection called for sid=" + sid);
    }

    private void onMapVote(ServerContext ctx, int sid, Protocol.Frame f) {
        int mapId = Protocol.parseMapVote(f.payload);
        ctx.log("[RX] MAP_VOTE sid=" + sid + " mapId=" + mapId);
        hooks.registerMapVote(sid, mapId);
        ctx.log("[HOOK] registerMapVote called for sid=" + sid);
    }

    // Session lifecycle hooks (optional)

    public void onOpen(ServerContext ctx, int sessionId) {
        ctx.log("session open: " + sessionId + " phase=" + hooks.currentPhase());
    }

    public void onClosed(ServerContext ctx, int sessionId, String reason) {
        ctx.log("session closed: " + sessionId + " reason=" + reason);
        hooks.onClientBye(sessionId);
    }
}
