package net.kdt.pojavlaunch.rkb.ui;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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

public class ModManagerFragment extends Fragment {
    public static final String TAG = "RKB_MOD_MANAGER";

    private static final int BG      = 0xFF0A0E14;
    private static final int CARD    = 0xFF121820;
    private static final int ACCENT  = 0xFF00B4FF;
    private static final int GREEN   = 0xFF00E676;
    private static final int RED     = 0xFFFF5252;
    private static final int TEXT    = 0xFFE8EEF5;
    private static final int MUTED   = 0xFF8B9BB4;

    private LinearLayout mList;
    private TextView mHeader;

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
        root.setPadding(dp(16), dp(16), dp(16), dp(24));
        scroll.addView(root);

        // Top bar
        LinearLayout top = row();
        top.setPadding(0, 0, 0, dp(12));

        Button back = pillButton("← Back", 0xFF1A2332, TEXT);
        back.setOnClickListener(v -> requireActivity().onBackPressed());
        top.addView(back);

        TextView title = new TextView(requireContext());
        title.setText("  Mod Manager");
        title.setTextColor(ACCENT);
        title.setTextSize(20);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER_VERTICAL);
        top.addView(title);
        root.addView(top);

        // Header card
        LinearLayout headerCard = card();
        mHeader = new TextView(requireContext());
        mHeader.setTextColor(MUTED);
        mHeader.setTextSize(13);
        mHeader.setLineSpacing(0, 1.2f);
        headerCard.addView(mHeader);
        root.addView(headerCard);

        space(root, 12);

        mList = new LinearLayout(requireContext());
        mList.setOrientation(LinearLayout.VERTICAL);
        root.addView(mList);

        space(root, 16);

        LinearLayout actions = row();
        Button refresh = pillButton("Refresh", 0xFF1A2332, TEXT);
        refresh.setOnClickListener(v -> reload());
        LinearLayout.LayoutParams lp1 = new LinearLayout.LayoutParams(0, dp(44), 1f);
        lp1.setMarginEnd(dp(8));
        refresh.setLayoutParams(lp1);
        actions.addView(refresh);

        Button openFolder = pillButton("Open folder", ACCENT, Color.WHITE);
        openFolder.setOnClickListener(v -> {
            File mods = ModManager.getModsDir(getGameDir());
            //noinspection ResultOfMethodCallIgnored
            mods.mkdirs();
            Tools.openPath(requireContext(), mods, false);
        });
        LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(0, dp(44), 1f);
        openFolder.setLayoutParams(lp2);
        actions.addView(openFolder);
        root.addView(actions);

        reload();
        return scroll;
    }

    private void reload() {
        mList.removeAllViews();
        File gameDir = getGameDir();
        List<ModInfo> mods = ModManager.listMods(gameDir);

        int on = 0;
        for (ModInfo m : mods) if (m.enabled) on++;

        mHeader.setText("Instance mods folder\n"
                + gameDir.getName()
                + "\n\n"
                + mods.size() + " mods  ·  " + on + " enabled  ·  " + (mods.size() - on) + " disabled\n"
                + "Tap ON/OFF to toggle (does not delete)");

        if (mods.isEmpty()) {
            TextView empty = new TextView(requireContext());
            empty.setTextColor(MUTED);
            empty.setTextSize(14);
            empty.setPadding(0, dp(24), 0, dp(24));
            empty.setText("No mods found.\nPut .jar files in the mods folder.");
            mList.addView(empty);
            return;
        }

        for (ModInfo mod : mods) {
            mList.addView(modRow(gameDir, mod));
            space(mList, 8);
        }
    }

    private View modRow(File gameDir, ModInfo mod) {
        LinearLayout row = card();
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout info = new LinearLayout(requireContext());
        info.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams infoLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        info.setLayoutParams(infoLp);

        TextView name = new TextView(requireContext());
        name.setText(mod.displayName);
        name.setTextColor(TEXT);
        name.setTextSize(14);
        name.setTypeface(Typeface.DEFAULT_BOLD);
        name.setMaxLines(2);
        info.addView(name);

        TextView status = new TextView(requireContext());
        status.setText(mod.enabled ? "Enabled" : "Disabled");
        status.setTextColor(mod.enabled ? GREEN : RED);
        status.setTextSize(12);
        info.addView(status);
        row.addView(info);

        Button toggle = pillButton(mod.enabled ? "ON" : "OFF",
                mod.enabled ? 0xFF0D2818 : 0xFF2A1515,
                mod.enabled ? GREEN : RED);
        toggle.setMinWidth(dp(64));
        final ModInfo modRef = mod;
        toggle.setOnClickListener(v -> {
            boolean ok = modRef.enabled
                    ? ModManager.disableMod(gameDir, modRef)
                    : ModManager.enableMod(gameDir, modRef);
            Toast.makeText(requireContext(),
                    ok ? ((modRef.enabled ? "Disabled" : "Enabled") + ": " + modRef.displayName) : "Failed",
                    Toast.LENGTH_SHORT).show();
            reload();
        });
        row.addView(toggle);
        return row;
    }

    private File getGameDir() {
        try {
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

    private LinearLayout card() {
        LinearLayout c = new LinearLayout(requireContext());
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(14), dp(12), dp(14), dp(12));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(CARD);
        bg.setCornerRadius(dp(12));
        c.setBackground(bg);
        return c;
    }

    private Button pillButton(String text, int bgColor, int textColor) {
        Button b = new Button(requireContext());
        b.setText(text);
        b.setTextColor(textColor);
        b.setTextSize(13);
        b.setAllCaps(false);
        b.setPadding(dp(16), dp(8), dp(16), dp(8));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(bgColor);
        bg.setCornerRadius(dp(22));
        b.setBackground(bg);
        return b;
    }

    private void space(LinearLayout parent, int dp) {
        View v = new View(requireContext());
        v.setLayoutParams(new LinearLayout.LayoutParams(1, dp(dp)));
        parent.addView(v);
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

}