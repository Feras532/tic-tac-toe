package sa.kfupm.tic_tac_toe;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class OnlineTypeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_online_type);

        Button buttonCreateGame = findViewById(R.id.buttonCreateGame);
        Button buttonJoinGame = findViewById(R.id.buttonJoinGame);
        EditText editTextGameId = findViewById(R.id.editTextGameId);

        buttonCreateGame.setOnClickListener(view -> {
            Intent intent = new Intent(this, WaitingActivity.class);
            startActivity(intent);
        });

        buttonJoinGame.setOnClickListener(view -> {
            String gameId = editTextGameId.getText().toString();
            if (!gameId.isEmpty()) {
                Intent intent = new Intent(this, GameActivity.class);
                intent.putExtra("gameId", gameId);
                startActivity(intent);
            } else {
                editTextGameId.setError("Please enter a game ID");
            }
        });
    }
}