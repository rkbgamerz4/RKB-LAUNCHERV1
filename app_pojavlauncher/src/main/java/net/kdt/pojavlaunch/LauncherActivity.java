package net.kdt.pojavlaunch;

import static android.content.res.Configuration.ORIENTATION_PORTRAIT;

import android.Manifest;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.system.Os;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentContainerView;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.kdt.mcgui.ProgressLayout;

import net.kdt.pojavlaunch.authenticator.AuthType;
import net.kdt.pojavlaunch.authenticator.accounts.Account;
import net.kdt.pojavlaunch.authenticator.accounts.Accounts;
import net.kdt.pojavlaunch.authenticator.listener.LoginListener;
import net.kdt.pojavlaunch.extra.ExtraConstants;
import net.kdt.pojavlaunch.extra.ExtraCore;
import net.kdt.pojavlaunch.extra.ExtraListener;
import net.kdt.pojavlaunch.fragments.MicrosoftLoginFragment;
import net.kdt.pojavlaunch.instances.Instance;
import net.kdt.pojavlaunch.instances.InstanceInstaller;
import net.kdt.pojavlaunch.instances.Instances;
import net.kdt.pojavlaunch.lifecycle.ContextAwareDoneListener;
import net.kdt.pojavlaunch.lifecycle.ContextExecutor;
import net.kdt.pojavlaunch.modloaders.modpacks.imagecache.IconCacheJanitor;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;
import net.kdt.pojavlaunch.progresskeeper.ProgressKeeper;
import net.kdt.pojavlaunch.progresskeeper.TaskCountListener;
import net.kdt.pojavlaunch.rkb.ui.RkbNav;
import net.kdt.pojavlaunch.services.ProgressServiceKeeper;
import net.kdt.pojavlaunch.tasks.MoJsonExtras;
import net.kdt.pojavlaunch.tasks.AsyncVersionList;
import net.kdt.pojavlaunch.tasks.MoJsonDownloader;
import net.kdt.pojavlaunch.utils.NotificationUtils;

import java.io.IOException;

/** Host activity for the RKB launcher UI. */
public class LauncherActivity extends BaseActivity {
    public static final String SETTING_FRAGMENT_TAG = "SETTINGS_FRAGMENT";

    private final Handler mMain = new Handler(Looper.getMainLooper());
    private long mLastBackPress;

    private FragmentContainerView mFragmentView;
    private ProgressLayout mProgressLayout;
    private ProgressServiceKeeper mProgressServiceKeeper;
    private NotificationManager mNotificationManager;
    private static ActivityResultLauncher<String> mRequestPermissionLauncher;

    private final ExtraListener<String> mBackPreferenceListener = (key, value) -> {
        if (value.equals("true")) onBackPressed();
        return false;
    };

    /** Launch with no account opens the Account screen, where all three real sign-in methods live. */
    private final ExtraListener<Boolean> mSelectAuthMethod = (key, value) -> {
        if (!value || isFinishing() || getSupportFragmentManager().isStateSaved()) return false;
        navigate(RkbNav.Dest.SKIN);
        return false;
    };

    private final LoginListener mAccountLoginListener = new LoginListener() {
        @Override public void onLoginDone(Account account) {
            if (account != null) Accounts.setCurrent(account);
            Toast.makeText(LauncherActivity.this, R.string.main_login_done, Toast.LENGTH_SHORT).show();
            // OAuthFragment / LocalLoginFragment remove their own fragment first; refresh the
            // account screen afterwards so the new account is shown by the RKB UI.
            mMain.post(() -> {
                if (!isFinishing() && !isDestroyed()) navigate(RkbNav.Dest.SKIN);
            });
        }

        @Override public void onLoginError(Throwable errorMessage) {
            Tools.showErrorRemote(errorMessage);
        }

        @Override public void onLoginProgress(int step) { }

        @Override public void setMaxLoginProgress(int max) { }
    };

    private final ExtraListener<String> mMicrosoftLoginListener = (key, value) -> {
        AuthType.MICROSOFT.createAuth().createAccount(mAccountLoginListener, value);
        return false;
    };

    private final ExtraListener<String> mElyByLoginListener = (key, value) -> {
        AuthType.ELY_BY.createAuth().createAccount(mAccountLoginListener, value);
        return false;
    };

    private final ExtraListener<String[]> mLocalLoginListener = (key, value) -> {
        if (value == null || value.length == 0) return false;
        try {
            mAccountLoginListener.onLoginDone(Accounts.create(acc -> acc.username = value[0]));
        } catch (IOException e) {
            mAccountLoginListener.onLoginError(e);
        }
        return false;
    };

    private final ExtraListener<Boolean> mLaunchGameListener = (key, value) -> {
        if (mProgressLayout.hasProcesses()) {
            Toast.makeText(this, R.string.tasks_ongoing, Toast.LENGTH_LONG).show();
            return false;
        }

        Instance selectedInstance = Instances.loadSelectedInstance();

        if (selectedInstance == null) {
            Toast.makeText(this, R.string.no_instance, Toast.LENGTH_LONG).show();
            return false;
        }

        if (selectedInstance.installer != null) {
            selectedInstance.installer.start();
            return false;
        }

        if (!Tools.isValidString(selectedInstance.versionId)) {
            Toast.makeText(this, R.string.error_no_version, Toast.LENGTH_LONG).show();
            return false;
        }

        if (Accounts.getCurrent() == null) {
            Toast.makeText(this, R.string.no_saved_accounts, Toast.LENGTH_LONG).show();
            ExtraCore.setValue(ExtraConstants.SELECT_AUTH_METHOD, true);
            return false;
        }

        String normalizedVersionId = MoJsonExtras.normalizeVersionId(selectedInstance.versionId);
        JVersionList.Version mcVersion = MoJsonExtras.getListedVersion(normalizedVersionId);
        new MoJsonDownloader().start(
                this.getAssets(),
                mcVersion,
                normalizedVersionId,
                new ContextAwareDoneListener(this, normalizedVersionId)
        );
        return false;
    };

    private final TaskCountListener mDoubleLaunchPreventionListener = taskCount -> {
        if (taskCount > 0) {
            Tools.runOnUiThread(() ->
                    mNotificationManager.cancel(NotificationUtils.NOTIFICATION_ID_GAME_START)
            );
        }
        return false;
    };

    @Override
    protected boolean shouldIgnoreNotch() {
        return getResources().getConfiguration().orientation == ORIENTATION_PORTRAIT;
    }

    @Override
    public boolean setFullscreen() {
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pojav_launcher);
        MoJsonDownloader.prepareSubstitutionMap(getAssets());

        try {
            Os.setenv("TMPDIR", Tools.DIR_CACHE.getAbsolutePath(), true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        IconCacheJanitor.runJanitor();

        getWindow().setBackgroundDrawable(null);
        bindViews();
        mRequestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isAllowed -> {
                    if (!isAllowed) Tools.runOnUiThread(() ->
                            Toast.makeText(this, R.string.notification_permission_toast, Toast.LENGTH_LONG).show());
                }
        );
        checkNotificationPermission();
        if (LauncherPreferences.PREF_MIGRATION_NOTICE)
            PojavApplication.sExecutorService.submit(this::checkPreviousInstalls);

        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        ProgressKeeper.addTaskCountListener(mDoubleLaunchPreventionListener);
        ProgressKeeper.addTaskCountListener((mProgressServiceKeeper = new ProgressServiceKeeper(this)));

        ProgressKeeper.addTaskCountListener(mProgressLayout);
        ExtraCore.addExtraListener(ExtraConstants.BACK_PREFERENCE, mBackPreferenceListener);
        ExtraCore.addExtraListener(ExtraConstants.SELECT_AUTH_METHOD, mSelectAuthMethod);
        ExtraCore.addExtraListener(ExtraConstants.MOJANG_LOGIN_TODO, mLocalLoginListener);
        ExtraCore.addExtraListener(ExtraConstants.MICROSOFT_LOGIN_TODO, mMicrosoftLoginListener);
        ExtraCore.addExtraListener(ExtraConstants.ELYBY_LOGIN_TODO, mElyByLoginListener);
        ExtraCore.addExtraListener(ExtraConstants.LAUNCH_GAME, mLaunchGameListener);

        new AsyncVersionList().getVersionList(versions ->
                ExtraCore.setValue(ExtraConstants.RELEASE_TABLE, versions));

        mProgressLayout.observe(ProgressLayout.DOWNLOAD_GAME);
        mProgressLayout.observe(ProgressLayout.UNPACK_RUNTIME);
        mProgressLayout.observe(ProgressLayout.INSTALL_MODPACK);
        mProgressLayout.observe(ProgressLayout.AUTHENTICATE);
        mProgressLayout.observe(ProgressLayout.DOWNLOAD_VERSION_LIST);
        mProgressLayout.observe(ProgressLayout.INSTANCE_INSTALL);
        mProgressLayout.observe(ProgressLayout.DATA_MIGRATION);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .setReorderingAllowed(true)
                    .replace(R.id.container_fragment, RkbNav.create(RkbNav.Dest.HOME),
                            RkbNav.Dest.HOME.tag)
                    .commit();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        ContextExecutor.setActivity(this);
        InstanceInstaller.postInstallCheck(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        ContextExecutor.clearActivity();
    }

    @Override
    protected void onDestroy() {
        mMain.removeCallbacksAndMessages(null);
        super.onDestroy();
        mProgressLayout.cleanUpObservers();
        ProgressKeeper.removeTaskCountListener(mProgressLayout);
        ProgressKeeper.removeTaskCountListener(mProgressServiceKeeper);
        ProgressKeeper.removeTaskCountListener(mDoubleLaunchPreventionListener);
        ExtraCore.removeExtraListenerFromValue(ExtraConstants.BACK_PREFERENCE, mBackPreferenceListener);
        ExtraCore.removeExtraListenerFromValue(ExtraConstants.SELECT_AUTH_METHOD, mSelectAuthMethod);
        ExtraCore.removeExtraListenerFromValue(ExtraConstants.MOJANG_LOGIN_TODO, mLocalLoginListener);
        ExtraCore.removeExtraListenerFromValue(ExtraConstants.MICROSOFT_LOGIN_TODO, mMicrosoftLoginListener);
        ExtraCore.removeExtraListenerFromValue(ExtraConstants.ELYBY_LOGIN_TODO, mElyByLoginListener);
        ExtraCore.removeExtraListenerFromValue(ExtraConstants.LAUNCH_GAME, mLaunchGameListener);
    }

    /** Opens an RKB destination. Home is always the root; secondary screens sit above it. */
    public void navigate(RkbNav.Dest dest) {
        if (isFinishing() || isDestroyed()) return;
        FragmentManager fm = getSupportFragmentManager();
        if (fm.isStateSaved()) return;

        Fragment current = fm.findFragmentById(R.id.container_fragment);
        if (current != null && dest.tag.equals(current.getTag())) return;

        fm.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        FragmentTransaction transaction = fm.beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.container_fragment, RkbNav.create(dest), dest.tag);
        if (dest != RkbNav.Dest.HOME) transaction.addToBackStack(dest.tag);
        transaction.commit();
    }

    /** Compatibility helper for existing Pojav fragments that swap into the launcher container. */
    @SuppressWarnings("unused")
    public void swapFragment(Class<? extends Fragment> fragmentClass) {
        if (isFinishing() || isDestroyed()) return;
        FragmentManager fm = getSupportFragmentManager();
        if (fm.isStateSaved()) return;
        fm.beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.container_fragment, fragmentClass, null, fragmentClass.getName())
                .addToBackStack(fragmentClass.getName())
                .commit();
    }

    /** Back returns to Home first; a second press on Home exits. */
    @Override
    public void onBackPressed() {
        MicrosoftLoginFragment fragment =
                (MicrosoftLoginFragment) getVisibleFragment(MicrosoftLoginFragment.TAG);
        if (fragment != null && fragment.canGoBack()) {
            fragment.goBack();
            return;
        }

        if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
            long now = SystemClock.elapsedRealtime();
            if (now - mLastBackPress < 2000) {
                super.onBackPressed();
                return;
            }
            mLastBackPress = now;
            Toast.makeText(this, "Press back again to exit", Toast.LENGTH_SHORT).show();
            return;
        }
        super.onBackPressed();
    }

    @SuppressWarnings("SameParameterValue")
    private Fragment getVisibleFragment(String tag) {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(tag);
        return fragment != null && fragment.isVisible() ? fragment : null;
    }

    @SuppressWarnings("unused")
    private Fragment getVisibleFragment(int id) {
        Fragment fragment = getSupportFragmentManager().findFragmentById(id);
        return fragment != null && fragment.isVisible() ? fragment : null;
    }

    public void askForPermission(int minApi, final String permission) {
        if (Build.VERSION.SDK_INT < minApi) return;
        mRequestPermissionLauncher.launch(permission);
    }

    public boolean checkForPermission(int minApi, final String permission) {
        return Build.VERSION.SDK_INT < minApi ||
                ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_DENIED;
    }

    public boolean checkForPermissionRationale(int minApi, final String permission) {
        return checkForPermission(minApi, permission) ||
                ActivityCompat.shouldShowRequestPermissionRationale(this, permission);
    }

    private void checkNotificationPermission() {
        if (LauncherPreferences.PREF_SKIP_NOTIFICATION_PERMISSION_CHECK ||
                checkForPermission(33, Manifest.permission.POST_NOTIFICATIONS)) {
            return;
        }
        showNotificationPermissionReasoning();
    }

    private void checkPreviousInstalls() {
        final String[] packages = {"net.kdt.pojavlaunch", "net.kdt.pojavlaunch.debug", "net.kdt.pojavlaunch.pub"};
        for (String s : packages) {
            Intent i = getPackageManager().getLaunchIntentForPackage(s);
            if (i == null) continue;
            Tools.runOnUiThread(() ->
                    new AlertDialog.Builder(this)
                            .setTitle(R.string.migration_progress_warning_title)
                            .setMessage(R.string.migration_notice)
                            .setPositiveButton(android.R.string.ok,
                                    (d, button) -> LauncherPreferences.DEFAULT_PREF.edit()
                                            .putBoolean("migrationNotice", false).apply())
                            .setOnDismissListener(d -> LauncherPreferences.PREF_MIGRATION_NOTICE = false)
                            .show());
            break;
        }
    }

    private void showNotificationPermissionReasoning() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.notification_permission_dialog_title)
                .setMessage(R.string.notification_permission_dialog_text)
                .setPositiveButton(android.R.string.ok, (d, w) ->
                        askForPermission(33, Manifest.permission.POST_NOTIFICATIONS))
                .setNegativeButton(android.R.string.cancel, (d, w) -> handleNoNotificationPermission())
                .show();
    }

    private void handleNoNotificationPermission() {
        LauncherPreferences.PREF_SKIP_NOTIFICATION_PERMISSION_CHECK = true;
        LauncherPreferences.DEFAULT_PREF.edit()
                .putBoolean(LauncherPreferences.PREF_KEY_SKIP_NOTIFICATION_CHECK, true)
                .apply();
    }

    private void bindViews() {
        mFragmentView = findViewById(R.id.container_fragment);
        mProgressLayout = findViewById(R.id.progress_layout);
    }
}
