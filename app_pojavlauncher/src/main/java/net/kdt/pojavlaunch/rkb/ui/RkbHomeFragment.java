package net.kdt.pojavlaunch.rkb.ui;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.extra.ExtraConstants;
import net.kdt.pojavlaunch.extra.ExtraCore;

public class RkbHomeFragment extends Fragment {

    private static final int BG = Color.rgb(5, 7, 12);
    private static final int PANEL = Color.rgb(7, 13, 22);
    private static final int PANEL_2 = Color.rgb(9, 18, 30);
    private static final int BLUE = Color.rgb(0, 168, 255);
    private static final int BLUE_2 = Color.rgb(27, 96, 255);
    private static final int WHITE = Color.WHITE;
    private static final int MUTED = Color.rgb(150, 168, 190);

    private FrameLayout root;
    private LinearLayout sidebar;
    private LinearLayout center;
    private LinearLayout rightPanel;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull android.view.LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        root = new FrameLayout(requireContext());
        root.setBackgroundColor(BG);

        buildUi();

        return root;
    }

    private void buildUi() {

        root.removeAllViews();

        // =========================================================
        // LEFT SIDEBAR
        // =========================================================

        sidebar = new LinearLayout(requireContext());
        sidebar.setOrientation(LinearLayout.VERTICAL);
        sidebar.setGravity(Gravity.CENTER_HORIZONTAL);
        sidebar.setPadding(dp(10), dp(14), dp(10), dp(14));
        sidebar.setBackgroundResource(R.drawable.bg_rkb_sidebar);

        FrameLayout.LayoutParams sideParams =
                new FrameLayout.LayoutParams(
                        dp(78),
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        Gravity.LEFT
                );

        root.addView(sidebar, sideParams);

        // HOME
        addNav(
                sidebar,
                R.drawable.ic_rkb_play,
                true
        );

        // CURSOR
        addNav(
                sidebar,
                android.R.drawable.ic_menu_crop,
                false
        );

        // MODS
        addNav(
                sidebar,
                android.R.drawable.ic_menu_manage,
                false
        );

        // CONTROLS
        addNav(
                sidebar,
                android.R.drawable.ic_media_play,
                false
        );

        // ABOUT
        addNav(
                sidebar,
                android.R.drawable.ic_menu_info_details,
                false
        );

        // SETTINGS
        addNav(
                sidebar,
                android.R.drawable.ic_menu_preferences,
                false
        );

        // =========================================================
        // CENTER
        // =========================================================

        center = new LinearLayout(requireContext());
        center.setOrientation(LinearLayout.VERTICAL);
        center.setPadding(
                dp(18),
                dp(8),
                dp(12),
                dp(10)
        );

        FrameLayout.LayoutParams centerParams =
                new FrameLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.MATCH_PARENT
                );

        centerParams.leftMargin = dp(78);
        centerParams.rightMargin = dp(430);

        root.addView(center, centerParams);

        buildHeader();
        buildCenterSpace();
        buildInstanceRow();

        // =========================================================
        // RIGHT AREA
        // =========================================================

        rightPanel = new LinearLayout(requireContext());
        rightPanel.setOrientation(LinearLayout.VERTICAL);
        rightPanel.setPadding(
                dp(12),
                dp(8),
                dp(18),
                dp(14)
        );

        FrameLayout.LayoutParams rightParams =
                new FrameLayout.LayoutParams(
                        dp(430),
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        Gravity.RIGHT
                );

        root.addView(rightPanel, rightParams);

        buildAccount();
        buildPlayerArea();
    }

    // =============================================================
    // HEADER
    // =============================================================

    private void buildHeader() {

        LinearLayout header = new LinearLayout(requireContext());
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(12), dp(8), dp(12), dp(8));
        header.setBackgroundResource(R.drawable.bg_rkb_header);

        LinearLayout logoBox = new LinearLayout(requireContext());
        logoBox.setGravity(Gravity.CENTER_VERTICAL);

        TextView rLogo = new TextView(requireContext());
        rLogo.setText("R");
        rLogo.setTextColor(BLUE);
        rLogo.setTextSize(25);
        rLogo.setTypeface(Typeface.DEFAULT_BOLD);
        rLogo.setGravity(Gravity.CENTER);
        rLogo.setBackgroundResource(R.drawable.bg_rkb_logo);

        logoBox.addView(
                rLogo,
                new LinearLayout.LayoutParams(
                        dp(52),
                        dp(52)
                )
        );

        logoBox.addView(spaceH(10));

        LinearLayout logoText = new LinearLayout(requireContext());
        logoText.setOrientation(LinearLayout.VERTICAL);

        TextView launcher = text(
                "RKB LAUNCHER",
                18,
                WHITE,
                true
        );

        TextView slogan = text(
                "PLAY  •  EXPLORE  •  CREATE",
                8,
                MUTED,
                true
        );

        logoText.addView(launcher);
        logoText.addView(space(2));
        logoText.addView(slogan);

        logoBox.addView(logoText);

        header.addView(
                logoBox,
                new LinearLayout.LayoutParams(
                        0,
                        -1,
                        1
                )
        );

        TextView youtube = smallButton(
                "YouTube",
                Color.rgb(235, 35, 45)
        );

        header.addView(
                youtube,
                new LinearLayout.LayoutParams(
                        dp(118),
                        dp(46)
                )
        );

        header.addView(spaceH(8));

        TextView discord = smallButton(
                "Discord",
                Color.rgb(52, 93, 235)
        );

        header.addView(
                discord,
                new LinearLayout.LayoutParams(
                        dp(118),
                        dp(46)
                )
        );

        center.addView(
                header,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(76)
                )
        );
    }

    // =============================================================
    // CENTER EMPTY SPACE
    // =============================================================

    private void buildCenterSpace() {

        Space space = new Space(requireContext());

        center.addView(
                space,
                new LinearLayout.LayoutParams(
                        -1,
                        0,
                        1
                )
        );
    }

    // =============================================================
    // INSTANCE ROW
    // =============================================================

    private void buildInstanceRow() {

        LinearLayout row = new LinearLayout(requireContext());
        row.setGravity(Gravity.CENTER_VERTICAL);

        // EXISTING INSTANCE
        LinearLayout instance = new LinearLayout(requireContext());
        instance.setGravity(Gravity.CENTER_VERTICAL);
        instance.setPadding(
                dp(10),
                dp(8),
                dp(8),
                dp(8)
        );
        instance.setBackgroundResource(R.drawable.bg_rkb_instance);

        TextView accent = new TextView(requireContext());
        accent.setBackgroundColor(BLUE);

        instance.addView(
                accent,
                new LinearLayout.LayoutParams(
                        dp(4),
                        dp(52)
                )
        );

        instance.addView(spaceH(8));

        TextView grass = text(
                "▣",
                30,
                Color.rgb(90, 205, 75),
                true
        );

        grass.setGravity(Gravity.CENTER);

        instance.addView(
                grass,
                new LinearLayout.LayoutParams(
                        dp(54),
                        dp(54)
                )
        );

        instance.addView(spaceH(8));

        LinearLayout instanceText =
                new LinearLayout(requireContext());

        instanceText.setOrientation(
                LinearLayout.VERTICAL
        );

        instanceText.addView(
                text(
                        "Minecraft...",
                        14,
                        WHITE,
                        true
                )
        );

        instanceText.addView(space(3));

        instanceText.addView(
                text(
                        "1.21.11",
                        12,
                        MUTED,
                        false
                )
        );

        instance.addView(
                instanceText,
                new LinearLayout.LayoutParams(
                        dp(120),
                        -2
                )
        );

        instance.addView(spaceH(8));

        ImageView play = new ImageView(requireContext());
        play.setImageResource(R.drawable.ic_rkb_play);
        play.setPadding(dp(9), dp(9), dp(9), dp(9));
        play.setBackgroundResource(
                R.drawable.bg_rkb_nav_item
        );

        play.setOnClickListener(
                v -> launchMinecraft()
        );

        instance.addView(
                play,
                new LinearLayout.LayoutParams(
                        dp(52),
                        dp(52)
                )
        );

        instance.addView(spaceH(4));

        TextView more = text(
                "⋮",
                25,
                WHITE,
                false
        );

        more.setGravity(Gravity.CENTER);

        instance.addView(
                more,
                new LinearLayout.LayoutParams(
                        dp(34),
                        dp(52)
                )
        );

        row.addView(
                instance,
                new LinearLayout.LayoutParams(
                        dp(340),
                        dp(82)
                )
        );

        row.addView(spaceH(18));

        // NEW INSTANCE
        LinearLayout newInstance =
                new LinearLayout(requireContext());

        newInstance.setGravity(Gravity.CENTER);
        newInstance.setBackgroundResource(
                R.drawable.bg_rkb_new_instance
        );

        ImageView plus = new ImageView(requireContext());
        plus.setImageResource(
                R.drawable.ic_rkb_add
        );

        newInstance.addView(
                plus,
                new LinearLayout.LayoutParams(
                        dp(30),
                        dp(30)
                )
        );

        newInstance.addView(spaceH(9));

        newInstance.addView(
                text(
                        "New Instance",
                        12,
                        BLUE,
                        true
                )
        );

        row.addView(
                newInstance,
                new LinearLayout.LayoutParams(
                        dp(290),
                        dp(82)
                )
        );

        center.addView(
                row,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(94)
                )
        );
    }

    // =============================================================
    // ACCOUNT
    // =============================================================

    private void buildAccount() {

        LinearLayout account =
                new LinearLayout(requireContext());

        account.setGravity(Gravity.CENTER_VERTICAL);
        account.setPadding(
                dp(14),
                dp(8),
                dp(10),
                dp(8)
        );

        account.setBackgroundResource(
                R.drawable.bg_rkb_account
        );

        TextView avatar = text(
                "R",
                18,
                WHITE,
                true
        );

        avatar.setGravity(Gravity.CENTER);
        avatar.setBackground(
                rounded(
                        Color.rgb(35, 35, 43),
                        18
                )
        );

        account.addView(
                avatar,
                new LinearLayout.LayoutParams(
                        dp(46),
                        dp(46)
                )
        );

        account.addView(spaceH(12));

        account.addView(
                text(
                        "RKB_GAMERZ",
                        17,
                        WHITE,
                        true
                )
        );

        account.addView(spaceH(12));

        TextView local = text(
                "LOCAL",
                11,
                Color.rgb(190, 150, 255),
                true
        );

        local.setGravity(Gravity.CENTER);
        local.setPadding(
                dp(9),
                0,
                dp(9),
                0
        );

        local.setBackground(
                rounded(
                        Color.rgb(50, 35, 82),
                        14
                )
        );

        account.addView(
                local,
                new LinearLayout.LayoutParams(
                        dp(66),
                        dp(34)
                )
        );

        account.addView(spaceH(4));

        account.addView(
                text(
                        "▼",
                        11,
                        MUTED,
                        true
                )
        );

        rightPanel.addView(
                account,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(70)
                )
        );
    }

    // =============================================================
    // PLAYER + LAUNCH
    // =============================================================

    private void buildPlayerArea() {

        LinearLayout player =
                new LinearLayout(requireContext());

        player.setOrientation(
                LinearLayout.VERTICAL
        );
        player.setGravity(Gravity.CENTER_HORIZONTAL);
        player.setPadding(
                dp(18),
                dp(18),
                dp(18),
                dp(10)
        );

        // PLAYER LABEL
        TextView label = text(
                "Player",
                17,
                WHITE,
                true
        );

        label.setGravity(Gravity.CENTER);
        label.setPadding(
                dp(18),
                dp(8),
                dp(18),
                dp(8)
        );

        label.setBackgroundResource(
                R.drawable.bg_rkb_version
        );

        player.addView(
                label,
                new LinearLayout.LayoutParams(
                        dp(112),
                        dp(48)
                )
        );

        player.addView(space(12));

        // SIMPLE PLAYER PREVIEW
        LinearLayout skin =
                createPlayerPreview();

        player.addView(
                skin,
                new LinearLayout.LayoutParams(
                        dp(150),
                        dp(205)
                )
        );

        player.addView(space(6));

        // LAUNCH BUTTON
        TextView launch =
                new TextView(requireContext());

        launch.setText("▶  LAUNCH");
        launch.setTextSize(22);
        launch.setTextColor(WHITE);
        launch.setTypeface(
                Typeface.DEFAULT,
                Typeface.BOLD
        );
        launch.setGravity(Gravity.CENTER);
        launch.setBackgroundResource(
                R.drawable.bg_rkb_launch
        );

        launch.setOnClickListener(
                v -> launchMinecraft()
        );

        player.addView(
                launch,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(78)
                )
        );

        player.addView(space(14));

        // VERSION SELECTOR
        LinearLayout version =
                new LinearLayout(requireContext());

        version.setGravity(Gravity.CENTER_VERTICAL);
        version.setPadding(
                dp(18),
                0,
                dp(16),
                0
        );

        version.setBackgroundResource(
                R.drawable.bg_rkb_version
        );

        ImageView vPlay =
                new ImageView(requireContext());

        vPlay.setImageResource(
                R.drawable.ic_rkb_play
        );

        version.addView(
                vPlay,
                new LinearLayout.LayoutParams(
                        dp(28),
                        dp(28)
                )
        );

        version.addView(spaceH(12));

        TextView versionName =
                text(
                        "Minecraft 1.21.1",
                        17,
                        WHITE,
                        true
                );

        version.addView(
                versionName,
                new LinearLayout.LayoutParams(
                        0,
                        -2,
                        1
                )
        );

        ImageView down =
                new ImageView(requireContext());

        down.setImageResource(
                R.drawable.ic_rkb_chevron_down
        );

        version.addView(
                down,
                new LinearLayout.LayoutParams(
                        dp(28),
                        dp(28)
                )
        );

        player.addView(
                version,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(58)
                )
        );

        rightPanel.addView(
                player,
                new LinearLayout.LayoutParams(
                        -1,
                        0,
                        1
                )
        );
    }

    // =============================================================
    // PLAYER PREVIEW
    // =============================================================

    private LinearLayout createPlayerPreview() {

        LinearLayout wrapper =
                new LinearLayout(requireContext());

        wrapper.setOrientation(
                LinearLayout.VERTICAL
        );
        wrapper.setGravity(Gravity.CENTER);

        // HEAD
        TextView head = text(
                "■",
                64,
                Color.rgb(205, 150, 105),
                true
        );

        head.setGravity(Gravity.CENTER);

                wrapper.addView(
                head,
                new LinearLayout.LayoutParams(
                        dp(70),
                        dp(62)
                )
        );

        // BODY
        LinearLayout body =
                new LinearLayout(requireContext());

        body.setOrientation(
                LinearLayout.HORIZONTAL
        );

        body.setGravity(Gravity.CENTER);

        TextView leftArm = text(
                "█",
                42,
                Color.rgb(20, 28, 40),
                true
        );

        TextView torso = text(
                "██",
                50,
                Color.rgb(24, 70, 125),
                true
        );

        TextView rightArm = text(
                "█",
                42,
                Color.rgb(20, 28, 40),
                true
        );

        body.addView(leftArm);
        body.addView(torso);
        body.addView(rightArm);

        wrapper.addView(
                body,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(58)
                )
        );

        // LEGS
        LinearLayout legs =
                new LinearLayout(requireContext());

        legs.setGravity(Gravity.CENTER);

        TextView leftLeg = text(
                "█",
                45,
                Color.rgb(15, 35, 75),
                true
        );

        TextView rightLeg = text(
                "█",
                45,
                Color.rgb(15, 35, 75),
                true
        );

        legs.addView(leftLeg);
        legs.addView(rightLeg);

        wrapper.addView(
                legs,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(58)
                )
        );

        return wrapper;
    }

    // =============================================================
    // NAV ITEM
    // =============================================================

    private void addNav(
            LinearLayout parent,
            int iconRes,
            boolean active) {

        ImageView item =
                new ImageView(requireContext());

        item.setImageResource(iconRes);

        item.setPadding(
                dp(13),
                dp(13),
                dp(13),
                dp(13)
        );

        item.setBackgroundResource(
                active
                        ? R.drawable.bg_rkb_nav_item_active
                        : R.drawable.bg_rkb_nav_item
        );

        parent.addView(
                item,
                new LinearLayout.LayoutParams(
                        dp(56),
                        dp(56)
                )
        );

        parent.addView(
                space(10)
        );
    }

    // =============================================================
    // HEADER BUTTON
    // =============================================================

    private TextView smallButton(
            String name,
            int color) {

        TextView button =
                text(
                        name,
                        13,
                        WHITE,
                        true
                );

        button.setGravity(Gravity.CENTER);

        button.setBackground(
                rounded(
                        color,
                        24
                )
        );

        return button;
    }

    // =============================================================
    // LAUNCH
    // =============================================================

    private void launchMinecraft() {

        try {

            ExtraCore.setValue(
                    ExtraConstants.LAUNCH_GAME,
                    true
            );

        } catch (Exception e) {

            Toast.makeText(
                    requireContext(),
                    "Unable to start Minecraft",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    // =============================================================
    // TEXT
    // =============================================================

    private TextView text(
            String value,
            float size,
            int color,
            boolean bold) {

        TextView t =
                new TextView(requireContext());

        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);

        if (bold) {
            t.setTypeface(
                    Typeface.DEFAULT,
                    Typeface.BOLD
            );
        }

        return t;
    }

    // =============================================================
    // VERTICAL SPACE
    // =============================================================

    private Space space(int value) {

        Space s =
                new Space(requireContext());

        s.setLayoutParams(
                new LinearLayout.LayoutParams(
                        1,
                        dp(value)
                )
        );

        return s;
    }

    // =============================================================
    // HORIZONTAL SPACE
    // =============================================================

    private Space spaceH(int value) {

        Space s =
                new Space(requireContext());

        s.setLayoutParams(
                new LinearLayout.LayoutParams(
                        dp(value),
                        1
                )
        );

        return s;
    }

    // =============================================================
    // ROUNDED BACKGROUND
    // =============================================================

    private GradientDrawable rounded(
            int color,
            int radius) {

        GradientDrawable drawable =
                new GradientDrawable();

        drawable.setColor(color);
        drawable.setCornerRadius(
                dp(radius)
        );

        return drawable;
    }

    // =============================================================
    // DP
    // =============================================================

    private int dp(int value) {

        return Math.round(
                value *
                        getResources()
                                .getDisplayMetrics()
                                .density
        );
    }
}