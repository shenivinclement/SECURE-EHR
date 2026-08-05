package com.secureehr.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.secureehr.app.data.api.RetrofitClient
import com.secureehr.app.data.local.TokenManager
import kotlinx.coroutines.flow.first
import com.secureehr.app.ui.components.BorderColorDim
import com.secureehr.app.ui.components.CardBgColor
import com.secureehr.app.ui.components.EmptyState
import com.secureehr.app.ui.components.SectionLabel
import com.secureehr.app.ui.components.TextSecondaryColor
import com.secureehr.app.ui.theme.CyanAccent
import com.secureehr.app.ui.theme.TealPrimary

data class DetailedVisit(
    val id: String,
    val date: String,
    val purpose: String,
    val doctor: String,
    val hospital: String,
    val department: String,
    val doctorSpecialization: String,
    val status: String,
    val diagnosis: String? = null,
    val treatment: String? = null,
    val medications: String? = null,
    val followUpDate: String? = null,
    val cost: String? = null,
    val insuranceClaimed: String? = null,
    val notes: String? = null,
    val endDate: String? = null
)


private fun isUpcoming(visit: DetailedVisit): Boolean {
    val today = "2026-05-11"
    return visit.date.take(10) >= today || visit.status == "Upcoming"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisitsScreen(navController: NavController) {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    var visits by remember { mutableStateOf<List<DetailedVisit>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            val token = tokenManager.token.first()
            if (token != null) {
                val apiVisits = RetrofitClient.apiService.getVisits("Bearer $token")
                visits = apiVisits.map { v ->
                    DetailedVisit(
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
                }
            }
        } catch (_: Exception) {
            // visits stays empty on error
        } finally {
            isLoading = false
        }
    }

    val upcoming = remember(visits) { visits.filter { isUpcoming(it) } }
    val past = remember(visits) { visits.filter { !isUpcoming(it) } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Visits", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF141824))
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

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { SectionLabel("Upcoming Visits") }

            if (upcoming.isEmpty()) {
                item {
                    EmptyState(
                        title = "No Upcoming Visits",
                        subtitle = "Your scheduled appointments will appear here",
                        icon = Icons.Default.EventAvailable
                    )
                }
            } else {
                items(upcoming, key = { it.id }) { VisitCard(it) }
            }

            item { Spacer(Modifier.height(8.dp)); SectionLabel("Past Visits") }

            if (past.isEmpty()) {
                item {
                    EmptyState(
                        title = "No Past Visits",
                        subtitle = "Completed visits will be archived here",
                        icon = Icons.Default.EventNote
                    )
                }
            } else {
                items(past, key = { it.id }) { VisitCard(it) }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
fun VisitCard(visit: DetailedVisit) {
    var expanded by remember { mutableStateOf(false) }
    val isUpcoming = isUpcoming(visit)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBgColor, RoundedCornerShape(16.dp))
            .border(
                1.dp,
                if (isUpcoming) TealPrimary.copy(alpha = 0.4f) else BorderColorDim,
                RoundedCornerShape(16.dp)
            )
            .clickable { expanded = !expanded }
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        if (isUpcoming) TealPrimary.copy(alpha = 0.15f)
                        else Color.Gray.copy(alpha = 0.1f),
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isUpcoming) Icons.Default.Event else Icons.Default.CheckCircle,
                    null,
                    tint = if (isUpcoming) TealPrimary else Color.Gray,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(visit.purpose, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Color.White)
                Text("${visit.doctor} · ${visit.department}", color = TextSecondaryColor, fontSize = 12.sp)
                Text(visit.hospital, color = if (isUpcoming) TealPrimary else TextSecondaryColor, fontSize = 11.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Box(
                    modifier = Modifier
                        .background(
                            if (isUpcoming) TealPrimary.copy(alpha = 0.15f)
                            else Color.Gray.copy(alpha = 0.1f),
                            RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        if (isUpcoming) "Upcoming" else "Completed",
                        fontSize = 10.sp,
                        color = if (isUpcoming) TealPrimary else Color.Gray,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(visit.date.take(10), fontSize = 10.sp, color = TextSecondaryColor)
                Spacer(Modifier.height(2.dp))
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
            Column(modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp)) {
                HorizontalDivider(color = BorderColorDim, thickness = 0.5.dp)
                Spacer(Modifier.height(10.dp))

                VisitDetailRow(Icons.Default.LocalHospital, "Hospital", visit.hospital)
                VisitDetailRow(Icons.Default.MedicalServices, "Department", visit.department)
                VisitDetailRow(Icons.Default.Person, "Doctor", "${visit.doctor} (${visit.doctorSpecialization})")
                if (!visit.diagnosis.isNullOrBlank())
                    VisitDetailRow(Icons.Default.LocalHospital, "Diagnosis", visit.diagnosis)
                if (!visit.treatment.isNullOrBlank())
                    VisitDetailRow(Icons.Default.HealthAndSafety, "Treatment", visit.treatment)
                if (!visit.medications.isNullOrBlank())
                    VisitDetailRow(Icons.Default.Medication, "Medications", visit.medications)
                if (!visit.followUpDate.isNullOrBlank())
                    VisitDetailRow(Icons.Default.EventRepeat, "Follow-up", visit.followUpDate)
                if (!visit.notes.isNullOrBlank())
                    VisitDetailRow(Icons.Default.Notes, "Notes", visit.notes)

                Spacer(Modifier.height(8.dp))
                // Cost row
                Row(Modifier.fillMaxWidth()) {
                    if (!visit.cost.isNullOrBlank()) {
                        CostChip("Cost", visit.cost, Color(0xFF37474F))
                        Spacer(Modifier.width(8.dp))
                    }
                    if (!visit.insuranceClaimed.isNullOrBlank()) {
                        CostChip("Insured", visit.insuranceClaimed, TealPrimary.copy(alpha = 0.3f))
                    }
                }
            }
        }
    }
}

@Composable
fun VisitDetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(icon, null, tint = TextSecondaryColor, modifier = Modifier.size(14.dp).padding(top = 2.dp))
        Spacer(Modifier.width(6.dp))
        Text("$label: ", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextSecondaryColor)
        Text(value, fontSize = 12.sp, color = Color.White, modifier = Modifier.weight(1f))
    }
}

@Composable
fun CostChip(label: String, value: String, bgColor: Color) {
    Row(
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 10.sp, color = TextSecondaryColor)
        Spacer(Modifier.width(4.dp))
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}
