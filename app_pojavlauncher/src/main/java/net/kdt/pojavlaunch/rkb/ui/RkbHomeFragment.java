package net.kdt.pojavlaunch.rkb.ui;

import android.app.Activity;
import android.app.AlertDialog;
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

import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.authenticator.accounts.Account;
import net.kdt.pojavlaunch.extra.ExtraConstants;
import net.kdt.pojavlaunch.extra.ExtraCore;
import net.kdt.pojavlaunch.instances.DisplayInstance;
import net.kdt.pojavlaunch.instances.Instances;
import net.kdt.pojavlaunch.rkb.skin.RkbSkinLoader;
import net.kdt.pojavlaunch.rkb.skin.RkbSkinView;

import java.io.IOException;
import java.util.Locale;

/** Home screen: account at the top, one selected instance, one prominent Launch action. */
public class RkbHomeFragment extends Fragment {
    private TextView mInstanceName;
    private TextView mInstanceVersion;
    private int mSkinRequest;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        final ContextHolder holder = new ContextHolder(requireContext());

        LinearLayout body = new LinearLayout(holder.context);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(RkbUi.dp(holder.context, 14), RkbUi.dp(holder.context, 10),
                RkbUi.dp(holder.context, 14), RkbUi.dp(holder.context, 18));

        body.addView(accountCard(holder.context), matchWidth());
        body.addView(RkbUi.spacer(holder.context, 12));
        body.addView(heroCard(holder.context), matchWidth());
        body.addView(RkbUi.spacer(holder.context, 12));
        body.addView(launchCard(holder.context), matchWidth());

        ScrollView scroll = new ScrollView(holder.context);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setFillViewport(true);
        scroll.addView(body, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        return RkbUi.scaffold(holder.context, RkbNav.Dest.HOME, "Home",
                "Play Minecraft: Java Edition", scroll);
    }

    @Override public void onDestroyView() {
        mSkinRequest++; // ignore in-flight skin callbacks
        mInstanceName = null;
        mInstanceVersion = null;
        super.onDestroyView();
    }

    private static final class ContextHolder {
        final android.content.Context context;
        ContextHolder(android.content.Context c) { this.context = c; }
    }

    private LinearLayout.LayoutParams matchWidth() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private View accountCard(android.content.Context c) {
        LinearLayout card = RkbUi.card(c);
        final Account acc = RkbUi.currentAccount(c);

        RkbSkinView skin = new RkbSkinView(c);
        LinearLayout.LayoutParams skinLp = new LinearLayout.LayoutParams(
                RkbUi.dp(c, 44), RkbUi.dp(c, 62));
        skinLp.rightMargin = RkbUi.dp(c, 12);

        final int request = ++mSkinRequest;
        RkbSkinLoader.load(c, acc, result -> {
            if (request != mSkinRequest || !isAdded() || getView() == null) return;
            skin.setSkin(result.skin, result.slim);
        });

        LinearLayout names = new LinearLayout(c);
        names.setOrientation(LinearLayout.VERTICAL);
        names.setGravity(Gravity.CENTER_VERTICAL);
        names.addView(RkbUi.text(c, "PLAYER", 10, RkbUi.MUTED, true));
        names.addView(RkbUi.text(c, RkbUi.accountName(c), 17, RkbUi.WHITE, true));
        names.addView(RkbUi.text(c, RkbUi.accountBadge(c), 11, RkbUi.BLUE, true));

        LinearLayout top = new LinearLayout(c);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.addView(skin, skinLp);
        top.addView(names, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        top.addView(RkbUi.button(c, "ACCOUNT", false, v -> {
            Activity a = RkbUi.activityFrom(v.getContext());
            if (a != null) RkbNav.openAccount(a);
        }), new LinearLayout.LayoutParams(RkbUi.dp(c, 96), RkbUi.dp(c, 42)));
        card.addView(top, matchWidth());
        return card;
    }

    private View heroCard(android.content.Context c) {
        LinearLayout card = RkbUi.card(c);
        card.setPadding(RkbUi.dp(c, 20), RkbUi.dp(c, 20), RkbUi.dp(c, 20), RkbUi.dp(c, 20));

        TextView eyebrow = RkbUi.text(c, "RKB LAUNCHER", 11, RkbUi.BLUE, true);
        card.addView(eyebrow);
        TextView title = RkbUi.text(c, "Your world starts here.", 26, RkbUi.WHITE, true);
        title.setPadding(0, RkbUi.dp(c, 6), 0, 0);
        card.addView(title);
        TextView desc = RkbUi.text(c,
                "Pick an instance below and launch Minecraft: Java Edition.", 13, RkbUi.MUTED, false);
        desc.setPadding(0, RkbUi.dp(c, 6), 0, 0);
        card.addView(desc, matchWidth());
        card.addView(RkbUi.spacer(c, 14));
        return card;
    }

    private View launchCard(android.content.Context c) {
        LinearLayout card = RkbUi.card(c);
        card.setPadding(RkbUi.dp(c, 14), RkbUi.dp(c, 14), RkbUi.dp(c, 14), RkbUi.dp(c, 14));

        LinearLayout row = new LinearLayout(c);
        row.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout version = new LinearLayout(c);
        version.setOrientation(LinearLayout.VERTICAL);
        version.setGravity(Gravity.CENTER_VERTICAL);
        version.setPadding(RkbUi.dp(c, 14), RkbUi.dp(c, 8), RkbUi.dp(c, 14), RkbUi.dp(c, 8));
        version.setBackground(RkbUi.ripple(c, RkbUi.rounded(c, RkbUi.CARD_2, 16, RkbUi.BORDER, 1)));
        version.setClickable(true);
        version.setOnClickListener(v -> showInstanceChooser());
        RkbMotion.press(version);

        version.addView(RkbUi.text(c, "VERSION / INSTANCE", 10, RkbUi.MUTED, true));
        mInstanceName = RkbUi.text(c, "Loading...", 16, RkbUi.WHITE, true);
        mInstanceVersion = RkbUi.text(c, "", 11, RkbUi.BLUE, false);
        version.addView(mInstanceName);
        version.addView(mInstanceVersion);

        row.addView(version, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(RkbUi.spacer(c, 1));

        TextView launch = RkbUi.button(c, "LAUNCH", true, v -> launchMinecraft());
        launch.setTextSize(18);
        launch.setMinHeight(RkbUi.dp(c, 64));
        launch.setPadding(RkbUi.dp(c, 28), 0, RkbUi.dp(c, 28), 0);
        LinearLayout.LayoutParams launchLp = new LinearLayout.LayoutParams(
                RkbUi.dp(c, 156), RkbUi.dp(c, 64));
        launchLp.leftMargin = RkbUi.dp(c, 12);
        row.addView(launch, launchLp);

        card.addView(row, matchWidth());
        refreshSelectedInstance();
        return card;
    }

    private void refreshSelectedInstance() {
        try {
            Instances instances = Instances.loadDisplay();
            DisplayInstance selected = instances.list.get(instances.selectedIndex);
            if (mInstanceName != null) {
                String name = Tools.isValidString(selected.name) ? selected.name : "Minecraft";
                mInstanceName.setText(name);
            }
            if (mInstanceVersion != null) {
                String version = Tools.isValidString(selected.versionId) ? selected.versionId : "unknown version";
                mInstanceVersion.setText(version);
            }
        } catch (IOException | IndexOutOfBoundsException e) {
            if (mInstanceName != null) mInstanceName.setText("No instance");
            if (mInstanceVersion != null) mInstanceVersion.setText("Create one in Versions");
        }
    }

    private void showInstanceChooser() {
        if (!isAdded()) return;
        try {
            final Instances instances = Instances.loadDisplay();
            if (instances.list.isEmpty()) {
                Toast.makeText(requireContext(), "No instances yet. Open Versions to create one.",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            String[] labels = new String[instances.list.size()];
            for (int i = 0; i < labels.length; i++) {
                DisplayInstance instance = instances.list.get(i);
                String name = Tools.isValidString(instance.name) ? instance.name : "Instance " + (i + 1);
                String version = Tools.isValidString(instance.versionId) ? instance.versionId : "?";
                labels[i] = name + "  -  " + version + (i == instances.selectedIndex ? "  (active)" : "");
            }
            new AlertDialog.Builder(requireContext())
                    .setTitle("Select instance")
                    .setSingleChoiceItems(labels, instances.selectedIndex, (dialog, which) -> {
                        Instances.setSelectedInstance(instances.list.get(which));
                        dialog.dismiss();
                        refreshSelectedInstance();
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        } catch (IOException e) {
            Toast.makeText(requireContext(), "Could not read instances: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void launchMinecraft() {
        try {
            ExtraCore.setValue(ExtraConstants.LAUNCH_GAME, true);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Unable to start Minecraft", Toast.LENGTH_SHORT).show();
        }
    }
}
