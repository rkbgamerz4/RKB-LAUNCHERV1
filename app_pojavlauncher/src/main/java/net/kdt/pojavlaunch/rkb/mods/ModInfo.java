package net.kdt.pojavlaunch.rkb.mods;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * RKB Launcher Mod Manager — description of a single installed mod.
 * DISABLE does not delete the file; ENABLE does not re-download.
 */
@Keep
public class ModInfo {
    public String fileName;          // e.g. sodium.jar
    public String displayName;       // human readable
    public String version;           // if detectable
    public String loader;            // fabric / forge / quilt / unknown
    public String mcVersionRange;    // if detectable
    public boolean enabled;          // true = active for launch
    public String absolutePath;      // current path on disk

    public ModInfo() {}

    public ModInfo(String fileName, String absolutePath, boolean enabled) {
        this.fileName = fileName;
        this.absolutePath = absolutePath;
        this.enabled = enabled;
        this.displayName = fileName.replace(".jar", "").replace(".disabled", "");
        this.loader = "unknown";
    }

    @NonNull
    @Override
    public String toString() {
        return (enabled ? "[ON] " : "[OFF] ") + displayName +
                (version != null ? " (" + version + ")" : "");
    }
}
