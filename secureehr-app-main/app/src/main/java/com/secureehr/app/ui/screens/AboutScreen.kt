package com.secureehr.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.secureehr.app.ui.components.BorderColorDim
import com.secureehr.app.ui.components.CardBgColor
import com.secureehr.app.ui.components.TextSecondaryColor
import com.secureehr.app.ui.theme.CyanAccent
import com.secureehr.app.ui.theme.TealPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About SecureEHR", fontWeight = FontWeight.Bold, color = Color.White) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Logo
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(
                        Brush.linearGradient(
                            listOf(TealPrimary, CyanAccent),
                            start = Offset(0f, 0f),
                            end = Offset(80f, 80f)
                        ),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.LocalHospital, null, tint = Color.White, modifier = Modifier.size(44.dp))
            }

            Spacer(Modifier.height(16.dp))
            Text("SecureEHR", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.height(4.dp))
            Text("Version 1.0.0", color = TextSecondaryColor, fontSize = 13.sp)

            Spacer(Modifier.height(28.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardBgColor, RoundedCornerShape(16.dp))
                    .border(1.dp, BorderColorDim, RoundedCornerShape(16.dp))
                    .padding(20.dp)
            ) {
                Text(
                    "SecureEHR is a next-generation healthcare platform that puts you in control of your medical data. Using state-of-the-art security and AI, we ensure your records are safe, accessible, and actionable.",
                    fontSize = 14.sp,
                    color = TextSecondaryColor,
                    lineHeight = 22.sp
                )
            }

            Spacer(Modifier.height(20.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardBgColor, RoundedCornerShape(16.dp))
                    .border(1.dp, BorderColorDim, RoundedCornerShape(16.dp))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Key Features", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                listOf(
                    "Encrypted Medical Records",
                    "AI-Powered Health Insights",
                    "Smart Hospital Finder",
                    "Secure Consent Management",
                    "Health Wallet with Blockchain Verification"
                ).forEach { feature ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, null, tint = TealPrimary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(feature, fontSize = 14.sp, color = Color.White)
                    }
                }
            }

            Spacer(Modifier.weight(1f))
            Spacer(Modifier.height(24.dp))

            Text("© 2024 SecureEHR Inc.", fontSize = 12.sp, color = TextSecondaryColor)
        }
    }
}
