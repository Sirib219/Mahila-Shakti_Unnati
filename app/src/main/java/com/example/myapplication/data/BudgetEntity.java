package com.example.myapplication.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "budgets")
public class BudgetEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public String periodType;
    public String periodKey;
    public double limitAmount;
    public String ownerEmail;

    public BudgetEntity(long id, String periodType, String periodKey, double limitAmount, String ownerEmail) {
        this.id = id;
        this.periodType = periodType;
        this.periodKey = periodKey;
        this.limitAmount = limitAmount;
        this.ownerEmail = ownerEmail;
    }
}
