package net.kdt.pojavlaunch.rkb.ui;

import android.graphics.Color;
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

/** Hosts the existing launcher preferences inside the RKB shell without changing any option. */
public class RkbSettingsFragment extends Fragment {

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        FrameLayout host = new FrameLayout(requireContext());
        host.setId(R.id.rkb_settings_container);
        if (getChildFragmentManager().findFragmentById(R.id.rkb_settings_container) == null) {
            getChildFragmentManager().beginTransaction()
                    .setReorderingAllowed(true)
                    .replace(R.id.rkb_settings_container, new TransparentPreferenceFragment())
                    .commit();
        }
        return RkbUi.scaffold(requireContext(), RkbNav.Dest.SETTINGS, "Settings",
                "Customize your launcher preferences.", host);
    }

    /** Keeps every real preference from pref_main.xml, but lets the RKB background show through. */
    public static class TransparentPreferenceFragment extends LauncherPreferenceFragment {
        @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            view.setBackgroundColor(Color.TRANSPARENT);
        }
    }
}
