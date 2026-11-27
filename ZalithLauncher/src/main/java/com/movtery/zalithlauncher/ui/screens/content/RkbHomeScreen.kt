/*
 * RKB LAUNCHER - reference-style Home (wallpaper banner, version picker, PLAY, RAM/renderer card).
 * Based on Zalith Launcher 2 (GPLv3).
 * Redesigned to match reference images: top pill navigation, glass background, wallpaper system.
 */
package com.movtery.zalithlauncher.ui.screens.content

import androidx.compose.foundation.BorderStroke
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
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val version = VersionsManager.currentVersion.collectAsState().value
    val wallpaperPath = AllSettings.homeWallpaper.state
    val wallpaperFile = remember(wallpaperPath) {
        wallpaperPath.takeIf { it.isNotBlank() }?.let { File(it) }?.takeIf { it.exists() }
    }

    var freeRam by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(Unit) {
        while (true) {
            freeRam = withContext(Dispatchers.IO) { runCatching { getFreeMemory(context) }.getOrNull() }
            delay(5000)
        }
    }
    val rendererName = remember(AllSettings.renderer.state) {
        runCatching { Renderers.getCurrentRenderer().getRendererName() }.getOrNull()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            // Glass background with blue-tinted wallpaper effect
            .background(
                // Layer 1: Deep blue-tinted glass gradient
                Brush.linearGradient(
                    listOf(
                        Color(0xFF0B1E45),
                        Color(0xFF1A2D55),
                        Color(0xFF0A0C1A)
                    ),
                    start = androidx.compose.ui.graphics.IntOffset(0, 0),
                    end = androidx.compose.ui.graphics.IntOffset(0, Int.MaxValue)
                ),
                // Layer 2: Wallpaper if available, blended with glass
                if (wallpaperFile != null) {
                    AsyncImage(
                        model = androidx.compose.ui.platform rememberImageLocalUri(
                            androidx.compose.ui.platform.LocalContext.current,
                            androidx.compose.ui.platform.AndroidContentProviderLocalUri(
                                androidx.compose.ui.platform.LocalContext.current
                            )
                        ),
                        uri = androidx.compose.ui.platform.AndroidContentProviderLocalUri(
                            androidx.compose.ui.platform.LocalContext.current
                        ),
                        placeholder = painterResource(R.drawable.img_launcher),
                        contentDescription = "Home wallpaper",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        // Fallback: simple Minecraft-like pattern
                        val wallpaperSize = size
                        val centerX = wallpaperSize.width / 2f
                        val centerY = wallpaperSize.height / 2f
                        // Draw simple grass block pattern
                        val blockSize = 40f
                        val paint = androidx.compose.ui paint.Paint()
                        paint.color = Color(0xFF155A2A).copy(alpha = 0.02f)
                        for (x in (0f..wallpaperSize.width).step blockSize) {
                            for (y in (0f..wallpaperSize.height).step blockSize) {
                                drawRect(
                                    androidx.compose.ui.draw.Rectangle(
                                        x = x,
                                        y = y,
                                        width = blockSize,
                                        height = blockSize
                                    ),
                                    paint = paint
                                )
                            }
                        }
                    }
                }
            )
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(24.dp)
                .widthIn(max = 340.dp)
        ) {
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = scheme.primary)) { append("RKB ") }
                    append("LAUNCHER")
                },
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
            Text(
                text = "PLAY  \u2022  EXPLORE  \u2022  CREATE",
                style = MaterialTheme.typography.labelMedium,
                color = scheme.secondary,
                letterSpacing = 3.sp
            )
            Spacer(Modifier.height(14.dp))

            Surface(
                modifier = Modifier.fillMaxWidth().clickable(onOpenVersions),
                shape = RoundedCornerShape(14.dp),
                color = scheme.surfaceContainerHigh.copy(alpha = 0.95f),
                border = BorderStroke(1.dp, scheme.outline)
            ) {
                Column(Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                    Text(
                        text = version?.getVersionName() ?: "No instance selected",
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1
                    )
                    Text(
                        text = version?.getVersionInfo()?.minecraftVersion ?: "Tap to choose or create one",
                        style = MaterialTheme.typography.labelMedium,
                        color = scheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = onPlay,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = scheme.primaryContainer,
                    contentColor = scheme.onPrimaryContainer
                )
            ) {
                Text(
                    "\u25B6  PLAY",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        }

        // Bottom-right: RAM / Renderer card
        Surface(
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
            shape = RoundedCornerShape(14.dp),
            color = scheme.surface.copy(alpha = 0.85f),
            border = BorderStroke(1.dp, scheme.outlineVariant)
        ) {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF2FE37A)))
                    Spacer(Modifier.size(8.dp))
                    val ram = freeRam?.let { String.format(Locale.US, "%.1f GB", it / 1024.0 / 1024.0 / 1024.0) } ?: "..."
                    Text("RAM free $ram", style = MaterialTheme.typography.labelMedium)
                }
                Text(
                    text = "Renderer ${rendererName ?: "-"}",
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.secondary
                )
            }
        }
    }
}