package com.fpsgame.client.model;

import javax.swing.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

/**
 * PhaseEventLogger
 * ------------------------------------------------------------
 * ClientPhaseBus 이벤트(PHASE_UPDATE/COUNTDOWN/ROUND_RESULT/READY_TOGGLE)를
 * 콘솔 또는 JTextArea로 로그 출력하는 경량 디버그 유틸.
 *
 * 사용 예시 1) 콘솔로만 보기:
 *   PhaseEventLogger.attachConsole(); // 싱글톤 버스 자동 구독
 *
 * 사용 예시 2) JTextArea에 출력:
 *   JTextArea area = new JTextArea(12, 60);
 *   PhaseEventLogger.attach(area);
 *
 * 해제:
 *   PhaseEventLogger logger = PhaseEventLogger.attach(...);
 *   logger.detach();
 */
public final class PhaseEventLogger implements ClientPhaseBus.PhaseListener {

    /** 타임스탬프 포맷 */
    private static final SimpleDateFormat TS = new SimpleDateFormat("HH:mm:ss.SSS");

    private final ClientPhaseBus bus;
    private final JTextArea sink; // null이면 System.out 사용
    private boolean attached;

    private PhaseEventLogger(ClientPhaseBus bus, JTextArea sink) {
        this.bus = Objects.requireNonNull(bus, "bus");
        this.sink = sink;
    }

    // --------------- 팩토리 ---------------

    /** 싱글톤 버스에 연결하여 콘솔(System.out)로만 출력 */
    public static PhaseEventLogger attachConsole() {
        PhaseEventLogger l = new PhaseEventLogger(ClientPhaseBus.get(), null);
        l.attach();
        return l;
    }

    /** 싱글톤 버스에 연결하여 지정 JTextArea로 출력(EDT 안전) */
    public static PhaseEventLogger attach(JTextArea sink) {
        PhaseEventLogger l = new PhaseEventLogger(ClientPhaseBus.get(), Objects.requireNonNull(sink, "sink"));
        l.attach();
        return l;
    }

    /** 커스텀 버스/싱크로 연결 */
    public static PhaseEventLogger attach(ClientPhaseBus bus, JTextArea sink) {
        PhaseEventLogger l = new PhaseEventLogger(bus, sink);
        l.attach();
        return l;
    }

    // --------------- 생명주기 ---------------

    public synchronized void attach() {
        if (attached) return;
        bus.addListener(this);
        attached = true;
        log("[LOGGER] attached");
    }

    public synchronized void detach() {
        if (!attached) return;
        attached = false;
        bus.removeListener(this);
        log("[LOGGER] detached");
    }

    // --------------- 리스너 구현 ---------------

    @Override
    public void onPhaseUpdate(ClientPhaseBus.PhaseState state) {
        log("PHASE_UPDATE " + state);
    }

    @Override
    public void onCountdown(int sec) {
        log("COUNTDOWN sec=" + sec);
    }

    @Override
    public void onRoundResult(ClientPhaseBus.RoundResult result) {
        log("ROUND_RESULT " + result);
    }

    @Override
    public void onReadyToggle(int sessionId, boolean ready) {
        log("READY_TOGGLE sessionId=" + sessionId + " ready=" + ready);
    }

    // --------------- 출력 유틸 ---------------

    private void log(String msg) {
        String line = "[" + TS.format(new Date()) + "] " + msg;
        if (sink == null) {
            System.out.println(line);
            return;
        }
        if (SwingUtilities.isEventDispatchThread()) {
            appendLine(line);
        } else {
            SwingUtilities.invokeLater(() -> appendLine(line));
        }
    }

    private void appendLine(String line) {
        sink.append(line);
        sink.append("\n");
        sink.setCaretPosition(sink.getDocument().getLength());
    }

    // --------------- 데모 ---------------

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("PhaseEventLogger Demo");
            f.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            JTextArea area = new JTextArea(16, 80);
            area.setEditable(false);
            JScrollPane sp = new JScrollPane(area);
            f.setContentPane(sp);
            f.pack();
            f.setLocationRelativeTo(null);
            f.setVisible(true);

            // 로거 장착
            PhaseEventLogger.attach(area);
        });

        // 버스에 모의 이벤트 쏴보기
        var bus = ClientPhaseBus.get();
        Thread.sleep(100);
        bus.publishPhase(new ClientPhaseBus.PhaseState("LOBBY", 0, 0, 0, 0, 0, -1));
        bus.publishReadyToggle(1001, true);
        Thread.sleep(50);
        bus.publishPhase(new ClientPhaseBus.PhaseState("COUNTDOWN", 1, 0, 0, 5, 5, 3));
        bus.publishCountdown(3);
        Thread.sleep(50);
        bus.publishCountdown(2);
        Thread.sleep(50);
        bus.publishCountdown(1);
        Thread.sleep(50);
        bus.publishRoundResult(new ClientPhaseBus.RoundResult("RED", "TEAM_WIPE"));
    }
}
