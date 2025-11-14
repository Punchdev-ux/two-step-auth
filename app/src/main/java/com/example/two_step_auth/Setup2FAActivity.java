package com.example.two_step_auth;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.util.HashMap;
import java.util.Map;

import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrDataFactory;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;

public class Setup2FAActivity extends AppCompatActivity {

    private ImageView qrCodeImageView;
    private TextView secretTextView;
    private EditText verificationCodeEditText;
    private Button confirmButton;
    private ProgressBar progressBar;

    private String secret; // To hold the generated secret

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup_2fa);

        // Initialize UI components
        qrCodeImageView = findViewById(R.id.qr_code_image);
        secretTextView = findViewById(R.id.text_secret_key);
        verificationCodeEditText = findViewById(R.id.edit_text_verification_code);
        confirmButton = findViewById(R.id.button_confirm_2fa);
        progressBar = findViewById(R.id.progressBar_2fa);

        // Generate and display the 2FA secret and QR code
        setup2FA();

        // Set up the listener for the confirmation button
        confirmButton.setOnClickListener(v -> {
            String code = verificationCodeEditText.getText().toString().trim();
            if (code.isEmpty()) {
                Toast.makeText(this, "Please enter the verification code.", Toast.LENGTH_SHORT).show();
                return;
            }
            // Verify the code and save the secret if it's correct
            verifyCodeAndCompleteSetup(code);
        });
    }

    private void setup2FA() {
        // 1. Generate a new secret key for the user
        SecretGenerator secretGenerator = new DefaultSecretGenerator(64);
        secret = secretGenerator.generate();

        // Display the secret key for manual entry
        secretTextView.setText(secret);

        // 2. Create QR code data
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String userEmail = (user != null && user.getEmail() != null) ? user.getEmail() : "user@example.com";

        QrData data = new QrData.Builder()
                .label(userEmail)
                .secret(secret)
                .issuer("Two-Step Auth App")
                .algorithm(HashingAlgorithm.SHA1) // Must match the verifier
                .digits(6)
                .period(30)
                .build();

        // 3. Generate a QR code image from the data
        try {
            // Using the zxing-android-embedded library's BarcodeEncoder for simplicity
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            Bitmap bitmap = barcodeEncoder.encodeBitmap(data.getUri(), BarcodeFormat.QR_CODE, 400, 400);
            qrCodeImageView.setImageBitmap(bitmap);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error generating QR code.", Toast.LENGTH_SHORT).show();
        }
    }

    private void verifyCodeAndCompleteSetup(String code) {
        progressBar.setVisibility(View.VISIBLE);
        confirmButton.setEnabled(false);

        // Use the same parameters as the QR code to verify
        TimeProvider timeProvider = new SystemTimeProvider();
        CodeVerifier verifier = new DefaultCodeVerifier(new DefaultCodeGenerator(HashingAlgorithm.SHA1), timeProvider);

        // The secret is the one we generated earlier in setup2FA()
        boolean isSuccessful = verifier.isValidCode(secret, code);

        if (isSuccessful) {
            // If the code is correct, save the secret to the user's document in Firestore
            saveSecretToFirestore();
        } else {
            // If the code is incorrect, inform the user
            progressBar.setVisibility(View.GONE);
            confirmButton.setEnabled(true);
            Toast.makeText(this, "Invalid code. Please try again.", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveSecretToFirestore() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Error: No user logged in.", Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.GONE);
            confirmButton.setEnabled(true);
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String uid = user.getUid();

        // Create a map to store the user's 2FA secret
        Map<String, Object> userData = new HashMap<>();
        userData.put("totpSecret", secret);

        // Save the secret to a 'users' collection, with the document ID being the user's UID
        db.collection("users").document(uid)
                .set(userData)
                .addOnSuccessListener(aVoid -> {
                    // Success!
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(Setup2FAActivity.this, "2FA Setup Successful!", Toast.LENGTH_SHORT).show();

                    // Navigate to the main activity, as the user is now fully authenticated
                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                    // Clear the activity stack so the user can't go back to the setup screen
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    // Failure
                    progressBar.setVisibility(View.GONE);
                    confirmButton.setEnabled(true);
                    Toast.makeText(Setup2FAActivity.this, "Failed to save 2FA settings. Please try again.", Toast.LENGTH_SHORT).show();
                });
    }
}