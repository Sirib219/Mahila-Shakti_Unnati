package com.example.myapplication.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "expenses")
public class ExpenseEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public double amount;
    public String category;
    public long date;
    public String note;
    public String paymentMethod;
    public String ownerEmail;

    public ExpenseEntity(long id, double amount, String category, long date, String note, String paymentMethod, String ownerEmail) {
        this.id = id;
        this.amount = amount;
        this.category = category;
        this.date = date;
        this.note = note;
        this.paymentMethod = paymentMethod;
        this.ownerEmail = ownerEmail;
    }
}
