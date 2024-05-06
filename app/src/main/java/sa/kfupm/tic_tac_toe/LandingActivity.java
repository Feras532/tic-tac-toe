package sa.kfupm.tic_tac_toe;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LandingActivity extends AppCompatActivity {

    private TextView welcomeText;
    private Button playOnline;
    private Button playOffline;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_landinng);

        welcomeText = findViewById(R.id.welcomeText);
        playOnline = findViewById(R.id.onlineButton);
        playOffline = findViewById(R.id.offlineButton);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String displayName = user != null ? user.getDisplayName() : null;

        if (displayName != null && !displayName.isEmpty()) {
            welcomeText.setText("Welcome, " + displayName + "!");
        } else {
            welcomeText.setText("Welcome, User ID: " + (user != null ? user.getUid() : "Unknown") + "!");
        }

        playOnline.setOnClickListener( view -> {
            Intent intent = new Intent(this, OnlineTypeActivity.class);
            startActivity(intent);
        });

        playOffline.setOnClickListener( view -> {

        });
    }
}