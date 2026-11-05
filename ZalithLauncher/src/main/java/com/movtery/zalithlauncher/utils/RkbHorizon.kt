/*
 * RKB LAUNCHER - Horizon (Rust Edition) helpers.
 *
 * Verified facts (official README, 2026-11): Horizon is Minecraft Java 26.3 rewritten in Rust; the OFFICIAL
 * launcher builds exist for Linux x86-64, macOS and Windows x64 only - there is no official Android build.
 * A third-party, unofficial Android port exists (package dev.horizonlab.horizon). RKB does not bundle,
 * download or silently install it: it only detects it and starts it as a separate native app.
 */
package com.movtery.zalithlauncher.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri

object RkbHorizon {
    /** Package id of the unofficial Android port (Rayan-XOO/horizon, android-port branch). */
    const val PACKAGE = "dev.horizonlab.horizon"
    const val UNOFFICIAL_RELEASES = "https://github.com/Rayan-XOO/horizon/releases/latest"
    const val OFFICIAL_SITE = "https://playhorizon.dev"
    const val OFFICIAL_REPO = "https://github.com/horizon-rust/horizon"

    /** Installed version name, or null when the app is not installed. */
    fun installedVersion(context: Context): String? = try {
        @Suppress("DEPRECATION")
        context.packageManager.getPackageInfo(PACKAGE, 0).versionName ?: "?"
    } catch (_: PackageManager.NameNotFoundException) {
        null
    } catch (_: Exception) {
        null
    }

    /** Starts the Horizon app. Returns false if it is not installed or cannot be started. */
    fun launch(context: Context): Boolean = try {
        val intent = context.packageManager.getLaunchIntentForPackage(PACKAGE)
        if (intent == null) {
            false
        } else {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        }
    } catch (_: Exception) {
        false
    }

    fun openUrl(context: Context, url: String) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Exception) {
        }
    }
}
