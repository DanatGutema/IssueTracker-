package com.example.issuetracker;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.HashMap;



public class Edit extends AppCompatActivity {

    private TableLayout issuesTable;
    private FirebaseFirestore db;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit);

        issuesTable = findViewById(R.id.issuesTable);
        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        addTableHeader(); // add column headers
        loadUserIssues();
    }

    private void addTableHeader() {
        TableRow headerRow = new TableRow(this);

        TextView idHeader = createCell("Issue ID", true);
        TextView titleHeader = createCell("Title", true);
        TextView actionHeader = createCell("Action", true);

        headerRow.addView(idHeader);
        headerRow.addView(titleHeader);
        headerRow.addView(actionHeader);

        issuesTable.addView(headerRow);
    }

    private void loadUserIssues() {
        db.collection("issues")
                .whereEqualTo("createdBy", userId)
                //i added this line
                //.orderBy("Number") // order ascending by issueNumber
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    List<QueryDocumentSnapshot> docs = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        docs.add(doc);
                    }

                    // Map to hold the most recent doc for each issue Number
                    Map<String, QueryDocumentSnapshot> latestDocs = new HashMap<>();

                    for (QueryDocumentSnapshot doc : docs) {
                        String num = doc.getString("Number");
                        if (num == null) num = "0";

                        long currentTime = 0;
                        if (doc.contains("timestamp")) {
                            Object tsObj = doc.get("timestamp");
                            if (tsObj instanceof com.google.firebase.Timestamp) {
                                currentTime = ((com.google.firebase.Timestamp) tsObj).toDate().getTime();
                            }
                        }

                        if (!latestDocs.containsKey(num)) {
                            latestDocs.put(num, doc);
                        } else {
                            QueryDocumentSnapshot existing = latestDocs.get(num);

                            long existingTime = 0;
                            if (existing.contains("timestamp")) {
                                Object tsObj = existing.get("timestamp");
                                if (tsObj instanceof com.google.firebase.Timestamp) {
                                    existingTime = ((com.google.firebase.Timestamp) tsObj).toDate().getTime();
                                }
                            }

                            // Replace if this doc is more recent
                            if (currentTime > existingTime) {
                                latestDocs.put(num, doc);
                            }
                        }
                    }

                    // Convert map values back to list and sort numerically by Number
                    List<QueryDocumentSnapshot> uniqueDocs = new ArrayList<>(latestDocs.values());
                    uniqueDocs.sort((doc1, doc2) -> {
                        String num1 = doc1.getString("Number");
                        String num2 = doc2.getString("Number");

                        if (num1 == null) num1 = "0";
                        if (num2 == null) num2 = "0";

                        // If you want numeric sort (001, 2, 10 properly ordered)
                        return Integer.compare(
                                Integer.parseInt(num1),
                                Integer.parseInt(num2)
                        );
                    });

                    for (QueryDocumentSnapshot doc : uniqueDocs) {
                        String issueId = doc.getId();
                        String issueNumber = doc.getString("Number");
                        String title = doc.getString("title");

                        if (issueNumber == null || issueNumber.isEmpty()) issueNumber = "-";
                        if (title == null || title.isEmpty()) title = "-";

                        addIssueRow(issueId, issueNumber, title);
                    }
                })
                .addOnFailureListener(e -> {
                    TableRow errorRow = new TableRow(this);
                    TextView errorCell = createCell("Error: " + e.getMessage(), false);
                    errorCell.setTextColor(Color.RED);
                    errorRow.addView(errorCell);
                    issuesTable.addView(errorRow);
                });
    }

    private void addIssueRow(String issueId, String issueNumber, String title) {
        TableRow row = new TableRow(this);

        TextView idCell = createCell(issueNumber, false);
        TextView titleCell = createCell(title, false);

        Button editBtn = new Button(this);
        editBtn.setText("Edit");
        editBtn.setTextSize(10);
        editBtn.setTextColor(Color.WHITE);
        editBtn.setBackgroundResource(R.drawable.edit_button_style);

        // set fixed small size
//        TableRow.LayoutParams params = new TableRow.LayoutParams(
//                TableRow.LayoutParams.WRAP_CONTENT,
//                TableRow.LayoutParams.WRAP_CONTENT
//        );
        // Force smaller height
        int heightInDp = 32; // adjust as needed (default ~48dp)
        TableRow.LayoutParams params = new TableRow.LayoutParams(
                TableRow.LayoutParams.WRAP_CONTENT,
                dpToPx(heightInDp) // <-- use dpToPx helper
        );

        params.setMargins(8, 0, 8, 8);
        editBtn.setLayoutParams(params);

        editBtn.setPadding(10, 3, 10, 3); // reduce padding inside

        editBtn.setOnClickListener(v -> {
            Intent intent = new Intent(Edit.this, EditIssue.class);
            intent.putExtra("issueId", issueId);
            startActivity(intent);
        });

        row.addView(idCell);
        row.addView(titleCell);
        row.addView(editBtn);

        issuesTable.addView(row);
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private TextView createCell(String text, boolean isHeader) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setPadding(16, 16, 16, 16);
        tv.setTextSize(13);
        tv.setMaxLines(4);
        tv.setTextColor(Color.BLACK);
        // Apply border
        tv.setBackgroundResource(R.drawable.cell_border);
        tv.setEllipsize(TextUtils.TruncateAt.END);


        if (isHeader) {
            tv.setTextColor(Color.WHITE); // make header text white
            tv.setBackgroundResource(R.drawable.header_cell_border);
            //tv.setBackgroundColor(Color.parseColor("#BACAEB"));//header bg
            tv.setTypeface(null, android.graphics.Typeface.BOLD); // make header text bold
            tv.setTextSize(15);
        }
        return tv;
    }

}