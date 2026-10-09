package net.kdt.pojavlaunch.rkb.ui;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.kdt.pojavlaunch.PojavProfile;
import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.value.MinecraftAccount;

/** Shared RKB design tokens + small view helpers + the sidebar/header shell used by secondary screens. */
public final class RkbUi {
    public static final int BG = 0xFF05070C;
    public static final int PANEL = 0xFF070D16;
    public static final int CARD = 0xFF0E1423;
    public static final int CARD_2 = 0xFF121A2C;
    public static final int BLUE = 0xFF00A8FF;
    public static final int BORDER = 0xFF16314A;
    public static final int WHITE = 0xFFFFFFFF;
    public static final int MUTED = 0xFF96A8BE;
    public static final int GREEN = 0xFF2ECC71;
    public static final int RED = 0xFFEB232D;
    public static final int DISCORD = 0xFF345DEB;

    private RkbUi() {}

    public static int dp(Context c, float v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }

    public static boolean isCompact(Context c) {
        DisplayMetrics m = c.getResources().getDisplayMetrics();
        return m.widthPixels / m.density < 600f;
    }

    public static GradientDrawable rounded(Context c, int fill, float radiusDp, int stroke, float strokeDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(dp(c, radiusDp));
        if (strokeDp > 0) g.setStroke(Math.max(1, dp(c, strokeDp)), stroke);
        return g;
    }

    public static Drawable ripple(Context c, Drawable bg) {
        return new RippleDrawable(ColorStateList.valueOf(0x3300A8FF), bg, null);
    }

    public static TextView text(Context c, CharSequence s, float sp, int color, boolean bold) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    /** Pill button. filled=true -> neon blue, false -> dark with blue border. */
    public static TextView button(Context c, String label, boolean filled, View.OnClickListener l) {
        TextView b = text(c, label, 13, WHITE, true);
        b.setGravity(Gravity.CENTER);
        b.setSingleLine(true);
        b.setPadding(dp(c, 16), 0, dp(c, 16), 0);
        GradientDrawable bg = filled ? rounded(c, BLUE, 12, 0, 0) : rounded(c, CARD, 12, BORDER, 1);
        b.setBackground(ripple(c, bg));
        b.setMinHeight(dp(c, 40));
        b.setClickable(true);
        b.setFocusable(true);
        if (l != null) b.setOnClickListener(l);
        return b;
    }

    public static LinearLayout card(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(c, 14), dp(c, 12), dp(c, 14), dp(c, 12));
        l.setBackground(rounded(c, CARD, 14, BORDER, 1));
        return l;
    }

    public static View spacer(Context c, int heightDp) {
        View v = new View(c);
        v.setLayoutParams(new LinearLayout.LayoutParams(1, dp(c, heightDp)));
        return v;
    }

    // ---- account helpers (read-only, uses the existing account store) ----
    @Nullable
    public static MinecraftAccount currentAccount(Context c) {
        try {
            return PojavProfile.getCurrentProfileContent(c, null);
        } catch (Exception e) {
            return null;
        }
    }

    public static String accountName(Context c) {
        MinecraftAccount a = currentAccount(c);
        return a == null || a.username == null || a.username.isEmpty() ? "No account" : a.username;
    }

    public static String accountBadge(Context c) {
        MinecraftAccount a = currentAccount(c);
        if (a == null) return "SIGN IN";
        if (a.isDemo()) return "DEMO";
        return a.isMicrosoft ? "MICROSOFT" : "LOCAL";
    }

    // ---- sidebar ----
    public static int navItemSize(Context c) {
        DisplayMetrics m = c.getResources().getDisplayMetrics();
        float hDp = m.heightPixels / m.density;
        int count = RkbNav.Dest.values().length;
        float size = (hDp - 36f) / count - 8f;
        return Math.round(Math.max(40f, Math.min(56f, size)));
    }

    /** Fills the sidebar container with one real, clickable item per destination. */
    public static void populateSidebar(@NonNull LinearLayout sidebar, @NonNull RkbNav.Dest active) {
        final Context c = sidebar.getContext();
        sidebar.removeAllViews();
        int size = navItemSize(c);
        for (RkbNav.Dest d : RkbNav.Dest.values()) {
            ImageView item = new ImageView(c);
            item.setImageResource(d.icon);
            item.setColorFilter(d == active ? WHITE : MUTED);
            int pad = dp(c, size >= 52 ? 13 : 10);
            item.setPadding(pad, pad, pad, pad);
            item.setBackgroundResource(d == active ? R.drawable.bg_rkb_nav_item_active : R.drawable.bg_rkb_nav_item);
            item.setContentDescription(d.label);
            item.setClickable(true);
            item.setFocusable(true);
            item.setOnClickListener(v -> {
                Context ctx = v.getContext();
                while (ctx instanceof android.content.ContextWrapper && !(ctx instanceof Activity)) {
                    ctx = ((android.content.ContextWrapper) ctx).getBaseContext();
                }
                if (ctx instanceof Activity) RkbNav.go((Activity) ctx, d);
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(c, size), dp(c, size));
            lp.bottomMargin = dp(c, 8);
            sidebar.addView(item, lp);
        }
    }

    public static View buildSidebar(Context c, RkbNav.Dest active) {
        LinearLayout col = new LinearLayout(c);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER_HORIZONTAL);
        col.setPadding(dp(c, 10), dp(c, 12), dp(c, 10), dp(c, 12));
        populateSidebar(col, active);
        ScrollView sv = new ScrollView(c);
        sv.setFillViewport(true);
        sv.setVerticalScrollBarEnabled(false);
        sv.setBackgroundResource(R.drawable.bg_rkb_sidebar);
        sv.addView(col, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return sv;
    }

    // ---- header ----
    public static View buildHeader(Context c) {
        boolean compact = isCompact(c);
        LinearLayout header = new LinearLayout(c);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(c, 12), dp(c, 6), dp(c, 12), dp(c, 6));
        header.setBackgroundResource(R.drawable.bg_rkb_header);

        TextView logo = text(c, "R", 20, BLUE, true);
        logo.setGravity(Gravity.CENTER);
        logo.setBackgroundResource(R.drawable.bg_rkb_logo);
        header.addView(logo, new LinearLayout.LayoutParams(dp(c, 40), dp(c, 40)));

        LinearLayout names = new LinearLayout(c);
        names.setOrientation(LinearLayout.VERTICAL);
        names.setPadding(dp(c, 10), 0, 0, 0);
        names.addView(text(c, "RKB LAUNCHER", 16, WHITE, true));
        if (!compact) names.addView(text(c, "PLAY  •  EXPLORE  •  CREATE", 8, MUTED, true));
        header.addView(names, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView yt = pillLink(c, "YouTube", RED, () -> RkbLinks.open(c, RkbLinks.YOUTUBE, "YouTube"));
        TextView dc = pillLink(c, "Discord", DISCORD, () -> RkbLinks.open(c, RkbLinks.DISCORD, "Discord"));
        header.addView(yt);
        header.addView(dc);

        TextView acc = text(c, accountName(c) + (compact ? "" : "  " + accountBadge(c)), 12, WHITE, true);
        acc.setSingleLine(true);
        acc.setPadding(dp(c, 12), 0, dp(c, 12), 0);
        acc.setGravity(Gravity.CENTER);
        acc.setBackground(ripple(c, rounded(c, CARD, 12, BORDER, 1)));
        acc.setOnClickListener(v -> {
            Context ctx = v.getContext();
            while (ctx instanceof android.content.ContextWrapper && !(ctx instanceof Activity)) {
                ctx = ((android.content.ContextWrapper) ctx).getBaseContext();
            }
            if (ctx instanceof Activity) RkbNav.go((Activity) ctx, RkbNav.Dest.SKIN);
        });
        LinearLayout.LayoutParams alp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(c, 36));
        alp.leftMargin = dp(c, 8);
        header.addView(acc, alp);
        return header;
    }

    private static TextView pillLink(Context c, String label, int color, Runnable r) {
        TextView t = text(c, label, 12, WHITE, true);
        t.setGravity(Gravity.CENTER);
        t.setBackground(ripple(c, rounded(c, color, 12, 0, 0)));
        t.setClickable(true);
        t.setOnClickListener(v -> r.run());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(c, isCompact(c) ? 72 : 96), dp(c, 36));
        lp.leftMargin = dp(c, 8);
        t.setLayoutParams(lp);
        return t;
    }

    /**
     * Standard RKB screen: sidebar + header + title + content.
     * The content view is given the remaining space and handles its own scrolling.
     */
    public static View scaffold(Context c, RkbNav.Dest active, String title, String subtitle, View content) {
        LinearLayout root = new LinearLayout(c);
        root.setOrientation(LinearLayout.HORIZONTAL);
        root.setBackgroundColor(BG);
        root.addView(buildSidebar(c, active), new LinearLayout.LayoutParams(dp(c, navItemSize(c) + 22), ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout col = new LinearLayout(c);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(c, 12), dp(c, 8), dp(c, 12), dp(c, 8));
        col.addView(buildHeader(c), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(c, 54)));

        LinearLayout titles = new LinearLayout(c);
        titles.setOrientation(LinearLayout.VERTICAL);
        titles.setPadding(dp(c, 4), dp(c, 10), 0, dp(c, 8));
        titles.addView(text(c, title, 20, WHITE, true));
        if (subtitle != null) titles.addView(text(c, subtitle, 12, MUTED, false));
        col.addView(titles);

        col.addView(content, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        root.addView(col, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));
        return root;
    }

    public static FrameLayout.LayoutParams match() {
        return new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    }

    public static int withAlpha(int color, float alpha) {
        return Color.argb(Math.round(255 * alpha), Color.red(color), Color.green(color), Color.blue(color));
    }
}
