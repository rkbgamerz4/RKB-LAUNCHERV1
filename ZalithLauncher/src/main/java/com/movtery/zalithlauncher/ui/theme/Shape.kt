/*
 * RKB Launcher
 * Premium Minecraft Java Launcher — AMOLED Dark Design System
 *
 * Shape.kt
 * Corner-radius system for buttons, cards, dialogs, and text fields.
 */

package com.movtery.zalithlauncher.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val RkbShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

object RkbShape {
    val button = RoundedCornerShape(16.dp)
    val buttonPill = RoundedCornerShape(percent = 50)
    val buttonSmall = RoundedCornerShape(12.dp)
    val card = RoundedCornerShape(20.dp)
    val cardLarge = RoundedCornerShape(24.dp)
    val cardCompact = RoundedCornerShape(14.dp)
    val dialog = RoundedCornerShape(28.dp)
    val textField = RoundedCornerShape(14.dp)
    val navIndicator = RoundedCornerShape(14.dp)
    val avatar = RoundedCornerShape(12.dp)
}