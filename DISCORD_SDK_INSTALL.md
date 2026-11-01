# Discord Social SDK - build fix for `Unresolved reference 'discord'`

## Cause (verified)
The official `discord_partner_sdk.aar` keeps its Java classes in `libs/discord_partner_sdk.jar`
and has **no `classes.jar`**. Android Gradle Plugin only puts an AAR's `classes.jar` on the
compile classpath (the other AARs in `ZalithLauncher/libs/`, e.g. SDL, NG-GL4ES, all have one),
so `import com.discord.socialsdk.*` could not resolve.

## What was verified in the real SDK (version from your download: DiscordSocialSdk 1.10.19337)
- Package: `com.discord.socialsdk` (the imports in `RkbDiscord.kt` are correct, nothing to change).
- `DiscordSocialSdkInit` is present, with `public static void setEngineActivity(android.app.Activity)`,
  `getEngineActivity()` and `getApplicationContext()`.
- `AuthenticationActivity` is present (OAuth redirect handler).
- Classes are Java 17 bytecode (class version 61): compatible with the project's JVM target 17.
- Native libs for arm64-v8a, armeabi-v7a, x86, x86_64 are unchanged inside the AAR.

## What changed
Only the packaging of the AAR: the real, unmodified `libs/discord_partner_sdk.jar` was moved to
`classes.jar` (and removed from `libs/` so classes are not duplicated), an empty `R.txt` was added
(the SDK has no resources), and the duplicate `prefab/` copy was dropped (headers and .so used by
the C++ wrapper already live in `src/main/jni/rkb_discord/`). `AndroidManifest.xml`, `proguard.txt`,
`jni/**` and `libs/libwebrtc.jar` are byte-for-byte from the official file.
No SDK class was written or faked.

## Install
1. Copy the ZIP's `ZalithLauncher/libs/discord_partner_sdk.aar` over the existing file at the same path.
2. Nothing else. No Gradle change is needed: `implementation(fileTree(... "*.aar" ...))`,
   `androidx.browser:androidx.browser:1.8.0` and `pickFirsts += "**/libdiscord_partner_sdk.so"`
   are already in `ZalithLauncher/build.gradle.kts` from the previous Discord batch.
3. Build: `./gradlew :ZalithLauncher:assembleDebug -Darch=arm64`

## If you still see errors
- Make sure the old AAR is really replaced (new file is about 14 MB, old one about 29 MB).
- Send the next error line; the C++ part (`src/main/jni/rkb_discord/`) has not been compiled yet.

## Official source / license
Download: https://discord.com/developers/applications -> your application -> Social SDK -> Downloads.
Use of the SDK is governed by Discord's Developer Terms and the SDK license notices shipped in the SDK zip
(`License-Notices.txt`). This ZIP only repackages the file you supplied.
