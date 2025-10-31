package com.fpsgame.client.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

/**
 * 맵 정보 패널
 * - 3개 맵 카드 표시 (terminal, neonCity, forestOutpost)
 * - 그리드 레이아웃
 */
public class MapInfoPanel extends JPanel {

    private final String[] mapIds = {"terminal", "neonCity", "forestOutpost"};
    private final JPanel gridPanel = new JPanel(new GridLayout(1, 3, 16, 16));

    public MapInfoPanel() {
        setLayout(new BorderLayout());
        setOpaque(false);
        setBorder(new EmptyBorder(12, 12, 12, 12));

        gridPanel.setOpaque(false);

        for (String mapId : mapIds) {
            gridPanel.add(buildMapCard(mapId));
        }

        add(gridPanel, BorderLayout.CENTER);
    }

    private JPanel buildMapCard(String mapId) {
        JPanel card = new JPanel(new BorderLayout(8, 8));
        card.setBackground(new Color(0x2a2f38));
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0x3a3f47), 2),
            new EmptyBorder(12, 12, 12, 12)
        ));

        // 맵 이름
        JLabel nameLabel = new JLabel(getMapName(mapId), SwingConstants.CENTER);
        nameLabel.setForeground(Color.WHITE);
        nameLabel.setFont(new Font("맑은 고딕", Font.BOLD, 20));

        // 맵 이미지
        JLabel imageLabel = new JLabel();
        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        imageLabel.setPreferredSize(new Dimension(350, 220));
        imageLabel.setBackground(new Color(0x1a1d24));
        imageLabel.setOpaque(true);
        loadMapImage(mapId, imageLabel);

        // 맵 설명
        JTextArea descArea = new JTextArea(getMapDescription(mapId));
        descArea.setEditable(false);
        descArea.setOpaque(false);
        descArea.setForeground(new Color(0xcccccc));
        descArea.setFont(new Font("맑은 고딕", Font.PLAIN, 14));
        descArea.setLineWrap(true);
        descArea.setWrapStyleWord(true);
        descArea.setBorder(new EmptyBorder(10, 0, 0, 0));

        card.add(nameLabel, BorderLayout.NORTH);
        card.add(imageLabel, BorderLayout.CENTER);
        card.add(descArea, BorderLayout.SOUTH);

        return card;
    }

    private String getMapName(String mapId) {
        return switch (mapId.toLowerCase()) {
            case "terminal" -> "Terminal";
            case "neoncity" -> "Neon City";
            case "forestoutpost" -> "Forest Outpost";
            default -> mapId;
        };
    }

    private String getMapDescription(String mapId) {
        return switch (mapId.toLowerCase()) {
            case "terminal" -> "도시 공항\n좁은 복도와 개방된 광장\n밸런스형 전투";
            case "neoncity" -> "사이버펑크 도시\n수직 레벨과 네온 거리\n기동력 중시";
            case "forestoutpost" -> "숲속 요새\n자연 엄폐와 벙커\n전술적 플레이";
            default -> "맵 설명이 표시됩니다.";
        };
    }

    private void loadMapImage(String mapId, JLabel target) {
        BufferedImage img = null;
        
        // 1) 클래스패스 리소스 우선
        img = ImageUtil.loadResource(MapInfoPanel.class, "/assets/maps/" + mapId + ".png");
        if (img == null) img = ImageUtil.loadResource(MapInfoPanel.class, "/assets/maps/" + mapId + ".jpg");
        
        // 2) 파일 경로
        if (img == null) img = ImageUtil.loadFile("assets/maps/" + mapId + ".png");
        if (img == null) img = ImageUtil.loadFile("assets/maps/" + mapId + ".jpg");

        if (img != null) {
            img = ImageUtil.scale(img, 350, 220); // 350x220으로 스케일 통일
            target.setIcon(new ImageIcon(img));
            target.setText(""); // 이미지 로드 성공 시 텍스트 제거
        } else {
            target.setText(mapId); // 이미지 없으면 ID 표시
            target.setForeground(Color.WHITE);
            target.setFont(new Font("맑은 고딕", Font.BOLD, 24));
            System.err.println("Failed to load map image: " + mapId);
        }
    }
}
