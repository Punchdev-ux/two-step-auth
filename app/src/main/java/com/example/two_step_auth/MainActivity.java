package com.example.two_step_auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.Random; // Using Random for OTP generation

public class MainActivity extends AppCompatActivity {

    // --- Variables from Calculator & Auth ---
    private FirebaseAuth auth;
    private FirebaseUser user;
    private TextView userDetails;
    private Button logoutButton;

    // --- Variables from OTP Sender ---
    private EditText emailFieldForOtp; // Renamed to avoid confusion
    private Button sendOtpBtn;
    private FirebaseFirestore db;

    // --- Variables for Calculator ---
    private TextView calculatorDisplay;
    private String currentNumber = "0";
    private String operator = "";
    private double firstNumber = 0;
    private boolean isNewOperation = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // --- Initialize Firebase Services ---
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // --- Initialize Views from both files ---
        // Auth Views
        userDetails = findViewById(R.id.user_details);
        logoutButton = findViewById(R.id.logout);

        // OTP Sender Views
        emailFieldForOtp = findViewById(R.id.emailField); // Assumes this ID exists in your layout
        sendOtpBtn = findViewById(R.id.sendOtpBtn);       // Assumes this ID exists in your layout

        // Calculator Views
        calculatorDisplay = findViewById(R.id.calculator_display);

        // --- Setup Listeners ---
        setupLogoutButton();
        setupOtpSender();
        setupCalculator();

        // --- Handle Window Insets (from OTP file) ---
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        user = auth.getCurrentUser();
        if (user == null) {
            // If no user is logged in, redirect to the LoginActivity
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
            finish(); // Close this activity
        } else {
            // If a user is logged in, display their email
            userDetails.setText(user.getEmail());
            // Pre-fill the OTP email field with the current user's email
            emailFieldForOtp.setText(user.getEmail());
        }
    }

    private void setupLogoutButton() {
        logoutButton.setOnClickListener(v -> {
            auth.signOut();
            // After signing out, redirect to the LoginActivity
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
            finish(); // Close the MainActivity so the user can't go back to it
        });
    }

    private void setupOtpSender() {
        sendOtpBtn.setOnClickListener(v -> {
            String email = emailFieldForOtp.getText().toString().trim();

            if (email.isEmpty()) {
                Toast.makeText(this, "Please enter an email", Toast.LENGTH_SHORT).show();
                return;
            }

            // Generate OTP (using a simple Random for this example)
            String otp = String.valueOf(new Random().nextInt(900000) + 100000);
            long expiryTime = System.currentTimeMillis() + (2 * 60 * 1000); // 2 mins expiry

            // Send OTP via email in a background thread
            new Thread(() -> EmailSender.sendEmail(email, otp)).start();

            // Save OTP to Firestore
            Map<String, Object> otpData = new HashMap<>();
            otpData.put("otp", otp);
            otpData.put("expiry", expiryTime);
            otpData.put("used", false);

            db.collection("otps").document(email).set(otpData)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "OTP sent to " + email, Toast.LENGTH_SHORT).show();
                        // Optionally, navigate to a verification screen
                        // Intent intent = new Intent(MainActivity.this, VerifyOtpActivity.class);
                        // intent.putExtra("email", email);
                        // startActivity(intent);
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to store OTP: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });
    }

    private void setupCalculator() {
        // Set initial display
        calculatorDisplay.setText(currentNumber);

        // Number buttons
        int[] numberButtons = {R.id.btn_0, R.id.btn_1, R.id.btn_2, R.id.btn_3,
                R.id.btn_4, R.id.btn_5, R.id.btn_6, R.id.btn_7,
                R.id.btn_8, R.id.btn_9};

        View.OnClickListener numberListener = v -> {
            Button button = (Button) v;
            if (isNewOperation) {
                currentNumber = button.getText().toString();
                isNewOperation = false;
            } else {
                if (currentNumber.equals("0")) {
                    currentNumber = button.getText().toString();
                } else {
                    currentNumber += button.getText().toString();
                }
            }
            calculatorDisplay.setText(currentNumber);
        };

        for (int id : numberButtons) {
            findViewById(id).setOnClickListener(numberListener);
        }

        // Decimal button
        findViewById(R.id.btn_decimal).setOnClickListener(v -> {
            if (!currentNumber.contains(".")) {
                currentNumber += ".";
                calculatorDisplay.setText(currentNumber);
            }
        });

        // Operation buttons
        findViewById(R.id.btn_add).setOnClickListener(v -> setOperation("+"));
        findViewById(R.id.btn_subtract).setOnClickListener(v -> setOperation("-"));
        findViewById(R.id.btn_multiply).setOnClickListener(v -> setOperation("×"));
        findViewById(R.id.btn_divide).setOnClickListener(v -> setOperation("÷"));

        // Clear button
        findViewById(R.id.btn_clear).setOnClickListener(v -> {
            currentNumber = "0";
            operator = "";
            firstNumber = 0;
            isNewOperation = true;
            calculatorDisplay.setText(currentNumber);
        });

        // Equals button
        findViewById(R.id.btn_equals).setOnClickListener(v -> calculateResult());
    }

    private void setOperation(String op) {
        if (operator.isEmpty() && !isNewOperation) {
            firstNumber = Double.parseDouble(currentNumber);
            operator = op;
            isNewOperation = true;
        } else if (!operator.isEmpty()) {
            calculateResult();
            firstNumber = Double.parseDouble(currentNumber);
            operator = op;
            isNewOperation = true;
        } else {
            firstNumber = Double.parseDouble(currentNumber);
            operator = op;
            isNewOperation = true;
        }
    }

    private void calculateResult() {
        if (operator.isEmpty() || isNewOperation) return;

        double secondNumber = Double.parseDouble(currentNumber);
        double result = 0;

        switch (operator) {
            case "+":
                result = firstNumber + secondNumber;
                break;
            case "-":
                result = firstNumber - secondNumber;
                break;
            case "×":
                result = firstNumber * secondNumber;
                break;
            case "÷":
                if (secondNumber != 0) {
                    result = firstNumber / secondNumber;
                } else {
                    Toast.makeText(this, "Cannot divide by zero", Toast.LENGTH_SHORT).show();
                    calculatorDisplay.setText("Error");
                    currentNumber = "0";
                    operator = "";
                    isNewOperation = true;
                    return;
                }
                break;
        }

        // Format the result to remove ".0" for whole numbers
        if (result == (long) result) {
            currentNumber = String.format("%d", (long) result);
        } else {
            currentNumber = String.format("%s", result);
        }

        calculatorDisplay.setText(currentNumber);
        operator = "";
        isNewOperation = true;
    }
}