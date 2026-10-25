package net.kdt.pojavlaunch.rkb.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.kdt.mcgui.ProgressLayout;

import net.kdt.pojavlaunch.NewJREUtil;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.multirt.MultiRTUtils;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;
import net.kdt.pojavlaunch.progresskeeper.ProgressKeeper;
import net.kdt.pojavlaunch.progresskeeper.ProgressListener;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * "Java for Minecraft" picker (first run + Settings > RKB interface > Java runtime setup).
 * Offers only runtimes that are neither installed nor bundled in the APK, downloads the selected
 * ones through NewJREUtil (same store/validation as Settings > Java) and reports the real result
 * for every version: success is only claimed for runtimes that are actually installed afterwards.
 */
public final class RkbRuntimeSetup {
    private static final String PREF_DONE = "rkb_runtime_setup_done";
    private static final AtomicBoolean sRunning = new AtomicBoolean(false);

    private RkbRuntimeSetup() {}

    private static final class Option {
        final int version; final String compat; final boolean recommended;
        Option(int version, String compat, boolean recommended) { this.version = version; this.compat = compat; this.recommended = recommended; }
    }

    private static final Option[] OPTIONS = {
            new Option(8, "Minecraft 1.16.5 and older", false),
            new Option(17, "Minecraft 1.17 - 1.20.4", true),
            new Option(21, "Minecraft 1.20.5 and newer", true),
            new Option(25, "Latest Java - needed by Minecraft 26.1 and newer", false),
    };

    private static boolean isBundled(AssetManager am, int version) {
        String dir = version == 8 ? "jre" : version == 17 ? "jre-new" : version == 21 ? "jre-21" : null;
        if (dir == null) return false;
        try {
            String[] files = am.list("components/" + dir);
            return files != null && files.length > 0;
        } catch (Exception e) {
            return false;
        }
    }

    private static List<Option> missingOptions(Context c) {
        List<Option> list = new ArrayList<>();
        for (Option o : OPTIONS) {
            if (!NewJREUtil.isJavaVersionAvailableForDownload(o.version)) continue;
            if (Tools.DEVICE_ARCHITECTURE == net.kdt.pojavlaunch.Architecture.ARCH_X86 && o.version >= 21) continue;
            if (MultiRTUtils.getExactJreName(o.version) != null) continue;
            if (isBundled(c.getAssets(), o.version)) continue;
            list.add(o);
        }
        return list;
    }

    /** Shows the picker once on first run, only if something is missing and the device is online. */
    public static void maybeShowFirstRun(Activity a) {
        if (LauncherPreferences.DEFAULT_PREF == null) return;
        if (LauncherPreferences.DEFAULT_PREF.getBoolean(PREF_DONE, false)) return;
        if (!Tools.isOnline(a)) return; // try again next start instead of marking it done
        if (missingOptions(a).isEmpty()) {
            LauncherPreferences.DEFAULT_PREF.edit().putBoolean(PREF_DONE, true).apply();
            return;
        }
        show(a);
    }

    public static void show(final Activity a) {
        if (a.isFinishing() || a.isDestroyed()) return;
        if (sRunning.get()) {
            Toast.makeText(a, "A runtime download is already running", Toast.LENGTH_SHORT).show();
            return;
        }
        final List<Option> options = missingOptions(a);
        if (options.isEmpty()) {
            Toast.makeText(a, "All Java versions are already installed or bundled", Toast.LENGTH_LONG).show();
            return;
        }
        if (!Tools.isOnline(a)) {
            Toast.makeText(a, a.getString(net.kdt.pojavlaunch.R.string.multirt_no_internet), Toast.LENGTH_LONG).show();
            return;
        }

        LinearLayout box = new LinearLayout(a);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = RkbUi.dp(a, 18);
        box.setPadding(pad, RkbUi.dp(a, 8), pad, 0);
        TextView intro = RkbUi.text(a, "Minecraft needs Java. Choose which versions to install - you can add more later in Settings.",
                13, RkbUi.MUTED, false);
        box.addView(intro);
        final List<CheckBox> boxes = new ArrayList<>();
        for (Option o : options) {
            CheckBox cb = new CheckBox(a);
            cb.setChecked(o.recommended);
            cb.setText("Java " + o.version + (o.recommended ? "   [recommended]" : "") + "\n" + o.compat);
            cb.setTextColor(RkbUi.WHITE);
            cb.setTextSize(14);
            cb.setPadding(RkbUi.dp(a, 6), RkbUi.dp(a, 8), 0, RkbUi.dp(a, 8));
            box.addView(cb);
            boxes.add(cb);
        }
        ScrollView sv = new ScrollView(a);
        sv.addView(box);

        new AlertDialog.Builder(a)
                .setTitle("Java for Minecraft")
                .setView(sv)
                .setNegativeButton("Skip for now", (d, w) -> markDone())
                .setPositiveButton("Install", (d, w) -> {
                    markDone();
                    List<Integer> chosen = new ArrayList<>();
                    for (int i = 0; i < options.size(); i++) if (boxes.get(i).isChecked()) chosen.add(options.get(i).version);
                    if (chosen.isEmpty()) return;
                    install(a, chosen);
                })
                .show();
    }

    private static void markDone() {
        if (LauncherPreferences.DEFAULT_PREF != null) {
            LauncherPreferences.DEFAULT_PREF.edit().putBoolean(PREF_DONE, true).apply();
        }
    }

    private static void install(final Activity a, final List<Integer> versions) {
        if (!sRunning.compareAndSet(false, true)) return;
        final Handler main = new Handler(Looper.getMainLooper());

        LinearLayout box = new LinearLayout(a);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = RkbUi.dp(a, 20);
        box.setPadding(pad, pad, pad, pad);
        final TextView status = RkbUi.text(a, "Starting...", 14, RkbUi.WHITE, true);
        final ProgressBar bar = new ProgressBar(a, null, android.R.attr.progressBarStyleHorizontal);
        bar.setMax(100);
        bar.setIndeterminate(true);
        box.addView(status);
        box.addView(bar);
        final AlertDialog dialog = new AlertDialog.Builder(a).setTitle("Installing Java").setView(box)
                .setCancelable(false).create();
        dialog.show();

        final ProgressListener listener = new ProgressListener() {
            @Override public void onProgressStarted() { }
            @Override public void onProgressUpdated(int progress, int resid, Object... va) {
                main.post(() -> { if (progress >= 0) { bar.setIndeterminate(false); bar.setProgress(progress); } });
            }
            @Override public void onProgressEnded() { }
        };
        ProgressKeeper.addListener(ProgressLayout.UNPACK_RUNTIME, listener);

        new Thread(() -> {
            StringBuilder summary = new StringBuilder();
            int done = 0;
            for (int v : versions) {
                final int idx = ++done;
                main.post(() -> { status.setText("Java " + v + "  (" + idx + "/" + versions.size() + ")"); bar.setIndeterminate(true); });
                try {
                    NewJREUtil.tryDownloadRuntime(a.getApplicationContext(), v);
                    boolean ok = MultiRTUtils.getExactJreName(v) != null;
                    summary.append(ok ? "Java " + v + ": installed\n" : "Java " + v + ": not installed (unknown reason)\n");
                } catch (Throwable t) {
                    android.util.Log.e("RkbRuntimeSetup", "Java " + v + " failed", t);
                    summary.append("Java ").append(v).append(": FAILED - ").append(t.getMessage()).append('\n');
                }
            }
            ProgressKeeper.removeListener(ProgressLayout.UNPACK_RUNTIME, listener);
            sRunning.set(false);
            final String result = summary.toString().trim();
            main.post(() -> {
                try { dialog.dismiss(); } catch (Exception ignored) { }
                if (!a.isFinishing() && !a.isDestroyed()) {
                    new AlertDialog.Builder(a).setTitle("Java setup result").setMessage(result)
                            .setPositiveButton(android.R.string.ok, null).show();
                }
            });
        }, "RKB-RuntimeSetup").start();
    }
}
