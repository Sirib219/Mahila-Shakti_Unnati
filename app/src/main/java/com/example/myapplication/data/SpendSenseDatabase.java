package com.example.myapplication.data;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(
        entities = {
                ExpenseEntity.class,
                IncomeEntity.class,
                BudgetEntity.class,
                UserProfileEntity.class,
                UserAccountEntity.class,
                MemberEntity.class,
                LoanEntity.class,
                SavingsEntity.class
        },
        version = 8,
        exportSchema = false
)
public abstract class SpendSenseDatabase extends RoomDatabase {
    private static volatile SpendSenseDatabase instance;

    public abstract FinanceDao financeDao();

    public static SpendSenseDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (SpendSenseDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            SpendSenseDatabase.class,
                            "mahila_shakti_unnati.db"
                    ).fallbackToDestructiveMigration().build();
                }
            }
        }
        return instance;
    }
}
