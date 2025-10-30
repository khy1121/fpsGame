package com.fpsgame.client.ui;

import com.fpsgame.client.model.ClientPhaseBus;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * LobbyReadyPanel
 * ------------------------------------------------------------
 * 로비의 우측 사이드바: 플레이어 READY 리스트 + 내 READY 토글 버튼.
 *
 * 특징
 * - ClientPhaseBus의 READY_TOGGLE 이벤트를 구독하여 리스트를 실시간 반영.
 * - "Ready" 버튼 클릭 시 서버로 토글을 전송(네트워크 의존 최소화를 위해
 *   내부에서 관대하게 여러 전송 메서드를 시도한다: sendReady(bool), ready(bool),
 *   send(opcode, payload) 등).
 * - 외부 콜백 주입(onSendReady)도 지원(테스트/커스텀 통신 경로).
 *
 * 사용:
 *   LobbyReadyPanel panel = new LobbyReadyPanel(netClient);
 *   frame.getContentPane().add(panel, BorderLayout.EAST);
 */
public final class LobbyReadyPanel extends JPanel implements ClientPhaseBus.PhaseListener {

    /** 서버 전송에 사용할 네트클라 (리플렉션 호환을 위해 Object) */
    private Object netClient;

    /** 외부에서 직접 전송하고 싶을 때 주입하는 콜백(널이면 내부 전송 사용) */
    private java.util.function.Consumer<Boolean> onSendReady;

    /** READY 상태 테이블: sessionId -> ready */
    private final Map<Integer, Boolean> readyMap = new ConcurrentHashMap<>();

    /** UI */
    private final DefaultListModel<String> listModel = new DefaultListModel<>();
    private final JList<String> readyList = new JList<>(listModel);
    private final JToggleButton btnReady = new JToggleButton("Ready");

    /** 버스 핸들 */
    private final ClientPhaseBus bus = ClientPhaseBus.get();

    /** 내 세션 id(선택) — 알 수 없으면 음수 유지 */
    private int mySessionId = -1;

    public LobbyReadyPanel(Object netClient) {
        super(new BorderLayout(8, 8));
        this.netClient = Objects.requireNonNull(netClient, "netClient");
        buildUi();
        wireEvents();
        bus.addListener(this);
    }

    /** 네트클라를 나중에 주입하고자 할 때(테스트/샌드박스용) */
    public LobbyReadyPanel setNetClient(Object netClient) {
        this.netClient = netClient;
        return this;
    }

    /** 내가 누구인지 알 때 세션 id를 알려주면 리스트에서 강조표시 */
    public LobbyReadyPanel setMySessionId(int sessionId) {
        this.mySessionId = sessionId;
        refreshList();
        return this;
    }

    /** 외부 전송 콜백(테스트용). 주입 시 내부 리플렉션 전송 대신 사용 */
    public LobbyReadyPanel onSendReady(java.util.function.Consumer<Boolean> cb) {
        this.onSendReady = cb;
        return this;
    }

    private void buildUi() {
        setBorder(new EmptyBorder(8, 8, 8, 8));
        setPreferredSize(new Dimension(260, 0));
        setBackground(new Color(28, 34, 46));

        JLabel title = new JLabel("플레이어 준비 현황");
        title.setForeground(new Color(230, 238, 246));
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));

        readyList.setBorder(BorderFactory.createLineBorder(new Color(62, 70, 86)));
        readyList.setBackground(new Color(36, 42, 56));
        readyList.setForeground(new Color(210, 220, 236));
        readyList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        btnReady.setFocusPainted(false);
        btnReady.setBackground(new Color(48, 76, 96));
        btnReady.setForeground(Color.WHITE);

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(title, BorderLayout.WEST);

        add(top, BorderLayout.NORTH);
        add(new JScrollPane(readyList), BorderLayout.CENTER);
        add(btnReady, BorderLayout.SOUTH);
    }

    private void wireEvents() {
        btnReady.addActionListener(e -> {
            boolean ready = btnReady.isSelected();
            btnReady.setText(ready ? "Ready ✔" : "Ready");
            // 외부 콜백이 있으면 우선 사용
            if (onSendReady != null) {
                try { onSendReady.accept(ready); } catch (Throwable t) { t.printStackTrace(); }
                return;
            }
            // 내부 전송
            sendReadyToggle(ready);
        });
    }

    // ---------------- ClientPhaseBus.PhaseListener ----------------

    @Override
    public void onReadyToggle(int sessionId, boolean ready) {
        readyMap.put(sessionId, ready);
        refreshList();
        // 내가 나 자신을 토글한 것이라면 버튼 상태도 동기화(중복 호출 안전)
        if (sessionId == mySessionId) {
            SwingUtilities.invokeLater(() -> {
                if (btnReady.isSelected() != ready) {
                    btnReady.setSelected(ready);
                    btnReady.setText(ready ? "Ready ✔" : "Ready");
                }
            });
        }
    }

    @Override public void onPhaseUpdate(ClientPhaseBus.PhaseState s) { /* ignore for now */ }
    @Override public void onCountdown(int sec) { /* ignore */ }
    @Override public void onRoundResult(ClientPhaseBus.RoundResult r) { /* ignore */ }

    // ---------------- 리스트 갱신 ----------------

    private void refreshList() {
        SwingUtilities.invokeLater(() -> {
            listModel.clear();
            // 간단 포맷: [✔/ ] sessionId
            readyMap.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(e -> {
                        boolean r = Boolean.TRUE.equals(e.getValue());
                        String self = (e.getKey() != null && e.getKey() == mySessionId) ? " (me)" : "";
                        listModel.addElement(String.format("[%s] %d%s",
                                r ? "✔" : " ", e.getKey(), self));
                    });
        });
    }

    // ---------------- 내부: 전송 경로 ----------------

    /**
     * 여러 전송 메서드를 관대하게 시도:
     *  - sendReady(boolean)
     *  - ready(boolean)
     *  - send(byte opcode, byte[] payload) / write(...)
     *
     * 마지막 경로에서는 Protocol.Opcode.READY_TOGGLE = 0x?? 가정 없이
     * "READY_TOGGLE(=6)" 등의 상수를 몰라도 되도록 리플렉션으로 Protocol에서 찾아볼 수 있지만,
     * 이 클래스는 프로토콜에 직접 의존하지 않기 위해 opcode 값은 0xFE로 보냅니다.
     * (필요 시 외부에서 onSendReady(...) 콜백을 주입하세요)
     */
    private void sendReadyToggle(boolean ready) {
        Object nc = this.netClient;
        if (nc == null) return;

        // 1) 직관적 메서드명
        if (invokeBool(nc, "sendReady", ready)) return;
        if (invokeBool(nc, "ready", ready)) return;

        // 2) (opcode, payload) 범용 경로 시도
        byte[] payload = new byte[]{ (byte)(ready ? 1 : 0) };
        if (invokeSendFrame(nc, (byte) 0xFE, payload)) return; // 임시 opcode

        // 실패: 조용히 무시(버튼은 로컬 상태 유지, 서버에서 브로드캐스트 오면 정합)
    }

    private static boolean invokeBool(Object target, String name, boolean arg) {
        try {
            Method m = target.getClass().getMethod(name, boolean.class);
            m.invoke(target, arg);
            return true;
        } catch (Throwable ignore) { /* fallthrough */ }
        try {
            Method m = target.getClass().getMethod(name, Boolean.class);
            m.invoke(target, Boolean.valueOf(arg));
            return true;
        } catch (Throwable ignore) { /* fallthrough */ }
        return false;
    }

    private static boolean invokeSendFrame(Object target, byte opcode, byte[] payload) {
        String[] names = { "send", "write" };
        for (String n : names) {
            if (invoke2(target, n, byte.class, byte[].class, opcode, payload)) return true;
            if (invoke2(target, n, int.class,  byte[].class, (int)(opcode & 0xFF), payload)) return true;
            // (len+opcode+payload)를 스스로 프레이밍해서 write(byte[])만 받는 구현을 위해:
            if (invoke1(target, n, byte[].class, frame(opcode, payload))) return true;
        }
        return false;
    }

    private static boolean invoke1(Object target, String name, Class<?> p0, Object a0) {
        try {
            Method m = target.getClass().getMethod(name, p0);
            m.invoke(target, a0);
            return true;
        } catch (Throwable ignore) {
            return false;
        }
    }

    private static boolean invoke2(Object target, String name, Class<?> p0, Class<?> p1, Object a0, Object a1) {
        try {
            Method m = target.getClass().getMethod(name, p0, p1);
            m.invoke(target, a0, a1);
            return true;
        } catch (Throwable ignore) {
            return false;
        }
    }

    /** length(int) + opcode(byte) + payload 형식으로 간단 프레이밍 */
    private static byte[] frame(byte opcode, byte[] payload) {
        int len = 1 + (payload == null ? 0 : payload.length);
        ByteBuffer bb = ByteBuffer.allocate(4 + len).order(ByteOrder.BIG_ENDIAN);
        bb.putInt(len).put(opcode);
        if (payload != null && payload.length > 0) bb.put(payload);
        return bb.array();
    }

    // ---------------- 정리 ----------------

    /** 해제(중복 호출 안전) */
    public void dispose() {
        try { bus.removeListener(this); } catch (Throwable ignore) {}
    }
}
