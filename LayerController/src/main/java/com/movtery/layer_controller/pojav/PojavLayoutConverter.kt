/*
 * RKB LAUNCHER - PojavLauncher control layout importer / exporter.
 *
 * Reads and writes the PojavLauncher "CustomControls" JSON (versions 2-8:
 * mControlDataList / mDrawerDataList / mJoystickDataList, dynamicX/dynamicY expressions,
 * keycodes[4] with negative special codes) and converts it to/from the Zalith layer format.
 *
 * Format knowledge comes from PojavLauncher's customcontrols package (LGPL-3.0).
 * This module is part of a GPL-3.0 combined work (see RKB_NOTICE.md).
 */
package com.movtery.layer_controller.pojav

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
    // Pojav special keycodes (ControlData.SPECIALBTN_*)
    private const val SPECIAL_KEYBOARD = -1
    private const val SPECIAL_TOGGLECTRL = -2
    private const val SPECIAL_MOUSEPRI = -3
    private const val SPECIAL_MOUSESEC = -4
    private const val SPECIAL_VIRTUALMOUSE = -5
    private const val SPECIAL_MOUSEMID = -6
    private const val SPECIAL_SCROLLUP = -7
    private const val SPECIAL_SCROLLDOWN = -8
    private const val SPECIAL_MENU = -9

    // Launcher event ids used by Zalith (same values as ui/control/event/Events.kt)
    private const val EV_SWITCH_IME = "launcher.event.switch_ime"
    private const val EV_SWITCH_MENU = "launcher.event.switch_menu"
    private const val EV_SCROLL_UP = "launcher.event.scroll_up"
    private const val EV_SCROLL_UP_SINGLE = "launcher.event.scroll_up.single"
    private const val EV_SCROLL_DOWN = "launcher.event.scroll_down"
    private const val EV_SCROLL_DOWN_SINGLE = "launcher.event.scroll_down.single"

    private const val POJAV_LATEST_VERSION = 8
    private const val MIN_DP = 5f

    class ImportResult(val layout: ControlLayout, val warnings: List<String>)
    class ExportResult(val json: String, val warnings: List<String>)

    private val parser = Json { ignoreUnknownKeys = true; isLenient = true }
    private val writer = Json { prettyPrint = false }

    /** GLFW key code -> Zalith key name ("GLFW_KEY_W"), built once from the real key tables. */
    private val codeToKeyName: Map<Int, String> by lazy {
        val map = HashMap<Int, String>()
        for (field in LwjglGlfwKeycode::class.java.fields) {
            val name = field.name
            if (!name.startsWith("GLFW_KEY_")) continue
            val code = try { (field.get(null) as Number).toInt() } catch (_: Exception) { continue }
            if (code <= 0) continue
            if (ControlEventKeycode.getKeycodeFromEvent(name) == null) continue
            map.putIfAbsent(code, name)
        }
        map
    }

    /** True if the text looks like a PojavLauncher control layout (and not a Zalith one). */
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
        val version = root["version"]?.jsonPrimitive?.intOrNull ?: 1
        if (version > POJAV_LATEST_VERSION) warnings += "Layout version $version is newer than supported ($POJAV_LATEST_VERSION); imported on a best-effort basis."
        if (version < 3) warnings += "Old layout version $version: sizes were stored differently, buttons may need resizing."

        val ctx = Ctx(maxOf(screenWidthPx, screenHeightPx), minOf(screenWidthPx, screenHeightPx), density)
        val styles = LinkedHashMap<StyleKey, ButtonStyle>()
        val mainButtons = ArrayList<NormalData>()
        val mainJoysticks = ArrayList<JoystickData>()
        val extraLayers = ArrayList<ControlLayer>()

        (root["mControlDataList"] as? JsonArray)?.forEach { el ->
            convertButton(el.jsonObject, version, ctx, styles, warnings)?.let { mainButtons += it }
        }

        (root["mJoystickDataList"] as? JsonArray)?.forEach { el ->
            convertJoystick(el.jsonObject, version, ctx, warnings)?.let { mainJoysticks += it }
        }

        (root["mDrawerDataList"] as? JsonArray)?.forEach { el ->
            val drawer = el.jsonObject
            val props = drawer["properties"]?.jsonObject ?: return@forEach
            val children = (drawer["buttonProperties"] as? JsonArray)?.map { it.jsonObject } ?: emptyList()
            val orientation = drawer["orientation"]?.jsonPrimitive?.contentOrNull ?: "FREE"
            val layerId = randomUUID()
            val drawerName = props["name"]?.jsonPrimitive?.contentOrNull ?: "Drawer"

            // The drawer button itself toggles a hidden layer that holds its children.
            val head = convertButton(props, version, ctx, styles, warnings, forcedEvents = listOf(ClickEvent(ClickEvent.Type.SwitchLayer, layerId)))
            if (head != null) mainButtons += head

            val headW = dpToPx(props.float("width", 50f), ctx)
            val headH = dpToPx(props.float("height", 50f), ctx)
            val headX = evalPos(props, "dynamicX", "x", headW, headH, ctx, warnings)
            val headY = evalPos(props, "dynamicY", "y", headW, headH, ctx, warnings)
            val margin = 2f * ctx.density
            val childButtons = ArrayList<NormalData>()
            children.forEachIndexed { i, child ->
                val cw = dpToPx(child.float("width", 50f), ctx)
                val ch = dpToPx(child.float("height", 50f), ctx)
                val (px, py) = when (orientation.uppercase()) {
                    "DOWN" -> headX to (headY + (i + 1) * (headH + margin))
                    "UP" -> headX to (headY - (i + 1) * (ch + margin))
                    "LEFT" -> (headX - (i + 1) * (cw + margin)) to headY
                    "RIGHT" -> (headX + (i + 1) * (headW + margin)) to headY
                    else -> evalPos(child, "dynamicX", "x", cw, ch, ctx, warnings) to evalPos(child, "dynamicY", "y", cw, ch, ctx, warnings)
                }
                convertButton(child, version, ctx, styles, warnings, positionPx = px to py)?.let { childButtons += it }
            }
            if (childButtons.isNotEmpty()) {
                extraLayers += ControlLayer(
                    name = drawerName,
                    uuid = layerId,
                    hide = true,
                    visibilityType = VisibilityType.ALWAYS,
                    normalButtons = childButtons
                )
                warnings += "Drawer \"$drawerName\" was converted to a hidden layer toggled by its button."
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

    private class Ctx(val screenW: Int, val screenH: Int, val density: Float)

    private data class StyleKey(val opacity: Float, val bg: Int, val stroke: Int, val strokeW: Int, val radius: Float)

    private fun dpToPx(dp: Float, ctx: Ctx) = dp * ctx.density

    private fun JsonObject.float(key: String, def: Float): Float = this[key]?.jsonPrimitive?.floatOrNull ?: def
    private fun JsonObject.int(key: String, def: Int): Int = this[key]?.jsonPrimitive?.intOrNull ?: def
    private fun JsonObject.bool(key: String, def: Boolean): Boolean = this[key]?.jsonPrimitive?.booleanOrNull ?: def

    private fun convertButton(
        o: JsonObject,
        version: Int,
        ctx: Ctx,
        styles: MutableMap<StyleKey, ButtonStyle>,
        warnings: MutableList<String>,
        forcedEvents: List<ClickEvent>? = null,
        positionPx: Pair<Float, Float>? = null
    ): NormalData? {
        val name = o["name"]?.jsonPrimitive?.contentOrNull ?: ""
        val events = forcedEvents ?: eventsFromKeycodes(o, name, warnings)
        if (events.isEmpty()) {
            warnings += "Button \"$name\" skipped: it only used actions RKB does not support."
            return null
        }
        val wDp = o.float("width", 50f).coerceAtLeast(MIN_DP)
        val hDp = o.float("height", 50f).coerceAtLeast(MIN_DP)
        val wPx = dpToPx(wDp, ctx)
        val hPx = dpToPx(hDp, ctx)
        val (xPx, yPx) = positionPx ?: (evalPos(o, "dynamicX", "x", wPx, hPx, ctx, warnings) to evalPos(o, "dynamicY", "y", wPx, hPx, ctx, warnings))

        val opacity = o.float("opacity", 1f).coerceIn(0f, 1f)
        val bg = o.int("bgColor", 0x4D000000)
        val stroke = o.int("strokeColor", -1)
        val strokeW = o.float("strokeWidth", 0f).roundToInt().coerceIn(0, 50)
        val radius = o.float("cornerRadius", 0f).coerceIn(0f, 100f)
        val key = StyleKey(opacity, bg, stroke, strokeW, radius)
        val style = styles.getOrPut(key) {
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
            ButtonStyle(name = "Pojav ${styles.size + 1}", uuid = randomUUID(), animateSwap = false, commonStyle = true, lightStyle = cfg, darkStyle = cfg)
        }

        val inGame = o.bool("displayInGame", true)
        val inMenu = o.bool("displayInMenu", true)
        val visibility = when {
            inGame && !inMenu -> VisibilityType.IN_GAME
            !inGame && inMenu -> VisibilityType.IN_MENU
            else -> VisibilityType.ALWAYS
        }
        if (!inGame && !inMenu) warnings += "Button \"$name\" was hidden everywhere in Pojav; it is shown always here."

        return NormalData(
            text = TranslatableString(name, emptyList()),
            uuid = getAButtonUUID(),
            position = toPosition(xPx, yPx, wPx, hPx, ctx),
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

    private fun convertJoystick(
        o: JsonObject,
        version: Int,
        ctx: Ctx,
        warnings: MutableList<String>
    ): JoystickData? {
        var wDp = o.float("width", 100f)
        var hDp = o.float("height", 100f)
        if (version < 8 && hDp > wDp) hDp = wDp // Pojav v6/7 joystick fix: always square
        val sizeDp = maxOf(wDp, hDp).coerceAtLeast(20f)
        val px = dpToPx(sizeDp, ctx)
        val xPx = evalPos(o, "dynamicX", "x", px, px, ctx, warnings)
        val yPx = evalPos(o, "dynamicY", "y", px, px, ctx, warnings)
        return JoystickData(
            uuid = getAButtonUUID(),
            position = toPosition(xPx, yPx, px, px, ctx),
            sizeType = ButtonSize.Type.Dp,
            sizeDp = sizeDp,
            canLock = o.bool("forwardLock", false),
            triggerMode = if (o.bool("absolute", false)) JoystickTriggerMode.TOUCH else JoystickTriggerMode.DRAG,
            visibilityType = if (o.bool("displayInGame", true) && !o.bool("displayInMenu", true)) VisibilityType.IN_GAME else VisibilityType.ALWAYS
        )
    }

    private fun eventsFromKeycodes(o: JsonObject, name: String, warnings: MutableList<String>): List<ClickEvent> {
        val out = ArrayList<ClickEvent>()
        val arr = (o["keycodes"] as? JsonArray) ?: return out
        for (el in arr) {
            val code = (el as? JsonPrimitive)?.intOrNull ?: continue
            if (code == 0) continue
            val ev: ClickEvent? = when {
                code > 0 -> codeToKeyName[code]?.let { ClickEvent(ClickEvent.Type.Key, it) }
                    ?: run { warnings += "Button \"$name\": unknown key code $code ignored."; null }
                code == SPECIAL_KEYBOARD -> ClickEvent(ClickEvent.Type.LauncherEvent, EV_SWITCH_IME)
                code == SPECIAL_MENU -> ClickEvent(ClickEvent.Type.LauncherEvent, EV_SWITCH_MENU)
                code == SPECIAL_MOUSEPRI -> ClickEvent(ClickEvent.Type.Key, ControlEventKeycode.GLFW_MOUSE_BUTTON_LEFT)
                code == SPECIAL_MOUSESEC -> ClickEvent(ClickEvent.Type.Key, ControlEventKeycode.GLFW_MOUSE_BUTTON_RIGHT)
                code == SPECIAL_MOUSEMID -> ClickEvent(ClickEvent.Type.Key, ControlEventKeycode.GLFW_MOUSE_BUTTON_MIDDLE)
                code == SPECIAL_SCROLLUP -> ClickEvent(ClickEvent.Type.LauncherEvent, EV_SCROLL_UP)
                code == SPECIAL_SCROLLDOWN -> ClickEvent(ClickEvent.Type.LauncherEvent, EV_SCROLL_DOWN)
                code == SPECIAL_TOGGLECTRL -> { warnings += "Button \"$name\": \"GUI\" (hide controls) has no RKB equivalent."; null }
                code == SPECIAL_VIRTUALMOUSE -> { warnings += "Button \"$name\": \"virtual mouse\" toggle has no RKB equivalent."; null }
                else -> { warnings += "Button \"$name\": special code $code not supported."; null }
            }
            if (ev != null && out.none { it == ev }) out += ev
        }
        return out
    }

    private fun toPosition(xPx: Float, yPx: Float, wPx: Float, hPx: Float, ctx: Ctx): ButtonPosition {
        val freeX = ctx.screenW - wPx
        val freeY = ctx.screenH - hPx
        val px = if (freeX > 0f) (xPx / freeX * 10000f).roundToInt().coerceIn(0, 10000) else 0
        val py = if (freeY > 0f) (yPx / freeY * 10000f).roundToInt().coerceIn(0, 10000) else 0
        return ButtonPosition(px, py)
    }

    /** Evaluate Pojav's dynamicX / dynamicY expression (pixels). Falls back to the legacy absolute x / y field. */
    private fun evalPos(o: JsonObject, dynKey: String, legacyKey: String, wPx: Float, hPx: Float, ctx: Ctx, warnings: MutableList<String>): Float {
        val expr = o[dynKey]?.jsonPrimitive?.contentOrNull
        if (expr.isNullOrBlank()) return o.float(legacyKey, 0f)
        val vars = mapOf(
            "top" to 0f, "left" to 0f,
            "right" to ctx.screenW - wPx, "bottom" to ctx.screenH - hPx,
            "width" to wPx, "height" to hPx,
            "screen_width" to ctx.screenW.toFloat(), "screen_height" to ctx.screenH.toFloat(),
            "margin" to 2f * ctx.density, "preferred_scale" to 100f
        )
        return try {
            ExprEval(expr, vars, ctx.density).evaluate()
        } catch (e: Exception) {
            warnings += "Could not evaluate position \"$expr\"; the button was placed at the top-left."
            0f
        }
    }

    // ---------------------------------------------------------------- export

    fun exportToPojav(layout: ControlLayout, screenWidthPx: Int, screenHeightPx: Int, density: Float): ExportResult {
        val warnings = ArrayList<String>()
        val ctx = Ctx(maxOf(screenWidthPx, screenHeightPx), minOf(screenWidthPx, screenHeightPx), density)
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
                    warnings += "Button \"${b.text.default}\" skipped: it has no action Pojav can run."
                    continue
                }
                val (wDp, hDp) = sizeDp(b.buttonSize, b.text.default, ctx, warnings)
                val st = b.buttonStyle?.let { styleById[it] }
                val cfg = st?.let { if (it.commonStyle) it.lightStyle else it.lightStyle }
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
                    put("cornerRadius", (cfg?.borderRadius?.topStart ?: 0f).coerceIn(0f, 100f))
                    put("isToggle", b.isToggleable)
                    put("isSwipeable", b.isSwipple)
                    put("passThruEnabled", b.isPenetrable)
                    put("isDynamicBtn", false)
                    put("displayInGame", b.visibilityType != VisibilityType.IN_MENU)
                    put("displayInMenu", b.visibilityType != VisibilityType.IN_GAME)
                }
            }
            for (j in layer.joystickButtons) {
                val dp = j.sizeDp.coerceAtLeast(20f).let {
                    if (j.sizeType == ButtonSize.Type.Percentage) (j.sizePercentage / 10000f * ctx.screenH / ctx.density) else it
                }
                joysticks += buildJsonObject {
                    put("name", "joystick")
                    put("keycodes", buildJsonArray { listOf(87, 65, 83, 68).forEach { add(JsonPrimitive(it)) } })
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
            put("version", POJAV_LATEST_VERSION)
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

    /** Resolution independent: percent of the free space (screen - widget), exactly like Zalith. */
    private fun dynExpr(percentage: Float, screenVar: String, sizeVar: String): String =
        "(\${$screenVar} - \${$sizeVar}) * ${"%.5f".format(java.util.Locale.US, percentage)}"

    private fun sizeDp(size: ButtonSize, name: String, ctx: Ctx, warnings: MutableList<String>): Pair<Float, Float> = when (size.type) {
        ButtonSize.Type.Dp -> size.widthDp to size.heightDp
        ButtonSize.Type.Percentage -> {
            val wRef = if (size.widthReference == ButtonSize.Reference.ScreenWidth) ctx.screenW else ctx.screenH
            val hRef = if (size.heightReference == ButtonSize.Reference.ScreenWidth) ctx.screenW else ctx.screenH
            (size.widthPercentage / 10000f * wRef / ctx.density) to (size.heightPercentage / 10000f * hRef / ctx.density)
        }
        else -> {
            warnings += "Button \"$name\" auto-sizes to its text; exported with an estimated size."
            (50f + name.length * 8f) to 40f
        }
    }

    private fun keycodesFor(events: List<ClickEvent>, name: String, warnings: MutableList<String>): List<Int> {
        val out = ArrayList<Int>()
        for (e in events) {
            val code: Int? = when (e.type) {
                ClickEvent.Type.Key -> when (e.key) {
                    ControlEventKeycode.GLFW_MOUSE_BUTTON_LEFT -> SPECIAL_MOUSEPRI
                    ControlEventKeycode.GLFW_MOUSE_BUTTON_RIGHT -> SPECIAL_MOUSESEC
                    ControlEventKeycode.GLFW_MOUSE_BUTTON_MIDDLE -> SPECIAL_MOUSEMID
                    else -> ControlEventKeycode.getKeycodeFromEvent(e.key)?.toInt()?.takeIf { it > 0 }
                }
                ClickEvent.Type.LauncherEvent -> when (e.key) {
                    EV_SWITCH_IME -> SPECIAL_KEYBOARD
                    EV_SWITCH_MENU -> SPECIAL_MENU
                    EV_SCROLL_UP, EV_SCROLL_UP_SINGLE -> SPECIAL_SCROLLUP
                    EV_SCROLL_DOWN, EV_SCROLL_DOWN_SINGLE -> SPECIAL_SCROLLDOWN
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
}

/** Small arithmetic evaluator for Pojav dynamic positions: + - * / % ^, parentheses, dp(), px(), abs(), sqrt(). */
private class ExprEval(src: String, vars: Map<String, Float>, private val density: Float) {
    private val s: String
    private var pos = 0

    init {
        var text = src
        val re = Regex("\\$\\{(\\w+)}")
        text = re.replace(text) { m ->
            val v = vars[m.groupValues[1]] ?: throw IllegalArgumentException("unknown variable ${m.groupValues[1]}")
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
            val fn = s.substring(start, pos)
            expect('(')
            val arg = parseExpr()
            expect(')')
            return when (fn.lowercase()) {
                "dp" -> arg / density
                "px" -> arg * density
                "abs" -> Math.abs(arg)
                "sqrt" -> Math.sqrt(arg)
                else -> throw IllegalArgumentException("unknown function $fn")
            }
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

    private fun expect(ch: Char) {
        if (pos >= s.length || s[pos] != ch) throw IllegalArgumentException("expected '$ch'")
        pos++
    }
}
