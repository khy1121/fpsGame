package com.fpsgame.client.ui;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

/**
 * 로비 중앙 스택 패널
 * - 맵 투표 패널과 캐릭터 선택 패널을 CardLayout으로 전환 표시
 * - showVote()/showSelect()로 간단히 토글
 * - BorderLayout.CENTER에 바로 붙여 쓰도록 설계
 */
public final class LobbyCenterStackPanel extends JPanel {

    private JComponent votePanel;    // MapVotePanel(권장)
    private JComponent selectPanel;  // CharacterSelectPanel(권장)

    public LobbyCenterStackPanel() {
        setLayout(new CardLayout());
        setOpaque(false);
    }

    /** 투표 패널 설정(교체 가능) */
    public void setVotePanel(JComponent panel) {
        Objects.requireNonNull(panel, "panel");
        if (votePanel != null) remove(votePanel);
        votePanel = panel;
        add(votePanel, "VOTE");
        revalidate();
        repaint();
    }

    /** 캐릭터 선택 패널 설정(교체 가능) */
    public void setSelectPanel(JComponent panel) {
        Objects.requireNonNull(panel, "panel");
        if (selectPanel != null) remove(selectPanel);
        selectPanel = panel;
        add(selectPanel, "SELECT");
        revalidate();
        repaint();
    }

    /** 투표 화면 표시 */
    public void showVote() {
        CardLayout cl = (CardLayout) getLayout();
        cl.show(this, "VOTE");
    }

    /** 캐릭터 선택 화면 표시 */
    public void showSelect() {
        CardLayout cl = (CardLayout) getLayout();
        cl.show(this, "SELECT");
    }

    /** 모든 카드 숨김(빈 카드로 전환) */
    public void hideAll() {
        String NONE = "NONE";
        if (findComponentByName(NONE) == null) {
            JPanel empty = new JPanel();
            empty.setOpaque(false);
            add(empty, NONE);
        }
        ((CardLayout) getLayout()).show(this, NONE);
    }

    // CardLayout의 카드 이름 검색(필요 시 확장용)
    private Component findComponentByName(String name) {
        for (Component c : getComponents()) {
            // 이름 매핑은 CardLayout 내부에 있으므로 여기서는 단순 순회만 제공
        }
        return null;
    }

    // 데모 실행 ---------------------------------------------------------
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("LobbyCenterStackPanel Demo");
            f.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            f.setSize(900, 560);
            f.setLocationRelativeTo(null);

            LobbyCenterStackPanel stack = new LobbyCenterStackPanel();

            JPanel vote = new JPanel();
            vote.setBackground(new Color(44, 54, 74));
            vote.add(new JLabel("Vote"));

            JPanel sel = new JPanel();
            sel.setBackground(new Color(54, 50, 40));
            sel.add(new JLabel("Select"));

            stack.setVotePanel(vote);
            stack.setSelectPanel(sel);

            f.getContentPane().setLayout(new BorderLayout());
            f.getContentPane().add(stack, BorderLayout.CENTER);

            JPanel south = new JPanel();
            JButton b1 = new JButton("Show Vote");
            JButton b2 = new JButton("Show Select");
            JButton b3 = new JButton("Hide All");
            b1.addActionListener(e -> stack.showVote());
            b2.addActionListener(e -> stack.showSelect());
            b3.addActionListener(e -> stack.hideAll());
            south.add(b1); south.add(b2); south.add(b3);
            f.getContentPane().add(south, BorderLayout.SOUTH);

            f.setVisible(true);
        });
    }
}

