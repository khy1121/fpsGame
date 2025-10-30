package com.fpsgame.client.ui;

import com.fpsgame.client.model.ClientPhaseBus;
import com.fpsgame.client.model.ClientPhaseModel;

import javax.swing.SwingUtilities;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * PhaseHudBinder
 * ------------------------------------------------------------
 * HUD/오버레이 구성요소(텍스트 라벨, 진행바, 센터 메시지 등)에
 * ClientPhaseModel의 변화를 바인딩하기 위한 경량 어댑터.
 *
 * 목적
 * - 기존 HUD 컴포넌트(HudBarPanel, CenterMessageOverlay 등)의 내부 코드를
 *   수정하지 않고, "값을 세팅하는 콜백"만 주입받아 안전하게 갱신하도록 한다.
 *
 * 사용 예시(어디서든, 보통 GameFrame 초기화 시점):
 *   PhaseHudBinder binder = new PhaseHudBinder(ClientPhaseModel.installDefault())
 *           .onPhaseText(label::setText)
 *           .onScoreText(scoreLabel::setText)
 *           .onAliveText(aliveLabel::setText)
 *           .onCountdownText(countdownLabel::setText)
 *           .onCenterMessage(centerOverlay::showMessage);
 *   binder.bind(); // 구독 시작 (EDT 안전)
 *
 * 해제:
 *   binder.unbind();
 *
 * 스레드
 * - 내부적으로 EDT 보장. (ClientPhaseModel도 EDT로 이벤트를 발생시킴)
 *
 * 의존
 * - java.util.function.Consumer 만 사용(프로젝트 컴포넌트에 직접 의존하지 않음).
 */
public final class PhaseHudBinder {

    private final ClientPhaseModel model;
    private final PropertyChangeListener pcl = this::onProperty;

    // UI 콜백(필요한 것만 설정하면 됨)
    private Consumer<String> phaseTextSetter;
    private Consumer<String> scoreTextSetter;
    private Consumer<String> aliveTextSetter;
    private Consumer<String> countdownTextSetter;
    private Consumer<String> centerMessageSetter;

    private volatile boolean bound;

    public PhaseHudBinder(ClientPhaseModel model) {
        this.model = Objects.requireNonNull(model, "model");
    }

    // --------- 플루언트 설정 메서드 ------------------------------------------

    /** 상단 HUD의 페이즈 텍스트(예: "LOBBY", "COUNTDOWN", "RUNNING"...) */
    public PhaseHudBinder onPhaseText(Consumer<String> setter) {
        this.phaseTextSetter = setter;
        return this;
    }

    /** 스코어 텍스트(예: "RED 2 — 1 BLUE") */
    public PhaseHudBinder onScoreText(Consumer<String> setter) {
        this.scoreTextSetter = setter;
        return this;
    }

    /** 생존 현황 텍스트(예: "RED 3 alive | BLUE 2 alive") */
    public PhaseHudBinder onAliveText(Consumer<String> setter) {
        this.aliveTextSetter = setter;
        return this;
    }

    /** 카운트다운 텍스트(예: "3", "2", "1", "" 등) */
    public PhaseHudBinder onCountdownText(Consumer<String> setter) {
        this.countdownTextSetter = setter;
        return this;
    }

    /** 센터 메시지(라운드 결과/공지 등) */
    public PhaseHudBinder onCenterMessage(Consumer<String> setter) {
        this.centerMessageSetter = setter;
        return this;
    }

    // --------- 바인딩/해제 ---------------------------------------------------

    /** 모델 구독을 시작하고, 현재 상태를 즉시 1회 반영한다. */
    public synchronized void bind() {
        if (bound) return;
        bound = true;
        model.addPropertyChangeListener(pcl);
        // 최초 1회 스냅샷 반영
        applyAll();
    }

    /** 모델 구독을 해제한다. */
    public synchronized void unbind() {
        if (!bound) return;
        bound = false;
        model.removePropertyChangeListener(pcl);
    }

    // --------- 내부: 이벤트 처리 ---------------------------------------------

    private void onProperty(PropertyChangeEvent evt) {
        // EDT 보장이나, 방어적으로 한 번 더 확인
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> onProperty(evt));
            return;
        }

        String name = evt.getPropertyName();
        switch (name) {
            case "phase" -> updatePhaseText();
            case "round", "scoreA", "scoreB" -> updateScoreText();
            case "aliveA", "aliveB" -> updateAliveText();
            case "remainSec" -> { /* 필요 시 진행바 등으로 확장 가능 */ }
            case "countdown" -> updateCountdownText();
            case "lastResult" -> showRoundResultMessage();
            case "readyToggle" -> { /* 로비 패널 등에서 별도 바인더 사용 권장 */ }
            default -> { /* 무시 */ }
        }
    }

    // --------- 개별 갱신 로직 ------------------------------------------------

    private void updatePhaseText() {
        if (phaseTextSetter == null) return;
        String phase = safe(model.getPhase());
        phaseTextSetter.accept(phase);
    }

    private void updateScoreText() {
        if (scoreTextSetter == null) return;
        // 팀 이름은 프로젝트 규칙에 맞춰 색/문구를 바꾸면 됨. 여기선 RED/BLUE 고정 문자열 사용.
        String score = "RED " + model.getScoreA() + " — " + model.getScoreB() + " BLUE";
        scoreTextSetter.accept(score);
    }

    private void updateAliveText() {
        if (aliveTextSetter == null) return;
        String alive = "RED " + model.getAliveA() + " alive | BLUE " + model.getAliveB() + " alive";
        aliveTextSetter.accept(alive);
    }

    private void updateCountdownText() {
        if (countdownTextSetter == null) return;
        int c = model.getCountdown();
        countdownTextSetter.accept(c > 0 ? Integer.toString(c) : "");
        // 센터 메시지도 함께 보여주고 싶다면 아래 라인을 해제:
        // if (centerMessageSetter != null && c > 0) centerMessageSetter.accept(Integer.toString(c));
    }

    private void showRoundResultMessage() {
        if (centerMessageSetter == null) return;
        ClientPhaseBus.RoundResult rr = model.getLastResult();
        if (rr == null) return;
        String msg = switch (safe(rr.winnerTeam)) {
            case "A", "RED" -> "RED 팀 승리 (" + safe(rr.reason) + ")";
            case "B", "BLUE" -> "BLUE 팀 승리 (" + safe(rr.reason) + ")";
            case "DRAW" -> "무승부";
            default -> "라운드 결과: " + safe(rr.winnerTeam) + " (" + safe(rr.reason) + ")";
        };
        centerMessageSetter.accept(msg);
    }

    private void applyAll() {
        // 현재 모델 상태를 한 번에 HUD에 반영
        updatePhaseText();
        updateScoreText();
        updateAliveText();
        updateCountdownText();
        showRoundResultMessage();
    }

    private static String safe(String s) { return s == null ? "" : s; }

    // --------- 데모 main() ---------------------------------------------------
    /**
     * 콘솔 출력 데모:
     * - 실제 UI 없이 콜백만 연결해 동작 확인.
     */
    public static void main(String[] args) throws Exception {
        ClientPhaseModel model = ClientPhaseModel.installDefault();
        PhaseHudBinder binder = new PhaseHudBinder(model)
                .onPhaseText(s -> System.out.println("[HUD] phase=" + s))
                .onScoreText(s -> System.out.println("[HUD] score=" + s))
                .onAliveText(s -> System.out.println("[HUD] alive=" + s))
                .onCountdownText(s -> System.out.println("[HUD] countdown=" + s))
                .onCenterMessage(s -> System.out.println("[CENTER] " + s));
        binder.bind();

        // 버스를 통해 모의 이벤트 전파
        var bus = ClientPhaseBus.get();
        bus.publishPhase(new ClientPhaseBus.PhaseState("LOBBY", 0, 0, 0, 0, 0, -1));
        Thread.sleep(50);
        bus.publishPhase(new ClientPhaseBus.PhaseState("COUNTDOWN", 1, 0, 0, 5, 5, 3));
        bus.publishCountdown(3);
        Thread.sleep(50);
        bus.publishCountdown(2);
        Thread.sleep(50);
        bus.publishCountdown(1);
        Thread.sleep(50);
        bus.publishPhase(new ClientPhaseBus.PhaseState("RUNNING", 1, 0, 0, 4, 3, 40));
        Thread.sleep(50);
        bus.publishRoundResult(new ClientPhaseBus.RoundResult("RED", "TEAM_WIPE"));
        Thread.sleep(50);

        binder.unbind();
    }
}
