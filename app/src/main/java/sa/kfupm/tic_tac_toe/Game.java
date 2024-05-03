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

public class Game extends AppCompatActivity {
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

        String action = getIntent().getStringExtra("action");
        if ("create".equals(action)) {
            startNewGame();
        } else if ("join".equals(action)) {
            gameId = getIntent().getStringExtra("gameId");
            joinGame();
        }
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

    private void startNewGame() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        gameId = db.collection("games").document().getId();
        GameData newGameData = new GameData(currentUser.getUid()); // Assuming constructor initializes the game state
        db.collection("games").document(gameId).set(newGameData)
                .addOnSuccessListener(aVoid -> {
                    gameData = newGameData;
                    addGameListener();
                    enableBoard();
                    Toast.makeText(this, "New game started. You are 'X'", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e("Game", "Failed to create game", e);
                    Toast.makeText(this, "Failed to start new game", Toast.LENGTH_SHORT).show();
                });
    }

    private void joinGame() {
        DocumentReference gameRef = db.collection("games").document(gameId);
        gameRef.get().addOnSuccessListener(documentSnapshot -> {
            gameData = documentSnapshot.toObject(GameData.class);
            if (gameData.getPlayer2Id() == null || gameData.getPlayer2Id().isEmpty()) {
                gameData.setPlayer2Id(mAuth.getCurrentUser().getUid());
                gameRef.set(gameData)
                        .addOnSuccessListener(aVoid -> {
                            addGameListener();
                            enableBoard();
                            Toast.makeText(this, "Joined game. You are 'O'", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            Log.e("Game", "Failed to update game with player 2 ID", e);
                            Toast.makeText(this, "Failed to update game data", Toast.LENGTH_SHORT).show();
                        });
            } else {
                addGameListener();
                enableBoard();
                Toast.makeText(this, "Game already has two players", Toast.LENGTH_SHORT).show();
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
}