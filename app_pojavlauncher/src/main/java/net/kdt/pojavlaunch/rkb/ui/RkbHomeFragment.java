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
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.extra.ExtraConstants;
import net.kdt.pojavlaunch.extra.ExtraCore;

public class RkbHomeFragment extends Fragment {

    private static final int BG         = 0xFF070B12;
    private static final int SIDE_BG    = 0xFF0B1220;
    private static final int CARD       = 0xFF121A2A;
    private static final int ACCENT     = 0xFF00B4FF;
    private static final int TEXT_MAIN  = 0xFFE8F4FF;
    private static final int TEXT_MUTED = 0xFF8BA3C7;
    private static final int YT_RED     = 0xFFE53935;
    private static final int DC_BLUE    = 0xFF5865F2;
    private static final int GREEN      = 0xFF00C853;

    private TextView mInstanceLabel;
    private TextView mVersionLabel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Context ctx = requireContext();

        LinearLayout root = new LinearLayout(ctx);
        root.setOrientation(LinearLayout.HORIZONTAL);
        root.setBackgroundColor(BG);
        root.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        // ========== LEFT SIDE NAV ==========
        LinearLayout sideNav = new LinearLayout(ctx);
        sideNav.setOrientation(LinearLayout.VERTICAL);
        sideNav.setBackgroundResource(R.drawable.bg_side_nav);
        sideNav.setGravity(Gravity.CENTER_HORIZONTAL);
        sideNav.setPadding(dp(6), dp(14), dp(6), dp(14));
        sideNav.setLayoutParams(new LinearLayout.LayoutParams(dp(62), ViewGroup.LayoutParams.MATCH_PARENT));

        // Accent bar
        View accent = new View(ctx);
        accent.setBackgroundColor(ACCENT);
        sideNav.addView(accent, new LinearLayout.LayoutParams(dp(3), dp(26)));
        sideNav.addView(space(ctx, 10));

        // Nav items using our drawables
        sideNav.addView(navItem(ctx, R.drawable.ic_nav_home, true, v -> {}));
        sideNav.addView(space(ctx, 8));
        sideNav.addView(navItem(ctx, R.drawable.ic_nav_instances, false, v ->
                Toast.makeText(ctx, "Instances – Phase 2", Toast.LENGTH_SHORT).show()));
        sideNav.addView(space(ctx, 8));
        sideNav.addView(navItem(ctx, R.drawable.ic_nav_controls, false, v ->
                Toast.makeText(ctx, "Controls", Toast.LENGTH_SHORT).show()));
        sideNav.addView(space(ctx, 8));
        sideNav.addView(navItem(ctx, R.drawable.ic_nav_skins, false, v ->
                Toast.makeText(ctx, "Skins", Toast.LENGTH_SHORT).show()));
        sideNav.addView(space(ctx, 8));
        sideNav.addView(navItem(ctx, R.drawable.ic_nav_info, false, v ->
                Toast.makeText(ctx, "About RKB Launcher", Toast.LENGTH_SHORT).show()));

        // Push settings to bottom
        View flex = new View(ctx);
        sideNav.addView(flex, new LinearLayout.LayoutParams(1, 0, 1f));

        sideNav.addView(navItem(ctx, R.drawable.ic_nav_settings, false, v -> {
            try {
                int id = getResources().getIdentifier("setting_button", "id", requireContext().getPackageName());
                if (id != 0) {
                    View btn = requireActivity().findViewById(id);
                    if (btn != null) {
                        btn.performClick();
                        return;
                    }
                }
            } catch (Exception ignored) {}
            Toast.makeText(ctx, "Settings", Toast.LENGTH_SHORT).show();
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
        LinearLayout logoPill = new LinearLayout(ctx);
        logoPill.setOrientation(LinearLayout.HORIZONTAL);
        logoPill.setGravity(Gravity.CENTER_VERTICAL);
        logoPill.setBackgroundResource(R.drawable.bg_pill);
        logoPill.setPadding(dp(8), dp(5), dp(12), dp(5));

        ImageView logoIv = new ImageView(ctx);
        logoIv.setImageResource(R.drawable.logo_rkb);
        logoIv.setLayoutParams(new LinearLayout.LayoutParams(dp(26), dp(26)));
        logoPill.addView(logoIv);
        logoPill.addView(hSpace(ctx, 8));

        LinearLayout logoTxt = new LinearLayout(ctx);
        logoTxt.setOrientation(LinearLayout.VERTICAL);
        TextView t1 = new TextView(ctx);
        t1.setText("RKB LAUNCHER");
        t1.setTextColor(TEXT_MAIN);
        t1.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        t1.setTypeface(Typeface.DEFAULT_BOLD);
        TextView t2 = new TextView(ctx);
        t2.setText("PLAY  •  EXPLORE  •  CREATE");
        t2.setTextColor(TEXT_MUTED);
        t2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9);
        logoTxt.addView(t1);
        logoTxt.addView(t2);
        logoPill.addView(logoTxt);

        topBar.addView(logoPill);
        topBar.addView(hSpace(ctx, 8));

        // YouTube
        TextView yt = chip(ctx, "YouTube", YT_RED);
        yt.setOnClickListener(v -> openUrl("https://youtube.com"));
        topBar.addView(yt);
        topBar.addView(hSpace(ctx, 6));

        // Discord
        TextView dc = chip(ctx, "Discord", DC_BLUE);
        dc.setOnClickListener(v -> openUrl("https://discord.gg"));
        topBar.addView(dc);

        // Spacer
        topBar.addView(new View(ctx), new LinearLayout.LayoutParams(0, 1, 1f));

        // Account pill
        LinearLayout acc = new LinearLayout(ctx);
        acc.setOrientation(LinearLayout.HORIZONTAL);
        acc.setGravity(Gravity.CENTER_VERTICAL);
        acc.setBackgroundResource(R.drawable.bg_pill);
        acc.setPadding(dp(12), dp(6), dp(12), dp(6));
        TextView accTxt = new TextView(ctx);
        accTxt.setText("RKB_GAMERZ  ·  LOCAL");
        accTxt.setTextColor(TEXT_MAIN);
        accTxt.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        accTxt.setTypeface(Typeface.DEFAULT_BOLD);
        acc.addView(accTxt);
        acc.setOnClickListener(v -> {
            try {
                ExtraCore.setValue(ExtraConstants.SELECT_AUTH_METHOD, true);
            } catch (Exception e) {
                Toast.makeText(ctx, "Accounts", Toast.LENGTH_SHORT).show();
            }
        });
        topBar.addView(acc);

        main.addView(topBar);
        main.addView(space(ctx, 10));

        // --- CENTER (skin + launch) ---
        LinearLayout center = new LinearLayout(ctx);
        center.setOrientation(LinearLayout.HORIZONTAL);
        center.setGravity(Gravity.CENTER);
        center.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout skinCol = new LinearLayout(ctx);
        skinCol.setOrientation(LinearLayout.VERTICAL);
        skinCol.setGravity(Gravity.CENTER_HORIZONTAL);

        // Player tag
        TextView playerTag = new TextView(ctx);
        playerTag.setText("Player");
        playerTag.setTextColor(TEXT_MAIN);
        playerTag.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        playerTag.setBackgroundResource(R.drawable.bg_pill);
        playerTag.setPadding(dp(14), dp(4), dp(14), dp(4));
        playerTag.setGravity(Gravity.CENTER);
        skinCol.addView(playerTag);
        skinCol.addView(space(ctx, 8));

        // Skin frame
        FrameLayout skinFrame = new FrameLayout(ctx);
        GradientDrawable skinBg = new GradientDrawable();
        skinBg.setColor(0xFF0D1525);
        skinBg.setCornerRadius(dp(16));
        skinBg.setStroke(dp(2), ACCENT);
        skinFrame.setBackground(skinBg);
        skinFrame.setLayoutParams(new LinearLayout.LayoutParams(dp(130), dp(170)));

        // Simple block skin (placeholder)
        LinearLayout skinInner = new LinearLayout(ctx);
        skinInner.setOrientation(LinearLayout.VERTICAL);
        skinInner.setGravity(Gravity.CENTER_HORIZONTAL);
        skinInner.setPadding(0, dp(14), 0, 0);

        View head = new View(ctx);
        head.setBackground(rounded(0xFF4FC3F7, 6));
        skinInner.addView(head, new LinearLayout.LayoutParams(dp(44), dp(44)));
        skinInner.addView(space(ctx, 3));

        View body = new View(ctx);
        body.setBackground(rounded(0xFF0288D1, 4));
        skinInner.addView(body, new LinearLayout.LayoutParams(dp(52), dp(64)));
        skinInner.addView(space(ctx, 3));

        LinearLayout legs = new LinearLayout(ctx);
        legs.setOrientation(LinearLayout.HORIZONTAL);
        View legL = new View(ctx);
        legL.setBackground(rounded(0xFF01579B, 3));
        View legR = new View(ctx);
        legR.setBackground(rounded(0xFF01579B, 3));
        legs.addView(legL, new LinearLayout.LayoutParams(dp(20), dp(28)));
        legs.addView(hSpace(ctx, 6));
        legs.addView(legR, new LinearLayout.LayoutParams(dp(20), dp(28)));
        skinInner.addView(legs);

        skinFrame.addView(skinInner, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
                Gravity.CENTER));
        skinCol.addView(skinFrame);
        skinCol.addView(space(ctx, 12));

        // LAUNCH button
        TextView launchBtn = new TextView(ctx);
        launchBtn.setText("▶  LAUNCH");
        launchBtn.setTextColor(Color.WHITE);
        launchBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        launchBtn.setTypeface(Typeface.DEFAULT_BOLD);
        launchBtn.setGravity(Gravity.CENTER);
        launchBtn.setBackgroundResource(R.drawable.bg_launch_btn);
        launchBtn.setPadding(dp(34), dp(13), dp(34), dp(13));
        launchBtn.setOnClickListener(v -> doLaunch());
        skinCol.addView(launchBtn);
        skinCol.addView(space(ctx, 8));

        // Version
        mVersionLabel = new TextView(ctx);
        mVersionLabel.setText("▶  Minecraft 1.21.11  ▼");
        mVersionLabel.setTextColor(TEXT_MAIN);
        mVersionLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        mVersionLabel.setGravity(Gravity.CENTER);
        mVersionLabel.setBackgroundResource(R.drawable.bg_card);
        mVersionLabel.setPadding(dp(16), dp(7), dp(16), dp(7));
        mVersionLabel.setOnClickListener(v ->
                Toast.makeText(ctx, "Version picker – Phase 2", Toast.LENGTH_SHORT).show());
        skinCol.addView(mVersionLabel);

        center.addView(skinCol);
        main.addView(center);

        // --- BOTTOM INSTANCE ROW ---
        LinearLayout bottom = new LinearLayout(ctx);
        bottom.setOrientation(LinearLayout.HORIZONTAL);
        bottom.setGravity(Gravity.CENTER_VERTICAL);
        bottom.setPadding(0, dp(6), 0, 0);

        // Instance card
        LinearLayout instCard = new LinearLayout(ctx);
        instCard.setOrientation(LinearLayout.HORIZONTAL);
        instCard.setGravity(Gravity.CENTER_VERTICAL);
        instCard.setBackgroundResource(R.drawable.bg_card);
        instCard.setPadding(dp(8), dp(7), dp(8), dp(7));

        View greenBar = new View(ctx);
        greenBar.setBackgroundColor(GREEN);
        instCard.addView(greenBar, new LinearLayout.LayoutParams(dp(3), dp(26)));
        instCard.addView(hSpace(ctx, 7));

        ImageView grass = new ImageView(ctx);
        grass.setImageResource(R.drawable.ic_grass_block);
        grass.setLayoutParams(new LinearLayout.LayoutParams(dp(26), dp(26)));
        instCard.addView(grass);
        instCard.addView(hSpace(ctx, 7));

        LinearLayout instTxt = new LinearLayout(ctx);
        instTxt.setOrientation(LinearLayout.VERTICAL);
        mInstanceLabel = new TextView(ctx);
        mInstanceLabel.setText("Default");
        mInstanceLabel.setTextColor(TEXT_MAIN);
        mInstanceLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        mInstanceLabel.setTypeface(Typeface.DEFAULT_BOLD);
        TextView ver = new TextView(ctx);
        ver.setText("1.21.11");
        ver.setTextColor(TEXT_MUTED);
        ver.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        instTxt.addView(mInstanceLabel);
        instTxt.addView(ver);
        instCard.addView(instTxt);
        instCard.addView(hSpace(ctx, 8));

        TextView miniPlay = new TextView(ctx);
        miniPlay.setText("▶");
        miniPlay.setTextColor(Color.WHITE);
        miniPlay.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        miniPlay.setGravity(Gravity.CENTER);
        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        circle.setColor(ACCENT);
        miniPlay.setBackground(circle);
        miniPlay.setWidth(dp(26));
        miniPlay.setHeight(dp(26));
        miniPlay.setOnClickListener(v -> doLaunch());
        instCard.addView(miniPlay);

        bottom.addView(instCard);
        bottom.addView(hSpace(ctx, 10));

        // + New Instance
        TextView newInst = new TextView(ctx);
        newInst.setText("+  New Instance");
        newInst.setTextColor(ACCENT);
        newInst.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        newInst.setGravity(Gravity.CENTER);
        GradientDrawable dashed = new GradientDrawable();
        dashed.setColor(Color.TRANSPARENT);
        dashed.setCornerRadius(dp(14));
        dashed.setStroke(dp(1), ACCENT, dp(5), dp(3));
        newInst.setBackground(dashed);
        newInst.setPadding(dp(16), dp(9), dp(16), dp(9));
        newInst.setOnClickListener(v ->
                Toast.makeText(ctx, "New Instance – Phase 2", Toast.LENGTH_SHORT).show());
        bottom.addView(newInst);

        main.addView(bottom);
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

    private View navItem(Context ctx, int drawableRes, boolean active, View.OnClickListener click) {
        FrameLayout wrap = new FrameLayout(ctx);
        int size = dp(42);
        wrap.setLayoutParams(new LinearLayout.LayoutParams(size, size));

        if (active) {
            wrap.setBackgroundResource(R.drawable.bg_nav_active);
        }

        ImageView iv = new ImageView(ctx);
        try {
            iv.setImageResource(drawableRes);
        } catch (Exception ignored) {}
        iv.setColorFilter(active ? ACCENT : TEXT_MUTED);
        FrameLayout.LayoutParams ivLp = new FrameLayout.LayoutParams(dp(22), dp(22), Gravity.CENTER);
        wrap.addView(iv, ivLp);
        wrap.setOnClickListener(click);
        return wrap;
    }

    private TextView chip(Context ctx, String text, int color) {
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
        t.setPadding(dp(12), dp(5), dp(12), dp(5));
        return t;
    }

    private GradientDrawable rounded(int color, int radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(radiusDp));
        return g;
    }

    private View space(Context ctx, int h) {
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