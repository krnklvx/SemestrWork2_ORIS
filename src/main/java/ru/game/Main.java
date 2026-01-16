package ru.game;

import ru.game.client.ConnectionWindow;
import ru.game.client.GameWindow;
import javax.swing.*;

/**
 * Главный класс
 */
public class Main {
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        SwingUtilities.invokeLater(() -> {
            ConnectionWindow connectionWindow = new ConnectionWindow(client -> {
                GameWindow gameWindow = new GameWindow(client);
                gameWindow.setVisible(true);
            });
            connectionWindow.setVisible(true);
        });
    }
}
