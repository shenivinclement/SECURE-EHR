package com.secureehr.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.secureehr.app.navigation.Screen
import com.secureehr.app.ui.components.TextSecondaryColor
import com.secureehr.app.ui.theme.CyanAccent
import com.secureehr.app.ui.theme.TealPrimary
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(navController: NavController) {
    var visible by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.4f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "splashScale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(600),
        label = "splashAlpha"
    )

    LaunchedEffect(Unit) {
        delay(100)
        visible = true
        delay(2200)
        navController.navigate(Screen.Login.route) {
            popUpTo(Screen.Splash.route) { inclusive = true }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0E1A)),
        contentAlignment = Alignment.Center
    ) {
        // Background blobs
        Box(
            modifier = Modifier
                .size(280.dp)
                .offset((-80).dp, (-120).dp)
                .background(TealPrimary.copy(alpha = 0.05f), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(200.dp)
                .align(Alignment.BottomEnd)
                .offset(60.dp, 60.dp)
                .background(CyanAccent.copy(alpha = 0.04f), CircleShape)
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.scale(scale).alpha(alpha)
        ) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .background(
                        Brush.linearGradient(
                            listOf(TealPrimary, CyanAccent),
                            start = Offset(0f, 0f),
                            end = Offset(88f, 88f)
                        ),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.LocalHospital,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }
            Spacer(Modifier.height(24.dp))
            Text(
                "SecureEHR",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Your health, secured.",
                fontSize = 14.sp,
                color = TextSecondaryColor
            )
            Spacer(Modifier.height(40.dp))
            CircularProgressIndicator(
                color = TealPrimary,
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp
            )
        }
    }
}
