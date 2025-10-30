package com.fpsgame.server;

import com.fpsgame.common.ProjectilesV2;
import com.fpsgame.common.Protocol;
import com.fpsgame.common.Vec2;
import com.fpsgame.common.character.Character;
import com.fpsgame.common.character.CharacterFactory;
import com.fpsgame.common.character.projectile.Projectile;
import com.fpsgame.common.character.projectile.ProjectileManager;
import java.io.DataOutput;
import java.io.IOException;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/** Session registry, routing and broadcast helpers (ASCII only). */
public class SessionRegistry implements DefaultServerRouter.ServerContext {

    // Id/source/state
    private final AtomicInteger idGen = new AtomicInteger(1);
    private final ConcurrentHashMap<Integer, ServerSession> sessions = new ConcurrentHashMap<>();
    private final DefaultServerRouter router;

    @FunctionalInterface public interface Logger { void log(String msg); }
    private volatile Logger logger = (msg) -> System.out.println("[Server] " + msg);

    public SessionRegistry(DefaultServerRouter router) { this.router = Objects.requireNonNull(router, "router"); }

    // Snapshot service
    private volatile PlayerSyncService syncService;
    private volatile boolean snapshotRunning = false;
    private Thread snapshotThread;

    // Keep last selection (team/character) for re-sending WELCOME(v2)
    private final ConcurrentHashMap<Integer, Sel> lastSelection = new ConcurrentHashMap<>();
    private static final class Sel { final int team, character; Sel(int t,int c){ team=t; character=c; } }

    public void setSyncService(PlayerSyncService sync) {
        this.syncService = sync;
        try {
            if (sync != null) {
                sync.setCharacterProvider(this::getCharacter);
            }
        } catch (Throwable ignore) {}
    }

    // Keep created characters per session (server-side authoritative objects; minimal for now)
    private final ConcurrentHashMap<Integer, Character> characters = new ConcurrentHashMap<>();

    /**
     * Create or update a Character instance for a session id using the current world size as spawn hint.
     * This does not yet affect SNAPSHOT flow but prepares server-side state for future integration.
     */
    public Character createOrUpdateCharacter(int sessionId, int teamIdx, int characterIdx) {
        com.fpsgame.common.GameEnums.Team[] teams = com.fpsgame.common.GameEnums.Team.values();
        com.fpsgame.common.GameEnums.Team team = teams[(teamIdx < 0 || teamIdx >= teams.length) ? 0 : teamIdx];
        float sx = (team == com.fpsgame.common.GameEnums.Team.RED) ? (worldW * 0.1f) : (worldW * 0.9f);
        float sy = (worldH * 0.5f);
        Vec2 spawn = new Vec2(sx, sy);
        Character ch = CharacterFactory.create(characterIdx, team, spawn);
        characters.put(sessionId, ch);
        // Also align PlayerSyncService position to spawn
        PlayerSyncService s = syncService;
        if (s != null) {
            try { s.setPosition(sessionId, spawn.x, spawn.y); } catch (Throwable ignore) {}
        }
        return ch;
    }

    /** Get character for session (nullable). */
    public Character getCharacter(int sessionId) { return characters.get(sessionId); }

    /** Start snapshot loop at given Hz. */
    public synchronized void startSnapshotLoop(double hz) {
        if (snapshotRunning) return;
        if (hz <= 0.1) hz = 20.0;
        final long nanosPerTick = (long) Math.max(1, 1_000_000_000L / hz);
        final float dt = (float) (1.0 / hz);
        snapshotRunning = true;
        snapshotThread = new Thread(() -> {
            long next = System.nanoTime();
            while (snapshotRunning) {
                long now = System.nanoTime();
                long sleepNs = next - now;
                if (sleepNs > 0) {
                    try { Thread.sleep(sleepNs / 1_000_000L, (int)(sleepNs % 1_000_000L)); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                } else {
                    try {
                        PlayerSyncService s = syncService;
                        if (s != null) {
                            s.tick(dt);
                            byte[] payload = s.buildSnapshotFramePayload();
                            broadcast(Protocol.Opcode.SNAPSHOT, payload);
                        }
                    } catch (Exception e) {
                        log("snapshot loop error: " + e.getMessage());
                    }
                    // Projectiles v2 broadcast (id/type/active/x/y)
                    try {
                        java.util.List<ProjectilesV2.Entry> plist = new java.util.ArrayList<>();
                        int __id = 1;
                        for (Projectile p : ProjectileManager.getInstance().getProjectiles()) {
                            Vec2 pos = p.getPosition();
                            int type = ProjectilesV2.typeOf(p);
                            boolean active = p.isActive();
                            plist.add(new ProjectilesV2.Entry(__id++, type, active, pos.x, pos.y));
                        }
                        byte[] pbin = ProjectilesV2.build(plist);
                        broadcast(Protocol.PROJECTILES, pbin);
                    } catch (Exception ignore) {}
                    next += nanosPerTick;
                    if (System.nanoTime() - next > nanosPerTick * 5) next = System.nanoTime() + nanosPerTick;
                }
            }
        }, "SnapshotLoop-" + (int)Math.round(hz) + "Hz");
        snapshotThread.setDaemon(true);
        snapshotThread.start();
    }

    public synchronized void stopSnapshotLoop() {
        snapshotRunning = false;
        Thread t = snapshotThread;
        if (t != null) {
            try { t.join(1000); } catch (InterruptedException ignore) { Thread.currentThread().interrupt(); }
            snapshotThread = null;
        }
    }

    public void setLogger(Logger logger) { if (logger != null) this.logger = logger; }

    // DefaultServerRouter.ServerContext
    @Override public DataOutput getSender(int sessionId) {
        ServerSession s = sessions.get(sessionId);
        if (s == null) throw new IllegalStateException("No session: " + sessionId);
        DataOutput out = s.getDataOutput();
        if (out == null) throw new IllegalStateException("Session not started/closed: " + sessionId);
        return out;
    }

    @Override public void broadcast(byte opcode, byte[] payload) throws IOException {
        ArrayList<Map.Entry<Integer, ServerSession>> list = new ArrayList<>(sessions.entrySet());
        for (Map.Entry<Integer, ServerSession> e : list) {
            ServerSession s = e.getValue();
            try {
                s.send(opcode, payload);
            } catch (SocketException se) {
                log("broadcast: session " + s.getSessionId() + " closed: " + se.getMessage());
                try { closeSession(s.getSessionId()); } catch (Exception ignore) {}
            } catch (IOException ioe) {
                log("broadcast I/O to " + s.getSessionId() + " failed: " + ioe.getMessage());
            }
        }
    }

    @Override public void closeSession(int sessionId) {
        ServerSession s = sessions.remove(sessionId);
        if (s != null) {
            try { s.close(); } catch (Exception ignore) {}
            log("session closed: " + sessionId + " (remain=" + sessions.size() + ")");
        }
        
        // GameServer에 플레이어 이탈 처리
        GameServer gs = gameServer;
        if (gs != null) {
            try { gs.onPlayerLeave(sessionId); } catch (Throwable ignore) {}
        }
        
        PlayerSyncService ss = syncService;
        if (ss != null) {
            try { ss.removePlayer(sessionId); } catch (Throwable ignore) {}
        }
    }
    
    /** GameServer 참조 (플레이어 등록용) */
    private volatile GameServer gameServer;
    
    public void setGameServer(GameServer gs) {
        this.gameServer = gs;
    }

    @Override public void log(String msg) { Logger l = this.logger; if (l != null) l.log(msg); }

    // Public API
    public ServerSession createAndStartSession(java.net.Socket socket) throws IOException {
        int id = idGen.getAndIncrement();
        ServerSession s = new ServerSession(id, socket, router, this);
        sessions.put(id, s);
        s.start();
        log("session opened: " + id + " (total=" + sessions.size() + ")");
        
        // GameServer에 플레이어 등록
        GameServer gs = gameServer;
        if (gs != null) {
            try { gs.onPlayerJoin(id); } catch (Throwable ignore) {}
        }
        
        PlayerSyncService ss = syncService;
        if (ss != null) { try { ss.addPlayer(id); } catch (Throwable ignore) {} }
        try { s.sendWelcome(id, 0, 0, worldW, worldH, mapId); } catch (Throwable ignore) {}
        return s;
    }

    public int size() { return sessions.size(); }
    public java.util.ArrayList<Integer> getSessionIds() { return new java.util.ArrayList<>(sessions.keySet()); }

    public void broadcastSystemChat(String text) {
        try {
            ArrayList<Map.Entry<Integer, ServerSession>> list = new ArrayList<>(sessions.entrySet());
            for (Map.Entry<Integer, ServerSession> e : list) {
                try { e.getValue().send(Protocol.CHAT, buildUtf8Payload(text)); }
                catch (IOException ioe) { log("broadcastSystemChat failed to " + e.getKey() + ": " + ioe.getMessage()); }
            }
        } catch (Exception ex) { log("broadcastSystemChat error: " + ex.getMessage()); }
    }
    
    public void broadcastReadyStatus(int ready, int total) {
        try {
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream(8);
            Protocol.putInt(baos, ready);
            Protocol.putInt(baos, total);
            byte[] payload = baos.toByteArray();
            
            ArrayList<Map.Entry<Integer, ServerSession>> list = new ArrayList<>(sessions.entrySet());
            for (Map.Entry<Integer, ServerSession> e : list) {
                try { e.getValue().send(Protocol.READY_STATUS, payload); }
                catch (IOException ioe) { log("broadcastReadyStatus failed to " + e.getKey() + ": " + ioe.getMessage()); }
            }
        } catch (Exception ex) { log("broadcastReadyStatus error: " + ex.getMessage()); }
    }

    private static byte[] buildUtf8Payload(String text) {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream(32);
        Protocol.putUtf8(baos, text == null ? "" : text);
        return baos.toByteArray();
    }

    // World/Map state reflected in WELCOME
    private volatile int worldW = 3000;
    private volatile int worldH = 2000;
    private volatile int mapId = 0;

    public void setWorldSize(int w, int h) {
        this.worldW = Math.max(1, w);
        this.worldH = Math.max(1, h);
        broadcastWelcomeAll();
    }

    public void setMapId(int mapId) {
        this.mapId = Math.max(0, mapId);
        broadcastWelcomeAll();
    }

    // Input forwarding
    public void handleInput(int sessionId, byte[] payload) {
        PlayerSyncService ss = syncService;
        if (ss != null) { try { ss.onInputFrame(sessionId, payload); } catch (Throwable ignore) {} }
    }

    // Welcome helpers
    public void sendWelcomeTo(int sessionId, int team, int character) {
        ServerSession s = sessions.get(sessionId);
        if (s == null) return;
        lastSelection.put(sessionId, new Sel(team, character));
        try { s.sendWelcome(sessionId, team, character, worldW, worldH, mapId); } catch (Throwable ignore) {}
    }

    private void broadcastWelcomeAll() {
        try {
            ArrayList<Map.Entry<Integer, ServerSession>> list = new ArrayList<>(sessions.entrySet());
            for (Map.Entry<Integer, ServerSession> e : list) {
                int sid = e.getKey();
                Sel sel = lastSelection.get(sid);
                int t = (sel == null ? 0 : sel.team);
                int c = (sel == null ? 0 : sel.character);
                try { e.getValue().sendWelcome(sid, t, c, worldW, worldH, mapId); } catch (Throwable ignore) {}
            }
        } catch (Throwable t) { log("broadcastWelcomeAll error: " + t.getMessage()); }
    }
}
