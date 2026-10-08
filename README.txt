RKB Launcher UI/Fix Patch

Files:
- RkbHomeFragment.java: responsive RKB UI, working navigation, Discord invite, skin/cape URL dialog, existing launch system.
- ModManagerFragment.java: RKB-themed mod card icon without emoji.
- CursorStudioFragment.java: density-correct RKB layout spacing.
- MinecraftAccount.java: stores capeUrl alongside the existing skinUrl.
- build-apk.yml: builds an installable signed debug APK instead of release-unsigned APK.

Discord invite configured in the RKB header:
https://discord.gg/M2FskvuRJ7

Important:
- Skin URL uses the existing SkinUrlHandler system already present in the source.
- Cape URL is persisted on the selected account. This source tree does not contain a real custom-cape renderer/injection API, so the patch does not fake one; a real cape renderer must consume capeUrl.
- Minecraft FPS is not artificially capped or altered by this UI patch. The launcher UI avoids heavy images/animations and uses existing launcher performance settings.
