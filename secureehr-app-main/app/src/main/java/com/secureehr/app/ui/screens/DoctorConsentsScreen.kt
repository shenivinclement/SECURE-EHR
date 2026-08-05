package com.secureehr.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.secureehr.app.data.api.DoctorConsentView
import com.secureehr.app.data.api.RetrofitClient
import com.secureehr.app.data.local.TokenManager
import com.secureehr.app.ui.components.BorderColorDim
import com.secureehr.app.ui.components.CardBgColor
import com.secureehr.app.ui.components.EmptyState
import com.secureehr.app.ui.components.SectionLabel
import com.secureehr.app.ui.components.ShimmerRecordCard
import com.secureehr.app.ui.components.TextSecondaryColor
import com.secureehr.app.ui.theme.TealPrimary
import kotlinx.coroutines.flow.first

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorConsentsScreen(navController: NavController) {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    var consents by remember { mutableStateOf<List<DoctorConsentView>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            val token = tokenManager.token.first() ?: return@LaunchedEffect
            consents = RetrofitClient.apiService.getDoctorConsents("Bearer $token")
        } catch (e: Exception) {
            error = "Failed to load consents. Please try again."
        } finally {
            isLoading = false
        }
    }

    val active = remember(consents) { consents.filter { it.status?.lowercase() == "active" } }
    val revoked = remember(consents) { consents.filter { it.status?.lowercase() != "active" } }

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
        bottomBar = { DoctorBottomNavigationBar(navController) },
        containerColor = Color(0xFF0A0E1A)
    ) { padding ->
        if (isLoading) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(4) { ShimmerRecordCard() }
            }
            return@Scaffold
        }

        if (error != null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                    Icon(Icons.Default.Error, null, tint = Color(0xFFEF5350), modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(16.dp))
                    Text(error ?: "Unknown error", fontSize = 14.sp, color = TextSecondaryColor, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
            return@Scaffold
        }

        if (consents.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                EmptyState(
                    title = "No Consents",
                    subtitle = "Patient consents granted to you will appear here",
                    icon = Icons.Default.Shield
                )
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { SectionLabel("Active Consents (${active.size})") }

            if (active.isEmpty()) {
                item {
                    Text(
                        "No active consents",
                        fontSize = 13.sp,
                        color = TextSecondaryColor,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                    )
                }
            } else {
                items(active) { consent ->
                    DoctorConsentCard(consent)
                }
            }

            item { Spacer(Modifier.height(8.dp)); SectionLabel("Revoked Consents (${revoked.size})") }

            if (revoked.isEmpty()) {
                item {
                    Text(
                        "No revoked consents",
                        fontSize = 13.sp,
                        color = TextSecondaryColor,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                    )
                }
            } else {
                items(revoked) { consent ->
                    DoctorConsentCard(consent)
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun DoctorConsentCard(consent: DoctorConsentView) {
    val isActive = consent.status?.lowercase() == "active"
    val statusColor = if (isActive) Color(0xFF4CAF50) else Color(0xFF78909C)
    val statusBg = if (isActive) Color(0xFF1B5E20).copy(alpha = 0.35f) else Color(0xFF37474F).copy(alpha = 0.3f)
    val borderColor = if (isActive) TealPrimary.copy(alpha = 0.25f) else BorderColorDim

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBgColor, RoundedCornerShape(16.dp))
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        if (isActive) TealPrimary.copy(alpha = 0.15f) else Color.Gray.copy(alpha = 0.08f),
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Person,
                    null,
                    tint = if (isActive) TealPrimary else TextSecondaryColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(consent.patientName ?: "Unknown", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                if (!consent.hospitalName.isNullOrBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocalHospital, null, tint = TextSecondaryColor, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(consent.hospitalName, fontSize = 12.sp, color = TextSecondaryColor)
                    }
                }
            }
            Box(
                modifier = Modifier
                    .background(statusBg, RoundedCornerShape(8.dp))
                    .border(1.dp, statusColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    (consent.status ?: "Unknown").replaceFirstChar { it.uppercase() },
                    fontSize = 11.sp,
                    color = statusColor,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        if (!consent.specialization.isNullOrBlank() || !consent.expiryDate.isNullOrBlank()) {
            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = BorderColorDim, thickness = 0.5.dp)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                if (!consent.specialization.isNullOrBlank()) {
                    Column {
                        Text("Specialization", fontSize = 10.sp, color = TextSecondaryColor)
                        Text(consent.specialization, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }
                if (!consent.expiryDate.isNullOrBlank()) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Expires", fontSize = 10.sp, color = TextSecondaryColor)
                        Text(
                            consent.expiryDate.take(10),
                            fontSize = 12.sp,
                            color = if (isActive) Color(0xFFFFB74D) else TextSecondaryColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Read-only notice
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier
                .background(Color(0xFF1A1F2E), RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Info, null, tint = TextSecondaryColor.copy(alpha = 0.6f), modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(6.dp))
            Text("Consent status is managed by the patient", fontSize = 11.sp, color = TextSecondaryColor.copy(alpha = 0.7f))
        }
    }
}
