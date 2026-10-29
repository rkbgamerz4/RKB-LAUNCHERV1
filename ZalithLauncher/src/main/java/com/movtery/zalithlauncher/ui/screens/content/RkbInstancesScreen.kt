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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.movtery.zalithlauncher.ui.screens.NormalNavKey
import com.movtery.zalithlauncher.ui.screens.content.elements.VersionItemLayout
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
    submitError: (ErrorViewModel.ThrowableMessage) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val allVersions by VersionsManager.versions.collectAsStateWithLifecycle()
    val current by VersionsManager.currentVersion.collectAsStateWithLifecycle()

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
                    Text("Instances", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "Manage your Minecraft instances. Create, edit or launch your worlds.",
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
                    Text("New")
                }
            }

            Surface(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = scheme.surface,
                border = BorderStroke(1.dp, scheme.outlineVariant)
            ) {
                if (shown.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(shown, key = { it.toString() }) { version ->
                            VersionItemLayout(
                                modifier = Modifier.fillMaxWidth(),
                                version = version,
                                selected = version == current,
                                submitError = submitError,
                                onSelected = {
                                    if (version != current && !VersionsManager.saveVersion(version)) {
                                        operation = VersionsOperation.InvalidDelete(version)
                                    }
                                },
                                onSettingsClick = { navigateToVersions(version) },
                                onRenameClick = { operation = VersionsOperation.Rename(version) },
                                onCopyClick = { operation = VersionsOperation.Copy(version) },
                                onExportClick = { navigateToExport(version) },
                                onDeleteClick = { operation = VersionsOperation.Delete(version) },
                                onPinned = { resortKey++ }
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
