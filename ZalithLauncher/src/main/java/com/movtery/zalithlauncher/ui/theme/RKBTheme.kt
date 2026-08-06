/*
 * RKB Launcher
 * Premium Minecraft Java Launcher — AMOLED Dark Design System
 *
 * RKBTheme.kt
 * Separate, additive theme — does NOT touch or replace ZalithLauncherTheme.
 * Wraps content in RKBLauncherTheme { ... } wherever you want the RKB
 * brand look; everywhere else keeps using ZalithLauncherTheme as before.
 */

package com.movtery.zalithlauncher.ui.theme

import android.app.Activity
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ---------- Brand palette ----------

val RkbAccentBlue = Color(0xFF00A8FF)
val RkbAccentBlueBright = Color(0xFF3FC4FF)
val RkbAccentBlueDim = Color(0xFF0077B3)
val RkbBackground = Color(0xFF050814)
val RkbSurfacePrimary = Color(0xFF101827)
val RkbSurfaceSecondary = Color(0xFF1B2435)
val RkbSurfaceElevated = Color(0xFF26314A)
val RkbAccentViolet = Color(0xFF7C4DFF)
val RkbAccentVioletContainer = Color(0xFF241B45)
val RkbAccentMint = Color(0xFF00E5A0)
val RkbAccentMintContainer = Color(0xFF00301F)
val RkbError = Color(0xFFFF3B5C)
val RkbErrorContainer = Color(0xFF3D0A17)
val RkbWarning = Color(0xFFFFB020)

val RkbTextPrimary = Color(0xFFF2F5FA)
val RkbTextSecondary = Color(0xFFA9B4C6)
val RkbTextTertiary = Color(0xFF6C7891)
val RkbTextDisabled = Color(0xFF454F63)
val RkbIconActive = RkbAccentBlue
val RkbIconInactive = Color(0xFF6C7891)
val RkbIconOnAccent = Color(0xFF03111C)

val RkbOutline = Color(0xFF2E3B52)
val RkbOutlineVariant = Color(0xFF1B2435)
val RkbDivider = Color(0xFF1B2435)

val RkbGlassFill = Color(0x14FFFFFF)
val RkbGlassFillStrong = Color(0x22FFFFFF)
val RkbGlassStroke = Color(0x33FFFFFF)
val RkbGlowBlueShadow = Color(0x6600A8FF)
val RkbGlowBlueShadowStrong = Color(0x9900A8FF)
val RkbGlowVioletShadow = Color(0x557C4DFF)

object RkbGradients {
    val accent = listOf(RkbAccentBlue, RkbAccentViolet)
    val cardBorder = listOf(
        RkbAccentBlue.copy(alpha = 0.6f),
        RkbAccentViolet.copy(alpha = 0.25f),
        Color.Transparent,
    )
}

private val RkbDarkScheme: ColorScheme = darkColorScheme(
    primary = RkbAccentBlue,
    onPrimary = Color(0xFF00151F),
    primaryContainer = Color(0xFF0A2E45),
    onPrimaryContainer = Color(0xFF7FDBFF),
    secondary = RkbAccentViolet,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = RkbAccentVioletContainer,
    onSecondaryContainer = Color(0xFFC9B6FF),
    tertiary = RkbAccentMint,
    onTertiary = Color(0xFF00291C),
    tertiaryContainer = RkbAccentMintContainer,
    onTertiaryContainer = Color(0xFF6FFFD1),
    error = RkbError,
    onError = Color(0xFFFFFFFF),
    errorContainer = RkbErrorContainer,
    onErrorContainer = Color(0xFFFFB3C1),
    background = RkbBackground,
    onBackground = RkbTextPrimary,
    surface = RkbSurfacePrimary,
    onSurface = RkbTextPrimary,
    surfaceVariant = RkbSurfaceSecondary,
    onSurfaceVariant = RkbTextSecondary,
    outline = RkbOutline,
    outlineVariant = RkbOutlineVariant,
)

private val RkbLightScheme: ColorScheme = lightColorScheme(
    primary = Color(0xFF0077B3),
    onPrimary = Color(0xFFFFFFFF),
    background = Color(0xFFF7F9FC),
    onBackground = Color(0xFF0F1420),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F1420),
)

data class RkbExtendedColors(
    val glassFill: Color,
    val glassFillStrong: Color,
    val glassStroke: Color,
    val glowBlue: Color,
    val glowBlueStrong: Color,
    val glowViolet: Color,
    val accentBlue: Color,
    val accentViolet: Color,
    val accentMint: Color,
    val warning: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val textDisabled: Color,
    val iconActive: Color,
    val iconInactive: Color,
    val iconOnAccent: Color,
    val divider: Color,
)

private val LocalRkbExtendedColors = staticCompositionLocalOf {
    RkbExtendedColors(
        glassFill = RkbGlassFill,
        glassFillStrong = RkbGlassFillStrong,
        glassStroke = RkbGlassStroke,
        glowBlue = RkbGlowBlueShadow,
        glowBlueStrong = RkbGlowBlueShadowStrong,
        glowViolet = RkbGlowVioletShadow,
        accentBlue = RkbAccentBlue,
        accentViolet = RkbAccentViolet,
        accentMint = RkbAccentMint,
        warning = RkbWarning,
        textPrimary = RkbTextPrimary,
        textSecondary = RkbTextSecondary,
        textTertiary = RkbTextTertiary,
        textDisabled = RkbTextDisabled,
        iconActive = RkbIconActive,
        iconInactive = RkbIconInactive,
        iconOnAccent = RkbIconOnAccent,
        divider = RkbDivider,
    )
}

val MaterialTheme.rkbColors: RkbExtendedColors
    @Composable
    get() = LocalRkbExtendedColors.current

@Composable
fun RKBLauncherTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) RkbDarkScheme else RkbLightScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
            @Suppress("DEPRECATION")
            window.statusBarColor = colorScheme.background.toArgb()
            @Suppress("DEPRECATION")
            window.navigationBarColor = colorScheme.background.toArgb()
        }
    }

    CompositionLocalProvider(LocalRkbExtendedColors provides LocalRkbExtendedColors.current) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = RkbTypography,
            shapes = RkbShapes,
            content = content,
        )
    }
}