package com.fpsgame.client.ui;

import com.fpsgame.client.NetClient;
import com.fpsgame.common.OpNames;
import com.fpsgame.common.Protocol;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * 최소한의 네트워크 콘솔 프레임.
 *
 * <p>역할</p>
 * <ul>
 *   <li>상단에 {@link ConnectPanel} 로 서버 접속 제어</li>
 *   <li>중앙에 {@link MiniConsolePanel} 로 이벤트/프레임 로그 출력</li>
 *   <li>프레임 수신 시 opcode를 사람이 읽기 쉬운 이름으로 출력({@link OpNames})</li>
 * </ul>
 *
 * <p>주의</p>
 * 채팅 전송 입력창 등은 포함하지 않았다(순수 모니터링용). 필요 시 후속 확장.
 */
public final class ChatConsoleFrame extends JFrame {

    private final ConnectPanel connect = new ConnectPanel();
    private final MiniConsolePanel console = new MiniConsolePanel(600);

    /** 현재 연결(없을 수 있음) */
    private NetClient net;

    public ChatConsoleFrame() {
        super("Net Console");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        add(connect, BorderLayout.NORTH);
        add(console, BorderLayout.CENTER);

        connect.setListener(new ConnectPanel.Listener() {
            @Override public void onConnect(String host, int port) {
                doConnect(host, port);
            }
            @Override public void onDisconnect() {
                doDisconnect("user request");
            }
        });

        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                doDisconnect("window closing");
            }
        });
    }

    // ================= 연결/해제 =================

    private void doConnect(String host, int port) {
        if (net != null) {
            console.println("이미 연결되어 있습니다. 먼저 Disconnect 하세요.");
            return;
        }
        console.println("Connecting to " + host + ":" + port + " ...");
        connect.setStatusText("Connecting...", new Color(220, 180, 60));

        NetClient.Listener listener = new NetClient.Listener() {
            @Override public void onOpen(NetClient c) {
                SwingUtilities.invokeLater(() -> {
                    net = c;
                    connect.setConnected(true);
                    connect.setStatusText("Connected " + c.remoteAddress(), new Color(0, 140, 0));
                    console.println("Connected: " + c.remoteAddress());
                });
            }

            @Override public void onClosed(NetClient c, String reason) {
                SwingUtilities.invokeLater(() -> {
                    console.println("Closed: " + reason);
                    connect.setConnected(false);
                    connect.setStatusText("Closed", new Color(180, 0, 0));
                    net = null;
                });
            }

            @Override public void onDisconnected(String reason) {
                SwingUtilities.invokeLater(() -> {
                    console.println("Disconnected: " + reason);
                    connect.setConnected(false);
                    connect.setStatusText("Disconnected", new Color(180, 0, 0));
                    net = null;
                });
            }

            @Override public void onChat(String text) {
                SwingUtilities.invokeLater(() -> console.println("<CHAT> " + text));
            }

            @Override public void onFrame(NetClient c, Protocol.Frame frame) {
                if (frame == null) return;
                String name = OpNames.name(frame.opcode);
                int len = frame.payload == null ? 0 : frame.payload.length;
                SwingUtilities.invokeLater(() ->
                        console.println(String.format("<FRAME> %s (%d bytes)", name, len)));
            }
        };

        try {
            net = new NetClient(host, port, 10_000, listener);
        } catch (Exception ex) {
            connect.setConnected(false);
            connect.setStatusText("Connect failed", new Color(180, 0, 0));
            console.println("Connect failed: " + ex.getMessage());
            net = null;
        }
    }

    private void doDisconnect(String reason) {
        NetClient c = net;
        net = null;
        if (c != null) {
            try { c.close(); } catch (Exception ignored) {}
        }
        connect.setConnected(false);
        connect.setStatusText("Disconnected", new Color(180, 0, 0));
        if (reason != null) console.println("Disconnected: " + reason);
    }

    // ================= 실행 엔트리 =================

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}
            new ChatConsoleFrame().setVisible(true);
        });
    }
}
