package net.kdt.pojavlaunch.rkb.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import net.kdt.pojavlaunch.PojavProfile;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.fragments.SelectAuthFragment;
import net.kdt.pojavlaunch.value.MinecraftAccount;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Skin & Account. Uses only the existing account store (Tools.DIR_ACCOUNT_NEW json files +
 * PojavProfile). MinecraftAccount has a skinUrl field and no cape field, so cape is shown as
 * unsupported instead of pretending to work.
 */
public class RkbSkinFragment extends Fragment {
    private final Handler mMain = new Handler(Looper.getMainLooper());
    private ExecutorService mExecutor;
    private EditText mSkinInput;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        mExecutor = Executors.newSingleThreadExecutor();
        final Context c = requireContext();
        final MinecraftAccount acc = RkbUi.currentAccount(c);

        LinearLayout body = new LinearLayout(c);
        body.setOrientation(LinearLayout.VERTICAL);

        // ---- current account
        LinearLayout accCard = RkbUi.card(c);
        accCard.addView(RkbUi.text(c, "Current account", 12, RkbUi.MUTED, true));
        LinearLayout accRow = new LinearLayout(c);
        accRow.setGravity(Gravity.CENTER_VERTICAL);
        accRow.setPadding(0, RkbUi.dp(c, 8), 0, RkbUi.dp(c, 8));
        View avatar = buildAvatar(c, acc);
        accRow.addView(avatar, new LinearLayout.LayoutParams(RkbUi.dp(c, 52), RkbUi.dp(c, 52)));
        LinearLayout names = new LinearLayout(c);
        names.setOrientation(LinearLayout.VERTICAL);
        names.setPadding(RkbUi.dp(c, 12), 0, 0, 0);
        names.addView(RkbUi.text(c, RkbUi.accountName(c), 17, RkbUi.WHITE, true));
        names.addView(RkbUi.text(c, RkbUi.accountBadge(c), 11, RkbUi.BLUE, true));
        accRow.addView(names, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        accCard.addView(accRow);

        LinearLayout accButtons = new LinearLayout(c);
        TextView sw = RkbUi.button(c, "Switch account", false, v -> showSwitchDialog());
        TextView add = RkbUi.button(c, "Add account", true, v -> {
            if (isAdded()) Tools.swapFragment(requireActivity(), SelectAuthFragment.class, SelectAuthFragment.TAG, null);
        });
        LinearLayout.LayoutParams l1 = new LinearLayout.LayoutParams(0, RkbUi.dp(c, 42), 1f);
        l1.rightMargin = RkbUi.dp(c, 8);
        accButtons.addView(sw, l1);
        accButtons.addView(add, new LinearLayout.LayoutParams(0, RkbUi.dp(c, 42), 1f));
        accCard.addView(accButtons);
        body.addView(accCard);

        // ---- skin url
        LinearLayout skinCard = RkbUi.card(c);
        skinCard.addView(RkbUi.text(c, "Skin URL", 12, RkbUi.MUTED, true));
        mSkinInput = new EditText(c);
        mSkinInput.setHint("https://example.com/skin.png");
        mSkinInput.setHintTextColor(RkbUi.MUTED);
        mSkinInput.setTextColor(RkbUi.WHITE);
        mSkinInput.setTextSize(13);
        mSkinInput.setSingleLine(true);
        mSkinInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        mSkinInput.setPadding(RkbUi.dp(c, 12), 0, RkbUi.dp(c, 12), 0);
        mSkinInput.setBackground(RkbUi.rounded(c, RkbUi.CARD_2, 10, RkbUi.BORDER, 1));
        if (acc != null && acc.skinUrl != null) mSkinInput.setText(acc.skinUrl);
        LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, RkbUi.dp(c, 42));
        ilp.topMargin = RkbUi.dp(c, 8);
        skinCard.addView(mSkinInput, ilp);

        LinearLayout skinButtons = new LinearLayout(c);
        TextView clear = RkbUi.button(c, "Clear", false, v -> saveSkinUrl(acc, ""));
        TextView apply = RkbUi.button(c, "Save", true, v -> saveSkinUrl(acc, mSkinInput.getText().toString()));
        LinearLayout.LayoutParams s1 = new LinearLayout.LayoutParams(0, RkbUi.dp(c, 42), 1f);
        s1.rightMargin = RkbUi.dp(c, 8);
        s1.topMargin = RkbUi.dp(c, 8);
        LinearLayout.LayoutParams s2 = new LinearLayout.LayoutParams(0, RkbUi.dp(c, 42), 1f);
        s2.topMargin = RkbUi.dp(c, 8);
        skinButtons.addView(clear, s1);
        skinButtons.addView(apply, s2);
        skinCard.addView(skinButtons);
        TextView skinNote = RkbUi.text(c, "The URL is stored on this account. Not every server accepts custom skin URLs.",
                11, RkbUi.MUTED, false);
        skinNote.setPadding(0, RkbUi.dp(c, 6), 0, 0);
        skinCard.addView(skinNote);
        LinearLayout.LayoutParams sclp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sclp.topMargin = RkbUi.dp(c, 10);
        body.addView(skinCard, sclp);

        // ---- cape (not supported by the account model)
        LinearLayout capeCard = RkbUi.card(c);
        capeCard.setAlpha(0.75f);
        capeCard.addView(RkbUi.text(c, "Cape", 12, RkbUi.MUTED, true));
        TextView capeMsg = RkbUi.text(c, "Cape URLs are not supported by this launcher's account system yet, "
                + "so this option is disabled instead of pretending to work.", 12, RkbUi.WHITE, false);
        capeMsg.setPadding(0, RkbUi.dp(c, 6), 0, 0);
        capeCard.addView(capeMsg);
        LinearLayout.LayoutParams cclp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cclp.topMargin = RkbUi.dp(c, 10);
        body.addView(capeCard, cclp);

        ScrollView sv = new ScrollView(c);
        sv.setVerticalScrollBarEnabled(false);
        sv.addView(body);
        return RkbUi.scaffold(c, RkbNav.Dest.SKIN, "Skin & Account",
                "Manage your skin and account settings.", sv);
    }

    private View buildAvatar(Context c, @Nullable MinecraftAccount acc) {
        Bitmap face = null;
        try { if (acc != null) face = acc.getSkinFace(); } catch (Exception ignored) { }
        if (face != null) {
            ImageView iv = new ImageView(c);
            iv.setImageBitmap(face);
            iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
            iv.setBackground(RkbUi.rounded(c, RkbUi.CARD_2, 12, RkbUi.BORDER, 1));
            return iv;
        }
        String name = RkbUi.accountName(c);
        TextView t = RkbUi.text(c, name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase(Locale.ROOT), 22, RkbUi.BLUE, true);
        t.setGravity(Gravity.CENTER);
        t.setBackground(RkbUi.rounded(c, RkbUi.CARD_2, 12, RkbUi.BORDER, 1));
        return t;
    }

    // ---------------------------------------------------------------- switching

    private void showSwitchDialog() {
        final Context c = requireContext();
        File dir = new File(Tools.DIR_ACCOUNT_NEW);
        File[] files = dir.listFiles((d, n) -> n.endsWith(".json"));
        final List<String> names = new ArrayList<>();
        if (files != null) {
            for (File f : files) names.add(f.getName().substring(0, f.getName().length() - 5));
        }
        if (names.isEmpty()) {
            Toast.makeText(c, "No saved accounts. Use Add account.", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] arr = names.toArray(new String[0]);
        Arrays.sort(arr, String.CASE_INSENSITIVE_ORDER);
        new AlertDialog.Builder(c)
                .setTitle("Switch account")
                .setItems(arr, (d, which) -> {
                    PojavProfile.setCurrentProfile(c, arr[which]);
                    if (isAdded()) {
                        // Rebuild every view that shows the account (header, this screen) by reopening it.
                        requireActivity().getSupportFragmentManager().beginTransaction()
                                .setReorderingAllowed(true)
                                .detach(this).commit();
                        requireActivity().getSupportFragmentManager().beginTransaction()
                                .setReorderingAllowed(true)
                                .attach(this).commit();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    // ---------------------------------------------------------------- skin URL

    /** @return null if valid (or empty = clear), otherwise a user-facing error. */
    static String validateSkinUrl(String raw) {
        if (raw.isEmpty()) return null;
        if (raw.length() > 2048) return "URL is too long";
        if (raw.contains(" ")) return "URL must not contain spaces";
        try {
            URI u = new URI(raw);
            String scheme = u.getScheme();
            if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
                return "URL must start with http:// or https://";
            }
            if (u.getHost() == null || u.getHost().isEmpty()) return "URL has no host name";
        } catch (Exception e) {
            return "That is not a valid URL";
        }
        return null;
    }

    private void saveSkinUrl(@Nullable final MinecraftAccount acc, String input) {
        final Context c = requireContext();
        if (acc == null) {
            Toast.makeText(c, "Select or add an account first", Toast.LENGTH_SHORT).show();
            return;
        }
        final String url = input.trim();
        String error = validateSkinUrl(url);
        if (error != null) {
            mSkinInput.setError(error);
            return;
        }
        if (mExecutor == null) return;
        if (url.isEmpty()) mSkinInput.setText("");
        try {
            mExecutor.execute(() -> {
                String result;
                try {
                    acc.skinUrl = url.isEmpty() ? null : url;
                    acc.save();
                    result = url.isEmpty() ? "Skin URL cleared" : "Skin URL saved to this account";
                } catch (Exception e) {
                    result = "Could not save: " + e.getMessage();
                }
                final String msg = result;
                mMain.post(() -> { if (isAdded()) Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show(); });
            });
        } catch (java.util.concurrent.RejectedExecutionException ignored) { }
    }

    @Override
    public void onDestroyView() {
        mMain.removeCallbacksAndMessages(null);
        if (mExecutor != null) mExecutor.shutdown(); // let an in-flight account save finish
        mExecutor = null;
        mSkinInput = null;
        super.onDestroyView();
    }
}
