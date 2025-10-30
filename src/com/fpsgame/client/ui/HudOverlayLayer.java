package com.fpsgame.client.ui;

import javax.swing.*;
import java.awt.*;

/**
 * 게임 화면 위에 겹쳐 표시되는 HUD 레이어 구성용 JLayeredPane.
 *
 * <p>구성 요소</p>
 * <ul>
 *   <li>{@link HudBarPanel}            : 상단 중앙의 경기 정보 바</li>
 *   <li>{@link FpsOverlayPanel}        : 좌상단 FPS 박스(GlassPane 대체)</li>
 *   <li>{@link LatencyGraphPanel}      : 우하단 RTT 스파크라인</li>
 *   <li>{@link CenterMessageOverlay}   : 중앙 대형 메시지(라운드 시작 등)</li>
 * </ul>
 *
 * <p>특징</p>
 * <ul>
 *   <li>부모 컨테이너 크기 변경 시 자동으로 앵커 배치</li>
 *   <li>각 구성 요소의 참조 접근자 제공 → 외부 컨트롤러에서 상태 갱신</li>
 *   <li>JLayeredPane을 사용하여 Z-순서가 명확</li>
 * </ul>
 *
 * <p>사용 예</p>
 * <pre>
 *   HudOverlayLayer hud = new HudOverlayLayer();
 *   frame.setContentPane(new JLayeredPane() {{ add(hud, Integer.valueOf(1)); }});
 *   // 또는 일반 컨테이너에 BorderLayout.CENTER로 추가 가능
 *   hud.getHudBar().setPhaseText("ROUND_RUNNING");
 *   hud.getCenterOverlay().showMessage("ROUND START", 1200);
 * </pre>
 */
public final class HudOverlayLayer extends JLayeredPane {

    // 레이어 구분(값이 클수록 위)
    private static final Integer L_HUDBAR   = 10;
    private static final Integer L_SMALLUI  = 20;
    private static final Integer L_CENTER   = 30;

    // 구성 요소
    private final HudBarPanel hudBar = new HudBarPanel();
    private final FpsOverlayPanel fps = new FpsOverlayPanel();
    private final LatencyGraphPanel rtt = new LatencyGraphPanel();
    private final CenterMessageOverlay centerOverlay = new CenterMessageOverlay(); // 설치형 대신 독립 컴포넌트

    // 내부 패딩/마진
    private static final int PAD = 10;

    public HudOverlayLayer() {
        setLayout(null); // 수동 배치

        // 배경 투명
        setOpaque(false);

        // 기본 크기 지정
        fps.setSize(fps.getPreferredSize());
        rtt.setSize(rtt.getPreferredSize());

        // 레이어에 추가
        add(hudBar, L_HUDBAR);
        add(fps, L_SMALLUI);
        add(rtt, L_SMALLUI);
        add(centerOverlay, L_CENTER);

        // 초기 배치
        doLayoutAnchored();

        // 리사이즈 시 재배치
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override public void componentResized(java.awt.event.ComponentEvent e) {
                doLayoutAnchored();
            }
        });
    }

    // ====================== 앵커 배치 ======================

    /** 부모 크기에 맞춰 각 구성 요소를 앵커 기준으로 배치한다. */
    private void doLayoutAnchored() {
        int w = getWidth();
        int h = getHeight();

        // 상단 HUD 바(전체 폭)
        int hudH = 34;
        hudBar.setBounds(0, 0, w, hudH);

        // 좌상단 FPS
        Dimension fpsSz = fps.getPreferredSize();
        fps.setBounds(PAD, PAD + 2, fpsSz.width, fpsSz.height);

        // 우하단 RTT 그래프
        Dimension rttSz = rtt.getPreferredSize();
        int rx = Math.max(PAD, w - rttSz.width - PAD);
        int ry = Math.max(PAD, h - rttSz.height - PAD);
        rtt.setBounds(rx, ry, rttSz.width, rttSz.height);

        // 중앙 메시지(레이어 전체를 차지하게 두고 자체 렌더에서 중앙 정렬)
        centerOverlay.setBounds(0, 0, w, h);

        revalidate();
        repaint();
    }

    // ====================== 접근자 ======================

    /** 상단 HUD 바 */
    public HudBarPanel getHudBar() { return hudBar; }

    /** 좌상단 FPS 박스 */
    public FpsOverlayPanel getFpsPanel() { return fps; }

    /** 우하단 RTT 그래프 */
    public LatencyGraphPanel getRttGraph() { return rtt; }

    /** 중앙 대형 메시지 오버레이 */
    public CenterMessageOverlay getCenterOverlay() { return centerOverlay; }

    // ====================== 단독 실행 데모 ======================

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}

            JFrame f = new JFrame("HudOverlayLayer Demo");
            f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            f.setSize(1024, 640);
            f.setLocationRelativeTo(null);

            // 배경(게임 화면 대체)
            JPanel bg = new JPanel() {
                { setBackground(new Color(36, 38, 42)); }
                @Override protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D gg = (Graphics2D) g;
                    gg.setColor(new Color(52, 54, 60));
                    for (int y = 0; y < getHeight(); y += 32) {
                        gg.drawLine(0, y, getWidth(), y);
                    }
                    for (int x = 0; x < getWidth(); x += 32) {
                        gg.drawLine(x, 0, x, getHeight());
                    }
                }
            };
            bg.setLayout(new BorderLayout());

            // 오버레이 레이어
            HudOverlayLayer hud = new HudOverlayLayer();

            // 루트에 겹치기
            JLayeredPane root = new JLayeredPane();
            f.setContentPane(root);
            root.setLayout(null);
            root.add(bg, Integer.valueOf(0));
            root.add(hud, Integer.valueOf(1));

            // 레이아웃 동기화
            root.addComponentListener(new java.awt.event.ComponentAdapter() {
                @Override public void componentResized(java.awt.event.ComponentEvent e) {
                    int w = root.getWidth(), h = root.getHeight();
                    bg.setBounds(0, 0, w, h);
                    hud.setBounds(0, 0, w, h);
                }
            });

            // 데모: HUD 값 갱신
            hud.getHudBar().setPhaseText("LOBBY");
            hud.getHudBar().setScores(0, 0);
            hud.getHudBar().setAliveCounts(5, 5);
            hud.getHudBar().setTimerSeconds(90);

            // FPS 업데이트 타이머
            new Timer(16, e -> hud.getFpsPanel().frameArrived()).start();

            // RTT 샘플 타이머
            new Timer(250, e -> {
                long ms = 25 + (long) (Math.random() * 15);
                if (Math.random() < 0.1) ms += (long) (Math.random() * 120);
                hud.getRttGraph().addSample(ms);
            }).start();

            // 중앙 메시지 테스트
            SwingUtilities.invokeLater(() -> hud.getCenterOverlay().showMessage("WELCOME", 1200));

            f.setVisible(true);
        });
    }
}
