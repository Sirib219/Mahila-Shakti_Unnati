package com.example.myapplication.data;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "user_accounts", indices = {@Index(value = {"email"}, unique = true)})
public class UserAccountEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public String name;
    public String email;
    public String phone;
    public String password;
    public String userType;
    public String groupName;
    public String village;
    public String dateOfBirth;
    public boolean eligibilityConfirmed;
    public String idProofReference;
    public String approvalStatus;
    public String requestedBy;
    public long createdAt;

    public UserAccountEntity(long id, String name, String email, String phone, String password, String userType, String groupName, String village, String dateOfBirth, boolean eligibilityConfirmed, String idProofReference, String approvalStatus, String requestedBy, long createdAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.password = password;
        this.userType = userType;
        this.groupName = groupName;
        this.village = village;
        this.dateOfBirth = dateOfBirth;
        this.eligibilityConfirmed = eligibilityConfirmed;
        this.idProofReference = idProofReference;
        this.approvalStatus = approvalStatus;
        this.requestedBy = requestedBy;
        this.createdAt = createdAt;
    }
}
