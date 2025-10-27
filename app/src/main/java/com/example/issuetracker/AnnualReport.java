package com.example.issuetracker;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;

import android.widget.PopupMenu;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import android.widget.*;
import com.google.firebase.firestore.*;
import java.util.*;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.util.CellRangeAddress;


import android.util.Log;


public class AnnualReport extends AppCompatActivity {

    private static final int REQ_WRITE = 99;

    private ImageView ivCalendar, ivDownload, iconArrow;
    private EditText etSearch;
    private TextView tvDateRange;
    private TableLayout tableReport;

    private Calendar startDateCal, endDateCal;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private String userId, userDept, userSection, userRole;
    private boolean userLoaded = false;

    private final List<Map<String, Object>> reportData = new ArrayList<>();
    private final List<Map<String, Object>> viewData = new ArrayList<>();

    // private final SimpleDateFormat displayDate = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
    private final SimpleDateFormat displayDate = new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault());

    // private final SimpleDateFormat displayDate = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());


    private String reportType; // field available to whole Activity

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //EdgeToEdge.enable(this);
        setContentView(R.layout.activity_report);

        ivCalendar = findViewById(R.id.ivCalendar);
        ivDownload = findViewById(R.id.ivDownload);
        etSearch = findViewById(R.id.etSearch);
        tableReport = findViewById(R.id.tableReport);
        tvDateRange = findViewById(R.id.tvDateRange);
        iconArrow = findViewById(R.id.iconArrow);

        // Optional: Get which report was clicked (like Q1, Q2, etc.)
        reportType = getIntent().getStringExtra("reportType");
        if (reportType != null) {
            Toast.makeText(this, "Selected: " + reportType, Toast.LENGTH_SHORT).show();
        }



        // for dropdown of type filter
        iconArrow.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(this, v);

            // Load array from resources
            String[] types = getResources().getStringArray(R.array.type);

            for (String type : types) {
                popup.getMenu().add(type);
            }

            popup.setOnMenuItemClickListener(item -> {
                String selectedType = item.getTitle().toString();
                filterReportByType(selectedType); // <-- you define this method below
                return true;
            });

            popup.show();
        });

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Not signed in!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        userId = mAuth.getCurrentUser().getUid();


        // Load current user profile
        db.collection("users").document(userId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        userRole = safeString(doc.get("role"));
                        userDept = safeString(doc.get("dept"));
                        userSection = safeString(doc.get("section"));
                        userLoaded = true;
                        fetchReportData(null, null);
                    } else {
                        Toast.makeText(this, "User profile not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load user profile", Toast.LENGTH_SHORT).show()
                );


        // Search filter
        etSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                applySearchAndRender();
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });


        // Download menu
        ivDownload.setOnClickListener(v -> {
            PopupMenu menu = new PopupMenu(this, ivDownload);
            menu.getMenu().add(0, 1, 0, "Export to Excel (.xlsx)");
            menu.getMenu().add(0, 2, 1, "Export to PDF (.pdf)");
            menu.setOnMenuItemClickListener(this::onDownloadMenuClick);
            menu.show();
        });

        // Date range picker
        View dateBox = findViewById(R.id.DateBox);
        dateBox.setOnClickListener(v -> openDateRangePicker());
        dateBox.setOnLongClickListener(v -> {
            startDateCal = endDateCal = null;
            tvDateRange.setText("All dates");
            fetchReportData(null, null);
            return true;
        });

    }

    private List<Integer> getAllowedMonths(String reportType) {
        List<Integer> months = new ArrayList<>();

        switch (reportType) {
            case "Q1": // July - September
                months.add(Calendar.JULY);
                months.add(Calendar.AUGUST);
                months.add(Calendar.SEPTEMBER);
                break;

            case "Q2": // October - December
                months.add(Calendar.OCTOBER);
                months.add(Calendar.NOVEMBER);
                months.add(Calendar.DECEMBER);
                break;

            case "Q3": // January - March
                months.add(Calendar.JANUARY);
                months.add(Calendar.FEBRUARY);
                months.add(Calendar.MARCH);
                break;

            case "Q4": // April - June
                months.add(Calendar.APRIL);
                months.add(Calendar.MAY);
                months.add(Calendar.JUNE);
                break;

            case "The First 6 Months": // July - December
//                months.addAll(Arrays.asList(Calendar.JULY, Calendar.AUGUST, Calendar.SEPTEMBER,
//                        Calendar.OCTOBER, Calendar.NOVEMBER, Calendar.DECEMBER));
                months.add(Calendar.JULY);
                months.add(Calendar.AUGUST);
                months.add(Calendar.SEPTEMBER);
                months.add(Calendar.OCTOBER);
                months.add(Calendar.NOVEMBER);
                months.add(Calendar.DECEMBER);
                break;

            case "The Second 6 Months": // January - June
                months.addAll(Arrays.asList(Calendar.JANUARY, Calendar.FEBRUARY, Calendar.MARCH,
                        Calendar.APRIL, Calendar.MAY, Calendar.JUNE));
                break;

            case "Annual": // July - June
                months.addAll(Arrays.asList(Calendar.JULY, Calendar.AUGUST, Calendar.SEPTEMBER,
                        Calendar.OCTOBER, Calendar.NOVEMBER, Calendar.DECEMBER,
                        Calendar.JANUARY, Calendar.FEBRUARY, Calendar.MARCH,
                        Calendar.APRIL, Calendar.MAY, Calendar.JUNE));
                break;
        }
        return months;
    }




    private void filterByQuarterAndRender(List<Map<String, Object>> reportData, String reportType) {
        viewData.clear();


        List<Integer> allowedMonths = getAllowedMonths(reportType);
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

        //i added this new part
        SimpleDateFormat sdfTimestamp = new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault());
        // Firestore timestamp format (adjust if different)

        List<Map<String, Object>> filteredByQuarter = new ArrayList<>();

        // Filter by quarter based on registrationDate
        for (Map<String, Object> row : reportData) {
            Object regDateObj = row.get("registrationDate");
            if (!(regDateObj instanceof String)) continue;

            try {
                Date regDate = sdf.parse((String) regDateObj);
                Calendar cal = Calendar.getInstance();
                cal.setTime(regDate);
                int month = cal.get(Calendar.MONTH); // 0-based (Jan=0, Sep=8, etc.)

                if (allowedMonths.contains(month)) {
                    //viewData.add(row);
                    filteredByQuarter.add(row);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        Log.d("DEBUG", "Filtered by quarter: " + filteredByQuarter.size());

// ✅ Step 2: Group by (userId + issueNumber) — use fullName only if userId not available
        Map<String, List<Map<String, Object>>> grouped = new HashMap<>();

        for (Map<String, Object> row : filteredByQuarter) {
            // Prefer stable identifier if available
            String userId = String.valueOf(row.getOrDefault("user_id", row.getOrDefault("fullName", "-")));
            String issueId = String.valueOf(row.getOrDefault("Number", "-"));
            String key = userId.trim().toLowerCase() + "_" + issueId.trim();

            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(row);
        }

// ✅ Step 3: For each (user, issue) group, keep only the latest timestamp
        List<Map<String, Object>> latestRows = new ArrayList<>();

        for (Map.Entry<String, List<Map<String, Object>>> entry : grouped.entrySet()) {
            List<Map<String, Object>> groupRows = entry.getValue();
            Map<String, Object> latestRow = null;
            Date latestTimestamp = null;

            for (Map<String, Object> row : groupRows) {
                Object tsObj = row.get("timestamp");
                if (!(tsObj instanceof String)) continue;

                try {
                    Date tsDate = sdfTimestamp.parse((String) tsObj);
                    Log.d("DEBUG", "Parsed timestamp successfully: " + tsObj);

                    if (latestTimestamp == null || tsDate.after(latestTimestamp)) {
                        latestTimestamp = tsDate;
                        latestRow = row;
                    }
                } catch (Exception e) {
                    Log.e("DEBUG", "Failed to parse timestamp: " + tsObj, e);
                }
            }

            if (latestRow != null) {
                latestRows.add(latestRow);
            }
        }

// ✅ Step 4: Update viewData and render
        viewData.clear();
        viewData.addAll(latestRows);

        Log.d("DEBUG", "Rendering table, viewData size = " + viewData.size());
        renderTable();

    }



    /** Fetch Firestore issues based on role/section/dept logic */
    private void fetchReportData(Date start, Date end) {
        if (!userLoaded) return;

        reportData.clear();
        viewData.clear();
        tableReport.removeAllViews();

        Query q = db.collection("issues");

        switch (userRole) {
            case "Software Engineer":
                q = q.whereEqualTo("createdBy", userId);
                break;

            case "Section Manager":
                q = db.collection("issues");
                break;

            case "Division Manager":
                q = db.collection("issues");
                break;


            case "Department Manager":
                q = db.collection("issues"); // get all, filter later
                break;

            case "Admin":
                q = db.collection("issues");
                break;
        }




        // Fetch data
        q.get().addOnSuccessListener(snapshot -> {
            reportData.clear();

            for (DocumentSnapshot d : snapshot.getDocuments()) {
                String dept = safeString(d.get("dept"));
                String section = safeString(d.get("section"));
                String role = safeString(d.get("role"));
                String createdById = safeString(d.get("createdBy"));




                // Parse Firestore data (your logic stays the same)
                String regDateStr = safeString(d.get("registrationDate"));
                Date regDate = null;
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                    regDate = sdf.parse(regDateStr);
                } catch (Exception e) {
                    continue;
                }

                // Skip if outside selected date range
                if (start != null && end != null && regDate != null) {
                    if (regDate.before(start) || regDate.after(end)) continue;
                }


                // Section Manager: keep own issues or Employee issues in same dept & section
                if (userRole.equals("Section Manager")) {
                    boolean isOwnIssue = createdById.equals(userId);
                    boolean isEmployeeInSection = role.equals("Software Engineer") &&
                            (dept.equals(userDept) || dept.equals("-") || dept.isEmpty()) &&
                            (section.equals(userSection) || section.equals("-") || section.isEmpty());
                    if (!isOwnIssue && !isEmployeeInSection) continue;
                }


                // Map division section to allowed subsections
                Map<String, List<String>> allowedSections = new HashMap<>();
                allowedSections.put("Digital Banking Solution Division", Arrays.asList("POS/TOMS", "Axis Camera/Vision"));
                allowedSections.put("Self Service Solution Division", Arrays.asList("ATM/ITM/Bulk....", "Voice Guidance/CVM"));

                if (userRole.equals("Division Manager")) {
                    boolean isOwnIssue = createdById.equals(userId);

                    // Get sections allowed for this division manager
                    List<String> managerAllowedSections = allowedSections.getOrDefault(userSection, new ArrayList<>());

                    // Check if issue belongs to allowed section + role
                    boolean isEmployeeInAllowedSection = role.equals("Software Engineer") &&
                            managerAllowedSections.contains(section);

                    boolean isManagerInAllowedSection = role.equals("Section Manager") &&
                            managerAllowedSections.contains(section);

                    // Skip if it does not match any condition
                    if (!isOwnIssue && !isEmployeeInAllowedSection && !isManagerInAllowedSection) {
                        continue;
                    }
                }

                if (userRole.equals("Department Manager")) {
                    boolean isOwnIssue = createdById.equals(userId);
                    boolean isEmployeeOrSectionManager = role.equals("Software Engineer") || role.equals("Section Manager") || role.equals("Division Manager");
                    boolean sameDept = dept.equals(userDept) || dept.equals("-") || dept.isEmpty();

                    if (!isOwnIssue && !(isEmployeeOrSectionManager && sameDept)) continue;
                }

                Map<String,Object> row = new HashMap<>();
                row.put("issueNumber", safeString(d.get("Number")));
                row.put("title", safeString(d.get("title")));
                row.put("description", safeString(d.get("description")));
                row.put("bankName", safeString(d.get("bankName")));
                row.put("bankLocation", safeString(d.get("bankLocation")));
                row.put("priority", safeString(d.get("priority")));
                row.put("status", safeString(d.get("status")));
                row.put("machineType", safeString(d.get("machineType")));
                row.put("SupportEngineer", safeString(d.get("SupportEngineer")));
                row.put("registrationDate", regDateStr);
                row.put("timestamp", formatDateObject(d.get("timestamp"))); // ✅ only timestamp
                row.put("resolvedOrInProgressDate", formatDateObject(d.get("resolvedOrInProgressDate")));
                row.put("Result", safeString(d.get("Result")));
                row.put("type", safeString(d.get("type")));
                row.put("userName", safeString(d.get("fullName")));
                row.put("section", safeString(d.get("section")));
                row.put("role", safeString(d.get("role")));

                //row.put("timestampObj", d.get("timestamp")); // keep original object
                //row.put("timestamp", formatDateObject(d.get("timestamp"))); // formatted string for display

                reportData.add(row);
            }

            // Sort by issue number
            reportData.sort(Comparator.comparing(a -> String.valueOf(a.get("issueNumber"))));


            Log.d("DEBUG", "reportType = " + reportType + ", total records fetched: " + reportData.size());

            // ✅ Apply filter immediately based on received intent
            if (reportType != null && !reportType.isEmpty()) {
                filterByQuarterAndRender(reportData, reportType);
            } else {
                viewData.clear();
                viewData.addAll(reportData);
                renderTable();
            }


        }).addOnFailureListener(e ->
                Toast.makeText(this, "Failed to fetch issues: " + e.getMessage(), Toast.LENGTH_SHORT).show()
        );
    }

    private String safeString(Object o) {
        return o == null ? "-" : String.valueOf(o);
    }



    private void openDateRangePicker() {
        final Calendar now = Calendar.getInstance();
        int y = now.get(Calendar.YEAR);
        int m = now.get(Calendar.MONTH);
        int d = now.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog startPicker = new DatePickerDialog(this, (view, yy, mm, dd) -> {
            startDateCal = Calendar.getInstance();
            startDateCal.set(yy, mm, dd, 0, 0, 0);

            DatePickerDialog endPicker = new DatePickerDialog(this, (view2, yy2, mm2, dd2) -> {
                endDateCal = Calendar.getInstance();
                endDateCal.set(yy2, mm2, dd2, 23, 59, 59);

                tvDateRange.setText(displayDate.format(startDateCal.getTime()) + " - " +
                        displayDate.format(endDateCal.getTime()));

                fetchReportData(startDateCal.getTime(), endDateCal.getTime());
            }, y, m, d);
            endPicker.show();

        }, y, m, d);
        startPicker.show();
    }

    private boolean onDownloadMenuClick(MenuItem item) {
        if (viewData.isEmpty()) {
            Toast.makeText(this, "Nothing to export", Toast.LENGTH_SHORT).show();
            return true;
        }
        if (!ensureWritePermission()) return true;

        switch (item.getItemId()) {
            case 1:
                exportToExcel(viewData);
                return true;
            case 2:
                exportToPdf(viewData);
                return true;
        }
        return false;
    }


    private boolean ensureWritePermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQ_WRITE);
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] perms, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, perms, grantResults);
        if (requestCode == REQ_WRITE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission granted. Tap download again.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Storage permission is required to save files.", Toast.LENGTH_SHORT).show();
            }
        }
    }

//    private void filterReportByType(String selectedType) {
//        TableLayout table = findViewById(R.id.tableReport);
//
//
//        for (int i = 0; i < table.getChildCount(); i++) {
//            View rowView = table.getChildAt(i);
//
//            if (!(rowView instanceof TableRow)) continue;
//
//            TableRow row = (TableRow) rowView;
//
//            // Skip header rows if needed, or treat all rows the same
//            TextView typeCell = (TextView) row.getChildAt(7); // change 2 to your Type column index
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

    private void filterReportByType(String selectedType) {
        viewData.clear();

        for (Map<String, Object> r : reportData) {
            String type = String.valueOf(r.getOrDefault("type", "-")).trim();

            if (selectedType.equals("Select Type") || type.equalsIgnoreCase(selectedType)) {
                viewData.add(r); // only add matching rows
            }
        }

        renderTable(); // rebuild table with filtered rows
    }



    private String formatDateObject(Object obj) {
        if (obj == null) return "-";

        try {
            if (obj instanceof com.google.firebase.Timestamp) {
                // Firestore timestamp
                Date date = ((com.google.firebase.Timestamp) obj).toDate();
                return displayDate.format(date);
            } else if (obj instanceof Date) {
                // Plain Java date
                return displayDate.format((Date) obj);
            } else if (obj instanceof String) {
                // Parse string like "11/9/2025"
                try {
                    SimpleDateFormat inputFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                    Date parsedDate = inputFormat.parse((String) obj);
                    return displayDate.format(parsedDate);
                } catch (Exception e) {
                    // If parsing fails, just return the raw string
                    return obj.toString();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "-";
    }



    private void applySearchAndRender() {
        viewData.clear();
        String q = etSearch.getText().toString().trim().toLowerCase(Locale.ROOT);
        for (Map<String, Object> r : reportData) {
            if (TextUtils.isEmpty(q)) {
                viewData.add(r);
                continue;
            }
            String bank = String.valueOf(r.getOrDefault("bankName", "-")).toLowerCase(Locale.ROOT);
            String machine = String.valueOf(r.getOrDefault("machineType", "-")).toLowerCase(Locale.ROOT);
            String user = String.valueOf(r.getOrDefault("userName", "-")).toLowerCase(Locale.ROOT);
            String type = String.valueOf(r.getOrDefault("type", "-")).toLowerCase(Locale.ROOT);
            String status = String.valueOf(r.getOrDefault("status", "-")).toLowerCase(Locale.ROOT);

            if (bank.contains(q) || machine.contains(q) || user.contains(q) || type.contains(q) || status.contains(q)) {
                viewData.add(r);
            }
        }
        renderTable();
    }


    private int getStatusColor(String status) {
        if (status == null) return Color.BLACK;
        switch (status.toLowerCase()) {
            case "in progress":
                return Color.parseColor("#FFA500"); // Orange
            case "on hold":
                return Color.parseColor("#0000FF"); // Blue
            case "waiting for confirmation":
                return Color.parseColor("#800080"); // Purple
            case "pending":
                return Color.parseColor("#FF0000"); // Red
            case "completed":
                return Color.parseColor("#008000"); // Green
            case "open":
                return Color.parseColor("#A52A2A"); // brown
            case "open ticket":
                return Color.parseColor("#A52A2A"); // brown
            default:
                return Color.BLACK; // Default
        }
    }




    //render table, export excel, export pdf
    /** Render viewData into TableLayout */
    private void renderTable() {
        tableReport.removeAllViews();

        if (viewData == null || viewData.isEmpty()) {
            // ✅ Show message instead of blank screen
            TextView noDataMsg = new TextView(this);
            String msg = "No data available";
            if (reportType != null && !reportType.isEmpty()) {
                msg = "No data found for " + reportType;
            }
            noDataMsg.setText(msg);
            noDataMsg.setTextSize(18);
            noDataMsg.setPadding(32, 64, 32, 32);
            noDataMsg.setTypeface(noDataMsg.getTypeface(), Typeface.BOLD);
            noDataMsg.setTextColor(Color.RED);
            noDataMsg.setGravity(Gravity.CENTER);

            tableReport.addView(noDataMsg);
            ivDownload.setVisibility(View.GONE);
            return;
        }

        Log.d("DEBUG", "Rendering table, viewData size = " + viewData.size());

        // 1️⃣ Group by status instead of date
        Map<String, List<Map<String, Object>>> grouped = new TreeMap<>(); // TreeMap keeps sorted order
        for (Map<String, Object> row : viewData) {
            String status = String.valueOf(row.getOrDefault("status", "-"));
            grouped.computeIfAbsent(status, k -> new ArrayList<>()).add(row);
        }

        boolean hasData = false;

        for (String status : grouped.keySet()) {
            // Status heading
            TextView statusHeader = new TextView(this);
            statusHeader.setText("📌 Status: " + status);
            statusHeader.setPadding(16, 32, 16, 16);
            statusHeader.setTextSize(18);
            statusHeader.setTypeface(statusHeader.getTypeface(), Typeface.BOLD);
            statusHeader.setTextColor(Color.BLACK);
            tableReport.addView(statusHeader);


            TableRow header = new TableRow(this);
            String[] headers = {"Number", "User", "Section", "Title", "Description", "Bank Name", "Branch", "Type",
                    "Support Engineer", "Reported Date", "Priority", "Machine Type", "Status", "Resolved Date/In Progress Date", "Result"}; // Timestamp
            for (String h : headers) {
                TextView tv = new TextView(this);
                tv.setText(h);
                tv.setPadding(16, 16, 16, 16);
                tv.setTextSize(14);
                tv.setTypeface(tv.getTypeface(), android.graphics.Typeface.BOLD);
                tv.setGravity(Gravity.CENTER);
                tv.setTextColor(Color.parseColor("#FFFFFF")); // black color
                // Apply border
                tv.setBackgroundResource(R.drawable.header_cell_border);
                header.addView(tv);
            }
            tableReport.addView(header);

            // Rows for this date
            for (Map<String, Object> row : grouped.get(status)) {
                hasData = true;
                TableRow tr = new TableRow(this);
                tr.addView(createCell(String.valueOf(row.getOrDefault("issueNumber", "-")), false));
                tr.addView(createCell(String.valueOf(row.getOrDefault("userName", "-")), false));
                tr.addView(createCell(String.valueOf(row.getOrDefault("section", "-")), false));
                tr.addView(createCell(String.valueOf(row.getOrDefault("title", "-")), false));
                tr.addView(createCell(String.valueOf(row.getOrDefault("description", "-")), false));
                tr.addView(createCell(String.valueOf(row.getOrDefault("bankName", "-")), false));
                tr.addView(createCell(String.valueOf(row.getOrDefault("bankLocation", "-")), false));
                tr.addView(createCell(String.valueOf(row.getOrDefault("type", "-")), false));
                tr.addView(createCell(String.valueOf(row.getOrDefault("SupportEngineer", "-")), false));
                tr.addView(createCell(String.valueOf(row.getOrDefault("registrationDate", "-")), false));
//                tr.addView(createCell(String.valueOf(row.getOrDefault("timestamp", "-"))));
                tr.addView(createCell(String.valueOf(row.getOrDefault("priority", "-")), false));
                tr.addView(createCell(String.valueOf(row.getOrDefault("machineType", "-")), false));
                tr.addView(createCell(String.valueOf(row.getOrDefault("status", "-")), true));
                tr.addView(createCell(String.valueOf(row.getOrDefault("resolvedOrInProgressDate", "-")), false));
                tr.addView(createCell(String.valueOf(row.getOrDefault("Result", "-")), false));
                tableReport.addView(tr);
            }
        }

        ivDownload.setVisibility(hasData ? View.VISIBLE : View.GONE);
    }

    private TextView createCell(String text, boolean isStatus) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setPadding(16,16,16,16);
        tv.setTextSize(13);
        //tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setMaxLines(4);
        //tv.setTextColor(Color.BLACK);
        if (isStatus) {
            tv.setTextColor(getStatusColor(text)); // status-specific color
            tv.setTypeface(Typeface.DEFAULT_BOLD);
        } else {
            tv.setTextColor(Color.BLACK);
        }
        // Apply border
        tv.setBackgroundResource(R.drawable.cell_border);
        tv.setEllipsize(TextUtils.TruncateAt.END);
        return tv;
    }



    // Export to Excel and PDF methods remain the same...
    // Utils: safeString and formatDateObject remain the same
    /* ---------- EXPORT: EXCEL ---------- */
    private void exportToExcel(List<Map<String, Object>> data) {

        // Group data by status
        Map<String, List<Map<String, Object>>> grouped = new TreeMap<>();
        for (Map<String, Object> row : data) {
            String status = String.valueOf(row.getOrDefault("status", "-"));
            grouped.computeIfAbsent(status, k -> new ArrayList<>()).add(row);
        }


        try {
            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("Report");
            String[] headers = {"Number", "User", "Section", "Title", "Description", "Bank Name", "Branch", "Type",
                    "Support Engineer", "Reported Date", "Priority", "Machine Type", "Status", "Resolved Date/In Progress Date", "Result"};

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++)
                headerRow.createCell(i).setCellValue(headers[i]);


            int rowIdx = 1;

            for (String status : grouped.keySet()) {
                // Add a status header row
                Row statusRow = sheet.createRow(rowIdx++);
                Cell statusCell = statusRow.createCell(0);
                statusCell.setCellValue("📌 Status: " + status);

                // Style it bold
                CellStyle headerStyle = workbook.createCellStyle();
                Font headerFont = workbook.createFont();
                headerFont.setBold(true);
                headerStyle.setFont(headerFont);
                statusCell.setCellStyle(headerStyle);

                // Merge across all columns for better look
                sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(
                        rowIdx - 1, rowIdx - 1, 0, headers.length - 1
                ));

                // Now add rows for this status
                for (Map<String, Object> row : grouped.get(status)) {
                    Row xl = sheet.createRow(rowIdx++);
                    xl.createCell(0).setCellValue(String.valueOf(row.getOrDefault("issueNumber", "-")));
                    xl.createCell(1).setCellValue(String.valueOf(row.getOrDefault("userName", "-")));
                    xl.createCell(2).setCellValue(String.valueOf(row.getOrDefault("section", "-")));
                    xl.createCell(3).setCellValue(String.valueOf(row.getOrDefault("title", "-")));
                    xl.createCell(4).setCellValue(String.valueOf(row.getOrDefault("description", "-")));
                    xl.createCell(5).setCellValue(String.valueOf(row.getOrDefault("bankName", "-")));
                    xl.createCell(6).setCellValue(String.valueOf(row.getOrDefault("bankLocation", "-")));
                    xl.createCell(7).setCellValue(String.valueOf(row.getOrDefault("type", "-")));
                    xl.createCell(8).setCellValue(String.valueOf(row.getOrDefault("SupportEngineer", "-")));
                    xl.createCell(9).setCellValue(String.valueOf(row.getOrDefault("registrationDate", "-")));
//                xl.createCell(7).setCellValue(String.valueOf(row.getOrDefault("timestamp", "-")));
                    xl.createCell(10).setCellValue(String.valueOf(row.getOrDefault("priority", "-")));
                    xl.createCell(11).setCellValue(String.valueOf(row.getOrDefault("machineType", "-")));
                    xl.createCell(12).setCellValue(String.valueOf(row.getOrDefault("status", "-")));
                    xl.createCell(13).setCellValue(String.valueOf(row.getOrDefault("resolvedOrInProgressDate", "-")));
                    xl.createCell(14).setCellValue(String.valueOf(row.getOrDefault("Result", "-")));


                    // ===== Add color styling for status =====
                    int statusColIndex = 12; // Status column
                    //org.apache.poi.ss.usermodel.Cell statusCell = xl.getCell(statusColIndex);
                    org.apache.poi.ss.usermodel.CellStyle style = workbook.createCellStyle();
                    org.apache.poi.ss.usermodel.Font font = workbook.createFont();


                    switch (String.valueOf(row.getOrDefault("status", "-")).toLowerCase()) {
                        case "in progress":
                            font.setColor(IndexedColors.ORANGE.getIndex());
                            font.setBold(true);
                            break;
                        case "on hold":
                            font.setColor(IndexedColors.BLUE.getIndex());
                            font.setBold(true);
                            break;
                        case "waiting for confirmation":
                            font.setColor(IndexedColors.VIOLET.getIndex());
                            font.setBold(true);
                            break;
                        case "pending":
                            font.setColor(IndexedColors.RED.getIndex());
                            font.setBold(true);
                            break;
                        case "completed":
                            font.setColor(IndexedColors.GREEN.getIndex());
                            font.setBold(true);
                            break;
                        case "open":
                            font.setColor(IndexedColors.DARK_RED.getIndex());
                            font.setBold(true);
                            break;
                        case "open ticket":
                            font.setColor(IndexedColors.DARK_RED.getIndex());
                            font.setBold(true);
                            break;
                        default:
                            font.setColor(IndexedColors.BLACK.getIndex());
                            font.setBold(true);
                            break;
                    }


                    font.setBold(true);
                    style.setFont(font);
                    statusCell.setCellStyle(style);
                }
            }

            for (int i = 0; i < headers.length; i++) sheet.setColumnWidth(i, 22 * 256);
            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (!downloadsDir.exists()) downloadsDir.mkdirs();

            String filename = "IssueReport_" + System.currentTimeMillis() + ".xlsx";
            File file = new File(downloadsDir, filename);

            FileOutputStream fos = new FileOutputStream(file);
            workbook.write(fos);
            fos.close();
            workbook.close();

            Toast.makeText(this, "Excel saved: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Export error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    /* ---------- EXPORT: PDF ---------- */
    private void exportToPdf(List<Map<String, Object>> data) {
        try {
            String[] headers = {"Number", "User", "Section", "Title", "Description", "Bank Name", "Branch", "Type",
                    "Support Engineer", "Reported Date", "Priority", "Machine Type", "Status", "Resolved Date/In Progress Date", "Result"};
//            int pageWidth = 595;   // A4: 595x842 (points)
//            int pageHeight = 842;
            int pageWidth = 842;   // Landscape width
            int pageHeight = 595;  // Landscape height


            PdfDocument pdf = new PdfDocument();
            Paint paint = new Paint();
            paint.setTextSize(10f);

            int xStart = 20;
            int y = 40;
            int lineHeight = 18;

            int rowsPerPage = 40; // approx
            int rowCount = 0;

            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();
            PdfDocument.Page page = pdf.startPage(pageInfo);
            Canvas canvas = page.getCanvas();

            // Title
            paint.setTextSize(14f);
            canvas.drawText("Issue Report", xStart, y, paint);
            paint.setTextSize(10f);
            y += lineHeight;

            // Date range display
            canvas.drawText("Range: " + tvDateRange.getText().toString(), xStart, y, paint);
            y += (lineHeight + 6);

            // Header
            // drawRow(canvas, paint, xStart, y, headers);
            drawRow(canvas, paint, y, headers, pageWidth);

            y += lineHeight;
            //rowCount++;

            // Rows
            // int index = 1;

            // Group data by status (like Excel)
            Map<String, List<Map<String, Object>>> grouped = new TreeMap<>();
            for (Map<String, Object> row : data) {
                String status = String.valueOf(row.getOrDefault("status", "-"));
                grouped.computeIfAbsent(status, k -> new ArrayList<>()).add(row);
            }



            for (String status : grouped.keySet()) {
                // Draw status header
                paint.setColor(Color.BLACK);
                paint.setFakeBoldText(true);
                canvas.drawText("📌 Status: " + status, xStart, y, paint);
                y += lineHeight;

                // Reset to normal
                paint.setFakeBoldText(false);


                for (Map<String, Object> row : grouped.get(status)) {
                    String[] cols = new String[]{
                            String.valueOf(row.getOrDefault("issueNumber", "-")),
                            cut(String.valueOf(row.getOrDefault("userName", "-")), 16),
                            cut(String.valueOf(row.getOrDefault("section", "-")), 20),
                            cut(String.valueOf(row.getOrDefault("title", "-")), 30),
                            cut(String.valueOf(row.getOrDefault("description", "-")), 40),
                            cut(String.valueOf(row.getOrDefault("bankName", "-")), 16),
                            cut(String.valueOf(row.getOrDefault("bankLocation", "-")), 16),
                            cut(String.valueOf(row.getOrDefault("type", "-")), 16),
                            cut(String.valueOf(row.getOrDefault("SupportEngineer", "-")), 16),
                            String.valueOf(row.getOrDefault("registrationDate", "-")),
//                        String.valueOf(row.getOrDefault("timestamp", "-")),  // ✅ timestamp
                            String.valueOf(row.getOrDefault("priority", "-")),
                            cut(String.valueOf(row.getOrDefault("machineType", "-")), 20),
                            String.valueOf(row.getOrDefault("status", "-")),
                            String.valueOf(row.getOrDefault("resolvedOrInProgressDate", "-")),
                            String.valueOf(row.getOrDefault("Result", "-"))
                    };

                    //drawRow(canvas, paint, xStart, y, cols);
                    drawRow(canvas, paint, y, cols, pageWidth);

                    y += lineHeight;


                    if (y > pageHeight - 40) {
                        pdf.finishPage(page);
                        pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, (pdf.getPages().size() + 1)).create();
                        page = pdf.startPage(pageInfo);
                        canvas = page.getCanvas();
                        y = 40;
                        // redraw header
                        drawRow(canvas, paint, y, headers, pageWidth);
                        y += lineHeight;

                    }


                }
            }

            pdf.finishPage(page);

            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (!downloadsDir.exists()) downloadsDir.mkdirs();

            String filename = "IssueReport_" + System.currentTimeMillis() + ".pdf";
            File file = new File(downloadsDir, filename);

            FileOutputStream fos = new FileOutputStream(file);
            //pdf.finishPage(page);
            pdf.writeTo(fos);
            pdf.close();
//            fos.close();

            Toast.makeText(this, "PDF saved: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "PDF error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }



    //draw for the pdf
    private void drawRow(Canvas c, Paint p, int y, String[] cols, int pageWidth) {
        int margin = 20;

        // Relative weights for columns (must add up to 100 or so)
        float[] weights = new float[]{
                6,   // Number
                8,   // User
                10,  // Section
                12,  // Title
                18,  // Description
                10,  // Bank Name
                8,   // Branch
                8,  //type
                16,  //Support Engineer
                16,  // Reported Date
                6,   // Priority
                10,  // Machine Type
                10,  // Status
                15,   // Resolved/In Progress Date
                25   //result
        };



        // total available width
        int availableWidth = pageWidth - 2 * margin;

        // calculate actual column widths
        int[] colWidths = new int[weights.length];
        float totalWeight = 0;
        for (float w : weights) totalWeight += w;

        for (int i = 0; i < weights.length; i++) {
            colWidths[i] = (int) (availableWidth * (weights[i] / totalWeight));
        }

        // draw row text
        int x = margin;
        int statusCol = 12;
        for (int i = 0; i < cols.length && i < colWidths.length; i++) {
            String text = cols[i];

            if (i == statusCol) {
                p.setColor(getStatusColorPdf(text)); // helper method for PDF
                p.setFakeBoldText(true);
            } else {
                p.setColor(Color.BLACK);
                p.setFakeBoldText(false);
            }

            // If text too long, cut or ellipsize
            if (p.measureText(text) > colWidths[i]) {
                while (p.measureText(text + "...") > colWidths[i] && text.length() > 0) {
                    text = text.substring(0, text.length() - 1);
                }
                text += "...";
            }

            c.drawText(text, x, y, p);
            x += colWidths[i]; // move to next column
        }
    }


    //get the color of status column
    private int getStatusColorPdf(String status) {
        if (status == null) return Color.BLACK;
        switch (status.toLowerCase()) {
            case "in progress": return Color.rgb(255, 165, 0); // orange
            case "on hold": return Color.BLUE;
            case "waiting for confirmation": return Color.MAGENTA;
            case "pending": return Color.RED;
            case "completed": return Color.GREEN;
            case "open": return Color.rgb(165, 42, 42);   // a classic brown;
            case "open ticket": return Color.rgb(165, 42, 42);   // a classic brown;
            default: return Color.BLACK;
        }
    }


    private String cut(String s, int max) {
        return s.length() > max ? s.substring(0, max - 3) + "..." : s;
    }




}