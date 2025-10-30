package com.fpsgame.client.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * ReadyButton
 * ------------------------------------------------------------
 * 로비에서 사용할 READY 토글 전용 버튼.
 *
 * 특징
 * - 토글 상태를 내부에서 관리(isSelected)
 * - 시각적 라벨/색 자동 변경
 * - 외부에 상태 변경 리스너를 제공(addReadyChangeListener)
 *
 * 사용 예시:
 *   ReadyButton btnReady = new ReadyButton();
 *   btnReady.addReadyChangeListener(ready -> {
 *       // 서버 전송 등
 *       // ReadyToggleSender.send(netClient, ready);
 *   });
 *
 *   // 기존 JButton을 쓰고 싶다면 이 클래스 대신 JButton을 사용해도 되지만,
 *   // 본 클래스를 쓰면 라벨/색/토글 처리가 자동화되어 편리합니다.
 */
public final class ReadyButton extends JButton {

    /** 상태 변경 콜백 */
    public interface ReadyChangeListener {
        void onReadyChanged(boolean ready);
    }

    private boolean selected;
    private ReadyChangeListener listener;

    public ReadyButton() {
        super("READY");
        setFocusPainted(false);
        setFont(getFont().deriveFont(Font.BOLD, 13f));
        setBackground(new Color(40, 60, 90));
        setForeground(Color.WHITE);

        addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                setReady(!isReady()); // 토글
            }
        });

        // 초기 상태 반영
        refreshUi();
    }

    /** 현재 READY 여부 */
    public boolean isReady() {
        return selected;
    }

    /** READY 상태를 설정(시각/콜백 포함) */
    public void setReady(boolean ready) {
        if (this.selected == ready) return;
        this.selected = ready;
        refreshUi();
        fireReadyChanged();
    }

    /** 콜백 등록(한 개만 유지; 필요 시 래핑해서 멀티지원 가능) */
    public void addReadyChangeListener(ReadyChangeListener l) {
        this.listener = l;
    }

    public void removeReadyChangeListener() {
        this.listener = null;
    }

    private void fireReadyChanged() {
        if (listener != null) {
            try {
                listener.onReadyChanged(this.selected);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }

    private void refreshUi() {
        setText(selected ? "READY ✅" : "READY");
        setBackground(selected ? new Color(26, 128, 61) : new Color(40, 60, 90));
        setToolTipText(selected ? "준비 취소" : "준비하기");
    }

    // --- 간단 데모 -----------------------------------------------------------
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("ReadyButton Demo");
            f.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            f.setSize(320, 160);
            f.setLocationRelativeTo(null);

            ReadyButton btn = new ReadyButton();
            btn.addReadyChangeListener(ready -> System.out.println("READY = " + ready));

            JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 30));
            p.setBackground(new Color(30, 40, 55));
            p.add(btn);

            f.setContentPane(p);
            f.setVisible(true);
        });
    }
}
