package com.fpsgame.client.ui;

import com.fpsgame.client.model.ClientPhaseModel;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Objects;
import java.util.function.BiConsumer;
import javax.swing.SwingUtilities;

/**
 * LobbyReadyBinder
 * ------------------------------------------------------------
 * 로비 화면의 "플레이어 READY 상태" 갱신을 위한 경량 바인더.
 *
 * 역할
 * - ClientPhaseModel 에서 발생하는 "readyToggle" PropertyChange 이벤트를 구독하여
 *   로비 UI(플레이어 리스트, 체크마크, 버튼 상태 등)에 반영한다.
 *
 * 의존
 * - ClientPhaseModel (installDefault()로 간편 구독 가능)
 * - UI 측에는 구체 클래스에 의존하지 않고 BiConsumer 콜백만 요청
 *
 * 사용 예시:
 *   LobbyReadyBinder binder = new LobbyReadyBinder(ClientPhaseModel.installDefault())
 *       .onReadyToggle((sessionId, ready) -> lobbyPanel.updateReady(sessionId, ready));
 *   binder.bind();
 *
 * 해제:
 *   binder.unbind();
 *
 * 스레드
 * - 이벤트는 EDT에서 호출된다(이 클래스가 보장).
 */
public final class LobbyReadyBinder {

    private final ClientPhaseModel model;
    private final PropertyChangeListener pcl = this::onProperty;

    /** READY 토글이 들어올 때 호출될 UI 콜백(sessionId, ready) */
    private BiConsumer<Integer, Boolean> readyToggleHandler;

    private volatile boolean bound;

    public LobbyReadyBinder(ClientPhaseModel model) {
        this.model = Objects.requireNonNull(model, "model");
    }

    /** 기본 버스에 자동 구독하는 생성 헬퍼 */
    public static LobbyReadyBinder installDefault() {
        return new LobbyReadyBinder(ClientPhaseModel.installDefault());
    }

    /** READY 토글 콜백 등록 */
    public LobbyReadyBinder onReadyToggle(BiConsumer<Integer, Boolean> handler) {
        this.readyToggleHandler = handler;
        return this;
    }

    /** 바인딩 시작(EDT에서 현 상태 즉시 동기화는 필요 없음) */
    public synchronized void bind() {
        if (bound) return;
        bound = true;
        model.addPropertyChangeListener(pcl);
    }

    /** 바인딩 해제 */
    public synchronized void unbind() {
        if (!bound) return;
        bound = false;
        model.removePropertyChangeListener(pcl);
    }

    // ---- 내부: 모델 이벤트 처리 --------------------------------------------

    private void onProperty(PropertyChangeEvent evt) {
        if (!"readyToggle".equals(evt.getPropertyName())) return;

        // EDT 보장(이중 확인)
        Runnable r = () -> {
            if (readyToggleHandler == null) return;
            Object newVal = evt.getNewValue();
            if (newVal instanceof ClientPhaseModel.ReadyEvent re) {
                readyToggleHandler.accept(re.sessionId, re.ready);
            }
        };

        if (SwingUtilities.isEventDispatchThread()) r.run();
        else SwingUtilities.invokeLater(r);
    }
}
