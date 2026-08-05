package com.secureehr.app.ui.screens

import androidx.compose.animation.core.*
import kotlinx.coroutines.delay
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.annotation.StringRes
import androidx.navigation.NavController
import com.secureehr.app.R
import com.secureehr.app.data.api.RetrofitClient
import com.secureehr.app.data.local.TokenManager
import com.secureehr.app.navigation.Screen
import kotlinx.coroutines.flow.first
import com.secureehr.app.ui.theme.TealPrimary

data class DashboardItem(@StringRes val titleRes: Int, val icon: ImageVector, val route: String, val gradient: List<Color>)

val dashboardItems = listOf(
    DashboardItem(R.string.nav_medical_records, Icons.Default.Description, Screen.Records.route,
        listOf(Color(0xFF00897B), Color(0xFF004D40))),
    DashboardItem(R.string.nav_hospital_finder, Icons.Default.LocalHospital, Screen.HospitalFinder.route,
        listOf(Color(0xFF1E88E5), Color(0xFF0D47A1))),
    DashboardItem(R.string.ai_chat, Icons.Default.Chat, Screen.AIChat.route,
        listOf(Color(0xFF8E24AA), Color(0xFF4A148C))),
    DashboardItem(R.string.visits_label, Icons.Default.Event, Screen.Visits.route,
        listOf(Color(0xFF3949AB), Color(0xFF1A237E))),
    DashboardItem(R.string.nav_consent, Icons.Default.Security, Screen.Consent.route,
        listOf(Color(0xFF00ACC1), Color(0xFF006064)))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(navController: NavController) {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }

    var userName by remember { mutableStateOf("") }
    var recordCount by remember { mutableStateOf<Int?>(null) }
    var visitCount by remember { mutableStateOf<Int?>(null) }
    var activeConsentCount by remember { mutableStateOf<Int?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    val defaultUser = stringResource(R.string.default_user)
    val loadingText = stringResource(R.string.loading_ellipsis)

    LaunchedEffect(Unit) {
        try {
            val token = tokenManager.token.first() ?: return@LaunchedEffect
            val bearer = "Bearer $token"
            try { userName = RetrofitClient.apiService.getMyProfile(bearer).name ?: defaultUser } catch (_: Exception) {}
            try { recordCount = RetrofitClient.apiService.getRecords(bearer).size } catch (_: Exception) {}
            try { visitCount = RetrofitClient.apiService.getVisits(bearer).size } catch (_: Exception) {}
            try {
                activeConsentCount = RetrofitClient.apiService.getConsents(bearer)
                    .count { it.status?.lowercase() == "active" }
            } catch (_: Exception) {}
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name), fontWeight = FontWeight.Bold, color = TealPrimary) },
                actions = {
                    IconButton(onClick = { navController.navigate(Screen.Profile.route) }) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(TealPrimary.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                                .border(1.dp, TealPrimary.copy(alpha = 0.3f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Person, null, tint = TealPrimary, modifier = Modifier.size(20.dp))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = { AppBottomNavigationBar(navController) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ── Gradient header ──────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(TealPrimary.copy(alpha = 0.12f), MaterialTheme.colorScheme.background),
                            start = Offset(0f, 0f),
                            end = Offset(0f, Float.POSITIVE_INFINITY)
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                Column {
                    Text(
                        stringResource(R.string.good_day),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        if (isLoading && userName.isBlank()) loadingText else userName.ifBlank { defaultUser },
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(Modifier.height(16.dp))

                    // Health summary strip
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        HealthStatChip(
                            Icons.Default.Description,
                            if (isLoading) "…" else "${recordCount ?: 0}",
                            stringResource(R.string.records_label),
                            Modifier.weight(1f)
                        )
                        HealthStatChip(
                            Icons.Default.Event,
                            if (isLoading) "…" else "${visitCount ?: 0}",
                            stringResource(R.string.visits_label),
                            Modifier.weight(1f)
                        )
                        HealthStatChip(
                            Icons.Default.Security,
                            if (isLoading) "…" else "${activeConsentCount ?: 0}",
                            stringResource(R.string.consents_label),
                            Modifier.weight(1f)
                        )
                    }
                }
            }

            // ── Feature grid ─────────────────────────────────────────────
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(dashboardItems) { item ->
                    DashboardCard(item) { navController.navigate(item.route) }
                }
            }
        }
    }
}

@Composable
fun HealthStatChip(icon: ImageVector, value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, null, tint = TealPrimary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.height(4.dp))
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun DashboardCard(item: DashboardItem, onClick: () -> Unit) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "cardScale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .scale(scale)
            .background(
                Brush.linearGradient(
                    colors = item.gradient,
                    start = Offset(0f, 0f),
                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                ),
                RoundedCornerShape(18.dp)
            )
            .clickable {
                pressed = true
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        val title = stringResource(item.titleRes)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    item.icon,
                    contentDescription = title,
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = Color.White
            )
        }
    }
    LaunchedEffect(pressed) {
        if (pressed) {
            delay(150)
            pressed = false
        }
    }
}

@Composable
fun AppBottomNavigationBar(navController: NavController) {
    val currentRoute = navController.currentBackStackEntry?.destination?.route
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        modifier = Modifier.border(
            width = 0.5.dp,
            color = MaterialTheme.colorScheme.outline,
            shape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp)
        )
    ) {
        listOf(
            Triple(Icons.Default.Home, stringResource(R.string.home_label), Screen.Dashboard.route),
            Triple(Icons.Default.Chat, stringResource(R.string.ai_chat), Screen.AIChat.route),
            Triple(Icons.Default.History, stringResource(R.string.history_label), Screen.HospitalHistory.route),
            Triple(Icons.Default.Settings, stringResource(R.string.nav_settings), Screen.Settings.route)
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
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}
