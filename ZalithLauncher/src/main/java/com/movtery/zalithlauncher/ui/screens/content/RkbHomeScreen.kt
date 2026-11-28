/*
 * RKB LAUNCHER - reference-style Home (wallpaper banner, version picker, PLAY, RAM/renderer card).
 * Based on Zalith Launcher 2 (GPLv3).
 */
package com.movtery.zalithlauncher.ui.screens.content

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import com.movtery.zalithlauncher.R
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.movtery.zalithlauncher.game.renderer.Renderers
import com.movtery.zalithlauncher.game.version.installed.VersionsManager
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.utils.platform.getFreeMemory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

@Composable
fun RkbHomeScreen(
    onPlay: () -> Unit,
    onOpenVersions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val version = VersionsManager.currentVersion.collectAsState().value
    val info = version?.getVersionInfo()
    val loaderName = info?.primaryLoader?.loader?.displayName?.takeIf { it.isNotBlank() }
    val mcVersion = info?.minecraftVersion

    BoxWithConstraints(
        modifier = modifier.fillMaxSize()
    ) {
        val wide = maxWidth >= 600.dp
        // bottom-left scrim keeps the title readable on any wallpaper
        Box(
            Modifier.fillMaxSize().background(
                Brush.linearGradient(listOf(Color(0xCC05070C), Color.Transparent), start = Offset(0f, Float.POSITIVE_INFINITY), end = Offset(Float.POSITIVE_INFINITY, 0f))
            )
        )

        // Upcoming features: right column in landscape, a row above the title in portrait.
        if (wide) {
            Column(
                Modifier.align(Alignment.BottomEnd).padding(20.dp).width(230.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                RkbUpcoming.entries.forEach { RkbComingSoonCard(it, Modifier.fillMaxWidth()) }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp)
                .widthIn(max = if (wide) 420.dp else 600.dp)
        ) {
            if (!wide) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RkbUpcoming.entries.forEach { RkbComingSoonCard(it, Modifier.weight(1f)) }
                }
                Spacer(Modifier.height(18.dp))
            }
            Text(
                text = buildAnnotatedString {
                    if (mcVersion != null) {
                        append(mcVersion)
                        if (loaderName != null) {
                            append(" ")
                            withStyle(SpanStyle(color = scheme.primary)) { append(loaderName) }
                        }
                    } else append("No instance")
                },
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = version?.getVersionName() ?: "Tap the options button to choose or create one",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onPlay,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = scheme.primaryContainer,
                        contentColor = scheme.onPrimaryContainer
                    ),
                    border = BorderStroke(1.dp, scheme.primary.copy(alpha = 0.7f))
                ) {
                    Icon(painterResource(R.drawable.ic_play_arrow_filled), null, Modifier.size(24.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Launch", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                OutlinedIconButton(
                    onClick = onOpenVersions,
                    modifier = Modifier.size(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, scheme.outline)
                ) {
                    Icon(painterResource(R.drawable.ic_settings_filled), contentDescription = "Choose or manage instance", tint = Color.White)
                }
            }
        }
    }
}
