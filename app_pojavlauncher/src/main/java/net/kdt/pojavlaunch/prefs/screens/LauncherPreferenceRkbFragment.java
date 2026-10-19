package net.kdt.pojavlaunch.prefs.screens;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.format.Formatter;
import android.widget.Toast;

import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.prefs.CustomSeekBarPreference;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;
import net.kdt.pojavlaunch.progresskeeper.ProgressKeeper;
import net.kdt.pojavlaunch.rkb.ui.RkbRuntimeSetup;

import java.io.File;

/**
 * Settings > RKB interface. The animation preferences are read live by RkbMotion; the three actions
 * below do real work (Java downloads, cache deletion, settings reset) and report real results.
 */
public class LauncherPreferenceRkbFragment extends LauncherPreferenceFragment {
    private final Handler mMain = new Handler(Looper.getMainLooper());

    @Override
    public void onCreatePreferences(Bundle b, String str) {
        addPreferencesFromResource(R.xml.pref_rkb);

        requirePreference("rkb_runtime_setup").setOnPreferenceClickListener(p -> {
            if (getActivity() != null) RkbRuntimeSetup.show(getActivity());
            return true;
        });
        requirePreference("rkb_clear_caches").setOnPreferenceClickListener(p -> { confirmClearCaches(); return true; });
        requirePreference("rkb_reset_settings").setOnPreferenceClickListener(p -> { confirmReset(); return true; });
    }

    // ---------------------------------------------------------------- caches

    private void confirmClearCaches() {
        final Context c = getContext();
        if (c == null) return;
        if (ProgressKeeper.hasOngoingTasks()) {
            Toast.makeText(c, R.string.tasks_ongoing, Toast.LENGTH_LONG).show();
            return;
        }
        new AlertDialog.Builder(c)
                .setTitle(R.string.rkb_pref_clear_caches)
                .setMessage(R.string.rkb_pref_clear_caches_desc)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, (d, w) -> clearCaches(c.getApplicationContext()))
                .show();
    }

    private void clearCaches(final Context app) {
        new Thread(() -> {
            long freed = 0;
            File cache = app.getCacheDir();
            File[] children = cache == null ? null : cache.listFiles();
            if (children != null) for (File f : children) freed += deleteRecursive(f);
            final String msg = "Cleared " + Formatter.formatShortFileSize(app, freed);
            mMain.post(() -> Toast.makeText(app, msg, Toast.LENGTH_LONG).show());
        }, "RKB-ClearCaches").start();
    }

    /** @return bytes actually deleted (files that cannot be deleted are skipped, never fatal) */
    private static long deleteRecursive(File f) {
        long size = 0;
        if (f.isDirectory()) {
            File[] kids = f.listFiles();
            if (kids != null) for (File k : kids) size += deleteRecursive(k);
        } else {
            size = f.length();
        }
        return f.delete() ? size : 0;
    }

    // ---------------------------------------------------------------- reset

    private void confirmReset() {
        final Context c = getContext();
        if (c == null) return;
        new AlertDialog.Builder(c)
                .setTitle(R.string.rkb_pref_reset)
                .setMessage(R.string.rkb_pref_reset_desc)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton("Reset", (d, w) -> {
                    // Keep which instance is selected. The signed-in account lives in its own file
                    // ("pojav_profile") and is not touched by clearing the launcher preferences.
                    String instance = LauncherPreferences.DEFAULT_PREF.getString(LauncherPreferences.PREF_KEY_CURRENT_PROFILE, null);
                    LauncherPreferences.DEFAULT_PREF.edit().clear().commit();
                    if (instance != null) {
                        LauncherPreferences.DEFAULT_PREF.edit().putString(LauncherPreferences.PREF_KEY_CURRENT_PROFILE, instance).commit();
                    }
                    LauncherPreferences.loadPreferences(c.getApplicationContext());
                    Toast.makeText(c, "Launcher settings reset", Toast.LENGTH_LONG).show();
                    if (getActivity() != null) getActivity().recreate(); // rebuild every screen with the defaults
                }).show();
    }

    @Override
    public void onDestroyView() {
        mMain.removeCallbacksAndMessages(null);
        super.onDestroyView();
    }
}
