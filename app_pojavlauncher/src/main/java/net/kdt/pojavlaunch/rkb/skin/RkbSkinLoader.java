package net.kdt.pojavlaunch.rkb.skin;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.value.MinecraftAccount;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Resolves the skin of the selected account WITHOUT touching the main thread.
 *
 * Microsoft accounts: the profile UUID is looked up on Mojang's session server, the "textures"
 * property gives the real skin URL and model (classic/slim). Results are cached on disk for an hour
 * (the session server is rate limited) and a stale cache is used when offline.
 * Local accounts: there is no Mojang profile, so the stored custom skin URL is previewed if set,
 * otherwise the default skin is shown. Nothing here claims a skin is applied in multiplayer.
 * No tokens are sent or logged; the lookup is a public, unauthenticated request.
 */
public final class RkbSkinLoader {
    private static final String TAG = "RKB-Skin";
    private static final long CACHE_MAX_AGE_MS = 60L * 60L * 1000L;
    private static final int MAX_BYTES = 256 * 1024;
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    public static final class Result {
        public final Bitmap skin;
        public final boolean slim;
        public final boolean isDefault;
        @Nullable public final String note;

        Result(Bitmap skin, boolean slim, boolean isDefault, @Nullable String note) {
            this.skin = skin; this.slim = slim; this.isDefault = isDefault; this.note = note;
        }
    }

    public interface Callback { void onResult(@NonNull Result result); }

    private RkbSkinLoader() {}

    /** Callback is delivered on the main thread. Callers must check that their view is still alive. */
    public static void load(@NonNull Context context, @Nullable MinecraftAccount account, @NonNull Callback cb) {
        final Context app = context.getApplicationContext();
        // copy the few fields we need so the account object is not touched from the worker thread
        final boolean ms = account != null && account.isMicrosoft;
        final String uuid = account == null ? null : account.profileId;
        final String customUrl = account == null ? null : account.skinUrl;
        EXECUTOR.execute(() -> {
            Result r;
            try {
                r = resolve(app, ms, uuid, customUrl);
            } catch (Throwable t) {
                Log.w(TAG, "skin resolve failed: " + t);
                r = defaultResult(app, "Could not load the skin, showing the default one");
            }
            final Result out = r;
            MAIN.post(() -> cb.onResult(out));
        });
    }

    private static Result resolve(Context ctx, boolean microsoft, String uuid, String customUrl) {
        if (microsoft) {
            String id = normalizeUuid(uuid);
            if (id != null) {
                File png = new File(ctx.getCacheDir(), "rkb_skin_" + id + ".png");
                File meta = new File(ctx.getCacheDir(), "rkb_skin_" + id + ".slim");
                boolean fresh = png.exists() && System.currentTimeMillis() - png.lastModified() < CACHE_MAX_AGE_MS;
                if (!fresh) {
                    try {
                        fetchProfileSkin(id, png, meta);
                        fresh = true;
                    } catch (Exception e) {
                        Log.w(TAG, "profile skin fetch failed: " + e);
                    }
                }
                Bitmap cached = decodeFile(png);
                if (cached != null) {
                    return new Result(cached, meta.exists(), false, fresh ? null : "Offline - showing the last saved skin");
                }
                return defaultResult(ctx, "Skin unavailable (offline or no skin set) - showing the default");
            }
        } else if (customUrl != null && !customUrl.isEmpty()) {
            try {
                Bitmap b = downloadSkin(customUrl, false);
                if (b != null) return new Result(b, false, false, "Preview of your saved skin URL");
            } catch (Exception e) {
                Log.w(TAG, "custom skin preview failed: " + e);
            }
            return defaultResult(ctx, "Could not load the saved skin URL");
        }
        return defaultResult(ctx, null);
    }

    private static Result defaultResult(Context ctx, @Nullable String note) {
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inScaled = false;
        Bitmap b = BitmapFactory.decodeResource(ctx.getResources(), R.drawable.rkb_default_alex, o);
        return new Result(b, true, true, note);
    }

    @Nullable
    private static String normalizeUuid(@Nullable String raw) {
        if (raw == null) return null;
        String s = raw.replace("-", "").toLowerCase(Locale.ROOT);
        if (!s.matches("[0-9a-f]{32}") || s.matches("0+")) return null;
        return s;
    }

    private static void fetchProfileSkin(String id, File png, File meta) throws Exception {
        String json = readString(open("https://sessionserver.mojang.com/session/minecraft/profile/" + id), 64 * 1024);
        JSONArray props = new JSONObject(json).getJSONArray("properties");
        String value = null;
        for (int i = 0; i < props.length(); i++) {
            JSONObject p = props.getJSONObject(i);
            if ("textures".equals(p.optString("name"))) { value = p.getString("value"); break; }
        }
        if (value == null) throw new IllegalStateException("profile has no textures");
        JSONObject tex = new JSONObject(new String(Base64.decode(value, Base64.DEFAULT), "UTF-8"))
                .getJSONObject("textures");
        JSONObject skin = tex.optJSONObject("SKIN");
        if (skin == null) throw new IllegalStateException("profile has no custom skin");
        boolean slim = false;
        JSONObject md = skin.optJSONObject("metadata");
        if (md != null) slim = "slim".equals(md.optString("model"));
        Bitmap b = downloadSkin(skin.getString("url"), true);
        if (b == null) throw new IllegalStateException("skin image invalid");
        try (FileOutputStream out = new FileOutputStream(png)) {
            b.compress(Bitmap.CompressFormat.PNG, 100, out);
        }
        //noinspection ResultOfMethodCallIgnored
        if (slim) meta.createNewFile(); else meta.delete();
    }

    @Nullable
    private static Bitmap downloadSkin(String url, boolean mojangOnly) throws Exception {
        if (url.startsWith("http://")) url = "https://" + url.substring(7);
        URL u = new URL(url);
        if (!"https".equals(u.getProtocol())) throw new IllegalArgumentException("https required");
        if (mojangOnly && !u.getHost().endsWith(".minecraft.net")) throw new IllegalArgumentException("unexpected skin host");
        byte[] data = readBytes(open(u.toString()), MAX_BYTES);
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inScaled = false;
        Bitmap b = BitmapFactory.decodeByteArray(data, 0, data.length, o);
        if (b == null) return null;
        boolean ok = b.getWidth() == 64 && (b.getHeight() == 64 || b.getHeight() == 32);
        return ok ? b : null;
    }

    private static HttpURLConnection open(String url) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(8000);
        c.setReadTimeout(8000);
        c.setInstanceFollowRedirects(true);
        int code = c.getResponseCode();
        if (code != 200) throw new IllegalStateException("HTTP " + code);
        return c;
    }

    private static String readString(HttpURLConnection c, int max) throws Exception {
        return new String(readBytes(c, max), "UTF-8");
    }

    private static byte[] readBytes(HttpURLConnection c, int max) throws Exception {
        try (InputStream in = c.getInputStream()) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
                if (out.size() > max) throw new IllegalStateException("response too large");
            }
            return out.toByteArray();
        } finally {
            c.disconnect();
        }
    }

    @Nullable
    private static Bitmap decodeFile(File f) {
        if (!f.exists()) return null;
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inScaled = false;
        return BitmapFactory.decodeFile(f.getAbsolutePath(), o);
    }
}
