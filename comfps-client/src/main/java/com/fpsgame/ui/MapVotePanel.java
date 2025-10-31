package com.fpsgame.client.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

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

        // 맵 이미지 로드
        JLabel image = new JLabel();
        image.setHorizontalAlignment(SwingConstants.CENTER);
        image.setOpaque(false);
        loadMapImage(mapId, image, 200, 120);

        JTextArea desc = new JTextArea(getMapDescription(mapId));
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
        JPanel center = new JPanel(new BorderLayout());
        center.setOpaque(false);
        center.add(image, BorderLayout.CENTER);
        center.add(desc, BorderLayout.SOUTH);
        card.add(center, BorderLayout.CENTER);
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

    // CHARACTER_SPECS.md 기반 맵 설명
    private static String getMapDescription(String mapId) {
        if (mapId == null) return "맵 설명이 표시됩니다.";
        String key = mapId.toLowerCase();
        return switch (key) {
            case "terminal" -> "도시 공항 | 좁은 복도와 개방된 광장 | 밸런스형 전투";
            case "neoncity" -> "사이버펑크 도시 | 수직 레벨과 네온 거리 | 기동력 중시";
            case "forestoutpost" -> "숲속 요새 | 자연 엄폐와 벙커 | 전술적 플레이";
            default -> "맵 설명이 표시됩니다.";
        };
    }

    // 맵 이미지 로더
    private static void loadMapImage(String id, JLabel target, int w, int h) {
        if (target == null) return;
        String base = id == null ? "" : id.trim();
        java.awt.image.BufferedImage img = null;
        // 1) 프로젝트 로컬 assets 경로 우선
        img = ImageUtil.loadFile("assets/maps/" + base + ".png");
        if (img == null) img = ImageUtil.loadFile("assets/maps/" + base + ".jpg");
        // 2) 클래스패스 리소스
        if (img == null) img = ImageUtil.loadResource(MapVotePanel.class, "/assets/maps/" + base + ".png");
        if (img == null) img = ImageUtil.loadResource(MapVotePanel.class, "/assets/maps/" + base + ".jpg");
        if (img != null) {
            img = ImageUtil.scale(img, w, h);
            target.setIcon(new ImageIcon(img));
        } else {
            target.setText("No Image");
        }
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

