package net.kdt.pojavlaunch.rkb.discord;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * RKB Discord Rich Presence + OAuth.
 * Social SDK AAR + JNI (rkb_discord_jni) → Discord_Client_UpdateRichPresence.
 */
public final class DiscordRPC {

    private static final String TAG = "RKB-DiscordRPC";
    public static final String CLIENT_ID = "1555829146490241095";
    public static final String REDIRECT_URI = "rkblauncher://oauth/discord";

    private static final String PREF = "rkb_discord";
    private static final String KEY_USER = "discord_user";
    private static final String KEY_ID = "discord_id";
    private static final String KEY_TOKEN = "access_token";
    private static final String KEY_ENABLED = "rpc_enabled";

    private static boolean sSdkReady = false;

    private DiscordRPC() {}

    public static void initSdk(Activity activity) {
        if (activity == null) return;
        try {
            Class<?> clazz = Class.forName("com.discord.socialsdk.DiscordSocialSdkInit");
            clazz.getMethod("setEngineActivity", Activity.class).invoke(null, activity);
            sSdkReady = true;
            Log.i(TAG, "DiscordSocialSdkInit OK");
        } catch (Throwable t) {
            Log.w(TAG, "DiscordSocialSdkInit: " + t.getMessage());
        }

        boolean nativeOk = DiscordNative.init();
        Log.i(TAG, "DiscordNative.init = " + nativeOk);

        if (isLinked(activity)) {
            String token = getAccessToken(activity);
            if (token != null) DiscordNative.updateToken(token);
            setBrowsingLauncher();
        }
    }

    public static boolean isSdkReady() {
        return sSdkReady || DiscordNative.isLoaded();
    }

    public static String getAuthorizeUrl() {
        return "https://discord.com/api/oauth2/authorize"
                + "?client_id=" + CLIENT_ID
                + "&response_type=token"
                + "&scope=identify"
                + "&redirect_uri=" + android.net.Uri.encode(REDIRECT_URI)
                + "&prompt=none";
    }

    public static void startLink(Context ctx) {
        try {
            Intent i = new Intent(ctx, DiscordAuthActivity.class);
            if (!(ctx instanceof Activity)) i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(i);
        } catch (Exception e) {
            Log.e(TAG, "startLink", e);
        }
    }

    public static void saveSession(Context ctx, String username, String userId, String token) {
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
                .putString(KEY_USER, username)
                .putString(KEY_ID, userId != null ? userId : "")
                .putString(KEY_TOKEN, token != null ? token : "")
                .putBoolean(KEY_ENABLED, true)
                .apply();
        Log.i(TAG, "Discord linked: " + username);
        if (token != null) DiscordNative.updateToken(token);
        setBrowsingLauncher();
    }

    public static String getLinkedUser(Context ctx) {
        return ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString(KEY_USER, null);
    }

    public static String getAccessToken(Context ctx) {
        return ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString(KEY_TOKEN, null);
    }

    public static boolean isLinked(Context ctx) {
        return getLinkedUser(ctx) != null;
    }

    public static boolean isEnabled(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        return sp.getBoolean(KEY_ENABLED, true) && sp.getString(KEY_USER, null) != null;
    }

    public static void setEnabled(Context ctx, boolean enabled) {
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
                .putBoolean(KEY_ENABLED, enabled).apply();
        if (!enabled) clearPresence();
        else setBrowsingLauncher();
    }

    public static void unlink(Context ctx) {
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().clear().apply();
        clearPresence();
        Log.i(TAG, "Discord unlinked");
    }

    public static void setPlaying(String details) {
        Log.i(TAG, "RPC → " + details);
        DiscordNative.setPresence(details, "RKB Launcher");
    }

    public static void setBrowsingLauncher() {
        setPlaying("Browsing launcher");
    }

    public static void setPlayingMinecraft(String version) {
        if (version == null || version.isEmpty()) setPlaying("Playing Minecraft");
        else setPlaying("Playing Minecraft " + version);
    }

    public static void clearPresence() {
        DiscordNative.clearPresence();
        Log.i(TAG, "RPC cleared");
    }
}
