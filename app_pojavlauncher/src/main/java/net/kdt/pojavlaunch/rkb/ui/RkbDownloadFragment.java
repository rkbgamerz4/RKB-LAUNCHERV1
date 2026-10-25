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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

/** Download discovery hub. Opens the real Modrinth catalog in the browser for each content type. */
public class RkbDownloadFragment extends Fragment {
    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Context c = requireContext();
        LinearLayout body = new LinearLayout(c);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(RkbUi.dp(c, 2), RkbUi.dp(c, 2), RkbUi.dp(c, 2), RkbUi.dp(c, 12));

        body.addView(category(c, "MODS", "Performance, utility and gameplay mods.",
                "https://modrinth.com/mods", "Open Modrinth mods"));
        body.addView(category(c, "MODPACKS", "Browse curated packs. Check the Minecraft version and loader before installing.",
                "https://modrinth.com/modpacks", "Browse modpacks"));
        body.addView(category(c, "SHADERS", "Shader packs can be demanding on mobile GPUs; choose a compatible renderer.",
                "https://modrinth.com/shaders", "Browse shaders"));
        body.addView(category(c, "TEXTURE PACKS", "Resource packs and visual customizations for supported game versions.",
                "https://modrinth.com/resourcepacks", "Browse texture packs"));

        TextView note = RkbUi.text(c,
                "Downloads open the official Modrinth catalog in your browser. After downloading, install files into the correct Minecraft instance folders; automatic in-app installation is not yet wired to this screen.",
                11, RkbUi.MUTED, false);
        note.setPadding(RkbUi.dp(c, 8), RkbUi.dp(c, 8), RkbUi.dp(c, 8), RkbUi.dp(c, 8));
        body.addView(note);

        ScrollView scroll = new ScrollView(c);
        scroll.setFillViewport(false);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.addView(body, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return RkbUi.scaffold(c, RkbNav.Dest.DOWNLOAD, "Download Hub", "Mods, modpacks, shaders and texture packs.", scroll);
    }

    private View category(Context c, String title, String description, String url, String action) {
        LinearLayout card = RkbUi.card(c);
        card.setPadding(RkbUi.dp(c, 14), RkbUi.dp(c, 14), RkbUi.dp(c, 14), RkbUi.dp(c, 14));
        TextView heading = RkbUi.text(c, title, 15, RkbUi.BLUE, true);
        card.addView(heading);
        TextView desc = RkbUi.text(c, description, 12, RkbUi.WHITE, false);
        desc.setPadding(0, RkbUi.dp(c, 6), 0, RkbUi.dp(c, 10));
        card.addView(desc);
        TextView button = RkbUi.button(c, action, true, v -> RkbLinks.open(v.getContext(), url, title));
        card.addView(button, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, RkbUi.dp(c, 40)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = RkbUi.dp(c, 10);
        card.setLayoutParams(lp);
        return card;
    }
}
