/*
 * RKB LAUNCHER - control layout sanity checks.
 *
 * These helpers deliberately use the same position/size math as the real
 * renderer (widgetPosition + ButtonSize), so a layout can be validated before
 * it is saved or rendered. They never change the layout.
 */
package com.movtery.layer_controller.compat

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import com.movtery.layer_controller.data.ButtonPosition
import com.movtery.layer_controller.data.ButtonSize
import com.movtery.layer_controller.layout.ControlLayout
import com.movtery.layer_controller.utils.widgetPosition
import kotlin.math.roundToInt

/**
 * A normal button together with the size and offset the renderer will use.
 */
data class RenderedControl(
    val layer: String,
    val label: String,
    val uuid: String,
    val position: ButtonPosition,
    val sizePx: IntSize,
    val offset: Offset
)

/**
 * Read-only checks for imported layouts.
 */
object ControlLayoutSanity {
    /**
     * True when the layout was produced by the Pojav importer.
     *
     * Both the metadata written by the converter and the layer name are checked
     * so older imported files (created before the metadata was added) are still
     * recognised.
     */
    fun isPojavImport(layout: ControlLayout): Boolean {
        val info = layout.info
        return info.versionName.startsWith("pojav-", ignoreCase = true) ||
            info.author.default.contains("Pojav", ignoreCase = true) ||
            layout.layers.any { it.name.equals("Pojav", ignoreCase = true) }
    }

    /**
     * Detect the specific failure reported by users: every normal button of a
     * Pojav import resolves to one and the same position, which makes all but
     * the top control untouchable and draws them stacked in one corner.
     *
     * A healthy Pojav layout can legitimately contain one control at (0,0)
     * (for example ESC), but not all of them.
     */
    fun isCollapsedPojavImport(layout: ControlLayout): Boolean {
        if (!isPojavImport(layout)) return false
        val buttons = layout.layers.flatMap { it.normalButtons }
        if (buttons.size < 2) return false
        return buttons.map { it.position }.distinct().size == 1
    }

    /**
     * Compute the exact offsets the renderer will use for every normal button.
     *
     * `WrapContent` buttons are measured by Compose from their text; without a
     * text measurement they are reported with a zero size. Imported Pojav
     * layouts always use explicit dp sizes, so the pipeline checks are exact.
     */
    fun renderedNormalButtons(
        layout: ControlLayout,
        screenSize: IntSize,
        density: Float
    ): List<RenderedControl> {
        require(screenSize.width > 0 && screenSize.height > 0) {
            "Rendered control size requires a real viewport, got ${screenSize.width}x${screenSize.height}px."
        }
        require(density > 0f) {
            "Rendered control size requires a positive density, got $density."
        }

        return layout.layers.flatMap { layer ->
            layer.normalButtons.map { button ->
                val size = renderedSize(button.buttonSize, screenSize, density)
                RenderedControl(
                    layer = layer.name,
                    label = button.text.default,
                    uuid = button.uuid,
                    position = button.position,
                    sizePx = size,
                    offset = widgetPosition(
                        xPercentage = button.position.xPercentage(),
                        yPercentage = button.position.yPercentage(),
                        widgetSize = size,
                        screenSize = screenSize
                    )
                )
            }
        }
    }

    /**
     * The px size the renderer will measure for a button size.
     *
     * `Modifier.buttonSize` converts a percentage to px through dp and back, so
     * the density cancels out; dp sizes are simply multiplied by the density.
     */
    fun renderedSize(size: ButtonSize, screenSize: IntSize, density: Float): IntSize {
        return when (size.type) {
            ButtonSize.Type.Dp -> IntSize(
                width = (size.widthDp * density).roundToInt(),
                height = (size.heightDp * density).roundToInt()
            )
            ButtonSize.Type.Percentage -> {
                val widthReference = if (size.widthReference == ButtonSize.Reference.ScreenWidth) {
                    screenSize.width.toFloat()
                } else {
                    screenSize.height.toFloat()
                }
                val heightReference = if (size.heightReference == ButtonSize.Reference.ScreenWidth) {
                    screenSize.width.toFloat()
                } else {
                    screenSize.height.toFloat()
                }
                IntSize(
                    width = (widthReference * (size.widthPercentage / 10000f)).roundToInt(),
                    height = (heightReference * (size.heightPercentage / 10000f)).roundToInt()
                )
            }
            ButtonSize.Type.WrapContent -> IntSize.Zero
        }
    }

    /**
     * Group controls that would be drawn at the same integer pixel offset.
     * A non-empty result means controls overlap unexpectedly.
     */
    fun duplicateOffsets(
        controls: List<RenderedControl>,
        tolerancePx: Int = 1
    ): List<List<RenderedControl>> {
        require(tolerancePx > 0) { "Tolerance must be greater than zero." }
        val groups = LinkedHashMap<Pair<Int, Int>, MutableList<RenderedControl>>()
        controls.forEach { control ->
            val key = (control.offset.x / tolerancePx).roundToInt() to
                (control.offset.y / tolerancePx).roundToInt()
            groups.getOrPut(key) { ArrayList() } += control
        }
        return groups.values.filter { it.size > 1 }
    }
}
