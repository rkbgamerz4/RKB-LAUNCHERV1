/*
 * Zalith Launcher 2
 * Copyright (C) 2025 MovTery <movtery228@qq.com> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/gpl-3.0.txt>.
 */

package com.movtery.zalithlauncher.ui.screens.content

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.download.assets.platform.PlatformClasses
import com.movtery.zalithlauncher.game.version.installed.Version
import com.movtery.zalithlauncher.game.version.installed.VersionsManager
import com.movtery.zalithlauncher.notification.NotificationManager
import com.movtery.zalithlauncher.ui.base.BaseScreen
import com.movtery.zalithlauncher.ui.base.VersionViewModelStoreScope
import com.movtery.zalithlauncher.ui.screens.NestedNavKey
import com.movtery.zalithlauncher.ui.screens.NormalNavKey
import com.movtery.zalithlauncher.ui.screens.TitledNavKey
import com.movtery.zalithlauncher.ui.screens.content.versions.ModifyVersionScreen
import com.movtery.zalithlauncher.ui.screens.content.versions.ModsManagerScreen
import com.movtery.zalithlauncher.ui.screens.content.versions.ResourcePackManageScreen
import com.movtery.zalithlauncher.ui.screens.content.versions.SavesManagerScreen
import com.movtery.zalithlauncher.ui.screens.content.versions.ScreenshotsManagerScreen
import com.movtery.zalithlauncher.ui.screens.content.versions.ServerListScreen
import com.movtery.zalithlauncher.ui.screens.content.versions.ShadersManagerScreen
import com.movtery.zalithlauncher.ui.screens.content.versions.VersionConfigScreen
import com.movtery.zalithlauncher.ui.screens.content.versions.VersionOverViewScreen
import com.movtery.zalithlauncher.ui.screens.navigateOnce
import com.movtery.zalithlauncher.ui.screens.onBack
import com.movtery.zalithlauncher.ui.screens.rememberTransitionSpec
import com.movtery.zalithlauncher.ui.screens.main.RkbTabChip
import com.movtery.zalithlauncher.viewmodel.ErrorViewModel
import com.movtery.zalithlauncher.viewmodel.EventViewModel
import com.movtery.zalithlauncher.viewmodel.ModifyOperation
import com.movtery.zalithlauncher.viewmodel.ModifyPayload
import com.movtery.zalithlauncher.viewmodel.ModifyVersionViewModel
import com.movtery.zalithlauncher.viewmodel.ScreenBackStackViewModel
import com.movtery.zalithlauncher.viewmodel.sendToast

@Composable
fun VersionSettingsScreen(
    key: NestedNavKey.VersionSettings,
    modifyViewModel: ModifyVersionViewModel,
    backScreenViewModel: ScreenBackStackViewModel,
    backToMainScreen: () -> Unit,
    onExportModpack: () -> Unit,
    eventViewModel: EventViewModel,
    submitError: (ErrorViewModel.ThrowableMessage) -> Unit
) {
    val context = LocalContext.current

    val cBackToMainScreen by rememberUpdatedState(backToMainScreen)
    DisposableEffect(key) {
        val listener = object : suspend () -> Unit {
            override suspend fun invoke() {
                cBackToMainScreen()
            }
        }
        VersionsManager.registerListener(listener)
        onDispose {
            VersionsManager.unregisterListener(listener)
        }
    }

    VersionViewModelStoreScope(version = key.version) {
        BaseScreen(
            screenKey = key,
            currentKey = backScreenViewModel.mainScreen.currentKey
        ) { _ ->
            Column(modifier = Modifier.fillMaxSize()) {
                RkbInstanceTabs(
                    backStack = key.backStack,
                    versionsScreenKey = key.currentKey
                )

                RkbInstancePages(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    key = key,
                    modifyViewModel = modifyViewModel,
                    backScreenViewModel = backScreenViewModel,
                    versionsScreenKey = key.currentKey,
                    onCurrentKeyChange = { newKey ->
                        key.currentKey = newKey
                    },
                    backToMainScreen = backToMainScreen,
                    onExport = onExportModpack,
                    version = key.version,
                    onModify = { payload ->
                        if (modifyViewModel.installOperation !is ModifyOperation.None) {
                            //不是待修改状态，拒绝此次修改
                            return@RkbInstancePages
                        }
                        if (!NotificationManager.checkNotificationEnabled(context)) {
                            //警告通知权限
                            modifyViewModel.installOperation = ModifyOperation.WarningForNotification(payload)
                        } else {
                            modifyViewModel.installOperation = ModifyOperation.Confirm(payload)
                        }
                    },
                    eventViewModel = eventViewModel,
                    submitError = submitError
                )
            }
        }
    }
}

private data class RkbInstanceTab(val key: TitledNavKey, val icon: Int, val label: Int)

private val instanceTabs = listOf(
    RkbInstanceTab(NormalNavKey.Versions.OverView, R.drawable.ic_dashboard_outlined, R.string.versions_settings_overview),
    RkbInstanceTab(NormalNavKey.Versions.Config, R.drawable.ic_build_outlined, R.string.versions_settings_config),
    RkbInstanceTab(NormalNavKey.Versions.ModifyVersion, R.drawable.ic_edit_outlined, R.string.versions_modify_version),
    RkbInstanceTab(NormalNavKey.Versions.ModsManager, R.drawable.ic_extension_outlined, R.string.mods_manage),
    RkbInstanceTab(NormalNavKey.Versions.SavesManager, R.drawable.ic_public, R.string.saves_manage),
    RkbInstanceTab(NormalNavKey.Versions.ResourcePackManager, R.drawable.ic_format_paint_outlined, R.string.resource_pack_manage),
    RkbInstanceTab(NormalNavKey.Versions.ShadersManager, R.drawable.ic_lightbulb, R.string.shader_pack_manage),
    RkbInstanceTab(NormalNavKey.Versions.ScreenshotsManager, R.drawable.ic_photo_library_outlined, R.string.screenshots_manage),
    RkbInstanceTab(NormalNavKey.Versions.ServerList, R.drawable.ic_dns_outlined, R.string.servers_list),
)

@Composable
private fun RkbInstanceTabs(
    backStack: NavBackStack<TitledNavKey>,
    versionsScreenKey: TitledNavKey?
) {
    val scrollState = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        instanceTabs.forEach { tab ->
            RkbTabChip(
                selected = versionsScreenKey === tab.key,
                onClick = { backStack.navigateOnce(tab.key) }
            ) {
                Icon(painter = painterResource(tab.icon), contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(
                    text = stringResource(tab.label),
                    maxLines = 1,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
private fun RkbInstancePages(
    modifier: Modifier = Modifier,
    key: NestedNavKey.VersionSettings,
    modifyViewModel: ModifyVersionViewModel,
    backScreenViewModel: ScreenBackStackViewModel,
    versionsScreenKey: TitledNavKey?,
    onCurrentKeyChange: (TitledNavKey?) -> Unit,
    backToMainScreen: () -> Unit,
    onExport: () -> Unit,
    version: Version,
    onModify: (ModifyPayload) -> Unit,
    eventViewModel: EventViewModel,
    submitError: (ErrorViewModel.ThrowableMessage) -> Unit
) {
    val mainScreenKey = backScreenViewModel.mainScreen.currentKey

    val backStack = key.backStack
    val stackTopKey = backStack.lastOrNull()
    LaunchedEffect(stackTopKey) {
        onCurrentKeyChange(stackTopKey)
    }

    if (backStack.isNotEmpty()) {
        NavDisplay(
            backStack = backStack,
            modifier = modifier,
            onBack = {
                onBack(backStack)
            },
            transitionSpec = rememberTransitionSpec(),
            popTransitionSpec = rememberTransitionSpec(),
            entryProvider = entryProvider {
                entry<NormalNavKey.Versions.OverView> {
                    VersionOverViewScreen(
                        mainScreenKey = mainScreenKey,
                        versionsScreenKey = versionsScreenKey,
                        backToMainScreen = backToMainScreen,
                        onExport = onExport,
                        version = version,
                        eventViewModel = eventViewModel,
                        submitError = submitError
                    )
                }
                entry<NormalNavKey.Versions.Config> {
                    VersionConfigScreen(
                        mainScreenKey = mainScreenKey,
                        versionsScreenKey = versionsScreenKey,
                        version = version,
                        backToMainScreen = backToMainScreen,
                        onCheckVulkan = { version ->
                            eventViewModel.sendEvent(
                                EventViewModel.Event.VulkanCheck(version)
                            )
                        },
                        showToast = { text ->
                            eventViewModel.sendToast(text)
                        },
                        submitError = submitError
                    )
                }
                entry<NormalNavKey.Versions.ModifyVersion> {
                    ModifyVersionScreen(
                        viewModel = modifyViewModel,
                        mainScreenKey = mainScreenKey,
                        versionsScreenKey = versionsScreenKey,
                        version = version,
                        eventViewModel = eventViewModel,
                        onModify = onModify
                    )
                }
                entry(NormalNavKey.Versions.ModsManager) {
                    ModsManagerScreen(
                        mainScreenKey = mainScreenKey,
                        versionsScreenKey = versionsScreenKey,
                        version = version,
                        backToMainScreen = backToMainScreen,
                        swapToDownload = {
                            backScreenViewModel.navigateToDownload(
                                targetScreen = backScreenViewModel.downloadModScreen
                            )
                        },
                        onSwapMoreInfo = { projectId, platform ->
                            backScreenViewModel.mainScreen.removeAndNavigateTo(
                                NestedNavKey.AssetInfo::class,
                                NestedNavKey.AssetInfo(platform, projectId, PlatformClasses.MOD)
                            )
                        },
                        eventViewModel = eventViewModel,
                        submitError = submitError
                    )
                }
                entry<NormalNavKey.Versions.SavesManager> {
                    SavesManagerScreen(
                        mainScreenKey = mainScreenKey,
                        versionsScreenKey = versionsScreenKey,
                        version = version,
                        backToMainScreen = backToMainScreen,
                        swapToDownload = {
                            backScreenViewModel.navigateToDownload(
                                targetScreen = backScreenViewModel.downloadSavesScreen
                            )
                        },
                        onQuickPlay = { version, saveName ->
                            eventViewModel.sendEvent(
                                EventViewModel.Event.Launch.PlaySave(
                                    version = version,
                                    saveName = saveName
                                )
                            )
                        },
                        submitError = submitError
                    )
                }
                entry<NormalNavKey.Versions.ResourcePackManager> {
                    ResourcePackManageScreen(
                        mainScreenKey = mainScreenKey,
                        versionsScreenKey = versionsScreenKey,
                        version = version,
                        backToMainScreen = backToMainScreen,
                        swapToDownload =  {
                            backScreenViewModel.navigateToDownload(
                                targetScreen = backScreenViewModel.downloadResourcePackScreen
                            )
                        },
                        submitError = submitError
                    )
                }
                entry<NormalNavKey.Versions.ShadersManager> {
                    ShadersManagerScreen(
                        mainScreenKey = mainScreenKey,
                        versionsScreenKey = versionsScreenKey,
                        version = version,
                        backToMainScreen = backToMainScreen,
                        swapToDownload = {
                            backScreenViewModel.navigateToDownload(
                                targetScreen = backScreenViewModel.downloadShadersScreen
                            )
                        },
                        submitError = submitError
                    )
                }
                entry<NormalNavKey.Versions.ScreenshotsManager> {
                    ScreenshotsManagerScreen(
                        mainScreenKey = mainScreenKey,
                        versionsScreenKey = versionsScreenKey,
                        version = version,
                        backToMainScreen = backToMainScreen,
                        eventViewModel = eventViewModel,
                        submitError = submitError
                    )
                }
                entry<NormalNavKey.Versions.ServerList> {
                    ServerListScreen(
                        mainScreenKey = mainScreenKey,
                        versionsScreenKey = versionsScreenKey,
                        version = version,
                        onQuickPlay = { version, address ->
                            eventViewModel.sendEvent(
                                EventViewModel.Event.Launch.PlayServer(
                                    version = version,
                                    address = address
                                )
                            )
                        },
                        backToMainScreen = backToMainScreen,
                    )
                }
            }
        )
    } else {
        Box(modifier)
    }
}
