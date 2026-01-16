package ru.game.client;

import ru.game.protocol.Protocol;

import javax.swing.*;
import java.io.*;
import java.net.Socket;

/**
 * Клиент для игры
 */
public class GameClient {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String nickname;
    private boolean isDrawer;
    private GameWindow gameWindow; //графический интерфейс клиента
    private boolean connected;
    
    public GameClient(String nickname) {
        this.nickname = nickname;
        this.connected = false;
    }

    /**
     * подключается к игровому серверу
     * устанавливает сетевое соединение создает потоки ввода и вывода
     * отправляет серверу запрос на присоединение к игре
     */

    public void connect(String host, int port) throws IOException { //адрес и порт сервера
        System.out.println("Подключение к " + host + ":" + port + "...");
        
        try {
            socket = new Socket(host, port); //устанавливаем соединение
            System.out.println("Соединение установлено!");

            //потоки для общения с сервером
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            connected = true;
            
            System.out.println("Потоки созданы, отправляем JOIN...");
            
            // отправляем JOIN
            String joinMsg = Protocol.createJoin(nickname);
            System.out.println("Отправляем: " + joinMsg);
            send(joinMsg);
            
            // Запускаем фоновый поток для получения сообщений от сервера
            new Thread(() -> receiveMessages()).start();
            System.out.println("Клиент готов к работе");
        } catch (java.net.ConnectException e) {
            System.err.println("Не удалось подключиться к серверу!");
            System.err.println("Убедитесь, что сервер запущен на " + host + ":" + port);
            throw e;
        } catch (IOException e) {
            System.err.println("Ошибка подключения: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
    
    public void send(String message) {
        if (connected && out != null) {
            out.println(message);
            out.flush();
        }
    }

    //отправляет на сервер данные о рисовании линии
    public void sendDraw(int x1, int y1, int x2, int y2, String color) {
        send(Protocol.createDraw(x1, y1, x2, y2, color));
    }

    // отправляет на сервер предположение слова
    public void sendGuess(String guess) {
        send(Protocol.createGuess(guess));
    }

    //отправляет на сервер команду очистки холста
    public void sendClear() {
        send(Protocol.CLEAR + ":");
    }

     //отправляет на сервер сообщение в чат
    public void sendChat(String message) {
        // Отправляем только сообщение, никнейм добавит сервер
        send(Protocol.CHAT + ":" + message);
    }

    //для фонового потока получения сообщений от сервера
    //постоянно читает данные из входящего потока и обрабатывает их
    private void receiveMessages() {
        try {
            String line;
            while (connected && (line = in.readLine()) != null) {
                Protocol.Message message = Protocol.parse(line); //парсим соо для объекта соо
                if (message != null) {
                    handleMessage(message); //обрабатываем
                }
            }
        } catch (IOException e) { //соединение разорвано
            connected = false;
            if (gameWindow != null) {
                gameWindow.onDisconnect(); //уведомляем о разрыве
            }
        }
    }

    //обрабатывает входящее сообщение от сервера.
    //определяет тип команды и выполняет соответствующие действия.
    private void handleMessage(Protocol.Message message) {
        String command = message.getCommand(); //команда
        String data = message.getData(); //соо
        
        // ERROR обрабатываем даже когда gameWindow еще не создан так как ошибка может произойти на этапе подключения
        if (Protocol.ERROR.equals(command)) {
            SwingUtilities.invokeLater(() -> { //выполнение кода в главном потоке edt который обрабатывает потоки gui
                //показывает диалоговое окно с сообщением (окно по центру, текст соо, заголовок, вид окна с красной кнопкой)
                JOptionPane.showMessageDialog(null,
                    "Ошибка: " + data, 
                    "Ошибка подключения", 
                    JOptionPane.ERROR_MESSAGE);
            });
            // Закрываем соединение при ошибке
            disconnect(); //закрывает сокет и освобгождает ресурсы
            return;
        }
        
        // остальные сообщения требуют gameWindow
        if (gameWindow == null) return;

        // определяем тип команды и выполняем соответствующие действия
        switch (command) {
            case Protocol.ROLE: //сервер назначает роль игроку
                isDrawer = "DRAWER".equals(data);
                gameWindow.setRole(isDrawer);
                break;
            case Protocol.WORD:
                gameWindow.setWord(data);
                break;
            case Protocol.DRAW:
                // Парсим координаты x1,y1,x2,y2,color
                String[] parts = data.split(",");
                if (parts.length == 5) {
                    try {
                        //строки в числа
                        int x1 = Integer.parseInt(parts[0]);
                        int y1 = Integer.parseInt(parts[1]);
                        int x2 = Integer.parseInt(parts[2]);
                        int y2 = Integer.parseInt(parts[3]);
                        String color = parts[4];  // цвет в формате HEX (#RRGGBB)
                        gameWindow.drawLine(x1, y1, x2, y2, color); //передаем данные в окно для отрисовки
                    } catch (NumberFormatException e) {
                        System.err.println("Ошибка парсинга координат: " + data);
                    }
                }
                break;
            case Protocol.CLEAR:
                gameWindow.clearCanvas();
                break;
            case Protocol.SCORE:
                // игрок1=10,игрок2=5
                gameWindow.updateScore(data);
                break;
            case Protocol.CHAT:
                // ник:сообщение
                int colonIndex = data.indexOf(':');
                if (colonIndex > 0) {
                    String chatNick = data.substring(0, colonIndex);
                    String chatMsg = data.substring(colonIndex + 1);
                    gameWindow.addChatMessage(chatNick, chatMsg); //добавляем соо в чат игрового окна
                }
                break;
            case Protocol.GAME_START:
                gameWindow.onGameStart(data); //запускаем подготовку к игре
                break;
            case Protocol.CORRECT:
                gameWindow.onCorrectGuess(data); //обновляем интерфейс
                break;
        }
    }
    
    public void setGameWindow(GameWindow gameWindow) {
        this.gameWindow = gameWindow;
    }
    
    public boolean isDrawer() {
        return isDrawer;
    }
    
    public String getNickname() {
        return nickname;
    }
    
    public void disconnect() {
        connected = false;
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            System.err.println("Ошибка отключения: " + e.getMessage());
        }
    }
}
