package com.secureehr.app.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.secureehr.app.data.api.RetrofitClient
import com.secureehr.app.data.api.ResearchSharingRequest
import com.secureehr.app.data.api.TwoFactorRequest
import com.secureehr.app.data.api.PatientUpdateRequest
import com.secureehr.app.data.blockchain.BlockchainService
import com.secureehr.app.data.local.TokenManager
import com.secureehr.app.data.local.db.AppDatabase
import com.secureehr.app.navigation.Screen
import com.secureehr.app.ui.components.BorderColorDim
import com.secureehr.app.ui.components.CardBgColor
import com.secureehr.app.ui.components.TextSecondaryColor
import com.secureehr.app.ui.theme.TealPrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class WalletDocument(
    val name: String,
    val date: String,
    val type: String,
    val ipfsHash: String = "",
    val verified: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController) {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    val scope = rememberCoroutineScope()
    val db = remember { AppDatabase.getDatabase(context) }

    var userName by remember { mutableStateOf("") }
    var userEmail by remember { mutableStateOf("") }
    var dateOfBirth by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var bloodGroup by remember { mutableStateOf("") }
    var allergies by remember { mutableStateOf("") }
    var emergencyContact by remember { mutableStateOf("") }
    var dataSharingEnabled by remember { mutableStateOf(true) }
    var twoFaEnabled by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            val token = tokenManager.token.first() ?: return@LaunchedEffect
            val bearer = "Bearer $token"
            try {
                val profile = RetrofitClient.apiService.getMyProfile(bearer)
                userName = profile.name ?: ""
                userEmail = profile.email ?: ""
                twoFaEnabled = profile.twoFactorEnabled ?: false
            } catch (_: Exception) {}
            try {
                val patient = RetrofitClient.apiService.getMyPatient(bearer)
                if (!patient.name.isNullOrBlank()) userName = patient.name ?: ""
                dateOfBirth = patient.dateOfBirth ?: ""
                gender = patient.gender ?: ""
                phone = patient.phone ?: ""
                address = patient.address ?: ""
                bloodGroup = patient.bloodGroup ?: ""
                allergies = patient.allergies ?: ""
                emergencyContact = patient.emergencyContact ?: ""
                dataSharingEnabled = patient.researchDataSharing ?: true
            } catch (_: Exception) {}
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Profile", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF141824))
            )
        },
        bottomBar = { AppBottomNavigationBar(navController) },
        containerColor = Color(0xFF0A0E1A)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .background(TealPrimary.copy(alpha = 0.15f), CircleShape)
                    .border(2.dp, TealPrimary.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.AccountCircle,
                    contentDescription = null,
                    modifier = Modifier.size(72.dp),
                    tint = TealPrimary
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = if (isLoading && userName.isBlank()) "Loading…" else userName.ifBlank { "User" },
                fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White
            )
            Text(
                text = userEmail.ifBlank { if (isLoading) "…" else "—" },
                color = TextSecondaryColor, fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(24.dp))

            ProfileAccordionSection(title = "Personal Information", icon = Icons.Default.Person) {
                var isEditingProfile by remember { mutableStateOf(false) }
                var isSavingProfile by remember { mutableStateOf(false) }
                var draftName by remember { mutableStateOf(userName) }
                var draftPhone by remember { mutableStateOf(phone) }
                var draftDob by remember { mutableStateOf(dateOfBirth) }
                var draftGender by remember { mutableStateOf(gender) }
                var draftBloodGroup by remember { mutableStateOf(bloodGroup) }
                var draftAddress by remember { mutableStateOf(address) }
                var draftAllergies by remember { mutableStateOf(allergies) }
                var draftEmergencyContact by remember { mutableStateOf(emergencyContact) }

                ProfileInfoRow("Email", userEmail.ifBlank { "—" })

                if (isEditingProfile) {
                    OutlinedTextField(draftName, { draftName = it }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(draftPhone, { draftPhone = it }, label = { Text("Phone") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(draftDob, { draftDob = it }, label = { Text("Date of Birth (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(draftGender, { draftGender = it }, label = { Text("Gender") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(draftBloodGroup, { draftBloodGroup = it }, label = { Text("Blood Group") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(draftAddress, { draftAddress = it }, label = { Text("Address") }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(draftAllergies, { draftAllergies = it }, label = { Text("Allergies") }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(draftEmergencyContact, { draftEmergencyContact = it }, label = { Text("Emergency Contact") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                } else {
                    ProfileInfoRow("Full Name", userName.ifBlank { "—" })
                    ProfileInfoRow("Phone", phone.ifBlank { "—" })
                    ProfileInfoRow("Date of Birth", dateOfBirth.ifBlank { "—" })
                    ProfileInfoRow("Gender", gender.ifBlank { "—" })
                    ProfileInfoRow("Blood Group", bloodGroup.ifBlank { "—" })
                    ProfileInfoRow("Address", address.ifBlank { "—" })
                    ProfileInfoRow("Allergies", allergies.ifBlank { "—" })
                    ProfileInfoRow("Emergency Contact", emergencyContact.ifBlank { "—" })
                }

                Spacer(modifier = Modifier.height(8.dp))
                if (isEditingProfile) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { isEditingProfile = false },
                            modifier = Modifier.weight(1f),
                            enabled = !isSavingProfile,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondaryColor),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderColorDim)
                        ) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                isSavingProfile = true
                                scope.launch {
                                    try {
                                        val token = tokenManager.token.first() ?: return@launch
                                        RetrofitClient.apiService.updateMyPatient(
                                            "Bearer $token",
                                            PatientUpdateRequest(
                                                name = draftName,
                                                dateOfBirth = draftDob,
                                                gender = draftGender,
                                                phone = draftPhone,
                                                address = draftAddress,
                                                bloodGroup = draftBloodGroup,
                                                allergies = draftAllergies,
                                                emergencyContact = draftEmergencyContact
                                            )
                                        )
                                        userName = draftName
                                        phone = draftPhone
                                        dateOfBirth = draftDob
                                        gender = draftGender
                                        bloodGroup = draftBloodGroup
                                        address = draftAddress
                                        allergies = draftAllergies
                                        emergencyContact = draftEmergencyContact
                                        isEditingProfile = false
                                        Toast.makeText(context, "Profile updated", Toast.LENGTH_SHORT).show()
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Failed to save: ${e.message}", Toast.LENGTH_SHORT).show()
                                    } finally {
                                        isSavingProfile = false
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = !isSavingProfile,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                        ) {
                            if (isSavingProfile) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Save")
                            }
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = {
                            draftName = userName
                            draftPhone = phone
                            draftDob = dateOfBirth
                            draftGender = gender
                            draftBloodGroup = bloodGroup
                            draftAddress = address
                            draftAllergies = allergies
                            draftEmergencyContact = emergencyContact
                            isEditingProfile = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TealPrimary),
                        border = androidx.compose.foundation.BorderStroke(1.dp, TealPrimary.copy(alpha = 0.5f))
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Edit Information")
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            HealthWalletCard(navController = navController)

            Spacer(modifier = Modifier.height(8.dp))

            ProfileAccordionSection(title = "Privacy & Security", icon = Icons.Default.Lock) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Two-Factor Authentication", fontSize = 14.sp, color = Color.White)
                        Text("Preference only — full verification coming soon", fontSize = 10.sp, color = TextSecondaryColor)
                    }
                    Switch(
                        checked = twoFaEnabled,
                        onCheckedChange = { newValue ->
                            val previous = twoFaEnabled
                            twoFaEnabled = newValue
                            scope.launch {
                                try {
                                    val token = tokenManager.token.first() ?: return@launch
                                    RetrofitClient.apiService.updateTwoFactor(
                                        "Bearer $token",
                                        TwoFactorRequest(newValue)
                                    )
                                } catch (e: Exception) {
                                    twoFaEnabled = previous
                                    Toast.makeText(context, "Failed to update preference: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = TealPrimary)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Research Data Sharing", fontSize = 14.sp, color = Color.White)
                    Switch(
                        checked = dataSharingEnabled,
                        onCheckedChange = { newValue ->
                            val previous = dataSharingEnabled
                            dataSharingEnabled = newValue
                            scope.launch {
                                try {
                                    val token = tokenManager.token.first() ?: return@launch
                                    RetrofitClient.apiService.updateResearchSharing(
                                        "Bearer $token",
                                        ResearchSharingRequest(newValue)
                                    )
                                } catch (e: Exception) {
                                    dataSharingEnabled = previous
                                    Toast.makeText(context, "Failed to update preference: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = TealPrimary)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { navController.navigate(Screen.Settings.route) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TealPrimary),
                    border = androidx.compose.foundation.BorderStroke(1.dp, TealPrimary.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Change Password")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            ProfileAccordionSection(title = "Help & Support", icon = Icons.Default.Help) {
                FaqItem("How do I add a medical record?", "Tap Medical Records on the dashboard, then tap the + button.")
                FaqItem("How do I grant consent?", "Go to Consent Manager and toggle the switch next to a doctor.")
                FaqItem("Is my data secure?", "Yes, all data is encrypted end-to-end using industry standards.")
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                                data = android.net.Uri.parse("mailto:support@secureehr.com")
                                putExtra(android.content.Intent.EXTRA_SUBJECT, "SecureEHR Support Request")
                            }
                            context.startActivity(intent)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TealPrimary),
                        border = androidx.compose.foundation.BorderStroke(1.dp, TealPrimary.copy(alpha = 0.5f))
                    ) {
                        Text("Contact Support", fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = {
                            val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                                data = android.net.Uri.parse("mailto:support@secureehr.com")
                                putExtra(android.content.Intent.EXTRA_SUBJECT, "SecureEHR Issue Report")
                            }
                            context.startActivity(intent)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TealPrimary),
                        border = androidx.compose.foundation.BorderStroke(1.dp, TealPrimary.copy(alpha = 0.5f))
                    ) {
                        Text("Report Issue", fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedButton(
                onClick = {
                    scope.launch {
                        db.medicalRecordDao().deleteAll()
                        db.consentAuditDao().deleteAll()
                        tokenManager.clearHealthWallet()
                        tokenManager.clearToken()
                        navController.navigate(Screen.Login.route) { popUpTo(0) }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Logout", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun HealthWalletCard(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val tokenManager = remember { TokenManager(context) }
    val gson = remember { Gson() }
    val db = remember { AppDatabase.getDatabase(context) }
    val chainRecords by db.medicalRecordDao().observeBlockchainRecords().collectAsState(initial = emptyList())
    val walletAddress = remember { BlockchainService.getWalletAddress("default_user") }

    var expanded by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var saveSuccess by remember { mutableStateOf(false) }

    // Insurance fields
    var insuranceProvider by remember { mutableStateOf("") }
    var policyNumber by remember { mutableStateOf("") }
    var coverageAmount by remember { mutableStateOf("") }
    var expiryDate by remember { mutableStateOf("") }
    var insuranceCardAdded by remember { mutableStateOf(false) }

    // Health ID fields
    var healthId by remember { mutableStateOf("") }
    var abhaNumber by remember { mutableStateOf("") }
    var showQr by remember { mutableStateOf(false) }
    var apiRecordCount by remember { mutableStateOf(0) }

    // Documents
    val documents = remember { mutableStateListOf<WalletDocument>() }

    // Load saved data from DataStore, then enrich with API data
    LaunchedEffect(Unit) {
        insuranceProvider = tokenManager.insuranceProvider.first()
        policyNumber = tokenManager.policyNumber.first()
        coverageAmount = tokenManager.coverageAmount.first()
        expiryDate = tokenManager.insuranceExpiry.first()
        healthId = tokenManager.healthId.first()
        abhaNumber = tokenManager.abhaNumber.first()
        val docsJson = tokenManager.walletDocuments.first()
        if (docsJson.isNotBlank() && docsJson != "[]") {
            runCatching {
                val type = object : TypeToken<List<WalletDocument>>() {}.type
                val loaded = gson.fromJson<List<WalletDocument>>(docsJson, type)
                if (loaded != null) { documents.clear(); documents.addAll(loaded) }
            }
        }
        // Fetch real medical records for document count and insurance auto-fill
        try {
            val token = tokenManager.token.first()
            if (token != null) {
                val records = RetrofitClient.apiService.getRecords("Bearer $token")
                apiRecordCount = records.size
                if (insuranceProvider.isBlank()) {
                    val regex = Regex("(?i)insurance:\\s*([\\w\\s&.'\\-]+?)(?:\\n|,|;|\$)")
                    val counts = mutableMapOf<String, Int>()
                    records.forEach { record ->
                        regex.find(record.notes ?: "")?.groupValues?.get(1)?.trim()?.let { name ->
                            if (name.isNotBlank()) counts[name] = (counts[name] ?: 0) + 1
                        }
                    }
                    counts.maxByOrNull { it.value }?.key?.let { insuranceProvider = it }
                }
            }
        } catch (_: Exception) {}
    }

    val documentPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val name = it.lastPathSegment?.substringAfterLast('/') ?: "Doc_${System.currentTimeMillis()}"
            documents.add(WalletDocument(name = name, date = "2026-05-11", type = "Health Record"))
            saveSuccess = false
        }
    }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) { insuranceCardAdded = true; saveSuccess = false }
    }

    val walletGradient = Brush.linearGradient(
        colors = listOf(Color(0xFF00897B), Color(0xFF004D40))
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            // ── Premium gradient header ──────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(walletGradient)
                    .clickable { expanded = !expanded; saveSuccess = false }
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color.White.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Health Wallet", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                        val totalDocs = apiRecordCount + documents.size
                        Text(
                            text = "$totalDocs document${if (totalDocs != 1) "s" else ""}  ·  " +
                                    if (insuranceProvider.isNotBlank()) "Insurance Active" else "No Insurance",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "${walletAddress.take(8)}…${walletAddress.takeLast(6)}",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.65f),
                            fontWeight = FontWeight.Medium
                        )
                        if (chainRecords.isNotEmpty()) {
                            Text(
                                text = "${chainRecords.size} record${if (chainRecords.size != 1) "s" else ""} on-chain",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.65f)
                            )
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Icon(
                            if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = Color.White
                        )
                        if (chainRecords.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                    .clickable { navController.navigate(Screen.BlockchainStatus.route) }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("On-chain ›", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            // ── Expandable body ──────────────────────────────────────
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(animationSpec = tween(300)) + fadeIn(animationSpec = tween(300)),
                exit = shrinkVertically(animationSpec = tween(300)) + fadeOut(animationSpec = tween(300))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {

                    // ── Insurance Details ────────────────────────────
                    WalletSubHeader("Insurance Details", Icons.Default.Favorite)
                    OutlinedTextField(
                        value = insuranceProvider,
                        onValueChange = { insuranceProvider = it; saveSuccess = false },
                        label = { Text("Insurance Provider") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Business, null, Modifier.size(18.dp)) }
                    )
                    OutlinedTextField(
                        value = policyNumber,
                        onValueChange = { policyNumber = it; saveSuccess = false },
                        label = { Text("Policy Number") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = coverageAmount,
                        onValueChange = { coverageAmount = it; saveSuccess = false },
                        label = { Text("Coverage Amount") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.AttachMoney, null, Modifier.size(18.dp)) }
                    )
                    OutlinedTextField(
                        value = expiryDate,
                        onValueChange = { expiryDate = it; saveSuccess = false },
                        label = { Text("Expiry Date (YYYY-MM-DD)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.DateRange, null, Modifier.size(18.dp)) }
                    )
                    OutlinedButton(
                        onClick = { imagePicker.launch("image/*") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (insuranceCardAdded) Color(0xFF4CAF50) else TealPrimary
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (insuranceCardAdded) Color(0xFF4CAF50).copy(alpha = 0.5f) else TealPrimary.copy(alpha = 0.5f)
                        )
                    ) {
                        Icon(
                            if (insuranceCardAdded) Icons.Default.CheckCircle else Icons.Default.CreditCard,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (insuranceCardAdded) "Insurance Card Added ✓" else "Add Insurance Card")
                    }

                    HorizontalDivider()

                    // ── Health ID ────────────────────────────────────
                    WalletSubHeader("Health ID", Icons.Default.AssignmentInd)
                    OutlinedTextField(
                        value = abhaNumber,
                        onValueChange = { abhaNumber = it; saveSuccess = false },
                        label = { Text("ABHA Number") },
                        placeholder = { Text("14-digit ABHA number") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = healthId,
                        onValueChange = { healthId = it; saveSuccess = false },
                        label = { Text("Ayushman Bharat Health ID") },
                        placeholder = { Text("yourname@abdm") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                if (healthId.isBlank()) healthId = "user${(1000..9999).random()}@abdm"
                                if (abhaNumber.isBlank()) abhaNumber = (10000000000000L + (0..9999999999999L).random()).toString()
                                showQr = true
                                saveSuccess = false
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.VpnKey, null, Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Generate ID", fontSize = 13.sp)
                        }
                        OutlinedButton(
                            onClick = { showQr = !showQr },
                            modifier = Modifier.weight(1f),
                            enabled = healthId.isNotBlank(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TealPrimary),
                            border = androidx.compose.foundation.BorderStroke(1.dp, TealPrimary.copy(alpha = 0.5f))
                        ) {
                            Icon(Icons.Default.QrCode, null, Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("QR Code", fontSize = 13.sp)
                        }
                    }
                    AnimatedVisibility(
                        visible = showQr && healthId.isNotBlank(),
                        enter = expandVertically(tween(250)) + fadeIn(tween(250)),
                        exit = shrinkVertically(tween(250)) + fadeOut(tween(250))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(CardBgColor, RoundedCornerShape(12.dp))
                                .border(1.dp, BorderColorDim, RoundedCornerShape(12.dp))
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Health ID QR Code", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color.White)
                            Spacer(modifier = Modifier.height(12.dp))
                            Box(
                                modifier = Modifier
                                    .size(130.dp)
                                    .background(Color.White, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.QrCode,
                                    contentDescription = "QR Code",
                                    modifier = Modifier.size(100.dp),
                                    tint = Color.Black
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = healthId,
                                fontSize = 13.sp,
                                color = TealPrimary,
                                fontWeight = FontWeight.Medium
                            )
                            if (abhaNumber.isNotBlank()) {
                                Text(text = "ABHA: $abhaNumber", fontSize = 11.sp, color = TextSecondaryColor)
                            }
                            Text("Scan to verify identity", fontSize = 11.sp, color = TextSecondaryColor)
                        }
                    }

                    HorizontalDivider()

                    // ── Documents ────────────────────────────────────
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        WalletSubHeader("Documents", Icons.Default.Description)
                        if (documents.isNotEmpty()) {
                            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary) {
                                Text(
                                    text = "${documents.size}",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    fontSize = 11.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    if (documents.isEmpty()) {
                        Text("No documents added yet.", fontSize = 13.sp, color = TextSecondaryColor)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            documents.toList().forEachIndexed { index, doc ->
                                DocumentItem(doc) {
                                    documents.removeAt(index)
                                    saveSuccess = false
                                }
                            }
                        }
                    }
                    OutlinedButton(
                        onClick = { documentPicker.launch("*/*") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TealPrimary),
                        border = androidx.compose.foundation.BorderStroke(1.dp, TealPrimary.copy(alpha = 0.5f))
                    ) {
                        Icon(Icons.Default.Upload, null, Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Document")
                    }

                    HorizontalDivider()

                    // ── Blockchain Activity ──────────────────────────
                    WalletSubHeader("Blockchain Activity", Icons.Default.Security)
                    if (chainRecords.isEmpty()) {
                        Text(
                            "No records secured on-chain yet. Secure records from the Medical Records screen.",
                            fontSize = 13.sp,
                            color = TextSecondaryColor
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            chainRecords.forEach { record ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(CardBgColor, RoundedCornerShape(10.dp))
                                        .border(1.dp, BorderColorDim, RoundedCornerShape(10.dp))
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Shield, null, tint = TealPrimary, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(record.title, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color.White)
                                        if (!record.txHash.isNullOrBlank()) {
                                            Text(
                                                "TX: ${record.txHash.take(8)}…${record.txHash.takeLast(6)}",
                                                fontSize = 10.sp, color = TextSecondaryColor
                                            )
                                        }
                                        if (!record.ipfsCid.isNullOrBlank()) {
                                            Text(
                                                "IPFS: ${record.ipfsCid.take(12)}…",
                                                fontSize = 10.sp, color = Color(0xFF4CAF50)
                                            )
                                        }
                                    }
                                    Box(
                                        modifier = Modifier
                                            .background(TealPrimary.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("Secured", fontSize = 9.sp, color = TealPrimary, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { navController.navigate(Screen.BlockchainStatus.route) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TealPrimary),
                            border = androidx.compose.foundation.BorderStroke(1.dp, TealPrimary.copy(alpha = 0.5f))
                        ) {
                            Icon(Icons.Default.Hub, null, Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Blockchain Activity", fontSize = 12.sp)
                        }
                        if (chainRecords.isNotEmpty()) {
                            OutlinedButton(
                                onClick = {
                                    val report = buildString {
                                        appendLine("SecureEHR Blockchain Report")
                                        appendLine("Wallet: $walletAddress")
                                        appendLine("Records secured: ${chainRecords.size}")
                                        appendLine()
                                        chainRecords.forEach { r ->
                                            appendLine("• ${r.title}")
                                            if (!r.txHash.isNullOrBlank()) appendLine("  TX: ${r.txHash}")
                                            if (!r.ipfsCid.isNullOrBlank()) appendLine("  IPFS: ${r.ipfsCid}")
                                            if (!r.chainTimestamp.isNullOrBlank()) appendLine("  Time: ${r.chainTimestamp}")
                                        }
                                    }
                                    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(android.content.Intent.EXTRA_TEXT, report)
                                        putExtra(android.content.Intent.EXTRA_SUBJECT, "SecureEHR Blockchain Report")
                                    }
                                    context.startActivity(android.content.Intent.createChooser(intent, "Export Report"))
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondaryColor),
                                border = androidx.compose.foundation.BorderStroke(1.dp, BorderColorDim)
                            ) {
                                Icon(Icons.Default.Share, null, Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Export Report", fontSize = 12.sp)
                            }
                        }
                    }

                    HorizontalDivider()

                    // ── Save ─────────────────────────────────────────
                    AnimatedVisibility(visible = saveSuccess) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Saved successfully!", color = Color(0xFF4CAF50), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                    Button(
                        onClick = {
                            isSaving = true
                            saveSuccess = false
                            scope.launch {
                                tokenManager.saveHealthWallet(
                                    provider = insuranceProvider,
                                    policy = policyNumber,
                                    coverage = coverageAmount,
                                    expiry = expiryDate,
                                    healthId = healthId,
                                    abha = abhaNumber
                                )
                                tokenManager.saveWalletDocuments(gson.toJson(documents.toList()))
                                isSaving = false
                                saveSuccess = true
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isSaving
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Save, null, Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save All Changes")
                    }
                }
            }
        }
    }
}

@Composable
fun WalletSubHeader(title: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(
            icon,
            contentDescription = null,
            tint = TealPrimary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TealPrimary)
    }
}

@Composable
fun DocumentItem(doc: WalletDocument, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF141824), RoundedCornerShape(10.dp))
            .border(1.dp, BorderColorDim, RoundedCornerShape(10.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Description,
            contentDescription = null,
            tint = TealPrimary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = doc.name, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color.White)
            Text(text = "${doc.type} · ${doc.date}", fontSize = 11.sp, color = TextSecondaryColor)
            if (doc.verified && doc.ipfsHash.isNotBlank()) {
                Text(text = "IPFS: ${doc.ipfsHash}", fontSize = 10.sp, color = Color(0xFF4CAF50))
            }
        }
        if (doc.verified) {
            Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(4.dp))
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Delete",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun BlockchainDocumentRow(doc: WalletDocument) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (doc.verified) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (doc.verified) Color(0xFF4CAF50) else TextSecondaryColor,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(doc.name, fontSize = 13.sp, color = Color.White)
            if (doc.verified && doc.ipfsHash.isNotBlank()) {
                Text("IPFS: ${doc.ipfsHash}", fontSize = 10.sp, color = TextSecondaryColor)
            }
        }
        Text(
            text = if (doc.verified) "Verified" else "Unverified",
            fontSize = 11.sp,
            color = if (doc.verified) Color(0xFF4CAF50) else TextSecondaryColor,
            fontWeight = if (doc.verified) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
fun ProfileAccordionSection(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBgColor, RoundedCornerShape(16.dp))
            .border(1.dp, BorderColorDim, RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(TealPrimary.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(title, fontWeight = FontWeight.SemiBold, color = Color.White, modifier = Modifier.weight(1f))
            Icon(
                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = TextSecondaryColor
            )
        }
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(spring()) + fadeIn(tween(200)),
            exit = shrinkVertically(tween(200)) + fadeOut(tween(200))
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                HorizontalDivider(color = BorderColorDim, thickness = 0.5.dp, modifier = Modifier.padding(bottom = 12.dp))
                content()
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun ProfileInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = TextSecondaryColor, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color.White, modifier = Modifier.weight(1.5f))
    }
}

@Composable
fun FaqItem(question: String, answer: String) {
    var open by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { open = !open }
            .padding(vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(question, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color.White, modifier = Modifier.weight(1f))
            Icon(
                if (open) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = TextSecondaryColor
            )
        }
        AnimatedVisibility(visible = open) {
            Text(answer, fontSize = 12.sp, color = TextSecondaryColor, modifier = Modifier.padding(top = 4.dp))
        }
    }
    HorizontalDivider(color = BorderColorDim, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 2.dp))
}
