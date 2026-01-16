package ru.game.server;

import ru.game.model.GameState;
import ru.game.model.Player;
import ru.game.protocol.Protocol;
import ru.game.storage.GameStorage;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * сервер для игры
 * создает отдельный поток для каждого клиента
 * читает его сообщения передает их серверу
 * отправляет ответы и закрывает
 * соединение при отключении
 */
public class GameServer {
    private static final int PORT = 8888;            // порт для подключения
    private ServerSocket serverSocket;               // сокет сервера
    private GameState gameState;                     // состояние игры
    private List<ClientHandler> clients;             // список подключенных клиентов

    // СОЗДАНИЕ СЕРВЕРА
    public GameServer() {
        this.clients = new ArrayList<>();
        this.gameState = new GameState();
    }

    // ТОЧКА ВХОДА ПРОГРАММЫ
    public static void main(String[] args) {
        System.out.println("Запуск сервера...");
        System.out.flush();
        try {
            System.out.println("Создание объекта GameServer...");
            System.out.flush();
            GameServer server = new GameServer();    // Создаем сервер
            System.out.println("Сервер создан, запускаем...");
            System.out.flush();
            server.start();                          // Запускаем сервер
        } catch (Exception e) {
            System.err.println("КРИТИЧЕСКАЯ ОШИБКА при запуске сервера:");
            e.printStackTrace();
            System.exit(1);
        }
    }

    //ЗАПУСК СЕРВЕРА
    // создаем ServerSocket на порту
    // запускаем бесконечный цикл принятия клиентов
    // для каждого клиента создаем свой обработчик ClientHandler
    // запускаем ClientHandler в отдельном потоке
    public void start() {
        try {
            serverSocket = new ServerSocket(PORT);   // создаем серверный сокет
            System.out.println("Сервер запущен на порту " + PORT);
            System.out.println("Ожидание игроков...");
            System.out.println("__________________________________");

            while (true) {  // бесконечный цикл
                // ждет пока подключится клиент
                Socket clientSocket = serverSocket.accept();
                System.out.println("Новое подключение: " + clientSocket.getInetAddress());

                // для каждого клиента создаем обработчик
                ClientHandler handler = new ClientHandler(clientSocket, this);
                clients.add(handler);  // добавляем в список
                new Thread(handler).start(); // запускаем в отдельном потоке
            }
        } catch (java.net.BindException e) {
            System.err.println("__________________________________");
            System.err.println("ОШИБКА: Порт " + PORT + " уже занят!");
            System.err.println("Измените порт");
            System.err.println("__________________________________");
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("__________________________________");
            System.err.println("ОШИБКА сервера: " + e.getMessage());
            System.err.println("__________________________________");
            e.printStackTrace();
        }
    }

    // КЛАСС ClientHandler
    /**
     * обработчик подключения клиента
     * КАЖДЫЙ КЛИЕНТ В ОТДЕЛЬНОМ ПОТОКЕ:
     * имеет свое соединение Socket
     * читает сообщения от клиента BufferedReader
     * отправляет ответы клиенту PrintWriter
     * работает параллельно с другими клиентами
     */
    static class ClientHandler implements Runnable {
        private Socket socket;        // Соединение с клиентом
        private GameServer server;    // Ссылка на главный сервер
        private BufferedReader in;    // ОТ клиента
        private PrintWriter out;      //  КЛИЕНТУ
        private Player player;
        private String nickname;

        // КОНСТРУКТОР ClientHandler
        public ClientHandler(Socket socket, GameServer server) {
            this.socket = socket;
            this.server = server;
        }

        //  ГЛАВНЫЙ ЦИКЛ КЛИЕНТА
        // МЕТОД ВЫПОЛНЯЕТСЯ ПРИ ЗАПУСКЕ ПОТОКА:
        // устанавливает потоки ввода и вывода
        // запускает бесконечный цикл чтения сообщений
        // разбираем сообщения через Protocol
        //передаем серверу на обработку
        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true); // autoflush сразу отправлять

                String line;
                // пока клиент не отключится
                while ((line = in.readLine()) != null) {
                    // разбираем строку в объект сообщения
                    Protocol.Message message = Protocol.parse(line);
                    if (message != null) {
                        // передаем сообщение серверу для обработки
                        server.handleMessage(this, message);
                    }
                }
            } catch (IOException e) {
                System.out.println("Клиент отключен: " + (nickname != null ? nickname : "unknown"));
            } finally { // В ЛЮБОМ СЛУЧАЕ
                try {
                    server.removeClient(this);  // удаляем клиента из списка
                    socket.close();             // закрываем соединение
                } catch (IOException e) {
                    System.err.println("Ошибка закрытия соединения: " + e.getMessage());
                }
            }
        }

        // ОТПРАВКА СООБЩЕНИЯ КЛИЕНТУ
        public void send(String message) throws IOException {
            out.println(message);//пишет сообщение в выходной поток
            out.flush();
        }

        // БЕЗОПАСНОЕ ЗАКРЫТИЕ
        public void close() {
            try {
                // проверяем что сокет существует и еще не закрыт
                if (socket != null && !socket.isClosed()) {
                    socket.close(); //закрваем
                }
            } catch (IOException e) {
                System.err.println("Ошибка закрытия соединения: " + e.getMessage());
            }
        }

        // геттеры и сеттеры
        public Player getPlayer() { return player; }
        public void setPlayer(Player player) { this.player = player; }
        public String getNickname() { return nickname; }
        public void setNickname(String nickname) { this.nickname = nickname; }
    }

    // ОБРАБОЧИК КОМАНД
    // synchronized защита от гонки данных
    // извлекаем команду и данные из сообщения
    // делегируем соответствующему обработчику
    public synchronized void handleMessage(ClientHandler client, Protocol.Message message) {
        String command = message.getCommand();
        String data = message.getData();

        // в зависимости от команды вызываем соответствующий метод
        switch (command) {
            case Protocol.JOIN:    // присоединение к игре
                handleJoin(client, data);
                break;
            case Protocol.DRAW:    // рисование
                handleDraw(client, data);
                break;
            case Protocol.GUESS:   // попытка угадать слово
                handleGuess(client, data);
                break;
            case Protocol.CLEAR:   // очистка холста
                handleClear(client);
                break;
            case Protocol.CHAT:    // сообщение в чат
                handleChat(client, data);
                break;
        }
    }

    // ПРИСОЕДИНЕНИЕ К ИГРЕ
    // проверяем не больше ли 2 игроков отказываем третьему
    // создаем нового игрока с ником
    // добавляем в состояние игры
    // если 2 игрока начинаем игру
    private void handleJoin(ClientHandler client, String nickname) {
        if (gameState.getPlayers().size() >= 2) {
            System.out.println("Попытка подключения третьего игрока: " + nickname);
            sendToClient(client, Protocol.ERROR + ":Сервер полон (максимум 2 игрока)");
            // даем время на отправку сообщения
            try { Thread.sleep(200); } catch (InterruptedException e) {}
            // закрываем соединение с третьим клиентом
            System.out.println("Отключение третьего игрока: " + nickname);
            client.close();
            removeClient(client);
            return;
        }

        // создаем нового игрока
        Player player = new Player(nickname);
        gameState.addPlayer(player); // добавляем в состояние игры
        client.setPlayer(player); // связываем с клиентом
        client.setNickname(nickname); //сохраняем ник

        System.out.println("Игрок подключен: " + nickname + " (всего: " + gameState.getPlayers().size() + ")");

        // если оба игрока подключены, начинаем игру
        if (gameState.getPlayers().size() == 2) {
            System.out.println("Оба игрока подключены, начинаем игру!");
            // задержка чтобы второй клиент успел инициализироваться
            try { Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            startGame();  // начинае игру
        } else {
            System.out.println("Ожидаем второго игрока...");
            // первому игроку об ожидании
            sendToClient(client, Protocol.GAME_START + ":Ожидание 2-го игрока...");
            updateScores(); // отправляем счет
        }
    }

    // НАЧАЛО ИГРЫ
    // устанавливаем роли первый рисует, второй угадывает
    // выбираем случайное слово
    // отправляем роли и информацию игрокам
    private void startGame() {
        gameState.setGameStarted(true);

        // устанавливаем роли первый игрок рисует, второй угадывает
        if (gameState.getPlayers().size() == 2) {
            gameState.getPlayers().get(0).setDrawer(true);
            gameState.getPlayers().get(1).setDrawer(false);
        }

        // выбираем случайное слово из списка
        gameState.setCurrentWord(gameState.getRandomWord());

        System.out.println("Игра началась!");
        System.out.println("Рисующий: " + gameState.getDrawer().getNickname());
        System.out.println("Угадывающий: " + gameState.getGuesser().getNickname());
        System.out.println("Слово: " + gameState.getCurrentWord());

        Player drawer = gameState.getDrawer();    // рисует
        Player guesser = gameState.getGuesser();  // угадывает

        // сначала отправляем счет
        updateScores();

        // отправляем роли и информацию о игре
        for (ClientHandler client : clients) {
            if (client.getPlayer() == drawer) {
                // рисующему отправляем роль и слово
                sendToClient(client, Protocol.createRole("DRAWER"));
                sendToClient(client, Protocol.createWord(gameState.getCurrentWord()));
                sendToClient(client, Protocol.GAME_START + ":Начните рисовать!");
            } else if (client.getPlayer() == guesser) {
                // угадывающему отправляем роль и сообщение
                sendToClient(client, Protocol.createRole("GUESSER"));
                sendToClient(client, Protocol.GAME_START + ":Угадайте, что рисует " + drawer.getNickname());
            }
        }
    }

    //  ИГРОВАЯ ЛОГИКА
    // ОБРАБОТКА ПОПЫТКИ УГАДАТЬ СЛОВО:
    // проверяем угадал ли игрок слово
    // если угадал даем очки сохраняем статистику меняем роли
    // если не угадал отправляем догадку в чат
    private void handleGuess(ClientHandler client, String guess) {
        Player player = client.getPlayer();
        // проверяем что угадывает именно угадывающий игрок
        if (player != null && !player.isDrawer()) {
            String correctWord = gameState.getCurrentWord();

            if (guess.trim().equalsIgnoreCase(correctWord)) {
                // ОТВЕТ!
                player.addScore(10);  // даем 10 очков

                saveStatistics(); // сохраняем статистику в файл

                // отправляем всем сообщение об успехе
                String guesserRole = player.getNickname() + "(угадывает)";
                broadcast(Protocol.createChat("СИСТЕМА", guesserRole + " угадал(а)! Слово: " + correctWord));
                broadcast(Protocol.CORRECT + ":" + player.getNickname());

                broadcast(Protocol.CLEAR + ":");  // очищаем холст у всех

                try { Thread.sleep(2000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                // МЕНЯЕМ РОЛИ
                gameState.nextRound();

                System.out.println("Новый раунд! Рисующий: " + gameState.getDrawer().getNickname());
                System.out.println("Новое слово: " + gameState.getCurrentWord());

                // очищаем слово у всех перед отправкой новых ролей
                for (ClientHandler c : clients) {
                    sendToClient(c, Protocol.createWord(""));
                }

                // отправляем новые роли и слово
                Player drawer = gameState.getDrawer();
                Player guesser = gameState.getGuesser();

                for (ClientHandler c : clients) {
                    if (c.getPlayer() == drawer) {
                        // рисующему отправляем роль и новое слово
                        sendToClient(c, Protocol.createRole("DRAWER"));
                        sendToClient(c, Protocol.createWord(gameState.getCurrentWord()));
                        sendToClient(c, Protocol.GAME_START + ":Начните рисовать!");
                    } else if (c.getPlayer() == guesser) {
                        // угадывающему отправляем роль
                        sendToClient(c, Protocol.createRole("GUESSER"));
                        sendToClient(c, Protocol.GAME_START + ":Угадайте, что рисует " + drawer.getNickname());
                    }
                }

                updateScores();  // обновляем счет
            } else {
                // НЕПРАВИЛЬНЫЙ ОТВЕТ
                String nicknameWithRole = player.getNickname() + "(угадывает)";
                broadcast(Protocol.createChat(nicknameWithRole, guess.trim()));  // догадка в чат
                sendToClient(client, Protocol.createChat("СИСТЕМА", "Неправильно! Попробуйте еще раз."));
            }
        }
    }

    // ПЕРЕДАЧА РИСУНКА
    // проверяем что рисующий игрок отправил координаты
    // пересылаем координаты угадывающему игроку
    private void handleDraw(ClientHandler client, String data) {
        Player player = client.getPlayer();
        if (player != null && player.isDrawer()) {
            // отправляем координаты рисования другому игроку
            for (ClientHandler c : clients) {
                if (c != client && c.getPlayer() != null && !c.getPlayer().isDrawer()) {
                    sendToClient(c, Protocol.DRAW + ":" + data);
                }
            }
        }
    }

    // МЕТОДЫ ОТПРАВКИ
    private void sendToClient(ClientHandler client, String message) {
        try {
            client.send(message); //пишем в сокет и отправляем флэш
        } catch (IOException e) {
            System.err.println("Ошибка отправки клиенту " + client.getNickname() + ": " + e.getMessage());
        }
    }

    // отправка сообщения всем клиентам
    private void broadcast(String message) {
        for (ClientHandler client : clients) {
            sendToClient(client, message);
        }
    }

    private void handleClear(ClientHandler client) {
        broadcast(Protocol.CLEAR + ":");  // очистка холста
    }

    private void handleChat(ClientHandler client, String data) {
        Player player = client.getPlayer();
        if (player == null) return;

        String nickname = client.getNickname();
        String message = data.trim();

        // проверяем не пытается ли рисующий написать слово загаданное
        if (player.isDrawer() && gameState.getCurrentWord() != null) {
            String currentWord = gameState.getCurrentWord().trim();
            if (message.equalsIgnoreCase(currentWord)) {
                sendToClient(client, Protocol.createChat("СИСТЕМА", "Ошибка: нельзя писать слова, которые нужно угадывать!"));
                return;
            }
        }

        // ник(роль):сообщение
        String role = player.isDrawer() ? "рисует" : "угадывает";
        String nicknameWithRole = nickname + "(" + role + ")";
        broadcast(Protocol.createChat(nicknameWithRole, message));
    }

    private void updateScores() {
        if (gameState.getPlayers().size() == 2) {
            Player p1 = gameState.getPlayers().get(0);
            Player p2 = gameState.getPlayers().get(1);
            String scoreMsg = Protocol.createScore(p1.getNickname(), p1.getScore(),
                    p2.getNickname(), p2.getScore());
            broadcast(scoreMsg);
        }
    }

    private void saveStatistics() {
        //загружаем существующую статистику
        List<GameStorage.PlayerStats> allStats = GameStorage.loadStats();

        for (Player player : gameState.getPlayers()) {
            //для всех игроков в текущем состоянии игры
            //статистику всех игроков преобразуем в поток для обработки
            GameStorage.PlayerStats stats = allStats.stream()
                    //оставляем только ту статистику где ник текущего из потока совп с ником из внешнего цикла
                    .filter(s -> s.getNickname().equals(player.getNickname()))
                    .findFirst() //берем первый подходящий
                    .orElse(null); //если не найдено то null

            if (stats == null) { //если статистики нет
                // создаем ему статистику по никнейму
                stats = new GameStorage.PlayerStats(player.getNickname());
                allStats.add(stats); //добавляем
            }

            stats.setTotalScore(player.getScore()); //устанавливаем очки
            stats.setGamesPlayed(stats.getGamesPlayed() + 1); //увеличиваем кол-во сыгранных игр
        }

        GameStorage.saveStats(allStats); //сохраняем в json
    }

    public synchronized void removeClient(ClientHandler client) {
        // сохраняем статистику при отключении
        if (client.getPlayer() != null) {
            saveStatistics();
        }

        clients.remove(client); //удаляем игрока из списка подключений
        if (client.getPlayer() != null) {
            gameState.getPlayers().remove(client.getPlayer()); //удаляем игрока из состояния игры
            System.out.println("Игрок отключен: " + client.getNickname());
        }
    }
}