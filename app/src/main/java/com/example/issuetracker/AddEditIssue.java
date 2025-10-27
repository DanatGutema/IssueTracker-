package com.example.issuetracker;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class AddEditIssue extends AppCompatActivity {

    Button btnAdd, btnEdit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_issue); // your XML layout

        // Initialize buttons
        btnAdd = findViewById(R.id.btnAdd);
        btnEdit = findViewById(R.id.btnEdit);

        // Navigate to Issue List when Add is clicked
        btnAdd.setOnClickListener(v -> {
            Intent intent = new Intent(AddEditIssue.this, IssueList.class);
            startActivity(intent);
        });

        // Navigate to Edit Issue page when Edit is clicked
        btnEdit.setOnClickListener(v -> {
            Intent intent = new Intent(AddEditIssue.this, Edit.class);
            startActivity(intent);
        });
    }
}
