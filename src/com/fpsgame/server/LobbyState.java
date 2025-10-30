package com.fpsgame.server;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lobby state management.
 * - Track player READY state by sessionId
 * - Quick check for "everyone ready"
 * - Thread-safe via ConcurrentHashMap
 */
public final class LobbyState {

    /** sessionId -> READY flag */
    private final ConcurrentHashMap<Integer, Boolean> readyMap = new ConcurrentHashMap<>();

    /** Register player (default READY=false). */
    public void join(int sessionId) {
        if (sessionId <= 0) return;
        readyMap.put(sessionId, Boolean.FALSE);
    }

    /** Player leaves. */
    public void leave(int sessionId) {
        readyMap.remove(sessionId);
    }

    /** Current player count. */
    public int size() {
        return readyMap.size();
    }

    /** Set READY for sessionId. */
    public void setReady(int sessionId, boolean ready) {
        if (!readyMap.containsKey(sessionId)) return;
        readyMap.put(sessionId, ready);
    }

    /** Get READY for sessionId. */
    public boolean isReady(int sessionId) {
        return Objects.equals(readyMap.get(sessionId), Boolean.TRUE);
    }

    /** Everyone ready (size>=1 and all true). */
    public boolean isEveryoneReady() {
        int n = readyMap.size();
        if (n <= 0) return false;
        for (Map.Entry<Integer, Boolean> e : readyMap.entrySet()) {
            if (!Boolean.TRUE.equals(e.getValue())) return false;
        }
        return true;
    }

    /** Reset all READY flags to false. */
    public void resetReady() {
        readyMap.replaceAll((k, v) -> Boolean.FALSE);
    }

    /** Reset all lobby state (clear). */
    public void resetAll() {
        readyMap.clear();
    }

    @Override
    public String toString() {
        return "LobbyState{size=" + size() + ", ready=" + readyMap + "}";
    }
}

