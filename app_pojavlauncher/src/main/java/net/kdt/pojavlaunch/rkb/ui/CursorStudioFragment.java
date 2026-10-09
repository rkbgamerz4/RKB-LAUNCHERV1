package net.kdt.pojavlaunch.rkb.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import net.kdt.pojavlaunch.rkb.cursor.CursorStudioPrefs;

/**
 * RKB Cursor Studio. Edits are staged in this screen and written on Save. The saved values are
 * read by the in-game virtual mouse (customcontrols.mouse.Touchpad) the next time a game starts.
 */
public class CursorStudioFragment extends Fragment {
    public static final String TAG = "RKB_CURSOR_STUDIO";

    private static final String[][] PRESETS = {
            {CursorStudioPrefs.STYLE_CLASSIC, "Classic Arrow"},
            {CursorStudioPrefs.STYLE_NEON, "Neon Arrow"},
            {CursorStudioPrefs.STYLE_CROSSHAIR, "Crosshair"},
            {CursorStudioPrefs.STYLE_DOT, "Dot"},
            {CursorStudioPrefs.STYLE_RING, "Ring"},
    };
    private static final String[] COLORS = {
            "#00A8FF", "#FF3B3B", "#2ECC71", "#A855F7", "#FFD60A", "#FFFFFF", "#FF8A00"};

    private String mStyle;
    private String mColor;
    private int mSize;
    private int mOpacity;

    private PreviewView mPreview;
    private TextView mSizeLabel, mOpacityLabel;
    private SeekBar mSizeBar, mOpacityBar;
    private LinearLayout mPresetHolder, mColorHolder;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        final Context c = requireContext();
        mStyle = CursorStudioPrefs.getStyle();
        mColor = CursorStudioPrefs.getColorHex();
        mSize = CursorStudioPrefs.getSizePercent();
        mOpacity = CursorStudioPrefs.getOpacityPercent();

        boolean wide = !RkbUi.isCompact(c);
        LinearLayout body = new LinearLayout(c);
        body.setOrientation(wide ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);

        // ---- left: presets
        LinearLayout left = new LinearLayout(c);
        left.setOrientation(LinearLayout.VERTICAL);
        left.addView(RkbUi.text(c, "Presets", 13, RkbUi.MUTED, true));
        mPresetHolder = new LinearLayout(c);
        mPresetHolder.setOrientation(LinearLayout.VERTICAL);
        left.addView(mPresetHolder);
        buildPresets();

        // ---- right: preview + controls
        LinearLayout right = RkbUi.card(c);
        right.addView(RkbUi.text(c, "Preview", 13, RkbUi.MUTED, true));
        mPreview = new PreviewView(c);
        LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, RkbUi.dp(c, 110));
        plp.topMargin = RkbUi.dp(c, 6);
        right.addView(mPreview, plp);
        mPreview.setBackground(RkbUi.rounded(c, 0xFF0A1220, 12, RkbUi.BORDER, 1));

        mSizeLabel = RkbUi.text(c, "", 12, RkbUi.WHITE, true);
        right.addView(mSizeLabel);
        mSizeBar = seek(c, 50, 150, mSize, p -> { mSize = p; refreshLabels(); mPreview.invalidate(); });
        right.addView(mSizeBar);

        mOpacityLabel = RkbUi.text(c, "", 12, RkbUi.WHITE, true);
        right.addView(mOpacityLabel);
        mOpacityBar = seek(c, 20, 100, mOpacity, p -> { mOpacity = p; refreshLabels(); mPreview.invalidate(); });
        right.addView(mOpacityBar);

        right.addView(RkbUi.text(c, "Color", 12, RkbUi.WHITE, true));
        mColorHolder = new LinearLayout(c);
        mColorHolder.setPadding(0, RkbUi.dp(c, 6), 0, RkbUi.dp(c, 8));
        right.addView(mColorHolder);
        buildColors();

        LinearLayout buttons = new LinearLayout(c);
        TextView reset = RkbUi.button(c, "Reset", false, v -> doReset());
        TextView save = RkbUi.button(c, "Save", true, v -> doSave());
        LinearLayout.LayoutParams b1 = new LinearLayout.LayoutParams(0, RkbUi.dp(c, 42), 1f);
        b1.rightMargin = RkbUi.dp(c, 8);
        buttons.addView(reset, b1);
        buttons.addView(save, new LinearLayout.LayoutParams(0, RkbUi.dp(c, 42), 1f));
        right.addView(buttons);

        TextView note = RkbUi.text(c, "Applies to the on-screen virtual mouse cursor the next time a game starts. "
                + "Cursor drawn by Minecraft itself (menus with a grabbed mouse, crosshair) is not changed.",
                11, RkbUi.MUTED, false);
        note.setPadding(0, RkbUi.dp(c, 8), 0, 0);
        right.addView(note);

        refreshLabels();

        if (wide) {
            body.addView(left, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            rlp.leftMargin = RkbUi.dp(c, 12);
            body.addView(right, rlp);
        } else {
            body.addView(left);
            LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rlp.topMargin = RkbUi.dp(c, 12);
            body.addView(right, rlp);
        }

        ScrollView sv = new ScrollView(c);
        sv.setFillViewport(false);
        sv.setVerticalScrollBarEnabled(false);
        sv.addView(body);
        return RkbUi.scaffold(c, RkbNav.Dest.CURSOR, "Cursor Studio",
                "Customize your cursor for a better experience.", sv);
    }

    // ---------------------------------------------------------------- building

    private void buildPresets() {
        final Context c = requireContext();
        mPresetHolder.removeAllViews();
        int perRow = RkbUi.isCompact(c) ? 2 : 3;
        LinearLayout row = null;
        for (int i = 0; i < PRESETS.length; i++) {
            if (i % perRow == 0) {
                row = new LinearLayout(c);
                LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                rp.topMargin = RkbUi.dp(c, 8);
                mPresetHolder.addView(row, rp);
            }
            final String style = PRESETS[i][0];
            boolean sel = style.equals(mStyle);
            LinearLayout tile = new LinearLayout(c);
            tile.setOrientation(LinearLayout.VERTICAL);
            tile.setGravity(Gravity.CENTER);
            tile.setClickable(true);
            tile.setBackground(RkbUi.ripple(c, RkbUi.rounded(c, sel ? 0xFF0B2236 : RkbUi.CARD, 12,
                    sel ? RkbUi.BLUE : RkbUi.BORDER, sel ? 2 : 1)));
            tile.setOnClickListener(v -> { mStyle = style; buildPresets(); mPreview.invalidate(); });
            TileIcon icon = new TileIcon(c, style);
            tile.addView(icon, new LinearLayout.LayoutParams(RkbUi.dp(c, 44), RkbUi.dp(c, 44)));
            TextView name = RkbUi.text(c, PRESETS[i][1], 11, RkbUi.WHITE, sel);
            name.setGravity(Gravity.CENTER);
            name.setSingleLine(true);
            tile.addView(name);
            LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0, RkbUi.dp(c, 84), 1f);
            tlp.rightMargin = RkbUi.dp(c, 8);
            row.addView(tile, tlp);
        }
    }

    private void buildColors() {
        final Context c = requireContext();
        mColorHolder.removeAllViews();
        for (final String hex : COLORS) {
            boolean sel = hex.equalsIgnoreCase(mColor);
            View dot = new View(c);
            dot.setBackground(RkbUi.rounded(c, CursorStudioPrefs.parseColor(hex), 14,
                    sel ? RkbUi.WHITE : RkbUi.BORDER, sel ? 2 : 1));
            dot.setClickable(true);
            dot.setContentDescription("Color " + hex);
            dot.setOnClickListener(v -> { mColor = hex; buildColors(); buildPresetsKeepScroll(); mPreview.invalidate(); });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(RkbUi.dp(c, 28), RkbUi.dp(c, 28));
            lp.rightMargin = RkbUi.dp(c, 8);
            mColorHolder.addView(dot, lp);
        }
    }

    private void buildPresetsKeepScroll() { buildPresets(); }

    private interface IntConsumer { void accept(int v); }

    private SeekBar seek(Context c, final int min, int max, int value, final IntConsumer l) {
        SeekBar s = new SeekBar(c);
        s.setMax(max - min);
        s.setProgress(value - min);
        s.setPadding(RkbUi.dp(c, 4), RkbUi.dp(c, 6), RkbUi.dp(c, 4), RkbUi.dp(c, 6));
        s.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar sb, int p, boolean fromUser) { l.accept(p + min); }
            @Override public void onStartTrackingTouch(SeekBar sb) {}
            @Override public void onStopTrackingTouch(SeekBar sb) {}
        });
        return s;
    }

    private void refreshLabels() {
        mSizeLabel.setText("Size: " + mSize + "%");
        mOpacityLabel.setText("Opacity: " + mOpacity + "%");
    }

    // ---------------------------------------------------------------- actions

    private void doSave() {
        CursorStudioPrefs.save(mStyle, mColor, mSize, mOpacity);
        Toast.makeText(requireContext(), "Cursor saved. It applies when the next game starts.", Toast.LENGTH_SHORT).show();
    }

    private void doReset() {
        CursorStudioPrefs.resetToDefaults();
        mStyle = CursorStudioPrefs.getStyle();
        mColor = CursorStudioPrefs.getColorHex();
        mSize = CursorStudioPrefs.getSizePercent();
        mOpacity = CursorStudioPrefs.getOpacityPercent();
        mSizeBar.setProgress(mSize - 50);
        mOpacityBar.setProgress(mOpacity - 20);
        buildPresets();
        buildColors();
        refreshLabels();
        mPreview.invalidate();
        Toast.makeText(requireContext(), "Cursor reset to default", Toast.LENGTH_SHORT).show();
    }

    // ---------------------------------------------------------------- views

    /** Draws the staged cursor using the same factory as the in-game touchpad. */
    private final class PreviewView extends View {
        PreviewView(Context c) { super(c); }

        @Override
        protected void onDraw(Canvas canvas) {
            Drawable d = CursorStudioPrefs.createDrawable(getContext(), mStyle,
                    CursorStudioPrefs.parseColor(mColor), mOpacity);
            float density = getResources().getDisplayMetrics().density;
            CursorStudioPrefs.applyBounds(d, mStyle, density * 1.6f * mSize / 100f);
            canvas.save();
            if (CursorStudioPrefs.isCentered(mStyle)) {
                canvas.translate(getWidth() / 2f, getHeight() / 2f);
            } else {
                Drawable b = d;
                canvas.translate((getWidth() - b.getBounds().width()) / 2f, (getHeight() - b.getBounds().height()) / 2f);
            }
            d.draw(canvas);
            canvas.restore();
        }
    }

    /** Small static icon for a preset tile (always shown at 100% opacity, current color for tinted styles). */
    private final class TileIcon extends View {
        private final String style;
        TileIcon(Context c, String style) { super(c); this.style = style; }

        @Override
        protected void onDraw(Canvas canvas) {
            Drawable d = CursorStudioPrefs.createDrawable(getContext(), style,
                    CursorStudioPrefs.parseColor(mColor), 100);
            float scale = getHeight() / (54f * 1.25f);
            CursorStudioPrefs.applyBounds(d, style, scale * (CursorStudioPrefs.isCentered(style) ? 1.35f : 1f));
            canvas.save();
            if (CursorStudioPrefs.isCentered(style)) canvas.translate(getWidth() / 2f, getHeight() / 2f);
            else canvas.translate((getWidth() - d.getBounds().width()) / 2f, (getHeight() - d.getBounds().height()) / 2f);
            d.draw(canvas);
            canvas.restore();
        }
    }
}
