package net.kdt.pojavlaunch.rkb.ui;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import net.kdt.pojavlaunch.rkb.cursor.CursorStudioPrefs;

/**
 * RKB Cursor Studio — style, color, size, opacity for the virtual mouse cursor.
 * Wire into main navigation (same place as HTML side-nav 🖱️).
 */
public class CursorStudioFragment extends Fragment {
    public static final String TAG = "RKB_CURSOR_STUDIO";

    private View previewRing;
    private TextView sizeLabel;
    private TextView opacityLabel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        ScrollView scroll = new ScrollView(requireContext());
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        scroll.setBackgroundColor(0xFF05080F);

        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(18), dp(20), dp(24));
        root.setBackgroundColor(0xFF05080F);
        scroll.addView(root, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView title = new TextView(requireContext());
        title.setText("RKB Cursor Studio");
        title.setTextColor(0xFFEEF6FF);
        title.setTextSize(20);
        title.setPadding(0, 0, 0, 8);
        root.addView(title);

        TextView sub = new TextView(requireContext());
        sub.setText("Style · color · size · opacity");
        sub.setTextColor(0xFF7A8FA8);
        sub.setTextSize(12);
        sub.setPadding(0, 0, 0, 20);
        root.addView(sub);

        // Preview
        LinearLayout previewBox = new LinearLayout(requireContext());
        previewBox.setGravity(Gravity.CENTER);
        previewBox.setPadding(0, 24, 0, 24);
        GradientDrawable previewBg = new GradientDrawable();
        previewBg.setColor(0xFF0A1520);
        previewBg.setCornerRadius(24);
        previewBg.setStroke(2, 0x3300B4FF);
        previewBox.setBackground(previewBg);
        previewBox.setMinimumHeight(180);

        previewRing = new View(requireContext());
        LinearLayout.LayoutParams ringLp = new LinearLayout.LayoutParams(dp(72), dp(72));
        previewRing.setLayoutParams(ringLp);
        updatePreview();
        previewBox.addView(previewRing);
        root.addView(previewBox, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // Styles
        root.addView(sectionLabel("QUICK STYLE"));
        LinearLayout styles = row();
        styles.addView(styleBtn("Classic", CursorStudioPrefs.STYLE_CLASSIC));
        styles.addView(styleBtn("Pulse", CursorStudioPrefs.STYLE_PULSE));
        styles.addView(styleBtn("Gamepad", CursorStudioPrefs.STYLE_GAMEPAD));
        styles.addView(styleBtn("Custom", CursorStudioPrefs.STYLE_CUSTOM));
        root.addView(styles);

        // Colors
        root.addView(sectionLabel("COLOR"));
        LinearLayout colors = row();
        int[] palette = {0xFF00B4FF, 0xFF3DD6FF, 0xFFFFFFFF, 0xFF22D3EE, 0xFF4ADE80, 0xFFF472B6};
        for (int c : palette) {
            colors.addView(colorDot(c));
        }
        root.addView(colors);

        // Size
        root.addView(sectionLabel("SIZE"));
        sizeLabel = new TextView(requireContext());
        sizeLabel.setTextColor(0xFF7A8FA8);
        sizeLabel.setTextSize(11);
        root.addView(sizeLabel);
        SeekBar sizeBar = new SeekBar(requireContext());
        sizeBar.setMax(100); // 50..150 → map 0..100
        sizeBar.setProgress(CursorStudioPrefs.getSizePercent() - 50);
        sizeBar.setOnSeekBarChangeListener(simpleSeek(progress -> {
            CursorStudioPrefs.setSizePercent(progress + 50);
            refreshLabels();
            updatePreview();
        }));
        root.addView(sizeBar);

        // Opacity
        root.addView(sectionLabel("OPACITY"));
        opacityLabel = new TextView(requireContext());
        opacityLabel.setTextColor(0xFF7A8FA8);
        opacityLabel.setTextSize(11);
        root.addView(opacityLabel);
        SeekBar opBar = new SeekBar(requireContext());
        opBar.setMax(80); // 20..100
        opBar.setProgress(CursorStudioPrefs.getOpacityPercent() - 20);
        opBar.setOnSeekBarChangeListener(simpleSeek(progress -> {
            CursorStudioPrefs.setOpacityPercent(progress + 20);
            refreshLabels();
            updatePreview();
        }));
        root.addView(opBar);

        // Save
        Button save = new Button(requireContext());
        save.setText("SAVE & APPLY");
        save.setAllCaps(false);
        save.setTextSize(14);
        save.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        save.setTextColor(0xFF021018);
        GradientDrawable saveBg = new GradientDrawable();
        saveBg.setColor(0xFF00A8FF);
        saveBg.setCornerRadius(dp(12));
        save.setBackground(saveBg);
        save.setOnClickListener(v -> {
            Toast.makeText(requireContext(),
                    "Cursor saved: " + CursorStudioPrefs.getStyle()
                            + " · " + CursorStudioPrefs.getSizePercent() + "%"
                            + " · " + CursorStudioPrefs.getOpacityPercent() + "%",
                    Toast.LENGTH_SHORT).show();
        });
        LinearLayout.LayoutParams saveLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        saveLp.topMargin = 28;
        root.addView(save, saveLp);

        refreshLabels();
        return scroll;
    }

    private void refreshLabels() {
        if (sizeLabel != null) {
            sizeLabel.setText(CursorStudioPrefs.getSizePercent() + "%");
        }
        if (opacityLabel != null) {
            opacityLabel.setText(CursorStudioPrefs.getOpacityPercent() + "%");
        }
    }

    private void updatePreview() {
        if (previewRing == null) return;
        int color = CursorStudioPrefs.getColorArgb();
        int alpha = Math.round(255 * CursorStudioPrefs.getOpacity());
        int withAlpha = (color & 0x00FFFFFF) | (alpha << 24);
        float scale = CursorStudioPrefs.getSizeMultiplier();
        int size = Math.round(dp(72) * scale);
        ViewGroup.LayoutParams lp = previewRing.getLayoutParams();
        if (lp != null) {
            lp.width = size;
            lp.height = size;
            previewRing.setLayoutParams(lp);
        }
        GradientDrawable ring = new GradientDrawable();
        ring.setShape(GradientDrawable.OVAL);
        ring.setStroke(dp(3), withAlpha);
        ring.setColor(Color.TRANSPARENT);
        previewRing.setBackground(ring);
    }

    private TextView sectionLabel(String t) {
        TextView tv = new TextView(requireContext());
        tv.setText(t);
        tv.setTextColor(0xFF7A8FA8);
        tv.setTextSize(11);
        tv.setPadding(0, dp(18), 0, dp(8));
        return tv;
    }

    private LinearLayout row() {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 0, 0, 4);
        return row;
    }

    private Button styleBtn(String label, String style) {
        Button b = new Button(requireContext());
        b.setText(label);
        b.setTextSize(11);
        b.setPadding(dp(4), 0, dp(4), 0);
        b.setAllCaps(false);
        boolean on = style.equals(CursorStudioPrefs.getStyle());
        b.setTextColor(on ? 0xFF021018 : 0xFFEEF6FF);
        b.setBackgroundColor(on ? 0xFF00B4FF : 0xFF141C2A);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp.setMargins(4, 0, 4, 0);
        b.setLayoutParams(lp);
        b.setOnClickListener(v -> {
            CursorStudioPrefs.setStyle(style);
            // rebuild parent styles row highlight is light — toast is enough
            Toast.makeText(requireContext(), "Style: " + label, Toast.LENGTH_SHORT).show();
            updatePreview();
        });
        return b;
    }

    private View colorDot(int color) {
        View v = new View(requireContext());
        int s = dp(28);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(s, s);
        lp.setMargins(6, 4, 6, 4);
        v.setLayoutParams(lp);
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.OVAL);
        d.setColor(color);
        if (color == CursorStudioPrefs.getColorArgb()) {
            d.setStroke(dp(2), 0xFFFFFFFF);
        }
        v.setBackground(d);
        v.setOnClickListener(x -> {
            String hex = String.format("#%06X", color & 0xFFFFFF);
            CursorStudioPrefs.setColorHex(hex);
            updatePreview();
            Toast.makeText(requireContext(), "Color " + hex, Toast.LENGTH_SHORT).show();
        });
        return v;
    }

    private interface ProgressCb { void on(int progress); }

    private SeekBar.OnSeekBarChangeListener simpleSeek(ProgressCb cb) {
        return new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) cb.on(progress);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        };
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
