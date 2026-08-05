package com.secureehr.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val CardBgColor = Color(0xFF1E2435)
val BorderColorDim = Color(0xFF2A3347)
val TextSecondaryColor = Color(0xFF8892A4)
val TealColor = Color(0xFF00897B)
val CyanColor = Color(0xFF00BCD4)

val expandSpring: SpringSpec<Float> = spring(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness = Spring.StiffnessMediumLow
)

@Composable
fun ShimmerBrush(): Brush {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val x by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerX"
    )
    return Brush.linearGradient(
        colors = listOf(CardBgColor, Color(0xFF2A3347), CardBgColor),
        start = Offset(x - 400f, 0f),
        end = Offset(x, 0f)
    )
}

@Composable
fun ShimmerBox(modifier: Modifier = Modifier, cornerRadius: Dp = 8.dp) {
    Box(modifier = modifier.background(ShimmerBrush(), RoundedCornerShape(cornerRadius)))
}

@Composable
fun ShimmerRecordCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp)
            .background(ShimmerBrush(), RoundedCornerShape(16.dp))
            .border(1.dp, BorderColorDim, RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ShimmerBox(modifier = Modifier.size(42.dp), cornerRadius = 10.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ShimmerBox(modifier = Modifier.fillMaxWidth(0.65f).height(14.dp))
                ShimmerBox(modifier = Modifier.fillMaxWidth(0.45f).height(11.dp))
            }
        }
    }
}

@Composable
fun EmptyState(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .background(CardBgColor, RoundedCornerShape(48.dp))
                .border(1.dp, BorderColorDim, RoundedCornerShape(48.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(44.dp),
                tint = TextSecondaryColor.copy(alpha = 0.4f)
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
        Spacer(Modifier.height(6.dp))
        Text(
            text = subtitle,
            fontSize = 13.sp,
            color = TextSecondaryColor,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
    }
}

@Composable
fun SectionLabel(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title.uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = TextSecondaryColor,
        letterSpacing = 1.2.sp,
        modifier = modifier.padding(horizontal = 4.dp, vertical = 10.dp)
    )
}

@Composable
fun PremiumCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(CardBgColor, RoundedCornerShape(16.dp))
            .border(1.dp, BorderColorDim, RoundedCornerShape(16.dp)),
        content = content
    )
}

@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false
) {
    val gradient = if (enabled)
        Brush.linearGradient(listOf(TealColor, CyanColor))
    else
        Brush.linearGradient(listOf(Color(0xFF37474F), Color(0xFF455A64)))

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .background(gradient, RoundedCornerShape(27.dp))
            .then(
                if (enabled && !isLoading)
                    Modifier.then(Modifier)
                else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = text,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
    }
}
