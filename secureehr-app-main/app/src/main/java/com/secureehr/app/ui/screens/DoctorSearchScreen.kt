package com.secureehr.app.ui.screens

import android.util.Log
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
import com.secureehr.app.data.api.PatientSearchResult
import com.secureehr.app.data.api.RetrofitClient
import com.secureehr.app.data.local.TokenManager
import com.secureehr.app.navigation.Screen
import com.secureehr.app.ui.components.BorderColorDim
import com.secureehr.app.ui.components.CardBgColor
import com.secureehr.app.ui.components.TextSecondaryColor
import com.secureehr.app.ui.theme.TealPrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorSearchScreen(navController: NavController) {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<PatientSearchResult>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var hasSearched by remember { mutableStateOf(false) }
    var noConsentMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(query) {
        if (query.length < 2) {
            results = emptyList()
            hasSearched = false
            return@LaunchedEffect
        }
        delay(400) // debounce
        isSearching = true
        hasSearched = true
        try {
            val token = tokenManager.token.first() ?: return@LaunchedEffect
            results = RetrofitClient.apiService.searchPatients("Bearer $token", query)
            results.forEach { result ->
                Log.d("DoctorSearch", "Result: $result, hasConsent=${result.hasConsent}")
            }
        } catch (_: Exception) {
            results = emptyList()
        } finally {
            isSearching = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Search Patients", fontWeight = FontWeight.Bold, color = Color.White) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it; noConsentMessage = null },
                placeholder = { Text("Search patients by name…", color = TextSecondaryColor) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = TealPrimary) },
                trailingIcon = {
                    if (isSearching) CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = TealPrimary,
                        strokeWidth = 2.dp
                    )
                    else if (query.isNotEmpty()) {
                        IconButton(onClick = { query = ""; results = emptyList(); hasSearched = false }) {
                            Icon(Icons.Default.Clear, null, tint = TextSecondaryColor)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = CardBgColor,
                    unfocusedContainerColor = CardBgColor,
                    focusedBorderColor = TealPrimary,
                    unfocusedBorderColor = BorderColorDim
                )
            )

            if (noConsentMessage != null) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF37474F).copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        .border(1.dp, BorderColorDim, RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Lock, null, tint = TextSecondaryColor, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(noConsentMessage!!, fontSize = 13.sp, color = TextSecondaryColor)
                }
            }

            Spacer(Modifier.height(12.dp))

            if (!hasSearched || query.length < 2) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Search, null, tint = TextSecondaryColor.copy(alpha = 0.3f), modifier = Modifier.size(64.dp))
                        Spacer(Modifier.height(16.dp))
                        Text("Search for patients by name", fontSize = 15.sp, color = TextSecondaryColor)
                        Text("Type at least 2 characters", fontSize = 12.sp, color = TextSecondaryColor.copy(alpha = 0.6f))
                    }
                }
            } else if (results.isEmpty() && !isSearching) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Person, null, tint = TextSecondaryColor.copy(alpha = 0.3f), modifier = Modifier.size(64.dp))
                        Spacer(Modifier.height(16.dp))
                        Text("No patients found", fontSize = 15.sp, color = TextSecondaryColor)
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(results) { patient ->
                        PatientSearchResultCard(
                            patient = patient,
                            onClick = {
                                if (patient.hasConsent == true) {
                                    patient.id?.let { navController.navigate(Screen.PatientDetail.route(it, patient.name ?: "Patient")) }
                                } else {
                                    noConsentMessage = "Request consent from ${patient.name ?: "this patient"} to view their records"
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PatientSearchResultCard(patient: PatientSearchResult, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBgColor, RoundedCornerShape(14.dp))
            .border(
                1.dp,
                if (patient.hasConsent == true) TealPrimary.copy(alpha = 0.3f) else BorderColorDim,
                RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(
                    if (patient.hasConsent == true) TealPrimary.copy(alpha = 0.15f)
                    else Color.Gray.copy(alpha = 0.1f),
                    RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Person,
                null,
                tint = if (patient.hasConsent == true) TealPrimary else TextSecondaryColor,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(patient.name ?: "Unknown", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Color.White)
            if (!patient.gender.isNullOrBlank()) {
                Text(patient.gender ?: "", fontSize = 12.sp, color = TextSecondaryColor)
            }
        }
        if (patient.hasConsent == true) {
            Row(
                modifier = Modifier
                    .background(Color(0xFF1B5E20).copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0xFF4CAF50).copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Shield, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(4.dp))
                Text("Consented", fontSize = 11.sp, color = Color(0xFF4CAF50), fontWeight = FontWeight.SemiBold)
            }
        } else {
            Row(
                modifier = Modifier
                    .background(Color(0xFF37474F).copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .border(1.dp, BorderColorDim, RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Lock, null, tint = TextSecondaryColor, modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(4.dp))
                Text("No Consent", fontSize = 11.sp, color = TextSecondaryColor, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
