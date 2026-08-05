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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.secureehr.app.data.api.MedicalRecord
import com.secureehr.app.data.api.RetrofitClient
import com.secureehr.app.data.api.Visit
import com.secureehr.app.data.local.TokenManager
import com.secureehr.app.ui.components.BorderColorDim
import com.secureehr.app.ui.components.CardBgColor
import com.secureehr.app.ui.components.EmptyState
import com.secureehr.app.ui.components.ShimmerRecordCard
import com.secureehr.app.ui.components.TextSecondaryColor
import com.secureehr.app.ui.theme.TealPrimary
import kotlinx.coroutines.flow.first
import retrofit2.HttpException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientDetailScreen(navController: NavController, patientId: Int, patientName: String = "Patient") {
    val decodedName = remember(patientName) {
        try { java.net.URLDecoder.decode(patientName, "UTF-8") } catch (_: Exception) { patientName }
    }
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }

    var records by remember { mutableStateOf<List<MedicalRecord>>(emptyList()) }
    var visits by remember { mutableStateOf<List<Visit>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var accessDenied by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) }
    var retryKey by remember { mutableStateOf(0) }

    LaunchedEffect(patientId, retryKey) {
        isLoading = true
        accessDenied = false
        try {
            val token = tokenManager.token.first() ?: return@LaunchedEffect
            val bearer = "Bearer $token"
            try {
                records = RetrofitClient.apiService.getPatientRecords(bearer, patientId)
            } catch (e: HttpException) {
                if (e.code() == 403) { accessDenied = true; return@LaunchedEffect }
            } catch (_: Exception) {}
            try {
                visits = RetrofitClient.apiService.getPatientVisits(bearer, patientId)
            } catch (e: HttpException) {
                if (e.code() == 403) { accessDenied = true; return@LaunchedEffect }
            } catch (_: Exception) {}
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Patient: $decodedName", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF141824))
            )
        },
        containerColor = Color(0xFF0A0E1A)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Consent banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TealPrimary.copy(alpha = 0.12f))
                    .border(width = 0.dp, color = Color.Transparent, shape = RoundedCornerShape(0.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Shield, null, tint = TealPrimary, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "Viewing with patient consent · Blockchain verified",
                    fontSize = 12.sp,
                    color = TealPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (accessDenied) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .background(CardBgColor, RoundedCornerShape(40.dp))
                                .border(1.dp, BorderColorDim, RoundedCornerShape(40.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Lock, null, tint = TextSecondaryColor.copy(alpha = 0.5f), modifier = Modifier.size(38.dp))
                        }
                        Spacer(Modifier.height(20.dp))
                        Text("Access Denied", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "No active consent from this patient.\nRequest consent to view their records.",
                            fontSize = 13.sp,
                            color = TextSecondaryColor,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(Modifier.height(24.dp))
                        Button(
                            onClick = { retryKey++ },
                            colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                        ) {
                            Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Retry")
                        }
                    }
                }
                return@Scaffold
            }

            // Tab row
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color(0xFF141824),
                contentColor = TealPrimary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = TealPrimary
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            "Medical Records",
                            fontSize = 13.sp,
                            color = if (selectedTab == 0) TealPrimary else TextSecondaryColor
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            "Visits",
                            fontSize = 13.sp,
                            color = if (selectedTab == 1) TealPrimary else TextSecondaryColor
                        )
                    }
                )
            }

            Box(Modifier.weight(1f)) {
                when (selectedTab) {
                    0 -> PatientRecordsTab(records, isLoading)
                    1 -> PatientVisitsTab(visits, isLoading)
                }
            }
        }
    }
}

@Composable
private fun PatientRecordsTab(records: List<MedicalRecord>, isLoading: Boolean) {
    if (isLoading) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(4) { ShimmerRecordCard() }
        }
        return
    }
    if (records.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyState(
                title = "No Records",
                subtitle = "No medical records available for this patient",
                icon = Icons.Default.Description
            )
        }
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(records) { record ->
            ReadOnlyRecordCard(record)
        }
    }
}

@Composable
private fun ReadOnlyRecordCard(record: MedicalRecord) {
    var expanded by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBgColor, RoundedCornerShape(16.dp))
            .border(1.dp, BorderColorDim, RoundedCornerShape(16.dp))
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
                Text(record.title ?: "—", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Color.White)
                Spacer(Modifier.height(2.dp))
                Text("${record.doctor ?: "Unknown"} · ${record.date ?: "—"}", color = TextSecondaryColor, fontSize = 12.sp)
                Text(record.hospital ?: "—", color = TealPrimary, fontSize = 11.sp)
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
                if (!record.diagnosis.isNullOrBlank()) DetailRow("Diagnosis", record.diagnosis)
                if (!record.treatment.isNullOrBlank()) DetailRow("Treatment", record.treatment)
                if (!record.prescription.isNullOrBlank()) DetailRow("Prescription", record.prescription)
                if (!record.notes.isNullOrBlank()) DetailRow("Notes", record.notes ?: "")
                if (!record.type.isNullOrBlank()) DetailRow("Type", record.type ?: "")
            }
        }
    }
}

@Composable
private fun PatientVisitsTab(visits: List<Visit>, isLoading: Boolean) {
    if (isLoading) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(4) { ShimmerRecordCard() }
        }
        return
    }
    if (visits.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyState(
                title = "No Visits",
                subtitle = "No visit history available for this patient",
                icon = Icons.Default.Event
            )
        }
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(visits) { v ->
            VisitCard(
                visit = DetailedVisit(
                    id = v.id?.toString() ?: "",
                    date = v.date ?: "",
                    purpose = v.purpose ?: "",
                    doctor = v.doctor ?: "",
                    hospital = v.hospital ?: "",
                    department = v.department ?: "",
                    doctorSpecialization = v.doctorSpecialization ?: "",
                    status = v.status ?: "",
                    diagnosis = v.diagnosis,
                    treatment = v.treatment,
                    medications = v.medications,
                    followUpDate = v.followUpDate,
                    cost = v.cost,
                    insuranceClaimed = v.insuranceClaimed,
                    notes = v.notes,
                    endDate = v.endDate
                )
            )
        }
    }
}
