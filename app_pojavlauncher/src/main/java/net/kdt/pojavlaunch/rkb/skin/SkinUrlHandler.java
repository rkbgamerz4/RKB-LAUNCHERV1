package net.kdt.pojavlaunch.rkb.skin;

import android.util.Log;

import net.kdt.pojavlaunch.authenticator.accounts.Account;

/**
 * Safe skin-command helper.
 *
 * This project branch's {@link Account} model does not persist a skin URL, so there is no real
 * command to send after joining a multiplayer server. The hook is intentionally a no-op rather
 * than inventing a setting or injecting an unimplemented chat command.
 */
public final class SkinUrlHandler {
    private static final String TAG = "RKB-SkinUrl";
    public static final String COMMAND_PREFIX = "/skin url ";

    private SkinUrlHandler() {}

    /**
     * @return always null in this branch: the account model has no saved skin URL.
     */
    public static String buildCommand(Account account) {
        Log.d(TAG, "Account model has no skinUrl; nothing to send");
        return null;
    }

    /**
     * Called by a real multiplayer-join hook. Left as a guarded no-op until an account model with
     * a persisted skin URL exists in this branch.
     */
    public static void onMultiplayerJoined(Account account, ChatCommandSender sendChatCommand) {
        String cmd = buildCommand(account);
        if (cmd == null) {
            Log.d(TAG, "No skin URL for account, nothing to send");
            return;
        }
        if (sendChatCommand == null) {
            Log.e(TAG, "No ChatCommandSender provided; cannot send skin command");
            return;
        }
        try {
            Log.i(TAG, "Sending skin command after multiplayer join");
            sendChatCommand.send(cmd);
        } catch (Exception e) {
            Log.e(TAG, "Failed to send skin command", e);
        }
    }

    /** Minimal callback interface for a real client chat-injection implementation. */
    public interface ChatCommandSender {
        void send(String command);
    }
}
