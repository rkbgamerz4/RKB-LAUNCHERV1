package net.kdt.pojavlaunch.rkb.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.extra.ExtraConstants;
import net.kdt.pojavlaunch.extra.ExtraCore;
import net.kdt.pojavlaunch.fragments.InstanceEditorFragment;
import net.kdt.pojavlaunch.instances.Instance;
import net.kdt.pojavlaunch.instances.Instances;

import java.io.IOException;
import java.util.List;

/** Separate Instances screen: real profiles from Instances storage, with launch/edit/delete actions. */
public class RkbInstancesFragment extends Fragment {
    private LinearLayout mGridHost;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        final android.content.Context c = requireContext();

        LinearLayout body = new LinearLayout(c);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(RkbUi.dp(c, 14), RkbUi.dp(c, 8), RkbUi.dp(c, 14), RkbUi.dp(c, 18));

        LinearLayout actions = new LinearLayout(c);
        actions.setGravity(Gravity.CENTER_VERTICAL);
        actions.addView(RkbUi.text(c, "Installed versions & profiles", 14, RkbUi.WHITE, true),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        actions.addView(RkbUi.button(c, "+ CREATE INSTANCE", true,
                v -> openNewInstance(v.getContext())),
                new LinearLayout.LayoutParams(RkbUi.dp(c, 168), RkbUi.dp(c, 44)));
        body.addView(actions, layoutParams(c));

        body.addView(RkbUi.spacer(c, 12));
        mGridHost = new LinearLayout(c);
        mGridHost.setOrientation(LinearLayout.VERTICAL);
        body.addView(mGridHost, layoutParams(c));

        body.addView(RkbUi.spacer(c, 12));
        TextView createHint = RkbUi.button(c, "Create a new instance", false,
                v -> openNewInstance(v.getContext()));
        body.addView(createHint, layoutParams(c));

        ScrollView scroll = new ScrollView(c);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.addView(body, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        reload();
        return RkbUi.scaffold(c, RkbNav.Dest.INSTANCES, "Instances",
                "Manage the Minecraft versions you play.", scroll);
    }

    private static LinearLayout.LayoutParams layoutParams(android.content.Context c) {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private void openNewInstance(android.content.Context c) {
        Activity a = RkbUi.activityFrom(c);
        if (a != null) RkbNav.go(a, RkbNav.Dest.NEW_INSTANCE);
    }

    private void reload() {
        if (mGridHost == null || !isAdded()) return;
        final android.content.Context c = requireContext();
        mGridHost.removeAllViews();

        final List<Instance> instances;
        try {
            instances = Instances.loadAllInstances();
        } catch (IOException e) {
            mGridHost.addView(RkbUi.text(c, "Could not read instances: " + e.getMessage(),
                    13, RkbUi.WHITE, false), layoutParams(c));
            return;
        }
        if (instances.isEmpty()) {
            mGridHost.addView(RkbUi.text(c, "No instances yet. Create one to get started.",
                    13, RkbUi.WHITE, false), layoutParams(c));
            return;
        }

        String activeName = null;
        String activeVersion = null;
        try {
            net.kdt.pojavlaunch.instances.DisplayInstance active =
                    Instances.loadDisplay().list.get(Instances.loadDisplay().selectedIndex);
            activeName = active.name;
            activeVersion = active.versionId;
        } catch (Exception ignored) { }

        GridLayout grid = new GridLayout(c);
        grid.setColumnCount(RkbUi.isCompact(c) ? 1 : 2);
        grid.setUseDefaultMargins(false);

        for (final Instance instance : instances) {
            boolean active = sameInstance(instance.name, activeName, instance.versionId, activeVersion);
            View card = instanceCard(c, instance, active);
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = 0;
            lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            lp.setMargins(RkbUi.dp(c, 6), RkbUi.dp(c, 6),
                    RkbUi.dp(c, 6), RkbUi.dp(c, 6));
            grid.addView(card, lp);
        }
        mGridHost.addView(grid, layoutParams(c));
    }

    private static boolean sameInstance(String name, String activeName, String version, String activeVersion) {
        if (activeName == null || activeVersion == null) return false;
        return activeName.equals(name) && activeVersion.equals(version);
    }

    private View instanceCard(android.content.Context c, final Instance instance, boolean active) {
        LinearLayout card = RkbUi.card(c);
        card.setPadding(RkbUi.dp(c, 14), RkbUi.dp(c, 14), RkbUi.dp(c, 14), RkbUi.dp(c, 14));

        LinearLayout top = new LinearLayout(c);
        top.setGravity(Gravity.CENTER_VERTICAL);
        ImageView icon = new ImageView(c);
        icon.setImageResource(iconFor(instance.icon));
        icon.setPadding(RkbUi.dp(c, 8), RkbUi.dp(c, 8), RkbUi.dp(c, 8), RkbUi.dp(c, 8));
        icon.setBackground(RkbUi.rounded(c, RkbUi.CARD_2, 14, RkbUi.BORDER, 1));
        top.addView(icon, new LinearLayout.LayoutParams(
                RkbUi.dp(c, 48), RkbUi.dp(c, 48)));

        LinearLayout names = new LinearLayout(c);
        names.setOrientation(LinearLayout.VERTICAL);
        names.setPadding(RkbUi.dp(c, 12), 0, 0, 0);
        names.addView(RkbUi.text(c, Tools.isValidString(instance.name) ? instance.name : "Unnamed",
                16, RkbUi.WHITE, true));
        names.addView(RkbUi.text(c, Tools.isValidString(instance.versionId) ? instance.versionId : "?",
                12, RkbUi.MUTED, false));
        if (active) names.addView(RkbUi.text(c, "ACTIVE", 10, RkbUi.GREEN, true));
        top.addView(names, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView more = RkbUi.text(c, "•••", 18, RkbUi.WHITE, true);
        more.setGravity(Gravity.CENTER);
        more.setClickable(true);
        more.setOnClickListener(v -> showMenu(v, instance));
        top.addView(more, new LinearLayout.LayoutParams(RkbUi.dp(c, 36), RkbUi.dp(c, 36)));
        card.addView(top, layoutParams(c));

        card.addView(RkbUi.spacer(c, 12));
        LinearLayout buttons = new LinearLayout(c);
        TextView play = RkbUi.button(c, "PLAY", true, v -> {
            Instances.setSelectedInstance(instance);
            try {
                ExtraCore.setValue(ExtraConstants.LAUNCH_GAME, true);
            } catch (Exception e) {
                Toast.makeText(v.getContext(), "Unable to start Minecraft", Toast.LENGTH_SHORT).show();
            }
        });
        TextView edit = RkbUi.button(c, "EDIT", false, v -> {
            Instances.setSelectedInstance(instance);
            if (isAdded()) Tools.swapFragment(requireActivity(), InstanceEditorFragment.class,
                    InstanceEditorFragment.TAG, null);
        });
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, RkbUi.dp(c, 42), 1.4f);
        LinearLayout.LayoutParams e = new LinearLayout.LayoutParams(0, RkbUi.dp(c, 42), 1f);
        e.leftMargin = RkbUi.dp(c, 8);
        buttons.addView(play, p);
        buttons.addView(edit, e);
        card.addView(buttons, layoutParams(c));
        return card;
    }

    private void showMenu(View anchor, final Instance instance) {
        PopupMenu menu = new PopupMenu(anchor.getContext(), anchor);
        menu.getMenu().add("Select");
        menu.getMenu().add("Edit");
        menu.getMenu().add("Delete");
        menu.setOnMenuItemClickListener(item -> {
            if ("Select".equals(item.getTitle())) {
                Instances.setSelectedInstance(instance);
                reload();
                return true;
            }
            if ("Edit".equals(item.getTitle())) {
                Instances.setSelectedInstance(instance);
                if (isAdded()) Tools.swapFragment(requireActivity(), InstanceEditorFragment.class,
                        InstanceEditorFragment.TAG, null);
                return true;
            }
            new AlertDialog.Builder(anchor.getContext())
                    .setTitle("Delete instance?")
                    .setMessage("This removes the instance folder and all of its data.")
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton("Delete", (d, w) -> {
                        try {
                            Instances.removeInstance(instance);
                            reload();
                        } catch (IOException e) {
                            Toast.makeText(anchor.getContext(), "Could not delete: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    })
                    .show();
            return true;
        });
        menu.show();
    }

    private static int iconFor(String name) {
        if ("fabric".equals(name)) return R.drawable.ic_fabric;
        if ("quilt".equals(name)) return R.drawable.ic_quilt;
        if ("forge".equals(name)) return R.drawable.ic_forge;
        if ("neoforge".equals(name)) return R.drawable.ic_neoforge;
        return R.drawable.ic_pojav_full;
    }
}
