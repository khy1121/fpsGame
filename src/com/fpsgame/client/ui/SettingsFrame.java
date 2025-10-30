package com.fpsgame.client.ui;

import com.fpsgame.client.model.SettingsStore;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.IOException;

/**
 * 간단한 설정(오디오/마우스/FOV/최근 서버)을 편집/저장하는 단독 프레임.
 *
 * <p>의존성</p>
 * - {@link SettingsStore} : 실제 저장/로드 담당
 *
 * <p>특징</p>
 * - 모든 갱신은 EDT에서 수행
 * - 저장 실패 시 사용자에게 메시지로 알림
 */
public final class SettingsFrame extends JFrame {

    // ===== 모델 =====
    private final SettingsStore store = new SettingsStore();

    // ===== 위젯 =====
    private final JSlider sMaster = new JSlider(0, 100, 80);
    private final JSlider sSfx    = new JSlider(0, 100, 90);
    private final JSpinner spSens = new JSpinner(new SpinnerNumberModel(1.00, 0.10, 10.0, 0.10));
    private final JSpinner spFov  = new JSpinner(new SpinnerNumberModel(90, 60, 120, 1));
    private final JTextField tfHost = new JTextField(16);
    private final JSpinner spPort  = new JSpinner(new SpinnerNumberModel(7777, 1, 65535, 1));

    public SettingsFrame() {
        super("Settings");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setSize(520, 360);
        setLocationRelativeTo(null);

        buildUi();
        loadFromDiskOrDefaults();
    }

    // ===================== UI 구축 =====================

    private void buildUi() {
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(new EmptyBorder(12, 12, 12, 12));
        setContentPane(root);

        JPanel form = new JPanel(new GridBagLayout());
        root.add(form, BorderLayout.CENTER);

        GridBagConstraints c = baseGbc();

        // 섹션: 오디오
        addSection(form, c, "Audio");
        addRow(form, c, "Master", sMaster, "%", lbl -> lbl.setText(String.valueOf(sMaster.getValue())));
        addRow(form, c, "SFX",    sSfx,    "%", lbl -> lbl.setText(String.valueOf(sSfx.getValue())));

        // 섹션: 입력/영상
        addSection(form, c, "Controls / Video");
        ((JSpinner.DefaultEditor) spSens.getEditor()).getTextField().setColumns(6);
        addRow(form, c, "Mouse Sens", spSens, null, null);
        addRow(form, c, "FOV",        spFov,  "deg", null);

        // 섹션: 네트워크
        addSection(form, c, "Network (last server)");
        ((JSpinner.DefaultEditor) spPort.getEditor()).getTextField().setColumns(6);
        addRow(form, c, "Host", tfHost, null, null);
        addRow(form, c, "Port", spPort, null, null);

        // 하단 버튼
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton btnLoad = new JButton("Reload");
        JButton btnSave = new JButton("Save");
        JButton btnClose = new JButton("Close");
        buttons.add(new JLabel(store.getFile().toString()));
        buttons.add(Box.createHorizontalStrut(8));
        buttons.add(btnLoad);
        buttons.add(btnSave);
        buttons.add(btnClose);
        root.add(buttons, BorderLayout.SOUTH);

        // 동작
        btnLoad.addActionListener(e -> loadFromDiskOrDefaults());
        btnSave.addActionListener(e -> saveToDisk());
        btnClose.addActionListener(e -> dispose());
    }

    private GridBagConstraints baseGbc() {
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 6, 4, 6);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        c.gridy = 0;
        return c;
    }

    /** 섹션 제목 라벨을 한 줄 추가 */
    private void addSection(JPanel form, GridBagConstraints c, String title) {
        JLabel t = new JLabel(title);
        t.setFont(t.getFont().deriveFont(Font.BOLD));
        t.setForeground(new Color(60, 80, 120));
        c.gridx = 0; c.gridwidth = 3; c.gridy++;
        form.add(t, c);
        c.gridwidth = 1;
    }

    /**
     * 라벨/컴포넌트/(옵션)우측 보조라벨을 한 줄로 배치하는 헬퍼.
     * @param unitText 우측 단위 텍스트(예: "%", "deg"), null 허용
     * @param updater  보조라벨 갱신 콜백(슬라이더 값 등 실시간 반영), null 허용
     */
    private void addRow(JPanel form, GridBagConstraints c, String label, JComponent comp,
                        String unitText, java.util.function.Consumer<JLabel> updater) {
        JLabel l = new JLabel(label);
        c.gridy++; c.gridx = 0; c.weightx = 0; form.add(l, c);

        c.gridx = 1; c.weightx = 1.0; form.add(comp, c);

        JLabel unit = new JLabel(unitText == null ? "" : unitText);
        unit.setForeground(new Color(120, 120, 120));
        c.gridx = 2; c.weightx = 0; form.add(unit, c);

        if (comp instanceof JSlider slider && updater != null) {
            JLabel val = new JLabel();
            val.setForeground(new Color(120, 120, 120));
            c.gridx = 3; form.add(val, c);
            slider.addChangeListener(e -> updater.accept(val));
            updater.accept(val);
        }
    }

    // ===================== 로드/세이브 =====================

    /** 디스크에서 로드(실패 시 기본값 유지) 후 위젯에 반영 */
    private void loadFromDiskOrDefaults() {
        store.load();
        sMaster.setValue((int) Math.round(store.getMasterVolume() * 100));
        sSfx.setValue((int) Math.round(store.getSfxVolume() * 100));
        spSens.setValue(store.getMouseSensitivity());
        spFov.setValue(store.getFov());
        tfHost.setText(store.getLastHost());
        spPort.setValue(store.getLastPort());
    }

    /** 위젯 값을 모델로 옮긴 뒤 저장 */
    private void saveToDisk() {
        // 위젯 → 모델
        store.setMasterVolume(sMaster.getValue() / 100.0);
        store.setSfxVolume(sSfx.getValue() / 100.0);
        store.setMouseSensitivity(((Number) spSens.getValue()).doubleValue());
        store.setFov((Integer) spFov.getValue());
        store.setLastHost(tfHost.getText().trim().isEmpty() ? "127.0.0.1" : tfHost.getText().trim());
        store.setLastPort((Integer) spPort.getValue());

        try {
            store.save();
            Toast.success(this, "Saved");
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                    "저장 실패: " + ex.getMessage(),
                    "Save Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ===================== 단독 실행 =====================

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}
        SwingUtilities.invokeLater(() -> new SettingsFrame().setVisible(true));
    }
}
