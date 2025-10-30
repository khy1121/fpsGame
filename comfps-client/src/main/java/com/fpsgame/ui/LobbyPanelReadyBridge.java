package com.fpsgame.client.ui;

import com.fpsgame.client.model.ClientPhaseModel;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * LobbyPanelReadyBridge
 * ------------------------------------------------------------
 * 기존 로비 패널 위젯(JList, READY 토글 버튼 등)에
 * READY 이벤트 흐름을 간단히 붙이는 브리지.
 *
 * 구성
 * - LobbyReadyListModel: 플레이어 목록 + READY 상태 모델
 * - LobbyReadyBinder: ClientPhaseModel(=버스 구독) → 모델 업데이트
 * - (옵션) readySender: 사용자의 READY 버튼을 눌렀을 때 서버로 토글 전송하는 콜백
 *
 * 사용 예시:
 *   JList<String> list = new JList<>();
 *   JButton btnReady = new JButton("READY");
 *   LobbyPanelReadyBridge bridge = LobbyPanelReadyBridge.install(list, btnReady)
 *       .onSendReadyToggle(ready -> netClient.sendReadyToggle(ready)); // 서버 송신 콜백
 *
 *   // 닉네임 수신 시(선택):
 *   bridge.updateName(sessionId, nickname);
 *
 *   // 패널 dispose 시:
 *   bridge.uninstall();
 */
public final class LobbyPanelReadyBridge {

    private final LobbyReadyListModel listModel = new LobbyReadyListModel();
    private final LobbyReadyBinder readyBinder;
    private final ClientPhaseModel phaseModel;

    private final JList<String> jList;
    private final JButton btnReady;              // null 허용
    private Consumer<Boolean> readySender;       // 서버 송신 콜백 (null 허용)
    private ActionListener btnListener;          // 제거용 보관

    private LobbyPanelReadyBridge(JList<String> list, JButton btnReady, ClientPhaseModel model) {
        this.jList = Objects.requireNonNull(list, "list");
        this.btnReady = btnReady;
        this.phaseModel = Objects.requireNonNull(model, "model");

        // 리스트에 모델 장착
        this.jList.setModel(listModel);

        // 버스→모델 갱신(READY 이벤트)
        this.readyBinder = new LobbyReadyBinder(phaseModel)
                .onReadyToggle((sessionId, ready) -> listModel.updateReady(sessionId, ready));
        this.readyBinder.bind();

        // READY 버튼이 있다면 클릭 시 서버로 토글 요청을 보낼 수 있도록 연결
        if (btnReady != null) {
            btnListener = e -> {
                // UI 상태 토글(시각적)과 서버 송신을 분리
                boolean wantReady = !btnReady.isSelected();
                btnReady.setSelected(wantReady);
                btnReady.setText(wantReady ? "READY ✅" : "READY");
                if (readySender != null) readySender.accept(wantReady);
            };
            btnReady.addActionListener(btnListener);
            // 초기 라벨
            btnReady.setText(btnReady.isSelected() ? "READY ✅" : "READY");
        }
    }

    /** 가장 간단한 설치 헬퍼(기본 버스 모델 자동 구독) */
    public static LobbyPanelReadyBridge install(JList<String> list, JButton btnReady) {
        return new LobbyPanelReadyBridge(list, btnReady, ClientPhaseModel.installDefault());
    }

    /** 커스텀 ClientPhaseModel을 넘기고 싶을 때 */
    public static LobbyPanelReadyBridge install(JList<String> list, JButton btnReady, ClientPhaseModel model) {
        return new LobbyPanelReadyBridge(list, btnReady, model);
    }

    // ------------------- 외부 API -------------------

    /**
     * 사용자의 READY 버튼 클릭 시 서버로 보낼 콜백을 등록.
     * 예: .onSendReadyToggle(ready -> ReadyToggleSender.send(netClient, ready))
     */
    public LobbyPanelReadyBridge onSendReadyToggle(Consumer<Boolean> sender) {
        this.readySender = sender;
        return this;
    }

    /** 특정 세션의 닉네임을 갱신(선택). */
    public void updateName(int sessionId, String nickname) {
        listModel.updateName(sessionId, nickname);
    }

    /** 초기 전체 리스트를 외부에서 세팅하고 싶을 때(선택). */
    public void setInitial(java.util.Collection<LobbyReadyListModel.Entry> entries) {
        listModel.setInitialList(entries);
    }

    /** 브리지를 해제(리스너/바인딩 제거) */
    public void uninstall() {
        try { readyBinder.unbind(); } catch (Throwable ignore) {}
        if (btnReady != null && btnListener != null) {
            try { btnReady.removeActionListener(btnListener); } catch (Throwable ignore) {}
            btnListener = null;
        }
        jList.setModel(new DefaultListModel<>()); // 분리 후 빈 모델 부착(선택)
    }

    // ------------------- 편의 메서드 ----------------

    /** 현재 리스트 모델 스냅샷을 가져온다(디버그/저장 용도). */
    public java.util.List<LobbyReadyListModel.Entry> snapshot() {
        return listModel.snapshot();
    }
}
