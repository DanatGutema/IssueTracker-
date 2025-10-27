package com.example.issuetracker;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FieldValue;

import java.util.Date;
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

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import com.google.firebase.auth.FirebaseUser;
import android.widget.ArrayAdapter;

import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;
import androidx.core.app.NotificationCompat;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;


import com.google.firebase.firestore.FirebaseFirestore;

public class IssueList extends AppCompatActivity {
    EditText mSerialNumber, mIssueTitle, mIssueDes, mBankLoc, mRegDate, mSupportEngineer;
    Button mButtonAdd;
    Spinner StatusSpinner, prioritySpinner, TypeSpinner, MachineTypeSpinner, mBankName;
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
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "You’re not signed in.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
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
        // MachineTypeSpinner = findViewById(R.id.MachineTypeSpinner);
        MachineTypeSpinner  = findViewById(R.id.MachineTypeSpinner);
        mButtonAdd = findViewById(R.id.ButtonAdd);
        EditText regDate = findViewById(R.id.RegDate);


        // Load arrays from resources
        List<String> issueTypes = Arrays.asList(getResources().getStringArray(R.array.type));
        List<String> priorities = Arrays.asList(getResources().getStringArray(R.array.priority));
        List<String> statuses   = Arrays.asList(getResources().getStringArray(R.array.Status));
        List<String> bankNames = Arrays.asList(getResources().getStringArray(R.array.bankName));


        //Bank Names
        ArrayAdapter<String> bankNameAdapter = new ArrayAdapter<>(
                this,
                R.layout.spinner_item, // selected item layout
                bankNames
        );
        bankNameAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        mBankName.setAdapter(bankNameAdapter);


        // Issue Type Spinner
        ArrayAdapter<String> issueTypeAdapter = new ArrayAdapter<>(
                this,
                R.layout.spinner_item, // selected item layout
                issueTypes
        );
        issueTypeAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        TypeSpinner.setAdapter(issueTypeAdapter);

        // Priority Spinner
        ArrayAdapter<String> priorityAdapter = new ArrayAdapter<>(
                this,
                R.layout.spinner_item,
                priorities
        );
        priorityAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        prioritySpinner.setAdapter(priorityAdapter);

// Status Spinner
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(
                this,
                R.layout.spinner_item,
                statuses
        );
        statusAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        StatusSpinner.setAdapter(statusAdapter);


        //  Fetch section from Firestore
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

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

//        regDate.setOnClickListener(v -> {
//            final Calendar calendar = Calendar.getInstance();
//            int year = calendar.get(Calendar.YEAR);
//            int month = calendar.get(Calendar.MONTH);
//            int day = calendar.get(Calendar.DAY_OF_MONTH);
//
//            DatePickerDialog datePickerDialog = new DatePickerDialog(
//                    IssueList.this,
//                    (view, selectedYear, selectedMonth, selectedDay) -> {
//                        // Format date as needed
//                        String date = selectedDay + "/" + (selectedMonth + 1) + "/" + selectedYear;
//                        regDate.setText(date);
//                    },
//                    year, month, day
//            );
//           datePickerDialog.show();
//        });
        regDate.setOnClickListener(v -> {
            final Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    IssueList.this,
                    (view, selectedYear, selectedMonth, selectedDay) -> {
                        // Use selected date
                        Calendar cal = Calendar.getInstance();
                        cal.set(selectedYear, selectedMonth, selectedDay);

                        // Add current system time
                        Calendar now = Calendar.getInstance();
                        cal.set(Calendar.HOUR_OF_DAY, now.get(Calendar.HOUR_OF_DAY));
                        cal.set(Calendar.MINUTE, now.get(Calendar.MINUTE));
                        //cal.set(Calendar.SECOND, now.get(Calendar.SECOND));

                        // Validate: selected date must not be in the future
                        if (cal.after(now)) {
                            Toast.makeText(IssueList.this, "Future dates are not allowed!", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // Show formatted date in EditText
                        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm");
                        regDate.setText(sdf.format(cal.getTime()));
                    },
                    year, month, day
            );
            //  Prevent choosing future dates
            datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());

            datePickerDialog.show();
        });



        mButtonAdd.setOnClickListener(v -> {
            String Number = mSerialNumber.getText().toString().trim();
            String Title = mIssueTitle.getText().toString().trim();
            String IssueDes = mIssueDes.getText().toString().trim();
            // String Name = mYourName.getText().toString().trim();
            String BankName = mBankName.getSelectedItem().toString();
            String BankLoc = mBankLoc.getText().toString().trim();
            String RegDate = mRegDate.getText().toString().trim();
            String SupportEngineer = mSupportEngineer.getText().toString().trim();
            String Status = StatusSpinner.getSelectedItem().toString();
            String Priority = prioritySpinner.getSelectedItem().toString();
            String Type = TypeSpinner.getSelectedItem().toString();
            String MachineType = MachineTypeSpinner.getSelectedItem().toString();


            //  Validation: Check all fields
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
//            if (BankName.isEmpty()) {
//                mBankName.setError("Bank Name is required");
//                mBankName.requestFocus();
//                return;
//            }
            if (BankName.equals("Select The Bank Name")) {
                Toast.makeText(IssueList.this, "Please select the bank name", Toast.LENGTH_SHORT).show();
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
            if (MachineType.equals("Select Machine Type/Software")) { // Assuming first item is "Select Machine Type"
                Toast.makeText(IssueList.this, "Please select a machine type", Toast.LENGTH_SHORT).show();
                return;
            }


            //  Fetch the fullName from "users" collection before saving issue
            db.collection("users").document(userId).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String fullName = documentSnapshot.getString("fullName");
                            String department = documentSnapshot.getString("dept");
                            String section = documentSnapshot.getString("section");
                            String role = documentSnapshot.getString("role");
                            String email = documentSnapshot.getString("email");
                            String managerType = documentSnapshot.getString("managerType"); // only for managers

                            //  Build a map of issue data
                            Map<String, Object> issue = new HashMap<>();
                            issue.put("Number", Number);
                            issue.put("title", Title);
                            issue.put("description", IssueDes);
                            //issue.put("yourName", Name);
                            issue.put("bankName",BankName);
                            issue.put("bankLocation",BankLoc);

//                            String registeredDateStr = mRegDate.getText().toString();
//                            Timestamp registerdTimestamp = null;
//
//                            if (!registeredDateStr.isEmpty()) {
//                                try {
//                                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm");
//                                    Date date = sdf.parse(registeredDateStr);
//                                    registerdTimestamp = new Timestamp(date);
//                                } catch (Exception e) {
//                                    e.printStackTrace();
//                                }
//                            }

// Then save in Firestore
//                            issue.put("registrationDate", mRegDate);
                            issue.put("registrationDate", mRegDate.getText().toString());


                            issue.put("status", Status);
                            issue.put("priority", Priority);
                            issue.put("SupportEngineer", SupportEngineer);
                            issue.put("type", Type);
                            issue.put("machineType", MachineType);
                            issue.put("timestamp", FieldValue.serverTimestamp()); // optional, auto-time
                            issue.put("createdBy", FirebaseAuth.getInstance().getCurrentUser().getUid());

                            //  Add role/department/section to match rules
                            issue.put("dept", department);
                            issue.put("section", section);
                            issue.put("role", role);
                            issue.put("fullName", fullName);
                            issue.put("email", email);
                            if (managerType != null) {
                                issue.put("managerType", managerType);
                            }

                            //Add to firestore database. (auto ID)
                            db.collection("issues")
                                    .add(issue)
                                    .addOnSuccessListener(documentReference -> {
                                        Toast.makeText(IssueList.this, "Issue added successfully!", Toast.LENGTH_SHORT).show();

                                        // Clear all EditTexts
                                        mSerialNumber.setText("");
                                        mIssueTitle.setText("");
                                        mIssueDes.setText("");
                                        mBankName.setSelection(0);
                                        mBankLoc.setText("");
                                        mRegDate.setText("");
                                        mSupportEngineer.setText("");

                                        // Reset Spinners to first item
                                        StatusSpinner.setSelection(0);
                                        prioritySpinner.setSelection(0);
                                        TypeSpinner.setSelection(0);
                                        MachineTypeSpinner.setSelection(0);

                                        generateNextSerialNumber();





//                                        List<String> targetUserIds = new ArrayList<>();
//
//                                        if ("Software Engineer".equalsIgnoreCase(role)) {
//                                            // get Section Manager of the same department & section
//                                            db.collection("users")
//                                                    .whereEqualTo("dept", department)
//                                                    .whereEqualTo("section", section)
//                                                    .whereEqualTo("role", "Section Manager")
//                                                    .get()
//                                                    .addOnSuccessListener(query -> {
//                                                        for (var doc : query.getDocuments()) {
//                                                            targetUserIds.add(doc.getId());
//                                                        }
//                                                        sendNotificationToUsers(targetUserIds, "A new issue has been added in your section.");
//                                                    });
//
//                                        } else if ("Section Manager".equalsIgnoreCase(role)) {
//                                            // Department Manager + Division Manager of specific section
//                                            db.collection("users")
//                                                    .whereEqualTo("dept", department)
//                                                    .whereEqualTo("role", "Department Manager")
//                                                    .get().addOnSuccessListener(query -> {
//                                                        for (var doc : query.getDocuments()) targetUserIds.add(doc.getId());
//
//                                                        // Division Manager query
//                                                        db.collection("users")
//                                                                .whereEqualTo("division", getDivisionForSection(section)) // helper function
//                                                                .whereEqualTo("role", "Division Manager")
//                                                                .get().addOnSuccessListener(query2 -> {
//                                                                    for (var doc : query2.getDocuments()) targetUserIds.add(doc.getId());
//                                                                    sendNotificationToUsers(targetUserIds, "A new issue has been added by Section Manager.");
//                                                                });
//                                                    });
//
//                                        } else if ("Division Manager".equalsIgnoreCase(role)) {
//                                            // Only Department Manager
//                                            db.collection("users")
//                                                    .whereEqualTo("dept", department)
//                                                    .whereEqualTo("role", "Department Manager")
//                                                    .get().addOnSuccessListener(query -> {
//                                                        for (var doc : query.getDocuments()) targetUserIds.add(doc.getId());
//                                                        sendNotificationToUsers(targetUserIds, "A new issue has been added by Division Manager.");
//                                                    });
//                                        }
//
//
//
//
//
//                                    })
//                                    .addOnFailureListener(e -> {
//                                        Toast.makeText(IssueList.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
//                                        Log.e("FirestoreError", e.getMessage(), e);
//                                    });
//                        } else {
//                            Toast.makeText(IssueList.this, "User profile not found.", Toast.LENGTH_SHORT).show();
//                        }
//                    })
//                    .addOnFailureListener(e -> {
//                        Toast.makeText(IssueList.this, "Failed to fetch user info", Toast.LENGTH_SHORT).show();
//                    });
//        });
//    }
                                        //  Send notifications to relevant managers
                                        List<String> targetUserIds = new ArrayList<>();
                                        if ("Software Engineer".equalsIgnoreCase(role)) {
                                            // Fetch Section Manager
                                            db.collection("users")
                                                    .whereEqualTo("dept", department)
                                                    .whereEqualTo("section", section)
                                                    .whereEqualTo("role", "Section Manager")
                                                    .get()
                                                    .addOnSuccessListener(query -> {
                                                        for (var doc : query.getDocuments()) {
                                                            targetUserIds.add(doc.getId());
                                                        }

                                                        // Fetch Division Manager
                                                        db.collection("users")
                                                                .whereEqualTo("division", getDivisionForSection(section))
                                                                .whereEqualTo("role", "Division Manager")
                                                                .get()
                                                                .addOnSuccessListener(query2 -> {
                                                                    for (var doc : query2.getDocuments()) {
                                                                        targetUserIds.add(doc.getId());
                                                                    }
                                                                    //sendNotificationToUsers(targetUserIds, "A new issue has been added in your section.");
                                                                });
                                                    });
                                        }
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


    private String getDivisionForSection(String section) {
        switch (section) {
            case "POS/TOMS":
            case "Axis Camera/Vision":
                return "Digital Banking Solution Division";
            case "ATM/ITM/Bulk....":
            case "Voice Guidance/CVM":
                return "Self Service Solution Division";
            default:
                return "";
        }
    }

    /** Computes the next per-user 3-digit serial: 001, 002, ... */
    private void generateNextSerialNumber() {
        serialReady = false;
        db.collection("issues")
                .whereEqualTo("createdBy", userId)  //  Will need change if storing fullName
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

    private void setupMachineTypeSpinner(String section) {
        MachineTypeSpinner  = findViewById(R.id.MachineTypeSpinner);
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
            // items.addAll(Arrays.asList(getResources().getStringArray(R.array.AxisCamera)));
        } else if ("Self Service Solution Division".equalsIgnoreCase(section)) {
            items.addAll(Arrays.asList(getResources().getStringArray(R.array.MachineTypeSelfService)));
            // items.addAll(Arrays.asList(getResources().getStringArray(R.array.VoiceGuidance)));
        }

//        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, items);
//        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//        MachineTypeSpinner.setAdapter(adapter);

        //  Use your custom black-text layouts
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                R.layout.spinner_item, // selected item (black text)
                items
        );
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item); // dropdown items (black text)
        MachineTypeSpinner.setAdapter(adapter);
    }

//    private void sendNotificationToUsers(List<String> targetUserIds, String message) {
//        for (String uid : targetUserIds) {
//            db.collection("users").document(uid).get()
//                    .addOnSuccessListener(doc -> {
//                        if (doc.exists()) {
//                            String fullName = doc.getString("fullName");
//
//                            Data data = new Data.Builder()
//                                    .putString("message", message)
//                                    .putString("fullName", fullName)
//                                    .build();
//
//                            OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(IssueNotificationWorker.class)
//                                    .setInitialDelay(3, TimeUnit.SECONDS) // short delay for demo
//                                    .setInputData(data)
//                                    .build();
//
//                            WorkManager.getInstance(this)
//                                    .enqueue(workRequest);
//                        }
//                    });
//        }
//    }
//
//
//    public static class IssueNotificationWorker extends Worker {
//        private static final String CHANNEL_ID = "issue_notification_channel";
//
//        public IssueNotificationWorker(@NonNull Context context, @NonNull WorkerParameters params) {
//            super(context, params);
//        }
//
//        @NonNull
//        @Override
//        public Result doWork() {
//            String message = getInputData().getString("message");
//            String fullName = getInputData().getString("fullName");
//
//            Context context = getApplicationContext();
//            NotificationManager notificationManager =
//                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
//
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                NotificationChannel channel = new NotificationChannel(
//                        CHANNEL_ID, "Issue Notification", NotificationManager.IMPORTANCE_HIGH);
//                notificationManager.createNotificationChannel(channel);
//            }
//
//            Intent intent = new Intent(context, IssueList.class);
//            PendingIntent pendingIntent = PendingIntent.getActivity(
//                    context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
//
//            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
//                    .setContentTitle("New Issue Assigned")
//                    .setContentText("Hey " + fullName + ", " + message)
//                    .setSmallIcon(android.R.drawable.ic_dialog_info)
//                    .setContentIntent(pendingIntent)
//                    .setAutoCancel(true)
//                    .setPriority(NotificationCompat.PRIORITY_HIGH);
//
//            notificationManager.notify((int) System.currentTimeMillis(), builder.build());
//            return Result.success();
//        }
//    }


}