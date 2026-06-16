---
title: Build Logic And Module Templates
audience: Android engineers and AI agents
purpose: 새 `api`/`impl`/`assertions` 모듈을 추가할 때 Gradle convention과 템플릿을 정의한다.
status: draft
owner: notmid Android architecture
source_of_truth: docs/specs/android-commonization
last_verified: 2026-06-16
applies_to: build-logic, settings.gradle.kts, module build.gradle.kts
related_pages:
  - 02-target-module-taxonomy.md
  - 07-state-assertions-testing.md
---

# Build Logic And Module Templates

## Current Build Logic

Notmid에는 현재 네 가지 convention plugin이 있다.

```text
glassnavlab.android.application
glassnavlab.android.library
glassnavlab.android.library.compose
glassnavlab.kotlin.library
```

이 상태는 작고 명확하다. 참조 프로젝트처럼 router, network, control, repository, hilt, ksp, jacoco, detekt convention을 한 번에 복사하지 않는다.

## Decision

첫 구현에서 필요한 build logic은 최대 하나다.

추천:

```text
glassnavlab.kotlin.assertions
```

단, 첫 `assertions` 모듈이 단순 Kotlin/JVM fake만 필요하면 `glassnavlab.kotlin.library`를 재사용하고 convention 추가를 미룬다.

Android/Compose assertion이 두 개 이상 생기면 다음을 검토한다.

```text
glassnavlab.android.assertions
glassnavlab.android.compose.assertions
```

## Convention 추가 기준

새 convention을 만든다:

- 같은 설정이 두 개 이상의 모듈에서 반복된다.
- assertions 모듈마다 `api(project(":...:api"))`, `testImplementation`, coroutine test dependency가 반복된다.
- Android test manifest/namespace가 반복된다.
- module naming과 visibility를 build logic으로 강제하면 review 비용이 줄어든다.

새 convention을 만들지 않는다:

- 첫 모듈 하나만 필요하다.
- dependency가 모듈마다 다르다.
- convention이 product behavior나 DI binding을 숨긴다.
- Hilt/KSP/Firebase/ads 같은 reference-only 의존성을 끌어오게 된다.

## Module Template: Pure API

Use when:

- caller-facing route/network/domain contract.
- no Android framework type.
- no Compose type.

```kotlin
plugins {
    id("glassnavlab.kotlin.library")
}

dependencies {
    testImplementation(libs.junit)
}
```

Example target:

```text
:core:router:api
:core:network:api
```

## Module Template: Pure Impl

Use when:

- implementation can run on JVM.
- Android runtime is not required.

```kotlin
plugins {
    id("glassnavlab.kotlin.library")
}

dependencies {
    implementation(project(":core:network:api"))
    testImplementation(libs.junit)
}
```

Example target:

```text
:core:router:impl
:core:network:impl
```

## Module Template: Android/Compose App API

Use when:

- contract exposes Compose lifecycle/renderer concepts.
- contract must be imported by feature/app modules.
- Android framework type is part of the contract.

Prefer not to expose Android concrete classes unless the platform is the contract.

```kotlin
plugins {
    id("glassnavlab.android.library.compose")
}

android {
    namespace = "app.thdev.glassnavlab.coreapp.feedback.api"
}

dependencies {
    api(project(":core:model"))
}
```

If the API can be pure Kotlin, use `glassnavlab.kotlin.library` instead.

## Package Template: Android/Compose App Runtime In `:core-app`

Use when:

- rendering Toast, AlertDialog, Snackbar, permission launcher, ActivityResult, WebView.
- implementation depends on Compose Material3 or Android framework.
- the code belongs to app-runtime commonization but does not deserve a new Gradle module.

```kotlin
plugins {
    id("glassnavlab.android.library.compose")
}

android {
    namespace = "app.thdev.glassnavlab.coreapp"
}

dependencies {
    implementation(project(":core:router:api"))
}
```

Rule:

- Keep API-shaped contracts in a stable package such as `coreapp.feedback.api`.
- Keep production implementation in a sibling package such as `coreapp.feedback.impl`.
- Keep shared test helpers in `src/test` until two or more external test boundaries need them as a real Gradle artifact.

## Module Template: Assertions

Use when:

- multiple tests need the same fake/recording helper.
- the helper should not live in production `impl`.
- tests should compile against a stable contract without app shell setup.

Pure Kotlin:

```kotlin
plugins {
    id("glassnavlab.kotlin.library")
}

dependencies {
    api(project(":core:router:api"))
    testImplementation(libs.junit)
}
```

Android/Compose inside `:core-app`:

```kotlin
plugins {
    id("glassnavlab.android.library")
}

android {
    namespace = "app.thdev.glassnavlab.coreapp"
}

dependencies {
    testImplementation(libs.junit)
}
```

Use `api(project(...))` in assertions only when test source should access the API contract through the assertions artifact. Otherwise use `implementation`.

## Settings Gradle Rule

Group modules by family and keep comments short.

```kotlin
include(
    ":core:router:api",
    ":core:router:impl",
    ":core:router:assertions",
    ":core:network:api",
    ":core:network:impl",
    ":core:network:assertions",
)

include(
    ":core-app",
)
```

Do not create the full target list in `settings.gradle.kts` before there is code for it. Add modules in the implementation phase that actually needs them.

## Namespace Rule

Prefer stable namespace names that describe ownership.

```text
app.thdev.glassnavlab.core.router.assertions
app.thdev.glassnavlab.core.network.assertions
app.thdev.glassnavlab.coreapp.feedback.api
app.thdev.glassnavlab.coreapp.feedback.impl
app.thdev.glassnavlab.coreapp.feedback.assertions
```

Avoid:

```text
app.thdev.glassnavlab.common
app.thdev.glassnavlab.utils
app.thdev.glassnavlab.vendorname
```

## Dependency Catalog

Do not add new dependencies in the planning phase.

Allowed later only with a concrete need:

- `kotlinx-coroutines-test` for assertion or ViewModel tests.
- Compose UI test dependency if a reusable Compose renderer gets tests.
- OkHttp mock web server only if network impl tests need protocol-level behavior and current fake client is insufficient.

Do not add:

- Hilt/Dagger.
- KSP generation.
- Retrofit only because the reference project used it.
- Android Navigation Compose only because router exists.
- Firebase SDK for commonization.

## Review Checklist

- Did the module use an existing convention first?
- Is a new convention backed by at least two modules?
- Are reference project plugin IDs absent?
- Does `assertions` avoid production implementation dependencies?
- Are module names and package names consistent with `core` vs `core-app`?

## Verification

For build logic or module creation:

```bash
./gradlew help
./gradlew :app:compileDebugKotlin
./gradlew test
git diff --check
```

For docs-only planning:

```bash
git diff --check
```
