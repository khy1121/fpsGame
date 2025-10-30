package com.fpsgame.client.ui;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/** 간단 캐릭터 선택 다이얼로그: B 키로 호출하도록 사용 가능. */
public final class CharacterSelectDialog {
    private CharacterSelectDialog() {}

    public static void show(Component owner) {
        Window w = owner == null ? null : SwingUtilities.getWindowAncestor(owner);
        JDialog d = new JDialog(w, "Select Character", Dialog.ModalityType.MODELESS);
        CharacterSelectPanel panel = new CharacterSelectPanel();
        panel.withDefaultCharacters();
        panel.onSendSelection(id -> {
            d.dispose();
            // 선택 전달 경로는 추후 NetClient와 연계 가능
        });
        d.setContentPane(panel);
        d.setSize(560, 400);
        d.setLocationRelativeTo(w);
        d.setVisible(true);
    }
}

