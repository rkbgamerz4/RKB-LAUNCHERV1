package net.kdt.pojavlaunch.rkb.ui;

import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;
import net.kdt.pojavlaunch.rkb.mods.ModInfo;
import net.kdt.pojavlaunch.rkb.mods.ModManager;
import net.kdt.pojavlaunch.value.launcherprofiles.LauncherProfiles;
import net.kdt.pojavlaunch.value.launcherprofiles.MinecraftProfile;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * RKB Mod Manager screen. All mod state changes go through the existing {@link ModManager}
 * (files move between mods/ and mods/disabled/). Disk work runs on a single background thread and
 * results are dropped if the screen was destroyed in the meantime.
 */
public class ModManagerFragment extends Fragment {
    public static final String TAG = "RKB_MOD_MANAGER";

    private static final int FILTER_ALL = 0, FILTER_ON = 1, FILTER_OFF = 2;
    private static final int SORT_AZ = 0, SORT_ZA = 1, SORT_ON_FIRST = 2;
    private static final String[] SORT_LABELS = {"Name A-Z", "Name Z-A", "Enabled first"};

    private final Handler mMain = new Handler(Looper.getMainLooper());
    private ExecutorService mExecutor;
    private int mGeneration; // bumped on every reload / destroy to invalidate stale results

    private final List<ModInfo> mAll = new ArrayList<>();
    private final List<ModInfo> mShown = new ArrayList<>();
    private String mSelectedKey;
    private String mQuery = "";
    private int mFilter = FILTER_ALL;
    private int mSort = SORT_AZ;
    private boolean mLoading;
    private String mError;

    private ModAdapter mAdapter;
    private TextView mStateView;
    private TextView mSortButton;
    private LinearLayout mTabs;
    private LinearLayout mFilterRow;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        mExecutor = Executors.newSingleThreadExecutor();
        final android.content.Context c = requireContext();

        LinearLayout content = new LinearLayout(c);
        content.setOrientation(LinearLayout.VERTICAL);

        // search + sort
        LinearLayout searchRow = new LinearLayout(c);
        searchRow.setGravity(Gravity.CENTER_VERTICAL);
        EditText search = new EditText(c);
        search.setHint("Search mods...");
        search.setHintTextColor(RkbUi.MUTED);
        search.setTextColor(RkbUi.WHITE);
        search.setTextSize(14);
        search.setSingleLine(true);
        search.setPadding(RkbUi.dp(c, 14), 0, RkbUi.dp(c, 14), 0);
        search.setBackground(RkbUi.rounded(c, RkbUi.CARD, 12, RkbUi.BORDER, 1));
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int d) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int d) {}
            @Override public void afterTextChanged(Editable s) {
                mQuery = s.toString().trim().toLowerCase(Locale.ROOT);
                applyFilter();
            }
        });
        searchRow.addView(search, new LinearLayout.LayoutParams(0, RkbUi.dp(c, 42), 1f));

        mSortButton = RkbUi.button(c, "Sort: " + SORT_LABELS[mSort], false, v -> {
            mSort = (mSort + 1) % SORT_LABELS.length;
            mSortButton.setText("Sort: " + SORT_LABELS[mSort]);
            applyFilter();
        });
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, RkbUi.dp(c, 42));
        slp.leftMargin = RkbUi.dp(c, 8);
        searchRow.addView(mSortButton, slp);
        content.addView(searchRow);

        // filter chips
        mFilterRow = new LinearLayout(c);
        mFilterRow.setPadding(0, RkbUi.dp(c, 8), 0, RkbUi.dp(c, 4));
        content.addView(mFilterRow);
        buildFilterChips();

        // instance tabs (existing per-instance behaviour)
        HorizontalScrollView hsv = new HorizontalScrollView(c);
        hsv.setHorizontalScrollBarEnabled(false);
        mTabs = new LinearLayout(c);
        hsv.addView(mTabs);
        content.addView(hsv);

        // list + state text
        android.widget.FrameLayout listFrame = new android.widget.FrameLayout(c);
        RecyclerView rv = new RecyclerView(c);
        rv.setLayoutManager(new LinearLayoutManager(c));
        rv.setHasFixedSize(false);
        rv.setClipToPadding(false);
        rv.setNestedScrollingEnabled(true);
        rv.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
        rv.setItemViewCacheSize(10);
        // extra bottom space so the last mod can scroll fully clear of the action row
        rv.setPadding(0, RkbUi.dp(c, 8), 0, RkbUi.dp(c, 24));
        mAdapter = new ModAdapter();
        rv.setAdapter(mAdapter);
        listFrame.addView(rv, RkbUi.match());

        mStateView = RkbUi.text(c, "", 14, RkbUi.MUTED, false);
        mStateView.setGravity(Gravity.CENTER);
        listFrame.addView(mStateView, RkbUi.match());
        content.addView(listFrame, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        // actions
        LinearLayout actions = new LinearLayout(c);
        actions.setGravity(Gravity.CENTER_VERTICAL);
        actions.addView(actionButton(c, "Refresh", false, v -> reload()));
        actions.addView(actionButton(c, "Enable all", false, v -> bulk(true)));
        actions.addView(actionButton(c, "Disable all", false, v -> bulk(false)));
        actions.addView(actionButton(c, "Open folder", true, v -> openFolder()));
        content.addView(actions, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, RkbUi.dp(c, 44)));

        buildTabs();
        reload();
        return RkbUi.scaffold(c, RkbNav.Dest.MODS, "Mod Manager",
                "Manage your mods and enhance your gameplay.", content);
    }

    @Override
    public void onDestroyView() {
        mGeneration++;
        mMain.removeCallbacksAndMessages(null);
        if (mExecutor != null) mExecutor.shutdownNow();
        mExecutor = null;
        mAdapter = null;
        super.onDestroyView();
    }

    private View actionButton(android.content.Context c, String label, boolean filled, View.OnClickListener l) {
        TextView b = RkbUi.button(c, label, filled, l);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
        lp.rightMargin = RkbUi.dp(c, 6);
        b.setLayoutParams(lp);
        b.setTextSize(12);
        b.setPadding(RkbUi.dp(c, 6), 0, RkbUi.dp(c, 6), 0);
        return b;
    }

    // ---------------------------------------------------------------- filters / tabs

    private void buildFilterChips() {
        final android.content.Context c = requireContext();
        mFilterRow.removeAllViews();
        String[] names = {"All", "Enabled", "Disabled"};
        for (int i = 0; i < names.length; i++) {
            final int f = i;
            TextView chip = RkbUi.button(c, names[i], mFilter == i, v -> {
                mFilter = f;
                buildFilterChips();
                applyFilter();
            });
            chip.setMinHeight(RkbUi.dp(c, 32));
            chip.setTextSize(12);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, RkbUi.dp(c, 32));
            lp.rightMargin = RkbUi.dp(c, 8);
            mFilterRow.addView(chip, lp);
        }
    }

    private void buildTabs() {
        mTabs.removeAllViews();
        final android.content.Context c = requireContext();
        try {
            LauncherProfiles.load();
            Map<String, MinecraftProfile> map = LauncherProfiles.mainProfileJson.profiles;
            mSelectedKey = LauncherPreferences.DEFAULT_PREF.getString(
                    LauncherPreferences.PREF_KEY_CURRENT_PROFILE, null);
            if (map == null || map.size() < 2) return; // a single instance needs no tab bar
            if (mSelectedKey == null || !map.containsKey(mSelectedKey)) {
                mSelectedKey = map.keySet().iterator().next();
            }
            for (Map.Entry<String, MinecraftProfile> e : map.entrySet()) {
                final String key = e.getKey();
                String name = Tools.isValidString(e.getValue().name) ? e.getValue().name : key;
                TextView tab = RkbUi.button(c, name, key.equals(mSelectedKey), v -> {
                    mSelectedKey = key;
                    LauncherPreferences.DEFAULT_PREF.edit()
                            .putString(LauncherPreferences.PREF_KEY_CURRENT_PROFILE, key).apply();
                    buildTabs();
                    reload();
                });
                tab.setMinHeight(RkbUi.dp(c, 32));
                tab.setTextSize(12);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, RkbUi.dp(c, 32));
                lp.rightMargin = RkbUi.dp(c, 8);
                lp.topMargin = RkbUi.dp(c, 4);
                mTabs.addView(tab, lp);
            }
        } catch (Exception ex) {
            android.util.Log.w("RKB-ModManager", "Could not build instance tabs", ex);
        }
    }

    // ---------------------------------------------------------------- loading (background)

    private File getGameDir() {
        try {
            String key = Tools.isValidString(mSelectedKey) ? mSelectedKey
                    : LauncherPreferences.DEFAULT_PREF.getString(LauncherPreferences.PREF_KEY_CURRENT_PROFILE, null);
            if (Tools.isValidString(key)) {
                LauncherProfiles.load();
                MinecraftProfile p = LauncherProfiles.mainProfileJson.profiles.get(key);
                if (p != null) return Tools.getGameDirPath(p);
            }
        } catch (Exception ignored) { }
        return new File(Tools.DIR_GAME_NEW);
    }

    private void reload() {
        if (mExecutor == null) return;
        final int gen = ++mGeneration;
        final File gameDir = getGameDir(); // tiny json read; the heavy scan happens below
        mLoading = true;
        mError = null;
        updateState();
        try {
            mExecutor.execute(() -> {
                List<ModInfo> mods = null;
                String err = null;
                try {
                    mods = ModManager.listMods(gameDir);
                    for (ModInfo m : mods) {
                        if (gen != mGeneration) return; // cancelled
                        ModManager.readMeta(m);
                    }
                } catch (Throwable t) {
                    err = String.valueOf(t.getMessage());
                }
                final List<ModInfo> result = mods;
                final String error = err;
                mMain.post(() -> {
                    if (gen != mGeneration || !isAdded() || mAdapter == null) return;
                    mLoading = false;
                    mError = error;
                    mAll.clear();
                    if (result != null) mAll.addAll(result);
                    applyFilter();
                });
            });
        } catch (java.util.concurrent.RejectedExecutionException ignored) { }
    }

    private void applyFilter() {
        if (mAdapter == null) return;
        mShown.clear();
        for (ModInfo m : mAll) {
            if (mFilter == FILTER_ON && !m.enabled) continue;
            if (mFilter == FILTER_OFF && m.enabled) continue;
            if (!mQuery.isEmpty()) {
                String hay = (title(m) + " " + m.fileName + " " + (m.description == null ? "" : m.description))
                        .toLowerCase(Locale.ROOT);
                if (!hay.contains(mQuery)) continue;
            }
            mShown.add(m);
        }
        Collections.sort(mShown, (a, b) -> {
            if (mSort == SORT_ON_FIRST && a.enabled != b.enabled) return a.enabled ? -1 : 1;
            int cmp = title(a).toLowerCase(Locale.ROOT).compareTo(title(b).toLowerCase(Locale.ROOT));
            return mSort == SORT_ZA ? -cmp : cmp;
        });
        mAdapter.notifyDataSetChanged();
        updateState();
    }

    private void updateState() {
        if (mStateView == null) return;
        String msg = null;
        if (mLoading) msg = "Loading mods...";
        else if (mError != null) msg = "Could not read the mods folder:\n" + mError + "\n\nTap Refresh to retry.";
        else if (mAll.isEmpty()) msg = "No mods in this instance.\nPut .jar files in the mods folder.";
        else if (mShown.isEmpty()) msg = "No mods match your search or filter.";
        mStateView.setText(msg == null ? "" : msg);
        mStateView.setVisibility(msg == null ? View.GONE : View.VISIBLE);
    }

    // ---------------------------------------------------------------- actions (background file moves)

    private static String title(ModInfo m) {
        return m.metaName != null ? m.metaName : m.displayName;
    }

    private final java.util.Set<String> mBusy = new java.util.HashSet<>();

    private void toggle(ModInfo mod) {
        if (mExecutor == null) return;
        // ignore taps on a mod whose file move is still in flight (prevents double toggles)
        if (!mBusy.add(mod.absolutePath)) return;
        final String busyKey = mod.absolutePath;
        final File gameDir = getGameDir();
        final boolean wasEnabled = mod.enabled;
        try {
            mExecutor.execute(() -> {
                final boolean ok = ModManager.toggleMod(gameDir, mod);
                mMain.post(() -> {
                    mBusy.remove(busyKey);
                    if (!isAdded() || mAdapter == null) return;
                    if (!ok) Toast.makeText(requireContext(), "Could not change " + title(mod), Toast.LENGTH_SHORT).show();
                    else Toast.makeText(requireContext(), (wasEnabled ? "Disabled " : "Enabled ") + title(mod), Toast.LENGTH_SHORT).show();
                    applyFilter();
                });
            });
        } catch (java.util.concurrent.RejectedExecutionException ignored) { mBusy.remove(busyKey); }
    }

    private void bulk(boolean enable) {
        if (mExecutor == null || mAll.isEmpty()) return;
        final File gameDir = getGameDir();
        new AlertDialog.Builder(requireContext())
                .setTitle(enable ? "Enable all mods?" : "Disable all mods?")
                .setMessage("This moves every mod file in this instance " + (enable ? "into" : "out of") + " the active mods folder. Nothing is deleted.")
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, (d, w) -> {
                    try {
                        mExecutor.execute(() -> {
                            if (enable) ModManager.enableAll(gameDir); else ModManager.disableAll(gameDir);
                            mMain.post(() -> { if (isAdded()) reload(); });
                        });
                    } catch (java.util.concurrent.RejectedExecutionException ignored) { }
                }).show();
    }

    private void openFolder() {
        try {
            File mods = ModManager.getModsDir(getGameDir());
            //noinspection ResultOfMethodCallIgnored
            mods.mkdirs();
            Tools.openPath(requireContext(), mods, false);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Could not open the mods folder", Toast.LENGTH_SHORT).show();
        }
    }

    private void showDetails(ModInfo m) {
        String msg = "File: " + m.fileName
                + "\nStatus: " + (m.enabled ? "Enabled" : "Disabled")
                + (m.version != null ? "\nVersion: " + m.version : "")
                + "\nSize: " + android.text.format.Formatter.formatShortFileSize(requireContext(), m.sizeBytes)
                + (m.description != null ? "\n\n" + m.description : "")
                + "\n\n" + m.absolutePath;
        new AlertDialog.Builder(requireContext()).setTitle(title(m)).setMessage(msg)
                .setPositiveButton(android.R.string.ok, null).show();
    }

    private void confirmDelete(ModInfo m) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete " + title(m) + "?")
                .setMessage("The jar file will be permanently deleted from this instance.")
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton("Delete", (d, w) -> {
                    if (mExecutor == null) return;
                    try {
                        mExecutor.execute(() -> {
                            final boolean ok = ModManager.deleteMod(m);
                            mMain.post(() -> {
                                if (!isAdded()) return;
                                Toast.makeText(requireContext(), ok ? "Deleted" : "Delete failed", Toast.LENGTH_SHORT).show();
                                reload();
                            });
                        });
                    } catch (java.util.concurrent.RejectedExecutionException ignored) { }
                }).show();
    }

    // ---------------------------------------------------------------- adapter

    private final class ModAdapter extends RecyclerView.Adapter<ModAdapter.Holder> {
        final class Holder extends RecyclerView.ViewHolder {
            final TextView icon, name, meta, desc, toggle, more;
            Holder(View v, TextView icon, TextView name, TextView meta, TextView desc, TextView toggle, TextView more) {
                super(v);
                this.icon = icon; this.name = name; this.meta = meta; this.desc = desc;
                this.toggle = toggle; this.more = more;
            }
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            android.content.Context c = parent.getContext();
            LinearLayout card = new LinearLayout(c);
            card.setGravity(Gravity.CENTER_VERTICAL);
            card.setPadding(RkbUi.dp(c, 12), RkbUi.dp(c, 10), RkbUi.dp(c, 8), RkbUi.dp(c, 10));
            card.setBackground(RkbUi.rounded(c, RkbUi.CARD, 14, RkbUi.BORDER, 1));
            RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = RkbUi.dp(c, 8);
            card.setLayoutParams(lp);

            TextView icon = RkbUi.text(c, "", 16, RkbUi.BLUE, true);
            icon.setGravity(Gravity.CENTER);
            icon.setBackground(RkbUi.rounded(c, RkbUi.CARD_2, 10, RkbUi.BORDER, 1));
            LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(RkbUi.dp(c, 40), RkbUi.dp(c, 40));
            ilp.rightMargin = RkbUi.dp(c, 12);
            card.addView(icon, ilp);

            LinearLayout info = new LinearLayout(c);
            info.setOrientation(LinearLayout.VERTICAL);
            TextView name = RkbUi.text(c, "", 14, RkbUi.WHITE, true);
            name.setSingleLine(true);
            name.setEllipsize(TextUtils.TruncateAt.END);
            TextView meta = RkbUi.text(c, "", 11, RkbUi.BLUE, false);
            meta.setSingleLine(true);
            TextView desc = RkbUi.text(c, "", 12, RkbUi.MUTED, false);
            desc.setMaxLines(1);
            desc.setEllipsize(TextUtils.TruncateAt.END);
            info.addView(name); info.addView(meta); info.addView(desc);
            card.addView(info, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            TextView toggle = RkbUi.text(c, "", 11, RkbUi.WHITE, true);
            toggle.setGravity(Gravity.CENTER);
            toggle.setClickable(true);
            LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(RkbUi.dp(c, 58), RkbUi.dp(c, 30));
            tlp.leftMargin = RkbUi.dp(c, 8);
            card.addView(toggle, tlp);

            TextView more = RkbUi.text(c, "\u22EE", 22, RkbUi.WHITE, false);
            more.setGravity(Gravity.CENTER);
            more.setClickable(true);
            card.addView(more, new LinearLayout.LayoutParams(RkbUi.dp(c, 36), RkbUi.dp(c, 40)));
            return new Holder(card, icon, name, meta, desc, toggle, more);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder h, int position) {
            final ModInfo m = mShown.get(position);
            android.content.Context c = h.itemView.getContext();
            String t = title(m);
            h.icon.setText(t.isEmpty() ? "?" : t.substring(0, 1).toUpperCase(Locale.ROOT));
            h.name.setText(t);
            h.meta.setText((m.version != null ? "v" + m.version + "  \u2022  " : "") + (m.enabled ? "Enabled" : "Disabled"));
            h.meta.setTextColor(m.enabled ? RkbUi.GREEN : RkbUi.MUTED);
            h.desc.setText(m.description != null ? m.description : m.fileName);
            h.toggle.setText(m.enabled ? "ON" : "OFF");
            h.toggle.setTextColor(m.enabled ? RkbUi.WHITE : RkbUi.MUTED);
            h.toggle.setBackground(RkbUi.ripple(c, m.enabled
                    ? RkbUi.rounded(c, RkbUi.BLUE, 15, 0, 0)
                    : RkbUi.rounded(c, RkbUi.CARD_2, 15, RkbUi.BORDER, 1)));
            h.toggle.setOnClickListener(v -> toggle(m));
            h.itemView.setOnClickListener(v -> showDetails(m));
            h.more.setOnClickListener(v -> {
                PopupMenu pm = new PopupMenu(v.getContext(), v);
                pm.getMenu().add(0, 1, 0, "Details");
                pm.getMenu().add(0, 2, 1, m.enabled ? "Disable" : "Enable");
                pm.getMenu().add(0, 3, 2, "Delete");
                pm.setOnMenuItemClickListener(item -> {
                    if (item.getItemId() == 1) showDetails(m);
                    else if (item.getItemId() == 2) toggle(m);
                    else confirmDelete(m);
                    return true;
                });
                pm.show();
            });
        }

        @Override
        public int getItemCount() {
            return mShown.size();
        }
    }
}
