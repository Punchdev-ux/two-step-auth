package com.example.two_step_auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    // --- Variables for Firebase Authentication ---
    private FirebaseAuth auth;
    private FirebaseUser user;
    private TextView userDetails;
    private Button logoutButton;

    // --- Variables for Calculator ---
    private TextView calculatorDisplay;
    private String currentNumber = "0";
    private String operator = "";
    private double firstNumber = 0;
    private boolean isNewOperation = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // --- Initialize Firebase Auth ---
        auth = FirebaseAuth.getInstance();

        // --- Initialize Views ---
        calculatorDisplay = findViewById(R.id.calculator_display);
        userDetails = findViewById(R.id.user_details);
        logoutButton = findViewById(R.id.logout); // Assuming your logout button has the id 'logout'

        // --- Setup Logout Button ---
        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                auth.signOut();
                // After signing out, redirect to the LoginActivity
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                startActivity(intent);
                finish(); // Close the MainActivity so the user can't go back to it
            }
        });

        // --- Setup the calculator functionality ---
        setupCalculator();
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
        }
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
        // Avoid calculation if operator is set twice
        if (!operator.isEmpty()) {
            calculateResult();
        }
        firstNumber = Double.parseDouble(currentNumber);
        operator = op;
        isNewOperation = true;
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