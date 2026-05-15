package com.example.myapplication.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "savings")
public class SavingsEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public String memberName;
    public double amount;
    public long date;
    public String note;
    public String groupName;

    public SavingsEntity(long id, String memberName, double amount, long date, String note, String groupName) {
        this.id = id;
        this.memberName = memberName;
        this.amount = amount;
        this.date = date;
        this.note = note;
        this.groupName = groupName;
    }
}
