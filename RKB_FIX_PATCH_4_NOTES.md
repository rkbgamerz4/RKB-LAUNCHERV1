# RKB fix patch #4 - compact Home, instance list, runtime, Voice

STATUS: NOT compiled / NOT run (no Gradle, Android SDK or device in the authoring sandbox).
XML + workflow YAML parsed OK; Java syntax-checked only. Build it and send the first error list.

## 1-3 Home / instances
- Center of Home (previously an empty spacer) is now a scrollable grid of ALL instances read from launcher_profiles.json
  (LauncherProfiles = the real Pojav store). Columns adapt to width. Tap = select (writes the same current-profile
  preference the Launch flow reads, so Launch starts exactly the selected instance). Dots menu: Select / Edit / Delete.
- Edit selects that instance then opens Pojav's ProfileEditorFragment (loads the instance's real settings, saves to the same key).
- Delete asks for confirmation, removes only the profile entry (worlds/mods on disk kept), refuses to delete the last instance.
- List is rebuilt whenever Home is recreated (after create / edit / delete / select / login).
- Compaction: account card 70->54/62dp, avatar + YouTube/Discord pills smaller, bottom cards 82->54/64dp, tighter paddings on short screens.
  App is forced sensorLandscape in the manifest, so portrait is not applicable; widths from phone to tablet are handled by the grid.

## 4 Java runtime - what the investigation found
No .git folder was in the upload, so there is no diff history to inspect. From the source:
- Runtime CODE is intact: Settings > Java > "Install Java runtime" (MultiRTConfigDialog, install from .tar.xz / select / delete),
  MultiRTUtils, NewJREUtil (auto-installs bundled Java 17/21), AsyncAssetManager.unpackRuntime (Java 8). Nothing was removed.
- The runtime FILES are not in the repo on purpose: assets/components/jre, jre-new, jre-21 are .gitignored and are fetched by CI from
  PojavLauncherTeam/android-openjdk-build-multiarch. In .github/workflows/build-rkb.yml those 3 downloads are `continue-on-error: true`,
  so if the upstream artifact is expired/unavailable the APK is silently built WITHOUT any bundled Java. That is the most likely cause.
- Fixes: (a) new CI step "Verify bundled Java runtimes" prints OK/ERROR per runtime and surfaces the problem in the Actions log;
  (b) Settings > Java now shows the real state: installed runtimes and which Java versions are NOT bundled in this APK.
- Still needed from you if the CI log says missing: working jre8/jre17/jre21 artifacts (files: jre/version, universal.tar.xz,
  bin-<arch>.tar.xz per folder) or install a .tar.xz runtime manually in the app. I did not invent runtime entries.

## 5 Voice
New Settings > Voice (LauncherPreferenceVoiceFragment, pref_voice.xml, ic_rkb_voice, strings): ON/OFF (voice_input_enabled),
permission status + request via the official API, app-settings shortcut when permanently denied, language (system/English/Bangla),
live microphone level test (AudioRecord, always released), speech-recognition test (Android RecognizerIntent, uses the language).
Turning it off or leaving the screen stops capture. RECORD_AUDIO was already in the manifest. In-game voice chat does NOT exist in this
launcher; the screen says so and nothing pretends otherwise. Bangla recognition depends on the device's speech service.

## Files changed (vs patch 3)
RkbHomeFragment.java, LauncherPreferenceJavaFragment.java, LauncherPreferenceVoiceFragment.java (new), pref_voice.xml (new),
pref_main.xml (+Voice entry), ic_rkb_voice.xml (new), rkb_strings.xml, build-rkb.yml.

## Test checklist
1 Create 3 instances -> all appear in the Home grid; restart app -> still there; tap one -> highlighted, Launch uses it.
2 Dots > Edit -> editor shows that instance's version/dir/args; change name, save -> list updates. Dots > Delete -> confirm -> removed.
3 Settings > Java: summary lists installed runtimes and what is missing from the APK.
4 Settings > Voice: switch on -> permission prompt; deny twice -> status says open settings; test mic shows level; switch off stops it.
