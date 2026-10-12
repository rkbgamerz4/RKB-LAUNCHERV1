package net.kdt.pojavlaunch;

import static net.kdt.pojavlaunch.Architecture.archAsString;

import android.app.Activity;
import android.content.Context;
import com.kdt.mcgui.ProgressLayout;
import net.kdt.pojavlaunch.progresskeeper.DownloaderProgressWrapper;
import net.kdt.pojavlaunch.utils.DownloadUtils;
import java.io.File;
import java.io.FileInputStream;
import android.content.res.AssetManager;
import android.util.Log;

import net.kdt.pojavlaunch.multirt.MultiRTUtils;
import net.kdt.pojavlaunch.multirt.Runtime;
import net.kdt.pojavlaunch.utils.MathUtils;
import net.kdt.pojavlaunch.value.launcherprofiles.LauncherProfiles;
import net.kdt.pojavlaunch.value.launcherprofiles.MinecraftProfile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class NewJREUtil {
    private static boolean checkInternalRuntime(AssetManager assetManager, InternalRuntime internalRuntime) {
        String launcher_runtime_version;
        String installed_runtime_version = MultiRTUtils.readInternalRuntimeVersion(internalRuntime.name);
        try {
            launcher_runtime_version = Tools.read(assetManager.open(internalRuntime.path+"/version"));
        }catch (IOException exc) {
            //we don't have a runtime included!
            //if we have one installed -> return true -> proceed (no updates but the current one should be functional)
            //if we don't -> return false -> Cannot find compatible Java runtime
            return installed_runtime_version != null;
        }
        // this implicitly checks for null, so it will unpack the runtime even if we don't have one installed
        if(!launcher_runtime_version.equals(installed_runtime_version))
            return unpackInternalRuntime(assetManager, internalRuntime, launcher_runtime_version);
        else return true;
    }

    private static boolean unpackInternalRuntime(AssetManager assetManager, InternalRuntime internalRuntime, String version) {
        try {
            MultiRTUtils.installRuntimeNamedBinpack(
                    assetManager.open(internalRuntime.path+"/universal.tar.xz"),
                    assetManager.open(internalRuntime.path+"/bin-" + archAsString(Tools.DEVICE_ARCHITECTURE) + ".tar.xz"),
                    internalRuntime.name, version);
            MultiRTUtils.postPrepare(internalRuntime.name);
            return true;
        }catch (IOException e) {
            Log.e("NewJREAuto", "Internal JRE unpack failed", e);
            return false;
        }
    }

    private static InternalRuntime getInternalRuntime(Runtime runtime) {
        for(InternalRuntime internalRuntime : InternalRuntime.values()) {
            if(internalRuntime.name.equals(runtime.name)) return internalRuntime;
        }
        return null;
    }

    private static MathUtils.RankedValue<Runtime> getNearestInstalledRuntime(int targetVersion) {
        List<Runtime> runtimes = MultiRTUtils.getRuntimes();
        return MathUtils.findNearestPositive(targetVersion, runtimes, (runtime)->runtime.javaVersion);
    }

    private static MathUtils.RankedValue<InternalRuntime> getNearestInternalRuntime(int targetVersion) {
        List<InternalRuntime> runtimeList = Arrays.asList(InternalRuntime.values());
        return MathUtils.findNearestPositive(targetVersion, runtimeList, (runtime)->runtime.majorVersion);
    }


    /** @return true if everything is good, false otherwise.  */
    public static boolean installNewJreIfNeeded(Activity activity, JMinecraftVersionList.Version versionInfo) {
        //Now we have the reliable information to check if our runtime settings are good enough
        if (versionInfo.javaVersion == null || versionInfo.javaVersion.component.equalsIgnoreCase("jre-legacy"))
            return true;

        int gameRequiredVersion = versionInfo.javaVersion.majorVersion;

        LauncherProfiles.load();
        AssetManager assetManager = activity.getAssets();
        MinecraftProfile minecraftProfile = LauncherProfiles.getCurrentProfile();
        String profileRuntime = Tools.getSelectedRuntime(minecraftProfile);
        Runtime runtime = MultiRTUtils.read(profileRuntime);
        // Partly trust the user with his own selection, if the game can even try to run in this case
        if (runtime.javaVersion >= gameRequiredVersion) {
            // Check whether the selection is an internal runtime
            InternalRuntime internalRuntime = getInternalRuntime(runtime);
            // If it is, check if updates are available from the APK file
            if(internalRuntime != null) {
                // Not calling showRuntimeFail on failure here because we did, technically, find the compatible runtime
                return checkInternalRuntime(assetManager, internalRuntime);
            }
            return true;
        }

        // If the runtime version selected by the user is not appropriate for this version (which means the game won't run at all)
        // automatically pick from either an already installed runtime, or a runtime packed with the launcher
        MathUtils.RankedValue<?> nearestInstalledRuntime = getNearestInstalledRuntime(gameRequiredVersion);
        MathUtils.RankedValue<?> nearestInternalRuntime = getNearestInternalRuntime(gameRequiredVersion);

        MathUtils.RankedValue<?> selectedRankedRuntime = MathUtils.objectMin(
                nearestInternalRuntime, nearestInstalledRuntime, (value)->value.rank
        );

        // No possible selections: before giving up, try to download a suitable runtime
        // (same source and install path as Settings > Java > Java Runtimes).
        if(selectedRankedRuntime == null) {
            if(!tryDownloadRuntime(activity, gameRequiredVersion)) {
                showRuntimeFail(activity, versionInfo);
                return false;
            }
            selectedRankedRuntime = getNearestInstalledRuntime(gameRequiredVersion);
            if(selectedRankedRuntime == null) {
                showRuntimeFail(activity, versionInfo);
                return false;
            }
        }

        Object selected = selectedRankedRuntime.value;
        String appropriateRuntime;
        InternalRuntime internalRuntime;

        // Perform checks on the picked runtime
        if(selected instanceof Runtime) {
            // If it's an already installed runtime, save its name and check if
            // it's actually an internal one (just in case)
            Runtime selectedRuntime = (Runtime) selected;
            appropriateRuntime = selectedRuntime.name;
            internalRuntime = getInternalRuntime(selectedRuntime);
        } else if (selected instanceof InternalRuntime) {
            // If it's an internal runtime, set it's name as the appropriate one.
            internalRuntime = (InternalRuntime) selected;
            appropriateRuntime = internalRuntime.name;
        } else {
            throw new RuntimeException("Unexpected type of selected: "+selected.getClass().getName());
        }

        // If it turns out the selected runtime is actually an internal one, attempt automatic installation or update
        if(internalRuntime != null && !checkInternalRuntime(assetManager, internalRuntime)) {
            // The APK does not contain this runtime (build without the bundled JREs). Download an
            // equivalent one instead of failing with a misleading "failed to unpack" error.
            if(!tryDownloadRuntime(activity, internalRuntime.majorVersion)) {
                showRuntimeFail(activity, versionInfo);
                return false;
            }
            MathUtils.RankedValue<Runtime> downloaded = getNearestInstalledRuntime(gameRequiredVersion);
            if(downloaded == null) {
                showRuntimeFail(activity, versionInfo);
                return false;
            }
            appropriateRuntime = downloaded.value.name;
        }

        minecraftProfile.javaDir = Tools.LAUNCHERPROFILES_RTPREFIX + appropriateRuntime;
        LauncherProfiles.write();
        return true;
    }

    // ------------------------------------------------------------------------------------------
    // Runtime download (public OpenJDK Android builds, installed through the normal MultiRT store)
    // ------------------------------------------------------------------------------------------

    /**
     * Where the runtime archives come from: the public AngelAuraMC OpenJDK-for-Android build releases
     * (the same archives the Amethyst/CS launchers use). Layout: download_jre&lt;N&gt;/jre&lt;N&gt;-android-&lt;arch&gt;.tar.xz
     * The files are standard OpenJDK builds (GPLv2 + Classpath Exception), unmodified, installed as-is.
     */
    public static String getJreSource(int javaVersion, String arch) {
        return String.format("https://github.com/AngelAuraMC/angelauramc-openjdk-build/releases/download/download_jre%1$s/jre%1$s-android-%2$s.tar.xz",
                javaVersion, arch);
    }

    public static boolean isJavaVersionAvailableForDownload(int version) {
        for (ExternalRuntime r : ExternalRuntime.values()) if (r.majorVersion == version) return true;
        return false;
    }

    /** Smallest downloadable major version that is &gt;= the required one, or -1. */
    public static int pickDownloadableVersion(int required) {
        int best = -1;
        for (ExternalRuntime r : ExternalRuntime.values()) {
            if (r.majorVersion >= required && (best == -1 || r.majorVersion < best)) best = r.majorVersion;
        }
        return best;
    }

    private static final Object sRuntimeDownloadLock = new Object();

    /**
     * Download + install a runtime for the required Java version.
     * Blocking: call from a background thread. Never reports success unless the runtime is installed.
     * @return true if a suitable runtime is installed afterwards
     * @throws RuntimeException with the real reason (offline, HTTP error, unsupported arch, extraction error)
     */
    public static boolean tryDownloadRuntime(Context context, int requiredVersion) {
        int version = pickDownloadableVersion(requiredVersion);
        if (version == -1) {
            Log.w("NewJREAuto", "No downloadable runtime for Java " + requiredVersion);
            return false;
        }
        synchronized (sRuntimeDownloadLock) { // one runtime download at a time
            String existing = MultiRTUtils.getExactJreName(version);
            if (existing != null) return true; // someone installed it while we waited
            File archive = downloadRuntimeArchive(context, version, ProgressLayout.UNPACK_RUNTIME);
            installRuntimeArchive(archive, version);
            return true;
        }
    }

    public static File downloadRuntimeArchive(Context context, int javaVersion, String progressRecord) {
        if (!Tools.isOnline(context)) {
            throw new RuntimeException(context.getString(R.string.multirt_no_internet));
        }
        if (!isJavaVersionAvailableForDownload(javaVersion)) {
            throw new RuntimeException("Java " + javaVersion + " is not available for download");
        }
        int archId = Tools.DEVICE_ARCHITECTURE;
        if (archId == Architecture.ARCH_X86 && javaVersion >= 21) {
            throw new RuntimeException("Java " + javaVersion + " is not available for 32-bit x86 devices");
        }
        String arch = archAsString(archId);
        String url = getJreSource(javaVersion, arch);
        File output = new File(Tools.DIR_CACHE, "jre" + javaVersion + "-android-" + arch + ".tar.xz");
        DownloaderProgressWrapper monitor = new DownloaderProgressWrapper(R.string.newdl_downloading_jre_runtime, progressRecord);
        monitor.extraString = Integer.toString(javaVersion);
        try {
            DownloadUtils.downloadFileMonitored(url, output, null, monitor);
            return output;
        } catch (IOException e) {
            output.delete();
            throw new RuntimeException("Failed to download Java " + javaVersion + " for " + arch + " from " + url
                    + ": " + e.getMessage(), e);
        } finally {
            ProgressLayout.clearProgress(progressRecord);
        }
    }

    public static void installRuntimeArchive(File archive, int javaVersion) {
        String name = "External-" + javaVersion;
        try (FileInputStream in = new FileInputStream(archive)) {
            MultiRTUtils.installRuntimeNamed(Tools.NATIVE_LIB_DIR, in, name);
            MultiRTUtils.postPrepare(name);
        } catch (IOException e) {
            // do not leave a half-extracted runtime behind
            try { MultiRTUtils.removeRuntimeNamed(name); } catch (IOException ignored) { }
            throw new RuntimeException("Failed to install Java " + javaVersion + ": " + e.getMessage(), e);
        } finally {
            archive.delete();
            ProgressLayout.clearProgress(ProgressLayout.UNPACK_RUNTIME);
        }
    }

    /** Runtimes that can be downloaded from the settings dialog or automatically when a game needs one. */
    public enum ExternalRuntime {
        JRE_8(8), JRE_17(17), JRE_21(21), JRE_25(25);
        public final int majorVersion;
        public final String name;
        /** UI-only flag: a download for this entry is in progress. */
        public volatile boolean isDownloading = false;
        ExternalRuntime(int majorVersion) {
            this.majorVersion = majorVersion;
            this.name = "External-" + majorVersion;
        }
        public String getDownloadLink() {
            return getJreSource(majorVersion, archAsString(Tools.DEVICE_ARCHITECTURE));
        }
        public boolean isSupportedOnThisDevice() {
            return !(Tools.DEVICE_ARCHITECTURE == Architecture.ARCH_X86 && majorVersion >= 21);
        }
    }

    private static void showRuntimeFail(Activity activity, JMinecraftVersionList.Version verInfo) {
        Tools.dialogOnUiThread(activity, activity.getString(R.string.global_error),
                activity.getString(R.string.multirt_nocompatiblert, verInfo.javaVersion.majorVersion));
    }

    private enum InternalRuntime {
        JRE_17(17, "Internal-17", "components/jre-new"),
        JRE_21(21, "Internal-21", "components/jre-21");
        public final int majorVersion;
        public final String name;
        public final String path;
        InternalRuntime(int majorVersion, String name, String path) {
            this.majorVersion = majorVersion;
            this.name = name;
            this.path = path;
        }
    }

}