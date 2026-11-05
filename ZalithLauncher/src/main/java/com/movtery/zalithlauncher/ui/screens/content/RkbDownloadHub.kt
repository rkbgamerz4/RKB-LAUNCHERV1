/*
 * RKB LAUNCHER - "Instance Download" category hub: Normal Versions / Modpack / Rust Edition.
 * The three categories are kept separate; each opens its own existing or dedicated flow.
 */
package com.movtery.zalithlauncher.ui.screens.content

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.R

/** Which page of the Download screen is showing. */
object RkbDownloadHub {
    enum class Page { Hub, Normal, ModPack, Rust }

    var page by mutableStateOf(Page.Hub)
}

private val Blue = Color(0xFF00A8FF)
private val Violet = Color(0xFF8A5CFF)
private val Orange = Color(0xFFFF6A1A)

@Composable
fun RkbDownloadHubScreen(
    onNormal: () -> Unit,
    onModpack: () -> Unit,
    onRust: () -> Unit,
    modifier: Modifier = Modifier
) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val appear by animateFloatAsState(if (shown) 1f else 0f, tween(350), label = "hubAppear")

    Column(
        modifier = modifier
            .fillMaxSize()
            .alpha(appear)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column {
            Text("INSTANCE DOWNLOAD", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
            Text(
                "Download and manage Minecraft instances",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val landscape = maxWidth > maxHeight
            if (landscape) {
                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    HubCard("NORMAL", "VERSIONS", "Vanilla, Fabric, Forge, NeoForge, Quilt and more", listOf("Vanilla", "Fabric", "Forge", "NeoForge", "Quilt"),
                        "Browse Versions", Blue, onNormal, Modifier.weight(1f).fillMaxHeight())
                    HubCard("MOD", "PACK", "Modrinth and CurseForge modpacks, installed as instances", listOf("Modrinth", "CurseForge"),
                        "Browse Modpacks", Violet, onModpack, Modifier.weight(1f).fillMaxHeight())
                    HubCard("RUST", "EDITION", "Minecraft 26.3 (Rust Edition) with Horizon", listOf("Horizon", "Native", "Experimental"),
                        "Browse Horizon", Orange, onRust, Modifier.weight(1f).fillMaxHeight())
                }
            } else {
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HubCard("NORMAL", "VERSIONS", "Vanilla, Fabric, Forge, NeoForge, Quilt and more", listOf("Vanilla", "Fabric", "Forge", "NeoForge", "Quilt"),
                        "Browse Versions", Blue, onNormal, Modifier.fillMaxWidth())
                    HubCard("MOD", "PACK", "Modrinth and CurseForge modpacks, installed as instances", listOf("Modrinth", "CurseForge"),
                        "Browse Modpacks", Violet, onModpack, Modifier.fillMaxWidth())
                    HubCard("RUST", "EDITION", "Minecraft 26.3 (Rust Edition) with Horizon", listOf("Horizon", "Native", "Experimental"),
                        "Browse Horizon", Orange, onRust, Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun HubCard(
    white: String,
    accentWord: String,
    description: String,
    chips: List<String>,
    button: String,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier
) {
    val shape = RoundedCornerShape(20.dp)
    Surface(
        modifier = modifier
            .drawBehind {
                // soft glow: a wider translucent stroke behind the border
                drawRoundRect(
                    color = accent.copy(alpha = 0.22f),
                    cornerRadius = CornerRadius(20.dp.toPx()),
                    style = Stroke(width = 10.dp.toPx())
                )
            }
            .clip(shape)
            .clickable(onClick = onClick),
        shape = shape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(2.dp, Brush.verticalGradient(listOf(accent, accent.copy(alpha = 0.35f))))
    ) {
        Column(
            Modifier
                .background(Brush.verticalGradient(listOf(accent.copy(alpha = 0.18f), Color.Transparent)))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painterResource(
                    when (accent) {
                        Blue -> R.drawable.ic_sports_esports_outlined
                        Violet -> R.drawable.ic_package_2_outlined
                        else -> R.drawable.ic_settings_filled
                    }
                ),
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(52.dp)
            )
            Row {
                Text(white, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Spacer(Modifier.width(6.dp))
                Text(accentWord, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = accent)
            }
            Text(
                description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                chips.take(4).forEach { chip ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = accent.copy(alpha = 0.15f)
                    ) {
                        Text(chip, Modifier.padding(horizontal = 6.dp, vertical = 3.dp), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            Spacer(Modifier.weight(1f, fill = false))
            Surface(
                modifier = Modifier.fillMaxWidth().height(44.dp),
                shape = RoundedCornerShape(12.dp),
                color = accent
            ) {
                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Icon(painterResource(R.drawable.ic_download_2_outlined), null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(button, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
