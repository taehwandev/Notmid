# Android Reference Modularization

Use this reference when a notmid task asks to modularize the app, add `build-logic`, introduce a design system, or split data/domain/fake service code. The source project should be supplied outside committed repo-local docs as `${REFERENCE_ANDROID_PROJECT_ROOT}`.

## What Was Inspected

- Root module registry: `settings.gradle.kts`.
- Included build: `build-logic/settings.gradle.kts`.
- Convention plugin registry: `build-logic/convention/build.gradle.kts`.
- Base convention plugins:
  - `AndroidApplicationConventionPlugin.kt`
  - `AndroidLibraryConventionPlugin.kt`
  - `AndroidLibraryComposeConventionPlugin.kt`
  - `AndroidLibraryComposeFeatureConventionPlugin.kt`
  - `AndroidLibraryRepositoryApiConventionPlugin.kt`
  - `AndroidLibraryRepositoryConventionPlugin.kt`
  - `KotlinLibraryConventionPlugin.kt`
- Shared Gradle helpers:
  - `reference/gradle/KotlinAndroid.kt`
  - `reference/gradle/ComposeAndroid.kt`
  - `reference/gradle/extensions/InternalProjectExtension.kt`
  - `reference/gradle/extensions/AppExtension.kt`
- Representative modules:
  - `core-app/compose/compose-design-system`
  - `feature/manage/family/manage/family-manage`
  - `feature/manage/family/manage/family-manage-api`
  - `core-data/domain/domain-family`
  - `core-data/repository/family/repository-family-api`
  - `core-data/repository/family/repository-family`

## Transferable Structure

The reference project uses a large production shape:

- `:app` is the root Android application and aggregates many feature/core modules.
- `:feature:*:*` modules own user-facing screens.
- `:feature:*:*-api` modules expose navigation or journey contracts without pulling in implementation UI.
- The reference app-runtime module family owns app-facing shared Android/Compose infrastructure such as design-system, resources, network, lifecycle, dialog, toast, and permissions. In this repository, shrink that idea into packages inside the single `:core-app` module unless a real Gradle boundary is proven.
- `:core-data:domain:*` modules own use cases that compose repository contracts.
- `:core-data:repository:*:*-api` modules expose repository interfaces and entities.
- `:core-data:repository:*:*` modules implement repository contracts and handle API/cache/mapper details.
- `build-logic` centralizes Gradle plugin application, SDK versions, Kotlin options, Compose enablement, and common dependencies.

For this repository, shrink that into:

- `:app`
- `:feature:notmid:api`
- `:feature:notmid:impl`
- `:core:designsystem`
- `:core:model`
- `:core:domain`
- `:core:data`
- `:core:router:api`
- `:core:router:impl`
- `build-logic`

Do not mirror the reference project's full folder depth unless notmid grows multiple independent product areas.

## Build Logic Pattern

The reference project's `settings.gradle.kts` adds:

```kotlin
pluginManagement {
    includeBuild("build-logic")
}
```

Its `build-logic` is an included build with a `:convention` module that registers convention plugins. The current notmid Android build keeps only these convention plugin ids:

- `glassnavlab.android.application`
- `glassnavlab.android.library`
- `glassnavlab.android.library.compose`
- `glassnavlab.kotlin.library`

Use the reference project's helper pattern, but rename and reduce it:

- `findLibrary(name)` and `findVersion(name)` read from the `libs` version catalog.
- `androidExtension` hides whether the target is an application or library.
- `setNamespace(name)` is useful, but this repository should generate `app.thdev.glassnavlab.<name>` or set explicit namespaces per module until the Android package is intentionally renamed.
- `configureKotlinAndroid()` centralizes SDK, Java/Kotlin target, defaultConfig, and test options.
- `configureComposeAndroid()` applies `org.jetbrains.kotlin.plugin.compose`, enables Compose, and adds the Compose BOM plus common Compose dependencies.

Avoid copying reference build logic for:

- Hilt, KSP, generated data modules.
- Firebase, Crashlytics, Google services.
- detekt, Jacoco, JUnit5, Appsuit.
- Flavors, signing configs, ad repositories, banking libraries.
- `evaluationDependsOn` based generated module wiring.

## Module Dependency Direction

Use one-way dependencies:

```text
:app
  -> :feature:notmid:api
  -> :feature:notmid:impl
  -> :core:router:api
  -> :core:router:impl

:feature:notmid:impl
  -> :feature:notmid:api
  -> :core:designsystem
  -> :core:domain
  -> :core:model

:feature:notmid:api
  -> :core:router:api

:core:domain
  -> :core:model

:core:data
  -> :core:domain
  -> :core:model
```

If the app directly constructs fake repositories, `:app` may depend on `:core:data`. If DI is not introduced, keep the wiring explicit and local in `:app` or `:feature:notmid:impl`.

Do not allow:

- `:core:designsystem` depending on `:feature:notmid:impl`.
- `:core:model` depending on Android, Compose, `Color`, `Dp`, or resources.
- `:core:domain` depending on repository implementation modules.
- `:core:data` depending on Compose UI.

## Design System Lessons

The reference project keeps many shared components in `core-app/compose/compose-design-system`, including buttons, text fields, list components, navigation, scaffold, status bar, badges, selectors, placeholders, and visual transformations.

For this repository:

- Move reusable theme files from `app/ui/theme` to `:core:designsystem`.
- Move reusable Liquid Glass navigation component code to `:core:designsystem` if more than one screen uses it.
- Keep feature-only cards, header, icons, and copy in `:feature:notmid:impl`.
- Treat `LiquidGlassNavigationStyle` as design-system tokens.
- Keep names product-specific, not imported. Prefer `notmidTheme`, `LiquidGlass*`, and `Notmid*` over imported vendor-specific naming.

## Data And Domain Lessons

The reference project's family example shows three separate layers:

- Repository API module: interfaces and data entities, such as `FamilyRepository` and `FamilyEntity`.
- Repository implementation module: API calls, local cache, mapping from response DTOs to entities, and mutation methods.
- Domain module: use cases, such as filtering family members in `FetchAllFamilyMembersUseCase`.

For notmid fake data:

- Put pure product concepts in `:core:model`, for example destination id, title, subtitle, clip descriptors, and place descriptors.
- Put repository contracts/use cases in `:core:domain`, for example `NotmidContentRepository` and `GetNotmidDestinationsUseCase`.
- Put deterministic fake/static data in `:core:data`, for example `StaticNotmidContentRepository`.
- Keep Compose-specific projection in `:feature:notmid:impl`, for example mapping palette tokens to `Color`, `Dp`, icon lambdas, and UI-only navigation items.

Do not over-engineer with network APIs, Hilt scopes, generated factories, or lifecycle annotations until a real runtime need appears.

## Feature Module Lessons

Reference feature modules tend to contain:

- Activity or route entry point.
- ViewModel or state holder.
- Compose screen functions under `compose`.
- UI state models under `model`.
- Converters under `model/convert`.
- Small component leaves under `compose/component`.
- API modules for navigation contracts.

For this repository:

- `:feature:notmid:impl` should own `NotmidShellScreen`, feature screen state, feature card/header components, and UI mapping.
- Use `:feature:notmid:api` when app, another feature, or another Activity needs a stable route/event contract without depending on feature UI implementation.
- Keep `:core:router:api` as pure route/event/web-link interfaces. Let `:app` own the actual navigation state and translate feature events or web links into route stack changes.
- Keep state-holder and UI composables split: the holder obtains domain data and selects destinations; the plain UI renders immutable state and callbacks.
- Keep preview providers and placeholder states beside the feature UI if added.

## App Module Lessons

The reference project's `:app` aggregates all modules and contains production flavor/signing/build config wiring. This repository should keep `:app` much thinner:

- `MainActivity`
- manifest and launcher resources
- `enableEdgeToEdge()`
- theme wrapping
- explicit construction of fake data/repository dependencies if no DI is used

Do not use the reference pattern where `:app` automatically depends on almost every subproject. For this sample, explicit module dependencies are easier to reason about.

## Practical Implementation Order

1. Add `build-logic` with minimal convention plugins.
2. Convert existing `:app` to `glassnavlab.android.application`.
3. Add `:core:designsystem`, move theme and Liquid Glass reusable components.
4. Add `:core:model`, move pure non-Compose models first.
5. Add `:core:domain`, define repository contracts/use cases.
6. Add `:core:data`, move static fake data behind the domain contract.
7. Add `:core:router:api`, `:core:router:impl`, and `:feature:notmid:api` when feature route/event/web-link contracts must outlive the UI implementation.
8. Add `:feature:notmid:impl`, move screen orchestration and feature-only UI.
9. Reduce `:app` to `MainActivity` and module wiring.
10. Run `./gradlew help` and `./gradlew :app:compileDebugKotlin` after each major step.

## Review Checklist

- Build convention ids are project-specific, not vendor-prefixed ids.
- No reference-project package names, secrets, signing config, ad/network repositories, or generated-module dependencies were copied.
- Module names are proportional to notmid's current product scope.
- Pure model/domain modules do not import Android or Compose.
- Design system has shared UI/tokens only; feature copy/fake data stays out.
- Data module can be swapped without touching the UI module.
- The final dependency graph is one-way and explicit.
