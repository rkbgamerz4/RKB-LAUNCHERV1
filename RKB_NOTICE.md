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
