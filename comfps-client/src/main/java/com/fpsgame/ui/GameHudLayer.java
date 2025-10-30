package com.fpsgame.client.ui;

import com.fpsgame.common.Protocol;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * 게임 화면 상단에 표시되는 HUD 레이어(합성 컴포넌트).
 *
 * <p>구성 요소</p>
 * <ul>
 *   <li>{@link HudPanel} : 페이즈/카운트다운/스코어/메시지</li>
 *   <li>{@link LatencyIndicator} : RTT 상태</li>
 *   <li>{@link FpsOverlayPanel} : FPS 표시(옵션)</li>
 * </ul>
 *
 * <p>특징</p>
 * <ul>
 *   <li>상단 바 형태로 배치되며, 우측에 RTT 지표를 정렬</li>
 *   <li>필요 시 하단/좌측 상단 등 원하는 위치에 {@link #getFpsOverlay()} 를 중첩 배치 가능</li>
 *   <li>외부에 단순한 업데이트 API 제공</li>
 * </ul>
 */
public class GameHudLayer extends JPanel {

    private final HudPanel hud = new HudPanel();
    private final LatencyIndicator latency = new LatencyIndicator();
    private final FpsOverlayPanel fpsOverlay = new FpsOverlayPanel();

    public GameHudLayer() {
        setLayout(new BorderLayout());
        setOpaque(false);

        // 상단 바(왼쪽: HUD, 오른쪽: RTT)
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBorder(new EmptyBorder(4, 6, 2, 6));
        topBar.setBackground(new Color(18, 18, 18, 210));

        topBar.add(hud, BorderLayout.CENTER);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        right.setOpaque(false);
        right.add(latency);
        topBar.add(right, BorderLayout.EAST);

        add(topBar, BorderLayout.NORTH);
    }

    // ===================== 외부 업데이트 API =====================

    /** 페이즈 코드 갱신 */
    public void updatePhase(int phaseCode) { hud.onPhaseUpdate(phaseCode); }

    /** 카운트다운 갱신(초) */
    public void updateCountdown(int seconds) { hud.onCountdown(seconds); }

    /** 라운드 결과/스코어 갱신 */
    public void updateRoundResult(Protocol.RoundResult rr) { hud.onRoundResult(rr); }

    /** 시스템 메시지 표시 */
    public void setSystemMessage(String text) { hud.setSystemMessage(text); }

    /** RTT(ms) 갱신 */
    public void setRtt(long millis) { latency.setRtt(millis); }

    /** FPS 오버레이 컴포넌트를 반환(원하는 레이어에 추가해서 사용). */
    public FpsOverlayPanel getFpsOverlay() { return fpsOverlay; }

    /** 내부 HUD를 그대로 노출(리스너 체인 등에 사용 가능). */
    public HudPanel getHudPanel() { return hud; }

    /** 내부 RTT 인디케이터 노출 */
    public LatencyIndicator getLatencyIndicator() { return latency; }

    // ===================== 단독 실행 데모 =====================

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("GameHudLayer Demo");
            f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            f.setSize(960, 540);
            f.setLocationRelativeTo(null);

            GameHudLayer layer = new GameHudLayer();

            JPanel center = new JPanel(null) {
                { setBackground(new Color(36, 36, 36)); }
                @Override public void doLayout() {
                    // 상단 바는 BorderLayout으로 관리(부모가 수행)
                }
            };

            f.setLayout(new BorderLayout());
            f.add(layer, BorderLayout.NORTH); // 상단에 HUD 바
            f.add(center, BorderLayout.CENTER);

            // FPS 오버레이를 좌측 상단에 배치(샘플)
            FpsOverlayPanel fps = layer.getFpsOverlay();
            JPanel overlayHolder = new JPanel(new GridBagLayout()) {
                { setOpaque(false); }
            };
            GridBagConstraints c = new GridBagConstraints();
            c.anchor = GridBagConstraints.NORTHWEST;
            c.insets = new Insets(10, 10, 10, 10);
            overlayHolder.add(fps, c);

            f.setGlassPane(overlayHolder);
            overlayHolder.setVisible(true);

            // 렌더 루프 시뮬레이션: FPS 샘플 업데이트
            new Timer(16, e -> {
                fps.frameArrived();
                center.repaint();
            }).start();

            // HUD 데모 타이머
            new Timer(750, e -> {
                int s = (int) ((System.currentTimeMillis() / 1000) % 6);
                layer.updatePhase(s);
                layer.updateCountdown(10 - (int) (System.currentTimeMillis() / 1000 % 11));
                layer.updateRoundResult(new Protocol.RoundResult((s % 2) + 1, s, 5 - s, s == 5));
                layer.setRtt(35 + (s * 30L));
            }).start();

            f.setVisible(true);
        });
    }
}
