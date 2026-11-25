/*
 * RKB LAUNCHER - control layout import compatibility layer.
 *
 * Each supported launcher format is a small adapter that produces the existing
 * RKB ControlLayout model. Adapters never render or handle input themselves;
 * the normal RKB editor / renderer / input system stays in charge.
 */
package com.movtery.layer_controller.compat

import com.movtery.layer_controller.layout.ControlLayout
import com.movtery.layer_controller.pojav.PojavLayoutConverter

/**
 * Geometry used while converting absolute source coordinates.
 *
 * Pojav evaluates dynamicX/dynamicY in pixels against the current control view.
 * RKB stores positions as a percentage, so the conversion must happen against a
 * concrete landscape viewport. Keep the caller's width/height order: the
 * converter does not guess or silently swap them.
 */
data class ControlImportGeometry(
    val widthPx: Int,
    val heightPx: Int,
    val density: Float
) {
    init {
        require(widthPx > 0) { "Control import width must be greater than zero." }
        require(heightPx > 0) { "Control import height must be greater than zero." }
        require(density > 0f) { "Control import density must be greater than zero." }
    }
}

data class ImportedControlLayout(
    val layout: ControlLayout,
    val warnings: List<String>,
    val sourceFormat: String
)

/**
 * A launcher control-layout adapter.
 */
interface ControlLayoutImporter {
    val formatName: String

    /** Returns true only when the actual JSON schema was recognised. */
    fun matches(json: String): Boolean

    fun import(
        json: String,
        geometry: ControlImportGeometry,
        layoutName: String
    ): ImportedControlLayout
}

private object PojavControlLayoutImporter : ControlLayoutImporter {
    override val formatName: String = "PojavLauncher"

    override fun matches(json: String): Boolean = PojavLayoutConverter.looksLikePojav(json)

    override fun import(
        json: String,
        geometry: ControlImportGeometry,
        layoutName: String
    ): ImportedControlLayout {
        val result = PojavLayoutConverter.importPojav(
            jsonString = json,
            screenWidthPx = geometry.widthPx,
            screenHeightPx = geometry.heightPx,
            density = geometry.density,
            layoutName = layoutName
        )
        return ImportedControlLayout(
            layout = result.layout,
            warnings = result.warnings,
            sourceFormat = formatName
        )
    }
}

/**
 * Registry of compatible launcher control formats.
 *
 * Add a new adapter here after inspecting the real format. Keeping detection
 * explicit prevents an unrelated JSON file from being silently interpreted as
 * another launcher's controls.
 */
object ControlLayoutImporters {
    private val importers: List<ControlLayoutImporter> = listOf(
        PojavControlLayoutImporter
        // Future: Mojo / other launchers, only after schema inspection + tests.
    )

    fun findImporter(json: String): ControlLayoutImporter? =
        importers.firstOrNull { it.matches(json) }

    /**
     * Import a recognised third-party layout, or return null when the JSON is
     * not one of the supported foreign formats (the caller should then use the
     * normal RKB layout loader).
     */
    fun importIfSupported(
        json: String,
        geometry: ControlImportGeometry,
        layoutName: String
    ): ImportedControlLayout? {
        return findImporter(json)?.import(json, geometry, layoutName)
    }
}
