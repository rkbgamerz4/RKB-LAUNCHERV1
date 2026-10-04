package net.kdt.pojavlaunch.rkb.discord;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

/**
 * JNI bridge to libdiscord_partner_sdk.so (Discord Social SDK).
 */
public final class DiscordNative {
    private static final String TAG = "RKB-DiscordNative";
    private static boolean sLoaded = false;
    private static final Handler sMain = new Handler(Looper.getMainLooper());
    private static Runnable sCallbackLoop;

    static {
        try {
            System.loadLibrary("discord_partner_sdk");
            System.loadLibrary("rkb_discord_jni");
            sLoaded = true;
            Log.i(TAG, "Native libraries loaded");
        } catch (UnsatisfiedLinkError e) {
            sLoaded = false;
            Log.e(TAG, "Failed to load native Discord libs", e);
        }
    }

    private DiscordNative() {}

    public static boolean isLoaded() {
        return sLoaded;
    }

    public static boolean init() {
        if (!sLoaded) return false;
        try {
            boolean ok = nativeInit();
            startCallbackLoop();
            return ok;
        } catch (Throwable t) {
            Log.e(TAG, "nativeInit failed", t);
            return false;
        }
    }

    public static void shutdown() {
        stopCallbackLoop();
        if (!sLoaded) return;
        try {
            nativeShutdown();
        } catch (Throwable t) {
            Log.w(TAG, "nativeShutdown", t);
        }
    }

    public static void setPresence(String details, String state) {
        if (!sLoaded) return;
        try {
            nativeSetPresence(details != null ? details : "", state != null ? state : "RKB Launcher");
        } catch (Throwable t) {
            Log.w(TAG, "setPresence", t);
        }
    }

    public static void clearPresence() {
        if (!sLoaded) return;
        try {
            nativeClearPresence();
        } catch (Throwable ignored) {}
    }

    public static void updateToken(String token) {
        if (!sLoaded || token == null || token.isEmpty()) return;
        try {
            nativeUpdateToken(token);
        } catch (Throwable t) {
            Log.w(TAG, "updateToken", t);
        }
    }

    public static boolean isReady() {
        if (!sLoaded) return false;
        try {
            return nativeIsReady();
        } catch (Throwable t) {
            return false;
        }
    }

    private static void startCallbackLoop() {
        stopCallbackLoop();
        sCallbackLoop = new Runnable() {
            @Override
            public void run() {
                try {
                    nativeRunCallbacks();
                } catch (Throwable ignored) {}
                sMain.postDelayed(this, 500);
            }
        };
        sMain.post(sCallbackLoop);
    }

    private static void stopCallbackLoop() {
        if (sCallbackLoop != null) {
            sMain.removeCallbacks(sCallbackLoop);
            sCallbackLoop = null;
        }
    }

    private static native boolean nativeInit();
    private static native void nativeShutdown();
    private static native void nativeRunCallbacks();
    private static native void nativeSetPresence(String details, String state);
    private static native void nativeClearPresence();
    private static native void nativeUpdateToken(String token);
    private static native boolean nativeIsReady();
}
