package net.kdt.pojavlaunch.rkb.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.rkb.mods.ModInfo;
import net.kdt.pojavlaunch.rkb.mods.ModManager;
import net.kdt.pojavlaunch.value.launcherprofiles.MinecraftProfile;

import java.io.File;
import java.util.List;

/**
 * RKB Launcher — Mod Manager UI stub.
 *
 * Future: full list with checkboxes, search, enable-all / disable-all,
 * delete, open mods folder. Uses ModManager for enable/disable without deletion.
 *
 * This fragment is intentionally lightweight so it can be wired into
 * the main navigation when the RKB Home UI is completed.
 */
public class ModManagerFragment extends Fragment {
    public static final String TAG = "RKB_MOD_MANAGER";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Minimal programmatic UI until dedicated layout XML is added
        TextView tv = new TextView(requireContext());
        tv.setTextColor(0xFFFFFFFF);
        tv.setPadding(32, 32, 32, 32);
        tv.setText(buildSummary());
        return tv;
    }

    private String buildSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("RKB Mod Manager\n\n");
        try {
            // Best-effort: use default game dir; real UI will take selected profile/instance
            File gameDir = new File(Tools.DIR_GAME_NEW);
            List<ModInfo> mods = ModManager.listMods(gameDir);
            if (mods.isEmpty()) {
                sb.append("No mods found in:\n").append(ModManager.getModsDir(gameDir));
            } else {
                sb.append("Installed mods (").append(mods.size()).append("):\n\n");
                for (ModInfo m : mods) {
                    sb.append(m.toString()).append("\n");
                }
            }
            sb.append("\n\nToggle enable/disable via ModManager API.\n");
            sb.append("Disabled mods are kept under mods/disabled/.");
        } catch (Exception e) {
            sb.append("Error scanning mods: ").append(e.getMessage());
        }
        return sb.toString();
    }
}
