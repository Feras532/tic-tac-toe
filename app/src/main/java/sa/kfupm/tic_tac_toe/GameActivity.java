package sa.kfupm.tic_tac_toe;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class GameActivity extends AppCompatActivity {

    private TextView textViewPlayer, winsCounter, losesCounter;
    private String displayName, gameId;
    private GameData gameData;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private Button[] buttons = new Button[9];
    private Boolean stopUpdate = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        textViewPlayer = findViewById(R.id.playerName);
        winsCounter = findViewById(R.id.winsCounter);
        losesCounter = findViewById(R.id.losesCounter);

        FirebaseUser user = mAuth.getCurrentUser();
        displayName = (user != null && user.getDisplayName() != null && !user.getDisplayName().isEmpty()) ? user.getDisplayName() : (user != null ? user.getUid() : "Unknown");

        setupBoard();
        disableBoard();

        gameId = getIntent().getStringExtra("gameId");
        if (gameId != null && !gameId.isEmpty()) {
            joinGame();
        } else {
            Log.e("GameActivity", "No game ID provided");
            finish();
        }

        findViewById(R.id.buttonReplay).setOnClickListener(v -> replayGame());
    }

    private void setupBoard() {
        for (int i = 0; i < 9; i++) {
            int resID = getResources().getIdentifier("button" + i, "id", getPackageName());
            buttons[i] = findViewById(resID);
            final int finalI = i;
            buttons[i].setOnClickListener(v -> makeMove(finalI));
        }
    }

    @SuppressLint("ResourceAsColor")
    private void enableBoard() {
        int boardColor = getResources().getColor(gameData.getCurrentTurn().equals(gameData.getPlayer1Id()) ? R.color.board_player1 : R.color.board_player2);
        for (Button button : buttons) {
            button.setEnabled(true);
            if (!stopUpdate) button.setBackgroundColor(boardColor);
        }
    }

    @SuppressLint("ResourceAsColor")
    private void disableBoard() {
        for (Button button : buttons) {
            button.setEnabled(false);
            if (!stopUpdate) button.setBackgroundColor(getResources().getColor(R.color.board_default));
        }
    }

    private void makeMove(int index) {
        if (gameData == null || !gameData.getCurrentTurn().equals(mAuth.getCurrentUser().getUid())) {
            Toast.makeText(this, "It's not your turn!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (gameData.getBoard().get(index).isEmpty() && !gameData.isGameOver()) {
            String currentSymbol = gameData.getCurrentTurn().equals(gameData.getPlayer1Id()) ? "X" : "O";
            gameData.getBoard().set(index, currentSymbol);
            updateGameBoardUI(index, currentSymbol);
            checkGameState();
            gameData.setCurrentTurn(gameData.getCurrentTurn().equals(gameData.getPlayer1Id()) ? gameData.getPlayer2Id() : gameData.getPlayer1Id());
            updateFirestore();
            updateGameUI();
        } else {
            Log.d("Game", "Invalid move attempt at index " + index);
        }
    }

    private void updateGameBoardUI(int index, String playerSymbol) {
        buttons[index].setText(playerSymbol);
    }

    private void checkGameState() {
        int[][] winningLines = {
                {0, 1, 2}, {3, 4, 5}, {6, 7, 8}, {0, 3, 6},
                {1, 4, 7}, {2, 5, 8}, {0, 4, 8}, {2, 4, 6}
        };

        for (int[] line : winningLines) {
            if (!gameData.getBoard().get(line[0]).isEmpty() &&
                    gameData.getBoard().get(line[0]).equals(gameData.getBoard().get(line[1])) &&
                    gameData.getBoard().get(line[1]).equals(gameData.getBoard().get(line[2]))) {

                stopUpdate = true;
                gameData.setGameOver(true);
                gameData.setWinner(gameData.getCurrentTurn());
                gameData.setWinningLineIndices(Arrays.asList(line[0], line[1], line[2]));
                updateGameResult(gameData.getCurrentTurn().equals(gameData.getPlayer1Id()));
                updateFirestore();
                disableBoard();
                return;
            }
        }

        boolean isDraw = gameData.getBoard().stream().noneMatch(String::isEmpty);
        if (isDraw) {
            gameData.setGameOver(true);
            gameData.setWinner("Draw");
            updateFirestore();
            disableBoard();
        }
    }

    private void updateFirestore() {
        Map<String, Object> updates = new HashMap<>();
        updates.put("board", gameData.getBoard());
        updates.put("currentTurn", gameData.getCurrentTurn());
        updates.put("gameOver", gameData.isGameOver());
        updates.put("winner", gameData.getWinner());
        updates.put("winningLineIndices", gameData.getWinningLineIndices());

        db.collection("games").document(gameId)
                .update(updates)
                .addOnSuccessListener(aVoid -> Log.d("Firestore", "Game updated successfully"))
                .addOnFailureListener(e -> Log.d("Firestore", "Error updating game", e));
    }

    private void joinGame() {
        DocumentReference gameRef = db.collection("games").document(gameId);
        gameRef.get().addOnSuccessListener(documentSnapshot -> {
            gameData = documentSnapshot.toObject(GameData.class);
            if (gameData != null) {
                FirebaseUser currentUser = mAuth.getCurrentUser();
                if (currentUser != null) {
                    if (gameData.getPlayer1Id().equals(currentUser.getUid())) {
                        addGameListener();
                        enableBoard();
                        Toast.makeText(this, "You are 'X'. Waiting for Player 'O' to join.", Toast.LENGTH_LONG).show();
                        textViewPlayer.setText(displayName + " as X");
                    } else if (gameData.getPlayer2Id() == null || gameData.getPlayer2Id().isEmpty()) {
                        gameData.setPlayer2Id(currentUser.getUid());
                        gameRef.update("player2Id", currentUser.getUid())
                                .addOnSuccessListener(aVoid -> {
                                    addGameListener();
                                    enableBoard();
                                    Toast.makeText(this, "You are 'O'. Game on!", Toast.LENGTH_SHORT).show();
                                    textViewPlayer.setText(displayName + " as O");
                                })
                                .addOnFailureListener(e -> Log.e("Game", "Failed to join game", e));
                    } else {
                        addGameListener();
                        enableBoard();
                        Toast.makeText(this, "Game in progress.", Toast.LENGTH_SHORT).show();
                    }
                }
            } else {
                Toast.makeText(this, "Game data not found.", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> {
            Log.e("Game", "Error fetching game data", e);
            Toast.makeText(this, "Error fetching game data", Toast.LENGTH_SHORT).show();
        });
    }

    private void addGameListener() {
        db.collection("games").document(gameId)
                .addSnapshotListener((documentSnapshot, e) -> {
                    if (e != null) {
                        Log.w("Firestore", "Listen failed.", e);
                        return;
                    }

                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        GameData updatedGameData = documentSnapshot.toObject(GameData.class);
                        if (updatedGameData != null) {
                            gameData = updatedGameData;
                            updateGameUI();
                            if (gameData.getWinningLineIndices() != null && !gameData.getWinningLineIndices().isEmpty()) {
                                highlightWinningButtons(gameData.getWinningLineIndices().stream().mapToInt(i -> i).toArray());
                            }
                            updateWinLossUI();
                            showGameStatusToast();
                        }
                    } else {
                        Log.d("Firestore", "Current game data: null");
                    }
                });
    }

    private void updateGameUI() {
        for (int i = 0; i < gameData.getBoard().size(); i++) {
            buttons[i].setText(gameData.getBoard().get(i));
        }
        if (gameData.getCurrentTurn().equals(mAuth.getCurrentUser().getUid())) {
            enableBoard();
        } else {
            disableBoard();
        }
    }

    private void showGameStatusToast() {
        if (gameData.isGameOver()) {
            if (gameData.getWinner().equals("Draw")) {
                Toast.makeText(this, "The game is a draw!", Toast.LENGTH_LONG).show();
            } else {
                String winner = gameData.getWinner().equals(gameData.getPlayer1Id()) ? "X" : "O";
                Toast.makeText(this, "Player " + winner + " wins!", Toast.LENGTH_LONG).show();
            }
        } else {
            String currentTurnPlayer = gameData.getCurrentTurn().equals(gameData.getPlayer1Id()) ? "X" : "O";
            Toast.makeText(this, "It's Player " + currentTurnPlayer + "'s turn!", Toast.LENGTH_SHORT).show();
        }
    }

    private void replayGame() {
        if (gameId != null && gameData != null) {
            gameData.reset();
            stopUpdate = false;
            updateFirestoreWithNewGame();
            updateGameUI();
            enableBoard();
        }
    }

    private void updateFirestoreWithNewGame() {
        Map<String, Object> updates = new HashMap<>();
        updates.put("board", gameData.getBoard());
        updates.put("currentTurn", gameData.getCurrentTurn());
        updates.put("gameOver", gameData.isGameOver());
        updates.put("winner", gameData.getWinner());
        updates.put("winningLineIndices", gameData.getWinningLineIndices());

        db.collection("games").document(gameId).update(updates)
                .addOnSuccessListener(aVoid -> Log.d("Firestore", "Game reset successfully"))
                .addOnFailureListener(e -> Log.d("Firestore", "Error resetting game", e));
    }

    private void updateGameResult(boolean player1Wins) {
        DocumentReference gameRef = db.collection("games").document(gameId);

        db.runTransaction(transaction -> {
                    DocumentSnapshot snapshot = transaction.get(gameRef);
                    if (player1Wins) {
                        long winsPlayer1 = snapshot.getLong("winsPlayer1") + 1;
                        transaction.update(gameRef, "winsPlayer1", winsPlayer1);
                        long lossesPlayer2 = snapshot.getLong("lossesPlayer2") + 1;
                        transaction.update(gameRef, "lossesPlayer2", lossesPlayer2);
                    } else {
                        long winsPlayer2 = snapshot.getLong("winsPlayer2") + 1;
                        transaction.update(gameRef, "winsPlayer2", winsPlayer2);
                        long lossesPlayer1 = snapshot.getLong("lossesPlayer1") + 1;
                        transaction.update(gameRef, "lossesPlayer1", lossesPlayer1);
                    }
                    return null;
                }).addOnSuccessListener(result -> updateWinLossUI())
                .addOnFailureListener(e -> Log.e("GameActivity", "Transaction failure: ", e));
    }

    private void updateWinLossUI() {
        DocumentReference gameRef = db.collection("games").document(gameId);
        gameRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                Long winsPlayer1 = documentSnapshot.getLong("winsPlayer1");
                Long lossesPlayer1 = documentSnapshot.getLong("lossesPlayer1");
                Long winsPlayer2 = documentSnapshot.getLong("winsPlayer2");
                Long lossesPlayer2 = documentSnapshot.getLong("lossesPlayer2");

                if (mAuth.getCurrentUser().getUid().equals(gameData.getPlayer1Id())) {
                    winsCounter.setText("Wins: " + (winsPlayer1 != null ? winsPlayer1.toString() : "0"));
                    losesCounter.setText("Losses: " + (lossesPlayer1 != null ? lossesPlayer1.toString() : "0"));
                } else {
                    winsCounter.setText("Wins: " + (winsPlayer2 != null ? winsPlayer2.toString() : "0"));
                    losesCounter.setText("Losses: " + (lossesPlayer2 != null ? lossesPlayer2.toString() : "0"));
                }
            }
        }).addOnFailureListener(e -> Log.e("GameActivity", "Error fetching game results", e));
    }

    @SuppressLint("ResourceAsColor")
    private void highlightWinningButtons(int[] winningLine) {
        for (int index : winningLine) {
            buttons[index].setBackgroundColor(getResources().getColor(R.color.dark_green));
        }
    }
}
