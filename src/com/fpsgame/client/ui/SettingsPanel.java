package com.fpsgame.client.ui;

import com.fpsgame.client.model.SettingsStore;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.IOException;

/**
 * {@link SettingsStore} 와 연동되는 간단한 설정 패널.
 *
 * <p>항목</p>
 * <ul>
 *   <li>마스터 볼륨, SFX 볼륨</li>
 *   <li>마우스 감도</li>
 *   <li>시야각(FOV)</li>
 *   <li>마지막 접속 Host/Port</li>
 * </ul>
 *
 * <p>기능</p>
 * <ul>
 *   <li>Load/Save 버튼으로 디스크와 동기화</li>
 *   <li>적용(Apply) 시 현재 UI 값을 {@link SettingsStore} 에 반영만 수행</li>
 *   <li>Reset(기본값) 버튼 제공</li>
 * </ul>
 */
public class SettingsPanel extends JPanel {

    // ===== 모델 =====
    private final SettingsStore store;

    // ===== UI 위젯 =====
    private final JSlider sMaster = slider01();
    private final JSlider sSfx    = slider01();
    private final JSlider sSens   = sliderRange(1, 100);   // 0.1~10.0 → 1~100 매핑
    private final JSpinner spFov  = new JSpinner(new SpinnerNumberModel(90, 60, 120, 1));

    private final JTextField tfHost = new JTextField(16);
    private final JSpinner spPort   = new JSpinner(new SpinnerNumberModel(7777, 1, 65535, 1));

    private final JButton btnLoad   = new JButton("Load");
    private final JButton btnApply  = new JButton("Apply");
    private final JButton btnSave   = new JButton("Save");
    private final JButton btnReset  = new JButton("Reset");

    private final JLabel status = new JLabel("—");

    public SettingsPanel() {
        this(new SettingsStore());
    }

    public SettingsPanel(SettingsStore store) {
        super(new BorderLayout(0, 8));
        this.store = store;

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(new EmptyBorder(10, 12, 10, 12));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;

        int row = 0;

        // 볼륨
        addRow(form, c, row++, "Master Volume", sMaster, valueLabel(sMaster, v -> format01(v)));
        addRow(form, c, row++, "SFX Volume", sSfx, valueLabel(sSfx, v -> format01(v)));

        // 마우스 감도(0.1~10.0)
        addRow(form, c, row++, "Mouse Sensitivity", sSens, valueLabel(sSens, v -> String.format("%.2f", mapSens(v))));

        // FOV
        ((JSpinner.DefaultEditor) spFov.getEditor()).getTextField().setColumns(4);
        addRow(form, c, row++, "FOV", spFov, new JLabel("deg"));

        // 네트
        JPanel hostPort = new JPanel(new GridBagLayout());
        GridBagConstraints hc = new GridBagConstraints();
        hc.insets = new Insets(0, 4, 0, 4);
        hc.gridy = 0;
        hc.gridx = 0; hostPort.add(new JLabel("Host"), hc);
        hc.gridx = 1; hc.weightx = 1.0; hc.fill = GridBagConstraints.HORIZONTAL; hostPort.add(tfHost, hc);
        hc.gridx = 2; hc.weightx = 0; hc.fill = GridBagConstraints.NONE; hostPort.add(new JLabel("Port"), hc);
        hc.gridx = 3; hostPort.add(spPort, hc);
        addRow(form, c, row++, "Last Server", hostPort, null);

        add(form, BorderLayout.CENTER);

        // 하단 버튼/상태
        JPanel bottom = new JPanel(new GridBagLayout());
        bottom.setBorder(new EmptyBorder(0, 12, 12, 12));
        GridBagConstraints bc = new GridBagConstraints();
        bc.insets = new Insets(0, 6, 0, 6);
        bc.gridy = 0;

        bc.gridx = 0; bottom.add(btnLoad, bc);
        bc.gridx = 1; bottom.add(btnReset, bc);
        bc.gridx = 2; bottom.add(btnApply, bc);
        bc.gridx = 3; bottom.add(btnSave, bc);

        bc.gridx = 4; bc.weightx = 1.0; bc.anchor = GridBagConstraints.EAST;
        status.setForeground(new Color(180, 180, 180));
        bottom.add(status, bc);

        add(bottom, BorderLayout.SOUTH);

        // 액션
        btnLoad.addActionListener(e -> doLoad());
        btnApply.addActionListener(e -> doApply());
        btnSave.addActionListener(e -> doSave());
        btnReset.addActionListener(e -> doReset());

        // 초기 로드
        doLoad();
    }

    // ========================== 행 추가 유틸 ==========================

    /**
     * 폼 그리드에 한 행을 추가하는 헬퍼.
     * 좌측 라벨, 가운데 필드 컴포넌트, 우측 보조 라벨/뷰(옵션) 순으로 배치한다.
     */
    private static void addRow(JPanel form, GridBagConstraints base, int row,
                               String title, JComponent field, JComponent tail) {
        GridBagConstraints c = (GridBagConstraints) base.clone();

        // 왼쪽 제목
        c.gridx = 0; c.gridy = row; c.weightx = 0; c.fill = GridBagConstraints.NONE; c.anchor = GridBagConstraints.WEST;
        form.add(new JLabel(title), c);

        // 가운데 필드(확장)
        c = (GridBagConstraints) base.clone();
        c.gridx = 1; c.gridy = row; c.weightx = 1.0; c.fill = GridBagConstraints.HORIZONTAL;
        form.add(field, c);

        // 우측 보조(있을 때만)
        if (tail != null) {
            c = (GridBagConstraints) base.clone();
            c.gridx = 2; c.gridy = row; c.weightx = 0; c.fill = GridBagConstraints.NONE;
            form.add(tail, c);
        }
    }

    // ========================== 액션 ==========================

    /** 디스크에서 로드하여 UI에 반영 */
    private void doLoad() {
        store.load();
        sMaster.setValue((int) Math.round(store.getMasterVolume() * 100));
        sSfx.setValue((int) Math.round(store.getSfxVolume() * 100));
        sSens.setValue(invMapSens(store.getMouseSensitivity()));
        spFov.setValue(store.getFov());
        tfHost.setText(store.getLastHost());
        spPort.setValue(store.getLastPort());
        setStatus("Loaded");
    }

    /** UI의 현재 상태를 모델에 반영(저장은 하지 않음) */
    private void doApply() {
        store.setMasterVolume(sMaster.getValue() / 100.0);
        store.setSfxVolume(sSfx.getValue() / 100.0);
        store.setMouseSensitivity(mapSens(sSens.getValue()));
        store.setFov((Integer) spFov.getValue());
        store.setLastHost(tfHost.getText().trim());
        store.setLastPort((Integer) spPort.getValue());
        setStatus("Applied");
    }

    /** 모델을 디스크에 저장(필요 시 디렉터리 생성) */
    private void doSave() {
        doApply();
        try {
            store.save();
            setStatus("Saved to " + store.getFile().toString());
        } catch (IOException e) {
            setStatus("Save failed: " + e.getMessage(), true);
            // ★ 문법 오류 수정: '=>' 가 아니라 '+' 이어야 함
            JOptionPane.showMessageDialog(this, "저장 실패: " + e.getMessage(),
                    "Save Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** 기본값으로 되돌린 뒤 UI에 반영(저장은 하지 않음) */
    private void doReset() {
        sMaster.setValue(80);
        sSfx.setValue(90);
        sSens.setValue(invMapSens(1.0)); // 1.0
        spFov.setValue(90);
        tfHost.setText("127.0.0.1");
        spPort.setValue(7777);
        setStatus("Reset to defaults");
    }

    // ========================== 유틸 ==========================

    private static JSlider slider01() {
        JSlider s = new JSlider(0, 100, 80);
        s.setPaintTicks(true);
        s.setMajorTickSpacing(20);
        s.setMinorTickSpacing(5);
        return s;
    }

    private static JSlider sliderRange(int min, int max) {
        JSlider s = new JSlider(min, max, (min + max) / 2);
        s.setPaintTicks(true);
        s.setMajorTickSpacing((max - min) / 5);
        s.setMinorTickSpacing(Math.max(1, (max - min) / 20));
        return s;
    }

    private interface Format { String format(int sliderValue); }
    private static JLabel valueLabel(JSlider s, Format f) {
        JLabel l = new JLabel();
        l.setPreferredSize(new Dimension(60, l.getPreferredSize().height));
        Runnable update = () -> l.setText(f.format(s.getValue()));
        s.addChangeListener(e -> update.run());
        update.run();
        return l;
    }

    private static String format01(int v) {
        return String.format("%.0f%%", v / 100.0 * 100.0);
    }

    /** 슬라이더 1..100 → 0.1..10.0 비선형 맵(초반부 세밀하게) */
    private static double mapSens(int slider) {
        slider = Math.max(1, Math.min(100, slider));
        double t = (slider - 1) / 99.0;         // 0..1
        double curved = Math.pow(t, 0.65);      // 곡선
        return 0.1 + curved * (10.0 - 0.1);
    }

    /** 감도 값(0.1..10.0) → 슬라이더 값(1..100) 역변환(근사) */
    private static int invMapSens(double sens) {
        sens = Math.max(0.1, Math.min(10.0, sens));
        double t = (sens - 0.1) / (10.0 - 0.1); // 0..1
        double inv = Math.pow(t, 1.0 / 0.65);
        return (int) Math.round(inv * 99.0 + 1.0);
    }

    private void setStatus(String text) { setStatus(text, false); }
    private void setStatus(String text, boolean error) {
        status.setText(text == null ? "" : text);
        status.setForeground(error ? new Color(200, 60, 60) : new Color(150, 150, 150));
    }

    // ========================== 단독 실행 데모 ==========================

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}

            JFrame f = new JFrame("SettingsPanel Demo");
            f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            f.setSize(640, 420);
            f.setLocationRelativeTo(null);

            f.setContentPane(new SettingsPanel());
            f.setVisible(true);
        });
    }
}
