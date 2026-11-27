package net.kdt.pojavlaunch.rkb.ui;

import android.app.Activity;

import androidx.annotation.DrawableRes;
import androidx.fragment.app.Fragment;

import net.kdt.pojavlaunch.LauncherActivity;
import net.kdt.pojavlaunch.R;

/** Single source of truth for RKB destinations. */
public final class RkbNav {
    private RkbNav() {}

    public enum Dest {
        HOME("rkb_home", "Home", "Home", R.drawable.ic_rkb_nav_home),
        INSTANCES("rkb_instances", "Versions", "Instances", R.drawable.ic_rkb_add),
        NEW_INSTANCE("rkb_new_instance", "New Instance", "New Instance", R.drawable.ic_rkb_add),
        DOWNLOAD("rkb_download", "Browse", "Download", R.drawable.ic_rkb_nav_download),
        MODS("rkb_mods", "Mods", "Mod Manager", R.drawable.ic_rkb_nav_mods),
        CONTROLS("rkb_controls", "Controls", "Controls", R.drawable.ic_rkb_nav_controls),
        CURSOR("rkb_cursor", "Cursor", "Cursor Studio", R.drawable.ic_rkb_nav_cursor),
        SKIN("rkb_skin", "Account", "Account", R.drawable.ic_rkb_nav_skin),
        ABOUT("rkb_about", "About", "About", R.drawable.ic_rkb_nav_about),
        SETTINGS("rkb_settings", "Settings", "Settings", R.drawable.ic_rkb_nav_settings);

        public final String tag;
        /** Short label used in the horizontal top navigation. */
        public final String navLabel;
        /** Full label used as a screen title when needed. */
        public final String label;
        @DrawableRes public final int icon;

        Dest(String tag, String navLabel, String label, @DrawableRes int icon) {
            this.tag = tag;
            this.navLabel = navLabel;
            this.label = label;
            this.icon = icon;
        }
    }

    /** Destinations rendered as pills in the reference top navigation. */
    public static final Dest[] TOP_NAV = {
            Dest.HOME, Dest.INSTANCES, Dest.DOWNLOAD, Dest.MODS,
            Dest.CONTROLS, Dest.CURSOR, Dest.SKIN, Dest.ABOUT, Dest.SETTINGS
    };

    public static Fragment create(Dest d) {
        switch (d) {
            case INSTANCES: return new RkbInstancesFragment();
            case NEW_INSTANCE: return new RkbNewInstanceFragment();
            case DOWNLOAD: return new RkbDownloadFragment();
            case MODS: return new ModManagerFragment();
            case CONTROLS: return new RkbControlsFragment();
            case CURSOR: return new CursorStudioFragment();
            case SKIN: return new RkbSkinFragment();
            case ABOUT: return new RkbAboutFragment();
            case SETTINGS: return new RkbSettingsFragment();
            case HOME:
            default: return new RkbHomeFragment();
        }
    }

    /** Account entry point. Sign-in methods live on the Account screen in this UI. */
    public static void openAccount(Activity activity) {
        go(activity, Dest.SKIN);
    }

    /** Safe from any fragment: ignored if the host is not the launcher. */
    public static void go(Activity activity, Dest d) {
        if (activity instanceof LauncherActivity) ((LauncherActivity) activity).navigate(d);
    }
}
