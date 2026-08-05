package com.secureehr.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.secureehr.app.ui.theme.TealPrimary

private val DoctorPurple = Color(0xFF9C27B0)

@Composable
fun RoleBadge(role: String) {
    val isDoctor = role.equals("doctor", ignoreCase = true)
    val accent = if (isDoctor) DoctorPurple else TealPrimary
    Text(
        text = if (isDoctor) "Doctor" else "Patient",
        color = accent,
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .background(accent.copy(alpha = 0.18f), RoundedCornerShape(20.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}
