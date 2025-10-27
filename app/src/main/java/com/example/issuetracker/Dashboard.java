package com.example.issuetracker;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.PopupMenu;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import com.google.firebase.auth.FirebaseAuth;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.content.SharedPreferences;
import android.view.MenuItem;
import android.view.MenuItem;
import androidx.annotation.NonNull;



public class Dashboard extends AppCompatActivity {

    Button btnIssueList, btnAnnualReport, btnReports, btnReminders, btnStatus;
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
       // btnIssueList = findViewById(R.id.btnIssueList);
        btnAnnualReport = findViewById(R.id.btnAnnualReport);
        btnReports = findViewById(R.id.btnReports);


        btnAnnualReport.setOnClickListener(v -> {
            startActivity(new Intent(Dashboard.this, AnnualDashboard.class));
        });

        btnReports.setOnClickListener(v -> {
            startActivity(new Intent(Dashboard.this, Report.class));
        });

    }



//    private void filterReportByQuarter(String selectedType) {
//        TableLayout table = findViewById(R.id.tableReport);
//
//        for (int i = 0; i < table.getChildCount(); i++) {
//            View rowView = table.getChildAt(i);
//
//            if (!(rowView instanceof TableRow)) continue;
//
//            TableRow row = (TableRow) rowView;
//
//            // Skip header rows if needed, or treat all rows the same
//            TextView typeCell = (TextView) row.getChildAt(2); // change 2 to your Type column index
//            if (typeCell == null) continue;
//
//            String typeText = typeCell.getText().toString().trim();
//
//            if (selectedType.equals("Select Type") || typeText.equalsIgnoreCase(selectedType)) {
//                row.setVisibility(View.VISIBLE); // show matching rows
//            } else {
//                row.setVisibility(View.GONE);    // hide non-matching rows
//            }
//        }
//    }





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
//            startActivity(new Intent(Dashboard.this, Settings.class));
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
            Intent intent = new Intent(Dashboard.this, Login.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish(); // Close current activity
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
