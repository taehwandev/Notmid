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

3. If modularizing, compare `${REFERENCE_ANDROID_PROJECT_ROOT}` only for structure: included `build-logic`, convention plugin names, and `feature`/`core-data` style boundaries. Do not copy Firebase, Hilt, ad, banking, or enterprise-specific dependencies unless the task explicitly requires them.
   - For the distilled reference, read `references/android-reference-modularization.md`.
4. If routing, route events, deep links, `ActivityRoute`, Compose back stacks, or a mixed Activity/Compose navigation surface is involved, use `.agents/skills/android-mixed-activity-compose-router/SKILL.md` first. For the Notmid/reference-project comparison notes, read `references/mixed-activity-compose-router-reference.md`. Borrow the reference project's caller-facing route contract idea, not its production-only KSP, Hilt, or Activity-first implementation.
5. When the task touches shared Android structure, read `docs/specs/android-commonization/README.md` for the current `core` / `core/app` / `api` / `impl` / `assertions` rollout plan. The planning specs are Korean until they are translated before commit or release.
6. Define the change type: build convention, module split, behavior-preserving move, behavior change, UI/design-system change, router contract change, fake data/service change, or documentation-only.
7. Prefer move-first refactors. Keep behavior changes separate from file moves when feasible.

## Target Shape

Use this module direction unless the current task gives a better reason:

- `:app`: Android entry point only. Owns `MainActivity`, manifest, launcher resources, app theme wiring, top-level composition, runtime config injection, pending deep-link handoff, and concrete platform launch binding. It should depend on feature modules, not own reusable UI, fake data, route registry construction, deep-link resolver construction, or feature event mapping.
- `:core:designsystem`: Compose theme, colors, typography, shape/elevation tokens, reusable app primitives, visual feedback primitives, and the Liquid Glass navigation component if it is shared across screens. It must not collect feedback flows, show Toasts, or own app-runtime effects.
- `:core:model`: pure Kotlin immutable models that contain no Android, Compose, `Color`, `Dp`, resource, repository implementation, or UI effect runtime types.
- `:core:domain`: pure Kotlin use cases and repository contracts. No Android plugin unless a real Android dependency is needed.
- `:core:data`: repository implementations and fake service data. Keep mapping from raw product data to domain models here.
- `:core:feedback:api`: pure Kotlin feedback request/effect contracts. Split by role, such as `model/` for `FeedbackRequest`, `FeedbackPresentation`, `FeedbackTone`, and `FeedbackAction`, and `effect/` for `FeedbackEffect` and delegates. Use `sealed interface` for closed feedback event/action families owned here; use open interfaces only for injection points such as delegates.
- `:core:*`: pure Kotlin or implementation-neutral contracts for route, network, model, domain, and reusable test-support boundaries. These modules should stay free of Android and Compose runtime unless a specific legacy boundary is being migrated.
- `:core:app`: Android/Compose app-runtime commonization. It may contain router runtime, feedback hosts, permission adapters, ActivityRoute launch adapters, reusable WebView runtime, resources, and app-shell helpers as packages, but should stay feature-policy free.
- `:core:app` feedback packages: reusable Activity/Compose host installation and Android runtime rendering. Keep them under role packages such as `feedback/host/`. They may depend on `:core:feedback:api` and `:core:designsystem`; they must not own feature copy, repositories, product route policy, or network error mapping.
- `:core:app` router packages: reusable Android/Compose app-router runtime contracts and default execution state. They own `config/` bundle/config assembly, `planner/` app route planning, `deeplink/` app resolver adapters, `runtime/` Compose `RouteStack` state and pending ActivityRoute queueing, and `activity/` ActivityRoute launch contracts/effects/handler registry. Keep URI parsing, base-path stripping, route event planning, deep-link matching, Notmid feature policy, repositories, feature impl imports, WebView Intent construction, auth policy, analytics, and data access out. `Context` is allowed only in Android launch adapters such as `DefaultActivityRouteLauncher`, not in pure router API or product policy.
- `:core:router:api`: pure Kotlin router contracts shared by app and feature API modules. Keep it free of AndroidX Navigation, `Context`, `Intent`, and `NavController` until an Android-specific execution adapter is introduced.
- `:core:router:impl`: default registry, route event planner, URI-to-`DeepLinkRequest` parser, scheme/host/base-path policy normalization, static/prefix deep-link spec implementations, and deep-link resolver. Split packages by role: `registry/`, `event/`, and `deeplink/`. Keep Android Activity launching, `Context`, `Intent`, feature implementation imports, auth policy, analytics, and repositories outside this module.
- `:core:*:assertions` and `:core:app` test source: reusable test doubles, recording fakes, fixtures, and assertion helpers that compile against stable API contracts without pulling production implementation modules by default. Add a new `core:app:assertions` Gradle module only after two or more external test boundaries need the same helpers.
- `:core:network:assertions`: reusable network test fakes and assertion helpers. Keep request recording, queued responses/failures, safe header redaction, and API error envelope fixtures here once two or more repository/auth/data tests need them. It depends on `:core:network:api` only, not `:core:network:impl`, app, feature impl, or Android runtime.
- `:feature:notmid:api`: shared notmid route markers, destination ids, route events, and helpers for feature route/deep-link specs.
- `:feature:notmid:impl`: the notmid Liquid Glass product shell, UI state mapping, product-only components, previews, feature orchestration, and Notmid route registrations/event handlers that compose the reusable `:core:app` router bundle. It may depend on `:core:app`, `:core:domain`, `:core:model`, `:core:designsystem`, feature API modules, and its own `:feature:notmid:api`.
- `:feature:*:assertions`: only when route/display fixtures or recording helpers are reused across more than one test boundary. Keep one-off preview data inside the feature impl.

Avoid creating extra modules just to mirror a large production app. Add a module when it creates a real ownership boundary or removes coupling from `:app`.

## API / Impl / Assertions Direction

- `api` modules expose stable contracts, route data, DTO-like request/response shapes, interfaces, and error models.
- `impl` modules own production implementations: screens, adapters, repositories, clients, route registries, and runtime orchestration.
- `assertions` modules own deterministic fake implementations, recording sinks/sources, fixtures, and assertion DSLs for tests.
- `assertions` modules should depend on the matching `api` module and test libraries. They should not depend on production `impl` modules unless they live inside that impl module's own test source set.
- Router commonization has two active layers: `:core:router:assertions` for pure route stack/plan helpers and route event/deep-link contract tests, and `:core:app` test source for app-runtime fakes such as app deep-link resolver adapters and ActivityRoute launch recorders. Extract a `:core:app:assertions` Gradle module only after two or more external test boundaries need those fakes without depending on the runtime module.
- Do not create feature assertions modules for previews, static sample data, or a single test. Keep those local until the reuse is real.
- Use plural `assertions` for module names so Gradle paths stay consistent.

## Compose App Shell Direction

- Do not recreate a broad `BaseActivity`, `BaseFragment`, or universal `BaseViewModel` hierarchy. Prefer small Compose-first runtime contracts such as `AppEnvironment`, `AppRoot`, `RouteCoordinator`, `FeedbackHost`, and `PermissionHost`.
- `MainActivity` should remain the Android entry holder. Product behavior belongs in app ViewModels, feature state holders, route coordinators, or domain services.
- Shared `:core:app` helpers must not absorb feature copy, product route policy, analytics policy, repositories, or screen-specific state.
- Keep WebView, permission, feedback, and Activity launching commonization behind caller-facing contracts so feature code does not know whether a destination is Compose-backed, Activity-backed, or browser/WebView-backed.

## Build Logic

Introduce `build-logic` as a small included build, not a copy of the reference build:

- Add `pluginManagement { includeBuild("build-logic") }` in `settings.gradle.kts`.
- Add `build-logic/settings.gradle.kts` with a `libs` version catalog imported from `../gradle/libs.versions.toml`.
- Keep convention plugins minimal:
  - `glassnavlab.android.application`
  - `glassnavlab.android.library`
  - `glassnavlab.android.library.compose`
  - `glassnavlab.kotlin.library`
- Centralize `compileSdk`, `minSdk`, `targetSdk`, Java/Kotlin targets, Compose enablement, and common dependencies.
- Keep plugin ids project-specific; do not use copied reference-project ids.
- Add only catalog entries the project uses. Do not add Hilt, KSP, Firebase, detekt, or navigation before the code needs them.
- Add assertion-specific convention plugins only after at least two `assertions` modules repeat the same Gradle setup.

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
- A copied reference-project convention would pull in production-only tooling or dependencies.
- The desired module split requires a new architecture decision not covered above.
- A proposed `assertions` module needs production `impl` dependencies to be useful.
- A proposed `:core:app` helper starts owning feature copy, route policy, analytics policy, repositories, or screen-specific state.
- Tests expose pre-existing failures that make verification ambiguous.
- Continuing would require reverting or overwriting user changes.
