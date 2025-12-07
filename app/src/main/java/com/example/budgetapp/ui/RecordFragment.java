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
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.budgetapp.R;
import com.example.budgetapp.database.Transaction;
import com.example.budgetapp.viewmodel.FinanceViewModel;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class RecordFragment extends Fragment {
    private FinanceViewModel viewModel;
    private CalendarAdapter adapter;
    private YearMonth currentMonth;
    private LocalDate selectedDate;
    private TextView tvMonthTitle;

    private TextView tvIncome, tvExpense, tvBalance;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_record, container, false);
        viewModel = new ViewModelProvider(requireActivity()).get(FinanceViewModel.class);
        currentMonth = YearMonth.now();

        tvMonthTitle = view.findViewById(R.id.tv_month_title);
        tvIncome = view.findViewById(R.id.tv_month_income);
        tvExpense = view.findViewById(R.id.tv_month_expense);
        tvBalance = view.findViewById(R.id.tv_month_balance);
        RecyclerView recyclerView = view.findViewById(R.id.calendar_recycler);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 7));

        adapter = new CalendarAdapter(date -> {
            if (date.equals(selectedDate)) {
                // 第二次点击：显示该日期的详细列表灯箱
                showDateDetailDialog(date);
            } else {
                selectedDate = date;
                adapter.setSelectedDate(date);
            }
        });
        recyclerView.setAdapter(adapter);

        // --- 新增代码开始：给标题添加点击事件，弹出日期选择器 ---
        tvMonthTitle.setOnClickListener(v -> {
            // 1. 获取当前显示的年份和月份 (DatePickerDialog 需要 0-11 的月份)
            int year = currentMonth.getYear();
            int month = currentMonth.getMonthValue() - 1;
            int day = 1; // 默认日，不重要

            // 2. 创建并显示日期选择器
            DatePickerDialog datePicker = new DatePickerDialog(requireContext(), (picker, selectYear, selectMonth, selectDay) -> {
                // 3. 用户点击确定后，更新 currentMonth
                // selectMonth 是 0-11，YearMonth 需要 1-12，所以 +1
                currentMonth = YearMonth.of(selectYear, selectMonth + 1);

                // 4. 刷新日历视图
                updateCalendar();

            }, year, month, day);

            // 可选：设置只显示标题为"选择月份" (有些手机系统会自动处理)
            datePicker.setTitle("选择月份");
            datePicker.show();
        });
        // --- 新增代码结束 ---

        view.findViewById(R.id.btn_prev_month).setOnClickListener(v -> {
            currentMonth = currentMonth.minusMonths(1);
            updateCalendar();
        });
        view.findViewById(R.id.btn_next_month).setOnClickListener(v -> {
            currentMonth = currentMonth.plusMonths(1);
            updateCalendar();
        });

        viewModel.getAllTransactions().observe(getViewLifecycleOwner(), list -> updateCalendar());
        updateCalendar();
        return view;
    }

    // 修改 updateCalendar 方法，增加计算逻辑
    private void updateCalendar() {
        tvMonthTitle.setText(currentMonth.format(DateTimeFormatter.ofPattern("yyyy年MM月")));

        // 1. 生成日历格子数据 (保持不变)
        List<LocalDate> days = new ArrayList<>();
        int length = currentMonth.lengthOfMonth();
        for (int i = 1; i <= length; i++) {
            days.add(currentMonth.atDay(i));
        }

        List<Transaction> allList = viewModel.getAllTransactions().getValue();
        List<Transaction> currentList = allList != null ? allList : new ArrayList<>();

        // 2. 更新 Adapter (保持不变)
        adapter.updateData(days, currentList);

        // --- 新增：计算本月总计 ---
        calculateMonthTotals(currentList);
    }

    // 新增：计算逻辑方法
    private void calculateMonthTotals(List<Transaction> transactions) {
        double totalIncome = 0;
        double totalExpense = 0;

        int year = currentMonth.getYear();
        int month = currentMonth.getMonthValue();

        for (Transaction t : transactions) {
            // 将时间戳转为 LocalDate
            LocalDate date = java.time.Instant.ofEpochMilli(t.date)
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDate();

            // 只统计当前展示月份的数据
            if (date.getYear() == year && date.getMonthValue() == month) {
                if (t.type == 1) {
                    totalIncome += t.amount;
                } else {
                    totalExpense += t.amount;
                }
            }
        }

        double balance = totalIncome - totalExpense;

        // 更新 UI
        tvIncome.setText(String.format("+%.0f", totalIncome));
        tvExpense.setText(String.format("-%.0f", totalExpense));

        String sign = balance >= 0 ? "+" : "";
        tvBalance.setText(String.format("%s%.0f", sign, balance));
    }


    // --- 新增：日详情弹窗 (灯箱) ---
    private void showDateDetailDialog(LocalDate date) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        // 我们动态创建一个 View 作为 Dialog 内容，包含一个列表和一个添加按钮
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_date_detail_list, null); // 需新建布局
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        TextView tvTitle = dialogView.findViewById(R.id.tv_detail_date);
        tvTitle.setText(date.toString());

        // 设置列表
        RecyclerView rvList = dialogView.findViewById(R.id.rv_detail_list);
        rvList.setLayoutManager(new LinearLayoutManager(getContext()));
        TransactionListAdapter listAdapter = new TransactionListAdapter(transaction -> {
            // 点击条目 -> 修改/删除
            showEditDialog(transaction, dialog);
        });
        rvList.setAdapter(listAdapter);

        // 筛选当天数据
        List<Transaction> all = viewModel.getAllTransactions().getValue();
        if (all != null) {
            long start = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
            long end = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
            List<Transaction> dayList = all.stream()
                    .filter(t -> t.date >= start && t.date < end)
                    .collect(Collectors.toList());
            listAdapter.setTransactions(dayList);
        }

        // 添加按钮
        dialogView.findViewById(R.id.btn_add_new).setOnClickListener(v -> {
            showAddOrEditDialog(null, date); // null 表示新增
            dialog.dismiss();
        });

        dialog.show();
    }

    // --- 新增：编辑/删除 选择弹窗 ---
    private void showEditDialog(Transaction t, AlertDialog parentDialog) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("操作选择")
                .setItems(new String[]{"修改", "删除"}, (dialog, which) -> {
                    if (which == 0) {
                        // 修改
                        parentDialog.dismiss();
                        LocalDate date = Instant.ofEpochMilli(t.date).atZone(ZoneId.systemDefault()).toLocalDate();
                        showAddOrEditDialog(t, date);
                    } else {
                        // 删除
                        viewModel.deleteTransaction(t);
                        parentDialog.dismiss();
                        // 稍微延迟一下重新打开日历详情有点麻烦，简单做法是直接关掉，用户体验也可接受
                        Toast.makeText(getContext(), "已删除", Toast.LENGTH_SHORT).show();
                    }
                });
        builder.show();
    }

    // --- 统一的 添加/修改 弹窗 ---
    private void showAddOrEditDialog(Transaction existingTransaction, LocalDate date) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_transaction, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        TextView tvDate = dialogView.findViewById(R.id.tv_dialog_date);
        tvDate.setText(date.toString());
        
        RadioGroup rgType = dialogView.findViewById(R.id.rg_type);
        RadioGroup rgCategory = dialogView.findViewById(R.id.rg_category);
        EditText etAmount = dialogView.findViewById(R.id.et_amount);
        Button btnSave = dialogView.findViewById(R.id.btn_save);

        rgType.setOnCheckedChangeListener((g, id) -> rgCategory.setVisibility(id == R.id.rb_expense ? View.VISIBLE : View.GONE));

        // 如果是编辑模式，回填数据
        if (existingTransaction != null) {
            btnSave.setText("保存修改");
            etAmount.setText(String.valueOf(existingTransaction.amount));
            if (existingTransaction.type == 1) {
                rgType.check(R.id.rb_income);
            } else {
                rgType.check(R.id.rb_expense);
                // 简单回填分类 (实际项目可能需要遍历 RadioButton 比较 text)
                switch (existingTransaction.category) {
                    case "饮食": rgCategory.check(R.id.rb_food); break;
                    case "娱乐": rgCategory.check(R.id.rb_fun); break;
                    case "教育": rgCategory.check(R.id.rb_edu); break;
                    case "网购": rgCategory.check(R.id.rb_shop); break;
                }
            }
        }

        btnSave.setOnClickListener(v -> {
            String amountStr = etAmount.getText().toString();
            if (!amountStr.isEmpty()) {
                double amount = Double.parseDouble(amountStr);
                int type = rgType.getCheckedRadioButtonId() == R.id.rb_income ? 1 : 0;
                String category = "收入";
                if (type == 0) {
                    int catId = rgCategory.getCheckedRadioButtonId();
                    RadioButton rb = dialogView.findViewById(catId);
                    if(rb != null) category = rb.getText().toString();
                }

                long ts = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
                
                if (existingTransaction == null) {
                    // 新增
                    viewModel.addTransaction(new Transaction(ts, type, category, amount));
                } else {
                    // 修改：保持 ID 不变，更新内容
                    Transaction updateT = new Transaction(ts, type, category, amount);
                    updateT.id = existingTransaction.id; 
                    viewModel.updateTransaction(updateT);
                }
                dialog.dismiss();
            }
        });
        dialog.show();
    }
}