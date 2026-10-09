package net.kdt.pojavlaunch.rkb.ui;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

/** Real external links used by the RKB UI. Empty value = no real URL known, nothing is opened. */
public final class RkbLinks {
    public static final String DISCORD = "https://discord.gg/M2FskvuRJ7";
    public static final String GITHUB = "https://github.com/rkbgamerz4/RKB-LAUNCHERV1";
    /** TODO: put the real RKB GAMERZ YouTube channel URL here. Left empty on purpose (not invented). */
    public static final String YOUTUBE = "";
    /** No website URL exists in the project, so none is shown. */
    public static final String WEBSITE = "";

    private RkbLinks() {}

    public static void open(Context ctx, String url, String label) {
        if (url == null || url.isEmpty()) {
            Toast.makeText(ctx, label + " link is not configured yet", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(i);
        } catch (Exception e) {
            Toast.makeText(ctx, "No app can open " + label, Toast.LENGTH_SHORT).show();
        }
    }
}
