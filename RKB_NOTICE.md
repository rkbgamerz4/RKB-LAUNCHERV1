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
