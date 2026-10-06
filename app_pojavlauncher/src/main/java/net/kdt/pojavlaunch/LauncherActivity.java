package net.kdt.pojavlaunch;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Spinner;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import net.kdt.pojavlaunch.extra.ExtraConstants;
import net.kdt.pojavlaunch.extra.ExtraCore;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;
import net.kdt.pojavlaunch.rkb.ui.RkbHomeFragment;

public class LauncherActivity extends AppCompatActivity {

    @Nullable
    private Spinner mAccountSpinner;
    @Nullable
    private ImageButton mSettingsButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Prefer activity_launcher, fallback if needed
        int layoutId = getResources().getIdentifier("activity_launcher", "layout", getPackageName());
        if (layoutId == 0) {
            layoutId = getResources().getIdentifier("activity_main", "layout", getPackageName());
        }
        if (layoutId != 0) {
            setContentView(layoutId);
        } else {
            setContentView(R.layout.activity_launcher);
        }

        bindViews();
        hideStockTopChrome();

        // Open RKB custom home
        if (savedInstanceState == null) {
            openRkbHome();
        }
    }

    private void bindViews() {
        try {
            int accId = getResources().getIdentifier("account_spinner", "id", getPackageName());
            int setId = getResources().getIdentifier("setting_button", "id", getPackageName());
            if (accId != 0) mAccountSpinner = findViewById(accId);
            if (setId != 0) mSettingsButton = findViewById(setId);
        } catch (Exception ignored) {}
    }

    private void openRkbHome() {
        int containerId = getResources().getIdentifier("container_fragment", "id", getPackageName());
        if (containerId == 0) {
            containerId = getResources().getIdentifier("main_fragment", "id", getPackageName());
        }
        if (containerId == 0) {
            containerId = getResources().getIdentifier("fragment_container", "id", getPackageName());
        }
        if (containerId == 0) {
            // last resort – common Pojav id
            try {
                containerId = R.id.container_fragment;
            } catch (Exception e) {
                return;
            }
        }

        getSupportFragmentManager()
                .beginTransaction()
                .replace(containerId, new RkbHomeFragment())
                .commitAllowingStateLoss();
    }

    private void hideStockTopChrome() {
        try {
            if (mAccountSpinner != null) {
                mAccountSpinner.setVisibility(View.GONE);
                hideParent(mAccountSpinner);
            }
            if (mSettingsButton != null) {
                mSettingsButton.setVisibility(View.GONE);
                hideParent(mSettingsButton);
            }

            // hide by common resource names
            String[] names = {
                    "account_spinner",
                    "setting_button",
                    "settings_button",
                    "top_bar",
                    "toolbar",
                    "account_layout",
                    "account_bar",
                    "header_layout"
            };
            for (String name : names) {
                int id = getResources().getIdentifier(name, "id", getPackageName());
                if (id != 0) {
                    View v = findViewById(id);
                    if (v != null) {
                        v.setVisibility(View.GONE);
                        hideParent(v);
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    private void hideParent(View v) {
        try {
            if (v.getParent() instanceof View) {
                View p = (View) v.getParent();
                p.setVisibility(View.GONE);
                if (p.getParent() instanceof View) {
                    ((View) p.getParent()).setVisibility(View.GONE);
                }
            }
        } catch (Exception ignored) {}
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideStockTopChrome();
    }

    /** Used by other Pojav code / fragments */
    public void swapFragment(Class<? extends Fragment> clazz) {
        try {
            Fragment f = clazz.getDeclaredConstructor().newInstance();
            int containerId = getResources().getIdentifier("container_fragment", "id", getPackageName());
            if (containerId == 0) {
                try { containerId = R.id.container_fragment; } catch (Exception ignored) {}
            }
            if (containerId != 0) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(containerId, f)
                        .addToBackStack(null)
                        .commitAllowingStateLoss();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }
}