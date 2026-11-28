/*
 * RKB LAUNCHER - automatic Discord Rich Presence via the official Discord Social SDK (1.10+).
 *
 * No sign-in of any kind: no OAuth, no tokens, no Connect button. The SDK hands the activity to
 * the Discord app on this device (Client::UpdateRichPresence while not connected), using only the
 * public application ID below. Requirements: the Discord app must be installed and signed in;
 * otherwise nothing is shown and the launcher is unaffected.
 *
 * Lifecycle: presence is set when Minecraft launches, cleared when the game process exits, and
 * cleared when the launcher goes to the background without a running game.
 */
package com.movtery.zalithlauncher.utils

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.discord.socialsdk.DiscordSocialSdkInit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object RkbDiscord {
    /** Public application (client) ID of the RKB LAUNCHER Discord application. Not a secret. */
    const val APP_ID = 1555829146490241095L

    private val _status = MutableStateFlow("Starting...")
    /** Human-readable state for the (read-only) settings line. */
    val status: StateFlow<String> = _status

    private val handler = Handler(Looper.getMainLooper())
    private var loaded = false
    private var discordInstalled: Boolean? = null
    private var appContext: Context? = null

    private var details = "In the launcher"
    private var state = "RKB LAUNCHER"
    private var startSeconds = 0L

    // Game-process tracking: "Playing" only while the :game process is alive.
    private var playing = false
    private var gameSeen = false
    private var playingSinceMs = 0L
    private var inForeground = true
    private var cleared = false
    private var lastWatchMs = 0L

    private val pump = object : Runnable {
        override fun run() {
            if (!loaded) return
            runCatching { nativeRunCallbacks() }
            val now = System.currentTimeMillis()
            if (now - lastWatchMs >= 3000) {
                lastWatchMs = now
                watchGame()
            }
            // Only needed to deliver the SDK's callbacks: slow in the foreground, slower in the background.
            handler.postDelayed(this, if (inForeground) 500L else 2000L)
        }
    }

    /** Call from the main activity. Safe to call repeatedly; any failure just disables the feature. */
    fun init(activity: Activity) {
        runCatching {
            DiscordSocialSdkInit.setEngineActivity(activity)
            appContext = activity.applicationContext
            if (!loaded) {
                System.loadLibrary("rkb_discord")
                nativeInit(APP_ID)
                loaded = true
                // Remove the access token stored by older builds that used the sign-in flow.
                activity.applicationContext.getSharedPreferences("rkb_discord", Context.MODE_PRIVATE).edit().clear().apply()
                nativeCheckInstalled()
                handler.post(pump)
                setInLauncher()
            }
        }.onFailure {
            loaded = false
            _status.value = "Unavailable on this build/device"
        }
    }

    fun setPlaying(versionName: String) {
        playing = true
        gameSeen = false
        playingSinceMs = System.currentTimeMillis()
        details = "Playing Minecraft $versionName"
        state = "RKB LAUNCHER"
        startSeconds = System.currentTimeMillis() / 1000
        sendPresence()
    }

    fun setInLauncher() {
        details = "In the launcher"
        state = "RKB LAUNCHER"
        startSeconds = 0L
        sendPresence()
    }

    private fun isGameRunning(): Boolean {
        val ctx = appContext ?: return false
        val am = ctx.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return false
        val name = ctx.packageName + ":game"
        return am.runningAppProcesses?.any { it.processName == name } == true
    }

    /** Runs every ~3s: ends the "Playing" presence once the game process has exited. */
    private fun watchGame() {
        if (!playing) return
        if (isGameRunning()) { gameSeen = true; return }
        val launchFailed = !gameSeen && System.currentTimeMillis() - playingSinceMs > 60_000
        if (gameSeen || launchFailed) {
            playing = false
            gameSeen = false
            if (inForeground) setInLauncher() else clearPresence()
        }
    }

    /** Launcher left the screen: keep "Playing" only while a game is actually running. */
    fun onBackground() {
        inForeground = false
        if (!playing) clearPresence()
    }

    fun onForeground() {
        inForeground = true
        if (playing && !isGameRunning() && System.currentTimeMillis() - playingSinceMs > 15_000) {
            playing = false
            setInLauncher()
        } else if (cleared && !playing) {
            setInLauncher()
        }
    }

    private fun clearPresence() {
        if (!loaded) return
        cleared = true
        runCatching { nativeClear() }
    }

    private fun sendPresence() {
        if (!loaded || discordInstalled == false) return
        cleared = false
        runCatching { nativeSetPresence(details, state, startSeconds) }
    }

    /** Called from native code (main thread, via RunCallbacks). */
    @JvmStatic
    fun onNativeEvent(type: String, a: String, @Suppress("UNUSED_PARAMETER") b: String) {
        when (type) {
            "installed" -> {
                discordInstalled = a == "true"
                _status.value = if (discordInstalled == true) "Discord app found" else "Discord app not installed"
                if (discordInstalled == true) sendPresence()
            }
            "presence" -> _status.value = if (cleared) "Idle" else "Active"
            "error" -> _status.value = "Waiting for the Discord app ($a)"
        }
    }

    @JvmStatic private external fun nativeInit(appId: Long)
    @JvmStatic private external fun nativeCheckInstalled()
    @JvmStatic private external fun nativeSetPresence(details: String, state: String, startSeconds: Long)
    @JvmStatic private external fun nativeRunCallbacks()
    @JvmStatic private external fun nativeClear()
}
