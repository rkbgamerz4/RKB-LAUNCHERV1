/*
 * RKB LAUNCHER - tests for PojavLauncher control-layout compatibility.
 */
package com.movtery.layer_controller.pojav

import com.movtery.layer_controller.data.JoystickTriggerMode
import com.movtery.layer_controller.data.VisibilityType
import com.movtery.layer_controller.event.ClickEvent
import com.movtery.layer_controller.layout.ControlLayout
import com.movtery.layer_controller.layout.loadLayoutFromString
import com.movtery.layer_controller.utils.layoutJson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class PojavLayoutConverterTest {
    private companion object {
        const val WIDTH = 2400
        const val HEIGHT = 1080
        const val DENSITY = 2.75f
    }

    private fun fixture(name: String): String =
        javaClass.getResourceAsStream("/$name")!!.bufferedReader().use { it.readText() }

    private fun import(json: String, width: Int = WIDTH, height: Int = HEIGHT, density: Float = DENSITY) =
        PojavLayoutConverter.importPojav(json, width, height, density, "Pojav import")

    private fun pct(offsetPx: Float, widgetPx: Float, screenPx: Int): Int =
        (offsetPx / (screenPx - widgetPx) * 10000f).toInt()

    @Test
    fun `supplied v8 layout keeps every button and its geometry`() {
        val result = import(fixture("pojav_v8_default.json"))
        val layer = result.layout.layers.first()

        assertEquals(19, layer.normalButtons.size)
        assertTrue(layer.joystickButtons.isEmpty())

        val first = layer.normalButtons.first()
        assertEquals("key", first.text.default)
        // dynamicX simplifies to 160dp, dynamicY to 30dp at preferred_scale=100.
        assertEquals(pct(160f * DENSITY, 80f * DENSITY, WIDTH).toDouble(), first.position.x.toDouble(), 2.0)
        assertEquals(pct(30f * DENSITY, 30f * DENSITY, HEIGHT).toDouble(), first.position.y.toDouble(), 2.0)
        assertEquals(80f, first.buttonSize.widthDp, 0.01f)
        assertEquals(30f, first.buttonSize.heightDp, 0.01f)

        val style = result.layout.styles.first { it.uuid == first.buttonStyle }
        // v8 strokeWidth is already dp; corner radius is half of Pojav's percent.
        assertEquals(1, style.lightStyle.borderWidth)
        assertEquals(7f, style.lightStyle.borderRadius.topStart, 0.01f)
    }

    @Test
    fun `actual v4 layout with margin right and bottom expressions is imported`() {
        val result = import(fixture("pojav_v4_user.json"))
        val layer = result.layout.layers.first()

        assertEquals(16, layer.normalButtons.size)
        assertTrue(layer.joystickButtons.isEmpty())
        assertEquals(1, result.layout.layers.size) // mDrawerDataList is empty.

        // ${margin} = (int)(2dp * density) = 5px at density 2.75.
        assertEquals(pct(455f, 220f, WIDTH).toDouble(), layer.normalButtons[0].position.x.toDouble(), 2.0)
        assertEquals(pct(5f, 82.5f, HEIGHT).toDouble(), layer.normalButtons[0].position.y.toDouble(), 2.0)
        assertEquals(80f, layer.normalButtons[0].buttonSize.widthDp, 0.01f)
        assertEquals(10000, layer.normalButtons[4].position.x) // ${right}
        assertTrue(layer.normalButtons[0].clickEvents.any {
            it.type == ClickEvent.Type.LauncherEvent && it.key == "launcher.event.switch_ime"
        })
        assertTrue(layer.normalButtons[1].clickEvents.isEmpty()) // GUI (-2)
    }

    @Test
    fun `special buttons are kept as visual-only and key bindings map correctly`() {
        val result = import(fixture("pojav_v8_default.json"))
        val buttons = result.layout.layers.first().normalButtons

        assertTrue(buttons[0].clickEvents.any {
            it.type == ClickEvent.Type.LauncherEvent && it.key == "launcher.event.switch_ime"
        })
        assertTrue(buttons[1].clickEvents.isEmpty()) // GUI (-2)
        assertTrue(buttons[4].clickEvents.isEmpty()) // virtual mouse (-5)
        assertTrue(result.warnings.any { it.contains("GUI / hide controls") })
        assertTrue(result.warnings.any { it.contains("visual-mouse") || it.contains("virtual mouse") })

        // v8 positive codes are GLFW keycodes.
        assertTrue(buttons.any { it.clickEvents.any { e -> e.key == "GLFW_KEY_W" } })
        assertTrue(buttons.any {
            it.clickEvents.any { e -> e.key == "GLFW_MOUSE_BUTTON_LEFT" }
        })
    }

    @Test
    fun `v4 expressions percent stroke widths and drawers are converted`() {
        val json = """
            {
              "mControlDataList": [
                {
                  "name": "A",
                  "keycodes": [65, 0, 0, 0],
                  "dynamicX": "${'$'}{right} - ${'$'}{margin}",
                  "dynamicY": "${'$'}{bottom} - ${'$'}{height}",
                  "width": 50,
                  "height": 50,
                  "opacity": 0.75,
                  "strokeWidth": 10,
                  "cornerRadius": 40
                }
              ],
              "mDrawerDataList": [
                {
                  "orientation": "DOWN",
                  "properties": {
                    "name": "Drawer",
                    "keycodes": [0, 0, 0, 0],
                    "dynamicX": "0 * ${'$'}{screen_width}",
                    "dynamicY": "0 * ${'$'}{screen_height}",
                    "width": 40,
                    "height": 40
                  },
                  "buttonProperties": [
                    { "name": "Child", "keycodes": [65, 0, 0, 0], "width": 40, "height": 40 }
                  ]
                }
              ],
              "scaledAt": 100,
              "version": 4
            }
        """.trimIndent()

        val result = import(json, width = 2400, height = 1080, density = 2f)
        val main = result.layout.layers.first()
        val button = main.normalButtons.first()

        // right = 2400-100, margin = (2dp * 2).toInt() = 4
        assertEquals(pct(2296f, 100f, WIDTH).toDouble(), button.position.x.toDouble(), 2.0)
        // bottom = 1080-100, minus own height 100
        assertEquals(pct(880f, 100f, HEIGHT).toDouble(), button.position.y.toDouble(), 2.0)
        // v4 stroke is 10% of half the longer side: 100px/2*0.1 = 5px = 2.5dp -> 3dp
        val style = result.layout.styles.first { it.uuid == button.buttonStyle }
        assertEquals(3, style.lightStyle.borderWidth)
        assertEquals(20f, style.lightStyle.borderRadius.topStart, 0.01f)

        assertEquals(2, result.layout.layers.size)
        val drawer = result.layout.layers[1]
        assertTrue(drawer.hide)
        assertEquals(1, drawer.normalButtons.size)
        assertEquals(840, drawer.normalButtons.first().position.y)
        assertTrue(main.normalButtons[1].clickEvents.any { it.type == ClickEvent.Type.SwitchLayer })
        assertTrue(result.warnings.any { it.contains("Drawer") })
    }

    @Test
    fun `v9 uses Android keycodes while v8 uses GLFW keycodes`() {
        val v9 = """
            {"mControlDataList":[
              {"name":"W9","keycodes":[51,0,0,0],"width":50,"height":50,
               "dynamicX":"0 * ${'$'}{screen_width}","dynamicY":"0 * ${'$'}{screen_height}"}
            ],"scaledAt":100,"version":9}
        """.trimIndent()
        val v8 = v9.replace("\"version\":9", "\"version\":8").replace("[51,", "[87,")

        assertEquals(
            "GLFW_KEY_W",
            import(v9).layout.layers.first().normalButtons.first().clickEvents.first().key
        )
        assertEquals(
            "GLFW_KEY_W",
            import(v8).layout.layers.first().normalButtons.first().clickEvents.first().key
        )
    }

    @Test
    fun `scaledAt sizes are normalized to one hundred percent`() {
        fun widthAt(scaledAt: Int): Float {
            val json = """
                {"mControlDataList":[
                  {"name":"A","keycodes":[87,0,0,0],"width":100,"height":50,
                   "dynamicX":"0 * ${'$'}{screen_width}","dynamicY":"0 * ${'$'}{screen_height}"}
                ],"scaledAt":$scaledAt,"version":8}
            """.trimIndent()
            return import(json, density = 2f).layout.layers.first().normalButtons.first().buttonSize.widthDp
        }

        assertEquals(100f, widthAt(100), 0.01f)
        assertEquals(50f, widthAt(200), 0.01f)
        assertTrue(import(
            """{"mControlDataList":[],"scaledAt":200,"version":8}""",
            density = 2f
        ).warnings.any { it.contains("button scale") })
    }

    @Test
    fun `unsupported specials become visual-only buttons with warnings`() {
        val json = """
            {"mControlDataList":[
              {"name":"GUI","keycodes":[-2,0,0,0],"width":50,"height":50},
              {"name":"Mouse","keycodes":[-5,0,0,0],"width":50,"height":50},
              {"name":"Pan","keycodes":[-10,0,0,0],"width":50,"height":50},
              {"name":"W","keycodes":[87,0,0,0],"width":50,"height":50}
            ],"scaledAt":100,"version":8}
        """.trimIndent()
        val result = import(json)
        val buttons = result.layout.layers.first().normalButtons

        assertEquals(4, buttons.size)
        assertTrue(buttons[0].clickEvents.isEmpty())
        assertTrue(buttons[1].clickEvents.isEmpty())
        assertTrue(buttons[2].clickEvents.isEmpty())
        assertTrue(buttons[3].clickEvents.isNotEmpty())
        assertTrue(result.warnings.any { it.contains("keyboard pan") })
    }

    @Test
    fun `v1 legacy transparency single keycode and modifiers are converted`() {
        val json = """
            {"mControlDataList":[
              {"name":"Shift+W","dynamicX":"0 * ${'$'}{screen_width}",
               "dynamicY":"0 * ${'$'}{screen_height}","keycode":87,"holdShift":true,
               "transparency":25,"isRound":true,"width":100,"height":100,
               "passThruEnabled":false,"isToggle":false}
            ],"scaledAt":100}
        """.trimIndent()
        val result = import(json, density = 2f)
        val button = result.layout.layers.first().normalButtons.first()
        val style = result.layout.styles.first { it.uuid == button.buttonStyle }

        assertEquals(50f, button.buttonSize.widthDp, 0.01f) // v1 sizes were px.
        assertTrue(button.clickEvents.any { it.key == "GLFW_KEY_W" })
        assertTrue(button.clickEvents.any { it.key == "GLFW_KEY_LEFT_SHIFT" })
        assertEquals(0.75f, style.lightStyle.alpha, 0.01f)
        assertEquals(17.5f, style.lightStyle.borderRadius.topStart, 0.01f)
        assertTrue(result.warnings.any { it.contains("Old Pojav layout") })
    }

    @Test
    fun `joystick settings and v6 v7 square fix are preserved`() {
        val json = """
            {"mControlDataList":[],"mJoystickDataList":[
              {"name":"Joy","keycodes":[87,65,83,68],"dynamicX":"0 * ${'$'}{screen_width}",
               "dynamicY":"0 * ${'$'}{screen_height}","width":100,"height":200,
               "forwardLock":true,"absolute":true,"displayInGame":true,"displayInMenu":false}
            ],"scaledAt":100,"version":7}
        """.trimIndent()
        val result = import(json, density = 2f)
        val joystick = result.layout.layers.first().joystickButtons.first()

        assertEquals(100f, joystick.sizeDp, 0.01f)
        assertTrue(joystick.canLock)
        assertEquals(JoystickTriggerMode.TOUCH, joystick.triggerMode)
        assertEquals(VisibilityType.IN_GAME, joystick.visibilityType)
        assertTrue(result.warnings.any { it.contains("v6/v7 non-square fix") })
    }

    @Test
    fun `malformed layout produces a meaningful error`() {
        try {
            import("""{"mControlDataList":{}}""")
            fail("Expected an import error")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("mControlDataList"))
        }
    }

    @Test
    fun `out of bounds positions are reported and clamped`() {
        val json = """
            {"mControlDataList":[
              {"name":"Outside","keycodes":[87,0,0,0],"width":50,"height":50,
               "dynamicX":"-100","dynamicY":"100000"}
            ],"scaledAt":100,"version":8}
        """.trimIndent()
        val result = import(json)
        val button = result.layout.layers.first().normalButtons.first()

        assertEquals(0, button.position.x)
        assertEquals(10000, button.position.y)
        assertTrue(result.warnings.any { it.contains("clamped to the edge") })
    }

    @Test
    fun `imported layout survives save and reload without moving buttons`() {
        val result = import(fixture("pojav_v8_default.json"))
        val encoded = layoutJson.encodeToString(ControlLayout.serializer(), result.layout)
        val loaded = loadLayoutFromString(encoded)

        assertEquals(result.layout.layers.size, loaded.layers.size)
        val original = result.layout.layers.first().normalButtons
        val reloaded = loaded.layers.first().normalButtons
        assertEquals(original.size, reloaded.size)
        original.forEachIndexed { index, button ->
            assertEquals(button.position, reloaded[index].position)
            assertEquals(button.buttonSize.widthDp, reloaded[index].buttonSize.widthDp, 0.01f)
            assertEquals(button.clickEvents, reloaded[index].clickEvents)
        }
    }

    @Test
    fun `export then import preserves the same pixel positions`() {
        val original = import(fixture("pojav_v8_default.json")).layout
        val exported = PojavLayoutConverter.exportToPojav(original, WIDTH, HEIGHT, DENSITY)
        val reimported = import(exported.json).layout

        val before = original.layers.first().normalButtons
        val after = reimported.layers.first().normalButtons
        assertEquals(before.size, after.size)
        before.forEachIndexed { index, button ->
            assertEquals(button.position.x.toDouble(), after[index].position.x.toDouble(), 2.0)
            assertEquals(button.position.y.toDouble(), after[index].position.y.toDouble(), 2.0)
            assertEquals(button.buttonSize.widthDp, after[index].buttonSize.widthDp, 0.1f)
        }
        assertNotNull(exported.json)
    }
}
