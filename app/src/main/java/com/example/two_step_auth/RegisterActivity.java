package com.example.two_step_auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

// ADDED: Import the Random class to generate an OTP
import java.util.Random;

public class RegisterActivity extends AppCompatActivity {

    TextInputEditText editTextEmail, editTextPassword;
    Button buttonReg;
    FirebaseAuth mAuth;
    ProgressBar progressBar;
    TextView textView;

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(currentUser != null){
            // MODIFIED: Changed this to MainActivity to avoid a loop if already logged in
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            startActivity(intent);
            finish();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.register);
        mAuth= FirebaseAuth.getInstance();
        editTextEmail = findViewById(R.id.email);
        editTextPassword = findViewById(R.id.password);
        buttonReg = findViewById(R.id.btn_REGISTER);
        progressBar = findViewById(R.id.progressBar);
        textView = findViewById(R.id.loginNow);
        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                startActivity(intent);
                finish();
            }
        });

        buttonReg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                progressBar.setVisibility(View.VISIBLE);
                String email, password;
                email = String.valueOf(editTextEmail.getText());
                password = String.valueOf(editTextPassword.getText());

                if (TextUtils.isEmpty(email)) {
                    Toast.makeText( RegisterActivity.this,  "Enter email", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE); // ADDED: Hide progress bar on error
                    return;
                }

                // MODIFIED: Corrected the check to be for the password field
                if (TextUtils.isEmpty(password)) {
                    Toast.makeText( RegisterActivity.this,  "Enter password", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE); // ADDED: Hide progress bar on error
                    return;
                }

                mAuth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener( new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                progressBar.setVisibility(View.GONE);
                                if (task.isSuccessful()) {
                                    Toast.makeText(RegisterActivity.this, "Account Created. Sending verification OTP.",
                                            Toast.LENGTH_LONG).show(); // MODIFIED: Longer toast message

                                    // --- START OF ADDED CODE ---

                                    // 1. Get the user's email from the input field
                                    String userEmail = email;

                                    // 2. Generate a 6-digit random OTP
                                    String generatedOtp = String.valueOf(new Random().nextInt(900000) + 100000);
                                    Log.d("OTP_GENERATED", "The OTP is: " + generatedOtp); // For debugging

                                    // 3. Send the email in a background thread to avoid crashing the app
                                    new Thread(new Runnable() {
                                        @Override
                                        public void run() {
                                            // Make sure your EmailSender class is in the correct package
                                            // For example: com.example.two_step_auth.EmailSender
                                            EmailSender.sendEmail(userEmail, generatedOtp);
                                        }
                                    }).start();

                                    // 4. Navigate to the OTP verification screen (Setup2FAActivity)
                                    Intent intent = new Intent(getApplicationContext(), Setup2FAActivity.class);
                                    // Pass the email and the generated OTP to the next activity
                                    intent.putExtra("USER_EMAIL", userEmail);
                                    intent.putExtra("GENERATED_OTP", generatedOtp);
                                    startActivity(intent);
                                    finish();

                                    // --- END OF ADDED CODE ---

                                } else {
                                    // If sign in fails, display a message to the user.
                                    Toast.makeText(RegisterActivity.this, "Authentication failed: " + task.getException().getMessage(),
                                            Toast.LENGTH_LONG).show(); // MODIFIED: Show detailed error
                                }
                            }
                        });
            }
        });
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}