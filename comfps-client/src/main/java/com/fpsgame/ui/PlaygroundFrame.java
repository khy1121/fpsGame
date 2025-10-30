package com.fpsgame.client.ui;

import com.fpsgame.client.NetClient;
import com.fpsgame.common.Protocol;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

/**
 * 네트워크/렌더/HUD를 한 화면에서 테스트할 수 있는 종합 플레이그라운드 프레임.
 *
 * <p>구성</p>
 * <ul>
 *   <li>상단: {@link ConnectPanel} (서버 접속/해제)</li>
 *   <li>중앙: {@link GameCanvasNetDemo} (서버 권위 스냅샷 렌더 + 입력 송신)</li>
 *   <li>상단 오버레이: {@link GameHudLayer} (페이즈/카운트다운/스코어/RTT/FPS)</li>
 *   <li>하단: {@link MiniConsolePanel} (간단 로그)</li>
 * </ul>
 *
 * <p>특징</p>
 * <ul>
 *   <li>{@link NetClient.Listener} 체인을 구성해 HUD와 콘솔에 프레임/채팅을 반영</li>
 *   <li>연결/해제/오류를 토스트로 간단 알림</li>
 *   <li>에러 발생 가능성을 낮추기 위해 외부 의존 최소화 및 예외 처리 강화</li>
 * </ul>
 */
public class PlaygroundFrame extends JFrame {

    private final ConnectPanel connectPanel = new ConnectPanel();
    private final GameCanvasNetDemo canvas = new GameCanvasNetDemo();
    private final GameHudLayer hudLayer = new GameHudLayer();
    private final MiniConsolePanel console = new MiniConsolePanel(500);

    // 현재 NetClient 핸들(데모 내에서만 사용)
    private NetClient net;

    public PlaygroundFrame() {
        super("FPS Playground");

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(1200, 720);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // 상단 연결 패널
        add(connectPanel, BorderLayout.NORTH);

        // 중앙 게임 패널(상단 HUD 오버레이를 GlassPane으로 얹음)
        JPanel center = new JPanel(new BorderLayout());
        center.add(canvas, BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);

        // 글래스팬에 HUD 오버레이 추가
        JPanel overlayHolder = new JPanel(new BorderLayout()) {{ setOpaque(false); }};
        overlayHolder.add(hudLayer, BorderLayout.NORTH);
        setGlassPane(overlayHolder);
        overlayHolder.setVisible(true);

        // 하단 콘솔
        add(console, BorderLayout.SOUTH);

        // 연결 리스너
        connectPanel.setListener(new ConnectPanel.Listener() {
            @Override public void onConnect(String host, int port) {
                tryConnect(host, port);
            }
            @Override public void onDisconnect() {
                tryDisconnect("user request");
            }
        });
    }

    // ===================== 연결/해제 =====================

    private void tryConnect(String host, int port) {
        // 이미 연결되어 있으면 무시
        if (net != null && net.isConnected()) {
            connectPanel.setStatusText("Already connected", new Color(0, 130, 0));
            return;
        }
        connectPanel.setStatusText("Connecting...", new Color(120, 120, 0));
        console.println("Connecting to " + host + ":" + port + "...");

        try {
            // HUD를 NetClient 리스너 체인에 연결
            NetClient.Listener listener = hudLayer.getHudPanel().asListener(new NetClient.Listener() {
                @Override public void onOpen(NetClient c) {
                    SwingUtilities.invokeLater(() -> {
                        connectPanel.setConnected(true);
                        connectPanel.setStatusText("Connected " + c.remoteAddress(), new Color(0, 130, 0));
                        Toast.success(PlaygroundFrame.this, "서버에 연결되었습니다");
                        console.println("[OPEN] " + c.remoteAddress());
                    });
                }

                @Override public void onClosed(NetClient c, String reason) {
                    SwingUtilities.invokeLater(() -> {
                        connectPanel.setConnected(false);
                        connectPanel.setStatusText("Closed: " + reason, new Color(180, 0, 0));
                        Toast.error(PlaygroundFrame.this, "연결 종료: " + reason);
                        console.println("[CLOSED] " + reason);
                    });
                }

                @Override public void onDisconnected(String reason) {
                    SwingUtilities.invokeLater(() -> {
                        connectPanel.setConnected(false);
                        connectPanel.setStatusText("Disconnected: " + reason, new Color(180, 0, 0));
                        Toast.error(PlaygroundFrame.this, "연결 끊김: " + reason);
                        console.println("[DISCONNECTED] " + reason);
                    });
                }

                @Override public void onChat(String text) {
                    console.println("[CHAT] " + text);
                }

                @Override public void onFrame(NetClient c, Protocol.Frame frame) {
                    if (frame == null) return;
                    // 간단한 시스템 메시지 로그
                    if (frame.opcode == Protocol.Opcode.WELCOME) {
                        console.println("[FRAME] WELCOME (" + frame.payload.length + " bytes)");
                    } else if (frame.opcode == Protocol.Opcode.CHAT) {
                        String msg = Protocol.parseChat(frame.payload);
                        console.println("[CHAT] " + msg);
                    }
                }
            });

            // 스냅샷 브리지(보간 버퍼)는 GameCanvasNetDemo 내부에서 생성/사용
            // 여기서는 동일 리스너 체인을 NetClient 생성자에 전달
            net = new NetClient(host, port, 12_000, listener);

            // GameCanvas 쪽도 같은 연결을 사용하도록 connect 호출
            // (GameCanvasNetDemo는 내부에서 자체 SnapshotBridge/입력펌프를 구성)
            canvas.connect(host, port);

            // 성공
            console.println("Connected.");
        } catch (IOException ex) {
            connectPanel.setConnected(false);
            connectPanel.setStatusText("Connect failed: " + ex.getMessage(), new Color(180, 0, 0));
            Toast.error(this, "연결 실패: " + ex.getMessage());
            console.println("Connect failed: " + ex);
        }
    }

    private void tryDisconnect(String reason) {
        // 캔버스 먼저 해제(내부 입력 펌프 종료)
        try { canvas.disconnect(); } catch (Exception ignored) {}

        NetClient c = net;
        net = null;
        if (c != null) {
            try { c.close(); } catch (Exception ignored) {}
        }
        connectPanel.setConnected(false);
        connectPanel.setStatusText("Disconnected", new Color(180, 0, 0));
        console.println("Disconnected: " + reason);
        Toast.show(this, "연결 해제됨", 1200);
    }

    // ===================== 엔트리 =====================

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}
            new PlaygroundFrame().setVisible(true);
        });
    }
}
