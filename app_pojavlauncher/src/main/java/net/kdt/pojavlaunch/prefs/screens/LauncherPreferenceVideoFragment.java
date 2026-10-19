package net.kdt.pojavlaunch.prefs.screens;

import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;

import androidx.preference.ListPreference;
import androidx.preference.SwitchPreference;
import androidx.preference.SwitchPreferenceCompat;

import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.prefs.CustomSeekBarPreference;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;

/**
 * Fragment for any settings video related
 */
public class LauncherPreferenceVideoFragment extends LauncherPreferenceFragment {
    @Override
    public void onCreatePreferences(Bundle b, String str) {
        addPreferencesFromResource(R.xml.pref_video);
        int resolution = (int) (LauncherPreferences.PREF_SCALE_FACTOR * 100);

        //Disable notch checking behavior on android 8.1 and below.
        requirePreference("ignoreNotch").setVisible(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && LauncherPreferences.PREF_NOTCH_SIZE > 0);

        CustomSeekBarPreference resolutionSeekbar = requirePreference("resolutionRatio",
                CustomSeekBarPreference.class);
        resolutionSeekbar.setSuffix(" %");

        // #724 bug fix
        if (resolution < 25) {
            resolutionSeekbar.setValue(100);
        } else {
            resolutionSeekbar.setValue(resolution);
        }

        // Sustained performance is only available since Nougat
        SwitchPreference sustainedPerfSwitch = requirePreference("sustainedPerformance",
                SwitchPreference.class);
        sustainedPerfSwitch.setVisible(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N);
        sustainedPerfSwitch.setChecked(LauncherPreferences.PREF_SUSTAINED_PERFORMANCE);

        requirePreference("alternate_surface", SwitchPreferenceCompat.class).setChecked(LauncherPreferences.PREF_USE_ALTERNATE_SURFACE);
        requirePreference("force_vsync", SwitchPreferenceCompat.class).setChecked(LauncherPreferences.PREF_FORCE_VSYNC);

        ListPreference rendererListPreference = requirePreference("renderer",
                ListPreference.class);
        Tools.RenderersList renderersList = Tools.getCompatibleRenderers(getContext());
        rendererListPreference.setEntries(renderersList.rendererDisplayNames);
        rendererListPreference.setEntryValues(renderersList.rendererIds.toArray(new String[0]));

        requirePreference("rkb_quick_optimizer").setOnPreferenceClickListener(p -> { showOptimizer(); return true; });
        computeVisibility();
    }

    /**
     * One-tap presets. They only write EXISTING preferences (RAM allocation, resolution scale,
     * sustained performance) and the dialog lists the exact values before applying anything.
     */
    private void showOptimizer() {
        final android.content.Context c = getContext();
        if (c == null) return;
        final int deviceRam = Tools.getTotalDeviceMemory(c);
        final int maxRam = deviceRam < 3064 ? deviceRam - 800 : deviceRam - 1024;
        final int[][] presets = {   // {ramMB, resolution%, sustainedPerformance}
                {1536, 75, 1}, {2048, 90, 1}, {3072, 100, 0}};
        final String[] names = {"4 GB phone (budget)", "6 GB phone (balanced)", "8 GB+ phone (flagship)"};
        String[] labels = new String[names.length];
        for (int i = 0; i < names.length; i++) {
            int ram = Math.max(512, Math.min(presets[i][0], maxRam));
            labels[i] = names[i] + "\nRAM " + ram + " MB, resolution " + presets[i][1] + "%, sustained performance "
                    + (presets[i][2] == 1 ? "on" : "off");
        }
        new android.app.AlertDialog.Builder(c)
                .setTitle(R.string.rkb_pref_optimizer_title)
                .setItems(labels, (d, which) -> {
                    int ram = Math.max(512, Math.min(presets[which][0], maxRam));
                    LauncherPreferences.DEFAULT_PREF.edit()
                            .putInt("allocation", ram)
                            .putInt("resolutionRatio", presets[which][1])
                            .putBoolean("sustainedPerformance", presets[which][2] == 1)
                            .commit();
                    LauncherPreferences.loadPreferences(c.getApplicationContext());
                    ((CustomSeekBarPreference) requirePreference("resolutionRatio")).setValue(presets[which][1]);
                    ((SwitchPreference) requirePreference("sustainedPerformance")).setChecked(presets[which][2] == 1);
                    android.widget.Toast.makeText(c, "Applied: " + names[which], android.widget.Toast.LENGTH_LONG).show();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences p, String s) {
        super.onSharedPreferenceChanged(p, s);
        computeVisibility();
    }

    private void computeVisibility(){
        requirePreference("force_vsync", SwitchPreferenceCompat.class)
                .setVisible(LauncherPreferences.PREF_USE_ALTERNATE_SURFACE);
    }
}
