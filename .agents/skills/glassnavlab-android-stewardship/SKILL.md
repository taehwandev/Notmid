---
name: glassnavlab-android-stewardship
description: Use when changing the notmid Android architecture, Gradle build-logic, module boundaries, design system, data/domain layers, product fake services, Compose screen structure, shared UI placement, or verification strategy before feature-specific implementation.
---

# notmid Android Stewardship

## Purpose

Keep the repository product-shaped enough for `notmid` while preserving the reusable Liquid Glass Android reference. Use this skill before moving files, adding build conventions, creating data/domain layers, changing shared UI, or deciding where code belongs.

If the change touches Liquid Glass navigation rendering, AGSL, backdrop capture, gesture behavior, screenshots, or release polish, also use `.agents/skills/android-liquid-glass-compose/SKILL.md`.

## Start Here

1. Run `git status --short` and keep unrelated user changes out of the write set.
2. Inspect the smallest relevant build and source shape:

   ```bash
   rg --files
   sed -n '1,220p' settings.gradle.kts
   sed -n '1,260p' gradle/libs.versions.toml
   ```

3. If modularizing, compare `/Users/taehwankwon/Downloads/firfin-android-main` only for structure: included `build-logic`, convention plugin names, and `feature`/`core-data` style boundaries. Do not copy Firebase, Hilt, ad, banking, or enterprise-specific dependencies unless the task explicitly requires them.
   - For the distilled FirFin reference, read `references/firfin-modularization-reference.md`.
4. If routing, route events, deep links, `ActivityRoute`, Compose back stacks, or a mixed Activity/Compose navigation surface is involved, use `.agents/skills/android-mixed-activity-compose-router/SKILL.md` first. For the Notmid/FirFin comparison notes, read `references/mixed-activity-compose-router-reference.md`. Borrow FirFin's caller-facing route contract idea, not its production-only KSP, Hilt, or Activity-first implementation.
5. Define the change type: build convention, module split, behavior-preserving move, behavior change, UI/design-system change, router contract change, fake data/service change, or documentation-only.
6. Prefer move-first refactors. Keep behavior changes separate from file moves when feasible.

## Target Shape

Use this module direction unless the current task gives a better reason:

- `:app`: Android entry point only. Owns `MainActivity`, manifest, launcher resources, app theme wiring, top-level composition, app route graph assembly, auth/deferred routing, and Activity launch dispatch. It should depend on feature modules, not own reusable UI or fake data.
- `:core:designsystem`: Compose theme, colors, typography, shape/elevation tokens, reusable app primitives, and the Liquid Glass navigation component if it is shared across screens.
- `:core:model`: pure Kotlin immutable models that contain no Android, Compose, `Color`, `Dp`, resource, or repository implementation types.
- `:core:domain`: pure Kotlin use cases and repository contracts. No Android plugin unless a real Android dependency is needed.
- `:core:data`: repository implementations and fake service data. Keep mapping from raw product data to domain models here.
- `:core:router:api`: pure Kotlin router contracts shared by app and feature API modules. Keep it free of AndroidX Navigation, `Context`, `Intent`, and `NavController` until an Android-specific execution adapter is introduced.
- `:core:router:impl`: default registry and deep-link matching implementation. Keep Android Activity launching and app-link host policy outside this module until an Android-specific router module is needed.
- `:feature:notmid:api`: shared notmid route markers, destination ids, route events, and helpers for feature route/deep-link specs.
- `:feature:notmid:impl`: the notmid Liquid Glass product shell, UI state mapping, product-only components, previews, and feature orchestration. It may depend on `core:domain`, `core:model`, `core:designsystem`, and its own `feature:notmid:api`.

Avoid creating extra modules just to mirror a large production app. Add a module when it creates a real ownership boundary or removes coupling from `:app`.

## Build Logic

Introduce `build-logic` as a small included build, not a copy of the FirFin build:

- Add `pluginManagement { includeBuild("build-logic") }` in `settings.gradle.kts`.
- Add `build-logic/settings.gradle.kts` with a `libs` version catalog imported from `../gradle/libs.versions.toml`.
- Keep convention plugins minimal:
  - `glassnavlab.android.application`
  - `glassnavlab.android.library`
  - `glassnavlab.android.library.compose`
  - `glassnavlab.kotlin.library`
- Centralize `compileSdk`, `minSdk`, `targetSdk`, Java/Kotlin targets, Compose enablement, and common dependencies.
- Keep plugin ids project-specific; do not use copied `fir.fin.*` ids.
- Add only catalog entries the project uses. Do not add Hilt, KSP, Firebase, detekt, or navigation before the code needs them.

After build-logic changes, verify plugin wiring with:

```bash
./gradlew help
./gradlew :app:compileDebugKotlin
```

## Compose And UI Rules

- Split state-holder composables from plain UI composables. The holder wires repositories, state, effects, and navigation callbacks; the UI composable takes immutable UI state plus explicit callbacks.
- Feature-to-feature and activity-to-activity communication should go through route/event contracts, not implementation module references. A feature emits its own `feature:*:api` event, and `:app` decides whether that event changes route, opens another feature/activity, or is ignored.
- Keep caller-facing navigation high level. Feature UI should emit a route event or route intent; `RouteStack`, Activity launch requests, `Intent`, and NavController operations should stay in app/router execution code unless the feature owns a local-only nested graph.
- Web links must resolve to ordered route stacks. For example, `https://thdev.app/notmid/profile/settings` should become `[Profile, Settings]`, not only a single top destination.
- Keep UI-local state such as scroll, gesture, animation, focus, and interaction state in the UI when it only affects rendering.
- Leaf components should accept `Modifier`, plain values, and slots/callbacks. Do not pass repositories, activities, or whole state holders into leaf UI.
- Keep `:core:designsystem` free of feature product data. It may expose reusable components and tokens, but not destinations or feature copy.
- Keep domain models free of Compose types. Map domain color tokens, ids, or semantic palette names to Compose `Color`/`Dp` in `:feature:notmid:impl` or `:core:designsystem`.
- Preserve edge-to-edge behavior: `MainActivity` should call `enableEdgeToEdge()`, and floating bottom navigation must account for navigation bar insets.

## Liquid Glass Boundaries

When moving Liquid Glass code, preserve these ownership lines:

- Stateful API: `LiquidGlassBottomNavigation`.
- Stateless API: `LiquidGlassBottomNavigationBar`.
- State holder: `LiquidGlassNavigationState`.
- Style tokens: `LiquidGlassNavigationStyle` and defaults.
- Backdrop host: backdrop capture and content layering.
- Android 13+ AGSL layer: `LiquidGlassAgslOverlay.kt`.

Android 12 and lower must keep non-crashing fallbacks for AGSL and lens effects. Do not describe backdrop capture as pure AGSL: backdrop handles captured background blur; AGSL handles procedural surface light.

## Data And Product Services

For product-shaped fake services:

- Define repository contracts in `:core:domain`.
- Keep fake/static implementations in `:core:data`.
- Use deterministic local data before adding network, persistence, or DI.
- If a model needs UI-only concepts such as `Dp`, `Color`, selected icon lambdas, or localized labels, keep that shape in `:feature:notmid:impl`.
- Prefer explicit domain names such as `NotmidDestination`, `NotmidClip`, and `NotmidPlace` over generic `Item` or `Data`.

## Verification

Choose the narrowest useful gate:

- Skill/docs-only:

  ```bash
  git diff --check
  ```

- Build logic or module graph:

  ```bash
  ./gradlew help
  ./gradlew :app:compileDebugKotlin
  ```

- Kotlin/domain logic:

  ```bash
  ./gradlew test
  ```

- UI or Liquid Glass visual behavior:

  ```bash
  ./gradlew :app:compileDebugKotlin
  ./gradlew :app:installDebug
  /Users/taehwankwon/Library/Android/sdk/platform-tools/adb shell am start -n app.thdev.glassnavlab/.MainActivity
  ```

Store refreshed screenshots or short behavior captures in `docs/assets` only when the visual contract changed.

## Stop Conditions

Stop and ask or narrow the change when:

- A refactor starts touching unrelated app behavior.
- A copied FirFin convention would pull in production-only tooling or dependencies.
- The desired module split requires a new architecture decision not covered above.
- Tests expose pre-existing failures that make verification ambiguous.
- Continuing would require reverting or overwriting user changes.
