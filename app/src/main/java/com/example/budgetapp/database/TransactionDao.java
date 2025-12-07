package com.example.budgetapp.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete; // 必须导入
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update; // 必须导入
import java.util.List;

@Dao
public interface TransactionDao {
    @Insert
    void insert(Transaction transaction);

    @Delete  // 新增：允许删除
    void delete(Transaction transaction);

    @Update  // 新增：允许修改
    void update(Transaction transaction);

    @Query("SELECT * FROM transactions ORDER BY date DESC")
    LiveData<List<Transaction>> getAllTransactions();
}