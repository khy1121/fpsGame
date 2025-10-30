package com.fpsgame.client.ui;

import javax.swing.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * LobbyReadyListModel
 * ------------------------------------------------------------
 * 로비 화면에서 "플레이어 준비 상태(READY)"를 표시하기 위한 간단한 ListModel.
 *
 * 특징
 * - 세션ID 중심으로 항목을 관리한다.
 * - 닉네임을 아직 모를 때는 "Player-<id>" 플레이스홀더를 사용하고,
 *   나중에 별도 경로(예: WELCOME/PLAYER_LIST 브로드캐스트)에서 이름을 알려주면 updateName()으로 갱신 가능.
 * - LobbyReadyBinder와 연결하여 ready 토글을 반영하면 된다.
 *
 * 사용 예시:
 *   LobbyReadyListModel model = new LobbyReadyListModel();
 *   JList<String> list = new JList<>(model);
 *   // 바인더 연결
 *   new LobbyReadyBinder(ClientPhaseModel.installDefault())
 *       .onReadyToggle((id, ready) -> model.updateReady(id, ready))
 *       .bind();
 *
 *   // 이름 동기화(선택): 서버에서 누군가의 닉이 도착했을 때
 *   model.updateName(sessionId, nickname);
 */
public final class LobbyReadyListModel extends AbstractListModel<String> {

    /** 항목 한 개 */
    public static final class Entry {
        public final int sessionId;
        public String name;    // 표시용 이름
        public boolean ready;  // READY 상태

        Entry(int sessionId, String name, boolean ready) {
            this.sessionId = sessionId;
            this.name = name;
            this.ready = ready;
        }

        @Override public String toString() {
            return (ready ? "✅ " : "⏳ ") + name + "  (#" + sessionId + ")";
        }
    }

    // 세션ID → Entry
    private final Map<Integer, Entry> map = new ConcurrentHashMap<>();
    // 고정된 표시 순서를 위해 별도 리스트 유지(세션ID 정렬)
    private final java.util.List<Integer> order = new ArrayList<>();

    @Override
    public int getSize() {
        return order.size();
    }

    @Override
    public String getElementAt(int index) {
        if (index < 0 || index >= order.size()) return "";
        Entry e = map.get(order.get(index));
        return e == null ? "" : e.toString();
    }

    /** READY 토글 또는 신규 세션 등장 시 호출 */
    public void updateReady(int sessionId, boolean ready) {
        Entry e = map.get(sessionId);
        if (e == null) {
            e = new Entry(sessionId, defaultName(sessionId), ready);
            map.put(sessionId, e);
            insertOrdered(sessionId);
            int idx = indexOf(sessionId);
            if (idx >= 0) {
                fireIntervalAdded(this, idx, idx);
            } else {
                // fallback: 전체 리프레시
                fireContentsChanged(this, 0, Math.max(0, order.size() - 1));
            }
        } else {
            if (e.ready != ready) {
                e.ready = ready;
                int idx = indexOf(sessionId);
                if (idx >= 0) fireContentsChanged(this, idx, idx);
            }
        }
    }

    /** 이름 동기화(선택적). 존재하지 않으면 생성하지 않고 무시한다. */
    public void updateName(int sessionId, String name) {
        Entry e = map.get(sessionId);
        if (e == null) return;
        if (name != null && !name.isBlank() && !name.equals(e.name)) {
            e.name = name;
            int idx = indexOf(sessionId);
            if (idx >= 0) fireContentsChanged(this, idx, idx);
        }
    }

    /** 전체 초기 목록을 세팅하고 기존 내용을 교체한다. */
    public void setInitialList(Collection<Entry> entries) {
        map.clear();
        order.clear();
        if (entries != null) {
            for (Entry e : entries) {
                map.put(e.sessionId, new Entry(e.sessionId,
                        e.name == null || e.name.isBlank() ? defaultName(e.sessionId) : e.name,
                        e.ready));
                order.add(e.sessionId);
            }
            Collections.sort(order);
        }
        fireContentsChanged(this, 0, Math.max(0, order.size() - 1));
    }

    /** 항목 제거 */
    public void remove(int sessionId) {
        int idx = indexOf(sessionId);
        if (idx >= 0) {
            map.remove(sessionId);
            order.remove(idx);
            fireIntervalRemoved(this, idx, idx);
        }
    }

    /** 모두 제거 */
    public void clear() {
        int n = order.size();
        map.clear();
        order.clear();
        if (n > 0) fireIntervalRemoved(this, 0, n - 1);
    }

    /** 외부에서 Entry를 직접 얻고 싶을 때(읽기 전용 복사본) */
    public java.util.List<Entry> snapshot() {
        java.util.List<Entry> list = new ArrayList<>(order.size());
        for (Integer id : order) {
            Entry e = map.get(id);
            if (e != null) list.add(new Entry(e.sessionId, e.name, e.ready));
        }
        return list;
    }

    // ---------------- 내부 유틸 ----------------

    private static String defaultName(int sessionId) {
        return "Player-" + sessionId;
    }

    private void insertOrdered(int sessionId) {
        int pos = Collections.binarySearch(order, sessionId);
        if (pos < 0) pos = -(pos + 1);
        order.add(pos, sessionId);
    }

    private int indexOf(int sessionId) {
        int pos = Collections.binarySearch(order, sessionId);
        return pos >= 0 ? pos : -1;
    }
}
