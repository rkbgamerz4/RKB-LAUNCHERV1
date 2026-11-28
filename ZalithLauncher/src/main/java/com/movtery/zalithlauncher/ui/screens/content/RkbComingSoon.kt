/*
 * RKB LAUNCHER - "Coming soon" entries (Rust Edition, RKB BOOST).
 * These are intentionally NON-functional: no install, no download, no play, no performance
 * controls. They only announce the feature.
 */
package com.movtery.zalithlauncher.ui.screens.content

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.movtery.zalithlauncher.R

enum class RkbUpcoming(
    val title: String,
    val tagline: String,
    val monogram: String,
    val tileColor: Color,
    val tileContent: Color,
) {
    RustEdition("Rust Edition", "Experience the Rust Edition of Minecraft.", "RUST", Color(0xFFCD412B), Color(0xFF1A0A06)),
    RkbBoost("RKB BOOST", "A performance-focused experience.", "BOOST", Color(0xFF0B2A57), Color(0xFF00A8FF)),
}

@Composable
fun RkbComingSoonBadge(modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .background(scheme.primary.copy(alpha = 0.18f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 3.dp)
    ) {
        Text(
            "COMING SOON",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = scheme.primary,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
private fun RkbUpcomingTile(item: RkbUpcoming, size: Int) {
    Box(
        modifier = Modifier.size(size.dp).background(item.tileColor, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (item == RkbUpcoming.RkbBoost) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    painterResource(R.drawable.ic_keyboard_double_arrow_up), null,
                    tint = item.tileContent, modifier = Modifier.size((size * 0.38f).dp)
                )
                Text(item.monogram, color = item.tileContent, fontWeight = FontWeight.Black, fontSize = (size * 0.2f).sp, maxLines = 1)
            }
        } else {
            Text(item.monogram, color = item.tileContent, fontWeight = FontWeight.Black, fontSize = (size * 0.26f).sp, maxLines = 1)
        }
    }
}

/** Compact row card (used on Home). Not clickable on purpose. */
@Composable
fun RkbComingSoonCard(item: RkbUpcoming, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.semantics { disabled(); contentDescription = "${item.title}, coming soon, not available yet" },
        shape = RoundedCornerShape(16.dp),
        color = scheme.surface.copy(alpha = 0.62f),
        border = BorderStroke(1.dp, scheme.primary.copy(alpha = 0.3f))
    ) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.alpha(0.85f)) { RkbUpcomingTile(item, 44) }
            Spacer(Modifier.width(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(item.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1)
                RkbComingSoonBadge()
            }
        }
    }
}

/** Large card for the "New Instance" chooser. Shows a disabled "Coming soon" button, never "Create". */
@Composable
fun RkbComingSoonTypeCard(item: RkbUpcoming, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.semantics { disabled(); contentDescription = "${item.title}, coming soon, not available yet" },
        shape = RoundedCornerShape(20.dp),
        color = scheme.surface.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, scheme.primary.copy(alpha = 0.25f))
    ) {
        Column(
            Modifier.padding(16.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(Modifier.alpha(0.8f)) { RkbUpcomingTile(item, 72) }
            Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(item.tagline, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Surface(
                modifier = Modifier.fillMaxWidth().height(44.dp),
                shape = RoundedCornerShape(22.dp),
                color = scheme.surfaceContainerHigh.copy(alpha = 0.6f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("COMING SOON", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold,
                        color = scheme.onSurfaceVariant, letterSpacing = 1.sp)
                }
            }
        }
    }
}
