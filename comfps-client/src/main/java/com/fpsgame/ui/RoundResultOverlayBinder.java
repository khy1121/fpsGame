package com.fpsgame.client.ui;

import com.fpsgame.client.model.ClientPhaseBus;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Objects;

/**
 * RoundResultOverlayBinder (경량)
 * ------------------------------------------------------------
 * ClientPhaseBus 의 라운드 결과 이벤트를 중앙 오버레이(CenterMessageOverlayPanel)에 바인딩한다.
 *
 * 특징
 * - 이미 레이어에 존재하는 오버레이가 있으면 재사용, 없으면 경량 오버레이를 직접 추가
 * - 결과 메시지를 크게 표시하고, 지정 시간(기본 3초) 후 자동 숨김
 * - 모든 UI 갱신은 EDT에서 수행, 클래스/필드 시그니처가 달라도 최대한 관용적으로 처리
 *
 * 사용
 * <pre>
 *   RoundResultOverlayBinder b = RoundResultOverlayBinder.install(frame);
 *   ...
 *   b.uninstall();
 * </pre>
 */
public final class RoundResultOverlayBinder implements ClientPhaseBus.PhaseListener {

    private final JFrame frame;
    private CenterMessageOverlayPanel overlay; // 재사용 또는 생성
    private boolean ownOverlay;                // 우리가 추가했는지 여부
    private ComponentAdapter resizeSync;       // 오버레이 사이즈 동기화
    private Timer autohideTimer;               // 자동 숨김 타이머
    private int autohideMillis = 3000;         // 기본 3초

    private boolean installed;

    private RoundResultOverlayBinder(JFrame frame) {
        this.frame = Objects.requireNonNull(frame, "frame");
    }

    /** 권장: 간편 설치 (버스 구독 + 오버레이 확보) */
    public static RoundResultOverlayBinder install(JFrame frame) {
        RoundResultOverlayBinder b = new RoundResultOverlayBinder(frame);
        b.bind();
        return b;
    }

    /** 버스 구독 시작(체이닝 가능) */
    public RoundResultOverlayBinder bind() {
        if (installed) return this;
        SwingUtilities.invokeLater(() -> {
            ensureOverlay();
            ClientPhaseBus.get().addListener(this);
            installed = true;
        });
        return this;
    }

    /** 자동 숨김 시간(밀리초) 설정 */
    public RoundResultOverlayBinder withAutohideMillis(int millis) {
        if (millis > 0) this.autohideMillis = millis;
        return this;
    }

    /** 버스 구독 해제 별칭 */
    public void unbind() {
        uninstall();
    }

    /** 해제(중복 호출 안전) */
    public void uninstall() {
        if (!installed) return;
        SwingUtilities.invokeLater(() -> {
            try {
                ClientPhaseBus.get().removeListener(this);
            } catch (Throwable ignore) {}

            if (autohideTimer != null) {
                try { autohideTimer.stop(); } catch (Throwable ignore) {}
                autohideTimer = null;
            }
            if (overlay != null && ownOverlay) {
                try { frame.getLayeredPane().remove(overlay); } catch (Throwable ignore) {}
                overlay = null;
            }
            if (resizeSync != null) {
                try { frame.removeComponentListener(resizeSync); } catch (Throwable ignore) {}
                resizeSync = null;
            }
            frame.revalidate();
            frame.repaint();
            installed = false;
        });
    }

    // ========================= ClientPhaseBus 리스너 =========================

    @Override
    public void onPhaseUpdate(ClientPhaseBus.PhaseState state) {
        // 사용 안 함
    }

    @Override
    public void onCountdown(int seconds) {
        // 사용 안 함
    }

    @Override
    public void onRoundResult(ClientPhaseBus.RoundResult rr) {
        SwingUtilities.invokeLater(() -> {
            ensureOverlay();
            if (overlay == null) return;

            String msg = buildMessage(rr);
            overlay.showBigText(msg);

            // 자동 숨김 타이머 재시작
            if (autohideTimer != null) {
                try { autohideTimer.stop(); } catch (Throwable ignore) {}
            }
            autohideTimer = new Timer(autohideMillis, e -> {
                overlay.hideOverlay();
                if (autohideTimer != null) autohideTimer.stop();
            });
            autohideTimer.setRepeats(false);
            autohideTimer.start();
        });
    }

    @Override
    public void onReadyToggle(int sessionId, boolean ready) {
        // 사용 안 함
    }

    // ========================= 내부 구현 =========================

    /** 프레임 레이어드 페인에서 오버레이를 찾아 재사용하거나, 없으면 생성/추가 */
    private void ensureOverlay() {
        if (overlay != null) return;

        // 1) 이미 존재하는 CenterMessageOverlayPanel 재사용 시도
        JLayeredPane lp = frame.getLayeredPane();
        for (Component c : lp.getComponentsInLayer(JLayeredPane.PALETTE_LAYER)) {
            if (c instanceof CenterMessageOverlayPanel) {
                overlay = (CenterMessageOverlayPanel) c;
                ownOverlay = false;
                return;
            }
        }

        // 2) 없으면 우리가 생성/추가 (경량)
        overlay = new CenterMessageOverlayPanel();
        overlay.setVisible(false);
        lp.add(overlay, JLayeredPane.PALETTE_LAYER);
        ownOverlay = true;

        // 크기 동기화
        resizeSync = new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) { syncOverlayBounds(); }
            @Override public void componentShown  (ComponentEvent e) { syncOverlayBounds(); }
        };
        frame.addComponentListener(resizeSync);
        syncOverlayBounds();
    }

    /** 오버레이를 프레임 콘텐츠 크기에 맞게 확장 */
    private void syncOverlayBounds() {
        if (overlay == null) return;
        int w = Math.max(1, frame.getContentPane().getWidth());
        int h = Math.max(1, frame.getContentPane().getHeight());
        overlay.setBounds(0, 0, w, h);
        overlay.revalidate();
        overlay.repaint();
    }

    // --------------------- 메시지 빌더(관용적 리플렉션) ---------------------

    private String buildMessage(Object rr) {
        if (rr == null) return "ROUND RESULT";
        // 필드/게터 후보
        Integer winner = asInt(readAny(rr, "winnerTeam", "winner", "team", "victor"));
        Integer blue   = asInt(readAny(rr, "blueRounds", "blue", "scoreBlue", "scoreB"));
        Integer red    = asInt(readAny(rr, "redRounds",  "red",  "scoreRed",  "scoreR"));
        Boolean ended  = asBool(readAny(rr, "matchEnded", "ended", "matchEnd", "final"));

        String winnerTxt = (winner == null) ? "?" : (winner == 0 ? "BLUE" : (winner == 1 ? "RED" : "#" + winner));
        String scoreTxt  = (blue == null || red == null) ? "" : ("  " + blue + " : " + red);
        String endTxt    = (ended != null && ended) ? "  (MATCH END)" : "";

        return "WINNER: " + winnerTxt + scoreTxt + endTxt;
    }

    private static Object readAny(Object obj, String... names) {
        for (String n : names) {
            // 1) getter 우선
            try {
                Method m = obj.getClass().getMethod("get" + up(n));
                return m.invoke(obj);
            } catch (Throwable ignore) {}
            // 2) 같은 이름의 메서드
            try {
                Method m = obj.getClass().getMethod(n);
                return m.invoke(obj);
            } catch (Throwable ignore) {}
            // 3) public 필드
            try {
                Field f = obj.getClass().getField(n);
                return f.get(obj);
            } catch (Throwable ignore) {}
        }
        return null;
    }

    private static String up(String n) {
        if (n == null || n.isEmpty()) return n;
        return Character.toUpperCase(n.charAt(0)) + n.substring(1);
    }

    private static Integer asInt(Object o) {
        if (o == null) return null;
        if (o instanceof Number) return ((Number) o).intValue();
        try { return Integer.parseInt(o.toString().trim()); } catch (Exception e) { return null; }
    }

    private static Boolean asBool(Object o) {
        if (o == null) return null;
        if (o instanceof Boolean) return (Boolean) o;
        String s = o.toString().trim().toLowerCase();
        if ("true".equals(s) || "yes".equals(s) || "y".equals(s) || "1".equals(s)) return true;
        if ("false".equals(s) || "no".equals(s)  || "n".equals(s) || "0".equals(s)) return false;
        return null;
    }
}
