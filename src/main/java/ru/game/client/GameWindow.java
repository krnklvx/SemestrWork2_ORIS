package ru.game.client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

/**
 * главное окно игры через Graphics
 */
public class GameWindow extends JFrame {
    // клиент для сетевого взаимодействия с сервером
    private GameClient client;

    // Флаг определяющий роль игрока
    // true = художник, false = угадывающий
    private boolean isDrawer;

    //ИНТЕРФЕЙСА
    private DrawingCanvas canvas;     // холст для рисования
    private JLabel wordLabel;         // метка для отображения слова только для художника
    private JTextField guessField;    // поле для ввода сообщения в чат
    private JButton guessButton;      // кнопка отправить
    private JTextArea chatArea;       // для отображения чата
    private JButton clearButton;      // кнопка очистить холст
    private JLabel scoreLabel;        // счет игрока
    private JLabel roleLabel;         // текущая роль игрока
    private JLabel statusLabel;       // статус игры

    // ПЕРЕМЕННЫЕ ДЛЯ РИСОВАНИЯ
    private int lastX, lastY; // последние координаты мыши чтобы рисовать линии
    private Color currentColor; // текущий цвет для рисования

    /**
     * Конструктор основного игрового окна
     */

    public GameWindow(GameClient client) {
        this.client = client; // ссылка на клиент
        this.client.setGameWindow(this);
        this.currentColor = Color.BLACK;  // по умолчанию черный цвет
        this.isDrawer = false;

        initializeUI(); // создаем и настраиваем все элементы интерфейса
        setupDrawing(); // Настраиваем обработчики мыши для рисования
    }

    /**
     * Создание и настройка графического интерфейса
     */
    private void initializeUI() {
        // настройка основого окна
        setTitle("🎨 Рисуй и Угадывай"); // Заголовок окна
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);  // При закрытии окна завершить программу
        setSize(1000, 700);// Размер окна
        setLocationRelativeTo(null); // Центрировать

        // создаем главную панель
        JPanel mainPanel = new JPanel(new BorderLayout(5, 5));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // Отступы от краев

        // ЛЕВАЯ ПАНЕЛЬ
        JPanel leftPanel = new JPanel(new BorderLayout(5, 5));
        leftPanel.setPreferredSize(new Dimension(250, 0));  // Фиксированная ширина

        // Панель с информацией о роли и слове
        JPanel infoPanel = new JPanel(new GridLayout(0, 1, 5, 5)); // Вертикальное расположение

        roleLabel = new JLabel("");  // Пока пустая будет заполнена сервером
        roleLabel.setFont(new Font("Arial", Font.BOLD, 14));
        infoPanel.add(roleLabel);

        statusLabel = new JLabel("");  // Статус игры
        statusLabel.setFont(new Font("Arial", Font.ITALIC, 12));
        statusLabel.setForeground(Color.GRAY);
        infoPanel.add(statusLabel);

        wordLabel = new JLabel("");  // слово для художника
        wordLabel.setFont(new Font("Arial", Font.BOLD, 16));
        wordLabel.setForeground(new Color(0, 100, 0));  // Темно-зеленый
        wordLabel.setHorizontalAlignment(JLabel.CENTER); // Центрируем текст
        wordLabel.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2)); // Рамка вокруг
        infoPanel.add(wordLabel);

        scoreLabel = new JLabel("Счет: 0 - 0");  // Начальный счет
        scoreLabel.setFont(new Font("Arial", Font.BOLD, 12));
        infoPanel.add(scoreLabel);

        leftPanel.add(infoPanel, BorderLayout.NORTH);  // Инфопанель в верхней части левой панели

        // ПАНЕЛЬ ДЛЯ УГАДЫВАНИЯ И ЧАТА
        JPanel inputPanel = new JPanel(new BorderLayout(5, 5));
        inputPanel.setBorder(BorderFactory.createTitledBorder("Угадайте слово / Чат"));

        // Область чата
        chatArea = new JTextArea(12, 20);  // 12 строк 20 столбцов
        chatArea.setEditable(false); // Пользователь не может редактировать чат
        chatArea.setFont(new Font("Arial", Font.PLAIN, 12));
        JScrollPane chatScroll = new JScrollPane(chatArea); //скроллбар

        // Поле ввода и для угадывания и для чата
        guessField = new JTextField();
        guessField.setToolTipText("Введите слово для угадывания или сообщение для чата");

        guessButton = new JButton("Отправить");
        // Обработчик нажатия кнопки Отправить
        guessButton.addActionListener(e -> {
            String text = guessField.getText().trim();  // Получаем текст убираем пробелы
            if (!text.isEmpty()) {  // Если что-то введено
                if (isDrawer) {
                    // Если мы художник отправляем как сообщение в чат
                    client.sendChat(text);
                } else {
                    // Если мы угадывающий отправляем как догадку
                    client.sendGuess(text);
                }
                guessField.setText("");  // Очищаем поле ввода после отправки
            }
        });

        // Нажатие Enter в поле ввода нажатие кнопки Отправить
        guessField.addActionListener(e -> guessButton.doClick());

        // компоненты на панель ввода
        inputPanel.add(chatScroll, BorderLayout.CENTER);
        JPanel inputRow = new JPanel(new BorderLayout(5, 5));
        inputRow.add(guessField, BorderLayout.CENTER);
        inputRow.add(guessButton, BorderLayout.EAST);
        inputPanel.add(inputRow, BorderLayout.SOUTH);

        leftPanel.add(inputPanel, BorderLayout.CENTER);  // Панель ввода в центре левой панели

        // холст для рисования
        JPanel centerPanel = new JPanel(new BorderLayout(5, 5));
        centerPanel.setBorder(BorderFactory.createTitledBorder("Холст"));

        canvas = new DrawingCanvas();  // Создаем холст
        canvas.setPreferredSize(new Dimension(600, 500));
        canvas.setBackground(Color.WHITE);

        // кнопки и выбор цвета ===
        JPanel toolPanel = new JPanel(new FlowLayout());

        clearButton = new JButton("Очистить");
        clearButton.addActionListener(e -> {
            canvas.clear();      // Очищаем холст локально
            client.sendClear();  // Отправляем команду очистки на сервер
        });

        // Добавляем кнопку очистки
        toolPanel.add(clearButton);
        toolPanel.add(new JLabel("Цвет:"));

        //СОЗДАЕМ КНОПКИ ВЫБОРА ЦВЕТА


        // Черный
        JButton blackBtn = new JButton("⚫");
        blackBtn.setBackground(Color.BLACK);
        blackBtn.setForeground(Color.WHITE);
        blackBtn.addActionListener(e -> currentColor = Color.BLACK);
        toolPanel.add(blackBtn);

        // Красный
        JButton redBtn = new JButton("🔴");
        redBtn.setBackground(Color.RED);
        redBtn.addActionListener(e -> currentColor = Color.RED);
        toolPanel.add(redBtn);

        // Оранжевый
        JButton orangeBtn = new JButton("🟠");
        orangeBtn.setBackground(new Color(255, 165, 0));
        orangeBtn.addActionListener(e -> currentColor = new Color(255, 165, 0));
        toolPanel.add(orangeBtn);

        // Желтый
        JButton yellowBtn = new JButton("🟡");
        yellowBtn.setBackground(Color.YELLOW);
        yellowBtn.addActionListener(e -> currentColor = Color.YELLOW);
        toolPanel.add(yellowBtn);

        // Зеленый
        JButton greenBtn = new JButton("🟢");
        greenBtn.setBackground(Color.GREEN);
        greenBtn.addActionListener(e -> currentColor = Color.GREEN);
        toolPanel.add(greenBtn);

        // Синий
        JButton blueBtn = new JButton("🔵");
        blueBtn.setBackground(Color.BLUE);
        blueBtn.addActionListener(e -> currentColor = Color.BLUE);
        toolPanel.add(blueBtn);

        // Фиолетовый
        JButton purpleBtn = new JButton("🟣");
        purpleBtn.setBackground(new Color(128, 0, 128));
        purpleBtn.addActionListener(e -> currentColor = new Color(128, 0, 128));
        toolPanel.add(purpleBtn);

        // Белый
        JButton whiteBtn = new JButton("⚪");
        whiteBtn.setBackground(Color.WHITE);
        whiteBtn.setBorder(BorderFactory.createLineBorder(Color.GRAY)); // рамка для видимости на белом фоне
        whiteBtn.addActionListener(e -> currentColor = Color.WHITE);
        toolPanel.add(whiteBtn);

        // Собираем центральную панель
        centerPanel.add(canvas, BorderLayout.CENTER);
        centerPanel.add(toolPanel, BorderLayout.SOUTH);

        // Собираем главную панель
        mainPanel.add(leftPanel, BorderLayout.WEST);     // Левая панель слева
        mainPanel.add(centerPanel, BorderLayout.CENTER); // Холст в центре

        // Добавляем главную панель в окно
        add(mainPanel);
    }

    /**
     * настройка обработчиков мыши для рисования
     */
    private void setupDrawing() {
        // обработчик нажатия кнопки мыши
        canvas.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (isDrawer) {  // Только художник может рисовать
                    // Масштабируем координаты с размера панели на размер изображения
                    int x = scaleX(e.getX());
                    int y = scaleY(e.getY());
                    lastX = x;  // Запоминаем начальную точку
                    lastY = y;
                }
            }
        });

        // Обработчик перетаскивания мыши
        canvas.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (isDrawer) {  // Только художник может рисовать
                    // Масштабируем текущие координаты
                    int x = scaleX(e.getX());
                    int y = scaleY(e.getY());

                    // 1. Рисуем локально на своем холсте
                    canvas.drawLine(lastX, lastY, x, y, currentColor);

                    // 2. Отправляем на сервер чтобы другие игроки увидели
                    String colorStr = colorToString(currentColor);  // Преобразуем цвет в строку
                    client.sendDraw(lastX, lastY, x, y, colorStr);

                    // Запоминаем текущую точку как начало следующей линии
                    lastX = x;
                    lastY = y;
                }
            }
        });
    }

    /**
     * Масштабирование координаты X
     * размер окна может меняться изображение всегда 600x500
     */
    private int scaleX(int panelX) {
        if (canvas.getWidth() == 0) return panelX;  // Защита от деления на 0
        // Формула panelX / текущая_ширина_панели * 600
        return (int) ((double) panelX / canvas.getWidth() * 600);
    }

    /**
     * Масштабирование координаты Y
     */
    private int scaleY(int panelY) {
        if (canvas.getHeight() == 0) return panelY;
        return (int) ((double) panelY / canvas.getHeight() * 500);
    }

    /**
     * Преобразование цвета в строку
     */
    private String colorToString(Color color) {
        if (color.equals(Color.BLACK)) return "BLACK";
        if (color.equals(Color.RED)) return "RED";
        if (color.equals(new Color(255, 165, 0))) return "ORANGE";
        if (color.equals(Color.YELLOW)) return "YELLOW";
        if (color.equals(Color.GREEN)) return "GREEN";
        if (color.equals(Color.BLUE)) return "BLUE";
        if (color.equals(new Color(128, 0, 128))) return "PURPLE";
        if (color.equals(Color.WHITE)) return "WHITE";
        return "BLACK";  // По умолчанию черный
    }

    /**
     * Преобразование строки обратно в цвет
     */
    private Color stringToColor(String colorStr) {
        switch (colorStr) {
            case "RED": return Color.RED;
            case "ORANGE": return new Color(255, 165, 0);
            case "YELLOW": return Color.YELLOW;
            case "GREEN": return Color.GREEN;
            case "BLUE": return Color.BLUE;
            case "PURPLE": return new Color(128, 0, 128);
            case "WHITE": return Color.WHITE;
            default: return Color.BLACK;  // По умолчанию черный
        }
    }

    //МЕТОДЫ КОТОРЫЕ ВЫЗЫВАЕТ GAMECLIENT

    /**
     * Установка роли игрока
     * true = художник, false = угадывающий
     */
    public void setRole(boolean isDrawer) {
        this.isDrawer = isDrawer;
        System.out.println("Установка роли: isDrawer = " + isDrawer);

        // UI обновится в правильном потоке
        SwingUtilities.invokeLater(() -> {
            if (isDrawer) {
                roleLabel.setText("🎨 Вы рисуете");
                roleLabel.setForeground(new Color(0, 100, 200));  // Синий
                guessField.setToolTipText("Введите сообщение в чат");
                guessField.setEnabled(true);   // Включаем поле ввода
                guessButton.setEnabled(true);  // Включаем кнопку
            } else {
                roleLabel.setText("🔍 Вы угадываете");
                roleLabel.setForeground(new Color(200, 0, 0));  // Красный
                guessField.setToolTipText("Введите слово для угадывания");
                guessField.setEnabled(true);
                guessButton.setEnabled(true);
            }
            statusLabel.setText("");  // Очищаем статус при смене роли
            roleLabel.revalidate();   // Принудительно обновляем интерфейс
            roleLabel.repaint();
        });
    }

    /**
     * Установка слова для угадывания
     */
    public void setWord(String word) {
        SwingUtilities.invokeLater(() -> {
            if (word == null || word.isEmpty()) {
                // Очищаем слово между раундами
                wordLabel.setText("");
                wordLabel.setVisible(false);
            } else if (isDrawer) {
                // Художнику показываем слово
                wordLabel.setText("🎯 СЛОВО: " + word.toUpperCase());
                wordLabel.setForeground(new Color(0, 150, 0));  // Зеленый
                wordLabel.setVisible(true);
                wordLabel.setFont(new Font("Arial", Font.BOLD, 18));
            } else {
                // Угадывающему не показываем слово
                wordLabel.setText("❓ Угадайте слово!");
                wordLabel.setForeground(new Color(200, 0, 0));  // Красный
                wordLabel.setVisible(true);
                wordLabel.setFont(new Font("Arial", Font.BOLD, 16));
            }
        });
    }

    /**
     * Нарисовать линию
     */
    public void drawLine(int x1, int y1, int x2, int y2, String color) {
        SwingUtilities.invokeLater(() -> {
            canvas.drawLine(x1, y1, x2, y2, stringToColor(color));
        });
    }

    /**
     * Очистить холст
     */
    public void clearCanvas() {
        SwingUtilities.invokeLater(() -> {
            canvas.clear();
        });
    }

    /**
     * Обновить счет игры
     */
    public void updateScore(String scoreData) {
        SwingUtilities.invokeLater(() -> {
            String formatted = scoreData.replace(",", " - ").replace("=", ": ");
            scoreLabel.setText("Счет: " + formatted);
        });
    }

    /**
     * Добавить сообщение в чат
     */
    public void addChatMessage(String nickname, String message) {
        SwingUtilities.invokeLater(() -> {
            String formattedMessage;
            if (nickname.equals("СИСТЕМА")) {
                formattedMessage = "💬 " + nickname + ": " + message;  // Системное сообщение
            } else {
                formattedMessage = nickname + ": " + message;          // Сообщение игрока
            }
            chatArea.append(formattedMessage + "\n");
            // Прокручиваем вниз чтобы видеть новые сообщения
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
        });
    }

    /**
     * Обработка начала игры
     */
    public void onGameStart(String message) {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText(message);  // надпись игра началась

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

            // Если роль еще не отображается первый запуск
            if (roleLabel.getText().isEmpty()) {
                if (isDrawer) {
                    roleLabel.setText("🎨 Вы рисуете");
                } else {
                    roleLabel.setText("🔍 Ожидание");
                }
            }
        });
    }

    /**
     * Обработка правильного угадывания слова
     */
    public void onCorrectGuess(String winner) {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText("Правильно! " + winner + " угадал(а)! Новый раунд...");
            // Холст будет очищен автоматически
        });
    }

    /**
     * Показать сообщение об ошибке
     */
    public void showError(String error) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(this, error, "Ошибка", JOptionPane.ERROR_MESSAGE);
        });
    }

    /**
     * Обработка разрыва соединения
     */
    public void onDisconnect() {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(this, "Соединение потеряно", "Ошибка", JOptionPane.ERROR_MESSAGE);
            System.exit(0);  // Завершаем программу
        });
    }

    /**
     * Внутренний класс холст для рисования
     */
    private class DrawingCanvas extends JPanel {
        private BufferedImage image;  // Изображение на котором рисуем
        private Graphics2D g2d;       // Инструмент для рисования на изображении

        public DrawingCanvas() {
            // Создаем изображение фиксированного размера
            image = new BufferedImage(600, 500, BufferedImage.TYPE_INT_RGB);
            g2d = image.createGraphics();  // Получаем инструмент для рисования

            // Настройки для красивого рисования
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); //сглаживание кисти

            // Заливаем белым цветом фон
            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, 0, 600, 500);

            // Устанавливаем черный цвет по умолчанию
            g2d.setColor(Color.BLACK);
        }

        /**
         * Метод который рисует компонент на экране
         */
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            if (image != null) {
                // Рисуем изображение с масштабированием
                g.drawImage(image, 0, 0, getWidth(), getHeight(), null);
            }
        }

        /**
         * Нарисовать линию на холсте
         */
        public void drawLine(int x1, int y1, int x2, int y2, Color color) {
            g2d.setColor(color); // Устанавливаем цвет
            g2d.setStroke(new BasicStroke(3)); // Толщина линии 3 пикселя
            g2d.drawLine(x1, y1, x2, y2); // Рисуем линию
            repaint(); // Просим Swing перерисовать компонент
        }

        /**
         * Очистить холст (залить белым)
         */
        public void clear() {
            g2d.setColor(Color.WHITE); // Берем белый цвет
            g2d.fillRect(0, 0, image.getWidth(), image.getHeight()); // Заливаем весь холст
            g2d.setColor(Color.BLACK); // Возвращаем черный цвет по умолчанию
            repaint(); // Перерисовываем
        }
    }
}