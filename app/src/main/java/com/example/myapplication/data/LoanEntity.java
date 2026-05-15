package com.example.myapplication.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "loans")
public class LoanEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public String borrowerName;
    public double principalAmount;
    public double paidAmount;
    public double interestRate;
    public long issueDate;
    public long dueDate;
    public String purpose;
    public String status;
    public String groupName;

    public LoanEntity(long id, String borrowerName, double principalAmount, double paidAmount, double interestRate, long issueDate, long dueDate, String purpose, String status, String groupName) {
        this.id = id;
        this.borrowerName = borrowerName;
        this.principalAmount = principalAmount;
        this.paidAmount = paidAmount;
        this.interestRate = interestRate;
        this.issueDate = issueDate;
        this.dueDate = dueDate;
        this.purpose = purpose;
        this.status = status;
        this.groupName = groupName;
    }
}
