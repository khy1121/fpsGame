package com.fpsgame.client.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * 맵 투표 패널(경량)
 * - 카드/버튼으로 맵 목록 표시, 클릭 시 mapId를 콜백으로 전송
 * - 공개 API는 EDT에서 안전하게 호출 가능
 */
public class MapVotePanel extends JPanel {

    // 투표 전송 콜백(mapId)
    private volatile Consumer<String> voteSender;
    // 카드 그리드 컨테이너(3열)
    private final JPanel grid = new JPanel(new GridLayout(0, 3, 10, 10));
    // 현재 표시할 맵 목록
    private final List<String> maps = new ArrayList<>();

    public MapVotePanel() {
        setLayout(new BorderLayout(10, 10));
        setOpaque(true);
        setBackground(new Color(24, 24, 24));
        setBorder(new EmptyBorder(12, 12, 12, 12));

        JLabel title = new JLabel("Map Vote");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));
        title.setForeground(new Color(230, 230, 230));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(title, BorderLayout.WEST);

        grid.setOpaque(false);

        JScrollPane scroll = new JScrollPane(grid);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        add(header, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
    }

    // 공개 API ---------------------------------------------------------
    /** 맵 목록을 갱신한다. null 허용. */
    public void setMaps(List<String> mapIds) {
        runEdt(() -> {
            maps.clear();
            if (mapIds != null) {
                for (String id : mapIds) {
                    if (id != null && !id.isBlank()) maps.add(id.trim());
                }
            }
            rebuildGrid();
        });
    }

    /** 투표 전송 콜백을 설정한다. null이면 비활성. */
    public void onSendVote(Consumer<String> sender) { this.voteSender = sender; }

    // 내부 구현 --------------------------------------------------------
    private void rebuildGrid() {
        grid.removeAll();

        if (maps.isEmpty()) {
            JLabel empty = new JLabel("표시할 맵이 없습니다.", SwingConstants.CENTER);
            empty.setForeground(new Color(180, 180, 180));
            grid.setLayout(new GridLayout(1, 1));
            grid.add(wrap(empty));
        } else {
            grid.setLayout(new GridLayout(Math.max(1, (maps.size() + 2) / 3), 3, 10, 10));
            for (String id : maps) grid.add(buildCard(id));
        }

        grid.revalidate();
        grid.repaint();
    }

    private JComponent buildCard(String mapId) {
        JPanel card = new JPanel(new BorderLayout(6, 6));
        card.setOpaque(true);
        card.setBackground(new Color(36, 36, 36));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(60, 60, 60)),
                new EmptyBorder(10, 10, 10, 10)
        ));

        JLabel name = new JLabel(pretty(mapId));
        name.setForeground(new Color(235, 235, 235));
        name.setFont(name.getFont().deriveFont(Font.BOLD, 14f));

        JTextArea desc = new JTextArea("맵 설명이 표시됩니다.");
        desc.setEditable(false);
        desc.setOpaque(false);
        desc.setForeground(new Color(180, 180, 180));
        desc.setFont(desc.getFont().deriveFont(12f));
        desc.setLineWrap(true);
        desc.setWrapStyleWord(true);
        desc.setBorder(new EmptyBorder(4, 0, 8, 0));

        JButton vote = new JButton("Vote");
        vote.addActionListener(e -> sendVote(mapId));

        card.add(name, BorderLayout.NORTH);
        card.add(desc, BorderLayout.CENTER);
        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        south.setOpaque(false);
        south.add(vote);
        card.add(south, BorderLayout.SOUTH);

        // 카드 전체 클릭 지원
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) { sendVote(mapId); }
        });
        return card;
    }

    private void sendVote(String mapId) {
        Consumer<String> sender = this.voteSender;
        if (sender != null) {
            try { sender.accept(Objects.toString(mapId, "")); } catch (Throwable ignore) { }
        }
    }

    private static String pretty(String id) {
        if (id == null || id.isBlank()) return "-";
        StringBuilder sb = new StringBuilder();
        char prev = 0;
        for (char ch : id.toCharArray()) {
            if (prev == 0 || prev == '_' || prev == '-' ) sb.append(Character.toUpperCase(ch));
            else if (Character.isUpperCase(ch) && Character.isLowerCase(prev)) sb.append(' ').append(ch);
            else sb.append(ch);
            prev = ch;
        }
        return sb.toString().replace('_', ' ').replace('-', ' ');
    }

    private static JComponent wrap(JComponent c) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(false);
        p.add(c);
        return p;
    }

    private static void runEdt(Runnable r) {
        if (SwingUtilities.isEventDispatchThread()) r.run();
        else SwingUtilities.invokeLater(r);
    }
}

