package sa.kfupm.tic_tac_toe;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

public class WaitingActivity extends AppCompatActivity {
    private TextView gameCodeTextView;
    private String newGameId;

    private boolean gameStarted = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_waiting);

        gameCodeTextView = findViewById(R.id.gameCode);
        createGameWithUniqueShortId();
    }



    private void createGameWithUniqueShortId() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        newGameId = Utils.generateShortId();

        db.collection("games").document(newGameId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                    createGameWithUniqueShortId();
                } else {
                    FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                    GameData newGameData = new GameData(currentUser.getUid());

                    db.collection("games").document(newGameId).set(newGameData).addOnSuccessListener(aVoid -> {
                        gameCodeTextView.setText(newGameId);
                        listenForJoin(newGameId);
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

            if (snapshot != null && snapshot.exists() && !gameStarted) {
                GameData updatedGameData = snapshot.toObject(GameData.class);
                if (updatedGameData != null && updatedGameData.getPlayer2Id() != null && !updatedGameData.getPlayer2Id().isEmpty()) {
                    gameStarted = true;
                    Intent intent = new Intent(WaitingActivity.this, GameActivity.class);
                    intent.putExtra("gameId", gameId);
                    startActivity(intent);
                    finish();
                }
            }
        });
    }
}
