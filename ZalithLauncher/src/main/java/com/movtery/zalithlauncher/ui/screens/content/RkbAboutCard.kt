/*
 * RKB LAUNCHER - About header (reference: About screen).
 * Only real data is shown: app name, version and package come from BuildConfig / the running app.
 * No external links are invented; the upstream credits and open-source licenses stay below this card
 * in the existing About screen (required by the GPLv3 attribution terms).
 */
package com.movtery.zalithlauncher.ui.screens.content

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.BuildConfig
import com.movtery.zalithlauncher.R

@Composable
fun RkbAboutCard(modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val context: Context = LocalContext.current
    val rows = listOf(
        "App Name" to "RKB Launcher",
        "Version" to BuildConfig.VERSION_NAME,
        "Version code" to BuildConfig.VERSION_CODE.toString(),
        "Package" to context.packageName,
        "Platform" to "Android ${android.os.Build.VERSION.RELEASE}"
    )
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = scheme.surface.copy(alpha = 0.62f),
        border = BorderStroke(1.dp, scheme.primary.copy(alpha = 0.3f))
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(painterResource(R.drawable.rkb_logo), "RKB LAUNCHER", Modifier.height(56.dp))
                Spacer(Modifier.width(14.dp))
                Column {
                    Text("RKB LAUNCHER", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                    Text("Play \u2022 Explore \u2022 Enjoy", style = MaterialTheme.typography.labelLarge, color = scheme.primary)
                }
            }
            Text(
                "A Minecraft: Java Edition launcher for Android, built on Zalith Launcher 2.",
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant
            )
            Text("App Information", style = MaterialTheme.typography.titleSmall, color = scheme.primary)
            rows.forEach { (k, v) ->
                Row(Modifier.fillMaxWidth()) {
                    Text(k, Modifier.width(110.dp), style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
                    Text(v, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
                }
            }
            Text("Made by RKB Studio", style = MaterialTheme.typography.labelLarge, color = scheme.primary)
        }
    }
}
