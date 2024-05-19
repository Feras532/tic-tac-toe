package sa.kfupm.tic_tac_toe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class GameData {
    private String player1Id;
    private String player2Id;
    private List<String> board;
    private String currentTurn;
    private boolean gameOver;
    private String winner;
    private int winsPlayer1;
    private int winsPlayer2;
    private int lossesPlayer1;
    private int lossesPlayer2;
    private List<Integer> winningLineIndices;

    // Default constructor needed for Firebase
    public GameData() {
    }

    // Constructor to initialize a new game
    public GameData(String player1Id) {
        this.player1Id = player1Id;
        this.player2Id = "";
        this.board = new ArrayList<>(Arrays.asList("", "", "", "", "", "", "", "", ""));
        this.currentTurn = player1Id;
        this.gameOver = false;
        this.winner = "";
        this.winsPlayer1 = 0;
        this.winsPlayer2 = 0;
        this.lossesPlayer1 = 0;
        this.lossesPlayer2 = 0;
        this.winningLineIndices = new ArrayList<>();
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

    public int getWinsPlayer1() {
        return winsPlayer1;
    }

    public void setWinsPlayer1(int winsPlayer1) {
        this.winsPlayer1 = winsPlayer1;
    }

    public int getWinsPlayer2() {
        return winsPlayer2;
    }

    public void setWinsPlayer2(int winsPlayer2) {
        this.winsPlayer2 = winsPlayer2;
    }

    public int getLossesPlayer1() {
        return lossesPlayer1;
    }

    public void setLossesPlayer1(int lossesPlayer1) {
        this.lossesPlayer1 = lossesPlayer1;
    }

    public int getLossesPlayer2() {
        return lossesPlayer2;
    }

    public void setLossesPlayer2(int lossesPlayer2) {
        this.lossesPlayer2 = lossesPlayer2;
    }

    public List<Integer> getWinningLineIndices() {
        return winningLineIndices;
    }

    public void setWinningLineIndices(List<Integer> winningLineIndices) {
        this.winningLineIndices = winningLineIndices;
    }

    public void reset() {
        this.board = Arrays.asList("", "", "", "", "", "", "", "", "");
        this.gameOver = false;
        this.winner = "";
        // Randomly choose who starts the game
        Random random = new Random();
        this.currentTurn = random.nextBoolean() ? player1Id : player2Id;
        this.winningLineIndices.clear();
    }
}