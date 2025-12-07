package com.example.budgetapp.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.budgetapp.R;
import com.example.budgetapp.database.Transaction;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class TransactionListAdapter extends RecyclerView.Adapter<TransactionListAdapter.ViewHolder> {
    
    // 1. 定义数据源，初始化为空防止空指针
    private List<Transaction> list = new ArrayList<>();
    
    // 2. 定义点击监听器接口
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Transaction transaction);
    }

    // --- 构造函数 1：给“统计”模块使用 (只传数据，不可点击) ---
    public TransactionListAdapter(List<Transaction> list) {
        this.list = list;
    }

    // --- 构造函数 2：给“记账”模块使用 (传监听器，数据后续通过 setTransactions 传入) ---
    public TransactionListAdapter(OnItemClickListener listener) {
        this.listener = listener;
    }

    // --- 新增方法：动态更新数据 (解决 setTransactions 报错) ---
    public void setTransactions(List<Transaction> list) {
        this.list = list;
        notifyDataSetChanged(); // 刷新列表显示
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // 复用之前的 item_transaction_detail 布局
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_transaction_detail, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Transaction t = list.get(position);
        
        // 设置金额
        holder.tvAmount.setText(String.format("%.0f", t.amount));
        
        // 设置日期 (格式：MM-dd)
        String dateStr = Instant.ofEpochMilli(t.date)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
                .format(DateTimeFormatter.ofPattern("MM-dd"));
        
        // 如果有备注或分类，也可以在这里显示，目前显示日期
        holder.tvDate.setText(dateStr + " " + t.category);

        // 设置颜色：收入红，支出绿
        if (t.type == 1) {
            holder.tvAmount.setTextColor(holder.itemView.getContext().getColor(R.color.income_red));
            holder.tvAmount.setText("+" + String.format("%.0f", t.amount));
        } else {
            holder.tvAmount.setTextColor(holder.itemView.getContext().getColor(R.color.expense_green));
            holder.tvAmount.setText("-" + String.format("%.0f", t.amount));
        }

        // --- 核心：绑定点击事件 ---
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(t);
            }
        });
    }

    @Override
    public int getItemCount() {
        return list == null ? 0 : list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate, tvAmount;
        ViewHolder(View v) {
            super(v);
            tvDate = v.findViewById(R.id.tv_detail_date);
            tvAmount = v.findViewById(R.id.tv_detail_amount);
        }
    }
}