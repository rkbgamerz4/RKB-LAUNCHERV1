package net.kdt.pojavlaunch.rkb.ui;

import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.authenticator.AuthType;
import net.kdt.pojavlaunch.authenticator.accounts.Account;
import net.kdt.pojavlaunch.authenticator.accounts.Accounts;
import net.kdt.pojavlaunch.fragments.ElyByLoginFragment;
import net.kdt.pojavlaunch.fragments.LocalLoginFragment;
import net.kdt.pojavlaunch.fragments.MicrosoftLoginFragment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Account screen: real Microsoft / Ely.by / Offline login paths from the existing account system. */
public class RkbSkinFragment extends Fragment {

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        final android.content.Context c = requireContext();

        LinearLayout body = new LinearLayout(c);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(RkbUi.dp(c, 14), RkbUi.dp(c, 8), RkbUi.dp(c, 14), RkbUi.dp(c, 18));

        body.addView(loginCard(c), lp(c));
        body.addView(RkbUi.spacer(c, 12));
        body.addView(currentCard(c), lp(c));
        body.addView(RkbUi.spacer(c, 12));
        body.addView(tipCard(c), lp(c));

        ScrollView scroll = new ScrollView(c);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.addView(body, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return RkbUi.scaffold(c, RkbNav.Dest.SKIN, "Account",
                "Sign in and manage your player.", scroll);
    }

    private static LinearLayout.LayoutParams lp(android.content.Context c) {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private View loginCard(android.content.Context c) {
        LinearLayout card = RkbUi.card(c);
        card.addView(RkbUi.text(c, "SIGN IN METHOD", 11, RkbUi.BLUE, true));
        card.addView(RkbUi.spacer(c, 10));

        addMethod(c, card, "Microsoft", "Owns and downloads Minecraft: Java Edition",
                AuthType.MICROSOFT.iconResource, () ->
                        open(MicrosoftLoginFragment.class, MicrosoftLoginFragment.TAG));
        addMethod(c, card, "Ely.by", "Alternative authentication service",
                AuthType.ELY_BY.iconResource, () ->
                        open(ElyByLoginFragment.class, ElyByLoginFragment.TAG));
        addMethod(c, card, "Offline", "Play locally without an online account",
                R.drawable.ic_pojav_full, () ->
                        open(LocalLoginFragment.class, LocalLoginFragment.TAG));
        return card;
    }

    private void addMethod(android.content.Context c, LinearLayout parent, String title,
                           String subtitle, int iconRes, Runnable action) {
        LinearLayout row = new LinearLayout(c);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(RkbUi.dp(c, 12), RkbUi.dp(c, 12), RkbUi.dp(c, 12), RkbUi.dp(c, 12));
        row.setBackground(RkbUi.ripple(c, RkbUi.rounded(c, RkbUi.CARD_2, 14, RkbUi.BORDER, 1)));
        row.setClickable(true);
        row.setOnClickListener(v -> action.run());
        RkbMotion.press(row);

        ImageView icon = new ImageView(c);
        icon.setImageResource(iconRes);
        icon.setPadding(RkbUi.dp(c, 8), RkbUi.dp(c, 8), RkbUi.dp(c, 8), RkbUi.dp(c, 8));
        icon.setBackground(RkbUi.rounded(c, 0x44FFFFFF, 12, 0, 0));
        row.addView(icon, new LinearLayout.LayoutParams(RkbUi.dp(c, 44), RkbUi.dp(c, 44)));

        LinearLayout names = new LinearLayout(c);
        names.setOrientation(LinearLayout.VERTICAL);
        names.setPadding(RkbUi.dp(c, 12), 0, 0, 0);
        names.addView(RkbUi.text(c, title, 15, RkbUi.WHITE, true));
        names.addView(RkbUi.text(c, subtitle, 11, RkbUi.MUTED, false));
        row.addView(names, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        parent.addView(row, lp(c));
    }

    private View currentCard(android.content.Context c) {
        LinearLayout card = RkbUi.card(c);
        final Account acc = RkbUi.currentAccount(c);
        card.addView(RkbUi.text(c, "CURRENT ACCOUNT", 11, RkbUi.BLUE, true));

        LinearLayout row = new LinearLayout(c);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, RkbUi.dp(c, 10), 0, RkbUi.dp(c, 10));

        ImageView face = new ImageView(c);
        Bitmap skinFace = null;
        try { if (acc != null) skinFace = acc.getSkinFace(); } catch (Exception ignored) { }
        if (skinFace != null) {
            face.setImageBitmap(skinFace);
            face.setScaleType(ImageView.ScaleType.FIT_CENTER);
        } else {
            String initial = RkbUi.accountName(c).substring(0, 1).toUpperCase(Locale.ROOT);
            TextView letter = RkbUi.text(c, initial, 24, RkbUi.BLUE, true);
            letter.setGravity(Gravity.CENTER);
            face.setImageDrawable(null);
            row.addView(letter, new LinearLayout.LayoutParams(
                    RkbUi.dp(c, 52), RkbUi.dp(c, 52)));
        }
        if (skinFace != null) {
            row.addView(face, new LinearLayout.LayoutParams(RkbUi.dp(c, 52), RkbUi.dp(c, 52)));
        }

        LinearLayout names = new LinearLayout(c);
        names.setOrientation(LinearLayout.VERTICAL);
        names.setPadding(RkbUi.dp(c, 12), 0, 0, 0);
        names.addView(RkbUi.text(c, RkbUi.accountName(c), 17, RkbUi.WHITE, true));
        names.addView(RkbUi.text(c, RkbUi.accountBadge(c), 11, RkbUi.GREEN, true));
        row.addView(names, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        card.addView(row, lp(c));

        LinearLayout buttons = new LinearLayout(c);
        TextView switchBtn = RkbUi.button(c, "SWITCH", false, v -> showSwitchDialog());
        TextView refresh = RkbUi.button(c, "REFRESH SKIN", false, v -> refreshSkin(acc));
        TextView remove = RkbUi.button(c, "REMOVE", false, v -> removeAccount(acc));
        LinearLayout.LayoutParams b1 = new LinearLayout.LayoutParams(0, RkbUi.dp(c, 42), 1f);
        LinearLayout.LayoutParams b2 = new LinearLayout.LayoutParams(0, RkbUi.dp(c, 42), 1.3f);
        LinearLayout.LayoutParams b3 = new LinearLayout.LayoutParams(0, RkbUi.dp(c, 42), 1f);
        b2.leftMargin = RkbUi.dp(c, 6);
        b3.leftMargin = RkbUi.dp(c, 6);
        buttons.addView(switchBtn, b1);
        buttons.addView(refresh, b2);
        buttons.addView(remove, b3);
        card.addView(buttons, lp(c));
        return card;
    }

    private View tipCard(android.content.Context c) {
        LinearLayout card = RkbUi.card(c);
        card.setBackground(RkbUi.rounded(c, 0xCC1B4B7A, 18, RkbUi.BORDER, 1));
        card.addView(RkbUi.text(c, "TIP", 11, RkbUi.WHITE, true));
        TextView text = RkbUi.text(c,
                "A Microsoft account is required to download Minecraft files. Offline accounts can play versions already installed on this device.",
                12, RkbUi.WHITE, false);
        text.setPadding(0, RkbUi.dp(c, 6), 0, 0);
        card.addView(text);
        return card;
    }

    private void open(Class<? extends Fragment> fragmentClass, String tag) {
        if (isAdded()) Tools.swapFragment(requireActivity(), fragmentClass, tag, null);
    }

    private void showSwitchDialog() {
        final android.content.Context c = requireContext();
        final List<Account> accounts = new ArrayList<>();
        try { accounts.addAll(Accounts.load().accounts); } catch (IOException ignored) { }
        if (accounts.isEmpty()) {
            Toast.makeText(c, "No saved accounts. Add one first.", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] labels = new String[accounts.size()];
        for (int i = 0; i < labels.length; i++) {
            labels[i] = accounts.get(i).username + " (" + RkbUi.accountBadge(accounts.get(i)) + ")";
        }
        new AlertDialog.Builder(c)
                .setTitle("Switch account")
                .setItems(labels, (d, which) -> {
                    Accounts.setCurrent(accounts.get(which));
                    Toast.makeText(c, "Switched to " + accounts.get(which).username,
                            Toast.LENGTH_SHORT).show();
                    if (isAdded()) recreateScreen();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void refreshSkin(final Account acc) {
        if (acc == null) {
            Toast.makeText(requireContext(), "No account selected", Toast.LENGTH_SHORT).show();
            return;
        }
        new Thread(() -> {
            acc.updateSkinFace();
            if (isAdded()) mMain.post(this::recreateScreen);
        }, "rkb-skin-refresh").start();
    }

    private final android.os.Handler mMain = new android.os.Handler(android.os.Looper.getMainLooper());

    private void removeAccount(final Account acc) {
        if (acc == null) {
            Toast.makeText(requireContext(), "No account selected", Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(requireContext())
                .setTitle("Remove account?")
                .setMessage(acc.username)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton("Remove", (d, w) -> {
                    Accounts.delete(acc);
                    Toast.makeText(requireContext(), "Account removed", Toast.LENGTH_SHORT).show();
                    if (isAdded()) recreateScreen();
                })
                .show();
    }

    private void recreateScreen() {
        if (!isAdded() || getActivity() == null) return;
        getActivity().getSupportFragmentManager().beginTransaction()
                .setReorderingAllowed(true)
                .detach(this)
                .attach(this)
                .commitAllowingStateLoss();
    }
}
