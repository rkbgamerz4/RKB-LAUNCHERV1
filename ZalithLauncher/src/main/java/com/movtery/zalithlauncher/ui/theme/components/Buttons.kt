/*
 * RKB Launcher
 * Premium Minecraft Java Launcher — AMOLED Dark Design System
 *
 * Buttons.kt
 * Branded button set: gradient glow primary, outlined secondary, icon
 * buttons and a compact chip button — all with spring press animations
 * and ripple feedback.
 */

package com.movtery.zalithlauncher.ui.theme.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import com.movtery.zalithlauncher.ui.theme.RkbShape
import com.movtery.zalithlauncher.ui.theme.rkbColors

private val PressSpring = spring<Float>(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness = Spring.StiffnessMedium,
)

/**
 * Primary CTA button. Gradient fill (accentBlue -> accentViolet), soft blue
 * glow shadow, scale-down press animation, ripple. This is the "Play"
 * button style.
 */
@Composable
fun RKBPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    shape: Shape = RkbShape.buttonPill,
    contentPadding: PaddingValues = PaddingValues(horizontal = 28.dp, vertical = 16.dp),
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = PressSpring,
        label = "primaryButtonScale",
    )
    val glowAlpha by animateFloatAsState(
        targetValue = if (pressed) 0.9f else 0.55f,
        animationSpec = PressSpring,
        label = "primaryButtonGlow",
    )
    val ext = MaterialTheme.rkbColors

    Box(
        modifier = modifier
            .wrapContentWidth()
            .scale(scale)
            .shadow(
                elevation = if (enabled) 18.dp else 0.dp,
                shape = shape,
                ambientColor = ext.glowBlue.copy(alpha = glowAlpha),
                spotColor = ext.glowBlue.copy(alpha = glowAlpha),
            )
            .clip(shape)
            .background(
                brush = if (enabled) {
                    Brush.horizontalGradient(listOf(ext.accentBlue, ext.accentViolet))
                } else {
                    Brush.horizontalGradient(
                        listOf(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.surfaceVariant)
                    )
                }
            )
            .clickable(
                interactionSource = interactionSource,
                indication = rememberRipple(color = Color.White),
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(contentPadding),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = ext.iconOnAccent,
                    modifier = Modifier.size(20.dp),
                )
                Box(modifier = Modifier.size(8.dp))
            }
            Text(
                text = text,
                color = ext.iconOnAccent,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

/**
 * Secondary action button — transparent glass fill, glowing outline,
 * used for "Cancel", "View more", toolbar actions.
 */
@Composable
fun RKBSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    shape: Shape = RkbShape.button,
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = PressSpring,
        label = "secondaryButtonScale",
    )
    val ext = MaterialTheme.rkbColors
    val borderColor by animateColorAsState(
        targetValue = if (pressed) ext.accentBlue else ext.glassStroke,
        label = "secondaryButtonBorder",
    )

    Box(
        modifier = modifier
            .wrapContentWidth()
            .scale(scale)
            .clip(shape)
            .background(ext.glassFill)
            .border(width = 1.dp, color = borderColor, shape = shape)
            .clickable(
                interactionSource = interactionSource,
                indication = rememberRipple(color = ext.accentBlue),
                role = Role.Button,
                onClick = onClick,
            )
            .padding(contentPadding),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = ext.textPrimary,
                    modifier = Modifier.size(18.dp),
                )
                Box(modifier = Modifier.size(8.dp))
            }
            Text(
                text = text,
                color = ext.textPrimary,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

/**
 * Circular icon button with an animated glow ring when active/selected —
 * used in the sidebar and toolbars.
 */
@Composable
fun RKBIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    size: Dp = 44.dp,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.9f else 1f,
        animationSpec = PressSpring,
        label = "iconButtonScale",
    )
    val ext = MaterialTheme.rkbColors
    val bg by animateColorAsState(
        targetValue = if (active) ext.accentBlue.copy(alpha = 0.16f) else Color.Transparent,
        label = "iconButtonBg",
    )
    val tint by animateColorAsState(
        targetValue = if (active) ext.iconActive else ext.iconInactive,
        label = "iconButtonTint",
    )

    Box(
        modifier = modifier
            .size(size)
            .scale(scale)
            .clip(CircleShape)
            .background(bg)
            .clickable(
                interactionSource = interactionSource,
                indication = rememberRipple(bounded = true, color = ext.accentBlue),
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(size * 0.5f),
        )
    }
}

/**
 * Small pill chip-button — quick actions, filter toggles, version tags.
 */
@Composable
fun RKBChipButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    leadingIcon: ImageVector? = null,
) {
    val ext = MaterialTheme.rkbColors
    val interactionSource = remember { MutableInteractionSource() }
    val bg by animateColorAsState(
        targetValue = if (selected) ext.accentBlue.copy(alpha = 0.18f) else ext.glassFill,
        label = "chipBg",
    )
    val border by animateColorAsState(
        targetValue = if (selected) ext.accentBlue else ext.glassStroke,
        label = "chipBorder",
    )
    val content by animateColorAsState(
        targetValue = if (selected) ext.accentBlue else ext.textSecondary,
        label = "chipContent",
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .height(34.dp)
            .clip(RkbShape.buttonPill)
            .background(bg)
            .border(1.dp, border, RkbShape.buttonPill)
            .clickable(
                interactionSource = interactionSource,
                indication = rememberRipple(color = ext.accentBlue),
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp),
    ) {
        if (leadingIcon != null) {
            Icon(leadingIcon, contentDescription = null, tint = content, modifier = Modifier.size(14.dp))
            Box(modifier = Modifier.size(6.dp))
        }
        Text(text = text, color = content, style = MaterialTheme.typography.labelMedium)
    }
}
