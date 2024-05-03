package sa.kfupm.tic_tac_toe;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

public class UserDetailsActivity extends AppCompatActivity {
    private EditText nameInput;
    private Button saveButton;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_details);

        mAuth = FirebaseAuth.getInstance();
        nameInput = findViewById(R.id.editTextName);
        saveButton = findViewById(R.id.buttonSave);

        saveButton.setOnClickListener(v -> saveUserName());
    }

    private void saveUserName() {
        String name = nameInput.getText().toString();
        if (!TextUtils.isEmpty(name)) {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                    .setDisplayName(name)
                    .build();

            if (user != null) {
                user.updateProfile(profileUpdates)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Log.d("UpdateProfile", "User profile updated.");
                                // Redirect to Welcome Activity
                                Intent intent = new Intent(UserDetailsActivity.this, Welcome.class);
                                intent.putExtra("userId", user.getUid());
                                intent.putExtra("userName", name);
                                startActivity(intent);
                                finish();
                            } else {
                                Log.e("UpdateProfile", "Error updating profile", task.getException());
                                Toast.makeText(UserDetailsActivity.this, "Failed to update name", Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        } else {
            Toast.makeText(this, "Please enter a name", Toast.LENGTH_SHORT).show();
        }
    }
}