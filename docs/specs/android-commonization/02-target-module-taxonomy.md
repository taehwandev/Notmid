---
title: Target Module Taxonomy
audience: Android engineers and AI agents
purpose: Notmid의 `core`, `core-app`, feature, assertions 모듈 분류와 import 규칙을 정의한다.
status: draft
owner: notmid Android architecture
source_of_truth: docs/specs/android-commonization
last_verified: 2026-06-16
applies_to: settings.gradle.kts, Gradle modules, package ownership
related_pages:
  - README.md
  - 03-build-logic-module-templates.md
  - llm-wiki/module-map.md
---

# Target Module Taxonomy

## Decision

Notmid의 목표 구조는 다음 두 축을 분리한다.

- `core`: 순수 Kotlin 또는 플랫폼 독립 계약. 앱이 아니어도 재사용 가능해야 한다.
- `core-app`: Android/Compose 앱 런타임. UI renderer, lifecycle, permission, WebView, Android resource, app shell helper를 둔다.

이 분리는 “모든 모듈을 쪼갠다”가 아니라 “의존성 방향을 문서화하고 테스트 가능한 계약을 만든다”가 목적이다.

## Target Tree

장기 목표:

```text
app/
  MainActivity, runtime config injection, pending deep-link handoff,
  concrete platform launch binding

core/
  model/
  domain/
  router/
    api/
    impl/
    assertions/
  network/
    api/
    impl/
    assertions/
  auth/
    api/
    impl/
    assertions/        only if auth tests need shared fake gateway

core-app/
  router/              active app-runtime contract + implementation package
  feedback/            later package inside the same module
  permissions/         later package inside the same module
  webview/             later package inside the same module after reusable WebView pressure appears
  src/test/...         app-runtime fakes, recorders, and assertions until external reuse proves a module boundary

feature/
  <name>/
    api/
    impl/
    assertions/        only when feature route/state helpers are reused across modules
    common/            only for product UI shared by several feature impls
```

## Current-To-Target Mapping

| Current | Target | Migration |
| --- | --- | --- |
| `:core:model` | keep in `core` | Pure models stay. UI feedback/effect contracts should move out only when the `:core-app` feedback API package exists. |
| `:core:domain` | keep in `core` | Keep repository contracts/use cases pure. |
| `:core:data` | keep short term | Later split repository APIs/impl only when there are real multi-caller contracts. |
| `:core:network:api` | keep in `core` | Expand typed failures and safe server error envelope. |
| `:core:network:impl` | keep in `core` or move to `core-app` only if Android-specific | OkHttp JVM impl can stay `core`; Android connectivity/auth interceptors go `core-app`. |
| `:core:router:api` | keep in `core` | Pure contracts stay here. |
| `:core:router:impl` | keep in `core` | Pure route registry, route event planner, URI parser, scheme/host/base-path normalizer, deep-link resolver, and static/prefix matcher implementations. |
| `:core-app` router package | active app-runtime commonization | Single Android/Compose runtime module package that owns runtime execution state, app deep-link resolver adapter, pending ActivityRoute queue/effect, and module-local runtime test doubles. Split into a submodule only after external caller/test pressure proves the boundary. |
| `:core:designsystem` | transitional | Do not rename first. New app runtime feedback should move to the `:core-app` feedback package; later decide design system migration. |
| `:feature:webview:api/impl` | keep first | Route target remains feature. Extract reusable holder to a `:core-app` webview package only after hardening need appears. |
| `:app` route graph/event mapper/activity launcher | keep app-owned | App provides Notmid route/spec registration values, host/base path values, feature event handlers, and concrete Activity launch to core router impl and the `:core-app` runtime. |

## Module Family Rules

### `core`

Allowed:

- immutable models and IDs.
- repository contracts and domain failures.
- route contracts, route stacks, deep-link specs, route registry.
- HTTP method/request/response contracts.
- typed transport/protocol/server error shapes.
- fake/recording test helpers that depend on API contracts only.

Not allowed:

- `Context`, `Activity`, `Intent`, `Toast`, `WebView`, Android permission APIs.
- Compose UI components, Material3 components, `Color`, `Dp`.
- Firebase SDK, OkHttp implementation types in API contracts.
- feature implementation imports.

### `core-app`

Allowed:

- Compose renderer and lifecycle-aware effect collectors.
- Android resource/message resolver.
- permission launcher and result adapters.
- ActivityRoute launcher implementation.
- WebView holder/runtime state when reusable.
- app shell base helpers such as `AppEnvironment`, `AppRoot`, `FeedbackHost`.

Not allowed:

- feature-specific copy or product policy.
- repository implementations.
- route decisions that belong to app graph.
- raw server DTO parsing unless the module owns a platform adapter.

### Feature API

Allowed:

- public route data classes.
- route keys, top-level route metadata, and deep-link specs.
- route events emitted by feature impl.
- small caller-facing models that another module needs.
- package by import boundary once more than one contract family exists:
  `route/`, `deeplink/`, `event/`, and `activity/` only for real ActivityRoute
  launcher keys.

Not allowed:

- ViewModels, screens, Compose internals.
- repository implementation dependencies.
- Android Activity launching.

### Feature Impl

Allowed:

- stateful Route composable.
- ViewModel/state/effects/actions.
- stateless screen and feature-local components.
- UI mappers from domain/model to display models.
- platform detail only when the feature owns that platform surface, such as CameraX in capture.

Not allowed:

- direct dependency on another feature impl.
- app router implementation import.
- raw network client calls from composables.
- shared design primitive ownership.

### Assertions

Allowed:

- fake implementation of an API contract.
- recording sink/source.
- deterministic fixtures.
- assertion helper functions.
- test-only builders that make edge states easy to express.

Not allowed:

- production behavior.
- app shell dependencies.
- implementation module dependency unless the assertion explicitly verifies an implementation and lives in that implementation's `src/test`.
- Android UI launch as a side effect.

## Naming

Use plural `assertions`.

```text
:core:router:assertions
:core:network:assertions
:feature:feed:assertions
router assertions Gradle module only after external router fake reuse appears
```

Do not use the reference project's singular `assertion` naming in new Notmid modules.

Package naming:

```text
app.thdev.glassnavlab.core.router.assertions
app.thdev.glassnavlab.core.network.assertions
app.thdev.glassnavlab.coreapp.feedback.api
app.thdev.glassnavlab.coreapp.feedback.impl
app.thdev.glassnavlab.coreapp.feedback.assertions
```

If the repo prefers `core.app` instead of `coreapp`, decide once before implementation. The package should not encode Gradle punctuation mechanically if it makes imports noisy.

## Import Direction

Allowed:

```text
app -> feature:*:api
app -> feature:*:impl
app -> core:*:api/impl selected by app graph
app -> core-app selected packages selected by app graph

feature:*:impl -> feature:*:api
feature:*:impl -> core:model
feature:*:impl -> core:domain
feature:*:impl -> core-app feedback API package
feature:*:impl -> core:designsystem

core-app implementation package -> sibling core-app API package
core-app implementation package -> core:*:api where needed

assertions -> matching api
test source -> assertions
```

Forbidden:

```text
core -> core-app
core -> app
core -> feature:*:impl
feature:*:api -> feature:*:impl
feature:*:api -> app
feature:*:api -> core-app implementation package
assertions -> production impl by default
core-app feedback package -> feature route decisions
```

## Base/App Shell Rule

Do not introduce a large `BaseActivity` or `BaseViewModel` hierarchy.

Compose-first base commonization should be compositional:

```text
AppEnvironment
  dependencies selected by build/runtime config

AppRoot
  theme, route host, feedback host, permission host

RouteCoordinator
  route command/event/deep-link planning

FeedbackHost
  lifecycle-aware UI effect collection and rendering

PermissionHost
  ActivityResult launcher ownership and result mapping
```

`MainActivity` should remain the Android entry holder. It may delegate to app shell helpers, but those helpers should not own product feature behavior.

## Transition Policy

Do not start by moving all existing modules.

Order:

1. Add `assertions` where existing API boundary already exists.
2. Add new `:core-app` packages for new app-runtime contracts.
3. Move feedback renderer out of `:core:designsystem` only after the `:core-app` feedback API package is stable.
4. Decide design system migration separately. `:core:designsystem` can remain as a compatibility module until there is a real need to rename.
5. Split `:core:data` only after repository API pressure repeats across features or tests.

## Review Checklist

- Does the new module protect a caller-facing boundary?
- Can tests use the API without depending on implementation?
- Does `assertions` depend only on stable contracts?
- Does `core` stay Android/Compose-free except for already accepted transitional modules?
- Did the design avoid broad renames before behavior-preserving assertions are in place?
