# Horizon 0.3.8 standalone Android - verification plan and status

Scope: ONLY the standalone Horizon app (unofficial Android port, tag `android-v0.3.8`, commit
`fc3e4d395016ae7210d1f54da5a09c0b2763b9ea`). RKB Launcher is not touched. Branch: `feature/horizon-engine`.

## Status (honest)
| Item | Status |
| --- | --- |
| Engine/APK build from source in CI | workflow written, **not run** |
| Derived pack generation (no PC) | workflow written, **not run**; helper script tested offline on synthetic data only |
| Validation with Horizon's own logic | **partly open**: CI checks zip integrity, the `horizon-resources.json` marker, size sanity and the client SHA-1 against `research-lock.json`. The installer that really consumes the pack runs inside the app, so acceptance = installing on a phone |
| Setup/launch fixes | **none made**: no failing log exists yet; fixing blind would be guessing |
| ARM64 / graphics checks | **not tested** (no device here) |
| Gameplay | **not tested** |

## What is known from the sources (no testing involved)
- Official derived-pack process (README + ANDROID.md): your own 26.3 client in a `.minecraft` dir + the 26.3 server jar ->
  `horizon-prepare --prepare --server-jar` (Rust 1.90+, Python 3.10+, JDK 25) -> `packaging/android/make_derived_pack.sh`
  -> `target/android/horizon-derived.zip` (~4 MB). The app then reads `files/horizon-derived.zip` (or `HORIZON_DERIVED_URL`).
- Minecraft files are never bundled. The pack is derived from Mojang's game and must not be shared or published.
- 0.3.8 release notes: controller/keyboard/mouse support (0.3.7), `.hnmod` Android mods (0.3.8); Microsoft sign-in needs your own
  Mojang-approved client ID (offline name works); Turnip driver is experimental and only relevant to Adreno GPUs.
- 0.2.5 notes: GPUs limited to 256 texture-array layers are supported (Mali-T760 mentioned).
- Reported tested device: Galaxy S25. Target device here: MT6769 (Helio G80-class, Mali-G52): **unverified**.

## How to run the stages (each must pass before the next)
1. **Engine**: Actions -> "Horizon Engine (arm64) - build from source" (defaults are pinned to 0.3.8). PASS = green + artifact with `horizon.apk`,
   `libhorizon.so`, `ENGINE_REPORT.md`. On failure send the failing step's log; the fix goes into the workflow (or a patch applied in CI), never silently.
2. **Derived pack** (PRIVATE repository only; the workflow refuses public repos): Actions -> "Horizon derived pack", tick the ownership box.
   PASS = green + `horizon-derived-pack-PRIVATE` + `horizon-derived-report`. Optionally add a repo secret `DERIVED_PACK_PASSPHRASE` to encrypt it
   (decrypt: `gpg -d horizon-derived.zip.gpg > horizon-derived.zip`).
3. **Install on the phone**: install `horizon.apk` (note: the CI APK is signed with a CI debug key, so it cannot update over the author's release APK; uninstall first).
   Copy `horizon-derived.zip` to `Android/data/dev.horizonlab.horizon/files/` BEFORE the first launch, then start the app and let setup download the game files (~525 MB, SHA-1 verified by the app).
4. **ARM64/graphics**: record `ro.product.cpu.abi` (must be arm64-v8a), Android version (8+), and with a Vulkan capability app the Vulkan API version,
   `maxImageArrayLayers`/texture-array limits and available RAM. Note whether the game picked Vulkan or the GLES3 fallback.
5. **Gameplay checklist** (each a pass/fail with a screenshot or log): reaches title screen; creates a world; walks, jumps, breaks/places blocks;
   opens inventory; stays up 10 minutes; background/foreground; quit and relaunch; worlds persist. Record FPS (in-game F3), peak RAM, thermal throttling.
6. **Collect on failure**: the setup screen's "Copy error text", the game folder logs, `spikes.txt` if enabled, the Android exit history
   (Settings -> diagnostics), device model/SoC/GPU, exact app version and APK SHA-256.

## Acceptance for "standalone verified"
All of 1-5 pass on at least one real device, with the reports attached. Until then Horizon is **not** verified and RKB integration stays out of scope.
