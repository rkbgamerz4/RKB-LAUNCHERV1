package net.kdt.pojavlaunch.rkb.skin;

import android.util.Log;

import net.kdt.pojavlaunch.value.MinecraftAccount;

/**
 * RKB Launcher — Skin URL automatic application.
 *
 * Flow required by product:
 *   User saves Skin URL on account
 *   → Launch Minecraft
 *   → Join multiplayer server
 *   → Detect successful multiplayer connection
 *   → Automatically send: /skin url <saved URL>
 *
 * Rules:
 * - Do NOT send before successful server join.
 * - Do NOT create a server plugin.
 * - Use the selected account's skinUrl only.
 * - If skinUrl is null/empty → do nothing.
 * - Account switching uses the newly selected account's URL.
 * - Launcher UI does not expose a manual "send skin command" button.
 *
 * Integration note:
 * The actual injection point must be wired into the real Minecraft client
 * connection lifecycle (after multiplayer join succeeds). This class only
 * provides the safe command builder and guards; it does not invent APIs.
 */
public final class SkinUrlHandler {
    private static final String TAG = "RKB-SkinUrl";
    public static final String COMMAND_PREFIX = "/skin url ";

    private SkinUrlHandler() {}

    /**
     * @return the full chat command, or null if the account has no usable skinUrl.
     */
    public static String buildCommand(MinecraftAccount account) {
        if (account == null) return null;
        String url = account.skinUrl;
        if (url == null) return null;
        url = url.trim();
        if (url.isEmpty()) return null;
        // Basic sanity: must look like a URL
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            Log.w(TAG, "skinUrl does not look like an http(s) URL, skipping");
            return null;
        }
        return COMMAND_PREFIX + url;
    }

    /**
     * Called by the launch / multiplayer join hook when a successful
     * multiplayer connection has been established.
     *
     * @param account currently selected account
     * @param sendChatCommand callback that actually injects the chat message
     *                        into the running Minecraft client (must be real)
     */
    public static void onMultiplayerJoined(MinecraftAccount account,
                                           ChatCommandSender sendChatCommand) {
        String cmd = buildCommand(account);
        if (cmd == null) {
            Log.d(TAG, "No skinUrl for account, nothing to send");
            return;
        }
        if (sendChatCommand == null) {
            Log.e(TAG, "No ChatCommandSender provided — cannot send skin command");
            return;
        }
        try {
            Log.i(TAG, "Sending skin command after multiplayer join");
            sendChatCommand.send(cmd);
        } catch (Exception e) {
            Log.e(TAG, "Failed to send skin command", e);
        }
    }

    /** Minimal callback interface — implement with real client chat injection. */
    public interface ChatCommandSender {
        void send(String command);
    }
}
