package net.kdt.pojavlaunch.rkb.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.provider.Settings;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.OvershootInterpolator;
import android.view.animation.PathInterpolator;

import androidx.annotation.NonNull;

import net.kdt.pojavlaunch.prefs.LauncherPreferences;

/**
 * RKB motion language (modelled on the reference recording):
 *  - screens fade in (200 ms) and their content cascades up with a short stagger,
 *  - the sidebar slides in from the left, the player panel from the right,
 *  - buttons/cards squash to ~96% on touch and spring back,
 *  - dialogs pop in (res/anim/rkb_dialog_*), screens cross-fade (res/anim/rkb_fade_*),
 *  - an optional slow idle float on the player skin.
 *
 * Everything is driven by two real settings (Settings > RKB interface):
 *  rkb_anim_enabled (also forced off when the system "animator duration scale" is 0) and
 *  rkb_anim_amplitude (1..10, 5 = reference; scales travel distance and duration).
 * Only ViewPropertyAnimator / one cancellable ValueAnimator are used: no allocations per frame, nothing
 * outlives its view (idle animators cancel when the view detaches).
 */
public final class RkbMotion {
    public static final String PREF_ENABLED = "rkb_anim_enabled";
    public static final String PREF_AMPLITUDE = "rkb_anim_amplitude";
    public static final String PREF_AMBIENT = "rkb_anim_ambient";

    /** cubic-bezier(0.05, 0.7, 0.1, 1.0): fast start, long soft landing. */
    private static final PathInterpolator EMPHASIZED_DECELERATE = new PathInterpolator(0.05f, 0.7f, 0.1f, 1f);
    private static final OvershootInterpolator SPRING_BACK = new OvershootInterpolator(2.2f);

    private RkbMotion() {}

    // ------------------------------------------------------------------ settings

    public static boolean enabled(Context c) {
        try {
            if (LauncherPreferences.DEFAULT_PREF != null
                    && !LauncherPreferences.DEFAULT_PREF.getBoolean(PREF_ENABLED, true)) return false;
            // honour "Remove animations" / animator scale 0 from the system settings
            return Settings.Global.getFloat(c.getContentResolver(), Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f;
        } catch (Exception e) {
            return true;
        }
    }

    /** 0.2 .. 2.0 (setting 1..10, default 5 = 1.0). */
    public static float amplitude(Context c) {
        int v = 5;
        if (LauncherPreferences.DEFAULT_PREF != null) v = LauncherPreferences.DEFAULT_PREF.getInt(PREF_AMPLITUDE, 5);
        return Math.max(1, Math.min(10, v)) / 5f;
    }

    public static boolean ambientEnabled(Context c) {
        return enabled(c) && LauncherPreferences.DEFAULT_PREF != null
                && LauncherPreferences.DEFAULT_PREF.getBoolean(PREF_AMBIENT, false);
    }

    private static float dp(View v, float value) {
        return value * v.getResources().getDisplayMetrics().density;
    }

    // ------------------------------------------------------------------ entrance

    /**
     * Fade + slide a view in from an offset. The view is only hidden once the first frame is about to
     * be drawn, so a view that never gets drawn can never stay invisible.
     */
    public static void slideIn(final View view, final float dxDp, final float dyDp, final long delayMs, final long durationMs) {
        if (view == null || !enabled(view.getContext())) return;
        final float amp = amplitude(view.getContext());
        final float dx = dp(view, dxDp) * amp, dy = dp(view, dyDp) * amp;
        final long dur = Math.round(durationMs * (0.6f + 0.4f * amp));
        Runnable start = () -> {
            view.setAlpha(0f);
            view.setTranslationX(dx);
            view.setTranslationY(dy);
            view.animate().alpha(1f).translationX(0f).translationY(0f)
                    .setStartDelay(delayMs).setDuration(dur)
                    .setInterpolator(EMPHASIZED_DECELERATE).start();
        };
        if (view.isAttachedToWindow() && view.getWidth() > 0) {
            start.run();
            return;
        }
        view.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                ViewTreeObserver vto = view.getViewTreeObserver();
                if (vto.isAlive()) vto.removeOnPreDrawListener(this);
                start.run();
                return true;
            }
        });
    }

    /** Children of a container rise in one after another (max 10 so long lists do not lag). */
    public static void cascade(ViewGroup container, long firstDelayMs, long staggerMs) {
        if (container == null || !enabled(container.getContext())) return;
        int n = Math.min(container.getChildCount(), 10);
        for (int i = 0; i < n; i++) {
            slideIn(container.getChildAt(i), 0f, 14f, firstDelayMs + i * staggerMs, 300);
        }
    }

    /** Standard screen entrance used by the RKB scaffold: title block, then the content block. */
    public static void revealScreen(View titles, View content) {
        slideIn(titles, 0f, 8f, 0, 260);
        slideIn(content, 0f, 14f, 60, 320);
    }

    // ------------------------------------------------------------------ touch feedback

    /** Squash to 96% while pressed, spring back on release. Does not consume the touch. */
    public static void press(View... views) {
        for (View v : views) {
            if (v == null) continue;
            v.setOnTouchListener((view, event) -> {
                if (!enabled(view.getContext())) return false;
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        view.animate().scaleX(0.96f).scaleY(0.96f).setDuration(90)
                                .setInterpolator(EMPHASIZED_DECELERATE).start();
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        view.animate().scaleX(1f).scaleY(1f).setDuration(190)
                                .setInterpolator(SPRING_BACK).start();
                        break;
                    default:
                        break;
                }
                return false;
            });
        }
    }

    // ------------------------------------------------------------------ ambient

    /** Slow idle float (reference: the player skin hovers). Cancels itself when the view detaches. */
    public static void floatIdle(final View view) {
        if (view == null || !ambientEnabled(view.getContext())) return;
        final float amplitudePx = dp(view, 3f) * amplitude(view.getContext());
        final ValueAnimator animator = ValueAnimator.ofFloat(-1f, 1f);
        animator.setDuration(2600);
        animator.setRepeatMode(ValueAnimator.REVERSE);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator());
        animator.addUpdateListener(a -> view.setTranslationY((float) a.getAnimatedValue() * amplitudePx));
        view.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override public void onViewAttachedToWindow(@NonNull View v) { if (!animator.isStarted()) animator.start(); }
            @Override public void onViewDetachedFromWindow(@NonNull View v) {
                animator.cancel();
                v.setTranslationY(0f);
                v.removeOnAttachStateChangeListener(this);
            }
        });
        if (view.isAttachedToWindow()) animator.start();
    }
}
