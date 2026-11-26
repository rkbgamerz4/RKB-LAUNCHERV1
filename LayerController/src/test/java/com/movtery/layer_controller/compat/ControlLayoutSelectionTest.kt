/*
 * RKB LAUNCHER - runtime control-layout selection regressions.
 *
 * These tests exercise the same policy GameScreen uses when a version launches:
 * resolve the requested file, fall back only to a valid layout, and repair the
 * persisted global/per-version selection.
 */
package com.movtery.layer_controller.compat

import androidx.compose.ui.unit.IntSize
import com.movtery.layer_controller.compat.ControlLayoutSelection.Candidate
import com.movtery.layer_controller.compat.ControlLayoutSelection.Reason
import com.movtery.layer_controller.layout.ControlLayout
import com.movtery.layer_controller.layout.EmptyControlLayout
import com.movtery.layer_controller.layout.loadLayoutFromString
import com.movtery.layer_controller.pojav.PojavLayoutConverter
import com.movtery.layer_controller.utils.layoutJson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ControlLayoutSelectionTest {
    @get:Rule
    val folder = TemporaryFolder()

    private fun importedLayout(): ControlLayout =
        PojavLayoutConverter.importPojav(
            jsonString = javaClass.getResourceAsStream("/pojav_v8_default.json")!!
                .bufferedReader().use { it.readText() },
            screenWidthPx = 2400,
            screenHeightPx = 1080,
            density = 2.75f,
            layoutName = "Pojav import"
        ).layout

    private fun collapsedLayout(base: ControlLayout): ControlLayout = base.copy(
        layers = base.layers.map { layer ->
            layer.copy(
                normalButtons = layer.normalButtons.map { button ->
                    button.copy(position = com.movtery.layer_controller.data.ButtonPosition(0, 0))
                }
            )
        }
    )

    private fun write(name: String, layout: ControlLayout): File =
        File(folder.root, name).also {
            it.writeText(layoutJson.encodeToString(ControlLayout.serializer(), layout))
        }

    private val load: (File) -> ControlLayout? = { file ->
        runCatching { loadLayoutFromString(file.readText()) }.getOrNull()
    }

    private fun resolve(
        requested: File?,
        globalName: String?,
        versionOverride: String?,
        candidates: List<Candidate> = emptyList(),
        directory: File? = folder.root
    ) = ControlLayoutSelection.resolve(
        requested = requested,
        globalName = globalName,
        versionOverride = versionOverride,
        managerCandidates = candidates,
        directory = directory,
        load = load,
        emptyLayout = EmptyControlLayout
    )

    @Test
    fun `stale per-version override falls back to imported layout and is repaired`() {
        val healthy = importedLayout()
        val collapsed = collapsedLayout(healthy)
        val collapsedFile = write("collapsed.json", collapsed)
        val importedFile = write("imported.json", healthy)

        val result = resolve(
            requested = collapsedFile,
            globalName = "imported.json",
            versionOverride = "collapsed.json",
            candidates = listOf(
                Candidate(collapsedFile, collapsed),
                Candidate(importedFile, healthy)
            )
        )

        assertEquals(Reason.FALLBACK, result.reason)
        assertEquals("imported.json", result.file?.name)
        assertNull(result.repairGlobalTo)
        assertTrue(result.clearVersionOverride)

        val rendered = ControlLayoutSanity.renderedNormalButtons(
            result.layout, IntSize(2400, 1080), 2.75f
        )
        assertEquals(19, rendered.size)
        assertTrue(
            "Imported layout must not render overlapping controls: $rendered",
            ControlLayoutSanity.duplicateOffsets(rendered).isEmpty()
        )
    }

    @Test
    fun `collapsed global selection is repaired to the fallback`() {
        val healthy = importedLayout()
        val collapsed = collapsedLayout(healthy)
        val collapsedFile = write("collapsed.json", collapsed)
        val importedFile = write("imported.json", healthy)

        val result = resolve(
            requested = collapsedFile,
            globalName = "collapsed.json",
            versionOverride = "",
            candidates = listOf(
                Candidate(collapsedFile, collapsed),
                Candidate(importedFile, healthy)
            )
        )

        assertEquals(Reason.FALLBACK, result.reason)
        assertEquals("imported.json", result.repairGlobalTo)
        assertFalse(result.clearVersionOverride)
    }

    @Test
    fun `valid per-version override stays primary and is never repaired`() {
        val healthy = importedLayout()
        val collapsed = collapsedLayout(healthy)
        val collapsedFile = write("collapsed.json", collapsed)
        val importedFile = write("imported.json", healthy)

        val result = resolve(
            requested = importedFile,
            globalName = "collapsed.json",
            versionOverride = "imported.json",
            candidates = listOf(
                Candidate(collapsedFile, collapsed),
                Candidate(importedFile, healthy)
            )
        )

        assertEquals(Reason.PRIMARY, result.reason)
        assertEquals("imported.json", result.file?.name)
        assertNull(result.repairGlobalTo)
        assertFalse(result.clearVersionOverride)
    }

    @Test
    fun `missing override file falls back and clears the stale override`() {
        val healthy = importedLayout()
        val validFile = write("valid.json", healthy)
        val missing = File(folder.root, "missing.json")

        val result = resolve(
            requested = missing,
            globalName = "",
            versionOverride = "missing.json",
            candidates = listOf(Candidate(validFile, healthy))
        )

        assertEquals(Reason.FALLBACK, result.reason)
        assertEquals("valid.json", result.file?.name)
        assertNull(result.repairGlobalTo)
        assertTrue(result.clearVersionOverride)
    }

    @Test
    fun `directory fallback prefers the globally selected layout over a newer file`() {
        val healthy = importedLayout()
        val importedFile = write("imported.json", healthy)
        val newerFile = write("newer.json", healthy.copy(
            info = healthy.info.copy(name = healthy.info.name.copy(default = "Other"))
        ))
        importedFile.setLastModified(1_000L)
        newerFile.setLastModified(2_000L)

        val result = resolve(
            requested = write("collapsed.json", collapsedLayout(healthy)),
            globalName = "imported.json",
            versionOverride = "",
            candidates = emptyList(),
            directory = folder.root
        )

        assertEquals(Reason.FALLBACK, result.reason)
        assertEquals("imported.json", result.file?.name)
    }

    @Test
    fun `collapsed primary with no valid fallback is kept without fake repairs`() {
        val healthy = importedLayout()
        val first = write("first.json", collapsedLayout(healthy))
        val second = write("second.json", collapsedLayout(healthy))

        val result = resolve(
            requested = first,
            globalName = "first.json",
            versionOverride = "first.json",
            candidates = listOf(Candidate(first, collapsedLayout(healthy)))
        )

        assertEquals(Reason.PRIMARY_KEPT, result.reason)
        assertEquals("first.json", result.file?.name)
        assertNull(result.repairGlobalTo)
        assertFalse(result.clearVersionOverride)
        assertTrue("The other collapsed file must not be chosen as a fallback", second.exists())
    }
}
