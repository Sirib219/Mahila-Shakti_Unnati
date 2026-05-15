package com.example.myapplication.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "income")
public class IncomeEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public double amount;
    public long date;
    public String source;
    public String note;
    public String ownerEmail;

    public IncomeEntity(long id, double amount, long date, String source, String note, String ownerEmail) {
        this.id = id;
        this.amount = amount;
        this.date = date;
        this.source = source;
        this.note = note;
        this.ownerEmail = ownerEmail;
    }
}
