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
import java.util.HashMap;
import java.util.Map;

public class GameActivity extends AppCompatActivity {

    private TextView textViewPlayer;
    private TextView winsCounter;
    private TextView losesCounter;
    private String displayName;
    private GameData gameData;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private FirebaseAuth mAuth;
    private Button[] buttons = new Button[9];
    private String gameId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        textViewPlayer = findViewById(R.id.playerName);
        winsCounter = findViewById(R.id.winsCounter);
        losesCounter = findViewById(R.id.losesCounter);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        displayName = user != null ? user.getDisplayName() : null;
        if (!(displayName != null && !displayName.isEmpty())) {
            displayName = user != null ? user.getUid() : "Unknown";
        }

        setupBoard();
        disableBoard();

        gameId = getIntent().getStringExtra("gameId");
        if (gameId != null && !gameId.isEmpty()) {
            joinGame();
        } else {
            Log.e("GameActivity", "No game ID provided");
            finish(); // Exit if no game ID is provided
        }

        // Initialize the Replay button
        Button replayButton = findViewById(R.id.buttonReplay);
        replayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                replayGame();
            }
        });
    }

    private void setupBoard() {
        for (int i = 0; i < 9; i++) {
            String buttonID = "button" + i;
            int resID = getResources().getIdentifier(buttonID, "id", getPackageName());
            buttons[i] = findViewById(resID);
            final int finalI = i;
            buttons[i].setOnClickListener(v -> makeMove(finalI));
        }
    }

    @SuppressLint("ResourceAsColor")
    private void disableBoard() {
        for (Button button : buttons) {
            button.setEnabled(false);
            button.setBackgroundColor(R.color.black);
        }
    }

    @SuppressLint("ResourceAsColor")
    private void enableBoard() {
        for (Button button : buttons) {
            button.setEnabled(true);
            button.setBackgroundColor(R.color.red);
        }
    }

    private void makeMove(int index) {
        if (gameData == null || !gameData.getCurrentTurn().equals(mAuth.getCurrentUser().getUid())) {
            Toast.makeText(this, "It's not your turn!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if the spot is taken or the game is over
        if (gameData.getBoard().get(index).isEmpty() && !gameData.isGameOver()) {
            String currentSymbol = gameData.getCurrentTurn().equals(gameData.getPlayer1Id()) ? "X" : "O";
            gameData.getBoard().set(index, currentSymbol);
            updateGameBoardUI(index, currentSymbol);
            checkGameState();

            // Switch turns
            gameData.setCurrentTurn(gameData.getCurrentTurn().equals(gameData.getPlayer1Id()) ? gameData.getPlayer2Id() : gameData.getPlayer1Id());
            updateFirestore();
        } else {
            Log.d("Game", "Invalid move attempt at index " + index);
        }
    }

    private void updateGameBoardUI(int index, String playerSymbol) {
        buttons[index].setText(playerSymbol);
    }

    private void checkGameState() {
        int[][] winningLines = {
                {0, 1, 2}, // Row 1
                {3, 4, 5}, // Row 2
                {6, 7, 8}, // Row 3
                {0, 3, 6}, // Column 1
                {1, 4, 7}, // Column 2
                {2, 5, 8}, // Column 3
                {0, 4, 8}, // Diagonal 1
                {2, 4, 6}  // Diagonal 2
        };

        for (int[] line : winningLines) {
            if (!gameData.getBoard().get(line[0]).isEmpty() &&
                    gameData.getBoard().get(line[0]).equals(gameData.getBoard().get(line[1])) &&
                    gameData.getBoard().get(line[1]).equals(gameData.getBoard().get(line[2]))) {

                gameData.setGameOver(true);
                gameData.setWinner(gameData.getCurrentTurn()); // Set the current player as winner
                updateGameResult(gameData.getCurrentTurn().equals(gameData.getPlayer1Id())); // Pass true if player 1 wins
                updateFirestore(); // Update the database with the new game state
                disableBoard(); // Disable the board as the game is over

                // Highlight the winning buttons
                //highlightWinningButtons(line);
                return;
            }
        }

        // Check for draw
        boolean isDraw = true;
        for (String cell : gameData.getBoard()) {
            if (cell.isEmpty()) {
                isDraw = false;
                break;
            }
        }

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

        db.collection("games").document(gameId)
                .update(updates)
                .addOnSuccessListener(aVoid -> Log.d("Firestore", "Game updated successfully"))
                .addOnFailureListener(e -> Log.d("Firestore", "Error updating game", e));
    }

    private void joinGame() {
        gameId = getIntent().getStringExtra("gameId"); // Getting the gameId passed from WaitingActivity

        if (gameId == null || gameId.isEmpty()) {
            Toast.makeText(this, "Game ID is required to join a game", Toast.LENGTH_SHORT).show();
            return;
        }

        DocumentReference gameRef = db.collection("games").document(gameId);
        gameRef.get().addOnSuccessListener(documentSnapshot -> {
            gameData = documentSnapshot.toObject(GameData.class);
            if (gameData != null) {
                FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                if (currentUser != null) {
                    if (gameData.getPlayer1Id().equals(currentUser.getUid())) {
                        // The current user is Player X
                        addGameListener();
                        enableBoard();
                        Toast.makeText(this, "You are 'X'. Waiting for Player 'O' to join.", Toast.LENGTH_LONG).show();
                        textViewPlayer.setText(displayName + " as X");
                    } else if (gameData.getPlayer2Id() == null || gameData.getPlayer2Id().isEmpty()) {
                        // Second player joins the game as Player O
                        gameData.setPlayer2Id(currentUser.getUid());
                        gameRef.update("player2Id", currentUser.getUid())
                                .addOnSuccessListener(aVoid -> {
                                    addGameListener();
                                    enableBoard();
                                    Toast.makeText(this, "You are 'O'. Game on!", Toast.LENGTH_SHORT).show();
                                    textViewPlayer.setText(displayName + " as O");
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("Game", "Failed to join game", e);
                                });
                    } else {
                        // Game already has two players
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
                            updateGameUI(); // Update the UI based on the new game state
                            updateWinLossUI(); // Make sure this is called to update counters
                            showGameStatusToast();
                        }
                    } else {
                        Log.d("Firestore", "Current game data: null");
                    }
                });
    }

    private void updateGameUI() {
        // Update the board based on gameData
        for (int i = 0; i < gameData.getBoard().size(); i++) {
            buttons[i].setText(gameData.getBoard().get(i));
        }
        // Enable or disable the board based on whose turn it is
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
            // Reset the game data locally
            gameData.reset();
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
        }).addOnSuccessListener(result -> {
            updateWinLossUI();
        }).addOnFailureListener(e -> {
            Log.e("GameActivity", "Transaction failure: ", e);
        });
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
        }).addOnFailureListener(e -> {
            Log.e("GameActivity", "Error fetching game results", e);
        });
    }

    @SuppressLint("ResourceAsColor")
    private void highlightWinningButtons(int[] winningLine) {
        for (int index : winningLine) {
            buttons[index].setBackgroundColor(R.color.dark_green);
        }
    }
}