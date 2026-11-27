package net.kdt.pojavlaunch.rkb.ui;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.fragments.BTAInstallFragment;
import net.kdt.pojavlaunch.fragments.FabricInstallFragment;
import net.kdt.pojavlaunch.fragments.ForgeInstallFragment;
import net.kdt.pojavlaunch.fragments.InstanceEditorFragment;
import net.kdt.pojavlaunch.fragments.LegacyFabricInstallFragment;
import net.kdt.pojavlaunch.fragments.NeoforgeInstallFragment;
import net.kdt.pojavlaunch.fragments.OptiFineInstallFragment;
import net.kdt.pojavlaunch.fragments.QuiltInstallFragment;
import net.kdt.pojavlaunch.fragments.SearchModFragment;
import net.kdt.pojavlaunch.instances.Instance;
import net.kdt.pojavlaunch.instances.Instances;

import java.io.IOException;

/** New Instance screen. Only real project creation flows are clickable. */
public class RkbNewInstanceFragment extends Fragment {

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        final android.content.Context c = requireContext();

        LinearLayout body = new LinearLayout(c);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(RkbUi.dp(c, 14), RkbUi.dp(c, 8), RkbUi.dp(c, 14), RkbUi.dp(c, 18));

        TextView intro = RkbUi.text(c, "Choose how your new instance should be created.",
                13, RkbUi.MUTED, false);
        body.addView(intro, lp(c));
        body.addView(RkbUi.spacer(c, 12));

        GridLayout grid = new GridLayout(c);
        grid.setColumnCount(2);
        addTile(c, grid, "NORMAL", "Vanilla Minecraft", true,
                v -> createNormal());
        addTile(c, grid, "MODPACK", "Install from a modpack", true,
                v -> open(SearchModFragment.class, SearchModFragment.TAG));
        addTile(c, grid, "RUST EDITION", "Not available in this build", false, null);
        addTile(c, grid, "RKB BOOST", "Not available in this build", false, null);
        body.addView(grid, lp(c));

        body.addView(RkbUi.spacer(c, 16));
        body.addView(RkbUi.text(c, "Other real modded loaders", 14, RkbUi.WHITE, true), lp(c));
        body.addView(RkbUi.spacer(c, 8));

        GridLayout loaders = new GridLayout(c);
        loaders.setColumnCount(2);
        addTile(c, loaders, "FABRIC", "Lightweight mod loader", true,
                v -> open(FabricInstallFragment.class, FabricInstallFragment.TAG));
        addTile(c, loaders, "FORGE", "Classic mod loader", true,
                v -> open(ForgeInstallFragment.class, ForgeInstallFragment.TAG));
        addTile(c, loaders, "QUILT", "Fabric-compatible loader", true,
                v -> open(QuiltInstallFragment.class, QuiltInstallFragment.TAG));
        addTile(c, loaders, "NEOFORGE", "Modern Forge fork", true,
                v -> open(NeoforgeInstallFragment.class, NeoforgeInstallFragment.TAG));
        addTile(c, loaders, "LEGACY FABRIC", "Older Minecraft versions", true,
                v -> open(LegacyFabricInstallFragment.class, LegacyFabricInstallFragment.TAG));
        addTile(c, loaders, "OPTIFINE", "Performance & visuals", true,
                v -> open(OptiFineInstallFragment.class, OptiFineInstallFragment.TAG));
        addTile(c, loaders, "BTA", "Better Than Adventure", true,
                v -> open(BTAInstallFragment.class, BTAInstallFragment.TAG));
        body.addView(loaders, lp(c));

        ScrollView scroll = new ScrollView(c);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.addView(body, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return RkbUi.scaffold(c, RkbNav.Dest.NEW_INSTANCE, "New Instance",
                "Create a version to launch.", scroll);
    }

    private static LinearLayout.LayoutParams lp(android.content.Context c) {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private void addTile(android.content.Context c, GridLayout grid, String title, String subtitle,
                         boolean enabled, View.OnClickListener click) {
        LinearLayout card = RkbUi.card(c);
        card.setPadding(RkbUi.dp(c, 16), RkbUi.dp(c, 18), RkbUi.dp(c, 16), RkbUi.dp(c, 18));
        card.setMinimumHeight(RkbUi.dp(c, 112));
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.addView(RkbUi.text(c, title, 16, RkbUi.WHITE, true));
        TextView sub = RkbUi.text(c, subtitle, 11, enabled ? RkbUi.MUTED : 0x99FFFFFF, false);
        sub.setPadding(0, RkbUi.dp(c, 6), 0, 0);
        card.addView(sub);
        if (enabled) {
            card.setBackground(RkbUi.ripple(c,
                    RkbUi.rounded(c, RkbUi.CARD_2, 18, RkbUi.BORDER, 1)));
            card.setClickable(true);
            card.setFocusable(true);
            card.setOnClickListener(click);
            RkbMotion.press(card);
        } else {
            card.setAlpha(0.55f);
        }

        GridLayout.LayoutParams glp = new GridLayout.LayoutParams();
        glp.width = 0;
        glp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        glp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        glp.setMargins(RkbUi.dp(c, 6), RkbUi.dp(c, 6), RkbUi.dp(c, 6), RkbUi.dp(c, 6));
        grid.addView(card, glp);
    }

    private void createNormal() {
        if (!isAdded()) return;
        try {
            Instance instance = Instances.createDefaultInstance();
            Instances.setSelectedInstance(instance);
            Tools.swapFragment(requireActivity(), InstanceEditorFragment.class,
                    InstanceEditorFragment.TAG, null);
        } catch (IOException e) {
            Toast.makeText(requireContext(), "Could not create instance: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void open(Class<? extends Fragment> fragmentClass, String tag) {
        if (!isAdded()) return;
        Tools.swapFragment(requireActivity(), fragmentClass, tag, null);
    }
}
