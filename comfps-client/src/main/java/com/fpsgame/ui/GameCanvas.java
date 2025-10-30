package com.fpsgame.client.ui;

import com.fpsgame.client.model.Keybinds;
import com.fpsgame.client.model.PlayerState;
import com.fpsgame.client.model.Viewport;
import com.fpsgame.common.Rect;
import com.fpsgame.common.Vec2;
import com.fpsgame.common.character.projectile.Projectile;
import com.fpsgame.common.character.projectile.ProjectileManager;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

/**
 * 게임 화면을 그리는 스윙 패널.
 *
 * <p>
 * - {@link Viewport}를 사용해 월드→스크린 좌표 변환 처리<br>
 * - 마우스 줌/Shift+드래그 패닝/키보드 WASD 이동<br>
 * - 장애물(직사각형 Rect)과 원형 플레이어의 충돌을 단순 처리<br>
 * - Swing Timer로 60FPS 렌더 루프를 돌리며(EDT에서 실행) 투사체도 갱신
 * </p>
 */
public final class GameCanvas extends JPanel {

    // ----- 렌더/입력 기본 설정 -----
    private static final int TARGET_FPS = 60;
    private static final float MOVE_SPEED_WU = 200f; // 초당 200 월드유닛(wu)

    // ----- 모델 -----
    private final Viewport vp = new Viewport(3000f, 2000f);
    private final PlayerState local = new PlayerState("Player", PlayerState.Team.RED);

    // 장애물 콜리전(단순 직사각형, 단위: wu)
    private final List<Rect> obstacles = new ArrayList<>();

    // 입력 상태
    private final Keybinds keybinds = Keybinds.defaults();
    private boolean up, down, left, right;
    private boolean panning = false;         // Shift 누른 채 마우스 드래그로 패닝
    private Point lastMouse;                  // 패닝 시 마지막 마우스 위치
    private final Vec2 tmp = new Vec2();     // 임시 벡터(가비지 줄이기용)

    // HUD 폰트
    private final Font hudFont = new Font(Font.MONOSPACED, Font.PLAIN, 12);
    private BufferedImage mapImage; // 맵 배경 이미지
    private String mapId = "terminal";

    public GameCanvas() {
        setBackground(new Color(0x111318));
        setDoubleBuffered(true);
        setFocusable(true);

        initDemoWorld();
        installResizeHook();
        installMouse();
        installKeyboard();
        startRenderTimer();
        // 기본 맵 배경 로드
        setMapByName(mapId);
    }

    // ---------------------------------------------------------------------
    // 데모 월드 구성
    // ---------------------------------------------------------------------

    /** 장애물/초기 위치 구성 */
    private void initDemoWorld() {
        obstacles.add(new Rect(400, 300, 300, 80));
        obstacles.add(new Rect(900, 600, 120, 280));
        obstacles.add(new Rect(1400, 400, 420, 120));
        obstacles.add(new Rect(1750, 900, 260, 220));
        local.setPosition(250, 250);
    }

    // ---------------------------------------------------------------------
    // 리사이즈 훅/뷰포트 갱신
    // ---------------------------------------------------------------------

    private void installResizeHook() {
        addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) {
                Dimension d = getSize();
                vp.resize(Math.max(1, d.width), Math.max(1, d.height));
                repaint();
            }
        });
    }

    // ---------------------------------------------------------------------
    // 마우스 입력: 줌/ Shift+드래그 패닝
    // ---------------------------------------------------------------------

    private void installMouse() {
        // 휠 줌
        addMouseWheelListener(e -> {
            float factor = (e.getWheelRotation() < 0) ? 1.1f : (1f / 1.1f);
            vp.zoomBy(factor);
            repaint();
        });

        // Shift+좌클릭 드래그 패닝
        addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                if (e.isShiftDown() && SwingUtilities.isLeftMouseButton(e)) {
                    panning = true;
                    lastMouse = e.getPoint();
                    setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                }
                requestFocusInWindow();
            }

            @Override public void mouseReleased(MouseEvent e) {
                if (panning && SwingUtilities.isLeftMouseButton(e)) {
                    panning = false;
                    setCursor(Cursor.getDefaultCursor());
                }
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseDragged(MouseEvent e) {
                if (!panning || lastMouse == null) return;
                Point now = e.getPoint();
                int dx = now.x - lastMouse.x;
                int dy = now.y - lastMouse.y;
                lastMouse = now;

                // 스크린 이동량을 월드 이동량으로 변환(뷰포트 스케일 고려)
                float wx = -dx / vp.getScale();
                float wy = -dy / vp.getScale();
                Vec2 center = vp.getCenter(null).add(wx, wy);
                vp.setCenter(center.x, center.y);
                repaint();
            }
        });
    }

    // ---------------------------------------------------------------------
    // 키보드 입력(WASD 이동)
    // ---------------------------------------------------------------------

    private void installKeyboard() {
        // 포커스 이동 키 비활성화(탭 등)
        setFocusTraversalKeysEnabled(false);

        // 입력 매핑/액션 맵
        InputMap im = getInputMap(WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getActionMap();

        // WASD + 방향키: 누름/뗌 이벤트
        bindKey(im, am, KeyStroke.getKeyStroke('W'), "w-press", true, () -> up = true);
        bindKey(im, am, KeyStroke.getKeyStroke('S'), "s-press", true, () -> down = true);
        bindKey(im, am, KeyStroke.getKeyStroke('A'), "a-press", true, () -> left = true);
        bindKey(im, am, KeyStroke.getKeyStroke('D'), "d-press", true, () -> right = true);

        bindKey(im, am, KeyStroke.getKeyStroke('W', 0, true), "w-release", false, () -> up = false);
        bindKey(im, am, KeyStroke.getKeyStroke('S', 0, true), "s-release", false, () -> down = false);
        bindKey(im, am, KeyStroke.getKeyStroke('A', 0, true), "a-release", false, () -> left = false);
        bindKey(im, am, KeyStroke.getKeyStroke('D', 0, true), "d-release", false, () -> right = false);

        bindKey(im, am, KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), "up-press", true, () -> up = true);
        bindKey(im, am, KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "down-press", true, () -> down = true);
        bindKey(im, am, KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "left-press", true, () -> left = true);
        bindKey(im, am, KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "right-press", true, () -> right = true);

        bindKey(im, am, KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0, true), "up-release", false, () -> up = false);
        bindKey(im, am, KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0, true), "down-release", false, () -> down = false);
        bindKey(im, am, KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0, true), "left-release", false, () -> left = false);

        // B 키: 캐릭터 선택 창 열기
        bindKey(im, am, KeyStroke.getKeyStroke(KeyEvent.VK_B, 0), "open-chooser", true, () -> {
            SwingUtilities.invokeLater(() -> CharacterSelectDialog.show(this));
        });

        bindKey(im, am, KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0, true), "right-release", false, () -> right = false);
    }

    private void bindKey(InputMap im, ActionMap am, KeyStroke ks, String name, boolean pressed, Runnable r) {
        im.put(ks, name);
        am.put(name, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { r.run(); }
        });
    }

    // ---------------------------------------------------------------------
    // 렌더 루프(Swing Timer)
    // ---------------------------------------------------------------------

    private void startRenderTimer() {
        int delay = Math.max(1, 1000 / TARGET_FPS);
        new Timer(delay, e -> {
            updatePlayer(delay / 1000f);
            // 투사체 매니저 업데이트(예외 안전)
            try { ProjectileManager.getInstance().update(delay / 1000f); } catch (Throwable ignore) {}
            repaint();
        }).start();
    }

    // 플레이어 이동/충돌(원형 vs AABB 단순 충돌)
    private void updatePlayer(float dt) {
        float dx = 0, dy = 0;
        if (up) dy -= 1f;
        if (down) dy += 1f;
        if (left) dx -= 1f;
        if (right) dx += 1f;

        if (dx != 0 || dy != 0) {
            float len = (float) Math.sqrt(dx * dx + dy * dy);
            dx /= len; dy /= len;
            float stepX = dx * MOVE_SPEED_WU * dt;
            float stepY = dy * MOVE_SPEED_WU * dt;

            // 이동 + 충돌 처리(원형 플레이어 vs AABB 장애물)
            float nextX = local.getX() + stepX;
            float nextY = local.getY() + stepY;
            float r = 18f;

            // X축
            if (!collides(nextX, local.getY(), r)) {
                local.setPosition(nextX, local.getY());
            }
            // Y축
            if (!collides(local.getX(), nextY, r)) {
                local.setPosition(local.getX(), nextY);
            }
            // 카메라는 플레이어를 따라감(부드럽게가 필요하면 추후 보간)
            vp.setCenter(local.getX(), local.getY());
        }
    }

    private boolean collides(float cx, float cy, float radius) {
        for (Rect ob : obstacles) {
            float nearestX = clamp(cx, ob.x, ob.right());
            float nearestY = clamp(cy, ob.y, ob.bottom());
            float dx = cx - nearestX;
            float dy = cy - nearestY;
            if (dx * dx + dy * dy < radius * radius) return true;
        }
        return false;
    }

    private static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }

    // ---------------------------------------------------------------------
    // 렌더링
    // ---------------------------------------------------------------------

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0.create();
        
        if (mapImage != null) {
            g.drawImage(mapImage, 0, 0, getWidth(), getHeight(), null);
        }

        // 그리드(좌표 가이드)
        drawGrid(g);

        // 장애물
        g.setColor(new Color(0x455A64));
        for (Rect r : obstacles) {
            Point p = worldToScreen(r.x, r.y);
            int w = Math.round(r.w * vp.getScale());
            int h = Math.round(r.h * vp.getScale());
            g.fillRect(p.x, p.y, w, h);
        }

        // 플레이어(원)
        Point pc = worldToScreen(local.getX(), local.getY());
        int pr = Math.round(18f * vp.getScale());
        g.setColor(new Color(0x90CAF9));
        g.fillOval(pc.x - pr, pc.y - pr, pr * 2, pr * 2);
        g.setColor(new Color(0x1976D2));
        g.drawOval(pc.x - pr, pc.y - pr, pr * 2, pr * 2);

        // 투사체(예외 안전)
        try {
            var list = ProjectileManager.getInstance().getProjectiles();
            g.setColor(new Color(0xFFD54F));
            int r = Math.max(2, Math.round(6f * vp.getScale()));
            for (Projectile p : list) {
                Vec2 wp = p.getPosition();
                Point sp = worldToScreen(wp.x, wp.y);
                g.fillOval(sp.x - r, sp.y - r, r * 2, r * 2);
            }
        } catch (Throwable ignore) {}

        // HUD
        g.setFont(hudFont);
        g.setColor(new Color(0xEEEEEE));
        g.drawString(String.format("pos(%.1f, %.1f) scale=%.2f cam(%.1f, %.1f)",
                local.getX(), local.getY(), vp.getScale(),
                vp.getCenter(tmp).x, vp.getCenter(tmp).y), 8, getHeight() - 8);

        g.dispose();
    }

    private void drawGrid(Graphics2D g) {
        Rect view = vp.getViewBounds(null);
        float grid = 100f; // 100 wu 간격
        float startX = (float) (Math.floor(view.x / grid) * grid);
        float startY = (float) (Math.floor(view.y / grid) * grid);

        g.setColor(new Color(0x263238));
        for (float x = startX; x <= view.right(); x += grid) {
            Point p1 = worldToScreen(x, view.y);
            Point p2 = worldToScreen(x, view.bottom());
            g.drawLine(p1.x, p1.y, p2.x, p2.y);
        }
        for (float y = startY; y <= view.bottom(); y += grid) {
            Point p1 = worldToScreen(view.x, y);
            Point p2 = worldToScreen(view.right(), y);
            g.drawLine(p1.x, p1.y, p2.x, p2.y);
        }
    }

    private Point worldToScreen(float wx, float wy) {
        return vp.worldToScreen(wx, wy);
    }

    // ---------------------------------------------------------------------
    // 게터/설정자
    // ---------------------------------------------------------------------

    /** 로컬 플레이어 상태(디버그/화면 HUD에서 사용) */
    public PlayerState getLocalPlayer() {
        return local;
    }

    /** 맵 이미지 설정(파일 또는 클래스패스에서 로드) */
    public void setMapByName(String id) {
        this.mapId = (id == null || id.isBlank()) ? "terminal" : id.trim();
        BufferedImage img = ImageUtil.loadFile("assets/maps/" + mapId + ".png");
        if (img == null) img = ImageUtil.loadFile("assets/maps/" + mapId + ".jpg");
        if (img == null) img = ImageUtil.loadResource(GameCanvas.class, "/assets/maps/" + mapId + ".png");
        if (img == null) img = ImageUtil.loadResource(GameCanvas.class, "/assets/maps/" + mapId + ".jpg");
        this.mapImage = img;
        repaint();
    }
}
