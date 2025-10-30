package com.fpsgame.server;

import com.fpsgame.common.GameEnums;

import java.util.Objects;
import java.util.function.BooleanSupplier;

/**
 * 라운드/매치 진행을 담당하는 간단한 FSM 컨트롤러
 * - 상태: LOBBY → VOTE → COUNTDOWN → ROUND_RUNNING → ROUND_RESULT → MATCH_END
 * - 규칙: 5라운드 3선승(기본값, GameEnums.Rules 참조)
 * - 종료 조건: 팀 전멸 등 외부 BooleanSupplier로 판단
 * - 실제 브로드캐스트/로그는 상위(GameServer.Events 등)에서 처리
 */
public final class RoundController {

    /** 라운드 이벤트 콜백 인터페이스 */
    public interface Listener {
        /** 상태 변경 통지 */
        void onPhaseChanged(GameEnums.Phase newPhase);
        /** 카운트다운 남은 초 통지 */
        void onCountdown(int secondsLeft);
        /** 라운드 결과 통지 */
        void onRoundResult(GameEnums.Team winner, int redScore, int blueScore, int roundNumber, boolean matchEnded);
    }

    // 구성 --------------------------------------------------------------
    private final Listener listener;
    private final BooleanSupplier everyoneReady; // LOBBY→VOTE 조건
    private final BooleanSupplier voteComplete;  // VOTE→COUNTDOWN 조건
    private final BooleanSupplier redAlive;      // ROUND_RUNNING 종료 판단
    private final BooleanSupplier blueAlive;     // ROUND_RUNNING 종료 판단

    // 상태 --------------------------------------------------------------
    private GameEnums.Phase phase = GameEnums.Phase.LOBBY;
    private int roundNo = 1;      // 1..MAX_ROUNDS
    private int redScore = 0;
    private int blueScore = 0;
    private long countdownMs;     // COUNTDOWN 잔여(ms)
    private int lastCountdownSec = -1; // 중복 통지 방지

    // 규칙 --------------------------------------------------------------
    private static final int MAX_ROUNDS = GameEnums.Rules.MAX_ROUNDS;
    private static final int WINS_TO_TAKE_MATCH = GameEnums.Rules.WINS_TO_TAKE_MATCH;
    private static final int ROUND_COUNTDOWN_SEC = GameEnums.Rules.ROUND_COUNTDOWN_SEC;

    public RoundController(Listener listener,
                           BooleanSupplier everyoneReady,
                           BooleanSupplier voteComplete,
                           BooleanSupplier redAlive,
                           BooleanSupplier blueAlive) {
        this.listener = Objects.requireNonNull(listener, "listener");
        this.everyoneReady = Objects.requireNonNull(everyoneReady, "everyoneReady");
        this.voteComplete = Objects.requireNonNull(voteComplete, "voteComplete");
        this.redAlive = Objects.requireNonNull(redAlive, "redAlive");
        this.blueAlive = Objects.requireNonNull(blueAlive, "blueAlive");
    }

    // 제어 --------------------------------------------------------------
    /** 로비로 초기화(라운드/스코어 리셋) */
    public void resetToLobby() {
        roundNo = 1;
        redScore = 0;
        blueScore = 0;
        enter(GameEnums.Phase.LOBBY);
    }

    /** 고정 틱에서 호출. deltaMs는 경과(ms). */
    public void update(long deltaMs) {
        switch (phase) {
            case LOBBY:
                if (everyoneReady.getAsBoolean()) enter(GameEnums.Phase.VOTE);
                break;
            case VOTE:
                if (voteComplete.getAsBoolean()) startCountdown();
                break;
            case COUNTDOWN:
                tickCountdown(deltaMs);
                break;
            case ROUND_RUNNING:
                boolean rAlive = redAlive.getAsBoolean();
                boolean bAlive = blueAlive.getAsBoolean();
                if (!rAlive && !bAlive)      endRound(null);               // 동시 전멸 → 무승부
                else if (!rAlive)            endRound(GameEnums.Team.BLUE);
                else if (!bAlive)            endRound(GameEnums.Team.RED);
                break;
            case ROUND_RESULT:
                nextAfterResult();
                break;
            case MATCH_END:
                // 상위에서 resetToLobby() 호출 전까지 대기
                break;
            default:
                break;
        }
    }

    // 내부 로직 ---------------------------------------------------------
    private void startCountdown() {
        countdownMs = ROUND_COUNTDOWN_SEC * 1000L;
        lastCountdownSec = -1;
        enter(GameEnums.Phase.COUNTDOWN);
        notifyCountdown();
    }

    private void tickCountdown(long deltaMs) {
        countdownMs = Math.max(0, countdownMs - Math.max(0, deltaMs));
        notifyCountdown();
        if (countdownMs <= 0) enter(GameEnums.Phase.ROUND_RUNNING);
    }

    private void notifyCountdown() {
        int sec = (int) Math.ceil(countdownMs / 1000.0);
        if (sec != lastCountdownSec) {
            lastCountdownSec = sec;
            listener.onCountdown(sec);
        }
    }

    /** 라운드 종료 처리 및 결과 통지 */
    private void endRound(GameEnums.Team winner) {
        if (winner == GameEnums.Team.RED) redScore++;
        else if (winner == GameEnums.Team.BLUE) blueScore++;

        boolean matchEnded = (redScore >= WINS_TO_TAKE_MATCH) ||
                             (blueScore >= WINS_TO_TAKE_MATCH) ||
                             (roundNo >= MAX_ROUNDS);

        listener.onRoundResult(winner, redScore, blueScore, roundNo, matchEnded);
        enter(matchEnded ? GameEnums.Phase.MATCH_END : GameEnums.Phase.ROUND_RESULT);
    }

    /** 결과 표시 후 다음 라운드 준비 */
    private void nextAfterResult() {
        roundNo = Math.min(MAX_ROUNDS, roundNo + 1);
        enter(GameEnums.Phase.VOTE);
    }

    /** 상태 진입 공통 처리 */
    private void enter(GameEnums.Phase next) {
        if (this.phase == next) return;
        this.phase = next;
        listener.onPhaseChanged(next);
    }

    // Getter ------------------------------------------------------------
    public GameEnums.Phase getPhase() { return phase; }
    public int getRoundNo() { return roundNo; }
    public int getRedScore() { return redScore; }
    public int getBlueScore() { return blueScore; }
}

