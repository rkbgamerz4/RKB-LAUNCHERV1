/*
 * RKB LAUNCHER - drawn banner for an instance card (no bundled artwork). Deterministic per instance name:
 * a pixel-style sky, sun/moon and three hill layers, tinted by the loader.
 */
package com.movtery.zalithlauncher.ui.screens.content

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.util.Random

private class Palette(val skyTop: Color, val skyBottom: Color, val sun: Color, val far: Color, val mid: Color, val near: Color)

private fun paletteFor(loader: String): Palette = when {
    loader.contains("fabric", true) -> Palette(Color(0xFF16343F), Color(0xFF5FA8A0), Color(0xFFF2E6C4), Color(0xFF2E5F5C), Color(0xFF23483F), Color(0xFF1A352F))
    loader.contains("neoforge", true) -> Palette(Color(0xFF2A1F3D), Color(0xFFE08A4C), Color(0xFFFFD9A8), Color(0xFF5B3A4A), Color(0xFF43303A), Color(0xFF2D2229))
    loader.contains("forge", true) -> Palette(Color(0xFF2B1B14), Color(0xFFD9733A), Color(0xFFFFE0B0), Color(0xFF5A3524), Color(0xFF42281C), Color(0xFF2C1B13))
    loader.contains("quilt", true) -> Palette(Color(0xFF241C3D), Color(0xFF8E7BD8), Color(0xFFE8E0FF), Color(0xFF45396E), Color(0xFF342B54), Color(0xFF251F3B))
    loader.contains("modpack", true) -> Palette(Color(0xFF1D1A3F), Color(0xFF6E5BE0), Color(0xFFE6E0FF), Color(0xFF3F3880), Color(0xFF302B63), Color(0xFF211E45))
    loader.contains("native", true) || loader.contains("horizon", true) -> Palette(Color(0xFF2A1410), Color(0xFFE0642A), Color(0xFFFFD2A0), Color(0xFF6B3420), Color(0xFF4A2418), Color(0xFF30170F))
    else -> Palette(Color(0xFF14304A), Color(0xFFE6A25C), Color(0xFFFFF0C8), Color(0xFF2F6B3A), Color(0xFF245A30), Color(0xFF1B4425))
}

@Composable
fun RkbInstanceBanner(seedText: String, loader: String, modifier: Modifier = Modifier, height: Dp = 84.dp) {
    val palette = remember(loader) { paletteFor(loader) }
    val seed = remember(seedText) { seedText.hashCode().toLong() }
    Canvas(modifier.fillMaxWidth().height(height)) {
        val cols = 40
        val cell = size.width / cols
        drawRect(Brush.verticalGradient(listOf(palette.skyTop, palette.skyBottom)))
        val rnd = Random(seed)
        // sun / moon: one pixel block
        val sunX = (6 + rnd.nextInt(cols - 14)) * cell
        drawRect(palette.sun, Offset(sunX, size.height * 0.16f), Size(cell * 3, cell * 3))
        // three hill layers, back to front, stepped like blocks
        val layers = listOf(palette.far to 0.58f, palette.mid to 0.72f, palette.near to 0.86f)
        for ((color, base) in layers) {
            var h = (size.height * base)
            for (c in 0 until cols) {
                h += (rnd.nextInt(3) - 1) * cell
                h = h.coerceIn(size.height * (base - 0.12f), size.height * (base + 0.08f))
                val top = (h / cell).toInt() * cell
                drawRect(color, Offset(c * cell, top), Size(cell + 0.5f, size.height - top))
            }
        }
        // two small trees on the front layer
        repeat(2) {
            val tx = (3 + rnd.nextInt(cols - 6)) * cell
            val ty = size.height * 0.80f
            drawRect(palette.near.copy(alpha = 0.9f), Offset(tx + cell, ty), Size(cell, cell * 2))
            drawRect(palette.mid, Offset(tx, ty - cell * 2), Size(cell * 3, cell * 2))
            drawRect(palette.mid, Offset(tx + cell, ty - cell * 3), Size(cell, cell))
        }
    }
}
