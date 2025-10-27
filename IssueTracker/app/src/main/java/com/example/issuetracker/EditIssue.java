package com.example.issuetracker;

import android.app.DatePickerDialog;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.FirebaseFirestore;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import com.google.firebase.firestore.FieldValue;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import com.google.firebase.auth.FirebaseAuth;
import android.widget.ArrayAdapter;



public class EditIssue extends AppCompatActivity {
    EditText SerialNumberEdit, IssueTitleEdit, IssueDesEdit, BankNameEdit, BankLocEdit, RegDateEdit, SupportEdit;
    Spinner prioritySpinnerEdit, StatusSpinnerEdit, TypeSpinnerEdit, MachineTypeSpinnerEdit;
    Button ButtonAddEdit;

    FirebaseFirestore db;
    String issueId;

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


        db = FirebaseFirestore.getInstance();

        // Get issueId passed from list page
        issueId = getIntent().getStringExtra("issueId");

        //generate date
        RegDateEdit.setOnClickListener(v -> {
            final Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    EditIssue.this,
                    (view, selectedYear, selectedMonth, selectedDay) -> {
                        // Format date as needed
                        String date = selectedDay + "/" + (selectedMonth + 1) + "/" + selectedYear;
                        RegDateEdit.setText(date);
                    },
                    year, month, day
            );
            datePickerDialog.show();
        });

        if(issueId != null){
            db.collection("issues").document(issueId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if(documentSnapshot.exists()){
                            // Fill form with existing data
                            SerialNumberEdit.setText(documentSnapshot.getString("Number"));
                            IssueTitleEdit.setText(documentSnapshot.getString("title"));
                            IssueDesEdit.setText(documentSnapshot.getString("description"));
                            BankNameEdit.setText(documentSnapshot.getString("bankName"));
                            BankLocEdit.setText(documentSnapshot.getString("bankLocation"));
                            RegDateEdit.setText(documentSnapshot.getString("registrationDate"));
                            SupportEdit.setText(documentSnapshot.getString("supportEngineer"));

                          //  issues.put("timestamp", FieldValue.serverTimestamp()); // optional, auto-time

                            // Spinner (Priority & Status)
                            String priority = documentSnapshot.getString("priority");
                            String status = documentSnapshot.getString("status");
                            String type = documentSnapshot.getString("type");
                            String machineType = documentSnapshot.getString("machineType");

                            setSpinnerSelection(prioritySpinnerEdit, priority);
                            setSpinnerSelection(StatusSpinnerEdit, status);
                            setSpinnerSelection(TypeSpinnerEdit, type);
                            setSpinnerSelection(MachineTypeSpinnerEdit, machineType);
                        } else {
                            Toast.makeText(this, "Document does not exist", Toast.LENGTH_SHORT).show();
                        }
                    });
        }

        // Save changes
        ButtonAddEdit.setOnClickListener(v -> saveChanges());
    }

    // Helper for spinner
    private void setSpinnerSelection(Spinner spinner, String value){
        ArrayAdapter adapter = (ArrayAdapter) spinner.getAdapter();
        if(value != null){
            int pos = adapter.getPosition(value);
            if(pos >= 0){
                spinner.setSelection(pos);
            }
        }
    }

    private void saveChanges(){
        String number = SerialNumberEdit.getText().toString().trim();
        String title = IssueTitleEdit.getText().toString().trim();
        String description = IssueDesEdit.getText().toString().trim();
        String bankName = BankNameEdit.getText().toString().trim();
        String bankLocation = BankLocEdit.getText().toString().trim();
        String supportEngineer = SupportEdit.getText().toString().trim();
        String registrationDate = RegDateEdit.getText().toString().trim();
        String priority = prioritySpinnerEdit.getSelectedItem().toString();
        String status = StatusSpinnerEdit.getSelectedItem().toString();
        String type = TypeSpinnerEdit.getSelectedItem().toString();
        String machineType = MachineTypeSpinnerEdit.getSelectedItem().toString();

        Map<String, Object> updated = new HashMap<>();
        updated.put("Number", number.isEmpty() ? "-" : number);
        updated.put("title", title.isEmpty() ? "-" : title);
        updated.put("description", description.isEmpty() ? "-" : description);
        updated.put("bankName", number.isEmpty() ? "-" : bankName);
        updated.put("bankLocation", title.isEmpty() ? "-" : bankLocation);
        updated.put("supportEngineer", description.isEmpty() ? "-" : supportEngineer);
        updated.put("registrationDate", description.isEmpty() ? "-" : registrationDate);
        updated.put("priority", priority);
        updated.put("status", status);
        updated.put("type", type);
        updated.put("machineType", machineType);
        updated.put("timestamp", FieldValue.serverTimestamp()); // optional, auto-time

        db.collection("issues").document(issueId)
                .update(updated)
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(this, "Changes saved!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
