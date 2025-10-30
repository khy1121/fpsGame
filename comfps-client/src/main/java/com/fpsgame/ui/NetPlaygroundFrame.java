package com.fpsgame.client.ui;

import com.fpsgame.client.NetClient;
import com.fpsgame.client.model.RttMonitor;
import com.fpsgame.common.Protocol;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;

/**
 * 네트워크 샌드박스 프레임.
 *
 * <p>구성</p>
 * <ul>
 *   <li>{@link GameCanvasNetDemo} : 중앙 월드 렌더 & 입력 전송</li>
 *   <li>{@link FpsOverlayPanel}   : 좌상단 FPS 표시(GlassPane)</li>
 *   <li>상단 툴바: Connect / Disconnect / Ping</li>
 *   <li>상태바: 연결 상태 및 RTT</li>
 * </ul>
 *
 * <p>목적</p>
 * - 서버의 {@code PlayerSyncService} + {@code PlayerServerRouter} + {@code NetTickLoop}
 *   조합을 빠르게 검증할 수 있는 단독 실행 테스트 UI.
 */
public final class NetPlaygroundFrame extends JFrame {

    private final GameCanvasNetDemo canvas = new GameCanvasNetDemo();
    private final JLabel status = new JLabel("Disconnected");
    private final JLabel rttLabel = new JLabel("RTT —");

    /** RTT 측정기(연결 시 생성, 해제 시 close) */
    private RttMonitor rtt;

    /** 현재 연결 객체(있을 수도, 없을 수도) */
    private NetClient net;

    public NetPlaygroundFrame() {
        super("FPS Net Playground");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setSize(1100, 700);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // 중앙: 캔버스
        add(canvas, BorderLayout.CENTER);

        // 상단: 툴바
        JToolBar tb = new JToolBar();
        tb.setFloatable(false);
        JButton btnConnect = new JButton("Connect");
        JButton btnDisconnect = new JButton("Disconnect");
        JButton btnPing = new JButton("Ping");
        tb.add(btnConnect);
        tb.add(btnDisconnect);
        tb.addSeparator();
        tb.add(btnPing);
        add(tb, BorderLayout.NORTH);

        // 하단: 상태바
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        status.setForeground(new Color(200, 200, 200));
        rttLabel.setForeground(new Color(160, 200, 255));
        bottom.add(status, BorderLayout.WEST);
        bottom.add(rttLabel, BorderLayout.EAST);
        add(bottom, BorderLayout.SOUTH);

        // FPS 오버레이(좌상단)
        FpsOverlayPanel fps = new FpsOverlayPanel();
        JComponent glass = (JComponent) getRootPane().getGlassPane();
        glass.setVisible(true);
        glass.setLayout(null);
        glass.add(fps);
        fps.setBounds(10, 10, fps.getPreferredSize().width, fps.getPreferredSize().height);
        // 60fps 근사 타이머 → FPS 계산용
        new Timer(16, e -> fps.frameArrived()).start();

        // 버튼 액션
        btnConnect.addActionListener(e -> onConnect());
        btnDisconnect.addActionListener(e -> onDisconnect());
        btnPing.addActionListener(e -> {
            if (rtt != null && rtt.pingOnce()) {
                Toast.show(this, "PING...", 700);
            } else {
                Toast.error(this, "Not connected");
            }
        });

        // 창 닫힘 시 리소스 정리
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                cleanup();
            }
        });
    }

    // ================= 연결/해제 =================

    private void onConnect() {
        JoinDialog.Result r = JoinDialog.show(this, "127.0.0.1", 7777, System.getProperty("user.name", "player"));
        if (r == null) return;

        try {
            // 기존 연결이 있으면 종료
            onDisconnect();

            // NetClient를 만들어 리스너로 RTT/스냅샷 훅을 달고 상태 표시
            NetClient.Listener listener = new NetClient.Listener() {
                @Override public void onOpen(NetClient c) {
                    SwingUtilities.invokeLater(() -> {
                        net = c;
                        status.setText("Connected " + c.remoteAddress());
                        Toast.success(NetPlaygroundFrame.this, "Connected");
                    });
                }

                @Override public void onClosed(NetClient c, String reason) {
                    SwingUtilities.invokeLater(() -> {
                        status.setText("Closed: " + reason);
                        rttLabel.setText("RTT —");
                        Toast.error(NetPlaygroundFrame.this, "Closed: " + reason);
                    });
                }

                @Override public void onDisconnected(String reason) {
                    SwingUtilities.invokeLater(() -> {
                        status.setText("Disconnected: " + reason);
                        rttLabel.setText("RTT —");
                        Toast.error(NetPlaygroundFrame.this, "Disconnected");
                    });
                }

                @Override public void onChat(String text) {
                    // 데모에서는 무시
                }

                @Override public void onFrame(NetClient c, Protocol.Frame frame) {
                    if (rtt != null) rtt.onFrame(frame);
                }
            };

            net = new NetClient(r.host, r.port, 10_000, listener);

            // 캔버스도 같은 서버로 연결
            canvas.connect(r.host, r.port);

            // RTT 모니터 시작(1초 주기)
            rtt = new RttMonitor(net, ms -> SwingUtilities.invokeLater(() -> rttLabel.setText("RTT " + ms + "ms")));
            rtt.start(1000);

            status.setText("Connecting...");
        } catch (IOException ex) {
            status.setText("Connect failed: " + ex.getMessage());
            Toast.error(this, "Connect failed: " + ex.getMessage());
        }
    }

    private void onDisconnect() {
        // RTT 모니터 정리
        if (rtt != null) {
            try { rtt.close(); } catch (Exception ignored) {}
            rtt = null;
        }
        // 캔버스 연결 해제
        canvas.disconnect();
        // NetClient 정리
        if (net != null) {
            try { net.close(); } catch (Exception ignored) {}
            net = null;
        }
        status.setText("Disconnected");
        rttLabel.setText("RTT —");
    }

    private void cleanup() {
        // 이전 버전에서는 canvas.close() 를 호출했으나,
        // 일부 환경에서 해당 메서드가 없다는 컴파일 오류가 발생하여
        // 연결 해제만 수행하도록 단순화했다.
        onDisconnect();
    }

    // ================= 엔트리 =================

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}
            new NetPlaygroundFrame().setVisible(true);
        });
    }
}
