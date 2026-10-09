package net.kdt.pojavlaunch.rkb.ui;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;
import net.kdt.pojavlaunch.rkb.mods.ModInfo;
import net.kdt.pojavlaunch.rkb.mods.ModManager;
import net.kdt.pojavlaunch.value.launcherprofiles.LauncherProfiles;
import net.kdt.pojavlaunch.value.launcherprofiles.MinecraftProfile;

import java.io.File;
import java.util.List;
import java.util.Map;

public class ModManagerFragment extends Fragment {
    public static final String TAG = "RKB_MOD_MANAGER";

    private static final int BG     = 0xFF070B12;
    private static final int CARD   = 0xFF0F1620;
    private static final int ACCENT = 0xFF00B4FF;
    private static final int GREEN  = 0xFF00E676;
    private static final int RED    = 0xFFFF5252;
    private static final int TEXT   = 0xFFE8EEF5;
    private static final int MUTED  = 0xFF7A8BA0;
    private static final int CHIP   = 0xFF1A2433;

    private LinearLayout mList;
    private LinearLayout mTabs;
    private TextView mSubtitle;
    private String mSelectedKey;

    @Nullable
    @Override
    public View onCreateView(@NonNull android.view.LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        ScrollView scroll = new ScrollView(requireContext());
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);

        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(12), dp(16), dp(28));
        scroll.addView(root);

        // Header row
        LinearLayout header = row();
        Button back = chip("← Back", CHIP, TEXT);
        back.setOnClickListener(v -> requireActivity().onBackPressed());
        header.addView(back);

        TextView title = new TextView(requireContext());
        title.setText("  RKB Mods");
        title.setTextColor(ACCENT);
        title.setTextSize(20);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(title);
        root.addView(header);

        space(root, 6);

        mSubtitle = new TextView(requireContext());
        mSubtitle.setTextColor(MUTED);
        mSubtitle.setTextSize(12);
        mSubtitle.setText("Per instance · enable / disable");
        root.addView(mSubtitle);

        space(root, 12);

        // Instance tabs
        HorizontalScrollView hsv = new HorizontalScrollView(requireContext());
        hsv.setHorizontalScrollBarEnabled(false);
        mTabs = row();
        hsv.addView(mTabs);
        root.addView(hsv);

        space(root, 14);

        mList = new LinearLayout(requireContext());
        mList.setOrientation(LinearLayout.VERTICAL);
        root.addView(mList);

        space(root, 16);

        LinearLayout actions = row();
        Button refresh = chip("Refresh", CHIP, TEXT);
        LinearLayout.LayoutParams p1 = new LinearLayout.LayoutParams(0, dp(42), 1f);
        p1.setMarginEnd(dp(8));
        refresh.setLayoutParams(p1);
        refresh.setOnClickListener(v -> reload());
        actions.addView(refresh);

        Button open = chip("Open folder", ACCENT, Color.WHITE);
        LinearLayout.LayoutParams p2 = new LinearLayout.LayoutParams(0, dp(42), 1f);
        open.setLayoutParams(p2);
        open.setOnClickListener(v -> {
            File mods = ModManager.getModsDir(getGameDir());
            //noinspection ResultOfMethodCallIgnored
            mods.mkdirs();
            Tools.openPath(requireContext(), mods, false);
        });
        actions.addView(open);
        root.addView(actions);

        buildTabs();
        reload();
        return scroll;
    }

    private void buildTabs() {
        mTabs.removeAllViews();
        try {
            LauncherProfiles.load();
            Map<String, MinecraftProfile> map = LauncherProfiles.mainProfileJson.profiles;
            mSelectedKey = LauncherPreferences.DEFAULT_PREF.getString(
                    LauncherPreferences.PREF_KEY_CURRENT_PROFILE, null);

            if (map == null || map.isEmpty()) {
                addTab("Default", null, true);
                return;
            }

            boolean any = false;
            for (Map.Entry<String, MinecraftProfile> e : map.entrySet()) {
                String key = e.getKey();
                String name = e.getValue().name;
                if (!Tools.isValidString(name)) name = key;
                boolean selected = key.equals(mSelectedKey);
                if (selected) any = true;
                addTab(name, key, selected);
            }
            if (!any && map.size() > 0) {
                // first as selected
                String first = map.keySet().iterator().next();
                mSelectedKey = first;
                buildTabs();
            }
        } catch (Exception ex) {
            addTab("Default", null, true);
        }
    }

    private void addTab(String label, String key, boolean selected) {
        Button b = chip(label, selected ? ACCENT : CHIP, selected ? Color.WHITE : TEXT);
        b.setOnClickListener(v -> {
            mSelectedKey = key;
            if (key != null) {
                LauncherPreferences.DEFAULT_PREF.edit()
                        .putString(LauncherPreferences.PREF_KEY_CURRENT_PROFILE, key)
                        .apply();
            }
            buildTabs();
            reload();
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(36));
        lp.setMarginEnd(dp(8));
        b.setLayoutParams(lp);
        mTabs.addView(b);
    }

    private void reload() {
        mList.removeAllViews();
        File gameDir = getGameDir();
        List<ModInfo> mods = ModManager.listMods(gameDir);

        int on = 0;
        for (ModInfo m : mods) if (m.enabled) on++;

        mSubtitle.setText(mods.size() + " mods  ·  " + on + " enabled  ·  "
                + (mods.size() - on) + " disabled");

        if (mods.isEmpty()) {
            TextView empty = new TextView(requireContext());
            empty.setTextColor(MUTED);
            empty.setTextSize(14);
            empty.setPadding(0, dp(32), 0, 0);
            empty.setText("No mods in this instance.\nPut .jar files in the mods folder.");
            mList.addView(empty);
            return;
        }

        for (ModInfo mod : mods) {
            mList.addView(modCard(gameDir, mod));
            space(mList, 10);
        }
    }

    private View modCard(File gameDir, ModInfo mod) {
        LinearLayout card = new LinearLayout(requireContext());
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(14), dp(12), dp(12), dp(12));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(CARD);
        bg.setCornerRadius(dp(14));
        bg.setStroke(dp(1), 0xFF1C2838);
        card.setBackground(bg);

        // Icon circle
        TextView icon = new TextView(requireContext());
        icon.setText("MOD");
        icon.setTextSize(10);
        icon.setTypeface(Typeface.DEFAULT_BOLD);
        icon.setTextColor(ACCENT);
        icon.setTextSize(18);
        icon.setGravity(Gravity.CENTER);
        GradientDrawable iconBg = new GradientDrawable();
        iconBg.setColor(0xFF152030);
        iconBg.setCornerRadius(dp(10));
        icon.setBackground(iconBg);
        icon.setPadding(dp(8), dp(8), dp(8), dp(8));
        LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(dp(40), dp(40));
        iconLp.setMarginEnd(dp(12));
        icon.setLayoutParams(iconLp);
        card.addView(icon);

        // Name + status
        LinearLayout info = new LinearLayout(requireContext());
        info.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams infoLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        info.setLayoutParams(infoLp);

        TextView name = new TextView(requireContext());
        name.setText(mod.displayName);
        name.setTextColor(TEXT);
        name.setTextSize(14);
        name.setTypeface(Typeface.DEFAULT_BOLD);
        name.setMaxLines(1);
        info.addView(name);

        TextView sub = new TextView(requireContext());
        sub.setText(mod.enabled ? "Enabled" : "Disabled");
        sub.setTextColor(mod.enabled ? GREEN : RED);
        sub.setTextSize(12);
        info.addView(sub);
        card.addView(info);

        // Switch-style toggle
        Button toggle = switchBtn(mod.enabled);
        final ModInfo ref = mod;
        toggle.setOnClickListener(v -> {
            boolean ok = ref.enabled
                    ? ModManager.disableMod(gameDir, ref)
                    : ModManager.enableMod(gameDir, ref);
            Toast.makeText(requireContext(),
                    ok ? ((ref.enabled ? "OFF" : "ON") + " · " + ref.displayName) : "Failed",
                    Toast.LENGTH_SHORT).show();
            reload();
        });
        card.addView(toggle);
        return card;
    }

    private Button switchBtn(boolean on) {
        Button b = new Button(requireContext());
        b.setText(on ? "ON" : "OFF");
        b.setTextColor(on ? Color.WHITE : MUTED);
        b.setTextSize(11);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setAllCaps(true);
        b.setMinWidth(dp(56));
        b.setPadding(dp(14), dp(6), dp(14), dp(6));
        GradientDrawable g = new GradientDrawable();
        g.setColor(on ? ACCENT : 0xFF1A2230);
        g.setCornerRadius(dp(20));
        if (on) g.setStroke(0, 0);
        else g.setStroke(dp(1), 0xFF2A3545);
        b.setBackground(g);
        return b;
    }

    private File getGameDir() {
        try {
            if (Tools.isValidString(mSelectedKey)) {
                LauncherProfiles.load();
                MinecraftProfile p = LauncherProfiles.mainProfileJson.profiles.get(mSelectedKey);
                if (p != null) return Tools.getGameDirPath(p);
            }
            String key = LauncherPreferences.DEFAULT_PREF.getString(
                    LauncherPreferences.PREF_KEY_CURRENT_PROFILE, null);
            if (Tools.isValidString(key)) {
                LauncherProfiles.load();
                MinecraftProfile p = LauncherProfiles.mainProfileJson.profiles.get(key);
                if (p != null) return Tools.getGameDirPath(p);
            }
        } catch (Exception ignored) {}
        return new File(Tools.DIR_GAME_NEW);
    }

    private LinearLayout row() {
        LinearLayout l = new LinearLayout(requireContext());
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    private Button chip(String text, int bgColor, int textColor) {
        Button b = new Button(requireContext());
        b.setText(text);
        b.setTextColor(textColor);
        b.setTextSize(13);
        b.setAllCaps(false);
        b.setPadding(dp(14), dp(6), dp(14), dp(6));
        GradientDrawable g = new GradientDrawable();
        g.setColor(bgColor);
        g.setCornerRadius(dp(20));
        b.setBackground(g);
        return b;
    }

    private void space(LinearLayout parent, int d) {
        View v = new View(requireContext());
        v.setLayoutParams(new LinearLayout.LayoutParams(1, dp(d)));
        parent.addView(v);
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
    }
