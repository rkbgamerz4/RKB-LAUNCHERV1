/*
 * RKB LAUNCHER - About page (replaces the inherited Zalith About page in the UI).
 * Everything shown is read from the running app (version, renderer, bundled runtimes) or is plain text.
 * Open-source credits and license links are kept; nothing is claimed that has not been verified.
 */
package com.movtery.zalithlauncher.ui.screens.content.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.BuildConfig
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.renderer.Renderers
import com.movtery.zalithlauncher.library.libraryData
import com.movtery.zalithlauncher.ui.base.BaseScreen
import com.movtery.zalithlauncher.ui.screens.NestedNavKey
import com.movtery.zalithlauncher.ui.screens.NormalNavKey
import com.movtery.zalithlauncher.ui.screens.TitledNavKey

private const val URL_GITHUB = "https://github.com/rkbgamerz4/RKBLAUNCHER"
private const val URL_YOUTUBE = "https://youtube.com/@rkblauncher?si=rpsAboww30Eaotf_"
private const val URL_DISCORD = "https://discord.gg/qPkTcFp55x"

@Composable
fun RkbAboutScreen(
    key: NestedNavKey.Settings,
    settingsScreenKey: TitledNavKey?,
    mainScreenKey: TitledNavKey?,
    openLicense: (raw: Int) -> Unit,
    openLink: (url: String) -> Unit
) {
    BaseScreen(
        Triple(key, mainScreenKey, false),
        Triple(NormalNavKey.Settings.AboutInfo, settingsScreenKey, false)
    ) { _ ->
        val context = LocalContext.current
        val renderer = remember {
            runCatching { Renderers.getCurrentRenderer().getRendererName() }.getOrNull()
        }
        val javaRuntimes = remember {
            runCatching {
                context.assets.list("runtimes")?.filter { it.startsWith("jre-") }
                    ?.map { it.removePrefix("jre-") }?.sortedBy { it.toIntOrNull() ?: 0 }
            }.getOrNull().orEmpty()
        }
        val buildCode = remember {
            runCatching {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0).versionCode
            }.getOrNull()
        }

        BoxWithConstraints(Modifier.fillMaxSize()) {
            val wide = maxWidth > maxHeight
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AboutHeader(version = BuildConfig.VERSION_NAME, build = buildCode)
                if (wide) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            EnginesPanel(renderer = renderer, runtimes = javaRuntimes)
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            LinksPanel(openLink)
                            CreditsPanel(openLicense, openLink)
                        }
                    }
                } else {
                    EnginesPanel(renderer = renderer, runtimes = javaRuntimes)
                    LinksPanel(openLink)
                    CreditsPanel(openLicense, openLink)
                }
            }
        }
    }
}

@Composable
private fun AboutHeader(version: String, build: Int?) {
    val scheme = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(
            painter = painterResource(R.drawable.rkb_logo),
            contentDescription = "RKB",
            modifier = Modifier.height(56.dp)
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = scheme.primary)) { append("RKB ") }
                    append("LAUNCHER")
                },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Version $version" + (build?.let { "  (build $it)" } ?: ""),
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant
            )
        }
    }
    Text(
        "A launcher for Minecraft: Java Edition on Android. It installs and runs Java Edition versions, Fabric, Forge, NeoForge and Quilt instances and modpacks, with its own interface on top of the Zalith Launcher 2 and PojavLauncher technology.",
        style = MaterialTheme.typography.bodyMedium
    )
    Text("Developer: RKB GAMERZ", style = MaterialTheme.typography.titleSmall)
}

@Composable
private fun Panel(title: String, content: @Composable () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = scheme.surfaceContainer,
        border = BorderStroke(1.dp, scheme.outlineVariant)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title.uppercase(), style = MaterialTheme.typography.labelMedium, color = scheme.primary)
            content()
        }
    }
}

@Composable
private fun EnginesPanel(renderer: String?, runtimes: List<String>) {
    val scheme = MaterialTheme.colorScheme
    Panel("Java engine (default)") {
        Text("Runs Minecraft: Java Edition through the bundled Java runtimes and OpenGL renderers.", style = MaterialTheme.typography.bodyMedium)
        Text(
            "Bundled Java runtimes: " + (if (runtimes.isEmpty()) "not detected" else runtimes.joinToString(", ")),
            style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant
        )
        Text(
            "Active renderer: " + (renderer ?: "not selected yet"),
            style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant
        )
        Text(
            "Minecraft versions: any Java Edition version the launcher can download. RKB has not certified individual versions.",
            style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant
        )
    }
    Panel("Rust Edition (Horizon)") {
        Text("Optional second engine, not part of this build.", style = MaterialTheme.typography.bodyMedium)
        Text(
            "Horizon is Minecraft: Java Edition 26.3 rewritten in Rust. It targets 26.3 only, has no Java mod or loader support, and has no official Android release. A separate, unofficial Android port exists; it has not been verified in RKB.",
            style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant
        )
        Text(
            "Engine version, Minecraft version and launcher version are three separate things.",
            style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant
        )
    }
}

@Composable
private fun LinksPanel(openLink: (String) -> Unit) {
    Panel("Official links") {
        LinkRow("GitHub", URL_GITHUB, openLink)
        HorizontalDivider()
        LinkRow("YouTube", URL_YOUTUBE, openLink)
        HorizontalDivider()
        LinkRow("Discord", URL_DISCORD, openLink)
    }
}

@Composable
private fun LinkRow(label: String, url: String, openLink: (String) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth().clickable { openLink(url) }.padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.titleSmall)
            Text(url.removePrefix("https://"), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant, maxLines = 1)
        }
        Text("Open", style = MaterialTheme.typography.labelLarge, color = scheme.primary)
    }
}

@Composable
private fun CreditsPanel(openLicense: (Int) -> Unit, openLink: (String) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    var showLibs by remember { mutableStateOf(false) }
    Panel("Open source and credits") {
        Text("RKB LAUNCHER is open source (GPL-3.0). It builds on:", style = MaterialTheme.typography.bodyMedium)
        Text("- Zalith Launcher 2 by MovTery and contributors (GPL-3.0)", style = MaterialTheme.typography.bodySmall)
        Text("- PojavLauncher by PojavLauncherTeam (LGPL-3.0)", style = MaterialTheme.typography.bodySmall)
        Text("- Horizon by the Horizon project (MIT), referenced for the optional Rust Edition", style = MaterialTheme.typography.bodySmall)
        Text("- Discord Social SDK, used for Discord Rich Presence", style = MaterialTheme.typography.bodySmall)
        Text("Minecraft is a trademark of Mojang AB / Microsoft. RKB LAUNCHER is not affiliated with them.", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
        OutlinedButton(onClick = { openLicense(R.raw.lgpl_3_license) }) { Text("LGPL-3.0 license text") }
        OutlinedButton(onClick = { showLibs = !showLibs }) {
            Text(if (showLibs) "Hide libraries" else "Show libraries (" + libraryData.size + ")")
        }
        if (showLibs) {
            libraryData.forEach { info ->
                Column(Modifier.padding(vertical = 4.dp)) {
                    Text(info.name, style = MaterialTheme.typography.titleSmall)
                    info.copyrightInfo?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant) }
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            "Licensed under the " + info.license.name,
                            modifier = Modifier.clickable { openLicense(info.license.raw) },
                            style = MaterialTheme.typography.bodySmall.copy(textDecoration = TextDecoration.Underline)
                        )
                        Text(
                            "Website",
                            modifier = Modifier.clickable { openLink(info.webUrl) },
                            style = MaterialTheme.typography.bodySmall.copy(textDecoration = TextDecoration.Underline),
                            color = scheme.primary
                        )
                    }
                }
            }
        }
    }
}
