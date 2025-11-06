package com.fpsgame.client;

import com.fpsgame.client.ui.*;

import javax.swing.*;
import java.awt.*;

/**
 * 클라이언트 측 디버그/도구 실행 진입점
 * - 각종 샌드박스/콘솔/설정 화면을 버튼으로 실행
 * - 독립 실행 가능한 가벼운 툴 모음
 */
public final class ClientToolsMain {

    private ClientToolsMain() {}

    public static void main(String[] args) {
        // 시스템 기본 Look & Feel 적용(가능한 경우)
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("FPS Client Tools");
            f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            f.setSize(520, 360);
            f.setLocationRelativeTo(null);

            JPanel root = new JPanel(new BorderLayout());
            root.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

            JLabel title = new JLabel("FPS Client Tools");
            title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));
            root.add(title, BorderLayout.NORTH);

            JPanel grid = new JPanel(new GridLayout(0, 1, 8, 8));

            // Net Playground 실행
            JButton btnPlayground = new JButton("Open Net Playground");
            btnPlayground.setToolTipText("네트워크 샌드박스");
            btnPlayground.addActionListener(e -> new NetPlaygroundFrame().setVisible(true));
            grid.add(btnPlayground);

            // 채팅/프레임 콘솔 실행
            JButton btnConsole = new JButton("Open Chat/Frame Console");
            btnConsole.setToolTipText("네트워크 프레임/채팅 로그 확인");
            btnConsole.addActionListener(e -> new ChatConsoleFrame().setVisible(true));
            grid.add(btnConsole);

            // 설정 화면 실행
            JButton btnSettings = new JButton("Open Settings");
            btnSettings.setToolTipText("설정 패널 실행");
            btnSettings.addActionListener(e -> new SettingsFrame().setVisible(true));
            grid.add(btnSettings);

            // Raw Canvas(Net Demo) 실행
            JButton btnCanvas = new JButton("Open Raw Canvas (Net Demo)");
            btnCanvas.setToolTipText("간단 캔버스 + 입력 송신 데모");
            btnCanvas.addActionListener(e -> {
                JFrame w = new JFrame("Raw Canvas (Net Demo)");
                w.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
                w.setSize(1000, 640);
                w.setLocationRelativeTo(null);
                GameCanvasNetDemo canvas = new GameCanvasNetDemo();
                w.setContentPane(canvas);
                w.addWindowListener(new java.awt.event.WindowAdapter() {
                    @Override public void windowClosing(java.awt.event.WindowEvent e2) {
                        try { canvas.close(); } catch (Exception ignored) {}
                    }
                });
                w.setVisible(true);

                // 실행 직후 연결 다이얼로그 표시(선택)
                SwingUtilities.invokeLater(() -> {
                    JoinDialog.Result r = JoinDialog.show(w, "127.0.0.1", 7777, System.getProperty("user.name", "player"));
                    if (r != null) {
                        try {
                            canvas.connect(r.host, r.port);
                            Toast.success(w, "Connected");
                        } catch (Exception ex) {
                            JOptionPane.showMessageDialog(w, "Connect Error: " + ex.getMessage(),
                                    "Connect Error", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                });
            });
            grid.add(btnCanvas);

            root.add(grid, BorderLayout.CENTER);

            // 간단 설명 라벨
            JTextArea desc = new JTextArea(
                    "Tools: Net Playground / Console / Settings / Raw Canvas");
            desc.setEditable(false);
            desc.setOpaque(false);
            desc.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
            root.add(desc, BorderLayout.SOUTH);

            f.setContentPane(root);
            f.setVisible(true);
        });
    }
}

