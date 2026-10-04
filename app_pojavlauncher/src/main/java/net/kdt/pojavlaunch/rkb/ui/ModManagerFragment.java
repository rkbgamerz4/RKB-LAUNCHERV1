package net.kdt.pojavlaunch.rkb.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
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

/**
 * RKB Mod Manager — list mods for current profile, enable/disable (move to mods/disabled).
 */
public class ModManagerFragment extends Fragment {
    public static final String TAG = "RKB_MOD_MANAGER";

    private LinearLayout mList;
    private TextView mHeader;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        ScrollView scroll = new ScrollView(requireContext());
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(0xFF0D1117);

        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(28, 28, 28, 28);
        scroll.addView(root);

        // Title bar
        LinearLayout titleRow = new LinearLayout(requireContext());
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);

        Button back = new Button(requireContext());
        back.setText("← Back");
        back.setOnClickListener(v -> requireActivity().onBackPressed());
        titleRow.addView(back);

        TextView title = new TextView(requireContext());
        title.setText("  Mod Manager");
        title.setTextColor(0xFF00B4FF);
        title.setTextSize(20);
        titleRow.addView(title);
        root.addView(titleRow);

        mHeader = new TextView(requireContext());
        mHeader.setTextColor(0xFFAAAAAA);
        mHeader.setTextSize(13);
        mHeader.setPadding(0, 16, 0, 16);
        root.addView(mHeader);

        mList = new LinearLayout(requireContext());
        mList.setOrientation(LinearLayout.VERTICAL);
        root.addView(mList);

        Button refresh = new Button(requireContext());
        refresh.setText("Refresh");
        refresh.setOnClickListener(v -> reload());
        root.addView(refresh);

        Button openFolder = new Button(requireContext());
        openFolder.setText("Open mods folder");
        openFolder.setOnClickListener(v -> {
            File mods = ModManager.getModsDir(getGameDir());
            // ensure exists
            //noinspection ResultOfMethodCallIgnored
            mods.mkdirs();
            Tools.openPath(requireContext(), mods, false);
        });
        root.addView(openFolder);

        reload();
        return scroll;
    }

    private File getGameDir() {
        try {
            String currentProfile = LauncherPreferences.DEFAULT_PREF.getString(
                    LauncherPreferences.PREF_KEY_CURRENT_PROFILE, null);
            if (Tools.isValidString(currentProfile)) {
                LauncherProfiles.load();
                MinecraftProfile p = LauncherProfiles.mainProfileJson.profiles.get(currentProfile);
                if (p != null) return Tools.getGameDirPath(p);
            }
        } catch (Exception ignored) {}
        return new File(Tools.DIR_GAME_NEW);
    }

    private void reload() {
        mList.removeAllViews();
        File gameDir = getGameDir();
        List<ModInfo> mods = ModManager.listMods(gameDir);

        mHeader.setText("Game dir: " + gameDir.getAbsolutePath()
                + "\nMods: " + mods.size()
                + "  (tap ON/OFF to enable/disable — not delete)");

        if (mods.isEmpty()) {
            TextView empty = new TextView(requireContext());
            empty.setTextColor(0xFF888888);
            empty.setText("No mods found.\nPut .jar files in the mods folder.");
            empty.setPadding(0, 24, 0, 24);
            mList.addView(empty);
            return;
        }

        for (ModInfo mod : mods) {
            LinearLayout row = new LinearLayout(requireContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(8, 12, 8, 12);
            row.setBackgroundColor(mod.enabled ? 0xFF161B22 : 0xFF1A1A1A);

            TextView name = new TextView(requireContext());
            name.setText(mod.displayName);
            name.setTextColor(Color.WHITE);
            name.setTextSize(14);
            name.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            row.addView(name);

            Button toggle = new Button(requireContext());
            toggle.setText(mod.enabled ? "ON" : "OFF");
            toggle.setTextColor(mod.enabled ? 0xFF00FF88 : 0xFFFF6666);
            final ModInfo modRef = mod;
            toggle.setOnClickListener(v -> {
                boolean ok;
                if (modRef.enabled) {
                    ok = ModManager.disableMod(gameDir, modRef);
                } else {
                    ok = ModManager.enableMod(gameDir, modRef);
                }
                Toast.makeText(requireContext(),
                        ok ? (modRef.enabled ? "Disabled" : "Enabled") + ": " + modRef.displayName
                           : "Failed",
                        Toast.LENGTH_SHORT).show();
                reload();
            });
            row.addView(toggle);

            mList.addView(row);
        }
    }
}
