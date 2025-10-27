package com.example.issuetracker;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

import androidx.core.app.NotificationCompat;
import com.google.firebase.auth.FirebaseUser;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.ExistingPeriodicWorkPolicy;
import android.util.Log;

public class Login extends AppCompatActivity {

    private EditText mEmail, mPassword;
    private Button mLoginBtn;
    private TextView mCreateBtn, mforgotPassword;
    private FirebaseAuth fAuth;
    private FirebaseFirestore db;
    private CheckBox rememberMeCheckBox;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private String fullName;

    private String tempRoleForRedirect;
    private String tempUserIdForNotif;

    private ActivityResultLauncher<String> requestNotifPermission;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        fAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        editor = sharedPreferences.edit();

        // 🔹 Auto-login check (Remember Me)
        boolean isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false);
        if (isLoggedIn) {
            String role = sharedPreferences.getString("role", "");
            redirectToDashboard(role);
            return; // Don't load login page
        }


        setContentView(R.layout.activity_login);

        mEmail = findViewById(R.id.editTextTextEmailAddress);
        mPassword = findViewById(R.id.editTextTextPassword);
        mLoginBtn = findViewById(R.id.button4);
        mCreateBtn = findViewById(R.id.textView6);
        mforgotPassword = findViewById(R.id.forgotPassword);
        rememberMeCheckBox = findViewById(R.id.remember);

        fAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        editor = sharedPreferences.edit();

//        if (sharedPreferences.getBoolean("isLoggedIn", false)) {
//            String role = sharedPreferences.getString("role", "");
//            redirectToDashboard(role);
//        }
        FirebaseUser currentUser = fAuth.getCurrentUser();
        boolean rememberMe = sharedPreferences.getBoolean("rememberMe", false);

        if (currentUser != null && rememberMe) {
            String userId = currentUser.getUid();
            db.collection("users").document(userId).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String role = documentSnapshot.getString("role");
                            if (role != null) {
                                // 🔹 Save login state
                                editor.putBoolean("isLoggedIn", true);
                                editor.putString("role", role);
                                editor.apply();

                                redirectToDashboard(role);
                            } else {
                                Toast.makeText(Login.this, "Role not found!", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(Login.this, "User data not found", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(Login.this, "Error fetching user data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }


        requestNotifPermission =
                registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                    savePermissionState(isGranted);
                    if (isGranted) {
                        fetchUserRoleAndSchedule(tempUserIdForNotif);
                    } else {
                        fetchUserRoleAndRedirect(tempUserIdForNotif);
                    }
                });

        mforgotPassword.setOnClickListener(v -> startActivity(new Intent(Login.this, ForgotPassword.class)));
        mCreateBtn.setOnClickListener(v -> startActivity(new Intent(Login.this, Registration.class)));
        mLoginBtn.setOnClickListener(v -> handleLogin());
    }

    private void handleLogin() {
        String email = mEmail.getText().toString().trim();
        String password = mPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            mEmail.setError("Email is required");
            mEmail.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(password)) {
            mPassword.setError("Password is required");
            return;
        }

        fAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String userId = fAuth.getCurrentUser().getUid();
                        tempUserIdForNotif = userId;

                        db.collection("users").document(userId)
                                .get().addOnSuccessListener(documentSnapshot -> {
                                    if (documentSnapshot.exists()) {
                                        String status = documentSnapshot.getString("status");
                                        if ("pending".equals(status)) {
                                            Toast.makeText(Login.this, "Waiting for admin approval", Toast.LENGTH_LONG).show();
                                            fAuth.signOut();
                                        } else if ("active".equals(status)) {
                                            tempRoleForRedirect = documentSnapshot.getString("role");
                                            fullName = documentSnapshot.getString("fullName");

                                            // Save rememberMe choice
                                            editor.putBoolean("rememberMe", rememberMeCheckBox.isChecked());
                                            editor.apply();

                                            handleNotificationPermission();
                                        } else {
                                            Toast.makeText(Login.this, "Unknown status", Toast.LENGTH_SHORT).show();
                                            fAuth.signOut();
                                        }
                                    }
                                }).addOnFailureListener(e -> {
                                    Toast.makeText(Login.this, "Error fetching user status: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    fAuth.signOut();
                                });

                    } else {
                        Toast.makeText(Login.this, "Login failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void handleNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            boolean granted = sharedPreferences.getBoolean("notifGranted_" + tempUserIdForNotif, false);

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED) {
                savePermissionState(true);
                fetchUserRoleAndSchedule(tempUserIdForNotif);
            } else {
                requestNotifPermission.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        } else {
            fetchUserRoleAndSchedule(tempUserIdForNotif);
        }
    }

    private void savePermissionState(boolean granted) {
        editor.putBoolean("notifAsked_" + tempUserIdForNotif, true);
        editor.putBoolean("notifGranted_" + tempUserIdForNotif, granted);
        editor.apply();
    }

    private void fetchUserRoleAndSchedule(String userId) {
        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        tempRoleForRedirect = documentSnapshot.getString("role");
                        fullName = documentSnapshot.getString("fullName");

                        if (!"admin".equalsIgnoreCase(tempRoleForRedirect)) {
//                            scheduleFixedReminders();
                            listenAndScheduleReminders(userId);
                        }

                        redirectToDashboard(tempRoleForRedirect);
                    }
                });
    }

    private void fetchUserRoleAndRedirect(String userId) {
        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        tempRoleForRedirect = documentSnapshot.getString("role");
                        redirectToDashboard(tempRoleForRedirect);
                    }
                });
    }

    private void redirectToDashboard(String role) {
        if (role == null) return;

        switch (role.toLowerCase()) {
            case "admin":
                startActivity(new Intent(Login.this, AdminDashboard.class));
                break;
            case "department manager":
                startActivity(new Intent(Login.this, Dashboard.class));
                break;
            case "division manager":
                startActivity(new Intent(Login.this, DivisionMDashboared.class));
                break;
            case "section manager":
                startActivity(new Intent(Login.this, NonDivisionMDashboared.class));
                break;
            case "software engineer":
                startActivity(new Intent(Login.this, EmployeeDashboard.class));
                break;
            default:
                Toast.makeText(this, "Unknown role", Toast.LENGTH_SHORT).show();
        }
        finish();
    }

    //newly added
    // 🔹 Schedule reminders dynamically from Firestore
    // 🔹 Schedule exactly two reminders per day based on Firestore, auto-updates
    private void listenAndScheduleReminders(String userId) {
        db.collection("reminders")
                .whereEqualTo("userId", userId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Error listening to reminders: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String[] defaultTimes = {"08:30", "16:30"};
                    String[] times = new String[2];
                    String[] messages = new String[2];

                    // Fill times and messages from Firestore documents (max 2)
                    int index = 0;
                    if (value != null && !value.isEmpty()) {
                        for (var doc : value.getDocuments()) {
                            if (index >= 2) break; // only first 2 reminders
                            String timeStr = doc.getString("time");
                            String message = doc.getString("message");
                            if (timeStr != null && message != null) {
                                times[index] = timeStr;
                                messages[index] = message;
                                index++;
                            }
                        }
                    }

                    // Fill missing times/messages with defaults
                    for (int i = 0; i < 2; i++) {
                        if (times[i] == null) times[i] = defaultTimes[i];
                        if (messages[i] == null) messages[i] = "This is your reminder to update your daily task!!!" +
//                                "(string)\n" +
                                "\n" +
                                "\n!";
                    }

                    // Cancel existing scheduled reminders
                    for (int i = 0; i < 2; i++) {
                        WorkManager.getInstance(this)
                                .cancelUniqueWork("dailyReminder_" + times[i]);
                    }

                    // Schedule exactly two periodic reminders
                    for (int i = 0; i < 2; i++) {
                        schedulePeriodicNotification(times[i], messages[i]);
                    }
                });
    }


    // 🔹 Schedule periodic notifications
    private void schedulePeriodicNotification(String timeStr, String message) {
        String[] parts = timeStr.split(":");
        int hour = Integer.parseInt(parts[0]);
        int minute = Integer.parseInt(parts[1]);

        Calendar now = Calendar.getInstance();
        Calendar firstTrigger = (Calendar) now.clone();
        firstTrigger.set(Calendar.HOUR_OF_DAY, hour);
        firstTrigger.set(Calendar.MINUTE, minute);
        firstTrigger.set(Calendar.SECOND, 0);

        if (firstTrigger.before(now)) firstTrigger.add(Calendar.DAY_OF_MONTH, 1);

        long initialDelay = firstTrigger.getTimeInMillis() - now.getTimeInMillis();

        Data data = new Data.Builder()
                .putString("message", message)
                .putString("fullName", fullName)
                .putString("time", timeStr)
                .build();

        PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(
                ReminderWorker.class, 24, TimeUnit.HOURS)
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .setInputData(data)
                .build();

        WorkManager.getInstance(this)
                .enqueueUniquePeriodicWork(
                        "dailyReminder_" + timeStr,
                        ExistingPeriodicWorkPolicy.REPLACE, // ✅ fixed here
                        workRequest
                );

        Log.d("ReminderScheduler", "Scheduling notification for " + timeStr);

    }


    public static class ReminderWorker extends Worker {
        private static final String CHANNEL_ID = "daily_reminder_channel";

        public ReminderWorker(@NonNull Context context, @NonNull WorkerParameters params) {
            super(context, params);
        }

        @NonNull
        @Override
        public Result doWork() {
            String message = getInputData().getString("message");
            String fullName = getInputData().getString("fullName");
            String timeStr = getInputData().getString("time");

            showNotification("Hey " + fullName + ", " + message, timeStr);

            // Schedule same reminder for next day
            //scheduleNextDay(timeStr, message, fullName);

            return Result.success();

        }

        private void showNotification(String text, String timeStr) {
            Context context = getApplicationContext();
            NotificationManager notificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID, "Daily Reminder", NotificationManager.IMPORTANCE_HIGH);
                notificationManager.createNotificationChannel(channel);
            }

            Intent intent = new Intent(context, Login.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    context, 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setContentTitle("Daily Task Reminder")
                    .setContentText(text)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_HIGH);

            notificationManager.notify(timeStr.hashCode(), builder.build());
        }

//        private void scheduleNextDay(String timeStr, String message, String fullName) {
//            String[] parts = timeStr.split(":");
//            int hour = Integer.parseInt(parts[0]);
//            int minute = Integer.parseInt(parts[1]);
//
//            Calendar now = Calendar.getInstance();
//            Calendar nextTime = (Calendar) now.clone();
//            nextTime.set(Calendar.HOUR_OF_DAY, hour);
//            nextTime.set(Calendar.MINUTE, minute);
//            nextTime.set(Calendar.SECOND, 0);
//            nextTime.add(Calendar.DAY_OF_MONTH, 1);
//
//            long delay = nextTime.getTimeInMillis() - now.getTimeInMillis();
//
//            Data data = new Data.Builder()
//                    .putString("message", message)
//                    .putString("fullName", fullName)
//                    .putString("time", timeStr)
//                    .build();
//
//            OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(ReminderWorker.class)
//                    .setInitialDelay(delay, TimeUnit.MILLISECONDS)
//                    .setInputData(data)
//                    .build();
//
//            WorkManager.getInstance(getApplicationContext())
//                    .enqueueUniqueWork("dailyReminder_" + timeStr, ExistingWorkPolicy.REPLACE, workRequest);
//        }


    }
}