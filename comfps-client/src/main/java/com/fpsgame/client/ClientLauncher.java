package com.fpsgame.client;

import com.fpsgame.client.model.Settings;
import com.fpsgame.client.ui.ChatWindow;
import com.fpsgame.client.ui.GameFrame;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Path;

/**
 * Simple launcher/entry for the FPS client.
 * <p>
 * Responsibilities:
 * - Boot on the Swing EDT
 * - Load/save persisted {@link Settings}
 * - Provide a tiny start menu to open Chat (network test) or GameFrame (UI shell)
 * - Centralize graceful shutdown
 *
 * This launcher does not start the actual game loop yet; it is a safe
 * place to wire controllers as the project grows.
 */
public final class ClientLauncher {

    private final Settings settings = Settings.defaults();
    private final Path settingsFile = Path.of(System.getProperty("user.home"), ".fpsgame", "client.properties");

    private JFrame menuFrame;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ClientLauncher app = new ClientLauncher();
            app.init();
        });
    }

    private void init() {
        // Load settings (best-effort)
        try {
            settings.load(settingsFile);
        } catch (Exception ex) {
            System.err.println("[warn] Failed to load settings: " + ex.getMessage());
        }

        // Build minimal start menu
        menuFrame = new JFrame("FPS Client Launcher");
        menuFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        menuFrame.addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { shutdown(); }
        });

        JPanel root = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(8, 8, 8, 8);
        gc.gridx = 0; gc.gridy = 0; gc.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel("Multiplayer FPS (Swing + TCP)");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));
        root.add(title, gc);

        gc.gridy++;
        JButton chatBtn = new JButton("Open Chat Client");
        chatBtn.addActionListener(e -> openChatWindow());
        root.add(chatBtn, gc);

        gc.gridy++;
        JButton gameBtn = new JButton("Open Game Shell");
        gameBtn.addActionListener(e -> openGameFrame());
        root.add(gameBtn, gc);

        gc.gridy++;
        JButton exitBtn = new JButton("Exit");
        exitBtn.addActionListener(e -> shutdown());
        root.add(exitBtn, gc);

        menuFrame.setContentPane(root);
        menuFrame.pack();
        menuFrame.setSize(420, 240);
        menuFrame.setLocationRelativeTo(null);
        menuFrame.setVisible(true);
    }

    private void openChatWindow() {
        ChatWindow w = new ChatWindow();
        w.setVisible(true);
    }

    private void openGameFrame() {
        GameFrame f = new GameFrame();
        f.setVisible(true);
    }

    private void shutdown() {
        // Save settings on exit (best-effort)
        try {
            settings.save(settingsFile, "FPS Client Settings");
        } catch (Exception ex) {
            System.err.println("[warn] Failed to save settings: " + ex.getMessage());
        }
        if (menuFrame != null) {
            menuFrame.dispose();
        }
        System.exit(0);
    }
}
