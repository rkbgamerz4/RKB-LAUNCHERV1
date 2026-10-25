package net.kdt.pojavlaunch.rkb.discord;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewGroup;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.Toast;

import net.kdt.pojavlaunch.BaseActivity;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Full Discord OAuth (implicit token) → fetch @me → save session.
 * Redirect: rkblauncher://oauth/discord#access_token=...
 */
public class DiscordAuthActivity extends BaseActivity {

    private static final String TAG = "RKB-DiscordAuth";
    private WebView mWebView;
    private boolean mHandled;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FrameLayout root = new FrameLayout(this);
        root.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(root);

        mWebView = new WebView(this);
        root.addView(mWebView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        WebSettings s = mWebView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setSaveFormData(false);

        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                checkRedirect(url);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                if (checkRedirect(url)) return true;
                return false;
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (checkRedirect(url)) return true;
                return false;
            }
        });

        mWebView.loadUrl(DiscordRPC.getAuthorizeUrl());
    }

    /** Returns true if this was the OAuth redirect */
    private boolean checkRedirect(String url) {
        if (mHandled || url == null) return false;
        if (!url.startsWith("rkblauncher://") && !url.contains("access_token=")) {
            return false;
        }
        mHandled = true;

        String fragment = url;
        int hash = url.indexOf('#');
        if (hash >= 0) fragment = url.substring(hash + 1);
        int q = url.indexOf('?');
        if (hash < 0 && q >= 0) fragment = url.substring(q + 1);

        Map<String, String> params = parseParams(fragment);
        String token = params.get("access_token");
        if (token == null || token.isEmpty()) {
            runOnUiThread(() -> {
                Toast.makeText(this, "Discord login failed", Toast.LENGTH_LONG).show();
                finish();
            });
            return true;
        }

        // Fetch user on background thread
        new Thread(() -> {
            try {
                JSONObject user = fetchMe(token);
                String username = user.optString("global_name", null);
                if (username == null || username.isEmpty()) {
                    username = user.optString("username", "Discord User");
                }
                String id = user.optString("id", "");
                DiscordRPC.saveSession(this, username, id, token);
                final String name = username;
                runOnUiThread(() -> {
                    Toast.makeText(this, "Connected to Discord: " + name, Toast.LENGTH_LONG).show();
                    finish();
                });
            } catch (Exception e) {
                Log.e(TAG, "Failed to fetch Discord user", e);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Discord login error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    finish();
                });
            }
        }).start();
        return true;
    }

    private static Map<String, String> parseParams(String query) {
        Map<String, String> map = new HashMap<>();
        if (query == null) return map;
        for (String part : query.split("&")) {
            int eq = part.indexOf('=');
            if (eq > 0) {
                map.put(part.substring(0, eq), part.substring(eq + 1));
            }
        }
        return map;
    }

    private static JSONObject fetchMe(String token) throws Exception {
        URL url = new URL("https://discord.com/api/users/@me");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "Bearer " + token);
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(15000);
        int code = conn.getResponseCode();
        BufferedReader br = new BufferedReader(new InputStreamReader(
                code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) sb.append(line);
        br.close();
        if (code < 200 || code >= 300) {
            throw new Exception("HTTP " + code + ": " + sb);
        }
        return new JSONObject(sb.toString());
    }

    @Override
    protected void onDestroy() {
        if (mWebView != null) {
            mWebView.stopLoading();
            mWebView.destroy();
            mWebView = null;
        }
        super.onDestroy();
    }
}
