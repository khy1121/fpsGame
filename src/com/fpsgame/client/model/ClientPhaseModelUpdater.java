package com.fpsgame.client.model;

import javax.swing.*;
import java.lang.reflect.Method;
import java.util.Objects;

/**
 * ClientPhaseModelUpdater
 * ------------------------------------------------------------
 * ClientPhaseBus 에 올라오는 이벤트(PHASE_UPDATE/COUNTDOWN/ROUND_RESULT)를
 * ClientPhaseModel 에 반영하는 경량 어댑터.
 *
 * 특징
 * - EDT 안전: 모든 모델 갱신은 EDT에서 수행.
 * - 느슨한 결합: 모델의 세터 시그니처가 달라도 리플렉션으로 관대하게 호출 시도.
 *   (예: setPhase(String)/phase(String), setRound(int)/round(int) 등)
 *
 * 사용 예시:
 *   ClientPhaseModel model = ClientPhaseModel.installDefault();
 *   ClientPhaseModelUpdater updater = new ClientPhaseModelUpdater(model).bind();
 *
 *   // 네트워크 수신부에서는 PhaseFrameAdapter가 ClientPhaseBus에 publish 한다.
 *   // 본 업데이터가 버스를 구독하여 모델을 갱신.
 *
 * 해제:
 *   updater.unbind();
 */
public final class ClientPhaseModelUpdater implements ClientPhaseBus.PhaseListener {

    private final ClientPhaseModel model;
    private final ClientPhaseBus bus;

    private boolean bound;

    public ClientPhaseModelUpdater(ClientPhaseModel model) {
        this(model, ClientPhaseBus.get());
    }

    public ClientPhaseModelUpdater(ClientPhaseModel model, ClientPhaseBus bus) {
        this.model = Objects.requireNonNull(model, "model");
        this.bus = Objects.requireNonNull(bus, "bus");
    }

    /** 버스 구독 시작(중복 호출 안전) */
    public synchronized ClientPhaseModelUpdater bind() {
        if (bound) return this;
        bound = true;
        bus.addListener(this);
        return this;
    }

    /** 버스 구독 해제(중복 호출 안전) */
    public synchronized void unbind() {
        if (!bound) return;
        bound = false;
        bus.removeListener(this);
    }

    // ---------------- ClientPhaseBus.PhaseListener ----------------

    @Override
    public void onPhaseUpdate(ClientPhaseBus.PhaseState s) {
        runEdt(() -> {
            // 문자열/정수 필드 모두 관대하게 세터 호출을 시도
            // 메서드 후보: setXxx(...), xxx(...)
            setString("Phase", s.phase);
            setInt("Round", s.round);
            setInt("ScoreA", s.scoreA);
            setInt("ScoreB", s.scoreB);
            setInt("AliveA", s.aliveA);
            setInt("AliveB", s.aliveB);
            setInt("Countdown", s.remainSec);
        });
    }

    @Override
    public void onCountdown(int sec) {
        runEdt(() -> setInt("Countdown", sec));
    }

    @Override
    public void onRoundResult(ClientPhaseBus.RoundResult r) {
        // 라운드 결과는 모델에 선택적으로 반영.
        // 후보 세터: setWinnerTeam(String)/winnerTeam(String), setRoundReason(String)/roundReason(String)
        runEdt(() -> {
            setString("WinnerTeam", r.winnerTeam);
            setString("RoundReason", r.reason);
            // 페이즈를 RESULT 로 두고 싶다면:
            setString("Phase", "RESULT");
            // 카운트다운 종료
            setInt("Countdown", 0);
        });
    }
 // onReadyToggle(...) 내부만 교체
    @Override
    public void onReadyToggle(int sessionId, boolean ready) {
        runEdt(() -> {
            // 순차 시도: 하나라도 성공하면 이후는 건너뜀
            if (!call2("setReadyOf", int.class, boolean.class, sessionId, ready)) {
                if (!call2("readyOf", int.class, boolean.class, sessionId, ready)) {
                    call2("setLobbyReady", int.class, boolean.class, sessionId, ready);
                }
            }
        });
    }
    // ---------------- 내부 유틸(EDT/리플렉션) ----------------

    private static void runEdt(Runnable r) {
        if (SwingUtilities.isEventDispatchThread()) r.run();
        else SwingUtilities.invokeLater(r);
    }

    private void setString(String base, String value) {
        // setBase(String) → base(String) 순으로 시도
        if (call1("set" + base, String.class, value)) return;
        call1(lowerFirst(base), String.class, value);
    }

    private void setInt(String base, int value) {
        // setBase(int) → setBase(Integer) → base(int) → base(Integer)
        if (call1("set" + base, int.class, value)) return;
        if (call1("set" + base, Integer.class, value)) return;
        if (call1(lowerFirst(base), int.class, value)) return;
        call1(lowerFirst(base), Integer.class, value);
    }

    private boolean call1(String name, Class<?> p0, Object a0) {
        try {
            Method m = model.getClass().getMethod(name, p0);
            m.setAccessible(true);
            m.invoke(model, a0);
            return true;
        } catch (Throwable ignore) {
            return false;
        }
    }

    private boolean call2(String name, Class<?> p0, Class<?> p1, Object a0, Object a1) {
        try {
            Method m = model.getClass().getMethod(name, p0, p1);
            m.setAccessible(true);
            m.invoke(model, a0, a1);
            return true;
        } catch (Throwable ignore) {
            return false;
        }
    }

    private static String lowerFirst(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toLowerCase(s.charAt(0)) + s.substring(1);
    }

    // ---------------- 데모 ----------------

    public static void main(String[] args) throws Exception {
        // 기본 모델 + 업데이터
        ClientPhaseModel model = ClientPhaseModel.installDefault();
        ClientPhaseModelUpdater up = new ClientPhaseModelUpdater(model).bind();

        // 모델 변경 로그를 간단히 보기 위해 PropertyChangeListener 연결(선택)
        model.addPropertyChangeListener(evt -> {
            System.out.println("[MODEL] " + evt.getPropertyName() + " -> " + evt.getNewValue());
        });

        // 버스에 모의 이벤트 발행
        ClientPhaseBus bus = ClientPhaseBus.get();
        bus.publishPhase(new ClientPhaseBus.PhaseState("LOBBY", 0, 0, 0, 5, 5, -1));
        Thread.sleep(100);
        bus.publishCountdown(3);
        Thread.sleep(100);
        bus.publishPhase(new ClientPhaseBus.PhaseState("COUNTDOWN", 1, 0, 0, 5, 5, 2));
        Thread.sleep(100);
        bus.publishRoundResult(new ClientPhaseBus.RoundResult("RED", "TEAM_WIPE"));

        // 정리
        up.unbind();
    }
}
