/*
 * RKB LAUNCHER - deterministic control-layout selection policy.
 *
 * Gameplay must not render a collapsed Pojav import merely because a stale
 * per-version override points at it. This policy is intentionally kept free of
 * Android/Compose dependencies so the real import -> select -> launch path can
 * be unit tested without a device.
 */
package com.movtery.layer_controller.compat

import com.movtery.layer_controller.layout.ControlLayout
import java.io.File

object ControlLayoutSelection {
    data class Candidate(
        val file: File,
        val layout: ControlLayout,
        val isSupported: Boolean = true
    )

    enum class Reason {
        /** The requested file is present and usable. */
        PRIMARY,
        /** A collapsed/missing primary was replaced by a valid layout. */
        FALLBACK,
        /** No valid replacement was found; the caller keeps the primary layout. */
        PRIMARY_KEPT
    }

    data class Result(
        val file: File?,
        val layout: ControlLayout,
        val reason: Reason,
        /** Non-null when the global selection should be repaired to this name. */
        val repairGlobalTo: String? = null,
        /** True when a per-version override pointing at the primary should be cleared. */
        val clearVersionOverride: Boolean = false
    )

    /**
     * Resolve the file gameplay should render.
     *
     * Ordering is explicit: the requested file first; then the globally selected
     * file; then the most recently modified valid layout. A collapsed or empty
     * fallback is never chosen.
     */
    fun resolve(
        requested: File?,
        globalName: String?,
        versionOverride: String?,
        managerCandidates: List<Candidate>,
        directory: File?,
        load: (File) -> ControlLayout?,
        emptyLayout: ControlLayout
    ): Result {
        val primaryLayout = requested?.let(load)
        if (primaryLayout != null && !ControlLayoutSanity.isCollapsedPojavImport(primaryLayout)) {
            return Result(requested, primaryLayout, Reason.PRIMARY)
        }

        val requestedPath = requested?.canonicalPathOrNull()
        fun isPrimary(file: File): Boolean =
            requestedPath != null && file.canonicalPathOrNull() == requestedPath

        fun order(files: List<File>): List<File> = files
            .distinctBy { it.canonicalPathOrNull() ?: it.path }
            .filter { !isPrimary(it) }
            .sortedWith(
                compareByDescending<File> { it.name == globalName }
                    .thenByDescending { it.lastModified() }
                    .thenBy { it.name }
            )

        val seen = LinkedHashSet<String>()
        fun accept(file: File, layout: ControlLayout?): Boolean {
            if (layout == null) return false
            if (!ControlLayoutSanity.hasControls(layout)) return false
            if (ControlLayoutSanity.isCollapsedPojavImport(layout)) return false
            val key = file.canonicalPathOrNull() ?: file.path
            if (!seen.add(key)) return false
            return true
        }

        val fromManager = order(
            managerCandidates
                .filter { it.isSupported && it.file.exists() }
                .map { it.file }
        )
        for (file in fromManager) {
            val candidate = managerCandidates.first { it.file.canonicalPathOrNull() == file.canonicalPathOrNull() }
            if (accept(file, candidate.layout)) {
                return fallbackResult(requested, globalName, versionOverride, file, candidate.layout)
            }
        }

        val fromDirectory = order(
            directory?.listFiles()?.toList()
                ?.filter { it.isFile && it.exists() && it.extension.equals("json", true) }
                .orEmpty()
        )
        for (file in fromDirectory) {
            val layout = runCatching { load(file) }.getOrNull()
            if (accept(file, layout)) {
                return fallbackResult(requested, globalName, versionOverride, file, layout!!)
            }
        }

        return Result(
            file = requested,
            layout = primaryLayout ?: emptyLayout,
            reason = Reason.PRIMARY_KEPT
        )
    }

    private fun fallbackResult(
        requested: File?,
        globalName: String?,
        versionOverride: String?,
        file: File,
        layout: ControlLayout
    ): Result {
        val primaryName = requested?.name
        return Result(
            file = file,
            layout = layout,
            reason = Reason.FALLBACK,
            repairGlobalTo = primaryName?.takeIf { it == globalName }?.let { file.name },
            clearVersionOverride = primaryName != null && primaryName == versionOverride
        )
    }

    private fun File.canonicalPathOrNull(): String? =
        runCatching { canonicalPath }.getOrNull()
}
