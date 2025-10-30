package com.fpsgame.client.ui;

import com.fpsgame.client.NetClient;
import com.fpsgame.client.model.PlayerSnapshotBuffer;
import com.fpsgame.common.Binary;
import com.fpsgame.common.Protocol;
import com.fpsgame.common.GameEnums;
import com.fpsgame.common.ProjectilesV2;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

/**
 * 네트워크 데모 캔버스(간단 렌더 + 입력 전송)
 * - 주기적으로 INPUT 전송(WASD + 조준 각도)
 * - SNAPSHOT(v1/v2) 보간 렌더
 * - PROJECTILES(v2) 렌더
 */
public final class GameCanvasNetDemo extends JPanel implements KeyListener, MouseMotionListener, MouseListener, FocusListener, AutoCloseable {

    // 네트워킹
    private volatile NetClient net;

    // 스냅샷 보간 버퍼
    private final PlayerSnapshotBuffer snapshots = new PlayerSnapshotBuffer(100_000_000L); // 100ms 지연
    private static final class P { final float x,y; final int type; P(float x,float y,int type){ this.x=x; this.y=y; this.type=type; } }
    private volatile List<P> projectiles = java.util.Collections.emptyList();

    // 입력 상태
    private volatile boolean keyW, keyA, keyS, keyD;
    private volatile float aimRad = 0f; // 라디안
    // 타이머
    private final Timer repaintTimer; // ~60fps
    private final Timer inputTimer;   // 30Hz 입력 전송

    // 카메라
    private float pixelsPerUnit = 32f;
    private float camX = 0f, camY = 0f;
    private final boolean followFirst = true;

    // 패닝(Shift + 드래그)
    private boolean panning = false;
    private Point lastMouse;

    public GameCanvasNetDemo() {
        setBackground(new Color(30, 32, 36));
        setFocusable(true);
        setDoubleBuffered(true);

        setToolTipText("WASD to move, mouse to aim, Shift+Drag to pan");

        repaintTimer = new Timer(16, e -> repaint());
        repaintTimer.start();

        SwingUtilities.invokeLater(() -> {
            addKeyListener(this);
            addMouseMotionListener(this);
            addMouseListener(this);
            addFocusListener(this);
            addMouseWheelListener(this::onMouseWheel);
        });

        inputTimer = new Timer(33, e -> sendInputIfConnected());
        inputTimer.start();
    }

    /** 서버 연결(기존 연결이 있으면 닫음) */
    public void connect(String host, int port) throws IOException {
        Objects.requireNonNull(host, "host");
        disconnect();

        NetClient.Listener listener = new NetClient.Listener() {
            @Override public void onOpen(NetClient c) {
                SwingUtilities.invokeLater(() -> {
                    Window w = SwingUtilities.getWindowAncestor(GameCanvasNetDemo.this);
                    if (w != null) JOptionPane.showMessageDialog(w, "Connected", "Info", JOptionPane.INFORMATION_MESSAGE);
                    requestFocusInWindow();
                });
            }
            @Override public void onClosed(NetClient c, String reason) {}
            @Override public void onDisconnected(String reason) {}
            @Override public void onChat(String text) {}

            @Override public void onFrame(NetClient c, Protocol.Frame frame) {
                if (frame == null) return;
                try {
                    if (frame.opcode == Protocol.Opcode.SNAPSHOT) {
                        snapshots.push(frame.payload, System.nanoTime());
                        SwingUtilities.invokeLater(GameCanvasNetDemo.this::repaint);
                    } else if (frame.opcode == Protocol.PROJECTILES) {
                        projectiles = parseProjectilesV2(frame.payload);
                        SwingUtilities.invokeLater(GameCanvasNetDemo.this::repaint);
                    }
                } catch (Exception ignore) {}
            }
        };

        net = new NetClient(host, port, 10_000, listener);
    }

    /** 연결 해제 */
    public void disconnect() {
        NetClient c = net;
        net = null;
        if (c != null) {
            try { c.close(); } catch (Exception ignored) {}
        }
        snapshots.clear();
        projectiles = java.util.Collections.emptyList();
    }

    @Override public void close() {
        try { disconnect(); } catch (Exception ignore) {}
        repaintTimer.stop();
        inputTimer.stop();
    }

    private void sendInputIfConnected() {
        NetClient c = net;
        if (c == null) return;
        try {
            int mask = 0;
            if (keyW) mask |= 0x01; // 위
            if (keyS) mask |= 0x02; // 아래
            if (keyA) mask |= 0x04; // 왼쪽
            if (keyD) mask |= 0x08; // 오른쪽
            ByteArrayOutputStream baos = new ByteArrayOutputStream(5);
            Binary.putByte(baos, mask);
            Binary.putFloat(baos, aimRad);
            c.send(Protocol.Opcode.INPUT, baos.toByteArray());
        } catch (IOException ignored) {}
    }

    // ====================== Render ======================

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // interpolate snapshot to current time
            List<PlayerSnapshotBuffer.EntryEx> players = snapshots.sampleEx(System.nanoTime());

            // follow first player
            if (followFirst && !players.isEmpty()) {
                camX = players.get(0).x;
                camY = players.get(0).y;
            }

            // grid
            drawGrid(g);

            // players
            g.setStroke(new BasicStroke(1f));
            for (PlayerSnapshotBuffer.EntryEx e : players) {
                drawPlayer(g, e);
            }

            // projectiles
            if (projectiles != null && !projectiles.isEmpty()) {
                int r = 4; // radius in pixels
                for (P p : projectiles) {
                    if (p.type == ProjectilesV2.TYPE_BULLET) g.setColor(new Color(0xFFD54F)); else g.setColor(new Color(0xFF7043));
                    int sx = (int) Math.round(toScreenX(p.x));
                    int sy = (int) Math.round(toScreenY(p.y));
                    g.fillOval(sx - r, sy - r, r * 2, r * 2);
                }
            }

            // HUD
            g.setColor(new Color(230, 230, 230));
            g.setFont(getFont().deriveFont(Font.BOLD, 12f));
            g.drawString((net != null ? "CONNECTED" : "DISCONNECTED") +
                         String.format("  cam(%.1f, %.1f)  zoom=%.1f", camX, camY, pixelsPerUnit), 10, 18);
        } finally {
            g.dispose();
        }
    }

    private void drawPlayer(Graphics2D g, PlayerSnapshotBuffer.EntryEx e) {
        float rWorld = 0.35f; // world radius
        int cx = (int) Math.round(toScreenX(e.x));
        int cy = (int) Math.round(toScreenY(e.y));
        int rr = (int) Math.round(rWorld * pixelsPerUnit);

        // 팀색(기본: RED=주황계열, BLUE=하늘계열)
        if (e.team == 1) g.setColor(new Color(120, 200, 255)); else g.setColor(new Color(255, 170, 120));
        g.fillOval(cx - rr, cy - rr, rr * 2, rr * 2);
        g.setColor(new Color(20, 40, 60));
        g.drawOval(cx - rr, cy - rr, rr * 2, rr * 2);

        // 조준선
        float ax = (float) Math.cos(e.aim);
        float ay = (float) Math.sin(e.aim);
        int lx = (int) Math.round(cx + ax * rr * 1.6);
        int ly = (int) Math.round(cy - ay * rr * 1.6); // screen Y invert
        g.drawLine(cx, cy, lx, ly);

        // 플레이어 위에 캐릭터 뱃지(한 글자 표시)
        try {
            int chr = e.characterId;
            var ids = GameEnums.CharacterId.values();
            String name = (chr >= 0 && chr < ids.length) ? ids[chr].displayName() : "?";
            String badge = name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase();
            g.setFont(getFont().deriveFont(Font.BOLD, Math.max(10f, rr * 0.9f)));
            FontMetrics fm = g.getFontMetrics();
            int bw = fm.stringWidth(badge);
            int bh = fm.getAscent();
            g.setColor(new Color(10, 10, 10, 160));
            g.fillRoundRect(cx - bw/2 - 3, cy - rr - bh - 6, bw + 6, bh + 4, 6, 6);
            g.setColor(Color.WHITE);
            g.drawString(badge, cx - bw/2, cy - rr - 6);
        } catch (Throwable ignore) {}
    }

    private void drawGrid(Graphics2D g) {
        int w = getWidth(), h = getHeight();
        // 화면 중앙을 기준으로 월드 좌표 영역
        float left = camX - w / (2f * pixelsPerUnit);
        int startX = (int) Math.floor(left);
        int startY = (int) Math.floor((camY - h / (2f * pixelsPerUnit)));

        g.setColor(new Color(52, 54, 60));
        // vertical lines
        for (int ix = startX; toScreenX(ix) < w; ix++) {
            int sx = (int) Math.round(toScreenX(ix));
            g.drawLine(sx, 0, sx, h);
        }
        // horizontal lines
        for (int iy = startY; toScreenY(iy) < h; iy++) {
            int sy = (int) Math.round(toScreenY(iy));
            g.drawLine(0, sy, w, sy);
        }

        // axes highlight
        int zx = (int) Math.round(toScreenX(0));
        int zy = (int) Math.round(toScreenY(0));
        g.setColor(new Color(80, 90, 110));
        g.drawLine(0, zy, w, zy);
        g.drawLine(zx, 0, zx, h);
    }

    private float toScreenX(float wx) { return getWidth() * 0.5f + (wx - camX) * pixelsPerUnit; }
    private float toScreenY(float wy) { return getHeight() * 0.5f - (wy - camY) * pixelsPerUnit; }
    private float toWorldX(float sx) { return camX + (sx - getWidth() * 0.5f) / pixelsPerUnit; }
    private float toWorldY(float sy) { return camY - (sy - getHeight() * 0.5f) / pixelsPerUnit; }

    // ====================== Mouse helpers ======================

    private void onMouseWheel(MouseWheelEvent e) {
        float factor = (e.getWheelRotation() < 0) ? 1.1f : (1f / 1.1f);
        pixelsPerUnit *= factor;
        pixelsPerUnit = Math.max(8f, Math.min(160f, pixelsPerUnit));
        repaint();
    }

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
    @Override public void mouseDragged(MouseEvent e) {
        if (panning && lastMouse != null) {
            Point now = e.getPoint();
            int dx = now.x - lastMouse.x;
            int dy = now.y - lastMouse.y;
            lastMouse = now;
            camX -= dx / pixelsPerUnit;
            camY += dy / pixelsPerUnit;
            repaint();
        } else {
            mouseMoved(e);
        }
    }

    @Override public void mouseMoved(MouseEvent e) {
        // 화면 중앙에서 조준 각도 계산
        float wx = toWorldX(e.getX());
        float wy = toWorldY(e.getY());
        float dx = wx - camX;
        float dy = wy - camY;
        aimRad = (float) Math.atan2(dy, dx);
    }

    @Override public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_W -> keyW = true;
            case KeyEvent.VK_S -> keyS = true;
            case KeyEvent.VK_A -> keyA = true;
            case KeyEvent.VK_D -> keyD = true;
            default -> {}
        }
    }
    @Override public void keyReleased(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_W -> keyW = false;
            case KeyEvent.VK_S -> keyS = false;
            case KeyEvent.VK_A -> keyA = false;
            case KeyEvent.VK_D -> keyD = false;
            default -> {}
        }
    }

    @Override public void focusLost(FocusEvent e) { keyW = keyA = keyS = keyD = false; }

    // 사용하지 않는 콜백 스텁
    @Override public void mouseClicked(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}
    @Override public void keyTyped(KeyEvent e) {}
    @Override public void focusGained(FocusEvent e) {}

    // ====================== Standalone main ======================

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("GameCanvasNetDemo");
            f.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            f.setSize(1000, 640);
            f.setLocationRelativeTo(null);
            GameCanvasNetDemo canvas = new GameCanvasNetDemo();
            f.setContentPane(canvas);

            f.addWindowListener(new WindowAdapter() {
                @Override public void windowClosing(WindowEvent e) {
                    try { canvas.close(); } catch (Exception ignored) {}
                }
            });

            f.setVisible(true);

            // Auto connect prompt
            JoinDialog.Result r = JoinDialog.show(f, "127.0.0.1", 7777, System.getProperty("user.name", "player"));
            if (r != null) {
                try {
                    canvas.connect(r.host, r.port);
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(f, "Connect failed: " + ex.getMessage(), "Connect Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
    }

    // v2 parser only
    private static java.util.List<P> parseProjectilesV2(byte[] payload) throws java.io.IOException {
        if (payload == null || payload.length == 0) return java.util.Collections.emptyList();
        java.util.List<ProjectilesV2.Entry> v2 = ProjectilesV2.parse(payload);
        java.util.ArrayList<P> out = new java.util.ArrayList<>(v2.size());
        for (ProjectilesV2.Entry e : v2) out.add(new P(e.x, e.y, e.type));
        return out;
    }
}

