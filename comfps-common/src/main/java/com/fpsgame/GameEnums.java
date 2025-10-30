package com.fpsgame.common;

import java.util.Locale;

/**
 * 클라이언트/서버 공용 열거형과 규칙 모음
 * - 공용 규칙, 식별자, 문자열 매핑 유틸리티 포함
 */
public final class GameEnums {

    private GameEnums() {}

    // 매치/라운드 단계(서버 FSM/클라 HUD 공용)
    public enum Phase {
        LOBBY,          // 대기실(레디 수집)
        VOTE,           // 맵 투표
        COUNTDOWN,      // 라운드 시작 카운트다운
        ROUND_RUNNING,  // 라운드 진행
        ROUND_RESULT,   // 라운드 결과 표시
        MATCH_END       // 매치 종료(3선승)
    }

    // 팀
    public enum Team {
        RED("Red"),
        BLUE("Blue");

        private final String display;
        Team(String display) { this.display = display; }
        public String displayName() { return display; }
        public static Team fromName(String s) {
            if (s == null) return null;
            String n = s.trim().toLowerCase(Locale.ROOT);
            if (n.startsWith("r")) return RED;
            if (n.startsWith("b")) return BLUE;
            return null;
        }
    }

    // 캐릭터 식별자
    public enum CharacterId {
        RAVEN, PIPER, BULLDOG, SAGE, GHOST, SNIPER, TANK,
        GENERAL, TECHNICIAN, WILDCAT, SKULL, STEAM;

        public String displayName() {
            return switch (this) {
                case RAVEN -> "Raven";
                case PIPER -> "Piper";
                case BULLDOG -> "Bulldog";
                case SAGE -> "Sage";
                case GHOST -> "Ghost";
                case GENERAL -> "General";
                case TECHNICIAN -> "Technician";
                case WILDCAT -> "Wildcat";
                case SKULL -> "Skull";
                case STEAM -> "Steam";
                case SNIPER -> "Sniper";
                case TANK -> "Tank";
            };
        }

        public static CharacterId fromName(String s) {
            if (s == null) return null;
            String n = s.trim().toLowerCase(Locale.ROOT);
            for (CharacterId c : values()) {
                if (c.name().toLowerCase(Locale.ROOT).equals(n)) return c;
                if (c.displayName().toLowerCase(Locale.ROOT).equals(n)) return c;
            }
            // 약칭/부분 매칭
            if (n.startsWith("rav")) return RAVEN;
            if (n.startsWith("pip")) return PIPER;
            if (n.startsWith("bul")) return BULLDOG;
            if (n.startsWith("sa"))  return SAGE;
            if (n.startsWith("gho")) return GHOST;
            if (n.startsWith("gen")) return GENERAL;
            if (n.startsWith("tec")) return TECHNICIAN;
            if (n.startsWith("wil")) return WILDCAT;
            if (n.startsWith("sku")) return SKULL;
            if (n.startsWith("ste")) return STEAM;
            if (n.startsWith("sni")) return SNIPER;
            if (n.startsWith("tan")) return TANK;
            return null;
        }
    }

    // 맵 식별자
    public enum MapId {
        TERMINAL("Terminal"),
        NEON_CITY("Neon City"),
        FOREST_OUTPOST("Forest Outpost");

        private final String display;
        MapId(String display) { this.display = display; }
        public String displayName() { return display; }

        /** UI 문자열에서 MapId로 매핑 */
        public static MapId fromName(String s) {
            if (s == null) return null;
            String n = s.trim().toLowerCase(Locale.ROOT);
            for (MapId m : values()) {
                if (m.display.toLowerCase(Locale.ROOT).equals(n)) return m;
                if (m.name().toLowerCase(Locale.ROOT).equals(n)) return m;
            }
            if (n.startsWith("term"))   return TERMINAL;
            if (n.startsWith("neon"))   return NEON_CITY;
            if (n.startsWith("forest")) return FOREST_OUTPOST;
            return null;
        }

        /** 기본 맵(초기 화면/기본값) */
        public static MapId defaultMap() { return TERMINAL; }
    }

    // 규칙(공용 상수)
    public static final class Rules {
        private Rules() {}
        /** 최대 라운드 수 */
        public static final int MAX_ROUNDS = 5;
        /** 승리 조건(3선승 = Best of 5) */
        public static final int WINS_TO_TAKE_MATCH = 3;
        /** 라운드 시작 카운트다운(초) */
        public static final int ROUND_COUNTDOWN_SEC = 5;
    }
}

