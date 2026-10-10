package net.kdt.pojavlaunch.prefs.screens;


import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import net.kdt.pojavlaunch.rkb.ui.RkbUi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import net.kdt.pojavlaunch.LauncherActivity;
import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;

/**
 * Preference for the main screen, any sub-screen should inherit this class for consistent behavior,
 * overriding only onCreatePreferences
 */
public class LauncherPreferenceFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        view.setBackgroundColor(getResources().getColor(R.color.background_app));
        super.onViewCreated(view, savedInstanceState);
        applyRkbStyle();
    }

    /**
     * RKB look for the REAL preference rows: every row becomes a rounded navy card and category
     * headers become neon-blue labels. Keys, values, click handling and dialogs are untouched.
     */
    private void applyRkbStyle() {
        final RecyclerView rv = getListView();
        if (rv == null) return;
        final android.content.Context c = rv.getContext();
        rv.setClipToPadding(false);
        rv.setPadding(RkbUi.dp(c, 4), RkbUi.dp(c, 4), RkbUi.dp(c, 4), RkbUi.dp(c, 16));
        rv.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
        rv.addOnChildAttachStateChangeListener(new RecyclerView.OnChildAttachStateChangeListener() {
            @Override public void onChildViewAttachedToWindow(@NonNull View v) { styleRow(c, v); }
            @Override public void onChildViewDetachedFromWindow(@NonNull View v) { }
        });
        for (int i = 0; i < rv.getChildCount(); i++) styleRow(c, rv.getChildAt(i));
    }

    private static void styleRow(android.content.Context c, View v) {
        TextView title = v.findViewById(android.R.id.title);
        if (title == null) return;
        TextView summary = v.findViewById(android.R.id.summary);
        boolean category = summary == null
                && v.findViewById(android.R.id.widget_frame) == null
                && v.findViewById(android.R.id.icon) == null;
        ViewGroup.LayoutParams lp = v.getLayoutParams();
        if (category) {
            v.setBackground(null);
            title.setTextColor(RkbUi.BLUE);
            if (lp instanceof ViewGroup.MarginLayoutParams) {
                ((ViewGroup.MarginLayoutParams) lp).setMargins(0, RkbUi.dp(c, 10), 0, RkbUi.dp(c, 2));
                v.setLayoutParams(lp);
            }
        } else {
            v.setBackground(RkbUi.ripple(c, RkbUi.rounded(c, 0xFF0B1220, 14, RkbUi.BORDER, 1)));
            title.setTextColor(RkbUi.WHITE);
            if (summary != null) summary.setTextColor(RkbUi.MUTED);
            if (lp instanceof ViewGroup.MarginLayoutParams) {
                ((ViewGroup.MarginLayoutParams) lp).setMargins(0, RkbUi.dp(c, 3), 0, RkbUi.dp(c, 3));
                v.setLayoutParams(lp);
            }
        }
    }

    @Override
    public void onCreatePreferences(Bundle b, String str) {
        addPreferencesFromResource(R.xml.pref_main);
        setupNotificationRequestPreference();
    }

    private void setupNotificationRequestPreference() {
        Preference mRequestNotificationPermissionPreference = requirePreference("notification_permission_request");
        Activity activity = getActivity();
        if(activity instanceof LauncherActivity) {
            LauncherActivity launcherActivity = (LauncherActivity)activity;
            mRequestNotificationPermissionPreference.setVisible(!launcherActivity.checkForNotificationPermission());
            mRequestNotificationPermissionPreference.setOnPreferenceClickListener(preference -> {
                launcherActivity.askForNotificationPermission(()->mRequestNotificationPermissionPreference.setVisible(false));
                return true;
            });
        }else{
            mRequestNotificationPermissionPreference.setVisible(false);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        SharedPreferences sharedPreferences = getPreferenceManager().getSharedPreferences();
        if(sharedPreferences != null) sharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        SharedPreferences sharedPreferences = getPreferenceManager().getSharedPreferences();
        if(sharedPreferences != null) sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences p, String s) {
        LauncherPreferences.loadPreferences(getContext());
    }

    protected Preference requirePreference(CharSequence key) {
        Preference preference = findPreference(key);
        if(preference != null) return preference;
        throw new IllegalStateException("Preference "+key+" is null");
    }
    @SuppressWarnings("unchecked")
    protected <T extends Preference> T requirePreference(CharSequence key, Class<T> preferenceClass) {
        Preference preference = requirePreference(key);
        if(preferenceClass.isInstance(preference)) return (T)preference;
        throw new IllegalStateException("Preference "+key+" is not an instance of "+preferenceClass.getSimpleName());
    }
}
