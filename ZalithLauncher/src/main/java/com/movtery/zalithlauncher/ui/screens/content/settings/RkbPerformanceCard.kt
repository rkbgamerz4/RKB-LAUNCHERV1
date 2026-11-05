/*
 * RKB LAUNCHER - launch speed controls and honest timing. GPLv3.
 */
package com.movtery.zalithlauncher.ui.screens.content.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.game.launch.RkbLaunchTrace
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.ui.components.TitleAndSummary

@Composable
fun RkbPerformanceCard(modifier: Modifier = Modifier) {
    val fast = AllSettings.rkbFastVerify.state
    var refresh by remember { mutableIntStateOf(0) }
    val runs = remember(refresh) { RkbLaunchTrace.recent() }
    val last = runs.lastOrNull()
    val median = RkbLaunchTrace.medianPreLaunchMs(runs.takeLast(5))

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TitleAndSummary(
                    modifier = Modifier.weight(1f),
                    title = "Fast file verification",
                    summary = "Do not re-hash game files that are unchanged since their last successful check (size and modified time). Turn off to always re-hash everything."
                )
                Switch(checked = fast, onCheckedChange = { AllSettings.rkbFastVerify.save(it) })
            }
            Text(
                text = if (last == null) {
                    "No launch recorded yet. After pressing PLAY, the time spent in the launcher before the game process starts is shown here."
                } else {
                    "Last PLAY, launcher side only: " + last.preLaunchMs + " ms (median of last " +
                        runs.takeLast(5).size + ": " + median + " ms). Files checked " + last.filesChecked +
                        ", skipped " + last.cacheHits + ", re-hashed " + last.rehashed + " in " + last.hashMs + " ms."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "This does not include Minecraft's own startup or world loading; time those separately.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (runs.isNotEmpty()) {
                OutlinedButton(onClick = { RkbLaunchTrace.clear(); refresh++ }) { Text("Clear timing history") }
            }
        }
    }
}
