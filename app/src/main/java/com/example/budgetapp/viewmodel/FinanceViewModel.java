package com.example.budgetapp.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import com.example.budgetapp.database.AppDatabase;
import com.example.budgetapp.database.Transaction;
import com.example.budgetapp.database.TransactionDao;
import java.util.List;

public class FinanceViewModel extends AndroidViewModel {
    private final TransactionDao dao;
    private final LiveData<List<Transaction>> allTransactions;

    public FinanceViewModel(@NonNull Application application) {
        super(application);
        AppDatabase db = AppDatabase.getDatabase(application);
        dao = db.transactionDao();
        allTransactions = dao.getAllTransactions();
    }

    public LiveData<List<Transaction>> getAllTransactions() {
        return allTransactions;
    }

    // 原有的添加方法
    public void addTransaction(Transaction transaction) {
        AppDatabase.databaseWriteExecutor.execute(() -> dao.insert(transaction));
    }

    // --- 下面是你缺少的两个方法 ---

    // 新增：删除方法
    public void deleteTransaction(Transaction transaction) {
        AppDatabase.databaseWriteExecutor.execute(() -> dao.delete(transaction));
    }

    // 新增：更新方法
    public void updateTransaction(Transaction transaction) {
        AppDatabase.databaseWriteExecutor.execute(() -> dao.update(transaction));
    }
}