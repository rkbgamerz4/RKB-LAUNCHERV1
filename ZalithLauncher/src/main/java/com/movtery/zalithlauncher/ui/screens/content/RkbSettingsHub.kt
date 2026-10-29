/*
 * RKB LAUNCHER - reference-style Settings hub (card list). Every card opens an existing Zalith settings page.
 */
package com.movtery.zalithlauncher.ui.screens.content

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.ui.screens.NormalNavKey
import com.movtery.zalithlauncher.ui.screens.TitledNavKey

/** Whether the Settings screen currently shows the card list (true) or one settings page (false). */
object RkbSettingsHub {
    var show by mutableStateOf(true)
}

private class HubItem(val icon: Int, val title: String, val summary: String, val key: TitledNavKey)

private val hubItems = listOf(
    HubItem(R.drawable.ic_java, "Java & Runtime", "Manage Java versions and runtime.", NormalNavKey.Settings.JavaManager),
    HubItem(R.drawable.ic_video_settings, "Video & Renderer", "Resolution, renderer and performance settings.", NormalNavKey.Settings.Renderer),
    HubItem(R.drawable.ic_rocket_launch_filled, "Game", "RAM, JVM arguments and game options.", NormalNavKey.Settings.Game),
    HubItem(R.drawable.ic_videogame_asset_outlined, "Control Customization", "Gestures, buttons and scaling options.", NormalNavKey.Settings.Control),
    HubItem(R.drawable.ic_sports_esports_outlined, "Gamepad", "Controller mapping and options.", NormalNavKey.Settings.Gamepad),
    HubItem(R.drawable.ic_setting_launcher, "Launcher", "Theme, Home wallpaper and launcher behavior.", NormalNavKey.Settings.Launcher),
    HubItem(R.drawable.ic_info_outlined, "About", "Version, credits and links.", NormalNavKey.Settings.AboutInfo),
)

@Composable
fun RkbSettingsHubScreen(
    onBackHome: () -> Unit,
    onOpen: (TitledNavKey) -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        RkbBar(text = "Back to home", onClick = onBackHome)
        Text("Settings", style = MaterialTheme.typography.headlineSmall)
        Text("Customize your launcher experience.", style = MaterialTheme.typography.bodyMedium, color = scheme.primary)
        hubItems.forEach { item ->
            Surface(
                modifier = Modifier.fillMaxWidth().clickable { onOpen(item.key) },
                shape = RoundedCornerShape(16.dp),
                color = scheme.surfaceContainerHigh,
                border = BorderStroke(1.dp, scheme.outlineVariant)
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(painterResource(item.icon), contentDescription = null, tint = scheme.primary, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(item.title, style = MaterialTheme.typography.titleMedium)
                        Text(item.summary, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                    }
                    Text("\u203A", style = MaterialTheme.typography.headlineSmall, color = scheme.primary)
                }
            }
        }
    }
}

/** A slim back bar shown above a single settings page. */
@Composable
fun RkbBar(text: String, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = scheme.surfaceContainer,
        border = BorderStroke(1.dp, scheme.outlineVariant)
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("\u2190", style = MaterialTheme.typography.titleMedium, color = scheme.primary)
            Spacer(Modifier.width(10.dp))
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    }
}
