# RKB LAUNCHER

Android Minecraft: Java Edition launcher. Built on **Zalith Launcher 2** (GPLv3,
(c) MovTery and contributors). Reference project: **PojavLauncher** (LGPLv3). The combined
work is distributed under **GPLv3**; upstream license files and notices must be kept, and
an attribution is required in the About screen.

## Status (nothing here has been compiled or run yet)
Changed so far:
- `ZalithLauncher/gradle.properties`: name/short name/version -> RKB; embedded signing passwords removed.
- `ZalithLauncher/build.gradle.kts`: applicationId `com.rkbgamerz.rkblauncher` (Kotlin/JNI package unchanged);
  bundled keystores deleted; release signing only from RKB_* env vars; debug uses the default debug key.
- Theme: new `ColorThemeType.RKB` (#05070C/#070D16/#09121E, blue #00A8FF/#1B60FF, muted #96A8BE),
  default theme, default dark mode. Files: `Theme.kt`, `ColorTheme.kt`, `NativeThemeUtils.kt`,
  `LauncherSettingsScreen.kt`, `AllSettings.kt`, `strings.xml`.
- New `ui/screens/main/RkbNavigationRail.kt` and `MainScreen.kt` wiring: left rail
  Home / Account / Mods / Control / About / Instances / Download / Setting, with selected state.
- CI: `.github/workflows/rkb-debug.yml` (old Zalith release workflows removed).
- Removed: `zalith_launcher.jks`, `zalith_launcher_debug.jks`.

Not done yet: header bar with account chip, Home banner layout, instances/download/mods card
restyle, Settings card list, replacing Zalith logo/strings/links, About credits.
Also unverified: Mods rail item opens the per-instance mod manager directly.

## Batch 2 (still uncompiled)
- Home replaced by `RkbHomeScreen` (wallpaper banner, version picker, PLAY, RAM/renderer card); Zalith `LauncherScreen` file kept but unused.
- New setting `homeWallpaper` + `RkbWallpaperCard` in Settings > Launcher (picks an image, copies it into app storage).
- Top bar: RKB title + tagline + account chip; folder/multiplayer/download/settings icons removed (the rail covers them).
- Rail made compact (36dp items, no logo text) so all 8 items fit in landscape.

## Batch 3 (still uncompiled)
- R logo (cut from user JPG) -> `res/drawable-nodpi/rkb_logo.png`; used in header and as adaptive launcher icon (background #05070C, monochrome layer removed).
- Settings: inner Zalith tab rail replaced by a card-list hub (`RkbSettingsHub.kt`); each card opens an existing Zalith settings page, with a "Settings" back bar.
- Download: inner left category rail replaced by horizontal tab chips (`RkbTabChip`).
- Header chip now shows the account skin head (`PlayerFace`) plus a wallpaper button (`rememberWallpaperPicker`).
- About credit string corrected ("Author of Zalith Launcher 2, the base of RKB LAUNCHER").
- `libdiscord_partner_sdk.so` NOT added (no JNI/Java bindings, needs Discord app id; see chat).

## Batch 4 (still uncompiled)
- Instances: `RkbInstancesScreen.kt` replaces the Zalith list layout (search, sort, loader filter, New, empty state). Still uses Zalith's `VersionItemLayout` + rename/copy/export/delete dialogs. Not present any more: game-folder switcher and "clean up game files" buttons (old `VersionsManageScreen.kt` is kept in the tree, unused).

## Batch 5: Discord Rich Presence (uncompiled)
- `libs/discord_partner_sdk.aar` (Discord Social SDK, from the user's download; Discord's terms apply), headers + prebuilt .so under `src/main/jni/rkb_discord/`.
- `rkb_discord.cpp` JNI wrapper, `utils/RkbDiscord.kt`, Settings > Launcher "Discord Rich Presence" card (Connect / Disconnect).
- Manifest redirect scheme `discord-1555829146490241095` on `com.discord.socialsdk.AuthenticationActivity`; dependency `androidx.browser:1.8.0`.
- Token is kept only in private SharedPreferences `rkb_discord`; never logged.
- Presence is set to "Playing Minecraft <version>" when PLAY is pressed. It is NOT cleared when the game process exits (not implemented).

## Batch 6 (uncompiled)
- Discord presence: cleared when the launcher goes to the background unless a game is running; "Playing" ends when the `<pkg>:game` process exits (polled every ~3s); reconnects on return using the saved login.
- Home wallpaper accepts image, animated GIF/WebP (Android 9+) and video (looping, muted, center-crop) via `RkbWallpaperView.kt`; picker is the system photo picker (images + videos).

## Batch 7: in-game R logo + Pojav layout compatibility (uncompiled)
- In-game floating ball (`GameBall.kt`): menu icon replaced with the RKB R logo inside a #00A8FF ring; tap behaviour unchanged (opens the existing in-game menu).
- `LayerController/.../pojav/PojavLayoutConverter.kt`: PojavLauncher control-layout (v2-v8) importer/exporter. No Pojav source was copied; only the file format (documented by reading Pojav's `customcontrols` package) is re-implemented.
- Import hook in `ControlManager.importControl` (Pojav JSON is detected and converted); export card `RkbPojavExportCard` in Settings > Control.
- NOT done: porting Pojav's View-based runtime (ControlLayout/ControlButton/touch handling) into the game screen. Zalith's runtime is still the one running.

## Batch 8: launcher-side idle cost (uncompiled)
- Video wallpaper releases its decoder whenever the launcher window is not visible (e.g. while the game runs).
- Discord callback pump drops from 10/s to 1/s while the launcher is in the background; game-process check is time-based (3 s).

## Batch 9 (uncompiled)
- Fixed DEBUG-ONLY signing key `ZalithLauncher/rkb-debug.jks` (public password "android"): every CI build is signed the same, so installs update in place. Not for publishing. Deliberately tracked (`.gitignore` exception). Remove it if you prefer secrets-based signing.
- versionCode = GITHUB_RUN_NUMBER on CI.
- Release workflow `rkb-release.yml` (R8-minified; signed with the same key unless RKB_KEYSTORE* env is provided). Keep rules added for RkbDiscord / Discord SDK / key tables.
- Control editor menu ball now shows the R logo too.

## Batch 10: launch speed + honest timing (uncompiled, NOT benchmarked)
- Finding (read from source): every PLAY runs a verify step that SHA-1 hashes every existing game file (client jar, libraries, all asset objects) via `compareSHA1` / `DownloadTask.existingFileValid` unless the per-version "skip integrity check" is on.
- `VerifiedHashCache`: trusts a file only if path, size, mtime and expected SHA-1 match a previous successful full verification (cache file `rkb_verified_files.tsv`). Setting `rkbFastVerify` (default on) in Settings > Game.
- `RkbLaunchTrace`: records launcher-side PLAY time (ms) plus hashing counters in `rkb_launch_times.tsv`; shown in Settings > Game. It does not time Minecraft startup or world loading.
- No JVM/GC/thread/chunk changes were made: no measurement exists to justify any.

## Batch 11 (branch feature/rkb-instance-download-horizon; uncompiled, untested)
- Instance Download hub (3 category cards: Normal Versions / Modpack / Rust Edition), landscape row + portrait column. Normal hides the Modpack tab; Modpack shows no tab row.
- Modpack browser: split layout (results left, versions of the selected pack right) reusing the existing Modrinth/CurseForge search, version list and installer.
- Rust Edition: verified compatibility panel + external-app launch of the unofficial Android port; Horizon card in Instances. See HORIZON_INTEGRATION.md.

## Batch 12: About page, complete-source ZIP (uncompiled)
- Old inherited About page removed from the UI (the file `AboutInfoScreen.kt` is kept in the tree but no longer referenced); new `RkbAboutScreen` shows logo, description, developer RKB GAMERZ, version read from the app, Java engine info (bundled runtimes and active renderer read at runtime), Rust Edition status (not integrated), the three official links in the required order, credits and the existing library/license list.
- Signing: the RKB debug key file is optional; without it Gradle falls back to Android's standard debug key.
- Rust Edition is NOT integrated: see docs/HORIZON_ENGINE_PLAN.md and docs/HORIZON_STANDALONE_TEST_PLAN.md.
