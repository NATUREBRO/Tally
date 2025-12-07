package com.example.budgetapp.ui;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat; // 引入 ContextCompat
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.budgetapp.R;
import com.example.budgetapp.database.Transaction;
import com.example.budgetapp.viewmodel.FinanceViewModel;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StatsFragment extends Fragment {

    private FinanceViewModel viewModel;
    private LineChart lineChart;
    private PieChart pieChart;
    private RadioGroup rgTimeScope;
    private TextView tvDateRange;

    // 模式：0=Year, 1=Month, 2=Week
    private int currentMode = 2;
    private LocalDate selectedDate = LocalDate.now();
    private List<Transaction> allTransactions = new ArrayList<>();
    private CustomMarkerView markerView;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_stats, container, false);

        initViews(view);
        setupLineChart();
        setupPieChart();

        viewModel = new ViewModelProvider(requireActivity()).get(FinanceViewModel.class);
        viewModel.getAllTransactions().observe(getViewLifecycleOwner(), list -> {
            this.allTransactions = list;
            refreshData();
        });

        setupListeners(view);
        updateDateRangeDisplay();

        return view;
    }

    private void initViews(View view) {
        lineChart = view.findViewById(R.id.chart_line);
        pieChart = view.findViewById(R.id.chart_pie);
        rgTimeScope = view.findViewById(R.id.rg_time_scope);
        tvDateRange = view.findViewById(R.id.tv_current_date_range);
    }

    private void setupListeners(View view) {
        ImageButton btnPrev = view.findViewById(R.id.btn_prev);
        ImageButton btnNext = view.findViewById(R.id.btn_next);

        rgTimeScope.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb_year) currentMode = 0;
            else if (checkedId == R.id.rb_month) currentMode = 1;
            else if (checkedId == R.id.rb_week) currentMode = 2;

            updateDateRangeDisplay();
            refreshData();
        });

        btnPrev.setOnClickListener(v -> changeDate(-1));
        btnNext.setOnClickListener(v -> changeDate(1));
        tvDateRange.setOnClickListener(v -> showDatePicker());
    }

    // --- 图表初始化 ---

    private void setupLineChart() {
        // [修复] 使用 ContextCompat 获取颜色，防止部分机型崩溃
        int textColor = ContextCompat.getColor(requireContext(), R.color.text_primary);

        lineChart.getDescription().setEnabled(false);
        lineChart.getAxisRight().setEnabled(false);
        lineChart.getAxisLeft().setTextColor(textColor);

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setTextColor(textColor);

        lineChart.getLegend().setTextColor(textColor);
        lineChart.setExtraBottomOffset(10f);
        lineChart.setDragEnabled(true);
        lineChart.setScaleEnabled(false);

        // 注意：请确保你的项目中存在 view_chart_marker 布局文件
        if (getContext() != null) {
            markerView = new CustomMarkerView(getContext(), R.layout.view_chart_marker);
            markerView.setChartView(lineChart);
            lineChart.setMarker(markerView);
        }
    }

    private void setupPieChart() {
        // [修复] 使用 ContextCompat 获取颜色
        int textColor = ContextCompat.getColor(requireContext(), R.color.text_primary);
        int holeColor = ContextCompat.getColor(requireContext(), R.color.white);

        pieChart.getDescription().setEnabled(false);
        pieChart.setHoleRadius(40f);
        pieChart.setTransparentCircleRadius(0);
        pieChart.setHoleColor(holeColor);

        pieChart.setEntryLabelColor(Color.BLACK);
        pieChart.setEntryLabelTextSize(10f);

        pieChart.getLegend().setTextColor(textColor);
        pieChart.getLegend().setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        pieChart.getLegend().setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        pieChart.getLegend().setOrientation(Legend.LegendOrientation.HORIZONTAL);
        pieChart.getLegend().setDrawInside(false);

        pieChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                String category = ((PieEntry) e).getLabel();
                showCategoryDetailDialog(category);
            }

            @Override
            public void onNothingSelected() { }
        });
    }

    // --- 日期逻辑 (保持原样) ---

    private void changeDate(int offset) {
        if (currentMode == 0) selectedDate = selectedDate.plusYears(offset);
        else if (currentMode == 1) selectedDate = selectedDate.plusMonths(offset);
        else selectedDate = selectedDate.plusWeeks(offset);

        updateDateRangeDisplay();
        refreshData();
    }

    private void updateDateRangeDisplay() {
        if (currentMode == 0) {
            tvDateRange.setText(selectedDate.format(DateTimeFormatter.ofPattern("yyyy年")));
        } else if (currentMode == 1) {
            tvDateRange.setText(selectedDate.format(DateTimeFormatter.ofPattern("yyyy年MM月")));
        } else {
            WeekFields weekFields = WeekFields.of(Locale.getDefault());
            int weekOfMonth = selectedDate.get(weekFields.weekOfMonth());
            tvDateRange.setText(selectedDate.format(DateTimeFormatter.ofPattern("yyyy年M月")) + " 第" + weekOfMonth + "周");
        }
    }

    private void showDatePicker() {
        DatePickerDialog datePicker = new DatePickerDialog(requireContext(), (view, year, month, dayOfMonth) -> {
            selectedDate = LocalDate.of(year, month + 1, dayOfMonth);
            updateDateRangeDisplay();
            refreshData();
        }, selectedDate.getYear(), selectedDate.getMonthValue() - 1, selectedDate.getDayOfMonth());
        datePicker.show();
    }

    // --- 数据处理逻辑 (保持原样) ---

    private void refreshData() {
        if (allTransactions == null) return;
        if (currentMode == 0) processYearlyData();
        else if (currentMode == 1) processMonthlyData();
        else processWeeklyData();
    }

    private void processYearlyData() {
        int year = selectedDate.getYear();
        aggregateData(t -> {
            LocalDate date = Instant.ofEpochMilli(t.date).atZone(ZoneId.systemDefault()).toLocalDate();
            return date.getYear() == year ? date.getMonthValue() : -1;
        }, 12, "月", null);
    }

    private void processMonthlyData() {
        int year = selectedDate.getYear();
        int month = selectedDate.getMonthValue();
        int daysInMonth = selectedDate.lengthOfMonth();

        aggregateData(t -> {
            LocalDate date = Instant.ofEpochMilli(t.date).atZone(ZoneId.systemDefault()).toLocalDate();
            return (date.getYear() == year && date.getMonthValue() == month) ? date.getDayOfMonth() : -1;
        }, daysInMonth, "日", null);
    }

    private void processWeeklyData() {
        LocalDate startOfWeek = selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate endOfWeek = selectedDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

        aggregateData(t -> {
            LocalDate date = Instant.ofEpochMilli(t.date).atZone(ZoneId.systemDefault()).toLocalDate();
            if (!date.isBefore(startOfWeek) && !date.isAfter(endOfWeek)) {
                return date.getDayOfWeek().getValue();
            }
            return -1;
        }, 7, "", new String[]{"", "周一", "周二", "周三", "周四", "周五", "周六", "周日"});
    }

    interface IndexExtractor {
        int getIndex(Transaction t);
    }

    private void aggregateData(IndexExtractor extractor, int maxX, String suffix, String[] customLabels) {
        Map<Integer, Double> incomeMap = new HashMap<>();
        Map<Integer, Double> expenseMap = new HashMap<>();
        Map<String, Double> pieCats = new HashMap<>();

        for (Transaction t : allTransactions) {
            int index = extractor.getIndex(t);
            if (index != -1) {
                if (t.type == 1) {
                    incomeMap.put(index, incomeMap.getOrDefault(index, 0.0) + t.amount);
                } else {
                    expenseMap.put(index, expenseMap.getOrDefault(index, 0.0) + t.amount);
                    pieCats.put(t.category, pieCats.getOrDefault(t.category, 0.0) + t.amount);
                }
            }
        }
        updateCharts(incomeMap, expenseMap, pieCats, maxX, suffix, customLabels);
    }

    private void updateCharts(Map<Integer, Double> incomeMap, Map<Integer, Double> expenseMap,
                              Map<String, Double> pieMap, int maxX, String suffix, String[] customLabels) {

        List<Entry> inEntries = new ArrayList<>();
        List<Entry> outEntries = new ArrayList<>();
        List<Entry> netEntries = new ArrayList<>();

        for (int i = 1; i <= maxX; i++) {
            double in = incomeMap.getOrDefault(i, 0.0);
            double out = expenseMap.getOrDefault(i, 0.0);

            if (incomeMap.containsKey(i)) inEntries.add(new Entry(i, (float) in));
            if (expenseMap.containsKey(i)) outEntries.add(new Entry(i, (float) out));

            // 只要有收支就显示净值点，或者你可以逻辑改为 (in != 0 || out != 0)
            if (incomeMap.containsKey(i) || expenseMap.containsKey(i)) {
                netEntries.add(new Entry(i, (float) (in - out)));
            }
        }

        LineDataSet setIn = createLineDataSet(inEntries, "收入", R.color.income_red);
        LineDataSet setOut = createLineDataSet(outEntries, "支出", R.color.expense_green);
        LineDataSet setNet = createLineDataSet(netEntries, "净收支", R.color.fixed_yellow);
        setNet.enableDashedLine(10f, 5f, 0f);

        LineData lineData = new LineData(setIn, setOut, setNet);
        lineChart.setData(lineData);

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setAxisMinimum(1f);
        xAxis.setAxisMaximum((float) maxX);

        if (maxX <= 12) {
            xAxis.setLabelCount(maxX);
        } else {
            xAxis.setLabelCount(6);
        }

        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int index = (int) value;
                if (index < 1 || index > maxX) return "";
                if (customLabels != null) {
                    if (index >= 1 && index < customLabels.length) return customLabels[index];
                    return "";
                }
                return index + suffix;
            }
        });

        if (markerView != null) {
            markerView.setSourceData(incomeMap, expenseMap, customLabels != null ? "" : suffix, customLabels);
        }

        lineChart.animateX(600);
        lineChart.invalidate();

        // --- 渲染 PieChart ---
        List<PieEntry> pieEntries = new ArrayList<>();
        for (Map.Entry<String, Double> entry : pieMap.entrySet()) {
            pieEntries.add(new PieEntry(entry.getValue().floatValue(), entry.getKey()));
        }

        PieDataSet pieSet = new PieDataSet(pieEntries, "");
        // [修复] 使用 ContextCompat 获取颜色
        pieSet.setColors(
                ContextCompat.getColor(requireContext(), R.color.pie_food),
                ContextCompat.getColor(requireContext(), R.color.pie_fun),
                ContextCompat.getColor(requireContext(), R.color.pie_edu),
                ContextCompat.getColor(requireContext(), R.color.pie_shop)
        );
        pieSet.setValueTextSize(12f);
        pieSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format("%.1f%%", value);
            }
        });

        PieData pieData = new PieData(pieSet);
        pieChart.setData(pieData);
        pieChart.setUsePercentValues(true);
        pieChart.animateY(800);
        pieChart.invalidate();
    }

    private LineDataSet createLineDataSet(List<Entry> entries, String label, int colorResId) {
        LineDataSet set = new LineDataSet(entries, label);
        // [修复] 使用 ContextCompat 获取颜色
        int color = ContextCompat.getColor(requireContext(), colorResId);
        set.setColor(color);
        set.setCircleColor(color);
        set.setLineWidth(2f);
        set.setDrawValues(false);
        return set;
    }

    // --- 二级界面：灯箱逻辑 (保持原样) ---

    private void showCategoryDetailDialog(String category) {
        if (allTransactions == null) return;

        long startMillis, endMillis;
        LocalDate startFuncDate, endFuncDate;
        int targetYear = selectedDate.getYear();
        int targetMonth = selectedDate.getMonthValue();

        if (currentMode == 0) {
            startFuncDate = LocalDate.of(targetYear, 1, 1);
            endFuncDate = LocalDate.of(targetYear, 12, 31);
        } else if (currentMode == 1) {
            startFuncDate = LocalDate.of(targetYear, targetMonth, 1);
            endFuncDate = startFuncDate.plusMonths(1).minusDays(1);
        } else {
            startFuncDate = selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            endFuncDate = selectedDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        }

        startMillis = startFuncDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        endMillis = endFuncDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();

        List<Transaction> filteredList = new ArrayList<>();
        for (Transaction t : allTransactions) {
            if (t.type == 0 && t.category.equals(category)) {
                if (t.date >= startMillis && t.date < endMillis) {
                    filteredList.add(t);
                }
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        // 确保你的布局文件中有 dialog_transaction_list
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_transaction_list, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        TextView tvTitle = dialogView.findViewById(R.id.tv_dialog_title);
        tvTitle.setText(category + " - 消费清单");

        RecyclerView rv = dialogView.findViewById(R.id.rv_detail_list);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        // 确保你已经创建了 TransactionListAdapter
        rv.setAdapter(new TransactionListAdapter(filteredList));

        Button btnClose = dialogView.findViewById(R.id.btn_close_dialog);
        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }
}