package com.example.myapplication.data;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;
import kotlinx.coroutines.flow.Flow;

@Dao
public interface FinanceDao {
    @Query("SELECT * FROM expenses ORDER BY date DESC")
    Flow<List<ExpenseEntity>> observeExpenses();

    @Query("SELECT * FROM income ORDER BY date DESC")
    Flow<List<IncomeEntity>> observeIncome();

    @Query("SELECT * FROM budgets ORDER BY id DESC")
    Flow<List<BudgetEntity>> observeBudgets();

    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    Flow<UserProfileEntity> observeUserProfile();

    @Query("SELECT * FROM members ORDER BY name ASC")
    Flow<List<MemberEntity>> observeMembers();

    @Query("SELECT * FROM loans ORDER BY dueDate ASC")
    Flow<List<LoanEntity>> observeLoans();

    @Query("SELECT * FROM savings ORDER BY date DESC")
    Flow<List<SavingsEntity>> observeSavings();

    @Query("SELECT * FROM user_accounts ORDER BY createdAt DESC")
    Flow<List<UserAccountEntity>> observeUserAccounts();

    @Query("SELECT * FROM user_accounts WHERE email = :email LIMIT 1")
    UserAccountEntity findUserByEmailBlocking(String email);

    @Query("SELECT * FROM user_accounts WHERE email = :email AND password = :password LIMIT 1")
    UserAccountEntity loginBlocking(String email, String password);

    @Query("UPDATE user_accounts SET password = :newPassword WHERE email = :email")
    int resetPasswordBlocking(String email, String newPassword);

    @Query("UPDATE user_accounts SET approvalStatus = :status WHERE id = :id")
    void updateUserApprovalBlocking(long id, String status);

    @Query("UPDATE members SET approvalStatus = :status WHERE id = :id")
    void updateMemberApprovalBlocking(long id, String status);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertExpenseBlocking(ExpenseEntity expense);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertIncomeBlocking(IncomeEntity income);

    @Update
    void updateExpenseBlocking(ExpenseEntity expense);

    @Update
    void updateIncomeBlocking(IncomeEntity income);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void saveBudgetBlocking(BudgetEntity budget);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void saveUserProfileBlocking(UserProfileEntity profile);

    @Insert(onConflict = OnConflictStrategy.ABORT)
    long insertUserBlocking(UserAccountEntity user);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertMemberBlocking(MemberEntity member);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertLoanBlocking(LoanEntity loan);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertSavingBlocking(SavingsEntity saving);

    @Update
    void updateLoanBlocking(LoanEntity loan);

    @Delete
    void deleteExpenseBlocking(ExpenseEntity expense);

    @Delete
    void deleteIncomeBlocking(IncomeEntity income);

    @Delete
    void deleteMemberBlocking(MemberEntity member);

    @Delete
    void deleteLoanBlocking(LoanEntity loan);

    @Delete
    void deleteSavingBlocking(SavingsEntity saving);
}
