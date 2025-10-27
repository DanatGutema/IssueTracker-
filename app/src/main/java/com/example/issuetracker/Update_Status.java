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

import com.google.firebase.Timestamp;   // For Firestore Timestamp
import java.util.Date;                  // For java.util.Date
import java.text.SimpleDateFormat;      // For formatting


import com.google.firebase.auth.FirebaseAuth;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import android.graphics.Color;
import android.util.Log;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import com.google.firebase.auth.FirebaseUser;
import android.widget.ArrayAdapter;
import android.widget.ArrayAdapter;
import java.util.List;
import java.util.ArrayList;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.google.firebase.firestore.Query;

import java.util.Set;
import java.util.HashSet;
import java.util.Collections;  // if you want to sort
import java.util.List;
import java.util.ArrayList;


public class Update_Status extends AppCompatActivity {

    private EditText titleEditText, descriptionEditText, regDateText, bankLocText, bankNameText,mResult;
    private Spinner statusSpinner, serialSpinner;
    private EditText resolvedDateEditText;
    private Button saveUpdateButton;
    private TextView tvPriorityLevel, MachineTypeSpinnerUpdate, TypeSpinnerUpdate, mSupportEngineer; //  Priority as TextView
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

        //  Fetch section from Firestore
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
/*
        db.collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String section = documentSnapshot.getString("section");
                        if (section != null) {
                            setupMachineTypeSpinner(section);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load section", Toast.LENGTH_SHORT).show();
                });
*/

        userId = getIntent().getStringExtra("userId");

        //  Get current user ID from FirebaseAuth
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
        tvPriorityLevel = findViewById(R.id.tvPriorityLevel); //  TextView instead of Spinner
        resolvedDateEditText = findViewById(R.id.ResolvedDateUpdate);
        saveUpdateButton = findViewById(R.id.ButtonUpdate);
        serialSpinner = findViewById(R.id.SerialNumberUpdate);
        EditText ResolvedDateUpdate = findViewById(R.id.ResolvedDateUpdate);
        statusSpinner = findViewById(R.id.StatusSpinnerUpdate); // assuming you have a spinner for status
        TypeSpinnerUpdate = findViewById(R.id.TypeSpinnerUpdate); // assuming you have a spinner for type
        MachineTypeSpinnerUpdate = findViewById(R.id.MachineTypeSpinnerUpdate); // assuming you have a spinner for machine type
        mSupportEngineer = findViewById(R.id.SupportEngineer); // assuming you have a spinner for support engineer
        mResult = findViewById(R.id.Result);
        //hide the visibility of the result description first
        mResult.setVisibility(View.GONE);


        // Make some fields read-only
        titleEditText.setEnabled(false);
        descriptionEditText.setEnabled(false);
        bankLocText.setEnabled(false);
        bankNameText.setEnabled(false);
        regDateText.setEnabled(false);
        TypeSpinnerUpdate.setEnabled(false);
        MachineTypeSpinnerUpdate.setEnabled(false);
        tvPriorityLevel.setEnabled(false);
        mSupportEngineer.setEnabled(false);

        if (TextUtils.isEmpty(userId)) {
            Toast.makeText(this, "No user ID provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

//        // Set a calendar for the resolved date
//        ResolvedDateUpdate.setOnClickListener(v -> {
//            final Calendar calendar = Calendar.getInstance();
//            int year = calendar.get(Calendar.YEAR);
//            int month = calendar.get(Calendar.MONTH);
//            int day = calendar.get(Calendar.DAY_OF_MONTH);
//
//            DatePickerDialog datePickerDialog = new DatePickerDialog(
//                    Update_Status.this,
//                    (view, selectedYear, selectedMonth, selectedDay) -> {
//                        // Format date as needed
//                        String date = selectedDay + "/" + (selectedMonth + 1) + "/" + selectedYear;
//                        ResolvedDateUpdate.setText(date);
//                    },
//                    year, month, day
//            );
//            datePickerDialog.show();
//        });



//        ResolvedDateUpdate.setOnClickListener(v -> {
//            final Calendar calendar = Calendar.getInstance();
//            int year = calendar.get(Calendar.YEAR);
//            int month = calendar.get(Calendar.MONTH);
//            int day = calendar.get(Calendar.DAY_OF_MONTH);
//
//            DatePickerDialog datePickerDialog = new DatePickerDialog(
//                    Update_Status.this,
//                    (view, selectedYear, selectedMonth, selectedDay) -> {
//                        // Use selected date
//                        Calendar cal = Calendar.getInstance();
//                        cal.set(selectedYear, selectedMonth, selectedDay);
//
//                        // Add system time (hour + minute)
//                        Calendar now = Calendar.getInstance();
//                        cal.set(Calendar.HOUR_OF_DAY, now.get(Calendar.HOUR_OF_DAY));
//                        cal.set(Calendar.MINUTE, now.get(Calendar.MINUTE));
//
//                        // Format with date + hr:min
//                        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm");
//                        ResolvedDateUpdate.setText(sdf.format(cal.getTime()));
//                    },
//                    year, month, day
//            );
//            datePickerDialog.show();
//        });

        ResolvedDateUpdate.setOnClickListener(v -> {
            final Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    Update_Status.this,
                    (view, selectedYear, selectedMonth, selectedDay) -> {
                        Calendar cal = Calendar.getInstance();
                        cal.set(selectedYear, selectedMonth, selectedDay);

                        Calendar now = Calendar.getInstance();

                        //  Check for future date
                        if (cal.after(now)) {
                            Toast.makeText(Update_Status.this, "Future dates are not allowed!", Toast.LENGTH_SHORT).show();
                            return; // exit without setting date
                        }

                        // Add system time (hour + minute)
                        cal.set(Calendar.HOUR_OF_DAY, now.get(Calendar.HOUR_OF_DAY));
                        cal.set(Calendar.MINUTE, now.get(Calendar.MINUTE));

                        // Format with date + hr:min
                        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm");
                        ResolvedDateUpdate.setText(sdf.format(cal.getTime()));
                    },
                    year, month, day
            );

            //  Optional: prevent picking future dates visually
            datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());

            datePickerDialog.show();
        });


        List<String> statuses   = Arrays.asList(getResources().getStringArray(R.array.StatusUpdate));

// Status Spinner
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(
                this,
                R.layout.spinner_item,
                statuses
        );
        statusAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        statusSpinner.setAdapter(statusAdapter);

        //show the result description based on criteria
        statusSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                String selectedStatus = parent.getItemAtPosition(position).toString();

                if (selectedStatus.equalsIgnoreCase("In progress") ||
                        selectedStatus.equalsIgnoreCase("On Hold") ||
                        selectedStatus.equalsIgnoreCase("Waiting for Confirmation") ||
                        selectedStatus.equalsIgnoreCase("Pending") ||
                        selectedStatus.equalsIgnoreCase("Completed"))
                {

                    mResult.setVisibility(View.VISIBLE);
                    mResult.setHint("Enter note for this status");
                    mResult.setHintTextColor(Color.parseColor("#000000"));

                } else {
                    mResult.setVisibility(View.GONE);
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });


//        //set the color of serial number spinner
//        ArrayAdapter<String> numbersAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, issueNumbers) {
//            @Override
//            public View getView(int position, View convertView, ViewGroup parent) {
//                TextView tv = (TextView) super.getView(position, convertView, parent);
//                tv.setTextColor(Color.BLACK);  //  set spinner text color here
//                return tv;
//            }
//
//            @Override
//            public View getDropDownView(int position, View convertView, ViewGroup parent) {
//                TextView tv = (TextView) super.getDropDownView(position, convertView, parent);
//                tv.setTextColor(Color.BLACK);  //  set dropdown text color here
//                return tv;
//            }
//        };
//
//        numbersAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//        serialSpinner.setAdapter(numbersAdapter);


        // Load all issues of this user
        db.collection("issues")
                .whereEqualTo("createdBy", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {

//                    issueNumbers.clear();
//                    userIssues.clear();
                    userIssues.clear(); // still store all docs if needed
                    Set<String> uniqueNumbers = new HashSet<>();

                    if (!querySnapshot.isEmpty()) {
                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            String num = doc.getString("Number"); //  field name in Firestore
                            if (num != null && !num.isEmpty()) {
                                userIssues.add(doc);// keep for reference
                                uniqueNumbers.add(num); // collect only unique numbers
                            }
                        }
                    }

                    // convert to list and sort
                    issueNumbers.clear();
                    issueNumbers.addAll(uniqueNumbers);
                    Collections.sort(issueNumbers); // optional: numeric or string sort


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

//                    // Sort issues by serial number (numeric)
//                    userIssues.sort((doc1, doc2) -> {
//                        try {
//                            int n1 = Integer.parseInt(doc1.getString("Number"));
//                            int n2 = Integer.parseInt(doc2.getString("Number"));
//                            return Integer.compare(n1, n2);
//                        } catch (NumberFormatException e) {
//                            return doc1.getString("Number").compareTo(doc2.getString("Number"));
//                        }
//                    });
//
//                    // Populate issueNumbers after sorting
//                    for (DocumentSnapshot doc : userIssues) {
//                        issueNumbers.add(doc.getString("Number"));
//                    }

                    // Attach numbers to spinner with black text
                    ArrayAdapter<String> numbersAdapter = new ArrayAdapter<String>(Update_Status.this, android.R.layout.simple_spinner_item, issueNumbers) {
                        @Override
                        public View getView(int position, View convertView, ViewGroup parent) {
                            TextView tv = (TextView) super.getView(position, convertView, parent);
                            tv.setTextColor(Color.BLACK);  // selected item color
                            return tv;
                        }

                        @Override
                        public View getDropDownView(int position, View convertView, ViewGroup parent) {
                            TextView tv = (TextView) super.getDropDownView(position, convertView, parent);
                            tv.setTextColor(Color.WHITE);  // dropdown items color
                            return tv;
                        }
                    };

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
//        serialSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
//            @Override
//            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
//                if (position >= 0 && position < userIssues.size()) {
//                    showIssue(userIssues.get(position));
//                }
//            }
//
//            @Override
//            public void onNothingSelected(android.widget.AdapterView<?> parent) {
//            }
//        });
        //this was the previous one

        serialSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                String selectedNumber = issueNumbers.get(position);

                // Query Firestore for the latest document of this Number
                db.collection("issues")
                        .whereEqualTo("Number", selectedNumber)
                        .whereEqualTo("createdBy", userId) // restrict to the logged-in user
                        .orderBy("timestamp", Query.Direction.DESCENDING)
                        .limit(1)
                        .get()
                        .addOnSuccessListener(querySnapshot -> {
                            if (!querySnapshot.isEmpty()) {
                                DocumentSnapshot latestDoc = querySnapshot.getDocuments().get(0);
                                showIssue(latestDoc);
                            } else {
                                Toast.makeText(Update_Status.this, "No issue found", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .addOnFailureListener(e ->
                                Toast.makeText(Update_Status.this, "Failed to load issue: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                        );
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });


        // Save Update
        saveUpdateButton.setOnClickListener(v -> {
            String title = titleEditText.getText().toString();
            String description = descriptionEditText.getText().toString();
            String number = (String) serialSpinner.getSelectedItem();
            String bankLocation = bankLocText.getText().toString();
            String bankName = bankNameText.getText().toString();
            String regDate = regDateText.getText().toString();
            String status = statusSpinner.getSelectedItem().toString();
            String resolvedDate = resolvedDateEditText.getText().toString();
            String priority = tvPriorityLevel.getText().toString();
            String type = TypeSpinnerUpdate.getText().toString();
            String machineType = MachineTypeSpinnerUpdate.getText().toString();
            String SupportEngineer = mSupportEngineer.getText().toString();
            String Result = mResult.getText().toString();

            //here is the validation
            if (title.isEmpty()) {
                titleEditText.setError("Title is required");
                titleEditText.requestFocus();
                return;
            }

            if (description.isEmpty()) {
                descriptionEditText.setError("Description is required");
                descriptionEditText.requestFocus();
                return;
            }

            if (bankName.isEmpty()) {
                bankNameText.setError("Bank Name is required");
                bankNameText.requestFocus();
                return;
            }

            if (bankLocation.isEmpty()) {
                bankLocText.setError("Bank Location is required");
                bankLocText.requestFocus();
                return;
            }
            if (SupportEngineer.isEmpty()) {
                mSupportEngineer.setError("Support Engineer is required");
                mSupportEngineer.requestFocus();
                return;
            }

            if (regDate.isEmpty()) {
                regDateText.setError("Registration Date is required");
                regDateText.requestFocus();
                return;
            }

            if (resolvedDate.isEmpty()) {
                resolvedDateEditText.setError("Resolved or In Progress Date is required");
                resolvedDateEditText.requestFocus();
                return;
            }
            if (type.isEmpty()) {
                TypeSpinnerUpdate.setError("Type is required");
                TypeSpinnerUpdate.requestFocus();
                return;
            }
            if (Result.isEmpty()) {
                mResult.setError("Result is required");
                mResult.requestFocus();
                return;
            }

            if (priority.equals("Select Priority Level")) {
                Toast.makeText(Update_Status.this, "Please select a priority", Toast.LENGTH_SHORT).show();
                return;
            }

            if (status.equals("Select The Current Status of The Issue")) {
                Toast.makeText(Update_Status.this, "Please select a status", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> updatedData = new HashMap<>();
            updatedData.put("title", title);
            updatedData.put("description", description);
            updatedData.put("bankName", bankName);
            updatedData.put("Number",   number);
            updatedData.put("bankLocation", bankLocation);
            updatedData.put("registrationDate", regDate);
            updatedData.put("status", status);

            //updatedData.put("resolvedOrInProgressDate", resolvedDate);


            String resolvedDateStr = resolvedDateEditText.getText().toString();
            if (!TextUtils.isEmpty(resolvedDateStr)) {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
                    Date parsed = sdf.parse(resolvedDateStr);
                    resolvedDateStr = sdf.format(parsed); // ensures hr:min is kept
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            updatedData.put("resolvedOrInProgressDate", resolvedDateStr);
            updatedData.put("Result", Result);


            updatedData.put("createdBy", userId); //  Important for Firestore rules
            updatedData.put("priority", priority);
            updatedData.put("type", type);
            updatedData.put("machineType", machineType);
            updatedData.put("SupportEngineer", SupportEngineer);
            Timestamp newTimestamp = Timestamp.now();
            updatedData.put("timestamp", newTimestamp);


            // find the issueId using serial number
            String selectedIssueId = null;
            for (DocumentSnapshot issue : userIssues) {
                if (number.equals(issue.getString("Number"))) {
                    selectedIssueId = issue.getId();
                    break;
                }
            }

//            if (selectedIssueId != null) {
//                db.collection("issues").document(selectedIssueId)
//                        .update(updatedData)//only updates the fields instead of replacing
//                        .addOnSuccessListener(aVoid -> {
//                            Toast.makeText(Update_Status.this, "Issue updated successfully", Toast.LENGTH_SHORT).show();
//                            finish();
//                        })
//                        .addOnFailureListener(e ->
//                                Toast.makeText(Update_Status.this, "Failed to update: " + e.getMessage(), Toast.LENGTH_SHORT).show()
//                        );
//            } else {
//                Toast.makeText(Update_Status.this, "No matching issue found to update", Toast.LENGTH_SHORT).show();
//            }
//        });
//    }
            // Get latest document for this Number
            db.collection("issues")
                    .whereEqualTo("Number", number)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(1)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        DocumentSnapshot latestDoc = querySnapshot.isEmpty() ? null : querySnapshot.getDocuments().get(0);

                        if (latestDoc != null) {
                            Timestamp oldTimestamp = latestDoc.getTimestamp("timestamp");

                            if (oldTimestamp != null) {
                                // Compare only the date part (ignore time)
                                Calendar calOld = Calendar.getInstance();
                                calOld.setTime(oldTimestamp.toDate());
                                calOld.set(Calendar.HOUR_OF_DAY, 0);
                                calOld.set(Calendar.MINUTE, 0);
                                calOld.set(Calendar.SECOND, 0);
                                calOld.set(Calendar.MILLISECOND, 0);

                                Calendar calNew = Calendar.getInstance();
                                calNew.setTime(newTimestamp.toDate());
                                calNew.set(Calendar.HOUR_OF_DAY, 0);
                                calNew.set(Calendar.MINUTE, 0);
                                calNew.set(Calendar.SECOND, 0);
                                calNew.set(Calendar.MILLISECOND, 0);

                                if (calOld.equals(calNew)) {
                                    // Same day → update latest document
                                    db.collection("issues").document(latestDoc.getId())
                                            .update(updatedData)
                                            .addOnSuccessListener(aVoid ->
                                                    Toast.makeText(this, "Issue updated", Toast.LENGTH_SHORT).show()
                                            )
                                            .addOnFailureListener(e ->
                                                    Toast.makeText(this, "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                                            );
                                    return;
                                }
                            }
                        }


                        // Fetch username & section first
                        db.collection("users").document(userId).get()
                                .addOnSuccessListener(userDoc -> {
                                    if (userDoc.exists()) {
                                        String fullName = userDoc.getString("fullName");
                                        String section = userDoc.getString("section");
                                        String role = userDoc.getString("role");
                                        String dept = userDoc.getString("dept");
                                        String email = userDoc.getString("email");


                                        if (fullName != null) {
                                            updatedData.put("fullName", fullName);
                                        }
                                        if (section != null) {
                                            updatedData.put("section", section);
                                        }
                                        if (role != null) {
                                            updatedData.put("role", role);
                                        }
                                        if (dept != null) {
                                            updatedData.put("dept", dept);
                                        }
                                        if (email != null) {
                                            updatedData.put("email", email);
                                        }
                                    }

                                    // Now save everything (issue + user data) in one write
                                    db.collection("issues")
                                            .add(updatedData)
                                            .addOnSuccessListener(aVoid ->
                                                    Toast.makeText(this, "New issue entry created.", Toast.LENGTH_SHORT).show()
                                            )
                                            .addOnFailureListener(e ->
                                                    Toast.makeText(this, "Failed to create new entry: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                                            );

                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(this, "Failed to fetch user data: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                                );
                    });
                    });
    }
    private void showIssue(DocumentSnapshot doc) {
        titleEditText.setText(doc.getString("title"));
        descriptionEditText.setText(doc.getString("description"));
        bankLocText.setText(doc.getString("bankLocation"));
        bankNameText.setText(doc.getString("bankName"));
        regDateText.setText(doc.getString("registrationDate"));

        //  Show priority level as TextView
        String priority = doc.getString("priority");
        if (priority != null && !priority.trim().isEmpty()) {
            tvPriorityLevel.setText(priority.trim());
            tvPriorityLevel.setTextColor(Color.parseColor("#000000"));
        } else {
            tvPriorityLevel.setText("N/A");
        }
        //  Show support engineer as TextView
        String SupportEngineer = doc.getString("SupportEngineer");
        if (SupportEngineer != null && !SupportEngineer.trim().isEmpty()) {
            mSupportEngineer.setText(SupportEngineer.trim());
        } else {
            mSupportEngineer.setText("N/A");
        }

        //set type as  textview
        String type = doc.getString("type");
        if (type != null && !type.trim().isEmpty()) {
            TypeSpinnerUpdate.setText(type.trim());
            TypeSpinnerUpdate.setTextColor(Color.parseColor("#000000"));
        } else {
            TypeSpinnerUpdate.setText("N/A");
        }
        //set machine type as  textview
        String machineType = doc.getString("machineType");
        if (machineType != null && !machineType.trim().isEmpty()) {
            MachineTypeSpinnerUpdate.setText(machineType.trim());
            MachineTypeSpinnerUpdate.setTextColor(Color.parseColor("#000000")); // Blue

        } else {
            MachineTypeSpinnerUpdate.setText("N/A");
        }
        // Set status spinner
//        String currentStatus = doc.getString("status");
//        if (currentStatus != null) {
//            if (currentStatus.equalsIgnoreCase("In Progress")) {
//                statusSpinner.setSelection(0);
//            } else if (currentStatus.equalsIgnoreCase("Resolved")) {
//                statusSpinner.setSelection(1);
//            }

        //  Set status spinner and control editability
        String currentStatus = doc.getString("status");
        if (currentStatus != null) {
            if (currentStatus.equalsIgnoreCase("In Progress") || currentStatus.equalsIgnoreCase("Open") || currentStatus.equalsIgnoreCase("Open Ticket") || currentStatus.equalsIgnoreCase("Pending") || currentStatus.equalsIgnoreCase("Waiting for Confirmation") || currentStatus.equalsIgnoreCase("On Hold")) {
                statusSpinner.setSelection(0);
                statusSpinner.setEnabled(true);
                resolvedDateEditText.setEnabled(true);
                mResult.setEnabled(true);
            } else if (currentStatus.equalsIgnoreCase("Completed")) {
                statusSpinner.setSelection(1);
                statusSpinner.setEnabled(false);           // disable spinner
                resolvedDateEditText.setEnabled(false);
                mResult.setEnabled(false);// disable date edit
            }

            //  Set result
            String result = doc.getString("Result");
            if (result != null) {
                mResult.setText(result);
            }

            //  Set resolved date
            resolvedDateEditText.setText(doc.getString("resolvedOrInProgressDate"));
        }
    }
    /*private void setupMachineTypeSpinner(String section) {
        MachineTypeSpinnerUpdate  = findViewById(R.id.MachineTypeSpinnerUpdate);
        List<String> items = new ArrayList<>();

        if ("POS/TOMs".equalsIgnoreCase(section)) {
            items.addAll(Arrays.asList(getResources().getStringArray(R.array.PosToms)));
        } else if ("ATM/ITM/Bulk....".equalsIgnoreCase(section)) {
            items.addAll(Arrays.asList(getResources().getStringArray(R.array.ATM)));
        } else if ("Voice Guidance/CVM".equalsIgnoreCase(section)) {
            items.addAll(Arrays.asList(getResources().getStringArray(R.array.VoiceGuidance)));
        } else if ("Axis Camera/Vision".equalsIgnoreCase(section)) {
            items.addAll(Arrays.asList(getResources().getStringArray(R.array.AxisCamera)));
        } else if ("Digital Banking Solution Division".equalsIgnoreCase(section)) {
            items.addAll(Arrays.asList(getResources().getStringArray(R.array.MachineTypeDigitalBanking)));
            // items.addAll(Arrays.asList(getResources().getStringArray(R.array.AxisCamera)));
        } else if ("Self Service Solution Division".equalsIgnoreCase(section)) {
            items.addAll(Arrays.asList(getResources().getStringArray(R.array.MachineTypeSelfService)));
            // items.addAll(Arrays.asList(getResources().getStringArray(R.array.VoiceGuidance)));
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, items);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        MachineTypeSpinnerUpdate.setAdapter(adapter);
    }*/
}