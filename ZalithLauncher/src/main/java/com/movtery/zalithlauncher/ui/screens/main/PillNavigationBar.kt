/*
 * RKB LAUNCHER
 * Based on Zalith Launcher 2 (GPLv3), Copyright (C) 2025 MovTery and contributors.
 * This program is free software under the GNU General Public License v3 or later.
 */
package com.movtery.zalithlauncher.ui.screens.main

import androidx.compose.foundation.Background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PillToken
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.R

/** Top pill navigation bar with 4–5 selectable tabs. */
@Composable
fun PillNavigationBar(
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val isLight = MaterialTheme.isLight

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        color = scheme.surface,
        shadowElevation = 1.dp,
    ) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .background(
                    Brush.surface,
                    RoundedCornerShape(20.dp, 20.dp, 0.dp, 0.dp)
                ),
            Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // Left spacer to push tabs left, or remove for full-width
            Spacer(Modifier.width(16.dp))

            val tabs = listOf(
                RkbDestination.Home,
                RkbDestination.Instances,
                RkbDestination.Download,
                RkbDestination.Mods
            )

            tabs.forEachIndexed { index, dest ->
                val isSelected = selectedIndex == index
                val textColor = if (isSelected) scheme.onPrimary else scheme.onSurfaceVariant
                val bgColor = if (isSelected) {
                    scheme.primary
                } else {
                    scheme.surfaceContainerHigh
                }
                val iconTint = if (isSelected) Color.White else scheme.onSurfaceVariant

                Surface(
                    modifier = Modifier
                        .height(36.dp)
                        .width(0f, min = 0.dp) // each tab takes available space proportionally
                        .background(
                            if (isSelected)
                                Brush.solid(scheme.primary)
                            else
                                Brush.solid(scheme.surfaceContainerHigh),
                            RoundedCornerShape(16.dp)
                        ),
                    onClick = { onSelect(index) }
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Icon(
                            painterResource(dest.icon),
                            contentDescription = dest.label,
                            tint = iconTint,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = dest.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = textColor,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            Spacer(Modifier.width(16.dp))
        }
    }
}