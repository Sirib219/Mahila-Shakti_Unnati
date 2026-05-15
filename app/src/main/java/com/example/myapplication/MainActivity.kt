package com.example.myapplication

import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.content.Context
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import java.io.File
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.example.myapplication.data.LoanEntity
import com.example.myapplication.data.MemberEntity
import com.example.myapplication.data.SavingsEntity
import com.example.myapplication.data.UserAccountEntity
import com.example.myapplication.viewmodel.CategoryTotal
import com.example.myapplication.viewmodel.MonthTotal
import com.example.myapplication.viewmodel.SpendSenseUiState
import com.example.myapplication.viewmodel.SpendSenseViewModel
import com.example.myapplication.viewmodel.TransactionItem
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min


class MainActivity : ComponentActivity() {
    private val viewModel: SpendSenseViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(AndroidColor.WHITE))
        setContent {
            MaterialTheme {
                SpendSenseApp(viewModel)
            }
        }
    }
}

@Composable
fun SpendSenseApp(viewModel: SpendSenseViewModel) {
    val state by viewModel.uiState.collectAsState()
    var hasSeenIntro by rememberSaveable { mutableStateOf(false) }
    var selectedTab by rememberSaveable { mutableStateOf(0) }
    var addReturnTab by rememberSaveable { mutableStateOf(0) }
    var editingItem by remember { mutableStateOf<TransactionItem?>(null) }
    val currentUser = state.currentUser
    val tabs = if (currentUser?.userType == "Group Coordinator") {
        listOf("Home", "Group", "Loans", "Add", "Reports", "Settings")
    } else if (currentUser?.userType == "Individual") {
        listOf("Home", "Join SHG", "Loans", "Add", "Reports", "Settings")
    } else {
        listOf("Home", "Loans", "Add", "Reports", "Settings")
    }

    if (!hasSeenIntro) {
        SplashIntro(onContinue = { hasSeenIntro = true })
        return
    }

    if (currentUser == null) {
        AuthScreen(state, viewModel)
        return
    }

    if (currentUser.userType == "Admin") {
        AdminScreen(state, viewModel)
        return
    }

    if (currentUser.approvalStatus != "Approved") {
        PendingApprovalScreen(currentUser, viewModel::logout)
        return
    }

    if (selectedTab >= tabs.size) selectedTab = 0

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = Surface, tonalElevation = 8.dp) {
                tabs.forEachIndexed { index, label ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = {
                            if (label == "Add") {
                                editingItem = null
                                addReturnTab = selectedTab
                            } else {
                                editingItem = null
                            }
                            selectedTab = index
                        },
                        icon = { Text(label.first().toString()) },
                        label = { Text(label) }
                    )
                }
            }
        },
        containerColor = Color.Transparent
    ) { padding ->
        Box(Modifier.fillMaxSize().background(AppBackground).padding(padding)) {
            when (tabs[selectedTab]) {
                "Home" -> Dashboard(
                    state,
                    onAdd = { editingItem = null; addReturnTab = selectedTab; selectedTab = tabs.indexOf("Add") },
                    onAnalytics = { selectedTab = tabs.indexOf("Loans").takeIf { it >= 0 } ?: selectedTab },
                    onReports = { selectedTab = tabs.indexOf("Reports") },
                    onSettings = { selectedTab = tabs.indexOf("Settings") },
                    onEdit = { editingItem = it; addReturnTab = selectedTab; selectedTab = tabs.indexOf("Add") },
                    onDelete = viewModel::deleteTransaction
                )
                "Group" -> CoordinatorGroupScreen(state, viewModel, onBack = { selectedTab = 0 })
                "Join SHG" -> JoinShgScreen(state, viewModel, onBack = { selectedTab = 0 })
                "Loans" -> LoansScreen(state, viewModel, onBack = { selectedTab = 0 })
                "Add" -> AddEntry(viewModel, state, editingItem, onBack = { editingItem = null; selectedTab = addReturnTab }, onSaved = { editingItem = null; selectedTab = addReturnTab })
                "Reports" -> Reports(state, onBack = { selectedTab = 0 }, onEdit = { editingItem = it; addReturnTab = selectedTab; selectedTab = tabs.indexOf("Add") }, onDelete = viewModel::deleteTransaction)
                "Settings" -> SettingsScreen(state, viewModel, onBack = { selectedTab = 0 })
            }
        }
    }
}

@Composable
fun AuthScreen(state: SpendSenseUiState, viewModel: SpendSenseViewModel) {
    var mode by rememberSaveable { mutableStateOf("Register") }
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var userType by rememberSaveable { mutableStateOf("Individual") }
    var groupName by rememberSaveable { mutableStateOf("") }
    var village by rememberSaveable { mutableStateOf("") }
    var dateOfBirth by rememberSaveable { mutableStateOf("") }
    var idProofReference by rememberSaveable { mutableStateOf("") }
    var pendingCameraUri by rememberSaveable { mutableStateOf("") }
    var womenOnlyConsent by rememberSaveable { mutableStateOf(false) }
    val coordinatorGroups = approvedCoordinatorGroupNames(state)
    val context = LocalContext.current
    val idProofPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            runCatching { context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            idProofReference = it.toString()
        }
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        if (saved) {
            idProofReference = pendingCameraUri
        }
    }
    val types = listOf("Individual", "SHG Member", "Group Coordinator")
    val isRegister = mode == "Register"
    val isLogin = mode == "Login"
    val isForgot = mode == "Forgot"

    LazyColumn(
        Modifier.fillMaxSize().background(AppBackground).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(Modifier.height(22.dp))
            Text("Mahila-Shakti Unnati", fontSize = 31.sp, fontWeight = FontWeight.Bold, color = Blue)
            Text("Register or login to manage savings, members, loans, repayments, and reports.", color = Muted)
        }
        item {
            Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(Surface), elevation = CardDefaults.cardElevation(5.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        PressableChip(selected = isRegister, onClick = { mode = "Register" }, label = "Register")
                        PressableChip(selected = isLogin, onClick = { mode = "Login" }, label = "Login")
                        PressableChip(selected = isForgot, onClick = { mode = "Forgot" }, label = "Forgot")
                    }
                    if (isRegister) {
                        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full name") }, modifier = Modifier.fillMaxWidth())
                        Text("User type", fontWeight = FontWeight.Bold)
                        ChipGrid(types, userType) { userType = it; groupName = "" }
                        if (userType == "SHG Member") {
                            Text("Select existing SHG group", fontWeight = FontWeight.Bold)
                            if (coordinatorGroups.isEmpty()) {
                                Text("No SHG group is open yet. A coordinator must create and get admin approval for a group first.", color = Red, fontSize = 12.sp)
                            } else {
                                ChipGrid(coordinatorGroups, groupName) { groupName = it }
                            }
                        }
                        if (userType == "Group Coordinator") {
                            OutlinedTextField(value = groupName, onValueChange = { groupName = it }, label = { Text("New group name / Self-help group *") }, modifier = Modifier.fillMaxWidth())
                        }
                        OutlinedTextField(value = village, onValueChange = { village = it }, label = { Text("Village / Area") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = dateOfBirth, onValueChange = { dateOfBirth = it.take(10) }, label = { Text("Date of birth * (DD/MM/YYYY)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text), modifier = Modifier.fillMaxWidth())
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            PressableButton("Upload ID", onClick = { idProofPicker.launch(arrayOf("image/*")) }, modifier = Modifier.weight(1f))
                            PressableButton("Camera", onClick = {
                                val uri = createIdProofUri(context)
                                pendingCameraUri = uri.toString()
                                cameraLauncher.launch(uri)
                            }, modifier = Modifier.weight(1f))
                        }
                        if (idProofReference.isNotBlank()) {
                            Text("ID photo selected for admin verification.", color = Green, fontSize = 12.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Checkbox(checked = womenOnlyConsent, onCheckedChange = { womenOnlyConsent = it })
                            Text("I confirm that I am a woman and I am registering for my own Mahila-Shakti Unnati account.", color = Muted, fontSize = 13.sp)
                        }
                    }
                    OutlinedTextField(value = email, onValueChange = { email = it.trim() }, label = { Text("Email address") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth())
                    if (isRegister) {
                        OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone number") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth())
                    }
                    OutlinedTextField(value = password, onValueChange = { password = it.trim() }, label = { Text(if (isForgot) "New password" else "Password") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
                    if (isRegister || isForgot) {
                        OutlinedTextField(value = confirmPassword, onValueChange = { confirmPassword = it.trim() }, label = { Text("Confirm password") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
                    }
                    state.authMessage?.let { Text(it, color = if (it.contains("Welcome") || it.contains("successful")) Green else Red, fontSize = 13.sp) }
                    PressableButton(
                        text = when (mode) {
                            "Register" -> "Create Account"
                            "Forgot" -> "Reset Password"
                            else -> "Login"
                        },
                        onClick = {
                            when (mode) {
                                "Register" -> viewModel.registerUser(name, email, phone, password, confirmPassword, userType, groupName, village, dateOfBirth, womenOnlyConsent, idProofReference)
                                "Forgot" -> viewModel.resetPassword(email, password, confirmPassword)
                                else -> viewModel.login(email, password)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (isRegister) {
                        Text("After registration, admin approval is required before app access. Demo admin: admin@mahila.local / admin123", color = Muted, fontSize = 12.sp)
                    }
                }
            }
        }
        item {
            DashboardSection("Access by user type", "Individual users track personal records. SHG members record savings and repayments. Coordinators manage members, loans, and reports.") {}
        }
    }
}

@Composable
fun PendingApprovalScreen(user: UserAccountEntity, onLogout: () -> Unit) {
    Box(Modifier.fillMaxSize().background(AppBackground).padding(20.dp), contentAlignment = Alignment.Center) {
        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(Surface), elevation = CardDefaults.cardElevation(5.dp)) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Approval Pending", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Blue)
                Text("${user.name}, your ${user.userType} account request has been sent to the admin.", color = Muted)
                Text("Status: ${user.approvalStatus}", fontWeight = FontWeight.Bold, color = Orange)
                PressableButton("Logout", onClick = onLogout, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
fun AdminScreen(state: SpendSenseUiState, viewModel: SpendSenseViewModel) {
    var selectedStatus by rememberSaveable { mutableStateOf("Requested") }
    var selectedType by rememberSaveable { mutableStateOf("Total") }
    val statusMap = mapOf("Requested" to "Pending", "Approved" to "Approved", "Rejected" to "Rejected")
    val status = statusMap[selectedStatus] ?: "Pending"
    val individualUsers = state.users.filter { it.userType == "Individual" && it.approvalStatus == status }
    val coordinatorUsers = state.users.filter { it.userType == "Group Coordinator" && it.approvalStatus == status }
    val shgUserRequests = state.users.filter { it.userType == "SHG Member" && it.approvalStatus == status }
    val shgMemberRequests = state.members.filter { it.approvalStatus == status }
    LazyColumn(Modifier.fillMaxSize().background(AppBackground).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Spacer(Modifier.height(10.dp))
            Text("Admin Approval", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Blue)
            Text("Review requested, approved, and rejected records by account type.", color = Muted)
        }
        item { ChipGrid(listOf("Requested", "Approved", "Rejected"), selectedStatus) { selectedStatus = it } }
        state.authMessage?.let { message ->
            item { Text(message, color = if (message.contains("Cannot")) Red else Green, fontSize = 13.sp) }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                SummaryCard("Individual", individualUsers.size.toString(), Blue, Modifier.weight(1f), onClick = { selectedType = "Individual" })
                SummaryCard("SHG Group", (shgUserRequests.size + shgMemberRequests.size).toString(), Orange, Modifier.weight(1f), onClick = { selectedType = "SHG Group" })
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                SummaryCard("Coordinator", coordinatorUsers.size.toString(), Green, Modifier.weight(1f), onClick = { selectedType = "Coordinator" })
                SummaryCard("Total", (individualUsers.size + shgUserRequests.size + shgMemberRequests.size + coordinatorUsers.size).toString(), Blue, Modifier.weight(1f), onClick = { selectedType = "Total" })
            }
        }
        item { Text("Showing: $selectedType", fontWeight = FontWeight.Bold, fontSize = 18.sp) }
        if (selectedType == "Individual" || selectedType == "Total") {
            item { AdminSectionHeader("Individual Users", selectedStatus) }
            if (individualUsers.isEmpty()) item { EmptyCard("No $selectedStatus individual users.") } else items(individualUsers, key = { "individual-${it.id}" }) { user ->
                UserApprovalRow(user, showActions = status == "Pending", onApprove = { viewModel.updateUserApproval(user, "Approved") }, onReject = { viewModel.updateUserApproval(user, "Rejected") })
            }
        }
        if (selectedType == "SHG Group" || selectedType == "Total") {
            item { AdminSectionHeader("SHG Group Members", selectedStatus) }
            if (shgUserRequests.isEmpty() && shgMemberRequests.isEmpty()) {
                item { EmptyCard("No $selectedStatus SHG group records.") }
            } else {
                items(shgUserRequests, key = { "shg-user-${it.id}" }) { user ->
                    UserApprovalRow(user, showActions = status == "Pending", onApprove = { viewModel.updateUserApproval(user, "Approved") }, onReject = { viewModel.updateUserApproval(user, "Rejected") })
                }
                items(shgMemberRequests, key = { "member-${it.id}" }) { member ->
                    val matchedUser = state.users.firstOrNull {
                        it.approvalStatus == "Approved" &&
                            it.name.equals(member.name, ignoreCase = true) &&
                            it.phone == member.phone
                    }
                    MemberApprovalRow(member, matchedUser, showActions = status == "Pending", onApprove = { viewModel.updateMemberApproval(member, "Approved") }, onReject = { viewModel.updateMemberApproval(member, "Rejected") })
                }
            }
        }
        if (selectedType == "Coordinator" || selectedType == "Total") {
            item { AdminSectionHeader("Coordinators", selectedStatus) }
            if (coordinatorUsers.isEmpty()) item { EmptyCard("No $selectedStatus coordinators.") } else items(coordinatorUsers, key = { "coordinator-${it.id}" }) { user ->
                UserApprovalRow(user, showActions = status == "Pending", onApprove = { viewModel.updateUserApproval(user, "Approved") }, onReject = { viewModel.updateUserApproval(user, "Rejected") })
            }
        }
        item { PressableButton("Logout Admin", onClick = viewModel::logout, modifier = Modifier.fillMaxWidth()) }
    }
}

@Composable
fun AdminSectionHeader(title: String, status: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(status, color = Muted, fontSize = 12.sp)
    }
}

@Composable
fun UserApprovalRow(user: UserAccountEntity, showActions: Boolean, onApprove: () -> Unit, onReject: () -> Unit) {
    Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(Surface), elevation = CardDefaults.cardElevation(3.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AdminTableHeader()
            AdminTableRow("Name", user.name)
            AdminTableRow("Type", user.userType)
            AdminTableRow("Phone", user.phone.ifBlank { "Not added" })
            AdminTableRow("Group", user.groupName.ifBlank { "No group" })
            AdminTableRow("Area", user.village.ifBlank { "No area" })
            AdminTableRow("Status", user.approvalStatus)
            AdminTableRow("DOB", user.dateOfBirth)
            IdProofPreview(user.idProofReference)
            if (showActions) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    PressableButton("Approve", onClick = onApprove, modifier = Modifier.weight(1f))
                    PressableButton("Reject", onClick = onReject, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun AdminTableHeader() {
    Row(Modifier.fillMaxWidth().background(AppBackground, RoundedCornerShape(8.dp)).padding(8.dp)) {
        Text("Field", fontWeight = FontWeight.Bold, color = Blue, modifier = Modifier.weight(0.35f))
        Text("Value", fontWeight = FontWeight.Bold, color = Blue, modifier = Modifier.weight(0.65f))
    }
}

@Composable
fun AdminTableRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 3.dp)) {
        Text(label, color = Muted, fontSize = 12.sp, modifier = Modifier.weight(0.35f))
        Text(value, fontSize = 12.sp, modifier = Modifier.weight(0.65f))
    }
}

@Composable
fun IdProofPreview(idProofReference: String) {
    val context = LocalContext.current
    val imageBitmap = remember(idProofReference, context) {
        runCatching {
            val uri = idProofReference.toUri()
            context.contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input)?.asImageBitmap()
            }
        }.getOrNull()
    }

    if (imageBitmap == null) {
        Text("ID proof photo is not available on this device.", color = Red, fontSize = 12.sp)
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Image(
            bitmap = imageBitmap,
            contentDescription = "ID proof photo",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().height(170.dp).background(AppBackground, RoundedCornerShape(10.dp))
        )
        PressableButton(
            "Open Full ID Photo",
            onClick = {
                runCatching {
                    val uri = idProofReference.toUri()
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "image/*")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(intent, "Open ID proof"))
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun MemberApprovalRow(member: MemberEntity, matchedUser: UserAccountEntity?, showActions: Boolean, onApprove: () -> Unit, onReject: () -> Unit) {
    Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(Surface), elevation = CardDefaults.cardElevation(3.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AdminTableHeader()
            AdminTableRow("Name", member.name)
            AdminTableRow("Role", member.role)
            AdminTableRow("Phone", member.phone)
            AdminTableRow("Group", member.groupName.ifBlank { "No group" })
            AdminTableRow("Requested by", member.requestedBy)
            AdminTableRow("Status", member.approvalStatus)
            AdminTableRow("Goal", money(member.monthlySavingGoal, "INR"))
            AdminTableRow("Matched user", matchedUser?.let { "${it.name} (${it.phone})" } ?: "No approved user match")
            if (showActions) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    PressableButton("Approve", onClick = onApprove, modifier = Modifier.weight(1f), enabled = matchedUser != null)
                    PressableButton("Reject", onClick = onReject, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun CoordinatorGroupScreen(state: SpendSenseUiState, viewModel: SpendSenseViewModel, onBack: () -> Unit) {
    val user = state.currentUser
    val groupName = user?.groupName.orEmpty()
    val groupMembers = state.members.filter { it.groupName == groupName && it.approvalStatus == "Approved" }
    val pendingMembers = state.members.filter { it.groupName == groupName && it.approvalStatus == "Pending" }
    val rejectedMembers = state.members.filter { it.groupName == groupName && it.approvalStatus == "Rejected" }
    var selectedGroupView by rememberSaveable { mutableStateOf("Approved") }
    var name by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var role by rememberSaveable { mutableStateOf("Member") }
    var savingGoal by rememberSaveable { mutableStateOf("") }
    var addMemberMessage by rememberSaveable { mutableStateOf<String?>(null) }

    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Spacer(Modifier.height(10.dp)); PageHeader("Group Coordinator", "Manage group members and view approved member savings and loan details.", onBack) }
        addMemberMessage?.let { item { Text(it, color = if (it.contains("sent")) Green else Red, fontSize = 13.sp) } }
        state.authMessage?.let { item { Text(it, color = if (it.contains("already") || it.contains("Cannot") || it.contains("required")) Red else Green, fontSize = 13.sp) } }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                SummaryCard(summaryLabel("Approved", selectedGroupView), groupMembers.size.toString(), Blue, Modifier.weight(1f), onClick = { selectedGroupView = "Approved" })
                SummaryCard(summaryLabel("Pending", selectedGroupView), pendingMembers.size.toString(), Orange, Modifier.weight(1f), onClick = { selectedGroupView = "Pending" })
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                SummaryCard(summaryLabel("Rejected", selectedGroupView), rejectedMembers.size.toString(), Red, Modifier.weight(1f), onClick = { selectedGroupView = "Rejected" })
                SummaryCard(summaryLabel("Total", selectedGroupView), (groupMembers.size + pendingMembers.size + rejectedMembers.size).toString(), Blue, Modifier.weight(1f), onClick = { selectedGroupView = "Total" })
            }
        }
        item {
            Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(Surface), elevation = CardDefaults.cardElevation(3.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Request New Member", fontWeight = FontWeight.Bold)
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Member name") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth())
                    ChipGrid(listOf("Member", "Treasurer"), role) { role = it }
                    OutlinedTextField(value = savingGoal, onValueChange = { savingGoal = it }, label = { Text("Monthly saving goal") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                    PressableButton(
                        "Send To Admin",
                        onClick = {
                            val duplicate = state.members.any {
                                it.groupName.equals(groupName, ignoreCase = true) &&
                                    it.name.equals(name, ignoreCase = true) &&
                                    it.phone == phone &&
                                    it.approvalStatus in listOf("Pending", "Approved")
                            }
                            when {
                                phone.isBlank() -> addMemberMessage = "Phone number is required."
                                numberFromText(savingGoal) == null -> addMemberMessage = "Enter a valid monthly saving goal."
                                duplicate -> addMemberMessage = "This member already has a pending or approved request in $groupName."
                                else -> {
                                    viewModel.addMember(name, phone, role, groupName, savingGoal)
                                    addMemberMessage = "Member request sent to admin."
                                    name = ""
                                    phone = ""
                                    savingGoal = ""
                                }
                            }
                        },
                        enabled = name.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        item { Text("Showing: $selectedGroupView", fontWeight = FontWeight.Bold, fontSize = 18.sp) }
        if (selectedGroupView == "Approved" || selectedGroupView == "Total") {
            item { Text("Approved Member Money Details", fontWeight = FontWeight.Bold, fontSize = 18.sp) }
            if (groupMembers.isEmpty()) item { EmptyCard("No approved members in this group yet.") } else items(groupMembers, key = { "money-${it.id}" }) { member ->
                GroupMoneyRow(member, state, viewModel)
            }
        }
        if (selectedGroupView == "Pending" || selectedGroupView == "Total") {
            item { Text("Waiting For Admin", fontWeight = FontWeight.Bold, fontSize = 18.sp) }
            if (pendingMembers.isEmpty()) item { EmptyCard("No pending members in this group.") }
            items(pendingMembers, key = { "pending-${it.id}" }) { member -> MemberRow(member, state.profile.currency, viewModel::deleteMember) }
        }
        if (selectedGroupView == "Rejected" || selectedGroupView == "Total") {
            item { Text("Rejected By Admin", fontWeight = FontWeight.Bold, fontSize = 18.sp) }
            if (rejectedMembers.isEmpty()) item { EmptyCard("No rejected members in this group.") }
            items(rejectedMembers, key = { "rejected-${it.id}" }) { member -> MemberRow(member, state.profile.currency, viewModel::deleteMember) }
        }
    }
}

@Composable
fun JoinShgScreen(state: SpendSenseUiState, viewModel: SpendSenseViewModel, onBack: () -> Unit) {
    val user = state.currentUser
    var groupName by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable(user?.phone) { mutableStateOf(user?.phone ?: "") }
    var savingGoal by rememberSaveable { mutableStateOf("") }
    var village by rememberSaveable(user?.village) { mutableStateOf(user?.village ?: "") }
    var message by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedJoinView by rememberSaveable { mutableStateOf("My Groups") }
    val coordinatorGroups = approvedCoordinatorGroupNames(state)
    val pendingRequests = state.members.filter { it.name.equals(user?.name.orEmpty(), ignoreCase = true) && it.phone == user?.phone && it.approvalStatus == "Pending" }
    val existingRequest = pendingRequests.firstOrNull()
    val approvedMemberships = state.members.filter {
        it.name.equals(user?.name.orEmpty(), ignoreCase = true) &&
            it.phone == user?.phone &&
            it.approvalStatus == "Approved"
    }
    val rejectedRequests = state.members.filter {
        it.name.equals(user?.name.orEmpty(), ignoreCase = true) &&
            it.phone == user?.phone &&
            it.approvalStatus == "Rejected"
    }

    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Spacer(Modifier.height(10.dp)); PageHeader("Join SHG Group", "Send a self-registration request to admin for SHG group membership.", onBack) }
        state.authMessage?.let { item { Text(it, color = if (it.contains("saved")) Green else Red, fontSize = 13.sp) } }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                SummaryCard(summaryLabel("My Groups", selectedJoinView), approvedMemberships.size.toString(), Blue, Modifier.weight(1f), onClick = { selectedJoinView = "My Groups" })
                SummaryCard(summaryLabel("Pending", selectedJoinView), pendingRequests.size.toString(), Orange, Modifier.weight(1f), onClick = { selectedJoinView = "Pending" })
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                SummaryCard(summaryLabel("Rejected", selectedJoinView), rejectedRequests.size.toString(), Red, Modifier.weight(1f), onClick = { selectedJoinView = "Rejected" })
                SummaryCard(summaryLabel("Total", selectedJoinView), (approvedMemberships.size + pendingRequests.size + rejectedRequests.size).toString(), Blue, Modifier.weight(1f), onClick = { selectedJoinView = "Total" })
            }
        }
        if (existingRequest != null) {
            item { EmptyCard("Your request for ${existingRequest.groupName} is waiting for admin approval.") }
        }
        item {
            Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(Surface), elevation = CardDefaults.cardElevation(3.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("SHG Member Request", fontWeight = FontWeight.Bold)
                    OutlinedTextField(value = user?.name.orEmpty(), onValueChange = {}, label = { Text("Full name") }, enabled = false, modifier = Modifier.fillMaxWidth())
                    Text("Select SHG group *", fontWeight = FontWeight.Bold)
                    if (coordinatorGroups.isEmpty()) {
                        Text("There is no open SHG group. A group must be created by a coordinator and approved by admin first.", color = Red, fontSize = 12.sp)
                    } else {
                        ChipGrid(coordinatorGroups, groupName) { groupName = it }
                    }
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone number *") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = village, onValueChange = { village = it }, label = { Text("Village / Area") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = savingGoal, onValueChange = { savingGoal = it }, label = { Text("Monthly saving goal *") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                    message?.let { Text(it, color = if (it.contains("sent")) Green else Red, fontSize = 13.sp) }
                    PressableButton(
                        "Send Request To Admin",
                        onClick = {
                            when {
                                user == null -> message = "Please login again."
                                groupName.isBlank() -> message = "SHG group name is required."
                                groupName !in coordinatorGroups -> message = "This SHG group is not open. Please select an existing coordinator-created group."
                                phone.isBlank() -> message = "Phone number is required."
                                numberFromText(savingGoal) == null -> message = "Enter a valid monthly saving goal."
                                state.members.any { it.groupName.equals(groupName, ignoreCase = true) && it.name.equals(user.name, ignoreCase = true) && it.phone == phone && it.approvalStatus in listOf("Pending", "Approved") } -> message = "You already have a pending or approved request for this group."
                                else -> {
                                    viewModel.addMember(user.name, phone, "Member", groupName, savingGoal)
                                    message = "SHG request sent to admin."
                                    groupName = ""
                                    savingGoal = ""
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        item { Text("Showing: $selectedJoinView", fontWeight = FontWeight.Bold, fontSize = 18.sp) }
        if (selectedJoinView == "My Groups" || selectedJoinView == "Total") {
            item { Text("My SHG Groups", fontWeight = FontWeight.Bold, fontSize = 18.sp) }
            if (approvedMemberships.isEmpty()) {
                item { EmptyCard("No approved SHG group membership yet.") }
            } else {
                items(approvedMemberships, key = { "joined-${it.id}" }) { membership ->
                    JoinedGroupRow(membership, state, viewModel)
                }
            }
        }
        if (selectedJoinView == "Pending" || selectedJoinView == "Total") {
            item { Text("Pending Requests", fontWeight = FontWeight.Bold, fontSize = 18.sp) }
            if (pendingRequests.isEmpty()) item { EmptyCard("No pending SHG requests.") }
            items(pendingRequests, key = { "pending-join-${it.id}" }) { request ->
                SimpleInfoRow(request.groupName, "Requested by ${request.requestedBy} - ${request.approvalStatus}")
            }
        }
        if (selectedJoinView == "Rejected" || selectedJoinView == "Total") {
            item { Text("Rejected Requests", fontWeight = FontWeight.Bold, fontSize = 18.sp) }
            if (rejectedRequests.isEmpty()) item { EmptyCard("No rejected SHG requests.") }
            items(rejectedRequests, key = { "rejected-${it.id}" }) { request ->
                EmptyCard("${request.groupName} was rejected by admin.")
            }
        }
    }
}

@Composable
fun JoinedGroupRow(membership: MemberEntity, state: SpendSenseUiState, viewModel: SpendSenseViewModel) {
    val coordinator = state.users.firstOrNull {
        it.userType == "Group Coordinator" &&
            it.approvalStatus == "Approved" &&
            it.groupName == membership.groupName
    }
    val otherMembers = state.members
        .filter {
            it.approvalStatus == "Approved" &&
                it.groupName == membership.groupName &&
                !it.name.equals(membership.name, ignoreCase = true)
        }
        .map { it.name }
        .distinct()
    Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(Surface), elevation = CardDefaults.cardElevation(3.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(membership.groupName, fontWeight = FontWeight.Bold, color = Blue)
            Text("Coordinator: ${coordinator?.name ?: "Not assigned"}", color = Muted, fontSize = 13.sp)
            Text("My role: ${membership.role}", color = Muted, fontSize = 13.sp)
            Text("Other members", fontWeight = FontWeight.SemiBold)
            if (otherMembers.isEmpty()) {
                Text("No other approved members yet.", color = Muted, fontSize = 12.sp)
            } else {
                otherMembers.forEach { Text(it, fontSize = 12.sp) }
            }
            ShgContributionPanel(membership, state, viewModel, allowPayment = true)
        }
    }
}

@Composable
fun GroupMoneyRow(member: MemberEntity, state: SpendSenseUiState, viewModel: SpendSenseViewModel) {
    val memberSavings = state.savings.filter { it.memberName.equals(member.name, ignoreCase = true) }.sumOf { it.amount }
    val memberLoans = state.loans.filter { it.borrowerName.equals(member.name, ignoreCase = true) }
    val loanPending = memberLoans.sumOf { loanBalanceDue(it) }
    Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(Surface), elevation = CardDefaults.cardElevation(3.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(member.name, fontWeight = FontWeight.Bold)
            Text("${member.role} - Goal ${money(member.monthlySavingGoal, state.profile.currency)} / month", color = Muted, fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                SummaryCard("Savings", money(memberSavings, state.profile.currency), Blue, Modifier.weight(1f))
                SummaryCard("Pending Loan", money(loanPending, state.profile.currency), Orange, Modifier.weight(1f))
            }
            ShgContributionPanel(member, state, viewModel, allowPayment = true)
        }
    }
}

@Composable
fun ShgContributionPanel(member: MemberEntity, state: SpendSenseUiState, viewModel: SpendSenseViewModel, allowPayment: Boolean) {
    var period by rememberSaveable(member.id) { mutableStateOf("Monthly") }
    var paymentAmount by rememberSaveable(member.id, period) { mutableStateOf("") }
    val due = shgContributionDue(member, period)
    val paid = shgContributionPaid(member, state, period)
    val pending = (due - paid).coerceAtLeast(0.0)
    val entered = numberFromText(paymentAmount)
    val exceedsPending = entered != null && entered > pending
    val canPay = allowPayment && pending > 0.0 && entered != null && entered > 0.0 && !exceedsPending
    val afterPaymentPending = (pending - (entered ?: 0.0).coerceAtLeast(0.0)).coerceAtLeast(0.0)

    Column(
        Modifier
            .fillMaxWidth()
            .background(SoftBlue.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("SHG Payment Fulfillment", fontWeight = FontWeight.Bold)
        ChipGrid(listOf("Monthly", "Weekly"), period) {
            period = it
            paymentAmount = ""
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            LoanMetric("Due", money(due, state.profile.currency), Modifier.weight(1f))
            LoanMetric("Paid", money(paid, state.profile.currency), Modifier.weight(1f))
            LoanMetric("Pending", money(pending, state.profile.currency), Modifier.weight(1f))
        }
        Text("Current ${period.lowercase(Locale.getDefault())} period: ${formatDate(shgContributionPeriodStart(period))} to ${formatDate(System.currentTimeMillis())}", color = Muted, fontSize = 12.sp)
        if (pending <= 0.0) {
            Text("Fulfilled for this ${period.lowercase(Locale.getDefault())} period.", color = Green, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
        if (allowPayment) {
            OutlinedTextField(
                value = paymentAmount,
                onValueChange = { paymentAmount = it },
                label = { Text("Payment amount") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            if (exceedsPending) {
                Text("Payment cannot be greater than pending amount ${money(pending, state.profile.currency)}.", color = Red, fontSize = 12.sp)
            }
            if (entered != null && entered > 0.0) {
                Text(
                    "${money(pending, state.profile.currency)} - ${money(entered.coerceAtMost(pending), state.profile.currency)} = ${money(afterPaymentPending, state.profile.currency)} pending",
                    color = if (exceedsPending) Red else Green,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                PressableButton("Fill Pending", onClick = { paymentAmount = "%.0f".format(pending) }, enabled = pending > 0.0, modifier = Modifier.weight(1f))
                PressableButton(
                    "Save Payment",
                    onClick = {
                        viewModel.recordShgContribution(member, paymentAmount, period)
                        paymentAmount = ""
                    },
                    enabled = canPay,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun SplashIntro(onContinue: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(AppBackground)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(Modifier.size(78.dp).background(Blue, RoundedCornerShape(18.dp)), contentAlignment = Alignment.Center) {
                Text("S", color = Color.White, fontSize = 42.sp, fontWeight = FontWeight.Bold)
            }
            Text("Mahila-Shakti Unnati", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Blue)
            Text("Women Micro-Finance Support App", color = Muted, fontSize = 16.sp)
            PressableButton("Start Managing Records", onClick = onContinue, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
fun Dashboard(
    state: SpendSenseUiState,
    onAdd: () -> Unit,
    onAnalytics: () -> Unit,
    onReports: () -> Unit,
    onSettings: () -> Unit,
    onEdit: (TransactionItem) -> Unit,
    onDelete: (TransactionItem) -> Unit
) {
    val budgetAmount = state.budget?.limitAmount ?: 0.0
    var selectedSummary by rememberSaveable { mutableStateOf<String?>(null) }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Spacer(Modifier.height(10.dp)); Text("Mahila-Shakti Unnati", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Blue); Text("Hi, ${state.profile.name}. Manage savings, loans, repayments, and budgets.", color = Muted) }
        item { SummaryGrid(state, selectedSummary) { selectedSummary = if (selectedSummary == it) null else it } }
        selectedSummary?.let { summary ->
            item { DashboardSummaryDetails(summary, state, onEdit, onDelete) }
        }
        item {
            DashboardSection("Micro-Finance Records", "Record savings, income, expenses, loan repayments, and member-related notes.") {
                PressableButton("Add Savings / Repayment / Income", onClick = onAdd, modifier = Modifier.fillMaxWidth())
                PressableButton("View Reports", onClick = onReports, modifier = Modifier.fillMaxWidth())
            }
        }
        item {
            DashboardSection("Budget & Loan Awareness", "See monthly limits, repayment trends, charts, and guidance for better planning.") {
                BudgetCard(state.totalExpense, budgetAmount, state.budget?.periodType ?: "Monthly", state.profile.currency)
                PressableButton("Open Loans", onClick = onAnalytics, modifier = Modifier.fillMaxWidth())
                PressableButton("Manage Budget", onClick = onSettings, modifier = Modifier.fillMaxWidth())
            }
        }
        item {
            DashboardSection("Member Profile", "Update member name, monthly income range, and currency preferences.") {
                SmartInsightCard(state)
                PressableButton("Open Settings", onClick = onSettings, modifier = Modifier.fillMaxWidth())
            }
        }
        item { Text("Recent Activity", fontWeight = FontWeight.Bold, fontSize = 18.sp) }
        if (state.transactions.isEmpty()) item { EmptyCard("No transactions yet. Add your first entry to start tracking.") } else items(state.transactions.take(8), key = { itemKey(it) }) { entry -> TransactionRow(entry, state.profile.currency, onEdit = { onEdit(entry) }, onDelete = { onDelete(entry) }) }
    }
}

@Composable
fun SummaryGrid(state: SpendSenseUiState, selected: String? = null, onSelect: (String) -> Unit = {}) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) { SummaryCard(summaryLabel("Income", selected), money(state.totalIncome, state.profile.currency), Green, Modifier.weight(1f), onClick = { onSelect("Income") }); SummaryCard(summaryLabel("Expense", selected), money(state.totalExpense, state.profile.currency), Red, Modifier.weight(1f), onClick = { onSelect("Expense") }) }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) { SummaryCard(summaryLabel("Savings", selected), money(state.totalSavings, state.profile.currency), Blue, Modifier.weight(1f), onClick = { onSelect("Savings") }); SummaryCard(summaryLabel("Pending Loan", selected), money(state.pendingLoanAmount, state.profile.currency), Orange, Modifier.weight(1f), onClick = { onSelect("Pending Loan") }) }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) { SummaryCard(summaryLabel("Members", selected), state.members.size.toString(), Blue, Modifier.weight(1f), onClick = { onSelect("Members") }); SummaryCard(summaryLabel("Active Loans", selected), state.loans.count { loanBalanceDue(it) > 0.0 }.toString(), Orange, Modifier.weight(1f), onClick = { onSelect("Active Loans") }) }
    }
}

fun summaryLabel(label: String, selected: String?): String = if (label == selected) "$label open" else label

@Composable
fun DashboardSummaryDetails(summary: String, state: SpendSenseUiState, onEdit: (TransactionItem) -> Unit, onDelete: (TransactionItem) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(summary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        when (summary) {
            "Income" -> {
                val rows = state.income.map { TransactionItem.Income(it) }
                if (rows.isEmpty()) EmptyCard("No income records.") else rows.forEach { row -> TransactionRow(row, state.profile.currency, onEdit = { onEdit(row) }, onDelete = { onDelete(row) }) }
            }
            "Expense" -> {
                val rows = state.expenses.map { TransactionItem.Expense(it) }
                if (rows.isEmpty()) EmptyCard("No expense records.") else rows.forEach { row -> TransactionRow(row, state.profile.currency, onEdit = { onEdit(row) }, onDelete = { onDelete(row) }) }
            }
            "Savings" -> if (state.savings.isEmpty()) EmptyCard("No savings records.") else state.savings.forEach { SimpleInfoRow(it.memberName, "${formatDate(it.date)} - ${money(it.amount, state.profile.currency)}") }
            "Pending Loan" -> {
                val pending = state.loans.filter { loanBalanceDue(it) > 0.0 }
                if (pending.isEmpty()) EmptyCard("No pending loans.") else pending.forEach { SimpleInfoRow(it.borrowerName, "Balance ${money(loanBalanceDue(it), state.profile.currency)}") }
            }
            "Members" -> if (state.members.isEmpty()) EmptyCard("No members to show.") else state.members.forEach { SimpleInfoRow(it.name, "${it.role} - ${it.groupName} - ${it.approvalStatus}") }
            "Active Loans" -> {
                val active = state.loans.filter { loanBalanceDue(it) > 0.0 }
                if (active.isEmpty()) EmptyCard("No active loans.") else active.forEach { SimpleInfoRow(it.borrowerName, "${it.purpose.ifBlank { "Loan" }} - balance ${money(loanBalanceDue(it), state.profile.currency)}") }
            }
        }
    }
}

@Composable
fun SimpleInfoRow(title: String, subtitle: String) {
    Column(Modifier.fillMaxWidth().background(Surface, RoundedCornerShape(10.dp)).padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, fontWeight = FontWeight.SemiBold)
        Text(subtitle, color = Muted, fontSize = 12.sp)
    }
}

@Composable
fun DashboardSection(title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(Surface), elevation = CardDefaults.cardElevation(4.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = Muted, fontSize = 13.sp)
            content()
        }
    }
}

@Composable
fun PageHeader(title: String, subtitle: String, onBack: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        PressableTextButton("←", onClick = onBack, color = Blue)
        Text(title, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text(subtitle, color = Muted)
    }
}

@Composable
fun PressableButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val context = LocalContext.current
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, animationSpec = tween(120), label = "buttonScale")
    Button(
        onClick = {
            Toast.makeText(context, "$text clicked", Toast.LENGTH_SHORT).show()
            onClick()
        },
        enabled = enabled,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(16.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 5.dp, pressedElevation = 1.dp, disabledElevation = 0.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Blue, contentColor = Color.White, disabledContainerColor = Muted.copy(alpha = 0.28f)),
        modifier = modifier.height(50.dp).graphicsLayer(scaleX = scale, scaleY = scale)
    ) { Text(text, fontWeight = FontWeight.SemiBold) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PressableChip(selected: Boolean, onClick: () -> Unit, label: String, enabled: Boolean = true) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val hovered by interactionSource.collectIsHoveredAsState()
    val context = LocalContext.current
    val scale by animateFloatAsState(
        targetValue = when {
            pressed -> 0.95f
            hovered -> 1.04f
            else -> 1f
        },
        animationSpec = tween(120),
        label = "chipScale"
    )
    FilterChip(
        selected = selected,
        onClick = {
            Toast.makeText(context, "$label selected", Toast.LENGTH_SHORT).show()
            onClick()
        },
        enabled = enabled,
        interactionSource = interactionSource,
        modifier = Modifier.hoverable(interactionSource).graphicsLayer(scaleX = scale, scaleY = scale),
        label = { Text(label) }
    )
}

@Composable
fun PressableTextButton(text: String, onClick: () -> Unit, color: Color = Blue) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val context = LocalContext.current
    val scale by animateFloatAsState(if (pressed) 0.94f else 1f, animationSpec = tween(120), label = "textButtonScale")
    TextButton(
        onClick = {
            Toast.makeText(context, "$text clicked", Toast.LENGTH_SHORT).show()
            onClick()
        },
        interactionSource = interactionSource,
        modifier = Modifier.graphicsLayer(scaleX = scale, scaleY = scale)
    ) {
        Text(text, color = color, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun SummaryCard(label: String, value: String, color: Color, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val context = LocalContext.current
    val cleanLabel = label.removeSuffix(" open")
    val cardModifier = if (onClick == null) modifier else modifier.clickable {
        Toast.makeText(context, "$cleanLabel opened", Toast.LENGTH_SHORT).show()
        onClick()
    }
    Card(modifier = cardModifier, shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(Surface), elevation = CardDefaults.cardElevation(3.dp)) { Column(Modifier.padding(14.dp)) { Text(label, color = Muted, fontSize = 12.sp); Spacer(Modifier.height(6.dp)); Text(value, color = color, fontWeight = FontWeight.Bold, fontSize = 18.sp) } }
}

@Composable
fun BudgetCard(expense: Double, budget: Double, periodType: String, currency: String) {
    val progress = if (budget <= 0.0) 0f else (expense / budget).coerceIn(0.0, 1.0).toFloat()
    val message = when { budget <= 0.0 -> "Set a monthly budget in Settings."; progress >= 1f -> "Limit reached. Review repayments and optional spending."; progress >= 0.9f -> "Alert: you are near your budget limit."; progress >= 0.7f -> "Heads up: spending is climbing."; else -> "Budget is under control." }
    Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(Surface), elevation = CardDefaults.cardElevation(4.dp)) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("$periodType Budget", fontWeight = FontWeight.Bold); Text(if (budget > 0.0) money(budget, currency) else "Not set", color = Muted) }; LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth(), color = if (progress >= 0.9f) Red else Blue, trackColor = SoftBlue); Text(message, color = if (progress >= 0.9f) Red else Muted, fontSize = 13.sp) } }
}

@Composable
fun SmartInsightCard(state: SpendSenseUiState) {
    val suggestion = state.suggestedMonthlyBudget
    Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(Surface), elevation = CardDefaults.cardElevation(4.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Financial Guidance", fontWeight = FontWeight.Bold)
            Text(state.budgetInsight, color = Muted, fontSize = 13.sp)
            if (suggestion > 0.0) {
                Text("Suggested monthly goal: ${money(suggestion, state.profile.currency)}", color = Blue, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEntry(viewModel: SpendSenseViewModel, state: SpendSenseUiState, editingItem: TransactionItem?, onBack: () -> Unit, onSaved: () -> Unit) {
    val editKey = itemKey(editingItem)
    val context = LocalContext.current
    var isExpense by rememberSaveable(editKey) { mutableStateOf(editingItem?.isExpense ?: true) }
    var amount by rememberSaveable(editKey) { mutableStateOf(editingItem?.amount?.toString() ?: "") }
    var note by rememberSaveable(editKey) { mutableStateOf(editingItem?.note ?: "") }
    var category by rememberSaveable(editKey) { mutableStateOf(editingItem?.label ?: "Savings") }
    var payment by rememberSaveable(editKey) { mutableStateOf((editingItem as? TransactionItem.Expense)?.paymentMethod ?: "UPI") }
    var selectedDate by rememberSaveable(editKey) { mutableStateOf(editingItem?.date ?: System.currentTimeMillis()) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDate)
    val categories = listOf("Savings", "Loan Repayment", "Household", "Business", "Health", "Education", "Personal", "Others")
    val title = if (editingItem == null) "Add Finance Record" else "Edit Finance Record"
    val isLoanRepayment = isExpense && category == "Loan Repayment" && editingItem == null
    val repaymentLoans = state.loans
        .filter { loanBalanceDue(it) > 0.0 }
        .sortedWith(compareBy<LoanEntity> { it.groupName.lowercase(Locale.getDefault()) }.thenBy { it.dueDate })
    var selectedLoanId by rememberSaveable(editKey) { mutableStateOf(repaymentLoans.firstOrNull()?.id ?: 0L) }
    var loanMenuExpanded by rememberSaveable(editKey) { mutableStateOf(false) }
    val selectedLoan = repaymentLoans.firstOrNull { it.id == selectedLoanId } ?: repaymentLoans.firstOrNull()
    val selectedLoanPending = selectedLoan?.let { loanBalanceDue(it) } ?: 0.0
    val repaymentHistory = selectedLoan?.let { loanRepaymentHistory(state, it) } ?: emptyList()
    val parsedAmount = numberFromText(amount)
    val hasPositiveAmount = parsedAmount != null && parsedAmount > 0.0
    val incomeLimit = numberFromText(state.profile.incomeRange)
    val exceedsIncomeLimit = !isExpense && incomeLimit != null && parsedAmount != null && parsedAmount > incomeLimit
    val currentExpenseAmount = (editingItem as? TransactionItem.Expense)?.amount ?: 0.0
    val availableForExpense = state.totalIncome - (state.totalExpense - currentExpenseAmount)
    val exceedsAvailableBalance = isExpense && category != "Loan Repayment" && parsedAmount != null && parsedAmount > availableForExpense
    val exceedsLoanPending = isLoanRepayment && selectedLoan != null && parsedAmount != null && parsedAmount > selectedLoanPending
    val canSave = hasPositiveAmount && !exceedsIncomeLimit && !exceedsAvailableBalance && !exceedsLoanPending && (!isLoanRepayment || selectedLoan != null)

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = { TextButton(onClick = { selectedDate = datePickerState.selectedDateMillis ?: selectedDate; showDatePicker = false }) { Text("OK") } },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = datePickerState) }
    }

    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Spacer(Modifier.height(10.dp)); PageHeader(title, "Record savings, income, loan repayment, expense, date, and note.", onBack) }
        item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { PressableChip(selected = isExpense, onClick = { if (editingItem == null) isExpense = true }, label = "Expense / Repayment", enabled = editingItem == null || isExpense); PressableChip(selected = !isExpense, onClick = { if (editingItem == null) isExpense = false }, label = "Income / Saving", enabled = editingItem == null || !isExpense) } }
        item { OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Amount") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth()) }
        if (exceedsIncomeLimit) item { Text("Income cannot be greater than your profile income range of ${money(incomeLimit ?: 0.0, state.profile.currency)}.", color = Red, fontSize = 13.sp) }
        if (exceedsAvailableBalance) item { Text("Expense cannot be greater than your available balance of ${money(availableForExpense, state.profile.currency)}.", color = Red, fontSize = 13.sp) }
        if (exceedsLoanPending) item { Text("Repayment cannot be greater than the pending loan amount of ${money(selectedLoanPending, state.profile.currency)}.", color = Red, fontSize = 13.sp) }
        item { PressableButton("Date: ${formatDateLong(selectedDate)}", onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) }
        item { Text(if (isExpense) "Record Type" else "Income / Savings Source", fontWeight = FontWeight.Bold); if (isExpense) ChipGrid(categories, category) { category = it } else OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Source, member, or saving group") }, modifier = Modifier.fillMaxWidth()) }
        if (isLoanRepayment) {
            item {
                Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(Surface), elevation = CardDefaults.cardElevation(3.dp)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Select group loan", fontWeight = FontWeight.Bold)
                        if (repaymentLoans.isEmpty()) {
                            Text("No active group loans are available for your enrolled groups.", color = Muted, fontSize = 13.sp)
                        } else {
                            Box(Modifier.fillMaxWidth()) {
                                PressableButton(
                                    text = selectedLoan?.let { loanDropdownLabel(it, state.profile.currency) } ?: "Choose loan",
                                    onClick = { loanMenuExpanded = true },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                DropdownMenu(
                                    expanded = loanMenuExpanded,
                                    onDismissRequest = { loanMenuExpanded = false },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    repaymentLoans.forEach { loan ->
                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text(loanChoiceLabel(loan), fontWeight = FontWeight.SemiBold)
                                                    Text("Balance ${money(loanBalanceDue(loan), state.profile.currency)} | Paid ${money(loan.paidAmount, state.profile.currency)}", color = Muted, fontSize = 12.sp)
                                                }
                                            },
                                            onClick = {
                                                selectedLoanId = loan.id
                                                loanMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (selectedLoan != null) {
                item { LoanRepaymentDetailCard(selectedLoan, repaymentHistory, state.profile.currency, context, parsedAmount ?: 0.0) }
            }
        }
        if (isExpense) item { OutlinedTextField(value = payment, onValueChange = { payment = it }, label = { Text(if (isLoanRepayment) "Payment method" else "Payment method / loan reference") }, modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(value = note, onValueChange = { note = it; if (isExpense && editingItem == null && category != "Loan Repayment") category = viewModel.suggestCategory(it) }, label = { Text("Short note, member name, or purpose") }, modifier = Modifier.fillMaxWidth(), minLines = 2) }
        item {
            PressableButton(
                text = if (editingItem == null) "Save Record" else "Update Record",
                onClick = {
                    val value = parsedAmount ?: return@PressableButton
                    if (editingItem == null) {
                        if (isLoanRepayment && selectedLoan != null) {
                            viewModel.recordLoanPaymentFromFinancePage(selectedLoan, value, selectedDate, payment, note)
                        } else {
                            viewModel.addEntry(value, category, note, payment, isExpense, selectedDate)
                        }
                    } else {
                        viewModel.updateTransaction(editingItem, value, category, note, payment, selectedDate)
                    }
                    onSaved()
                },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun ChipGrid(options: List<String>, selected: String, onSelect: (String) -> Unit) { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { options.chunked(3).forEach { row -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { row.forEach { item -> PressableChip(selected = selected == item, onClick = { onSelect(item) }, label = item) } } } } }

@Composable
fun LoanRepaymentDetailCard(loan: LoanEntity, history: List<TransactionItem.Expense>, currency: String, context: Context, draftPayment: Double = 0.0) {
    val totalDue = loanTotalDue(loan)
    val interestAmount = loanInterestAmount(loan)
    val pending = loanBalanceDue(loan)
    val appliedDraft = draftPayment.coerceIn(0.0, pending)
    val afterPaid = loan.paidAmount + appliedDraft
    val afterPending = (pending - appliedDraft).coerceAtLeast(0.0)
    val progress = if (totalDue <= 0.0) 0f else (loan.paidAmount / totalDue).coerceIn(0.0, 1.0).toFloat()
    Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(Surface), elevation = CardDefaults.cardElevation(3.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Loan details", fontWeight = FontWeight.Bold)
            Text("${loan.groupName} - ${loan.borrowerName}", color = Blue, fontWeight = FontWeight.SemiBold)
            Text("${loan.purpose.ifBlank { "Loan" }} - Due ${formatDateLong(loan.dueDate)} - ${loan.status}", color = Muted, fontSize = 12.sp)
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth(), color = if (pending > 0.0) Blue else Green, trackColor = SoftBlue)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                LoanMetric("Loan", money(loan.principalAmount, currency), Modifier.weight(1f))
                LoanMetric("Interest", money(interestAmount, currency), Modifier.weight(1f))
                LoanMetric("Total", money(totalDue, currency), Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                LoanMetric("Paid", money(loan.paidAmount, currency), Modifier.weight(1f))
                LoanMetric("Balance", money(pending, currency), Modifier.weight(1f))
                LoanMetric("After pay", money(afterPending, currency), Modifier.weight(1f))
            }
            if (appliedDraft > 0.0) Text("After saving: paid ${money(afterPaid, currency)}, balance ${money(afterPending, currency)}.", color = Green, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            PressableButton(
                "Export Loan History CSV",
                onClick = { exportLoanRepaymentCsv(context, loan, history, currency) },
                modifier = Modifier.fillMaxWidth()
            )
            Text("Payment History", fontWeight = FontWeight.Bold)
            if (history.isEmpty()) {
                Text("No repayment history recorded yet.", color = Muted, fontSize = 13.sp)
            } else {
                history.take(5).forEach { item ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(formatDateLong(item.date), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text(item.note, color = Muted, fontSize = 12.sp)
                        }
                        Text(money(item.amount, currency), color = Green, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun LoanMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, color = Muted, fontSize = 11.sp)
        Text(value, fontWeight = FontWeight.Bold, color = Blue, fontSize = 13.sp)
    }
}

fun approvedCoordinatorGroupNames(state: SpendSenseUiState): List<String> =
    state.users
        .filter { it.userType == "Group Coordinator" && it.approvalStatus == "Approved" && it.groupName.isNotBlank() }
        .map { it.groupName.trim() }
        .distinctBy { it.lowercase(Locale.getDefault()) }
        .sorted()

@Composable
fun MembersScreen(state: SpendSenseUiState, viewModel: SpendSenseViewModel, onBack: () -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var role by rememberSaveable { mutableStateOf("Member") }
    var groupName by rememberSaveable(state.currentUser?.groupName) { mutableStateOf(state.currentUser?.groupName ?: "") }
    var savingGoal by rememberSaveable { mutableStateOf("") }
    var savingMember by rememberSaveable { mutableStateOf("") }
    var savingAmount by rememberSaveable { mutableStateOf("") }
    var savingNote by rememberSaveable { mutableStateOf("") }

    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Spacer(Modifier.height(10.dp)); PageHeader("Members & Savings", "Add SHG members and record monthly savings contributions.", onBack) }
        item {
            Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(Surface), elevation = CardDefaults.cardElevation(3.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Register Member", fontWeight = FontWeight.Bold)
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Member name") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth())
                    ChipGrid(listOf("Member", "Coordinator", "Treasurer"), role) { role = it }
                    OutlinedTextField(value = groupName, onValueChange = { groupName = it }, label = { Text("Group name") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = savingGoal, onValueChange = { savingGoal = it }, label = { Text("Monthly saving goal") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                    PressableButton("Save Member", onClick = { viewModel.addMember(name, phone, role, groupName, savingGoal); name = ""; phone = ""; savingGoal = "" }, enabled = name.isNotBlank(), modifier = Modifier.fillMaxWidth())
                }
            }
        }
        item {
            Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(Surface), elevation = CardDefaults.cardElevation(3.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Record Saving", fontWeight = FontWeight.Bold)
                    OutlinedTextField(value = savingMember, onValueChange = { savingMember = it }, label = { Text("Member name") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = savingAmount, onValueChange = { savingAmount = it }, label = { Text("Savings amount") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = savingNote, onValueChange = { savingNote = it }, label = { Text("Note") }, modifier = Modifier.fillMaxWidth())
                    PressableButton("Add Saving", onClick = { viewModel.addSaving(savingMember, savingAmount, savingNote); savingMember = ""; savingAmount = ""; savingNote = "" }, enabled = savingMember.isNotBlank() && numberFromText(savingAmount) != null, modifier = Modifier.fillMaxWidth())
                }
            }
        }
        item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) { SummaryCard("Members", state.members.size.toString(), Blue, Modifier.weight(1f)); SummaryCard("Total Savings", money(state.totalSavings, state.profile.currency), Green, Modifier.weight(1f)) } }
        item { Text("Member List", fontWeight = FontWeight.Bold, fontSize = 18.sp) }
        if (state.members.isEmpty()) item { EmptyCard("No members added yet.") } else items(state.members, key = { it.id }) { member -> MemberRow(member, state.profile.currency, viewModel::deleteMember) }
        item { Text("Recent Savings", fontWeight = FontWeight.Bold, fontSize = 18.sp) }
        if (state.savings.isEmpty()) item { EmptyCard("No savings records yet.") } else items(state.savings.take(8), key = { it.id }) { saving -> SavingRow(saving, state.profile.currency, viewModel::deleteSaving) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoansScreen(state: SpendSenseUiState, viewModel: SpendSenseViewModel, onBack: () -> Unit) {
    var borrower by rememberSaveable { mutableStateOf("") }
    var amount by rememberSaveable { mutableStateOf("") }
    var interest by rememberSaveable { mutableStateOf("0") }
    var purpose by rememberSaveable { mutableStateOf("") }
    var dueDate by rememberSaveable { mutableStateOf(System.currentTimeMillis() + 30L * 24L * 60L * 60L * 1000L) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = dueDate)
    val draftPrincipal = numberFromText(amount) ?: 0.0
    val draftInterestRate = numberFromText(interest) ?: 0.0
    val draftInterestAmount = loanInterestAmount(draftPrincipal, draftInterestRate, System.currentTimeMillis(), dueDate)
    val draftTotalDue = draftPrincipal + draftInterestAmount

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = { TextButton(onClick = { dueDate = datePickerState.selectedDateMillis ?: dueDate; showDatePicker = false }) { Text("OK") } },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = datePickerState) }
    }

    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Spacer(Modifier.height(10.dp)); PageHeader("Loans & Repayments", "Create loan records, track pending amounts, and close loans after repayment.", onBack) }
        item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) { SummaryCard("Principal", money(state.totalLoanPrincipal, state.profile.currency), Blue, Modifier.weight(1f)); SummaryCard("Balance", money(state.pendingLoanAmount, state.profile.currency), Red, Modifier.weight(1f)) } }
        item {
            Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(Surface), elevation = CardDefaults.cardElevation(3.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("New Loan", fontWeight = FontWeight.Bold)
                    OutlinedTextField(value = borrower, onValueChange = { borrower = it }, label = { Text("Borrower / member name") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Loan amount") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = interest, onValueChange = { interest = it }, label = { Text("Interest rate (% yearly)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = purpose, onValueChange = { purpose = it }, label = { Text("Purpose") }, modifier = Modifier.fillMaxWidth())
                    PressableButton("Due date: ${formatDateLong(dueDate)}", onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth())
                    if (draftPrincipal > 0.0) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            LoanMetric("Interest", money(draftInterestAmount, state.profile.currency), Modifier.weight(1f))
                            LoanMetric("Total due", money(draftTotalDue, state.profile.currency), Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            LoanMetric("Monthly pay", money(loanPeriodDue(draftTotalDue, System.currentTimeMillis(), dueDate, "Monthly"), state.profile.currency), Modifier.weight(1f))
                            LoanMetric("Weekly pay", money(loanPeriodDue(draftTotalDue, System.currentTimeMillis(), dueDate, "Weekly"), state.profile.currency), Modifier.weight(1f))
                        }
                        Text("Interest is calculated by the app from today to the due date.", color = Muted, fontSize = 12.sp)
                    }
                    PressableButton("Create Loan", onClick = { viewModel.addLoan(borrower, amount, interest, dueDate, purpose); borrower = ""; amount = ""; purpose = "" }, enabled = borrower.isNotBlank() && numberFromText(amount) != null, modifier = Modifier.fillMaxWidth())
                }
            }
        }
        item { Text("Loan Records", fontWeight = FontWeight.Bold, fontSize = 18.sp) }
        if (state.loans.isEmpty()) item { EmptyCard("No loans added yet.") } else items(state.loans, key = { it.id }) { loan -> LoanRow(loan, state, viewModel) }
    }
}

@Composable
fun Analytics(state: SpendSenseUiState, onBack: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Spacer(Modifier.height(10.dp)); PageHeader("Analytics", "Category split and monthly repayment or expense trend.", onBack) }
        item { ChartCard("Category-wise Records") { PieChart(state.categoryTotals); Spacer(Modifier.height(12.dp)); if (state.categoryTotals.isEmpty()) Text("Add savings, repayment, or expense records to see analytics.", color = Muted) else state.categoryTotals.take(5).forEachIndexed { index, item -> LegendRow(index, item.category, item.total, state.profile.currency) } } }
        item { ChartCard("Monthly Trend") { BarChart(state.monthlyTotals, state.profile.currency) } }
        item { ChartCard("Top 3 Record Types") { if (state.categoryTotals.isEmpty()) Text("No micro-finance records yet.", color = Muted) else state.categoryTotals.take(3).forEachIndexed { index, item -> Text("${index + 1}. ${item.category}: ${money(item.total, state.profile.currency)}") } } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Reports(state: SpendSenseUiState, onBack: () -> Unit, onEdit: (TransactionItem) -> Unit, onDelete: (TransactionItem) -> Unit) {
    val context = LocalContext.current
    var typeFilter by rememberSaveable { mutableStateOf("All") }
    var categoryFilter by rememberSaveable { mutableStateOf("All") }
    var dateFilter by rememberSaveable { mutableStateOf("All") }
    var minAmount by rememberSaveable { mutableStateOf("") }
    var maxAmount by rememberSaveable { mutableStateOf("") }
    var startDate by rememberSaveable { mutableStateOf<Long?>(null) }
    var endDate by rememberSaveable { mutableStateOf<Long?>(null) }
    var pickingDate by rememberSaveable { mutableStateOf<String?>(null) }
    val categories = listOf("All") + state.transactions.map { it.label }.distinct().sorted()
    val minValue = minAmount.toDoubleOrNull()
    val maxValue = maxAmount.toDoubleOrNull()

    if (pickingDate != null) {
        val initialDate = if (pickingDate == "End") endDate else startDate
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialDate)
        DatePickerDialog(
            onDismissRequest = { pickingDate = null },
            confirmButton = {
                TextButton(onClick = {
                    val selected = datePickerState.selectedDateMillis
                    if (selected != null && pickingDate == "Start") startDate = selected
                    if (selected != null && pickingDate == "End") endDate = selected
                    pickingDate = null
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { pickingDate = null }) { Text("Cancel") } }
        ) { DatePicker(state = datePickerState) }
    }

    val filtered = state.transactions.filter { item ->
        val matchesType = typeFilter == "All" || (typeFilter == "Expense" && item.isExpense) || (typeFilter == "Income" && !item.isExpense)
        val matchesCategory = categoryFilter == "All" || item.label == categoryFilter
        val matchesQuickDate = when (dateFilter) { "Today" -> isToday(item.date); "This Month" -> isThisMonth(item.date); else -> true }
        val matchesRange = (startDate == null || item.date >= startOfDay(startDate!!)) && (endDate == null || item.date <= endOfDay(endDate!!))
        val matchesAmount = (minValue == null || item.amount >= minValue) && (maxValue == null || item.amount <= maxValue)
        matchesType && matchesCategory && matchesQuickDate && matchesRange && matchesAmount
    }
    val expense = filtered.filter { it.isExpense }.sumOf { it.amount }; val income = filtered.filter { !it.isExpense }.sumOf { it.amount }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Spacer(Modifier.height(10.dp)); PageHeader("Reports", "Filter by record type, category, date range, and amount for SHG review.", onBack) }
        item { FilterRow("Type", listOf("All", "Expense", "Income"), typeFilter) { typeFilter = it } }
        item { FilterRow("Date", listOf("All", "Today", "This Month"), dateFilter) { dateFilter = it } }
        item { FilterRow("Category", categories.ifEmpty { listOf("All") }, categoryFilter) { categoryFilter = it } }
        item {
            Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(Surface), elevation = CardDefaults.cardElevation(3.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Custom Range", fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        PressableButton(startDate?.let { formatDateLong(it) } ?: "Start Date", onClick = { pickingDate = "Start" }, modifier = Modifier.weight(1f))
                        PressableButton(endDate?.let { formatDateLong(it) } ?: "End Date", onClick = { pickingDate = "End" }, modifier = Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(value = minAmount, onValueChange = { minAmount = it }, label = { Text("Min") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                        OutlinedTextField(value = maxAmount, onValueChange = { maxAmount = it }, label = { Text("Max") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                    }
                    PressableTextButton("Clear custom filters", onClick = { startDate = null; endDate = null; minAmount = ""; maxAmount = "" })
                }
            }
        }
        item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) { SummaryCard("Report Income", money(income, state.profile.currency), Green, Modifier.weight(1f)); SummaryCard("Report Expense", money(expense, state.profile.currency), Red, Modifier.weight(1f)) } }
        item { PressableButton("Export Filtered CSV", onClick = { exportReportCsv(context, filtered, state.profile.currency) }, enabled = filtered.isNotEmpty(), modifier = Modifier.fillMaxWidth()) }
        if (filtered.isEmpty()) item { EmptyCard("No transactions match these filters.") } else items(filtered, key = { "r-${itemKey(it)}" }) { entry -> TransactionRow(entry, state.profile.currency, onEdit = { onEdit(entry) }, onDelete = { onDelete(entry) }) }
    }
}

@Composable
fun FilterRow(label: String, options: List<String>, selected: String, onSelect: (String) -> Unit) { Column(verticalArrangement = Arrangement.spacedBy(6.dp)) { Text(label, fontWeight = FontWeight.Bold); Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) { options.take(4).forEach { PressableChip(selected = selected == it, onClick = { onSelect(it) }, label = it) } } } }

@Composable
fun SettingsScreen(state: SpendSenseUiState, viewModel: SpendSenseViewModel, onBack: () -> Unit) {
    var budget by rememberSaveable(state.budget?.limitAmount) { mutableStateOf(state.budget?.limitAmount?.toString() ?: "") }; var period by rememberSaveable(state.budget?.periodType) { mutableStateOf(state.budget?.periodType ?: "Monthly") }; var name by rememberSaveable(state.profile.name) { mutableStateOf(state.profile.name) }; var incomeRange by rememberSaveable(state.profile.incomeRange) { mutableStateOf(state.profile.incomeRange) }; var currency by rememberSaveable(state.profile.currency) { mutableStateOf(state.profile.currency) }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Spacer(Modifier.height(10.dp)); PageHeader("Settings", "Member profile, currency, and persistent budget setup.", onBack) }
        item {
            Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(Surface), elevation = CardDefaults.cardElevation(3.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Budget Limit", fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Monthly", "Weekly").forEach { PressableChip(selected = period == it, onClick = { period = it }, label = it) }
                    }
                    OutlinedTextField(value = budget, onValueChange = { budget = it }, label = { Text("Monthly expense / repayment limit") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                    PressableButton("Save Budget", onClick = { viewModel.saveBudget(period, budget) }, enabled = budget.toDoubleOrNull() != null, modifier = Modifier.fillMaxWidth())
                }
            }
        }
        item {
            Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(Surface), elevation = CardDefaults.cardElevation(3.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Suggested Budget Goal", fontWeight = FontWeight.Bold)
                    Text(state.budgetInsight, color = Muted, fontSize = 13.sp)
                    if (state.suggestedMonthlyBudget > 0.0) {
                        Text(money(state.suggestedMonthlyBudget, state.profile.currency), color = Blue, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        PressableButton(
                            text = "Use Suggested Monthly Goal",
                            onClick = {
                                val suggested = "%.0f".format(state.suggestedMonthlyBudget)
                                period = "Monthly"
                                budget = suggested
                                viewModel.saveBudget("Monthly", suggested)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
        item {
            Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(Surface), elevation = CardDefaults.cardElevation(3.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Profile", fontWeight = FontWeight.Bold)
                    state.currentUser?.let {
                        Text("${it.userType} - ${it.groupName.ifBlank { "No group" }} - ${it.village.ifBlank { "No area" }}", color = Muted, fontSize = 13.sp)
                    }
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = incomeRange, onValueChange = { incomeRange = it }, label = { Text("Monthly income / savings capacity") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = currency, onValueChange = { currency = it.uppercase(Locale.getDefault()).take(3) }, label = { Text("Currency") }, modifier = Modifier.fillMaxWidth())
                    PressableButton("Save Profile", onClick = { viewModel.saveProfile(name, incomeRange, currency) }, modifier = Modifier.fillMaxWidth())
                    PressableTextButton("Logout", onClick = viewModel::logout, color = Red)
                }
            }
        }
    }
}

@Composable
fun ChartCard(title: String, content: @Composable ColumnScope.() -> Unit) { Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(Surface), elevation = CardDefaults.cardElevation(3.dp)) { Column(Modifier.padding(16.dp), content = { Text(title, fontWeight = FontWeight.Bold); Spacer(Modifier.height(12.dp)); content() }) } }
@Composable
fun EmptyCard(text: String) { Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(Surface), elevation = CardDefaults.cardElevation(2.dp)) { Text(text, color = Muted, modifier = Modifier.padding(16.dp)) } }

@Composable
fun TransactionRow(entry: TransactionItem, currency: String, onEdit: () -> Unit, onDelete: () -> Unit) {
    val subtitle = when (entry) { is TransactionItem.Expense -> "${formatDate(entry.date)} - ${entry.paymentMethod} - ${entry.note}"; is TransactionItem.Income -> "${formatDate(entry.date)} - ${entry.note}" }
    val isTrackedLoanRepayment = entry is TransactionItem.Expense && entry.label == "Loan Repayment" && entry.paymentMethod.contains("Loan #")
    Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(Surface), elevation = CardDefaults.cardElevation(3.dp)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(entry.label, fontWeight = FontWeight.SemiBold)
                    Text(subtitle.trimEnd(' ', '-'), color = Muted, fontSize = 12.sp)
                }
                Text(if (entry.isExpense) "-${money(entry.amount, currency)}" else "+${money(entry.amount, currency)}", color = if (entry.isExpense) Red else Green, fontWeight = FontWeight.Bold)
            }
            if (isTrackedLoanRepayment) {
                Text("Tracked in loan history", color = Blue, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    PressableTextButton("Edit", onClick = onEdit)
                    PressableTextButton("Delete", onClick = onDelete, color = Red)
                }
            }
        }
    }
}

@Composable
fun MemberRow(member: MemberEntity, currency: String, onDelete: (MemberEntity) -> Unit) {
    Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(Surface), elevation = CardDefaults.cardElevation(3.dp)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(member.name, fontWeight = FontWeight.SemiBold)
                    Text("${member.role} - ${member.groupName.ifBlank { "No group" }} - Joined ${formatDate(member.joinedDate)}", color = Muted, fontSize = 12.sp)
                    Text("Goal: ${money(member.monthlySavingGoal, currency)} / month", color = Blue, fontSize = 12.sp)
                }
                PressableTextButton("Delete", onClick = { onDelete(member) }, color = Red)
            }
        }
    }
}

@Composable
fun SavingRow(saving: SavingsEntity, currency: String, onDelete: (SavingsEntity) -> Unit) {
    Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(Surface), elevation = CardDefaults.cardElevation(3.dp)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(saving.memberName, fontWeight = FontWeight.SemiBold)
                Text("${formatDate(saving.date)} - ${saving.note}", color = Muted, fontSize = 12.sp)
            }
            Text("+${money(saving.amount, currency)}", color = Green, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(8.dp))
            PressableTextButton("Delete", onClick = { onDelete(saving) }, color = Red)
        }
    }
}

@Composable
fun LoanRow(loan: LoanEntity, state: SpendSenseUiState, viewModel: SpendSenseViewModel) {
    val currency = state.profile.currency
    var payment by rememberSaveable(loan.id) { mutableStateOf("") }
    var period by rememberSaveable(loan.id) { mutableStateOf("Monthly") }
    val totalDue = loanTotalDue(loan)
    val pending = loanBalanceDue(loan)
    val paidThisPeriod = loanPeriodPaid(loan, state, period)
    val dueThisPeriod = loanPeriodDue(loan, period)
    val pendingThisPeriod = (dueThisPeriod - paidThisPeriod).coerceAtLeast(0.0).coerceAtMost(pending)
    val paymentValue = numberFromText(payment)
    val afterPaymentBalance = (pending - (paymentValue ?: 0.0).coerceAtLeast(0.0)).coerceAtLeast(0.0)
    val progress = if (totalDue <= 0.0) 0f else (loan.paidAmount / totalDue).coerceIn(0.0, 1.0).toFloat()
    val status = if (pending > 0.0) "Active" else "Closed"
    Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(Surface), elevation = CardDefaults.cardElevation(3.dp)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(loan.borrowerName, fontWeight = FontWeight.SemiBold)
                    Text("${loan.purpose.ifBlank { "Loan" }} - Due ${formatDateLong(loan.dueDate)} - $status", color = Muted, fontSize = 12.sp)
                }
                Text(money(pending, currency), color = if (pending > 0.0) Red else Green, fontWeight = FontWeight.Bold)
            }
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth(), color = if (pending > 0.0) Blue else Green, trackColor = SoftBlue)
            Text("Loan ${money(loan.principalAmount, currency)} | Interest ${money(loanInterestAmount(loan), currency)} | Total ${money(totalDue, currency)} | Paid ${money(loan.paidAmount, currency)}", color = Muted, fontSize = 12.sp)
            Text("Payment Tracking", fontWeight = FontWeight.Bold)
            ChipGrid(listOf("Monthly", "Weekly"), period) { period = it }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                LoanMetric("Due", money(dueThisPeriod, currency), Modifier.weight(1f))
                LoanMetric("Paid", money(paidThisPeriod, currency), Modifier.weight(1f))
                LoanMetric("Pending", money(pendingThisPeriod, currency), Modifier.weight(1f))
            }
            Text("This ${period.lowercase(Locale.getDefault())}: ${formatDate(loanPeriodStart(loan, period))} to ${formatDate(System.currentTimeMillis())}", color = Muted, fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(value = payment, onValueChange = { payment = it }, label = { Text("Repayment") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                PressableButton("Pay", onClick = { viewModel.recordLoanPayment(loan, payment); payment = "" }, enabled = paymentValue != null && paymentValue > 0.0 && paymentValue <= pending)
            }
            if (paymentValue != null && paymentValue > 0.0) {
                Text("After payment: paid ${money(loan.paidAmount + paymentValue.coerceAtMost(pending), currency)}, balance ${money(afterPaymentBalance, currency)}.", color = if (paymentValue <= pending) Green else Red, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            PressableTextButton("Delete Loan", onClick = { viewModel.deleteLoan(loan) }, color = Red)
        }
    }
}

@Composable
fun PieChart(data: List<CategoryTotal>) {
    val colors = chartColors()
    val total = data.sumOf { it.total }.takeIf { it > 0.0 } ?: 1.0
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(210.dp)) {
            if (data.isEmpty()) {
                drawCircle(SoftBlue, style = Stroke(width = 34.dp.toPx()))
                return@Canvas
            }
            var start = -90f
            data.forEachIndexed { index, item ->
                val sweep = (item.total / total * 360).toFloat()
                drawArc(
                    color = colors[index % colors.size],
                    startAngle = start,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = Offset(18.dp.toPx(), 18.dp.toPx()),
                    size = Size(size.width - 36.dp.toPx(), size.height - 36.dp.toPx()),
                    style = Stroke(width = 30.dp.toPx(), cap = StrokeCap.Butt)
                )
                start += sweep
            }
        }
    }
}

@Composable
fun BarChart(data: List<MonthTotal>, currency: String) {
    val maxValue = max(1.0, data.maxOfOrNull { it.total } ?: 1.0)
    Canvas(Modifier.fillMaxWidth().height(170.dp)) {
        val baseline = size.height - 18.dp.toPx()
        val topPadding = 14.dp.toPx()
        drawLine(SoftBlue, Offset(0f, baseline), Offset(size.width, baseline), strokeWidth = 2.dp.toPx())
        drawLine(SoftBlue.copy(alpha = 0.6f), Offset(0f, topPadding), Offset(size.width, topPadding), strokeWidth = 1.dp.toPx())

        if (data.isEmpty()) return@Canvas

        val slotCount = max(data.size, 3)
        val slotWidth = size.width / slotCount
        val barWidth = min(54.dp.toPx(), slotWidth * 0.48f)
        val availableHeight = baseline - topPadding
        val groupOffset = (slotCount - data.size) * slotWidth / 2f
        data.forEachIndexed { index, item ->
            val center = groupOffset + slotWidth * index + slotWidth / 2f
            val barHeight = max(10.dp.toPx(), (item.total / maxValue * availableHeight).toFloat())
            drawRoundRect(
                color = Blue,
                topLeft = Offset(center - barWidth / 2f, baseline - barHeight),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
            )
        }
    }
    if (data.isEmpty()) Text("Add expenses to see monthly trends.", color = Muted) else Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { data.forEach { Text(it.month, fontSize = 11.sp, color = Muted) } }
    if (data.isNotEmpty()) Text("Peak month: ${money(data.maxBy { it.total }.total, currency)}", color = Muted, fontSize = 12.sp)
}

@Composable
fun LegendRow(index: Int, label: String, amount: Double, currency: String) { val colors = chartColors(); Row(verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(10.dp).background(colors[index % colors.size], CircleShape)); Spacer(Modifier.width(8.dp)); Text("$label - ${money(amount, currency)}", fontSize = 13.sp) } }

fun itemKey(item: TransactionItem?): String = when (item) { is TransactionItem.Expense -> "expense-${item.id}"; is TransactionItem.Income -> "income-${item.id}"; null -> "new" }
fun numberFromText(text: String): Double? =
    text.trim().replace(",", "").removeSuffix("%").trim().toDoubleOrNull()
fun money(amount: Double, currency: String): String = when (currency.uppercase(Locale.getDefault())) { "INR" -> "Rs ${"%.0f".format(amount)}"; "USD" -> "${"%.2f".format(amount)}"; else -> "${currency.uppercase(Locale.getDefault())} ${"%.2f".format(amount)}" }
fun formatDate(date: Long): String = SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(date))
fun formatDateLong(date: Long): String = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(date))
fun isToday(date: Long): Boolean = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date(date)) == SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
fun isThisMonth(date: Long): Boolean = SimpleDateFormat("yyyyMM", Locale.getDefault()).format(Date(date)) == SimpleDateFormat("yyyyMM", Locale.getDefault()).format(Date())
fun startOfDay(date: Long): Long = Calendar.getInstance().apply { timeInMillis = date; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
fun endOfDay(date: Long): Long = Calendar.getInstance().apply { timeInMillis = date; set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999) }.timeInMillis
fun startOfMonth(date: Long): Long = Calendar.getInstance().apply { timeInMillis = date; set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
fun startOfWeek(date: Long): Long = Calendar.getInstance().apply { firstDayOfWeek = Calendar.MONDAY; timeInMillis = date; set(Calendar.DAY_OF_WEEK, Calendar.MONDAY); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
fun chartColors(): List<Color> = listOf(Blue, Green, Orange, Red, Purple, Teal)
fun shgContributionPeriodStart(period: String): Long = if (period == "Weekly") startOfWeek(System.currentTimeMillis()) else startOfMonth(System.currentTimeMillis())
fun shgContributionDue(member: MemberEntity, period: String): Double = if (period == "Weekly") member.monthlySavingGoal / 4.0 else member.monthlySavingGoal
fun shgContributionPaid(member: MemberEntity, state: SpendSenseUiState, period: String): Double {
    val start = shgContributionPeriodStart(period)
    return state.savings
        .filter {
            it.groupName.equals(member.groupName, ignoreCase = true) &&
                it.memberName.equals(member.name, ignoreCase = true) &&
                it.date >= start
        }
        .sumOf { it.amount }
}
fun loanInterestAmount(principal: Double, interestRate: Double, issueDate: Long, dueDate: Long): Double {
    if (principal <= 0.0 || interestRate <= 0.0) return 0.0
    val days = ((dueDate - issueDate).coerceAtLeast(0L) / (24L * 60L * 60L * 1000L)).coerceAtLeast(1L)
    return principal * (interestRate / 100.0) * (days / 365.0)
}
fun loanInterestAmount(loan: LoanEntity): Double = loanInterestAmount(loan.principalAmount, loan.interestRate, loan.issueDate, loan.dueDate)
fun loanTotalDue(loan: LoanEntity): Double = loan.principalAmount + loanInterestAmount(loan)
fun loanBalanceDue(loan: LoanEntity): Double = (loanTotalDue(loan) - loan.paidAmount).coerceAtLeast(0.0)
fun loanPeriodStart(loan: LoanEntity, period: String): Long {
    val todayStart = if (period == "Weekly") startOfWeek(System.currentTimeMillis()) else startOfMonth(System.currentTimeMillis())
    return max(todayStart, startOfDay(loan.issueDate))
}
fun loanPeriodCount(issueDate: Long, dueDate: Long, period: String): Int {
    val dayCount = ((dueDate - issueDate).coerceAtLeast(0L) / (24L * 60L * 60L * 1000L)).coerceAtLeast(1L)
    val divisor = if (period == "Weekly") 7.0 else 30.0
    return max(1, kotlin.math.ceil(dayCount / divisor).toInt())
}
fun loanPeriodDue(totalDue: Double, issueDate: Long, dueDate: Long, period: String): Double =
    if (totalDue <= 0.0) 0.0 else totalDue / loanPeriodCount(issueDate, dueDate, period)
fun loanPeriodDue(loan: LoanEntity, period: String): Double =
    loanPeriodDue(loanTotalDue(loan), loan.issueDate, loan.dueDate, period).coerceAtMost(loanBalanceDue(loan).coerceAtLeast(0.0))
fun loanPeriodPaid(loan: LoanEntity, state: SpendSenseUiState, period: String): Double {
    val start = loanPeriodStart(loan, period)
    val idTag = "Loan #${loan.id}"
    return state.transactions
        .filterIsInstance<TransactionItem.Expense>()
        .filter { it.label == "Loan Repayment" && it.paymentMethod.contains(idTag) && it.date >= start }
        .sumOf { it.amount }
}
fun loanPaymentTag(loan: LoanEntity): String = "Loan #${loan.id} - ${loan.groupName}"
fun loanChoiceLabel(loan: LoanEntity): String = "${loan.groupName}: ${loan.purpose.ifBlank { loan.borrowerName }}"
fun loanDropdownLabel(loan: LoanEntity, currency: String): String = "${loanChoiceLabel(loan)} - balance ${money(loanBalanceDue(loan), currency)}"
fun loanRepaymentHistory(state: SpendSenseUiState, loan: LoanEntity): List<TransactionItem.Expense> {
    val idTag = "Loan #${loan.id}"
    return state.transactions
        .filterIsInstance<TransactionItem.Expense>()
        .filter { it.label == "Loan Repayment" && it.paymentMethod.contains(idTag) }
        .sortedByDescending { it.date }
}

val Blue = Color(0xFF1976D2)
val Green = Color(0xFF43A047)
val Red = Color(0xFFE53935)
val Orange = Color(0xFFF59E0B)
val Purple = Color(0xFF7C3AED)
val Teal = Color(0xFF0891B2)
val Background = Color(0xFFF5F5F5)
val Surface = Color(0xFFFEFEFF)
val SoftBlue = Color(0xFFE3F2FD)
val Muted = Color(0xFF667085)
val AppBackground = Brush.verticalGradient(
    listOf(
        Color(0xFFF7FBFF),
        Color(0xFFF5F7EE),
        Color(0xFFF7F3FA)
    )
)
fun exportReportCsv(context: Context, rows: List<TransactionItem>, currency: String) {
    val exportDir = File(context.cacheDir, "exports")
    exportDir.mkdirs()
    val fileName = "mahila-shakti-report-${SimpleDateFormat("yyyyMMdd-HHmm", Locale.getDefault()).format(Date())}.csv"
    val file = File(exportDir, fileName)
    val csv = buildString {
        appendLine("Type,Date,CategoryOrSource,Amount,Currency,PaymentMethod,Note")
        rows.forEach { item ->
            val type = if (item.isExpense) "Expense" else "Income"
            val paymentMethod = when (item) {
                is TransactionItem.Expense -> item.paymentMethod
                is TransactionItem.Income -> "Income"
            }
            appendLine(listOf(type, formatDateLong(item.date), item.label, "%.2f".format(item.amount), currency, paymentMethod, item.note).joinToString(",") { csvCell(it) })
        }
    }
    file.writeText(csv)
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, "Mahila-Shakti Unnati report")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(shareIntent, "Export Mahila-Shakti CSV"))
}

fun exportLoanRepaymentCsv(context: Context, loan: LoanEntity, history: List<TransactionItem.Expense>, currency: String) {
    val exportDir = File(context.cacheDir, "exports")
    exportDir.mkdirs()
    val fileName = "loan-repayment-${loan.id}-${SimpleDateFormat("yyyyMMdd-HHmm", Locale.getDefault()).format(Date())}.csv"
    val file = File(exportDir, fileName)
    val interest = loanInterestAmount(loan)
    val totalDue = loanTotalDue(loan)
    val pending = loanBalanceDue(loan)
    val csv = buildString {
        appendLine("LoanId,Group,Borrower,Purpose,LoanAmount,InterestRate,InterestAmount,TotalDue,PaidAmount,Balance,Currency,IssueDate,DueDate,Status")
        appendLine(
            listOf(
                loan.id.toString(),
                loan.groupName,
                loan.borrowerName,
                loan.purpose,
                "%.2f".format(loan.principalAmount),
                "%.2f".format(loan.interestRate),
                "%.2f".format(interest),
                "%.2f".format(totalDue),
                "%.2f".format(loan.paidAmount),
                "%.2f".format(pending),
                currency,
                formatDateLong(loan.issueDate),
                formatDateLong(loan.dueDate),
                if (pending > 0.0) "Active" else "Closed"
            ).joinToString(",") { csvCell(it) }
        )
        appendLine()
        appendLine("PaymentDate,Amount,Currency,PaymentMethod,Note")
        history.forEach { item ->
            appendLine(listOf(formatDateLong(item.date), "%.2f".format(item.amount), currency, item.paymentMethod, item.note).joinToString(",") { csvCell(it) })
        }
    }
    file.writeText(csv)
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, "Loan repayment history")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(shareIntent, "Export Loan History CSV"))
}

fun createIdProofUri(context: Context): Uri {
    val proofDir = File(context.cacheDir, "id_proofs")
    proofDir.mkdirs()
    val file = File(proofDir, "id-proof-${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

fun csvCell(value: String): String {
    val escaped = value.replace("\"", "\"\"")
    return if (escaped.any { it == ',' || it == '\n' || it == '\r' || it == '"' }) "\"$escaped\"" else escaped
}
