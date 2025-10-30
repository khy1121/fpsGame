package com.fpsgame.client.ui;

import com.fpsgame.common.GameEnums;
import java.awt.*;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/**
 * 로비 화면 패널(네트워크 비의존).
 * <p>
 * - 닉네임 입력, 팀 선택(RED/BLUE), 캐릭터 선택, Ready 토글을 제공
 * - 상위(Frame/Controller)가 콜백을 주입하여 변경 사항을 수신
 * - 실제 네트워크 전송/동기화는 상위에서 처리
 */
public final class LobbyPanel extends JPanel {

    /* --------------------- 콜백 --------------------- */

    /** 닉네임 변경 콜백 */
    private volatile Consumer<String> onNameChanged;
    /** 캐릭터/팀 선택 콜백 */
    private volatile BiConsumer<GameEnums.CharacterId, GameEnums.Team> onSelection;
    /** Ready 변경 콜백 */
    private volatile Consumer<Boolean> onReadyChanged;

    /* --------------------- UI 구성요소 --------------------- */

    private final JTextField nameField = new JTextField("Player", 14);

    private final JRadioButton redBtn = new JRadioButton("Red");
    private final JRadioButton blueBtn = new JRadioButton("Blue");
    private final ButtonGroup teamGroup = new ButtonGroup();

    private final CharacterSelectPanel charPanel = new CharacterSelectPanel();

    private final JToggleButton readyToggle = new JToggleButton("Ready");
    private final JLabel status = new JLabel("닉/팀/캐릭터를 선택하세요.");

    public LobbyPanel() {
        super(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(12, 12, 12, 12));

        // 상단: 제목
        JLabel title = new JLabel("로비");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));
        add(title, BorderLayout.NORTH);

        // 좌: 프로필(닉네임/팀/Ready)
        JPanel left = buildProfilePanel();
        add(left, BorderLayout.WEST);

        // 우: 캐릭터 선택
        add(charPanel, BorderLayout.CENTER);

        // 하단: 상태 라벨
        status.setBorder(new EmptyBorder(4, 2, 0, 2));
        add(status, BorderLayout.SOUTH);

        // 이벤트 연결
        wireEvents();
    }

    /* --------------------- 외부 API(콜백 주입) --------------------- */

    /** 닉네임 변경 콜백 등록(널 허용) */
    public void setOnNameChanged(Consumer<String> cb) { this.onNameChanged = cb; }

    /** 캐릭터/팀 선택 콜백 등록(널 허용) */
    public void setOnSelection(BiConsumer<GameEnums.CharacterId, GameEnums.Team> cb) { this.onSelection = cb; }

    /** Ready 변경 콜백 등록(널 허용) */
    public void setOnReadyChanged(Consumer<Boolean> cb) { this.onReadyChanged = cb; }

    /** 현재 선택된 팀 반환 */
    public GameEnums.Team getSelectedTeam() {
        return redBtn.isSelected() ? GameEnums.Team.RED : GameEnums.Team.BLUE;
    }

    /** 현재 선택된 캐릭터 반환 */
    public GameEnums.CharacterId getSelectedCharacter() {
        return charPanel.getSelected();
    }

    /** Ready 상태 반환 */
    public boolean isReady() {
        return readyToggle.isSelected();
    }

    /** 외부에서 Ready 강제 설정(콜백 호출 없음) */
    public void setReady(boolean ready) {
        readyToggle.setSelected(ready);
        updateReadyStyle();
    }

    /* --------------------- 내부 UI 빌드 --------------------- */

    private JPanel buildProfilePanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setPreferredSize(new Dimension(260, 0));
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(6, 6, 6, 6);
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.gridx = 0;
        gc.gridy = 0;
        gc.weightx = 1;

        // 닉네임
        p.add(new JLabel("닉네임"), gc);
        gc.gridy++;
        p.add(nameField, gc);

        // 팀 선택
        gc.gridy++;
        p.add(new JLabel("팀 선택"), gc);
        JPanel teams = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        teamGroup.add(redBtn);
        teamGroup.add(blueBtn);
        redBtn.setSelected(true);
        teams.add(redBtn);
        teams.add(blueBtn);
        gc.gridy++;
        p.add(teams, gc);

        // Ready 토글
        gc.gridy++;
        readyToggle.setFocusPainted(false);
        updateReadyStyle();
        p.add(readyToggle, gc);

        // 여백 채우기
        gc.gridy++;
        gc.weighty = 1;
        gc.fill = GridBagConstraints.BOTH;
        p.add(Box.createGlue(), gc);

        return p;
    }

    /* --------------------- 이벤트 배선 --------------------- */

    private void wireEvents() {
        // 닉네임 변경
        nameField.getDocument().addDocumentListener(new SimpleDocListener(() -> {
            String n = nameField.getText().trim();
            setStatus("닉네임: " + n);
            Consumer<String> cb = onNameChanged;
            if (cb != null) cb.accept(n);
        }));

        // 팀 선택
        Action teamChanged = new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                fireSelectionChanged();
            }
        };
        redBtn.setActionCommand("RED");
        blueBtn.setActionCommand("BLUE");
        redBtn.addActionListener(teamChanged);
        blueBtn.addActionListener(teamChanged);

        // 캐릭터 선택
        // ✅ enum 기반 콜백으로 연결 (displayName() 접근 가능)
        charPanel.setOnSelectEnum((Consumer<GameEnums.CharacterId>) cid -> {
            setStatus("캐릭터: " + (cid == null ? "?" : safeDisplay(cid)));
            fireSelectionChanged();
        });

        // Ready 토글
        readyToggle.addActionListener(e -> {
            updateReadyStyle();
            Consumer<Boolean> cb = onReadyChanged;
            if (cb != null) cb.accept(readyToggle.isSelected());
        });
    }

    /** 선택 상태(캐릭터/팀) 변경 알림 */
    private void fireSelectionChanged() {
        BiConsumer<GameEnums.CharacterId, GameEnums.Team> cb = onSelection;
        if (cb != null) cb.accept(getSelectedCharacter(), getSelectedTeam());
    }

    /** Ready 버튼 스타일 업데이트 */
    private void updateReadyStyle() {
        boolean r = readyToggle.isSelected();
        readyToggle.setText(r ? "Ready ✔" : "Ready");
        Color base = UIManager.getColor("Button.background");
        readyToggle.setBackground(r ? new Color(0xC8E6C9) : (base != null ? base : new JButton().getBackground()));
    }

    private void setStatus(String s) {
        status.setText(Objects.requireNonNullElse(s, ""));
    }

    /** enum 안전 표시: displayName() 있으면 사용, 없으면 name() */
    private static String safeDisplay(GameEnums.CharacterId cid) {
        try { return cid.displayName(); } catch (Throwable ignore) { return cid.name(); }
    }

    /* --------------------- 내부 유틸: 간단 DocListener --------------------- */

    private static final class SimpleDocListener implements javax.swing.event.DocumentListener {
        private final Runnable onChange;
        SimpleDocListener(Runnable r) { this.onChange = r; }
        @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { onChange.run(); }
        @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { onChange.run(); }
        @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { onChange.run(); }
    }
}
