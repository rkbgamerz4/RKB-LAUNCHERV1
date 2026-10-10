# RKB fix patch #3 - login navigation, settings, download screen

STATUS: NOT compiled / NOT run (no Gradle/Android SDK in the authoring sandbox). XML parsed OK, Java syntax-checked only.
I studied your screen recording: it showed (1) the stock PojavLauncher menu after local login, (2) Settings showing only ~3 rows,
(3) sidebar with repeated/clipped icons, (4) Home header buttons overlapping the logo.

## Root cause: login -> default PojavLauncher Home
1. LocalLoginFragment ended with Tools.swapFragment(MainMenuFragment) = the stock PojavLauncher menu (WIKI/DISCORD/PLAY bar).
2. Worse, nothing in the project consumed ExtraConstants.MOJANG_LOGIN_TODO (local) or MICROSOFT_LOGIN_TODO (Microsoft redirect),
   so no account was ever created/refreshed after login. LauncherActivity now registers both listeners
   (local: saves the account; Microsoft: existing MicrosoftBackgroundLogin), selects the account, and rebuilds the visible RKB screen.
3. Expired Microsoft sessions are refreshed with the stored refresh token before launch; if that fails (offline) you get a clear message.

## Files - navigation / login
LauncherActivity, Tools (swapFragment: no duplicate screens, no state-saved crash), LocalLoginFragment, SelectAuthFragment,
RkbNav (openAccount), RkbHomeFragment (refreshAccount, header fit), RkbUi (account chip, compact mode).

## Files - Settings
LauncherPreferenceFragment (real preference rows styled as cards, neon category headers; keys/values untouched),
styles.xml (RkbAlertDialog theme for preference dialogs/lists), rkb_dialog_bg.xml, RkbUi (shorter header/title on short screens
so the list gets ~50dp more height).

## Files - login / download restyle
fragment_select_auth_method.xml (two-pane "Who is playing?" with option cards), fragment_profile_type.xml (all ids kept;
the inner 0dp-high layout inside the ScrollView was why loaders below the fold could not be reached), rkb_item_card.xml, rkb_strings.xml,
7 custom vector nav icons (ic_rkb_nav_*), colors.xml (neon blue now exactly #00A8FF).

## Preserved
All preference keys/values and their handlers, account json files, MicrosoftBackgroundLogin, launch flow, profile format.

## Limits
- NeoForge: the project has no NeoForge installer fragment (only Fabric, Quilt, Forge, OptiFine, BTA, modpacks), so none is shown.
- Microsoft WebView page itself is Microsoft's page and cannot be themed.
- Other loader sub-screens (Fabric/Forge version lists, mod search) only get the shared button/card/input styling.
- Untested on device: the whole login -> Home -> Settings -> download -> Back sequence.
