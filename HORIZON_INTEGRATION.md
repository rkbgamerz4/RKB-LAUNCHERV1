# Horizon (Rust Edition) - integration report

Sources read: https://github.com/horizon-rust/horizon (official README), the unofficial Android port
https://github.com/Rayan-XOO/horizon (README + release android-v0.2.4). Date of reading: 2026-11.

## Findings
1. Horizon = Minecraft Java 26.3 rewritten in Rust (wgpu/Vulkan). MIT, unofficial, contains no Minecraft assets.
2. OFFICIAL launcher builds: Linux x86-64, macOS (Apple Silicon; Intel later), Windows x64. No Android and no ARM64-Linux build.
   Official build needs `libasound2-dev libudev-dev` on Linux (desktop audio/input stacks, not Android).
3. Official game-file preparation (`horizon-prepare`) needs Rust 1.90+, Python 3.10+ and JDK 25, plus the user's own Java 26.3 client
   and the 26.3 server jar. It builds resources from the user's own copy; they must never be redistributed.
4. A THIRD-PARTY Android port exists: package `dev.horizonlab.horizon`, Android 8.0+, arm64, Vulkan (GLES3 fallback), touch controls.
   Stated tested device: Galaxy S25 only. ~2.2 GB RAM. Early/experimental. The APK is signed with the author's key.
   It downloads ~525 MB of game files itself, but `horizon-derived.zip` (~4 MB) must be built on a PC (Java 25 + Rust) and copied
   to `Android/data/dev.horizonlab.horizon/files/` because it needs Java tooling.
5. Existing Minecraft game files cannot simply be reused by RKB: Horizon needs its own derived resources, produced by its Java tools.

## What RKB does (and does not do)
- Does: show these facts for the current device (ABI, RAM, Android version), detect whether the unofficial app is installed
  (`<queries>` entry added), start it as a separate native app, list it as a "Horizon 26.3 / Native" card in Instances, link to
  the official site/repo and to the unofficial release page.
- Does NOT: bundle, download or silently install any Horizon binary; run Linux/desktop binaries; claim Horizon works on this phone;
  run Horizon through the Java launch pipeline.

## Blockers for a deeper native integration
- No official Android artifact to download and verify (hash/signature) from the Horizon project.
- Embedding the Rust client as a native library needs its own Android windowing/audio/asset layer (what the third-party port provides),
  i.e. a maintained Android target of Horizon itself.
- `horizon-derived.zip` cannot be produced on the phone (needs JDK 25 + Rust toolchain on a PC).
- Unverified performance on MediaTek MT6769 / Mali-G52 class GPUs.
