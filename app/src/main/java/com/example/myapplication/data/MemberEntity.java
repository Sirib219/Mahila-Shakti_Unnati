package com.example.myapplication.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "members")
public class MemberEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public String name;
    public String phone;
    public String role;
    public String groupName;
    public double monthlySavingGoal;
    public long joinedDate;
    public String approvalStatus;
    public String requestedBy;

    public MemberEntity(long id, String name, String phone, String role, String groupName, double monthlySavingGoal, long joinedDate, String approvalStatus, String requestedBy) {
        this.id = id;
        this.name = name;
        this.phone = phone;
        this.role = role;
        this.groupName = groupName;
        this.monthlySavingGoal = monthlySavingGoal;
        this.joinedDate = joinedDate;
        this.approvalStatus = approvalStatus;
        this.requestedBy = requestedBy;
    }
}
