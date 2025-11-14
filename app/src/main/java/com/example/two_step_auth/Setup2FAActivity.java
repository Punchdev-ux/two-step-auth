package com.example.two_step_auth;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

public class Setup2FAActivity extends AppCompatActivity {

    private EditText otpField;
    private Button verifyBtn;
    private FirebaseFirestore db;
    private String email;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup_2fa);

        db = FirebaseFirestore.getInstance();
        otpField = findViewById(R.id.otpField);
        verifyBtn = findViewById(R.id.verifyBtn);
        email = getIntent().getStringExtra("email");

        verifyBtn.setOnClickListener(v -> {
            String enteredOtp = otpField.getText().toString().trim();

            if (enteredOtp.isEmpty()) {
                Toast.makeText(this, "Please enter OTP", Toast.LENGTH_SHORT).show();
                return;
            }

            db.collection("otps").document(email).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String storedOtp = documentSnapshot.getString("otp");
                            long expiry = documentSnapshot.getLong("expiry");
                            boolean used = documentSnapshot.getBoolean("used");

                            if (used) {
                                Toast.makeText(this, "OTP already used.", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            long now = System.currentTimeMillis();
                            if (now > expiry) {
                                Toast.makeText(this, "OTP expired!", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            if (enteredOtp.equals(storedOtp)) {
                                Toast.makeText(this, "OTP Verified Successfully!", Toast.LENGTH_LONG).show();

                                // Mark as used
                                db.collection("otps").document(email).update("used", true);
                            } else {
                                Toast.makeText(this, "Invalid OTP!", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(this, "No OTP found for this email.", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Verification failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });
    }
}