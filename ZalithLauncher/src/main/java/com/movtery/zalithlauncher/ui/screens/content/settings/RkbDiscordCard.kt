/*
 * RKB LAUNCHER - Discord Rich Presence setting card. GPLv3.
 */
package com.movtery.zalithlauncher.ui.screens.content.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.ui.components.TitleAndSummary
import com.movtery.zalithlauncher.utils.RkbDiscord

@Composable
fun RkbDiscordCard(modifier: Modifier = Modifier) {
    val status = RkbDiscord.status.collectAsState().value
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TitleAndSummary(
                modifier = Modifier.weight(1f),
                title = "Discord Rich Presence",
                summary = "Status: $status. Shows what you are playing on your Discord profile."
            )
            Button(onClick = { RkbDiscord.connect() }) { Text("Connect") }
            OutlinedButton(onClick = { RkbDiscord.disconnect() }) { Text("Disconnect") }
        }
    }
}
