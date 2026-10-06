package net.kdt.pojavlaunch;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Spinner;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import net.kdt.pojavlaunch.extra.ExtraConstants;
import net.kdt.pojavlaunch.extra.ExtraCore;
import net.kdt.pojavlaunch.fragments.MainMenuFragment;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;
import net.kdt.pojavlaunch.rkb.ui.RkbHomeFragment;

public class LauncherActivity extends AppCompatActivity {

    private Spinner mAccountSpinner;
    private ImageButton mSettingsButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launcher);

        // bind stock views (may be null if layout changed)
        try {
            mAccountSpinner = findViewById(R.id.account_spinner);
            mSettingsButton = findViewById(R.id.setting_button);
        } catch (Exception ignored) {}

        // ===== RKB: hide entire stock top bar =====
        hideStockTopChrome();

        // Open RKB Home instead of default MainMenu
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.container_fragment, new RkbHomeFragment())
                    .commit();
        }

        // Keep ExtraCore listeners alive if any
        ExtraCore.addExtraListener(ExtraConstants.LAUNCH_GAME, (key, value) -> {
            // default launch path already handled by Pojav core
        });
    }

    private void hideStockTopChrome() {
        try {
            // hide spinner + settings
            if (mAccountSpinner != null) {
                mAccountSpinner.setVisibility(View.GONE);
                View parent = (View) mAccountSpinner.getParent();
                if (parent != null) {
                    parent.setVisibility(View.GONE);
                    if (parent.getParent() instanceof View) {
                        ((View) parent.getParent()).setVisibility(View.GONE);
                    }
                }
            }
            if (mSettingsButton != null) {
                mSettingsButton.setVisibility(View.GONE);
            }

            // also try by resource name (safer)
            int accId = getResources().getIdentifier("account_spinner", "id", getPackageName());
            int setId = getResources().getIdentifier("setting_button", "id", getPackageName());
            int topBarId = getResources().getIdentifier("top_bar", "id", getPackageName());
            int toolbarId = getResources().getIdentifier("toolbar", "id", getPackageName());

            hideById(accId);
            hideById(setId);
            hideById(topBarId);
            hideById(toolbarId);

            // hide any view that contains "Add account"
            View root = findViewById(android.R.id.content);
            if (root != null) {
                hideViewsWithText(root, "Add account");
            }
        } catch (Exception ignored) {}
    }

    private void hideById(int id) {
        if (id == 0) return;
        View v = findViewById(id);
        if (v != null) {
            v.setVisibility(View.GONE);
            if (v.getParent() instanceof View) {
                ((View) v.getParent()).setVisibility(View.GONE);
            }
        }
    }

    private void hideViewsWithText(View root, String text) {
        if (root instanceof android.view.ViewGroup) {
            android.view.ViewGroup group = (android.view.ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                View child = group.getChildAt(i);
                if (child instanceof android.widget.TextView) {
                    CharSequence t = ((android.widget.TextView) child).getText();
                    if (t != null && t.toString().toLowerCase().contains(text.toLowerCase())) {
                        child.setVisibility(View.GONE);
                        if (child.getParent() instanceof View) {
                            ((View) child.getParent()).setVisibility(View.GONE);
                        }
                    }
                }
                hideViewsWithText(child, text);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // re-hide in case layout re-inflates
        hideStockTopChrome();
    }

    // Helper used by other Pojav fragments
    public void swapFragment(Class<? extends Fragment> fragmentClass) {
        try {
            Fragment f = fragmentClass.newInstance();
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.container_fragment, f)
                    .addToBackStack(null)
                    .commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}