/*
 * RKB Launcher
 * Premium Minecraft Java Launcher — AMOLED Dark Design System
 *
 * Cards.kt
 * Glassmorphism card primitives: translucent fill, gradient hairline
 * border, soft ambient shadow. Built as a single flexible GlassCard plus
 * a few purpose-built wrappers (stat tile, section card).
 */
package com.movtery.zalithlauncher.ui.theme.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import com.movtery.zalithlauncher.ui.theme.RkbGradients
import com.movtery.zalithlauncher.ui.theme.RkbShape
import com.movtery.zalithlauncher.ui.theme.rkbColors

/**
 * The core glassmorphism surface used everywhere in RKB Launcher:
 * translucent background over whatever sits behind it, a soft blue-tinted
 * gradient hairline border, and a diffuse ambient shadow. Optionally
 * interactive (press-scale + ripple) when [onClick] is supplied.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RkbShape.card,
    elevation: Dp = 12.dp,
    borderBrush: Brush = Brush.linearGradient(RkbGradients.cardBorder),
    onClick: (() -> Unit)? = null,
    contentPadding: Dp = 20.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val ext = MaterialTheme.rkbColors
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (onClick != null && pressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "glassCardScale",
    )

    val base = modifier
        .scale(scale)
        .shadow(
            elevation = elevation,
            shape = shape,
            ambientColor = ext.glowBlue.copy(alpha = 0.25f),
            spotColor = ext.glowBlue.copy(alpha = 0.25f),
        )
        .clip(shape)
        .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.72f))
        .background(ext.glassFill)
        .border(width = 1.dp, brush = borderBrush, shape = shape)

    val clickable = if (onClick != null) {
        base.clickable(
            interactionSource = interactionSource,
            indication = rememberRipple(color = ext.accentBlue),
            onClick = onClick,
        )
    } else base

    Column(
        modifier = clickable.padding(contentPadding),
        content = content,
    )
}

/**
 * Compact stat tile for quick-glance numbers (FPS avg, playtime, mods
 * installed, etc.) — used in grids on the home screen.
 */
@Composable
fun RKBStatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.rkbColors.accentBlue,
) {
    GlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RkbShape.cardCompact,
        elevation = 6.dp,
        contentPadding = 16.dp,
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = accent,
        )
        Box(modifier = Modifier.padding(top = 2.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.rkbColors.textSecondary,
            )
        }
    }
}

/**
 * A titled section wrapper — groups related settings/content under a
 * heading, consistent spacing, used throughout the Settings screen.
 */
@Composable
fun RKBSectionCard(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    GlassCard(modifier = modifier.fillMaxWidth(), borderBrush = Brush.linearGradient(
        listOf(MaterialTheme.rkbColors.glassStroke, MaterialTheme.rkbColors.glassStroke)
    )) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.rkbColors.textPrimary,
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.rkbColors.textTertiary,
                modifier = Modifier.padding(top = 2.dp, bottom = 12.dp),
            )
        } else {
            Box(modifier = Modifier.padding(top = 10.dp))
        }
        content()
    }
}
