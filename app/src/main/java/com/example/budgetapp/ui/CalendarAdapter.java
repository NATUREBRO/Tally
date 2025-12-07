package com.example.budgetapp.ui;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.budgetapp.R;
import com.example.budgetapp.database.Transaction;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

public class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.ViewHolder> {
    private List<LocalDate> days = new ArrayList<>();
    private List<Transaction> transactions = new ArrayList<>();
    private LocalDate selectedDate;
    private final OnDateClickListener listener;

    public interface OnDateClickListener {
        void onDateClick(LocalDate date);
    }

    public CalendarAdapter(OnDateClickListener listener) {
        this.listener = listener;
    }

    public void updateData(List<LocalDate> days, List<Transaction> transactions) {
        this.days = days;
        this.transactions = transactions;
        notifyDataSetChanged();
    }

    public void setSelectedDate(LocalDate date) {
        this.selectedDate = date;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_calendar_day, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LocalDate date = days.get(position);
        if (date == null) {
            holder.tvDay.setText("");
            holder.tvNet.setText("");
            holder.itemView.setBackgroundResource(0);
            return;
        }

        holder.tvDay.setText(String.valueOf(date.getDayOfMonth()));

        // 计算净收支
        double net = 0;
        long start = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long end = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();

        for (Transaction t : transactions) {
            if (t.date >= start && t.date < end) {
                if (t.type == 1) net += t.amount; else net -= t.amount;
            }
        }

        if (Math.abs(net) > 0.01) {
            holder.tvNet.setText(String.format("%.0f", net));
            // 净收入红色，净支出绿色
            holder.tvNet.setTextColor(net > 0 ? Color.parseColor("#FF5252") : Color.parseColor("#4CAF50"));
        } else {
            holder.tvNet.setText("");
        }

        // 选中样式
        if (date.equals(selectedDate)) {
            holder.itemView.setBackgroundResource(R.drawable.bg_selected_date);
        } else {
            holder.itemView.setBackgroundResource(0);
        }

        holder.itemView.setOnClickListener(v -> listener.onDateClick(date));
    }

    @Override
    public int getItemCount() {
        return days.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDay, tvNet;
        ViewHolder(View v) {
            super(v);
            tvDay = v.findViewById(R.id.tv_day);
            tvNet = v.findViewById(R.id.tv_net_amount);
        }
    }
}