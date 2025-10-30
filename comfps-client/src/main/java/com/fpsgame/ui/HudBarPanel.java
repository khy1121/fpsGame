package com.fpsgame.client.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.text.DecimalFormat;

/**
 * 경기 상단 HUD 바(라운드/점수/페이즈/타이머/생존 인원)를 표현하는 경량 패널.
 *
 * <p>의존성이 없도록 문자열/숫자 기반의 세터만 제공하며,
 * 서버/클라이언트 로직과의 결합은 호출 측에서 수행한다.</p>
 *
 * <p>표시 항목</p>
 * <ul>
 *   <li>왼쪽: 팀 A 점수 / 팀 B 점수</li>
 *   <li>가운데: 페이즈(예: LOBBY, VOTE, COUNTDOWN, ROUND_RUNNING, RESULT, MATCH_END)</li>
 *   <li>오른쪽: 라운드 타이머 MM:SS / 생존 인원(예: A 3 — 2 B)</li>
 * </ul>
 */
public final class HudBarPanel extends JPanel {

    // 좌측(점수)
    private final JLabel leftScore = labelBold(18);
    // 중앙(페이즈)
    private final JLabel centerPhase = labelBold(16);
    // 우측(타이머+생존)
    private final JLabel rightInfo = labelBold(16);

    // 내부 상태
    private int teamAScore = 0;
    private int teamBScore = 0;
    private String phaseText = "LOBBY";
    private int timerSec = 0;
    private int aliveA = 0, aliveB = 0;

    private static final DecimalFormat DF2 = new DecimalFormat("00");

    public HudBarPanel() {
        super(new BorderLayout());
        setBorder(new EmptyBorder(6, 10, 6, 10));
        setBackground(new Color(20, 22, 26));

        leftScore.setForeground(new Color(200, 220, 255));
        centerPhase.setForeground(new Color(230, 230, 230));
        rightInfo.setForeground(new Color(200, 220, 255));

        add(leftScore, BorderLayout.WEST);
        add(centerPhase, BorderLayout.CENTER);
        add(rightInfo, BorderLayout.EAST);

        refreshTexts();
    }

    // ============== 공개 세터(EDT 안전) ==============

    /** 팀 점수를 설정한다. 음수는 0으로 보정. */
    public void setScores(int teamA, int teamB) {
        int a = Math.max(0, teamA);
        int b = Math.max(0, teamB);
        if (SwingUtilities.isEventDispatchThread()) {
            teamAScore = a; teamBScore = b; refreshTexts();
        } else {
            SwingUtilities.invokeLater(() -> { teamAScore = a; teamBScore = b; refreshTexts(); });
        }
    }

    /** 중앙 페이즈 텍스트를 설정한다(예: "ROUND_RUNNING"). */
    public void setPhaseText(String phase) {
        String p = (phase == null || phase.isBlank()) ? "—" : phase.trim();
        if (SwingUtilities.isEventDispatchThread()) {
            phaseText = p; refreshTexts();
        } else {
            SwingUtilities.invokeLater(() -> { phaseText = p; refreshTexts(); });
        }
    }

    /** 남은 시간을 초 단위로 설정한다(음수는 0). */
    public void setTimerSeconds(int seconds) {
        int s = Math.max(0, seconds);
        if (SwingUtilities.isEventDispatchThread()) {
            timerSec = s; refreshTexts();
        } else {
            SwingUtilities.invokeLater(() -> { timerSec = s; refreshTexts(); });
        }
    }

    /** 각 팀의 생존 인원(A/B)을 설정한다(음수는 0). */
    public void setAliveCounts(int teamAAlive, int teamBAlive) {
        int a = Math.max(0, teamAAlive);
        int b = Math.max(0, teamBAlive);
        if (SwingUtilities.isEventDispatchThread()) {
            aliveA = a; aliveB = b; refreshTexts();
        } else {
            SwingUtilities.invokeLater(() -> { aliveA = a; aliveB = b; refreshTexts(); });
        }
    }

    // ============== 내부 구현 ==============

    private void refreshTexts() {
        leftScore.setText("A " + teamAScore + "  |  " + teamBScore + " B");
        centerPhase.setText(phaseText);

        String mm = DF2.format(timerSec / 60);
        String ss = DF2.format(timerSec % 60);
        String right = mm + ":" + ss + "   |   A " + aliveA + " — " + aliveB + " B";
        rightInfo.setText(right);

        revalidate();
        repaint();
    }

    private static JLabel labelBold(float size) {
        JLabel l = new JLabel();
        l.setFont(l.getFont().deriveFont(Font.BOLD, size));
        return l;
    }

    // ============== 단독 실행 데모 ==============

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}

            JFrame f = new JFrame("HudBarPanel Demo");
            f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            f.setSize(800, 120);
            f.setLocationRelativeTo(null);

            HudBarPanel hud = new HudBarPanel();
            f.add(hud, BorderLayout.NORTH);

            // 중앙에 더미 배경
            JPanel bg = new JPanel();
            bg.setBackground(new Color(36, 36, 36));
            f.add(bg, BorderLayout.CENTER);

            f.setVisible(true);

            // 데모 갱신 타이머: 타이머 초 감소, 랜덤 생존 수/점수 변경
            new Timer(1000, e -> {
                hud.setTimerSeconds(Math.max(0, hud.timerSec - 1));
            }).start();

            // 데모용 버튼 패널
            JPanel controls = new JPanel();
            JButton nextPhase = new JButton("Next Phase");
            JButton addA = new JButton("A+");
            JButton addB = new JButton("B+");
            JButton decA = new JButton("A-");
            JButton decB = new JButton("B-");
            controls.add(nextPhase); controls.add(addA); controls.add(addB); controls.add(decA); controls.add(decB);
            f.add(controls, BorderLayout.SOUTH);

            final String[] phases = {"LOBBY", "VOTE", "COUNTDOWN", "ROUND_RUNNING", "ROUND_RESULT", "MATCH_END"};
            final int[] idx = {0};
            nextPhase.addActionListener(ev -> {
                idx[0] = (idx[0] + 1) % phases.length;
                hud.setPhaseText(phases[idx[0]]);
                hud.setTimerSeconds(90);
            });
            addA.addActionListener(ev -> hud.setScores(hud.teamAScore + 1, hud.teamBScore));
            addB.addActionListener(ev -> hud.setScores(hud.teamAScore, hud.teamBScore + 1));
            decA.addActionListener(ev -> hud.setAliveCounts(Math.max(0, hud.aliveA - 1), hud.aliveB));
            decB.addActionListener(ev -> hud.setAliveCounts(hud.aliveA, Math.max(0, hud.aliveB - 1)));

            // 초기값
            hud.setScores(0, 0);
            hud.setPhaseText("LOBBY");
            hud.setTimerSeconds(90);
            hud.setAliveCounts(5, 5);
        });
    }
}
