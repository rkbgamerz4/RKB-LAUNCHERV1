/*
 * RKB LAUNCHER - reference-style Instances screen (search / sort / loader filter / New / empty state).
 * Reuses Zalith's real instance list item and rename/copy/delete/export actions. GPLv3.
 */
package com.movtery.zalithlauncher.ui.screens.content

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.addons.modloader.ModLoader
import com.movtery.zalithlauncher.game.version.installed.Version
import com.movtery.zalithlauncher.game.version.installed.VersionComparator
import com.movtery.zalithlauncher.game.version.installed.VersionType
import com.movtery.zalithlauncher.game.version.installed.VersionsManager
import com.movtery.zalithlauncher.ui.base.BaseScreen
import com.movtery.zalithlauncher.ui.screens.NestedNavKey
import com.movtery.zalithlauncher.ui.screens.NormalNavKey
import com.movtery.zalithlauncher.game.version.installed.RkbModpackMetaStore
import com.movtery.zalithlauncher.ui.screens.content.elements.CommonVersionInfoLayout
import com.movtery.zalithlauncher.ui.screens.content.elements.VersionsOperation
import com.movtery.zalithlauncher.viewmodel.ErrorViewModel
import com.movtery.zalithlauncher.viewmodel.ScreenBackStackViewModel

private const val LOADER_ALL = "ALL"
private const val LOADER_VANILLA = "VANILLA"

@Composable
fun RkbInstancesScreen(
    backScreenViewModel: ScreenBackStackViewModel,
    navigateToVersions: (Version) -> Unit,
    navigateToExport: (Version) -> Unit,
    submitError: (ErrorViewModel.ThrowableMessage) -> Unit,
    onPlay: (Version) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val allVersions by VersionsManager.versions.collectAsStateWithLifecycle()
    val current by VersionsManager.currentVersion.collectAsStateWithLifecycle()

    val appContext = androidx.compose.ui.platform.LocalContext.current
    val horizonVersion = remember { com.movtery.zalithlauncher.utils.RkbHorizon.installedVersion(appContext) }

    var query by remember { mutableStateOf("") }
    var sort by remember { mutableIntStateOf(0) } // 0 default order, 1 name A-Z, 2 name Z-A
    var loaderChoice by remember { mutableStateOf(LOADER_ALL) }
    var resortKey by remember { mutableIntStateOf(0) }
    var operation by remember { mutableStateOf<VersionsOperation>(VersionsOperation.None) }

    LaunchedEffect(Unit) {
        if (!VersionsManager.isRefreshing.value) VersionsManager.refresh("RkbInstancesScreen")
    }

    val shown = remember(allVersions, query, sort, loaderChoice, resortKey) {
        allVersions
            .filter { v ->
                when (loaderChoice) {
                    LOADER_ALL -> true
                    LOADER_VANILLA -> v.versionType == VersionType.VANILLA
                    else -> v.getVersionInfo()?.loaderInfos?.any { it.loader.name == loaderChoice } == true
                }
            }
            .filter { v -> query.isBlank() || v.getVersionName().contains(query.trim(), ignoreCase = true) }
            .let { list ->
                when (sort) {
                    1 -> list.sortedBy { it.getVersionName().lowercase() }
                    2 -> list.sortedByDescending { it.getVersionName().lowercase() }
                    else -> list.sortedWith(VersionComparator)
                }
            }
    }

    val showHorizon = horizonVersion != null && loaderChoice == LOADER_ALL &&
        (query.isBlank() || "horizon rust edition".contains(query.trim(), ignoreCase = true))

    BaseScreen(
        screenKey = NormalNavKey.VersionsManager,
        currentKey = backScreenViewModel.mainScreen.currentKey
    ) { _ ->
        VersionsOperation(
            versionsOperation = operation,
            updateVersionsOperation = { operation = it },
            submitError = submitError
        )

        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(painterResource(R.drawable.ic_folder_outlined), null, tint = scheme.primary, modifier = Modifier.size(34.dp))
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("INSTANCES", style = MaterialTheme.typography.headlineSmall, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    Text(
                        "MANAGE YOUR GAME PROFILES",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.primary
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    placeholder = { Text("Search instances...") }
                )
                RkbDropdown(
                    label = listOf("Default order", "Name A-Z", "Name Z-A")[sort],
                    options = listOf("Default order", "Name A-Z", "Name Z-A").mapIndexed { i, t -> t to { sort = i } }
                )
                val loaders = remember {
                    listOf("All Loaders" to LOADER_ALL, "Vanilla" to LOADER_VANILLA) +
                        ModLoader.entries.filter { it.isLoader }.map { it.displayName to it.name }
                }
                RkbDropdown(
                    label = loaders.firstOrNull { it.second == loaderChoice }?.first ?: "All Loaders",
                    options = loaders.map { (name, id) -> name to { loaderChoice = id } }
                )
                Button(
                    onClick = { backScreenViewModel.navigateToDownload() },
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(painterResource(R.drawable.ic_add), null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("CREATE INSTANCE")
                }
            }

            Surface(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = scheme.surface,
                border = BorderStroke(1.dp, scheme.outlineVariant)
            ) {
                if (shown.isNotEmpty() || showHorizon) {
                    LazyVerticalGrid(
                        modifier = Modifier.fillMaxSize(),
                        columns = GridCells.Adaptive(minSize = 290.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (showHorizon) {
                            item { RkbHorizonInstanceCard(horizonVersion ?: "?") }
                        }
                        items(shown, key = { it.toString() }) { version ->
                            RkbInstanceCard(
                                version = version,
                                selected = version == current,
                                onSelect = {
                                    if (version != current && !VersionsManager.saveVersion(version)) {
                                        operation = VersionsOperation.InvalidDelete(version)
                                    }
                                },
                                onPlay = { onPlay(version) },
                                onEdit = { navigateToVersions(version) },
                                onMods = {
                                    val settingsKey = NestedNavKey.VersionSettings(version)
                                    settingsKey.navigateTo(NormalNavKey.Versions.ModsManager)
                                    backScreenViewModel.mainScreen.navigateTo(settingsKey, useClassEquality = true)
                                },
                                onRename = { operation = VersionsOperation.Rename(version) },
                                onCopy = { operation = VersionsOperation.Copy(version) },
                                onExport = { navigateToExport(version) },
                                onDelete = { operation = VersionsOperation.Delete(version) }
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        if (allVersions.isEmpty()) {
                            Text("NO INSTANCES YET", style = MaterialTheme.typography.titleMedium)
                            Text("Create your first Minecraft instance.", color = scheme.onSurfaceVariant)
                            Spacer(Modifier.height(12.dp))
                            OutlinedButton(onClick = { backScreenViewModel.navigateToDownload() }) {
                                Icon(painterResource(R.drawable.ic_add), null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Add Instance")
                            }
                        } else {
                            Text("NO MATCHING INSTANCES", style = MaterialTheme.typography.titleMedium)
                            Text("Change the search text or the loader filter.", color = scheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RkbDropdown(label: String, options: List<Pair<String, () -> Unit>>) {
    var open by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { open = true }, shape = RoundedCornerShape(14.dp)) {
            Text(label, maxLines = 1)
            Spacer(Modifier.width(4.dp))
            Icon(painterResource(R.drawable.ic_keyboard_arrow_down), null, modifier = Modifier.size(18.dp))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { (text, action) ->
                DropdownMenuItem(text = { Text(text) }, onClick = { open = false; action() })
            }
        }
    }
}

/** Entry for the separately installed Horizon (Rust Edition) app. It starts the external native app, not the Java pipeline. */
@Composable
private fun RkbHorizonInstanceCard(version: String) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = scheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, Color(0xFFFF6A1A))
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Horizon 26.3", style = MaterialTheme.typography.titleMedium)
                Text("Minecraft Rust Edition, external native app (app version $version)", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                Text("Loader: Native", style = MaterialTheme.typography.labelMedium, color = Color(0xFFFF6A1A))
            }
            Button(onClick = { com.movtery.zalithlauncher.utils.RkbHorizon.launch(context) }) { Text("PLAY") }
        }
    }
}

/**
 * One instance (a real Zalith "version": vanilla or loader-based). Select by tapping the card; PLAY launches it with the
 * normal Java pipeline; Edit opens its settings (mods, game folder, ...); More holds rename / copy / export / delete (with the
 * existing confirmation dialogs).
 */
@Composable
private fun RkbInstanceCard(
    version: Version,
    selected: Boolean,
    onSelect: () -> Unit,
    onPlay: () -> Unit,
    onEdit: () -> Unit,
    onMods: () -> Unit,
    onRename: () -> Unit,
    onCopy: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val modpack = remember(version) { RkbModpackMetaStore.read(version) }
    var modpackOpen by remember { mutableStateOf(false) }
    val valid = remember(version) { version.isValid() }
    val info = remember(version) { version.getVersionInfo() }
    val loaderText = remember(version) {
        info?.loaderInfos?.takeIf { it.isNotEmpty() }?.joinToString(" + ") { it.loader.displayName } ?: "Vanilla"
    }
    val mcVersion = info?.minecraftVersion ?: "-"
    var menuOpen by remember { mutableStateOf(false) }

    Surface(
        onClick = { if (!selected) onSelect() },
        shape = RoundedCornerShape(12.dp),
        color = scheme.surfaceContainerHigh,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) scheme.primary else scheme.outlineVariant)
    ) {
        Column {
            RkbInstanceBanner(seedText = version.getVersionName(), loader = if (modpack != null) "modpack" else loaderText)
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (selected) {
                Text("SELECTED", style = MaterialTheme.typography.labelSmall, color = scheme.primary)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                CommonVersionInfoLayout(modifier = Modifier.weight(1f), version = version)
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (valid) Color(0xFF2FE37A).copy(alpha = 0.14f) else scheme.errorContainer
                ) {
                    Text(
                        if (valid) "Installed" else "Needs repair",
                        Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (valid) Color(0xFF2FE37A) else scheme.onErrorContainer
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Column {
                    Text("Loader", style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
                    Text(loaderText, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                }
                Column {
                    Text("Minecraft", style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
                    Text(mcVersion, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (selected) {
                    Button(
                        onClick = onPlay,
                        enabled = valid,
                        modifier = Modifier.weight(1.2f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(painterResource(R.drawable.ic_play_arrow_filled), null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("PLAY")
                    }
                }
                OutlinedButton(
                    onClick = { if (modpack != null) modpackOpen = true else onEdit() },
                    enabled = valid,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(painterResource(R.drawable.ic_edit_filled), null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (modpack != null) "Open" else "Edit")
                }
                Box {
                    OutlinedButton(
                        onClick = { menuOpen = true },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp)
                    ) {
                        Icon(painterResource(R.drawable.ic_more_horiz), contentDescription = "More", modifier = Modifier.size(20.dp))
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(text = { Text("Rename") }, leadingIcon = { Icon(painterResource(R.drawable.ic_edit_filled), null, modifier = Modifier.size(18.dp)) },
                            onClick = { menuOpen = false; onRename() })
                        DropdownMenuItem(text = { Text("Copy") }, leadingIcon = { Icon(painterResource(R.drawable.ic_file_copy_filled), null, modifier = Modifier.size(18.dp)) },
                            onClick = { menuOpen = false; onCopy() })
                        DropdownMenuItem(text = { Text("Export") }, leadingIcon = { Icon(painterResource(R.drawable.ic_folder_zip_filled), null, modifier = Modifier.size(18.dp)) },
                            onClick = { menuOpen = false; onExport() })
                        DropdownMenuItem(text = { Text("Delete") }, leadingIcon = { Icon(painterResource(R.drawable.ic_delete_filled), null, modifier = Modifier.size(18.dp)) },
                            onClick = { menuOpen = false; onDelete() })
                    }
                }
            }
            }
        }
    }
    if (modpackOpen && modpack != null) {
        RkbModpackInstanceDialog(
            version = version, meta = modpack,
            onDismiss = { modpackOpen = false },
            onPlay = { modpackOpen = false; onPlay() },
            onMods = { modpackOpen = false; onMods() },
            onExport = { modpackOpen = false; onExport() },
            onRename = { modpackOpen = false; onRename() },
            onDelete = { modpackOpen = false; onDelete() }
        )
    }
}

/**
 * Opened from a modpack instance: modpack information and modpack actions only. No version downloads, no loader browsing,
 * no world downloads and no instance creation here.
 */
@Composable
private fun RkbModpackInstanceDialog(
    version: Version,
    meta: com.movtery.zalithlauncher.game.version.installed.RkbModpackMeta,
    onDismiss: () -> Unit,
    onPlay: () -> Unit,
    onMods: () -> Unit,
    onExport: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Close") } },
        title = { Text(meta.name.ifBlank { version.getVersionName() }) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Source: " + meta.platform.ifBlank { "-" })
                Text("Minecraft: " + meta.minecraft.ifBlank { "-" })
                Text("Loader: " + meta.loaders.ifBlank { "-" })
                Spacer(Modifier.height(4.dp))
                Button(onClick = onPlay, modifier = Modifier.fillMaxWidth()) { Text("PLAY") }
                OutlinedButton(onClick = onMods, modifier = Modifier.fillMaxWidth()) { Text("Mods in this modpack") }
                OutlinedButton(onClick = onExport, modifier = Modifier.fillMaxWidth()) { Text("Export") }
                OutlinedButton(onClick = onRename, modifier = Modifier.fillMaxWidth()) { Text("Rename") }
                OutlinedButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) { Text("Delete") }
            }
        }
    )
}
