package com.example.myapplication.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FinanceRepository(private val dao: FinanceDao) {
    val expenses = dao.observeExpenses()
    val income = dao.observeIncome()
    val budgets = dao.observeBudgets()
    val profile = dao.observeUserProfile()
    val members = dao.observeMembers()
    val loans = dao.observeLoans()
    val savings = dao.observeSavings()
    val users = dao.observeUserAccounts()

    suspend fun registerUser(name: String, email: String, phone: String, password: String, userType: String, groupName: String, village: String, dateOfBirth: String, eligibilityConfirmed: Boolean, idProofReference: String): Result<UserAccountEntity> = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim().lowercase(Locale.getDefault())
        val cleanPhone = phone.trim()
        if (dao.findUserByEmailBlocking(cleanEmail) != null) {
            return@withContext Result.failure(IllegalArgumentException("Already registered. Please login with this email."))
        }
        val user = UserAccountEntity(0, name.trim(), cleanEmail, cleanPhone, password, userType, groupName.trim(), village.trim(), dateOfBirth.trim(), eligibilityConfirmed, idProofReference.trim(), "Pending", "Self", System.currentTimeMillis())
        val id = dao.insertUserBlocking(user)
        Result.success(UserAccountEntity(id, user.name, user.email, user.phone, user.password, user.userType, user.groupName, user.village, user.dateOfBirth, user.eligibilityConfirmed, user.idProofReference, user.approvalStatus, user.requestedBy, user.createdAt))
    }

    suspend fun login(email: String, password: String): UserAccountEntity? = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim().lowercase(Locale.getDefault())
        if (cleanEmail == "admin@mahila.local" && password == "admin123") {
            return@withContext UserAccountEntity(-1, "Admin", cleanEmail, "", password, "Admin", "All groups", "Office", "01/01/1990", true, "Built-in admin", "Approved", "System", System.currentTimeMillis())
        }
        dao.loginBlocking(cleanEmail, password)
    }

    suspend fun resetPassword(email: String, newPassword: String): Boolean = withContext(Dispatchers.IO) {
        dao.resetPasswordBlocking(email.trim().lowercase(Locale.getDefault()), newPassword) > 0
    }

    suspend fun updateUserApproval(id: Long, status: String) = withContext(Dispatchers.IO) {
        dao.updateUserApprovalBlocking(id, status)
    }

    suspend fun updateMemberApproval(id: Long, status: String) = withContext(Dispatchers.IO) {
        dao.updateMemberApprovalBlocking(id, status)
    }

    suspend fun addExpense(amount: Double, category: String, note: String, paymentMethod: String, date: Long, ownerEmail: String) = withContext(Dispatchers.IO) {
        dao.insertExpenseBlocking(ExpenseEntity(0, amount, category.ifBlank { "Others" }, date, note, paymentMethod.ifBlank { "Cash" }, ownerEmail))
    }

    suspend fun addIncome(amount: Double, source: String, note: String, date: Long, ownerEmail: String) = withContext(Dispatchers.IO) {
        dao.insertIncomeBlocking(IncomeEntity(0, amount, date, source.ifBlank { "Income" }, note, ownerEmail))
    }

    suspend fun updateExpense(id: Long, amount: Double, category: String, note: String, paymentMethod: String, date: Long, ownerEmail: String) = withContext(Dispatchers.IO) {
        dao.updateExpenseBlocking(ExpenseEntity(id, amount, category.ifBlank { "Others" }, date, note, paymentMethod.ifBlank { "Cash" }, ownerEmail))
    }

    suspend fun updateIncome(id: Long, amount: Double, source: String, note: String, date: Long, ownerEmail: String) = withContext(Dispatchers.IO) {
        dao.updateIncomeBlocking(IncomeEntity(id, amount, date, source.ifBlank { "Income" }, note, ownerEmail))
    }

    suspend fun saveBudget(periodType: String, limitAmount: Double, ownerEmail: String) = withContext(Dispatchers.IO) {
        dao.saveBudgetBlocking(BudgetEntity(0, periodType, currentPeriodKey(periodType), limitAmount, ownerEmail))
    }

    suspend fun saveProfile(name: String, incomeRange: String, currency: String) = withContext(Dispatchers.IO) {
        dao.saveUserProfileBlocking(UserProfileEntity(1, name.ifBlank { "Mahila User" }, incomeRange.ifBlank { "Not set" }, currency.ifBlank { "INR" }.uppercase(Locale.getDefault()).take(3)))
    }

    suspend fun deleteExpense(expense: ExpenseEntity) = withContext(Dispatchers.IO) { dao.deleteExpenseBlocking(expense) }

    suspend fun deleteIncome(income: IncomeEntity) = withContext(Dispatchers.IO) { dao.deleteIncomeBlocking(income) }

    suspend fun addMember(name: String, phone: String, role: String, groupName: String, monthlySavingGoal: Double, requestedBy: String) = withContext(Dispatchers.IO) {
        dao.insertMemberBlocking(MemberEntity(0, name.ifBlank { "Member" }, phone, role, groupName, monthlySavingGoal, System.currentTimeMillis(), "Pending", requestedBy))
    }

    suspend fun deleteMember(member: MemberEntity) = withContext(Dispatchers.IO) { dao.deleteMemberBlocking(member) }

    suspend fun addLoan(borrowerName: String, amount: Double, interestRate: Double, dueDate: Long, purpose: String, groupName: String) = withContext(Dispatchers.IO) {
        dao.insertLoanBlocking(LoanEntity(0, borrowerName.ifBlank { "Borrower" }, amount, 0.0, interestRate, System.currentTimeMillis(), dueDate, purpose, "Active", groupName))
    }

    suspend fun recordLoanPayment(
        loan: LoanEntity,
        paymentAmount: Double,
        ownerEmail: String? = null,
        paymentDate: Long = System.currentTimeMillis(),
        paymentMethod: String = "Loan repayment",
        note: String = ""
    ) = withContext(Dispatchers.IO) {
        val totalDue = loanTotalDue(loan)
        val pending = (totalDue - loan.paidAmount).coerceAtLeast(0.0)
        val appliedPayment = paymentAmount.coerceIn(0.0, pending)
        if (appliedPayment <= 0.0) return@withContext
        val paid = loan.paidAmount + appliedPayment
        val status = if (paid >= totalDue) "Closed" else "Active"
        dao.updateLoanBlocking(LoanEntity(loan.id, loan.borrowerName, loan.principalAmount, paid, loan.interestRate, loan.issueDate, loan.dueDate, loan.purpose, status, loan.groupName))
        if (!ownerEmail.isNullOrBlank()) {
            val detailNote = note.ifBlank { "Loan repayment for ${loan.borrowerName} in ${loan.groupName}" }
            dao.insertExpenseBlocking(ExpenseEntity(0, appliedPayment, "Loan Repayment", paymentDate, detailNote, paymentMethod, ownerEmail))
        }
    }

    suspend fun deleteLoan(loan: LoanEntity) = withContext(Dispatchers.IO) { dao.deleteLoanBlocking(loan) }

    suspend fun addSaving(memberName: String, amount: Double, note: String, groupName: String) = withContext(Dispatchers.IO) {
        dao.insertSavingBlocking(SavingsEntity(0, memberName.ifBlank { "Member" }, amount, System.currentTimeMillis(), note, groupName))
    }

    suspend fun deleteSaving(saving: SavingsEntity) = withContext(Dispatchers.IO) { dao.deleteSavingBlocking(saving) }

    private fun currentPeriodKey(periodType: String): String {
        val pattern = if (periodType == "Weekly") "YYYY-'W'ww" else "yyyy-MM"
        return SimpleDateFormat(pattern, Locale.getDefault()).format(Date())
    }

    private fun loanInterestAmount(loan: LoanEntity): Double {
        if (loan.principalAmount <= 0.0 || loan.interestRate <= 0.0) return 0.0
        val days = ((loan.dueDate - loan.issueDate).coerceAtLeast(0L) / (24L * 60L * 60L * 1000L)).coerceAtLeast(1L)
        return loan.principalAmount * (loan.interestRate / 100.0) * (days / 365.0)
    }

    private fun loanTotalDue(loan: LoanEntity): Double = loan.principalAmount + loanInterestAmount(loan)
}
