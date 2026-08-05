package com.secureehr.app.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.secureehr.app.data.api.Hospital
import com.secureehr.app.data.api.RetrofitClient
import com.secureehr.app.data.local.TokenManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.secureehr.app.ui.components.BorderColorDim
import com.secureehr.app.ui.components.CardBgColor
import com.secureehr.app.ui.components.EmptyState
import com.secureehr.app.ui.components.TextSecondaryColor
import com.secureehr.app.ui.theme.TealPrimary
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import kotlin.math.pow
import kotlin.math.sqrt

private val CONDITIONS = listOf("Diabetes", "Hypertension", "Asthma", "Arthritis", "Cancer", "Obesity")

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HospitalFinderScreen(navController: NavController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }
    var userLat by remember { mutableStateOf(13.0827) }
    var userLon by remember { mutableStateOf(80.2707) }
    var locationGranted by remember { mutableStateOf(false) }
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }
    var hospitals by remember { mutableStateOf<List<Hospital>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    // null = "All" (no condition filter); set to detected primary condition by default
    var selectedCondition by remember { mutableStateOf<String?>(null) }
    val tokenManager = remember { TokenManager(context) }

    val rankedHospitals = remember(hospitals) { hospitals }
    val filteredHospitals = remember(searchQuery, rankedHospitals) {
        if (searchQuery.isBlank()) rankedHospitals
        else rankedHospitals.filter {
            it.name.contains(searchQuery, true) || it.specialty.contains(searchQuery, true) ||
            it.specializations.any { s -> s.contains(searchQuery, true) }
        }
    }

    val locationPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        locationGranted = granted
        if (granted) {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            if (loc != null) {
                userLat = loc.latitude; userLon = loc.longitude
                mapViewRef?.controller?.setCenter(GeoPoint(userLat, userLon))
            }
        }
    }

    LaunchedEffect(Unit) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            locationGranted = true
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            if (loc != null) { userLat = loc.latitude; userLon = loc.longitude }
        } else {
            locationPermLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    // On location change: detect primary condition from records, pre-select it, then fetch hospitals.
    LaunchedEffect(userLat, userLon) {
        isLoading = true
        errorMessage = null
        try {
            val token = tokenManager.token.first()
            if (token != null) {
                var detectedCondition: String? = null
                try {
                    val records = RetrofitClient.apiService.getRecords("Bearer $token")
                    detectedCondition = records
                        .mapNotNull { it.title }
                        .filter { it.isNotBlank() && it != "Medical Record" }
                        .groupBy { it }
                        .maxByOrNull { it.value.size }
                        ?.key
                } catch (_: Exception) {
                    // Records unavailable — default to "All"
                }
                selectedCondition = detectedCondition
                Log.d("HospitalFinder", "Fetching hospitals for condition: $detectedCondition")
                hospitals = RetrofitClient.apiService.getHospitals(
                    token = "Bearer $token",
                    lat = userLat,
                    lon = userLon,
                    condition = detectedCondition
                )
            } else {
                errorMessage = "Authentication token not found."
            }
        } catch (e: Exception) {
            hospitals = emptyList()
            errorMessage = "Please check your connection and try again."
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hospital Finder", fontWeight = FontWeight.Bold) },
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
        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            // Search bar
            OutlinedTextField(
                value = searchQuery, onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search hospitals or specialties…") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (searchQuery.isNotBlank())
                        IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Clear, null) }
                },
                shape = RoundedCornerShape(14.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = CardBgColor,
                    unfocusedContainerColor = CardBgColor,
                    focusedBorderColor = TealPrimary,
                    unfocusedBorderColor = BorderColorDim
                )
            )

            // Disease filter chips — horizontally scrollable, teal when selected
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                // "All" chip
                item {
                    val isSelected = selectedCondition == null
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            if (!isSelected) {
                                selectedCondition = null
                                isLoading = true
                                coroutineScope.launch {
                                    errorMessage = null
                                    try {
                                        val token = tokenManager.token.first() ?: run {
                                            isLoading = false
                                            return@launch
                                        }
                                        hospitals = RetrofitClient.apiService.getHospitals(
                                            token = "Bearer $token",
                                            lat = userLat,
                                            lon = userLon,
                                            condition = null
                                        )
                                    } catch (e: Exception) {
                                        hospitals = emptyList()
                                        errorMessage = "Please check your connection and try again."
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            }
                        },
                        label = { Text("All", fontSize = 13.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TealPrimary,
                            selectedLabelColor = Color.White,
                            containerColor = CardBgColor,
                            labelColor = TextSecondaryColor
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = BorderColorDim,
                            selectedBorderColor = TealPrimary
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
                }

                // Condition chips
                items(CONDITIONS) { condition ->
                    val isSelected = selectedCondition == condition
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            if (!isSelected) {
                                selectedCondition = condition
                                isLoading = true
                                coroutineScope.launch {
                                    errorMessage = null
                                    try {
                                        val token = tokenManager.token.first() ?: run {
                                            isLoading = false
                                            return@launch
                                        }
                                        hospitals = RetrofitClient.apiService.getHospitals(
                                            token = "Bearer $token",
                                            lat = userLat,
                                            lon = userLon,
                                            condition = condition
                                        )
                                    } catch (e: Exception) {
                                        hospitals = emptyList()
                                        errorMessage = "Please check your connection and try again."
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            }
                        },
                        label = { Text(condition, fontSize = 13.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TealPrimary,
                            selectedLabelColor = Color.White,
                            containerColor = CardBgColor,
                            labelColor = TextSecondaryColor
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = BorderColorDim,
                            selectedBorderColor = TealPrimary
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
                }
            }

            if (!locationGranted) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .background(Color(0xFF8B0000).copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.LocationOff, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Enable location for nearby hospitals", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                }
            }

            // Map — fixed height with rounded corners
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
            ) {
                AndroidView(
                    factory = { ctx ->
                        Configuration.getInstance().userAgentValue = ctx.packageName
                        MapView(ctx).apply {
                            setTileSource(TileSourceFactory.MAPNIK)
                            setMultiTouchControls(true)
                            controller.setZoom(13.0)
                            controller.setCenter(GeoPoint(userLat, userLon))
                            mapViewRef = this
                            invalidate()
                        }
                    },
                    update = { mv ->
                        mv.overlays.clear()
                        rankedHospitals.forEach { h ->
                            val m = Marker(mv)
                            m.position = GeoPoint(h.latitude, h.longitude)
                            m.title = h.name
                            m.snippet = "Cure Rate: ${h.cureRate}%"
                            m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            mv.overlays.add(m)
                        }
                        val userM = Marker(mv)
                        userM.position = GeoPoint(userLat, userLon)
                        userM.title = "Your Location"
                        mv.overlays.add(userM)
                        mv.controller.setCenter(GeoPoint(userLat, userLon))
                        mv.invalidate()
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = BorderColorDim,
                thickness = 1.dp
            )

            // AI ranking header — label reflects the active chip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.AutoAwesome, null, tint = TealPrimary, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    if (selectedCondition != null) "AI-Ranked for $selectedCondition"
                    else "AI-Ranked by Overall Cure Rate",
                    fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TealPrimary
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "${filteredHospitals.size} hospitals",
                    fontSize = 11.sp, color = TextSecondaryColor
                )
            }

            // Hospital list
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = TealPrimary)
                    }
                }
                errorMessage != null -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        EmptyState(
                            title = "Could Not Load Hospitals",
                            subtitle = errorMessage!!,
                            icon = Icons.Default.WifiOff
                        )
                    }
                }
                filteredHospitals.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        EmptyState(
                            title = if (searchQuery.isBlank()) "No Hospitals Found" else "No Results",
                            subtitle = if (searchQuery.isBlank())
                                "No hospitals are available in your area."
                            else
                                "No hospitals match \"$searchQuery\". Try a different search.",
                            icon = if (searchQuery.isBlank()) Icons.Default.LocalHospital else Icons.Default.Search
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        itemsIndexed(filteredHospitals) { index, hospital ->
                            ExpandableHospitalCard(
                                hospital = hospital,
                                rank = index + 1,
                                userLat = userLat,
                                userLon = userLon
                            )
                        }
                        item { Spacer(Modifier.height(8.dp)) }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExpandableHospitalCard(hospital: Hospital, rank: Int, userLat: Double, userLon: Double) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }

    val distanceKm = remember(userLat, userLon) {
        val dLat = hospital.latitude - userLat
        val dLon = hospital.longitude - userLon
        (sqrt(dLat.pow(2) + dLon.pow(2)) * 111f).toFloat()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBgColor, RoundedCornerShape(16.dp))
            .border(1.dp, BorderColorDim, RoundedCornerShape(16.dp))
            .clickable { expanded = !expanded }
    ) {
        // Header row
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank badge
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(
                        if (rank == 1) TealPrimary else CardBgColor,
                        RoundedCornerShape(10.dp)
                    )
                    .border(1.dp, if (rank == 1) TealPrimary else BorderColorDim, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "#$rank",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = if (rank == 1) Color.White else TextSecondaryColor
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(hospital.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color.White)
                    if (rank == 1) {
                        Spacer(Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .background(
                                    Brush.linearGradient(listOf(TealPrimary, Color(0xFF00BCD4))),
                                    RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("AI Pick", fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Text(hospital.specialty, color = TextSecondaryColor, fontSize = 11.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${hospital.rating}★", color = Color(0xFFFFC107), fontSize = 11.sp)
                    Text("  ·  ${"%.1f".format(distanceKm)} km away", color = TextSecondaryColor, fontSize = 11.sp)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${hospital.cureRate.toInt()}%",
                    fontWeight = FontWeight.Bold,
                    color = when {
                        hospital.cureRate >= 90 -> Color(0xFF4CAF50)
                        hospital.cureRate >= 80 -> Color(0xFFFFC107)
                        else -> Color(0xFFF44336)
                    },
                    fontSize = 15.sp
                )
                Text("cure rate", fontSize = 9.sp, color = TextSecondaryColor)
                Spacer(Modifier.height(4.dp))
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    null, tint = TextSecondaryColor, modifier = Modifier.size(18.dp)
                )
            }
        }

        // Cure rate bar
        LinearProgressIndicator(
            progress = { hospital.cureRate / 100f },
            modifier = Modifier.fillMaxWidth().height(3.dp),
            color = when {
                hospital.cureRate >= 90 -> Color(0xFF4CAF50)
                hospital.cureRate >= 80 -> Color(0xFFFFC107)
                else -> Color(0xFFF44336)
            },
            trackColor = BorderColorDim
        )

        // Expandable details
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(spring()) + fadeIn(tween(200)),
            exit = shrinkVertically(tween(200)) + fadeOut(tween(200))
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                HorizontalDivider(color = BorderColorDim, thickness = 0.5.dp)
                Spacer(Modifier.height(10.dp))

                HospitalDetailRow(Icons.Default.LocationOn, hospital.address)
                HospitalDetailRow(Icons.Default.Phone, hospital.phone.ifBlank { "Not listed" })
                HospitalDetailRow(Icons.Default.Language, hospital.website.ifBlank { "Not listed" })
                HospitalDetailRow(Icons.Default.AccessTime, hospital.openingHours)

                if (hospital.specializations.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text("Specializations", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextSecondaryColor)
                    Spacer(Modifier.height(4.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        hospital.specializations.forEach { spec ->
                            Box(
                                modifier = Modifier
                                    .background(TealPrimary.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                                    .border(1.dp, TealPrimary.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(spec, fontSize = 10.sp, color = TealPrimary)
                            }
                        }
                    }
                }

                if (hospital.aiReason.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF00897B).copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, null, tint = TealPrimary, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(hospital.aiReason, fontSize = 11.sp, color = TextSecondaryColor)
                    }
                }

                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            val uri = Uri.parse("geo:${hospital.latitude},${hospital.longitude}?q=${Uri.encode(hospital.name)}")
                            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TealPrimary),
                        border = androidx.compose.foundation.BorderStroke(1.dp, TealPrimary)
                    ) {
                        Icon(Icons.Default.Map, null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Directions", fontSize = 12.sp)
                    }
                    Button(
                        onClick = {
                            if (hospital.phone.isNotBlank()) {
                                context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${hospital.phone}")))
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                    ) {
                        Icon(Icons.Default.Call, null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Call", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun HospitalDetailRow(icon: androidx.compose.ui.graphics.vector.ImageVector, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = TextSecondaryColor, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(8.dp))
        Text(value, fontSize = 12.sp, color = Color.White)
    }
}

@Composable
fun RankedHospitalItem(hospital: Hospital, rank: Int) {
    ExpandableHospitalCard(hospital = hospital, rank = rank, userLat = 13.0827, userLon = 80.2707)
}
