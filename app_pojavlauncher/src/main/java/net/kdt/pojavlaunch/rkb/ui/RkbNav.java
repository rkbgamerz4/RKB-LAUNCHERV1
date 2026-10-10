package net.kdt.pojavlaunch.rkb.ui;

import android.app.Activity;

import androidx.annotation.DrawableRes;
import androidx.fragment.app.Fragment;

import net.kdt.pojavlaunch.LauncherActivity;
import net.kdt.pojavlaunch.R;

/** Single source of truth for sidebar destinations. */
public final class RkbNav {
    private RkbNav() {}

    public enum Dest {
        HOME("rkb_home", "Home", R.drawable.ic_rkb_nav_home),
        CURSOR("rkb_cursor", "Cursor Studio", R.drawable.ic_rkb_nav_cursor),
        MODS("rkb_mods", "Mod Manager", R.drawable.ic_rkb_nav_mods),
        CONTROLS("rkb_controls", "Controls", R.drawable.ic_rkb_nav_controls),
        SKIN("rkb_skin", "Skin & Account", R.drawable.ic_rkb_nav_skin),
        ABOUT("rkb_about", "About", R.drawable.ic_rkb_nav_about),
        SETTINGS("rkb_settings", "Settings", R.drawable.ic_rkb_nav_settings);

        public final String tag;
        public final String label;
        @DrawableRes public final int icon;

        Dest(String tag, String label, @DrawableRes int icon) {
            this.tag = tag;
            this.label = label;
            this.icon = icon;
        }
    }

    public static Fragment create(Dest d) {
        switch (d) {
            case CURSOR: return new CursorStudioFragment();
            case MODS: return new ModManagerFragment();
            case SKIN: return new RkbSkinFragment();
            case ABOUT: return new RkbAboutFragment();
            case SETTINGS: return new RkbSettingsFragment();
            case HOME:
            default: return new RkbHomeFragment();
        }
    }

    /** Account entry point: not signed in -> the RKB auth-method screen, otherwise Skin & Account. */
    public static void openAccount(Activity activity) {
        if (!(activity instanceof LauncherActivity)) return;
        if (RkbUi.currentAccount(activity) == null) {
            net.kdt.pojavlaunch.Tools.swapFragment((LauncherActivity) activity,
                    net.kdt.pojavlaunch.fragments.SelectAuthFragment.class,
                    net.kdt.pojavlaunch.fragments.SelectAuthFragment.TAG, null);
        } else {
            go(activity, Dest.SKIN);
        }
    }

    /** Safe from any fragment: ignored if the host is not the launcher. */
    public static void go(Activity activity, Dest d) {
        if (activity instanceof LauncherActivity) ((LauncherActivity) activity).navigate(d);
    }
}
