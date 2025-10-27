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



public class DptManagerVisualizePage extends AppCompatActivity {

    Button  btnPOS, btnATM, btnVoiceGuide, btnAxisCamera;
    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dpt_manager_visualize_page);

        // Setup Toolbar
        //Toolbar toolbar = findViewById(R.id.toolbar);
        //setSupportActionBar(toolbar);

        // Remove default title
       // getSupportActionBar().setTitle("");

        // Dashboard feature buttons
        btnPOS = findViewById(R.id.btnPOS);
        btnATM = findViewById(R.id.btnATM);
        btnVoiceGuide = findViewById(R.id.btnVoiceGuide);
        //btnReminders = findViewById(R.id.btnRemindersNon);
        btnAxisCamera = findViewById(R.id.btnAxisCamera);

        btnPOS.setOnClickListener(v -> {
            //startActivity(new Intent(DptManagerVisualizePage.this, POSVisualize.class));
        });

        btnATM.setOnClickListener(v -> {
            //startActivity(new Intent(DptManagerVisualizePage.this, ATMVisualize.class));
        });

        btnVoiceGuide.setOnClickListener(v -> {
            //startActivity(new Intent(DptManagerVisualizePage.this, VoiceVisualize.class));
        });

        // btnReminders.setOnClickListener(v -> {
        //   startActivity(new Intent(NonDivisionMDashboared.this, Reminder.class));
        //});

        btnAxisCamera.setOnClickListener(v -> {
            //Intent intent = new Intent(DptManagerVisualizePage.this, AxisVisualize.class);
            //intent.putExtra("userId", FirebaseAuth.getInstance().getCurrentUser().getUid());
           // startActivity(intent);
           // startActivity(new Intent(DptManagerVisualizePage.this, AxisVisualize.class));
        });
    }

    // Inflate menu (Profile, Settings, Logout)
    //@Override
    //public boolean onCreateOptionsMenu(Menu menu) {
     //   getMenuInflater().inflate(R.menu.menu_dashboard, menu);
       // return true;
    }

    // Handle menu item clicks
    /*
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.menu_profile) {
            startActivity(new Intent(DptManagerVisualizePage.this, Profile.class));
            return true;
        } else if (id == R.id.menu_settings) {
            startActivity(new Intent(DptManagerVisualizePage.this, Settings.class));
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
            Intent intent = new Intent(DptManagerVisualizePage.this, Login.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish(); // Close current activity
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
*/
