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
        HOME("rkb_home", "Home", R.drawable.ic_rkb_play),
        CURSOR("rkb_cursor", "Cursor Studio", android.R.drawable.ic_menu_crop),
        MODS("rkb_mods", "Mod Manager", android.R.drawable.ic_menu_manage),
        CONTROLS("rkb_controls", "Controls", android.R.drawable.ic_media_play),
        SKIN("rkb_skin", "Skin & Account", android.R.drawable.ic_menu_myplaces),
        ABOUT("rkb_about", "About", android.R.drawable.ic_menu_info_details),
        SETTINGS("rkb_settings", "Settings", android.R.drawable.ic_menu_preferences);

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

    /** Safe from any fragment: ignored if the host is not the launcher. */
    public static void go(Activity activity, Dest d) {
        if (activity instanceof LauncherActivity) ((LauncherActivity) activity).navigate(d);
    }
}
