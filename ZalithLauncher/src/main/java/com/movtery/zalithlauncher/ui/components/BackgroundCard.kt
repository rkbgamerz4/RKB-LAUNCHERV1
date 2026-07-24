package com.movtery.zalithlauncher.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.UiComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.ui.screens.content.elements.backgroundGlass
import com.movtery.zalithlauncher.ui.theme.cardColor
import com.movtery.zalithlauncher.ui.theme.cardTitleColor
import com.movtery.zalithlauncher.ui.theme.onCardColor

@Composable
fun BackgroundCard(
    modifier: Modifier = Modifier,
    influencedByBackground: Boolean = true,
    shape: Shape = CardDefaults.shape,
    colors: CardColors = CardDefaults.cardColors(
        containerColor = cardColor(influencedByBackground),
        contentColor = onCardColor()
    ),
    elevation: CardElevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
    blur: Int = AllSettings.backgroundBlur.state,
    border: BorderStroke? = BorderStroke(1.dp, Color(0x2200A8FF)),
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border
    ) {
        Column(
            modifier = Modifier.backgroundGlass(
                blur,
                colors.containerColor,
                influencedByBackground
            ),
            content = content
        )
    }
}

@Composable
fun BackgroundCard(
    modifier: Modifier = Modifier,
    influencedByBackground: Boolean = true,
    shape: Shape = CardDefaults.shape,
    colors: CardColors = CardDefaults.cardColors(
        containerColor = cardColor(influencedByBackground),
        contentColor = onCardColor(),
        disabledContainerColor = cardColor(influencedByBackground)
    ),
    elevation: CardElevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
    blur: Int = AllSettings.backgroundBlur.state,
    border: BorderStroke? = BorderStroke(1.dp, Color(0x2200A8FF)),
    onClick: () -> Unit,
    enabled: Boolean = true,
    content: @Composable @UiComposable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border,
        onClick = onClick,
        enabled = enabled
    ) {
        Column(
            modifier = Modifier.backgroundGlass(
                blur,
                colors.containerColor,
                influencedByBackground
            ),
            content = content
        )
    }
}

@Composable
fun CardTitleLayout(
    modifier: Modifier = Modifier,
    influencedByBackground: Boolean = true,
    alpha: Float = 0.5f,
    color: Color = influencedByBackgroundColor(
        color = cardTitleColor(alpha),
        enabled = influencedByBackground
    ),
    contentColor: Color = onCardColor(),
    blur: Int = AllSettings.backgroundBlur.state,
    content: @Composable @UiComposable ColumnScope.() -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = color,
            contentColor = contentColor
        ) {
            Column(
                modifier = Modifier.backgroundGlass(
                    blur,
                    color,
                    influencedByBackground
                ),
                content = content
            )
        }
        HorizontalDivider(modifier = Modifier.fillMaxWidth())
    }
}