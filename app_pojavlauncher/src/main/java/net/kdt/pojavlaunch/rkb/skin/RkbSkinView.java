package net.kdt.pojavlaunch.rkb.skin;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

/** Draws the front of a player from a 64x64 (or legacy 64x32) skin texture, pixel-exact. */
public class RkbSkinView extends View {
    private final Paint mPaint = new Paint();
    private final Rect mSrc = new Rect();
    private final RectF mDst = new RectF();
    @Nullable private Bitmap mSkin;
    private boolean mSlim;

    public RkbSkinView(Context c) { this(c, null); }

    public RkbSkinView(Context c, @Nullable AttributeSet a) {
        super(c, a);
        mPaint.setFilterBitmap(false); // keep hard pixel edges
        mPaint.setAntiAlias(false);
    }

    public void setSkin(@Nullable Bitmap skin, boolean slim) {
        mSkin = skin;
        mSlim = slim;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Bitmap b = mSkin;
        if (b == null) return;
        boolean legacy = b.getHeight() < 64;
        float u = Math.min(getWidth() / 16f, getHeight() / 32f);
        if (u <= 0) return;
        float ox = (getWidth() - 16 * u) / 2f;
        float oy = (getHeight() - 32 * u) / 2f;
        int aw = mSlim ? 3 : 4;

        // base layer
        part(canvas, b, 8, 8, 8, 8, ox, oy, 4, 0, u, false);                 // head
        part(canvas, b, 20, 20, 8, 12, ox, oy, 4, 8, u, false);              // body
        part(canvas, b, 44, 20, aw, 12, ox, oy, 4 - aw, 8, u, false);        // right arm (viewer's left)
        if (legacy) {
            part(canvas, b, 44, 20, aw, 12, ox, oy, 12, 8, u, true);         // mirrored
            part(canvas, b, 4, 20, 4, 12, ox, oy, 8, 20, u, true);
        } else {
            part(canvas, b, 36, 52, aw, 12, ox, oy, 12, 8, u, false);        // left arm
            part(canvas, b, 20, 52, 4, 12, ox, oy, 8, 20, u, false);         // left leg
        }
        part(canvas, b, 4, 20, 4, 12, ox, oy, 4, 20, u, false);              // right leg

        // overlay layers (hat, jacket, sleeves, pants) exist on 64x64 skins only
        part(canvas, b, 40, 8, 8, 8, ox, oy, 4, 0, u, false);
        if (!legacy) {
            part(canvas, b, 20, 36, 8, 12, ox, oy, 4, 8, u, false);
            part(canvas, b, 44, 36, aw, 12, ox, oy, 4 - aw, 8, u, false);
            part(canvas, b, 52, 52, aw, 12, ox, oy, 12, 8, u, false);
            part(canvas, b, 4, 36, 4, 12, ox, oy, 4, 20, u, false);
            part(canvas, b, 4, 52, 4, 12, ox, oy, 8, 20, u, false);
        }
    }

    private void part(Canvas c, Bitmap b, int sx, int sy, int sw, int sh,
                      float ox, float oy, float dx, float dy, float u, boolean flip) {
        if (sx + sw > b.getWidth() || sy + sh > b.getHeight()) return;
        mSrc.set(sx, sy, sx + sw, sy + sh);
        mDst.set(ox + dx * u, oy + dy * u, ox + (dx + sw) * u, oy + (dy + sh) * u);
        if (!flip) {
            c.drawBitmap(b, mSrc, mDst, mPaint);
        } else {
            c.save();
            c.scale(-1f, 1f, mDst.centerX(), mDst.centerY());
            c.drawBitmap(b, mSrc, mDst, mPaint);
            c.restore();
        }
    }
}
