# RKB UI + navigation patch

NOT compiled: the authoring sandbox had no Gradle / Android SDK / network. Build it yourself
(`./gradlew :app_pojavlauncher:assembleDebug` or your GitHub Actions workflow) and send me the first error list.

## Apply
Unzip over the project root (paths are project-relative), then:

    git checkout -b rkb-ui-redesign
    git add -A
    git commit -m "RKB UI: working sidebar navigation, launch listener, redesigned secondary screens"
    git push -u origin rkb-ui-redesign

## Root causes fixed
1. Sidebar icons had no click listeners (RkbHomeFragment.addNav).
2. LauncherActivity never loaded a layout containing R.id.container_fragment, so Tools.swapFragment()
   (Mod Manager from MainMenu, login, profile editor) threw "No view found".
3. Nothing listened to ExtraConstants.LAUNCH_GAME, so the Launch button did nothing.
   ExtraCore holds listeners weakly, so the new listener is a field of LauncherActivity.
4. Tools.backToMainMenu() popped a "ROOT" back-stack entry that was never created.
5. Back from a screen could leave the app; now Back -> Home, and Home needs a double press to exit.
6. Home: YouTube/Discord/account/version/New Instance were decoration; Home showed hardcoded
   "RKB_GAMERZ / LOCAL / Minecraft 1.21.1". They now use the real account / profile.

## Needs your input
- YouTube URL: set RkbLinks.YOUTUBE (no URL exists in the project, so none was invented).
- Website: RkbLinks.WEBSITE left empty on purpose (hidden on About).

## Known limitations
- Controls opens the existing CustomControlsActivity unchanged (not restyled).
- Settings hosts the existing preference screens (same prefs); only the frame is RKB-styled.
- Cape URL: MinecraftAccount has no cape field, so it is shown as unsupported.
- Skin URL is saved on the account; SkinUrlHandler's /skin command is still not hooked into the game chat.
- Cursor Studio changes the on-screen virtual mouse (Touchpad) only, applied when a game starts.
- Not investigated yet: "isn't responding" ANR, offline-launch crash, update-without-uninstall
  (the last one is likely signing: debug.keystore / signing configs). Need logcat / the signing details.
