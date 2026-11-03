/*
 * RKB LAUNCHER - Discord Rich Presence via the Discord Social SDK.
 * Tokens are kept only in this app's private SharedPreferences and are never logged or shown.
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

    private const val PREFS = "rkb_discord"
    private const val KEY_ACCESS = "access"

    private val _status = MutableStateFlow("Not connected")
    val status: StateFlow<String> = _status

    private val handler = Handler(Looper.getMainLooper())
    private var loaded = false
    private var ready = false
    private var appContext: Context? = null
    private var details = "In the launcher"
    private var state = "RKB LAUNCHER"
    private var startSeconds = 0L

    // Game-process tracking: presence says "Playing" only while the :game process is alive.
    private var playing = false
    private var gameSeen = false
    private var playingSinceMs = 0L
    private var inForeground = true
    private var suspended = false
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
            // 10 wake-ups per second only while the launcher is on screen; 1 per second in the background
            // so the launcher process stays idle while the game is running.
            handler.postDelayed(this, if (inForeground) 100L else 1000L)
        }
    }

    /** Call from the main activity. Safe to call repeatedly; failures only disable the feature. */
    fun init(activity: Activity) {
        runCatching {
            DiscordSocialSdkInit.setEngineActivity(activity)
            appContext = activity.applicationContext
            if (!loaded) {
                System.loadLibrary("rkb_discord")
                nativeInit(APP_ID)
                loaded = true
                handler.post(pump)
                val saved = prefs()?.getString(KEY_ACCESS, null)
                if (!saved.isNullOrEmpty()) {
                    _status.value = "Connecting..."
                    nativeConnectWithToken(saved)
                }
            }
        }.onFailure {
            loaded = false
            _status.value = "Discord unavailable on this build/device"
        }
    }

    /** Starts the Discord login (opens Discord or a browser tab). */
    fun connect() {
        if (!loaded) { _status.value = "Discord unavailable on this build/device"; return }
        _status.value = "Waiting for Discord..."
        runCatching { nativeAuthorize() }
    }

    fun disconnect() {
        prefs()?.edit()?.remove(KEY_ACCESS)?.apply()
        ready = false
        if (loaded) runCatching { nativeDisconnect() }
        _status.value = "Not connected"
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
            if (inForeground) setInLauncher() else suspendPresence()
        }
    }

    /** Launcher moved to the background (user left it). Keeps "Playing" only while a game runs. */
    fun onBackground() {
        inForeground = false
        if (!playing) suspendPresence()
    }

    /** Launcher is visible again. */
    fun onForeground() {
        inForeground = true
        if (playing && !isGameRunning() && System.currentTimeMillis() - playingSinceMs > 15_000) {
            playing = false
            setInLauncher()
        }
        if (suspended && loaded) {
            suspended = false
            val saved = prefs()?.getString(KEY_ACCESS, null)
            if (!saved.isNullOrEmpty()) {
                if (!playing) { details = "In the launcher"; startSeconds = 0L }
                runCatching { nativeConnectWithToken(saved) }
            }
        }
    }

    /** Clears the presence and drops the connection, but keeps the saved login. */
    private fun suspendPresence() {
        // Only when connected; never interrupt a sign-in that is still in progress.
        if (!loaded || !ready) return
        suspended = true
        ready = false
        runCatching { nativeDisconnect() }
        _status.value = "Paused (launcher in background)"
    }

    private fun sendPresence() {
        if (loaded && ready) runCatching { nativeSetPresence(details, state, startSeconds) }
    }

    private fun prefs() = appContext?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Called from native code (main thread, via RunCallbacks). */
    @JvmStatic
    fun onNativeEvent(type: String, a: String, b: String) {
        when (type) {
            "status" -> {
                ready = a.equals("Ready", ignoreCase = true)
                _status.value = if (ready) "Connected" else a
                if (ready) sendPresence()
            }
            "token" -> prefs()?.edit()?.putString(KEY_ACCESS, a)?.apply()
            "error" -> _status.value = "Error: $a"
        }
    }

    @JvmStatic private external fun nativeInit(appId: Long)
    @JvmStatic private external fun nativeAuthorize()
    @JvmStatic private external fun nativeConnectWithToken(token: String)
    @JvmStatic private external fun nativeSetPresence(details: String, state: String, startSeconds: Long)
    @JvmStatic private external fun nativeRunCallbacks()
    @JvmStatic private external fun nativeDisconnect()
}
