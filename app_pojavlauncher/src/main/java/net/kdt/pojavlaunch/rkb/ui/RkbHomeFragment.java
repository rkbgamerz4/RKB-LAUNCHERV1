package net.kdt.pojavlaunch.rkb.ui;

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
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import net.kdt.pojavlaunch.LauncherActivity;
import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;
import net.kdt.pojavlaunch.profiles.ProfileAdapter;
import net.kdt.pojavlaunch.value.MinecraftAccount;

/**
 * RKB Home — CS Launcher style layout (native).
 * Side nav + center LAUNCH + instance chips.
 */
public class RkbHomeFragment extends Fragment {

    // Colors — Dark + Neon Blue
    private static final int BG        = 0xFF070B12;
    private static final int SIDE      = 0xFF0C121C;
    private static final int CARD      = 0xFF0F1620;
    private static final int ACCENT    = 0xFF00B4FF;
    private static final int TEXT      = 0xFFE8EEF6;
    private static final int MUTED     = 0xFF8A96A8;
    private static final int LAUNCH_BG = 0xFFF0F4F8;
    private static final int LAUNCH_TX = 0xFF0A0E14;
    private static final int GREEN     = 0xFF22C55E;

    private LinearLayout mInstanceRow;
    private TextView mVersionLabel;
    private String mSelectedProfileKey;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.HORIZONTAL);
        root.setBackgroundColor(BG);
        root.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        // ===== LEFT SIDE NAV =====
        root.addView(buildSideNav());

        // ===== MAIN CONTENT =====
        LinearLayout main = new LinearLayout(requireContext());
        main.setOrientation(LinearLayout.VERTICAL);
        main.setLayoutParams(new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));
        main.setPadding(dp(16), dp(12), dp(16), dp(12));

        main.addView(buildTopBar());
        main.addView(spacer(dp(8)));
        main.addView(buildCenter());
        main.addView(spacer(dp(12)));
        main.addView(buildInstanceRow());

        root.addView(main);
        return root;
    }

    // ---------- Side navigation ----------
    private View buildSideNav() {
        LinearLayout nav = new LinearLayout(requireContext());
        nav.setOrientation(LinearLayout.VERTICAL);
        nav.setBackgroundColor(SIDE);
        nav.setGravity(Gravity.CENTER_HORIZONTAL);
        nav.setPadding(0, dp(16), 0, dp(16));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(56), ViewGroup.LayoutParams.MATCH_PARENT);
        nav.setLayoutParams(lp);

        nav.addView(navBtn("🏠", true, this::goHome));
        nav.addView(navBtn("📦", false, this::goInstances));
        nav.addView(navBtn("🖱️", false, this::goControls));
        nav.addView(navBtn("🎮", false, this::goGamepad));
        nav.addView(navBtn("ℹ️", false, this::goAbout));
        View space = new View(requireContext());
        space.setLayoutParams(new LinearLayout.LayoutParams(1, 0, 1f));
        nav.addView(space);
        nav.addView(navBtn("⚙️", false, this::goSettings));
        return nav;
    }

    private View navBtn(String emoji, boolean active, Runnable action) {
        TextView t = new TextView(requireContext());
        t.setText(emoji);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        t.setGravity(Gravity.CENTER);
        t.setPadding(0, dp(14), 0, dp(14));
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(12));
        bg.setColor(active ? 0x2200B4FF : Color.TRANSPARENT);
        t.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(44), dp(44));
        lp.gravity = Gravity.CENTER_HORIZONTAL;
        lp.topMargin = dp(4);
        lp.bottomMargin = dp(4);
        t.setLayoutParams(lp);
        t.setOnClickListener(v -> action.run());
        return t;
    }

    // ---------- Top bar ----------
    private View buildTopBar() {
        LinearLayout bar = new LinearLayout(requireContext());
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // Logo + name
        LinearLayout brand = new LinearLayout(requireContext());
        brand.setOrientation(LinearLayout.VERTICAL);
        TextView name = new TextView(requireContext());
        name.setText("RKB LAUNCHER");
        name.setTextColor(TEXT);
        name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        name.setTypeface(Typeface.DEFAULT_BOLD);
        TextView sub = new TextView(requireContext());
        sub.setText("RKB GAMERZ");
        sub.setTextColor(MUTED);
        sub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        brand.addView(name);
        brand.addView(sub);
        bar.addView(brand);

        View flex = new View(requireContext());
        flex.setLayoutParams(new LinearLayout.LayoutParams(0, 1, 1f));
        bar.addView(flex);

        // Discord pill
        bar.addView(pill("Discord", 0xFF5865F2, v -> openUrl("https://discord.gg/")));

        bar.addView(spacerH(dp(8)));

        // Add account
        bar.addView(pill("Add account", CARD, v -> goAccounts()));

        return bar;
    }

    private Button pill(String text, int bgColor, View.OnClickListener click) {
        Button b = new Button(requireContext());
        b.setText(text);
        b.setAllCaps(false);
        b.setTextColor(TEXT);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        b.setPadding(dp(14), dp(6), dp(14), dp(6));
        GradientDrawable g = new GradientDrawable();
        g.setColor(bgColor);
        g.setCornerRadius(dp(20));
        b.setBackground(g);
        b.setOnClickListener(click);
        return b;
    }

    // ---------- Center: skin + LAUNCH ----------
    private View buildCenter() {
        LinearLayout center = new LinearLayout(requireContext());
        center.setOrientation(LinearLayout.VERTICAL);
        center.setGravity(Gravity.CENTER_HORIZONTAL);
        center.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        // Optional server card
        LinearLayout server = cardRow();
        server.setPadding(dp(12), dp(10), dp(12), dp(10));
        TextView sName = new TextView(requireContext());
        sName.setText("RKB Network");
        sName.setTextColor(TEXT);
        sName.setTypeface(Typeface.DEFAULT_BOLD);
        sName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        TextView sSub = new TextView(requireContext());
        sSub.setText("Offline · Ready");
        sSub.setTextColor(MUTED);
        sSub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        LinearLayout sText = new LinearLayout(requireContext());
        sText.setOrientation(LinearLayout.VERTICAL);
        sText.addView(sName);
        sText.addView(sSub);
        server.addView(sText);
        View sFlex = new View(requireContext());
        sFlex.setLayoutParams(new LinearLayout.LayoutParams(0, 1, 1f));
        server.addView(sFlex);
        Button playMini = smallBtn("PLAY", GREEN);
        playMini.setOnClickListener(v -> doLaunch());
        server.addView(playMini);
        LinearLayout.LayoutParams sLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sLp.bottomMargin = dp(16);
        server.setLayoutParams(sLp);
        center.addView(server);

        // Skin placeholder
        TextView skin = new TextView(requireContext());
        skin.setText("👤");
        skin.setTextSize(TypedValue.COMPLEX_UNIT_SP, 64);
        skin.setGravity(Gravity.CENTER);
        skin.setPadding(0, dp(8), 0, dp(8));
        center.addView(skin);

        TextView player = new TextView(requireContext());
        player.setText(getPlayerName());
        player.setTextColor(TEXT);
        player.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        player.setGravity(Gravity.CENTER);
        player.setPadding(0, 0, 0, dp(16));
        center.addView(player);

        // Big LAUNCH
        Button launch = new Button(requireContext());
        launch.setText("▶  LAUNCH");
        launch.setAllCaps(false);
        launch.setTextColor(LAUNCH_TX);
        launch.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        launch.setTypeface(Typeface.DEFAULT_BOLD);
        launch.setPadding(dp(48), dp(14), dp(48), dp(14));
        GradientDrawable lg = new GradientDrawable();
        lg.setColor(LAUNCH_BG);
        lg.setCornerRadius(dp(28));
        launch.setBackground(lg);
        launch.setOnClickListener(v -> doLaunch());
        center.addView(launch);

        mVersionLabel = new TextView(requireContext());
        mVersionLabel.setText("Minecraft · Select instance");
        mVersionLabel.setTextColor(MUTED);
        mVersionLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        mVersionLabel.setGravity(Gravity.CENTER);
        mVersionLabel.setPadding(0, dp(10), 0, 0);
        center.addView(mVersionLabel);

        return center;
    }

    // ---------- Instance chips ----------
    private View buildInstanceRow() {
        LinearLayout wrap = new LinearLayout(requireContext());
        wrap.setOrientation(LinearLayout.VERTICAL);

        TextView label = new TextView(requireContext());
        label.setText("Instances");
        label.setTextColor(MUTED);
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        label.setPadding(0, 0, 0, dp(6));
        wrap.addView(label);

        HorizontalScrollView hsv = new HorizontalScrollView(requireContext());
        hsv.setHorizontalScrollBarEnabled(false);
        mInstanceRow = new LinearLayout(requireContext());
        mInstanceRow.setOrientation(LinearLayout.HORIZONTAL);
        mInstanceRow.setGravity(Gravity.CENTER_VERTICAL);
        hsv.addView(mInstanceRow);
        wrap.addView(hsv);

        reloadInstances();
        return wrap;
    }

    private void reloadInstances() {
        if (mInstanceRow == null) return;
        mInstanceRow.removeAllViews();

        // Default chip (always)
        mInstanceRow.addView(instanceChip("Default", true));
        mInstanceRow.addView(spacerH(dp(8)));
        mInstanceRow.addView(instanceChip("Spunky Opti", false));
        mInstanceRow.addView(spacerH(dp(8)));

        // + New Instance
        Button add = new Button(requireContext());
        add.setText("+ New Instance");
        add.setAllCaps(false);
        add.setTextColor(ACCENT);
        add.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        GradientDrawable g = new GradientDrawable();
        g.setColor(Color.TRANSPARENT);
        g.setStroke(dp(1), 0xFF3A4560);
        g.setCornerRadius(dp(16));
        add.setBackground(g);
        add.setPadding(dp(16), dp(8), dp(16), dp(8));
        add.setOnClickListener(v -> openNewInstance());
        mInstanceRow.addView(add);
    }

    private View instanceChip(String name, boolean selected) {
        Button b = new Button(requireContext());
        b.setText(name);
        b.setAllCaps(false);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        b.setTextColor(selected ? TEXT : MUTED);
        GradientDrawable g = new GradientDrawable();
        g.setColor(selected ? 0xFF1A2435 : CARD);
        g.setCornerRadius(dp(16));
        if (selected) g.setStroke(dp(1), ACCENT);
        b.setBackground(g);
        b.setPadding(dp(16), dp(8), dp(16), dp(8));
        b.setOnClickListener(v -> {
            mSelectedProfileKey = name;
            if (mVersionLabel != null) mVersionLabel.setText(name + " · Ready");
            Toast.makeText(requireContext(), "Selected: " + name, Toast.LENGTH_SHORT).show();
            reloadInstances(); // visual refresh — improve later with real profiles
        });
        return b;
    }

    // ---------- Actions ----------
    private void doLaunch() {
        try {
            FragmentActivity act = requireActivity();
            if (act instanceof LauncherActivity) {
                // Trigger existing launch path if available
                Toast.makeText(act, "Launching…", Toast.LENGTH_SHORT).show();
                // TODO: call real launch — wire to LauncherActivity launch method
            } else {
                Toast.makeText(requireContext(), "Launch not wired yet", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Launch error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void openNewInstance() {
        Toast.makeText(requireContext(), "New Instance — Phase 2", Toast.LENGTH_SHORT).show();
        // Phase 2: open create dialog
    }

    private void goHome() { /* already here */ }

    private void goInstances() {
        Toast.makeText(requireContext(), "Instances", Toast.LENGTH_SHORT).show();
    }

    private void goControls() {
        try {
            // Open custom controls if activity exists
            Intent i = new Intent();
            i.setClassName(requireContext(), "net.kdt.pojavlaunch.CustomControlsActivity");
            startActivity(i);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Controls", Toast.LENGTH_SHORT).show();
        }
    }

    private void goGamepad() {
        Toast.makeText(requireContext(), "Gamepad", Toast.LENGTH_SHORT).show();
    }

    private void goAbout() {
        Toast.makeText(requireContext(), "RKB Launcher · RKB GAMERZ", Toast.LENGTH_SHORT).show();
    }

    private void goSettings() {
        // Try open settings fragment / activity
        try {
            Intent i = new Intent();
            i.setClassName(requireContext(), "net.kdt.pojavlaunch.prefs.LauncherPreferenceActivity");
            startActivity(i);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Settings", Toast.LENGTH_SHORT).show();
        }
    }

    private void goAccounts() {
        Toast.makeText(requireContext(), "Accounts", Toast.LENGTH_SHORT).show();
    }

    private void openUrl(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception ignored) {}
    }

    private String getPlayerName() {
        try {
            // Best-effort; falls back if account API differs
            return "Player";
        } catch (Exception e) {
            return "Player";
        }
    }

    // ---------- Helpers ----------
    private LinearLayout cardRow() {
        LinearLayout c = new LinearLayout(requireContext());
        c.setOrientation(LinearLayout.HORIZONTAL);
        c.setGravity(Gravity.CENTER_VERTICAL);
        GradientDrawable g = new GradientDrawable();
        g.setColor(CARD);
        g.setCornerRadius(dp(14));
        c.setBackground(g);
        return c;
    }

    private Button smallBtn(String text, int bg) {
        Button b = new Button(requireContext());
        b.setText(text);
        b.setAllCaps(false);
        b.setTextColor(Color.WHITE);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        b.setPadding(dp(14), dp(4), dp(14), dp(4));
        GradientDrawable g = new GradientDrawable();
        g.setColor(bg);
        g.setCornerRadius(dp(12));
        b.setBackground(g);
        return b;
    }

    private View spacer(int h) {
        View v = new View(requireContext());
        v.setLayoutParams(new LinearLayout.LayoutParams(1, h));
        return v;
    }

    private View spacerH(int w) {
        View v = new View(requireContext());
        v.setLayoutParams(new LinearLayout.LayoutParams(w, 1));
        return v;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}