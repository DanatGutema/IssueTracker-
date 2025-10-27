package com.example.issuetracker;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.PopupMenu;
import android.widget.Toast;
import com.google.firebase.auth.FirebaseAuth;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.content.SharedPreferences;
import android.view.MenuItem;
import android.view.MenuItem;
import androidx.annotation.NonNull;



public class DivisionMDashboared extends AppCompatActivity {

    Button btnIssueList, btnAnnualReportNonD, btnReports, btnReminders, btnStatus;
    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_division_mdashboared);

        // Setup Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Remove default title
        getSupportActionBar().setTitle("");

        // Dashboard feature buttons
        btnIssueList = findViewById(R.id.btnIssueListNonD);
        btnAnnualReportNonD = findViewById(R.id.btnAnnualReportNonD);
        btnReports = findViewById(R.id.btnReportsNonD);
       // btnReminders = findViewById(R.id.btnRemindersNonD);
        btnStatus = findViewById(R.id.btnStatusNonD);

        btnIssueList.setOnClickListener(v -> {
            startActivity(new Intent(DivisionMDashboared.this, AddEditIssue.class));
        });

//        btnCharts.setOnClickListener(v -> {
//            //startActivity(new Intent(DivisionMDashboared.this, Charts.class));
//        });

        btnReports.setOnClickListener(v -> {
            startActivity(new Intent(DivisionMDashboared.this, Report.class));
        });

        //btnReminders.setOnClickListener(v -> {
          //  startActivity(new Intent(DivisionMDashboared.this, Reminder.class));
        //});

        btnStatus.setOnClickListener(v -> {
            Intent intent = new Intent(DivisionMDashboared.this, Update_Status.class);
            intent.putExtra("userId", FirebaseAuth.getInstance().getCurrentUser().getUid());
            startActivity(intent);
        });


        btnAnnualReportNonD.setOnClickListener(v -> {
            startActivity(new Intent(DivisionMDashboared.this, AnnualDashboard.class));
        });

//        // for dropdown of type filter
//        btnAnnualReportNonD.setOnClickListener(v -> {
//            PopupMenu popup = new PopupMenu(this, v);
//
//            // Load array from resources
//            String[] types = getResources().getStringArray(R.array.AnnualReport);
//
//            for (String type : types) {
//                popup.getMenu().add(type);
//            }
//
//            popup.setOnMenuItemClickListener(item -> {
//                String selectedType = item.getTitle().toString();
//                if (selectedType.equals("Q1")) {
//                    // 🔹 Go to AnnualReport.java when Q1 is clicked
//                    Intent intent = new Intent(DivisionMDashboared.this, AnnualReport.class);
//                    //intent.putExtra("reportType", "Q1"); // send extra if you need to know which one
//                    startActivity(intent);
//                }
//                if (selectedType.equals("Q2")) {
//                    // 🔹 Go to AnnualReport.java when Q1 is clicked
//                    Intent intent = new Intent(DivisionMDashboared.this, AnnualReport.class);
//                    // intent.putExtra("reportType", "Q2"); // send extra if you need to know which one
//                    startActivity(intent);
//                }
//                if (selectedType.equals("Q3")) {
//                    // 🔹 Go to AnnualReport.java when Q1 is clicked
//                    Intent intent = new Intent(DivisionMDashboared.this, AnnualReport.class);
//                    //intent.putExtra("reportType", "Q3"); // send extra if you need to know which one
//                    startActivity(intent);
//                }
//                if (selectedType.equals("Q4")) {
//                    // 🔹 Go to AnnualReport.java when Q1 is clicked
//                    Intent intent = new Intent(DivisionMDashboared.this, AnnualReport.class);
//                    //intent.putExtra("reportType", "Q4"); // send extra if you need to know which one
//                    startActivity(intent);
//                }
//                if (selectedType.equals("The First 6 Months")) {
//                    // 🔹 Go to AnnualReport.java when Q1 is clicked
//                    Intent intent = new Intent(DivisionMDashboared.this, AnnualReport.class);
//                    //intent.putExtra("reportType", "The First 6 Months"); // send extra if you need to know which one
//                    startActivity(intent);
//                }
//                if (selectedType.equals("The Second 6 Months")) {
//                    // 🔹 Go to AnnualReport.java when Q1 is clicked
//                    Intent intent = new Intent(DivisionMDashboared.this, AnnualReport.class);
//                    //intent.putExtra("reportType", "The Second 6 Months"); // send extra if you need to know which one
//                    startActivity(intent);
//                }
//                if (selectedType.equals("Annual")) {
//                    // 🔹 Go to AnnualReport.java when Q1 is clicked
//                    Intent intent = new Intent(DivisionMDashboared.this, AnnualReport.class);
//                    // intent.putExtra("reportType", "Annual"); // send extra if you need to know which one
//                    startActivity(intent);
//                }
////                else {
////                    // 🔹 For others, just filter the table (you can expand later)
////                    filterReportByQuarter(selectedType);
////                }
//                return true;
//            });
//            popup.show();
//        });





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
            startActivity(new Intent(DivisionMDashboared.this, Profile.class));
            return true;
        } else if (id == R.id.menu_settings) {
//            startActivity(new Intent(DivisionMDashboared.this, Settings.class));
//            return true;
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
            Intent intent = new Intent(DivisionMDashboared.this, Login.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish(); // Close current activity
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
