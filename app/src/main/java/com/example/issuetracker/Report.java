package com.example.issuetracker;

import android.app.DatePickerDialog;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.graphics.Color;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

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

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFColor;


public class Report extends AppCompatActivity {

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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);

        ivCalendar = findViewById(R.id.ivCalendar);
        ivDownload = findViewById(R.id.ivDownload);
        etSearch = findViewById(R.id.etSearch);
        tableReport = findViewById(R.id.tableReport);
        tvDateRange = findViewById(R.id.tvDateRange);
        iconArrow = findViewById(R.id.iconArrow);


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

//        // for dropdown of status filter
//        iconArrow.setOnClickListener(v -> {
//            PopupMenu popup = new PopupMenu(this, v);
//
//            // Load array from resources
//            String[] statuses = getResources().getStringArray(R.array.StatusUpdateForReport);
//
//            for (String status : statuses) {
//                popup.getMenu().add(status);
//            }
//
//            popup.setOnMenuItemClickListener(item -> {
//                String selectedStatus = item.getTitle().toString();
//                filterReportByStatus(selectedStatus); // <-- you define this method below
//                return true;
//            });
//
//            popup.show();
//        });

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

        // Date range picker
        View dateBox = findViewById(R.id.DateBox);
        dateBox.setOnClickListener(v -> openDateRangePicker());
        dateBox.setOnLongClickListener(v -> {
            startDateCal = endDateCal = null;
            tvDateRange.setText("All dates");
            fetchReportData(null, null);
            return true;
        });

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

//    private void filterReportByType(String selectedType) {
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

    /** Fetch Firestore issues based on role/section/dept logic */
    /** Fetch Firestore issues based on role/section/dept logic */
    private void fetchReportData(Date start, Date end) {
        if (!userLoaded) return;

        reportData.clear();
        viewData.clear();
        tableReport.removeAllViews();

        Query q = db.collection("issues");

        switch (userRole) {
            case "Software Engineer":
                // Employee: own issues only
                q = q.whereEqualTo("createdBy", userId);
                break;

            case "Section Manager":
                // Section Manager: issues in same dept+section created by Employees
                //q = q.whereEqualTo("department", userDept)
                  //      .whereEqualTo("section", userSection)
                    //    .whereEqualTo("role", "Employee");
                //break;
                q = db.collection("issues");
                break;

            case "Division Manager":
                // Section Manager: issues in same dept+section created by Employees
                //q = q.whereEqualTo("department", userDept)
                //      .whereEqualTo("section", userSection)
                //    .whereEqualTo("role", "Employee");
                //break;
                q = db.collection("issues");
                break;

                //q = q.whereEqualTo("role", "Employee"); // fetch all Employee issue
            case "Department Manager":
                // Manager: all issues in same dept
                //q = q.whereEqualTo("department", userDept);
               // q = q; // fetch all issues, rules will filter later
                q = db.collection("issues"); // get all, filter later
                break;

            case "Admin":
                // Admin: everything
                q = db.collection("issues");
                break;
        }

        // Apply date filtering if selected
//        if (start != null && end != null) {
//            q = q.whereGreaterThanOrEqualTo("timestamp", start)
//                    .whereLessThanOrEqualTo("timestamp", end);
//        }



        // Fetch data
        q.get().addOnSuccessListener(snapshot -> {
            reportData.clear();

            for (DocumentSnapshot d : snapshot.getDocuments()) {
                String dept = safeString(d.get("dept"));
                String section = safeString(d.get("section"));
                String role = safeString(d.get("role"));
                String createdById = safeString(d.get("createdBy"));


                //filter date based on registration page
                String regDateStr = safeString(d.get("registrationDate"));
                Date regDate = null;

                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/M/yyyy HH:mm", Locale.getDefault()); // match your string format
                    regDate = sdf.parse(regDateStr);
                } catch (Exception e) {
                    // invalid date format, skip filtering
                    continue;
                }

// Skip if outside selected date range
                if (start != null && end != null && regDate != null) {
                    if (regDate.before(start) || regDate.after(end)) {
                        continue; // skip this issue
                    }
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

                // Division Manager: keep own issues, selected section managers issues or Employee issues of selected sections in same dept
               /*
                if (userRole.equals("Division Manager")) {
                    boolean isOwnIssue = createdById.equals(userId);
                    boolean isEmployeeInSection = role.equals("Software Engineer") &&
                            (dept.equals(userDept) || dept.equals("-") || dept.isEmpty()) &&
                            (section.equals(userSection) || section.equals("-") || section.isEmpty());
                    boolean isManagerInSection = role.equals("Section Manager") &&
                            (dept.equals(userDept) || dept.equals("-") || dept.isEmpty()) &&
                            (section.equals(userSection) || section.equals("-") || section.isEmpty());
                    if (!isOwnIssue && !isEmployeeInSection && !isManagerInSection) continue;
                }
                */

                // Manager: keep only same dept or "-" / missing
               // if (userRole.equals("Manager")) {
                 //   if (!dept.equals(userDept) && !dept.equals("-") && !dept.isEmpty()) continue;
                //}

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
                row.put("registrationDate", safeString(d.get("registrationDate")));
                row.put("timestamp", formatDateObject(d.get("timestamp"))); // ✅ only timestamp
                row.put("resolvedOrInProgressDate", formatDateObject(d.get("resolvedOrInProgressDate")));
                row.put("Result", safeString(d.get("Result")));
                row.put("type", safeString(d.get("type")));
                row.put("userName", safeString(d.get("fullName")));
                row.put("section", safeString(d.get("section")));
                row.put("role", safeString(d.get("role")));
                reportData.add(row);
            }

            // Sort by issue number
            reportData.sort(Comparator.comparing(a -> String.valueOf(a.get("issueNumber"))));

            // Apply search filter and render table
            applySearchAndRender();


        }).addOnFailureListener(e ->
                Toast.makeText(this, "Failed to fetch issues: " + e.getMessage(), Toast.LENGTH_SHORT).show()
        );
    }

    /** Apply client-side search on bankName OR machineType, then render */
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


    //color change
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




    /** Render viewData into TableLayout */
    private void renderTable() {
        tableReport.removeAllViews();

        // Date formatter to group by day only
        //private final SimpleDateFormat groupDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat groupDate = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault());


        // Group by timestamp
        Map<String, List<Map<String,Object>>> grouped = new TreeMap<>((a,b) -> b.compareTo(a));
        for (Map<String,Object> row : viewData) {
            Object tsObj = row.get("timestamp");
            String dateKey = "-";
            if (tsObj instanceof String) {
                try {
                    Date tsDate = displayDate.parse((String) tsObj);  // parse full timestamp
                    dateKey = groupDate.format(tsDate);              // get yyyy-MM-dd for grouping
                } catch (Exception e) { dateKey = "-"; }
            }
            grouped.computeIfAbsent(dateKey, k -> new ArrayList<>()).add(row);
        }

        boolean hasData = false;

        // 2. Render each group
        for (String date : grouped.keySet()) {
            // Heading for the date
            TextView dateHeader = new TextView(this);
            dateHeader.setText("📅 " + date);
            dateHeader.setPadding(16, 32, 16, 16);
            dateHeader.setTextSize(18);
            dateHeader.setTypeface(dateHeader.getTypeface(), android.graphics.Typeface.BOLD);
            dateHeader.setTextColor(Color.parseColor("#000000")); // blue heading
            tableReport.addView(dateHeader);

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
            for (Map<String, Object> row : grouped.get(date)) {
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
        try {
            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("Report");
            String[] headers = {"Number", "User", "Section", "Title", "Description", "Bank Name", "Branch", "Type",
                    "Support Engineer", "Reported Date", "Priority", "Machine Type", "Status", "Resolved Date/In Progress Date", "Result"};

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) headerRow.createCell(i).setCellValue(headers[i]);


            int rowIdx = 1;
            for (Map<String, Object> row : data) {
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
                org.apache.poi.ss.usermodel.Cell statusCell = xl.getCell(statusColIndex);
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



                style.setFont(font);
                statusCell.setCellStyle(style);
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
            for (Map<String, Object> row : data) {
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
                    // redraw header
                    //paint.setTextSize(14f);
                    //canvas.drawText("Issue Report (cont.)", xStart, y, paint);
                    //paint.setTextSize(10f);
                    //y += (lineHeight + 6);
                    //drawRow(canvas, paint, xStart, y, headers);
                    //y += lineHeight;
                }

               // drawRow(canvas, paint, xStart, y, cols);
               // y += lineHeight;

                //index++;
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

    /** Simple row drawer (columns laid out left-to-right with fixed stops) */
    //private void drawRow(Canvas c, Paint p, int x, int y, String[] cols) {
        // column x-positions tuned for A4 width
      //  int[] stops = new int[]{x, x + 50, x + 210, x + 330, x + 420, x + 480, x + 530, x + 570};
        //for (int i = 0; i < cols.length && i < stops.length; i++) {
          //  c.drawText(cols[i], stops[i], y, p);
        //}
    //}
    //private void drawRow(Canvas c, Paint p, int x, int y, String[] cols) {
      //  int pageWidth = 595; // A4
        //int colCount = cols.length;
        //int colWidth = (pageWidth - 40) / colCount; // 40 = margin

        //for (int i = 0; i < cols.length; i++) {
          //  int xPos = x + (i * colWidth);
            //c.drawText(cols[i], xPos, y, p);
        //}
    //}
//    private void drawRow(Canvas c, Paint p, int x, int y, String[] cols) {
//        int colWidth = 90;
//        for (int i = 0; i < cols.length; i++) {
//            c.drawText(cols[i], x + i * colWidth, y, p);
//        }
//    }

//    private void drawRow(Canvas c, Paint p, int x, int y, String[] cols) {
//        int margin = 20;
//        int pageWidth = 595;
//        int colCount = cols.length;
//        int colWidth = (pageWidth - 2 * margin) / colCount;
//
//        for (int i = 0; i < cols.length; i++) {
//            int xPos = margin + (i * colWidth);
//            c.drawText(cols[i], xPos, y, p);
//        }
//    }
//    private void drawRow(Canvas c, Paint p, int y, String[] cols, int pageWidth) {
//        int margin = 20;
//        int colCount = cols.length;
//        int colWidth = (pageWidth - 2 * margin) / colCount;
//
//        for (int i = 0; i < cols.length; i++) {
//            int xPos = margin + (i * colWidth);
//            c.drawText(cols[i], xPos, y, p);
//        }
//    }

//    private void drawRow(Canvas c, Paint p, int y, String[] cols, int pageWidth) {
//        int margin = 20;
//
//        // custom column widths (must add up to ~pageWidth - 2*margin)
//        int[] colWidths = new int[]{
//                40,   // Number
//                80,   // User
//                80,   // Section
//                120,  // Title
//                160,  // Description
//                100,  // Bank Name
//                80,   // Branch
//                100,  // Timestamp
//                60,   // Priority
//                70,   // Status
//                100,  // Machine Type
//                120   // Resolved/In Progress Date
//        };
//
//        int x = margin;
//        for (int i = 0; i < cols.length && i < colWidths.length; i++) {
//            c.drawText(cols[i], x, y, p);
//            x += colWidths[i]; // move to next column
//        }
//    }

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

    private String safeString(Object o) {
        return o == null ? "-" : String.valueOf(o);
    }

//    private String formatDateObject(Object o) {
//        if (o instanceof Timestamp) {
//            return displayDate.format(((Timestamp) o).toDate());
//        } else if (o instanceof Date) {
//            return displayDate.format((Date) o);
//        }
//        return "-";
//    }
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
                SimpleDateFormat inputFormat = new SimpleDateFormat("dd/M/yyyy HH:mm", Locale.getDefault());
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
}
