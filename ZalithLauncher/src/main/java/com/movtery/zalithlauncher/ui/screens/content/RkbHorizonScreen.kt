/*
 * RKB LAUNCHER - Rust Edition (Horizon) panel. Shows verified compatibility facts for THIS device and
 * starts the separate native Horizon app if the user installed it. No fake downloads.
 */
package com.movtery.zalithlauncher.ui.screens.content

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.utils.RkbHorizon
import java.util.Locale

private val Orange = Color(0xFFFF6A1A)

private fun totalRamGb(context: Context): Double {
    val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return 0.0
    val info = ActivityManager.MemoryInfo()
    am.getMemoryInfo(info)
    return info.totalMem / 1024.0 / 1024.0 / 1024.0
}

@Composable
fun RkbHorizonScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    var installed by remember { mutableStateOf(RkbHorizon.installedVersion(context)) }
    var launchFailed by remember { mutableStateOf(false) }
    val abi = Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown"
    val ram = remember { totalRamGb(context) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row {
            Text("RUST ", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
            Text("EDITION", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = Orange)
        }
        Text("Minecraft Java 26.3 rewritten in Rust (Horizon)", color = scheme.onSurfaceVariant)

        InfoCard(
            title = "Android support: what is verified",
            lines = listOf(
                "Official Horizon: Linux x86-64, macOS and Windows x64 only. There is no official Android or ARM64-Linux build.",
                "A third-party, UNOFFICIAL Android port exists (arm64, Android 8+, Vulkan with GLES 3 fallback). Its author tested it on one phone (Galaxy S25, Adreno 830); other devices are untested.",
                "It runs as its own native app (no Java). RKB cannot embed it: it has its own window, storage and downloader.",
                "It needs your own legal copy of Minecraft Java 26.3. It downloads about 525 MB itself, but one small archive (horizon-derived.zip) must be built once on a PC with Java 25 and Rust, then copied to the phone."
            )
        )
        InfoCard(
            title = "This device",
            lines = listOf(
                "CPU ABI: $abi" + if (abi == "arm64-v8a") " (matches the port's arm64 requirement)" else " (the port needs arm64-v8a)",
                "Total RAM: " + String.format(Locale.US, "%.1f GB", ram) + " (the port uses about 2.2 GB; 4 GB or more is advised)",
                "Android " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + "); the port needs Android 8.0 or newer",
                "GPU/Vulkan performance is not tested here; expect it to depend heavily on the GPU."
            )
        )
        InfoCard(
            title = "Horizon app status",
            lines = listOf(
                if (installed != null) "Installed: version $installed" else "Not installed on this phone.",
                "RKB does not download or install it for you. Get it only from a source you trust, and read its page first."
            )
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                enabled = installed != null,
                colors = ButtonDefaults.buttonColors(containerColor = Orange),
                onClick = { launchFailed = !RkbHorizon.launch(context) }
            ) { Text("Launch Horizon") }
            OutlinedButton(onClick = { installed = RkbHorizon.installedVersion(context) }) { Text("Re-check") }
        }
        if (launchFailed) {
            Text("Horizon could not be started. Is it still installed?", color = scheme.error)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { RkbHorizon.openUrl(context, RkbHorizon.UNOFFICIAL_RELEASES) }) { Text("Unofficial Android APK page") }
            OutlinedButton(onClick = { RkbHorizon.openUrl(context, RkbHorizon.OFFICIAL_SITE) }) { Text("Official site") }
        }
        OutlinedButton(onClick = { RkbHorizon.openUrl(context, RkbHorizon.OFFICIAL_REPO) }) { Text("Official source code") }
        Text(
            "RKB LAUNCHER is not affiliated with Horizon, Mojang or Microsoft. Horizon runs outside RKB's Java pipeline.",
            style = MaterialTheme.typography.labelSmall,
            color = scheme.onSurfaceVariant
        )
    }
}

@Composable
private fun InfoCard(title: String, lines: List<String>) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, Orange.copy(alpha = 0.5f))
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            lines.forEach { Text("- $it", style = MaterialTheme.typography.bodySmall) }
        }
    }
}
