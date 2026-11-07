# RKB upgrade - Stage 1: architecture findings, decisions and stage status

Branch chain (nothing touches `main`): `feature/horizon-engine` -> `feature/rkb-instances-reference`.

## Architecture (read from the code)
- **Instances** = Zalith "versions" (`VersionsManager`, `Version`): vanilla or loader-based (Fabric/Forge/NeoForge/Quilt/OptiFine...), each with
  its own game folder/settings. Select = `VersionsManager.saveVersion`. Launch = `GameLaunchFlow` -> separate `:game` process (Java pipeline).
- **Downloads** = `DownloadScreen` (+ Game / ModPack / Mod / ... screens) over `MinecraftDownloader` / modpack installers (Modrinth, CurseForge).
  A modpack install produces an ordinary instance; **no persistent "modpack instance" marker exists today.**
- **Controls** = `LayerController` (Compose layers/widgets) + `InputMap` + `CallbackBridge`. Gamepad, keyboard, touchpad and the input bridge all depend on it.
  RKB already imports/exports PojavLauncher layout files (`PojavLayoutConverter`); the Pojav *runtime* is not ported.
- **Horizon**: engine builds are only workflows so far (not run). Standalone verification is still open.

## Decisions on conflicting or unsafe requests
1. **Bundling `horizon-derived.zip` in the APK: not done.** It is derived from Mojang's game and Horizon's own docs say it must not be shared or published;
   an APK is distribution. Alternative to evaluate: in-app, one-time preparation from the user's own game files (Mojang-verified download + the exporters run
   on-device with RKB's bundled JRE 25). Feasibility is unproven.
2. **Horizon integration waits for a verified standalone build** (your previous instruction). No Rust Edition option in RKB until then.
3. **Replacing the Zalith control system** = porting Pojav's View-based runtime (about 40 classes, its touch/gesture handling, editor activity) into the Compose game screen.
   Zalith's control code is shared by gamepad/keyboard/touchpad/input bridge, so nothing is deleted first. Needs its own branch with a device test after every step.
4. **Style**: layout follows your reference; effects are restrained (flat surfaces, 1 dp borders, one blue accent) as asked in section 7.
5. **Modpack-focused instance**: needs (a) a marker saved at modpack-install time, (b) an instance screen that shows only modpack content. Not built yet.

## Stage status
| Stage | State |
| --- | --- |
| 1 plan | this document |
| 2 Horizon | blocked on standalone verification (workflows exist, not run) |
| 3 Pojav controls | layout import/export exists; runtime replacement not started |
| 4 Instances (your reference) | **built, uncompiled**: card grid, search, loader filter, create, select, PLAY, Edit, Rename/Copy/Export/Delete (existing confirmation dialogs). No Horizon card unless a Horizon app is installed; no modpack mode yet |
| 5 Downloads | waiting for your Download reference |
| 6 polish | the Download-screen bug list needs screenshots of the glitches |
| 7 CI APK | existing `rkb-debug.yml` / `rkb-release.yml` |
