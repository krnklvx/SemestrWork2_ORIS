package ru.game.model;

/**
 * Игрок
 */
public class Player {
    
    private String nickname;
    private int score;
    private boolean isDrawer; // true = рисует, false = угадывает
    
    public Player(String nickname) {
        this.nickname = nickname;
        this.score = 0;
        this.isDrawer = false;
    }
    
    public String getNickname() {
        return nickname;
    }
    
    public int getScore() {
        return score;
    }
    
    public void addScore(int points) {
        this.score += points;
    }
    
    public boolean isDrawer() {
        return isDrawer;
    }
    
    public void setDrawer(boolean drawer) {
        this.isDrawer = drawer;
    }
}
