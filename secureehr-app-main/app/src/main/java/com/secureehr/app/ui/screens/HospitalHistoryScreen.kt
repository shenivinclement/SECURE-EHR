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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.secureehr.app.data.api.RetrofitClient
import com.secureehr.app.data.local.TokenManager
import com.secureehr.app.ui.components.BorderColorDim
import com.secureehr.app.ui.components.CardBgColor
import com.secureehr.app.ui.components.EmptyState
import com.secureehr.app.ui.components.TextSecondaryColor
import com.secureehr.app.ui.theme.TealPrimary
import kotlinx.coroutines.flow.first

data class HospitalTreatment(
    val hospital: String,
    val date: String,
    val diagnosis: String,
    val treatment: String,
    val doctor: String,
    val duration: String,
    val outcome: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HospitalHistoryScreen(navController: NavController) {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    var treatments by remember { mutableStateOf<List<HospitalTreatment>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            val token = tokenManager.token.first()
            if (token != null) {
                val bearer = "Bearer $token"

                val fromVisits = try {
                    RetrofitClient.apiService.getVisits(bearer).map { v ->
                        HospitalTreatment(
                            hospital = (v.hospital ?: "").ifBlank { "Unknown Hospital" },
                            date = (v.date ?: "").take(10),
                            diagnosis = (v.purpose ?: "").ifBlank { v.diagnosis ?: "Visit" },
                            treatment = v.treatment ?: "—",
                            doctor = v.doctor ?: "",
                            duration = if (!v.endDate.isNullOrBlank())
                                "${(v.date ?: "").take(10)} – ${v.endDate.take(10)}" else "—",
                            outcome = when ((v.status ?: "").lowercase()) {
                                "completed" -> "Recovered"
                                else -> "Under Treatment"
                            }
                        )
                    }
                } catch (_: Exception) { emptyList() }

                val fromRecords = try {
                    RetrofitClient.apiService.getRecords(bearer).map { r ->
                        HospitalTreatment(
                            hospital = (r.hospital ?: "").ifBlank { "Unknown Hospital" },
                            date = (r.date ?: "").take(10),
                            diagnosis = r.title ?: "",
                            treatment = r.treatment ?: "—",
                            doctor = r.doctor ?: "",
                            duration = "—",
                            outcome = if ((r.notes ?: "").contains("Normal", ignoreCase = true) ||
                                r.diagnosis?.contains("Normal", ignoreCase = true) == true)
                                "Fully Recovered" else "Under Treatment"
                        )
                    }
                } catch (_: Exception) { emptyList() }

                treatments = (fromVisits + fromRecords).sortedByDescending { it.date }
            }
        } catch (_: Exception) {
            // treatments stays empty on error
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Treatment History", fontWeight = FontWeight.Bold, color = Color.White) },
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
        if (isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = TealPrimary)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    "Your complete hospital treatment history",
                    color = TextSecondaryColor,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            if (treatments.isEmpty()) {
                item {
                    EmptyState(
                        title = "No Treatment History",
                        subtitle = "Your hospital visits and treatment records will appear here",
                        icon = Icons.Default.LocalHospital
                    )
                }
            } else {
                items(treatments) { treatment ->
                    HospitalTreatmentCard(treatment)
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
fun HospitalTreatmentCard(treatment: HospitalTreatment) {
    var expanded by remember { mutableStateOf(false) }
    val isRecovered = treatment.outcome.contains("Recover", ignoreCase = true)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBgColor, RoundedCornerShape(16.dp))
            .border(1.dp, BorderColorDim, RoundedCornerShape(16.dp))
            .clickable { expanded = !expanded }
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(TealPrimary.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.LocalHospital, null, tint = TealPrimary, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(treatment.hospital, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Color.White)
                Text(treatment.diagnosis, color = TextSecondaryColor, fontSize = 12.sp)
                Text(treatment.date, fontSize = 11.sp, color = TealPrimary)
            }
            Column(horizontalAlignment = Alignment.End) {
                Box(
                    modifier = Modifier
                        .background(
                            if (isRecovered) Color(0xFF1B5E20).copy(alpha = 0.5f)
                            else Color.Gray.copy(alpha = 0.15f),
                            RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        treatment.outcome,
                        fontSize = 10.sp,
                        color = if (isRecovered) Color(0xFF4CAF50) else TextSecondaryColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }
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
            Column(modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp)) {
                HorizontalDivider(color = BorderColorDim, thickness = 0.5.dp)
                Spacer(Modifier.height(10.dp))
                HistoryDetailRow(Icons.Default.Person, "Doctor", treatment.doctor)
                HistoryDetailRow(Icons.Default.HealthAndSafety, "Treatment", treatment.treatment)
                HistoryDetailRow(Icons.Default.Schedule, "Duration", treatment.duration)
                HistoryDetailRow(Icons.Default.CheckCircle, "Outcome", treatment.outcome)
            }
        }
    }
}

@Composable
fun HistoryDetailRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
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
