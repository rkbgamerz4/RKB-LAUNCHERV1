package net.kdt.pojavlaunch;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.kdt.pojavlaunch.extra.ExtraConstants;
import net.kdt.pojavlaunch.extra.ExtraCore;
import net.kdt.pojavlaunch.extra.ExtraListener;
import net.kdt.pojavlaunch.authenticator.microsoft.MicrosoftBackgroundLogin;
import net.kdt.pojavlaunch.value.MinecraftAccount;
import net.kdt.pojavlaunch.rkb.ui.RkbHomeFragment;
import net.kdt.pojavlaunch.rkb.ui.RkbSkinFragment;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.kdt.pojavlaunch.fragments.ProfileEditorFragment;
import net.kdt.pojavlaunch.tasks.AsyncMinecraftDownloader.DoneListener;
import net.kdt.pojavlaunch.lifecycle.ContextAwareDoneListener;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;
import net.kdt.pojavlaunch.progresskeeper.ProgressKeeper;
import net.kdt.pojavlaunch.rkb.ui.RkbNav;
import net.kdt.pojavlaunch.rkb.ui.RkbUi;
import net.kdt.pojavlaunch.services.ProgressServiceKeeper;
import net.kdt.pojavlaunch.tasks.AsyncMinecraftDownloader;
import net.kdt.pojavlaunch.tasks.AsyncVersionList;
import net.kdt.pojavlaunch.tasks.MinecraftDownloader;
import net.kdt.pojavlaunch.value.launcherprofiles.LauncherProfiles;
import net.kdt.pojavlaunch.value.launcherprofiles.MinecraftProfile;

import java.util.Map;

/**
 * RKB launcher host activity.
 *
 * Navigation model: Home is the root fragment (no back-stack entry). Every other screen is one
 * back-stack entry on top of Home, so Back always returns to Home and sidebar clicks never stack
 * duplicates. Fragment tags come from {@link RkbNav.Dest#tag}.
 */
public class LauncherActivity extends AppCompatActivity {

    // Required by Tools.java
    public ActivityResultLauncher<Intent> modInstallerLauncher;

    private Runnable mNotificationPermissionCallback;
    private ProgressServiceKeeper mProgressServiceKeeper;
    private long mLastBackPress;
    private boolean mLaunching;          // blocks accidental double launches
    private final ExecutorService mIo = Executors.newSingleThreadExecutor();
    private boolean mLoginRunning;
    private LinearLayout mLaunchOverlay;
    private TextView mLaunchOverlayText;

    private final ActivityResultLauncher<String> mNotificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (mNotificationPermissionCallback != null) {
                    mNotificationPermissionCallback.run();
                    mNotificationPermissionCallback = null;
                }
            });

    /**
     * ExtraCore keeps only weak references to listeners, so this MUST stay a field.
     * Nothing in the project registered a LAUNCH_GAME listener before, which is why the Launch
     * button had no effect.
     */
    private final ExtraListener<Boolean> mLaunchGameListener = (key, value) -> {
        runOnUiThread(this::startLaunchFlow);
        return false;
    };

    /**
     * Keeps the "current profile" preference in sync after the profile editor / installers change
     * profiles. This used to be done by the removed version spinner; without it a newly created
     * instance was never selected and MainActivity could crash with "current profile stopped existing".
     */
    private final ExtraListener<String> mRefreshProfileListener = (key, value) -> {
        runOnUiThread(() -> selectProfileAfterChange(value));
        return false;
    };

    /**
     * Local/offline profile requested by LocalLoginFragment. Nothing consumed this request before,
     * and the fragment opened the stock PojavLauncher main menu instead (the "default Home" bug).
     */
    private final ExtraListener<String[]> mLocalLoginListener = (key, value) -> {
        if (value != null && value.length > 0) runOnUiThread(() -> createLocalAccount(value[0]));
        return false;
    };

    /** Microsoft redirect captured by MicrosoftLoginFragment (same missing-consumer problem). */
    private final ExtraListener<android.net.Uri> mMicrosoftLoginListener = (key, value) -> {
        if (value != null) runOnUiThread(() -> runMicrosoftLogin(value));
        return false;
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // The id R.id.container_fragment is what Tools.swapFragment() (login, profile editor,
        // mod install fragments) replaces into. It used to be missing from the screen.
        FrameLayout root = new FrameLayout(this);
        root.setId(R.id.container_fragment);
        root.setBackgroundColor(RkbUi.BG);
        root.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        buildLaunchOverlay(root);
        setContentView(root);

        modInstallerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> { /* handled by Tools / mod installer flow */ });

        mProgressServiceKeeper = new ProgressServiceKeeper(this);
        ProgressKeeper.addTaskCountListener(mProgressServiceKeeper);

        ExtraCore.addExtraListener(ExtraConstants.LAUNCH_GAME, mLaunchGameListener);
        ExtraCore.addExtraListener(ExtraConstants.REFRESH_VERSION_SPINNER, mRefreshProfileListener);
        ExtraCore.addExtraListener(ExtraConstants.MOJANG_LOGIN_TODO, mLocalLoginListener);
        ExtraCore.addExtraListener(ExtraConstants.MICROSOFT_LOGIN_TODO, mMicrosoftLoginListener);
        refreshVersionList();
        installBackHandler();

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .setReorderingAllowed(true)
                    .replace(R.id.container_fragment, RkbNav.create(RkbNav.Dest.HOME), RkbNav.Dest.HOME.tag)
                    .commit();
        }
    }

    // ------------------------------------------------------------------ navigation

    /** Opens a sidebar destination. Safe to call repeatedly and from any click handler. */
    public void navigate(RkbNav.Dest dest) {
        if (isFinishing() || isDestroyed()) return;

        if (dest == RkbNav.Dest.CONTROLS) {
            // Existing controls editor; it is a separate Activity in this project.
            startActivity(new Intent(this, CustomControlsActivity.class));
            return;
        }

        FragmentManager fm = getSupportFragmentManager();
        if (fm.isStateSaved()) return; // transaction would be invalid right now

        Fragment current = fm.findFragmentById(R.id.container_fragment);
        if (current != null && dest.tag.equals(current.getTag())) return; // already here

        // Drop everything above Home (secondary screens, login flows, ...).
        fm.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        if (dest == RkbNav.Dest.HOME) {
            Fragment now = fm.findFragmentById(R.id.container_fragment);
            if (now == null || !RkbNav.Dest.HOME.tag.equals(now.getTag())) {
                fm.beginTransaction().setReorderingAllowed(true)
                        .replace(R.id.container_fragment, RkbNav.create(dest), dest.tag).commit();
            }
            return;
        }
        Fragment target = RkbNav.create(dest);
        fm.beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.container_fragment, target, dest.tag)
                .setPrimaryNavigationFragment(target) // lets nested screens (Settings) handle Back first
                .addToBackStack(dest.tag)
                .commit();
    }

    /** Used by older Pojav fragments. */
    public void swapFragment(Class<? extends Fragment> clazz) {
        if (isFinishing() || isDestroyed()) return;
        FragmentManager fm = getSupportFragmentManager();
        if (fm.isStateSaved()) return;
        fm.beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.container_fragment, clazz, null, clazz.getName())
                .addToBackStack(clazz.getName())
                .commit();
    }

    private void installBackHandler() {
        final FragmentManager fm = getSupportFragmentManager();
        // Only active on Home (empty back stack). With a screen on top, the FragmentManager's own
        // back handling pops back to Home instead of leaving the app.
        final OnBackPressedCallback exitGuard = new OnBackPressedCallback(fm.getBackStackEntryCount() == 0) {
            @Override
            public void handleOnBackPressed() {
                long now = SystemClock.elapsedRealtime();
                if (now - mLastBackPress < 2000) {
                    setEnabled(false);
                    finish();
                } else {
                    mLastBackPress = now;
                    Toast.makeText(LauncherActivity.this, "Press back again to exit", Toast.LENGTH_SHORT).show();
                }
            }
        };
        getOnBackPressedDispatcher().addCallback(this, exitGuard);
        fm.addOnBackStackChangedListener(() -> exitGuard.setEnabled(fm.getBackStackEntryCount() == 0));
    }

    // ------------------------------------------------------------------ launching

    private void refreshVersionList() {
        // Runs on AsyncVersionList's own executor. Falls back to the cached list when offline.
        new AsyncVersionList().getVersionList(versions -> {
            if (versions != null) ExtraCore.setValue(ExtraConstants.RELEASE_TABLE, versions);
        }, false);
    }

    private void startLaunchFlow() {
        if (isFinishing() || isDestroyed()) return;
        if (mLaunching) return; // already launching: ignore repeated taps

        if (ProgressKeeper.hasOngoingTasks()) {
            Toast.makeText(this, R.string.tasks_ongoing, Toast.LENGTH_LONG).show();
            return;
        }
        // No account: do not bypass authentication, send the player to Skin & Account to sign in.
        if (RkbUi.currentAccount(this) == null) {
            Toast.makeText(this, "Select or add an account first", Toast.LENGTH_LONG).show();
            navigate(RkbNav.Dest.SKIN);
            return;
        }

        MinecraftAccount account = RkbUi.currentAccount(this);
        if (account.isMicrosoft && account.expiresAt > 0 && account.expiresAt < System.currentTimeMillis()) {
            refreshExpiredSessionThenLaunch(account);
            return;
        }

        MinecraftProfile profile = resolveSelectedProfile(); // also persists the selection
        if (profile == null || !Tools.isValidString(profile.lastVersionId)) {
            Toast.makeText(this, "No usable instance. Create one with New Instance first.", Toast.LENGTH_LONG).show();
            return;
        }

        final String versionId = AsyncMinecraftDownloader.normalizeVersionId(profile.lastVersionId);
        JMinecraftVersionList.Version listed = AsyncMinecraftDownloader.getListedVersion(versionId);
        setLaunching(true, "Preparing Minecraft " + versionId + "...");
        final ContextAwareDoneListener inner = new ContextAwareDoneListener(this, versionId);
        try {
            new MinecraftDownloader().start(this, listed, versionId, new DoneListener() {
                @Override public void onDownloadDone() { inner.onDownloadDone(); }
                @Override public void onDownloadFailed(Throwable t) {
                    runOnUiThread(() -> setLaunching(false, null)); // restore the UI after a failure
                    inner.onDownloadFailed(t);
                }
            });
        } catch (RuntimeException e) {
            setLaunching(false, null);
            Tools.showError(this, e);
        }
    }

    // ------------------------------------------------------------------ accounts

    private void createLocalAccount(final String username) {
        final String name = username == null ? "" : username.trim();
        if (name.isEmpty()) return;
        mIo.execute(() -> {
            String error = null;
            try {
                MinecraftAccount acc = new MinecraftAccount(); // defaults = local (token "0", zero UUID)
                acc.username = name;
                acc.save();
            } catch (Exception e) {
                android.util.Log.e("RKB-Login", "Could not save local account", e);
                error = e.getMessage();
            }
            final String err = error;
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                if (err != null) {
                    Toast.makeText(this, "Could not create the profile: " + err, Toast.LENGTH_LONG).show();
                    return;
                }
                PojavProfile.setCurrentProfile(this, name);
                refreshAccountUi();
                Toast.makeText(this, "Signed in as " + name, Toast.LENGTH_SHORT).show();
            });
        });
    }

    private void runMicrosoftLogin(android.net.Uri redirect) {
        final String code = redirect.getQueryParameter("code");
        if (code == null || code.isEmpty()) {
            Toast.makeText(this, "Microsoft sign-in was cancelled or failed", Toast.LENGTH_LONG).show();
            return;
        }
        if (mLoginRunning) return; // duplicate callback
        mLoginRunning = true;
        setLaunching(true, "Signing in...");
        new MicrosoftBackgroundLogin(false, code).performLogin(
                step -> { if (mLaunchOverlayText != null) mLaunchOverlayText.setText("Signing in (" + step + "/5)..."); },
                account -> {
                    mLoginRunning = false;
                    setLaunching(false, null);
                    PojavProfile.setCurrentProfile(this, account.username);
                    refreshAccountUi();
                    Toast.makeText(this, "Signed in as " + account.username, Toast.LENGTH_SHORT).show();
                },
                error -> {
                    mLoginRunning = false;
                    setLaunching(false, null);
                    Tools.showError(this, error);
                });
    }

    /** Microsoft sessions expire; refresh with the stored refresh token instead of launching with a dead token. */
    private void refreshExpiredSessionThenLaunch(MinecraftAccount stale) {
        if (mLoginRunning) return;
        mLoginRunning = true;
        setLaunching(true, "Refreshing your Microsoft session...");
        new MicrosoftBackgroundLogin(true, stale.msaRefreshToken).performLogin(
                null,
                account -> {
                    mLoginRunning = false;
                    setLaunching(false, null);
                    refreshAccountUi();
                    startLaunchFlow();
                },
                error -> {
                    mLoginRunning = false;
                    setLaunching(false, null);
                    Toast.makeText(this, "Your session expired and could not be refreshed (offline?). "
                            + "Connect to the internet or sign in again.", Toast.LENGTH_LONG).show();
                    android.util.Log.w("RKB-Login", "session refresh failed: " + error);
                });
    }

    /** Rebuilds whatever RKB screen is visible so the account name, badge and skin are re-read. */
    public void refreshAccountUi() {
        if (isFinishing() || isDestroyed()) return;
        FragmentManager fm = getSupportFragmentManager();
        if (fm.isStateSaved()) return;
        Fragment cur = fm.findFragmentById(R.id.container_fragment);
        if (cur instanceof RkbHomeFragment) {
            ((RkbHomeFragment) cur).refreshAccount();
        } else if (cur != null && cur.getTag() != null && cur.getTag().startsWith("rkb_")) {
            fm.beginTransaction().setReorderingAllowed(true).detach(cur).commit();
            fm.beginTransaction().setReorderingAllowed(true).attach(cur).commit();
        }
    }

    private void setLaunching(boolean launching, @Nullable String message) {
        mLaunching = launching;
        if (mLaunchOverlay == null) return;
        if (launching) {
            mLaunchOverlayText.setText(message);
            mLaunchOverlay.setVisibility(android.view.View.VISIBLE);
            mLaunchOverlay.bringToFront();
        } else {
            mLaunchOverlay.setVisibility(android.view.View.GONE);
        }
    }

    private void buildLaunchOverlay(FrameLayout root) {
        mLaunchOverlay = new LinearLayout(this);
        mLaunchOverlay.setOrientation(LinearLayout.HORIZONTAL);
        mLaunchOverlay.setGravity(Gravity.CENTER_VERTICAL);
        mLaunchOverlay.setVisibility(android.view.View.GONE);
        mLaunchOverlay.setClickable(true); // swallow touches behind the bar
        int p = RkbUi.dp(this, 12);
        mLaunchOverlay.setPadding(p, p, p, p);
        mLaunchOverlay.setBackground(RkbUi.rounded(this, RkbUi.CARD, 14, RkbUi.BLUE, 1));
        ProgressBar bar = new ProgressBar(this);
        bar.setIndeterminate(true);
        mLaunchOverlay.addView(bar, new LinearLayout.LayoutParams(RkbUi.dp(this, 28), RkbUi.dp(this, 28)));
        mLaunchOverlayText = RkbUi.text(this, "", 13, RkbUi.WHITE, true);
        mLaunchOverlayText.setPadding(RkbUi.dp(this, 10), 0, 0, 0);
        mLaunchOverlay.addView(mLaunchOverlayText);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        lp.bottomMargin = RkbUi.dp(this, 16);
        root.addView(mLaunchOverlay, lp);
    }

    /**
     * Returns the selected profile and makes sure PREF_KEY_CURRENT_PROFILE points at a real one.
     * MainActivity calls LauncherProfiles.getCurrentProfile(), which throws if the key is missing.
     */
    @Nullable
    private MinecraftProfile resolveSelectedProfile() {
        try {
            LauncherProfiles.load();
            Map<String, MinecraftProfile> profiles = LauncherProfiles.mainProfileJson.profiles;
            if (profiles == null || profiles.isEmpty()) return null;
            String key = LauncherPreferences.DEFAULT_PREF.getString(LauncherPreferences.PREF_KEY_CURRENT_PROFILE, null);
            if (key == null || !profiles.containsKey(key)) {
                key = profiles.keySet().iterator().next();
            }
            // commit(): the game runs in another process and must see the value immediately
            LauncherPreferences.DEFAULT_PREF.edit()
                    .putString(LauncherPreferences.PREF_KEY_CURRENT_PROFILE, key).commit();
            return profiles.get(key);
        } catch (Exception e) {
            android.util.Log.e("RKB-Launch", "Could not read selected profile", e);
            return null;
        }
    }

    /** value = key of the saved profile, or ProfileEditorFragment.DELETED_PROFILE. */
    private void selectProfileAfterChange(@Nullable String value) {
        try {
            LauncherProfiles.load();
            Map<String, MinecraftProfile> profiles = LauncherProfiles.mainProfileJson.profiles;
            if (profiles == null || profiles.isEmpty()) return;
            String key = value;
            if (key == null || ProfileEditorFragment.DELETED_PROFILE.equals(key) || !profiles.containsKey(key)) {
                String cur = LauncherPreferences.DEFAULT_PREF.getString(LauncherPreferences.PREF_KEY_CURRENT_PROFILE, null);
                key = (cur != null && profiles.containsKey(cur)) ? cur : profiles.keySet().iterator().next();
            }
            LauncherPreferences.DEFAULT_PREF.edit()
                    .putString(LauncherPreferences.PREF_KEY_CURRENT_PROFILE, key).commit();
        } catch (Exception e) {
            android.util.Log.e("RKB-Profiles", "Could not update the selected instance", e);
        }
    }

    // ------------------------------------------------------------------ permissions

    public boolean checkForNotificationPermission() {
        if (Build.VERSION.SDK_INT < 33) return true;
        return ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    }

    public void askForNotificationPermission(@Nullable Runnable onGranted) {
        if (Build.VERSION.SDK_INT < 33 || checkForNotificationPermission()) {
            if (onGranted != null) onGranted.run();
            return;
        }
        mNotificationPermissionCallback = onGranted;
        mNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
    }

    // ------------------------------------------------------------------ lifecycle

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    @Override
    protected void onDestroy() {
        ExtraCore.removeExtraListenerFromValue(ExtraConstants.LAUNCH_GAME, mLaunchGameListener);
        ExtraCore.removeExtraListenerFromValue(ExtraConstants.REFRESH_VERSION_SPINNER, mRefreshProfileListener);
        ExtraCore.removeExtraListenerFromValue(ExtraConstants.MOJANG_LOGIN_TODO, mLocalLoginListener);
        ExtraCore.removeExtraListenerFromValue(ExtraConstants.MICROSOFT_LOGIN_TODO, mMicrosoftLoginListener);
        mIo.shutdown();
        if (mProgressServiceKeeper != null) ProgressKeeper.removeTaskCountListener(mProgressServiceKeeper);
        super.onDestroy();
    }
}
