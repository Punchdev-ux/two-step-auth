package com.example.two_step_auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore; // <-- IMPORT THIS

public class LoginActivity extends AppCompatActivity {
    TextInputEditText editTextEmail, editTextPassword;
    Button buttonLogin;
    FirebaseAuth mAuth;
    ProgressBar progressBar;
    TextView textView;

    @Override
    public void onStart() {
        super.onStart();
        mAuth = FirebaseAuth.getInstance(); // Initialize mAuth here
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // If a user is already logged in, we assume they have passed 2FA previously.
            // For simplicity in this flow, we go to MainActivity.
            // A more complex app might require re-verification after a certain time.
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            startActivity(intent);
            finish();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login); // Assuming your layout file is named login.xml

        // It's better to initialize mAuth in onCreate as well
        mAuth = FirebaseAuth.getInstance();
        editTextEmail = findViewById(R.id.email);
        editTextPassword = findViewById(R.id.password);
        buttonLogin = findViewById(R.id.btn_login);
        progressBar = findViewById(R.id.progressBar);
        textView = findViewById(R.id.registerNow);

        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), RegisterActivity.class);
                startActivity(intent);
                // Do not finish() here, so the user can press back to return to the login screen
            }
        });

        buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressBar.setVisibility(View.VISIBLE);
                String email, password;
                email = String.valueOf(editTextEmail.getText());
                password = String.valueOf(editTextPassword.getText());

                if (TextUtils.isEmpty(email)) {
                    Toast.makeText(LoginActivity.this, "Enter email", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                    return;
                }

                // *** BUG FIX: Was checking email twice, now checking password ***
                if (TextUtils.isEmpty(password)) {
                    Toast.makeText(LoginActivity.this, "Enter password", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                    return;
                }

                mAuth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                progressBar.setVisibility(View.GONE);
                                if (task.isSuccessful()) {
                                    // *** MODIFIED LOGIC STARTS HERE ***
                                    // Password authentication was successful. Now check for 2FA configuration.
                                    FirebaseUser user = mAuth.getCurrentUser();
                                    if (user != null) {
                                        check2FAConfiguration(user.getUid());
                                    }
                                } else {
                                    // If sign in fails, display a message to the user.
                                    Toast.makeText(LoginActivity.this, "Authentication failed. Check your credentials.",
                                            Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
            }
        });
    }

    /**
     * Checks Firestore to see if the user has already set up 2FA.
     * Redirects to the appropriate activity (Setup or Verify).
     * @param uid The user's unique ID from Firebase Authentication.
     */
    private void check2FAConfiguration(String uid) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(uid).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && task.getResult().contains("totpSecret")) {
                        // 2FA is configured for this user.
                        // Redirect to the verification screen.
                        Toast.makeText(LoginActivity.this, "Password correct. Please verify 2FA.", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(getApplicationContext(), Setup2FAActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        // 2FA is NOT configured for this user.
                        // Redirect to the setup screen.
                        Toast.makeText(LoginActivity.this, "Login successful. Please set up 2FA.", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(getApplicationContext(), Setup2FAActivity.class);
                        startActivity(intent);
                        finish();
                    }
                });
    }
}