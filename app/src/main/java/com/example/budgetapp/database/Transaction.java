package com.example.budgetapp.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "transactions")
public class Transaction {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public long date;       // 时间戳
    public int type;        // 1: 收入, 0: 支出
    public String category; // 饮食, 娱乐...
    public double amount;

    public Transaction(long date, int type, String category, double amount) {
        this.date = date;
        this.type = type;
        this.category = category;
        this.amount = amount;
    }
}