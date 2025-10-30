package com.fpsgame.client.model;

import javax.swing.SwingUtilities;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Objects;

/**
 * ClientPhaseModel
 * ------------------------------------------------------------
 * UI가 손쉽게 바인딩할 수 있도록, 라운드/페이즈 상태를 보관하는
 * Observable(Model) 클래스.
 *
 * 역할
 * - ClientPhaseBus 를 구독하여 내부 필드를 갱신하고,
 *   변경 시 PropertyChange 이벤트를 발생시켜 HUD/패널이 EDT에서 안전하게 갱신되도록 한다.
 *
 * 사용 규칙
 * - 런타임 갱신은 ClientPhaseBus.publish*() 경유가 원칙.
 * - 테스트/데모 편의상 phase에 한해 공개 세터(setPhase) 제공.
 */
public final class ClientPhaseModel implements ClientPhaseBus.PhaseListener {

    // -------- 싱글톤 접근/설치 -----------------------------------------------
    private static volatile ClientPhaseModel DEFAULT;

    /** 기본 싱글톤 버스(ClientPhaseBus.get())에 자동 구독하여 모델을 반환한다. */
    public static ClientPhaseModel installDefault() {
        return install(ClientPhaseBus.get());
    }

    /** 지정한 버스에 자동 구독하여 모델을 반환한다. */
    public static ClientPhaseModel install(ClientPhaseBus bus) {
        ClientPhaseModel m = new ClientPhaseModel(bus);
        bus.addListener(m);
        return m;
    }

    // -------- 내부 상태 -----------------------------------------------------
    private final ClientPhaseBus bus;
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    private String phase = "UNKNOWN";
    private int round = 0;
    private int scoreA = 0;
    private int scoreB = 0;
    private int aliveA = 0;
    private int aliveB = 0;
    private int remainSec = 0; // countdown 초

    /** 마지막 라운드 결과(HUD에서 표시 용). null 일 수 있음. */
    private volatile ClientPhaseBus.RoundResult lastResult;

    private ClientPhaseModel(ClientPhaseBus bus) {
        this.bus = Objects.requireNonNull(bus, "bus");
    }

    // -------- 구독자 구현: 버스 → 모델 ---------------------------------------
    @Override
    public void onPhaseUpdate(ClientPhaseBus.PhaseState state) {
        // 일부 구현에서 숫자 필드가 String으로 올 수 있어 방어적 파싱을 사용
        Runnable r = () -> {
            setPhase(state.phase);
            setRoundValue(state.round);
            setScoreAValue(state.scoreA);
            setScoreBValue(state.scoreB);
            setAliveAValue(state.aliveA);
            setAliveBValue(state.aliveB);
            setRemainSecValue(state.remainSec);
        };
        if (SwingUtilities.isEventDispatchThread()) r.run();
        else SwingUtilities.invokeLater(r);
    }

    @Override
    public void onCountdown(int sec) {
        Runnable r = () -> setRemainSec(sec);
        if (SwingUtilities.isEventDispatchThread()) r.run();
        else SwingUtilities.invokeLater(r);
    }

    @Override
    public void onRoundResult(ClientPhaseBus.RoundResult result) {
        // 변환 없이 버스 DTO 그대로 캐시 (HUD는 ClientPhaseBus.RoundResult를 기대)
        Runnable r = () -> {
            ClientPhaseBus.RoundResult old = this.lastResult;
            this.lastResult = result;
            if (!Objects.equals(old, result)) {
                pcs.firePropertyChange("lastResult", old, result);
            }
        };
        if (SwingUtilities.isEventDispatchThread()) r.run();
        else SwingUtilities.invokeLater(r);
    }

    @Override
    public void onReadyToggle(int sessionId, boolean ready) {
        // 필요 시 READY 상태를 내 모델에 보관(현 단계는 HUD로 직접 전달)
    }

    // -------- 세터: 변경 시 PropertyChange 발생 ------------------------------
    // NOTE: 테스트/데모 용도로만 공개 허용 (기존 private → public)
    public void setPhase(String v)       { fireIfChanged("phase", this.phase, this.phase = nvl(v, "UNKNOWN")); }
    private void setRound(int v)          { fireIfChanged("round", this.round, this.round = v); }
    private void setScoreA(int v)         { fireIfChanged("scoreA", this.scoreA, this.scoreA = v); }
    private void setScoreB(int v)         { fireIfChanged("scoreB", this.scoreB, this.scoreB = v); }
    private void setAliveA(int v)         { fireIfChanged("aliveA", this.aliveA, this.aliveA = v); }
    private void setAliveB(int v)         { fireIfChanged("aliveB", this.aliveB, this.aliveB = v); }
    private void setRemainSec(int v)      { fireIfChanged("remainSec", this.remainSec, this.remainSec = v); }

    // -------- 오브젝트 수용 파서(문자열/숫자 모두 처리) -----------------------
    private void setRoundValue(Object v)     { setRound(toInt(v)); }
    private void setScoreAValue(Object v)    { setScoreA(toInt(v)); }
    private void setScoreBValue(Object v)    { setScoreB(toInt(v)); }
    private void setAliveAValue(Object v)    { setAliveA(toInt(v)); }
    private void setAliveBValue(Object v)    { setAliveB(toInt(v)); }
    private void setRemainSecValue(Object v) { setRemainSec(toInt(v)); }

    private static int toInt(Object v) {
        if (v == null) return 0;
        if (v instanceof Number) return ((Number) v).intValue();
        if (v instanceof CharSequence) {
            try {
                return Integer.parseInt(v.toString().trim());
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        // 알 수 없는 타입: 안전 기본값
        return 0;
    }

    // -------- 게터 (HUD 호환 포함) -------------------------------------------
    public String getPhase()        { return phase; }
    public int getRound()           { return round; }
    public int getScoreA()          { return scoreA; }
    public int getScoreB()          { return scoreB; }
    public int getAliveA()          { return aliveA; }
    public int getAliveB()          { return aliveB; }
    public int getRemainSec()       { return remainSec; }

    /** HUD 호환용 별칭: 많은 패널이 getCountdown()를 기대함 → remainSec 반환 */
    public int getCountdown()       { return remainSec; }

    /** 마지막 라운드 결과(HUD가 배너/오버레이 표시 시 사용). null 가능. */
    public ClientPhaseBus.RoundResult getLastResult() { return lastResult; }

    // -------- 리스너 등록/해제 ----------------------------------------------
    public void addPropertyChangeListener(PropertyChangeListener l) {
        pcs.addPropertyChangeListener(l);
    }
    public void removePropertyChangeListener(PropertyChangeListener l) {
        pcs.removePropertyChangeListener(l);
    }

    // -------- 유틸 ----------------------------------------------------------
    private static String nvl(String s, String def) {
        return (s == null || s.isEmpty()) ? def : s;
    }

    private void fireIfChanged(String name, Object oldV, Object newV) {
        if (!Objects.equals(oldV, newV)) {
            pcs.firePropertyChange(name, oldV, newV);
        }
    }

    // -------- READY 이벤트 DTO ----------------------------------------------
    /** READY_TOGGLE 이벤트를 UI로 전달하기 위한 간단 DTO */
    public static final class ReadyEvent {
        public final int sessionId;
        public final boolean ready;
        public ReadyEvent(int sessionId, boolean ready) {
            this.sessionId = sessionId;
            this.ready = ready;
        }
        @Override public String toString() { return "ReadyEvent{sessionId=" + sessionId + ", ready=" + ready + '}'; }
    }
}
