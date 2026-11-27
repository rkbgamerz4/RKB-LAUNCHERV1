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
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.instances.Instance;
import net.kdt.pojavlaunch.instances.Instances;
import net.kdt.pojavlaunch.rkb.mods.ModInfo;
import net.kdt.pojavlaunch.rkb.mods.ModManager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Mod Settings screen. Reads and edits the real mods folder of the selected instance.
 * Enabled/disabled states are moved between mods/ and mods/disabled/ by ModManager.
 */
public class ModManagerFragment extends Fragment {
    private final Handler mMain = new Handler(Looper.getMainLooper());
    private ExecutorService mExecutor;
    private int mGeneration;

    private final List<Instance> mInstances = new ArrayList<>();
    private int mSelectedInstance = 0;
    private final List<ModInfo> mMods = new ArrayList<>();
    private final Map<String, Boolean> mPending = new HashMap<>();
    private String mQuery = "";
    private boolean mLoading;

    private TextView mStatus;
    private RecyclerView mRecycler;
    private ModAdapter mAdapter;
    private Spinner mInstanceSpinner;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        mExecutor = Executors.newSingleThreadExecutor();
        final android.content.Context c = requireContext();

        LinearLayout content = new LinearLayout(c);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(RkbUi.dp(c, 12), RkbUi.dp(c, 4), RkbUi.dp(c, 12), RkbUi.dp(c, 8));

        LinearLayout featured = RkbUi.card(c);
        featured.addView(RkbUi.text(c, "MOD SETTINGS", 11, RkbUi.BLUE, true));
        mInstanceSpinner = new Spinner(c);
        mInstanceSpinner.setBackground(RkbUi.rounded(c, RkbUi.CARD_2, 12, RkbUi.BORDER, 1));
        featured.addView(mInstanceSpinner, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, RkbUi.dp(c, 44)));
        mStatus = RkbUi.text(c, "Loading mods...", 12, RkbUi.MUTED, false);
        mStatus.setPadding(0, RkbUi.dp(c, 8), 0, 0);
        featured.addView(mStatus);
        content.addView(featured, lp(c));

        EditText search = new EditText(c);
        search.setHint("Search mods");
        search.setHintTextColor(RkbUi.MUTED);
        search.setTextColor(RkbUi.WHITE);
        search.setSingleLine(true);
        search.setPadding(RkbUi.dp(c, 14), 0, RkbUi.dp(c, 14), 0);
        search.setBackground(RkbUi.rounded(c, RkbUi.CARD, 14, RkbUi.BORDER, 1));
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int d) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int d) { }
            @Override public void afterTextChanged(Editable s) {
                mQuery = s == null ? "" : s.toString().trim().toLowerCase(Locale.ROOT);
                if (mAdapter != null) mAdapter.notifyDataSetChanged();
            }
        });
        content.addView(search, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, RkbUi.dp(c, 44)));

        LinearLayout actions = new LinearLayout(c);
        actions.setPadding(0, RkbUi.dp(c, 10), 0, RkbUi.dp(c, 6));
        TextView refresh = RkbUi.button(c, "REFRESH", false, v -> reload());
        TextView allOn = RkbUi.button(c, "ENABLE ALL", false, v -> bulk(true));
        TextView allOff = RkbUi.button(c, "DISABLE ALL", false, v -> bulk(false));
        LinearLayout.LayoutParams a = new LinearLayout.LayoutParams(0, RkbUi.dp(c, 40), 1f);
        LinearLayout.LayoutParams b = new LinearLayout.LayoutParams(0, RkbUi.dp(c, 40), 1f);
        LinearLayout.LayoutParams d = new LinearLayout.LayoutParams(0, RkbUi.dp(c, 40), 1f);
        b.leftMargin = RkbUi.dp(c, 6);
        d.leftMargin = RkbUi.dp(c, 6);
        actions.addView(refresh, a);
        actions.addView(allOn, b);
        actions.addView(allOff, d);
        content.addView(actions, lp(c));

        mAdapter = new ModAdapter();
        mRecycler = new RecyclerView(c);
        mRecycler.setLayoutManager(new GridLayoutManager(c, 2));
        mRecycler.setAdapter(mAdapter);
        mRecycler.setClipToPadding(false);
        content.addView(mRecycler, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        TextView apply = RkbUi.button(c, "APPLY CHANGES", true, v -> applyChanges());
        LinearLayout.LayoutParams applyLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, RkbUi.dp(c, 50));
        applyLp.topMargin = RkbUi.dp(c, 8);
        content.addView(apply, applyLp);

        loadInstances();
        return RkbUi.scaffold(c, RkbNav.Dest.MODS, "Mod Settings",
                "Enable or disable mods for one instance.", content);
    }

    private static LinearLayout.LayoutParams lp(android.content.Context c) {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private void loadInstances() {
        mInstances.clear();
        try {
            mInstances.addAll(Instances.loadAllInstances());
        } catch (IOException ignored) { }
        List<String> labels = new ArrayList<>();
        for (Instance instance : mInstances) {
            String name = Tools.isValidString(instance.name) ? instance.name : "Unnamed";
            String version = Tools.isValidString(instance.versionId) ? instance.versionId : "?";
            labels.add(name + " - " + version);
        }
        if (labels.isEmpty()) labels.add("No instances");
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mInstanceSpinner.setAdapter(adapter);
        mSelectedInstance = 0;
        mInstanceSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> parent, View view,
                                                 int position, long id) {
                mSelectedInstance = position;
                reload();
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) { }
        });
        reload();
    }

    private File gameDir() {
        if (mSelectedInstance >= 0 && mSelectedInstance < mInstances.size()) {
            return mInstances.get(mSelectedInstance).getGameDirectory();
        }
        return new File(Tools.DIR_GAME_NEW);
    }

    private void reload() {
        if (mExecutor == null || !isAdded()) return;
        final int gen = ++mGeneration;
        final File dir = gameDir();
        mLoading = true;
        mStatus.setText("Loading mods...");
        mExecutor.execute(() -> {
            List<ModInfo> mods = null;
            String error = null;
            try {
                mods = ModManager.listMods(dir);
                for (ModInfo mod : mods) ModManager.readMeta(mod);
            } catch (Exception e) {
                error = e.getMessage();
            }
            final List<ModInfo> result = mods;
            final String err = error;
            mMain.post(() -> {
                if (gen != mGeneration || !isAdded()) return;
                mLoading = false;
                mMods.clear();
                mPending.clear();
                if (result != null) mMods.addAll(result);
                if (mAdapter != null) mAdapter.notifyDataSetChanged();
                mStatus.setText(err != null ? "Could not read mods: " + err : summary());
            });
        });
    }

    private String summary() {
        int enabled = 0;
        for (ModInfo mod : mMods) if (displayEnabled(mod)) enabled++;
        return enabled + " enabled / " + mMods.size() + " mods";
    }

    private boolean displayEnabled(ModInfo mod) {
        Boolean pending = mPending.get(mod.absolutePath);
        return pending != null ? pending : mod.enabled;
    }

    private List<ModInfo> visibleMods() {
        if (TextUtils.isEmpty(mQuery)) return mMods;
        List<ModInfo> out = new ArrayList<>();
        for (ModInfo mod : mMods) {
            String name = displayName(mod).toLowerCase(Locale.ROOT);
            if (name.contains(mQuery)) out.add(mod);
        }
        return out;
    }

    private static String displayName(ModInfo mod) {
        return Tools.isValidString(mod.metaName) ? mod.metaName : mod.displayName;
    }

    private void bulk(final boolean enable) {
        if (mExecutor == null || mLoading) return;
        final File dir = gameDir();
        mExecutor.execute(() -> {
            if (enable) ModManager.enableAll(dir); else ModManager.disableAll(dir);
            mMain.post(() -> {
                if (isAdded()) Toast.makeText(requireContext(),
                        enable ? "All mods enabled" : "All mods disabled", Toast.LENGTH_SHORT).show();
                reload();
            });
        });
    }

    private void applyChanges() {
        if (mExecutor == null || mLoading) return;
        if (mPending.isEmpty()) {
            Toast.makeText(requireContext(), "No changes to apply", Toast.LENGTH_SHORT).show();
            return;
        }
        final File dir = gameDir();
        final List<ModInfo> snapshot = new ArrayList<>(mMods);
        final Map<String, Boolean> changes = new HashMap<>(mPending);
        mLoading = true;
        mStatus.setText("Applying changes...");
        mExecutor.execute(() -> {
            int applied = 0;
            for (ModInfo mod : snapshot) {
                Boolean target = changes.get(mod.absolutePath);
                if (target == null || target == mod.enabled) continue;
                if (ModManager.toggleMod(dir, mod)) applied++;
            }
            final int count = applied;
            mMain.post(() -> {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), "Applied " + count + " change(s)",
                        Toast.LENGTH_SHORT).show();
                reload();
            });
        });
    }

    private void delete(ModInfo mod) {
        if (mExecutor == null || mLoading) return;
        mExecutor.execute(() -> {
            ModManager.deleteMod(mod);
            mMain.post(() -> { if (isAdded()) reload(); });
        });
    }

    @Override public void onDestroyView() {
        mGeneration++;
        if (mExecutor != null) mExecutor.shutdownNow();
        mExecutor = null;
        mMain.removeCallbacksAndMessages(null);
        super.onDestroyView();
    }

    private final class ModAdapter extends RecyclerView.Adapter<ModAdapter.VH> {
        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LinearLayout card = RkbUi.card(parent.getContext());
            card.setPadding(RkbUi.dp(parent.getContext(), 12), RkbUi.dp(parent.getContext(), 12),
                    RkbUi.dp(parent.getContext(), 12), RkbUi.dp(parent.getContext(), 12));
            return new VH(card);
        }

        @Override public void onBindViewHolder(@NonNull VH h, int position) {
            final ModInfo mod = visibleMods().get(position);
            h.name.setText(displayName(mod));
            String version = Tools.isValidString(mod.version) ? "v" + mod.version : mod.fileName;
            h.meta.setText(version);
            boolean on = displayEnabled(mod);
            h.toggle.setText(on ? "ON" : "OFF");
            h.toggle.setTextColor(on ? RkbUi.WHITE : RkbUi.MUTED);
            h.toggle.setBackground(RkbUi.rounded(h.itemView.getContext(),
                    on ? RkbUi.BLUE : RkbUi.CARD_2, 12,
                    on ? 0x00000000 : RkbUi.BORDER, on ? 0 : 1));
            h.toggle.setOnClickListener(v -> {
                boolean current = displayEnabled(mod);
                mPending.put(mod.absolutePath, !current);
                notifyItemChanged(position);
                mStatus.setText(summary());
            });
            h.delete.setOnClickListener(v -> new AlertDialog.Builder(v.getContext())
                    .setTitle("Delete mod?")
                    .setMessage(mod.fileName)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton("Delete", (d, w) -> delete(mod))
                    .show());
        }

        @Override public int getItemCount() { return visibleMods().size(); }

        final class VH extends RecyclerView.ViewHolder {
            final TextView name;
            final TextView meta;
            final TextView toggle;
            final TextView delete;
            VH(View item) {
                super(item);
                LinearLayout card = (LinearLayout) item;
                name = RkbUi.text(item.getContext(), "", 14, RkbUi.WHITE, true);
                meta = RkbUi.text(item.getContext(), "", 11, RkbUi.MUTED, false);
                LinearLayout top = new LinearLayout(item.getContext());
                top.setOrientation(LinearLayout.VERTICAL);
                top.addView(name);
                top.addView(meta);
                card.addView(top, new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));

                LinearLayout row = new LinearLayout(item.getContext());
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(0, RkbUi.dp(item.getContext(), 10), 0, 0);
                toggle = RkbUi.text(item.getContext(), "OFF", 11, RkbUi.MUTED, true);
                toggle.setGravity(Gravity.CENTER);
                toggle.setPadding(RkbUi.dp(item.getContext(), 12), 0,
                        RkbUi.dp(item.getContext(), 12), 0);
                toggle.setClickable(true);
                toggle.setMinimumHeight(RkbUi.dp(item.getContext(), 34));
                delete = RkbUi.text(item.getContext(), "DEL", 10, RkbUi.RED, true);
                delete.setGravity(Gravity.CENTER);
                delete.setPadding(RkbUi.dp(item.getContext(), 8), RkbUi.dp(item.getContext(), 8),
                        RkbUi.dp(item.getContext(), 8), RkbUi.dp(item.getContext(), 8));
                delete.setClickable(true);
                row.addView(toggle, new LinearLayout.LayoutParams(0,
                        RkbUi.dp(item.getContext(), 34), 1f));
                row.addView(delete);
                card.addView(row, new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            }
        }
    }
}
