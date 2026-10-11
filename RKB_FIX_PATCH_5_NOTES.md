# RKB fix patch #5 - LAUNCH never starts Minecraft

STATUS: NOT compiled / NOT run. No Gradle, Android SDK, device or logcat in the authoring sandbox, and no logcat was
attached this time (only the video and the Java 25 runtime). XML/Java were syntax-checked only.
=> The fix below is derived from reading the launch code; it is NOT confirmed by a real launch. Please test and send
   rkb_launch_error.txt (see below) + `adb logcat` if it still fails.

## What the recording shows
Instance "RK BOOST" = profile version id fabric-loader-0.19.3-26.1 (a Fabric profile whose parent is Minecraft 26.1,
which needs Java 25). You installed jre25 arm64 25.0.3 in Settings > Java (shown as DEFAULT). LAUNCH then shows
"Preparing Minecraft ..." and never ends.

## Root cause (launcher side) - found in source
LAUNCH -> ExtraCore LAUNCH_GAME -> LauncherActivity -> MinecraftDownloader (background) -> ContextAwareDoneListener.onDownloadDone()
-> ProgressKeeper.waitUntilDone -> ContextExecutor.execute(). ContextExecutor starts MainActivity only if an Activity was
registered with ContextExecutor.setActivity(). The stock PojavLauncher LauncherActivity registers itself in onResume();
this launcher never did. So when the download/preparation finished, ContextExecutor fell back to the Application and only
posted a "download finished" notification. MainActivity (the real game start) was never launched, and the overlay stayed on
"Preparing Minecraft" because nothing ever dismissed it. This affects every profile (vanilla too), not only Fabric.

## Other defects fixed on the same path
- Current-profile preference (MainActivity.getCurrentProfile() throws if invalid): now committed before launch (patch 2) - kept.
- Launch tapped before the Mojang version list loaded: a Fabric profile's parent (26.1) could not be resolved. Launch now loads the list first.
- No progress UI existed (the stock ProgressLayout view is not in this UI): the overlay now mirrors the real download progress text/percent.
- Failure path: overlay is dismissed, the original exception is shown (existing error screen) and saved with device info,
  installed runtimes and account TYPE (no tokens) to  Android/data/<package>/files/rkb_launch_error.txt
- "Starting Minecraft..." is shown after files are ready (not "success"). A 25 s watchdog dismisses it with a message and a
  diagnostics entry if MainActivity was not started.
- Duplicate launches: still blocked while a launch runs (flag + ProgressKeeper check).

## Runtime you attached (checked)
jre25-arm64-20251226-release_tar.xz: Android (bionic, linker64) aarch64 OpenJDK 25.0.3, has bin/java, lib/server/libjvm.so, libjli.so,
libawt*, release file (JAVA_VERSION="25.0.3", OS_ARCH="aarch64"). That is the layout MultiRTUtils/JREUtils expect and the app
already accepted it. Not bundled into the APK (your CI only fetches Java 8/17/21); keep installing it in Settings > Java.
Not verifiable here: whether Pojav's native/GL layer and Minecraft 26.1 + Fabric run on Java 25 on your device.

## Files changed
LauncherActivity.java (only code file) + this notes file.

## What I could NOT test
Real launch, vanilla vs Fabric comparison on device, Fabric JSON/library validity, native/OpenGL start, network downloads.
If it still fails, the first useful evidence is rkb_launch_error.txt, then logcat from the moment you press LAUNCH
(filter: RKB-Launch, MinecraftDownloader, NewJREUtil, AndroidRuntime).

## Test checklist
1 Launch the (Default) 1.7.10 vanilla instance: overlay shows download %, then the game opens (this isolates launcher vs Fabric issues).
2 Launch RK BOOST (Fabric 26.1): overlay shows "Loading version list" -> download % -> "Starting Minecraft..." -> game opens.
3 Turn on airplane mode before launching a not-yet-downloaded version: overlay disappears and the error is displayed.
4 Double-tap LAUNCH: only one launch.
