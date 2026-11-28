/*
 * RKB LAUNCHER - Mod Settings (reference: Mod Settings).
 *
 * Shows the REAL mods of the selected instance (its mods folder, read with the existing
 * AllModReader) and lets the user switch them on/off. A switch only records a pending change;
 * "Apply Changes" then enables/disables the files with the existing LocalMod.enable()/disable()
 * (a ".disabled" suffix), exactly like the full mod manager. No mod names are hard-coded.
 */
package com.movtery.zalithlauncher.ui.screens.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.version.installed.Version
import com.movtery.zalithlauncher.game.version.installed.VersionFolders
import com.movtery.zalithlauncher.game.version.mod.AllModReader
import com.movtery.zalithlauncher.game.version.mod.LocalMod
import com.movtery.zalithlauncher.game.version.mod.isEnabled
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun RkbModSettingsScreen(
    version: Version?,
    onChooseInstance: () -> Unit,
    onOpenFullManager: (Version) -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    var mods by remember { mutableStateOf<List<LocalMod>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var reloadKey by remember { mutableIntStateOf(0) }
    val pending: SnapshotStateMap<String, Boolean> = remember { mutableStateMapOf() }
    var applying by remember { mutableStateOf(false) }

    LaunchedEffect(version, reloadKey) {
        pending.clear()
        if (version == null || !version.isValid()) { mods = emptyList(); return@LaunchedEffect }
        loading = true
        val dir = File(version.getGameDir(), VersionFolders.MOD.folderName)
        mods = withContext(Dispatchers.IO) {
            runCatching { AllModReader(dir).readAllLocals().filter { !it.notMod }.sortedBy { it.name.lowercase() } }
                .getOrDefault(emptyList())
        }
        loading = false
    }

    // Desired state = pending change if any, else the file's current state.
    fun desired(m: LocalMod) = pending[m.file.absolutePath] ?: m.file.isEnabled()
    val total = mods.size
    val enabledNow = mods.count { desired(it) }

    Column(
        modifier = modifier.fillMaxSize().padding(RkbTokens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        RkbPageHeader(
            R.drawable.ic_extension_outlined, "Mod Settings",
            version?.let { "Enable or disable mods for ${it.getVersionName()}." } ?: "Select an instance to manage its mods."
        )

        if (version == null || !version.isValid()) {
            RkbGlassCard(Modifier.fillMaxWidth()) {
                Text("No instance selected", style = MaterialTheme.typography.titleMedium)
                Text("Mods belong to an instance. Choose one first.", color = scheme.onSurfaceVariant)
                Button(onClick = onChooseInstance, shape = RkbTokens.ButtonShape) { Text("Choose instance") }
            }
            return@Column
        }

        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val wide = maxWidth >= 720.dp
            val statusCard: @Composable (Modifier) -> Unit = { mod ->
                RkbGlassCard(mod) {
                    Text("Mod Status", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    StatusRow("Total Mods", total)
                    StatusRow("Enabled", enabledNow)
                    StatusRow("Disabled", total - enabledNow)
                    Text(
                        "Some mods may require a restart of the game to take effect.",
                        style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant
                    )
                }
            }
            val list: @Composable (Modifier) -> Unit = { mod ->
                when {
                    loading -> RkbGlassCard(mod) { Text("Loading mods...") }
                    mods.isEmpty() -> RkbGlassCard(mod) {
                        Text("No mods found in this instance.", style = MaterialTheme.typography.titleSmall)
                        Text("Install mods from Download > Mods.", color = scheme.onSurfaceVariant)
                    }
                    else -> LazyColumn(mod, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(mods, key = { it.file.absolutePath }) { m ->
                            RkbGlassCard(Modifier.fillMaxWidth()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(m.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(
                                            listOfNotNull(m.version, m.loader.displayName.takeIf { it.isNotBlank() }).joinToString(" \u2022 "),
                                            style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant, maxLines = 1
                                        )
                                    }
                                    Switch(
                                        checked = desired(m),
                                        onCheckedChange = { on ->
                                            val key = m.file.absolutePath
                                            // flipping back to the file's real state cancels the pending change
                                            if (on == m.file.isEnabled()) pending.remove(key) else pending[key] = on
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
            if (wide) {
                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    list(Modifier.weight(2f).fillMaxSize())
                    statusCard(Modifier.weight(1f))
                }
            } else {
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    statusCard(Modifier.fillMaxWidth())
                    list(Modifier.weight(1f).fillMaxWidth())
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = {
                    applying = true
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            mods.forEach { m ->
                                val want = pending[m.file.absolutePath] ?: return@forEach
                                if (want) m.enable() else m.disable()
                            }
                        }
                        applying = false
                        reloadKey++
                    }
                },
                enabled = pending.isNotEmpty() && !applying,
                modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                shape = RkbTokens.ButtonShape
            ) { Text(if (pending.isEmpty()) "Apply Changes" else "Apply Changes (${pending.size})") }
            OutlinedButton(
                onClick = { onOpenFullManager(version) },
                modifier = Modifier.heightIn(min = 52.dp),
                shape = RkbTokens.ButtonShape
            ) { Text("Full mod manager") }
        }
    }
}

@Composable
private fun StatusRow(label: String, value: Int) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.width(8.dp))
        Text(value.toString(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}
