package com.example.issuetracker;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class AnnualDashboard extends AppCompatActivity {

    Button btnFirst, btnSecond, btnThird, btnFourth, btnFirstSix, btnSecondSix, btnAnnual;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_annual_dashboard);



        btnFirst = findViewById(R.id.btnFirst);
        btnSecond = findViewById(R.id.btnSecond);
        btnThird = findViewById(R.id.btnThird);
        btnFourth = findViewById(R.id.btnFourth);
        btnFirstSix = findViewById(R.id.btnFirstSix);
        btnSecondSix = findViewById(R.id.btnSecondSix);
        btnAnnual = findViewById(R.id.btnAnnual);



        btnFirst.setOnClickListener(v -> {
            Intent intent = new Intent(AnnualDashboard.this, AnnualReport.class);
            intent.putExtra("reportType", "Q1");  // <-- send Q1
            startActivity(intent);
        });

        btnSecond.setOnClickListener(v -> {
            Intent intent = new Intent(AnnualDashboard.this, AnnualReport.class);
            intent.putExtra("reportType", "Q2");  // <-- send Q2
            startActivity(intent);
        });

        btnThird.setOnClickListener(v -> {
            Intent intent = new Intent(AnnualDashboard.this, AnnualReport.class);
            intent.putExtra("reportType", "Q3");  // <-- send Q3
            startActivity(intent);
        });

        btnFourth.setOnClickListener(v -> {
            Intent intent = new Intent(AnnualDashboard.this, AnnualReport.class);
            intent.putExtra("reportType", "Q4");  // <-- send Q4
            startActivity(intent);
        });

        btnFirstSix.setOnClickListener(v -> {
            Intent intent = new Intent(AnnualDashboard.this, AnnualReport.class);
            intent.putExtra("reportType", "The First 6 Months");  // <-- first half
            startActivity(intent);
        });

        btnSecondSix.setOnClickListener(v -> {
            Intent intent = new Intent(AnnualDashboard.this, AnnualReport.class);
            intent.putExtra("reportType", "The Second 6 Months");  // <-- second half
            startActivity(intent);
        });

        btnAnnual.setOnClickListener(v -> {
            Intent intent = new Intent(AnnualDashboard.this, AnnualReport.class);
            intent.putExtra("reportType", "Annual");  // <-- full year
            startActivity(intent);
        });

    }
}