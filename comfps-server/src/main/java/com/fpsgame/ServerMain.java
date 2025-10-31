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
        private SessionRegistry registry;
        private final GameServer gameServer;
        
        // 게임 상태 관리
        private final LobbyState lobbyState;
        private final MapVoteManager voteManager;

        // 투표 완료 규칙: 전체 투표 또는 타임아웃
        private volatile long voteDeadlineMs = 0L;
        private volatile int voteExpected = 0;
        private volatile boolean voteDecided = false; // 맵 결정 완료 플래그

        LobbyHooks(SessionRegistry registry, GameServer gameServer) {
            this.registry = registry;
            this.gameServer = Objects.requireNonNull(gameServer, "gameServer");
            this.lobbyState = new LobbyState();
            this.voteManager = new MapVoteManager();
            
            // SessionRegistry에 LobbyState 설정 (registry가 null이 아닐 때만)
            if (registry != null) {
                registry.setLobbyState(lobbyState);
            }

            // GameServer FSM 조건을 서버 로비 게이트 규칙으로 대체
            installGameServerConditions();
        }
        
        // Registry 나중에 설정 가능하도록
        void setRegistry(SessionRegistry registry) {
            this.registry = Objects.requireNonNull(registry, "registry");
            registry.setLobbyState(lobbyState);
            // 레지스트리 설정 이후에도 다시 조건을 설치(레퍼런스 갱신)
            installGameServerConditions();
        }

        /**
         * GameServer에 엄격한 진행 조건을 주입하여 단일 인원 READY로 VOTE로 가지 않도록 한다.
         * - everyoneReady: 총원>=2, 모두 READY, 모든 플레이어 팀 선택 완료, 팀 밸런스 |red-blue|<=1
         * - voteComplete: LobbyHooks의 voteDecided 플래그
         */
        private void installGameServerConditions() {
            try {
                gameServer.setConditions(
                    // everyoneReady
                    () -> {
                        SessionRegistry reg = this.registry;
                        if (reg == null) return false;
                        int total = reg.size();
                        if (total < 2) return false; // 인원 부족
                        if (!lobbyState.isEveryoneReady()) return false; // 모두 레디 여부
                        int red = 0, blue = 0;
                        for (int sid : reg.getSessionIds()) {
                            int team = reg.getCharacterTeam(sid);
                            if (team < 0) return false; // 팀 미선택 존재
                            if (team == com.fpsgame.common.GameEnums.Team.RED.ordinal()) red++;
                            else if (team == com.fpsgame.common.GameEnums.Team.BLUE.ordinal()) blue++;
                        }
                        return Math.abs(red - blue) <= 1;
                    },
                    // voteComplete
                    () -> this.voteDecided,
                    // 생존 조건(임시로 항상 true)
                    () -> true,
                    () -> true
                );
            } catch (Throwable ignore) {}
        }

        @Override
        public void onChat(int fromSessionId, String text, DefaultServerRouter.Broadcaster bc) throws java.io.IOException {
            // 닉네임 추출 및 저장 (형식: [nickname] : message)
            if (text.startsWith("[") && text.contains("] : ")) {
                int endBracket = text.indexOf("]");
                if (endBracket > 1) {
                    String nickname = text.substring(1, endBracket);
                    registry.setNickname(fromSessionId, nickname);
                }
            }
            
            // 팀 채팅 필터링
            if (text.startsWith("[TEAM]")) {
                // 팀 채팅: 같은 팀에만 전송
                String cleanText = text.substring(6); // [TEAM] 제거
                int senderTeam = registry.getCharacterTeam(fromSessionId);
                
                registry.log("[CHAT] TEAM mode - sid=" + fromSessionId + " team=" + senderTeam + " msg=" + cleanText);
                
                for (int sid : registry.getSessionIds()) {
                    int receiverTeam = registry.getCharacterTeam(sid);
                    registry.log("[CHAT] Check sid=" + sid + " team=" + receiverTeam);
                    
                    if (receiverTeam == senderTeam) {
                        try {
                            bc.sendChatTo(sid, "[TEAM]" + cleanText);
                            registry.log("[CHAT] Sent to sid=" + sid);
                        } catch (Exception e) {
                            registry.log("[CHAT] Failed to send to sid=" + sid + ": " + e.getMessage());
                        }
                    }
                }
            } else if (text.startsWith("[ALL]")) {
                // 전체 채팅
                String cleanText = text.substring(5); // [ALL] 제거
                bc.broadcastChat(cleanText);
                registry.log("[CHAT] ALL chat from sid=" + fromSessionId);
            } else {
                // 기본: 전체 브로드캐스트
                bc.broadcastChat(text);
            }
        }

        @Override
        public void setReady(int sessionId, boolean ready) {
            // GameServer를 통한 READY 상태 갱신
            gameServer.setReady(sessionId, ready);
            lobbyState.setReady(sessionId, ready);
            
            int rc = countReadyPlayers();
            int total = registry.size();
            
            registry.log("[AGGREGATE] READY sid=" + sessionId + " -> " + ready + " (readyCount=" + rc + "/total=" + total + ")");
            
            // READY 상태 브로드캐스트
            try {
                registry.broadcastReadyStatus(rc, total);
                registry.log("[BC] READY_STATUS ready=" + rc + " total=" + total);
            } catch (Throwable ignore) {}
            
            // 시스템 채팅 브로드캐스트
            try {
                registry.broadcastSystemChat("[READY] ready=" + rc + " total=" + total);
            } catch (Throwable ignore) {}
            
            // 개별 알림
            registry.broadcastSystemChat("[SYSTEM] sid=" + sessionId + (ready ? " READY" : " UNREADY"));
            
            // 게이트 체크 및 이유 로깅
            if (!canAdvanceToVote(total)) {
                logGateReason(total, rc);
                return;
            }

            // VOTE 진입: 투표 초기화 및 타이머 설정
            try {
                gameServer.resetVotes();
            } catch (Throwable ignore) {}
            this.voteExpected = total;
            this.voteDeadlineMs = System.currentTimeMillis() + 10_000L; // 10초 타이머
            this.voteDecided = false; // 투표 잠금 해제

            registry.log("[PHASE] Enter VOTE: expectedVotes=" + voteExpected + " deadlineMs=" + voteDeadlineMs);
            try {
                registry.broadcastPhaseUpdate(com.fpsgame.common.GameEnums.Phase.VOTE.ordinal());
                registry.broadcastSystemChat("[PHASE] 맵 투표를 시작합니다! 모두 투표하거나 10초 후 자동 결정됩니다.");
            } catch (Exception e) {
                registry.log("[ERROR] Phase transition failed: " + e.getMessage());
            }

            // 타이머 스레드 시작(원샷)
            startVoteTimerOnce();
        }
        
        private int countReadyPlayers() {
            int count = 0;
            for (int sid : registry.getSessionIds()) {
                if (lobbyState.isReady(sid)) count++;
            }
            return count;
        }

        @Override
        public void setSelection(int sessionId, int team, int character) {
            registry.log("[AGGREGATE] SELECT sid=" + sessionId + " team=" + team + " char=" + character);
            
            // 선택 결과를 월드/웰컴에 반영 (sendWelcomeTo에서 기존 값 처리)
            try { 
                registry.createOrUpdateCharacter(sessionId, team, character);
                registry.log("[UPDATE] World character created/updated for sid=" + sessionId);
            } catch (Throwable t) {
                registry.log("[ERROR] World character update failed for sid=" + sessionId + ": " + t);
            }
            
            // 해당 플레이어에게 웰컴 프레임 갱신 (기존 값 유지 로직 포함)
            try { 
                registry.sendWelcomeTo(sessionId, team, character);
                int finalTeam = registry.getCharacterTeam(sessionId);
                int finalChar = registry.getCharacterCharacter(sessionId);
                registry.log("[BC] WELCOME sent to sid=" + sessionId + " with team=" + finalTeam + " char=" + finalChar);
            } catch (Throwable t) {
                registry.log("[ERROR] Welcome send failed to sid=" + sessionId + ": " + t);
            }
            
            // 팀 슬롯 동기화: 모든 클라이언트에게 현재 플레이어 상태 브로드캐스트
            try {
                int readyCount = countReadyPlayers();
                int total = registry.size();
                registry.broadcastReadyStatus(readyCount, total); // 플레이어 리스트 포함
                registry.log("[BC] Team slots synced after selection (ready=" + readyCount + "/" + total + ")");
            } catch (Throwable t) {
                registry.log("[ERROR] Team slots sync failed: " + t);
            }
        }

        @Override
        public void registerMapVote(int sessionId, int mapId) {
            // 이미 맵이 결정되었으면 추가 투표 거부
            if (voteDecided) {
                registry.log("[VOTE] Rejected vote from sid=" + sessionId + " (already decided)");
                return;
            }

            // GameServer를 통한 맵 투표 처리
            com.fpsgame.common.GameEnums.MapId mapEnum = convertMapId(mapId);
            gameServer.voteMap(sessionId, mapEnum);
            registry.log("[AGGREGATE] VOTE sid=" + sessionId + " map=" + mapId + " (votes=" + gameServer.voterCount() + ")");
            
            // 완료 조건 검사: 전원 투표 또는 타임아웃 경과
            boolean allVoted = false;
            try { allVoted = gameServer.voterCount() >= voteExpected && voteExpected > 0; } catch (Throwable ignore) {}
            boolean timeout = System.currentTimeMillis() >= voteDeadlineMs;
            if (allVoted || timeout) {
                decideAndBroadcastMap();
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

        private boolean canAdvanceToVote(int total) {
            if (total < 2) return false; // 인원 부족
            if (!lobbyState.isEveryoneReady()) return false; // 준비 미완료
            // 팀/선택 확인 및 밸런스 체크
            int red = 0, blue = 0;
            for (int sid : registry.getSessionIds()) {
                int team = registry.getCharacterTeam(sid);
                if (team < 0) return false; // 팀 미선택
                if (team == com.fpsgame.common.GameEnums.Team.RED.ordinal()) red++;
                else if (team == com.fpsgame.common.GameEnums.Team.BLUE.ordinal()) blue++;
            }
            return Math.abs(red - blue) <= 1;
        }

        private void logGateReason(int total, int readyCount) {
            if (total < 2) { registry.log("[GATE] Not enough players: total=" + total); return; }
            if (readyCount != total) { registry.log("[GATE] Not everyone ready: " + readyCount + "/" + total); return; }
            int red = 0, blue = 0;
            for (int sid : registry.getSessionIds()) {
                int team = registry.getCharacterTeam(sid);
                if (team < 0) { registry.log("[GATE] Missing team selection for sid=" + sid); return; }
                if (team == com.fpsgame.common.GameEnums.Team.RED.ordinal()) red++;
                else if (team == com.fpsgame.common.GameEnums.Team.BLUE.ordinal()) blue++;
            }
            if (Math.abs(red - blue) > 1) {
                registry.log("[GATE] Unbalanced teams: red=" + red + " blue=" + blue);
            }
        }

        private synchronized void startVoteTimerOnce() {
            final long dl = this.voteDeadlineMs;
            // 스레드 하나만
            new Thread(() -> {
                long now = System.currentTimeMillis();
                long wait = Math.max(0L, dl - now);
                try { Thread.sleep(wait); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
                // 타임아웃 시점에서 아직 VOTE 페이즈로 간주하고 맵 결정
                decideAndBroadcastMap();
            }, "VoteDeadlineTimer").start();
        }

        private synchronized void decideAndBroadcastMap() {
            // 중복 실행 방지
            if (voteDecided) {
                registry.log("[DECIDE] Already decided, skipping duplicate call");
                return;
            }
            voteDecided = true; // 잠금

            com.fpsgame.common.GameEnums.MapId winnerMap = gameServer.currentVoteWinner();
            int winner = winnerMap.ordinal();
            int[] wh = dimsForMap(winner);

            registry.log("[DECIDE] Map winner: " + winner + " (" + winnerMap.name() + ")");

            registry.setWorldSize(wh[0], wh[1]);
            registry.setMapId(winner);
            registry.log("[UPDATE] World size set to " + wh[0] + "x" + wh[1] + " for map=" + winner);

            try {
                registry.broadcastSystemChat("[SYSTEM] mapSelected=" + winner + ", World=" + wh[0] + "x" + wh[1]);
                registry.log("[BC] Map selection broadcast sent");
            } catch (Throwable t) {
                registry.log("[ERROR] Map selection broadcast failed: " + t);
            }

            // Phase 전환은 GameServer FSM(voteComplete 조건)에서 처리
            try { registry.broadcastSystemChat("[PHASE] 투표가 완료되었습니다. 곧 시작합니다!"); } catch (Throwable ignore) {}
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
        
        // LobbyHooks 생성 (SessionRegistry는 나중에 설정)
        LobbyHooks hooks = new LobbyHooks(null, gameServer);
        
        // LobbyHooks를 사용하는 router로 SessionRegistry 생성
        DefaultServerRouter router = new DefaultServerRouter(hooks);
        SessionRegistry registry = new SessionRegistry(router);
        
        // LobbyHooks에 registry 설정
        hooks.setRegistry(registry);
        
        TcpServer server = new TcpServer(port, hooks, registry, 0, bind);
        
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

        // 스냅샷 동기화 루프 준비
        PlayerSyncService sync = new PlayerSyncService();
        SessionRegistry serverReg = server.getRegistry();
        serverReg.setSyncService(sync);
        int worldW = 3000, worldH = 2000;
        serverReg.setWorldSize(worldW, worldH);
        sync.setBounds(0, worldW, 0, worldH);
        serverReg.startSnapshotLoop(20.0);

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





