package com.secureehr.app.ui.screens

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
import com.secureehr.app.data.api.DoctorPatient
import com.secureehr.app.data.api.RetrofitClient
import com.secureehr.app.data.local.TokenManager
import com.secureehr.app.navigation.Screen
import com.secureehr.app.ui.components.BorderColorDim
import com.secureehr.app.ui.components.CardBgColor
import com.secureehr.app.ui.components.EmptyState
import com.secureehr.app.ui.components.ShimmerRecordCard
import com.secureehr.app.ui.components.TextSecondaryColor
import com.secureehr.app.ui.theme.TealPrimary
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorPatientsScreen(navController: NavController) {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    var patients by remember { mutableStateOf<List<DoctorPatient>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            val token = tokenManager.token.first() ?: return@LaunchedEffect
            patients = RetrofitClient.apiService.getDoctorPatients("Bearer $token")
        } catch (e: Exception) {
            errorMessage = e.message?.take(120) ?: "Failed to load patients"
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Patients", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF141824))
            )
        },
        bottomBar = { DoctorBottomNavigationBar(navController) },
        containerColor = Color(0xFF0A0E1A)
    ) { padding ->
        if (isLoading) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(5) { ShimmerRecordCard() }
            }
            return@Scaffold
        }

        if (errorMessage != null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                EmptyState(
                    title = "Couldn't Load Patients",
                    subtitle = errorMessage ?: "An error occurred. Please try again.",
                    icon = Icons.Default.Warning
                )
            }
            return@Scaffold
        }

        if (patients.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                EmptyState(
                    title = "No Patients Yet",
                    subtitle = "Patients who have granted you consent will appear here",
                    icon = Icons.Default.People
                )
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(patients) { patient ->
                DoctorPatientCard(patient) {
                    navController.navigate(Screen.PatientDetail.route(patient.id ?: 0, patient.name ?: "Patient"))
                }
            }
        }
    }
}

@Composable
fun DoctorPatientCard(patient: DoctorPatient, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBgColor, RoundedCornerShape(16.dp))
            .border(1.dp, TealPrimary.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(TealPrimary.copy(alpha = 0.15f), RoundedCornerShape(13.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, null, tint = TealPrimary, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(patient.name ?: "Unknown Patient", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (!patient.gender.isNullOrBlank()) {
                        Text(patient.gender, fontSize = 12.sp, color = TextSecondaryColor)
                        Text("·", fontSize = 12.sp, color = TextSecondaryColor)
                    }
                    if (!patient.bloodGroup.isNullOrBlank()) {
                        Text(patient.bloodGroup, fontSize = 12.sp, color = TextSecondaryColor)
                        Text("·", fontSize = 12.sp, color = TextSecondaryColor)
                    }
                    val age = calculateAge(patient.dateOfBirth)
                    if (age != null) Text("$age yrs", fontSize = 12.sp, color = TextSecondaryColor)
                }
            }
            // Active consent badge
            Box(
                modifier = Modifier
                    .background(Color(0xFF1B5E20).copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0xFF4CAF50).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text("Active", fontSize = 11.sp, color = Color(0xFF4CAF50), fontWeight = FontWeight.SemiBold)
            }
        }

        if (!patient.primaryCondition.isNullOrBlank()) {
            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .background(TealPrimary.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                    .border(1.dp, TealPrimary.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(patient.primaryCondition, fontSize = 12.sp, color = TealPrimary, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(Modifier.height(10.dp))
        HorizontalDivider(color = BorderColorDim, thickness = 0.5.dp)
        Spacer(Modifier.height(10.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                if (!patient.hospitalName.isNullOrBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocalHospital, null, tint = TextSecondaryColor, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(patient.hospitalName, fontSize = 12.sp, color = TextSecondaryColor)
                    }
                    Spacer(Modifier.height(4.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Description, null, tint = TextSecondaryColor, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("${patient.recordCount ?: 0} medical records", fontSize = 12.sp, color = TextSecondaryColor)
                }
            }
            if (!patient.expiryDate.isNullOrBlank()) {
                Column(horizontalAlignment = Alignment.End) {
                    Text("Consent expires", fontSize = 10.sp, color = TextSecondaryColor)
                    Text(patient.expiryDate.take(10), fontSize = 11.sp, color = Color(0xFFFFB74D), fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

private fun calculateAge(dob: String?): Int? {
    if (dob.isNullOrBlank()) return null
    return try {
        val birthDate = LocalDate.parse(dob.take(10), DateTimeFormatter.ISO_LOCAL_DATE)
        Period.between(birthDate, LocalDate.now()).years
    } catch (_: Exception) { null }
}
