# Horizon Engine integration plan (Route 2)

Branch: `feature/horizon-engine` (separate from main; no change to existing RKB UI).

## Verified facts (read from the sources, 2026-11)
Upstream: https://github.com/horizon-rust/horizon (MIT, official: Linux x86-64 / macOS / Windows x64 only).
Android port (unofficial, MIT, fork of upstream): https://github.com/Rayan-XOO/horizon branch `android-port`.
- Rust workspace, edition 2024, rust-version 1.85, ~30 crates. `[patch.crates-io]` uses vendored `third_party/wgpu-hal` and `third_party/winit`.
- Android build: `crates/horizon-android` builds `libhorizon.so` (cdylib) with `cargo ndk` (API 26, `arm64-v8a`), entry
  `crates/horizon-client/src/android_main.rs`; wgpu (Vulkan, GLES 3 fallback); audio via cpal/AAudio; no Gradle.
- Build script: `packaging/android/build_apk.sh` (aapt2, zipalign, debug-key signing). `PROFILE=release` for the fast game.
- Toolchain from ANDROID.md: Rust target `aarch64-linux-android`, `cargo-ndk`, platform 34, build-tools 34.0.0, NDK 27.2.12479018, JDK 21 `javac` (JDK 25 output breaks d8 34).
- Java part: `HorizonActivity` (extends NativeActivity; soft keyboard via JNI) and `SetupActivity` (first-run download, Rust).
  The Rust JNI symbols are named after the Java package `dev.horizonlab.horizon`; a host app must provide classes with exactly those names.
- Game home = app external `files/` dir (`HORIZON_HOME`); resources in `files/resources`; saves, logs, resourcepacks, shaderpacks inside it.
- Game files: Horizon never ships Minecraft files. The app can download ~525 MB (SHA-1 verified) itself, but a small "derived pack"
  (`horizon-derived.zip`, ~126 MB unpacked / 4 MB zipped) is produced by Horizon's Java exporters (`horizon-prepare`, JDK 25 + Rust on a PC)
  from the user's own game files and must not be redistributed.
- Reported tested device: Galaxy S25 only. ~2.2 GB RAM. Early/experimental. MT6769/Mali is unverified.

## Stages (each stage must build AND be checked before the next one starts)
1. **Engine build (this workflow)** - `horizon-engine-build.yml` builds `libhorizon.so` + APK from a pinned source SHA and writes
   `ENGINE_REPORT.md` (manifest, activities, JNI exports, needed libs, class list, hashes). PASS = workflow green, artifact downloaded.
2. **Embed the engine in RKB** - decide from the report: `libhorizon.so` as arm64 jni lib, the exact `dev.horizonlab.horizon` Java classes,
   manifest entries (NativeActivity/HorizonActivity/SetupActivity in a separate process), engine SHA-256 pinned. PASS = RKB builds and installs; a debug screen can start the engine to its first screen.
3. **Profiles + launch** - Horizon profile (name, resolution scale, own saves/options/resourcepacks/shaders dirs under RKB storage), PLAY starts the native activity (not the Java pipeline).
4. **Game files** - download (SHA-1 verified) and derived-pack preparation. Open research item: run Horizon's Java exporters on-device with RKB's bundled JRE 25 (needs the exporter jars/orchestration from the repo, not yet inspected).
5. **Polish** - controls, logs, error handling, progress UI, licenses (MIT notices for Horizon, the port author and THIRD_PARTY_NOTICES).

## Risks
- Build time/memory on a hosted runner (thin LTO, `codegen-units = 1`, ~30 crates).
- The port may change quickly (many releases per week); pin an exact SHA.
- The unofficial port is a single-author project with an early status; RKB would depend on it.
- Performance on the target phone (MT6769) is unknown; ~2.2 GB RAM.
