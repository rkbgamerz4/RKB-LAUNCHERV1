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
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.extra.ExtraConstants;
import net.kdt.pojavlaunch.extra.ExtraCore;

public class RkbHomeFragment extends Fragment {

    // Colors – Dark + Neon Blue
    private static final int BG          = 0xFF070B12;
    private static final int SIDE_BG     = 0xFF0B1220;
    private static final int CARD        = 0xFF121A2A;
    private static final int ACCENT      = 0xFF00B4FF;
    private static final int TEXT_MAIN   = 0xFFE8F4FF;
    private static final int TEXT_MUTED  = 0xFF8BA3C7;
    private static final int YT_RED      = 0xFFE53935;
    private static final int DC_BLUE     = 0xFF5865F2;
    private static final int GREEN_PLAY  = 0xFF00C853;

    private TextView mInstanceLabel;
    private TextView mVersionLabel;
    private String mSelectedInstance = "Default";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Context ctx = requireContext();

        // ROOT – horizontal
        LinearLayout root = new LinearLayout(ctx);
        root.setOrientation(LinearLayout.HORIZONTAL);
        root.setBackgroundColor(BG);
        root.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        // ========== LEFT SIDE NAV ==========
        LinearLayout sideNav = new LinearLayout(ctx);
        sideNav.setOrientation(LinearLayout.VERTICAL);
        sideNav.setBackgroundColor(SIDE_BG);
        sideNav.setGravity(Gravity.CENTER_HORIZONTAL);
        sideNav.setPadding(dp(8), dp(16), dp(8), dp(16));
        LinearLayout.LayoutParams sideLp = new LinearLayout.LayoutParams(dp(64), ViewGroup.LayoutParams.MATCH_PARENT);
        sideNav.setLayoutParams(sideLp);

        // Top accent line
        View accentLine = new View(ctx);
        accentLine.setBackgroundColor(ACCENT);
        sideNav.addView(accentLine, new LinearLayout.LayoutParams(dp(4), dp(28)));

        sideNav.addView(spacer(ctx, 12));

        // Nav items (ImageView – no emoji)
        sideNav.addView(navItem(ctx, android.R.drawable.ic_menu_compass, true, v -> { /* already home */ }));
        sideNav.addView(spacer(ctx, 10));
        sideNav.addView(navItem(ctx, android.R.drawable.ic_menu_manage, false, v ->
                Toast.makeText(ctx, "Instances – Phase 2", Toast.LENGTH_SHORT).show()));
        sideNav.addView(spacer(ctx, 10));
        sideNav.addView(navItem(ctx, android.R.drawable.ic_menu_view, false, v ->
                Toast.makeText(ctx, "Controls", Toast.LENGTH_SHORT).show()));
        sideNav.addView(spacer(ctx, 10));
        sideNav.addView(navItem(ctx, android.R.drawable.ic_menu_gallery, false, v ->
                Toast.makeText(ctx, "Skins", Toast.LENGTH_SHORT).show()));
        sideNav.addView(spacer(ctx, 10));
        sideNav.addView(navItem(ctx, android.R.drawable.ic_menu_info_details, false, v ->
                Toast.makeText(ctx, "About RKB Launcher", Toast.LENGTH_SHORT).show()));

        // Spacer push settings to bottom
        View flex = new View(ctx);
        LinearLayout.LayoutParams flexLp = new LinearLayout.LayoutParams(1, 0, 1f);
        sideNav.addView(flex, flexLp);

        // Settings – FIXED package
        sideNav.addView(navItem(ctx, android.R.drawable.ic_menu_preferences, false, v -> {
            try {
                Tools.swapFragment(requireActivity(),
                        net.kdt.pojavlaunch.prefs.LauncherPreferenceFragment.class,
                        "SETTINGS", null);
            } catch (Exception e) {
                Toast.makeText(ctx, "Settings", Toast.LENGTH_SHORT).show();
            }
        }));

        root.addView(sideNav);

        // ========== MAIN CONTENT ==========
        LinearLayout main = new LinearLayout(ctx);
        main.setOrientation(LinearLayout.VERTICAL);
        main.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));
        main.setPadding(dp(12), dp(10), dp(12), dp(10));

        // --- TOP BAR ---
        LinearLayout topBar = new LinearLayout(ctx);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(48)));

        // Logo pill
        LinearLayout logoPill = pill(ctx, CARD, dp(22));
        logoPill.setPadding(dp(10), dp(6), dp(14), dp(6));
        logoPill.setGravity(Gravity.CENTER_VERTICAL);

        TextView logoR = new TextView(ctx);
        logoR.setText("R");
        logoR.setTextColor(Color.WHITE);
        logoR.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        logoR.setTypeface(Typeface.DEFAULT_BOLD);
        logoR.setBackground(circle(ACCENT, dp(22)));
        logoR.setGravity(Gravity.CENTER);
        logoR.setWidth(dp(22));
        logoR.setHeight(dp(22));
        logoPill.addView(logoR);

        logoPill.addView(hSpace(ctx, 8));

        LinearLayout logoTextCol = new LinearLayout(ctx);
        logoTextCol.setOrientation(LinearLayout.VERTICAL);
        TextView title = new TextView(ctx);
        title.setText("RKB LAUNCHER");
        title.setTextColor(TEXT_MAIN);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        TextView sub = new TextView(ctx);
        sub.setText("PLAY  •  EXPLORE  •  CREATE");
        sub.setTextColor(TEXT_MUTED);
        sub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9);
        logoTextCol.addView(title);
        logoTextCol.addView(sub);
        logoPill.addView(logoTextCol);

        topBar.addView(logoPill);
        topBar.addView(hSpace(ctx, 10));

        // YouTube
        TextView ytBtn = actionChip(ctx, "YouTube", YT_RED);
        ytBtn.setOnClickListener(v -> openUrl("https://youtube.com"));
        topBar.addView(ytBtn);
        topBar.addView(hSpace(ctx, 6));

        // Discord
        TextView dcBtn = actionChip(ctx, "Discord", DC_BLUE);
        dcBtn.setOnClickListener(v -> openUrl("https://discord.gg"));
        topBar.addView(dcBtn);

        // Spacer
        View topFlex = new View(ctx);
        topBar.addView(topFlex, new LinearLayout.LayoutParams(0, 1, 1f));

        // Account pill
        LinearLayout accPill = pill(ctx, CARD, dp(20));
        accPill.setPadding(dp(12), dp(6), dp(12), dp(6));
        accPill.setGravity(Gravity.CENTER_VERTICAL);
        TextView accTxt = new TextView(ctx);
        accTxt.setText("RKB_GAMERZ  ·  LOCAL");
        accTxt.setTextColor(TEXT_MAIN);
        accTxt.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        accTxt.setTypeface(Typeface.DEFAULT_BOLD);
        accPill.addView(accTxt);
        accPill.setOnClickListener(v -> {
            try {
                ExtraCore.setValue(ExtraConstants.SELECT_AUTH_METHOD, true);
            } catch (Exception e) {
                Toast.makeText(ctx, "Accounts", Toast.LENGTH_SHORT).show();
            }
        });
        topBar.addView(accPill);

        main.addView(topBar);
        main.addView(vSpace(ctx, 12));

        // --- CENTER AREA (skin + launch) ---
        LinearLayout centerRow = new LinearLayout(ctx);
        centerRow.setOrientation(LinearLayout.HORIZONTAL);
        centerRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams centerLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        centerRow.setLayoutParams(centerLp);

        // Left empty space
        View leftPad = new View(ctx);
        centerRow.addView(leftPad, new LinearLayout.LayoutParams(0, 1, 0.35f));

        // Skin + Launch column
        LinearLayout skinCol = new LinearLayout(ctx);
        skinCol.setOrientation(LinearLayout.VERTICAL);
        skinCol.setGravity(Gravity.CENTER_HORIZONTAL);
        skinCol.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.45f));

        // Player name tag
        TextView playerTag = new TextView(ctx);
        playerTag.setText("Player");
        playerTag.setTextColor(TEXT_MAIN);
        playerTag.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        playerTag.setBackground(pillBg(0xCC1A2538, dp(12)));
        playerTag.setPadding(dp(14), dp(4), dp(14), dp(4));
        playerTag.setGravity(Gravity.CENTER);
        skinCol.addView(playerTag);
        skinCol.addView(vSpace(ctx, 8));

        // Skin placeholder (blue neon frame)
        FrameLayout skinFrame = new FrameLayout(ctx);
        GradientDrawable skinBg = new GradientDrawable();
        skinBg.setColor(0xFF0D1525);
        skinBg.setCornerRadius(dp(16));
        skinBg.setStroke(dp(2), ACCENT);
        skinFrame.setBackground(skinBg);
        skinFrame.setLayoutParams(new LinearLayout.LayoutParams(dp(140), dp(180)));

        // Simple blocky skin
        LinearLayout skinInner = new LinearLayout(ctx);
        skinInner.setOrientation(LinearLayout.VERTICAL);
        skinInner.setGravity(Gravity.CENTER_HORIZONTAL);
        skinInner.setPadding(0, dp(16), 0, 0);

        View head = new View(ctx);
        head.setBackground(rounded(0xFF4FC3F7, dp(6)));
        skinInner.addView(head, new LinearLayout.LayoutParams(dp(48), dp(48)));
        skinInner.addView(vSpace(ctx, 4));

        View body = new View(ctx);
        body.setBackground(rounded(0xFF0288D1, dp(4)));
        skinInner.addView(body, new LinearLayout.LayoutParams(dp(56), dp(70)));
        skinInner.addView(vSpace(ctx, 4));

        LinearLayout legs = new LinearLayout(ctx);
        legs.setOrientation(LinearLayout.HORIZONTAL);
        View legL = new View(ctx);
        legL.setBackground(rounded(0xFF01579B, dp(3)));
        View legR = new View(ctx);
        legR.setBackground(rounded(0xFF01579B, dp(3)));
        legs.addView(legL, new LinearLayout.LayoutParams(dp(22), dp(30)));
        legs.addView(hSpace(ctx, 6));
        legs.addView(legR, new LinearLayout.LayoutParams(dp(22), dp(30)));
        skinInner.addView(legs);

        skinFrame.addView(skinInner, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
                Gravity.CENTER));
        skinCol.addView(skinFrame);
        skinCol.addView(vSpace(ctx, 14));

        // LAUNCH button
        TextView launchBtn = new TextView(ctx);
        launchBtn.setText("▶  LAUNCH");
        launchBtn.setTextColor(Color.WHITE);
        launchBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        launchBtn.setTypeface(Typeface.DEFAULT_BOLD);
        launchBtn.setGravity(Gravity.CENTER);
        GradientDrawable launchBg = new GradientDrawable();
        launchBg.setColor(ACCENT);
        launchBg.setCornerRadius(dp(28));
        launchBtn.setBackground(launchBg);
        launchBtn.setPadding(dp(36), dp(14), dp(36), dp(14));
        launchBtn.setOnClickListener(v -> doLaunch());
        skinCol.addView(launchBtn);
        skinCol.addView(vSpace(ctx, 10));

        // Version selector
        mVersionLabel = new TextView(ctx);
        mVersionLabel.setText("▶  Minecraft 1.21.11  ▼");
        mVersionLabel.setTextColor(TEXT_MAIN);
        mVersionLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        mVersionLabel.setGravity(Gravity.CENTER);
        mVersionLabel.setBackground(pillBg(CARD, dp(16)));
        mVersionLabel.setPadding(dp(18), dp(8), dp(18), dp(8));
        mVersionLabel.setOnClickListener(v ->
                Toast.makeText(ctx, "Version picker – Phase 2", Toast.LENGTH_SHORT).show());
        skinCol.addView(mVersionLabel);

        centerRow.addView(skinCol);

        // Right pad
        View rightPad = new View(ctx);
        centerRow.addView(rightPad, new LinearLayout.LayoutParams(0, 1, 0.20f));

        main.addView(centerRow);

        // --- BOTTOM INSTANCE ROW ---
        LinearLayout bottomRow = new LinearLayout(ctx);
        bottomRow.setOrientation(LinearLayout.HORIZONTAL);
        bottomRow.setGravity(Gravity.CENTER_VERTICAL);
        bottomRow.setPadding(0, dp(8), 0, 0);

        // Current instance card
        LinearLayout instCard = pill(ctx, CARD, dp(14));
        instCard.setPadding(dp(10), dp(8), dp(10), dp(8));
        instCard.setGravity(Gravity.CENTER_VERTICAL);

        View bar = new View(ctx);
        bar.setBackgroundColor(GREEN_PLAY);
        instCard.addView(bar, new LinearLayout.LayoutParams(dp(3), dp(28)));
        instCard.addView(hSpace(ctx, 8));

        View block = new View(ctx);
        block.setBackground(rounded(0xFF8B6914, dp(4)));
        instCard.addView(block, new LinearLayout.LayoutParams(dp(28), dp(28)));
        instCard.addView(hSpace(ctx, 8));

        LinearLayout instText = new LinearLayout(ctx);
        instText.setOrientation(LinearLayout.VERTICAL);
        mInstanceLabel = new TextView(ctx);
        mInstanceLabel.setText(mSelectedInstance);
        mInstanceLabel.setTextColor(TEXT_MAIN);
        mInstanceLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        mInstanceLabel.setTypeface(Typeface.DEFAULT_BOLD);
        TextView ver = new TextView(ctx);
        ver.setText("1.21.11");
        ver.setTextColor(TEXT_MUTED);
        ver.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        instText.addView(mInstanceLabel);
        instText.addView(ver);
        instCard.addView(instText);
        instCard.addView(hSpace(ctx, 10));

        TextView miniPlay = new TextView(ctx);
        miniPlay.setText("▶");
        miniPlay.setTextColor(Color.WHITE);
        miniPlay.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        miniPlay.setBackground(circle(ACCENT, dp(26)));
        miniPlay.setGravity(Gravity.CENTER);
        miniPlay.setWidth(dp(26));
        miniPlay.setHeight(dp(26));
        miniPlay.setOnClickListener(v -> doLaunch());
        instCard.addView(miniPlay);

        bottomRow.addView(instCard);
        bottomRow.addView(hSpace(ctx, 10));

        // + New Instance
        TextView newInst = new TextView(ctx);
        newInst.setText("+  New Instance");
        newInst.setTextColor(ACCENT);
        newInst.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        newInst.setGravity(Gravity.CENTER);
        GradientDrawable dashed = new GradientDrawable();
        dashed.setColor(Color.TRANSPARENT);
        dashed.setCornerRadius(dp(14));
        dashed.setStroke(dp(1), ACCENT, dp(6), dp(4));
        newInst.setBackground(dashed);
        newInst.setPadding(dp(18), dp(10), dp(18), dp(10));
        newInst.setOnClickListener(v ->
                Toast.makeText(ctx, "New Instance – Phase 2", Toast.LENGTH_SHORT).show());
        bottomRow.addView(newInst);

        main.addView(bottomRow);

        root.addView(main);
        return root;
    }

    // ---------- helpers ----------

    private void doLaunch() {
        try {
            ExtraCore.setValue(ExtraConstants.LAUNCH_GAME, true);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Launch: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void openUrl(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception e) {
            Toast.makeText(requireContext(), url, Toast.LENGTH_SHORT).show();
        }
    }

    private View navItem(Context ctx, int iconRes, boolean active, View.OnClickListener click) {
        FrameLayout wrap = new FrameLayout(ctx);
        int size = dp(44);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
        wrap.setLayoutParams(lp);

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(12));
        if (active) {
            bg.setColor(0x3300B4FF);
            bg.setStroke(dp(2), ACCENT);
        } else {
            bg.setColor(0x00000000);
        }
        wrap.setBackground(bg);

        ImageView iv = new ImageView(ctx);
        try {
            iv.setImageResource(iconRes);
        } catch (Exception ignored) {}
        iv.setColorFilter(active ? ACCENT : TEXT_MUTED);
        FrameLayout.LayoutParams ivLp = new FrameLayout.LayoutParams(dp(24), dp(24), Gravity.CENTER);
        wrap.addView(iv, ivLp);
        wrap.setOnClickListener(click);
        return wrap;
    }

    private TextView actionChip(Context ctx, String text, int color) {
        TextView t = new TextView(ctx);
        t.setText(text);
        t.setTextColor(Color.WHITE);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setGravity(Gravity.CENTER);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(color);
        bg.setCornerRadius(dp(14));
        t.setBackground(bg);
        t.setPadding(dp(14), dp(6), dp(14), dp(6));
        return t;
    }

    private LinearLayout pill(Context ctx, int color, int radiusDp) {
        LinearLayout l = new LinearLayout(ctx);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setBackground(pillBg(color, radiusDp));
        return l;
    }

    private GradientDrawable pillBg(int color, int radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(radiusDp));
        return g;
    }

    private GradientDrawable circle(int color, int sizeDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setShape(GradientDrawable.OVAL);
        return g;
    }

    private GradientDrawable rounded(int color, int radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(radiusDp));
        return g;
    }

    private View spacer(Context ctx, int h) {
        View v = new View(ctx);
        v.setLayoutParams(new LinearLayout.LayoutParams(1, dp(h)));
        return v;
    }

    private View vSpace(Context ctx, int h) {
        View v = new View(ctx);
        v.setLayoutParams(new LinearLayout.LayoutParams(1, dp(h)));
        return v;
    }

    private View hSpace(Context ctx, int w) {
        View v = new View(ctx);
        v.setLayoutParams(new LinearLayout.LayoutParams(dp(w), 1));
        return v;
    }

    private int dp(int v) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, v,
                getResources().getDisplayMetrics());
    }
}