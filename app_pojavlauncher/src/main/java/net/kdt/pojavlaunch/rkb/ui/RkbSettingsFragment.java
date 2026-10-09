package net.kdt.pojavlaunch.rkb.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.prefs.screens.LauncherPreferenceFragment;

/**
 * Hosts the EXISTING settings screens (LauncherPreferenceFragment and its Video / Controls / Java /
 * Misc / Experimental sub-screens) inside the RKB shell. No preference is added, removed or renamed.
 */
public class RkbSettingsFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        FrameLayout host = new FrameLayout(requireContext());
        host.setId(R.id.rkb_settings_container);
        host.setBackground(RkbUi.rounded(requireContext(), RkbUi.PANEL, 14, RkbUi.BORDER, 1));
        host.setClipToOutline(false);
        if (getChildFragmentManager().findFragmentById(R.id.rkb_settings_container) == null) {
            getChildFragmentManager().beginTransaction()
                    .setReorderingAllowed(true)
                    .replace(R.id.rkb_settings_container, new LauncherPreferenceFragment())
                    .commit();
        }
        return RkbUi.scaffold(requireContext(), RkbNav.Dest.SETTINGS, "Settings",
                "Customize your launcher preferences.", host);
    }
}
