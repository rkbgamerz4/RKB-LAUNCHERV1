# RKB patch #7 - animations (from the video) + settings/runtime (from the CS ZIP)

BUILD/APK: NOT built. The authoring sandbox has no Gradle/Android SDK/network, so there is NO APK and nothing here was run on a device.
Checked: all XML parses; all changed Java passes a syntax-only compile (framework classes unresolved, no project-level symbol errors found).
Not tested: animations on a device, Minecraft launch (vanilla or Fabric), runtime downloads, settings persistence.

## About "get runtime on the ZIP"
CS-LAUNCHER-PLUS-1_2_2.zip is byte-identical to the earlier CS zip and contains NO runtime binaries (no assets/components/jre*). What it has
is the runtime DOWNLOAD code. That was integrated in patch 6 (Settings > Java > Java Runtimes download rows + auto-download on launch) and now also
as the first-run "Java for Minecraft" picker (like the video, 0:07-0:13): Java 8 / 17 / 21 / 25, 17 + 21 pre-ticked ("recommended" as in the video).
Only versions that are neither installed nor bundled in the APK are offered; install results are reported per version (no fake success).
Download source (unverified from here, no network): github.com/AngelAuraMC/angelauramc-openjdk-build releases, jre<N>-android-<arch>.tar.xz.
Sizes shown in the video (~42 MB etc.) are NOT shown because I cannot verify them.

## Video -> RKB animation checklist (timings measured at 1 fps and 10 fps; +-100 ms, ASSUMPTIONS marked)
| Video behaviour | RKB implementation |
|---|---|
| Screen change = cross-fade ~200 ms (58.2-58.4 s) | FragmentTransaction rkb_fade_in 220 ms / rkb_fade_out 140 ms (LauncherActivity.navigate); skipped when animations are off |
| New screen's cards rise in with a stagger (58.4-59.0 s) | RkbMotion.revealScreen: title block, then content (+60 ms), fade + slide-up, emphasized-decelerate curve; every RKB screen via RkbUi.scaffold |
| Home: header/sidebar/panels appear in order | Home entrance: sidebar from left, instance list rises, player panel from right (once per screen creation, not on every refresh) |
| Dialog pop-in ~220 ms, dismiss ~150 ms (28.1-30.5 s) | res/anim/rkb_dialog_in/out via the RkbAlertDialog window-animation style (all themed AlertDialogs). Not switchable by the in-app toggle (system animation scale still applies) |
| Buttons/cards visibly press (e.g. 30.0-30.3 s) | RkbMotion.press: 96% squash on touch, overshoot spring back, on buttons, sidebar, Launch, New Instance, account, instance cards |
| Player skin hovers (assumption: idle float) | RkbMotion.floatIdle, 3dp, 2.6 s loop; OFF by default (Settings > RKB interface > Idle motion), cancels on view detach |
| "Animation Amplitude" slider in Advanced (121-124 s) | rkb_anim_amplitude 1-10 (5 = reference) scales distance and duration; rkb_anim_enabled master switch; also honours Android's animator scale = 0 |
Not reproduced: the splash logo animation, the tutorial spotlight tour, jelly-bounce hero animations, shimmer/count-up effects, list-scroll physics
(standard RecyclerView scrolling is used).

## ZIP -> RKB settings checklist
| CS feature | Result in RKB |
|---|---|
| Animation Amplitude | DONE, real (Settings > RKB interface), drives RkbMotion |
| Animations on/off | DONE, real |
| Quick Device Optimizer (4/6/8 GB presets) | DONE: Settings > Video; writes only existing prefs allocation / resolutionRatio / sustainedPerformance, clamps RAM to the device, shows exact values first. Preset numbers are my own (1536/2048/3072 MB, 75/90/100 %), CS also changes renderer/FSR which RKB lacks |
| Java runtime picker + download | DONE (see above) |
| Clear shader & temporary caches | DONE: deletes the app cache directory contents, reports freed bytes, refuses while a download runs; worlds/mods/accounts untouched |
| Reset launcher settings | DONE: clears launcher preferences, keeps the selected instance; the account is stored separately and is kept; screen recreated |
| Check libraries after downloading | ALREADY in RKB (checkLibraries) |
| Download source / verify manifest / alternate surface / VSync / sustained performance / RAM / resolution | ALREADY in RKB (same Pojav keys) |
| Custom cursor | ALREADY covered by RKB Cursor Studio |
| Enable Game Sound / master volume, Allow microphone | Voice screen exists (patch 4); sound volume NOT integrated (CS code is in its own sound player) |
| ANGLE backend, multidraw emulation, FSR upscaling, GL 4.3 extensions, GLSL cache, error filtering, F3 GPU tweaks | NOT integrated: they configure CS's MobileGlues renderer library, which RKB does not ship. Adding switches would be fake |
| Force touch controller, Touch-controller vibrate | NOT integrated: RKB has no TouchController mod support code |
| Always grab mouse, gamepad passthrough | NOT integrated: needs changes inside RKB's pointer-capture/native input that I cannot verify here |
| Settings search, section hub redesign, "Pro" tier | NOT done (hub redesign is UI-only; Pro is CS's paid feature) |
| Skin Studio, Server Hub, Cursor Studio, control editor, tutorial | NOT ported (large CS-specific features; RKB has its own Skin & Account and Cursor Studio) |
Licensing: CS Launcher is GPLv3. No CS source was copied; the above are independent implementations of the observed behaviour.

## Files changed
New: RkbMotion.java, RkbRuntimeSetup.java, LauncherPreferenceRkbFragment.java, pref_rkb.xml, res/anim/rkb_fade_in|fade_out|dialog_in|dialog_out.xml
Edited: LauncherActivity, RkbUi, RkbHomeFragment, LauncherPreferenceVideoFragment, pref_main.xml, pref_video.xml, styles.xml, rkb_strings.xml

## Test checklist
1 Navigate Home <-> Cursor/Mods/Settings: ~200 ms cross-fade, content rises in. Turn off "Interface animations": instant switches, no motion.
2 Amplitude 1 vs 10: noticeably smaller/larger travel. Kill the app, reopen: both values persist.
3 Settings > Video > Quick device optimizer > pick a preset: RAM (Settings > Java), resolution and sustained performance change.
4 Fresh install, online: "Java for Minecraft" appears once; install 17+21; result dialog lists each version honestly. Settings > RKB interface > Java runtime setup reopens it.
5 Clear temporary caches reports bytes; Reset launcher settings keeps your account and selected instance.
