package sa.kfupm.tic_tac_toe;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class Welcome extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome); // Make sure to use your actual layout file name

        TextView textViewWelcome = findViewById(R.id.textViewWelcome);

        String userId = getIntent().getStringExtra("userId"); // Retrieve the user ID
        String userName = getIntent().getStringExtra("userName"); // Retrieve the user name

        if (userName != null && !userName.isEmpty()) {
            textViewWelcome.setText("Welcome, " + userName + "!");
        } else {
            textViewWelcome.setText("Welcome, User ID: " + userId + "!");
        }
    }
}
