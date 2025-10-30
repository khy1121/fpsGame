package com.fpsgame.client.model;

import javax.swing.SwingUtilities;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * ClientPhaseBus
 * ------------------------------------------------------------
 * 클라이언트 전역 페이즈/라운드 이벤트 버스.
 * - Net 수신부(어댑터)에서 호출 → EDT로 모든 리스너에게 브로드캐스트.
 * - UI/HUD/로비/투표/선택 패널 등에서 안전하게 구독.
 *
 * 이벤트 종류
 *  - PHASE_UPDATE : PhaseState 전체 스냅샷(라운드, 스코어, 생존, 남은 시간 등)
 *  - COUNTDOWN    : 남은 초
 *  - ROUND_RESULT : 라운드 결과 DTO
 *  - READY_TOGGLE : 로비에서 특정 세션의 준비 상태 변경
 *
 * 사용 예시:
 *   ClientPhaseBus bus = ClientPhaseBus.get();
 *   bus.addListener(new ClientPhaseBus.PhaseListener() {
 *       @Override public void onPhaseUpdate(PhaseState s) { ... }
 *       @Override public void onCountdown(int sec) { ... }
 *       @Override public void onRoundResult(RoundResult r) { ... }
 *       @Override public void onReadyToggle(int sessionId, boolean ready) { ... }
 *   });
 *
 * 스레드
 * - publish* 메서드는 어떤 스레드에서든 호출 가능.
 * - 모든 리스너 콜백은 EDT에서 실행 보장(SwingUtilities.invokeLater).
 */
public final class ClientPhaseBus {

    // --------------------- 싱글톤 ---------------------

    private static final ClientPhaseBus INSTANCE = new ClientPhaseBus();

    /** 전역 싱글톤 인스턴스를 반환 */
    public static ClientPhaseBus get() {
        return INSTANCE;
    }

    private ClientPhaseBus() {}

    // --------------------- 리스너 ---------------------

    /** 페이즈/라운드 관련 콜백 인터페이스 */
    public interface PhaseListener {
        /** 페이즈/라운드/스코어/생존/남은 시간 등 전체 스냅샷 */
        default void onPhaseUpdate(PhaseState state) {}
        /** 카운트다운 남은 초(>0), 0 또는 음수면 UI에서 비표시 처리 권장 */
        default void onCountdown(int sec) {}
        /** 라운드 결과 */
        default void onRoundResult(RoundResult result) {}
        /** 로비 READY 토글(특정 세션의 준비 상태) */
        default void onReadyToggle(int sessionId, boolean ready) {}
    }

    private final CopyOnWriteArrayList<PhaseListener> listeners = new CopyOnWriteArrayList<>();

    /** 리스너 등록(중복 허용 안 함) */
    public void addListener(PhaseListener l) {
        if (l == null) return;
        if (!listeners.contains(l)) listeners.add(l);
    }

    /** 리스너 해제(없으면 무시) */
    public void removeListener(PhaseListener l) {
        if (l == null) return;
        listeners.remove(l);
    }

    /** 모든 리스너 제거(디버그/테스트 용도) */
    public void clearListeners() {
        listeners.clear();
    }

    // --------------------- DTO ---------------------

    /** 전체 페이즈 상태 스냅샷 */
    public static final class PhaseState {
        public final String phase;  // 예: LOBBY, VOTE, SELECT, COUNTDOWN, RUNNING, RESULT ...
        public final int round;     // 현재 라운드(1-base 또는 0-base는 서버 규칙에 따름)
        public final int scoreA;    // 팀 A(RED 등) 라운드 점수
        public final int scoreB;    // 팀 B(BLUE 등) 라운드 점수
        public final int aliveA;    // 팀 A 생존 수
        public final int aliveB;    // 팀 B 생존 수
        public final int remainSec; // 남은 초(카운트다운/라운드 타이머). 미정/비표시는 -1 권장.

        public PhaseState(String phase, int round, int scoreA, int scoreB, int aliveA, int aliveB, int remainSec) {
            this.phase = nvl(phase, "UNKNOWN");
            this.round = round;
            this.scoreA = scoreA;
            this.scoreB = scoreB;
            this.aliveA = aliveA;
            this.aliveB = aliveB;
            this.remainSec = remainSec;
        }

        @Override public String toString() {
            return "PhaseState{phase=" + phase + ", round=" + round +
                    ", scoreA=" + scoreA + ", scoreB=" + scoreB +
                    ", aliveA=" + aliveA + ", aliveB=" + aliveB +
                    ", remainSec=" + remainSec + "}";
        }
    }

    /** 라운드 결과 DTO */
    public static final class RoundResult {
        public final String winnerTeam; // "RED"|"BLUE"|"DRAW"|그 외 텍스트
        public final String reason;     // "TEAM_WIPE"|"TIMEOUT"|"MATCH_END"|기타

        public RoundResult(String winnerTeam, String reason) {
            this.winnerTeam = nvl(winnerTeam, "UNKNOWN");
            this.reason = nvl(reason, "");
        }

        @Override public String toString() {
            return "RoundResult{winnerTeam=" + winnerTeam + ", reason=" + reason + "}";
        }
    }

    private static String nvl(String s, String def) { return (s == null || s.isEmpty()) ? def : s; }

    // --------------------- Publish (EDT 디스패치) ---------------------

    /** PHASE_UPDATE 브로드캐스트 */
    public void publishPhase(PhaseState state) {
        final PhaseState st = Objects.requireNonNull(state, "state");
        runEdt(() -> {
            for (PhaseListener l : listeners) {
                try { l.onPhaseUpdate(st); } catch (Throwable t) { t.printStackTrace(); }
            }
        });
    }

    /** COUNTDOWN 브로드캐스트 */
    public void publishCountdown(int sec) {
        runEdt(() -> {
            for (PhaseListener l : listeners) {
                try { l.onCountdown(sec); } catch (Throwable t) { t.printStackTrace(); }
            }
        });
    }

    /** ROUND_RESULT 브로드캐스트 */
    public void publishRoundResult(RoundResult result) {
        final RoundResult rr = Objects.requireNonNull(result, "result");
        runEdt(() -> {
            for (PhaseListener l : listeners) {
                try { l.onRoundResult(rr); } catch (Throwable t) { t.printStackTrace(); }
            }
        });
    }

    /** READY_TOGGLE 브로드캐스트 */
    public void publishReadyToggle(int sessionId, boolean ready) {
        runEdt(() -> {
            for (PhaseListener l : listeners) {
                try { l.onReadyToggle(sessionId, ready); } catch (Throwable t) { t.printStackTrace(); }
            }
        });
    }

    // --------------------- 유틸 ---------------------

    private static void runEdt(Runnable r) {
        if (SwingUtilities.isEventDispatchThread()) r.run();
        else SwingUtilities.invokeLater(r);
    }

    // --------------------- 데모 ---------------------

    /**
     * 콘솔 데모:
     *   - 간단 리스너 등록 후 몇 가지 이벤트를 발행한다.
     */
    public static void main(String[] args) throws Exception {
        ClientPhaseBus bus = ClientPhaseBus.get();
        bus.addListener(new PhaseListener() {
            @Override public void onPhaseUpdate(PhaseState state) {
                System.out.println("[L] PHASE_UPDATE " + state);
            }
            @Override public void onCountdown(int sec) {
                System.out.println("[L] COUNTDOWN " + sec);
            }
            @Override public void onRoundResult(RoundResult result) {
                System.out.println("[L] ROUND_RESULT " + result);
            }
            @Override public void onReadyToggle(int sessionId, boolean ready) {
                System.out.println("[L] READY_TOGGLE id=" + sessionId + " ready=" + ready);
            }
        });

        // 모의 이벤트 발행
        bus.publishPhase(new PhaseState("LOBBY", 0, 0, 0, 0, 0, -1));
        bus.publishReadyToggle(101, true);
        Thread.sleep(50);
        bus.publishPhase(new PhaseState("COUNTDOWN", 1, 0, 0, 5, 5, 3));
        bus.publishCountdown(3);
        Thread.sleep(50);
        bus.publishCountdown(2);
        Thread.sleep(50);
        bus.publishCountdown(1);
        Thread.sleep(50);
        bus.publishRoundResult(new RoundResult("RED", "TEAM_WIPE"));
    }
}
