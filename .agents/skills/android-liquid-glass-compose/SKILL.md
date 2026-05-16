---
name: android-liquid-glass-compose
description: Use when working on this repository's Android Jetpack Compose Liquid Glass navigation, AGSL rendering, backdrop behavior, demo screens, screenshots, or release polish.
---

# Android Liquid Glass Compose

Use this skill for changes in `glass-nav-compose`.

## Workflow

1. Inspect the local code before changing behavior:

   ```bash
   rg -n "LiquidGlass|RuntimeShader|Backdrop|Navigation" app/src/main/java
   ```

2. Keep component boundaries clear:

   - Stateful API: `LiquidGlassBottomNavigation`.
   - Stateless API: `LiquidGlassBottomNavigationBar`.
   - Style tokens: `LiquidGlassNavigationStyle`.
   - Android 13+ AGSL surface layer: `LiquidGlassAgslOverlay.kt`.
   - Demo app usage: `ui/demo/LiquidGlassDemoScreen.kt`.

3. Preserve the renderer split:

   - Android 13+ may use `RuntimeShader`, `RenderEffect`, or backdrop lens effects.
   - Android 12 and lower must keep a non-crashing fallback with AGSL/lens disabled.
   - Do not describe backdrop capture as pure AGSL. Backdrop handles background capture and blur; AGSL handles procedural surface light.

4. Keep the visual contract:

   - Selection is flat only after it settles.
   - Selection turns glass during long press, drag, or transition.
   - Selected content must be visibly stronger than unselected content.
   - The trailing circular action must press as a circle, not as a square ripple.
   - The bar must adapt to light/dark backdrops using two stable tone buckets.

5. Verify Android work before finalizing:

   ```bash
   ./gradlew :app:compileDebugKotlin
   ```

6. For visual changes, install and refresh captures:

   ```bash
   ./gradlew :app:installDebug
   /Users/taehwankwon/Library/Android/sdk/platform-tools/adb shell am start -n app.thdev.myapplication/.MainActivity
   ```

   Store screenshots and behavior videos in `docs/assets`.

## Quality Bar

- Prefer existing Compose patterns and local style tokens over new one-off modifiers.
- Keep AGSL shader constants small and explain only non-obvious shader decisions.
- Do not add a new dependency unless the repo cannot reasonably implement the behavior with Compose, Android graphics APIs, or the existing backdrop library.
- Commit UI-visible changes with updated screenshots when possible.
