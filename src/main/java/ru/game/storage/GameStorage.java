package ru.game.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * сохранение и загрузка состояния игры
 */

public class GameStorage {
    private static final String STATS_FILE = "game_stats.json";
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    
    /**
     * сохранить статистику игроков
     */
    public static void saveStats(List<PlayerStats> stats) {
        try (Writer writer = new FileWriter(STATS_FILE)) { //объект для записи в файл
            gson.toJson(stats, writer); //превратит stats в json и запишет в writer
        } catch (IOException e) {
            System.err.println("Ошибка сохранения статистики: " + e.getMessage());
        }
    }
    
    /**
     * загрузить статистику игроков
     */
    public static List<PlayerStats> loadStats() {
        File file = new File(STATS_FILE);
        if (!file.exists()) { //если файла не сущ
            return new ArrayList<>();
        }

        //открываем файл для чтения
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            StringBuilder json = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                json.append(line).append("\n");
            }
            
            String jsonString = json.toString().trim();
            if (jsonString.isEmpty()) {
                return new ArrayList<>();
            }

            //из json получаем массив обьектов PlayerStats
            PlayerStats[] stats = gson.fromJson(jsonString, PlayerStats[].class);
            if (stats != null) {
                return new ArrayList<>(Arrays.asList(stats));
            }
        } catch (IOException e) {
            System.err.println("Ошибка загрузки статистики: " + e.getMessage());
        }
        return new ArrayList<>();
    }
    
    /**
     * класс для статистики игрока
     */
    public static class PlayerStats {
        private String nickname;
        private int totalScore;
        private int gamesPlayed;
        
        public PlayerStats(String nickname) {
            this.nickname = nickname;
            this.totalScore = 0;
            this.gamesPlayed = 0;
        }
        
        public String getNickname() { return nickname; }
        public void setNickname(String nickname) { this.nickname = nickname; }
        public int getTotalScore() { return totalScore; }
        public void setTotalScore(int totalScore) { this.totalScore = totalScore; }
        public int getGamesPlayed() { return gamesPlayed; }
        public void setGamesPlayed(int gamesPlayed) { this.gamesPlayed = gamesPlayed; }
    }
}
