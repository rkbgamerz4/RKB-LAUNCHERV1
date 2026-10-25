package net.kdt.pojavlaunch.rkb.mods;

import android.util.Log;

import java.io.File;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONObject;
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

    // ---------------------------------------------------------------- metadata (read-only)

    /**
     * Fills name/version/description from the jar itself. Blocking disk work: call off the main thread.
     * Only values actually present in the jar are set; nothing is guessed.
     */
    public static void readMeta(ModInfo mod) {
        if (mod == null) return;
        File f = new File(mod.absolutePath);
        mod.sizeBytes = f.length();
        try (ZipFile zip = new ZipFile(f)) {
            String json = readEntry(zip, "fabric.mod.json");
            if (json == null) json = readEntry(zip, "quilt.mod.json");
            if (json != null) {
                JSONObject o = new JSONObject(json);
                if (o.has("quilt_loader")) {
                    JSONObject q = o.getJSONObject("quilt_loader");
                    mod.version = q.optString("version", null);
                    JSONObject md = q.optJSONObject("metadata");
                    if (md != null) {
                        mod.metaName = md.optString("name", null);
                        mod.description = md.optString("description", null);
                    }
                } else {
                    mod.metaName = o.optString("name", null);
                    mod.version = o.optString("version", null);
                    mod.description = o.optString("description", null);
                }
            } else {
                String toml = readEntry(zip, "META-INF/mods.toml");
                if (toml == null) toml = readEntry(zip, "META-INF/neoforge.mods.toml");
                if (toml != null) {
                    mod.metaName = tomlValue(toml, "displayName");
                    mod.version = tomlValue(toml, "version");
                    mod.description = tomlValue(toml, "description");
                } else {
                    String legacy = readEntry(zip, "mcmod.info");
                    if (legacy != null) {
                        JSONObject o = legacy.trim().startsWith("[")
                                ? new JSONArray(legacy).getJSONObject(0)
                                : new JSONObject(legacy).getJSONArray("modList").getJSONObject(0);
                        mod.metaName = o.optString("name", null);
                        mod.version = o.optString("version", null);
                        mod.description = o.optString("description", null);
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "readMeta failed for " + mod.fileName + ": " + e);
        }
        if (mod.version != null && (mod.version.contains("${") || mod.version.trim().isEmpty())) mod.version = null;
        if (mod.metaName != null && mod.metaName.trim().isEmpty()) mod.metaName = null;
        if (mod.description != null) {
            mod.description = mod.description.trim().replaceAll("\\s+", " ");
            if (mod.description.isEmpty()) mod.description = null;
        }
    }

    private static String readEntry(ZipFile zip, String name) throws Exception {
        ZipEntry e = zip.getEntry(name);
        if (e == null || e.getSize() > 512 * 1024) return null;
        try (InputStream in = zip.getInputStream(e)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            return out.toString("UTF-8");
        }
    }

    private static String tomlValue(String toml, String key) {
        Matcher m = Pattern.compile("(?m)^\\s*" + key + "\\s*=\\s*(?:\\'\\'\\'([\\s\\S]*?)\\'\\'\\'|\"\"\"([\\s\\S]*?)\"\"\"|\"([^\"]*)\")").matcher(toml);
        if (!m.find()) return null;
        for (int i = 1; i <= 3; i++) if (m.group(i) != null) return m.group(i);
        return null;
    }
}
