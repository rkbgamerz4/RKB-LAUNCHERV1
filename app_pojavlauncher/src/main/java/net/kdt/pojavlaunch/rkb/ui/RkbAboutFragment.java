package net.kdt.pojavlaunch.rkb.ui;

import android.content.Context;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import net.kdt.pojavlaunch.BuildConfig;
import net.kdt.pojavlaunch.R;

/** About screen. Version comes from BuildConfig; only links that really exist are shown. */
public class RkbAboutFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        final Context c = requireContext();
        LinearLayout body = RkbUi.card(c);
        body.setGravity(Gravity.CENTER_HORIZONTAL);
        body.setPadding(RkbUi.dp(c, 18), RkbUi.dp(c, 18), RkbUi.dp(c, 18), RkbUi.dp(c, 18));

        TextView logo = RkbUi.text(c, "R", 34, RkbUi.BLUE, true);
        logo.setGravity(Gravity.CENTER);
        logo.setBackgroundResource(R.drawable.bg_rkb_logo);
        body.addView(logo, new LinearLayout.LayoutParams(RkbUi.dp(c, 72), RkbUi.dp(c, 72)));

        TextView title = RkbUi.text(c, "RKB LAUNCHER", 20, RkbUi.WHITE, true);
        title.setPadding(0, RkbUi.dp(c, 10), 0, 0);
        body.addView(title);
        body.addView(RkbUi.text(c, "by RKB GAMERZ", 12, RkbUi.MUTED, false));
        TextView ver = RkbUi.text(c, "Version " + BuildConfig.VERSION_NAME, 12, RkbUi.BLUE, true);
        ver.setPadding(0, RkbUi.dp(c, 4), 0, RkbUi.dp(c, 10));
        ver.setGravity(Gravity.CENTER);
        body.addView(ver);

        TextView desc = RkbUi.text(c, "RKB Launcher is a Minecraft: Java Edition launcher for Android, "
                + "based on PojavLauncher.", 13, RkbUi.WHITE, false);
        desc.setGravity(Gravity.CENTER);
        body.addView(desc);

        addLink(body, "Discord", RkbLinks.DISCORD);
        addLink(body, "GitHub", RkbLinks.GITHUB);
        if (!RkbLinks.YOUTUBE.isEmpty()) addLink(body, "YouTube", RkbLinks.YOUTUBE);
        if (!RkbLinks.WEBSITE.isEmpty()) addLink(body, "Website", RkbLinks.WEBSITE);

        ScrollView sv = new ScrollView(c);
        sv.setVerticalScrollBarEnabled(false);
        sv.addView(body, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return RkbUi.scaffold(c, RkbNav.Dest.ABOUT, "About RKB Launcher",
                "Play  \u2022  Explore  \u2022  Create", sv);
    }

    private void addLink(LinearLayout parent, final String label, final String url) {
        Context c = parent.getContext();
        TextView b = RkbUi.button(c, label, false, v -> RkbLinks.open(v.getContext(), url, label));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, RkbUi.dp(c, 44));
        lp.topMargin = RkbUi.dp(c, 10);
        parent.addView(b, lp);
    }
}
