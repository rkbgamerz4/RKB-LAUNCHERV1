package com.movtery.zalithlauncher.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class ExtendedColors(
    val accentBlue: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val cardBackground: Color,
    val divider: Color
)

val LocalExtendedColors = staticCompositionLocalOf {
    ExtendedColors(
        accentBlue = Color(0xFF00A8FF),
        textPrimary = Color.White,
        textSecondary = Color(0xFFB8C7D9),
        textTertiary = Color(0xFF7E8A9A),
        cardBackground = Color(0xFF111827),
        divider = Color(0xFF1F2937)
    )
}