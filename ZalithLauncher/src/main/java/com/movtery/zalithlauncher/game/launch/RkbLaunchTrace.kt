/*
 * RKB LAUNCHER - honest launch timing. Measures the launcher-side part of PLAY (account check, mod scan,
 * file verification) in milliseconds and stores the last runs. It does NOT measure Minecraft's own
 * startup or world loading; time those with a stopwatch / screen recording.
 */
package com.movtery.zalithlauncher.game.launch

import com.movtery.zalithlauncher.path.PathManager
import com.movtery.zalithlauncher.utils.file.VerifiedHashCache
import java.io.File

object RkbLaunchTrace {
    class Run(
        val version: String,
        val preLaunchMs: Long,
        val filesChecked: Long,
        val cacheHits: Long,
        val rehashed: Long,
        val hashMs: Long
    )

    @Volatile private var startNanos = 0L
    @Volatile private var versionName = ""

    private fun file() = File(PathManager.DIR_FILES_PRIVATE, "rkb_launch_times.tsv")

    fun begin(version: String) {
        versionName = version
        VerifiedHashCache.resetStats()
        startNanos = System.nanoTime()
    }

    /** Call right before the game process is started. */
    fun preLaunchDone() {
        if (startNanos == 0L) return
        val ms = (System.nanoTime() - startNanos) / 1_000_000
        startNanos = 0L
        VerifiedHashCache.flush()
        runCatching {
            val line = listOf(
                versionName.replace('\t', ' '), ms,
                VerifiedHashCache.checked.get(), VerifiedHashCache.cacheHits.get(),
                VerifiedHashCache.rehashed.get(), VerifiedHashCache.hashNanos.get() / 1_000_000
            ).joinToString("\t")
            val lines = (if (file().exists()) file().readLines() else emptyList()) + line
            file().writeText(lines.takeLast(20).joinToString("\n") + "\n")
        }
    }

    fun recent(): List<Run> = runCatching {
        if (!file().exists()) return emptyList()
        file().readLines().mapNotNull { l ->
            val p = l.split('\t')
            if (p.size < 6) null else Run(
                p[0], p[1].toLongOrNull() ?: return@mapNotNull null,
                p[2].toLongOrNull() ?: 0, p[3].toLongOrNull() ?: 0,
                p[4].toLongOrNull() ?: 0, p[5].toLongOrNull() ?: 0
            )
        }
    }.getOrDefault(emptyList())

    fun medianPreLaunchMs(runs: List<Run>): Long? {
        if (runs.isEmpty()) return null
        val s = runs.map { it.preLaunchMs }.sorted()
        return if (s.size % 2 == 1) s[s.size / 2] else (s[s.size / 2 - 1] + s[s.size / 2]) / 2
    }

    fun clear() {
        runCatching { file().delete() }
    }
}
