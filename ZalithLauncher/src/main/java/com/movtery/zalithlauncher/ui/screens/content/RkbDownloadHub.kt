/*
 * RKB LAUNCHER - "New Instance" chooser (reference: New Instance screen) and Download page state.
 *
 * Normal and Modpack open their existing, fully functional install flows.
 * Rust Edition and RKB BOOST are COMING SOON placeholders: no create / download / play actions.
 */
package com.movtery.zalithlauncher.ui.screens.content

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.R

/** Which page of the Download screen is showing. */
object RkbDownloadHub {
    /** Hub = New Instance chooser, Browse = Mods/Resource Packs/Shaders/... browser. */
    enum class Page { Hub, Normal, ModPack, Browse }

    var page by mutableStateOf(Page.Browse)
}

@Composable
fun RkbDownloadHubScreen(
    onNormal: () -> Unit,
    onModpack: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = "Back")
            }
            Column {
                Text("New Instance", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Choose a type to create a new instance", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            if (maxWidth >= 720.dp) {
                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TypeCards(onNormal, onModpack, Modifier.weight(1f))
                }
            } else {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    TypeCards(onNormal, onModpack, Modifier.fillMaxWidth())
                }
            }
        }
    }
}

/** Emits the four type cards, each with [modifier]; works inside a Row or a Column. */
@Composable
private fun TypeCards(onNormal: () -> Unit, onModpack: () -> Unit, modifier: Modifier) {
    RkbTypeCard("Normal", "Play Minecraft with the vanilla experience, or add Fabric, Forge, NeoForge or Quilt.", R.drawable.ic_sports_esports_outlined, onNormal, modifier)
    RkbTypeCard("Modpack", "Play with Modrinth and CurseForge modpacks, installed as instances.", R.drawable.ic_package_2_outlined, onModpack, modifier)
    RkbComingSoonTypeCard(RkbUpcoming.RustEdition, modifier)
    RkbComingSoonTypeCard(RkbUpcoming.RkbBoost, modifier)
}

@Composable
private fun RkbTypeCard(title: String, description: String, icon: Int, onClick: () -> Unit, modifier: Modifier) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(20.dp)
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = shape,
        color = scheme.surface.copy(alpha = 0.7f),
        border = BorderStroke(1.dp, scheme.primary.copy(alpha = 0.7f))
    ) {
        Column(
            Modifier.padding(16.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(painterResource(icon), null, tint = scheme.primary, modifier = Modifier.size(72.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(description, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Button(onClick = onClick, modifier = Modifier.fillMaxWidth().height(44.dp), shape = RoundedCornerShape(22.dp)) {
                Icon(painterResource(R.drawable.ic_play_arrow_filled), null, Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
                Text("Create", fontWeight = FontWeight.Bold)
            }
        }
    }
}
