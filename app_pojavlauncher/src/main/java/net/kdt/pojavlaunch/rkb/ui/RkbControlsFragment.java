package net.kdt.pojavlaunch.rkb.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import net.kdt.pojavlaunch.CustomControlsActivity;
import net.kdt.pojavlaunch.ImportControlActivity;
import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Controls screen backed by the real Pojav control-layout folder and editor activity. */
public class RkbControlsFragment extends Fragment {
    private final ActivityResultLauncher<String[]> mImportLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), this::startImport);

    private LinearLayout mList;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        final android.content.Context c = requireContext();

        LinearLayout body = new LinearLayout(c);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(RkbUi.dp(c, 14), RkbUi.dp(c, 8), RkbUi.dp(c, 14), RkbUi.dp(c, 18));

        LinearLayout actions = new LinearLayout(c);
        actions.addView(RkbUi.button(c, "REFRESH", false, v -> reload()), actionLp(c, 1f));
        actions.addView(RkbUi.button(c, "IMPORT", false, v ->
                mImportLauncher.launch(new String[]{"application/json", "*/*"})), actionLp(c, 1f));
        actions.addView(RkbUi.button(c, "CREATE", true, v -> {
            Intent i = new Intent(v.getContext(), CustomControlsActivity.class);
            v.getContext().startActivity(i);
        }), actionLp(c, 1f));
        LinearLayout.LayoutParams aLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        aLp.bottomMargin = RkbUi.dp(c, 12);
        body.addView(actions, aLp);

        mList = new LinearLayout(c);
        mList.setOrientation(LinearLayout.VERTICAL);
        body.addView(mList, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        body.addView(RkbUi.spacer(c, 12));
        body.addView(RkbUi.button(c, "CURSOR STUDIO", false, v -> {
            Activity a = RkbUi.activityFrom(v.getContext());
            if (a != null) RkbNav.go(a, RkbNav.Dest.CURSOR);
        }), new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, RkbUi.dp(c, 46)));

        ScrollView scroll = new ScrollView(c);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.addView(body, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        reload();
        return RkbUi.scaffold(c, RkbNav.Dest.CONTROLS, "Controls",
                "Manage real Pojav control layouts.", scroll);
    }

    /** Hands the SAF-selected layout to the existing Pojav import/validate activity. */
    private void startImport(Uri uri) {
        Activity a = getActivity();
        if (uri == null || a == null || a.isFinishing() || a.isDestroyed()) return;
        Intent intent = new Intent(a, ImportControlActivity.class);
        intent.setData(uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        a.startActivity(intent);
    }

    private static LinearLayout.LayoutParams actionLp(android.content.Context c, float weight) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                RkbUi.dp(c, 42), weight);
        lp.leftMargin = RkbUi.dp(c, 4);
        lp.rightMargin = RkbUi.dp(c, 4);
        return lp;
    }

    private static LinearLayout.LayoutParams lp(android.content.Context c) {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private void reload() {
        if (mList == null || !isAdded()) return;
        final android.content.Context c = requireContext();
        mList.removeAllViews();

        File dir = new File(Tools.CTRLMAP_PATH);
        File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
        List<File> layouts = new ArrayList<>();
        if (files != null) layouts.addAll(Arrays.asList(files));
        layouts.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));

        if (layouts.isEmpty()) {
            mList.addView(RkbUi.text(c, "No layouts found. Import or create one above.",
                    13, RkbUi.WHITE, false), lp(c));
            return;
        }
        for (File file : layouts) mList.addView(layoutCard(c, file), lp(c));
    }

    private View layoutCard(android.content.Context c, final File file) {
        boolean active = file.getAbsolutePath().equals(LauncherPreferences.PREF_DEFAULTCTRL_PATH);
        LinearLayout card = RkbUi.card(c);
        card.setBackground(RkbUi.ripple(c, RkbUi.rounded(c,
                active ? RkbUi.CARD_2 : RkbUi.CARD, 18,
                active ? RkbUi.BLUE : RkbUi.BORDER, active ? 2 : 1)));

        LinearLayout top = new LinearLayout(c);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.addView(RkbUi.text(c, stripJson(file.getName()), 15, RkbUi.WHITE, true),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        top.addView(RkbUi.text(c, active ? "ACTIVE" : "LAYOUT", 10,
                active ? RkbUi.GREEN : RkbUi.MUTED, true));
        card.addView(top, lp(c));

        LinearLayout row = new LinearLayout(c);
        row.setPadding(0, RkbUi.dp(c, 10), 0, 0);
        TextView select = RkbUi.button(c, "SELECT", active, v -> {
            LauncherPreferences.PREF_DEFAULTCTRL_PATH = file.getAbsolutePath();
            LauncherPreferences.DEFAULT_PREF.edit()
                    .putString("defaultCtrl", file.getAbsolutePath()).apply();
            reload();
        });
        TextView edit = RkbUi.button(c, "EDIT", false, v -> {
            LauncherPreferences.PREF_DEFAULTCTRL_PATH = file.getAbsolutePath();
            LauncherPreferences.DEFAULT_PREF.edit()
                    .putString("defaultCtrl", file.getAbsolutePath()).apply();
            v.getContext().startActivity(new Intent(v.getContext(), CustomControlsActivity.class));
        });
        TextView share = RkbUi.button(c, "SHARE", false, v -> share(c, file));
        TextView delete = RkbUi.button(c, "DELETE", false, v -> confirmDelete(c, file));
        row.addView(select, actionLp(c, 1f));
        row.addView(edit, actionLp(c, 1f));
        row.addView(share, actionLp(c, 1f));
        row.addView(delete, actionLp(c, 1f));
        card.addView(row, lp(c));
        return card;
    }

    private void share(android.content.Context c, File file) {
        try {
            Uri uri = DocumentsContract.buildDocumentUri(
                    getString(R.string.storageProviderAuthorities), file.getAbsolutePath());
            Intent send = new Intent(Intent.ACTION_SEND);
            send.setType("application/json");
            send.putExtra(Intent.EXTRA_STREAM, uri);
            send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(send, file.getName()));
        } catch (Exception e) {
            Toast.makeText(c, "Could not share layout: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void confirmDelete(android.content.Context c, final File file) {
        if ("default.json".equals(file.getName())) {
            Toast.makeText(c, "The default layout cannot be deleted.", Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(c)
                .setTitle("Delete layout?")
                .setMessage(file.getName())
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton("Delete", (d, w) -> {
                    if (file.delete()) {
                        if (file.getAbsolutePath().equals(LauncherPreferences.PREF_DEFAULTCTRL_PATH)) {
                            LauncherPreferences.PREF_DEFAULTCTRL_PATH = Tools.CTRLDEF_FILE;
                            LauncherPreferences.DEFAULT_PREF.edit()
                                    .putString("defaultCtrl", Tools.CTRLDEF_FILE).apply();
                        }
                        reload();
                    } else {
                        Toast.makeText(c, "Could not delete layout", Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }

    private static String stripJson(String name) {
        return name.endsWith(".json") ? name.substring(0, name.length() - 5) : name;
    }
}
