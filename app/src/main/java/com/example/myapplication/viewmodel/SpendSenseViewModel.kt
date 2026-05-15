package com.example.myapplication.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.BudgetEntity
import com.example.myapplication.data.ExpenseEntity
import com.example.myapplication.data.FinanceRepository
import com.example.myapplication.data.IncomeEntity
import com.example.myapplication.data.LoanEntity
import com.example.myapplication.data.MemberEntity
import com.example.myapplication.data.SavingsEntity
import com.example.myapplication.data.SpendSenseDatabase
import com.example.myapplication.data.UserAccountEntity
import com.example.myapplication.data.UserProfileEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

sealed class TransactionItem {
    abstract val amount: Double
    abstract val label: String
    abstract val note: String
    abstract val date: Long
    abstract val isExpense: Boolean

    data class Expense(val row: ExpenseEntity) : TransactionItem() {
        override val amount = row.amount
        override val label = row.category
        override val note = row.note
        override val date = row.date
        override val isExpense = true
        val id = row.id
        val paymentMethod = row.paymentMethod
    }

    data class Income(val row: IncomeEntity) : TransactionItem() {
        override val amount = row.amount
        override val label = row.source
        override val note = row.note
        override val date = row.date
        override val isExpense = false
        val id = row.id
        val paymentMethod = "Income"
    }
}

data class CategoryTotal(val category: String, val total: Double)
data class MonthTotal(val month: String, val total: Double)

data class SpendSenseUiState(
    val expenses: List<ExpenseEntity> = emptyList(),
    val income: List<IncomeEntity> = emptyList(),
    val members: List<MemberEntity> = emptyList(),
    val loans: List<LoanEntity> = emptyList(),
    val savings: List<SavingsEntity> = emptyList(),
    val groupSavings: List<SavingsEntity> = emptyList(),
    val users: List<UserAccountEntity> = emptyList(),
    val budget: BudgetEntity? = null,
    val profile: UserProfileEntity = UserProfileEntity(1, "Mahila User", "Not set", "INR"),
    val currentUser: UserAccountEntity? = null,
    val authMessage: String? = null,
    val pendingUserCount: Int = 0,
    val pendingMemberCount: Int = 0,
    val transactions: List<TransactionItem> = emptyList(),
    val totalExpense: Double = 0.0,
    val totalIncome: Double = 0.0,
    val totalSavings: Double = 0.0,
    val totalLoanPrincipal: Double = 0.0,
    val totalLoanPaid: Double = 0.0,
    val pendingLoanAmount: Double = 0.0,
    val categoryTotals: List<CategoryTotal> = emptyList(),
    val monthlyTotals: List<MonthTotal> = emptyList(),
    val topCategory: String = "None",
    val averageMonthlyExpense: Double = 0.0,
    val suggestedMonthlyBudget: Double = 0.0,
    val budgetInsight: String = "Add savings, income, and repayments to generate financial guidance."
)

class SpendSenseViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = FinanceRepository(SpendSenseDatabase.getInstance(application).financeDao())
    private val currentUser = MutableStateFlow<UserAccountEntity?>(null)
    private val authMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<SpendSenseUiState> = combine(
        repository.expenses,
        repository.income,
        repository.budgets,
        repository.profile,
        repository.members,
        repository.loans,
        repository.savings,
        repository.users,
        currentUser,
        authMessage
    ) { values ->
        val expenses = values[0] as List<ExpenseEntity>
        val income = values[1] as List<IncomeEntity>
        val budgets = values[2] as List<BudgetEntity>
        val profile = values[3] as UserProfileEntity?
        val members = values[4] as List<MemberEntity>
        val loans = values[5] as List<LoanEntity>
        val savings = values[6] as List<SavingsEntity>
        val users = values[7] as List<UserAccountEntity>
        val user = values[8] as UserAccountEntity?
        val message = values[9] as String?
        val userEmail = user?.email.orEmpty()
        val groupName = user?.groupName.orEmpty()
        val userName = user?.name.orEmpty()
        val personalGroupName = "$userName Personal"
        val groupMemberNames = members.filter { it.groupName == groupName && it.approvalStatus == "Approved" }.map { it.name.lowercase(Locale.getDefault()) }.toSet()
        val userApprovedMemberships = members.filter {
            user != null &&
                it.approvalStatus == "Approved" &&
                it.name.equals(user.name, ignoreCase = true) &&
                it.phone == user.phone
        }
        val userGroupNames = userApprovedMemberships.map { it.groupName }.toSet()
        val visibleExpenses = expenses.filter { it.ownerEmail == userEmail }
        val visibleIncome = income.filter { it.ownerEmail == userEmail }
        val visibleBudget = budgets.firstOrNull { it.ownerEmail == userEmail }
        val visibleMembers = when (user?.userType) {
            "Admin" -> members
            "Group Coordinator" -> members.filter { it.groupName == groupName }
            "SHG Member" -> members.filter { it.name.equals(user.name, ignoreCase = true) || it.groupName == groupName }
            "Individual" -> members.filter {
                it.name.equals(user.name, ignoreCase = true) && it.phone == user.phone ||
                    (it.approvalStatus == "Approved" && it.groupName in userGroupNames)
            }
            else -> emptyList()
        }
        val groupSavings = when (user?.userType) {
            "Admin" -> savings
            "Group Coordinator" -> savings.filter { it.groupName == groupName || it.memberName.lowercase(Locale.getDefault()) in groupMemberNames }
            else -> emptyList()
        }
        val visibleSavings = when (user?.userType) {
            "Admin" -> savings
            "Group Coordinator" -> savings.filter { it.memberName.equals(userName, ignoreCase = true) }
            "SHG Member" -> savings.filter {
                it.memberName.equals(user.name, ignoreCase = true) ||
                    userGroupNames.any { group -> group.equals(it.groupName, ignoreCase = true) }
            }
            "Individual" -> savings.filter {
                it.memberName.equals(userName, ignoreCase = true) ||
                    userGroupNames.any { group -> group.equals(it.groupName, ignoreCase = true) }
            }
            else -> emptyList()
        }
        val visibleLoans = when (user?.userType) {
            "Admin" -> loans
            "Group Coordinator" -> loans.filter { it.groupName == groupName || it.borrowerName.lowercase(Locale.getDefault()) in groupMemberNames }
            "SHG Member" -> loans.filter {
                it.borrowerName.equals(userName, ignoreCase = true) &&
                    (it.groupName.equals(groupName, ignoreCase = true) || userGroupNames.any { group -> group.equals(it.groupName, ignoreCase = true) })
            }
            "Individual" -> loans.filter {
                it.groupName.equals(personalGroupName, ignoreCase = true) ||
                    (it.borrowerName.equals(userName, ignoreCase = true) && userGroupNames.any { group -> group.equals(it.groupName, ignoreCase = true) })
            }
            else -> emptyList()
        }
        val totalExpense = visibleExpenses.sumOf { it.amount }
        val totalIncome = visibleIncome.sumOf { it.amount }
        val totalSavings = visibleSavings.sumOf { it.amount }
        val totalLoanPrincipal = visibleLoans.sumOf { it.principalAmount }
        val totalLoanPaid = visibleLoans.sumOf { it.paidAmount }
        val pendingLoanAmount = visibleLoans.sumOf { loanBalanceDue(it) }
        val categoryTotals = visibleExpenses.groupBy { it.category }.map { CategoryTotal(it.key, it.value.sumOf { row -> row.amount }) }.sortedByDescending { it.total }
        val allMonthlyTotals = visibleExpenses.groupBy { monthKey(it.date) }.map { MonthTotal(it.key, it.value.sumOf { row -> row.amount }) }.sortedBy { parseMonthSortKey(it.month) }
        val monthlyTotals = allMonthlyTotals.takeLast(6)
        val recentMonthlyTotals = allMonthlyTotals.takeLast(3)
        val averageMonthlyExpense = recentMonthlyTotals.takeIf { it.isNotEmpty() }?.map { it.total }?.average() ?: 0.0
        val suggestedMonthlyBudget = if (averageMonthlyExpense > 0.0) averageMonthlyExpense * 0.9 else 0.0
        val budgetInsight = when {
            suggestedMonthlyBudget <= 0.0 -> "Add repayments or expenses to generate a budget suggestion."
            visibleBudget == null -> "Try a monthly budget near ${"%.0f".format(suggestedMonthlyBudget)} based on recent spending."
            visibleBudget.limitAmount >= averageMonthlyExpense -> "Your budget is above recent average spend."
            else -> "Your budget is tighter than recent average spend."
        }
        val transactions = visibleExpenses.map { TransactionItem.Expense(it) } + visibleIncome.map { TransactionItem.Income(it) }
        SpendSenseUiState(
            expenses = visibleExpenses,
            income = visibleIncome,
            members = visibleMembers,
            loans = visibleLoans,
            savings = visibleSavings,
            groupSavings = groupSavings,
            users = users,
            budget = visibleBudget,
            profile = profile ?: UserProfileEntity(1, "Mahila User", "Not set", "INR"),
            currentUser = user,
            authMessage = message,
            pendingUserCount = users.count { it.approvalStatus == "Pending" },
            pendingMemberCount = members.count { it.approvalStatus == "Pending" },
            transactions = transactions.sortedByDescending { it.date },
            totalExpense = totalExpense,
            totalIncome = totalIncome,
            totalSavings = totalSavings,
            totalLoanPrincipal = totalLoanPrincipal,
            totalLoanPaid = totalLoanPaid,
            pendingLoanAmount = pendingLoanAmount,
            categoryTotals = categoryTotals,
            monthlyTotals = monthlyTotals,
            topCategory = categoryTotals.firstOrNull()?.category ?: "None",
            averageMonthlyExpense = averageMonthlyExpense,
            suggestedMonthlyBudget = suggestedMonthlyBudget,
            budgetInsight = budgetInsight
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SpendSenseUiState())

    fun registerUser(name: String, email: String, phone: String, password: String, confirmPassword: String, userType: String, groupName: String, village: String, dateOfBirth: String, eligibilityConfirmed: Boolean, idProofReference: String) {
        val cleanEmail = email.trim().lowercase(Locale.getDefault())
        val birthDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply { isLenient = false }
        val parsedBirthDate = runCatching { birthDateFormat.parse(dateOfBirth.trim()) }.getOrNull()
        val age = parsedBirthDate?.let { calculateAge(it) }
        if (name.isBlank()) {
            authMessage.value = "Full name is required."
            return
        }
        if (cleanEmail.isBlank()) {
            authMessage.value = "Email address is required."
            return
        }
        if (phone.isBlank()) {
            authMessage.value = "Phone number is required."
            return
        }
        if (dateOfBirth.isBlank()) {
            authMessage.value = "Date of birth is required."
            return
        }
        if (idProofReference.isBlank()) {
            authMessage.value = "ID proof photo is required."
            return
        }
        if (password.isBlank()) {
            authMessage.value = "Password is required."
            return
        }
        if (confirmPassword.isBlank()) {
            authMessage.value = "Confirm password is required."
            return
        }
        if (userType != "Individual" && groupName.isBlank()) {
            authMessage.value = "Group name is required for SHG Member and Group Coordinator."
            return
        }
        val cleanGroupName = groupName.trim()
        val approvedCoordinatorGroups = uiState.value.users
            .filter { it.userType == "Group Coordinator" && it.approvalStatus == "Approved" }
            .map { it.groupName.lowercase(Locale.getDefault()) }
            .toSet()
        val activeCoordinatorGroups = uiState.value.users
            .filter { it.userType == "Group Coordinator" && it.approvalStatus in listOf("Pending", "Approved") }
            .map { it.groupName.lowercase(Locale.getDefault()) }
            .toSet()
        if (userType == "SHG Member" && cleanGroupName.lowercase(Locale.getDefault()) !in approvedCoordinatorGroups) {
            authMessage.value = "This SHG group is not open. A coordinator must create and get admin approval for the group first."
            return
        }
        if (userType == "Group Coordinator" && cleanGroupName.lowercase(Locale.getDefault()) in activeCoordinatorGroups) {
            authMessage.value = "This group already has a pending or approved coordinator."
            return
        }
        if (!eligibilityConfirmed) {
            authMessage.value = "Self declaration is required."
            return
        }
        if (parsedBirthDate == null) {
            authMessage.value = "Enter date of birth in DD/MM/YYYY format."
            return
        }
        if (age == null || age < 18) {
            authMessage.value = "Only users aged 18 years or above can register."
            return
        }
        if (!cleanEmail.contains("@") || !cleanEmail.contains(".")) {
            authMessage.value = "Enter a valid email address."
            return
        }
        if (password != password.trim() || confirmPassword != confirmPassword.trim()) {
            authMessage.value = "Password cannot start or end with spaces."
            return
        }
        if (password.length < 6) {
            authMessage.value = "Password must be at least 6 characters."
            return
        }
        if (password != confirmPassword) {
            authMessage.value = "Passwords do not match."
            return
        }
        viewModelScope.launch {
            val result = repository.registerUser(name, cleanEmail, phone, password, userType, groupName, village, dateOfBirth, eligibilityConfirmed, idProofReference)
            result.onSuccess {
                authMessage.value = "Registration request sent to admin. Please login after approval."
                saveProfile(it.name, "Not set", "INR")
            }.onFailure {
                authMessage.value = it.message ?: "Registration failed."
            }
        }
    }

    private fun calculateAge(dateOfBirth: Date): Int {
        val today = Calendar.getInstance()
        val dob = Calendar.getInstance().apply { time = dateOfBirth }
        var age = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR)
        if (
            today.get(Calendar.DAY_OF_YEAR) < dob.get(Calendar.DAY_OF_YEAR) ||
            today.get(Calendar.YEAR) == dob.get(Calendar.YEAR)
        ) {
            age--
        }
        return age
    }

    fun login(email: String, password: String) {
        val cleanEmail = email.trim().lowercase(Locale.getDefault())
        if (cleanEmail.isBlank() || password.isBlank()) {
            authMessage.value = "Enter email and password."
            return
        }
        viewModelScope.launch {
            val user = repository.login(cleanEmail, password)
            if (user == null) {
                authMessage.value = "Invalid email or password."
            } else {
                currentUser.value = user
                authMessage.value = if (user.approvalStatus == "Approved") "Welcome, ${user.name}." else "Your account is waiting for admin approval."
                saveProfile(user.name, "Not set", "INR")
            }
        }
    }

    fun updateUserApproval(user: UserAccountEntity, status: String) {
        viewModelScope.launch {
            repository.updateUserApproval(user.id, status)
            authMessage.value = "${user.name} marked as $status."
        }
    }

    fun updateMemberApproval(member: MemberEntity, status: String) {
        if (status == "Approved") {
            val matchedUser = uiState.value.users.any {
                it.approvalStatus == "Approved" &&
                    it.name.equals(member.name, ignoreCase = true) &&
                    it.phone == member.phone
            }
            if (!matchedUser) {
                authMessage.value = "Cannot approve ${member.name}. No approved registered user matches this name and phone."
                return
            }
            val groupExists = uiState.value.users.any {
                it.userType == "Group Coordinator" &&
                    it.approvalStatus == "Approved" &&
                    it.groupName.equals(member.groupName, ignoreCase = true)
            }
            if (!groupExists) {
                authMessage.value = "Cannot approve ${member.name}. This SHG group has no approved coordinator."
                return
            }
            val duplicateApproved = uiState.value.members.any {
                it.id != member.id &&
                    it.approvalStatus == "Approved" &&
                    it.groupName.equals(member.groupName, ignoreCase = true) &&
                    it.name.equals(member.name, ignoreCase = true) &&
                    it.phone == member.phone
            }
            if (duplicateApproved) {
                authMessage.value = "${member.name} is already approved in ${member.groupName}."
                return
            }
        }
        viewModelScope.launch {
            repository.updateMemberApproval(member.id, status)
            authMessage.value = "${member.name} SHG request marked as $status."
        }
    }

    fun resetPassword(email: String, newPassword: String, confirmPassword: String) {
        val cleanEmail = email.trim().lowercase(Locale.getDefault())
        if (cleanEmail.isBlank()) {
            authMessage.value = "Enter your registered email."
            return
        }
        if (newPassword != newPassword.trim() || confirmPassword != confirmPassword.trim()) {
            authMessage.value = "Password cannot start or end with spaces."
            return
        }
        if (newPassword.length < 6) {
            authMessage.value = "Password must be at least 6 characters."
            return
        }
        if (newPassword != confirmPassword) {
            authMessage.value = "Passwords do not match."
            return
        }
        viewModelScope.launch {
            val updated = repository.resetPassword(cleanEmail, newPassword)
            authMessage.value = if (updated) "Password reset successful. Please login." else "No account found for this email."
        }
    }

    fun logout() {
        currentUser.value = null
        authMessage.value = null
    }

    fun addEntry(amount: Double, categoryOrSource: String, note: String, paymentMethod: String, isExpense: Boolean, date: Long) {
        val ownerEmail = currentUser.value?.email ?: return
        viewModelScope.launch { if (isExpense) repository.addExpense(amount, categoryOrSource, note, paymentMethod, date, ownerEmail) else repository.addIncome(amount, categoryOrSource, note, date, ownerEmail) }
    }

    fun updateTransaction(item: TransactionItem, amount: Double, categoryOrSource: String, note: String, paymentMethod: String, date: Long) {
        val ownerEmail = currentUser.value?.email ?: return
        viewModelScope.launch {
            when (item) {
                is TransactionItem.Expense -> repository.updateExpense(item.id, amount, categoryOrSource, note, paymentMethod, date, ownerEmail)
                is TransactionItem.Income -> repository.updateIncome(item.id, amount, categoryOrSource, note, date, ownerEmail)
            }
        }
    }

    fun deleteTransaction(item: TransactionItem) {
        viewModelScope.launch { when (item) { is TransactionItem.Expense -> repository.deleteExpense(item.row); is TransactionItem.Income -> repository.deleteIncome(item.row) } }
    }

    fun saveBudget(periodType: String, limitText: String) {
        val amount = limitText.toDoubleOrNull() ?: return
        val ownerEmail = currentUser.value?.email ?: return
        viewModelScope.launch { repository.saveBudget(periodType, amount, ownerEmail) }
    }

    fun saveProfile(name: String, incomeRange: String, currency: String) {
        viewModelScope.launch { repository.saveProfile(name, incomeRange, currency) }
    }

    fun addMember(name: String, phone: String, role: String, groupName: String, monthlySavingGoal: String) {
        val cleanName = name.trim()
        val cleanPhone = phone.trim()
        val cleanGroup = groupName.trim()
        val amount = numberFromText(monthlySavingGoal)
        if (cleanName.isBlank()) {
            authMessage.value = "Member name is required."
            return
        }
        if (cleanPhone.isBlank()) {
            authMessage.value = "Phone number is required."
            return
        }
        if (cleanGroup.isBlank()) {
            authMessage.value = "SHG group is required."
            return
        }
        if (amount == null) {
            authMessage.value = "Enter a valid monthly saving goal."
            return
        }
        val groupExists = uiState.value.users.any {
            it.userType == "Group Coordinator" &&
                it.approvalStatus == "Approved" &&
                it.groupName.equals(cleanGroup, ignoreCase = true)
        }
        if (!groupExists) {
            authMessage.value = "This SHG group is not open. A coordinator must create and get admin approval for the group first."
            return
        }
        val duplicateRequest = uiState.value.members.any {
            it.groupName.equals(cleanGroup, ignoreCase = true) &&
                it.name.equals(cleanName, ignoreCase = true) &&
                it.phone == cleanPhone &&
                it.approvalStatus in listOf("Pending", "Approved")
        }
        if (duplicateRequest) {
            authMessage.value = "This person already has a pending or approved request for $cleanGroup."
            return
        }
        val requester = currentUser.value?.let { user ->
            when (user.userType) {
                "Group Coordinator" -> "Coordinator: ${user.name}"
                "Individual" -> "Self: ${user.name}"
                else -> user.name
            }
        } ?: "Unknown"
        viewModelScope.launch {
            repository.addMember(cleanName, cleanPhone, role, cleanGroup, amount, requester)
            authMessage.value = "SHG member request sent to admin."
        }
    }

    fun deleteMember(member: MemberEntity) {
        viewModelScope.launch { repository.deleteMember(member) }
    }

    fun addLoan(borrowerName: String, amountText: String, interestText: String, dueDate: Long, purpose: String) {
        val amount = numberFromText(amountText) ?: return
        val interest = numberFromText(interestText) ?: 0.0
        val user = currentUser.value ?: return
        val groupName = user.groupName.ifBlank { "${user.name} Personal" }
        val borrower = borrowerName.ifBlank { user.name }
        viewModelScope.launch { repository.addLoan(borrower, amount, interest, dueDate, purpose, groupName) }
    }

    fun recordLoanPayment(loan: LoanEntity, amountText: String) {
        val amount = numberFromText(amountText) ?: return
        val ownerEmail = currentUser.value?.email
        val paymentMethod = loanPaymentTag(loan)
        val note = "Direct loan repayment for ${loan.borrowerName} in ${loan.groupName}"
        viewModelScope.launch { repository.recordLoanPayment(loan, amount, ownerEmail, System.currentTimeMillis(), paymentMethod, note) }
    }

    fun recordLoanPaymentFromFinancePage(loan: LoanEntity, amount: Double, date: Long, paymentMethod: String, note: String) {
        val ownerEmail = currentUser.value?.email ?: return
        val pending = loanBalanceDue(loan)
        if (amount <= 0.0 || amount > pending) {
            authMessage.value = "Repayment must be between 1 and the pending loan amount."
            return
        }
        val method = "${paymentMethod.ifBlank { "Loan repayment" }} | ${loanPaymentTag(loan)}"
        val detailNote = listOf(
            note.ifBlank { "Loan repayment" },
            "Group: ${loan.groupName}",
            "Borrower: ${loan.borrowerName}",
            "Total due: ${loanTotalDue(loan)}"
        ).joinToString(" | ")
        viewModelScope.launch {
            repository.recordLoanPayment(loan, amount, ownerEmail, date, method, detailNote)
            authMessage.value = "Loan repayment saved for ${loan.groupName}."
        }
    }

    fun deleteLoan(loan: LoanEntity) {
        viewModelScope.launch { repository.deleteLoan(loan) }
    }

    fun addSaving(memberName: String, amountText: String, note: String) {
        val amount = numberFromText(amountText) ?: return
        val groupName = currentUser.value?.groupName ?: return
        viewModelScope.launch { repository.addSaving(memberName, amount, note, groupName) }
    }

    fun recordShgContribution(member: MemberEntity, amountText: String, periodType: String) {
        val amount = numberFromText(amountText) ?: return
        if (amount <= 0.0) {
            authMessage.value = "Enter a valid SHG payment amount."
            return
        }
        val note = "$periodType SHG contribution for ${member.groupName}"
        viewModelScope.launch {
            repository.addSaving(member.name, amount, note, member.groupName)
            authMessage.value = "$periodType SHG payment saved for ${member.name}."
        }
    }

    fun deleteSaving(saving: SavingsEntity) {
        viewModelScope.launch { repository.deleteSaving(saving) }
    }

    fun suggestCategory(note: String): String {
        val text = note.lowercase(Locale.getDefault())
        return when {
            listOf("saving", "savings", "deposit", "group").any(text::contains) -> "Savings"
            listOf("loan", "emi", "repay", "repayment").any(text::contains) -> "Loan Repayment"
            listOf("food", "lunch", "dinner", "coffee", "snack").any(text::contains) -> "Household"
            listOf("bus", "train", "fuel", "cab", "uber", "travel").any(text::contains) -> "Travel"
            listOf("shop", "mall", "amazon", "clothes").any(text::contains) -> "Personal"
            listOf("rent", "room", "hostel").any(text::contains) -> "Rent"
            listOf("doctor", "medicine", "hospital", "health").any(text::contains) -> "Health"
            listOf("business", "stock", "material", "product").any(text::contains) -> "Business"
            else -> "Others"
        }
    }

    private fun monthKey(date: Long): String = SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(Date(date))
    private fun parseMonthSortKey(month: String): Long = runCatching { SimpleDateFormat("MMM yyyy", Locale.getDefault()).parse(month)?.time ?: 0L }.getOrDefault(0L)
    private fun numberFromText(text: String): Double? =
        text.trim().replace(",", "").removeSuffix("%").trim().toDoubleOrNull()
    private fun loanInterestAmount(loan: LoanEntity): Double {
        if (loan.principalAmount <= 0.0 || loan.interestRate <= 0.0) return 0.0
        val days = ((loan.dueDate - loan.issueDate).coerceAtLeast(0L) / (24L * 60L * 60L * 1000L)).coerceAtLeast(1L)
        return loan.principalAmount * (loan.interestRate / 100.0) * (days / 365.0)
    }
    private fun loanTotalDue(loan: LoanEntity): Double = loan.principalAmount + loanInterestAmount(loan)
    private fun loanBalanceDue(loan: LoanEntity): Double = (loanTotalDue(loan) - loan.paidAmount).coerceAtLeast(0.0)
    private fun loanPaymentTag(loan: LoanEntity): String = "Loan #${loan.id} - ${loan.groupName}"
}
