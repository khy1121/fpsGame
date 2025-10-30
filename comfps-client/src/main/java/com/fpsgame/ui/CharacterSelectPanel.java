package com.fpsgame.client.ui;

import com.fpsgame.common.GameEnums;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/**
 * 캐릭터 선택 패널(경량)
 * - 카드 그리드로 선택 버튼 제공
 * - 선택 시 콜백으로 characterId 전달
 * - 공개 API는 EDT에서 안전하게 호출 가능
 */
public class CharacterSelectPanel extends JPanel {

    // 선택 전송 콜백(characterId)
    private volatile Consumer<String> selectionSender;

    // 카드가 배치될 그리드 컨테이너
    private final JPanel grid = new JPanel(new GridLayout(0, 5, 10, 10));

    // 현재 표시할 캐릭터 ID 목록
    private final List<String> characters = new ArrayList<>();

    public CharacterSelectPanel() {
        setLayout(new BorderLayout(10, 10));
        setOpaque(true);
        setBackground(new Color(24, 24, 24));
        setBorder(new EmptyBorder(12, 12, 12, 12));

        JLabel title = new JLabel("Character Select");
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

    /** 기본 캐릭터로 채우기(예시용). */
    public void withDefaultCharacters() {
        setCharacters(List.of(
                "Sage","Piper","Technician","General","Bulldog",
                "Wildcat","Raven","Ghost","Skull","Steam"
        ));
    }

    /** 캐릭터 목록을 갱신한다. null 허용. */
    public void setCharacters(List<String> ids) {
        runEdt(() -> {
            characters.clear();
            if (ids != null) {
                for (String id : ids) {
                    if (id != null && !id.isBlank()) characters.add(id.trim());
                }
            }
            rebuildGrid();
        });
    }

    /** 선택 전송 콜백을 설정한다. null이면 비활성. */
    public void onSendSelection(Consumer<String> sender) {
        this.selectionSender = sender;
    }

    // 내부 구현 --------------------------------------------------------

    private void rebuildGrid() {
        grid.removeAll();

        if (characters.isEmpty()) {
            // 캐릭터 목록이 비어 있을 때 메시지 표시
            JLabel empty = new JLabel("캐릭터 정보가 없습니다.", SwingConstants.CENTER);
            empty.setForeground(new Color(180, 180, 180));
            grid.setLayout(new GridLayout(1, 1));
            grid.add(wrap(empty));
        } else {
            int cols = 5; // 5열 그리드            grid.setLayout(new GridLayout(Math.max(1, (characters.size() + cols - 1) / cols), cols, 10, 10));
            for (String id : characters) {
                grid.add(buildCard(id));
            }
        }

        grid.revalidate();
        grid.repaint();
    }

    private JComponent buildCard(String characterId) {
        JPanel card = new JPanel(new BorderLayout(6, 6));
        card.setOpaque(true);
        card.setBackground(new Color(36, 36, 36));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(60, 60, 60)),
                new EmptyBorder(10, 10, 10, 10)
        ));

        JLabel name = new JLabel(pretty(characterId));
        name.setForeground(new Color(235, 235, 235));
        name.setFont(name.getFont().deriveFont(Font.BOLD, 14f));

        // 캐릭터 이미지(있을 경우)
        JLabel image = new JLabel();
        image.setHorizontalAlignment(SwingConstants.CENTER);
        image.setOpaque(false);
        BufferedImageLoader.loadCharacter(characterId, image, 120, 120);

        // 설명 영역(읽기 전용)
        JTextArea desc = new JTextArea(defaultDescription(characterId));
        desc.setEditable(false);
        desc.setOpaque(false);
        desc.setForeground(new Color(180, 180, 180));
        desc.setFont(desc.getFont().deriveFont(12f));
        desc.setLineWrap(true);
        desc.setWrapStyleWord(true);
        desc.setBorder(new EmptyBorder(4, 0, 8, 0));

        JButton choose = new JButton("Choose");
        choose.addActionListener(e -> sendSelection(characterId));

        card.add(name, BorderLayout.NORTH);
        JPanel center = new JPanel(new BorderLayout());
        center.setOpaque(false);
        center.add(image, BorderLayout.CENTER);
        center.add(desc, BorderLayout.SOUTH);
        card.add(center, BorderLayout.CENTER);

        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        south.setOpaque(false);
        south.add(choose);
        card.add(south, BorderLayout.SOUTH);

        // 카드 전체 클릭 가능 처리
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) { sendSelection(characterId); }
        });

        return card;
    }

    private void sendSelection(String characterId) {
        Consumer<String> sender = this.selectionSender;
        if (sender != null) {
            try {
                sender.accept(Objects.toString(characterId, ""));
            } catch (Throwable ignore) {
                // UI에서 조용히 무시
            }
        }
    }

    private static String pretty(String id) {
        if (id == null || id.isBlank()) return "-";
        // 예: "ghostRider" -> "Ghost Rider"
        StringBuilder sb = new StringBuilder();
        char prev = 0;
        for (char ch : id.toCharArray()) {
            if (prev == 0 || prev == '_' || prev == '-') {
                sb.append(Character.toUpperCase(ch));
            } else if (Character.isUpperCase(ch) && Character.isLowerCase(prev)) {
                sb.append(' ').append(ch);
            } else {
                sb.append(ch);
            }
            prev = ch;
        }
        return sb.toString().replace('_', ' ').replace('-', ' ');
    }

    // 간단 설명 더미(필요 시 외부에서 setCharacters와 함께 교체 가능)
    private static String defaultDescription(String id) {
        if (id == null) return "";
        String key = id.toLowerCase();
        return switch (key) {
            case "sage" -> "지원형: 회복/보조에 강점";
            case "piper" -> "정찰형: 시야/표식";
            case "technician" -> "공학: 유틸리티 장비";
            case "general" -> "밸런스형";
            case "bulldog" -> "화력: 미니건/압박";
            case "wildcat" -> "기동력/근접";
            case "raven" -> "기동/돌파";
            case "ghost" -> "은신/기만";
            case "skull" -> "공포/디버프";
            case "steam" -> "연막/제어";
            default -> "캐릭터 설명이 표시됩니다.";
        };
    }

    /** 내부 이미지 로더: 파일 또는 리소스에서 불러와 흰색 배경 제거 후 아이콘 설정 */
    static final class BufferedImageLoader {
        static void loadCharacter(String id, JLabel target, int w, int h) {
            if (target == null) return;
            String base = id == null ? "" : id.trim();
            BufferedImage img = null;
            // 1) 프로젝트 로컬 assets 경로 우선
            img = com.fpsgame.client.ui.ImageUtil.loadFile("assets/characters/" + base + ".png");
            if (img == null) img = com.fpsgame.client.ui.ImageUtil.loadFile("assets/characters/" + base + ".jpg");
            // 2) 클래스패스 리소스
            if (img == null) img = com.fpsgame.client.ui.ImageUtil.loadResource(CharacterSelectPanel.class, "/assets/characters/" + base + ".png");
            if (img == null) img = com.fpsgame.client.ui.ImageUtil.loadResource(CharacterSelectPanel.class, "/assets/characters/" + base + ".jpg");
            if (img != null) {
                img = com.fpsgame.client.ui.ImageUtil.whiteToTransparent(img, 20);
                img = com.fpsgame.client.ui.ImageUtil.scale(img, w, h);
                target.setIcon(new ImageIcon(img));
            } else {
                target.setText("");
            }
        }
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

    GameEnums.CharacterId getSelected() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setOnSelectEnum(Object object) {
        throw new UnsupportedOperationException("Unimplemented method 'setOnSelectEnum'");
    }
}
