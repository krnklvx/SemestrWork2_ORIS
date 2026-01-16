package ru.game.protocol;

public class Protocol {
    
    // команды протокола
    public static final String JOIN = "JOIN";
    public static final String DRAW = "DRAW";
    public static final String WORD = "WORD";
    public static final String GUESS = "GUESS";
    public static final String CLEAR = "CLEAR";
    public static final String SCORE = "SCORE";
    public static final String CHAT = "CHAT";
    public static final String ROLE = "ROLE"; // ROLE:DRAWER или ROLE:GUESSER
    public static final String GAME_START = "GAME_START";
    public static final String CORRECT = "CORRECT"; // Правильный ответ
    public static final String ERROR = "ERROR";
    
    /**
     * создать сообщение JOIN
     */
    public static String createJoin(String nickname) {
        return JOIN + ":" + nickname;
    }
    
    /**
     * создать сообщение DRAW
     */
    public static String createDraw(int x1, int y1, int x2, int y2, String color) {
        return DRAW + ":" + x1 + "," + y1 + "," + x2 + "," + y2 + "," + color;
    }
    
    /**
     * создать сообщение WORD отправляется только ведущему
     */
    public static String createWord(String word) {
        return WORD + ":" + word;
    }
    
    /**
     * создать сообщение GUESS
     */
    public static String createGuess(String guess) {
        return GUESS + ":" + guess;
    }
    
    /**
     * создать сообщение CLEAR
     */
    public static String createClear() {
        return CLEAR + ":";
    }
    
    /**
     * создать сообщение SCORE
     */
    public static String createScore(String player1, int score1, String player2, int score2) {
        return SCORE + ":" + player1 + "=" + score1 + "," + player2 + "=" + score2;
    }
    
    /**
     * создать сообщение CHAT
     */
    public static String createChat(String nickname, String message) {
        return CHAT + ":" + nickname + ":" + message;
    }
    
    /**
     * создать сообщение ROLE
     */
    public static String createRole(String role) {
        return ROLE + ":" + role; // DRAWER или GUESSER
    }
    
    /**
     * парсинг сообщения
     */
    public static Message parse(String line) {
        if (line == null || line.trim().isEmpty()) {
            return null;
        }
        
        int colonIndex = line.indexOf(':');
        if (colonIndex == -1) {
            return new Message(line.trim(), "");
        }
        
        String command = line.substring(0, colonIndex).trim();
        String data = line.substring(colonIndex + 1);
        
        return new Message(command, data);
    }



    public static class Message {
        private String command;
        private String data;
        
        public Message(String command, String data) {
            this.command = command;
            this.data = data;
        }
        
        public String getCommand() {
            return command;
        }
        
        public String getData() {
            return data;
        }
        
        @Override
        public String toString() {
            return command + ":" + data;
        }
    }
}

