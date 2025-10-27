package com.example.issuetracker;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Report extends AppCompatActivity {

    private ImageView ivCalendar, ivDownload;
    private EditText etSearchBank;
    private TextView tvDateRange;
    private TableLayout tableReport;

    private Calendar startDateCal, endDateCal;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private String userId, userDept, userSection, userRole;

    private final List<Map<String, Object>> reportData = new ArrayList<>();
    private final SimpleDateFormat displayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);

        ivCalendar = findViewById(R.id.ivCalendar);
        ivDownload = findViewById(R.id.ivDownload);
        etSearchBank = findViewById(R.id.etSearchBank);
        tableReport = findViewById(R.id.tableReport);
        //tvDateRange = findViewById(R.id.tvDateRange);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Not signed in!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        userId = mAuth.getCurrentUser().getUid();

        // Get logged-in user's role, dept, section
        db.collection("users").document(userId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        userRole = doc.getString("role");
                        userDept = doc.getString("dept");
                        userSection = doc.getString("section");
                    }
                });

        ivCalendar.setOnClickListener(v -> openDateRangePicker());

        etSearchBank.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterAndDisplayData(s.toString());
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });

        ivDownload.setOnClickListener(v -> exportToExcel(reportData));
    }

    private void openDateRangePicker() {
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog startPicker = new DatePickerDialog(this, (view, y, m, d) -> {
            startDateCal = Calendar.getInstance();
            startDateCal.set(y, m, d, 0, 0, 0);

            DatePickerDialog endPicker = new DatePickerDialog(this, (view2, y2, m2, d2) -> {
                endDateCal = Calendar.getInstance();
                endDateCal.set(y2, m2, d2, 23, 59, 59);

                tvDateRange.setText(displayDate.format(startDateCal.getTime()) + " - " +
                        displayDate.format(endDateCal.getTime()));

                fetchReportData();
            }, year, month, day);
            endPicker.show();

        }, year, month, day);
        startPicker.show();
    }

    private void fetchReportData() {
        if (startDateCal == null || endDateCal == null) return;

        reportData.clear();
        tableReport.removeAllViews();

        Date startDate = startDateCal.getTime();
        Date endDate = endDateCal.getTime();

        Query query = db.collection("issues")
                .whereGreaterThanOrEqualTo("timestamp", startDate)
                .whereLessThanOrEqualTo("timestamp", endDate);

        // Apply filtering based on role
        if ("Employee".equalsIgnoreCase(userRole)) {
            query = query.whereEqualTo("createdBy", userId);
        } else if ("Manager".equalsIgnoreCase(userRole)) {
            query = query.whereEqualTo("dept", userDept)
                    .whereEqualTo("section", userSection);
        } else if ("Division Manager".equalsIgnoreCase(userRole)) {
            query = query.whereEqualTo("dept", userDept);
        } // Admin sees all

        query.get().addOnSuccessListener((QuerySnapshot queryDocumentSnapshots) -> {
            List<DocumentSnapshot> docs = queryDocumentSnapshots.getDocuments();
            if (docs.isEmpty()) {
                Toast.makeText(this, "No issues in selected range", Toast.LENGTH_SHORT).show();
                filterAndDisplayData(etSearchBank.getText().toString());
                return;
            }

            AtomicInteger pending = new AtomicInteger(docs.size());

            for (DocumentSnapshot issueDoc : docs) {
                Map<String, Object> row = new HashMap<>();

                row.put("issueNumber", safeString(issueDoc.get("Number")));
                row.put("createdBy", safeString(issueDoc.get("createdBy")));
                row.put("section", safeString(issueDoc.get("section")));
                row.put("title", safeString(issueDoc.get("title")));
                row.put("description", safeString(issueDoc.get("description")));
                row.put("bank", safeString(issueDoc.get("bankName")));
                row.put("branch", safeString(issueDoc.get("bankLocation")));
                row.put("priority", safeString(issueDoc.get("priority")));
                row.put("status", safeString(issueDoc.get("status")));

                Object repObj = issueDoc.get("timestamp");
                row.put("reportedDate", formatDateObject(repObj));
                row.put("resolvedDate", "-");

                reportData.add(row);

                if (pending.decrementAndGet() == 0) {
                    // sort by number
                    reportData.sort((a, b) -> {
                        String na = a.get("issueNumber").toString();
                        String nb = b.get("issueNumber").toString();
                        return na.compareTo(nb);
                    });
                    filterAndDisplayData(etSearchBank.getText().toString());
                }
            }
        });
    }

    private void filterAndDisplayData(String bankNameFilter) {
        tableReport.removeAllViews();

        TableRow header = new TableRow(this);
        String[] headers = {"Number","Name","Section", "Title", "Description", "Bank", "Branch",
                "Reported Date", "Priority", "Status", "Resolved Date"};
        for (String h : headers) {
            TextView tv = new TextView(this);
            tv.setText(h);
            tv.setPadding(16, 16, 16, 16);
            tv.setTextSize(14);
            tv.setTypeface(tv.getTypeface(), android.graphics.Typeface.BOLD);
            tv.setGravity(Gravity.CENTER);
            header.addView(tv);
        }
        tableReport.addView(header);

        boolean hasData = false;
        for (Map<String, Object> row : reportData) {
            String bank = row.get("bank") != null ? row.get("bank").toString() : "-";
            if (!TextUtils.isEmpty(bankNameFilter) &&
                    !bank.toLowerCase().contains(bankNameFilter.toLowerCase())) continue;

            hasData = true;
            TableRow tr = new TableRow(this);
            tr.addView(createCell(String.valueOf(row.getOrDefault("issueNumber", "-"))));
            tr.addView(createCell(String.valueOf(row.getOrDefault("createdBy", "-"))));
            tr.addView(createCell(String.valueOf(row.getOrDefault("section", "-"))));
            tr.addView(createCell(String.valueOf(row.getOrDefault("title", "-"))));
            tr.addView(createCell(String.valueOf(row.getOrDefault("description", "-"))));
            tr.addView(createCell(String.valueOf(row.getOrDefault("bank", "-"))));
            tr.addView(createCell(String.valueOf(row.getOrDefault("branch", "-"))));
            tr.addView(createCell(String.valueOf(row.getOrDefault("reportedDate", "-"))));
            tr.addView(createCell(String.valueOf(row.getOrDefault("priority", "-"))));
            tr.addView(createCell(String.valueOf(row.getOrDefault("status", "-"))));
            tr.addView(createCell(String.valueOf(row.getOrDefault("resolvedDate", "-"))));

            tableReport.addView(tr);
        }
        ivDownload.setVisibility(hasData ? View.VISIBLE : View.GONE);
    }

    private TextView createCell(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setPadding(16, 16, 16, 16);
        tv.setTextSize(13);
        tv.setMaxLines(4);
        tv.setEllipsize(TextUtils.TruncateAt.END);
        return tv;
    }

    private void exportToExcel(List<Map<String, Object>> data) {
        if (data.isEmpty()) {
            Toast.makeText(this, "No data to export", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("Report");
            String[] headers = {"Number", "Name", "Section", "Title", "Description", "Bank", "Branch",
                    "Reported Date", "Priority", "Status", "Resolved Date"};

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) headerRow.createCell(i).setCellValue(headers[i]);

            int rowIdx = 1;
            for (Map<String, Object> row : data) {
                Row excelRow = sheet.createRow(rowIdx++);
                excelRow.createCell(0).setCellValue(String.valueOf(row.getOrDefault("issueNumber", "-")));
                excelRow.createCell(1).setCellValue(String.valueOf(row.getOrDefault("createdBy", "-")));
                excelRow.createCell(2).setCellValue(String.valueOf(row.getOrDefault("section", "-")));
                excelRow.createCell(1).setCellValue(String.valueOf(row.getOrDefault("title", "-")));
                excelRow.createCell(2).setCellValue(String.valueOf(row.getOrDefault("description", "-")));
                excelRow.createCell(3).setCellValue(String.valueOf(row.getOrDefault("bank", "-")));
                excelRow.createCell(4).setCellValue(String.valueOf(row.getOrDefault("branch", "-")));
                excelRow.createCell(5).setCellValue(String.valueOf(row.getOrDefault("reportedDate", "-")));
                excelRow.createCell(6).setCellValue(String.valueOf(row.getOrDefault("priority", "-")));
                excelRow.createCell(7).setCellValue(String.valueOf(row.getOrDefault("status", "-")));
                excelRow.createCell(8).setCellValue(String.valueOf(row.getOrDefault("resolvedDate", "-")));
            }

            for (int i = 0; i < headers.length; i++) sheet.setColumnWidth(i, 20 * 256);

            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (!downloadsDir.exists()) downloadsDir.mkdir();

            String filename = "IssueReport_" + System.currentTimeMillis() + ".xlsx";
            File file = new File(downloadsDir, filename);

            FileOutputStream fos = new FileOutputStream(file);
            workbook.write(fos);
            fos.close();
            workbook.close();

            Toast.makeText(this, "Saved: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Export error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    private String safeString(Object o) {
        if (o == null) return "-";
        String s = String.valueOf(o);
        return s.trim().isEmpty() ? "-" : s;
    }

    private String formatDateObject(Object obj) {
        if (obj == null) return "-";
        try {
            if (obj instanceof Timestamp) return displayDate.format(((Timestamp) obj).toDate());
            if (obj instanceof Date) return displayDate.format((Date) obj);
            if (obj instanceof Long) return displayDate.format(new Date((Long) obj));
            return String.valueOf(obj);
        } catch (Exception e) {
            return String.valueOf(obj);
        }
    }
}
