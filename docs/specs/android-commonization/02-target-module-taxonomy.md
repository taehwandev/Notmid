---
title: Target Module Taxonomy
audience: Android engineers and AI agents
purpose: Notmid의 `core`, `core/runtime`, feature, assertions 모듈 분류와 import 규칙을 정의한다.
status: draft
owner: notmid Android architecture
source_of_truth: docs/specs/android-commonization
last_verified: 2026-06-28
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
- `core/runtime`: Android/Compose 실행 런타임. Notice renderer,
  lifecycle collector, permission/WebView adapter, Android resource adapter,
  router runtime, ActivityRoute launcher 같은 실행 부품만 둔다.

이 분리는 “모든 모듈을 쪼갠다”가 아니라 “의존성 방향을 문서화하고 테스트 가능한 계약을 만든다”가 목적이다.

`core` module type 판단 기준은 AgentPlayBook
`platforms/android/android-module-structure.md`의 `Core Is A Capability Namespace`가
source of truth다. 이 문서는 그 기준을 Notmid module 이름으로 매핑한다.

## Target Tree

장기 목표:

```text
app/
  MainActivity, runtime config injection, product content wiring,
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

core/runtime/
  router/              active runtime contract + implementation package
  notice/              active runtime host package inside the same module
  permissions/         later package inside the same module
  webview/             later package inside the same module after reusable WebView pressure appears
  src/test/...         runtime fakes, recorders, and assertions until external reuse proves a module boundary

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
| `:core:model` | keep in `core` | Pure models stay. UI notice/effect contracts live in `:core:notice:api`. |
| `:core:notice:api` | active pure contract module | Notice request/effect contracts split into `model/` and `effect/` packages. No Android, Compose, resources, repositories, or rendering. |
| `:core:domain` | keep in `core` | Keep repository contracts/use cases pure. |
| `:core:data` | keep short term | Later split repository APIs/impl only when there are real multi-caller contracts. |
| `:core:network:api` | keep in `core` | Expand typed failures and safe server error envelope. |
| `:core:network:impl` | keep in `core` or move to `:core:runtime` only if Android-specific | OkHttp JVM impl can stay `core`; Android connectivity/auth interceptors go `:core:runtime`. |
| `:core:router:api` | keep in `core` | Pure contracts stay here. |
| `:core:router:impl` | keep in `core` | Pure route registry, route event planner, URI parser, scheme/host/base-path normalizer, deep-link resolver, and static/prefix matcher implementations. |
| `:core:base` | active app-shell commonization | Compose-only `BaseActivity`, edge-to-edge defaults, final Activity lifecycle template handling, `Content()` composition, `AppRoot`, and pending external deep-link handoff. It depends on `:core:runtime` runtime contracts but owns no product route policy, repositories, runtime config, or ViewModel creation. |
| `:core:runtime` router package | active runtime commonization | Single Android/Compose runtime module package that owns runtime execution state, app deep-link resolver adapter, pending ActivityRoute queue/effect, and module-local runtime test doubles. Split into a submodule only after external caller/test pressure proves the boundary. |
| `:core:runtime` notice package | active runtime commonization | Compose/Activity `NoticeHost`, lifecycle collection, Toast/Snackbar/Alert dispatch, and design-system visual adapter usage. |
| `:core:designsystem` | active visual system | Keep theme/tokens/visual primitives here. Do not collect notice flows or show Toasts. |
| `:feature:webview:api/impl` | keep first | Route target remains feature. Extract reusable holder to a `:core:runtime` webview package only after hardening need appears. |
| `:app` route graph/event mapper/activity launcher | keep app-owned | App provides Notmid route/spec registration values, host/base path values, feature event handlers, and concrete Activity launch to core router impl and the `:core:runtime` runtime. |

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

Pure `core` module이 아닌 경우에는 AgentPlayBook 기준에 따라 이름이나 package에서
Android/Compose 실행 의존성을 드러낸다. Notmid의 현재 예시는 `:core:base`,
`:core:runtime`, `:core:designsystem`, `:core:runtime/notice/host`다.

### `core/runtime`

Allowed:

- Compose renderer and lifecycle-aware effect collectors.
- Android resource/message resolver.
- permission launcher and result adapters.
- ActivityRoute launcher implementation.
- WebView holder/runtime state when reusable.
- app shell base helpers such as `AppEnvironment`, `AppRoot`, `NoticeHost`.

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

Do not use singular `assertion` naming in new Notmid modules.

Package naming:

```text
app.thdev.glassnavlab.core.router.assertions
app.thdev.glassnavlab.core.network.assertions
app.thdev.glassnavlab.core.notice.api
app.thdev.glassnavlab.core.runtime.notice.host
app.thdev.glassnavlab.core.notice.assertions
```

Use `app.thdev.glassnavlab.core.runtime` for Android/Compose runtime packages.
The package name should describe runtime adapter ownership, not global app
ownership.

## Import Direction

Allowed:

```text
app -> feature:*:api
app -> feature:*:impl
app -> core:*:api/impl selected by app graph
app -> core:base for reusable Compose Activity template behavior
app -> core:runtime selected packages selected by app graph

feature:*:impl -> feature:*:api
feature:*:impl -> core:model
feature:*:impl -> core:domain
feature:*:impl -> core:notice:api when it emits reusable notice effects
feature:*:impl -> core:designsystem

core:runtime implementation package -> core:notice:api
core:runtime implementation package -> core:*:api where needed
core:base -> core:runtime contracts
core:base -> core:notice:api

assertions -> matching api
test source -> assertions
```

Forbidden:

```text
core -> core:runtime
core -> app
core -> feature:*:impl
feature:*:api -> feature:*:impl
feature:*:api -> app
feature:*:api -> core:runtime implementation package
assertions -> production impl by default
core:runtime notice package -> feature route decisions
```

## Base/App Shell Rule

Do not introduce a large `BaseActivity` or `BaseViewModel` hierarchy. A narrow
`BaseActivity` belongs in `:core:base` when it owns only reusable Activity
template behavior such as edge-to-edge setup, `Content()` composition,
incoming intent/deep-link request identity, and root host installation.

Compose-first base commonization should be compositional:

```text
AppEnvironment
  dependencies selected by build/runtime config

AppRoot
  theme, route host, notice host, permission host

RouteCoordinator
  route command/event/deep-link planning

NoticeHost
  lifecycle-aware UI effect collection and rendering

PermissionHost
  ActivityResult launcher ownership and result mapping
```

`MainActivity` should remain the Android entry holder. It may delegate to
`:core:base` Activity helpers, but those helpers should not own product feature
behavior.

Activity가 있는 host와 없는 host의 공통 기준은 AgentPlayBook
`platforms/android/android-architecture.md`의 runtime boundary 예제를 따른다.
Notmid 매핑은 Activity lifecycle 자체가 필요한 `Intent`, `onNewIntent`,
`ActivityResultRegistry` 연결만 `:core:base`에 두고, `NoticeHost`, router runtime
state, ActivityRoute pending queue/effect, permission/result adapter contract는
`:core:runtime` 또는 pure API에 둔다.

## Transition Policy

Do not start by moving all existing modules.

Order:

1. Add `assertions` where existing API boundary already exists.
2. Add new `:core:runtime` packages for new runtime contracts.
3. Move notice renderer out of `:core:designsystem` after the `:core:notice:api` contract is stable.
4. Decide design system migration separately. `:core:designsystem` can remain as a compatibility module until there is a real need to rename.
5. Split `:core:data` only after repository API pressure repeats across features or tests.

## Review Checklist

- Does the new module protect a caller-facing boundary?
- Can tests use the API without depending on implementation?
- Does `assertions` depend only on stable contracts?
- Does `core` stay Android/Compose-free except for already accepted transitional modules?
- Did the design avoid broad renames before behavior-preserving assertions are in place?
