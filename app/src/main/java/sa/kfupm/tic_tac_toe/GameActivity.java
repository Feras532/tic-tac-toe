package sa.kfupm.tic_tac_toe;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class GameActivity extends AppCompatActivity {
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

    private void disableBoard() {
        for (Button button : buttons) {
            button.setEnabled(false);
        }
    }

    private void enableBoard() {
        for (Button button : buttons) {
            button.setEnabled(true);
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
        String[] lines = new String[]{
                gameData.getBoard().get(0) + gameData.getBoard().get(1) + gameData.getBoard().get(2),
                gameData.getBoard().get(3) + gameData.getBoard().get(4) + gameData.getBoard().get(5),
                gameData.getBoard().get(6) + gameData.getBoard().get(7) + gameData.getBoard().get(8),
                gameData.getBoard().get(0) + gameData.getBoard().get(3) + gameData.getBoard().get(6),
                gameData.getBoard().get(1) + gameData.getBoard().get(4) + gameData.getBoard().get(7),
                gameData.getBoard().get(2) + gameData.getBoard().get(5) + gameData.getBoard().get(8),
                gameData.getBoard().get(0) + gameData.getBoard().get(4) + gameData.getBoard().get(8),
                gameData.getBoard().get(2) + gameData.getBoard().get(4) + gameData.getBoard().get(6)
        };

        for (String line : lines) {
            if (line.equals("XXX") || line.equals("OOO")) {
                gameData.setGameOver(true);
                gameData.setWinner(gameData.getCurrentTurn()); // Set the current player as winner
                Toast.makeText(this, "Player " + (line.equals("XXX") ? "X" : "O") + " wins!", Toast.LENGTH_LONG).show();
                updateFirestore(); // Update the database with the new game state
                disableBoard(); // Disable the board as the game is over
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
            Toast.makeText(this, "The game is a draw!", Toast.LENGTH_LONG).show();
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
                    } else if (gameData.getPlayer2Id() == null || gameData.getPlayer2Id().isEmpty()) {
                        // Second player joins the game as Player O
                        gameData.setPlayer2Id(currentUser.getUid());
                        gameRef.update("player2Id", currentUser.getUid())
                                .addOnSuccessListener(aVoid -> {
                                    addGameListener();
                                    enableBoard();
                                    Toast.makeText(this, "You are 'O'. Game on!", Toast.LENGTH_SHORT).show();
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

        //update albtaa3
        db.collection("games").document(gameId).update(updates)
                .addOnSuccessListener(aVoid -> Log.d("Firestore", "Game reset successfully"))
                .addOnFailureListener(e -> Log.d("Firestore", "Error resetting game", e));
    }
}