package com.fpsgame.client.ui;

import com.fpsgame.client.NetClient;
import java.awt.*;
import java.io.IOException;
import javax.swing.*;

/**
 * 채팅 로그 + 입력을 하나로 묶은 경량 독 패널.
 *
 * <p>구성</p>
 * <ul>
 *   <li>{@link MiniConsolePanel} : 상단 로그 영역</li>
 *   <li>{@link ChatInputPanel}   : 하단 입력 영역</li>
 * </ul>
 *
 * <p>특징</p>
 * <ul>
 *   <li>{@link #setNetClient(NetClient)} 호출 시 온라인 모드로 전환되어 채팅 전송</li>
 *   <li>오프라인일 때는 로컬 에코로만 동작하여 테스트 용이</li>
 *   <li>EDT 안전</li>
 * </ul>
 */
public class ChatDock extends JPanel {

    private final MiniConsolePanel log = new MiniConsolePanel(400);
    private final ChatInputPanel input = new ChatInputPanel();

    public ChatDock() {
        super(new BorderLayout(0, 4));
        add(log, BorderLayout.CENTER);
        add(input, BorderLayout.SOUTH);

        // 최초엔 오프라인 로거로 연결
        input.setNetClient(null, s -> {
            log.println(s);
        });
    }

    /** NetClient 설정(online/offline 전환) */
    public void setNetClient(NetClient client) {
        input.setNetClient(client, s -> log.println(s));
    }

    /** 외부에서 로그를 보낼 때 사용(예: 서버 시스템 메시지). */
    public void println(String line) { log.println(line); }

    /** 로그 지우기 */
    public void clear() { log.clear(); }

    /** 입력 포커스 주기 */
    public void focusInput() { input.focusInput(); }

    // ===================== 단독 실행 데모 =====================

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("ChatDock Demo");
            f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            f.setSize(720, 360);
            f.setLocationRelativeTo(null);

            ChatDock dock = new ChatDock();

            // 상단에 임시 버튼줄: 온라인/오프라인 토글 시연
            JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
            JButton offlineBtn = new JButton("Offline");
            JButton fakeOnlineBtn = new JButton("Fake Online Echo");
            top.add(offlineBtn);
            top.add(fakeOnlineBtn);

            offlineBtn.addActionListener(e -> {
                dock.setNetClient(null);
                dock.println("[system] 오프라인 모드");
            });

            // 데모용 페이크 NetClient: sendChat 호출만 가로채서 로컬 출력
            fakeOnlineBtn.addActionListener(e -> {
                try {
                    dock.setNetClient(new NetClient("127.0.0.1", 0, 0, new NetClient.Listener() {
                        @Override public void onOpen(NetClient c) {}
                    }) {
                        @Override public boolean isConnected() { return true; }
                        @Override public void sendChat(String text) throws IOException {
                            dock.println("[sent] " + text);
                        }
                    });
                    dock.println("[system] 페이크 온라인(로컬 에코 NetClient)");
                } catch (IOException ex) {
                    // NetClient 생성자가 IOException을 던질 수 있으므로 여기서 처리
                    dock.println("[error] 페이크 NetClient 생성 실패: " + ex.getMessage());
                }
            });

            f.setLayout(new BorderLayout());
            f.add(top, BorderLayout.NORTH);
            f.add(dock, BorderLayout.CENTER);
            f.setVisible(true);

            dock.focusInput();
        });
    }
}
