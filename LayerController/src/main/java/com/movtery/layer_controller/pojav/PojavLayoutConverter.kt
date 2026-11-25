/*
 * RKB LAUNCHER - PojavLauncher control layout importer / exporter.
 *
 * Reads the PojavLauncher "CustomControls" JSON (v1-v9: mControlDataList /
 * mDrawerDataList / mJoystickDataList, dynamicX/dynamicY expressions, keycodes[4]
 * with negative special codes) and converts it to/from the existing RKB layout
 * model. Adapters do not render or handle input; the normal RKB editor, renderer
 * and input system stay in charge.
 *
 * Format knowledge comes from PojavLauncher's customcontrols package (LGPL-3.0),
 * in particular LayoutConverter.java, ControlData.java and ControlInterface.java.
 * This module is part of a GPL-3.0 combined work (see RKB_NOTICE.md).
 */
package com.movtery.layer_controller.pojav

import android.view.KeyEvent
import androidx.compose.ui.graphics.Color
import com.movtery.inputmap.keycodes.ControlEventKeycode
import com.movtery.inputmap.keycodes.LwjglGlfwKeycode
import com.movtery.layer_controller.EDITOR_VERSION
import com.movtery.layer_controller.data.ButtonPosition
import com.movtery.layer_controller.data.ButtonShape
import com.movtery.layer_controller.data.ButtonSize
import com.movtery.layer_controller.data.ButtonStyle
import com.movtery.layer_controller.data.JoystickData
import com.movtery.layer_controller.data.JoystickTriggerMode
import com.movtery.layer_controller.data.NormalData
import com.movtery.layer_controller.data.VisibilityType
import com.movtery.layer_controller.data.lang.TranslatableString
import com.movtery.layer_controller.event.ClickEvent
import com.movtery.layer_controller.layout.ControlLayer
import com.movtery.layer_controller.layout.ControlLayout
import com.movtery.layer_controller.utils.getAButtonUUID
import com.movtery.layer_controller.utils.randomUUID
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlin.math.roundToInt

object PojavLayoutConverter {
    // Pojav ControlData.SPECIALBTN_*
    private const val SPECIAL_KEYBOARD = -1
    private const val SPECIAL_TOGGLECTRL = -2
    private const val SPECIAL_MOUSEPRI = -3
    private const val SPECIAL_MOUSESEC = -4
    private const val SPECIAL_VIRTUALMOUSE = -5
    private const val SPECIAL_MOUSEMID = -6
    private const val SPECIAL_SCROLLUP = -7
    private const val SPECIAL_SCROLLDOWN = -8
    private const val SPECIAL_MENU = -9
    private const val SPECIAL_KEYBOARDPAN = -10

    // Launcher event ids already used by RKB (ui/control/event/Events.kt)
    private const val EV_SWITCH_IME = "launcher.event.switch_ime"
    private const val EV_SWITCH_MENU = "launcher.event.switch_menu"
    private const val EV_SCROLL_UP = "launcher.event.scroll_up"
    private const val EV_SCROLL_DOWN = "launcher.event.scroll_down"

    /** Highest schema this importer understands. Pojav v9 switched to Android keycodes. */
    private const val SUPPORTED_VERSION = 9
    private const val MIN_DP = 5f

    class ImportResult(val layout: ControlLayout, val warnings: List<String>)
    class ExportResult(val json: String, val warnings: List<String>)

    private val parser = Json { ignoreUnknownKeys = true; isLenient = true }
    private val writer = Json { prettyPrint = false }

    /**
     * The GLFW keycode -> Android keycode table used by Pojav's LayoutConverter.
     * v8 and older layouts store the left column, v9 stores the right column.
     */
    private val keycodePairs: List<Pair<String, Int>> = listOf(
        "GLFW_KEY_SPACE" to KeyEvent.KEYCODE_SPACE,
        "GLFW_KEY_APOSTROPHE" to KeyEvent.KEYCODE_APOSTROPHE,
        "GLFW_KEY_COMMA" to KeyEvent.KEYCODE_COMMA,
        "GLFW_KEY_MINUS" to KeyEvent.KEYCODE_MINUS,
        "GLFW_KEY_PERIOD" to KeyEvent.KEYCODE_PERIOD,
        "GLFW_KEY_SLASH" to KeyEvent.KEYCODE_SLASH,
        "GLFW_KEY_0" to KeyEvent.KEYCODE_0,
        "GLFW_KEY_1" to KeyEvent.KEYCODE_1,
        "GLFW_KEY_2" to KeyEvent.KEYCODE_2,
        "GLFW_KEY_3" to KeyEvent.KEYCODE_3,
        "GLFW_KEY_4" to KeyEvent.KEYCODE_4,
        "GLFW_KEY_5" to KeyEvent.KEYCODE_5,
        "GLFW_KEY_6" to KeyEvent.KEYCODE_6,
        "GLFW_KEY_7" to KeyEvent.KEYCODE_7,
        "GLFW_KEY_8" to KeyEvent.KEYCODE_8,
        "GLFW_KEY_9" to KeyEvent.KEYCODE_9,
        "GLFW_KEY_SEMICOLON" to KeyEvent.KEYCODE_SEMICOLON,
        "GLFW_KEY_EQUAL" to KeyEvent.KEYCODE_EQUALS,
        "GLFW_KEY_A" to KeyEvent.KEYCODE_A,
        "GLFW_KEY_B" to KeyEvent.KEYCODE_B,
        "GLFW_KEY_C" to KeyEvent.KEYCODE_C,
        "GLFW_KEY_D" to KeyEvent.KEYCODE_D,
        "GLFW_KEY_E" to KeyEvent.KEYCODE_E,
        "GLFW_KEY_F" to KeyEvent.KEYCODE_F,
        "GLFW_KEY_G" to KeyEvent.KEYCODE_G,
        "GLFW_KEY_H" to KeyEvent.KEYCODE_H,
        "GLFW_KEY_I" to KeyEvent.KEYCODE_I,
        "GLFW_KEY_J" to KeyEvent.KEYCODE_J,
        "GLFW_KEY_K" to KeyEvent.KEYCODE_K,
        "GLFW_KEY_L" to KeyEvent.KEYCODE_L,
        "GLFW_KEY_M" to KeyEvent.KEYCODE_M,
        "GLFW_KEY_N" to KeyEvent.KEYCODE_N,
        "GLFW_KEY_O" to KeyEvent.KEYCODE_O,
        "GLFW_KEY_P" to KeyEvent.KEYCODE_P,
        "GLFW_KEY_Q" to KeyEvent.KEYCODE_Q,
        "GLFW_KEY_R" to KeyEvent.KEYCODE_R,
        "GLFW_KEY_S" to KeyEvent.KEYCODE_S,
        "GLFW_KEY_T" to KeyEvent.KEYCODE_T,
        "GLFW_KEY_U" to KeyEvent.KEYCODE_U,
        "GLFW_KEY_V" to KeyEvent.KEYCODE_V,
        "GLFW_KEY_W" to KeyEvent.KEYCODE_W,
        "GLFW_KEY_X" to KeyEvent.KEYCODE_X,
        "GLFW_KEY_Y" to KeyEvent.KEYCODE_Y,
        "GLFW_KEY_Z" to KeyEvent.KEYCODE_Z,
        "GLFW_KEY_LEFT_BRACKET" to KeyEvent.KEYCODE_LEFT_BRACKET,
        "GLFW_KEY_BACKSLASH" to KeyEvent.KEYCODE_BACKSLASH,
        "GLFW_KEY_RIGHT_BRACKET" to KeyEvent.KEYCODE_RIGHT_BRACKET,
        "GLFW_KEY_GRAVE_ACCENT" to KeyEvent.KEYCODE_GRAVE,
        "GLFW_KEY_ESCAPE" to KeyEvent.KEYCODE_ESCAPE,
        "GLFW_KEY_ENTER" to KeyEvent.KEYCODE_ENTER,
        "GLFW_KEY_TAB" to KeyEvent.KEYCODE_TAB,
        "GLFW_KEY_BACKSPACE" to KeyEvent.KEYCODE_DEL,
        "GLFW_KEY_INSERT" to KeyEvent.KEYCODE_INSERT,
        "GLFW_KEY_DELETE" to KeyEvent.KEYCODE_FORWARD_DEL,
        "GLFW_KEY_RIGHT" to KeyEvent.KEYCODE_DPAD_RIGHT,
        "GLFW_KEY_LEFT" to KeyEvent.KEYCODE_DPAD_LEFT,
        "GLFW_KEY_UP" to KeyEvent.KEYCODE_DPAD_UP,
        "GLFW_KEY_DOWN" to KeyEvent.KEYCODE_DPAD_DOWN,
        "GLFW_KEY_PAGE_UP" to KeyEvent.KEYCODE_PAGE_UP,
        "GLFW_KEY_PAGE_DOWN" to KeyEvent.KEYCODE_PAGE_DOWN,
        "GLFW_KEY_HOME" to KeyEvent.KEYCODE_MOVE_HOME,
        "GLFW_KEY_END" to KeyEvent.KEYCODE_MOVE_END,
        "GLFW_KEY_CAPS_LOCK" to KeyEvent.KEYCODE_CAPS_LOCK,
        "GLFW_KEY_SCROLL_LOCK" to KeyEvent.KEYCODE_SCROLL_LOCK,
        "GLFW_KEY_NUM_LOCK" to KeyEvent.KEYCODE_NUM_LOCK,
        "GLFW_KEY_PRINT_SCREEN" to KeyEvent.KEYCODE_SYSRQ,
        "GLFW_KEY_PAUSE" to KeyEvent.KEYCODE_BREAK,
        "GLFW_KEY_F1" to KeyEvent.KEYCODE_F1,
        "GLFW_KEY_F2" to KeyEvent.KEYCODE_F2,
        "GLFW_KEY_F3" to KeyEvent.KEYCODE_F3,
        "GLFW_KEY_F4" to KeyEvent.KEYCODE_F4,
        "GLFW_KEY_F5" to KeyEvent.KEYCODE_F5,
        "GLFW_KEY_F6" to KeyEvent.KEYCODE_F6,
        "GLFW_KEY_F7" to KeyEvent.KEYCODE_F7,
        "GLFW_KEY_F8" to KeyEvent.KEYCODE_F8,
        "GLFW_KEY_F9" to KeyEvent.KEYCODE_F9,
        "GLFW_KEY_F10" to KeyEvent.KEYCODE_F10,
        "GLFW_KEY_F11" to KeyEvent.KEYCODE_F11,
        "GLFW_KEY_F12" to KeyEvent.KEYCODE_F12,
        "GLFW_KEY_KP_0" to KeyEvent.KEYCODE_NUMPAD_0,
        "GLFW_KEY_KP_1" to KeyEvent.KEYCODE_NUMPAD_1,
        "GLFW_KEY_KP_2" to KeyEvent.KEYCODE_NUMPAD_2,
        "GLFW_KEY_KP_3" to KeyEvent.KEYCODE_NUMPAD_3,
        "GLFW_KEY_KP_4" to KeyEvent.KEYCODE_NUMPAD_4,
        "GLFW_KEY_KP_5" to KeyEvent.KEYCODE_NUMPAD_5,
        "GLFW_KEY_KP_6" to KeyEvent.KEYCODE_NUMPAD_6,
        "GLFW_KEY_KP_7" to KeyEvent.KEYCODE_NUMPAD_7,
        "GLFW_KEY_KP_8" to KeyEvent.KEYCODE_NUMPAD_8,
        "GLFW_KEY_KP_9" to KeyEvent.KEYCODE_NUMPAD_9,
        "GLFW_KEY_KP_DECIMAL" to KeyEvent.KEYCODE_NUMPAD_DOT,
        "GLFW_KEY_KP_DIVIDE" to KeyEvent.KEYCODE_NUMPAD_DIVIDE,
        "GLFW_KEY_KP_SUBTRACT" to KeyEvent.KEYCODE_NUMPAD_SUBTRACT,
        "GLFW_KEY_KP_ADD" to KeyEvent.KEYCODE_NUMPAD_ADD,
        "GLFW_KEY_KP_ENTER" to KeyEvent.KEYCODE_NUMPAD_ENTER,
        "GLFW_KEY_KP_EQUAL" to KeyEvent.KEYCODE_NUMPAD_EQUALS,
        "GLFW_KEY_LEFT_SHIFT" to KeyEvent.KEYCODE_SHIFT_LEFT,
        "GLFW_KEY_LEFT_CONTROL" to KeyEvent.KEYCODE_CTRL_LEFT,
        "GLFW_KEY_LEFT_ALT" to KeyEvent.KEYCODE_ALT_LEFT,
        "GLFW_KEY_RIGHT_SHIFT" to KeyEvent.KEYCODE_SHIFT_RIGHT,
        "GLFW_KEY_RIGHT_CONTROL" to KeyEvent.KEYCODE_CTRL_RIGHT,
        "GLFW_KEY_RIGHT_ALT" to KeyEvent.KEYCODE_ALT_RIGHT
    ).filter { ControlEventKeycode.getKeycodeFromEvent(it.first) != null }

    /** GLFW numeric code (v8 and older) -> RKB event name. */
    private val glfwCodeToName: Map<Int, String> by lazy {
        val map = HashMap<Int, String>()
        for (field in LwjglGlfwKeycode::class.java.fields) {
            val name = field.name
            if (ControlEventKeycode.getKeycodeFromEvent(name) == null) continue
            val code = try { (field.get(null) as Number).toInt() } catch (_: Exception) { continue }
            if (code <= 0) continue
            map.putIfAbsent(code, name)
        }
        map
    }

    private val glfwNameToAndroid: Map<String, Int> by lazy { keycodePairs.toMap() }
    private val androidToGlfwName: Map<Int, String> by lazy {
        keycodePairs.associate { (name, code) -> code to name }
    }

    /** True if the text looks like a Pojav control layout (and not an RKB one). */
    fun looksLikePojav(jsonString: String): Boolean = try {
        val root = parser.parseToJsonElement(jsonString).jsonObject
        root.containsKey("mControlDataList") && !root.containsKey("editorVersion")
    } catch (_: Exception) {
        false
    }

    // ---------------------------------------------------------------- import

    fun importPojav(
        jsonString: String,
        screenWidthPx: Int,
        screenHeightPx: Int,
        density: Float,
        layoutName: String
    ): ImportResult {
        val warnings = ArrayList<String>()
        val root = parser.parseToJsonElement(jsonString).jsonObject
        val list = root["mControlDataList"]
        if (list !is JsonArray) {
            throw IllegalArgumentException("Pojav layout has no valid \"mControlDataList\" array.")
        }

        val version = root["version"]?.jsonPrimitive?.intOrNull ?: 1
        if (version > SUPPORTED_VERSION) {
            warnings += "Pojav layout version $version is newer than supported ($SUPPORTED_VERSION); imported on a best-effort basis."
        }
        if (version <= 1) {
            warnings += "Old Pojav layout (v1): sizes were stored in pixels and have been converted to dp."
        }
        warnings += "RKB stores positions as percentages of free space. At a different screen aspect ratio Pojav pixel anchors may shift."

        // Pojav resizes every stored control by 100 / scaledAt when the current button
        // size preference is 100%. Evaluate dynamic expressions with those same sizes.
        val scaledAt = root["scaledAt"]?.jsonPrimitive?.floatOrNull ?: 100f
        val sizeScale = if (scaledAt > 0f) 100f / scaledAt else 1f
        if (scaledAt > 0f && Math.abs(scaledAt - 100f) > 0.01f) {
            warnings += "Pojav layout was saved at ${format(scaledAt)}% button scale; sizes were normalized to 100%."
        } else if (scaledAt <= 0f) {
            warnings += "Invalid Pojav \"scaledAt\" value; sizes were imported unchanged."
        }

        val ctx = Ctx(screenWidthPx, screenHeightPx, density, version, sizeScale)
        val styles = LinkedHashMap<StyleKey, ButtonStyle>()
        val mainButtons = ArrayList<NormalData>()
        val mainJoysticks = ArrayList<JoystickData>()
        val extraLayers = ArrayList<ControlLayer>()

        list.forEach { el ->
            convertButton(el.jsonObject, ctx, styles, warnings)?.let { mainButtons += it }
        }

        (root["mJoystickDataList"] as? JsonArray)?.forEach { el ->
            convertJoystick(el.jsonObject, ctx, warnings)?.let { mainJoysticks += it }
        }

        (root["mDrawerDataList"] as? JsonArray)?.forEach { el ->
            convertDrawer(el.jsonObject, ctx, styles, warnings)?.let {
                mainButtons += it.first
                it.second?.let { layer -> extraLayers += layer }
            }
        }

        val mainLayer = ControlLayer(
            name = "Pojav",
            uuid = randomUUID(),
            hide = false,
            visibilityType = VisibilityType.ALWAYS,
            normalButtons = mainButtons,
            joystickButtons = mainJoysticks
        )

        val layout = ControlLayout(
            info = ControlLayout.Info(
                name = TranslatableString(layoutName, emptyList()),
                author = TranslatableString("Imported from PojavLauncher", emptyList()),
                description = TranslatableString("Pojav layout version $version", emptyList()),
                versionCode = 0,
                versionName = "pojav-v$version"
            ),
            layers = listOf(mainLayer) + extraLayers,
            styles = styles.values.toList(),
            joystickStyles = emptyList(),
            editorVersion = EDITOR_VERSION
        )
        return ImportResult(layout, warnings)
    }

    private class Ctx(
        val screenW: Int,
        val screenH: Int,
        val density: Float,
        val version: Int,
        val sizeScale: Float
    )

    private data class StyleKey(val opacity: Float, val bg: Int, val stroke: Int, val strokeW: Int, val radius: Float)

    private fun dpToPx(dp: Float, ctx: Ctx) = dp * ctx.density

    /** v1 stored px; v2+ stored dp. Pojav pre-processes sizes by 100 / scaledAt. */
    private fun sourceDp(raw: Float, ctx: Ctx): Float {
        val base = if (ctx.version <= 1) raw / ctx.density else raw
        return (base * ctx.sizeScale).coerceAtLeast(MIN_DP)
    }

    private fun JsonObject.float(key: String, def: Float): Float = this[key]?.jsonPrimitive?.floatOrNull ?: def
    private fun JsonObject.int(key: String, def: Int): Int = this[key]?.jsonPrimitive?.intOrNull ?: def
    private fun JsonObject.bool(key: String, def: Boolean): Boolean = this[key]?.jsonPrimitive?.booleanOrNull ?: def

    private fun convertButton(
        o: JsonObject,
        ctx: Ctx,
        styles: MutableMap<StyleKey, ButtonStyle>,
        warnings: MutableList<String>,
        forcedEvents: List<ClickEvent>? = null,
        positionPx: Pair<Float, Float>? = null
    ): NormalData? {
        val name = o["name"]?.jsonPrimitive?.contentOrNull ?: "Button"
        val events = forcedEvents ?: eventsFromKeycodes(o, name, ctx, warnings)
        if (events.isEmpty()) {
            warnings += "Button \"$name\" was kept as a visual-only button: RKB cannot perform its Pojav action(s)."
        }

        val wDp = sourceDp(o.float("width", 50f), ctx)
        val hDp = sourceDp(o.float("height", 50f), ctx)
        val wPx = dpToPx(wDp, ctx)
        val hPx = dpToPx(hDp, ctx)
        val (xPx, yPx) = positionPx ?: (
            evalPos(o["dynamicX"], o["x"], wPx, hPx, ctx, warnings, name) to
                evalPos(o["dynamicY"], o["y"], wPx, hPx, ctx, warnings, name)
            )

        val opacity = if (ctx.version <= 1 && o.containsKey("transparency")) {
            // v1: transparency is 0..100, where 100 is fully transparent.
            ((100 - o.int("transparency", 0)) / 100f).coerceIn(0f, 1f)
        } else {
            o.float("opacity", 1f).coerceIn(0f, 1f)
        }
        val bg = o.int("bgColor", 0x4D000000)
        val stroke = o.int("strokeColor", -1)
        val strokeW = strokeDp(o.float("strokeWidth", 0f), wPx, hPx, ctx, name, warnings)
        // Pojav corner radius is percent of half the short side; RKB's RoundedCornerShape
        // percentage is relative to the full short side, so the value is halved.
        val corner = if (ctx.version <= 1 && o.bool("isRound", false)) {
            35f // v1 LayoutConverter converts isRound to a 35% Pojav corner radius.
        } else {
            o.float("cornerRadius", 0f)
        }
        val radius = (corner / 2f).coerceIn(0f, 100f)
        if (corner < 0f || corner > 100f) {
            warnings += "Button \"$name\": corner radius $corner was clamped to the supported range."
        }

        val style = styles.getOrPut(StyleKey(opacity, bg, stroke, strokeW, radius)) {
            val cfg = ButtonStyle.StyleConfig(
                alpha = opacity,
                pressedAlpha = opacity,
                backgroundColor = Color(bg),
                pressedBackgroundColor = Color(0xB3808080.toInt()),
                contentColor = Color.White,
                pressedContentColor = Color.White,
                fontSize = null,
                pressedFontSize = null,
                borderWidth = strokeW,
                pressedBorderWidth = strokeW,
                borderColor = Color(stroke),
                pressedBorderColor = Color(stroke),
                borderRadius = ButtonShape(radius),
                pressedBorderRadius = ButtonShape(radius)
            )
            ButtonStyle(
                name = "Pojav ${styles.size + 1}",
                uuid = randomUUID(),
                animateSwap = false,
                commonStyle = true,
                lightStyle = cfg,
                darkStyle = cfg
            )
        }

        val inGame = o.bool("displayInGame", true)
        val inMenu = o.bool("displayInMenu", true)
        val visibility = when {
            inGame && !inMenu -> VisibilityType.IN_GAME
            !inGame && inMenu -> VisibilityType.IN_MENU
            else -> VisibilityType.ALWAYS
        }
        if (!inGame && !inMenu) {
            warnings += "Button \"$name\" was hidden in both game and menu in Pojav; it is shown in game here."
        }

        return NormalData(
            text = TranslatableString(name, emptyList()),
            uuid = getAButtonUUID(),
            position = toPosition(xPx, yPx, wPx, hPx, ctx, name, warnings),
            buttonSize = ButtonSize(
                type = ButtonSize.Type.Dp,
                widthDp = wDp,
                heightDp = hDp,
                widthPercentage = 100,
                heightPercentage = 100,
                widthReference = ButtonSize.Reference.ScreenHeight,
                heightReference = ButtonSize.Reference.ScreenHeight
            ),
            buttonStyle = style.uuid,
            visibilityType = visibility,
            _clickEvents = events,
            isSwipple = o.bool("isSwipeable", false),
            isPenetrable = o.bool("passThruEnabled", false),
            isToggleable = o.bool("isToggle", false)
        )
    }

    /** v2-v5 stored stroke width as percent of half the larger side; v6+ as dp. */
    private fun strokeDp(
        raw: Float,
        wPx: Float,
        hPx: Float,
        ctx: Ctx,
        name: String,
        warnings: MutableList<String>
    ): Int {
        val dp = when {
            ctx.version <= 1 -> 0f
            ctx.version <= 5 -> (maxOf(wPx, hPx) / 2f) * (raw / 100f) / ctx.density
            else -> raw
        }
        val rounded = dp.roundToInt()
        if (rounded !in 0..50) {
            warnings += "Button \"$name\": stroke width ${format(dp)}dp was clamped to ${rounded.coerceIn(0, 50)}dp."
        }
        return rounded.coerceIn(0, 50)
    }

    private fun convertDrawer(
        o: JsonObject,
        ctx: Ctx,
        styles: MutableMap<StyleKey, ButtonStyle>,
        warnings: MutableList<String>
    ): Pair<NormalData, ControlLayer?>? {
        val props = o["properties"]?.jsonObject ?: run {
            warnings += "Drawer has no \"properties\"; it was skipped."
            return null
        }
        val name = props["name"]?.jsonPrimitive?.contentOrNull ?: "Drawer"
        val layerId = randomUUID()
        val ownEvents = eventsFromKeycodes(props, name, ctx, warnings)
        val headEvents = if (ownEvents.any { it.type == ClickEvent.Type.SwitchLayer && it.key == layerId }) {
            ownEvents
        } else {
            ownEvents + ClickEvent(ClickEvent.Type.SwitchLayer, layerId)
        }
        val head = convertButton(
            props, ctx, styles, warnings,
            forcedEvents = headEvents
        ) ?: return null

        val headW = dpToPx(sourceDp(props.float("width", 50f), ctx), ctx)
        val headH = dpToPx(sourceDp(props.float("height", 50f), ctx), ctx)
        val headX = evalPos(props["dynamicX"], props["x"], headW, headH, ctx, warnings, name)
        val headY = evalPos(props["dynamicY"], props["y"], headW, headH, ctx, warnings, name)
        val margin = (2f * ctx.density).toInt().toFloat()

        val orientation = when (val raw = o["orientation"]?.jsonPrimitive) {
            is JsonPrimitive -> raw.contentOrNull?.uppercase()
                ?: raw.intOrNull?.let { ControlDrawerOrientation.fromInt(it) } ?: "FREE"
            else -> "FREE"
        }
        val children = ArrayList<NormalData>()
        (o["buttonProperties"] as? JsonArray)?.forEachIndexed { i, element ->
            val child = element.jsonObject
            val childW = dpToPx(sourceDp(child.float("width", 50f), ctx), ctx)
            val childH = dpToPx(sourceDp(child.float("height", 50f), ctx), ctx)
            val (px, py) = when (orientation) {
                "DOWN" -> headX to (headY + (i + 1) * (headH + margin))
                "UP" -> headX to (headY - (i + 1) * (childH + margin))
                "LEFT" -> (headX - (i + 1) * (childW + margin)) to headY
                "RIGHT" -> (headX + (i + 1) * (headW + margin)) to headY
                else -> evalPos(child["dynamicX"], child["x"], childW, childH, ctx, warnings, name) to
                    evalPos(child["dynamicY"], child["y"], childW, childH, ctx, warnings, name)
            }
            convertButton(child, ctx, styles, warnings, positionPx = px to py)?.let { children += it }
        }

        val layer = if (children.isEmpty()) null else ControlLayer(
            name = name,
            uuid = layerId,
            hide = true,
            visibilityType = VisibilityType.ALWAYS,
            normalButtons = children
        )
        if (layer != null) {
            warnings += "Drawer \"$name\" was converted to a layer toggled by its button (Pojav orientation: $orientation)."
        } else {
            warnings += "Drawer \"$name\" has no supported child buttons; only its toggle button was imported."
        }
        return head to layer
    }

    private object ControlDrawerOrientation {
        fun fromInt(value: Int): String = when (value) {
            0 -> "DOWN"
            1 -> "LEFT"
            2 -> "UP"
            3 -> "RIGHT"
            4 -> "FREE"
            else -> "FREE"
        }
    }

    private fun convertJoystick(
        o: JsonObject,
        ctx: Ctx,
        warnings: MutableList<String>
    ): JoystickData? {
        val name = o["name"]?.jsonPrimitive?.contentOrNull ?: "Joystick"
        var wDp = sourceDp(o.float("width", 100f), ctx)
        var hDp = sourceDp(o.float("height", 100f), ctx)
        var dx = o["dynamicX"]
        var dy = o["dynamicY"]
        if ((ctx.version == 6 || ctx.version == 7) && hDp > wDp) {
            // Pojav LayoutConverter.convertV6_7Layout: make the joystick square and
            // rebalance expressions that referenced its old height.
            val ratio = hDp / wDp
            dx = scaleHeightExpression(dx, ratio, appendDelta = false)
            dy = scaleHeightExpression(dy, ratio, appendDelta = true)
            warnings += "Joystick \"$name\" used the v6/v7 non-square fix; height was normalized to its width."
            hDp = wDp
        }
        val sizeDp = maxOf(wDp, hDp).coerceAtLeast(20f)
        val px = dpToPx(sizeDp, ctx)
        val xPx = evalPos(dx, o["x"], px, px, ctx, warnings, name)
        val yPx = evalPos(dy, o["y"], px, px, ctx, warnings, name)
        return JoystickData(
            uuid = getAButtonUUID(),
            position = toPosition(xPx, yPx, px, px, ctx, name, warnings),
            sizeType = ButtonSize.Type.Dp,
            sizeDp = sizeDp,
            canLock = o.bool("forwardLock", false),
            triggerMode = if (o.bool("absolute", false)) JoystickTriggerMode.TOUCH else JoystickTriggerMode.DRAG,
            visibilityType = if (o.bool("displayInGame", true) && !o.bool("displayInMenu", true)) {
                VisibilityType.IN_GAME
            } else {
                VisibilityType.ALWAYS
            }
        )
    }

    private fun scaleHeightExpression(value: JsonElement?, ratio: Float, appendDelta: Boolean): JsonElement? {
        val expr = value?.jsonPrimitive?.contentOrNull ?: return value
        val scaled = expr.replace("\${height}", "(${(format(ratio))} * \${height})")
        val result = if (appendDelta) "$scaled + ((${(format(ratio - 1f))}) * \${height})" else scaled
        return JsonPrimitive(result)
    }

    private fun eventsFromKeycodes(
        o: JsonObject,
        name: String,
        ctx: Ctx,
        warnings: MutableList<String>
    ): List<ClickEvent> {
        val out = ArrayList<ClickEvent>()
        if (ctx.version <= 1) {
            // v1 layouts have one GLFW keycode plus separate modifier flags.
            val modifiers = listOf(
                "holdShift" to KeyEvent.KEYCODE_SHIFT_LEFT,
                "holdCtrl" to KeyEvent.KEYCODE_CTRL_LEFT,
                "holdAlt" to KeyEvent.KEYCODE_ALT_LEFT
            )
            modifiers.forEach { (field, androidCode) ->
                if (o.bool(field, false)) {
                    androidToGlfwName[androidCode]?.let { out += ClickEvent(ClickEvent.Type.Key, it) }
                }
            }
            val code = o.int("keycode", -1)
            if (code > 0) {
                val keyName = glfwCodeToName[code]
                if (keyName != null) {
                    out += ClickEvent(ClickEvent.Type.Key, keyName)
                } else {
                    warnings += "Button \"$name\": unknown v1 GLFW key code $code ignored."
                }
            }
            return out
        }
        val arr = o["keycodes"] as? JsonArray ?: return out
        for (el in arr) {
            val code = (el as? JsonPrimitive)?.intOrNull ?: continue
            if (code == 0) continue
            val ev: ClickEvent? = when {
                code > 0 -> {
                    val keyName = if (ctx.version <= 8) {
                        glfwCodeToName[code]
                    } else {
                        androidToGlfwName[code]
                    }
                    keyName?.let { ClickEvent(ClickEvent.Type.Key, it) }
                        ?: run {
                            warnings += "Button \"$name\": unknown ${if (ctx.version <= 8) "GLFW" else "Android"} key code $code ignored."
                            null
                        }
                }
                code == SPECIAL_KEYBOARD -> ClickEvent(ClickEvent.Type.LauncherEvent, EV_SWITCH_IME)
                code == SPECIAL_MENU -> ClickEvent(ClickEvent.Type.LauncherEvent, EV_SWITCH_MENU)
                code == SPECIAL_MOUSEPRI -> ClickEvent(ClickEvent.Type.Key, ControlEventKeycode.GLFW_MOUSE_BUTTON_LEFT)
                code == SPECIAL_MOUSESEC -> ClickEvent(ClickEvent.Type.Key, ControlEventKeycode.GLFW_MOUSE_BUTTON_RIGHT)
                code == SPECIAL_MOUSEMID -> ClickEvent(ClickEvent.Type.Key, ControlEventKeycode.GLFW_MOUSE_BUTTON_MIDDLE)
                code == SPECIAL_SCROLLUP -> ClickEvent(ClickEvent.Type.LauncherEvent, EV_SCROLL_UP)
                code == SPECIAL_SCROLLDOWN -> ClickEvent(ClickEvent.Type.LauncherEvent, EV_SCROLL_DOWN)
                code == SPECIAL_TOGGLECTRL -> {
                    warnings += "Button \"$name\": Pojav \"GUI / hide controls\" action is not available in RKB."
                    null
                }
                code == SPECIAL_VIRTUALMOUSE -> {
                    warnings += "Button \"$name\": Pojav \"virtual mouse\" action is not available in RKB."
                    null
                }
                code == SPECIAL_KEYBOARDPAN -> {
                    warnings += "Button \"$name\": Pojav \"keyboard pan\" action is not available in RKB."
                    null
                }
                else -> {
                    warnings += "Button \"$name\": unsupported Pojav special key code $code ignored."
                    null
                }
            }
            if (ev != null && ev !in out) out += ev
        }
        return out
    }

    private fun toPosition(
        xPx: Float,
        yPx: Float,
        wPx: Float,
        hPx: Float,
        ctx: Ctx,
        name: String,
        warnings: MutableList<String>
    ): ButtonPosition {
        val freeX = ctx.screenW - wPx
        val freeY = ctx.screenH - hPx
        if (xPx < -1f || yPx < -1f || xPx > freeX + 1f || yPx > freeY + 1f) {
            warnings += "Button \"$name\" extends outside the import viewport (${ctx.screenW}x${ctx.screenH}); its position was clamped to the edge."
        }
        val px = if (freeX > 0f) (xPx / freeX * 10000f).roundToInt().coerceIn(0, 10000) else 0
        val py = if (freeY > 0f) (yPx / freeY * 10000f).roundToInt().coerceIn(0, 10000) else 0
        return ButtonPosition(px, py)
    }

    /**
     * Evaluate Pojav's dynamicX/dynamicY expression in pixels, falling back to the
     * legacy absolute x/y field when there is no expression.
     */
    private fun evalPos(
        dynamic: JsonElement?,
        legacy: JsonElement?,
        wPx: Float,
        hPx: Float,
        ctx: Ctx,
        warnings: MutableList<String>,
        name: String
    ): Float {
        val expr = dynamic?.jsonPrimitive?.contentOrNull
        if (expr.isNullOrBlank()) return legacy?.jsonPrimitive?.floatOrNull ?: 0f
        val margin = (2f * ctx.density).toInt().toFloat()
        val vars = mapOf(
            "top" to 0f, "left" to 0f,
            "right" to (ctx.screenW - wPx), "bottom" to (ctx.screenH - hPx),
            "width" to wPx, "height" to hPx,
            "screen_width" to ctx.screenW.toFloat(), "screen_height" to ctx.screenH.toFloat(),
            "margin" to margin, "preferred_scale" to 100f
        )
        return try {
            ExprEval(expr, vars, ctx.density).evaluate()
        } catch (e: Exception) {
            warnings += "Button \"$name\": could not evaluate position \"$expr\" (${e.message ?: "invalid expression"}); placed at the top-left."
            0f
        }
    }

    // ---------------------------------------------------------------- export

    fun exportToPojav(
        layout: ControlLayout,
        screenWidthPx: Int,
        screenHeightPx: Int,
        density: Float
    ): ExportResult {
        val warnings = ArrayList<String>()
        val ctx = Ctx(screenWidthPx, screenHeightPx, density, SUPPORTED_VERSION, 1f)
        val styleById = layout.styles.associateBy { it.uuid }
        val buttons = ArrayList<JsonElement>()
        val joysticks = ArrayList<JsonElement>()

        for (layer in layout.layers) {
            if (layer.hide) {
                warnings += "Layer \"${layer.name}\" is hidden (toggled by buttons); Pojav has no layer switching, so it was not exported."
                continue
            }
            for (b in layer.normalButtons) {
                val codes = keycodesFor(b.clickEvents, b.text.default, warnings)
                if (codes.isEmpty()) {
                    warnings += "Button \"${b.text.default}\" has no action Pojav can run; exported as a visual-only button."
                }
                val (wDp, hDp) = sizeDp(b.buttonSize, b.text.default, ctx, warnings)
                val cfg = b.buttonStyle?.let { styleById[it] }?.lightStyle
                buttons += buildJsonObject {
                    put("name", b.text.default)
                    put("keycodes", buildJsonArray { codes.forEach { add(JsonPrimitive(it)) } })
                    put("dynamicX", dynExpr(b.position.xPercentage(), "screen_width", "width"))
                    put("dynamicY", dynExpr(b.position.yPercentage(), "screen_height", "height"))
                    put("width", wDp)
                    put("height", hDp)
                    put("opacity", cfg?.alpha ?: 1f)
                    put("bgColor", cfg?.backgroundColor?.toArgbInt() ?: 0x4D000000)
                    put("strokeColor", cfg?.borderColor?.toArgbInt() ?: -1)
                    put("strokeWidth", (cfg?.borderWidth ?: 0).toFloat())
                    put("cornerRadius", ((cfg?.borderRadius?.topStart ?: 0f) * 2f).coerceIn(0f, 100f))
                    put("isToggle", b.isToggleable)
                    put("isSwipeable", b.isSwipple)
                    put("passThruEnabled", b.isPenetrable)
                    put("isDynamicBtn", false)
                    put("displayInGame", b.visibilityType != VisibilityType.IN_MENU)
                    put("displayInMenu", b.visibilityType != VisibilityType.IN_GAME)
                }
            }
            for (j in layer.joystickButtons) {
                val dp = if (j.sizeType == ButtonSize.Type.Percentage) {
                    j.sizePercentage / 10000f * ctx.screenH / ctx.density
                } else {
                    j.sizeDp
                }
                joysticks += buildJsonObject {
                    put("name", "joystick")
                    put("keycodes", buildJsonArray {
                        listOf(KeyEvent.KEYCODE_W, KeyEvent.KEYCODE_A, KeyEvent.KEYCODE_S, KeyEvent.KEYCODE_D)
                            .forEach { add(JsonPrimitive(it)) }
                    })
                    put("dynamicX", dynExpr(j.position.xPercentage(), "screen_width", "width"))
                    put("dynamicY", dynExpr(j.position.yPercentage(), "screen_height", "height"))
                    put("width", dp)
                    put("height", dp)
                    put("opacity", 1f)
                    put("bgColor", 0x4D000000)
                    put("strokeColor", -1)
                    put("strokeWidth", 0f)
                    put("cornerRadius", 100f)
                    put("isToggle", false)
                    put("isSwipeable", false)
                    put("passThruEnabled", false)
                    put("displayInGame", j.visibilityType != VisibilityType.IN_MENU)
                    put("displayInMenu", j.visibilityType != VisibilityType.IN_GAME)
                    put("forwardLock", j.canLock)
                    put("absolute", j.triggerMode == JoystickTriggerMode.TOUCH)
                }
            }
        }

        val root = buildJsonObject {
            put("mControlDataList", JsonArray(buttons))
            put("mDrawerDataList", JsonArray(emptyList()))
            put("mJoystickDataList", JsonArray(joysticks))
            put("scaledAt", 100f)
            put("version", SUPPORTED_VERSION)
        }
        warnings += "Zalith button styles are reduced to Pojav's single style (opacity, colors, border, corner)."
        return ExportResult(writer.encodeToString(JsonObject.serializer(), root), warnings)
    }

    private fun androidx.compose.ui.graphics.Color.toArgbInt(): Int {
        val a = (alpha * 255f).roundToInt().coerceIn(0, 255)
        val r = (red * 255f).roundToInt().coerceIn(0, 255)
        val g = (green * 255f).roundToInt().coerceIn(0, 255)
        val b = (blue * 255f).roundToInt().coerceIn(0, 255)
        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }

    /** Resolution independent: percent of the free space (screen - widget), exactly like RKB. */
    private fun dynExpr(percentage: Float, screenVar: String, sizeVar: String): String =
        "(\${$screenVar} - \${$sizeVar}) * ${format(percentage)}"

    private fun sizeDp(size: ButtonSize, name: String, ctx: Ctx, warnings: MutableList<String>): Pair<Float, Float> =
        when (size.type) {
            ButtonSize.Type.Dp -> size.widthDp to size.heightDp
            ButtonSize.Type.Percentage -> {
                val wRef = if (size.widthReference == ButtonSize.Reference.ScreenWidth) ctx.screenW else ctx.screenH
                val hRef = if (size.heightReference == ButtonSize.Reference.ScreenWidth) ctx.screenW else ctx.screenH
                (size.widthPercentage / 10000f * wRef / ctx.density) to
                    (size.heightPercentage / 10000f * hRef / ctx.density)
            }
            else -> {
                warnings += "Button \"$name\" auto-sizes to its text; exported with an estimated size."
                (50f + name.length * 8f) to 40f
            }
        }

    /** RKB GLFW event names -> Pojav v9 Android keycodes. */
    private fun keycodesFor(events: List<ClickEvent>, name: String, warnings: MutableList<String>): List<Int> {
        val out = ArrayList<Int>()
        for (e in events) {
            val code: Int? = when (e.type) {
                ClickEvent.Type.Key -> when (e.key) {
                    ControlEventKeycode.GLFW_MOUSE_BUTTON_LEFT -> SPECIAL_MOUSEPRI
                    ControlEventKeycode.GLFW_MOUSE_BUTTON_RIGHT -> SPECIAL_MOUSESEC
                    ControlEventKeycode.GLFW_MOUSE_BUTTON_MIDDLE -> SPECIAL_MOUSEMID
                    else -> glfwNameToAndroid[e.key]
                }
                ClickEvent.Type.LauncherEvent -> when (e.key) {
                    EV_SWITCH_IME -> SPECIAL_KEYBOARD
                    EV_SWITCH_MENU -> SPECIAL_MENU
                    EV_SCROLL_UP, "launcher.event.scroll_up.single" -> SPECIAL_SCROLLUP
                    EV_SCROLL_DOWN, "launcher.event.scroll_down.single" -> SPECIAL_SCROLLDOWN
                    else -> null
                }
                else -> null
            }
            if (code == null) {
                warnings += "Button \"$name\": action \"${e.type}\" (${e.key}) cannot be exported to Pojav."
            } else if (code !in out) {
                out += code
            }
        }
        if (out.size > 4) {
            warnings += "Button \"$name\" has more than 4 actions; Pojav keeps only the first 4."
            return out.take(4)
        }
        return out
    }

    private fun format(value: Float): String = String.format(java.util.Locale.US, "%.5f", value)
}

/**
 * Small arithmetic evaluator for Pojav dynamic positions. Supports the operators
 * and helpers exposed by Pojav's expression parser (exp4j): + - * / % ^, dp, px,
 * abs, sqrt, min, max, floor, ceil, round, pow.
 */
private class ExprEval(src: String, vars: Map<String, Float>, private val density: Float) {
    private val s: String
    private var pos = 0

    init {
        var text = src
        val re = Regex("\\$\\{(\\w+)}")
        text = re.replace(text) { m ->
            val v = vars[m.groupValues[1]] ?: throw IllegalArgumentException("unknown variable \${${m.groupValues[1]}}")
            "(" + v.toString() + ")"
        }
        s = text.replace(" ", "")
    }

    fun evaluate(): Float {
        val v = parseExpr()
        if (pos != s.length) throw IllegalArgumentException("unexpected '${s[pos]}'")
        return v.toFloat()
    }

    private fun parseExpr(): Double {
        var v = parseTerm()
        while (pos < s.length) {
            when (s[pos]) {
                '+' -> { pos++; v += parseTerm() }
                '-' -> { pos++; v -= parseTerm() }
                else -> return v
            }
        }
        return v
    }

    private fun parseTerm(): Double {
        var v = parsePower()
        while (pos < s.length) {
            when (s[pos]) {
                '*' -> { pos++; v *= parsePower() }
                '/' -> { pos++; v /= parsePower() }
                '%' -> { pos++; v %= parsePower() }
                else -> return v
            }
        }
        return v
    }

    private fun parsePower(): Double {
        val base = parseUnary()
        if (pos < s.length && s[pos] == '^') {
            pos++
            return Math.pow(base, parsePower())
        }
        return base
    }

    private fun parseUnary(): Double {
        if (pos < s.length && s[pos] == '-') { pos++; return -parseUnary() }
        if (pos < s.length && s[pos] == '+') { pos++; return parseUnary() }
        return parsePrimary()
    }

    private fun parsePrimary(): Double {
        if (pos >= s.length) throw IllegalArgumentException("unexpected end")
        val c = s[pos]
        if (c == '(') {
            pos++
            val v = parseExpr()
            expect(')')
            return v
        }
        if (c.isLetter()) {
            val start = pos
            while (pos < s.length && (s[pos].isLetterOrDigit() || s[pos] == '_')) pos++
            val fn = s.substring(start, pos).lowercase()
            expect('(')
            val args = ArrayList<Double>()
            args += parseExpr()
            while (pos < s.length && s[pos] == ',') {
                pos++
                args += parseExpr()
            }
            expect(')')
            return applyFunction(fn, args)
        }
        val start = pos
        while (pos < s.length && (s[pos].isDigit() || s[pos] == '.')) pos++
        if (pos < s.length && (s[pos] == 'E' || s[pos] == 'e')) {
            pos++
            if (pos < s.length && (s[pos] == '+' || s[pos] == '-')) pos++
            while (pos < s.length && s[pos].isDigit()) pos++
        }
        if (start == pos) throw IllegalArgumentException("unexpected '$c'")
        return s.substring(start, pos).toDouble()
    }

    private fun applyFunction(fn: String, args: List<Double>): Double {
        fun arg(index: Int) = args.getOrNull(index) ?: throw IllegalArgumentException("$fn needs another argument")
        return when (fn) {
            "dp" -> arg(0) / density
            "px" -> arg(0) * density
            "abs" -> Math.abs(arg(0))
            "sqrt" -> Math.sqrt(arg(0))
            "min" -> Math.min(arg(0), arg(1))
            "max" -> Math.max(arg(0), arg(1))
            "floor" -> Math.floor(arg(0))
            "ceil" -> Math.ceil(arg(0))
            "round" -> Math.round(arg(0)).toDouble()
            "pow" -> Math.pow(arg(0), arg(1))
            "sin" -> Math.sin(arg(0))
            "cos" -> Math.cos(arg(0))
            "tan" -> Math.tan(arg(0))
            else -> throw IllegalArgumentException("unknown function $fn")
        }
    }

    private fun expect(ch: Char) {
        if (pos >= s.length || s[pos] != ch) throw IllegalArgumentException("expected '$ch'")
        pos++
    }
}
