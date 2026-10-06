package net.kdt.pojavlaunch;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import net.kdt.pojavlaunch.extra.ExtraConstants;
import net.kdt.pojavlaunch.extra.ExtraCore;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;
import net.kdt.pojavlaunch.rkb.ui.RkbHomeFragment;

public class LauncherActivity extends AppCompatActivity {

    // Required by Tools.java
    public ActivityResultLauncher<Intent> modInstallerLauncher;

    @Nullable private Spinner mAccountSpinner;
    @Nullable private ImageButton mSettingsButton;

    private Runnable mNotificationPermissionCallback;

    private final ActivityResultLauncher<String> mNotificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (mNotificationPermissionCallback != null) {
                    mNotificationPermissionCallback.run();
                    mNotificationPermissionCallback = null;
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Layout – try common names
        int layoutId = getResources().getIdentifier("activity_launcher", "layout", getPackageName());
        if (layoutId == 0) layoutId = getResources().getIdentifier("activity_main", "layout", getPackageName());
        if (layoutId == 0) layoutId = getResources().getIdentifier("launcher_activity", "layout", getPackageName());
        if (layoutId != 0) {
            setContentView(layoutId);
        } else {
            // Absolute last resort – create empty container
            android.widget.FrameLayout root = new android.widget.FrameLayout(this);
            root.setId(android.view.View.generateViewId());
            root.setLayoutParams(new android.widget.FrameLayout.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT));
            setContentView(root);
            // store generated id for fragment later
            root.setTag("rkb_root");
        }

        // Mod installer launcher (required by Tools.java)
        modInstallerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    // handled by Tools / mod installer flow
                });

        bindViews();
        hideStockTopChrome();

        if (savedInstanceState == null) {
            openRkbHome();
        }
    }

    private void bindViews() {
        try {
            int accId = getResources().getIdentifier("account_spinner", "id", getPackageName());
            int setId = getResources().getIdentifier("setting_button", "id", getPackageName());
            if (setId == 0) setId = getResources().getIdentifier("settings_button", "id", getPackageName());
            if (accId != 0) mAccountSpinner = findViewById(accId);
            if (setId != 0) mSettingsButton = findViewById(setId);
        } catch (Exception ignored) {}
    }

    private void openRkbHome() {
        int containerId = getResources().getIdentifier("container_fragment", "id", getPackageName());
        if (containerId == 0) containerId = getResources().getIdentifier("main_fragment", "id", getPackageName());
        if (containerId == 0) containerId = getResources().getIdentifier("fragment_container", "id", getPackageName());
        if (containerId == 0) containerId = getResources().getIdentifier("content_frame", "id", getPackageName());

        if (containerId != 0) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(containerId, new RkbHomeFragment())
                    .commitAllowingStateLoss();
            return;
        }

        // Fallback: use android.R.id.content
        try {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(android.R.id.content, new RkbHomeFragment())
                    .commitAllowingStateLoss();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "RKB Home failed to open", Toast.LENGTH_LONG).show();
        }
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

            String[] names = {
                    "account_spinner", "setting_button", "settings_button",
                    "top_bar", "toolbar", "account_layout", "account_bar", "header_layout"
            };
            for (String name : names) {
                int id = getResources().getIdentifier(name, "id", getPackageName());
                if (id == 0) continue;
                View v = findViewById(id);
                if (v != null) {
                    v.setVisibility(View.GONE);
                    hideParent(v);
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

    // ===== Required by LauncherPreferenceFragment =====
    public boolean checkForNotificationPermission() {
        if (Build.VERSION.SDK_INT < 33) return true;
        return ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    }

    public void askForNotificationPermission(@Nullable Runnable onGranted) {
        if (Build.VERSION.SDK_INT < 33) {
            if (onGranted != null) onGranted.run();
            return;
        }
        if (checkForNotificationPermission()) {
            if (onGranted != null) onGranted.run();
            return;
        }
        mNotificationPermissionCallback = onGranted;
        mNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideStockTopChrome();
    }

    /** Used by other Pojav fragments */
    public void swapFragment(Class<? extends Fragment> clazz) {
        try {
            Fragment f = clazz.getDeclaredConstructor().newInstance();
            int containerId = getResources().getIdentifier("container_fragment", "id", getPackageName());
            if (containerId == 0) containerId = getResources().getIdentifier("main_fragment", "id", getPackageName());
            if (containerId == 0) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(android.R.id.content, f)
                        .addToBackStack(null)
                        .commitAllowingStateLoss();
            } else {
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