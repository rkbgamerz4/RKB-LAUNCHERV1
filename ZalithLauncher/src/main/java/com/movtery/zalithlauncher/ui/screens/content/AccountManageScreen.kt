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

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.nonInteractiveScrollbar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.context.COPY_LABEL_ACCOUNT_UUID
import com.movtery.zalithlauncher.game.account.Account
import com.movtery.zalithlauncher.game.account.AccountsManager
import com.movtery.zalithlauncher.game.account.auth_server.data.AuthServer
import com.movtery.zalithlauncher.game.account.isAuthServerAccount
import com.movtery.zalithlauncher.game.account.isMicrosoftAccount
import com.moverty.zalithlauncher.game.account.isMicrosoftLogging
import com.movtery.zalithlauncher.game.account.yggdrasil.PlayerProfile
import com.movtery.zalithlauncher.ui.AndroidStringText
import com.movtery.zalithlauncher.ui.androidText
import com.movtery.zalithlauncher.ui.base.BaseScreen
import com.movtery.zalithlauncher.ui.components.BackgroundCard
import com.movtery.zalithlauncher.ui.components.MarqueeText
import com.movtery.zalithlauncher.ui.components.ScalingActionButton
import com.movtery.zalithlauncher.ui.components.ScalingLabel
import com.movtery.zalithlauncher.ui.components.SimpleAlertDialog
import com.movtery.zalithlauncher.ui.components.SimpleEditDialog
import com.movtery.zalithlauncher.ui.components.SimpleListDialog
import com.movtery.zalithlauncher.ui.components.SimpleListItem
import com.movtery.zalithlauncher.ui.components.SkinPreview3D
import com.movtery.zalithlauncher.ui.screens.NormalNavKey
import com.movtery.zalithlauncher.ui.screens.content.elements.AccountItem
import com.moverty.zalithlauncher.ui.screens.content.elements.AccountOperation
import com.moverty.zalithlauncher.ui.screens.content.elements.AccountSkinOperation
import com.moverty.zalithlauncher.ui.screens.content.elements.ChangeSkinDialog
import com.moverty.zalithlauncher.ui.screens.content.elements.LocalLoginDialog
import com.moverty.zalithlauncher.ui.screens.content.elements.LocalLoginOperation
import com.moverty.zalithlauncher.ui.screens.content.elements.LoginMenuDialog
import com.moverty.zalithlauncher.ui.screens.content.elements.LoginMenuOperation
import com.moverty.zalithlauncher.ui.screens.content.elements.MicrosoftLoginOperation
import com.moverty.zalithlauncher.ui.screens.content.elements.MicrosoftLoginTipDialog
import com.moverty.zalithlauncher.ui.screens.content.elements.MicrosoftReloginDialog
import com.moverty.zalithlauncher.ui.screens.content.elements.OtherAccountReloginDialog
import com.moverty.zalithlauncher.ui.screens.content.elements.OtherLoginOperation
import com.moverty.zalithlauncher.ui.screens.content.elements.ServerOperation
import com.moverty.zalithlauncher.utils.animation.swapAnimateDpAsState
import com.moverty.zalithlauncher.utils.copyText
import com.moverty.zalithlauncher.utils.string.getMessageOrToString
import com.movtery.zalithlauncher.viewmodel.AccountManageEffect
import com.moverty.zalithlauncher.viewmodel.AccountManageIntent
import com.movtery.zalithlauncher.viewmodel.AccountManageViewModel
import com.movetry.zalithlauncher.viewmodel.ErrorViewModel
import com.movtery.zalithlauncher.viewmodel.EventViewModel
import com.movetry.zalithlauncher.viewmodel.LocalBackgroundViewModel
import com.movetry.zalithlauncher.viewmodel.ScreenBackStackViewModel

/**
 * 封装账号界面 UI 交互的回调函数
 *
 * @property onIntent 发送 MVI Intent 到 ViewModel
 * @property openLink 打开外部链接
 * @property backToMainScreen 返回主界面
 * @property navigateToWeb 导航到应用内浏览器界面
 * @property checkIfInWebScreen 检查当前是否在浏览器界面中（用于微软登录逻辑判断）
 * @property formatError 格式化异常为本地化字符串
 * @property submitError 提交错误到全局错误展示系统
 */
private data class AccountActions(
    val onIntent: (AccountManageIntent) -> Unit,
    val openLink: (url: String) -> Unit,
    val backToMainScreen: () -> Unit,
    val navigateToWeb: (url: String) -> Unit,
    val checkIfInWebScreen: () -> Boolean,
    val formatError: (Throwable) -> AndroidStringText,
    val submitError: (ErrorViewModel.ThrowableMessage) -> Unit,
)

/**
 * 进入账号管理器时，可附加的打开登录菜单选项
 */
enum class FirstLoginMenu {
    /** 不打开菜单 */
    NONE,
    /** 打开微软登录菜单 */
    MICROSOFT,
    /** 打开总登录菜单 */
    NORMAL
}

/**
 * 账号管理主界面
 *
 * @param backStackViewModel 屏幕堆栈管理器
 * @param backToMainScreen 返回主屏幕的回调
 * @param openLink 外部链接跳转回调
 * @param submitError 全局错误提交回调
 */
@Composable
fun AccountManageScreen(
    key: NormalNavKey.AccountManager,
    backStackViewModel: ScreenBackStackViewModel,
    backToMainScreen: () -> Unit,
    openLink: (url: String) -> Unit,
    submitError: (ErrorViewModel.ThrowableMessage) -> Unit,
    eventViewModel: EventViewModel
) {
    val viewModel: AccountManageViewModel = hiltViewModel { factory: AccountManageViewModel.Factory ->
        factory.create(eventViewModel)
    }

    val loginUiState by viewModel.loginUiState.collectAsStateWithLifecycle()
    val profileUiState by viewModel.profileUiState.collectAsStateWithLifecycle()
    val operationUiState by viewModel.operationUiState.collectAsStateWithLifecycle()

    val actions = remember(
        viewModel,
        backToMainScreen,
        openLink,
        backStackViewModel,
        submitError
    ) {
        AccountActions(
            onIntent = viewModel::onIntent,
            openLink = openLink,
            backToMainScreen = backToMainScreen,
            navigateToWeb = { url -> backStackViewModel.mainScreen.backStack.navigateToWeb(url) },
            checkIfInWebScreen = { backStackViewModel.mainScreen.currentKey is NormalNavKey.WebScreen },
            formatError = { th -> viewModel.formatAccountError(th) },
            submitError = submitError,
        )
    }

    LaunchedEffect(Unit) {
        when (key.loginMenu) {
            FirstLoginMenu.NONE -> {}
            FirstLoginMenu.MICROSOFT -> {
                actions.onIntent(AccountManageIntent.UpdateMicrosoftLoginOp(MicrosoftLoginOperation.Tip))
            }
            FirstLoginMenu.NORMAL -> {
                actions.onIntent(AccountManageIntent.UpdateLoginMenuOp(LoginMenuOperation.Login))
            }
        }

        viewModel.effect.collect { effect ->
            when (effect) {
                is AccountManageEffect.ShowError -> {
                    submitError(ErrorViewModel.ThrowableMessage(effect.title, effect.message))
                }
            }
        }
    }

    BaseScreen(
        screenKey = key,
        currentKey = backStackViewModel.mainScreen.currentKey
    ) { isVisible ->
        AccountManageContent(
            isVisible = isVisible,
            loginUiState = loginUiState,
            profileUiState = profileUiState,
            operationUiState = operationUiState,
            actions = actions
        )
    }
}

/**
 * 账号管理界面的实际内容布局
 */
@Composable
private fun AccountManageContent(
    isVisible: Boolean,
    loginUiState: AccountManageViewModel.LoginUiState,
    profileUiState: AccountManageViewModel.ProfileUiState,
    operationUiState: AccountManageViewModel.OperationUiState,
    actions: AccountActions
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        ActionsLayout(
            isVisible = isVisible,
            modifier = Modifier
                .fillMaxHeight()
                .padding(all = 12.dp)
                .weight(3f),
            currentAccount = profileUiState.currentAccount,
            actions = actions
        )

        AccountsLayout(
            isVisible = isVisible,
            modifier = Modifier
                .fillMaxHeight()
                .padding(top = 12.dp, end = 12.dp, bottom = 12.dp)
                .weight(7f),
            accounts = profileUiState.accounts,
            currentAccount = profileUiState.currentAccount,
            accountOperation = operationUiState.accountOp,
            accountSkinOperation = operationUiState.accountSkinOp,
            accountSkinDialogState = operationUiState.accountSkinDialogState,
            accountCapes = profileUiState.accountCapeOpMap,
            actions = actions
        )
    }

    LoginMenuOperation(loginUiState.menuOp, actions, profileUiState.authServers)
    MicrosoftLoginOperation(loginUiState.microsoftOp, actions)
    LocalLoginOperation(loginUiState.localOp, actions)
    OtherLoginOperation(loginUiState.otherOp, actions)
    ServerTypeOperation(operationUiState.serverOp, actions)
}

/**
 * 左侧登录方式菜单组件
 */
@Composable
private fun ActionsLayout(
    isVisible: Boolean,
    modifier: Modifier = Modifier,
    currentAccount: Account?,
    actions: AccountActions
) {
    val xOffset by swapAnimateDpAsState(
        targetValue = (-40).dp,
        swapIn = isVisible,
        isHorizontal = true
    )

    Column(
        modifier = modifier
            .offset { IntOffset(x = xOffset.roundToPx(), y = 0) }
            .fillMaxHeight()
    ) {
        // 玩家模型预览
        val refreshWardrobe by AccountsManager.refreshWardrobe.collectAsStateWithLifecycle()
        val skinFile = remember(currentAccount, refreshWardrobe) {
            currentAccount?.getSkinFile()?.takeIf { it.exists() }
        }
        val capeFile = remember(currentAccount, refreshWardrobe) {
            currentAccount?.getCapeFile()?.takeIf { it.exists() }
        }
        SkinPreview3D(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            skinFile = skinFile,
            capeFile = capeFile,
            modelType = currentAccount?.skinModelType,
            refreshKey = listOf(currentAccount?.uniqueUUID, refreshWardrobe, skinFile?.lastModified() ?: 0L, capeFile?.lastModified() ?: 0L)
        )

        // 添加账号
        ScalingActionButton(
            modifier = Modifier
                .fillMaxWidth(),
            onClick = {
                actions.onIntent(AccountManageIntent.UpdateLoginMenuOp(LoginMenuOperation.Login))
            }
        ) {
            MarqueeText(text = stringResource(R.string.account_add_new_account))
        }
    }
}

/* Remaining AccountManageScreen functions unchanged from original... */