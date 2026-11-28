/*
 * RKB LAUNCHER - shared design tokens and glass components.
 * One place for the glass card, page header, primary pill button and the global backdrop so that
 * Home, Instances, Download, Account, Settings, Mod Settings and About share the same look.
 */
package com.movtery.zalithlauncher.ui.screens.content

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.setting.AllSettings
import java.io.File

/** Design tokens shared by every RKB screen. */
object RkbTokens {
    val CardShape = RoundedCornerShape(20.dp)
    val ButtonShape = RoundedCornerShape(24.dp)
    const val GlassAlpha = 0.62f
    const val BorderAlpha = 0.30f
    val ScreenPadding = 14.dp
}

@Composable
fun RkbGlassCard(
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = modifier,
        shape = RkbTokens.CardShape,
        color = scheme.surface.copy(alpha = RkbTokens.GlassAlpha),
        border = BorderStroke(1.dp, scheme.primary.copy(alpha = if (selected) 0.85f else RkbTokens.BorderAlpha))
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}

@Composable
fun RkbPageHeader(icon: Int, title: String, subtitle: String, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(painterResource(icon), null, tint = scheme.primary, modifier = Modifier.size(34.dp))
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
        }
    }
}

/**
 * Global backdrop drawn behind every screen: the user's wallpaper (image or video, chosen in
 * Settings > Launcher) or, without one, a dark blue gradient. A scrim keeps glass cards readable.
 */
@Composable
fun RkbBackdrop(modifier: Modifier = Modifier) {
    val path = AllSettings.homeWallpaper.state
    val file = remember(path) { path.takeIf { it.isNotBlank() }?.let { File(it) }?.takeIf { it.exists() } }
    Box(
        modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0A1F4D), Color(0xFF071228), Color(0xFF05070C))))
    ) {
        if (file != null) RkbWallpaperView(file = file, modifier = Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(Color(0x59000000)))
    }
}
