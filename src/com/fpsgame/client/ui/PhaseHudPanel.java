package com.fpsgame.client.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * 상단 HUD 패널(경량).
 *
 * <p>표시 항목</p>
 * - Phase (문자열/코드)
 * - Round (현재 라운드)
 * - Score (Blue : Red)
 * - Alive (Blue / Red 생존)
 * - Countdown (남은 초)
 *
 * <p>설계/정책</p>
 * - 모든 공개 setter는 내부에서 EDT로 디스패치하여 Swing 안전성 보장.
 * - 다양한 호출부 호환을 위해 동일 의미의 오버로드(int/String 등)를 제공.
 * - UI는 BorderLayout NORTH에 얇게 들어가도록 최소 높이로 설계.
 */
public class PhaseHudPanel extends JPanel {

    private final JLabel phaseLabel     = label("PHASE: -");
    private final JLabel roundLabel     = label("ROUND: -");
    private final JLabel scoreLabel     = label("SCORE: 0 : 0");
    private final JLabel aliveLabel     = label("ALIVE: 0 / 0");
    private final JLabel countdownLabel = label("COUNTDOWN: -");

    public PhaseHudPanel() {
        setOpaque(true);
        setBackground(new Color(18, 18, 18));
        setBorder(new EmptyBorder(6, 10, 6, 10));
        setLayout(new GridBagLayout());

        phaseLabel.setForeground(new Color(220, 220, 220));
        roundLabel.setForeground(new Color(220, 220, 220));
        scoreLabel.setForeground(new Color(220, 220, 220));
        aliveLabel.setForeground(new Color(220, 220, 220));
        countdownLabel.setForeground(new Color(220, 220, 120));

        Font base = getFont().deriveFont(Font.BOLD, 12f);
        phaseLabel.setFont(base);
        roundLabel.setFont(base);
        scoreLabel.setFont(base);
        aliveLabel.setFont(base);
        countdownLabel.setFont(base);

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(0, 10, 0, 10);
        c.gridy = 0;

        c.gridx = 0; add(phaseLabel, c);
        c.gridx = 1; add(roundLabel, c);
        c.gridx = 2; add(scoreLabel, c);
        c.gridx = 3; add(aliveLabel, c);

        // 오른쪽 정렬로 카운트다운을 붙임
        c.gridx = 4; c.weightx = 1.0; c.anchor = GridBagConstraints.EAST;
        add(countdownLabel, c);
    }

    // ========================= 공개 API(EDT 안전) =========================

    /** PHASE 문자열 설정(예: "LOBBY", "VOTE", "SELECT", "ROUND") */
    public void setPhase(String phase) {
        runEdt(() -> phaseLabel.setText("PHASE: " + safe(phase)));
    }

    /** PHASE 코드(int) 설정 */
    public void setPhase(int phaseCode) {
        runEdt(() -> phaseLabel.setText("PHASE: " + phaseCode));
    }

    /** 라운드 번호 설정(1-base 가정) */
    public void setRound(int round) {
        runEdt(() -> roundLabel.setText("ROUND: " + Math.max(0, round)));
    }

    /** 스코어 설정: 블루/레드 */
    public void setScore(int blue, int red) {
        runEdt(() -> scoreLabel.setText("SCORE: " + Math.max(0, blue) + " : " + Math.max(0, red)));
    }

    /** 생존 수 설정: 블루/레드 */
    public void setAlive(int blueAlive, int redAlive) {
        runEdt(() -> aliveLabel.setText("ALIVE: " + Math.max(0, blueAlive) + " / " + Math.max(0, redAlive)));
    }

    /** 카운트다운 남은 초(<=0이면 '-'로 표기) */
    public void setCountdown(int seconds) {
        runEdt(() -> {
            if (seconds <= 0) countdownLabel.setText("COUNTDOWN: -");
            else countdownLabel.setText("COUNTDOWN: " + seconds + "s");
        });
    }

    // ========================= 내부 유틸 =========================

    private static JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setHorizontalAlignment(SwingConstants.LEFT);
        return l;
    }

    private static String safe(String s) {
        return (s == null) ? "-" : s;
    }

    private static void runEdt(Runnable r) {
        if (SwingUtilities.isEventDispatchThread()) r.run();
        else SwingUtilities.invokeLater(r);
    }
}
