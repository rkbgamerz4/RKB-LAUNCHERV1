package net.kdt.pojavlaunch.rkb.ui;

import android.app.Activity;
import android.os.Bundle;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;

/**
 * Download screen. Each category has its own real catalog URL; content is never mixed between
 * Mods / Texture Packs / Shaders / Worlds / Modpacks / Favorites.
 */
public class RkbDownloadFragment extends Fragment {
    private static final class Category {
        final String label;
        final String url;
        final boolean vanilla;
        Category(String label, String url) { this(label, url, false); }
        Category(String label, String url, boolean vanilla) {
            this.label = label;
            this.url = url;
            this.vanilla = vanilla;
        }
    }

    private static final Category[] CATEGORIES = {
            new Category("MODS", "https://modrinth.com/mods"),
            new Category("TEXTURE PACKS", "https://modrinth.com/resourcepacks"),
            new Category("SHADERS", "https://modrinth.com/shaders"),
            new Category("WORLDS", "https://www.planetminecraft.com/projects/world/"),
            new Category("MODPACKS", "https://modrinth.com/modpacks"),
            new Category("INSTALL VANILLA", "", true),
            new Category("FAVORITES", "https://modrinth.com/favorites")
    };

    private final List<TextView> mTabs = new ArrayList<>();
    private WebView mWebView;
    private LinearLayout mVanillaPanel;
    private EditText mSearch;
    private int mSelected = 0;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        final android.content.Context c = requireContext();

        LinearLayout content = new LinearLayout(c);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(RkbUi.dp(c, 12), RkbUi.dp(c, 4), RkbUi.dp(c, 12), RkbUi.dp(c, 8));
        content.addView(tabs(c));

        LinearLayout searchRow = new LinearLayout(c);
        searchRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
        searchRow.setPadding(0, RkbUi.dp(c, 10), 0, RkbUi.dp(c, 10));
        mSearch = new EditText(c);
        mSearch.setHint("Search this category");
        mSearch.setHintTextColor(RkbUi.MUTED);
        mSearch.setTextColor(RkbUi.WHITE);
        mSearch.setSingleLine(true);
        mSearch.setInputType(InputType.TYPE_CLASS_TEXT);
        mSearch.setPadding(RkbUi.dp(c, 14), 0, RkbUi.dp(c, 14), 0);
        mSearch.setBackground(RkbUi.rounded(c, RkbUi.CARD, 14, RkbUi.BORDER, 1));
        mSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (event == null || event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                loadSelected();
                return true;
            }
            return false;
        });
        searchRow.addView(mSearch, new LinearLayout.LayoutParams(0,
                RkbUi.dp(c, 44), 1f));
        TextView open = RkbUi.button(c, "OPEN", true, v -> loadSelected());
        LinearLayout.LayoutParams openLp = new LinearLayout.LayoutParams(
                RkbUi.dp(c, 84), RkbUi.dp(c, 44));
        openLp.leftMargin = RkbUi.dp(c, 8);
        searchRow.addView(open, openLp);
        content.addView(searchRow, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        mVanillaPanel = RkbUi.card(c);
        mVanillaPanel.addView(RkbUi.text(c, "Install Vanilla", 17, RkbUi.WHITE, true));
        TextView vanillaNote = RkbUi.text(c,
                "Create a normal instance, then choose the Minecraft version you want to install.",
                12, RkbUi.MUTED, false);
        vanillaNote.setPadding(0, RkbUi.dp(c, 6), 0, RkbUi.dp(c, 10));
        mVanillaPanel.addView(vanillaNote);
        mVanillaPanel.addView(RkbUi.button(c, "CREATE INSTANCE", true, v -> {
            Activity a = RkbUi.activityFrom(v.getContext());
            if (a != null) RkbNav.go(a, RkbNav.Dest.NEW_INSTANCE);
        }), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, RkbUi.dp(c, 46)));
        content.addView(mVanillaPanel, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        mWebView = new WebView(c);
        WebSettings settings = mWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        mWebView.setWebViewClient(new WebViewClient());
        mWebView.setBackgroundColor(RkbUi.CARD);
        content.addView(mWebView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        selectCategory(0);
        return RkbUi.scaffold(c, RkbNav.Dest.DOWNLOAD, "Download",
                "Browse real catalogs by content type.", content);
    }

    private View tabs(android.content.Context c) {
        HorizontalScrollView scroll = new HorizontalScrollView(c);
        scroll.setHorizontalScrollBarEnabled(false);
        LinearLayout row = new LinearLayout(c);
        row.setOrientation(LinearLayout.HORIZONTAL);
        for (int i = 0; i < CATEGORIES.length; i++) {
            final int index = i;
            TextView tab = RkbUi.text(c, CATEGORIES[i].label, 11, RkbUi.WHITE, true);
            tab.setGravity(android.view.Gravity.CENTER);
            tab.setPadding(RkbUi.dp(c, 12), 0, RkbUi.dp(c, 12), 0);
            tab.setClickable(true);
            tab.setOnClickListener(v -> selectCategory(index));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, RkbUi.dp(c, 38));
            lp.rightMargin = RkbUi.dp(c, 6);
            row.addView(tab, lp);
            mTabs.add(tab);
        }
        scroll.addView(row, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return scroll;
    }

    private void selectCategory(int index) {
        if (index < 0 || index >= CATEGORIES.length) return;
        mSelected = index;
        mVanillaPanel.setVisibility(CATEGORIES[index].vanilla ? View.VISIBLE : View.GONE);
        mWebView.setVisibility(CATEGORIES[index].vanilla ? View.GONE : View.VISIBLE);
        mSearch.setEnabled(!CATEGORIES[index].vanilla);
        for (int i = 0; i < mTabs.size(); i++) {
            boolean selected = i == index;
            mTabs.get(i).setBackground(RkbUi.rounded(
                    mTabs.get(i).getContext(),
                    selected ? RkbUi.BLUE : RkbUi.CARD_2, 18,
                    selected ? 0x00000000 : RkbUi.BORDER, selected ? 0 : 1));
            mTabs.get(i).setTextColor(selected ? RkbUi.WHITE : RkbUi.MUTED);
        }
        loadSelected();
    }

    private void loadSelected() {
        if (mWebView == null || mSelected < 0 || mSelected >= CATEGORIES.length) return;
        Category category = CATEGORIES[mSelected];
        if (category.vanilla) return;
        String url = category.url;
        String query = mSearch == null ? "" : mSearch.getText().toString().trim();
        if (!query.isEmpty()) {
            url += (url.contains("?") ? "&" : "?") + "q=" + android.net.Uri.encode(query);
        }
        mWebView.loadUrl(url);
    }

    @Override public void onDestroyView() {
        if (mWebView != null) {
            mWebView.stopLoading();
            mWebView.setWebViewClient(null);
            mWebView.destroy();
            mWebView = null;
        }
        mTabs.clear();
        super.onDestroyView();
    }
}
