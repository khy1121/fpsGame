package com.fpsgame.client;

import java.io.IOException;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * ChatCommandHandler parses simple slash commands from the chat input.
 *
 * Supported commands (client-side only):
 * - /help                       : show command list
 * - /ping [nonce]               : send ping (random or given nonce)
 * - /bye                        : send bye and close connection
 * - /ready on|off|1|0           : toggle ready state
 * - /team <teamId> <charId>     : set team and character selection
 * - /map <mapId>                : vote for a map
 * - /say <message>              : send chat
 * - /me <action>                : local action message (no server username)
 * Any other input is not consumed and should be sent as normal chat.
 */
public class ChatCommandHandler {

    private final NetClient net;              // may be null (offline)
    private final Consumer<String> logger;    // local output sink

    public ChatCommandHandler(NetClient net, Consumer<String> logger) {
        this.net = net; // nullable
        this.logger = Objects.requireNonNullElse(logger, s -> {});
    }

    /**
     * Handle one line of input. Returns true if the line was a command.
     */
    public boolean handleInput(String line) {
        if (line == null) return false;
        String s = line.trim();
        if (s.isEmpty() || s.charAt(0) != '/') return false;

        // strip leading '/'
        String body = s.substring(1).trim();
        if (body.isEmpty()) return true;

        String[] parts = body.split("\\s+", 2);
        String cmd = parts[0].toLowerCase(Locale.ROOT);
        String arg = parts.length > 1 ? parts[1].trim() : "";

        try {
            return switch (cmd) {
                case "help", "?" -> { doHelp(); yield true; }
                case "ping" -> { doPing(arg); yield true; }
                case "bye", "quit", "exit" -> { doBye(); yield true; }
                case "ready" -> { doReady(arg); yield true; }
                case "team" -> { doTeam(arg); yield true; }
                case "map" -> { doMap(arg); yield true; }
                case "say" -> { doSay(arg); yield true; }
                case "me" -> { doMe(arg); yield true; }
                default -> false;
            };
        } catch (IOException io) {
            logger.accept("send error: " + io.getMessage());
            return true; // consumed as command, even if failed
        } catch (IllegalArgumentException iae) {
            logger.accept("command error: " + iae.getMessage());
            return true;
        }
    }

    private boolean isOnline() { return net != null && net.isConnected(); }

    private void doHelp() {
        logger.accept("Commands: /help, /ping [nonce], /bye, /ready on|off|1|0, /team <teamId> <charId>, /map <mapId>, /say <msg>, /me <action>");
    }

    private void doPing(String arg) throws IOException {
        long nonce;
        if (arg.isEmpty()) {
            nonce = System.nanoTime();
        } else {
            try {
                nonce = Long.decode(arg);
            } catch (NumberFormatException nfe) {
                throw new IllegalArgumentException("invalid nonce: " + arg);
            }
        }
        if (isOnline()) {
            net.sendPing(nonce);
            logger.accept("[cmd] ping sent: " + nonce);
        } else {
            logger.accept("[cmd] offline: ping skipped");
        }
    }

    private void doBye() throws IOException {
        if (isOnline()) {
            net.sendBye();
            logger.accept("[cmd] bye sent");
        } else {
            logger.accept("[cmd] offline: bye skipped");
        }
    }

    private void doReady(String arg) throws IOException {
        if (arg.isEmpty()) throw new IllegalArgumentException("usage: /ready on|off|1|0");
        boolean ready = switch (arg.toLowerCase(Locale.ROOT)) {
            case "on", "true", "1", "yes", "y" -> true;
            case "off", "false", "0", "no", "n" -> false;
            default -> throw new IllegalArgumentException("usage: /ready on|off|1|0");
        };
        if (isOnline()) {
            net.sendReadyToggle(ready);
            logger.accept("[cmd] ready=" + ready);
        } else {
            logger.accept("[cmd] offline: ready toggle skipped");
        }
    }

    private void doTeam(String arg) throws IOException {
        String[] a = arg.split("\\s+");
        if (a.length < 2) throw new IllegalArgumentException("usage: /team <teamId> <charId>");
        int teamId, charId;
        try { teamId = Integer.parseInt(a[0]); } catch (NumberFormatException nfe) { throw new IllegalArgumentException("invalid teamId"); }
        try { charId = Integer.parseInt(a[1]); } catch (NumberFormatException nfe) { throw new IllegalArgumentException("invalid charId"); }
        if (isOnline()) {
            net.sendSetSelection(teamId, charId);
            logger.accept("[cmd] selection sent: team=" + teamId + " char=" + charId);
        } else {
            logger.accept("[cmd] offline: selection skipped");
        }
    }

    private void doMap(String arg) throws IOException {
        if (arg.isEmpty()) throw new IllegalArgumentException("usage: /map <mapId>");
        int id;
        try { id = Integer.parseInt(arg.split("\\s+")[0]); }
        catch (NumberFormatException nfe) { throw new IllegalArgumentException("invalid mapId"); }
        if (isOnline()) {
            net.sendMapVote(id);
            logger.accept("[cmd] map vote sent: id=" + id);
        } else {
            logger.accept("[cmd] offline: map vote skipped");
        }
    }

    private void doSay(String arg) throws IOException {
        if (arg.isEmpty()) throw new IllegalArgumentException("usage: /say <message>");
        if (isOnline()) {
            net.sendChat(arg);
            logger.accept("[me] " + arg);
        } else {
            logger.accept("[local] " + arg);
        }
    }

    private void doMe(String arg) {
        if (arg.isEmpty()) throw new IllegalArgumentException("usage: /me <action>");
        // Local action message; keep it local for now (no usernames).
        logger.accept("* " + arg);
    }
}

