package net.kdt.pojavlaunch.rkb.mods;

/**
 * One mod entry for RKB Mod Manager.
 */
public class ModInfo {
    public String fileName;
    public String absolutePath;
    public String displayName;
    public boolean enabled;

    public ModInfo(String fileName, String absolutePath, boolean enabled) {
        this.fileName = fileName;
        this.absolutePath = absolutePath;
        this.enabled = enabled;
        this.displayName = stripExtension(fileName);
    }

    private static String stripExtension(String name) {
        if (name == null) return "";
        String n = name;
        if (n.toLowerCase().endsWith(".jar.disabled")) {
            n = n.substring(0, n.length() - ".jar.disabled".length());
        } else if (n.toLowerCase().endsWith(".disabled")) {
            n = n.substring(0, n.length() - ".disabled".length());
        } else if (n.toLowerCase().endsWith(".jar")) {
            n = n.substring(0, n.length() - ".jar".length());
        }
        return n;
    }
}