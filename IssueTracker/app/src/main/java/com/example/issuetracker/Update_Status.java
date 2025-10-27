package com.example.issuetracker;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Update_Status extends AppCompatActivity {

    private EditText titleEditText, descriptionEditText, regDateText, bankLocText, bankNameText, SupportUpdate;
    private Spinner statusSpinner, serialSpinner;
    private EditText resolvedDateEditText;
    private Button saveUpdateButton;
    private TextView tvPriorityLevel, MachineTypeSpinnerUpdate, TypeSpinnerUpdate; // ✅ Priority as TextView
    private FirebaseFirestore db;
    private String userId;

    // list of user issues
    private List<DocumentSnapshot> userIssues = new ArrayList<>();
    private List<String> issueNumbers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_status);

        db = FirebaseFirestore.getInstance();
        userId = getIntent().getStringExtra("userId");

        // ✅ Get current user ID from FirebaseAuth
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        } else {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize UI components
        titleEditText = findViewById(R.id.IssueTitleUpdate);
        descriptionEditText = findViewById(R.id.IssueDesUpdate);
        bankLocText = findViewById(R.id.BankLocUpdate);
        bankNameText = findViewById(R.id.BankNameUpdate);
        regDateText = findViewById(R.id.RegDateUpdate);
        tvPriorityLevel = findViewById(R.id.tvPriorityLevel); // ✅ TextView instead of Spinner
        resolvedDateEditText = findViewById(R.id.ResolvedDateUpdate);
        saveUpdateButton = findViewById(R.id.ButtonUpdate);
        serialSpinner = findViewById(R.id.SerialNumberUpdate);
        SupportUpdate = findViewById(R.id.SupportUpdate);
        EditText ResolvedDateUpdate = findViewById(R.id.ResolvedDateUpdate);
        statusSpinner = findViewById(R.id.StatusSpinnerUpdate); // assuming you have a spinner for status
        TypeSpinnerUpdate = findViewById(R.id.TypeSpinnerUpdate); // assuming you have a spinner for type
        MachineTypeSpinnerUpdate = findViewById(R.id.MachineTypeSpinnerUpdate); // assuming you have a spinner for machine type

        // Make some fields read-only
        titleEditText.setEnabled(false);
        descriptionEditText.setEnabled(false);
        bankLocText.setEnabled(false);
        bankNameText.setEnabled(false);
        regDateText.setEnabled(false);
        TypeSpinnerUpdate.setEnabled(false);
        MachineTypeSpinnerUpdate.setEnabled(false);
        tvPriorityLevel.setEnabled(false);
        SupportUpdate.setEnabled(false);

        if (TextUtils.isEmpty(userId)) {
            Toast.makeText(this, "No user ID provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Set a calendar for the resolved date
        ResolvedDateUpdate.setOnClickListener(v -> {
            final Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    Update_Status.this,
                    (view, selectedYear, selectedMonth, selectedDay) -> {
                        // Format date as needed
                        String date = selectedDay + "/" + (selectedMonth + 1) + "/" + selectedYear;
                        ResolvedDateUpdate.setText(date);
                    },
                    year, month, day
            );
            datePickerDialog.show();
        });

        // Load all issues of this user
        db.collection("issues")
                .whereEqualTo("createdBy", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {

                    issueNumbers.clear();
                    userIssues.clear();

                    if (!querySnapshot.isEmpty()) {
                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            String num = doc.getString("Number"); // ⚠️ field name in Firestore
                            if (num != null && !num.isEmpty()) {
                                userIssues.add(doc);
                            }
                        }
                    }

                    if (userIssues.isEmpty()) {
                        ArrayAdapter<String> emptyAdapter = new ArrayAdapter<>(
                                Update_Status.this,
                                android.R.layout.simple_spinner_item,
                                issueNumbers
                        );
                        emptyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        serialSpinner.setAdapter(emptyAdapter);
                        saveUpdateButton.setEnabled(false);
                        Toast.makeText(Update_Status.this, "No issues found for this user", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Sort issues by serial number (numeric)
                    userIssues.sort((doc1, doc2) -> {
                        try {
                            int n1 = Integer.parseInt(doc1.getString("Number"));
                            int n2 = Integer.parseInt(doc2.getString("Number"));
                            return Integer.compare(n1, n2);
                        } catch (NumberFormatException e) {
                            return doc1.getString("Number").compareTo(doc2.getString("Number"));
                        }
                    });

                    // Populate issueNumbers after sorting
                    for (DocumentSnapshot doc : userIssues) {
                        issueNumbers.add(doc.getString("Number"));
                    }

                    // Attach numbers to spinner
                    ArrayAdapter<String> numbersAdapter = new ArrayAdapter<>(
                            Update_Status.this,
                            android.R.layout.simple_spinner_item,
                            issueNumbers
                    );
                    numbersAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    serialSpinner.setAdapter(numbersAdapter);

                    // Show first issue by default
                    serialSpinner.post(() -> {
                        serialSpinner.setSelection(0);
                        showIssue(userIssues.get(0));
                    });
                })
                .addOnFailureListener(e ->
                        Toast.makeText(Update_Status.this, "Failed to load issues: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );

        // When user selects a number, show that issue
        serialSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                if (position >= 0 && position < userIssues.size()) {
                    showIssue(userIssues.get(position));
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });

        // Save Update
        saveUpdateButton.setOnClickListener(v -> {
            String title = titleEditText.getText().toString();
            String description = descriptionEditText.getText().toString();
            String number = (String) serialSpinner.getSelectedItem();
            String bankLocation = bankLocText.getText().toString();
            String bankName = bankNameText.getText().toString();
            String regDate = regDateText.getText().toString();
            String mSupportUpdate = SupportUpdate.getText().toString();
            String status = statusSpinner.getSelectedItem().toString();
            String resolvedDate = resolvedDateEditText.getText().toString();
            String priority = tvPriorityLevel.getText().toString();
            String type = TypeSpinnerUpdate.getText().toString();
            String machineType = MachineTypeSpinnerUpdate.getText().toString();

            Map<String, Object> updatedData = new HashMap<>();
            updatedData.put("title", title);
            updatedData.put("description", description);
            updatedData.put("bankName", bankName);
            updatedData.put("number", number);
            updatedData.put("bankLocation", bankLocation);
            updatedData.put("registrationDate", regDate);
            updatedData.put("status", status);
            updatedData.put("supportEngineer", mSupportUpdate);
            updatedData.put("resolvedOrInProgressDate", resolvedDate);
            updatedData.put("createdBy", userId); // 🔑 Important for Firestore rules
            updatedData.put("priority", priority);
            updatedData.put("type", type);
            updatedData.put("machineType", machineType);

            // find the issueId using serial number
            String selectedIssueId = null;
            for (DocumentSnapshot issue : userIssues) {
                if (number.equals(issue.getString("Number"))) {
                    selectedIssueId = issue.getId();
                    break;
                }
            }

            if (selectedIssueId != null) {
                db.collection("update_issues").document(selectedIssueId).set(updatedData)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(Update_Status.this, "Issue updated successfully", Toast.LENGTH_SHORT).show();
                            finish();
                        })
                        .addOnFailureListener(e ->
                                Toast.makeText(Update_Status.this, "Failed to update: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                        );
            } else {
                Toast.makeText(Update_Status.this, "No matching issue found to update", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showIssue(DocumentSnapshot doc) {
        titleEditText.setText(doc.getString("title"));
        descriptionEditText.setText(doc.getString("description"));
        bankLocText.setText(doc.getString("bankLocation"));
        bankNameText.setText(doc.getString("bankName"));
        regDateText.setText(doc.getString("registrationDate"));
        SupportUpdate.setText(doc.getString("supportEngineer"));

        // ✅ Show priority level as TextView
        String priority = doc.getString("priority");
        if (priority != null && !priority.trim().isEmpty()) {
            tvPriorityLevel.setText(priority.trim());
        } else {
            tvPriorityLevel.setText("N/A");
        }
        //set type as  textview
        String type = doc.getString("type");
        if (type != null && !type.trim().isEmpty()) {
            TypeSpinnerUpdate.setText(type.trim());
        } else {
            TypeSpinnerUpdate.setText("N/A");
        }
        //set machine type as  textview
        String machineType = doc.getString("machineType");
        if (machineType != null && !machineType.trim().isEmpty()) {
            MachineTypeSpinnerUpdate.setText(machineType.trim());
        } else {
            MachineTypeSpinnerUpdate.setText("N/A");
        }
            // Set status spinner
            String currentStatus = doc.getString("status");
            if (currentStatus != null) {
                if (currentStatus.equalsIgnoreCase("In Progress")) {
                    statusSpinner.setSelection(0);
                } else if (currentStatus.equalsIgnoreCase("Resolved")) {
                    statusSpinner.setSelection(1);
                }
                // ✅ Set resolved date
                resolvedDateEditText.setText(doc.getString("resolvedOrInProgressDate"));
            }
        }
    }
