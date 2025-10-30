package com.fpsgame.client.ui;

import com.fpsgame.client.NetClient;
import com.fpsgame.client.model.RttMonitor;
import com.fpsgame.common.Protocol;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;

/**
 * 실제 게임 화면 형태를 모사한 통합 프레임.
 *
 * <p>구성</p>
 * <ul>
 *   <li>중앙: {@link GameCanvasNetDemo} — 월드 렌더 & 입력 전송</li>
 *   <li>상단 겹침: {@link HudOverlayLayer} — HUD/RTT/FPS/센터 메시지</li>
 *   <li>상단 툴바: Connect / Disconnect / Ready / CenterMsg</li>
 * </ul>
 *
 * <p>목적</p>
 * - 기존 샌드박스와 HUD 유틸을 하나의 화면에서 함께 검증.
 * - 서버 측 {@code PlayerSyncService + PlayerServerRouter + NetTickLoop} 조합과의 연동을
 *   실제 게임 레이아웃에 가깝게 테스트.
 */
public final class GameScreenFrame extends JFrame {

    // ===== 뷰 & 오버레이 =====
    private final GameCanvasNetDemo canvas = new GameCanvasNetDemo();
    private final HudOverlayLayer overlay = new HudOverlayLayer();

    // ===== 네트워크 상태 =====
    private NetClient net;
    private RttMonitor rtt;

    public GameScreenFrame() {
        super("FPS Game Screen (Demo)");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setSize(1280, 768);
        setLocationRelativeTo(null);

        // 중앙에 캔버스 + 그 위에 HUD를 겹치기 위한 레이어드팬
        JLayeredPane root = new JLayeredPane();
        setContentPane(root);
        root.setLayout(null);

        // 캔버스(바닥)
        root.add(canvas, Integer.valueOf(0));
        // HUD(위)
        root.add(overlay, Integer.valueOf(10));

        // 리사이즈 시 자식 크기 동기화
        root.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override public void componentResized(java.awt.event.ComponentEvent e) {
                int w = root.getWidth(), h = root.getHeight();
                canvas.setBounds(0, 0, w, h);
                overlay.setBounds(0, 0, w, h);
            }
        });

        // 상단 툴바
        JToolBar tb = new JToolBar();
        tb.setFloatable(false);
        JButton btnConnect = new JButton("Connect");
        JButton btnDisconnect = new JButton("Disconnect");
        JButton btnReady = new JButton("Ready");
        JButton btnMsg = new JButton("CenterMsg");
        tb.add(btnConnect);
        tb.add(btnDisconnect);
        tb.addSeparator();
        tb.add(btnReady);
        tb.add(btnMsg);
        setJMenuBar(buildMenuBar(btnConnect, btnDisconnect, btnReady, btnMsg));

        // FPS 오버레이 작동
        new Timer(16, e -> overlay.getFpsPanel().frameArrived()).start();

        // 버튼 액션
        btnConnect.addActionListener(e -> doConnect());
        btnDisconnect.addActionListener(e -> doDisconnect());
        btnMsg.addActionListener(e -> overlay.getCenterOverlay().showMessage("ROUND START", 1500));
        btnReady.addActionListener(e -> sendReadyToggle());

        // ESC로 테스트 메시지 출력
        canvas.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    overlay.getCenterOverlay().showMessage("PAUSED", 900);
                }
            }
        });

        // 창 닫힐 때 정리
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                cleanup();
            }
        });
    }

    // ======================= 메뉴 =======================

    private JMenuBar buildMenuBar(JButton btnConnect, JButton btnDisconnect, JButton btnReady, JButton btnMsg) {
        JMenuBar mb = new JMenuBar();

        JMenu mFile = new JMenu("Session");
        JMenuItem miConnect = new JMenuItem("Connect...");
        JMenuItem miDisconnect = new JMenuItem("Disconnect");
        JMenuItem miExit = new JMenuItem("Exit");
        miConnect.addActionListener(e -> btnConnect.doClick());
        miDisconnect.addActionListener(e -> btnDisconnect.doClick());
        miExit.addActionListener(e -> dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING)));
        mFile.add(miConnect);
        mFile.add(miDisconnect);
        mFile.addSeparator();
        mFile.add(miExit);

        JMenu mGame = new JMenu("Game");
        JMenuItem miReady = new JMenuItem("Toggle Ready");
        JMenuItem miMsg = new JMenuItem("Center Message");
        miReady.addActionListener(e -> btnReady.doClick());
        miMsg.addActionListener(e -> btnMsg.doClick());
        mGame.add(miReady);
        mGame.add(miMsg);

        mb.add(mFile);
        mb.add(mGame);
        return mb;
    }

    // ======================= 연결/해제 =======================

    private void doConnect() {
        JoinDialog.Result r = JoinDialog.show(this, "127.0.0.1", 7777, System.getProperty("user.name", "player"));
        if (r == null) return;

        try {
            doDisconnect(); // 기존 연결 정리

            // NetClient 리스너: RTT 전달만 처리(필요 시 확장 가능)
            NetClient.Listener listener = new NetClient.Listener() {
                @Override public void onOpen(NetClient c) {
                    SwingUtilities.invokeLater(() -> {
                        net = c;
                        Toast.success(GameScreenFrame.this, "Connected " + c.remoteAddress());
                        overlay.getHudBar().setPhaseText("CONNECTED");
                    });
                }
                @Override public void onClosed(NetClient c, String reason) {
                    SwingUtilities.invokeLater(() -> {
                        Toast.error(GameScreenFrame.this, "Closed: " + reason);
                        overlay.getHudBar().setPhaseText("CLOSED");
                        overlay.getRttGraph().addSample(0);
                    });
                }
                @Override public void onDisconnected(String reason) {
                    SwingUtilities.invokeLater(() -> {
                        Toast.error(GameScreenFrame.this, "Disconnected");
                        overlay.getHudBar().setPhaseText("DISCONNECTED");
                        overlay.getRttGraph().addSample(0);
                    });
                }
                @Override public void onChat(String text) {
                    // 필요 시 HUD로 플로팅 메시지를 띄울 수 있음
                }
                @Override public void onFrame(NetClient c, Protocol.Frame frame) {
                    // 현재는 RTT만 사용 → rtt.onFrame에서 처리
                    if (rtt != null) rtt.onFrame(frame);
                }
            };

            net = new NetClient(r.host, r.port, 10_000, listener);
            canvas.connect(r.host, r.port);

            // RTT 모니터 시작 → HUD에 그래프/텍스트 반영
            rtt = new RttMonitor(net, ms -> SwingUtilities.invokeLater(() -> {
                overlay.getRttGraph().addSample(ms);
            }));
            rtt.start(1000);

            overlay.getHudBar().setPhaseText("CONNECTING...");
            overlay.getCenterOverlay().showMessage("CONNECTING", 800);
        } catch (IOException ex) {
            Toast.error(this, "Connect failed: " + ex.getMessage());
        }
    }

    private void doDisconnect() {
        if (rtt != null) {
            try { rtt.close(); } catch (Exception ignored) {}
            rtt = null;
        }
        canvas.disconnect();
        if (net != null) {
            try { net.close(); } catch (Exception ignored) {}
            net = null;
        }
        overlay.getHudBar().setPhaseText("DISCONNECTED");
        overlay.getRttGraph().addSample(0);
    }

    private void cleanup() {
        doDisconnect();
        try { canvas.close(); } catch (Exception ignored) {}
    }

    // ======================= 액션(예: READY 토글) =======================

    /** 프로토콜 확장 전까지는 단순 메시지로 대체. */
    private void sendReadyToggle() {
        overlay.getCenterOverlay().showMessage("READY TOGGLED", 900);
        // 실제 READY opcode가 있다면 여기서 net.send(...) 호출로 연동
    }

    // ======================= 엔트리 =======================

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}
            new GameScreenFrame().setVisible(true);
        });
    }
}
