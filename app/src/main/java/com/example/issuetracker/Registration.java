package com.example.issuetracker;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;
import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.util.Patterns;

import java.util.Arrays;
import java.util.List;
import java.util.regex.*;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;
import com.google.firebase.firestore.FieldValue;


import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import com.google.firebase.auth.FirebaseUser;
import android.widget.ArrayAdapter;

/*
public class Registration extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_registration);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}
*/

public class Registration extends AppCompatActivity {

    EditText mFullName, mEmail, mPassword, mPhone, mConfirmPassword;

    Button mRegisterBtn;

    TextView mLoginBtn;

    FirebaseAuth fAuth;

    //ProgressBar progressBar;
    Spinner roleSpinner, deptSpinner, meditTextTextSection;


   // @Override

    FirebaseAuth mAuth;
    FirebaseFirestore db;
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_registration);
        mAuth = FirebaseAuth.getInstance();
        //initialize the firestore
        db = FirebaseFirestore.getInstance();

        mFullName = findViewById(R.id.fullName);

        mEmail = findViewById(R.id.editTextTextEmailAddress);

        mPassword = findViewById(R.id.editTextTextPassword);

        mPhone = findViewById(R.id.editTextPhone);
        mConfirmPassword = findViewById(R.id.editTextTextPassword2);
        roleSpinner = findViewById(R.id.roleSpinner);
        deptSpinner = findViewById(R.id.deptSpinner);
        meditTextTextSection = findViewById(R.id.editTextTextSection);
        mRegisterBtn = findViewById(R.id.registerBtn);



        // Load arrays from resources
        List<String> sectionType = Arrays.asList(getResources().getStringArray(R.array.section));
        List<String> deptType = Arrays.asList(getResources().getStringArray(R.array.dept));
        List<String> roleType = Arrays.asList(getResources().getStringArray(R.array.roles));



        // sectionTypee Spinner
        ArrayAdapter<String> sectionTypeAdapter = new ArrayAdapter<>(
                this,
                R.layout.spinner_item, // selected item layout
                sectionType
        );
        sectionTypeAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        meditTextTextSection.setAdapter(sectionTypeAdapter);


        //deptType spinner
        ArrayAdapter<String> deptTypeAdapter = new ArrayAdapter<>(
                this,
                R.layout.spinner_item, // selected item layout
                deptType
        );
        deptTypeAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        deptSpinner.setAdapter(deptTypeAdapter);


        //roleType spinner
        ArrayAdapter<String> roleTypeAdapter = new ArrayAdapter<>(
                this,
                R.layout.spinner_item, // selected item layout
                roleType
        );
        roleTypeAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        roleSpinner.setAdapter(roleTypeAdapter);



        //mLoginBtn = findViewById(R.id.createText);

        fAuth = FirebaseAuth.getInstance();

        //progressBar = findViewById(R.id.progressBar);

        if(fAuth.getCurrentUser() != null){
            startActivity(new Intent(getApplicationContext(), MainActivity.class));
            finish();
        }

        mRegisterBtn.setOnClickListener(v -> {
            String email = mEmail.getText().toString().trim();
            String password = mPassword.getText().toString().trim();
            String fullName = mFullName.getText().toString().trim();
            String phone = mPhone.getText().toString().trim();
            String role = roleSpinner.getSelectedItem().toString();
            String dept = deptSpinner.getSelectedItem().toString();
            String section = meditTextTextSection.getSelectedItem().toString();
            String confirmPassword = mConfirmPassword.getText().toString().trim();


            // Basic validation

            boolean hasError = false;
            //full name validation
            if (TextUtils.isEmpty(fullName) || fullName.length() < 3 || !fullName.matches("[a-zA-Z ]+")) {
                mFullName.setError("Name must be at least 3 alphabets.");
                mFullName.requestFocus(); // force focus so error text shows
                hasError = true;
                //return;
            }
            // email validation
            if (TextUtils.isEmpty(email)) {
                mEmail.setError("Email is required.");
                hasError = true;
                //return;
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                mEmail.setError("Enter a valid email.");
                hasError = true;
                //return;
            }
            //phone validation
            if (!phone.matches("09\\d{8}")) {
                mPhone.setError("Phone must start with 09 and be 10 digits.");
                hasError = true;
                //return;
            }
            //password validation
            /*
            if (TextUtils.isEmpty(password)) {
                mPassword.setError("Password is required.");
                return;
            }
            if (password.length() < 6) {
                mPassword.setError("Password must be at least 6 characters.");
                return;
            }
             */
            // 🔹 Password validation (Strong password)
            Pattern PASSWORD_PATTERN =
                    Pattern.compile("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#^$!%*?&])[A-Za-z\\d@#^$!%*?&]{6,}$");

            if (!PASSWORD_PATTERN.matcher(password).matches()) {
                mPassword.setError("Password must contain 6+ chars, upper, lower, digit & special char.");
                hasError = true;
                //return;
            }
            // Confirm password check
            if (!password.equals(confirmPassword)) {
                mConfirmPassword.setError("Passwords do not match.");
                hasError = true;
               // return;
            }

            // Role validation (optional)
            if (role.equals("Select Role")) {
                Toast.makeText(this, "Please select a role", Toast.LENGTH_SHORT).show();
                hasError = true;
            }

            // Stop registration if any error
            if (hasError) {
                return;
            }
            //dept validation
            if (dept.equals("Select Department")) {
                Toast.makeText(this, "Please select a department", Toast.LENGTH_SHORT).show();
                hasError = true;
            }
            // Stop registration if any error
            if (hasError) {
                return;
            }
            //section validation
            if (section.equals("Select Section")) {
                Toast.makeText(this, "Please select a section", Toast.LENGTH_SHORT).show();
                hasError = true;
            }
            // Stop registration if any error
            if (hasError) {
                return;
            }


            // Show progress bar
           // progressBar.setVisibility(View.VISIBLE);

            // Register user in Firebase

            fAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            //Toast.makeText(Registration.this, "User Created.", Toast.LENGTH_SHORT).show();
                            //startActivity(new Intent(getApplicationContext(), Login.class));
                            // Get current user ID
                            String userId = fAuth.getCurrentUser().getUid();

                            // Create a HashMap with user details
                            Map<String, Object> user = new HashMap<>();
                            user.put("fullName", fullName);
                            user.put("email", email);
                            user.put("phone", phone);
                            user.put("dept", dept);
                            user.put("section", section);
                            user.put("role", role);
                            user.put("status", "pending"); // <-- NEW
                            user.put("timestamp", FieldValue.serverTimestamp()); // optional

                            //store in firebase
                            db.collection("users").document(userId)
                                    .set(user)
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(Registration.this, "User registered successfully!", Toast.LENGTH_SHORT).show();
                                        startActivity(new Intent(getApplicationContext(), Login.class));
                                        finish();
                                    })
                                   .addOnFailureListener(e -> {
                                        Toast.makeText(Registration.this, "Error saving user info: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                   });

                        } else {
                            Toast.makeText(Registration.this, "Error ! " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                       // progressBar.setVisibility(View.GONE);
                    });


        });

    }
}