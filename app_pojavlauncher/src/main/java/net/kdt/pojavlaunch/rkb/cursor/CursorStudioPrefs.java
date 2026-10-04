package net.kdt.pojavlaunch.rkb.cursor;

import android.content.SharedPreferences;

import net.kdt.pojavlaunch.prefs.LauncherPreferences;

/**
 * RKB Cursor Studio preferences.
 * Style / color / size / opacity for the virtual mouse cursor preview & scale.
 * Size is applied on top of LauncherPreferences.PREF_MOUSESCALE when loading.
 */
public final class CursorStudioPrefs {
    public static final String PREF_FILE_HINT = "pojav_settings"; // uses DEFAULT_PREF

    public static final String KEY_STYLE = "rkb_cursor_style";       // classic | pulse | gamepad | custom
    public static final String KEY_COLOR = "rkb_cursor_color";       // ARGB int as hex string e.g. #00B4FF
    public static final String KEY_SIZE = "rkb_cursor_size";         // 50..150 percent
    public static final String KEY_OPACITY = "rkb_cursor_opacity";   // 20..100 percent

    public static final String STYLE_CLASSIC = "classic";
    public static final String STYLE_PULSE = "pulse";
    public static final String STYLE_GAMEPAD = "gamepad";
    public static final String STYLE_CUSTOM = "custom";

    public static final String DEFAULT_COLOR = "#00B4FF";
    public static final int DEFAULT_SIZE = 100;
    public static final int DEFAULT_OPACITY = 100;

    private CursorStudioPrefs() {}

    private static SharedPreferences prefs() {
        return LauncherPreferences.DEFAULT_PREF;
    }

    public static String getStyle() {
        return prefs().getString(KEY_STYLE, STYLE_PULSE);
    }

    public static void setStyle(String style) {
        prefs().edit().putString(KEY_STYLE, style).apply();
    }

    public static String getColorHex() {
        return prefs().getString(KEY_COLOR, DEFAULT_COLOR);
    }

    public static void setColorHex(String hex) {
        prefs().edit().putString(KEY_COLOR, hex).apply();
    }

    public static int getSizePercent() {
        return prefs().getInt(KEY_SIZE, DEFAULT_SIZE);
    }

    public static void setSizePercent(int percent) {
        int p = Math.max(50, Math.min(150, percent));
        prefs().edit().putInt(KEY_SIZE, p).apply();
    }

    public static int getOpacityPercent() {
        return prefs().getInt(KEY_OPACITY, DEFAULT_OPACITY);
    }

    public static void setOpacityPercent(int percent) {
        int p = Math.max(20, Math.min(100, percent));
        prefs().edit().putInt(KEY_OPACITY, p).apply();
    }

    /**
     * Multiplier for mouse pointer drawable scale (0.5 .. 1.5).
     * Combine with PREF_MOUSESCALE in UI code when drawing the cursor.
     */
    public static float getSizeMultiplier() {
        return getSizePercent() / 100f;
    }

    public static float getOpacity() {
        return getOpacityPercent() / 100f;
    }

    /** Parse stored color; falls back to neon blue. */
    public static int getColorArgb() {
        try {
            String hex = getColorHex();
            if (hex.startsWith("#")) hex = hex.substring(1);
            if (hex.length() == 6) {
                return 0xFF000000 | Integer.parseInt(hex, 16);
            }
            if (hex.length() == 8) {
                return (int) Long.parseLong(hex, 16);
            }
        } catch (Exception ignored) {}
        return 0xFF00B4FF;
    }
}
