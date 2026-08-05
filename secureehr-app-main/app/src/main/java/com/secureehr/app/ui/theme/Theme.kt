package com.secureehr.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary          = TealPrimary,
    secondary        = CyanAccent,
    tertiary         = Color(0xFF03DAC5),
    background       = DarkNavy,
    surface          = DarkSurface,
    surfaceVariant   = CardBg,
    outline          = BorderColor,
    onPrimary        = Color.White,
    onSecondary      = Color.White,
    onTertiary       = Color.White,
    onBackground     = Color.White,
    onSurface        = Color.White,
    onSurfaceVariant = TextSecondary,
    error            = Color(0xFFCF6679),
    onError          = Color.White,
    errorContainer   = Color(0xFF8B0000).copy(alpha = 0.35f),
    onErrorContainer = Color(0xFFFFB4AB)
)

private val LightColorScheme = lightColorScheme(
    primary          = TealPrimary,
    secondary        = Color(0xFF018786),
    background       = Color(0xFFF5F5F5),
    surface          = Color.White,
    surfaceVariant   = Color(0xFFF0F4F4),
    onPrimary        = Color.White,
    onBackground     = Color(0xFF1C1B1F),
    onSurface        = Color(0xFF1C1B1F),
    onSurfaceVariant = Color(0xFF49454F)
)

@Composable
fun SecureEHRTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography  = Typography,
        content     = content
    )
}
