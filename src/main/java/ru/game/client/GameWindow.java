package ru.game.client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

/**
 * Главное окно игры с рисованием через Graphics
 */
public class GameWindow extends JFrame {
    private GameClient client;
    private boolean isDrawer;
    
    // Компоненты
    private DrawingCanvas canvas;
    private JLabel wordLabel;
    private JTextField guessField;
    private JButton guessButton;
    private JTextArea chatArea;
    private JButton clearButton;
    private JLabel scoreLabel;
    private JLabel roleLabel;
    private JLabel statusLabel;
    
    // Для рисования
    private int lastX, lastY;
    private Color currentColor;
    
    public GameWindow(GameClient client) {
        this.client = client;
        this.client.setGameWindow(this);
        this.currentColor = Color.BLACK;
        this.isDrawer = false;
        
        initializeUI();
        setupDrawing();
    }
    
    private void initializeUI() {
        setTitle("🎨 Рисуй и Угадывай");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null);
        
        JPanel mainPanel = new JPanel(new BorderLayout(5, 5));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Левая панель - информация и управление
        JPanel leftPanel = new JPanel(new BorderLayout(5, 5));
        leftPanel.setPreferredSize(new Dimension(250, 0));
        
        // Информация о роли и слове
        JPanel infoPanel = new JPanel(new GridLayout(0, 1, 5, 5));
        roleLabel = new JLabel("");
        roleLabel.setFont(new Font("Arial", Font.BOLD, 14));
        infoPanel.add(roleLabel);
        
        statusLabel = new JLabel("");
        statusLabel.setFont(new Font("Arial", Font.ITALIC, 12));
        statusLabel.setForeground(Color.GRAY);
        infoPanel.add(statusLabel);
        
        wordLabel = new JLabel("");
        wordLabel.setFont(new Font("Arial", Font.BOLD, 16));
        wordLabel.setForeground(new Color(0, 100, 0));
        wordLabel.setHorizontalAlignment(JLabel.CENTER);
        wordLabel.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
        infoPanel.add(wordLabel);
        
        scoreLabel = new JLabel("Счет: 0 - 0");
        scoreLabel.setFont(new Font("Arial", Font.BOLD, 12));
        infoPanel.add(scoreLabel);
        
        leftPanel.add(infoPanel, BorderLayout.NORTH);
        
        // Объединенное поле для угадывания и чата
        JPanel inputPanel = new JPanel(new BorderLayout(5, 5));
        inputPanel.setBorder(BorderFactory.createTitledBorder("Угадайте слово / Чат"));
        
        // Область сообщений (чат + системные сообщения)
        chatArea = new JTextArea(12, 20);
        chatArea.setEditable(false);
        chatArea.setFont(new Font("Arial", Font.PLAIN, 12));
        JScrollPane chatScroll = new JScrollPane(chatArea);
        
        // Одно поле ввода - работает и для угадывания, и для чата
        guessField = new JTextField();
        guessField.setToolTipText("Введите слово для угадывания или сообщение для чата");
        guessButton = new JButton("Отправить");
        guessButton.addActionListener(e -> {
            String text = guessField.getText().trim();
            if (!text.isEmpty()) {
                if (isDrawer) {
                    // Если рисуем - отправляем как сообщение в чат
                    client.sendChat(text);
                } else {
                    // Если угадываем - отправляем как догадку
                    client.sendGuess(text);
                }
                guessField.setText("");
            }
        });
        guessField.addActionListener(e -> guessButton.doClick());
        
        inputPanel.add(chatScroll, BorderLayout.CENTER);
        JPanel inputRow = new JPanel(new BorderLayout(5, 5));
        inputRow.add(guessField, BorderLayout.CENTER);
        inputRow.add(guessButton, BorderLayout.EAST);
        inputPanel.add(inputRow, BorderLayout.SOUTH);
        
        leftPanel.add(inputPanel, BorderLayout.CENTER);
        
        // Центральная панель - холст для рисования
        JPanel centerPanel = new JPanel(new BorderLayout(5, 5));
        centerPanel.setBorder(BorderFactory.createTitledBorder("Холст"));
        
        canvas = new DrawingCanvas();
        canvas.setPreferredSize(new Dimension(600, 500));
        canvas.setBackground(Color.WHITE);
        
        // Панель инструментов
        JPanel toolPanel = new JPanel(new FlowLayout());
        clearButton = new JButton("Очистить");
        clearButton.addActionListener(e -> {
            canvas.clear();
            client.sendClear();
        });
        
        // Выбор цвета: 7 цветов радуги + белый и черный
        toolPanel.add(clearButton);
        toolPanel.add(new JLabel("Цвет:"));
        
        // Черный
        JButton blackBtn = new JButton("⚫");
        blackBtn.setBackground(Color.BLACK);
        blackBtn.setForeground(Color.WHITE);
        blackBtn.addActionListener(e -> currentColor = Color.BLACK);
        toolPanel.add(blackBtn);
        
        // Красный (радуга)
        JButton redBtn = new JButton("🔴");
        redBtn.setBackground(Color.RED);
        redBtn.addActionListener(e -> currentColor = Color.RED);
        toolPanel.add(redBtn);
        
        // Оранжевый (радуга)
        JButton orangeBtn = new JButton("🟠");
        orangeBtn.setBackground(new Color(255, 165, 0));
        orangeBtn.addActionListener(e -> currentColor = new Color(255, 165, 0));
        toolPanel.add(orangeBtn);
        
        // Желтый (радуга)
        JButton yellowBtn = new JButton("🟡");
        yellowBtn.setBackground(Color.YELLOW);
        yellowBtn.addActionListener(e -> currentColor = Color.YELLOW);
        toolPanel.add(yellowBtn);
        
        // Зеленый (радуга)
        JButton greenBtn = new JButton("🟢");
        greenBtn.setBackground(Color.GREEN);
        greenBtn.addActionListener(e -> currentColor = Color.GREEN);
        toolPanel.add(greenBtn);
        
        // Голубой/Синий (радуга)
        JButton blueBtn = new JButton("🔵");
        blueBtn.setBackground(Color.BLUE);
        blueBtn.addActionListener(e -> currentColor = Color.BLUE);
        toolPanel.add(blueBtn);
        
        // Фиолетовый (радуга)
        JButton purpleBtn = new JButton("🟣");
        purpleBtn.setBackground(new Color(128, 0, 128));
        purpleBtn.addActionListener(e -> currentColor = new Color(128, 0, 128));
        toolPanel.add(purpleBtn);
        
        // Белый
        JButton whiteBtn = new JButton("⚪");
        whiteBtn.setBackground(Color.WHITE);
        whiteBtn.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        whiteBtn.addActionListener(e -> currentColor = Color.WHITE);
        toolPanel.add(whiteBtn);
        
        centerPanel.add(canvas, BorderLayout.CENTER);
        centerPanel.add(toolPanel, BorderLayout.SOUTH);
        
        mainPanel.add(leftPanel, BorderLayout.WEST);
        mainPanel.add(centerPanel, BorderLayout.CENTER);
        
        add(mainPanel);
    }
    
    private void setupDrawing() {
        canvas.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (isDrawer) {
                    // Масштабируем координаты относительно реального размера изображения
                    int x = scaleX(e.getX());
                    int y = scaleY(e.getY());
                    lastX = x;
                    lastY = y;
                }
            }
        });
        
        canvas.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (isDrawer) {
                    // Масштабируем координаты
                    int x = scaleX(e.getX());
                    int y = scaleY(e.getY());
                    
                    // Рисуем локально
                    canvas.drawLine(lastX, lastY, x, y, currentColor);
                    
                    // Отправляем на сервер (масштабированные координаты)
                    String colorStr = colorToString(currentColor);
                    client.sendDraw(lastX, lastY, x, y, colorStr);
                    
                    lastX = x;
                    lastY = y;
                }
            }
        });
    }
    
    // Масштабирование координат X с учетом размера панели
    private int scaleX(int panelX) {
        if (canvas.getWidth() == 0) return panelX;
        return (int) ((double) panelX / canvas.getWidth() * 600);
    }
    
    // Масштабирование координат Y с учетом размера панели
    private int scaleY(int panelY) {
        if (canvas.getHeight() == 0) return panelY;
        return (int) ((double) panelY / canvas.getHeight() * 500);
    }
    
    private String colorToString(Color color) {
        if (color.equals(Color.BLACK)) return "BLACK";
        if (color.equals(Color.RED)) return "RED";
        if (color.equals(new Color(255, 165, 0))) return "ORANGE";
        if (color.equals(Color.YELLOW)) return "YELLOW";
        if (color.equals(Color.GREEN)) return "GREEN";
        if (color.equals(Color.BLUE)) return "BLUE";
        if (color.equals(new Color(128, 0, 128))) return "PURPLE";
        if (color.equals(Color.WHITE)) return "WHITE";
        return "BLACK";
    }
    
    private Color stringToColor(String colorStr) {
        switch (colorStr) {
            case "RED": return Color.RED;
            case "ORANGE": return new Color(255, 165, 0);
            case "YELLOW": return Color.YELLOW;
            case "GREEN": return Color.GREEN;
            case "BLUE": return Color.BLUE;
            case "PURPLE": return new Color(128, 0, 128);
            case "WHITE": return Color.WHITE;
            default: return Color.BLACK;
        }
    }
    
    
    // Методы, вызываемые из GameClient
    public void setRole(boolean isDrawer) {
        this.isDrawer = isDrawer;
        System.out.println("Установка роли: isDrawer = " + isDrawer);
        SwingUtilities.invokeLater(() -> {
            if (isDrawer) {
                roleLabel.setText("🎨 Вы рисуете");
                roleLabel.setForeground(new Color(0, 100, 200));
                guessField.setToolTipText("Введите сообщение в чат");
                guessField.setEnabled(true);
                guessButton.setEnabled(true);
            } else {
                roleLabel.setText("🔍 Вы угадываете");
                roleLabel.setForeground(new Color(200, 0, 0));
                guessField.setToolTipText("Введите слово для угадывания");
                guessField.setEnabled(true);
                guessButton.setEnabled(true);
            }
            // Очищаем статус при смене роли
            statusLabel.setText("");
            // Принудительно обновляем отображение
            roleLabel.revalidate();
            roleLabel.repaint();
        });
    }
    
    public void setWord(String word) {
        SwingUtilities.invokeLater(() -> {
            if (word == null || word.isEmpty()) {
                // Очищаем слово
                wordLabel.setText("");
                wordLabel.setVisible(false);
            } else if (isDrawer) {
                // Рисующему показываем слово
                wordLabel.setText("🎯 СЛОВО: " + word.toUpperCase());
                wordLabel.setForeground(new Color(0, 150, 0));
                wordLabel.setVisible(true);
                wordLabel.setFont(new Font("Arial", Font.BOLD, 18));
            } else {
                // Угадывающему не показываем слово
                wordLabel.setText("❓ Угадайте слово!");
                wordLabel.setForeground(new Color(200, 0, 0));
                wordLabel.setVisible(true);
                wordLabel.setFont(new Font("Arial", Font.BOLD, 16));
            }
        });
    }
    
    public void drawLine(int x1, int y1, int x2, int y2, String color) {
        SwingUtilities.invokeLater(() -> {
            canvas.drawLine(x1, y1, x2, y2, stringToColor(color));
        });
    }
    
    public void clearCanvas() {
        SwingUtilities.invokeLater(() -> {
            canvas.clear();
        });
    }
    
    public void updateScore(String scoreData) {
        SwingUtilities.invokeLater(() -> {
            // Формат: игрок1=10,игрок2=5
            String formatted = scoreData.replace(",", " - ").replace("=", ": ");
            scoreLabel.setText("Счет: " + formatted);
        });
    }
    
    public void addChatMessage(String nickname, String message) {
        SwingUtilities.invokeLater(() -> {
            // Упрощенный формат: никнейм: сообщение
            // Если никнейм уже содержит роль в скобках, просто добавляем сообщение
            String formattedMessage;
            if (nickname.equals("СИСТЕМА")) {
                formattedMessage = "💬 " + nickname + ": " + message;
            } else {
                formattedMessage = nickname + ": " + message;
            }
            chatArea.append(formattedMessage + "\n");
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
        });
    }
    
    public void onGameStart(String message) {
        SwingUtilities.invokeLater(() -> {
            // Обновляем статус
            statusLabel.setText(message);
            
            // Если это сообщение об ожидании, показываем его в статусе
            if (message.contains("Ожидание")) {
                statusLabel.setText(message);
                statusLabel.setForeground(Color.GRAY);
            } else if (!isDrawer) {
                statusLabel.setText(message);
                statusLabel.setForeground(Color.GRAY);
            } else {
                statusLabel.setText("Начните рисовать!");
                statusLabel.setForeground(Color.GRAY);
            }
            
            // Холст очищается автоматически при получении CLEAR
            // Убеждаемся, что роль отображается
            if (roleLabel.getText().isEmpty()) {
                // Если роль еще не установлена, устанавливаем временную
                if (isDrawer) {
                    roleLabel.setText("🎨 Вы рисуете");
                } else {
                    roleLabel.setText("🔍 Ожидание");
                }
            }
        });
    }
    
    public void onCorrectGuess(String winner) {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText("Правильно! " + winner + " угадал(а)! Новый раунд...");
            // Холст будет очищен при получении CLEAR от сервера
        });
    }
    
    public void showError(String error) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(this, error, "Ошибка", JOptionPane.ERROR_MESSAGE);
        });
    }
    
    public void onDisconnect() {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(this, "Соединение потеряно", "Ошибка", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        });
    }
    
    /**
     * Холст для рисования через Graphics
     */
    private class DrawingCanvas extends JPanel {
        private BufferedImage image;
        private Graphics2D g2d;
        
        public DrawingCanvas() {
            image = new BufferedImage(600, 500, BufferedImage.TYPE_INT_RGB);
            g2d = image.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, 0, 600, 500);
            g2d.setColor(Color.BLACK);
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (image != null) {
                // Рисуем изображение с масштабированием
                g.drawImage(image, 0, 0, getWidth(), getHeight(), null);
            }
        }
        
        public void drawLine(int x1, int y1, int x2, int y2, Color color) {
            // Координаты уже масштабированы (0-600 для X, 0-500 для Y)
            g2d.setColor(color);
            g2d.setStroke(new BasicStroke(3));
            g2d.drawLine(x1, y1, x2, y2);
            repaint();
        }
        
        public void clear() {
            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, 0, image.getWidth(), image.getHeight());
            g2d.setColor(Color.BLACK);
            repaint();
        }
    }
}
