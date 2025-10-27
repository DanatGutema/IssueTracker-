package com.example.issuetracker;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.widget.Toast;


import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

//public class Reminder extends AppCompatActivity {

  //  private FirebaseFirestore db;
    //private FirebaseAuth auth;
    //private String fullName;

    //@Override
   // protected void onCreate(Bundle savedInstanceState) {
      //  super.onCreate(savedInstanceState);
       // setContentView(R.layout.activity_reminder);

        // Request notification permission for Android 13+
       /*
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                        1001); // arbitrary request code
            }
        }

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Get current user's full name
        db.collection("users").document(auth.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        fullName = doc.getString("fullName"); // make sure "fullName" exists in user doc
                        fetchAndScheduleReminder();
                    }
                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1001) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Notification permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }


    private void fetchAndScheduleReminder() {
        db.collection("reminders").document("dailyReminder")
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String message = doc.getString("message");
                        String timeStr = doc.getString("time"); // format: "HH:mm" e.g. "09:00"

                        scheduleDailyNotification(timeStr, message);
                    }
                });
    }

    private void scheduleDailyNotification(String timeStr, String message) {
        String[] parts = timeStr.split(":");
        int hour = Integer.parseInt(parts[0]);
        int minute = Integer.parseInt(parts[1]);

        Calendar now = Calendar.getInstance();
        Calendar dueTime = (Calendar) now.clone();
        dueTime.set(Calendar.HOUR_OF_DAY, hour);
        dueTime.set(Calendar.MINUTE, minute);
        dueTime.set(Calendar.SECOND, 0);

        if (dueTime.before(now)) {
            dueTime.add(Calendar.DAY_OF_MONTH, 1);
        }

       // long delay = dueTime.getTimeInMillis() - now.getTimeInMillis();

        long delay = 10 * 1000; // 10 seconds
        Data data = new Data.Builder()
                .putString("message", message)
                .putString("fullName", fullName)
                .build();

        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(ReminderWorkerInner.class)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(data)
                .build();

        WorkManager.getInstance(this)
                .enqueueUniqueWork("dailyReminder", ExistingWorkPolicy.REPLACE, workRequest);
    }

    // ------------------- Inner Worker Class -------------------
    public static class ReminderWorkerInner extends Worker {

        private static final String CHANNEL_ID = "daily_reminder_channel";

        public ReminderWorkerInner(@NonNull Context context, @NonNull WorkerParameters params) {
            super(context, params);
        }

        @NonNull
        @Override
        public Result doWork() {
            String message = getInputData().getString("message");
            String fullName = getInputData().getString("fullName");

            showNotification("Hey " + fullName + ", " + message);

            // Reschedule for next day
            scheduleNextDay();

            return Result.success();
        }

        private void showNotification(String text) {
            Context context = getApplicationContext();

            NotificationManager notificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID, "Daily Reminder", NotificationManager.IMPORTANCE_HIGH);
                notificationManager.createNotificationChannel(channel);
            }

            Intent intent = new Intent(context, Reminder.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setContentTitle("Daily Task Reminder")
                    .setContentText(text)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)// replace with your app icon
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_HIGH);

            notificationManager.notify(1001, builder.build());
        }

        private void scheduleNextDay() {
            Data data = getInputData();

            long delay = 24 * 60 * 60 * 1000; //in every 24 hours it sends reminder text

            //long delay = 70 * 1000; // in every 70 seconds it sends reminder text
            OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(ReminderWorkerInner.class)
                    .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                    .setInputData(data)
                    .build();

            WorkManager.getInstance(getApplicationContext())
                    .enqueueUniqueWork("dailyReminder", ExistingWorkPolicy.REPLACE, workRequest);
        }
    }
}

        */