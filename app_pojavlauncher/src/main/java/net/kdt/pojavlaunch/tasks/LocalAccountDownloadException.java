package net.kdt.pojavlaunch.tasks;

/**
 * Thrown when game files must be downloaded (or repaired) but the selected account is a Local
 * (offline) account. Downloading Minecraft from Mojang's servers is only allowed for a Microsoft
 * account, which is a deliberate PojavLauncher rule that this launcher keeps. Local accounts can
 * still play any version that is already fully installed.
 *
 * It is its own type so the UI can explain the situation and offer to sign in, instead of showing
 * a generic stack trace.
 */
public class LocalAccountDownloadException extends RuntimeException {
    public final String firstFileName;
    public final int missingCount;

    public LocalAccountDownloadException(String firstFileName, int missingCount) {
        super("This is a Local (offline) account, and Minecraft files must be downloaded with a Microsoft "
                + "account. " + (missingCount > 0 ? missingCount + " required file(s) are missing" : "A required file is missing or damaged")
                + " (first: " + firstFileName + "). Sign in with a Microsoft account that owns Minecraft once "
                + "to download this version; afterwards it can be played offline.");
        this.firstFileName = firstFileName;
        this.missingCount = missingCount;
    }
}
