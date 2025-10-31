package com.fpsgame.server;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;

import com.fpsgame.common.FixedTickRunner;
import com.fpsgame.common.GameEnums;

/**
 * 서버측 게임 컨트롤러(최상위 조율자)
 * - RoundController/LobbyState/MapVoteManager를 보유하고 고정 틱 루프로 FSM을 구동
 * - 네트워크 브로드캐스트는 상위 계층(TcpServer/router)의 Events 리스너로 분리
 * - 팀 생존/투표 완료 여부 등은 외부 주입 가능한 조건(BooleanSupplier)로 판정
 */
public final class GameServer {

    /* ========================= 외부 이벤트 인터페이스 ========================= */
    /** 서버 진행 알림(상위 네트 계층이 브로드캐스트에 사용) */
    public interface Events {
        /** 페이즈 변경 */
        void onPhaseChanged(GameEnums.Phase phase);
        /** 카운트다운 갱신(초) */
        void onCountdown(int secondsLeft);
        /** 라운드 결과 */
        void onRoundResult(GameEnums.Team winner, int redScore, int blueScore, int roundNumber, boolean matchEnded);
    }

    /* ========================= 구성/상태 ========================= */
    private final int hz;                         // 고정 틱 주파수(Hz)
    private final FixedTickRunner loop;           // 틱 루프
    private final AtomicBoolean running = new AtomicBoolean(false);

    // 로비/맵투표/라운드 FSM 요소
    private final LobbyState lobby = new LobbyState();
    private final MapVoteManager vote = new MapVoteManager();
    private final RoundController fsm;

    // 이벤트 브로드캐스트 리스너(선택)
    private volatile Events events;

    // 외부 조건(기본값은 단순). 상위에서 주입 가능
    private volatile BooleanSupplier everyoneReadyCond = lobby::isEveryoneReady;
    private volatile BooleanSupplier voteCompleteCond = () -> vote.voterCount() > 0; // 데모: 1표 이상이면 진행
    private volatile BooleanSupplier redAliveCond = () -> true;   // TODO: 실제 서버 게임 상태 연동
    private volatile BooleanSupplier blueAliveCond = () -> true;  // TODO: 실제 서버 게임 상태 연동

    /* ========================= 생성 ========================= */
    /** @param hz 틱 주파수(Hz) 예: 30 */
    public GameServer(int hz) {
        if (hz < 1 || hz > 240) throw new IllegalArgumentException("invalid hz: " + hz);
        this.hz = hz;

        // FSM 생성(리스너로 이벤트 중계)
        this.fsm = new RoundController(new RoundController.Listener() {
            @Override public void onPhaseChanged(GameEnums.Phase newPhase) { Events ev = events; if (ev != null) ev.onPhaseChanged(newPhase); }
            @Override public void onCountdown(int secondsLeft) { Events ev = events; if (ev != null) ev.onCountdown(secondsLeft); }
            @Override public void onRoundResult(GameEnums.Team winner, int redScore, int blueScore, int roundNumber, boolean matchEnded) { Events ev = events; if (ev != null) ev.onRoundResult(winner, redScore, blueScore, roundNumber, matchEnded); }
        }, () -> everyoneReadyCond.getAsBoolean(), () -> voteCompleteCond.getAsBoolean(), () -> redAliveCond.getAsBoolean(), () -> blueAliveCond.getAsBoolean());

        // 고정 틱 루프 설정
        this.loop = new FixedTickRunner(hz, deltaMs -> fsm.update(deltaMs));
    }

    /* ========================= 라이프사이클 ========================= */
    /** 시작(중복 호출 무시) */
    public synchronized void start() {
        if (running.getAndSet(true)) return;
        fsm.resetToLobby();
        loop.start();
        log("started at " + hz + "Hz");
    }

    /** 정지 및 조인 */
    public synchronized void stop(long joinTimeoutMs) {
        if (!running.getAndSet(false)) return;
        loop.stopAndJoin(Math.max(0, joinTimeoutMs));
        log("stopped");
    }

    public boolean isRunning() { return running.get(); }

    /* ========================= 외부 API ========================= */
    /** 외부(라우터 등)에서 플레이어 진입 등록 */
    public void onPlayerJoin(int sessionId) { lobby.join(sessionId); }
    /** 외부(라우터 등)에서 플레이어 이탈 처리 */
    public void onPlayerLeave(int sessionId) { lobby.leave(sessionId); vote.revoke(sessionId); }
    /** 레디 상태 갱신 */
    public void setReady(int sessionId, boolean ready) { lobby.setReady(sessionId, ready); }
    /** 맵 투표 등록 */
    public void voteMap(int sessionId, GameEnums.MapId map) { vote.vote(sessionId, map); }
    /** 현재 투표 우승 맵 조회(동률 시 기본 맵) */
    public GameEnums.MapId currentVoteWinner() { return vote.winnerOrDefault(); }
    /** 현재 투표 인원 수 */
    public int voterCount() { return vote.voterCount(); }
    /** 투표 초기화(새 라운드/새 투표 시작 시) */
    public void resetVotes() { vote.reset(); }
    
    /** 현재 Phase 조회 */
    public GameEnums.Phase getPhase() { return fsm.getPhase(); }

    /** 이벤트 리스너 설정(null 허용) */
    public void setEvents(Events events) { this.events = events; }

    /** 외부 조건 주입(null이면 무시) */
    public void setConditions(BooleanSupplier everyoneReady, BooleanSupplier voteComplete,
                              BooleanSupplier redAlive, BooleanSupplier blueAlive) {
        if (everyoneReady != null) this.everyoneReadyCond = everyoneReady;
        if (voteComplete != null) this.voteCompleteCond = voteComplete;
        if (redAlive != null) this.redAliveCond = redAlive;
        if (blueAlive != null) this.blueAliveCond = blueAlive;
    }

    /* ========================= 유틸 ========================= */
    private static void log(String s) { System.out.println("[GameServer] " + s); }
}

