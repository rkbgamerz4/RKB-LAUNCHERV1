/*
 * RKB LAUNCHER - "Instance Download" category hub: Normal Versions / Modpack / Rust Edition (coming soon).
 * Compact on purpose: it has to fit a phone in landscape (about 300 dp of height) without clipping.
 */
package com.movtery.zalithlauncher.ui.screens.content

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
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
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column {
            Text("INSTANCE DOWNLOAD", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(
                "Download and manage Minecraft instances",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1
            )
        }
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val landscape = maxWidth > maxHeight
            if (landscape) {
                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    HubCard("NORMAL", "VERSIONS", "Vanilla, Fabric, Forge, NeoForge, Quilt", "Vanilla · Fabric · Forge · NeoForge · Quilt",
                        "Browse versions", Blue, true, onNormal, Modifier.weight(1f).fillMaxHeight())
                    HubCard("MOD", "PACK", "Modrinth and CurseForge modpacks, installed as instances", "Modrinth · CurseForge",
                        "Browse modpacks", Violet, true, onModpack, Modifier.weight(1f).fillMaxHeight())
                    HubCard("RUST", "EDITION", "Minecraft 26.3 on the Horizon engine", "Horizon · Native",
                        "Coming soon", Orange, false, onRust, Modifier.weight(1f).fillMaxHeight())
                }
            } else {
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HubCard("NORMAL", "VERSIONS", "Vanilla, Fabric, Forge, NeoForge, Quilt", "Vanilla · Fabric · Forge · NeoForge · Quilt",
                        "Browse versions", Blue, true, onNormal, Modifier.fillMaxWidth())
                    HubCard("MOD", "PACK", "Modrinth and CurseForge modpacks, installed as instances", "Modrinth · CurseForge",
                        "Browse modpacks", Violet, true, onModpack, Modifier.fillMaxWidth())
                    HubCard("RUST", "EDITION", "Minecraft 26.3 on the Horizon engine", "Horizon · Native",
                        "Coming soon", Orange, false, onRust, Modifier.fillMaxWidth())
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
    tags: String,
    button: String,
    accent: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(12.dp)
    Surface(
        modifier = modifier.then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        shape = shape,
        color = scheme.surfaceContainer,
        border = BorderStroke(if (enabled) 1.5.dp else 1.dp, if (enabled) accent else accent.copy(alpha = 0.4f))
    ) {
        Column(
            Modifier.padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(
                            when (accent) {
                                Blue -> R.drawable.ic_sports_esports_outlined
                                Violet -> R.drawable.ic_package_2_outlined
                                else -> R.drawable.ic_settings_filled
                            }
                        ),
                        contentDescription = null,
                        tint = if (enabled) accent else accent.copy(alpha = 0.6f),
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = buildAnnotatedString {
                            append("$white ")
                            withStyle(SpanStyle(color = accent)) { append(accentWord) }
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    tags,
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(8.dp))
            Surface(
                modifier = Modifier.fillMaxWidth().height(38.dp),
                shape = RoundedCornerShape(8.dp),
                color = if (enabled) accent else scheme.surfaceContainerHigh
            ) {
                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        button.uppercase(),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (enabled) Color.White else scheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
