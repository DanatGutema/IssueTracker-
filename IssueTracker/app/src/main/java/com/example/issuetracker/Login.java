package com.example.issuetracker;
import com.example.issuetracker.Registration;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.issuetracker.databinding.ActivityMainBinding;
import com.google.firebase.auth.FirebaseAuth;

import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;
import com.google.firebase.firestore.FieldValue;

//for the check box
import android.content.SharedPreferences;
import android.widget.CheckBox;
//for removing highlight
import android.os.Build;
import android.graphics.Color;


public class Login extends AppCompatActivity {

    EditText mEmail, mPassword;
    Button mLoginBtn;
    TextView mCreateBtn, mforgotPassword;
    FirebaseAuth fAuth;
    FirebaseFirestore db;
    //for check box
    CheckBox rememberMeCheckBox;
    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;


    //FirebaseAuth mAuth = FirebaseAuth.getInstance();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Match with XML IDs
        mEmail = findViewById(R.id.editTextTextEmailAddress);
        mPassword = findViewById(R.id.editTextTextPassword);
        mLoginBtn = findViewById(R.id.button4);
        mCreateBtn = findViewById(R.id.textView6);
        rememberMeCheckBox = findViewById(R.id.remember);

        mforgotPassword = findViewById(R.id.forgotPassword);

        //forgot password
        mforgotPassword.setOnClickListener(v -> {
            startActivity(new Intent(Login.this, ForgotPassword.class));
        });


        // Remove yellow autofill highlight
        /*
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mEmail.setAutofillHighlightColor(Color.TRANSPARENT);
            mPassword.setAutofillHighlightColor(Color.TRANSPARENT);
            android:importantForAutofill="noExcludeDescendants"
        }*/


        // FirebaseAuth fAuth;
        fAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // fAuth = FirebaseAuth.getInstance();
        sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        editor = sharedPreferences.edit();

        // 🔹 Auto-login check
        boolean isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false);
        if (isLoggedIn) {
            String role = sharedPreferences.getString("role", "");
            redirectToDashboard(role);
        }
        // Login button logic
        mLoginBtn.setOnClickListener(v -> {
            String email = mEmail.getText().toString().trim();
            String password = mPassword.getText().toString().trim();

            boolean hasError = false;

            if (TextUtils.isEmpty(email)) {
                mEmail.setError("Email is required");
                mEmail.requestFocus(); // force focus so error text shows
                hasError = true;
                // return;
            }
            if (TextUtils.isEmpty(password)) {
                mPassword.setError("Password is required");
                //  mPassword.requestFocus();
                hasError = true;
                //return;
            }

            if (hasError) {
                return; // stop login if there’s any error
            }

            // Firebase login
            fAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            String userId = fAuth.getCurrentUser().getUid();

                            // Fetch user document from Firestore
                            db.collection("users").document(userId).get()
                                    .addOnSuccessListener(documentSnapshot -> {
                                        if (documentSnapshot.exists()) {
                                            String role = documentSnapshot.getString("role");

                                            if (role != null) {
                                                if (rememberMeCheckBox.isChecked()) {
                                                    // 🔹 Save login state
                                                    editor.putBoolean("isLoggedIn", true);
                                                    editor.putString("role", role);
                                                    editor.apply();
                                                }
                                                redirectToDashboard(role);
                                            } else {
                                                Toast.makeText(Login.this, "Role not found!", Toast.LENGTH_SHORT).show();
                                            }
                                        } else {
                                            Toast.makeText(Login.this, "User data not found", Toast.LENGTH_SHORT).show();
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(Login.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    });
                        } else {
                            Toast.makeText(Login.this, "Login failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        //link go to registration
        mCreateBtn.setOnClickListener(v -> {
            Intent intent = new Intent(Login.this, Registration.class);
            startActivity(intent);
        });

    }

        // "Create Account" link → Go to Registration
        //mCreateBtn.setOnClickListener(v -> {
        //  startActivity(new Intent(getApplicationContext(), Registration.class));
        //});

        // 🔹 Helper function to redirect based on role
        private void redirectToDashboard(String role) {
            if (role == null) return;

            switch (role.toLowerCase()) {
                case "admin":
                    startActivity(new Intent(Login.this, AdminDashboard.class));
                    break;
                case "manager":
                    startActivity(new Intent(Login.this, Dashboard.class));
                    break;
                case "employee":
                    startActivity(new Intent(Login.this, EmployeeDashboard.class));
                    break;
                default:
                    Toast.makeText(Login.this, "Unknown role", Toast.LENGTH_SHORT).show();
            }
            finish();
        }
    }