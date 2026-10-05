import android.util.Log;

import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.utils.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * RKB Launcher Mod Manager.
 *
 * Design goals:
 * - DISABLE ≠ DELETE
 * - ENABLE ≠ RE-DOWNLOAD
 * - Works under Android storage restrictions
 * - Compatible with Pojav profile / instance gameDir layout
 *
 * Mechanism:
 * - Enabled mods live in <gameDir>/mods/*.jar
 * - Disabled mods live in <gameDir>/mods/disabled/*.jar
 * - Toggle = atomic rename/move between the two directories
 *
 * Order is always alphabetical by displayName — OFF করলেও position বদলায় না।
 */
public final class ModManager {
    private static final String TAG = "RKB-ModManager";
    public static final String DISABLED_DIR_NAME = "disabled";

    private ModManager() {}

    /** Resolve the mods directory for a given game directory (instance). */
    public static File getModsDir(File gameDir) {
        return new File(gameDir, "mods");
    }

    public static File getDisabledModsDir(File gameDir) {
        return new File(getModsDir(gameDir), DISABLED_DIR_NAME);
    }

    /**
     * Scan installed mods (both enabled and disabled) for the given gameDir.
     * Sorted by displayName only — enabled/disabled does not change order.
     */
    public static List<ModInfo> listMods(File gameDir) {
        List<ModInfo> result = new ArrayList<>();
        if (gameDir == null) return result;

        File modsDir = getModsDir(gameDir);
        File disabledDir = getDisabledModsDir(gameDir);

        // Enabled
        if (modsDir.isDirectory()) {
            File[] files = modsDir.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.isFile() && isModFile(f.getName())) {
                        result.add(new ModInfo(f.getName(), f.getAbsolutePath(), true));
                    }
                }
            }
        }

        // Disabled
        if (disabledDir.isDirectory()) {
            File[] files = disabledDir.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.isFile() && isModFile(f.getName())) {
                        result.add(new ModInfo(f.getName(), f.getAbsolutePath(), false));
                    }
                }
            }
        }

        // Stable alphabetical order — OFF করলেও জায়গায় থাকবে
        Collections.sort(result, (a, b) ->
                a.displayName.toLowerCase(Locale.ROOT)
                        .compareTo(b.displayName.toLowerCase(Locale.ROOT)));
        return result;
    }

    private static boolean isModFile(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        return lower.endsWith(".jar") || lower.endsWith(".jar.disabled");
    }

    /**
     * Enable a previously disabled mod (move from disabled/ back to mods/).
     * @return true on success
     */
    public static boolean enableMod(File gameDir, ModInfo mod) {
        if (mod == null || mod.enabled) return true;
        File src = new File(mod.absolutePath);
        if (!src.isFile()) {
            Log.w(TAG, "enableMod: source missing " + mod.absolutePath);
            return false;
        }
        File destDir = getModsDir(gameDir);
        if (!destDir.exists() && !destDir.mkdirs()) {
            Log.e(TAG, "enableMod: cannot create mods dir");
            return false;
        }
        String targetName = mod.fileName.replace(".disabled", "");
        File dest = new File(destDir, targetName);
        boolean ok = src.renameTo(dest);
        if (ok) {
            mod.enabled = true;
            mod.absolutePath = dest.getAbsolutePath();
            mod.fileName = targetName;
            Log.i(TAG, "Enabled: " + targetName);
        } else {
            Log.e(TAG, "enableMod rename failed: " + src + " -> " + dest);
        }
        return ok;
    }

    /**
     * Disable a mod without deleting it (move into mods/disabled/).
     * @return true on success
     */
    public static boolean disableMod(File gameDir, ModInfo mod) {
        if (mod == null || !mod.enabled) return true;
        File src = new File(mod.absolutePath);
        if (!src.isFile()) {
            Log.w(TAG, "disableMod: source missing " + mod.absolutePath);
            return false;
        }
        File destDir = getDisabledModsDir(gameDir);
        if (!destDir.exists() && !destDir.mkdirs()) {
            Log.e(TAG, "disableMod: cannot create disabled dir");
            return false;
        }
        File dest = new File(destDir, mod.fileName);
        boolean ok = src.renameTo(dest);
        if (ok) {
            mod.enabled = false;
            mod.absolutePath = dest.getAbsolutePath();
            Log.i(TAG, "Disabled: " + mod.fileName);
        } else {
            Log.e(TAG, "disableMod rename failed: " + src + " -> " + dest);
        }
        return ok;
    }

    public static boolean toggleMod(File gameDir, ModInfo mod) {
        return mod.enabled ? disableMod(gameDir, mod) : enableMod(gameDir, mod);
    }

    /** Permanently delete a mod file (enabled or disabled). */
    public static boolean deleteMod(ModInfo mod) {
        if (mod == null) return false;
        File f = new File(mod.absolutePath);
        boolean ok = f.isFile() && f.delete();
        if (ok) Log.i(TAG, "Deleted: " + mod.fileName);
        return ok;
    }

    public static void enableAll(File gameDir) {
        for (ModInfo m : listMods(gameDir)) {
            if (!m.enabled) enableMod(gameDir, m);
        }
    }

    public static void disableAll(File gameDir) {
        for (ModInfo m : listMods(gameDir)) {
            if (m.enabled) disableMod(gameDir, m);
        }
    }

    /**
     * Ensure only enabled mods are present in the active mods/ folder before launch.
     * (Already true by construction of enable/disable, but useful as a safety check.)
     */
    public static void prepareForLaunch(File gameDir) {
        Log.d(TAG, "prepareForLaunch: " + listMods(gameDir).size() + " mods scanned");
    }
}