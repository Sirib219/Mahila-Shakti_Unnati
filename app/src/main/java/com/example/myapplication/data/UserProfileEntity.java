package com.example.myapplication.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "user_profile")
public class UserProfileEntity {
    @PrimaryKey
    public long id;
    public String name;
    public String incomeRange;
    public String currency;

    public UserProfileEntity(long id, String name, String incomeRange, String currency) {
        this.id = id;
        this.name = name;
        this.incomeRange = incomeRange;
        this.currency = currency;
    }
}