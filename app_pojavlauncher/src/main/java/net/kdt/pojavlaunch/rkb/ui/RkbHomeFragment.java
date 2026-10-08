package net.kdt.pojavlaunch.rkb.ui;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import net.kdt.pojavlaunch.CustomControlsActivity;
import net.kdt.pojavlaunch.LauncherActivity;
import net.kdt.pojavlaunch.PojavProfile;
import net.kdt.pojavlaunch.fragments.SelectAuthFragment;
import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.extra.ExtraConstants;
import net.kdt.pojavlaunch.extra.ExtraCore;
import net.kdt.pojavlaunch.prefs.screens.LauncherPreferenceFragment;
import net.kdt.pojavlaunch.value.MinecraftAccount;

/**
 * RKB Launcher home.  Lightweight programmatic UI; no large background images
 * or animated effects are used so the launcher itself stays responsive.
 */
public class RkbHomeFragment extends Fragment {

    private static final int BG = 0xFF05080F;
    private static final int PANEL = 0xFF0A111C;
    private static final int PANEL_2 = 0xFF0D1724;
    private static final int BLUE = 0xFF00A8FF;
    private static final int BLUE_DARK = 0xFF075B91;
    private static final int WHITE = 0xFFF4F8FC;
    private static final int MUTED = 0xFF8294A8;
    private static final String DISCORD_URL = "https://discord.gg/M2FskvuRJ7";

    private FrameLayout root;
    private LinearLayout center;
    private LinearLayout rightPanel;

    @Nullable
    @Override
    public View onCreateView(@NonNull android.view.LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        root = new FrameLayout(requireContext());
        root.setBackgroundColor(BG);
        root.setClipToPadding(false);
        buildUi();
        return root;
    }

    private void buildUi() {
        root.removeAllViews();

        final LinearLayout sidebar = new LinearLayout(requireContext());
        sidebar.setOrientation(LinearLayout.VERTICAL);
        sidebar.setGravity(Gravity.CENTER_HORIZONTAL);
        sidebar.setPadding(dp(8), dp(12), dp(8), dp(12));
        sidebar.setBackgroundResource(R.drawable.bg_rkb_sidebar);

        FrameLayout.LayoutParams sideLp = new FrameLayout.LayoutParams(
                dp(76), ViewGroup.LayoutParams.MATCH_PARENT, Gravity.START);
        root.addView(sidebar, sideLp);

        addNav(sidebar, "⌂", "Home", true, v -> showHome());
        addNav(sidebar, "⌁", "Cursor", false, v -> openFragment(CursorStudioFragment.class));
        addNav(sidebar, "▦", "Mods", false, v -> openFragment(ModManagerFragment.class));
        addNav(sidebar, "◈", "Controls", false, v -> openControls());
        addNav(sidebar, "i", "About", false, v -> showAbout());
        addNav(sidebar, "◇", "Skin", false, v -> showSkinDialog());
        addNav(sidebar, "⚙", "Settings", false, v -> openFragment(LauncherPreferenceFragment.class));

        center = new LinearLayout(requireContext());
        center.setOrientation(LinearLayout.VERTICAL);
        center.setPadding(dp(14), dp(8), dp(10), dp(10));

        FrameLayout.LayoutParams centerLp = new FrameLayout.LayoutParams(
                0, ViewGroup.LayoutParams.MATCH_PARENT);
        centerLp.leftMargin = dp(76);
        centerLp.rightMargin = dp(360);
        root.addView(center, centerLp);

        buildHeader();
        buildHero();
        buildBottomInstances();

        rightPanel = new LinearLayout(requireContext());
        rightPanel.setOrientation(LinearLayout.VERTICAL);
        rightPanel.setPadding(dp(10), dp(8), dp(14), dp(12));

        FrameLayout.LayoutParams rightLp = new FrameLayout.LayoutParams(
                dp(360), ViewGroup.LayoutParams.MATCH_PARENT, Gravity.END);
        root.addView(rightPanel, rightLp);

        buildAccount();
        buildPlayerAndLaunch();
    }

    private void showHome() {
        // Home is already visible. Keeping this a no-op avoids recreating the view.
    }

    private void buildHeader() {
        LinearLayout header = panelRow(dp(66));

        TextView logo = text("R", 24, BLUE, true);
        logo.setGravity(Gravity.CENTER);
        logo.setBackgroundResource(R.drawable.bg_rkb_logo);
        header.addView(logo, new LinearLayout.LayoutParams(dp(48), dp(48)));
        header.addView(spaceH(10));

        LinearLayout brand = new LinearLayout(requireContext());
        brand.setOrientation(LinearLayout.VERTICAL);
        brand.addView(text("RKB LAUNCHER", 17, WHITE, true));
        brand.addView(text("PLAY  •  EXPLORE  •  CREATE", 8, MUTED, true));
        header.addView(brand, new LinearLayout.LayoutParams(0, -2, 1f));

        TextView discord = actionButton("Discord", 0xFF5865F2);
        discord.setOnClickListener(v -> openUrl(DISCORD_URL));
        header.addView(discord, new LinearLayout.LayoutParams(dp(105), dp(42)));

        center.addView(header);
    }

    private void buildHero() {
        LinearLayout hero = new LinearLayout(requireContext());
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setGravity(Gravity.CENTER);
        hero.setPadding(dp(18), dp(18), dp(18), dp(10));

        LinearLayout.LayoutParams heroLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        heroLp.topMargin = dp(10);
        center.addView(hero, heroLp);

        hero.addView(text("WELCOME TO", 10, MUTED, true));
        TextView title = text("RKB LAUNCHER", 30, WHITE, true);
        title.setGravity(Gravity.CENTER);
        hero.addView(title);
        hero.addView(text("YOUR MINECRAFT EXPERIENCE", 10, MUTED, true));
        hero.addView(space(18));

        LinearLayout featureRow = new LinearLayout(requireContext());
        featureRow.setGravity(Gravity.CENTER);
        featureRow.addView(feature("FAST"));
        featureRow.addView(feature("SECURE"));
        featureRow.addView(feature("OPTIMIZED"));
        hero.addView(featureRow);
        hero.addView(space(20));

        TextView launch = text("▶   LAUNCH", 21, WHITE, true);
        launch.setGravity(Gravity.CENTER);
        launch.setBackgroundResource(R.drawable.bg_rkb_launch);
        launch.setOnClickListener(v -> launchMinecraft());
        hero.addView(launch, new LinearLayout.LayoutParams(dp(250), dp(64)));
    }

    private TextView feature(String label) {
        TextView v = text("•  " + label, 10, BLUE, true);
        v.setPadding(dp(10), 0, dp(10), 0);
        return v;
    }

    private void buildAccount() {
        LinearLayout account = panelRow(dp(64));
        TextView avatar = text("R", 18, WHITE, true);
        avatar.setGravity(Gravity.CENTER);
        avatar.setBackground(rounded(0xFF202B38, 18));
        account.addView(avatar, new LinearLayout.LayoutParams(dp(44), dp(44)));
        account.addView(spaceH(10));

        LinearLayout info = new LinearLayout(requireContext());
        info.setOrientation(LinearLayout.VERTICAL);
        MinecraftAccount acc = currentAccount();
        String name = acc == null || acc.username == null || acc.username.isEmpty()
                ? "Add account" : acc.username;
        info.addView(text(name, 15, WHITE, true));
        info.addView(text(acc == null ? "LOCAL" : (acc.isLocal() ? "LOCAL" : "MICROSOFT"),
                9, BLUE, true));
        account.addView(info, new LinearLayout.LayoutParams(0, -2, 1f));

        TextView arrow = text("⌄", 18, MUTED, true);
        account.addView(arrow);
        account.setOnClickListener(v -> openAccountChooser());
        rightPanel.addView(account);
    }

    private void buildPlayerAndLaunch() {
        LinearLayout body = new LinearLayout(requireContext());
        body.setOrientation(LinearLayout.VERTICAL);
        body.setGravity(Gravity.CENTER_HORIZONTAL);
        body.setPadding(dp(14), dp(16), dp(14), dp(8));

        TextView player = text("PLAYER", 10, MUTED, true);
        player.setGravity(Gravity.CENTER);
        body.addView(player);
        body.addView(space(10));

        // Lightweight avatar placeholder; real account face is not decoded on the UI thread.
        TextView avatar = text("R", 52, BLUE, true);
        avatar.setGravity(Gravity.CENTER);
        avatar.setBackground(rounded(PANEL_2, 36));
        body.addView(avatar, new LinearLayout.LayoutParams(dp(112), dp(112)));
        body.addView(space(14));

        TextView launch = text("▶  LAUNCH", 20, WHITE, true);
        launch.setGravity(Gravity.CENTER);
        launch.setBackgroundResource(R.drawable.bg_rkb_launch);
        launch.setOnClickListener(v -> launchMinecraft());
        body.addView(launch, new LinearLayout.LayoutParams(-1, dp(66)));
        body.addView(space(10));

        LinearLayout version = panelRow(dp(52));
        version.addView(text("▶", 15, BLUE, true));
        version.addView(spaceH(10));
        version.addView(text("Minecraft 1.21.11", 14, WHITE, true),
                new LinearLayout.LayoutParams(0, -2, 1f));
        version.addView(text("⌄", 16, MUTED, true));
        version.setOnClickListener(v -> Toast.makeText(requireContext(),
                "Version selection is handled by the existing launcher profile system.",
                Toast.LENGTH_SHORT).show());
        body.addView(version);

        LinearLayout.LayoutParams bodyLp = new LinearLayout.LayoutParams(-1, 0, 1f);
        bodyLp.topMargin = dp(10);
        rightPanel.addView(body, bodyLp);
    }

    private void buildBottomInstances() {
        LinearLayout row = new LinearLayout(requireContext());
        row.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout instance = panelRow(dp(76));
        TextView block = text("■", 27, 0xFF65C84A, true);
        block.setGravity(Gravity.CENTER);
        instance.addView(block, new LinearLayout.LayoutParams(dp(46), dp(46)));
        instance.addView(spaceH(8));
        LinearLayout info = new LinearLayout(requireContext());
        info.setOrientation(LinearLayout.VERTICAL);
        info.addView(text("Minecraft...", 13, WHITE, true));
        info.addView(text("1.21.11", 10, MUTED, false));
        instance.addView(info, new LinearLayout.LayoutParams(0, -2, 1f));

        TextView play = text("▶", 16, BLUE, true);
        play.setGravity(Gravity.CENTER);
        play.setOnClickListener(v -> launchMinecraft());
        instance.addView(play, new LinearLayout.LayoutParams(dp(44), dp(44)));
        row.addView(instance, new LinearLayout.LayoutParams(0, dp(76), 1f));

        row.addView(spaceH(10));
        TextView add = text("＋  New Instance", 12, BLUE, true);
        add.setGravity(Gravity.CENTER);
        add.setBackgroundResource(R.drawable.bg_rkb_new_instance);
        add.setOnClickListener(v -> openAccountChooser());
        row.addView(add, new LinearLayout.LayoutParams(dp(170), dp(76)));

        center.addView(row, new LinearLayout.LayoutParams(-1, dp(86)));
    }

    private void addNav(LinearLayout parent, String icon, String label,
                        boolean active, View.OnClickListener listener) {
        LinearLayout item = new LinearLayout(requireContext());
        item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(Gravity.CENTER);
        item.setPadding(dp(4), dp(5), dp(4), dp(5));
        item.setBackgroundResource(active ? R.drawable.bg_rkb_nav_item_active : R.drawable.bg_rkb_nav_item);
        item.setOnClickListener(listener);

        TextView iconView = text(icon, 19, active ? WHITE : MUTED, true);
        iconView.setGravity(Gravity.CENTER);
        item.addView(iconView);
        TextView labelView = text(label, 8, active ? WHITE : MUTED, true);
        labelView.setGravity(Gravity.CENTER);
        item.addView(labelView);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(58));
        lp.bottomMargin = dp(6);
        parent.addView(item, lp);
    }

    private void openFragment(Class<? extends Fragment> clazz) {
        if (!(requireActivity() instanceof LauncherActivity)) return;
        ((LauncherActivity) requireActivity()).swapFragment(clazz);
    }

    private void openControls() {
        try {
            startActivity(new Intent(requireContext(), CustomControlsActivity.class));
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Controls could not be opened", Toast.LENGTH_SHORT).show();
        }
    }

    private void showAbout() {
        new AlertDialog.Builder(requireContext())
                .setTitle("RKB LAUNCHER")
                .setMessage("RKB GAMERZ\n\nA lightweight RKB-themed Minecraft launcher interface.\n\nDiscord community is available from the header.")
                .setPositiveButton("Discord", (d, w) -> openUrl(DISCORD_URL))
                .setNegativeButton("Close", null)
                .show();
    }

    private void openUrl(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception e) {
            Toast.makeText(requireContext(), "No browser available", Toast.LENGTH_SHORT).show();
        }
    }

    private void showSkinDialog() {
        MinecraftAccount account = currentAccount();
        if (account == null) {
            Toast.makeText(requireContext(), "Select or add an account first", Toast.LENGTH_SHORT).show();
            return;
        }

        LinearLayout box = new LinearLayout(requireContext());
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(6), dp(20), 0);

        EditText skin = new EditText(requireContext());
        skin.setHint("Skin URL (https://...)");
        skin.setSingleLine(true);
        skin.setText(account.skinUrl == null ? "" : account.skinUrl);
        skin.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        box.addView(skin, new LinearLayout.LayoutParams(-1, dp(52)));

        EditText cape = new EditText(requireContext());
        cape.setHint("Cape URL (https://...)");
        cape.setSingleLine(true);
        cape.setText(account.capeUrl == null ? "" : account.capeUrl);
        cape.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        box.addView(cape, new LinearLayout.LayoutParams(-1, dp(52)));

        TextView note = text("Skin URL uses the existing skin URL system. Cape URL is saved to the account for supported cape integrations.",
                10, MUTED, false);
        note.setPadding(0, dp(8), 0, 0);
        box.addView(note);

        new AlertDialog.Builder(requireContext())
                .setTitle("RKB Skin & Cape")
                .setView(box)
                .setPositiveButton("SAVE", (d, w) -> {
                    String skinUrl = skin.getText().toString().trim();
                    String capeUrl = cape.getText().toString().trim();
                    account.skinUrl = skinUrl.isEmpty() ? null : skinUrl;
                    account.capeUrl = capeUrl.isEmpty() ? null : capeUrl;
                    try {
                        account.save();
                        Toast.makeText(requireContext(), "Skin/Cape settings saved", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Toast.makeText(requireContext(), "Could not save skin settings", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("CANCEL", null)
                .show();
    }

    private void openAccountChooser() {
        if (requireActivity() instanceof LauncherActivity) {
            ((LauncherActivity) requireActivity()).swapFragment(SelectAuthFragment.class);
        } else {
            Toast.makeText(requireContext(), "Account manager could not be opened", Toast.LENGTH_SHORT).show();
        }
    }

    private MinecraftAccount currentAccount() {
        try {
            String name = PojavProfile.getCurrentProfileName(requireContext());
            if (name == null || name.isEmpty()) return null;
            return MinecraftAccount.load(name);
        } catch (Exception e) {
            return null;
        }
    }

    private void launchMinecraft() {
        try {
            ExtraCore.setValue(ExtraConstants.LAUNCH_GAME, true);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Unable to start Minecraft", Toast.LENGTH_SHORT).show();
        }
    }

    private LinearLayout panelRow(int height) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12), dp(7), dp(12), dp(7));
        row.setBackgroundResource(R.drawable.bg_rkb_card);
        return row;
    }

    private TextView actionButton(String value, int color) {
        TextView b = text(value, 11, WHITE, true);
        b.setGravity(Gravity.CENTER);
        b.setBackground(rounded(color, 20));
        return b;
    }

    private TextView text(String value, float size, int color, boolean bold) {
        TextView t = new TextView(requireContext());
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private Space space(int value) {
        Space s = new Space(requireContext());
        s.setLayoutParams(new LinearLayout.LayoutParams(1, dp(value)));
        return s;
    }

    private Space spaceH(int value) {
        Space s = new Space(requireContext());
        s.setLayoutParams(new LinearLayout.LayoutParams(dp(value), 1));
        return s;
    }

    private GradientDrawable rounded(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        return d;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
