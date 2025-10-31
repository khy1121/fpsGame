package com.fpsgame;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Path;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;

import com.fpsgame.client.model.Settings;
import com.fpsgame.client.ui.LobbyFrame;
import com.fpsgame.client.ui.OptionsWindow;

/**
 * FPS 게임 메인 런처
 * - 닉네임 입력 및 설정
 * - 게임 시작, 설정, 나가기 버튼
 * - 오버워치/발로란트 스타일 UI
 */
public final class MainLauncher extends JFrame {

    private final Settings settings = Settings.defaults();
    private final Path settingsFile = Path.of(System.getProperty("user.home"), ".fpsgame", "client.properties");

    private final JTextField nicknameField = new JTextField("Player");
    private LobbyFrame lobbyFrame;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
            catch (Exception ignore) {}
            
            MainLauncher launcher = new MainLauncher();
            launcher.setVisible(true);
        });
    }

    public MainLauncher() {
        super("FPS Client");
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setSize(1060, 720);
        setLocationRelativeTo(null);
        setResizable(false);

        // 설정 로드
        loadSettings();

        // UI 구성
        buildUI();

        // 윈도우 닫기 이벤트
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                exitApplication();
            }
        });
    }

    private void loadSettings() {
        try {
            settings.load(settingsFile);
            // 설정에서 닉네임 가져오기 (구현 필요)
        } catch (Exception ex) {
            System.err.println("[warn] Failed to load settings: " + ex.getMessage());
        }
    }

    private void buildUI() {
        // 메인 패널 (어두운 배경)
        JPanel mainPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // 그라데이션 배경
                GradientPaint gp = new GradientPaint(
                    0, 0, new Color(0x1a1d24),
                    0, getHeight(), new Color(0x0d0f13)
                );
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        mainPanel.setLayout(new GridBagLayout());
        setContentPane(mainPanel);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.insets = new Insets(12, 12, 12, 12);

        // 타이틀
        gbc.gridy = 0;
        JLabel titleLabel = new JLabel("FPS GAME");
        titleLabel.setFont(new Font("맑은 고딕", Font.BOLD, 48));
        titleLabel.setForeground(new Color(0xffd700)); // 골드
        mainPanel.add(titleLabel, gbc);

        // 버전
        gbc.gridy = 1;
        JLabel versionLabel = new JLabel("v1.0.0");
        versionLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 14));
        versionLabel.setForeground(new Color(0x999999));
        mainPanel.add(versionLabel, gbc);

        // 간격
        gbc.gridy = 2;
        mainPanel.add(Box.createVerticalStrut(30), gbc);

        // 닉네임 라벨
        gbc.gridy = 3;
        JLabel nicknameLabel = new JLabel("닉네임");
        nicknameLabel.setFont(new Font("맑은 고딕", Font.BOLD, 16));
        nicknameLabel.setForeground(Color.WHITE);
        mainPanel.add(nicknameLabel, gbc);

        // 닉네임 입력 필드
        gbc.gridy = 4;
        nicknameField.setPreferredSize(new Dimension(260, 40));
        nicknameField.setFont(new Font("맑은 고딕", Font.PLAIN, 16));
        nicknameField.setHorizontalAlignment(JTextField.CENTER);
        nicknameField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0x3a3f47), 2),
            new EmptyBorder(8, 12, 8, 12)
        ));
        mainPanel.add(nicknameField, gbc);

        // 간격
        gbc.gridy = 5;
        mainPanel.add(Box.createVerticalStrut(20), gbc);

        // 게임 시작 버튼 (GREEN)
        gbc.gridy = 6;
        JButton startButton = createStyledButton("게임 시작", new Color(0x4caf50), new Color(0x45a049));
        startButton.addActionListener(e -> startGame());
        mainPanel.add(startButton, gbc);

        // 설정 버튼 (GRAY)
        gbc.gridy = 7;
        JButton settingsButton = createStyledButton("설정", new Color(0x5a6268), new Color(0x4e555b));
        settingsButton.addActionListener(e -> openSettings());
        mainPanel.add(settingsButton, gbc);

        // 나가기 버튼 (RED)
        gbc.gridy = 8;
        JButton exitButton = createStyledButton("나가기", new Color(0xd32f2f), new Color(0xc62828));
        exitButton.addActionListener(e -> exitApplication());
        mainPanel.add(exitButton, gbc);
    }

    private JButton createStyledButton(String text, Color bgColor, Color hoverColor) {
        // 흰색 배경 쪽으로 톤을 올려 검정 글씨 대비 확보
        final Color baseFill = lighten(bgColor, 0.40);
        final Color hoverFill = lighten(hoverColor, 0.30);

        JButton button = new JButton(text) {
            private boolean isHovered = false;

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                Color currentColor = isHovered ? hoverFill : baseFill;
                g2.setColor(currentColor);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);

                g2.setColor(Color.BLACK); // 한글 검정 글씨
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(getText())) / 2;
                int y = (getHeight() + fm.getAscent()) / 2 - 2;
                g2.drawString(getText(), x, y);
            }

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(260, 50);
            }
        };

        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
    // 한글 가독성 좋은 폰트 사용
    button.setFont(new Font("맑은 고딕", Font.BOLD, 16));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Hover effect
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                try {
                    java.lang.reflect.Field f = button.getClass().getDeclaredField("isHovered");
                    f.setAccessible(true);
                    f.set(button, true);
                } catch (Exception ignore) {}
                button.repaint();
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                try {
                    java.lang.reflect.Field f = button.getClass().getDeclaredField("isHovered");
                    f.setAccessible(true);
                    f.set(button, false);
                } catch (Exception ignore) {}
                button.repaint();
            }
        });

        return button;
    }

    /** 원색을 흰색 쪽으로 f(0~1)만큼 보간하여 밝게 만든다. */
    private static Color lighten(Color c, double f) {
        f = Math.max(0.0, Math.min(1.0, f));
        int r = (int)Math.round(c.getRed()   + (255 - c.getRed())   * f);
        int g = (int)Math.round(c.getGreen() + (255 - c.getGreen()) * f);
        int b = (int)Math.round(c.getBlue()  + (255 - c.getBlue())  * f);
        return new Color(r, g, b);
    }

    private void startGame() {
        String nickname = nicknameField.getText().trim();
        if (nickname.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "닉네임을 입력해주세요!", 
                "알림", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        // 설정에 닉네임 저장
        saveSettings();

        // 로비 프레임 열기
        if (lobbyFrame == null || !lobbyFrame.isDisplayable()) {
            lobbyFrame = new LobbyFrame(nickname, settings);
        }
        lobbyFrame.setVisible(true);

        // 런처 숨기기
        setVisible(false);
    }

    private void openSettings() {
        // 옵션 창 열기: 현재 설정 값을 반영하고 저장 시 Settings에 적용 후 즉시 저장
        OptionsWindow w = new OptionsWindow(settings.getKeybinds());

        // 현재 설정 → 다이얼로그 슬라이더 반영
        w.setVolume(settings.getMasterVolume());
        w.setSensitivityPercent(sensitivityFloatToPercent(settings.getMouseSensitivity()));

        w.setSaveListener((props, volume, sensitivityPct) -> {
            // 키바인딩 저장
            settings.getKeybinds().load(props);
            // 볼륨/감도 저장
            settings.setMasterVolume(volume);
            settings.setMouseSensitivity(sensitivityPercentToFloat(sensitivityPct));
            // 즉시 저장
            saveSettings();
        });

        w.setLocationRelativeTo(this);
        w.setVisible(true);
    }

    // 감도 매핑: Settings(0.1~2.0) ⇄ UI(1~100)
    private static int sensitivityFloatToPercent(float sens) {
        float clamped = Math.max(0.1f, Math.min(2.0f, sens));
        // 0.1 -> 1, 2.0 -> 100 (선형 매핑)
        int pct = Math.round(((clamped - 0.1f) / 1.9f) * 99f) + 1;
        if (pct < 1) pct = 1; if (pct > 100) pct = 100;
        return pct;
    }

    private static float sensitivityPercentToFloat(int pct) {
        int p = Math.max(1, Math.min(100, pct));
        // 1 -> 0.1, 100 -> 2.0 (선형 매핑)
        return 0.1f + ((p - 1) / 99f) * 1.9f;
    }

    private void saveSettings() {
        try {
            settings.save(settingsFile, "FPS Client Settings");
        } catch (Exception ex) {
            System.err.println("[warn] Failed to save settings: " + ex.getMessage());
        }
    }

    private void exitApplication() {
        int result = JOptionPane.showConfirmDialog(this,
            "게임을 종료하시겠습니까?",
            "종료 확인",
            JOptionPane.YES_NO_OPTION);

        if (result == JOptionPane.YES_OPTION) {
            saveSettings();
            dispose();
            System.exit(0);
        }
    }
}
