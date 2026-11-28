/*
 * RKB LAUNCHER
 * Based on Zalith Launcher 2 (GPLv3), Copyright (C) 2025 MovTery and contributors.
 * This program is free software under the GNU General Public License v3 or later.
 */

package com.movtery.zalithlauncher.ui.screens.main

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.ui.screens.NestedNavKey
import com.movtery.zalithlauncher.ui.screens.NormalNavKey
import com.movtery.zalithlauncher.ui.screens.TitledNavKey

/** Destinations shown in the RKB rail, in the order used by the reference screenshots. */
enum class RkbDestination(val label: String, val icon: Int, val inPill: Boolean) {
    Home("Home", R.drawable.ic_home_filled, true),
    Instances("Instances", R.drawable.ic_folder_outlined, true),
    Download("Download", R.drawable.ic_download_2_outlined, true),
    Mods("Mods", R.drawable.ic_extension_outlined, true),
    Control("Controls", R.drawable.ic_videogame_asset_outlined, true),
    // Reached from the right-hand cluster (gear / avatar menu), not from the pill.
    Account("Account", R.drawable.ic_person_outlined, false),
    About("About", R.drawable.ic_info_outlined, false),
    Setting("Settings", R.drawable.ic_settings_filled, false),
}

/** Works out which rail item matches the screen currently shown. */
fun rkbSelectedDestination(mainScreenKey: TitledNavKey?): RkbDestination? = when (mainScreenKey) {
    null, is NormalNavKey.LauncherMain -> RkbDestination.Home
    is NormalNavKey.AccountManager -> RkbDestination.Account
    is NormalNavKey.VersionsManager -> RkbDestination.Instances
    is NormalNavKey.ModSettings -> RkbDestination.Mods
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

/** Frosted-glass container shared by the pill nav and the right-hand cluster. */
@Composable
private fun RkbGlassPill(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(26.dp)
    Row(
        modifier = modifier
            .clip(shape)
            .background(scheme.surface.copy(alpha = 0.62f), shape)
            .border(1.dp, scheme.primary.copy(alpha = 0.35f), shape)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

@Composable
private fun RkbPillItem(dest: RkbDestination, selected: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(20.dp)
    val tint = if (selected) scheme.onPrimaryContainer else scheme.onSurfaceVariant
    Row(
        modifier = Modifier
            .heightIn(min = 40.dp)
            .clip(shape)
            .background(if (selected) scheme.primaryContainer.copy(alpha = 0.85f) else Color.Transparent, shape)
            .then(if (selected) Modifier.border(1.dp, scheme.primary.copy(alpha = 0.8f), shape) else Modifier)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(painterResource(dest.icon), contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(6.dp))
        Text(dest.label, color = tint, style = MaterialTheme.typography.labelLarge, maxLines = 1, softWrap = false)
    }
}

/**
 * Top bar from the reference designs: brand logo, centered glass pill with the main destinations,
 * and a right-hand cluster (tasks, settings, account menu). The pill scrolls horizontally so it
 * never clips on narrow phones.
 */
@Composable
fun RkbTopNavBar(
    selected: RkbDestination?,
    showBack: Boolean,
    tasksActive: Boolean,
    onBack: () -> Unit,
    onSelect: (RkbDestination) -> Unit,
    onToggleTasks: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val account = com.movtery.zalithlauncher.game.account.AccountsManager.currentAccountFlow
        .collectAsState().value
    var menuOpen by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.displayCutout.union(WindowInsets.statusBars).only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (showBack) {
            IconButton(onClick = onBack, modifier = Modifier.size(44.dp)) {
                Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = "Back", tint = scheme.onSurface)
            }
        }
        Image(
            painter = painterResource(R.drawable.rkb_logo),
            contentDescription = "RKB LAUNCHER",
            modifier = Modifier.height(40.dp)
        )
        RkbGlassPill(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RkbDestination.entries.filter { it.inPill }.forEach { dest ->
                    RkbPillItem(dest, dest == selected) { onSelect(dest) }
                }
            }
        }
        RkbGlassPill {
            IconButton(onClick = onToggleTasks, modifier = Modifier.size(40.dp)) {
                Box {
                    Icon(painterResource(R.drawable.ic_assignment_filled), contentDescription = "Tasks", tint = scheme.onSurface, modifier = Modifier.size(22.dp))
                    if (tasksActive) {
                        Box(
                            Modifier.size(9.dp).align(Alignment.TopEnd)
                                .background(scheme.primary, androidx.compose.foundation.shape.CircleShape)
                        )
                    }
                }
            }
            IconButton(onClick = { onSelect(RkbDestination.Setting) }, modifier = Modifier.size(40.dp)) {
                Icon(
                    painterResource(R.drawable.ic_settings_filled), contentDescription = "Settings",
                    tint = if (selected == RkbDestination.Setting) scheme.primary else scheme.onSurface,
                    modifier = Modifier.size(22.dp)
                )
            }
            Box {
                Row(
                    modifier = Modifier
                        .heightIn(min = 40.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { menuOpen = true }
                        .padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (account != null) {
                        com.movtery.zalithlauncher.ui.screens.content.elements.PlayerFace(
                            modifier = Modifier.clip(androidx.compose.foundation.shape.CircleShape),
                            account = account,
                            avatarSize = 28.dp
                        )
                    } else {
                        Icon(painterResource(R.drawable.ic_person_outlined), contentDescription = "Account", tint = scheme.onSurface, modifier = Modifier.size(24.dp))
                    }
                    Icon(painterResource(R.drawable.ic_keyboard_arrow_down), contentDescription = null, tint = scheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    val name = account?.username ?: "No account"
                    val kind = when {
                        account == null -> "Add an account"
                        account.isLocalAccount() -> "Local account"
                        else -> "Online account"
                    }
                    DropdownMenuItem(
                        text = { Column { Text(name, style = MaterialTheme.typography.labelLarge); Text(kind, style = MaterialTheme.typography.labelSmall, color = scheme.primary) } },
                        onClick = { menuOpen = false; onSelect(RkbDestination.Account) },
                        leadingIcon = { Icon(painterResource(R.drawable.ic_person_outlined), null) }
                    )
                    DropdownMenuItem(
                        text = { Text("About") },
                        onClick = { menuOpen = false; onSelect(RkbDestination.About) },
                        leadingIcon = { Icon(painterResource(R.drawable.ic_info_outlined), null) }
                    )
                }
            }
        }
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
                withStyle(androidx.compose.ui.text.SpanStyle(color = scheme.primary)) { append("RKB ") }
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
