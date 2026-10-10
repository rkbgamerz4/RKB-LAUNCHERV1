package net.kdt.pojavlaunch.rkb.ui;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.TextView;
import android.widget.Toast;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.fragments.ProfileEditorFragment;
import net.kdt.pojavlaunch.fragments.ProfileTypeSelectFragment;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;
import net.kdt.pojavlaunch.value.launcherprofiles.LauncherProfiles;
import net.kdt.pojavlaunch.value.launcherprofiles.MinecraftProfile;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import net.kdt.pojavlaunch.rkb.skin.RkbSkinLoader;
import net.kdt.pojavlaunch.rkb.skin.RkbSkinView;
import net.kdt.pojavlaunch.value.MinecraftAccount;
import net.kdt.pojavlaunch.extra.ExtraConstants;
import net.kdt.pojavlaunch.extra.ExtraCore;
import net.kdt.pojavlaunch.profiles.ProfileIconCache;
import net.kdt.pojavlaunch.extra.ExtraConstants;
import net.kdt.pojavlaunch.extra.ExtraCore;

public class RkbHomeFragment extends Fragment {

    private static final int BG = Color.rgb(5, 7, 12);
    private static final int PANEL = Color.rgb(7, 13, 22);
    private static final int PANEL_2 = Color.rgb(9, 18, 30);
    private static final int BLUE = Color.rgb(0, 168, 255);
    private static final int BLUE_2 = Color.rgb(27, 96, 255);
    private static final int WHITE = Color.WHITE;
    private static final int MUTED = Color.rgb(150, 168, 190);

    private FrameLayout root;
    private LinearLayout sidebar;
    private LinearLayout center;
    private LinearLayout rightPanel;

    // Real data for the selected instance (loaded in loadInstance()).
    private String instName = "Minecraft";
    private String instVer = "";
    private int instCount = 1;
    private int mSkinRequest; // invalidates stale skin callbacks

    @Nullable
    @Override
    public View onCreateView(
            @NonNull android.view.LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        root = new FrameLayout(requireContext());
        root.setBackgroundColor(BG);

        buildUi();

        return root;
    }

    private void buildUi() {

        root.removeAllViews();
        loadInstance();
        final int sideW = dp(RkbUi.navItemSize(requireContext()) + 22);
        final int rightW = Math.min(dp(430), Math.round(getResources().getDisplayMetrics().widthPixels * 0.45f));

        // =========================================================
        // LEFT SIDEBAR
        // =========================================================

        sidebar = new LinearLayout(requireContext());
        sidebar.setOrientation(LinearLayout.VERTICAL);
        sidebar.setGravity(Gravity.CENTER_HORIZONTAL);
        sidebar.setPadding(dp(10), dp(14), dp(10), dp(14));
        sidebar.setBackgroundResource(R.drawable.bg_rkb_sidebar);

        FrameLayout.LayoutParams sideParams =
                new FrameLayout.LayoutParams(
                        sideW,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        Gravity.LEFT
                );

        android.widget.ScrollView sideScroll = new android.widget.ScrollView(requireContext());
        sideScroll.setFillViewport(true);
        sideScroll.setVerticalScrollBarEnabled(false);
        sideScroll.setBackgroundResource(R.drawable.bg_rkb_sidebar);
        sidebar.setBackground(null);
        sideScroll.addView(sidebar, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(sideScroll, sideParams);

        // Shared sidebar: every item has a real click handler (RkbNav).
        RkbUi.populateSidebar(sidebar, RkbNav.Dest.HOME);

        // =========================================================
        // CENTER
        // =========================================================

        center = new LinearLayout(requireContext());
        center.setOrientation(LinearLayout.VERTICAL);
        center.setPadding(
                dp(18),
                dp(8),
                dp(12),
                dp(10)
        );

        FrameLayout.LayoutParams centerParams =
                new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                );

        centerParams.leftMargin = sideW;
        centerParams.rightMargin = rightW;

        root.addView(center, centerParams);

        buildHeader();
        buildCenterSpace(sideW, rightW);
        buildInstanceRow();

        // =========================================================
        // RIGHT AREA
        // =========================================================

        rightPanel = new LinearLayout(requireContext());
        rightPanel.setOrientation(LinearLayout.VERTICAL);
        rightPanel.setPadding(
                dp(12),
                dp(6),
                dp(14),
                dp(8)
        );

        FrameLayout.LayoutParams rightParams =
                new FrameLayout.LayoutParams(
                        rightW,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        Gravity.RIGHT
                );

        root.addView(rightPanel, rightParams);

        buildAccount();
        buildPlayerArea();
    }

    // =============================================================
    // HEADER
    // =============================================================

    private void buildHeader() {

        LinearLayout header = new LinearLayout(requireContext());
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(12), dp(8), dp(12), dp(8));
        header.setBackgroundResource(R.drawable.bg_rkb_header);

        LinearLayout logoBox = new LinearLayout(requireContext());
        logoBox.setGravity(Gravity.CENTER_VERTICAL);

        TextView rLogo = new TextView(requireContext());
        rLogo.setText("R");
        rLogo.setTextColor(BLUE);
        rLogo.setTextSize(25);
        rLogo.setTypeface(Typeface.DEFAULT_BOLD);
        rLogo.setGravity(Gravity.CENTER);
        rLogo.setBackgroundResource(R.drawable.bg_rkb_logo);

        logoBox.addView(
                rLogo,
                new LinearLayout.LayoutParams(
                        dp(52),
                        dp(52)
                )
        );

        logoBox.addView(spaceH(10));

        LinearLayout logoText = new LinearLayout(requireContext());
        logoText.setOrientation(LinearLayout.VERTICAL);

        TextView launcher = text(
                "RKB LAUNCHER",
                headerButtonWidth() > 100 ? 18 : 15,
                WHITE,
                true
        );

        TextView slogan = text(
                "PLAY  •  EXPLORE  •  CREATE",
                8,
                MUTED,
                true
        );

        logoText.addView(launcher);
        if (headerButtonWidth() > 100) { // slogan only when there is room
            logoText.addView(space(2));
            logoText.addView(slogan);
        }

        logoBox.addView(logoText);

        header.addView(
                logoBox,
                new LinearLayout.LayoutParams(
                        0,
                        -1,
                        1
                )
        );

        TextView youtube = smallButton(
                "YouTube",
                Color.rgb(235, 35, 45)
        );
        youtube.setClickable(true);
        youtube.setOnClickListener(v -> RkbLinks.open(requireContext(), RkbLinks.YOUTUBE, "YouTube"));

        header.addView(
                youtube,
                new LinearLayout.LayoutParams(
                        dp(headerButtonWidth()),
                        dp(RkbUi.isShort(requireContext()) ? 38 : 44)
                )
        );

        header.addView(spaceH(8));

        TextView discord = smallButton(
                "Discord",
                Color.rgb(52, 93, 235)
        );
        discord.setClickable(true);
        discord.setOnClickListener(v -> RkbLinks.open(requireContext(), RkbLinks.DISCORD, "Discord"));

        header.addView(
                discord,
                new LinearLayout.LayoutParams(
                        dp(headerButtonWidth()),
                        dp(RkbUi.isShort(requireContext()) ? 38 : 44)
                )
        );

        center.addView(
                header,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(76)
                )
        );
    }

    // =============================================================
    // CENTER EMPTY SPACE
    // =============================================================

    private static final class InstanceItem {
        final String key, name, version;
        final boolean selected;
        InstanceItem(String key, String name, String version, boolean selected) {
            this.key = key; this.name = name; this.version = version; this.selected = selected;
        }
    }

    /** All instances from launcher_profiles.json (the real Pojav store), sorted by name. */
    private List<InstanceItem> readInstances() {
        List<InstanceItem> out = new ArrayList<>();
        try {
            LauncherProfiles.load();
            final Map<String, MinecraftProfile> map = LauncherProfiles.mainProfileJson.profiles;
            if (map == null) return out;
            String cur = LauncherPreferences.DEFAULT_PREF.getString(LauncherPreferences.PREF_KEY_CURRENT_PROFILE, null);
            if (cur == null || !map.containsKey(cur)) cur = map.isEmpty() ? null : map.keySet().iterator().next();
            for (Map.Entry<String, MinecraftProfile> e : map.entrySet()) {
                MinecraftProfile p = e.getValue();
                String v = Tools.isValidString(p.lastVersionId) ? p.lastVersionId : "?";
                if (MinecraftProfile.LATEST_RELEASE.equals(v)) v = "Latest release";
                else if (MinecraftProfile.LATEST_SNAPSHOT.equals(v)) v = "Latest snapshot";
                out.add(new InstanceItem(e.getKey(), Tools.isValidString(p.name) ? p.name : "Unnamed", v, e.getKey().equals(cur)));
            }
            Collections.sort(out, (x, y) -> x.name.compareToIgnoreCase(y.name));
        } catch (Exception e) {
            android.util.Log.w("RKB-Home", "could not read instances", e);
        }
        return out;
    }

    private void buildCenterSpace(int sideW, int rightW) {
        final List<InstanceItem> items = readInstances();
        TextView title = text("INSTANCES  \u2022  " + items.size(), 11, MUTED, true);
        title.setPadding(dp(4), dp(8), 0, dp(4));
        center.addView(title);

        if (items.isEmpty()) {
            TextView empty = text("No instances yet. Tap New Instance to create one.", 13, MUTED, false);
            empty.setGravity(Gravity.CENTER);
            center.addView(empty, new LinearLayout.LayoutParams(-1, 0, 1));
            return;
        }
        android.util.DisplayMetrics dm = getResources().getDisplayMetrics();
        float centreDp = (dm.widthPixels - sideW - rightW) / dm.density - 30f;
        int span = Math.max(1, (int) (centreDp / 190f));
        RecyclerView rv = new RecyclerView(requireContext());
        rv.setLayoutManager(new GridLayoutManager(requireContext(), span));
        rv.setClipToPadding(false);
        rv.setPadding(0, 0, 0, dp(8));
        rv.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
        rv.setAdapter(new InstanceAdapter(items));
        center.addView(rv, new LinearLayout.LayoutParams(-1, 0, 1));
    }

    private final class InstanceAdapter extends RecyclerView.Adapter<InstanceAdapter.VH> {
        private final List<InstanceItem> items;
        InstanceAdapter(List<InstanceItem> items) { this.items = items; }

        final class VH extends RecyclerView.ViewHolder {
            final TextView name, ver, more;
            VH(View v, TextView name, TextView ver, TextView more) { super(v); this.name = name; this.ver = ver; this.more = more; }
        }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            Context c = parent.getContext();
            LinearLayout card = new LinearLayout(c);
            card.setGravity(Gravity.CENTER_VERTICAL);
            card.setPadding(dp(10), dp(6), dp(2), dp(6));
            card.setClickable(true);
            RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(-1, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(dp(3), dp(3), dp(3), dp(3));
            card.setLayoutParams(lp);
            LinearLayout texts = new LinearLayout(c);
            texts.setOrientation(LinearLayout.VERTICAL);
            TextView name = text("", 13, WHITE, true);
            name.setSingleLine(true);
            name.setEllipsize(android.text.TextUtils.TruncateAt.END);
            TextView ver = text("", 11, MUTED, false);
            ver.setSingleLine(true);
            texts.addView(name);
            texts.addView(ver);
            card.addView(texts, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            TextView more = text("\u22EE", 20, WHITE, false);
            more.setGravity(Gravity.CENTER);
            more.setClickable(true);
            more.setContentDescription("Instance actions");
            card.addView(more, new LinearLayout.LayoutParams(dp(40), dp(40)));
            return new VH(card, name, ver, more);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            final InstanceItem it = items.get(position);
            Context c = h.itemView.getContext();
            h.name.setText(it.name);
            h.ver.setText(it.version);
            h.itemView.setBackground(RkbUi.ripple(c, RkbUi.rounded(c, it.selected ? 0xFF0B2236 : RkbUi.CARD, 12,
                    it.selected ? RkbUi.BLUE : RkbUi.BORDER, it.selected ? 2 : 1)));
            h.itemView.setOnClickListener(v -> selectInstance(it.key));
            h.more.setOnClickListener(v -> showItemMenu(v, it));
        }

        @Override public int getItemCount() { return items.size(); }
    }

    /** Selection = the same preference the launch flow reads, so Launch always starts the selected instance. */
    private void selectInstance(String key) {
        LauncherPreferences.DEFAULT_PREF.edit()
                .putString(LauncherPreferences.PREF_KEY_CURRENT_PROFILE, key).commit();
        if (isAdded()) buildUi();
    }

    private void showItemMenu(View anchor, final InstanceItem it) {
        android.widget.PopupMenu pm = new android.widget.PopupMenu(requireContext(), anchor);
        pm.getMenu().add(0, 1, 0, "Select");
        pm.getMenu().add(0, 2, 1, "Edit");
        pm.getMenu().add(0, 3, 2, "Delete");
        pm.setOnMenuItemClickListener(m -> {
            if (m.getItemId() == 1) selectInstance(it.key);
            else if (m.getItemId() == 2) editInstance(it.key);
            else confirmDeleteInstance(it.key, it.name);
            return true;
        });
        pm.show();
    }

    /** The Pojav editor edits the CURRENT profile, so select the target first, then open it. */
    private void editInstance(String key) {
        LauncherPreferences.DEFAULT_PREF.edit()
                .putString(LauncherPreferences.PREF_KEY_CURRENT_PROFILE, key).commit();
        editCurrentInstance();
    }

    // =============================================================
    // INSTANCE ROW
    // =============================================================

    private void buildInstanceRow() {

        LinearLayout row = new LinearLayout(requireContext());
        row.setGravity(Gravity.CENTER_VERTICAL);

        // EXISTING INSTANCE
        LinearLayout instance = new LinearLayout(requireContext());
        instance.setGravity(Gravity.CENTER_VERTICAL);
        instance.setPadding(
                dp(10),
                dp(8),
                dp(8),
                dp(8)
        );
        instance.setBackgroundResource(R.drawable.bg_rkb_instance);
        instance.setClickable(true);
        instance.setOnClickListener(v -> showInstanceChooser());

        TextView accent = new TextView(requireContext());
        accent.setBackgroundColor(BLUE);

        instance.addView(
                accent,
                new LinearLayout.LayoutParams(
                        dp(4),
                        dp(cardH() - 16)
                )
        );

        instance.addView(spaceH(8));

        TextView grass = text(
                "▣",
                30,
                Color.rgb(90, 205, 75),
                true
        );

        grass.setGravity(Gravity.CENTER);

        instance.addView(
                grass,
                new LinearLayout.LayoutParams(
                        dp(54),
                        dp(54)
                )
        );

        instance.addView(spaceH(8));

        LinearLayout instanceText =
                new LinearLayout(requireContext());

        instanceText.setOrientation(
                LinearLayout.VERTICAL
        );

        instanceText.addView(
                text(
                        instName,
                        14,
                        WHITE,
                        true
                )
        );

        instanceText.addView(space(3));

        instanceText.addView(
                text(
                        instVer + (instCount > 1 ? "  \u2022  " + instCount + " instances" : ""),
                        12,
                        MUTED,
                        false
                )
        );

        instance.addView(
                instanceText,
                new LinearLayout.LayoutParams(
                        dp(120),
                        -2
                )
        );

        instance.addView(spaceH(8));

        ImageView play = new ImageView(requireContext());
        play.setImageResource(R.drawable.ic_rkb_play);
        play.setPadding(dp(9), dp(9), dp(9), dp(9));
        play.setBackgroundResource(
                R.drawable.bg_rkb_nav_item
        );

        play.setOnClickListener(
                v -> launchMinecraft()
        );

        instance.addView(
                play,
                new LinearLayout.LayoutParams(
                        dp(52),
                        dp(52)
                )
        );

        instance.addView(spaceH(4));

        TextView more = text(
                "⋮",
                25,
                WHITE,
                false
        );

        more.setGravity(Gravity.CENTER);
        more.setClickable(true);
        more.setOnClickListener(v -> showInstanceMenu(v));

        instance.addView(
                more,
                new LinearLayout.LayoutParams(
                        dp(34),
                        dp(52)
                )
        );

        row.addView(
                instance,
                new LinearLayout.LayoutParams(
                        0,
                        dp(cardH()),
                        1.2f
                )
        );

        row.addView(spaceH(18));

        // NEW INSTANCE
        LinearLayout newInstance =
                new LinearLayout(requireContext());

        newInstance.setGravity(Gravity.CENTER);
        newInstance.setClickable(true);
        newInstance.setOnClickListener(v -> openNewInstance());
        newInstance.setBackgroundResource(
                R.drawable.bg_rkb_new_instance
        );

        ImageView plus = new ImageView(requireContext());
        plus.setImageResource(
                R.drawable.ic_rkb_add
        );

        newInstance.addView(
                plus,
                new LinearLayout.LayoutParams(
                        dp(30),
                        dp(30)
                )
        );

        newInstance.addView(spaceH(9));

        newInstance.addView(
                text(
                        "New Instance",
                        12,
                        BLUE,
                        true
                )
        );

        row.addView(
                newInstance,
                new LinearLayout.LayoutParams(
                        0,
                        dp(cardH()),
                        1f
                )
        );

        center.addView(
                row,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(94)
                )
        );
    }

    // =============================================================
    // ACCOUNT
    // =============================================================

    private void buildAccount() {

        LinearLayout account =
                new LinearLayout(requireContext());

        account.setGravity(Gravity.CENTER_VERTICAL);
        account.setPadding(
                dp(12),
                dp(5),
                dp(8),
                dp(5)
        );

        account.setBackgroundResource(
                R.drawable.bg_rkb_account
        );
        account.setClickable(true);
        account.setOnClickListener(v -> RkbNav.openAccount(requireActivity()));

        TextView avatar = text(
                "R",
                18,
                WHITE,
                true
        );

        avatar.setGravity(Gravity.CENTER);
        avatar.setBackground(
                rounded(
                        Color.rgb(35, 35, 43),
                        18
                )
        );

        account.addView(
                avatar,
                new LinearLayout.LayoutParams(
                        dp(RkbUi.isShort(requireContext()) ? 38 : 44),
                        dp(RkbUi.isShort(requireContext()) ? 38 : 44)
                )
        );

        account.addView(spaceH(12));

        account.addView(
                text(
                        RkbUi.accountName(requireContext()),
                        17,
                        WHITE,
                        true
                )
        );

        account.addView(spaceH(12));

        TextView local = text(
                RkbUi.accountBadge(requireContext()),
                11,
                Color.rgb(190, 150, 255),
                true
        );

        local.setGravity(Gravity.CENTER);
        local.setPadding(
                dp(9),
                0,
                dp(9),
                0
        );

        local.setBackground(
                rounded(
                        Color.rgb(50, 35, 82),
                        14
                )
        );

        account.addView(
                local,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        dp(34)
                )
        );

        account.addView(spaceH(4));

        account.addView(
                text(
                        "▼",
                        11,
                        MUTED,
                        true
                )
        );

        rightPanel.addView(
                account,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(RkbUi.isShort(requireContext()) ? 54 : 62)
                )
        );
    }

    // =============================================================
    // PLAYER + LAUNCH
    // =============================================================

    private void buildPlayerArea() {
        final android.util.DisplayMetrics dm = getResources().getDisplayMetrics();
        final int hDp = Math.round(dm.heightPixels / dm.density);
        final boolean tight = hDp < 440;
        // Fixed rows (label, launch, version, gaps, account panel) must always fit; the skin preview
        // is the only flexible element. Previously a fixed 205dp preview pushed LAUNCH off-screen.
        final int previewH = Math.max(64, Math.min(205, hDp - (tight ? 270 : 330)));

        LinearLayout player =
                new LinearLayout(requireContext());

        player.setOrientation(
                LinearLayout.VERTICAL
        );
        player.setGravity(Gravity.CENTER_HORIZONTAL);
        player.setPadding(
                dp(18),
                dp(tight ? 8 : 18),
                dp(18),
                dp(10)
        );

        // PLAYER LABEL
        TextView label = text(
                RkbUi.accountName(requireContext()),
                17,
                WHITE,
                true
        );

        label.setGravity(Gravity.CENTER);
        label.setPadding(
                dp(18),
                dp(8),
                dp(18),
                dp(8)
        );

        label.setBackgroundResource(
                R.drawable.bg_rkb_version
        );

        player.addView(
                label,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        dp(tight ? 34 : 48)
                )
        );

        player.addView(space(12));

        // SIMPLE PLAYER PREVIEW
        LinearLayout skin =
                createPlayerPreview();

        player.addView(
                skin,
                new LinearLayout.LayoutParams(
                        dp(Math.max(48, previewH * 150 / 205)),
                        dp(previewH)
                )
        );

        player.addView(space(6));

        // LAUNCH BUTTON
        TextView launch =
                new TextView(requireContext());

        launch.setText("▶  LAUNCH");
        launch.setTextSize(22);
        launch.setTextColor(WHITE);
        launch.setTypeface(
                Typeface.DEFAULT,
                Typeface.BOLD
        );
        launch.setGravity(Gravity.CENTER);
        launch.setBackgroundResource(
                R.drawable.bg_rkb_launch
        );

        launch.setContentDescription("Launch Minecraft");
        launch.setClickable(true);
        launch.setOnClickListener(
                v -> launchMinecraft()
        );

        player.addView(
                launch,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(tight ? 62 : 78)
                )
        );

        player.addView(space(14));

        // VERSION SELECTOR
        LinearLayout version =
                new LinearLayout(requireContext());

        version.setGravity(Gravity.CENTER_VERTICAL);
        version.setPadding(
                dp(18),
                0,
                dp(16),
                0
        );

        version.setBackgroundResource(
                R.drawable.bg_rkb_version
        );
        version.setClickable(true);
        version.setOnClickListener(v -> showInstanceChooser());

        ImageView vPlay =
                new ImageView(requireContext());

        vPlay.setImageResource(
                R.drawable.ic_rkb_play
        );

        version.addView(
                vPlay,
                new LinearLayout.LayoutParams(
                        dp(28),
                        dp(28)
                )
        );

        version.addView(spaceH(12));

        TextView versionName =
                text(
                        "Minecraft " + instVer,
                        17,
                        WHITE,
                        true
                );

        version.addView(
                versionName,
                new LinearLayout.LayoutParams(
                        0,
                        -2,
                        1
                )
        );

        ImageView down =
                new ImageView(requireContext());

        down.setImageResource(
                R.drawable.ic_rkb_chevron_down
        );

        version.addView(
                down,
                new LinearLayout.LayoutParams(
                        dp(28),
                        dp(28)
                )
        );

        player.addView(
                version,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(tight ? 46 : 58)
                )
        );

        android.widget.ScrollView playerScroll = new android.widget.ScrollView(requireContext());
        playerScroll.setFillViewport(true);
        playerScroll.setVerticalScrollBarEnabled(false);
        playerScroll.addView(player, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        rightPanel.addView(
                playerScroll,
                new LinearLayout.LayoutParams(
                        -1,
                        0,
                        1
                )
        );
    }

    // =============================================================
    // PLAYER PREVIEW
    // =============================================================

    private LinearLayout createPlayerPreview() {
        LinearLayout holder = new LinearLayout(requireContext());
        holder.setGravity(Gravity.CENTER);
        final RkbSkinView view = new RkbSkinView(requireContext());
        holder.addView(view, new LinearLayout.LayoutParams(-1, -1));
        // Resolved off the main thread: official profile skin for Microsoft accounts, default otherwise.
        final int request = ++mSkinRequest;
        final MinecraftAccount acc = RkbUi.currentAccount(requireContext());
        RkbSkinLoader.load(requireContext(), acc, result -> {
            if (request != mSkinRequest || !isAdded()) return; // screen rebuilt / destroyed
            view.setSkin(result.skin, result.slim);
        });
        return holder;
    }

    // =============================================================

    private TextView smallButton(
            String name,
            int color) {

        TextView button =
                text(
                        name,
                        13,
                        WHITE,
                        true
                );

        button.setGravity(Gravity.CENTER);

        button.setBackground(
                rounded(
                        color,
                        24
                )
        );

        return button;
    }

    // =============================================================
    // LAUNCH
    // =============================================================

    /** Called by LauncherActivity after login / account switch: re-reads account + skin. */
    public void refreshAccount() {
        if (isAdded() && getView() != null) buildUi();
    }

    private int headerButtonWidth() {
        android.util.DisplayMetrics dm = getResources().getDisplayMetrics();
        float centreDp = dm.widthPixels / dm.density * 0.55f - 70f; // space left of the player panel
        return centreDp < 420f ? 84 : 118;
    }

    /** Compact card heights: phones in landscape get small cards, tablets keep a bit more room. */
    private int cardH() {
        android.util.DisplayMetrics dm = getResources().getDisplayMetrics();
        return dm.heightPixels / dm.density < 440f ? 54 : 64;
    }

    private void loadInstance() {
        try {
            LauncherProfiles.load();
            Map<String, MinecraftProfile> map = LauncherProfiles.mainProfileJson.profiles;
            if (map == null || map.isEmpty()) return;
            String key = LauncherPreferences.DEFAULT_PREF.getString(
                    LauncherPreferences.PREF_KEY_CURRENT_PROFILE, null);
            MinecraftProfile p = key == null ? null : map.get(key);
            if (p == null) p = map.values().iterator().next();
            String n = Tools.isValidString(p.name) ? p.name : "Minecraft";
            instCount = map.size();
            instName = n.length() > 14 ? n.substring(0, 13) + "\u2026" : n;
            String v = Tools.isValidString(p.lastVersionId) ? p.lastVersionId : "";
            if (MinecraftProfile.LATEST_RELEASE.equals(v)) v = "Latest release";
            else if (MinecraftProfile.LATEST_SNAPSHOT.equals(v)) v = "Latest snapshot";
            instVer = v;
        } catch (Exception ignored) {
            // keep the defaults; the Home screen must still render
        }
    }

    private void openNewInstance() {
        if (!isAdded()) return;
        // ignore repeated taps: only one "new instance" flow at a time
        if (requireActivity().getSupportFragmentManager().findFragmentByTag(ProfileTypeSelectFragment.TAG) != null) return;
        Tools.swapFragment(requireActivity(), ProfileTypeSelectFragment.class,
                ProfileTypeSelectFragment.TAG, null);
    }

    /** All instances (sorted), current one checked. Selecting persists the choice. */
    private void showInstanceChooser() {
        if (!isAdded()) return;
        try {
            LauncherProfiles.load();
            final Map<String, MinecraftProfile> map = LauncherProfiles.mainProfileJson.profiles;
            final List<String> keys = new ArrayList<>(map.keySet());
            Collections.sort(keys, (a, b) -> String.valueOf(map.get(a).name)
                    .compareToIgnoreCase(String.valueOf(map.get(b).name)));
            String cur = LauncherPreferences.DEFAULT_PREF.getString(LauncherPreferences.PREF_KEY_CURRENT_PROFILE, null);
            String[] labels = new String[keys.size()];
            int checked = 0;
            for (int i = 0; i < keys.size(); i++) {
                MinecraftProfile p = map.get(keys.get(i));
                String n = Tools.isValidString(p.name) ? p.name : "Unnamed";
                String v = Tools.isValidString(p.lastVersionId) ? p.lastVersionId : "?";
                if (MinecraftProfile.LATEST_RELEASE.equals(v)) v = "Latest release";
                else if (MinecraftProfile.LATEST_SNAPSHOT.equals(v)) v = "Latest snapshot";
                labels[i] = n + "  \u2014  " + v;
                if (keys.get(i).equals(cur)) checked = i;
            }
            new android.app.AlertDialog.Builder(requireContext())
                    .setTitle("Instances (" + keys.size() + ")")
                    .setSingleChoiceItems(labels, checked, (d, which) -> {
                        LauncherPreferences.DEFAULT_PREF.edit()
                                .putString(LauncherPreferences.PREF_KEY_CURRENT_PROFILE, keys.get(which)).commit();
                        d.dismiss();
                        if (isAdded()) buildUi();
                    })
                    .setNeutralButton("New instance", (d, w) -> openNewInstance())
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Could not read instances: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void showInstanceMenu(View anchor) {
        android.widget.PopupMenu pm = new android.widget.PopupMenu(requireContext(), anchor);
        pm.getMenu().add(0, 1, 0, "Edit instance");
        pm.getMenu().add(0, 2, 1, "Switch instance");
        pm.getMenu().add(0, 3, 2, "Delete instance");
        pm.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 1) editCurrentInstance();
            else if (item.getItemId() == 2) showInstanceChooser();
            else confirmDeleteInstance();
            return true;
        });
        pm.show();
    }

    private void confirmDeleteInstance() {
        String key = LauncherPreferences.DEFAULT_PREF.getString(LauncherPreferences.PREF_KEY_CURRENT_PROFILE, null);
        confirmDeleteInstance(key, instName);
    }

    private void confirmDeleteInstance(final String key, String name) {
        if (!isAdded() || key == null) return;
        if (instCount < 2) {
            Toast.makeText(requireContext(), "You need at least one instance", Toast.LENGTH_SHORT).show();
            return;
        }
        new android.app.AlertDialog.Builder(requireContext())
                .setTitle("Delete " + name + "?")
                .setMessage("This removes the instance from the launcher. Its world and mod files on disk are not deleted.")
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton("Delete", (d, w) -> {
                    try {
                        LauncherProfiles.load();
                        Map<String, MinecraftProfile> map = LauncherProfiles.mainProfileJson.profiles;
                        if (!map.containsKey(key) || map.size() < 2) return;
                        ProfileIconCache.dropIcon(key);
                        map.remove(key);
                        LauncherProfiles.write();
                        String cur = LauncherPreferences.DEFAULT_PREF.getString(
                                LauncherPreferences.PREF_KEY_CURRENT_PROFILE, null);
                        if (key.equals(cur) || !map.containsKey(cur)) {
                            LauncherPreferences.DEFAULT_PREF.edit().putString(
                                    LauncherPreferences.PREF_KEY_CURRENT_PROFILE, map.keySet().iterator().next()).commit();
                        }
                    } catch (Exception e) {
                        Toast.makeText(requireContext(), "Delete failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                    if (isAdded()) buildUi();
                }).show();
    }

    private void editCurrentInstance() {
        if (!isAdded()) return;
        Tools.swapFragment(requireActivity(), ProfileEditorFragment.class,
                ProfileEditorFragment.TAG, null);
    }

    private void launchMinecraft() {

        try {

            ExtraCore.setValue(
                    ExtraConstants.LAUNCH_GAME,
                    true
            );

        } catch (Exception e) {

            Toast.makeText(
                    requireContext(),
                    "Unable to start Minecraft",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    // =============================================================
    // TEXT
    // =============================================================

    private TextView text(
            String value,
            float size,
            int color,
            boolean bold) {

        TextView t =
                new TextView(requireContext());

        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);

        if (bold) {
            t.setTypeface(
                    Typeface.DEFAULT,
                    Typeface.BOLD
            );
        }

        return t;
    }

    // =============================================================
    // VERTICAL SPACE
    // =============================================================

    private Space space(int value) {

        Space s =
                new Space(requireContext());

        s.setLayoutParams(
                new LinearLayout.LayoutParams(
                        1,
                        dp(value)
                )
        );

        return s;
    }

    // =============================================================
    // HORIZONTAL SPACE
    // =============================================================

    private Space spaceH(int value) {

        Space s =
                new Space(requireContext());

        s.setLayoutParams(
                new LinearLayout.LayoutParams(
                        dp(value),
                        1
                )
        );

        return s;
    }

    // =============================================================
    // ROUNDED BACKGROUND
    // =============================================================

    private GradientDrawable rounded(
            int color,
            int radius) {

        GradientDrawable drawable =
                new GradientDrawable();

        drawable.setColor(color);
        drawable.setCornerRadius(
                dp(radius)
        );

        return drawable;
    }

    // =============================================================
    // DP
    // =============================================================

    private int dp(int value) {

        return Math.round(
                value *
                        getResources()
                                .getDisplayMetrics()
                                .density
        );
    }
}