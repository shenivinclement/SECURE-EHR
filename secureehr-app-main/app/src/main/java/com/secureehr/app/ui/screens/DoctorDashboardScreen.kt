package com.secureehr.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.secureehr.app.R
import com.secureehr.app.data.api.RetrofitClient
import com.secureehr.app.data.local.TokenManager
import com.secureehr.app.navigation.Screen
import com.secureehr.app.ui.components.BorderColorDim
import com.secureehr.app.ui.components.CardBgColor
import com.secureehr.app.ui.components.TextSecondaryColor
import com.secureehr.app.ui.theme.CyanAccent
import com.secureehr.app.ui.theme.TealPrimary
import kotlinx.coroutines.flow.first

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorDashboardScreen(navController: NavController) {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }

    var doctorName by remember { mutableStateOf("") }
    var specialization by remember { mutableStateOf("") }
    var activePatients by remember { mutableStateOf(0) }
    var accessibleRecords by remember { mutableStateOf(0) }
    var totalConsents by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            val token = tokenManager.token.first() ?: return@LaunchedEffect
            val bearer = "Bearer $token"
            try {
                val profile = RetrofitClient.apiService.getMyProfile(bearer)
                doctorName = profile.name ?: "Doctor"
            } catch (_: Exception) {}
            try {
                val dash = RetrofitClient.apiService.getDoctorDashboard(bearer)
                activePatients = dash.activePatients
                accessibleRecords = dash.accessibleRecords
                totalConsents = dash.totalConsents
            } catch (_: Exception) {}
        } finally {
            isLoading = false
        }
    }

    val doctorItems = listOf(
        DashboardItem(R.string.my_patients_label, Icons.Default.People, Screen.DoctorPatients.route,
            listOf(Color(0xFF00897B), Color(0xFF004D40))),
        DashboardItem(R.string.search_patients_label, Icons.Default.Search, Screen.DoctorSearch.route,
            listOf(Color(0xFF1E88E5), Color(0xFF0D47A1))),
        DashboardItem(R.string.nav_consent, Icons.Default.Shield, Screen.DoctorConsents.route,
            listOf(Color(0xFF8E24AA), Color(0xFF4A148C))),
        DashboardItem(R.string.ai_chat, Icons.Default.Chat, Screen.AIChat.route,
            listOf(Color(0xFF3949AB), Color(0xFF1A237E)))
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SecureEHR", fontWeight = FontWeight.Bold, color = TealPrimary) },
                actions = {
                    Box(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .size(34.dp)
                            .background(TealPrimary.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                            .border(1.dp, TealPrimary.copy(alpha = 0.3f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.LocalHospital, null, tint = TealPrimary, modifier = Modifier.size(20.dp))
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
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF0D2B27), Color(0xFF0A0E1A)),
                            start = Offset(0f, 0f),
                            end = Offset(0f, Float.POSITIVE_INFINITY)
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(TealPrimary.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                .border(1.dp, TealPrimary.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.LocalHospital, null, tint = TealPrimary, modifier = Modifier.size(22.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Good Day,", fontSize = 13.sp, color = TextSecondaryColor)
                            Text(
                                "Dr. ${if (isLoading && doctorName.isBlank()) "Loading…" else doctorName.ifBlank { "Doctor" }}",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                    if (specialization.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(specialization, fontSize = 13.sp, color = TealPrimary)
                    }
                    Spacer(Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        HealthStatChip(
                            Icons.Default.People,
                            if (isLoading) "…" else "$activePatients",
                            "Patients",
                            Modifier.weight(1f)
                        )
                        HealthStatChip(
                            Icons.Default.Description,
                            if (isLoading) "…" else "$accessibleRecords",
                            "Records",
                            Modifier.weight(1f)
                        )
                        HealthStatChip(
                            Icons.Default.Security,
                            if (isLoading) "…" else "$totalConsents",
                            "Consents",
                            Modifier.weight(1f)
                        )
                    }
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(doctorItems) { item ->
                    DashboardCard(item) { navController.navigate(item.route) }
                }
            }
        }
    }
}

@Composable
fun DoctorBottomNavigationBar(navController: NavController) {
    val currentRoute = navController.currentBackStackEntry?.destination?.route
    NavigationBar(
        containerColor = Color(0xFF141824),
        tonalElevation = 0.dp,
        modifier = Modifier.border(
            width = 0.5.dp,
            color = BorderColorDim,
            shape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp)
        )
    ) {
        listOf(
            Triple(Icons.Default.Home, "Home", Screen.DoctorDashboard.route),
            Triple(Icons.Default.People, "Patients", Screen.DoctorPatients.route),
            Triple(Icons.Default.Search, "Search", Screen.DoctorSearch.route),
            Triple(Icons.Default.Settings, "Settings", Screen.Settings.route)
        ).forEach { (icon, label, route) ->
            NavigationBarItem(
                icon = {
                    Box(
                        modifier = if (currentRoute == route)
                            Modifier
                                .background(TealPrimary.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        else Modifier,
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, contentDescription = label, modifier = Modifier.size(22.dp))
                    }
                },
                label = { Text(label, fontSize = 11.sp) },
                selected = currentRoute == route,
                onClick = { navController.navigate(route) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = TealPrimary,
                    selectedTextColor = TealPrimary,
                    unselectedIconColor = TextSecondaryColor,
                    unselectedTextColor = TextSecondaryColor,
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}
