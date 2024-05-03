package sa.kfupm.tic_tac_toe;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;

public class Welcome extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        TextView textViewWelcome = findViewById(R.id.textViewWelcome);
        Button buttonCreateGame = findViewById(R.id.buttonCreateGame);
        Button buttonJoinGame = findViewById(R.id.buttonJoinGame);
        EditText editTextGameId = findViewById(R.id.editTextGameId);

        String userId = getIntent().getStringExtra("userId");
        String userName = getIntent().getStringExtra("userName");

        if (userName != null && !userName.isEmpty()) {
            textViewWelcome.setText("Welcome, " + userName + "!");
        } else {
            textViewWelcome.setText("Welcome, User ID: " + userId + "!");
        }

        buttonCreateGame.setOnClickListener(view -> {
            Intent intent = new Intent(this, Game.class);
            intent.putExtra("action", "create");
            startActivity(intent);
        });

        buttonJoinGame.setOnClickListener(view -> {
            String gameId = editTextGameId.getText().toString();
            if (!gameId.isEmpty()) {
                Intent intent = new Intent(this, Game.class);
                intent.putExtra("action", "join");
                intent.putExtra("gameId", gameId);
                startActivity(intent);
            } else {
                editTextGameId.setError("Please enter a game ID");
            }
        });
    }
}