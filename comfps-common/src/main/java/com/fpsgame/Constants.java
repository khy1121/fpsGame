package com.fpsgame.common;

/**
 * Application-wide constants for the FPS game.
 * Centralizes magic numbers and configuration values.
 */
public final class Constants {

    private Constants() {
        throw new UnsupportedOperationException("Utility class");
    }

    /** Network constants */
    public static final class Network {
        public static final int DEFAULT_SERVER_PORT = 7777;
        public static final int DEFAULT_SOCKET_TIMEOUT_MS = 20_000;
        public static final int DEFAULT_CONNECT_TIMEOUT_MS = 3_000;
        public static final int TCP_NO_DELAY = 1; // boolean as int
        
        private Network() {}
    }

    /** Game constants */
    public static final class Game {
        public static final float ULTIMATE_FULL_CHARGE = 100f;
        public static final float DEFAULT_WORLD_WIDTH = 3000f;
        public static final float DEFAULT_WORLD_HEIGHT = 2000f;
        public static final int DEFAULT_TICK_RATE_HZ = 30;
        public static final int DEFAULT_PLAYER_HP = 100;
        
        private Game() {}
    }

    /** UI constants */
    public static final class UI {
        public static final int SYSTEM_MESSAGE_RGB = 0x787878; // Color(120, 120, 120)
        public static final int DEFAULT_WINDOW_WIDTH = 760;
        public static final int DEFAULT_WINDOW_HEIGHT = 520;
        public static final int DEFAULT_FONT_SIZE = 13;
        
        private UI() {}
    }

    /** Time constants (milliseconds) */
    public static final class Time {
        public static final long ONE_SECOND_MS = 1000L;
        public static final long THREAD_JOIN_TIMEOUT_MS = 1000L;
        public static final long NANOSECONDS_PER_MILLISECOND = 1_000_000L;
        
        private Time() {}
    }
}
