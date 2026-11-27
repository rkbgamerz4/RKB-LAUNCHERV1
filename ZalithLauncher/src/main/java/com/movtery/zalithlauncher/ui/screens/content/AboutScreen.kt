/*
 * RKB LAUNCHER - About screen (redesigned).
 * Learn more about RKB Launcher.
 * Based on Zalith Launcher 2 (GPLv3).
 */
package com.movtery.zalithlauncher.ui.screens.content

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.movetry.zalithlauncher.R
import com.movetry.zalithlauncher.ui.androidText
import com.movetry.zalithlauncher.ui.base.BaseScreen
import com.movetry.zalithlauncher.ui.theme.onBackgroundColor
import com.movetry.zalithlauncher.viewmodel.ErrorViewModel

@Composable
fun AboutScreen(
    modifier: Modifier = Modifier,
    submitError: (ErrorViewModel.ThrowableMessage) -> Unit = { _ -> }
) {
    val scheme = MaterialTheme.colorScheme
    val context = LocalContext.current

    BaseScreen(
        screenKey = null,
        currentKey = null
    ) { isVisible ->

        Column(modifier = Modifier.fillMaxSize()) {
            // Header pill bar area
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(
                        Brush.solid(scheme.surfaceContainerHigh),
                        RoundedCornerShape(20.dp, 20.dp, 0.dp, 0.dp)
                    ),
                onClick = {}
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back button (subtle)
                    Surface(
                        modifier = Modifier.clickable { /* back action */ },
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Transparent
                    ) {
                        Icon(
                            painterResource(R.drawable.ic_arrow_back),
                            contentDescription = "Back",
                            tint = scheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Text(
                        text = "About",
                        style = MaterialTheme.typography.titleMedium,
                        color = scheme.onSurface,
                        maxLines = 1
                    )

                    // Three dots menu
                    Surface(
                        modifier = Modifier.clickable { /* menu action */ },
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Transparent
                    ) {
                        Icon(
                            painterResource(R.drawable.ic_menu_overflow),
                            contentDescription = "Menu",
                            tint = scheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Main content
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                shape = RoundedCornerShape(20.dp),
                color = scheme.surfaceContainerHigh,
                border = BorderStroke(1.dp, scheme.outlineVariant)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    // Left: Logo and description
                    Column(modifier = Modifier.width(200.dp)) {
                        androidx.compose.foundation.Image(
                            painterResource(R.drawable.rkb_logo),
                            contentDescription = "RKB Launcher",
                            modifier = Modifier.height(80.dp).width(80.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "RKB LAUNCHER",
                            style = MaterialTheme.typography.displaySmall,
                            color = scheme.primary,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            alignment = Alignment.CenterStart
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Play • Explore • Enjoy",
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.onSurfaceVariant,
                            alignment = Alignment.CenterStart
                        )
                        Spacer(Modifier.height(24.dp))

                        // App Information
                        Text(
                            text = "App Information",
                            style = MaterialTheme.typography.titleMedium,
                            color = scheme.onSurface,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.height(8.dp))

                        Column(Arrangement.spacedBy(8.dp)) {
                            _AboutRow(
                                icon = R.drawable.ic_build_filled,
                                label = "Version",
                                value = "1.0.0",
                                context = context
                            )
                            _AboutRow(
                                icon = R.drawable.ic_person_outlined,
                                label = "Package",
                                value = "com.rkbgamerz.rkblauncher.debug",
                                context = context
                            )
                            _AboutRow(
                                icon = R.drawable.ic_computer,
                                label = "Platform",
                                value = "Android",
                                context = context
                            )
                            _AboutRow(
                                icon = R.drawable.ic_calendar_today,
                                label = "Build Date",
                                value = "Nov 26, 2026",
                                context = context
                            )
                        }

                        Spacer(Modifier.height(24.dp))

                        // Features
                        Text(
                            text = "Features",
                            style = MaterialTheme.typography.titleMedium,
                            color = scheme.onSurface,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.height(8.dp))

                        Column(Arrangement.spacedBy(4.dp)) {
                            _FeatureChip("Modrinth Integration")
                            _FeatureChip("Custom Themes")
                            _FeatureChip("Easy Navigation")
                            _FeatureChip("High Performance")
                            _FeatureChip("And Much More...")
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Right: Quick Links
                    Column(Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Quick Links",
                            style = MaterialTheme.typography.titleMedium,
                            color = scheme.onSurface,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.height(8.dp))

                        Row(verticalAlignment = Alignment.CenterVertically, Arrangement.spacedBy(4.dp)) {
                            _QuickLinkItem(
                                icon = R.drawable.ic_group,
                                label = "Join Our Discord",
                                description = "Get support & updates"
                            )
                            _QuickLinkItem(
                                icon = R.drawable.ic_youtube_searched_for,
                                label = "Visit Our YouTube",
                                description = "Watch tutorials & more"
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, Arrangement.spacedBy(4.dp)) {
                            _QuickLinkItem(
                                icon = R.drawable.ic_github,
                                label = "View on GitHub",
                                description = "Check our source code"
                            )
                            _QuickLinkItem(
                                icon = R.drawable.ic_web,
                                label = "Our Website",
                                description = "Learn more about us"
                            )
                        }
                    }
                }
            }

            // Bottom bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .background(
                        Brush.solid(scheme.surfaceContainerHigh),
                        RoundedCornerShape(20.dp, 20.dp, 0.dp, 0.dp)
                    ),
                onClick = {}
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "RKB Studio",
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "❤️ Thanks for using!",
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.primary
                    )
                }
            }
        }
    }
}

/** About info row helper. */
@Composable
private fun _AboutRow(
    @DrawableRes val icon: Int,
    val label: String,
    val value: String,
    val context: androidx.compose.context.Context
) {
    Row(verticalAlignment = Alignment.CenterVertically, Arrangement.Start) {
        Icon(
            painterResource(icon),
            contentDescription = "$label info",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(8.dp))
        Column(Arrangement.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/** Feature chip helper. */
@Composable
private fun _FeatureChip(val text: String) {
    Surface(
        modifier = Modifier
            .height(24.dp)
            .background(
                MaterialTheme.colorScheme.surfaceContainerHigh,
                RoundedCornerShape(12.dp)
            ),
        onClick = {}
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )
    }
}

/** Quick link item helper. */
@Composable
private fun _QuickLinkItem(
    @DrawableRes val icon: Int,
    val label: String,
    val description: String
) {
    Surface(
        modifier = Modifier
            .height(40.dp)
            .background(
                MaterialTheme.colorScheme.surfaceContainerHigh,
                RoundedCornerShape(12.dp)
            ),
        onClick = {}
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(12.dp)) {
            Icon(
                painterResource(icon),
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column(Arrangement.Center) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}