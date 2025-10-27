package com.example.issuetracker;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import android.graphics.Color;

public class Edit extends AppCompatActivity {

    private LinearLayout issuesContainer;
    private FirebaseFirestore db;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit);

        issuesContainer = findViewById(R.id.issuesContainer);
        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        loadUserIssues();
    }

    private void loadUserIssues() {
        db.collection("issues")
                .whereEqualTo("createdBy", userId)  // fetch only this user's issues
                //.orderBy("timestamp")  // or "timestamp"
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String issueId = doc.getId();
                        String issueNumber = doc.getString("Number");
                        String title = doc.getString("title");
// Use "-" if field is null
                        if (issueNumber == null || issueNumber.isEmpty()) issueNumber = "-";
                        if (title == null || title.isEmpty()) title = "-";

                        addIssueRow(issueId, issueNumber, title);
                    }
                })
                .addOnFailureListener(e -> {
                    e.printStackTrace();
                    TextView error = new TextView(this);
                    error.setText("Error loading issues: " + e.getMessage());
                    error.setTextColor(Color.RED);
                    issuesContainer.addView(error);
                });
    }

    private void addIssueRow(String issueId, String issueNumber, String title) {
        // parent layout
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(10, 10, 10, 10);

        // issue number + title
        TextView textView = new TextView(this);
        textView.setText(issueNumber + " - " + title);
        textView.setTextSize(16); // <-- set text size in SP
        textView.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        // edit button
        Button editBtn = new Button(this);
        editBtn.setText("Edit");

        // the color of the text and button
        textView.setTextColor(Color.parseColor("#000000")); // text color
        editBtn.setBackgroundColor(Color.parseColor("#407AD5")); // button color
        editBtn.setTextColor(Color.WHITE); // button text color

        editBtn.setOnClickListener(v -> {
            Intent intent = new Intent(Edit.this, EditIssue.class);
            intent.putExtra("issueId", issueId); // pass issue id for editing
            startActivity(intent);
        });

        row.addView(textView);
        row.addView(editBtn);
        issuesContainer.addView(row);
    }
}
