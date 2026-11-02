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
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.layout.Box
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
import com.movtery.zalithlauncher.game.account.isLocalAccount
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
        modifier = modifier.fillMaxHeight().width(136.dp),
        color = scheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
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
            .height(36.dp)
            .background(bg, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(painterResource(dest.icon), contentDescription = dest.label, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(8.dp))
        Text(dest.label, color = tint, style = MaterialTheme.typography.labelLarge, maxLines = 1, softWrap = false)
    }
}


@Composable
fun RkbBrandTitle() {
    val scheme = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically) {
        androidx.compose.foundation.Image(
            painter = painterResource(R.drawable.rkb_logo),
            contentDescription = "RKB",
            modifier = Modifier.height(26.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = androidx.compose.ui.text.buildAnnotatedString {
                androidx.compose.ui.text.withStyle(androidx.compose.ui.text.SpanStyle(color = scheme.primary)) { append("RKB ") }
                append("LAUNCHER")
            },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Black,
            maxLines = 1,
            softWrap = false
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = "PLAY \u2022 EXPLORE \u2022 CREATE",
            style = MaterialTheme.typography.labelSmall,
            color = scheme.secondary,
            letterSpacing = 2.sp,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
fun RkbAccountChip() {
    val scheme = MaterialTheme.colorScheme
    val account = com.movtery.zalithlauncher.game.account.AccountsManager.currentAccountFlow
        .collectAsState().value
    val name = account?.username ?: "No account"
    val kind = if (account == null) "ADD ACCOUNT"
    else if (account.isLocalAccount()) "LOCAL" else "ONLINE"
    val pickWallpaper = com.movtery.zalithlauncher.ui.screens.content.settings.rememberWallpaperPicker()
    Row(verticalAlignment = Alignment.CenterVertically) {
        Row(
            modifier = Modifier
                .background(scheme.surfaceContainerHigh, RoundedCornerShape(20.dp))
                .padding(start = 6.dp, end = 12.dp, top = 3.dp, bottom = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (account != null) {
                com.movtery.zalithlauncher.ui.screens.content.elements.PlayerFace(
                    modifier = Modifier.clip(androidx.compose.foundation.shape.CircleShape),
                    account = account,
                    avatarSize = 30.dp
                )
            } else {
                Surface(shape = androidx.compose.foundation.shape.CircleShape, color = scheme.primaryContainer, modifier = Modifier.size(30.dp)) {}
            }
            Spacer(Modifier.width(8.dp))
            Column {
                Text(name, style = MaterialTheme.typography.labelMedium, maxLines = 1)
                Text(kind, style = MaterialTheme.typography.labelSmall, color = scheme.primary, maxLines = 1)
            }
        }
        Spacer(Modifier.width(6.dp))
        androidx.compose.material3.IconButton(onClick = pickWallpaper) {
            Icon(
                painter = painterResource(R.drawable.ic_format_paint_outlined),
                contentDescription = "Wallpaper",
                tint = scheme.primary
            )
        }
    }
}

/** Pill-style tab used for the horizontal category rows (Download etc.). */
@Composable
fun RkbTabChip(selected: Boolean, onClick: () -> Unit, content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val bg = if (selected) Brush.horizontalGradient(listOf(scheme.primaryContainer, scheme.primary))
    else Brush.horizontalGradient(listOf(scheme.surfaceContainerHigh, scheme.surfaceContainerHigh))
    Row(
        modifier = Modifier
            .background(bg, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}
