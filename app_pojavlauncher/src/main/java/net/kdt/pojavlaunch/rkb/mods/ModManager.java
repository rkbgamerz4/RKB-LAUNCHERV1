package net.kdt.pojavlaunch.rkb.mods;

import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * RKB Launcher Mod Manager.
 *
 * - Enabled  → <gameDir>/mods/*.jar
 * - Disabled → <gameDir>/mods/disabled/*.jar
 * - Toggle   = move between the two folders (no delete)
 *
 * Order is always A→Z by displayName (OFF করলেও position একই থাকে).
 */
public final class ModManager {
    private static final String TAG = "RKB-ModManager";
    public static final String DISABLED_DIR_NAME = "disabled";

    private ModManager() {}

    public static File getModsDir(File gameDir) {
        return new File(gameDir, "mods");
    }

    public static File getDisabledModsDir(File gameDir) {
        return new File(getModsDir(gameDir), DISABLED_DIR_NAME);
    }

    public static List<ModInfo> listMods(File gameDir) {
        List<ModInfo> result = new ArrayList<>();
        if (gameDir == null) return result;

        File modsDir = getModsDir(gameDir);
        File disabledDir = getDisabledModsDir(gameDir);

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

        // Stable alphabetical — enabled/disabled does NOT change order
        Collections.sort(result, (a, b) ->
                a.displayName.toLowerCase(Locale.ROOT)
                        .compareTo(b.displayName.toLowerCase(Locale.ROOT)));
        return result;
    }

    private static boolean isModFile(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        return lower.endsWith(".jar") || lower.endsWith(".jar.disabled");
    }

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

    public static void prepareForLaunch(File gameDir) {
        Log.d(TAG, "prepareForLaunch: " + listMods(gameDir).size() + " mods scanned");
    }
}