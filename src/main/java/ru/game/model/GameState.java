package ru.game.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Состояние игры
 */
public class GameState {
    
    private List<Player> players;
    private String currentWord;
    private boolean gameStarted;
    private int roundNumber;
    
    // Слова для игры
    private static final String[] WORDS = {
            "КОТ", "СОБАКА", "ДОМ", "МАШИНА", "СОЛНЦЕ", "ДЕРЕВО",
            "ЯБЛОКО", "КНИГА", "СТОЛ", "СТУЛ", "ОКНО", "ДВЕРЬ",
            "МОРЕ", "ГОРА", "ЦВЕТОК", "ПТИЦА", "РЫБА", "ЛЕВ",
            "СЛОН", "МЕДВЕДЬ", "ЗАЯЦ", "ЛИСА", "ВОЛК", "КОРОВА",
            "ТЕЛЕФОН", "КОМПЬЮТЕР", "ПЛАНШЕТ", "ТЕЛЕВИЗОР", "ХОЛОДИЛЬНИК",
            "МИКРОВОЛНОВКА", "ЧАЙНИК", "ТОСТЕР", "ФЕН", "УТЮГ", "ПЫЛЕСОС",
            "СКОВОРОДА", "КАСТРЮЛЯ", "ТАРЕЛКА", "ВИЛКА", "ЛОЖКА", "НОЖ",
            "КРОВАТЬ", "ШКАФ", "КОМОД", "ЗЕРКАЛО", "КОВЕР", "ПОДУШКА",
            "ОДЕЯЛО", "ПРОСТЫНЯ", "ПОЛОТЕНЦЕ", "МЫЛО", "ШАМПУНЬ", "ЗУБНАЯЩЕТКА",
            "ПАСТА", "РАЗЕТКА", "ВЫКЛЮЧАТЕЛЬ", "ЛАМПОЧКА", "ПРОВОД", "БАТАРЕЙКА",
            "АККУМУЛЯТОР", "ЗАРЯДКА", "НАУШНИКИ", "КОЛОНКИ", "МИКРОФОН", "КАМЕРА",
            "ФОТОАППАРАТ", "ВИДЕОКАМЕРА", "ПРОЕКТОР", "ЭКРАН", "КЛАВИАТУРА", "МЫШКА",
            "КОЛЕСО", "РУЛЬ", "ФАРА", "ДВИГАТЕЛЬ", "АККУМУЛЯТОР", "ШИНА",
            "ЛОДКА", "САМОЛЕТ", "ВЕРТОЛЕТ", "ПОЕЗД", "ТРАМВАЙ", "ТРОЛЛЕЙБУС",
            "ВЕЛОСИПЕД", "САМОКАТ", "РОЛИКИ", "СКЕЙТБОРД", "ЛЫЖИ", "СНЕГОХОД",
            "ПАРОВОЗ", "ТАНКЕР", "ПАРОМ", "ЯХТА", "КАНОЭ", "БАЙДАРКА",
            "ПАРАШЮТ", "ВОЗДУШНЫЙШАР", "ДИРИЖАБЛЬ", "РАКЕТА", "СПУТНИК", "ТЕЛЕСКОП"
    };
    
    public GameState() {
        this.players = new ArrayList<>();
        this.gameStarted = false;
        this.roundNumber = 0;
    }
    
    public List<Player> getPlayers() {
        return players;
    }
    
    public void addPlayer(Player player) {
        players.add(player);
    }
    
    public boolean isGameStarted() {
        return gameStarted;
    }
    
    public void setGameStarted(boolean gameStarted) {
        this.gameStarted = gameStarted;
    }
    
    public String getCurrentWord() {
        return currentWord;
    }
    
    public void setCurrentWord(String word) {
        this.currentWord = word;
    }
    
    public String getRandomWord() {
        Random random = new Random();
        return WORDS[random.nextInt(WORDS.length)];
    }
    
    public void nextRound() {
        roundNumber++;
        // Меняем роли
        for (Player player : players) {
            player.setDrawer(!player.isDrawer());
        }
        // Выбираем новое слово
        currentWord = getRandomWord();
    }
    
    public int getRoundNumber() {
        return roundNumber;
    }
    
    public Player getDrawer() {
        for (Player player : players) {
            if (player.isDrawer()) {
                return player;
            }
        }
        return null;
    }
    
    public Player getGuesser() {
        for (Player player : players) {
            if (!player.isDrawer()) {
                return player;
            }
        }
        return null;
    }
}
