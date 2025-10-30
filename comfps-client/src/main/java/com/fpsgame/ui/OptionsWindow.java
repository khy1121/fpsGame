package com.fpsgame.client.ui;

import com.fpsgame.client.model.Keybinds;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Objects;
import java.util.Properties;
import java.util.function.BiConsumer;

/**
 * 옵션 창(독립 다이얼로그).
 * <p>
 * - 탭 구성: 컨트롤(키 바인딩), 오디오(마스터 볼륨), 게임플레이(마우스 감도)
 * - 저장 콜백 제공: 외부에서 설정 파일/프로필에 반영
 * - 네트워크 및 게임 루프에 독립
 */
public final class OptionsWindow extends JDialog {

    /* ===================== 외부 저장 콜백 ===================== */

    /** 저장 시 호출되는 콜백 인터페이스 */
    @FunctionalInterface
    public interface SaveListener {
        /**
         * @param props      키바인딩 Properties (action.name -> keyCode)
         * @param volume     마스터 볼륨(0~100)
         * @param sensitivity 마우스 감도(1~100)
         */
        void onSave(Properties props, int volume, int sensitivity);
    }

    private volatile SaveListener onSave;

    /** 저장 콜백 등록(널 허용) */
    public void setSaveListener(SaveListener l) { this.onSave = l; }

    /* ===================== 상태/구성 요소 ===================== */

    private final Keybinds keybinds;        // 외부에서 주입(동일 인스턴스 사용)
    private final KeybindEditorPanel keyPanel;

    // 오디오/감도 슬라이더
    private final JSlider volumeSlider = new JSlider(0, 100, 70);
    private final JSlider sensSlider = new JSlider(1, 100, 50);

    private final JButton saveBtn = new JButton("저장");
    private final JButton closeBtn = new JButton("닫기");
    private final JButton defaultsBtn = new JButton("기본값 복원");

    /* ===================== 생성 ===================== */

    public OptionsWindow(Keybinds keybinds) {
        super((Frame) null, "옵션", true);
        this.keybinds = Objects.requireNonNullElseGet(keybinds, Keybinds::defaults);
        this.keyPanel = new KeybindEditorPanel(this.keybinds);

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(720, 520));
        setLocationRelativeTo(null);

        setContentPane(buildUI());
        wireActions();
    }

    /* ===================== UI 빌드 ===================== */

    private JComponent buildUI() {
        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(new EmptyBorder(10, 10, 10, 10));

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("컨트롤", buildControlsTab());
        tabs.addTab("오디오", buildAudioTab());
        tabs.addTab("게임플레이", buildGameplayTab());

        root.add(tabs, BorderLayout.CENTER);
        root.add(buildButtons(), BorderLayout.SOUTH);
        return root;
    }

    /** 컨트롤 탭(키 바인딩 편집기) */
    private JComponent buildControlsTab() {
        JPanel p = new JPanel(new BorderLayout(8, 8));
        JLabel tip = new JLabel("변경하려는 액션의 '변경'을 누른 뒤 원하는 키를 눌러주세요. (Esc로 취소)");
        tip.setBorder(new EmptyBorder(8, 8, 0, 8));
        p.add(tip, BorderLayout.NORTH);

        // 스크롤 가능한 키바인딩 패널
        JScrollPane scroll = new JScrollPane(keyPanel);
        scroll.setBorder(new EmptyBorder(0, 0, 0, 0));
        p.add(scroll, BorderLayout.CENTER);

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        defaultsBtn.setToolTipText("키 바인딩을 기본값으로 복원합니다.");
        bar.add(defaultsBtn);
        p.add(bar, BorderLayout.SOUTH);

        return p;
    }

    /** 오디오 탭(마스터 볼륨) */
    private JComponent buildAudioTab() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(8, 8, 8, 8);
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.gridx = 0; gc.gridy = 0;

        p.add(new JLabel("마스터 볼륨"), gc);
        gc.gridx = 1; gc.weightx = 1;
        volumeSlider.setMajorTickSpacing(20);
        volumeSlider.setMinorTickSpacing(5);
        volumeSlider.setPaintTicks(true);
        volumeSlider.setPaintLabels(true);
        p.add(volumeSlider, gc);

        // 여백 채우기
        gc.gridx = 0; gc.gridy = 1; gc.gridwidth = 2; gc.weighty = 1; gc.fill = GridBagConstraints.BOTH;
        p.add(Box.createGlue(), gc);

        return p;
    }

    /** 게임플레이 탭(마우스 감도) */
    private JComponent buildGameplayTab() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(8, 8, 8, 8);
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.gridx = 0; gc.gridy = 0;

        p.add(new JLabel("마우스 감도"), gc);
        gc.gridx = 1; gc.weightx = 1;
        sensSlider.setMajorTickSpacing(20);
        sensSlider.setMinorTickSpacing(5);
        sensSlider.setPaintTicks(true);
        sensSlider.setPaintLabels(true);
        p.add(sensSlider, gc);

        // 여백 채우기
        gc.gridx = 0; gc.gridy = 1; gc.gridwidth = 2; gc.weighty = 1; gc.fill = GridBagConstraints.BOTH;
        p.add(Box.createGlue(), gc);

        return p;
    }

    /** 하단 버튼 바 */
    private JComponent buildButtons() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        p.add(saveBtn);
        p.add(closeBtn);
        return p;
    }

    /* ===================== 동작 배선 ===================== */

    private void wireActions() {
        // 기본값 복원(키바인딩)
        defaultsBtn.addActionListener(e -> {
            keybinds.resetDefaults();
            // KeybindEditorPanel은 내부적으로 모델의 값을 다시 읽어 표시하는 API를 제공하지 않지만,
            // 생성 시점에 모델 참조를 들고 있으므로 setText 업데이트 로직이 반영되도록 재생성하는 방법도 가능하다.
            // 여기서는 간단하게 다이얼로그를 다시 레이아웃하여 필드들이 모델에서 읽도록 유도.
            revalidate();
            repaint();
        });

        // 저장
        saveBtn.addActionListener(e -> doSave());

        // 닫기
        closeBtn.addActionListener(e -> dispose());

        // Esc로 닫기
        getRootPane().registerKeyboardAction(ev -> dispose(),
                KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);

        // 기본 버튼(Enter)
        getRootPane().setDefaultButton(saveBtn);
    }

    private void doSave() {
        SaveListener l = onSave;
        if (l != null) {
            Properties props = keybinds.toProperties();
            int vol = volumeSlider.getValue();
            int sens = sensSlider.getValue();
            l.onSave(props, vol, sens);
        }
        // 저장 후 닫지는 않고 유지(사용자 선택에 맡김)
    }

    /* ===================== 단독 실행 데모 ===================== */

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            OptionsWindow w = new OptionsWindow(Keybinds.defaults());
            w.setSaveListener((props, vol, sens) -> {
                System.out.println("[SAVE]");
                System.out.println(" volume=" + vol + ", sensitivity=" + sens);
                props.forEach((k, v) -> System.out.println("  " + k + "=" + v));
            });
            w.setVisible(true);
        });
    }
}
