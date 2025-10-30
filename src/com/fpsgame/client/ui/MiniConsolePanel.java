package com.fpsgame.client.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * 가벼운 로그 콘솔 패널.
 *
 * <p>특징</p>
 * <ul>
 *   <li>상단에 간단한 툴바(클리어 버튼) + 중앙 스크롤 텍스트 영역</li>
 *   <li>최대 N줄 보존(기본 400줄), 초과 시 오래된 줄부터 제거</li>
 *   <li>EDT 안전: 외부 스레드에서 호출해도 내부에서 EDT 디스패치</li>
 *   <li>{@link #println(String)} / {@link #append(String)} / {@link #clear()} 제공</li>
 * </ul>
 */
public final class MiniConsolePanel extends JPanel {

    private final JTextArea area = new JTextArea();
    private final Deque<String> ring = new ArrayDeque<>();
    private final DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm:ss");

    /** 최대 보존 줄 수 */
    private volatile int maxLines;

    public MiniConsolePanel() {
        this(400);
    }

    public MiniConsolePanel(int maxLines) {
        super(new BorderLayout(0, 4));
        this.maxLines = Math.max(50, maxLines);
        buildUi();
    }

    private void buildUi() {
        setBorder(new EmptyBorder(4, 4, 4, 4));

        // 상단 툴바
        JToolBar tb = new JToolBar();
        tb.setFloatable(false);
        JButton btnClear = new JButton("Clear");
        tb.add(btnClear);
        add(tb, BorderLayout.NORTH);

        // 중앙 텍스트 영역
        area.setEditable(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        JScrollPane scroll = new JScrollPane(area,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        add(scroll, BorderLayout.CENTER);

        btnClear.addActionListener(e -> clear());
    }

    /** 최대 보존 줄 수를 설정(50 이상 권장). */
    public void setMaxLines(int lines) {
        this.maxLines = Math.max(50, lines);
        SwingUtilities.invokeLater(this::rebuildTextFromRing);
    }

    /** 한 줄을 출력(자동 개행, 타임스탬프 포함). */
    public void println(String line) {
        if (line == null) line = "";
        final String ts = "[" + LocalTime.now().format(timeFmt) + "] ";
        final String msg = ts + line;

        if (SwingUtilities.isEventDispatchThread()) {
            pushAndRender(msg);
        } else {
            final String m = msg;
            SwingUtilities.invokeLater(() -> pushAndRender(m));
        }
    }

    /** 개행 없이 텍스트 추가(마지막 줄에 덧붙임). 필요 시 자동 스크롤. */
    public void append(String text) {
        if (text == null) return;
        if (SwingUtilities.isEventDispatchThread()) {
            // 마지막 줄에 덧붙임: ring의 마지막 요소를 수정
            if (ring.isEmpty()) {
                ring.addLast(text);
            } else {
                String last = ring.removeLast();
                ring.addLast(last + text);
            }
            rebuildTextFromRing();
        } else {
            SwingUtilities.invokeLater(() -> append(text));
        }
    }

    /** 모든 로그를 지움. */
    public void clear() {
        if (SwingUtilities.isEventDispatchThread()) {
            ring.clear();
            area.setText("");
        } else {
            SwingUtilities.invokeLater(this::clear);
        }
    }

    // =================== 내부 구현 ===================

    private void pushAndRender(String line) {
        ring.addLast(line);
        while (ring.size() > maxLines) ring.removeFirst();
        rebuildTextFromRing();
    }

    private void rebuildTextFromRing() {
        StringBuilder sb = new StringBuilder(Math.min(8192, maxLines * 48));
        for (String s : ring) {
            sb.append(s).append('\n');
        }
        area.setText(sb.toString());
        area.setCaretPosition(area.getDocument().getLength());
    }
}
