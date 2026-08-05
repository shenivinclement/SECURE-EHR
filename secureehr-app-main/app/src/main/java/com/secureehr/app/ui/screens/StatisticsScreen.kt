package com.secureehr.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.secureehr.app.ui.components.BorderColorDim
import com.secureehr.app.ui.components.CardBgColor
import com.secureehr.app.ui.components.TextSecondaryColor
import com.secureehr.app.ui.theme.TealPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Health Statistics", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF141824))
            )
        },
        containerColor = Color(0xFF0A0E1A)
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    "Live health metrics overview",
                    color = TextSecondaryColor,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            item { StatCard("Blood Pressure", "120/80", "mmHg", "Normal", Color(0xFF4CAF50), Icons.Default.Favorite, 0.6f) }
            item { StatCard("Heart Rate", "72", "bpm", "Resting", Color(0xFFEF5350), Icons.Default.FavoriteBorder, 0.72f) }
            item { StatCard("Blood Sugar", "95", "mg/dL", "Fasting", Color(0xFFFF9800), Icons.Default.WaterDrop, 0.48f) }
            item { StatCard("Body Weight", "70", "kg", "Stable", TealPrimary, Icons.Default.FitnessCenter, 0.55f) }
            item { StatCard("Sleep Duration", "7.5", "hrs", "Good", Color(0xFF7C4DFF), Icons.Default.NightsStay, 0.75f) }
            item { StatCard("Steps Today", "8,432", "steps", "Active", Color(0xFF00BCD4), Icons.Default.DirectionsWalk, 0.84f) }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
fun StatCard(title: String, value: String, unit: String, status: String, accentColor: Color, icon: ImageVector, progress: Float) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBgColor, RoundedCornerShape(16.dp))
            .border(1.dp, BorderColorDim, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(accentColor.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = accentColor, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = TextSecondaryColor, fontSize = 13.sp)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(value, fontWeight = FontWeight.Bold, fontSize = 28.sp, color = Color.White)
                    Spacer(Modifier.width(4.dp))
                    Text(unit, fontSize = 13.sp, color = TextSecondaryColor, modifier = Modifier.padding(bottom = 4.dp))
                }
            }
            Box(
                modifier = Modifier
                    .background(accentColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(status, fontSize = 11.sp, color = accentColor, fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(Modifier.height(12.dp))
        // Progress bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(BorderColorDim, RoundedCornerShape(2.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .height(4.dp)
                    .background(
                        Brush.linearGradient(
                            listOf(accentColor, accentColor.copy(alpha = 0.6f)),
                            Offset(0f, 0f), Offset(Float.POSITIVE_INFINITY, 0f)
                        ),
                        RoundedCornerShape(2.dp)
                    )
            )
        }
    }
}
