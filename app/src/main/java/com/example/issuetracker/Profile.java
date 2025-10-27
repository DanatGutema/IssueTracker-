package com.example.issuetracker;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import android.graphics.Color;

public class Profile extends AppCompatActivity {

    private TextView tvFullName, tvEmail, tvDepartment, tvSection, tvRole, tvPhone;
    private Button btnChangePassword;

    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        tvFullName = findViewById(R.id.tvFullName);
        tvEmail = findViewById(R.id.tvEmail);
        tvDepartment = findViewById(R.id.tvDepartment);
        tvSection = findViewById(R.id.tvSection);
        tvRole = findViewById(R.id.tvRole);
        tvPhone = findViewById(R.id.tvPhone);
        btnChangePassword = findViewById(R.id.btnChangePassword);

        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        currentUser = auth.getCurrentUser();

        if (currentUser != null) {
            loadUserData();
        }

        btnChangePassword.setOnClickListener(v -> showChangePasswordDialog());
    }

    private void loadUserData() {
        String uid = currentUser.getUid();
        DocumentReference docRef = firestore.collection("users").document(uid);

        docRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                tvFullName.setText("Full Name:  " + documentSnapshot.getString("fullName"));
                tvEmail.setText("Email:   " + currentUser.getEmail());
                tvDepartment.setText("Department:   " + documentSnapshot.getString("dept"));
                tvSection.setText("Section:   " + documentSnapshot.getString("section"));
                tvRole.setText("Role:   " + documentSnapshot.getString("role"));
                tvPhone.setText("Phone:   " + documentSnapshot.getString("phone"));
            }
        }).addOnFailureListener(e ->
                Toast.makeText(Profile.this, "Failed to load user data", Toast.LENGTH_SHORT).show()
        );
    }

    private void showChangePasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomAlertDialog);
        builder.setTitle("Change Password");

        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_change_password, null);
        builder.setView(dialogView);

        final EditText etCurrentPassword = dialogView.findViewById(R.id.etCurrentPassword);
        final EditText etNewPassword = dialogView.findViewById(R.id.etNewPassword);
        final EditText etConfirmPassword = dialogView.findViewById(R.id.etConfirmPassword);
        final TextView tvError = dialogView.findViewById(R.id.tvError);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        Button btnSave = dialogView.findViewById(R.id.btnSave);

        // ✅ Force text color + hint color in Java
        etCurrentPassword.setTextColor(getResources().getColor(android.R.color.black));
        etCurrentPassword.setHintTextColor(Color.GRAY);

        etNewPassword.setTextColor(getResources().getColor(android.R.color.black));
        etNewPassword.setHintTextColor(Color.GRAY);

        etConfirmPassword.setTextColor(getResources().getColor(android.R.color.black));
        etConfirmPassword.setHintTextColor(Color.GRAY);

        AlertDialog dialog = builder.create();
        dialog.show();

        // Set background color
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.white);
        }

        // Error text style
        tvError.setTextColor(Color.RED);

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String currentPass = etCurrentPassword.getText().toString().trim();
            String newPass = etNewPassword.getText().toString().trim();
            String confirmPass = etConfirmPassword.getText().toString().trim();

            tvError.setVisibility(View.GONE);

            if (TextUtils.isEmpty(currentPass) || TextUtils.isEmpty(newPass) || TextUtils.isEmpty(confirmPass)) {
                tvError.setText("Please fill all fields");
                tvError.setVisibility(View.VISIBLE);
                return;
            }

            if (!newPass.equals(confirmPass)) {
                tvError.setText("New passwords do not match");
                tvError.setVisibility(View.VISIBLE);
                return;
            }

            // Re-authenticate
            if (currentUser != null && currentUser.getEmail() != null) {
                AuthCredential credential = EmailAuthProvider.getCredential(currentUser.getEmail(), currentPass);
                currentUser.reauthenticate(credential)
                        .addOnSuccessListener(unused -> {
                            currentUser.updatePassword(newPass)
                                    .addOnSuccessListener(aVoid -> {
                                        tvError.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                                        tvError.setText("Password changed successfully");
                                        tvError.setVisibility(View.VISIBLE);

                                        // Close dialog automatically after 2 seconds
                                        new android.os.Handler().postDelayed(() -> dialog.dismiss(), 1000);
                                    })
                                    .addOnFailureListener(e -> {
                                        tvError.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                                        tvError.setText("Failed to update: " + e.getMessage());
                                        tvError.setVisibility(View.VISIBLE);
                                    });
                        })
                        .addOnFailureListener(e -> {
                            tvError.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                            tvError.setText("Incorrect current password");
                            tvError.setVisibility(View.VISIBLE);
                        });
            }
        });
    }


    private void reauthenticateAndChangePassword(String currentPassword, String newPassword) {
        if (currentUser != null && currentUser.getEmail() != null) {
            AuthCredential credential = EmailAuthProvider.getCredential(currentUser.getEmail(), currentPassword);

            currentUser.reauthenticate(credential)
                    .addOnSuccessListener(unused -> {
                        currentUser.updatePassword(newPassword)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(Profile.this, "Password updated successfully", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(Profile.this, "Failed to update: " + e.getMessage(), Toast.LENGTH_LONG).show()
                                );
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(Profile.this, "Re-authentication failed: " + e.getMessage(), Toast.LENGTH_LONG).show()
                    );
        }
    }
}
