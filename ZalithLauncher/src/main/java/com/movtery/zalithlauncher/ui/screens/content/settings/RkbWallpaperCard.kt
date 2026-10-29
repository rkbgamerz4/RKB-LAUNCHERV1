/*
 * RKB LAUNCHER - Home wallpaper setting. Based on Zalith Launcher 2 (GPLv3).
 */
package com.movtery.zalithlauncher.ui.screens.content.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.ui.components.TitleAndSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** Returns a function that opens the image picker and stores the chosen image as the Home wallpaper. */
@Composable
fun rememberWallpaperPicker(): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val current = AllSettings.homeWallpaper.state
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                runCatching {
                    val dest = File(context.filesDir, "rkb_home_wallpaper_" + System.currentTimeMillis())
                    val input = context.contentResolver.openInputStream(uri) ?: error("cannot open image")
                    input.use { ins -> dest.outputStream().use { out -> ins.copyTo(out) } }
                    if (current.isNotBlank()) File(current).delete()
                    withContext(Dispatchers.Main) { AllSettings.homeWallpaper.save(dest.absolutePath) }
                }
            }
        }
    }
    return { launcher.launch("image/*") }
}

@Composable
fun RkbWallpaperCard(modifier: Modifier = Modifier) {
    val current = AllSettings.homeWallpaper.state
    val pick = rememberWallpaperPicker()

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TitleAndSummary(
                modifier = Modifier.weight(1f),
                title = "Home wallpaper",
                summary = if (current.isBlank()) "Default background. Pick an image to use on the Home screen."
                else "Custom image is set."
            )
            Button(onClick = pick) { Text("Choose") }
            if (current.isNotBlank()) {
                OutlinedButton(onClick = {
                    runCatching { File(current).delete() }
                    AllSettings.homeWallpaper.reset()
                }) { Text("Reset") }
            }
        }
    }
}
