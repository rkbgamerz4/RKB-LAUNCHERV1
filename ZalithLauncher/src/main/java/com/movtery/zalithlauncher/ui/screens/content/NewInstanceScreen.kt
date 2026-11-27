/*
 * RKB LAUNCHER - New Instance screen (redesigned).
 * Choose a type to create a new instance.
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
import androidx.compose.ui.unit.dp
import com.movetry.zalithlauncher.R

@Composable
fun NewInstanceScreen(
    onSelectNormal: () -> Unit,
    onSelectModpack: () -> Unit,
    onSelectRust: () -> Unit,
    onSelectBoost: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.Center) {
        // Header
        Text(
            text = "New Instance",
            style = MaterialTheme.typography.headlineMedium,
            color = scheme.onSurface,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(vertical = 24.dp)
        )

        // Instance type cards in a row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            Arrangement.Center,
            verticalAlignment = Alignment.Center
        ) {
            // Normal instance card
            InstanceTypeCard(
                icon = R.drawable.ic_build_filled,
                title = "Normal",
                description = "Play Minecraft with vanilla experience.",
                onClick = onSelectNormal,
                scheme = scheme,
                isComingSoon = false
            )

            // Modpack instance card
            InstanceTypeCard(
                icon = R.drawable.ic_library_books,
                title = "Modpack",
                description = "Play with amazing modpacks and content.",
                onClick = onSelectModpack,
                scheme = scheme,
                isComingSoon = false
            )

            // Rust Edition card - COMING SOON
            InstanceTypeCard(
                icon = R.drawable.ic_regular_expression,
                title = "Rust Edition",
                description = "Experience the Rust Edition of Minecraft.",
                onClick = onSelectRust,
                scheme = scheme,
                isComingSoon = true
            )

            // RKB BOOST card - COMING SOON
            InstanceTypeCard(
                icon = R.drawable.ic_compose,
                title = "RKB BOOST",
                description = "A performance optimized experience.",
                onClick = onSelectBoost,
                scheme = scheme,
                isComingSoon = true
            )
        }
    }
}

/** Instance type card component. */
@Composable
fun InstanceTypeCard(
    @DrawableRes val icon: Int,
    val title: String,
    val description: String,
    val onClick: () -> Unit,
    val scheme: MaterialColorScheme,
    val isComingSoon: Boolean
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .background(
                if (isComingSoon)
                    MaterialTheme.colorScheme.surfaceContainerHigh
                else
                    MaterialTheme.colorScheme.surface,
                RoundedCornerShape(16.dp)
            ),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterStart
        ) {
            // Icon
            Icon(
                painterResource(icon),
                contentDescription = title,
                tint = if (isComingSoon) MaterialTheme.colorScheme.error else scheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.height(8.dp))

            // Title
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = if (isComingSoon) MaterialTheme.colorScheme.error else scheme.onSurface,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                alignment = Alignment.CenterStart
            )
            Spacer(Modifier.height(4.dp))

            // Description
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = if (isComingSoon) MaterialTheme.colorScheme.onSurfaceVariant else scheme.onSurfaceVariant,
                maxLines = 1,
                alignment = Alignment.CenterStart
            )
            Spacer(Modifier.height(8.dp))

            // Coming soon badge or Create button
            if (isComingSoon) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.error,
                    onClick = {} // disabled
                ) {
                    Text(
                        text = "COMING SOON",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.White
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        alignment = Alignment.Center
                    )
                }
            } else {
                Button(
                    onClick = onClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = scheme.primaryContainer,
                        contentColor = scheme.onPrimaryContainer
                    )
                ) {
                    Text(
                        text = "Create",
                        style = MaterialTheme.typography.labelMedium,
                        color = scheme.onPrimaryContainer,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}