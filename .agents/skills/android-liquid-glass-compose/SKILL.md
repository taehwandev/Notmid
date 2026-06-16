---
name: android-liquid-glass-compose
description: Use when working on this repository's Android Jetpack Compose Liquid Glass navigation, AGSL rendering, backdrop behavior, notmid shell screens, screenshots, or release polish.
---

# Android Liquid Glass Compose

Use this skill for Liquid Glass UI-specific changes in the `notmid` repository and shell. For module boundaries, build-logic, design-system placement, data/domain layers, or broad refactors, start with `.agents/skills/glassnavlab-android-stewardship/SKILL.md` and use this skill for the Liquid Glass rendering details.

## Workflow

1. Inspect the local code before changing behavior:

   ```bash
   rg -n "LiquidGlass|RuntimeShader|Backdrop|Navigation" app core feature
   ```

2. Keep component boundaries clear:

   - Stateful API: `LiquidGlassBottomNavigation`.
   - Stateless API: `LiquidGlassBottomNavigationBar`.
   - Style tokens: `LiquidGlassNavigationStyle`.
   - Android 13+ AGSL surface layer: `LiquidGlassAgslOverlay.kt`.
   - Product shell: `feature/notmid/impl/.../NotmidShellScreen.kt`.
   - Feature design components: `feature/notmid/impl/.../components`.
   - Feature UI mapping: `feature/notmid/impl/.../model`.

3. Preserve the renderer split:

   - Android 13+ may use `RuntimeShader`, `RenderEffect`, or backdrop lens effects.
   - Android 12 and lower must keep a non-crashing fallback with AGSL/lens disabled.
   - Do not describe backdrop capture as pure AGSL. Backdrop handles background capture and blur; AGSL handles procedural surface light.

4. Keep the visual contract:

   - Selection is flat only after it settles.
   - Selection turns glass during long press, drag, or transition.
   - Selected content must be visibly stronger than unselected content.
   - If a trailing circular action is used, it must press as a circle, not as a square ripple.
   - The bar must adapt to light/dark backdrops using two stable tone buckets.

5. Verify Android work before finalizing:

   ```bash
   ./gradlew :app:compileDebugKotlin
   ```

6. For visual changes, install and refresh captures:

   ```bash
   ./gradlew :app:installDebug
   "${ANDROID_HOME:-$HOME/Library/Android/sdk}/platform-tools/adb" shell am start -n app.thdev.glassnavlab/.MainActivity
   ```

   Store screenshots, and only necessary behavior videos, in `docs/assets`.

## Quality Bar

- Prefer existing Compose patterns and local style tokens over new one-off modifiers.
- Keep shell orchestration separate from feature design components and product fake data.
- Keep AGSL shader constants small and explain only non-obvious shader decisions.
- Do not add a new dependency unless the repo cannot reasonably implement the behavior with Compose, Android graphics APIs, or the existing backdrop library.
- Commit UI-visible changes with updated screenshots when possible.
