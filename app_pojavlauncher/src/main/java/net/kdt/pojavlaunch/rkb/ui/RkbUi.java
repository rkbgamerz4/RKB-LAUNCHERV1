package net.kdt.pojavlaunch.rkb.ui;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.authenticator.accounts.Account;
import net.kdt.pojavlaunch.authenticator.accounts.Accounts;

/** Shared RKB design tokens, helpers and the reference top-navigation shell. */
public final class RkbUi {
    public static final int BG = 0xFF1878C8;
    public static final int SKY_TOP = 0xFF2196E8;
    public static final int SKY_BOTTOM = 0xFF0D4E96;
    public static final int PANEL = 0x99122B46;
    public static final int CARD = 0xB3122438;
    public static final int CARD_2 = 0xCC1B3552;
    public static final int BLUE = 0xFF38A9FF;
    public static final int BORDER = 0x66FFFFFF;
    public static final int WHITE = 0xFFFFFFFF;
    public static final int MUTED = 0xD6E8F6;
    public static final int GREEN = 0xFF55D98A;
    public static final int RED = 0xFFFF6B6B;
    public static final int DISCORD = 0xFF5865F2;

    private RkbUi() {}

    public static int dp(Context c, float v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }

    public static boolean isShort(Context c) {
        DisplayMetrics m = c.getResources().getDisplayMetrics();
        return m.heightPixels / m.density < 480f;
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
        return new RippleDrawable(ColorStateList.valueOf(0x33FFFFFF), bg, null);
    }

    public static TextView text(Context c, CharSequence s, float sp, int color, boolean bold) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    /** Pill button. filled=true -> bright blue, false -> translucent glass. */
    public static TextView button(Context c, String label, boolean filled, View.OnClickListener l) {
        TextView b = text(c, label, 13, WHITE, true);
        b.setGravity(Gravity.CENTER);
        b.setSingleLine(true);
        b.setPadding(dp(c, 16), 0, dp(c, 16), 0);
        int fill = filled ? BLUE : CARD_2;
        int border = filled ? 0x00000000 : BORDER;
        b.setBackground(ripple(c, rounded(c, fill, 12, border, filled ? 0 : 1)));
        b.setMinHeight(dp(c, 42));
        b.setClickable(true);
        b.setFocusable(true);
        if (l != null) b.setOnClickListener(l);
        RkbMotion.press(b);
        return b;
    }

    public static LinearLayout card(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(c, 16), dp(c, 14), dp(c, 16), dp(c, 14));
        l.setBackground(rounded(c, CARD, 18, BORDER, 1));
        return l;
    }

    public static View spacer(Context c, int heightDp) {
        View v = new View(c);
        v.setLayoutParams(new LinearLayout.LayoutParams(1, dp(c, heightDp)));
        return v;
    }

    @Nullable
    public static Activity activityFrom(Context c) {
        Context ctx = c;
        while (ctx instanceof android.content.ContextWrapper && !(ctx instanceof Activity)) {
            ctx = ((android.content.ContextWrapper) ctx).getBaseContext();
        }
        return ctx instanceof Activity ? (Activity) ctx : null;
    }

    // ---- account helpers (this branch's real account store) ----
    @Nullable
    public static Account currentAccount(Context c) {
        try {
            return Accounts.getCurrent();
        } catch (Exception e) {
            return null;
        }
    }

    public static String accountName(Context c) {
        Account a = currentAccount(c);
        return a == null || a.username == null || a.username.isEmpty() ? "No account" : a.username;
    }

    public static String accountBadge(Context c) {
        return accountBadge(currentAccount(c));
    }

    public static String accountBadge(Account a) {
        if (a == null) return "SIGN IN";
        if (a.isLocal()) return "OFFLINE";
        return a.authType == null ? "MICROSOFT" : a.authType.name().replace('_', ' ');
    }

    /** Blocky blue Minecraft-like landscape, painted directly so no external image is invented. */
    public static Drawable scenicBackground() {
        return new Drawable() {
            private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

            @Override public void draw(@NonNull Canvas canvas) {
                int w = getBounds().width();
                int h = getBounds().height();
                if (w <= 0 || h <= 0) return;

                mPaint.setShader(new LinearGradient(0, 0, w * 0.25f, h,
                        SKY_TOP, SKY_BOTTOM, Shader.TileMode.CLAMP));
                mPaint.setColor(SKY_TOP);
                canvas.drawRect(0, 0, w, h, mPaint);
                mPaint.setShader(null);

                // Distant block clouds.
                mPaint.setColor(0x30FFFFFF);
                cloud(canvas, w * 0.08f, h * 0.10f, w * 0.10f);
                cloud(canvas, w * 0.62f, h * 0.07f, w * 0.13f);
                cloud(canvas, w * 0.38f, h * 0.24f, w * 0.07f);

                // Layered voxel hills at the bottom of the screen.
                mPaint.setColor(0x2A0B3D6B);
                canvas.drawRect(0, h * 0.70f, w * 0.28f, h, mPaint);
                canvas.drawRect(w * 0.24f, h * 0.62f, w * 0.52f, h, mPaint);
                canvas.drawRect(w * 0.49f, h * 0.74f, w * 0.73f, h, mPaint);
                canvas.drawRect(w * 0.70f, h * 0.58f, w, h, mPaint);

                mPaint.setColor(0x22245C3D);
                canvas.drawRect(0, h * 0.84f, w, h, mPaint);
                mPaint.setColor(0x1AFFFFFF);
                for (int i = 0; i < 14; i++) {
                    float x = w * i / 14f;
                    canvas.drawRect(x, h * 0.94f, x + w / 28f, h, mPaint);
                }
            }

            private void cloud(Canvas canvas, float x, float y, float s) {
                canvas.drawRect(x, y, x + s * 2.2f, y + s * 0.55f, mPaint);
                canvas.drawRect(x + s * 0.45f, y - s * 0.45f, x + s * 1.65f, y + s * 0.55f, mPaint);
                canvas.drawRect(x + s * 0.9f, y - s * 0.85f, x + s * 1.35f, y + s * 0.55f, mPaint);
            }

            @Override public void setAlpha(int alpha) { mPaint.setAlpha(alpha); invalidateSelf(); }
            @Override public void setColorFilter(ColorFilter colorFilter) { mPaint.setColorFilter(colorFilter); invalidateSelf(); }
            @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
        };
    }

    // ---- top navigation ----
    public static View buildTopNav(Context c, RkbNav.Dest active) {
        HorizontalScrollView scroll = new HorizontalScrollView(c);
        scroll.setHorizontalScrollBarEnabled(false);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);

        LinearLayout row = new LinearLayout(c);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(c, 12), dp(c, 6), dp(c, 12), dp(c, 6));

        TextView brand = text(c, "RKB", 14, WHITE, true);
        brand.setGravity(Gravity.CENTER);
        brand.setBackground(ripple(c, rounded(c, 0x44FFFFFF, 11, BORDER, 1)));
        row.addView(brand, new LinearLayout.LayoutParams(dp(c, 56), dp(c, 34)));

        final int h = dp(c, isShort(c) ? 34 : 38);
        for (RkbNav.Dest d : RkbNav.TOP_NAV) {
            boolean selected = d == active;
            TextView pill = text(c, d.navLabel, 12, selected ? SKY_BOTTOM : WHITE, true);
            pill.setGravity(Gravity.CENTER);
            pill.setSingleLine(true);
            pill.setPadding(dp(c, 14), 0, dp(c, 14), 0);
            pill.setBackground(ripple(c, rounded(c,
                    selected ? WHITE : 0x33FFFFFF, 18,
                    selected ? 0x00000000 : BORDER, selected ? 0 : 1)));
            pill.setClickable(true);
            pill.setFocusable(true);
            pill.setOnClickListener(v -> {
                Activity a = activityFrom(v.getContext());
                if (a != null) RkbNav.go(a, d);
            });
            RkbMotion.press(pill);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, h);
            lp.leftMargin = dp(c, 8);
            row.addView(pill, lp);
        }
        scroll.addView(row, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        scroll.setBackground(rounded(c, 0x330A2A4C, 0, 0, 0));
        return scroll;
    }

    /** Header used by secondary screens: title/subtitle on the left, real actions on the right. */
    public static View buildHeader(Context c, String title, String subtitle) {
        LinearLayout header = new LinearLayout(c);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(c, 16), dp(c, isShort(c) ? 6 : 12), dp(c, 16), dp(c, isShort(c) ? 4 : 8));

        LinearLayout names = new LinearLayout(c);
        names.setOrientation(LinearLayout.VERTICAL);
        names.addView(text(c, title, isShort(c) ? 17 : 22, WHITE, true));
        if (subtitle != null && !isShort(c)) {
            TextView sub = text(c, subtitle, 12, MUTED, false);
            sub.setPadding(0, dp(c, 2), 0, 0);
            names.addView(sub);
        }
        header.addView(names, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        header.addView(iconButton(c, R.drawable.ic_rkb_nav_skin, "Account", v -> {
            Activity a = activityFrom(v.getContext());
            if (a != null) RkbNav.openAccount(a);
        }));
        header.addView(iconButton(c, R.drawable.ic_rkb_nav_settings, "Settings",
                v -> {
                    Activity a = activityFrom(v.getContext());
                    if (a != null) RkbNav.go(a, RkbNav.Dest.SETTINGS);
                }));
        header.addView(letterButton(c, "DC", DISCORD, "Discord",
                v -> RkbLinks.open(v.getContext(), RkbLinks.DISCORD, "Discord")));
        header.addView(letterButton(c, "GH", 0xFF1F2937, "GitHub",
                v -> RkbLinks.open(v.getContext(), RkbLinks.GITHUB, "GitHub")));
        return header;
    }

    private static View iconButton(Context c, int res, String desc, View.OnClickListener l) {
        ImageView iv = new ImageView(c);
        iv.setImageResource(res);
        iv.setColorFilter(WHITE);
        iv.setPadding(dp(c, 9), dp(c, 9), dp(c, 9), dp(c, 9));
        iv.setBackground(ripple(c, rounded(c, 0x33FFFFFF, 18, BORDER, 1)));
        iv.setContentDescription(desc);
        iv.setClickable(true);
        iv.setOnClickListener(l);
        RkbMotion.press(iv);
        return iv;
    }

    private static View letterButton(Context c, String label, int color, String desc, View.OnClickListener l) {
        TextView t = text(c, label, 10, WHITE, true);
        t.setGravity(Gravity.CENTER);
        t.setBackground(ripple(c, rounded(c, color, 18, BORDER, 1)));
        t.setContentDescription(desc);
        t.setClickable(true);
        t.setOnClickListener(l);
        RkbMotion.press(t);
        return t;
    }

    /**
     * Standard RKB screen: top pill navigation + title/actions + content.
     * The content view is given the remaining space and handles its own scrolling.
     */
    public static View scaffold(Context c, RkbNav.Dest active, String title, String subtitle, View content) {
        LinearLayout root = new LinearLayout(c);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackground(scenicBackground());
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            v.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                    insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            return insets;
        });

        root.addView(buildTopNav(c, active), new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout header = new LinearLayout(c);
        header.setOrientation(LinearLayout.VERTICAL);
        header.addView(buildHeader(c, title, subtitle));
        root.addView(header, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        root.addView(content, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        RkbMotion.revealScreen(header, content);
        return root;
    }

    public static FrameLayout.LayoutParams match() {
        return new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
    }

    public static int withAlpha(int color, float alpha) {
        return Color.argb(Math.round(255 * alpha), Color.red(color), Color.green(color), Color.blue(color));
    }
}
