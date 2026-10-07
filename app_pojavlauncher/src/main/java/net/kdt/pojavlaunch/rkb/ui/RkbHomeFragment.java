package net.kdt.pojavlaunch.rkb.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import net.kdt.pojavlaunch.LauncherActivity;
import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.extra.ExtraConstants;
import net.kdt.pojavlaunch.extra.ExtraCore;

public class RkbHomeFragment extends Fragment {

    // =========================
    // RKB LAUNCHER COLORS
    // =========================

    private static final int BG = 0xFF060A10;
    private static final int PANEL = 0xFF0A111C;
    private static final int PANEL_2 = 0xFF0D1724;

    private static final int ACCENT = 0xFF00A8FF;
    private static final int ACCENT_LIGHT = 0xFF39C2FF;

    private static final int TEXT_MAIN = 0xFFF1F7FF;
    private static final int TEXT_MUTED = 0xFF8095AF;

    private static final int GREEN = 0xFF00D084;
    private static final int RED = 0xFFFF5263;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        Context ctx = requireContext();

        // ============================================================
        // ROOT
        // ============================================================

        LinearLayout root = new LinearLayout(ctx);
        root.setOrientation(LinearLayout.HORIZONTAL);
        root.setBackgroundColor(BG);
        root.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        // ============================================================
        // LEFT NAVIGATION
        // ============================================================

        LinearLayout sideNav = new LinearLayout(ctx);
        sideNav.setOrientation(LinearLayout.VERTICAL);
        sideNav.setGravity(Gravity.CENTER_HORIZONTAL);
        sideNav.setPadding(dp(7), dp(12), dp(7), dp(12));

        GradientDrawable sideBg = new GradientDrawable();
        sideBg.setColor(PANEL);
        sideNav.setBackground(sideBg);

        LinearLayout.LayoutParams sideParams =
                new LinearLayout.LayoutParams(
                        dp(72),
                        ViewGroup.LayoutParams.MATCH_PARENT
                );

        sideNav.setLayoutParams(sideParams);

        // RKB logo
        FrameLayout logoBox = new FrameLayout(ctx);

        GradientDrawable logoBg = new GradientDrawable();
        logoBg.setColor(0xFF101D2B);
        logoBg.setCornerRadius(dp(15));
        logoBg.setStroke(dp(1), 0xFF17344D);

        logoBox.setBackground(logoBg);

        ImageView logo = new ImageView(ctx);
        try {
            logo.setImageResource(R.drawable.logo_rkb);
        } catch (Exception ignored) {
        }

        logoBox.addView(
                logo,
                new FrameLayout.LayoutParams(
                        dp(38),
                        dp(38),
                        Gravity.CENTER
                )
        );

        sideNav.addView(
                logoBox,
                new LinearLayout.LayoutParams(dp(50), dp(50))
        );

        sideNav.addView(space(ctx, 18));

        // Navigation
        sideNav.addView(
                navItem(
                        ctx,
                        R.drawable.ic_nav_home,
                        "HOME",
                        true,
                        v -> {
                        }
                )
        );

        sideNav.addView(space(ctx, 7));

        sideNav.addView(
                navItem(
                        ctx,
                        R.drawable.ic_nav_instances,
                        "CURSOR",
                        false,
                        v -> showMessage("Cursor Studio")
                )
        );

        sideNav.addView(space(ctx, 7));

        sideNav.addView(
                navItem(
                        ctx,
                        R.drawable.ic_nav_instances,
                        "MODS",
                        false,
                        v -> showMessage("Mods")
                )
        );

        sideNav.addView(space(ctx, 7));

        sideNav.addView(
                navItem(
                        ctx,
                        R.drawable.ic_nav_controls,
                        "CONTROLS",
                        false,
                        v -> showMessage("Controls")
                )
        );

        sideNav.addView(space(ctx, 7));

        sideNav.addView(
                navItem(
                        ctx,
                        R.drawable.ic_nav_info,
                        "ABOUT",
                        false,
                        v -> showMessage(
                                "RKB LAUNCHER\n\nMade by RKB GAMERZ"
                        )
                )
        );

        // Push settings + skin to bottom
        View spacer = new View(ctx);
        sideNav.addView(
                spacer,
                new LinearLayout.LayoutParams(
                        1,
                        0,
                        1f
                )
        );

        sideNav.addView(
                navItem(
                        ctx,
                        R.drawable.ic_nav_skins,
                        "SKIN",
                        false,
                        v -> showMessage("Skin")
                )
        );

        sideNav.addView(space(ctx, 7));

        sideNav.addView(
                navItem(
                        ctx,
                        R.drawable.ic_nav_settings,
                        "SETTINGS",
                        false,
                        v -> openSettings()
                )
        );

        root.addView(sideNav);

        // ============================================================
        // MAIN CONTENT
        // ============================================================

        ScrollView scroll = new ScrollView(ctx);
        scroll.setFillViewport(true);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);

        LinearLayout main = new LinearLayout(ctx);
        main.setOrientation(LinearLayout.VERTICAL);

        main.setPadding(
                dp(18),
                dp(15),
                dp(18),
                dp(18)
        );

        scroll.addView(main);

        root.addView(
                scroll,
                new LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        1f
                )
        );

        // ============================================================
        // TOP BAR
        // ============================================================

        LinearLayout top = new LinearLayout(ctx);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(ctx);
        title.setText("RKB LAUNCHER");
        title.setTextColor(TEXT_MAIN);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        title.setTypeface(Typeface.DEFAULT_BOLD);

        top.addView(
                title,
                new LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1f
                )
        );

        TextView status = new TextView(ctx);
        status.setText("●  READY");
        status.setTextColor(GREEN);
        status.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        status.setTypeface(Typeface.DEFAULT_BOLD);

        GradientDrawable statusBg = new GradientDrawable();
        statusBg.setColor(0x1419D084);
        statusBg.setCornerRadius(dp(12));
        status.setBackground(statusBg);

        status.setPadding(
                dp(10),
                dp(6),
                dp(10),
                dp(6)
        );

        top.addView(status);

        main.addView(top);

        main.addView(space(ctx, 6));

        TextView subtitle = new TextView(ctx);
        subtitle.setText("PLAY  •  EXPLORE  •  CREATE");
        subtitle.setTextColor(TEXT_MUTED);
        subtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9);

        main.addView(subtitle);

        main.addView(space(ctx, 18));

        // ============================================================
        // ACCOUNT CARD
        // ============================================================

        LinearLayout account = new LinearLayout(ctx);
        account.setOrientation(LinearLayout.HORIZONTAL);
        account.setGravity(Gravity.CENTER_VERTICAL);

        GradientDrawable accountBg = cardBackground();

        account.setBackground(accountBg);

        account.setPadding(
                dp(12),
                dp(10),
                dp(12),
                dp(10)
        );

        TextView avatar = new TextView(ctx);
        avatar.setText("R");
        avatar.setTextColor(Color.WHITE);
        avatar.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        avatar.setTypeface(Typeface.DEFAULT_BOLD);
        avatar.setGravity(Gravity.CENTER);

        GradientDrawable avatarBg = new GradientDrawable();
        avatarBg.setColor(ACCENT);
        avatarBg.setShape(GradientDrawable.OVAL);
        avatar.setBackground(avatarBg);

        account.addView(
                avatar,
                new LinearLayout.LayoutParams(
                        dp(38),
                        dp(38)
                )
        );

        account.addView(hSpace(ctx, 10));

        LinearLayout accountText = new LinearLayout(ctx);
        accountText.setOrientation(LinearLayout.VERTICAL);

        TextView username = new TextView(ctx);
        username.setText("RKB_GAMERZ");
        username.setTextColor(TEXT_MAIN);
        username.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        username.setTypeface(Typeface.DEFAULT_BOLD);

        TextView accountType = new TextView(ctx);
        accountType.setText("LOCAL ACCOUNT");
        accountType.setTextColor(TEXT_MUTED);
        accountType.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9);

        accountText.addView(username);
        accountText.addView(accountType);

        account.addView(
                accountText,
                new LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1f
                )
        );

        TextView arrow = new TextView(ctx);
        arrow.setText("›");
        arrow.setTextColor(TEXT_MUTED);
        arrow.setTextSize(TypedValue.COMPLEX_UNIT_SP, 25);

        account.addView(arrow);

        account.setOnClickListener(v -> {
            try {
                ExtraCore.setValue(
                        ExtraConstants.SELECT_AUTH_METHOD,
                        true
                );
            } catch (Exception e) {
                Toast.makeText(
                        ctx,
                        "Accounts",
                        Toast.LENGTH_SHORT
                ).show();
            }
        });

        main.addView(
                account,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        main.addView(space(ctx, 18));

        // ============================================================
        // WELCOME
        // ============================================================

        TextView welcome = new TextView(ctx);
        welcome.setText("Welcome back, RKB");
        welcome.setTextColor(TEXT_MAIN);
        welcome.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
        welcome.setTypeface(Typeface.DEFAULT_BOLD);

        main.addView(welcome);

        TextView ready = new TextView(ctx);
        ready.setText("Your Minecraft experience is ready.");
        ready.setTextColor(TEXT_MUTED);
        ready.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);

        main.addView(ready);

        main.addView(space(ctx, 18));

        // ============================================================
        // PLAYER / SKIN CARD
        // ============================================================

        LinearLayout playerCard = new LinearLayout(ctx);
        playerCard.setOrientation(LinearLayout.HORIZONTAL);
        playerCard.setGravity(Gravity.CENTER_VERTICAL);
        playerCard.setPadding(
                dp(15),
                dp(15),
                dp(15),
                dp(15)
        );
        playerCard.setBackground(cardBackground());

        // Skin preview
        FrameLayout skinFrame = new FrameLayout(ctx);

        GradientDrawable skinBorder = new GradientDrawable();
        skinBorder.setColor(0xFF0B1521);
        skinBorder.setCornerRadius(dp(16));
        skinBorder.setStroke(dp(1), 0xFF174765);

        skinFrame.setBackground(skinBorder);

        LinearLayout skin = new LinearLayout(ctx);
        skin.setOrientation(LinearLayout.VERTICAL);
        skin.setGravity(Gravity.CENTER_HORIZONTAL);

        skin.setPadding(
                0,
                dp(12),
                0,
                dp(8)
        );

        View head = new View(ctx);
        head.setBackground(
                rounded(
                        0xFF4FC3F7,
                        5
                )
        );

        skin.addView(
                head,
                new LinearLayout.LayoutParams(
                        dp(42),
                        dp(42)
                )
        );

        skin.addView(space(ctx, 3));

        View body = new View(ctx);
        body.setBackground(
                rounded(
                        0xFF0288D1,
                        4
                )
        );

        skin.addView(
                body,
                new LinearLayout.LayoutParams(
                        dp(48),
                        dp(52)
                )
        );

        skin.addView(space(ctx, 3));

        LinearLayout legs = new LinearLayout(ctx);
        legs.setOrientation(LinearLayout.HORIZONTAL);

        View leg1 = new View(ctx);
        leg1.setBackground(
                rounded(
                        0xFF01579B,
                        3
                )
        );

        View leg2 = new View(ctx);
        leg2.setBackground(
                rounded(
                        0xFF01579B,
                        3
                )
        );

        legs.addView(
                leg1,
                new LinearLayout.LayoutParams(
                        dp(18),
                        dp(25)
                )
        );

        legs.addView(hSpace(ctx, 5));

        legs.addView(
                leg2,
                new LinearLayout.LayoutParams(
                        dp(18),
                        dp(25)
                )
        );

        skin.addView(legs);

        skinFrame.addView(
                skin,
                new FrameLayout.LayoutParams(
                        dp(90),
                        dp(140),
                        Gravity.CENTER
                )
        );

        playerCard.addView(
                skinFrame,
                new LinearLayout.LayoutParams(
                        dp(115),
                        dp(155)
                )
        );

        playerCard.addView(hSpace(ctx, 16));

        // Player information
        LinearLayout playerInfo = new LinearLayout(ctx);
        playerInfo.setOrientation(LinearLayout.VERTICAL);

        TextView playerTitle = new TextView(ctx);
        playerTitle.setText("YOUR PLAYER");
        playerTitle.setTextColor(TEXT_MUTED);
        playerTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9);
        playerTitle.setTypeface(Typeface.DEFAULT_BOLD);

        TextView playerName = new TextView(ctx);
        playerName.setText("RKB_GAMERZ");
        playerName.setTextColor(TEXT_MAIN);
        playerName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 19);
        playerName.setTypeface(Typeface.DEFAULT_BOLD);

        TextView playerDesc = new TextView(ctx);
        playerDesc.setText("Minecraft Skin");
        playerDesc.setTextColor(TEXT_MUTED);
        playerDesc.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);

        playerInfo.addView(playerTitle);
        playerInfo.addView(space(ctx, 4));
        playerInfo.addView(playerName);
        playerInfo.addView(space(ctx, 3));
        playerInfo.addView(playerDesc);

        playerCard.addView(
                playerInfo,
                new LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1f
                )
        );

        TextView skinButton = new TextView(ctx);
        skinButton.setText("SKIN  ›");
        skinButton.setTextColor(ACCENT_LIGHT);
        skinButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        skinButton.setTypeface(Typeface.DEFAULT_BOLD);
        skinButton.setGravity(Gravity.CENTER);

        skinButton.setOnClickListener(
                v -> showMessage("Skin")
        );

        playerCard.addView(skinButton);

        main.addView(
                playerCard,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        main.addView(space(ctx, 15));

        // ============================================================
        // VERSION CARD
        // ============================================================

        LinearLayout version = new LinearLayout(ctx);
        version.setOrientation(LinearLayout.HORIZONTAL);
        version.setGravity(Gravity.CENTER_VERTICAL);
        version.setPadding(
                dp(14),
                dp(11),
                dp(14),
                dp(11)
        );

        version.setBackground(cardBackground());

        LinearLayout versionText = new LinearLayout(ctx);
        versionText.setOrientation(LinearLayout.VERTICAL);

        TextView versionTitle = new TextView(ctx);
        versionTitle.setText("MINECRAFT VERSION");
        versionTitle.setTextColor(TEXT_MUTED);
        versionTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 8);
        versionTitle.setTypeface(Typeface.DEFAULT_BOLD);

        TextView versionName = new TextView(ctx);
        versionName.setText("1.21.11");
        versionName.setTextColor(TEXT_MAIN);
        versionName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        versionName.setTypeface(Typeface.DEFAULT_BOLD);

        versionText.addView(versionTitle);
        versionText.addView(versionName);

        version.addView(
                versionText,
                new LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1f
                )
        );

        TextView change = new TextView(ctx);
        change.setText("CHANGE  ›");
        change.setTextColor(ACCENT);
        change.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9);
        change.setTypeface(Typeface.DEFAULT_BOLD);

        version.addView(change);

        version.setOnClickListener(
                v -> showMessage("Version picker")
        );

                main.addView(version);

        main.addView(space(ctx, 15));

        // ============================================================
        // LAUNCH BUTTON
        // ============================================================

        TextView launch = new TextView(ctx);
        launch.setText("▶   LAUNCH");
        launch.setTextColor(Color.WHITE);
        launch.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        launch.setTypeface(Typeface.DEFAULT_BOLD);
        launch.setGravity(Gravity.CENTER);

        GradientDrawable launchBg = new GradientDrawable();
        launchBg.setColor(ACCENT);
        launchBg.setCornerRadius(dp(15));

        launch.setBackground(launchBg);

        launch.setPadding(
                dp(20),
                dp(15),
                dp(20),
                dp(15)
        );

        launch.setOnClickListener(v -> doLaunch());

        main.addView(
                launch,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(54)
                )
        );

        main.addView(space(ctx, 15));

        // ============================================================
        // INSTANCE CARD
        // ============================================================

        LinearLayout instance = new LinearLayout(ctx);
        instance.setOrientation(LinearLayout.HORIZONTAL);
        instance.setGravity(Gravity.CENTER_VERTICAL);
        instance.setPadding(
                dp(12),
                dp(10),
                dp(12),
                dp(10)
        );

        instance.setBackground(cardBackground());

        View green = new View(ctx);
        green.setBackgroundColor(GREEN);

        instance.addView(
                green,
                new LinearLayout.LayoutParams(
                        dp(3),
                        dp(32)
                )
        );

        instance.addView(hSpace(ctx, 10));

        LinearLayout instanceText = new LinearLayout(ctx);
        instanceText.setOrientation(LinearLayout.VERTICAL);

        TextView instanceName = new TextView(ctx);
        instanceName.setText("Default");
        instanceName.setTextColor(TEXT_MAIN);
        instanceName.setTextSize(
                TypedValue.COMPLEX_UNIT_SP,
                12
        );
        instanceName.setTypeface(
                Typeface.DEFAULT_BOLD
        );

        TextView instanceVersion = new TextView(ctx);
        instanceVersion.setText("1.21.11  •  Ready");
        instanceVersion.setTextColor(TEXT_MUTED);
        instanceVersion.setTextSize(
                TypedValue.COMPLEX_UNIT_SP,
                9
        );

        instanceText.addView(instanceName);
        instanceText.addView(instanceVersion);

        instance.addView(
                instanceText,
                new LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1f
                )
        );

        TextView instancePlay = new TextView(ctx);
        instancePlay.setText("▶");
        instancePlay.setTextColor(Color.WHITE);
        instancePlay.setGravity(Gravity.CENTER);
        instancePlay.setTextSize(
                TypedValue.COMPLEX_UNIT_SP,
                11
        );

        GradientDrawable playBg = new GradientDrawable();
        playBg.setShape(GradientDrawable.OVAL);
        playBg.setColor(ACCENT);

        instancePlay.setBackground(playBg);

        instancePlay.setOnClickListener(
                v -> doLaunch()
        );

        instance.addView(
                instancePlay,
                new LinearLayout.LayoutParams(
                        dp(34),
                        dp(34)
                )
        );

        main.addView(instance);

        return root;
    }

    // ================================================================
    // NAV ITEM
    // ================================================================

    private View navItem(
            Context ctx,
            int iconRes,
            String label,
            boolean active,
            View.OnClickListener listener) {

        LinearLayout item = new LinearLayout(ctx);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(Gravity.CENTER);

        item.setLayoutParams(
                new LinearLayout.LayoutParams(
                        dp(58),
                        dp(62)
                )
        );

        GradientDrawable bg = new GradientDrawable();

        if (active) {
            bg.setColor(0xFF0D2639);
            bg.setCornerRadius(dp(14));
            bg.setStroke(dp(1), 0xFF125B83);
        } else {
            bg.setColor(Color.TRANSPARENT);
            bg.setCornerRadius(dp(14));
        }

        item.setBackground(bg);

        ImageView icon = new ImageView(ctx);

        try {
            icon.setImageResource(iconRes);
        } catch (Exception ignored) {
        }

        icon.setColorFilter(
                active ? ACCENT_LIGHT : TEXT_MUTED
        );

        item.addView(
                icon,
                new LinearLayout.LayoutParams(
                        dp(22),
                        dp(22)
                )
        );

        item.addView(space(ctx, 3));

        TextView text = new TextView(ctx);
        text.setText(label);
        text.setTextColor(
                active ? ACCENT_LIGHT : TEXT_MUTED
        );
        text.setTextSize(
                TypedValue.COMPLEX_UNIT_SP,
                7
        );
        text.setTypeface(
                Typeface.DEFAULT_BOLD
        );
        text.setGravity(Gravity.CENTER);

        item.addView(text);

        item.setOnClickListener(listener);

        return item;
    }

    // ================================================================
    // LAUNCH
    // ================================================================

    private void doLaunch() {
        try {

            ExtraCore.setValue(
                    ExtraConstants.LAUNCH_GAME,
                    true
            );

            Toast.makeText(
                    requireContext(),
                    "Launching...",
                    Toast.LENGTH_SHORT
            ).show();

        } catch (Exception e) {

            e.printStackTrace();

            Toast.makeText(
                    requireContext(),
                    "Launch failed: " + e.getMessage(),
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    // ================================================================
    // SETTINGS
    // ================================================================

    private void openSettings() {

        try {

            if (getActivity() instanceof LauncherActivity) {

                Class<?> prefClass =
                        Class.forName(
                                "net.kdt.pojavlaunch.prefs.LauncherPreferenceFragment"
                        );

                ((LauncherActivity) getActivity()).swapFragment(
                        (Class<? extends Fragment>) prefClass
                );

                return;
            }

        } catch (Exception ignored) {
        }

        try {

            int id = getResources().getIdentifier(
                    "setting_button",
                    "id",
                    requireContext().getPackageName()
            );

            if (id == 0) {
                id = getResources().getIdentifier(
                        "settings_button",
                        "id",
                        requireContext().getPackageName()
                );
            }

            if (id != 0) {

                View btn =
                        requireActivity().findViewById(id);

                if (btn != null) {
                    btn.performClick();
                    return;
                }
            }

        } catch (Exception ignored) {
        }

        showMessage("Settings");
    }

    // ================================================================
    // CARD
    // ================================================================

    private GradientDrawable cardBackground() {

        GradientDrawable bg =
                new GradientDrawable();

        bg.setColor(PANEL_2);
        bg.setCornerRadius(dp(15));
        bg.setStroke(
                dp(1),
                0xFF15283A
        );

        return bg;
    }

    // ================================================================
    // ROUNDED
    // ================================================================

    private GradientDrawable rounded(
            int color,
            int radiusDp) {

        GradientDrawable g =
                new GradientDrawable();

        g.setColor(color);
        g.setCornerRadius(dp(radiusDp));

        return g;
    }

    // ================================================================
    // MESSAGE
    // ================================================================

    private void showMessage(String message) {

        Toast.makeText(
                requireContext(),
                message,
                Toast.LENGTH_SHORT
        ).show();
    }

    // ================================================================
    // VERTICAL SPACE
    // ================================================================

    private View space(
            Context ctx,
            int height) {

        View v = new View(ctx);

        v.setLayoutParams(
                new LinearLayout.LayoutParams(
                        1,
                        dp(height)
                )
        );

        return v;
    }

    // ================================================================
    // HORIZONTAL SPACE
    // ================================================================

    private View hSpace(
            Context ctx,
            int width) {

        View v = new View(ctx);

        v.setLayoutParams(
                new LinearLayout.LayoutParams(
                        dp(width),
                        1
                )
        );

        return v;
    }

    // ================================================================
    // DP
    // ================================================================

    private int dp(int value) {

        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                getResources().getDisplayMetrics()
        );
    }
}

        