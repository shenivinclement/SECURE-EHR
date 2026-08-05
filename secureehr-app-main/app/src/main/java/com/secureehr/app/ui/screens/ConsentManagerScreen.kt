package com.secureehr.app.ui.screens

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import android.util.Log
import android.widget.Toast
import com.secureehr.app.data.api.Consent
import com.secureehr.app.data.api.ConsentActionRequest
import com.secureehr.app.data.api.ConsentGrantRequest
import com.secureehr.app.data.api.RetrofitClient
import com.secureehr.app.data.blockchain.BlockchainOpState
import com.secureehr.app.data.blockchain.BlockchainViewModel
import com.secureehr.app.data.local.TokenManager
import com.secureehr.app.data.local.db.AppDatabase
import com.secureehr.app.data.local.db.ConsentAuditEntity
import com.secureehr.app.ui.components.BorderColorDim
import com.secureehr.app.ui.components.CardBgColor
import com.secureehr.app.ui.components.EmptyState
import com.secureehr.app.ui.components.TextSecondaryColor
import com.secureehr.app.ui.theme.TealPrimary
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDateTime

data class ConsentExtra(
    val qualification: String = "",
    val yearsExp: Int = 0,
    val email: String = "",
    val grantedDate: String = "",
    val accessTypes: List<String> = listOf("Medical Records", "Lab Results", "Prescriptions")
)

private val consentExtras = mapOf(
    "1" to ConsentExtra("MD, DM (Cardiology)", 14, "wilson@citygeneral.com", "2024-01-15",
        listOf("Medical Records", "ECG Reports", "Lab Results", "Medication History")),
    "2" to ConsentExtra("MBBS, MD (Dermatology)", 8, "adams@metrohealthclinic.com", "2023-09-01",
        listOf("Medical Records", "Allergy History")),
    "3" to ConsentExtra("MBBS, DM (Neurology)", 18, "chen@specialistcenter.com", "2024-03-20",
        listOf("Medical Records", "MRI Reports", "Neurological Tests", "Prescriptions"))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsentScreen(navController: NavController) {
    val context      = LocalContext.current
    val scope        = rememberCoroutineScope()
    val tokenManager = remember { TokenManager(context) }
    val db           = remember { AppDatabase.getDatabase(context) }
    val blockchainVm: BlockchainViewModel = viewModel()

    val consents = remember { mutableStateListOf<Consent>() }
    var isLoading by remember { mutableStateOf(true) }
    var showAddDoctorDialog by remember { mutableStateOf(false) }
    val chainState by blockchainVm.state.collectAsState()

    LaunchedEffect(Unit) {
        try {
            val token = tokenManager.token.first() ?: return@LaunchedEffect
            val fetched = RetrofitClient.apiService.getConsents("Bearer $token")
            consents.clear()
            consents.addAll(fetched)
        } catch (_: Exception) {
            // consents stays empty on error; empty state will be shown
        } finally {
            isLoading = false
        }
    }

    fun toggleConsent(index: Int) {
        val consent    = consents[index]
        val newStatus  = if (consent.status?.lowercase() == "active") "Revoked" else "Active"
        val actionWord = if (newStatus == "Active") "GRANTED" else "REVOKED"
        consents[index] = consent.copy(status = newStatus)

        scope.launch {
            // Record on blockchain + persist audit
            blockchainVm.recordConsentAction(
                consentId  = consent.id ?: index.toString(),
                doctorName = consent.doctor_name ?: "",
                action     = actionWord
            ) { txHash, hash ->
                scope.launch {
                    db.consentAuditDao().insert(
                        ConsentAuditEntity(
                            id              = System.currentTimeMillis().toString(),
                            consentId       = consent.id ?: index.toString(),
                            doctorName      = consent.doctor_name ?: "",
                            action          = actionWord,
                            timestamp       = LocalDateTime.now().toString().take(19),
                            txHash          = txHash,
                            recordHash      = hash,
                            blockchainStatus = "Simulated"
                        )
                    )
                }
            }

            // Sync with backend
            try {
                val token = tokenManager.token.first() ?: return@launch
                if (newStatus == "Active")
                    RetrofitClient.apiService.grantConsent(
                        "Bearer $token",
                        ConsentGrantRequest(
                            doctorName     = consent.doctor_name ?: "",
                            hospitalName   = consent.hospital_name ?: "",
                            specialization = consent.specialization,
                            expiryDate     = consent.expiry_date ?: "2027-12-31"
                        )
                    )
                else {
                    val consentId = consent.id?.toIntOrNull()
                        ?: throw IllegalStateException("Missing or invalid consent id for revoke")
                    RetrofitClient.apiService.revokeConsent("Bearer $token", ConsentActionRequest(consentId))
                }
                // Refresh list so UI reflects authoritative server state
                val refreshed = RetrofitClient.apiService.getConsents("Bearer $token")
                consents.clear()
                consents.addAll(refreshed)
            } catch (e: Exception) {
                Log.e("ConsentManager", "Grant/revoke failed", e)
                // Undo the optimistic flip so the UI doesn't lie about what the server has
                consents[index] = consent
                Toast.makeText(context, "Failed to update consent: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Consent Manager", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF141824))
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDoctorDialog = true },
                icon = { Icon(Icons.Default.PersonAdd, null) },
                text = { Text("Add Doctor") },
                containerColor = TealPrimary,
                contentColor = Color.White
            )
        },
        containerColor = Color(0xFF0A0E1A)
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = TealPrimary)
            }
            return@Scaffold
        }
        if (consents.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                EmptyState("No Doctors Added", "Tap 'Add Doctor' to grant data access", Icons.Default.PersonRemove)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        "Manage who can access your health data",
                        color = TextSecondaryColor, fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

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

                itemsIndexed(consents) { index, consent ->
                    ExpandableConsentCard(
                        consent    = consent,
                        extra      = consentExtras[consent.id] ?: ConsentExtra(),
                        db         = db,
                        onToggle   = { toggleConsent(index) },
                        onRevokeAll = { if (consent.status?.lowercase() == "active") toggleConsent(index) }
                    )
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }

    if (showAddDoctorDialog) {
        AddDoctorDialog(
            onDismiss = { showAddDoctorDialog = false },
            onAdd = { newConsent ->
                consents.add(newConsent)
                showAddDoctorDialog = false
                // Record initial grant on blockchain
                blockchainVm.recordConsentAction(
                    consentId  = newConsent.id ?: "",
                    doctorName = newConsent.doctor_name ?: "",
                    action     = "GRANTED"
                ) { txHash, hash ->
                    scope.launch {
                        db.consentAuditDao().insert(
                            ConsentAuditEntity(
                                id              = System.currentTimeMillis().toString(),
                                consentId       = newConsent.id ?: "",
                                doctorName      = newConsent.doctor_name ?: "",
                                action          = "GRANTED",
                                timestamp       = LocalDateTime.now().toString().take(19),
                                txHash          = txHash,
                                recordHash      = hash,
                                blockchainStatus = "Simulated"
                            )
                        )
                    }
                }
                scope.launch {
                    try {
                        val token = tokenManager.token.first() ?: return@launch
                        RetrofitClient.apiService.grantConsent(
                            "Bearer $token",
                            ConsentGrantRequest(
                                doctorName     = newConsent.doctor_name ?: "",
                                hospitalName   = newConsent.hospital_name ?: "",
                                specialization = newConsent.specialization,
                                expiryDate     = newConsent.expiry_date ?: "2027-12-31"
                            )
                        )
                        val refreshed = RetrofitClient.apiService.getConsents("Bearer $token")
                        consents.clear()
                        consents.addAll(refreshed)
                    } catch (e: Exception) {
                        Log.e("ConsentManager", "Grant failed", e)
                        // Undo the optimistic add so the UI doesn't lie about what the server has
                        consents.remove(newConsent)
                        Toast.makeText(context, "Failed to grant access: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }
}

@Composable
fun ExpandableConsentCard(
    consent: Consent,
    extra: ConsentExtra,
    db: AppDatabase,
    onToggle: () -> Unit,
    onRevokeAll: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val isActive = consent.status?.lowercase() == "active"
    val accessChecks = remember {
        mutableStateMapOf(*extra.accessTypes.map { it to true }.toTypedArray())
    }

    // Observe audit trail for this consent
    val auditLog by db.consentAuditDao()
        .observeByConsent(consent.id ?: "")
        .collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBgColor, RoundedCornerShape(16.dp))
            .border(
                1.dp,
                if (isActive) TealPrimary.copy(alpha = 0.35f) else BorderColorDim,
                RoundedCornerShape(16.dp)
            )
    ) {
        // Header row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(
                        if (isActive) TealPrimary.copy(alpha = 0.15f) else Color.Gray.copy(alpha = 0.1f),
                        CircleShape
                    )
                    .border(1.dp, if (isActive) TealPrimary.copy(alpha = 0.4f) else BorderColorDim, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    consent.doctor_name?.firstOrNull()?.toString() ?: "D",
                    fontSize = 18.sp, fontWeight = FontWeight.Bold,
                    color = if (isActive) TealPrimary else Color.Gray
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(consent.doctor_name ?: "Unknown Doctor", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Color.White)
                Text(
                    "${consent.specialization ?: "General"} · ${consent.hospital_name ?: "Unknown"}",
                    color = TextSecondaryColor, fontSize = 12.sp
                )
                if (extra.yearsExp > 0)
                    Text("${extra.yearsExp} years experience", color = TextSecondaryColor, fontSize = 11.sp)
                // Show latest tx hash if available
                if (auditLog.isNotEmpty() && auditLog.first().txHash.isNotBlank()) {
                    val tx = auditLog.first().txHash
                    Text(
                        "TX: ${tx.take(10)}…${tx.takeLast(6)}",
                        fontSize = 10.sp, color = TealPrimary,
                        overflow = TextOverflow.Ellipsis, maxLines = 1
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Switch(
                    checked = isActive,
                    onCheckedChange = { onToggle() },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = TealPrimary),
                    modifier = Modifier.height(24.dp)
                )
                Spacer(Modifier.height(4.dp))
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    null, tint = TextSecondaryColor, modifier = Modifier.size(18.dp)
                )
            }
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(spring()) + fadeIn(tween(200)),
            exit = shrinkVertically(tween(200)) + fadeOut(tween(200))
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                HorizontalDivider(color = BorderColorDim, thickness = 0.5.dp)
                Spacer(Modifier.height(10.dp))

                if (extra.qualification.isNotBlank())
                    ConsentDetailRow(Icons.Default.School, "Qualification", extra.qualification)
                ConsentDetailRow(Icons.Default.LocalHospital, "Hospital", consent.hospital_name ?: "")
                if (extra.email.isNotBlank())
                    ConsentDetailRow(Icons.Default.Email, "Contact", extra.email)
                if (extra.grantedDate.isNotBlank())
                    ConsentDetailRow(Icons.Default.CalendarToday, "Access Granted", extra.grantedDate)
                ConsentDetailRow(Icons.Default.AccessTime, "Expires", consent.expiry_date ?: "")

                Spacer(Modifier.height(10.dp))
                Text("Data Access Permissions", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextSecondaryColor)
                Spacer(Modifier.height(6.dp))
                extra.accessTypes.forEach { accessType ->
                    val checked = accessChecks[accessType] ?: true
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { accessChecks[accessType] = !checked }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = checked, onCheckedChange = { accessChecks[accessType] = it },
                            colors = CheckboxDefaults.colors(checkedColor = TealPrimary),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(accessType, fontSize = 13.sp, color = if (checked) Color.White else TextSecondaryColor)
                    }
                }

                // ── Blockchain Audit Trail ─────────────────────────────────
                if (auditLog.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider(color = BorderColorDim, thickness = 0.5.dp)
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Shield, null, tint = TealPrimary, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Blockchain Audit Trail", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TealPrimary)
                    }
                    Spacer(Modifier.height(6.dp))
                    auditLog.take(5).forEach { entry ->
                        AuditTrailRow(entry)
                    }
                }

                Spacer(Modifier.height(12.dp))
                if (isActive) {
                    OutlinedButton(
                        onClick = onRevokeAll,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Block, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Revoke All Access", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun AuditTrailRow(entry: com.secureehr.app.data.local.db.ConsentAuditEntity) {
    val actionColor = if (entry.action == "GRANTED") Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (entry.action == "GRANTED") Icons.Default.CheckCircle else Icons.Default.Cancel,
            null, tint = actionColor, modifier = Modifier.size(12.dp)
        )
        Spacer(Modifier.width(6.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "${entry.action} · ${entry.timestamp.take(10)}",
                fontSize = 11.sp, color = actionColor, fontWeight = FontWeight.SemiBold
            )
            if (entry.txHash.isNotBlank()) {
                Text(
                    "TX: ${entry.txHash.take(10)}…${entry.txHash.takeLast(6)}",
                    fontSize = 10.sp, color = TextSecondaryColor,
                    overflow = TextOverflow.Ellipsis, maxLines = 1
                )
            }
        }
        Box(
            modifier = Modifier
                .background(
                    when (entry.blockchainStatus) {
                        "Confirmed" -> Color(0xFF1B5E20).copy(alpha = 0.5f)
                        "Failed"    -> MaterialTheme.colorScheme.errorContainer
                        "Simulated" -> Color(0xFF37474F)
                        else        -> Color(0xFF37474F)
                    },
                    RoundedCornerShape(4.dp)
                )
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                entry.blockchainStatus,
                fontSize = 9.sp,
                color = when (entry.blockchainStatus) {
                    "Confirmed" -> Color(0xFF4CAF50)
                    "Failed"    -> MaterialTheme.colorScheme.error
                    "Simulated" -> Color(0xFF90A4AE)
                    else        -> TextSecondaryColor
                }
            )
        }
    }
}

@Composable
fun ConsentDetailRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = TextSecondaryColor, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(8.dp))
        Text("$label: ", fontSize = 12.sp, color = TextSecondaryColor, fontWeight = FontWeight.Medium)
        Text(value, fontSize = 12.sp, color = Color.White, modifier = Modifier.weight(1f))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDoctorDialog(onDismiss: () -> Unit, onAdd: (Consent) -> Unit) {
    var doctorName     by remember { mutableStateOf("") }
    var specialization by remember { mutableStateOf("") }
    var hospital       by remember { mutableStateOf("") }
    var email          by remember { mutableStateOf("") }
    var nameError      by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = CardBgColor)) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Grant Doctor Access", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)

                OutlinedTextField(value = doctorName, onValueChange = { doctorName = it; nameError = false },
                    label = { Text("Doctor Name *") }, modifier = Modifier.fillMaxWidth(),
                    isError = nameError, singleLine = true)
                OutlinedTextField(value = specialization, onValueChange = { specialization = it },
                    label = { Text("Specialization") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = hospital, onValueChange = { hospital = it },
                    label = { Text("Hospital") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = email, onValueChange = { email = it },
                    label = { Text("Email") }, modifier = Modifier.fillMaxWidth(), singleLine = true)

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (doctorName.isBlank()) { nameError = true; return@Button }
                            onAdd(Consent(
                                id             = System.currentTimeMillis().toString(),
                                doctor_name    = "Dr. $doctorName",
                                hospital_name  = hospital.ifBlank { "Unknown Hospital" },
                                status         = "Active",
                                expiry_date    = "2026-12-31",
                                specialization = specialization.ifBlank { null }
                            ))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                    ) { Text("Grant Access") }
                }
            }
        }
    }
}
