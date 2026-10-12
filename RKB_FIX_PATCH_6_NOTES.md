# RKB fix patch #6 - download error, runtime download, downloader hardening

STATUS: NOT built with Gradle / NOT run on a device (no SDK/network in the authoring sandbox). XML/Java syntax-checked only.
One piece WAS executed for real: the new HTTP download logic was run against a local simulated server (see "Tested").

## Root cause of "Download failed. Please make sure you are logged in with a Microsoft Account."
It is not a network or SHA-1 failure. MinecraftDownloader.DownloaderTask.downloadFile() throws that RuntimeException
immediately when the selected account is a LOCAL account (your Home screen shows RKB_GAMERZ - LOCAL), before any request is
made. verifyFileSha1() just calls downloadFile() when a file is missing or its hash differs, which is why both frames appear.
Fabric 26.1 was not fully installed, so the first missing file triggered it. PojavLauncher's own string says the same
("...trying to download Minecraft from a Local account"). I KEPT this rule (downloading needs a Microsoft account that owns the game;
installed versions still play offline) but made it accurate:
- new LocalAccountDownloadException (counts the missing files, names the first one);
- pre-flight check before any thread pool starts, optional files are skipped instead of failing;
- Launch shows "Microsoft account needed" with a "Sign in with Microsoft" button instead of a raw stack trace.
To actually get the files you must sign in once with a Microsoft account that owns Minecraft and press LAUNCH.

## Real downloader fixes (apply to every download, Microsoft accounts included) - DownloadUtils.downloadFileMonitored
- Checks HTTP status; 404/410 still throw FileNotFoundException (mirror fallback relies on it); other codes -> HttpStatusException with the code and URL.
- Follows redirects itself, including http->https (HttpURLConnection does not; the redirect page used to be saved as the file and then failed SHA-1).
- Writes to <file>.part, checks received size vs Content-Length, moves into place only when complete; partial files removed on failure.
- Retries transient failures (timeouts, resets, truncated, 5xx, 408, 429) 3x with back-off; no retry for 403 etc.; connection always closed.
- User-Agent + Accept-Encoding: identity (so Content-Length is trustworthy).
- SHA-1 is NOT disabled. On mismatch the real expected/actual hashes are logged; two identical bad hashes in a row stop early with a clear message;
  files that fail verification are deleted.
- Download pool 4 -> min(8, max(4, cores)) threads. Native libraries are no longer re-extracted on every launch when nothing changed
  (marker file, only written after a complete extraction).

## Java runtime download restored (NewJREUtil, MultiRTUtils, RTRecyclerViewAdapter, strings)
- The runtime manager code was intact; what was missing is downloading. Added NewJREUtil.ExternalRuntime (Java 8/17/21/25), download + install through the
  existing MultiRTUtils store (same folder, same validation), source = public AngelAuraMC OpenJDK Android builds
  https://github.com/AngelAuraMC/angelauramc-openjdk-build/releases/download/download_jre<N>/jre<N>-android-<arch>.tar.xz  (URL pattern taken from the CS ZIP).
  I could not verify those URLs from here (no network).
- Settings > Java > Java Runtimes now lists not-installed versions with a Download button (progress %, error toast, no double taps).
- Launch auto-downloads the needed Java (e.g. 25 for Minecraft 26.1) when none is installed or the APK lacks the bundled one, with progress in the launch overlay.
- You can still install your jre25-arm64 .tar.xz manually; nothing was removed.

## CS Launcher reference - what was and was not done (honest scope)
- CS Launcher Plus is a 554-file GPLv3 fork. RKB's base is LGPLv3 PojavLauncher. Copying CS source into RKB would make the combined work GPLv3
  and needs attribution, so I did NOT copy any CS classes/resources. I studied it and re-implemented only the compatible IDEAS in my own code
  (runtime download source/flow, native-extraction caching, bigger download pool).
- NOT ported: CS settings "console" redesign, modpack storefront, drawer/quick-settings animations, CsPickerSheet, ely.by login, FPS-control editor, 3D head renderer.
  Each is a large UI project on top of CS-specific classes. Tell me which ones you want next and I will build them in RKB's own architecture.
- CS's "Download Console"/LaunchTracker/DownloadControl are CS-specific and not used.

## Files changed
LauncherActivity, Tools (+isOnline, +getSHA1), NewJREUtil, MultiRTUtils, RTRecyclerViewAdapter, DownloadUtils, MinecraftDownloader,
LocalAccountDownloadException (new), rkb_strings.xml.

## Tested
- DOWNLOAD LOGIC (extracted code, local server): 200 OK -> file written, no .part left; 302 redirect -> OK; 503,503,200 -> OK after retries;
  404 -> FileNotFoundException, no file; 403 -> fails without retry, no files; truncated body -> 3 attempts then fails, no truncated file left. PASS.
- Everything else: NOT TESTED (no Gradle/SDK/device). Real launch of vanilla and Fabric: NOT TESTED.

## Test checklist
1 Local account + Fabric 26.1 not installed -> "Microsoft account needed" dialog (not a stack trace). Sign in with Microsoft -> LAUNCH downloads.
2 Settings > Java > Java Runtimes -> "Java 8/17/21/25 - Download" rows; download one; it appears as installed; set default.
3 Remove all runtimes except none usable, launch Minecraft 26.1 -> overlay shows runtime download, then game files.
4 Turn on airplane mode mid-download -> clear error, no truncated files; retry works.
