/*
 * RKB LAUNCHER - control layout import compatibility layer.
 *
 * Each supported launcher format is a small adapter that produces the existing
 * RKB ControlLayout model. Adapters never render or handle input themselves;
 * the normal RKB editor / renderer / input system stays in charge.
 */
package com.movtery.layer_controller.compat

import android.content.Context
import com.movtery.layer_controller.layout.ControlLayout
import com.movtery.layer_controller.pojav.PojavLayoutConverter
import kotlin.math.roundToInt

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

    companion object {
        /**
         * Build a landscape import viewport from raw display metrics.
         *
         * Pojav expressions are evaluated in physical pixels. RKB renders the
         * control layer over the whole window, so the import viewport has to be
         * the real landscape window size. Normalising to landscape order keeps
         * the axes stable when the user imports while the device is in portrait.
         *
         * Degenerate metrics are rejected instead of being passed to the
         * converter: a zero-sized viewport makes every percentage collapse to
         * `(0, 0)` while the button sizes stay correct, which is exactly the
         * "all controls stacked in the upper-left" failure.
         */
        fun fromDisplayMetrics(
            widthPx: Int,
            heightPx: Int,
            density: Float
        ): ControlImportGeometry {
            require(widthPx > 0 && heightPx > 0) {
                "Control import requires a real display size, got ${widthPx}x${heightPx}px."
            }
            require(density > 0f) {
                "Control import requires a positive display density, got $density."
            }
            return ControlImportGeometry(
                widthPx = maxOf(widthPx, heightPx),
                heightPx = minOf(widthPx, heightPx),
                density = density
            )
        }

        /**
         * Resolve the real application viewport.
         *
         * `Resources.getSystem()` is intentionally not used here. Its
         * `DisplayMetrics` is not attached to a display and can report
         * `widthPixels == heightPixels == 0`; the old importer then divided by a
         * zero-sized free area and placed every control at `(0, 0)`.
         *
         * The application `Resources` follows the current window/display
         * configuration, and the configuration values are used as a fallback if
         * the metrics are ever incomplete.
         */
        fun fromContext(context: Context): ControlImportGeometry {
            val metrics = context.resources.displayMetrics
            var width = metrics.widthPixels
            var height = metrics.heightPixels
            var density = metrics.density

            if (width <= 0 || height <= 0 || density <= 0f) {
                val configuration = context.resources.configuration
                if (density <= 0f) {
                    density = configuration.densityDpi / 160f
                }
                if (density > 0f && (width <= 0 || height <= 0)) {
                    width = (configuration.screenWidthDp * density).roundToInt()
                    height = (configuration.screenHeightDp * density).roundToInt()
                }
            }

            return fromDisplayMetrics(width, height, density)
        }
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
