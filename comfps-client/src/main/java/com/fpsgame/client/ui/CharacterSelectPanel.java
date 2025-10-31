package com.fpsgame.client.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;
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

import com.fpsgame.common.GameEnums;

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
        title.setFont(new Font("맑은 고딕", Font.BOLD, 18));
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
        name.setFont(new Font("맑은 고딕", Font.BOLD, 15));

        // 캐릭터 이미지(있을 경우) - 어두운 배경 + 테두리로 강조
        JLabel image = new JLabel();
        image.setHorizontalAlignment(SwingConstants.CENTER);
        image.setOpaque(true);
        image.setBackground(new Color(18, 18, 18)); // 더 어두운 배경
        image.setPreferredSize(new java.awt.Dimension(140, 140));
        image.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(80, 80, 80), 2), // 테두리 강조
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        BufferedImageLoader.loadCharacter(characterId, image, 140, 140);

        // 설명 영역(읽기 전용)
        JTextArea desc = new JTextArea(defaultDescription(characterId));
        desc.setEditable(false);
        desc.setOpaque(false);
        desc.setForeground(new Color(180, 180, 180));
        desc.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        desc.setLineWrap(true);
        desc.setWrapStyleWord(true);
        desc.setBorder(new EmptyBorder(6, 0, 10, 0));

        JButton choose = new JButton("Choose");
        choose.setFocusPainted(false); // 포커스 테두리 제거
        choose.setFont(new Font("맑은 고딕", Font.BOLD, 12));
        choose.setBackground(new Color(60, 120, 180));
        choose.setForeground(Color.WHITE);
        choose.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(80, 140, 200), 1),
            BorderFactory.createEmptyBorder(6, 12, 6, 12) // 패딩 추가
        ));
        choose.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
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

    // CHARACTER_SPECS.md 기반 캐릭터 설명
    private static String defaultDescription(String id) {
        if (id == null) return "";
        String key = id.toLowerCase();
        return switch (key) {
            case "raven" -> "돌격형 | Basic: 빠른 속도 | Tactical: 시야 가리기 | Ultimate: 순간이동";
            case "piper" -> "정찰형 | Basic: 장거리 정밀 공격 | Tactical: 적 위치 표시 | Ultimate: 적 무력화";
            case "bulldog" -> "중화기 | Basic: 미니건 제압사격 | Tactical: 탄약 재장전 | Ultimate: 집중 화력";
            case "sage" -> "지원형 | Basic: 회복 기술 | Tactical: 방어막 | Ultimate: 회복 영역";
            case "ghost" -> "암살형 | Basic: 은신 | Tactical: 환영 생성 | Ultimate: 무음 이동";
            case "wildcat" -> "근접형 | Basic: 빠른 근접 공격 | Tactical: 돌진 | Ultimate: 광폭화";
            case "technician" -> "공학형 | Basic: 터렛 설치 | Tactical: 원격 해킹 | Ultimate: EMP 폭파";
            case "general" -> "전술가 | Basic: 밸런스 공격 | Tactical: 전술 명령 | Ultimate: 폭격 요청";
            case "steam" -> "연막형 | Basic: 연막탄 | Tactical: 시야 차단 | Ultimate: 독가스";
            case "skull" -> "공포형 | Basic: 적 두려움 | Tactical: 디버프 | Ultimate: 대규모 공포";
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
                target.setText(""); // 이미지 로드 성공 시 텍스트 제거
            } else {
                target.setText(base); // 이미지 없으면 캐릭터 ID 표시
                target.setForeground(Color.WHITE);
                target.setFont(new java.awt.Font("맑은 고딕", java.awt.Font.BOLD, 14));
                System.err.println("Failed to load character image: " + base);
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
