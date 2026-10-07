package net.kdt.pojavlaunch.rkb.ui;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Space;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import net.kdt.pojavlaunch.R;

public class RkbHomeFragment extends Fragment {

    private static final int BG = Color.rgb(7, 9, 16);
    private static final int PANEL = Color.rgb(8, 16, 27);
    private static final int TEXT = Color.rgb(235, 244, 252);
    private static final int MUTED = Color.rgb(135, 160, 184);
    private static final int BLUE = Color.rgb(0, 168, 255);

    private LinearLayout content;
    private TextView homeNav;
    private TextView cursorNav;
    private TextView modsNav;
    private TextView controlsNav;
    private TextView aboutNav;
    private TextView skinNav;
    private TextView settingsNav;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull android.view.LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.HORIZONTAL);
        root.setBackgroundColor(BG);

        // ============================================================
        // LEFT SIDEBAR
        // ============================================================

        LinearLayout sidebar = new LinearLayout(requireContext());
        sidebar.setOrientation(LinearLayout.VERTICAL);
        sidebar.setGravity(Gravity.CENTER_HORIZONTAL);
        sidebar.setPadding(dp(10), dp(16), dp(10), dp(12));
        sidebar.setBackgroundResource(R.drawable.bg_rkb_sidebar);

        root.addView(
                sidebar,
                new LinearLayout.LayoutParams(dp(78), -1)
        );

        // RKB LOGO
        TextView logo = new TextView(requireContext());
        logo.setText("RKB");
        logo.setTextColor(BLUE);
        logo.setTextSize(21);
        logo.setTypeface(Typeface.DEFAULT_BOLD);
        logo.setGravity(Gravity.CENTER);
        logo.setBackgroundResource(R.drawable.bg_rkb_logo);

        sidebar.addView(
                logo,
                new LinearLayout.LayoutParams(dp(56), dp(56))
        );

        sidebar.addView(space(18));

        homeNav = nav("⌂", "Home");
        cursorNav = nav("✦", "Cursor");
        modsNav = nav("M", "Mods");
        controlsNav = nav("⌁", "Controls");
        aboutNav = nav("i", "About");
        skinNav = nav("S", "Skin");
        settingsNav = nav("⚙", "Settings");

        sidebar.addView(homeNav);
        sidebar.addView(space(7));
        sidebar.addView(cursorNav);
        sidebar.addView(space(7));
        sidebar.addView(modsNav);
        sidebar.addView(space(7));
        sidebar.addView(controlsNav);
        sidebar.addView(space(7));
        sidebar.addView(aboutNav);
        sidebar.addView(space(7));
        sidebar.addView(skinNav);

        Space sideBottom = new Space(requireContext());
        sidebar.addView(
                sideBottom,
                new LinearLayout.LayoutParams(1, 0, 1)
        );

        sidebar.addView(settingsNav);

        homeNav.setSelected(true);

        homeNav.setOnClickListener(v -> showHome());
        cursorNav.setOnClickListener(v -> showPage("CURSOR STUDIO",
                "Customize your launcher cursor and pointer."));
        modsNav.setOnClickListener(v -> showPage("MODS",
                "Manage your installed Minecraft mods."));
        controlsNav.setOnClickListener(v -> showPage("CONTROLS",
                "Configure your touch and game controls."));
        aboutNav.setOnClickListener(v -> showPage("ABOUT",
                "RKB LAUNCHER — Minecraft Android launcher."));
        skinNav.setOnClickListener(v -> showPage("SKIN",
                "Manage your Minecraft skin."));
        settingsNav.setOnClickListener(v -> showPage("SETTINGS",
                "Launcher settings and preferences."));

        // ============================================================
        // MAIN AREA
        // ============================================================

        ScrollView scroll = new ScrollView(requireContext());
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);

        content = new LinearLayout(requireContext());
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(22), dp(18), dp(22), dp(24));

        scroll.addView(content);

        root.addView(
                scroll,
                new LinearLayout.LayoutParams(0, -1, 1)
        );

        showHome();

        return root;
    }

    // ================================================================
    // NEW HOME UI
    // ================================================================

    private void showHome() {

        content.removeAllViews();

        // TOP HEADER
        LinearLayout header = new LinearLayout(requireContext());
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(20), dp(14), dp(18), dp(14));
        header.setBackgroundResource(R.drawable.bg_rkb_header);

        LinearLayout titleBox = new LinearLayout(requireContext());
        titleBox.setOrientation(LinearLayout.VERTICAL);

        TextView title = text("RKB LAUNCHER", 22, TEXT, true);

        TextView subtitle = text(
                "YOUR MINECRAFT EXPERIENCE",
                10,
                MUTED,
                false
        );

        titleBox.addView(title);
        titleBox.addView(space(2));
        titleBox.addView(subtitle);

        header.addView(
                titleBox,
                new LinearLayout.LayoutParams(0, -2, 1)
        );

        TextView status = text("●  READY", 11,
                Color.rgb(50, 220, 145), true);

        status.setGravity(Gravity.CENTER);
        status.setPadding(dp(14), dp(8), dp(14), dp(8));
        status.setBackgroundResource(R.drawable.bg_rkb_status);

        header.addView(
                status,
                new LinearLayout.LayoutParams(-2, dp(38))
        );

        content.addView(header);

        content.addView(space(18));

        // ACCOUNT
        LinearLayout account = new LinearLayout(requireContext());
        account.setGravity(Gravity.CENTER_VERTICAL);
        account.setPadding(dp(18), dp(14), dp(18), dp(14));
        account.setBackgroundResource(R.drawable.bg_rkb_account);

        TextView avatar = text("R", 22, BLUE, true);
        avatar.setGravity(Gravity.CENTER);

        LinearLayout.LayoutParams avatarParams =
                new LinearLayout.LayoutParams(dp(52), dp(52));

        account.addView(avatar, avatarParams);

        account.addView(spaceH(14));

        LinearLayout accountInfo = new LinearLayout(requireContext());
        accountInfo.setOrientation(LinearLayout.VERTICAL);

        accountInfo.addView(
                text("RKB PLAYER", 16, TEXT, true)
        );

        accountInfo.addView(space(3));

        accountInfo.addView(
                text("Microsoft Account", 11, MUTED, false)
        );

        account.addView(
                accountInfo,
                new LinearLayout.LayoutParams(0, -2, 1)
        );

        TextView more = new TextView(requireContext());
        more.setText("•••");
        more.setTextSize(20);
        more.setTextColor(MUTED);
        more.setGravity(Gravity.CENTER);

        account.addView(
                more,
                new LinearLayout.LayoutParams(dp(40), dp(48))
        );

        content.addView(
                account,
                new LinearLayout.LayoutParams(-1, dp(82))
        );

        content.addView(space(20));

        // WELCOME
        content.addView(
                text("Welcome back, RKB", 27, TEXT, true)
        );

        content.addView(space(5));

        content.addView(
                text(
                        "Select an instance and launch Minecraft.",
                        13,
                        MUTED,
                        false
                )
        );

        content.addView(space(18));

        // ============================================================
        // INSTANCE ROW
        // ============================================================

        LinearLayout instanceRow = new LinearLayout(requireContext());
        instanceRow.setOrientation(LinearLayout.HORIZONTAL);

        LinearLayout instance = new LinearLayout(requireContext());
        instance.setOrientation(LinearLayout.VERTICAL);
        instance.setPadding(dp(18), dp(16), dp(18), dp(16));
        instance.setBackgroundResource(R.drawable.bg_rkb_instance);

        LinearLayout instanceTop = new LinearLayout(requireContext());
        instanceTop.setGravity(Gravity.CENTER_VERTICAL);

        TextView minecraft = text(
                "MINECRAFT",
                17,
                TEXT,
                true
        );

        instanceTop.addView(
                minecraft,
                new LinearLayout.LayoutParams(0, -2, 1)
        );

        TextView version = text(
                "1.21.1",
                11,
                BLUE,
                true
        );

        version.setPadding(dp(12), dp(6), dp(12), dp(6));
        version.setBackgroundResource(R.drawable.bg_rkb_version);

        instanceTop.addView(version);

        instance.addView(instanceTop);

        instance.addView(space(9));

        instance.addView(
                text(
                        "Fabric • Optimized",
                        12,
                        MUTED,
                        false
                )
        );

        instance.addView(space(13));

        LinearLayout progress = new LinearLayout(requireContext());
        progress.setGravity(Gravity.CENTER_VERTICAL);

        TextView ready = text(
                "READY TO PLAY",
                10,
                Color.rgb(50, 220, 145),
                true
        );

        progress.addView(
                ready,
                new LinearLayout.LayoutParams(0, -2, 1)
        );

        TextView arrow = text("›", 24, BLUE, false);
        progress.addView(arrow);

        instance.addView(progress);

        instance.setOnClickListener(v ->
                Toast.makeText(
                        requireContext(),
                        "Minecraft 1.21.1 selected",
                        Toast.LENGTH_SHORT
                ).show()
        );

        instanceRow.addView(
                instance,
                new LinearLayout.LayoutParams(0, dp(150), 1)
        );

        instanceRow.addView(spaceH(12));

        // NEW INSTANCE
        LinearLayout newInstance = new LinearLayout(requireContext());
        newInstance.setOrientation(LinearLayout.VERTICAL);
        newInstance.setGravity(Gravity.CENTER);
        newInstance.setBackgroundResource(R.drawable.bg_rkb_new_instance);

        ImageView add = new ImageView(requireContext());
        add.setImageResource(R.drawable.ic_rkb_add);

        newInstance.addView(
                add,
                new LinearLayout.LayoutParams(dp(34), dp(34))
        );

        newInstance.addView(space(8));

        TextView newText = text(
                "NEW INSTANCE",
                11,
                BLUE,
                true
        );

        newInstance.addView(newText);

        newInstance.setOnClickListener(v ->
                Toast.makeText(
                        requireContext(),
                        "New instance",
                        Toast.LENGTH_SHORT
                ).show()
        );

        instanceRow.addView(
                newInstance,
                new LinearLayout.LayoutParams(dp(145), dp(150))
        );

        content.addView(instanceRow);

        content.addView(space(20));

        // ============================================================
        // LAUNCH AREA
        // ============================================================

        LinearLayout launchCard = new LinearLayout(requireContext());
        launchCard.setOrientation(LinearLayout.VERTICAL);
        launchCard.setGravity(Gravity.CENTER);
        launchCard.setPadding(dp(20), dp(20), dp(20), dp(20));
        launchCard.setBackgroundResource(R.drawable.bg_rkb_card);

        TextView selected = text(
                "MINECRAFT 1.21.1",
                12,
                MUTED,
                true
        );

        launchCard.addView(selected);

        launchCard.addView(space(12));

        TextView launch = text(
                "LAUNCH",
                18,
                Color.WHITE,
                true
        );

        launch.setGravity(Gravity.CENTER);
        launch.setBackgroundResource(R.drawable.bg_rkb_launch);
        launch.setCompoundDrawablesWithIntrinsicBounds(
                R.drawable.ic_rkb_play,
                0,
                0,
                0
        );
        launch.setCompoundDrawablePadding(dp(10));

        launch.setOnClickListener(v -> launchMinecraft());

        launchCard.addView(
                launch,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(64)
                )
        );

        content.addView(
                launchCard,
                new LinearLayout.LayoutParams(-1, dp(140))
        );

        content.addView(space(18));

        // BOTTOM INFO
        LinearLayout infoRow = new LinearLayout(requireContext());

        infoRow.addView(
                infoCard("VERSION", "RKB Launcher 1.0"),
                new LinearLayout.LayoutParams(0, dp(82), 1)
        );

        infoRow.addView(spaceH(10));

        infoRow.addView(
                infoCard("ENGINE", "Pojav Engine"),
                new LinearLayout.LayoutParams(0, dp(82), 1)
        );

        infoRow.addView(spaceH(10));

        infoRow.addView(
                infoCard("STATUS", "Optimized"),
                new LinearLayout.LayoutParams(0, dp(82), 1)
        );

        content.addView(infoRow);
    }

    // ================================================================
    // OTHER PAGES
    // ================================================================

    private void showPage(String title, String description) {

        content.removeAllViews();

        TextView heading = text(title, 28, TEXT, true);
        content.addView(heading);

        content.addView(space(6));

        content.addView(
                text(description, 13, MUTED, false)
        );

        content.addView(space(22));

        LinearLayout card = new LinearLayout(requireContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(22), dp(22), dp(22), dp(22));
        card.setBackgroundResource(R.drawable.bg_rkb_card);

        card.addView(
                text(title + " PANEL", 18, TEXT, true)
        );

        card.addView(space(10));

        card.addView(
                text(
                        "This section is ready for the real launcher functionality.",
                        13,
                        MUTED,
                        false
                )
        );

        content.addView(card);
    }

    // ================================================================
    // NAV ITEM
    // ================================================================

    private TextView nav(String icon, String name) {

        TextView item = new TextView(requireContext());

        item.setText(icon);
        item.setTextSize(20);
        item.setTextColor(TEXT);
        item.setGravity(Gravity.CENTER);
        item.setTypeface(Typeface.DEFAULT_BOLD);
        item.setBackgroundResource(R.drawable.bg_rkb_nav_item_active);

        item.setContentDescription(name);

        item.setOnClickListener(v -> {
            homeNav.setSelected(false);
            cursorNav.setSelected(false);
            modsNav.setSelected(false);
            controlsNav.setSelected(false);
            aboutNav.setSelected(false);
            skinNav.setSelected(false);
            settingsNav.setSelected(false);

            v.setSelected(true);
        });

        return item;
    }

    // ================================================================
    // INFO CARD
    // ================================================================

    private View infoCard(String title, String value) {

        LinearLayout card = new LinearLayout(requireContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(15), dp(10), dp(15), dp(10));
        card.setBackgroundResource(R.drawable.bg_rkb_card);

        card.addView(
                text(title, 9, MUTED, true)
        );

        card.addView(space(4));

        card.addView(
                text(value, 12, TEXT, true)
        );

        return card;
    }

    // ================================================================
    // LAUNCH
    // ================================================================

    private void launchMinecraft() {

        try {

            Class<?> extraCore =
                    Class.forName("net.kdt.pojavlaunch.prefs.ExtraCore");

            Class<?> extraConstants =
                    Class.forName("net.kdt.pojavlaunch.prefs.ExtraConstants");

            java.lang.reflect.Field field =
                    extraConstants.getField("LAUNCH_GAME");

            Object launchKey = field.get(null);

            java.lang.reflect.Method method =
                    extraCore.getMethod(
                            "setValue",
                            launchKey.getClass(),
                            boolean.class
                    );

            method.invoke(
                    null,
                    launchKey,
                    true
            );

        } catch (Exception e) {

            Toast.makeText(
                    requireContext(),
                    "Launch system is not connected yet",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    // ================================================================
    // UI HELPERS
    // ================================================================

    private TextView text(
            String value,
            float size,
            int color,
            boolean bold) {

        TextView t = new TextView(requireContext());

        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);

        if (bold) {
            t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        }

        return t;
    }

    private Space space(int dp) {
        Space s = new Space(requireContext());
        s.setLayoutParams(
                new LinearLayout.LayoutParams(1, dp(dp))
        );
        return s;
    }

    private Space spaceH(int dp) {
        Space s = new Space(requireContext());
        s.setLayoutParams(
                new LinearLayout.LayoutParams(dp(dp), 1)
        );
        return s;
    }

    private int dp(int value) {
        return Math.round(
                value * getResources()
                        .getDisplayMetrics()
                        .density
        );
    }
}