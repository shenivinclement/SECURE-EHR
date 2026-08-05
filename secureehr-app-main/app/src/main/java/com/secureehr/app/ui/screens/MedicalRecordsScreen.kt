package com.secureehr.app.ui.screens

import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.secureehr.app.data.api.MedicalRecord
import com.secureehr.app.data.api.RetrofitClient
import com.secureehr.app.data.blockchain.BlockchainOpState
import com.secureehr.app.data.blockchain.BlockchainViewModel
import com.secureehr.app.data.local.TokenManager
import com.secureehr.app.data.local.db.AppDatabase
import com.secureehr.app.data.local.db.MedicalRecordEntity
import com.secureehr.app.ui.components.BorderColorDim
import com.secureehr.app.ui.components.CardBgColor
import com.secureehr.app.ui.components.EmptyState
import com.secureehr.app.ui.components.ShimmerRecordCard
import com.secureehr.app.ui.components.TextSecondaryColor
import com.secureehr.app.ui.theme.CyanAccent
import com.secureehr.app.ui.theme.TealPrimary
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDateTime

private fun MedicalRecord.toEntity() = MedicalRecordEntity(
    id = id ?: System.currentTimeMillis().toString(),
    title = title ?: "", date = date ?: "", doctor = doctor ?: "", hospital = hospital ?: "",
    notes = notes ?: "", type = type ?: "General", diagnosis = diagnosis,
    treatment = treatment, prescription = prescription
)

private fun MedicalRecordEntity.toApiModel() = MedicalRecord(
    id = id, title = title, date = date, doctor = doctor, hospital = hospital,
    notes = notes, type = type, diagnosis = diagnosis,
    treatment = treatment, prescription = prescription
)

private fun MedicalRecord.toRecordData() =
    "title=$title|date=$date|doctor=$doctor|hospital=$hospital|diagnosis=$diagnosis|treatment=$treatment|notes=$notes"

private val placeholderRecords = listOf(
    MedicalRecord("p1", "Blood Test",   "2024-03-10", "Dr. Smith", "City Hospital",   "CBC normal",         "Lab Report",   "Normal CBC",  "No treatment", "None"),
    MedicalRecord("p2", "X-Ray Chest",  "2024-02-15", "Dr. Jones", "General Clinic",  "Mild congestion",    "Imaging",      "Bronchitis",  "Rest and fluids", "Amoxicillin 500mg"),
    MedicalRecord("p3", "Vaccination",  "2024-01-20", "Dr. Lee",   "Health Center",   "Flu vaccine given",  "Immunization", "Preventive",  "N/A", "Influenza vaccine")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordsScreen(navController: NavController) {
    val context         = LocalContext.current
    val activity        = context as FragmentActivity
    val scope           = rememberCoroutineScope()
    val tokenManager    = remember { TokenManager(context) }
    val db              = remember { AppDatabase.getDatabase(context) }
    val blockchainVm: BlockchainViewModel = viewModel()

    var isAuthenticated by remember { mutableStateOf(false) }
    var authChecked     by remember { mutableStateOf(false) }
    var isLoading       by remember { mutableStateOf(false) }
    var showAddDialog   by remember { mutableStateOf(false) }

    val dbRecords  by db.medicalRecordDao().observeAll().collectAsState(initial = emptyList())
    val chainState by blockchainVm.state.collectAsState()

    // Biometric gate
    LaunchedEffect(Unit) {
        val biometricEnabled = tokenManager.biometricEnabled.first()
        if (!biometricEnabled) { isAuthenticated = true; authChecked = true; return@LaunchedEffect }
        val canAuth = BiometricManager.from(context)
            .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
        if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) {
            isAuthenticated = true; authChecked = true; return@LaunchedEffect
        }
        val executor = ContextCompat.getMainExecutor(context)
        BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                isAuthenticated = true; authChecked = true
            }
            override fun onAuthenticationFailed() {}
            override fun onAuthenticationError(code: Int, msg: CharSequence) {
                if (code == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                    code == BiometricPrompt.ERROR_USER_CANCELED
                ) navController.popBackStack()
                else { isAuthenticated = true; authChecked = true }
            }
        }).authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("Verify Identity")
                .setSubtitle("Biometric required to view Medical Records")
                .setNegativeButtonText("Cancel")
                .build()
        )
    }

    // Sync with backend after auth
    LaunchedEffect(isAuthenticated) {
        if (!isAuthenticated) return@LaunchedEffect
        isLoading = true
        try {
            val token = tokenManager.token.first()
            if (token != null) {
                val apiRecords = RetrofitClient.apiService.getRecords("Bearer $token")
                db.medicalRecordDao().deleteAll()
                db.medicalRecordDao().insertAll(apiRecords.map { it.toEntity() })
            }
        } catch (_: Exception) {
            if (dbRecords.isEmpty()) {
                db.medicalRecordDao().insertAll(placeholderRecords.map { it.toEntity() })
            }
        } finally {
            isLoading = false
        }
    }

    if (!authChecked && !isAuthenticated) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = TealPrimary)
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Medical Records", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF141824))
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }, containerColor = TealPrimary) {
                Icon(Icons.Default.Add, null, tint = Color.White)
            }
        },
        containerColor = Color(0xFF0A0E1A)
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Blockchain op banner
            if (chainState is BlockchainOpState.Loading) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(TealPrimary.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                            .border(1.dp, TealPrimary.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(color = TealPrimary, modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(10.dp))
                        Text((chainState as BlockchainOpState.Loading).message, color = TealPrimary, fontSize = 13.sp)
                    }
                }
            }

            when {
                isLoading && dbRecords.isEmpty() -> items(4) { ShimmerRecordCard() }
                dbRecords.isEmpty() -> item {
                    EmptyState(
                        title = "No Records Yet",
                        subtitle = "Tap the + button to add your first medical record",
                        icon = Icons.Default.Description
                    )
                }
                else -> items(dbRecords, key = { it.id }) { entity ->
                    ExpandableRecordItem(
                        entity = entity,
                        blockchainVm = blockchainVm
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddRecordDialog(
            onDismiss = { showAddDialog = false },
            onSave = { newRecord ->
                scope.launch {
                    val recordId = System.currentTimeMillis().toString()
                    val localRecord = newRecord.copy(id = recordId)
                    db.medicalRecordDao().insert(localRecord.toEntity())

                    // Secure on blockchain in background
                    blockchainVm.secureRecord(
                        recordId = recordId,
                        recordData = localRecord.toRecordData()
                    ) { txHash, hash ->
                        scope.launch {
                            db.medicalRecordDao().updateBlockchainInfo(
                                id = recordId,
                                txHash = txHash,
                                hash = hash,
                                timestamp = LocalDateTime.now().toString().take(19)
                            )
                        }
                    }

                    // Try to sync with backend
                    try {
                        val token = tokenManager.token.first()
                        if (token != null) {
                            val saved = RetrofitClient.apiService.createRecord("Bearer $token", newRecord)
                            db.medicalRecordDao().insert(saved.toEntity())
                        }
                    } catch (_: Exception) {
                        Toast.makeText(context, "Saved locally", Toast.LENGTH_SHORT).show()
                    }
                    showAddDialog = false
                }
            }
        )
    }
}

@Composable
fun ExpandableRecordItem(entity: MedicalRecordEntity, blockchainVm: BlockchainViewModel) {
    var expanded     by remember { mutableStateOf(false) }
    var verifyResult by remember { mutableStateOf<Boolean?>(null) }
    var verifying    by remember { mutableStateOf(false) }

    val isSecured = entity.blockchainVerified && !entity.txHash.isNullOrBlank()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBgColor, RoundedCornerShape(16.dp))
            .border(
                1.dp,
                if (isSecured) TealPrimary.copy(alpha = 0.35f) else BorderColorDim,
                RoundedCornerShape(16.dp)
            )
            .clickable { expanded = !expanded }
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(TealPrimary.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Description, null, tint = TealPrimary, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(entity.title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Color.White)
                    if (isSecured) {
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Default.Shield, null, tint = TealPrimary, modifier = Modifier.size(14.dp))
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text("${entity.doctor} · ${entity.date}", color = TextSecondaryColor, fontSize = 12.sp)
                Text(entity.hospital, color = TealPrimary, fontSize = 11.sp)
                if (!entity.txHash.isNullOrBlank()) {
                    Text(
                        "TX: ${entity.txHash.take(10)}…${entity.txHash.takeLast(6)}",
                        fontSize = 10.sp, color = TextSecondaryColor,
                        overflow = TextOverflow.Ellipsis, maxLines = 1
                    )
                }
            }
            Icon(
                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                null, tint = TextSecondaryColor, modifier = Modifier.size(20.dp)
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(spring()) + fadeIn(tween(180)),
            exit = shrinkVertically(tween(180)) + fadeOut(tween(180))
        ) {
            Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 14.dp)) {
                HorizontalDivider(color = BorderColorDim, thickness = 0.5.dp)
                Spacer(Modifier.height(10.dp))

                if (!entity.diagnosis.isNullOrBlank())   DetailRow("Diagnosis",    entity.diagnosis)
                if (!entity.treatment.isNullOrBlank())   DetailRow("Treatment",    entity.treatment)
                if (!entity.prescription.isNullOrBlank()) DetailRow("Prescription", entity.prescription)
                if (entity.notes.isNotBlank())            DetailRow("Notes",        entity.notes)
                DetailRow("Type", entity.type)

                // Blockchain section
                if (isSecured) {
                    Spacer(Modifier.height(10.dp))
                    HorizontalDivider(color = BorderColorDim, thickness = 0.5.dp)
                    Spacer(Modifier.height(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Shield, null, tint = TealPrimary, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Blockchain Secured", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TealPrimary)
                    }
                    Spacer(Modifier.height(6.dp))

                    if (!entity.chainTimestamp.isNullOrBlank())
                        DetailRow("Stored at", entity.chainTimestamp)
                    if (!entity.recordHash.isNullOrBlank())
                        DetailRow("Hash", "${entity.recordHash.take(12)}…${entity.recordHash.takeLast(8)}")
                    if (!entity.ipfsCid.isNullOrBlank())
                        DetailRow("IPFS CID", entity.ipfsCid)

                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Verify on-chain button
                        if (!entity.recordHash.isNullOrBlank()) {
                            OutlinedButton(
                                onClick = {
                                    verifying = true
                                    verifyResult = null
                                    blockchainVm.verifyRecord(entity.recordHash) {
                                        verifying = false
                                        verifyResult = it
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = when (verifyResult) {
                                        true  -> Color(0xFF4CAF50)
                                        false -> MaterialTheme.colorScheme.error
                                        else  -> TextSecondaryColor
                                    }
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    when (verifyResult) {
                                        true  -> Color(0xFF4CAF50).copy(alpha = 0.5f)
                                        false -> MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                                        else  -> BorderColorDim
                                    }
                                )
                            ) {
                                if (verifying) {
                                    CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp, color = TealPrimary)
                                } else {
                                    Icon(
                                        when (verifyResult) {
                                            true  -> Icons.Default.Verified
                                            false -> Icons.Default.Cancel
                                            else  -> Icons.Default.VerifiedUser
                                        },
                                        null, modifier = Modifier.size(14.dp)
                                    )
                                }
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    when {
                                        verifying        -> "Verifying…"
                                        verifyResult == true  -> "Verified ✓"
                                        verifyResult == false -> "Not found"
                                        else             -> "Verify"
                                    },
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(
            text = "$label: ",
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            color = TealPrimary,
            modifier = Modifier.width(110.dp)
        )
        Text(text = value, fontSize = 13.sp, color = Color.White, modifier = Modifier.weight(1f))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRecordDialog(onDismiss: () -> Unit, onSave: (MedicalRecord) -> Unit) {
    var title        by remember { mutableStateOf("") }
    var hospital     by remember { mutableStateOf("") }
    var doctor       by remember { mutableStateOf("") }
    var date         by remember { mutableStateOf("") }
    var diagnosis    by remember { mutableStateOf("") }
    var treatment    by remember { mutableStateOf("") }
    var notes        by remember { mutableStateOf("") }
    var prescription by remember { mutableStateOf("") }
    var titleError   by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = CardBgColor)) {
            Column(
                modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Add Medical Record", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White, modifier = Modifier.weight(1f))
                    Icon(Icons.Default.Shield, null, tint = TealPrimary, modifier = Modifier.size(18.dp))
                }
                Text("Record will be automatically secured on Ethereum Sepolia testnet", fontSize = 11.sp, color = TextSecondaryColor)

                OutlinedTextField(value = title, onValueChange = { title = it; titleError = false },
                    label = { Text("Title *") }, modifier = Modifier.fillMaxWidth(),
                    isError = titleError, singleLine = true)
                OutlinedTextField(value = hospital, onValueChange = { hospital = it },
                    label = { Text("Hospital") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = doctor, onValueChange = { doctor = it },
                    label = { Text("Doctor") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = date, onValueChange = { date = it },
                    label = { Text("Date (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = diagnosis, onValueChange = { diagnosis = it },
                    label = { Text("Diagnosis") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = treatment, onValueChange = { treatment = it },
                    label = { Text("Treatment") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = prescription, onValueChange = { prescription = it },
                    label = { Text("Prescription") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = notes, onValueChange = { notes = it },
                    label = { Text("Notes") }, modifier = Modifier.fillMaxWidth(), maxLines = 3)

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (title.isBlank()) { titleError = true; return@Button }
                            onSave(MedicalRecord(
                                title = title,
                                hospital = hospital.ifBlank { "Unknown" },
                                doctor = doctor.ifBlank { "Unknown" },
                                date = date.ifBlank { "N/A" },
                                diagnosis = diagnosis.ifBlank { null },
                                treatment = treatment.ifBlank { null },
                                prescription = prescription.ifBlank { null },
                                notes = notes,
                                type = "General"
                            ))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                    ) {
                        Icon(Icons.Default.Shield, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Save & Secure")
                    }
                }
            }
        }
    }
}
