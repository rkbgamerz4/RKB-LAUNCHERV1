RKB LAUNCHER - UI polish patch (based on supplied source archive)

Contains focused, low-risk changes outside the Home screen:
- Cursor Studio content now scrolls on smaller screens and uses density-independent padding; save button styling matches the RKB dark/neon-blue theme.
- Mod Manager replaces the emoji icon with a simple text badge to match the requested non-emoji style.
- Settings screen background uses the RKB dark background.
- Controls editor system bars use the RKB dark theme.

Home screen source is intentionally not included/changed, so its design remains as supplied.

Optimization scope: small UI layout improvements only. This patch does not claim to fix launcher ANR, offline-mode crashes, update behavior, or Minecraft in-game custom cursor rendering. Those need targeted diagnosis/testing.

Apply by extracting the app_pojavlauncher folder into the project root and allowing file merge/replace. Build with GitHub Actions and test before committing. This patch has not been Gradle-compiled in this environment.
