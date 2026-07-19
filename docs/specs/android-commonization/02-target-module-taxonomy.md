---
title: Target Module Taxonomy
audience: Android engineers and AI agents
purpose: Notmid의 `core`, `core/runtime`, feature, assertions 모듈 분류와 import 규칙을 정의한다.
status: draft
owner: notmid Android architecture
source_of_truth: docs/specs/android-commonization
last_verified: 2026-07-12
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

`core` module type 판단 기준은 Tao Agent OS
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

## Owner-First `api` / `impl` Family Grammar

`api`, `impl`, `assertions`는 최상위 분류가 아니라 같은 capability owner 아래의
역할 leaf다. Notmid의 Gradle path와 물리 폴더는 다음 문법을 따른다.

```text
:<family>:<owner>:<role>

family = core | feature
owner  = auth | network | router | feed | map | ...
role   = api | impl | assertions
```

예를 들어 `:core:auth:api`와 `:core:auth:impl`은 `auth`가 소유자이고,
`api`와 `impl`은 그 소유자 안의 공개 계약과 실행 역할이다. 물리 경로도
`core/auth/api`, `core/auth/impl`로 맞춘다. `core/api/auth`처럼 역할을 먼저
두면 한 capability의 계약, 구현, 테스트 지원이 떨어지고 소유권·리뷰 범위를
경로만으로 판단하기 어려우므로 사용하지 않는다.

역할별 책임:

- `api`: 다른 모듈이 구현 의존성 없이 컴파일해야 하는 route/event/value,
  repository port, provider contract 같은 caller-facing 계약만 소유한다.
- `impl`: 같은 owner의 `api`를 구현하며 screen, ViewModel, state, mapper,
  adapter, DI binding, SDK/platform 실행 세부사항을 소유한다.
- `assertions`: 같은 owner의 `api`에만 의존하는 reusable fixture, recording
  fake, builder, subject, matcher, contract test helper를 소유한다.

쌍 또는 trio는 대칭 모양을 맞추기 위해 만들지 않는다. 다음 중 하나라도
실제 압력이 있을 때만 `api` / `impl`을 분리한다.

- 다른 caller가 구현 의존성 없이 안정 계약만 import해야 한다.
- navigation, deep link, DI registration, replaceable implementation이 경계를 넘는다.
- 무거운 Android/Compose/SDK 의존성, 순환 의존성, build coupling을 격리한다.
- 계약과 구현을 서로 다른 테스트 또는 변경 소유권으로 검증해야 한다.

`assertions`는 두 개 이상의 테스트 경계가 helper를 공유하거나 contract
conformance를 재사용할 때만 추가한다. 한 구현 모듈의 테스트에서만 쓰는
helper는 그 `impl/src/test`에 둔다. 빈 `impl`, 빈 `assertions`, 사용자가 없는
`api`를 만들어 family 모양을 억지로 완성하지 않는다.

의도적으로 접힌 현재 예외도 불완전한 family로 보지 않는다.

- `:core:notice:api`는 순수 notice 계약만 소유한다. Android/Compose renderer는
  notice owner의 빈 `impl`이 아니라 실행 owner인 `:core:runtime`에 둔다.
- `:core:data`는 당분간 단일 구현 모듈이다. repository port는 이미
  `:core:domain`이 소유하므로 중복 `:core:data:api`를 만들지 않는다.
- `:core:runtime`, `:core:base`, `:core:designsystem`은 각각 runtime, app-shell
  template, visual system이라는 단일 owner다. 반복 caller/import/test 압력이
  생기기 전에는 package 경계로 유지한다.
- `:feature:notmid:common`은 Notmid product shell이 여러 feature impl 사이에서
  공유하는 product UI 경계다. 일반적인 cross-feature dependency bucket이 아니다.

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

Pure `core` module이 아닌 경우에는 Tao Agent OS 기준에 따라 이름이나 package에서
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
:<family>:<owner>:impl -> :<family>:<owner>:api
:<family>:<owner>:assertions -> :<family>:<owner>:api
test source -> :<family>:<owner>:assertions

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

:feature:notmid:impl -> selected feature:*:impl for product-shell composition
```

일반 consumer는 `api`에만 의존한다. `app`과 `:feature:notmid:impl`은 선택한
구현을 runtime graph에 조립하는 composition owner이므로 필요한 `impl`에
의존할 수 있다. 이 예외는 일반 feature impl 사이의 peer dependency를 허용하지
않는다.

Forbidden:

```text
core -> core:runtime
core -> app
core -> feature:*:impl
feature:*:api -> feature:*:impl
feature:*:api -> app
feature:*:api -> core:runtime implementation package
feature:*:impl -> another feature:*:impl except :feature:notmid:impl composition
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

Activity가 있는 host와 없는 host의 공통 기준은 Tao Agent OS
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
- Does the path place the named capability owner before `api`, `impl`, or `assertions`?
- Does every role leaf have a real caller, implementation, or reusable test boundary instead of completing an empty family shape?
- Can tests use the API without depending on implementation?
- Does `assertions` depend only on stable contracts?
- Does `core` stay Android/Compose-free except for already accepted transitional modules?
- Did the design avoid broad renames before behavior-preserving assertions are in place?
