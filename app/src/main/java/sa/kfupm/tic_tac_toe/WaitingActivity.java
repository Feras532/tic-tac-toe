package sa.kfupm.tic_tac_toe;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class WaitingActivity extends AppCompatActivity {
    private TextView gameCodeTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_waiting); // Make sure you have a TextView with ID gameCodeTextView

        gameCodeTextView = findViewById(R.id.gameCode);
        createGameWithUniqueShortId();
    }

    private void createGameWithUniqueShortId() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String newGameId = Utils.generateShortId();

        db.collection("games").document(newGameId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                    // ID collision, retry
                    createGameWithUniqueShortId();
                } else {
                    // Safe to create a new game
                    FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                    GameData newGameData = new GameData(currentUser.getUid());

                    db.collection("games").document(newGameId).set(newGameData).addOnSuccessListener(aVoid -> {
                        gameCodeTextView.setText(newGameId);
                        listenForJoin(newGameId);  // Start listening for the second player to join
                    }).addOnFailureListener(e -> {
                        Log.e("WaitingActivity", "Failed to create game", e);
                    });
                }
            } else {
                Log.e("WaitingActivity", "Failed to check for existing game ID", task.getException());
            }
        });
    }

    private void listenForJoin(String gameId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference gameRef = db.collection("games").document(gameId);

        gameRef.addSnapshotListener((snapshot, e) -> {
            if (e != null) {
                Log.w("WaitingActivity", "Listen failed.", e);
                return;
            }

            if (snapshot != null && snapshot.exists()) {
                GameData updatedGameData = snapshot.toObject(GameData.class);
                if (updatedGameData != null && updatedGameData.getPlayer2Id() != null && !updatedGameData.getPlayer2Id().isEmpty()) {
                    // Second player has joined, proceed to the game
                    Intent intent = new Intent(WaitingActivity.this, GameActivity.class);
                    intent.putExtra("gameId", gameId);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish(); // Finish this activity to prevent returning to it
                } else {
                    // Update UI to indicate still waiting for player O
                    gameCodeTextView.setText(gameId);
                }
            } else {
                Log.d("WaitingActivity", "Current game data: null");
            }
        });
    }

}