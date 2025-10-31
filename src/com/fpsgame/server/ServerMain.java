package com.fpsgame.server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Objects;

/** 서버 메인 진입점(주석 한국어 정리) */
public final class ServerMain {

    private ServerMain() {}

    /** 로비 훅 구현(선택/투표/레디 처리) */
    static final class LobbyHooks implements DefaultServerRouter.Hooks {
        // 세션 레지스트리 및 게임 서버 참조
        private SessionRegistry registry; // 지연 주입
        private final GameServer gameServer;
        
        // 게임 상태 관리
        private final LobbyState lobbyState;
        private final MapVoteManager voteManager;

        LobbyHooks(GameServer gameServer) {
            this.gameServer = Objects.requireNonNull(gameServer, "gameServer");
            this.lobbyState = new LobbyState();
            this.voteManager = new MapVoteManager();
        }

        /** TcpServer 생성 이후 레지스트리를 주입한다. */
        public void setRegistry(SessionRegistry registry) { this.registry = Objects.requireNonNull(registry, "registry"); }

        @Override
        public void onChat(int fromSessionId, String text, DefaultServerRouter.Broadcaster bc) throws java.io.IOException {
            //
            bc.broadcastChat("[" + fromSessionId + "] " + text);
        }

        @Override
        public void setReady(int sessionId, boolean ready) {
            // GameServer를 통한 READY 상태 갱신
            gameServer.setReady(sessionId, ready);
            lobbyState.setReady(sessionId, ready);
            
            int rc = countReadyPlayers();
            int total = registry != null ? registry.size() : 0;
            
            if (registry != null) registry.log("[AGGREGATE] READY sid=" + sessionId + " -> " + ready + " (readyCount=" + rc + "/total=" + total + ")");
            
            // READY 상태 브로드캐스트
            try {
                if (registry != null) {
                    registry.broadcastReadyStatus(rc, total);
                    registry.log("[BC] READY_STATUS ready=" + rc + " total=" + total);
                }
            } catch (Throwable ignore) {}
            
            // 시스템 채팅 브로드캐스트
            try {
                if (registry != null) registry.broadcastSystemChat("[READY] ready=" + rc + " total=" + total);
            } catch (Throwable ignore) {}
            
            // 개별 알림
            if (registry != null) registry.broadcastSystemChat("[SYSTEM] sid=" + sessionId + (ready ? " READY" : " UNREADY"));
        }
        
        private int countReadyPlayers() {
            int count = 0;
            if (registry == null) return 0;
            for (int sid : registry.getSessionIds()) {
                if (lobbyState.isReady(sid)) count++;
            }
            return count;
        }

        @Override
        public void setSelection(int sessionId, int team, int character) {
            if (registry != null) registry.log("[AGGREGATE] SELECT sid=" + sessionId + " team=" + team + " char=" + character);
            
            // 선택 결과를 월드/웰컴에 반영
            try { 
                if (registry != null) {
                    registry.createOrUpdateCharacter(sessionId, team, character);
                    registry.log("[UPDATE] World character created/updated for sid=" + sessionId);
                }
            } catch (Throwable t) {
                if (registry != null) registry.log("[ERROR] World character update failed for sid=" + sessionId + ": " + t);
            }
            
            // 해당 플레이어에게 웰컴 프레임 갱신
            try { 
                if (registry != null) {
                    registry.sendWelcomeTo(sessionId, team, character);
                    registry.log("[BC] WELCOME sent to sid=" + sessionId);
                }
            } catch (Throwable t) {
                if (registry != null) registry.log("[ERROR] Welcome send failed to sid=" + sessionId + ": " + t);
            }
        }

        @Override
        public void registerMapVote(int sessionId, int mapId) {
            // GameServer를 통한 맵 투표 처리
            com.fpsgame.common.GameEnums.MapId mapEnum = convertMapId(mapId);
            gameServer.voteMap(sessionId, mapEnum);
            if (registry != null) registry.log("[AGGREGATE] VOTE sid=" + sessionId + " map=" + mapId + " (votes=" + voteManager.voterCount() + ")");
            
            // 득표 집계 후 맵/월드 크기 반영 + 공지
            com.fpsgame.common.GameEnums.MapId winnerMap = gameServer.currentVoteWinner();
            int winner = winnerMap.ordinal();
            int[] wh = dimsForMap(winner);
            
            if (registry != null) registry.log("[DECIDE] Map winner: " + winner + " (" + winnerMap.name() + ")");
            
            if (registry != null) {
                registry.setWorldSize(wh[0], wh[1]);
                registry.setMapId(winner);
                registry.log("[UPDATE] World size set to " + wh[0] + "x" + wh[1] + " for map=" + winner);
            }
            
            try { 
                if (registry != null) {
                    registry.broadcastSystemChat("[SYSTEM] mapSelected=" + winner + ", World=" + wh[0] + "x" + wh[1]);
                    registry.log("[BC] Map selection broadcast sent");
                }
            } catch (Throwable t) {
                if (registry != null) registry.log("[ERROR] Map selection broadcast failed: " + t);
            }
        }
        
        private com.fpsgame.common.GameEnums.MapId convertMapId(int mapId) {
            return switch (mapId) {
                case 0 -> com.fpsgame.common.GameEnums.MapId.TERMINAL;
                case 1 -> com.fpsgame.common.GameEnums.MapId.NEON_CITY;
                case 2 -> com.fpsgame.common.GameEnums.MapId.FOREST_OUTPOST;
                default -> com.fpsgame.common.GameEnums.MapId.defaultMap();
            };
        }

        private int[] dimsForMap(int mapId) {
            return switch (mapId) {
                case 1 -> new int[]{2800, 1800}; // NEON_CITY
                case 2 -> new int[]{3200, 2200}; // FOREST_OUTPOST
                default -> new int[]{3000, 2000}; // TERMINAL
            };
        }

        @Override
        public void onClientBye(int sessionId) {
            // GameServer를 통한 플레이어 이탈 처리
            gameServer.onPlayerLeave(sessionId);
        }

        @Override
        public int currentPhase() {
            // GameServer의 현재 Phase를 반환
            return gameServer.getPhase().ordinal();
        }
        
        com.fpsgame.common.GameEnums.Phase getPhase() {
            return gameServer.getPhase();
        }

        /**
         * 모두 준비 + 팀 밸런스 OK + 2인 이상 판정
         * 캐릭터가 미선택인 세션이 하나라도 있으면 false 처리
         */
        public boolean isEveryoneReadyAndTeamsBalanced() {
            if (registry == null) return false;
            int total = registry.size();
            if (total < 2) return false;
            if (!lobbyState.isEveryoneReady()) return false;
            int red = 0, blue = 0;
            for (int sid : registry.getSessionIds()) {
                com.fpsgame.common.character.Character ch = registry.getCharacter(sid);
                if (ch == null || ch.getTeam() == null) return false;
                com.fpsgame.common.GameEnums.Team t = ch.getTeam();
                if (t == com.fpsgame.common.GameEnums.Team.RED) red++;
                else if (t == com.fpsgame.common.GameEnums.Team.BLUE) blue++;
            }
            return Math.abs(red - blue) <= 1;
        }
    }

    public static void main(String[] args) throws Exception {
        // UTF-8 콘솔 시도(옵션)
        try { com.fpsgame.common.Utf8.installConsoleUtf8(); } catch (Throwable ignore) {}
        // 기본 포트/바인드
        int port = 7777;
        String bind = null; // null이면 AnyAddr

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--port":
                case "-p":
                    if (i + 1 >= args.length) usageAndExit();
                    port = Integer.parseInt(args[++i]);
                    break;
                case "--bind":
                case "-b":
                    if (i + 1 >= args.length) usageAndExit();
                    bind = args[++i];
                    break;
                case "--help":
                case "-h":
                    usageAndExit();
                    break;
                default:
                    System.err.println("Unknown arg: " + args[i]);
                    usageAndExit();
            }
        }

    // 게임 서버 생성 및 시작
    GameServer gameServer = new GameServer(30); // 30Hz 고정 틱

    // 훅 생성(Registry는 서버 생성 후 주입)
    LobbyHooks hooks = new LobbyHooks(gameServer);
    TcpServer server = new TcpServer(port, hooks, 0, bind);
    SessionRegistry registry = server.getRegistry();
    hooks.setRegistry(registry);

    // 페이즈6 규칙: 모두 준비 + 팀 밸런스 + 2인 이상일 때 VOTE로 진입
    gameServer.setConditions(
        hooks::isEveryoneReadyAndTeamsBalanced,
        null,
        null,
        null
    );
        
        // 게임 서버 이벤트 리스너 설정 (브로드캐스트 자동화)
        gameServer.setEvents(new GameServer.Events() {
            @Override public void onPhaseChanged(com.fpsgame.common.GameEnums.Phase phase) {
                try {
                    new DefaultServerRouter.Broadcaster(registry).broadcastPhase(phase.ordinal());
                    System.out.println("[GameServer] Phase changed to " + phase);
                } catch (Exception e) {
                    System.err.println("[GameServer] Phase broadcast failed: " + e);
                }
            }
            
            @Override public void onCountdown(int secondsLeft) {
                try {
                    new DefaultServerRouter.Broadcaster(registry).broadcastCountdown(secondsLeft);
                } catch (Exception e) {
                    System.err.println("[GameServer] Countdown broadcast failed: " + e);
                }
            }
            
            @Override public void onRoundResult(com.fpsgame.common.GameEnums.Team winner, int redScore, int blueScore, int roundNumber, boolean matchEnded) {
                try {
                    new DefaultServerRouter.Broadcaster(registry).broadcastRoundResult(
                        winner == null ? -1 : winner.ordinal(),
                        redScore, blueScore, matchEnded
                    );
                    System.out.println("[GameServer] Round " + roundNumber + " result: " + (winner == null ? "DRAW" : winner) + 
                                     " (RED:" + redScore + " BLUE:" + blueScore + ")" + (matchEnded ? " MATCH END" : ""));
                } catch (Exception e) {
                    System.err.println("[GameServer] Round result broadcast failed: " + e);
                }
            }
        });
        
    // 게임 서버 시작
    gameServer.start();

    // GameServer 등록
    registry.setGameServer(gameServer);

        // 간단 로거
        registry.setLogger(msg -> System.out.println("[Server] " + msg));

        // 서버 시작 및 공지
        server.start();
        registry.broadcastSystemChat("[SYSTEM] FPS Server started on port " + server.getPort());

    // 스냅샷 동기화 루프 준비(동일 Registry 사용)
    PlayerSyncService sync = new PlayerSyncService();
    registry.setSyncService(sync);
    int worldW = 3000, worldH = 2000;
    registry.setWorldSize(worldW, worldH);
    sync.setBounds(0, worldW, 0, worldH);
    registry.startSnapshotLoop(20.0);

        // 종료 훅
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try { server.close(); } catch (Exception ignore) {}
        }, "shutdown-hook"));

        //
        System.out.println("Commands:");
        System.out.println("  say <msg>     - broadcast admin chat");
        System.out.println("  phase <code>  - broadcast phase update");
        System.out.println("  countdown <s> - broadcast countdown");
        System.out.println("  stop          - stop server");
        System.out.println();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                if (line.equalsIgnoreCase("stop") || line.equalsIgnoreCase("quit")) {
                    break;
                } else if (line.startsWith("say ")) {
                    String msg = line.substring(4);
                    registry.broadcastSystemChat("[ADMIN] " + msg);
                } else if (line.startsWith("phase ")) {
                    try {
                        int code = Integer.parseInt(line.substring(6).trim());
                        //
                        registry.getClass(); //
                        new DefaultServerRouter.Broadcaster(registry).broadcastPhase(code);
                    } catch (Exception e) {
                        System.out.println("  phase <code>  - broadcast phase update");
                    }
                } else if (line.startsWith("countdown ")) {
                    try {
                        int sec = Integer.parseInt(line.substring(10).trim());
                        new DefaultServerRouter.Broadcaster(registry).broadcastCountdown(sec);
                    } catch (Exception e) {
                        System.out.println("  countdown <s> - broadcast countdown");
                    }
                } else {
                    System.out.println("Unknown command: " + line);
                }
            }
        } catch (Exception e) {
            System.err.println("Console loop error: " + e);
        } finally {
            server.close();
        }
    }

    private static void usageAndExit() {
        System.out.println("Usage: java com.fpsgame.server.ServerMain [--port|-p <port>] [--bind|-b <addr>]");
        System.exit(1);
    }
}





