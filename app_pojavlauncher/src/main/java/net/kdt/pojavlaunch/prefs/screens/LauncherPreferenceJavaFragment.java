package net.kdt.pojavlaunch.prefs.screens;

import static net.kdt.pojavlaunch.Architecture.is32BitsDevice;
import static net.kdt.pojavlaunch.Tools.getTotalDeviceMemory;

import android.content.res.AssetManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.preference.Preference;
import net.kdt.pojavlaunch.multirt.MultiRTUtils;
import net.kdt.pojavlaunch.multirt.Runtime;
import java.util.List;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.preference.EditTextPreference;

import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.contracts.OpenDocumentWithExtension;
import net.kdt.pojavlaunch.multirt.MultiRTConfigDialog;
import net.kdt.pojavlaunch.prefs.CustomSeekBarPreference;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;

public class LauncherPreferenceJavaFragment extends LauncherPreferenceFragment {
    private MultiRTConfigDialog mDialogScreen;
    private final ActivityResultLauncher<Object> mVmInstallLauncher =
            registerForActivityResult(new OpenDocumentWithExtension("xz"), (data)->{
                if(data != null) Tools.installRuntimeFromUri(getContext(), data);
            });

    @Override
    public void onCreatePreferences(Bundle b, String str) {
        int ramAllocation = LauncherPreferences.PREF_RAM_ALLOCATION;
        // Triggers a write for some reason
        addPreferencesFromResource(R.xml.pref_java);

        CustomSeekBarPreference memorySeekbar = requirePreference("allocation",
                CustomSeekBarPreference.class);

        int maxRAM;
        int deviceRam = getTotalDeviceMemory(memorySeekbar.getContext());

        if(is32BitsDevice() || deviceRam < 2048) maxRAM = Math.min(1024, deviceRam);
        else maxRAM = deviceRam - (deviceRam < 3064 ? 800 : 1024); //To have a minimum for the device to breathe

        memorySeekbar.setMaxKeepIncrement(maxRAM);
        memorySeekbar.setValue(ramAllocation);
        memorySeekbar.setSuffix(" MB");

        EditTextPreference editJVMArgs = findPreference("javaArgs");
        if (editJVMArgs != null) {
            editJVMArgs.setOnBindEditTextListener(TextView::setSingleLine);
        }

        requirePreference("install_jre").setOnPreferenceClickListener(preference->{
            openMultiRTDialog();
            return true;
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshRuntimeStatus();
    }

    /**
     * Shows the REAL state: runtimes installed on the device and which Java versions this APK bundles.
     * The bundled runtimes (assets/components/jre, jre-new, jre-21) are build-time assets fetched by CI;
     * if they are missing the APK was built without them.
     */
    private void refreshRuntimeStatus() {
        final Preference pref = findPreference("install_jre");
        final android.content.Context ctx = getContext();
        if (pref == null || ctx == null) return;
        final AssetManager am = ctx.getAssets();
        final Handler main = new Handler(Looper.getMainLooper());
        new Thread(() -> {
            StringBuilder sb = new StringBuilder();
            try {
                List<Runtime> installed = MultiRTUtils.getRuntimes();
                if (installed.isEmpty()) sb.append("No Java runtime is installed.");
                else {
                    sb.append("Installed: ");
                    for (int i = 0; i < installed.size(); i++) {
                        Runtime r = installed.get(i);
                        if (i > 0) sb.append(", ");
                        sb.append(r.name).append(r.javaVersion > 0 ? " (Java " + r.javaVersion + ")" : "");
                    }
                }
            } catch (Exception e) {
                sb.append("Could not read installed runtimes.");
            }
            String[] dirs = {"jre", "jre-new", "jre-21"};
            String[] labels = {"Java 8", "Java 17", "Java 21"};
            StringBuilder missing = new StringBuilder();
            for (int i = 0; i < dirs.length; i++) {
                boolean present;
                try { present = am.list("components/" + dirs[i]).length > 0; } catch (Exception e) { present = false; }
                if (!present) missing.append(missing.length() > 0 ? ", " : "").append(labels[i]);
            }
            if (missing.length() > 0) {
                sb.append("\nNot bundled in this APK: ").append(missing)
                        .append(". Install a .tar.xz runtime here or rebuild with the runtimes.");
            }
            final String text = sb.toString();
            main.post(() -> { if (isAdded()) pref.setSummary(text); });
        }, "RKB-RuntimeStatus").start();
    }

    private void openMultiRTDialog() {
        if (mDialogScreen == null) {
            mDialogScreen = new MultiRTConfigDialog();
            mDialogScreen.prepare(getContext(), mVmInstallLauncher);
        }
        mDialogScreen.show();
    }
}
