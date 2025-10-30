package com.fpsgame.client.ui;

import com.fpsgame.client.model.Keybinds;
import java.awt.*;
import java.awt.event.*;
import java.util.EnumMap;
import java.util.Objects;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/**
 * 키 바인딩 편집 패널.
 * <p>
 * - {@link Keybinds} 모델을 직접 수정하는 UI
 * - 각 액션별 현재 키를 표시하고, "변경" 버튼으로 다음 키 입력을 캡처
 * - ESC로 캡처 취소, "지우기"로 바인딩 제거(UNDEFINED)
 * - 라벨 우클릭 컨텍스트 메뉴로 마우스 프록시(좌/우/중) 선택 가능
 *
 * 주의:
 * - 실제 런타임에서 마우스 버튼 이벤트를 키와 동일하게 캡처하기는 까다롭기 때문에,
 *   마우스는 컨텍스트 메뉴를 통해 설정하도록 했다.
 */
public final class KeybindEditorPanel extends JPanel {

    private final Keybinds model;

    /** 액션별 라벨/버튼 참조 저장(화면 갱신에 사용) */
    private final EnumMap<Keybinds.Action, JLabel> labelByAction = new EnumMap<>(Keybinds.Action.class);
    private final EnumMap<Keybinds.Action, JButton> changeBtnByAction = new EnumMap<>(Keybinds.Action.class);

    /** 현재 캡처 중인 액션 (null 이면 비캡처 상태) */
    private volatile Keybinds.Action capturing = null;

    /** 키 캡처를 위해 임시로 등록하는 디스패처 */
    private KeyEventDispatcher dispatcher;

    public KeybindEditorPanel(Keybinds keybinds) {
        super(new BorderLayout());
        this.model = Objects.requireNonNull(keybinds, "keybinds");
        setBorder(new EmptyBorder(8, 8, 8, 8));
        add(buildTable(), BorderLayout.CENTER);
    }

    /* ---------------------------------------------------------------------
     * UI 빌드: 표 형태
     * ------------------------------------------------------------------- */

    private JComponent buildTable() {
        JPanel grid = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(4, 6, 4, 6);
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.gridy = 0;
        gc.gridx = 0;

        // 헤더
        JLabel h1 = new JLabel("액션");
        JLabel h2 = new JLabel("현재 키");
        JLabel h3 = new JLabel("변경");
        JLabel h4 = new JLabel("지우기");
        h1.setFont(h1.getFont().deriveFont(Font.BOLD));
        h2.setFont(h2.getFont().deriveFont(Font.BOLD));
        h3.setFont(h3.getFont().deriveFont(Font.BOLD));
        h4.setFont(h4.getFont().deriveFont(Font.BOLD));

        gc.weightx = 0.6; grid.add(h1, gc);
        gc.gridx = 1; gc.weightx = 0.4; grid.add(h2, gc);
        gc.gridx = 2; gc.weightx = 0.0; grid.add(h3, gc);
        gc.gridx = 3; gc.weightx = 0.0; grid.add(h4, gc);

        // 각 액션 행
        for (Keybinds.Action a : Keybinds.Action.values()) {
            gc.gridy++;
            gc.gridx = 0; gc.weightx = 0.6;
            grid.add(new JLabel(actionDisplayName(a)), gc);

            // 현재 키 라벨(우클릭 컨텍스트 메뉴 제공)
            gc.gridx = 1; gc.weightx = 0.4;
            JLabel keyLabel = new JLabel(currentKeyName(a));
            keyLabel.setBorder(new EmptyBorder(0, 4, 0, 4));
            keyLabel.setComponentPopupMenu(buildMousePopup(a));
            labelByAction.put(a, keyLabel);
            grid.add(keyLabel, gc);

            // 변경 버튼
            gc.gridx = 2; gc.weightx = 0.0;
            JButton change = new JButton("변경");
            change.addActionListener(e -> startCapture(a));
            changeBtnByAction.put(a, change);
            grid.add(change, gc);

            // 지우기 버튼
            gc.gridx = 3;
            JButton clear = new JButton("지우기");
            clear.addActionListener(e -> clearBinding(a));
            grid.add(clear, gc);
        }

        // 남는 공간 채우기
        gc.gridy++; gc.gridx = 0; gc.gridwidth = 4; gc.weighty = 1; gc.fill = GridBagConstraints.BOTH;
        grid.add(Box.createGlue(), gc);

        JScrollPane scroll = new JScrollPane(grid);
        scroll.setBorder(new EmptyBorder(0, 0, 0, 0));
        return scroll;
    }

    /* ---------------------------------------------------------------------
     * 동작: 표시/갱신
     * ------------------------------------------------------------------- */

    /** 현재 모델 값을 라벨에 반영 */
    private void refreshRow(Keybinds.Action a) {
        JLabel lab = labelByAction.get(a);
        if (lab != null) {
            int code = model.get(a);
            String name = Keybinds.MouseEventProxy.name(code);
            lab.setText(name);
        }
    }

    private String currentKeyName(Keybinds.Action a) {
        int code = model.get(a);
        return Keybinds.MouseEventProxy.name(code);
    }

    /* ---------------------------------------------------------------------
     * 동작: 캡처/지우기
     * ------------------------------------------------------------------- */

    private void startCapture(Keybinds.Action a) {
        if (capturing != null) {
            // 이미 캡처 중이면 무시
            return;
        }
        capturing = a;

        JButton btn = changeBtnByAction.get(a);
        if (btn != null) {
            btn.setEnabled(false);
            btn.setText("입력 대기(Esc 취소)");
        }

        // 전역 키 디스패처 등록
        dispatcher = e -> {
            if (capturing == null) return false;

            // KeyPressed/Released 중 하나만 처리
            if (e.getID() != KeyEvent.KEY_PRESSED) return false;

            // ESC → 취소
            if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                stopCapture(false, 0);
                return true;
            }

            int code = e.getKeyCode();
            // 일부 시스템 키(Shift, Ctrl) 단독은 바인딩에 부적합 → 무시
            if (isModifierOnly(code)) {
                // 무시하되 캡처는 유지
                return true;
            }

            stopCapture(true, code);
            return true;
        };
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(dispatcher);
        requestFocusInWindow();
    }

    private void stopCapture(boolean apply, int keyCode) {
        Keybinds.Action a = capturing;
        capturing = null;

        // 디스패처 해제
        if (dispatcher != null) {
            KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(dispatcher);
            dispatcher = null;
        }

        // 버튼 복구
        JButton btn = changeBtnByAction.get(a);
        if (btn != null) {
            btn.setEnabled(true);
            btn.setText("변경");
        }

        if (apply && a != null) {
            model.set(a, keyCode);
            refreshRow(a);
        }
    }

    private void clearBinding(Keybinds.Action a) {
        model.set(a, KeyEvent.VK_UNDEFINED);
        refreshRow(a);
    }

    /* ---------------------------------------------------------------------
     * 컨텍스트 메뉴(마우스 프록시)
     * ------------------------------------------------------------------- */

    private JPopupMenu buildMousePopup(Keybinds.Action a) {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem left = new JMenuItem("마우스 좌클릭");
        JMenuItem right = new JMenuItem("마우스 우클릭");
        JMenuItem middle = new JMenuItem("마우스 중클릭");
        JMenuItem unset = new JMenuItem("해제");

        left.addActionListener(e -> { model.set(a, Keybinds.MouseEventProxy.VK_MOUSE_LEFT); refreshRow(a); });
        right.addActionListener(e -> { model.set(a, Keybinds.MouseEventProxy.VK_MOUSE_RIGHT); refreshRow(a); });
        middle.addActionListener(e -> { model.set(a, Keybinds.MouseEventProxy.VK_MOUSE_MIDDLE); refreshRow(a); });
        unset.addActionListener(e -> { model.set(a, KeyEvent.VK_UNDEFINED); refreshRow(a); });

        menu.add(new JLabel("마우스 바인딩 설정", SwingConstants.CENTER));
        menu.addSeparator();
        menu.add(left);
        menu.add(right);
        menu.add(middle);
        menu.addSeparator();
        menu.add(unset);
        return menu;
    }

    /* ---------------------------------------------------------------------
     * 유틸
     * ------------------------------------------------------------------- */

    private static String actionDisplayName(Keybinds.Action a) {
        switch (a) {
            case MOVE_UP: return "이동 ↑";
            case MOVE_DOWN: return "이동 ↓";
            case MOVE_LEFT: return "이동 ←";
            case MOVE_RIGHT: return "이동 →";
            case ATTACK_PRIMARY: return "주 공격 (LMB)";
            case SKILL_TACTICAL: return "전술 스킬 (E)";
            case SKILL_ULTIMATE: return "궁극기 (Q)";
            case OPEN_MENU: return "옵션 열기 (ESC)";
            case CHAT_SEND: return "채팅 전송 (Enter)";
            case READY_TOGGLE: return "레디 토글 (R)";
            case CHANGE_CHARACTER: return "캐릭터 변경 (B)";
            default: return a.name();
        }
    }

    private static boolean isModifierOnly(int keyCode) {
        return keyCode == KeyEvent.VK_SHIFT ||
               keyCode == KeyEvent.VK_CONTROL ||
               keyCode == KeyEvent.VK_ALT ||
               keyCode == KeyEvent.VK_META ||
               keyCode == KeyEvent.VK_ALT_GRAPH;
    }

    // 단독 실행 데모
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("KeybindEditorPanel Demo");
            f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            KeybindEditorPanel p = new KeybindEditorPanel(Keybinds.defaults());
            f.setContentPane(p);
            f.setSize(760, 520);
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        });
    }
}
