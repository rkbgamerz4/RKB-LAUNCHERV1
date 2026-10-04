# RKB Launcher

**RKB GAMERZ — RKB Launcher**

Independent Android Minecraft: Java Edition launcher built on the PojavLauncher open-source foundation (LGPL-3.0).

## Identity

| Item | Value |
|------|--------|
| Display name | RKB Launcher |
| Brand | RKB GAMERZ |
| applicationId | `com.rkbgamerz.rkblauncher` |
| Java namespace (source) | `net.kdt.pojavlaunch` (preserved for core stability) |
| New RKB packages | `net.kdt.pojavlaunch.rkb.*` |
| Storage root | `/games/RKBLauncher` (or scoped equivalent) |
| Theme | Dark AMOLED + neon blue accents |

## Foundation

This project is a **direct fork/evolution of the provided PojavLauncher-3_openjdk source**.

- **No Zalith Launcher code** was imported or referenced.
- Core systems (runtime, renderer, launch pipeline, multiplayer, input, downloads) are preserved.
- RKB features are added as modular extensions.

## Implemented in this tree

### PHASE 2 — RKB Foundation
- Application branding (name, strings, crash reports)
- applicationId → `com.rkbgamerz.rkblauncher`
- Storage path → `games/RKBLauncher`
- Dark AMOLED + neon blue color palette
- Root project name → RKBLauncher

### PHASE 3 — Instance / Mod architecture (foundation)
- `rkb.mods.ModInfo` — mod metadata model
- `rkb.mods.ModManager` — enable/disable without deletion
  - Enabled: `<gameDir>/mods/*.jar`
  - Disabled: `<gameDir>/mods/disabled/*.jar`
  - Toggle = rename/move (Android-friendly)
  - enableAll / disableAll / delete / list

### PHASE 5 — Skin URL (foundation)
- `MinecraftAccount.skinUrl` field added and persisted via existing JSON save/load
- `rkb.skin.SkinUrlHandler` — safe command builder + post-join callback contract
  - Builds `/skin url <URL>` only when skinUrl is non-empty and looks like http(s)
  - `onMultiplayerJoined(account, sender)` — call only after real multiplayer join succeeds

## Still required for full product (next implementation steps)

1. **Wire SkinUrlHandler** into the real Minecraft client multiplayer-join lifecycle (native/Java bridge). Do not send the command early.
2. **Mod Manager UI** fragment (list, search, enable/disable toggles, enable-all/disable-all, open folder).
3. **RKB Home navigation** — replace MainMenuFragment with sections: HOME / VERSIONS / DOWNLOADS / SETTINGS / ACCOUNT (landscape-first, AMOLED).
4. **Instance model** — extend `MinecraftProfile` with explicit loader + mod list metadata if needed.
5. **Download categories UI** for mods / modpacks / resource packs / shader packs / worlds (reuse existing downloaders).
6. **Account UI** field to edit/save `skinUrl`.
7. Full Android SDK + NDK build environment to produce Debug/Release APKs.

## License

Original PojavLauncher code remains under **GNU LGPL-3.0**.  
Preserve LICENSE and required attribution.  
New RKB-specific files are provided under the same license terms for compatibility.

## Build notes

This environment does not contain the Android SDK / NDK.  
To build on a development machine:

```bash
# Install Android SDK 34, NDK 25.2.9519653, JDK 17 or 21
./gradlew :app_pojavlauncher:assembleDebug
./gradlew :app_pojavlauncher:assembleRelease
```

Ensure `local.properties` points at your SDK.

## Critical rules followed

- Pojav core left intact
- No Zalith code
- No fake runtime / renderer / multiplayer
- No hard-coded versions, Java versions, servers, or Skin URLs
- DISABLE ≠ DELETE for mods
- Skin command only after successful multiplayer join (handler enforces the contract)
