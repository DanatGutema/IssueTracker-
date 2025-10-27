package com.example.issuetracker;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.graphics.Color;
import android.util.Log;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class Charts extends AppCompatActivity {

    private BarChart barChart;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_charts);

        barChart = findViewById(R.id.barChart);
        db = FirebaseFirestore.getInstance();

        loadDataFromFirestore();
    }

    private void loadDataFromFirestore() {
        db.collection("issues").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // map<month, map<bankName, count>>
                Map<String, Map<String, Integer>> monthBankCount = new HashMap<>();

                for (QueryDocumentSnapshot doc : task.getResult()) {
                    String bankName = doc.getString("bankName");
                    Object dateObj = doc.get("registrationDate");

                    if (bankName == null || dateObj == null) continue;

                    try {
                        Calendar cal = Calendar.getInstance();

                        if (dateObj instanceof String) {
                            // If registrationDate is a string
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                            cal.setTime(sdf.parse((String) dateObj));
                        } else if (dateObj instanceof Timestamp) {
                            // If registrationDate is a Firestore timestamp
                            cal.setTime(((Timestamp) dateObj).toDate());
                        }

                        String month = new SimpleDateFormat("MMM", Locale.US).format(cal.getTime());

                        // count per month+bank
                        monthBankCount.putIfAbsent(month, new HashMap<>());
                        Map<String, Integer> bankCount = monthBankCount.get(month);
                        bankCount.put(bankName, bankCount.getOrDefault(bankName, 0) + 1);

                    } catch (Exception e) {
                        Log.e("Charts", "Date parse error: " + e.getMessage());
                    }
                }

                updateChart(monthBankCount);
            } else {
                Log.e("Charts", "Error getting documents: ", task.getException());
            }
        });
    }

    private void updateChart(Map<String, Map<String, Integer>> monthBankCount) {
        ArrayList<BarEntry> entries = new ArrayList<>();
        ArrayList<String> months = new ArrayList<>();
        ArrayList<String> bankLabels = new ArrayList<>();

        int index = 0;
        for (String month : monthBankCount.keySet()) {
            months.add(month);

            Map<String, Integer> bankCount = monthBankCount.get(month);

            // total issues in that month
            int total = 0;
            for (int count : bankCount.values()) {
                total += count;
            }

            for (String bank : bankCount.keySet()) {
                float percentage = (bankCount.get(bank) * 100f) / total;
                entries.add(new BarEntry(index, percentage));
                bankLabels.add(bank);
                index++;
            }
        }

        BarDataSet dataSet = new BarDataSet(entries, "Issues % per Bank");
        dataSet.setColor(Color.parseColor("#3F51B5")); // nice blue

        BarData barData = new BarData(dataSet);
        barData.setValueTextSize(12f);

        // show bank name + % above each bar
        barData.setValueFormatter(new ValueFormatter() {
            @Override
            public String getBarLabel(BarEntry barEntry) {
                int idx = entries.indexOf(barEntry);
                return bankLabels.get(idx) + " " + String.format(Locale.US, "%.1f%%", barEntry.getY());
            }
        });

        barChart.setData(barData);

        // months on X-axis
        XAxis xAxis = barChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(months));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);

        barChart.getDescription().setEnabled(false);
        barChart.animateY(1000);
        barChart.invalidate();
    }
}
