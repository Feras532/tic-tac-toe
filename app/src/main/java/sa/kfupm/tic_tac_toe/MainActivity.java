package sa.kfupm.tic_tac_toe;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import java.util.concurrent.TimeUnit;




public class MainActivity extends AppCompatActivity {

    private static final String TAG = "PhoneAuthActivity";
    private FirebaseAuth mAuth;
    private String mVerificationId;
    private PhoneAuthProvider.ForceResendingToken mResendToken;
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks;

    private EditText phoneInput;
    private EditText otpInput;
    private Button sendCodeButton;
    private Button verifyCodeButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone_auth);
        FirebaseApp.initializeApp(this);

        mAuth = FirebaseAuth.getInstance();

        phoneInput = findViewById(R.id.editTextPhone);
        otpInput = findViewById(R.id.login_otp);
        sendCodeButton = findViewById(R.id.buttonSendCode);
        verifyCodeButton = findViewById(R.id.buttonVerifyCode);

        sendCodeButton.setOnClickListener(v -> startPhoneNumberVerification(phoneInput.getText().toString()));
        verifyCodeButton.setOnClickListener(v -> verifyPhoneNumberWithCode(mVerificationId, otpInput.getText().toString()));

        mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                Toast.makeText(MainActivity.this, "Verification completed", Toast.LENGTH_SHORT).show();
                signInWithPhoneAuthCredential(credential);
            }

            @Override
            public void onVerificationFailed(@NonNull FirebaseException e) {
                Toast.makeText(MainActivity.this, "Verification failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }


            @Override
            public void onCodeSent(@NonNull String verificationId, @NonNull PhoneAuthProvider.ForceResendingToken token) {
                mVerificationId = verificationId;
                mResendToken = token;
                Toast.makeText(MainActivity.this, "Code sent", Toast.LENGTH_SHORT).show();
            }
        };
    }

    private void startPhoneNumberVerification(String phoneNumber) {
        PhoneAuthProvider.verifyPhoneNumber(
                PhoneAuthOptions.newBuilder(mAuth)
                        .setPhoneNumber(phoneNumber)
                        .setTimeout(60L, TimeUnit.SECONDS)
                        .setActivity(this)
                        .setCallbacks(mCallbacks)
                        .build()
        );
        Toast.makeText(this, "Verification code sent to " + phoneNumber, Toast.LENGTH_SHORT).show();
    }

    private void verifyPhoneNumberWithCode(String verificationId, String code) {
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, code);
        signInWithPhoneAuthCredential(credential);
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(MainActivity.this, "Sign in successful", Toast.LENGTH_SHORT).show();
                        FirebaseUser user = task.getResult().getUser();
                        updateUI(user);
                    } else {
                        Toast.makeText(MainActivity.this, "Sign in failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateUI(FirebaseUser user) {
        if (user != null) {
            // Show a Toast message that user is logged in
            Toast.makeText(this, "User logged in: " + user.getPhoneNumber(), Toast.LENGTH_LONG).show();

            // Create an Intent to start WelcomeActivity
            Intent intent = new Intent(MainActivity.this, Welcome.class);
            // Pass the user's ID to the WelcomeActivity
            intent.putExtra("userId", user.getUid());
            startActivity(intent);
            finish(); // Optional: if you don't want to return to the MainActivity
        } else {
            // Show a Toast message that user is not logged in
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
        }
    }
}