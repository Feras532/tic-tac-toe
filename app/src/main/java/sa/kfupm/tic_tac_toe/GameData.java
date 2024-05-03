package sa.kfupm.tic_tac_toe;

import java.util.ArrayList;
import java.util.List;

public class GameData {
    private String player1Id;
    private String player2Id;
    private List<String> board;
    private String currentTurn;
    private boolean gameOver;
    private String winner;

    // Default constructor needed for Firebase
    public GameData() {
    }

    // Constructor to initialize a new game
    public GameData(String player1Id) {
        this.player1Id = player1Id;
        this.player2Id = "";
        this.board = new ArrayList<>(9); //Cuz it is a 3x3 tic-tac-toe board
        for (int i = 0; i < 9; i++) {
            this.board.add(""); // Start my cells with empty value
        }
        this.currentTurn = player1Id;
        this.gameOver = false;
        this.winner = "";
    }

    // Getters and setters
    public String getPlayer1Id() {
        return player1Id;
    }

    public void setPlayer1Id(String player1Id) {
        this.player1Id = player1Id;
    }

    public String getPlayer2Id() {
        return player2Id;
    }

    public void setPlayer2Id(String player2Id) {
        this.player2Id = player2Id;
    }

    public List<String> getBoard() {
        return board;
    }

    public void setBoard(List<String> board) {
        this.board = board;
    }

    public String getCurrentTurn() {
        return currentTurn;
    }

    public void setCurrentTurn(String currentTurn) {
        this.currentTurn = currentTurn;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public void setGameOver(boolean gameOver) {
        this.gameOver = gameOver;
    }

    public String getWinner() {
        return winner;
    }

    public void setWinner(String winner) {
        this.winner = winner;
    }
}