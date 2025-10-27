package com.example.issuetracker;

import android.graphics.Color;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import android.util.Log;


public class POSVisualize extends AppCompatActivity {

    private Map<String, String[]> sectionMachineTypeMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_posvisualize);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // initialize mapping section -> machine types from strings.xml
        initSectionMachineTypeMap();

        // Example: visualize Sept 2025 for POS/TOMs section
        showMonthlyChart("POS/TOMs", 2025, 9);
    }

    private void initSectionMachineTypeMap() {
        sectionMachineTypeMap = new HashMap<>();
        sectionMachineTypeMap.put("POS/TOMs", getResources().getStringArray(R.array.PosToms));
        sectionMachineTypeMap.put("ATM/ITM/Bulk....", getResources().getStringArray(R.array.ATM));
        sectionMachineTypeMap.put("Voice Guidance/CVM", getResources().getStringArray(R.array.VoiceGuidance));
        sectionMachineTypeMap.put("Axis Camera/Vision", getResources().getStringArray(R.array.AxisCamera));
        sectionMachineTypeMap.put("Digital Banking Solution Division", getResources().getStringArray(R.array.MachineTypeDigitalBanking));
        sectionMachineTypeMap.put("Self Service Solution Division", getResources().getStringArray(R.array.MachineTypeSelfService));
    }

    private void showMonthlyChart(String section, int selectedYear, int selectedMonth) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // pick machine types for the section
        String[] machineTypes = sectionMachineTypeMap.get(section);
        if (machineTypes == null || machineTypes.length == 0) {
            Log.d("POSVisualize", "No machine types found for section: " + section);
            return;
        }

        db.collection("issues")
                .whereEqualTo("section", section)
                .get()
                .addOnSuccessListener(query -> {
                    Log.d("POSVisualize", "Documents fetched: " + query.size());

                    Map<String, Map<String, Integer>> bankMachineCount = new HashMap<>();

                    for (DocumentSnapshot doc : query.getDocuments()) {
                        String bank = doc.getString("bankName");
                        String machine = doc.getString("machineType");
                        Timestamp ts = doc.getTimestamp("timestamp");

                        if (bank == null)
                            Log.d("POSVisualize", "Missing bankName in doc: " + doc.getId());
                        if (machine == null)
                            Log.d("POSVisualize", "Missing machineType in doc: " + doc.getId());
                        if (ts == null)
                            Log.d("POSVisualize", "Missing timestamp in doc: " + doc.getId());

                        if (bank == null || machine == null || ts == null) continue;

                        // filter by year-month
                        Calendar cal = Calendar.getInstance();
                        cal.setTime(ts.toDate());
                        int docYear = cal.get(Calendar.YEAR);
                        int docMonth = cal.get(Calendar.MONTH) + 1;

                        if (docYear != selectedYear || docMonth != selectedMonth) {
                            Log.d("POSVisualize", "Skipped doc by date: " + doc.getId() + " | " + ts.toDate());
                            continue;
                        }

                        // only count machine types valid for this section
                        if (!Arrays.asList(machineTypes).contains(machine)) {
                            Log.d("POSVisualize", "Skipped machine: " + machine + " in doc: " + doc.getId());
                            continue;
                        }

                        bankMachineCount.putIfAbsent(bank, new HashMap<>());
                        Map<String, Integer> machineCount = bankMachineCount.get(bank);

                        machineCount.put(machine, machineCount.getOrDefault(machine, 0) + 1);
                    }


                    Log.d("POSVisualize", "Bank counts map: " + bankMachineCount.toString());

                    if (bankMachineCount.isEmpty()) {
                        Log.d("POSVisualize", "No valid data to display for this month/section.");
                        return;
                    }

                    // build chart entries
                    List<BarEntry> entries = new ArrayList<>();
                    List<String> labels = new ArrayList<>();
                    int index = 0;

                    for (Map.Entry<String, Map<String, Integer>> entry : bankMachineCount.entrySet()) {
                        String bank = entry.getKey();
                        Map<String, Integer> machineMap = entry.getValue();

                        float[] values = new float[machineTypes.length];
                        for (int i = 0; i < machineTypes.length; i++) {
                            values[i] = machineMap.getOrDefault(machineTypes[i], 0);
                        }

                        entries.add(new BarEntry(index, values));
                        labels.add(bank);
                        index++;
                    }

                    // dataset
                    BarDataSet dataSet = new BarDataSet(entries, "Issues per Machine Type");
                    dataSet.setStackLabels(machineTypes);

                    int[] colors = new int[machineTypes.length];
                    for (int i = 0; i < machineTypes.length; i++) {
                        colors[i] = Color.HSVToColor(new float[]{(i * 60) % 360, 0.8f, 0.9f});
                    }
                    dataSet.setColors(colors);
                    dataSet.setValueTextSize(12f);

                    dataSet.setValueFormatter(new ValueFormatter() {
                        @Override
                        public String getBarStackedLabel(float value, BarEntry entry) {
                            if (value == 0) return "";
                            float[] values = entry.getYVals();
                            for (int i = 0; i < values.length; i++) {
                                if (values[i] == value) {
                                    return machineTypes[i] + "(" + (int) value + ")";
                                }
                            }
                            return String.valueOf((int) value);
                        }
                    });

                    BarData data = new BarData(dataSet);
                    data.setBarWidth(0.5f);

                    BarChart chart = findViewById(R.id.barChart);
                    chart.setData(data);
                    chart.setFitBars(true);
                    chart.getDescription().setEnabled(false);

                    // X Axis
                    XAxis xAxis = chart.getXAxis();
                    xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
                    xAxis.setGranularity(1f);
                    xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
                    xAxis.setDrawGridLines(false);

                    // Y Axis
                    chart.getAxisLeft().setAxisMinimum(0f);
                    chart.getAxisRight().setEnabled(false);

                    // legend
                    Legend legend = chart.getLegend();
                    legend.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
                    legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);
                    legend.setOrientation(Legend.LegendOrientation.VERTICAL);
                    legend.setDrawInside(false);

                    chart.animateY(1000);
                    chart.invalidate();
                })
                .addOnFailureListener(e -> Log.e("POSVisualize", "Error fetching documents", e));
    }
}