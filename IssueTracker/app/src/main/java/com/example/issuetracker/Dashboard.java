package com.example.issuetracker;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.Toast;
import com.google.firebase.auth.FirebaseAuth;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.content.SharedPreferences;
import android.view.MenuItem;
import android.view.MenuItem;
import androidx.annotation.NonNull;



public class Dashboard extends AppCompatActivity {

    Button btnIssueList, btnCharts, btnReports, btnReminders, btnStatus;
    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        // Setup Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Remove default title
        getSupportActionBar().setTitle("");

        // Dashboard feature buttons
        btnIssueList = findViewById(R.id.btnIssueList);
        btnCharts = findViewById(R.id.btnCharts);
        btnReports = findViewById(R.id.btnReports);
        btnReminders = findViewById(R.id.btnReminders);
        btnStatus = findViewById(R.id.btnStatus);

        btnIssueList.setOnClickListener(v -> {
            startActivity(new Intent(Dashboard.this, AddEditIssue.class));
        });

        btnCharts.setOnClickListener(v -> {
            startActivity(new Intent(Dashboard.this, Charts.class));
        });

        btnReports.setOnClickListener(v -> {
            startActivity(new Intent(Dashboard.this, Report.class));
        });

        btnReminders.setOnClickListener(v -> {
            startActivity(new Intent(Dashboard.this, Reminder.class));
        });

        btnStatus.setOnClickListener(v -> {
            Intent intent = new Intent(Dashboard.this, Update_Status.class);
            intent.putExtra("userId", FirebaseAuth.getInstance().getCurrentUser().getUid());
            startActivity(intent);
        });
    }

    // Inflate menu (Profile, Settings, Logout)
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_dashboard, menu);
        return true;
    }

    // Handle menu item clicks
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.menu_profile) {
            startActivity(new Intent(Dashboard.this, Profile.class));
            return true;
        } else if (id == R.id.menu_settings) {
            startActivity(new Intent(Dashboard.this, Settings.class));
            return true;
        } else if (id == R.id.menu_logout) {
            //  startActivity(new Intent(Dashboard.this, About.class));
            //    return true;

            // 🔹 Clear SharedPreferences
            SharedPreferences sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.clear();
            editor.apply();

            // 🔹 Firebase logout
            FirebaseAuth.getInstance().signOut();

            // 🔹 Redirect back to Login page
            Intent intent = new Intent(Dashboard.this, Login.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish(); // Close current activity
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
