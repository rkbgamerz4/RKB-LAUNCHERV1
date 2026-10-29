/*
 * RKB LAUNCHER
 * Based on Zalith Launcher 2 (GPLv3), Copyright (C) 2025 MovTery and contributors.
 * This program is free software under the GNU General Public License v3 or later.
 */

package com.movtery.zalithlauncher.ui.screens.main

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.ui.screens.NestedNavKey
import com.movtery.zalithlauncher.ui.screens.NormalNavKey
import com.movtery.zalithlauncher.ui.screens.TitledNavKey

/** Destinations shown in the RKB rail, in the order used by the reference screenshots. */
enum class RkbDestination(val label: String, val icon: Int) {
    Home("Home", R.drawable.ic_home_filled),
    Account("Account", R.drawable.ic_person_outlined),
    Mods("Mods", R.drawable.ic_extension_outlined),
    Control("Control", R.drawable.ic_videogame_asset_outlined),
    About("About", R.drawable.ic_info_outlined),
    Instances("Instances", R.drawable.ic_folder_outlined),
    Download("Download", R.drawable.ic_download_2_outlined),
    Setting("Setting", R.drawable.ic_settings_filled),
}

/** Works out which rail item matches the screen currently shown. */
fun rkbSelectedDestination(mainScreenKey: TitledNavKey?): RkbDestination? = when (mainScreenKey) {
    null, is NormalNavKey.LauncherMain -> RkbDestination.Home
    is NormalNavKey.AccountManager -> RkbDestination.Account
    is NormalNavKey.VersionsManager -> RkbDestination.Instances
    is NestedNavKey.Download -> RkbDestination.Download
    is NestedNavKey.VersionSettings ->
        if (mainScreenKey.currentKey is NormalNavKey.Versions.ModsManager) RkbDestination.Mods
        else RkbDestination.Instances
    is NestedNavKey.Settings -> when (mainScreenKey.currentKey) {
        is NormalNavKey.Settings.ControlManager, is NormalNavKey.Settings.Control,
        is NormalNavKey.Settings.Gamepad -> RkbDestination.Control
        is NormalNavKey.Settings.AboutInfo -> RkbDestination.About
        else -> RkbDestination.Setting
    }
    else -> null
}

@Composable
fun RkbNavigationRail(
    selected: RkbDestination?,
    onSelect: (RkbDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.fillMaxHeight().width(132.dp),
        color = scheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "RKB",
                style = MaterialTheme.typography.titleLarge,
                color = scheme.primary,
                modifier = Modifier.padding(start = 10.dp, bottom = 6.dp)
            )
            RkbDestination.entries.forEach { dest ->
                RkbRailItem(dest, dest == selected) { onSelect(dest) }
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun RkbRailItem(dest: RkbDestination, selected: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(12.dp)
    val bg = if (selected) Brush.horizontalGradient(listOf(scheme.primaryContainer, scheme.primary))
    else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
    val tint = if (selected) scheme.onPrimaryContainer else scheme.onSurfaceVariant
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(bg, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(painterResource(dest.icon), contentDescription = dest.label, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(10.dp))
        Text(dest.label, color = tint, style = MaterialTheme.typography.labelLarge, maxLines = 1)
    }
}
