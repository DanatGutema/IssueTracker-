package com.example.issuetracker;

import android.app.DatePickerDialog;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import com.google.firebase.firestore.FieldValue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.google.firebase.auth.FirebaseAuth;
import android.widget.ArrayAdapter;

public class EditIssue extends AppCompatActivity {
    EditText SerialNumberEdit, IssueTitleEdit, IssueDesEdit, BankLocEdit, RegDateEdit, SupportEdit;
    Spinner prioritySpinnerEdit, StatusSpinnerEdit, TypeSpinnerEdit, MachineTypeSpinnerEdit, BankNameEdit;
    Button ButtonAddEdit;

    FirebaseFirestore db;
    String issueId;
    private boolean isAlreadyUpdated = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_issue);

        // Bind views
        SerialNumberEdit = findViewById(R.id.SerialNumberEdit);
        IssueTitleEdit = findViewById(R.id.IssueTitleEdit);
        IssueDesEdit = findViewById(R.id.IssueDesEdit);
        BankNameEdit = findViewById(R.id.BankNameEdit);
        BankLocEdit = findViewById(R.id.BankLocEdit);
        RegDateEdit = findViewById(R.id.RegDateEdit);
        prioritySpinnerEdit = findViewById(R.id.prioritySpinnerEdit);
        StatusSpinnerEdit = findViewById(R.id.StatusSpinnerEdit);
        ButtonAddEdit = findViewById(R.id.ButtonAddEdit);
        SupportEdit = findViewById(R.id.SupportEdit);
        TypeSpinnerEdit = findViewById(R.id.TypeSpinnerEdit);
        MachineTypeSpinnerEdit = findViewById(R.id.MachineTypeSpinnerEdit);

        // Load arrays from resources
        List<String> issueTypes = Arrays.asList(getResources().getStringArray(R.array.type));
        List<String> priorities = Arrays.asList(getResources().getStringArray(R.array.priority));
        List<String> statuses   = Arrays.asList(getResources().getStringArray(R.array.Status));
        List<String> bankNames   = Arrays.asList(getResources().getStringArray(R.array.bankName));

        // Issue Type Spinner
        ArrayAdapter<String> issueTypeAdapter = new ArrayAdapter<>(
                this,
                R.layout.spinner_item,
                issueTypes
        );
        issueTypeAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        TypeSpinnerEdit.setAdapter(issueTypeAdapter);

        // Bank Name Spinner
        ArrayAdapter<String> bankNameAdapter = new ArrayAdapter<>(
                this,
                R.layout.spinner_item,
                bankNames
        );
        bankNameAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        BankNameEdit.setAdapter(bankNameAdapter);

        // Priority Spinner
        ArrayAdapter<String> priorityAdapter = new ArrayAdapter<>(
                this,
                R.layout.spinner_item,
                priorities
        );
        priorityAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        prioritySpinnerEdit.setAdapter(priorityAdapter);

        // Status Spinner
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(
                this,
                R.layout.spinner_item,
                statuses
        );
        statusAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        StatusSpinnerEdit.setAdapter(statusAdapter);

        db = FirebaseFirestore.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        // Get issueId passed from list page
        issueId = getIntent().getStringExtra("issueId");

        //  First fetch user section, then setup spinner, then fetch issue
        db.collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(userSnap -> {
                    if (userSnap.exists()) {
                        String section = userSnap.getString("section");
                        if (section != null) {
                            setupMachineTypeSpinner(section);
                        }

                        // After spinner is ready → load issue
                        if (issueId != null) {
                            db.collection("issues").document(issueId)
                                    .get()
                                    .addOnSuccessListener(issueSnap -> {
                                        if (issueSnap.exists()) {
                                            // i added this line
                                            String issueStatus = issueSnap.getString("status");

                                            // Fill form with existing data
                                            SerialNumberEdit.setText(issueSnap.getString("Number"));
                                            IssueTitleEdit.setText(issueSnap.getString("title"));
                                            IssueDesEdit.setText(issueSnap.getString("description"));
                                            //BankNameEdit.setText(issueSnap.getString("bankName"));
                                            BankLocEdit.setText(issueSnap.getString("bankLocation"));
                                            RegDateEdit.setText(issueSnap.getString("registrationDate"));
                                            SupportEdit.setText(issueSnap.getString("SupportEngineer"));

                                            // Spinners
                                            setSpinnerSelection(prioritySpinnerEdit, issueSnap.getString("priority"));
                                            setSpinnerSelection(StatusSpinnerEdit, issueSnap.getString("status"));
                                            setSpinnerSelection(TypeSpinnerEdit, issueSnap.getString("type"));
                                            setSpinnerSelection(BankNameEdit, issueSnap.getString("bankName"));
                                            setSpinnerSelection(MachineTypeSpinnerEdit, issueSnap.getString("machineType"));

                                            // ✅ Add check for resolvedOrInProgressDate
                                            if (issueSnap.contains("resolvedOrInProgressDate")) {
                                                // means this issue was already updated in Update page
                                                isAlreadyUpdated = true;
                                                StatusSpinnerEdit.setEnabled(false);
                                                Toast.makeText(this, "Status cannot be changed (already updated)", Toast.LENGTH_SHORT).show();
                                            } else {
                                                // still editable
                                                isAlreadyUpdated = false;
                                                StatusSpinnerEdit.setEnabled(true);
                                            }

                                            //i added this line too
                                            //  Check status: if Resolved → disable all fields
                                            if ("Completed".equalsIgnoreCase(issueStatus)) {
                                                SerialNumberEdit.setEnabled(false);
                                                IssueTitleEdit.setEnabled(false);
                                                IssueDesEdit.setEnabled(false);
                                                BankNameEdit.setEnabled(false);
                                                BankLocEdit.setEnabled(false);
                                                RegDateEdit.setEnabled(false);
                                                SupportEdit.setEnabled(false);

                                                prioritySpinnerEdit.setEnabled(false);
                                                StatusSpinnerEdit.setEnabled(false);
                                                TypeSpinnerEdit.setEnabled(false);
                                                MachineTypeSpinnerEdit.setEnabled(false);

                                                ButtonAddEdit.setEnabled(false); // disable save button
                                                Toast.makeText(this, "This issue is Completed and cannot be edited", Toast.LENGTH_LONG).show();
                                            }

                                        } else {
                                            Toast.makeText(this, "Issue not found", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        }
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load user section", Toast.LENGTH_SHORT).show()
                );

        // Date picker with time
        RegDateEdit.setOnClickListener(v -> {
            final Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    EditIssue.this,
                    (view, selectedYear, selectedMonth, selectedDay) -> {
                        Calendar selectedCal = Calendar.getInstance();
                        selectedCal.set(selectedYear, selectedMonth, selectedDay);

                        Calendar now = Calendar.getInstance();

                        //  Check for future date
                        if (selectedCal.after(now)) {
                            Toast.makeText(EditIssue.this, "Future dates are not allowed!", Toast.LENGTH_SHORT).show();
                            return; // stop and don’t set the date
                        }

                        int hour = now.get(Calendar.HOUR_OF_DAY);
                        int minute = now.get(Calendar.MINUTE);

                        String date = selectedDay + "/" + (selectedMonth + 1) + "/" + selectedYear
                                + " " + String.format("%02d:%02d", hour, minute);
                        RegDateEdit.setText(date);
                    },
                    year, month, day
            );

            //  Optional: also visually restrict in the picker
            datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());

            datePickerDialog.show();
        });


        // Save changes
        ButtonAddEdit.setOnClickListener(v -> saveChanges());
    }

    // Helper for spinner
    private void setSpinnerSelection(Spinner spinner, String value) {
        ArrayAdapter adapter = (ArrayAdapter) spinner.getAdapter();
        if (value != null) {
            int pos = adapter.getPosition(value);
            if (pos >= 0) {
                spinner.setSelection(pos);
            }
        }
    }

    private void saveChanges() {
        String number = SerialNumberEdit.getText().toString().trim();
        String title = IssueTitleEdit.getText().toString().trim();
        String description = IssueDesEdit.getText().toString().trim();
       // String bankName = BankNameEdit.getText().toString().trim();
        String bankLocation = BankLocEdit.getText().toString().trim();
        String SupportEngineer = SupportEdit.getText().toString().trim();
        String registrationDate = RegDateEdit.getText().toString().trim();
        String priority = prioritySpinnerEdit.getSelectedItem().toString();
        String status = StatusSpinnerEdit.getSelectedItem().toString();
        String type = TypeSpinnerEdit.getSelectedItem().toString();
        String bankName = BankNameEdit.getSelectedItem().toString();
        String machineType = MachineTypeSpinnerEdit.getSelectedItem().toString();

        //here is the validation
        if (title.isEmpty()) {
            IssueTitleEdit.setError("Title is required");
            IssueTitleEdit.requestFocus();
            return;
        }
        if (description.isEmpty()) {
            IssueDesEdit.setError("Description is required");
            IssueDesEdit.requestFocus();
            return;
        }

        if (bankName.equals("Select The Bank Name")) {
            Toast.makeText(this, "Bank Name is required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (bankLocation.isEmpty()) {
            BankLocEdit.setError("Bank Location is required");
            BankLocEdit.requestFocus();
            return;
        }
        if (SupportEngineer.isEmpty()) {
            SupportEdit.setError("Support Engineer is required");
            SupportEdit.requestFocus();
            return;
        }

        if (registrationDate.isEmpty()) {
            RegDateEdit.setError("Registration Date is required");
            RegDateEdit.requestFocus();
            return;
        }

        if (priority.equals("Select Priority Level")) {
            Toast.makeText(this, "Please select a priority", Toast.LENGTH_SHORT).show();
            return;
        }
        // only check status if not already updated
        if (!isAlreadyUpdated  && status.equals("Select The Current Status of The Issue")) {
            Toast.makeText(this, "Please select a status", Toast.LENGTH_SHORT).show();
            return;
        }
        if (type.equals("Select Type")) {
            Toast.makeText(this, "Please select a type", Toast.LENGTH_SHORT).show();
            return;
        }
        if (machineType.equals("Select Machine Type/Software")) {
            Toast.makeText(this, "Please select a machine type", Toast.LENGTH_SHORT).show();
            return;
        }


        Map<String, Object> updated = new HashMap<>();
        updated.put("Number", number.isEmpty() ? "-" : number);
        updated.put("title", title.isEmpty() ? "-" : title);
        updated.put("description", description.isEmpty() ? "-" : description);
        updated.put("bankName", bankName.isEmpty() ? "-" : bankName);
        updated.put("bankLocation", bankLocation.isEmpty() ? "-" : bankLocation);
        updated.put("SupportEngineer", SupportEngineer.isEmpty() ? "-" : SupportEngineer);
        updated.put("registrationDate", registrationDate.isEmpty() ? "-" : registrationDate);
        updated.put("priority", priority);
        //updated.put("status", status);
        // Only save status if it's a real selection
        if (!status.equals("Select The Current Status of The Issue")) {
            updated.put("status", status);
        } else {
            // do nothing, keep existing status in Firestore
        }
        updated.put("type", type);
        updated.put("machineType", machineType);
       // updated.put("timestamp", FieldValue.serverTimestamp());

//        // ✅ Only save status if the issue is not already updated
//        if (!issueSnap.contains("resolvedOrInProgressDate")) {
//            updated.put("status", status);
//        } else {
//            Toast.makeText(this, "Status not saved (already updated)", Toast.LENGTH_SHORT).show();
//        }

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        db.collection("issues")
                .whereEqualTo("Number", number)
                .whereEqualTo("createdBy", userId)
                .get()
                .addOnSuccessListener(querySnapshot  -> {
                    if (!querySnapshot.isEmpty()) {
                        for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshot) {
                            // Step 2: update each doc
                            db.collection("issues")
                                    .document(doc.getId())
                                    .update(updated);
                        }
                        Toast.makeText(this, "Changes saved for all related issues!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "No matching issues found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void setupMachineTypeSpinner(String section) {
        List<String> items = new ArrayList<>();

        if ("POS/TOMS".equalsIgnoreCase(section)) {
            items.addAll(Arrays.asList(getResources().getStringArray(R.array.PosToms)));
        } else if ("ATM/ITM/Bulk....".equalsIgnoreCase(section)) {
            items.addAll(Arrays.asList(getResources().getStringArray(R.array.ATM)));
        } else if ("Voice Guidance/CVM".equalsIgnoreCase(section)) {
            items.addAll(Arrays.asList(getResources().getStringArray(R.array.VoiceGuidance)));
        } else if ("Axis Camera/Vision".equalsIgnoreCase(section)) {
            items.addAll(Arrays.asList(getResources().getStringArray(R.array.AxisCamera)));
        } else if ("Digital Banking Solution Division".equalsIgnoreCase(section)) {
            items.addAll(Arrays.asList(getResources().getStringArray(R.array.MachineTypeDigitalBanking)));
        } else if ("Self Service Solution Division".equalsIgnoreCase(section)) {
            items.addAll(Arrays.asList(getResources().getStringArray(R.array.MachineTypeSelfService)));
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                R.layout.spinner_item,
                items
        );
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        MachineTypeSpinnerEdit.setAdapter(adapter);
    }
}