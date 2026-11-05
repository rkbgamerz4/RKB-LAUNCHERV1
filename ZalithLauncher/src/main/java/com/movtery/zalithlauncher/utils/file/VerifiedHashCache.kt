/*
 * RKB LAUNCHER - remembers which game files were already SHA-1 verified, so PLAY does not re-hash
 * thousands of unchanged asset/library files every time.
 *
 * A file is trusted only if (path, size, last-modified, expected SHA-1) all match a previous successful
 * full verification. Any change to a file's size or modification time forces a real re-hash.
 * Limitation: silent corruption that keeps size and mtime identical is not detected; turn the
 * "Fast file verification" setting off (Settings > Game) to always re-hash.
 */
package com.movtery.zalithlauncher.utils.file

import com.movtery.zalithlauncher.path.PathManager
import com.movtery.zalithlauncher.setting.AllSettings
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

object VerifiedHashCache {
    private class Entry(val size: Long, val mtime: Long, val sha1: String)

    private val map = ConcurrentHashMap<String, Entry>()
    private val dirty = AtomicBoolean(false)
    @Volatile private var loaded = false

    val checked = AtomicLong()
    val cacheHits = AtomicLong()
    val rehashed = AtomicLong()
    val hashNanos = AtomicLong()

    private fun cacheFile() = File(PathManager.DIR_FILES_PRIVATE, "rkb_verified_files.tsv")

    @Synchronized
    private fun ensureLoaded() {
        if (loaded) return
        loaded = true
        runCatching {
            val f = cacheFile()
            if (!f.exists()) return@runCatching
            f.forEachLine { line ->
                val p = line.split('\t')
                if (p.size == 4) {
                    val size = p[1].toLongOrNull() ?: return@forEachLine
                    val mtime = p[2].toLongOrNull() ?: return@forEachLine
                    map[p[0]] = Entry(size, mtime, p[3])
                }
            }
        }
    }

    fun enabled(): Boolean = AllSettings.rkbFastVerify.getValue()

    /** True only if this exact file state was fully verified against [expectedSha1] before. */
    fun isVerified(file: File, expectedSha1: String): Boolean {
        checked.incrementAndGet()
        if (!enabled()) return false
        ensureLoaded()
        val e = map[file.absolutePath] ?: return false
        val ok = e.size == file.length() && e.mtime == file.lastModified() && e.sha1.equals(expectedSha1, ignoreCase = true)
        if (ok) cacheHits.incrementAndGet()
        return ok
    }

    fun record(file: File, sha1: String, hashTimeNanos: Long) {
        rehashed.incrementAndGet()
        hashNanos.addAndGet(hashTimeNanos)
        if (!enabled()) return
        ensureLoaded()
        if (sha1.contains('\t') || file.absolutePath.contains('\t')) return
        map[file.absolutePath] = Entry(file.length(), file.lastModified(), sha1)
        dirty.set(true)
    }

    fun addHashTime(nanos: Long) {
        rehashed.incrementAndGet()
        hashNanos.addAndGet(nanos)
    }

    fun resetStats() {
        checked.set(0); cacheHits.set(0); rehashed.set(0); hashNanos.set(0)
    }

    /** Writes the cache atomically (temp file + rename). Cheap no-op when nothing changed. */
    fun flush() {
        if (!dirty.compareAndSet(true, false)) return
        runCatching {
            val f = cacheFile()
            val tmp = File(f.parentFile, f.name + ".tmp")
            tmp.bufferedWriter().use { w ->
                for ((path, e) in map) {
                    w.append(path).append('\t').append(e.size.toString()).append('\t')
                        .append(e.mtime.toString()).append('\t').append(e.sha1).append('\n')
                }
            }
            if (!tmp.renameTo(f)) {
                f.delete()
                tmp.renameTo(f)
            }
        }
    }
}
