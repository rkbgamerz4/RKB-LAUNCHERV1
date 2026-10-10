# RKB fix patch #2 (built on your uploaded RKB-LAUNCHERV1-main__1_.zip)

STATUS: NOT compiled / NOT run. No Gradle, Android SDK or device in the authoring sandbox. XML was parsed and
Java was syntax-checked only. Build it, then send the first error list and rkb_logcat.txt (the uploaded one is 0 bytes).

## Root causes
1. Launch crash right after download: MainActivity -> LauncherProfiles.getCurrentProfile() throws
   "The current profile stopped existing" when the saved current-profile key is empty/invalid. The old version
   spinner used to persist it; nothing did after it was removed. Now persisted (commit) before launching.
2. "Only one instance": Home showed just the current profile, and nothing selected a newly created profile
   (REFRESH_VERSION_SPINNER had no listener). Now: listener in LauncherActivity + instance chooser on Home.
3. Missing Launch button: right panel used fixed heights (~520dp) that do not fit a landscape phone; LAUNCH/version
   rows were pushed off-screen. Sizes now derive from screen height; panel also scrolls as a fallback.
4. Double launch / no feedback: launch guard + progress bar overlay; UI restored on download failure.

## Changed / new (vs your upload)
LauncherActivity, ProfileEditorFragment (save double-tap guard), RkbHomeFragment, RkbUi, ModManagerFragment,
CursorStudioFragment, CursorStudioPrefs (JSON export/import), RkbSkinFragment, new rkb/skin/RkbSkinLoader + RkbSkinView,
MineEditText, styles.xml (neon preference category headers), mine_button_background / background_card (neon buttons/cards),
rkb_input_background, rkb_default_alex.png, fragment_select_auth_method.xml + fragment_local_login.xml (scrollable).

## Limitations
- rkb_default_alex.png is an ORIGINAL placeholder skin (not Mojang's artwork). Replace with the real Alex texture if you have rights/assets.
- Skin lookup uses Mojang's public session server for Microsoft accounts only; local accounts get the default skin
  (or a preview of the saved skin URL). Custom skin is not applied in multiplayer.
- ANR: no evidence available (empty logcat). Main-thread disk work removed from skin/mod loading; others unverified.
- Microsoft login WebView/browser flow untouched. Download screens restyled only through shared button/card/input styles.
- App is forced sensorLandscape in the manifest, so portrait is not applicable.

## Test checklist
1 Create 2-3 instances via New Instance -> each appears in the chooser (tap instance card), the new one is selected.
2 Restart app -> instances + selection persist. Delete via the dots menu -> list updates, one instance minimum.
3 Mod Manager with > screen height of jars -> scroll to last item, toggle ON/OFF, verify files move in mods/ and mods/disabled/.
4 Home on a small landscape phone -> LAUNCH visible; tap twice quickly -> only one launch; no account -> goes to Skin & Account.
5 Skin: Microsoft account shows its skin (offline: cached/default); switch account -> preview refreshes.
6 Cursor Studio: change style/size/opacity/color -> preview; Save; Export -> pick location; Import it back; Share.
7 Back from any secondary screen -> Home; Back on Home twice -> exits.
