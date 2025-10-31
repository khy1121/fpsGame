package com.fpsgame.client.ui;

import com.fpsgame.client.NetClient;
import com.fpsgame.common.Protocol;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * HUD 패널(경량)
 * - 게임 진행 상태(Phase/Countdown/Score/Ready)와 월드/맵/능력 상태를 간단 표시
 * - 공개 API는 모두 EDT에서 안전하게 갱신
 */
public class HudPanel extends JPanel {

    // 상단 상태 라인
    private final JLabel phaseLabel = new JLabel("Phase: -");
    private final JLabel countdownLabel = new JLabel("Countdown: -");
    private final JLabel scoreLabel = new JLabel("Score 0 : 0");
    private final JLabel readyLabel = new JLabel("Ready 0/0");
    private final JLabel messageLabel = new JLabel("");
    private final JLabel connectionLabel = new JLabel("● Disconnected");

    // 하단 정보 라인
    private final JLabel worldLabel = new JLabel("World: -");
    private final JLabel mapLabel = new JLabel("Map: -");
    private final JLabel statusLabel = new JLabel("Stance: -   Scope: -   Ult: -");

    /** 구성: 라벨/레이아웃 초기화 */
    public HudPanel() {
        setLayout(new GridBagLayout());
        setBackground(new Color(18, 18, 18));
        setBorder(new EmptyBorder(6, 8, 6, 8));

        phaseLabel.setForeground(new Color(180, 220, 255));
        countdownLabel.setForeground(new Color(255, 220, 120));
        scoreLabel.setForeground(new Color(200, 255, 200));
        readyLabel.setForeground(new Color(200, 220, 255));
        messageLabel.setForeground(new Color(255, 190, 190));
        connectionLabel.setForeground(new Color(255, 100, 100)); // 빨간색 (연결 끊김)

        phaseLabel.setFont(phaseLabel.getFont().deriveFont(Font.BOLD, 14f));
        countdownLabel.setFont(countdownLabel.getFont().deriveFont(Font.BOLD, 14f));
        scoreLabel.setFont(scoreLabel.getFont().deriveFont(Font.BOLD, 14f));
        connectionLabel.setFont(connectionLabel.getFont().deriveFont(Font.BOLD, 13f));
        worldLabel.setFont(worldLabel.getFont().deriveFont(Font.PLAIN, 13f));
        mapLabel.setFont(mapLabel.getFont().deriveFont(Font.PLAIN, 13f));
        messageLabel.setFont(messageLabel.getFont().deriveFont(Font.BOLD, 14f));

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(2, 8, 2, 8);

        // 1행: 진행 상태 표시
        c.gridy = 0;
        c.gridx = 0; add(phaseLabel, c);
        c.gridx = 1; add(countdownLabel, c);
        c.gridx = 2; add(scoreLabel, c);
        c.gridx = 3; add(readyLabel, c);
        c.gridx = 4; c.weightx = 1.0; c.fill = GridBagConstraints.HORIZONTAL; add(messageLabel, c);

        // 2행: 월드/맵/능력 상태 표시
        c.gridy = 1; c.weightx = 0; c.fill = GridBagConstraints.NONE;
        c.gridx = 0; add(connectionLabel, c); // 연결 상태 표시
        c.gridx = 1; add(worldLabel, c);
        c.gridx = 2; add(mapLabel, c);
        c.gridx = 3; c.gridwidth = 2; add(statusLabel, c);
        c.gridwidth = 1;
    }

    // ===== 공개 API: 진행 상태 =====

    /** Phase 코드 수신 시 라벨 갱신 */
    public void onPhaseUpdate(int phaseCode) {
        String name = switch (phaseCode) {
            case 0 -> "LOBBY";
            case 1 -> "VOTE";
            case 2 -> "COUNTDOWN";
            case 3 -> "ROUND_RUNNING";
            case 4 -> "ROUND_RESULT";
            case 5 -> "MATCH_END";
            default -> "UNKNOWN(" + phaseCode + ")";
        };
        dispatchEdt(() -> phaseLabel.setText("Phase: " + name));
    }

    /** 카운트다운 초 표시 */
    public void onCountdown(int seconds) {
        dispatchEdt(() -> countdownLabel.setText("Countdown: " + Math.max(0, seconds) + "s"));
    }

    /** 라운드 결과/점수 표시 */
    public void onRoundResult(Protocol.RoundResult rr) {
        if (rr == null) return;
        dispatchEdt(() -> {
            scoreLabel.setText("Score " + rr.blueRounds + " : " + rr.redRounds);
            messageLabel.setText(rr.matchEnded
                    ? ("Match End | Winner Team " + rr.winnerTeam)
                    : ("Round Winner Team " + rr.winnerTeam));
        });
    }

    /** 상단 시스템 메시지 표시 */
    public void setSystemMessage(String text) {
        final String safe = (text == null ? "" : text);
        dispatchEdt(() -> messageLabel.setText(safe));
    }

    // 구버전 래퍼(호환용)
    public void updatePhase(int phaseCode) { onPhaseUpdate(phaseCode); }
    public void updateCountdown(int seconds) { onCountdown(seconds); }
    public void updateRoundResult(Protocol.RoundResult rr) { onRoundResult(rr); }
    public void log(String text) { setSystemMessage(text); }
    public void updateRtt(long millis) { setSystemMessage("RTT: " + Math.max(0, millis) + " ms"); }

    // ===== 공개 API: 월드/맵 =====

    public void setWorldInfo(int w, int h) {
        dispatchEdt(() -> worldLabel.setText("World: " + Math.max(0, w) + " x " + Math.max(0, h)));
    }

    public void setMapInfo(String mapName, int mapId) {
        final String name = (mapName == null || mapName.isBlank()) ? "Unknown" : mapName;
        dispatchEdt(() -> mapLabel.setText("Map: " + name + " (" + Math.max(0, mapId) + ")"));
    }

    public void setReadyInfo(int ready, int total) {
        dispatchEdt(() -> readyLabel.setText("Ready: " + Math.max(0, ready) + "/" + Math.max(0, total)));
    }

    // ===== 공개 API: 연결 상태 =====

    /** 연결됨 상태로 변경 (녹색) */
    public void setConnected() {
        dispatchEdt(() -> {
            connectionLabel.setText("● Connected");
            connectionLabel.setForeground(new Color(100, 255, 100)); // 녹색
        });
    }

    /** 연결 끊김 상태로 변경 (빨간색) */
    public void setDisconnected() {
        dispatchEdt(() -> {
            connectionLabel.setText("● Disconnected");
            connectionLabel.setForeground(new Color(255, 100, 100)); // 빨간색
        });
    }

    /** 재연결 시도 중 상태로 변경 (노란색) */
    public void setReconnecting() {
        dispatchEdt(() -> {
            connectionLabel.setText("● Reconnecting...");
            connectionLabel.setForeground(new Color(255, 220, 100)); // 노란색
        });
    }

    // ===== 공개 API: 능력/상태 =====

    public void setCoverStance(boolean active) { dispatchEdt(() -> rewriteStatus(active, null, null)); }
    public void setScopeActive(boolean active) { dispatchEdt(() -> rewriteStatus(null, active, null)); }
    public void setUltimateActive(boolean active, Integer remainingSec) { dispatchEdt(() -> rewriteStatus(null, null, active, remainingSec)); }

    /** 헬퍼: 상태 라벨 텍스트 갱신(부분 업데이트 용) */
    private void rewriteStatus(Boolean cover, Boolean scope, Boolean ult) { rewriteStatus(cover, scope, ult, null); }

    private void rewriteStatus(Boolean cover, Boolean scope, Boolean ult, Integer ultRemain) {
        String text = statusLabel.getText();
        boolean coverB = text.contains("Stance: ON");
        boolean scopeB = text.contains("Scope: ON");
        boolean ultB = text.contains("Ult: ON");
        int remain = (ultRemain != null ? Math.max(0, ultRemain) : -1);

        if (cover != null) coverB = cover;
        if (scope != null) scopeB = scope;
        if (ult != null) ultB = ult;

        String ultPart = ultB ? ("Ult: ON" + (remain >= 0 ? (" (" + remain + "s)") : "")) : "Ult: -";
        statusLabel.setText("Stance: " + (coverB ? "ON" : "-") + "   "
                + "Scope: " + (scopeB ? "ON" : "-") + "   " + ultPart);
    }

    /** 어댑터: NetClient 이벤트를 HUD 갱신에 매핑 */
    public NetClient.Listener asListener(NetClient.Listener delegate) {
        return new NetClient.Listener() {
            @Override public void onOpen(NetClient c) { 
                setConnected(); // 연결됨 상태로 변경
                if (delegate != null) delegate.onOpen(c); 
            }
            @Override public void onClosed(NetClient c, String reason) { 
                setDisconnected(); // 연결 끊김 상태로 변경
                if (delegate != null) delegate.onClosed(c, reason); 
            }
            @Override public void onDisconnected(String reason) { 
                setDisconnected(); // 연결 끊김 상태로 변경
                setSystemMessage("Disconnected: " + reason); // 시스템 메시지 표시
                if (delegate != null) delegate.onDisconnected(reason); 
            }
            @Override public void onChat(String text) { if (delegate != null) delegate.onChat(text); }
            @Override public void onFrame(NetClient c, Protocol.Frame frame) {
                if (frame == null) { if (delegate != null) delegate.onFrame(c, frame); return; }
                try {
                    switch (frame.opcode) {
                        case Protocol.Opcode.PHASE_UPDATE -> onPhaseUpdate(Protocol.parsePhaseUpdate(frame.payload));
                        case Protocol.Opcode.COUNTDOWN    -> onCountdown(Protocol.parseCountdown(frame.payload));
                        case Protocol.Opcode.ROUND_RESULT -> onRoundResult(Protocol.parseRoundResult(frame.payload));
                        default -> { if (delegate != null) delegate.onFrame(c, frame); }
                    }
                } catch (Throwable t) {
                    if (delegate != null) delegate.onFrame(c, frame);
                }
            }
        };
    }

    /** 구현 예정: 자동 배선(현재 미사용) */
    public void wire(NetClient net) { /* no-op */ }

    // 유틸리티: EDT 디스패치
    private static void dispatchEdt(Runnable r) {
        if (SwingUtilities.isEventDispatchThread()) r.run();
        else SwingUtilities.invokeLater(r);
    }
}

