package com.example.issuetracker;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import com.google.firebase.firestore.FieldValue;
import java.util.HashMap;
import java.util.Map;
import com.google.firebase.auth.FirebaseAuth;
//import for the calendar options
import java.util.Calendar;
import android.app.DatePickerDialog;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.util.Log;


import com.google.firebase.firestore.FirebaseFirestore;

public class IssueList extends AppCompatActivity {
    EditText mSerialNumber, mIssueTitle, mIssueDes, mBankName, mBankLoc, mRegDate, mSupportEngineer;
    Button mButtonAdd;
    Spinner StatusSpinner, prioritySpinner, TypeSpinner, MachineTypeSpinner;
    FirebaseFirestore db;
    FirebaseAuth mAuth = FirebaseAuth.getInstance();
    String userId;
    private boolean serialReady = false;  // guard so we don’t submit before serial is loaded

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_issue_list);

        db = FirebaseFirestore.getInstance();
        //ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
        //  Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
        //v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
        // return insets;
        //});


        mSerialNumber = findViewById(R.id.SerialNumber);
        userId = mAuth.getCurrentUser().getUid();
        mIssueTitle = findViewById(R.id.IssueTitle);

        mIssueDes = findViewById(R.id.IssueDes);
       // mYourName = findViewById(R.id.YourName);
        mBankName = findViewById(R.id.BankName);
        mBankLoc = findViewById(R.id.BankLoc);
        mSupportEngineer = findViewById(R.id.SupportEngineer);
        mRegDate = findViewById(R.id.RegDate);
        StatusSpinner = findViewById(R.id.StatusSpinner);
        prioritySpinner = findViewById(R.id.prioritySpinner);
        TypeSpinner = findViewById(R.id.TypeSpinner);
        MachineTypeSpinner = findViewById(R.id.MachineTypeSpinner);
        mButtonAdd = findViewById(R.id.ButtonAdd);
        EditText regDate = findViewById(R.id.RegDate);

        // Make serial read-only so users don’t edit it
        mSerialNumber.setKeyListener(null);
        // Safely get the logged-in user
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "You’re not signed in.", Toast.LENGTH_SHORT).show();
            finish(); // or navigate to login
            return;
        }
        userId = mAuth.getCurrentUser().getUid();

        // 1) Generate the next serial as soon as the screen opens
        generateNextSerialNumber();

        regDate.setOnClickListener(v -> {
            final Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    IssueList.this,
                    (view, selectedYear, selectedMonth, selectedDay) -> {
                        // Format date as needed
                        String date = selectedDay + "/" + (selectedMonth + 1) + "/" + selectedYear;
                        regDate.setText(date);
                    },
                    year, month, day
            );
            datePickerDialog.show();
        });

        mButtonAdd.setOnClickListener(v -> {
            String Number = mSerialNumber.getText().toString().trim();
            String Title = mIssueTitle.getText().toString().trim();
            String IssueDes = mIssueDes.getText().toString().trim();
           // String Name = mYourName.getText().toString().trim();
            String BankName = mBankName.getText().toString().trim();
            String BankLoc = mBankLoc.getText().toString().trim();
            String RegDate = mRegDate.getText().toString().trim();
            String SupportEngineer = mSupportEngineer.getText().toString().trim();
            String Status = StatusSpinner.getSelectedItem().toString();
            String Priority = prioritySpinner.getSelectedItem().toString();
            String Type = TypeSpinner.getSelectedItem().toString();
            String MachineType = MachineTypeSpinner.getSelectedItem().toString();

            // 🔹 Validation: Check all fields
            if (!serialReady || Number.isEmpty()) {
                Toast.makeText(IssueList.this, "Please wait… generating number.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (Title.isEmpty()) {
                mIssueTitle.setError("Title is required");
                mIssueTitle.requestFocus();
                return;
            }
            if (IssueDes.isEmpty()) {
                mIssueDes.setError("Description is required");
                mIssueDes.requestFocus();
                return;
            }
            if (BankName.isEmpty()) {
                mBankName.setError("Bank Name is required");
                mBankName.requestFocus();
                return;
            }
            if (BankLoc.isEmpty()) {
                mBankLoc.setError("Bank Location is required");
                mBankLoc.requestFocus();
                return;
            }
            if (SupportEngineer.isEmpty()) {
                mSupportEngineer.setError("Support Engineer is required");
                mSupportEngineer.requestFocus();
                return;
            }
            if (RegDate.isEmpty()) {
                mRegDate.setError("Registration Date is required");
                mRegDate.requestFocus();
                return;
            }
            if (Status.equals("Select The Current Status of The Issue")) {
                Toast.makeText(IssueList.this, "Please select a status", Toast.LENGTH_SHORT).show();
                return;
            }
            if (Priority.equals("Select Priority Level")) { // Assuming first item is "Select Priority"
                Toast.makeText(IssueList.this, "Please select a priority", Toast.LENGTH_SHORT).show();
                return;
            }
            if (Type.equals("Select Type")) { // Assuming first item is "Select Type"
                Toast.makeText(IssueList.this, "Please select a type", Toast.LENGTH_SHORT).show();
                return;
            }
            if (MachineType.equals("Select Machine Type")) { // Assuming first item is "Select Machine Type"
                Toast.makeText(IssueList.this, "Please select a machine type", Toast.LENGTH_SHORT).show();
                return;
            }
            // 🔹 Fetch the fullName from "users" collection before saving issue
            db.collection("users").document(userId).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String fullName = documentSnapshot.getString("fullName");
                            String dept = documentSnapshot.getString("dept");
                            String section = documentSnapshot.getString("section");
                            String role = documentSnapshot.getString("role");

            // 🔹 Build a map of issue data
            Map<String, Object> issue = new HashMap<>();
            issue.put("Number", Number);
            issue.put("title", Title);
            issue.put("description", IssueDes);
            //issue.put("yourName", Name);
            issue.put("bankName",BankName);
            issue.put("bankLocation",BankLoc);
            issue.put("registrationDate",RegDate);
            issue.put("status", Status);
            issue.put("priority", Priority);
            issue.put("SupportEngineer", SupportEngineer);
            issue.put("type", Type);
            issue.put("machineType", MachineType);
            issue.put("timestamp", FieldValue.serverTimestamp()); // optional, auto-time
            issue.put("createdBy", FirebaseAuth.getInstance().getCurrentUser().getUid());

            // add extra fields from user profile
                            issue.put("fullName", fullName);
            issue.put("dept", dept);
            issue.put("section", section);
            issue.put("role", role);

            //Add to firestore database. (auto ID)
            db.collection("issues")
                    .add(issue)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(IssueList.this, "Issue added successfully!", Toast.LENGTH_SHORT).show();
                        //resetForm();

                        // Clear all EditTexts
                        mSerialNumber.setText("");
                        mIssueTitle.setText("");
                        mIssueDes.setText("");
                        mBankName.setText("");
                        mBankLoc.setText("");
                        mRegDate.setText("");
                        mSupportEngineer.setText("");

                        // Reset Spinners to first item
                        StatusSpinner.setSelection(0);
                        prioritySpinner.setSelection(0);
                        TypeSpinner.setSelection(0);
                        MachineTypeSpinner.setSelection(0);

                        generateNextSerialNumber();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(IssueList.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e("FirestoreError", e.getMessage(), e);
                    });
                        } else {
                            Toast.makeText(IssueList.this, "User profile not found.", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(IssueList.this, "Failed to fetch user info", Toast.LENGTH_SHORT).show();
                    });
        });
    }
    /** Computes the next per-user 3-digit serial: 001, 002, ... */
    private void generateNextSerialNumber() {
        serialReady = false;
        db.collection("issues")
                .whereEqualTo("createdBy", userId)  // ⚠️ Will need change if storing fullName
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int count = querySnapshot.size();
                    String nextNumber = String.format("%03d", count + 1);
                    mSerialNumber.setText(nextNumber);
                    serialReady = true;
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to generate serial number", Toast.LENGTH_SHORT).show();
                    serialReady = false;
                });
    }
}
