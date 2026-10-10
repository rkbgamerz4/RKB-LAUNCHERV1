package net.kdt.pojavlaunch.rkb.cursor;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;

/**
 * RKB Cursor Studio preferences + drawable factory.
 *
 * The SAME factory is used by the Cursor Studio preview and by the in-game virtual mouse
 * ({@code Touchpad}), so what the preview shows is what the touchpad cursor draws.
 * Defaults (classic / 100% / 100%) reproduce the original PojavLauncher pointer exactly.
 */
public final class CursorStudioPrefs {
    public static final String KEY_STYLE = "rkb_cursor_style";
    public static final String KEY_COLOR = "rkb_cursor_color";       // #RRGGBB
    public static final String KEY_SIZE = "rkb_cursor_size";         // 50..150 percent
    public static final String KEY_OPACITY = "rkb_cursor_opacity";   // 20..100 percent

    public static final String STYLE_CLASSIC = "classic";     // original launcher pointer, never tinted
    public static final String STYLE_NEON = "neon";           // arrow tinted with the chosen color
    public static final String STYLE_CROSSHAIR = "crosshair";
    public static final String STYLE_DOT = "dot";
    public static final String STYLE_RING = "ring";

    public static final String DEFAULT_COLOR = "#00A8FF";
    public static final int DEFAULT_SIZE = 100;
    public static final int DEFAULT_OPACITY = 100;

    private CursorStudioPrefs() {}

    private static SharedPreferences prefs() {
        return LauncherPreferences.DEFAULT_PREF; // may be null very early; callers guard via the getters
    }

    public static String normalizeStyle(String s) {
        if (STYLE_NEON.equals(s) || STYLE_CROSSHAIR.equals(s) || STYLE_DOT.equals(s) || STYLE_RING.equals(s)) return s;
        return STYLE_CLASSIC; // also maps the legacy values (pulse/gamepad/custom) that were never rendered
    }

    public static String getStyle() {
        SharedPreferences p = prefs();
        return p == null ? STYLE_CLASSIC : normalizeStyle(p.getString(KEY_STYLE, STYLE_CLASSIC));
    }

    public static String getColorHex() {
        SharedPreferences p = prefs();
        return p == null ? DEFAULT_COLOR : p.getString(KEY_COLOR, DEFAULT_COLOR);
    }

    public static int getSizePercent() {
        SharedPreferences p = prefs();
        return p == null ? DEFAULT_SIZE : clamp(p.getInt(KEY_SIZE, DEFAULT_SIZE), 50, 150);
    }

    public static int getOpacityPercent() {
        SharedPreferences p = prefs();
        return p == null ? DEFAULT_OPACITY : clamp(p.getInt(KEY_OPACITY, DEFAULT_OPACITY), 20, 100);
    }

    public static float getSizeMultiplier() { return getSizePercent() / 100f; }
    public static float getOpacity() { return getOpacityPercent() / 100f; }

    public static int getColorArgb() { return parseColor(getColorHex()); }

    public static void save(String style, String colorHex, int sizePercent, int opacityPercent) {
        SharedPreferences p = prefs();
        if (p == null) return;
        p.edit()
                .putString(KEY_STYLE, normalizeStyle(style))
                .putString(KEY_COLOR, colorHex)
                .putInt(KEY_SIZE, clamp(sizePercent, 50, 150))
                .putInt(KEY_OPACITY, clamp(opacityPercent, 20, 100))
                .apply();
    }

    public static void resetToDefaults() {
        save(STYLE_CLASSIC, DEFAULT_COLOR, DEFAULT_SIZE, DEFAULT_OPACITY);
    }

    public static int parseColor(String hex) {
        try {
            String h = hex.startsWith("#") ? hex.substring(1) : hex;
            if (h.length() == 6) return 0xFF000000 | Integer.parseInt(h, 16);
            if (h.length() == 8) return (int) Long.parseLong(h, 16);
        } catch (Exception ignored) { }
        return 0xFF00A8FF;
    }

    private static int clamp(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }

    // ---------------------------------------------------------------- drawables

    /** Symmetric styles draw centred on the pointer position; arrows draw from their tip. */
    public static boolean isCentered(String style) {
        return STYLE_CROSSHAIR.equals(style) || STYLE_DOT.equals(style) || STYLE_RING.equals(style);
    }

    /** Builds a fresh (mutable, un-shared) drawable. Never returns null. */
    @NonNull
    public static Drawable createDrawable(@NonNull Context c, String style, int argb, int opacityPercent) {
        style = normalizeStyle(style);
        Drawable d;
        switch (style) {
            case STYLE_NEON: {
                Drawable outline = ResourcesCompat.getDrawable(c.getResources(), R.drawable.ic_rkb_cursor_arrow_outline, c.getTheme());
                Drawable fill = ResourcesCompat.getDrawable(c.getResources(), R.drawable.ic_rkb_cursor_arrow_fill, c.getTheme());
                Drawable fillWrapped = DrawableCompat.wrap(fill.mutate());
                DrawableCompat.setTint(fillWrapped, argb);
                d = new LayerDrawable(new Drawable[]{outline.mutate(), fillWrapped});
                break;
            }
            case STYLE_CROSSHAIR:
                d = tinted(c, R.drawable.ic_rkb_cursor_crosshair, argb);
                break;
            case STYLE_DOT:
                d = tinted(c, R.drawable.ic_rkb_cursor_dot, argb);
                break;
            case STYLE_RING:
                d = tinted(c, R.drawable.ic_rkb_cursor_ring, argb);
                break;
            case STYLE_CLASSIC:
            default:
                d = ResourcesCompat.getDrawable(c.getResources(), R.drawable.ic_mouse_pointer, c.getTheme());
                break;
        }
        d.mutate().setAlpha(Math.round(255 * clamp(opacityPercent, 20, 100) / 100f));
        return d;
    }

    private static Drawable tinted(Context c, int res, int argb) {
        Drawable base = ResourcesCompat.getDrawable(c.getResources(), res, c.getTheme());
        Drawable w = DrawableCompat.wrap(base.mutate());
        DrawableCompat.setTint(w, argb);
        return w;
    }

    /**
     * @param scale final multiplier (PREF_MOUSESCALE * size%). Arrow tip stays at (0,0); symmetric
     *              styles are centred on (0,0) so the click point is their middle.
     */
    public static void applyBounds(@NonNull Drawable d, String style, float scale) {
        if (isCentered(normalizeStyle(style))) {
            int half = Math.round(20 * scale);
            d.setBounds(-half, -half, half, half);
        } else {
            d.setBounds(0, 0, Math.round(36 * scale), Math.round(54 * scale));
        }
    }

    // ---------------------------------------------------------------- export / import (JSON)

    public static final String PRESET_FORMAT = "rkb-cursor-preset";

    /** Parsed + validated preset (values already clamped). */
    public static final class Preset {
        public final String style, color;
        public final int size, opacity;
        Preset(String style, String color, int size, int opacity) {
            this.style = style; this.color = color; this.size = size; this.opacity = opacity;
        }
    }

    public static String toJson(String style, String colorHex, int size, int opacity) throws org.json.JSONException {
        return new org.json.JSONObject()
                .put("format", PRESET_FORMAT)
                .put("version", 1)
                .put("style", normalizeStyle(style))
                .put("color", colorHex)
                .put("size", clamp(size, 50, 150))
                .put("opacity", clamp(opacity, 20, 100))
                .toString(2);
    }

    /** @throws IllegalArgumentException with a user-facing message when the file is not a valid preset. */
    public static Preset fromJson(String json) {
        try {
            org.json.JSONObject o = new org.json.JSONObject(json);
            if (!PRESET_FORMAT.equals(o.optString("format"))) {
                throw new IllegalArgumentException("This file is not an RKB cursor preset");
            }
            String style = o.getString("style");
            if (!normalizeStyle(style).equals(style)) throw new IllegalArgumentException("Unknown cursor style: " + style);
            String color = o.getString("color");
            if (!color.matches("#[0-9a-fA-F]{6}")) throw new IllegalArgumentException("Invalid color value");
            return new Preset(style, color.toUpperCase(java.util.Locale.ROOT),
                    clamp(o.getInt("size"), 50, 150), clamp(o.getInt("opacity"), 20, 100));
        } catch (org.json.JSONException e) {
            throw new IllegalArgumentException("The file is not valid preset JSON");
        }
    }
}
