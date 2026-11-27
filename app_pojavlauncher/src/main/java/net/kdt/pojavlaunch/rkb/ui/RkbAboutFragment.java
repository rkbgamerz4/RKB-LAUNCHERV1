package net.kdt.pojavlaunch.rkb.ui;

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

/** About screen: brand, factual app information, real features and real links only. */
public class RkbAboutFragment extends Fragment {

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        final android.content.Context c = requireContext();

        LinearLayout body = new LinearLayout(c);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(RkbUi.dp(c, 14), RkbUi.dp(c, 8), RkbUi.dp(c, 14), RkbUi.dp(c, 18));

        body.addView(brand(c), lp(c));
        body.addView(RkbUi.spacer(c, 12));
        body.addView(info(c), lp(c));
        body.addView(RkbUi.spacer(c, 12));
        body.addView(features(c), lp(c));
        body.addView(RkbUi.spacer(c, 12));
        body.addView(links(c), lp(c));
        body.addView(RkbUi.spacer(c, 12));
        body.addView(team(c), lp(c));

        ScrollView scroll = new ScrollView(c);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.addView(body, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return RkbUi.scaffold(c, RkbNav.Dest.ABOUT, "About",
                "RKB LAUNCHER for Minecraft: Java Edition", scroll);
    }

    private static LinearLayout.LayoutParams lp(android.content.Context c) {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private View brand(android.content.Context c) {
        LinearLayout card = RkbUi.card(c);
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        TextView logo = RkbUi.text(c, "R", 34, RkbUi.WHITE, true);
        logo.setGravity(Gravity.CENTER);
        logo.setBackground(RkbUi.rounded(c, RkbUi.BLUE, 20, RkbUi.BORDER, 1));
        card.addView(logo, new LinearLayout.LayoutParams(
                RkbUi.dp(c, 76), RkbUi.dp(c, 76)));
        TextView title = RkbUi.text(c, "RKB LAUNCHER", 20, RkbUi.WHITE, true);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, RkbUi.dp(c, 10), 0, 0);
        card.addView(title);
        TextView sub = RkbUi.text(c, "by RKB GAMERZ", 12, RkbUi.MUTED, false);
        sub.setGravity(Gravity.CENTER);
        card.addView(sub);
        TextView ver = RkbUi.text(c, "Version " + BuildConfig.VERSION_NAME, 11, RkbUi.BLUE, true);
        ver.setGravity(Gravity.CENTER);
        ver.setPadding(0, RkbUi.dp(c, 6), 0, 0);
        card.addView(ver);
        return card;
    }

    private View info(android.content.Context c) {
        LinearLayout card = RkbUi.card(c);
        card.addView(RkbUi.text(c, "APP INFORMATION", 12, RkbUi.BLUE, true));
        card.addView(row(c, "Platform", "Android"));
        card.addView(row(c, "Edition", "Minecraft: Java Edition"));
        card.addView(row(c, "Base", "PojavLauncher source"));
        card.addView(row(c, "Architecture", "arm64 / armv7 / x86"));
        return card;
    }

    private View features(android.content.Context c) {
        LinearLayout card = RkbUi.card(c);
        card.addView(RkbUi.text(c, "FEATURES", 12, RkbUi.BLUE, true));
        card.addView(bullet(c, "Multiple Minecraft instances and profiles"));
        card.addView(bullet(c, "Microsoft, Ely.by and offline accounts"));
        card.addView(bullet(c, "Pojav custom controls and Cursor Studio"));
        card.addView(bullet(c, "Per-instance mod enable / disable"));
        card.addView(bullet(c, "Modrinth browsing by content category"));
        return card;
    }

    private View links(android.content.Context c) {
        LinearLayout card = RkbUi.card(c);
        card.addView(RkbUi.text(c, "QUICK LINKS", 12, RkbUi.BLUE, true));
        card.addView(RkbUi.spacer(c, 8));
        card.addView(RkbUi.button(c, "DISCORD", false,
                v -> RkbLinks.open(v.getContext(), RkbLinks.DISCORD, "Discord")), lp(c));
        card.addView(RkbUi.spacer(c, 6));
        card.addView(RkbUi.button(c, "GITHUB", false,
                v -> RkbLinks.open(v.getContext(), RkbLinks.GITHUB, "GitHub")), lp(c));
        return card;
    }

    private View team(android.content.Context c) {
        LinearLayout card = RkbUi.card(c);
        card.addView(RkbUi.text(c, "OUR TEAM", 12, RkbUi.BLUE, true));
        TextView name = RkbUi.text(c, "RKB GAMERZ", 16, RkbUi.WHITE, true);
        name.setPadding(0, RkbUi.dp(c, 8), 0, 0);
        card.addView(name);
        TextView note = RkbUi.text(c,
                "Built on the open-source PojavLauncher project. Contributors and credits are listed in the project repository.",
                12, RkbUi.MUTED, false);
        note.setPadding(0, RkbUi.dp(c, 4), 0, 0);
        card.addView(note);
        return card;
    }

    private View row(android.content.Context c, String label, String value) {
        LinearLayout row = new LinearLayout(c);
        row.setPadding(0, RkbUi.dp(c, 8), 0, 0);
        row.addView(RkbUi.text(c, label, 12, RkbUi.MUTED, false),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(RkbUi.text(c, value, 12, RkbUi.WHITE, true),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.4f));
        return row;
    }

    private View bullet(android.content.Context c, String value) {
        TextView t = RkbUi.text(c, "\u2022  " + value, 12, RkbUi.WHITE, false);
        t.setPadding(0, RkbUi.dp(c, 5), 0, 0);
        return t;
    }
}
