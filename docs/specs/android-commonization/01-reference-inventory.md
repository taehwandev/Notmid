---
title: Android Commonization Pattern Inventory
audience: Android/product engineers and AI agents
purpose: 외부 Android 코드베이스에서 얻을 수 있는 일반 구조를 Notmid에 맞는 채택/제외 기준으로 정리한다.
status: draft
owner: notmid Android architecture
source_of_truth: Notmid repo docs and Tao Agent OS Android guidance
last_verified: 2026-06-28
applies_to: Android modularization planning
related_pages:
  - README.md
  - 02-target-module-taxonomy.md
  - 09-agent-example-packets.md
---

# Android Commonization Pattern Inventory

## Privacy Boundary

이 문서는 특정 외부 프로젝트의 이름, 로컬 경로, package id, class name,
generated namespace, credential, signing config, domain-specific dependency를
기록하지 않는다. 외부 Android 코드베이스에서 확인한 내용은 일반화된 모듈 경계와
검증 기준으로만 남긴다.

공통 규칙은 Tao Agent OS Android 문서에 둔다. Notmid 문서는 Notmid 모듈명,
제품 경계, 구현 순서만 가진다.

구현 agent가 실제 파일을 만들 수 있는지 검증해야 할 때는
[09-agent-example-packets.md](09-agent-example-packets.md)의 packet을 먼저
채운다. 이 inventory는 채택/제외 판단만 담당하고, 복사 가능한 예제와 중단
기준은 packet 문서가 담당한다.

## 확인한 Notmid 상태

Notmid Android 쪽 핵심 모듈:

```text
:app
:core:auth:api
:core:auth:impl
:core:base
:core:data
:core:designsystem
:core:domain
:core:model
:core:notice:api
:core:network:api
:core:network:assertions
:core:network:impl
:core:runtime
:core:router:api
:core:router:assertions
:core:router:impl
:feature:capture:api
:feature:capture:impl
:feature:feed:api
:feature:feed:impl
:feature:inbox:api
:feature:inbox:impl
:feature:map:api
:feature:map:impl
:feature:notmid:api
:feature:notmid:common
:feature:notmid:impl
:feature:profile:api
:feature:profile:impl
:feature:webview:api
:feature:webview:impl
```

이미 좋은 경계:

- feature API가 route/deep-link/event 계약을 소유한다.
- app과 runtime이 route graph, deep-link resolver, ActivityRoute launcher를
  실행한다.
- network는 `api`/`impl`로 분리되어 있고 transport implementation은 impl에
  있다.
- notice effect는 ViewModel에서 `Flow<NoticeEffect>`로 나가고 `:core:runtime`
  `NoticeHost`에서 렌더링한다.
- WebView는 ActivityRoute로 분리되어 있다.

현재 부족한 경계:

- `core/runtime`은 router/notice 중심으로 구현되었고 permission, WebView
  runtime package는 아직 reusable pressure가 작다.
- `assertions` 모듈군은 network/router 중심으로 시작되었고 runtime notice
  assertions는 아직 module-local test source 또는 future boundary다.
- 공통 server error envelope/presentation hint 계약은 network/data/domain/UI
  경계별 책임이 더 명확해야 한다.

## 채택할 일반 패턴

### API / Impl / Assertions

가장 중요한 패턴은 `api`/`impl`만 나누는 것이 아니라, 테스트 재사용을 위한
`assertions` 경계까지 함께 설계하는 것이다.

```text
:core:router:api
:core:router:impl
:core:router:assertions

:core:network:api
:core:network:impl
:core:network:assertions

:core:notice:api
:core:runtime
  notice host package
```

`assertions`는 production 구현이 아니라 테스트용 관찰 지점이다.

- 마지막 route plan.
- emit된 notice 목록.
- network request history.
- fake response queue.
- 호출 횟수와 마지막 builder state.

### Core / Runtime

- `core`: Android/Compose가 없어도 설명되는 계약, domain, model, route,
  network transport contract.
- `:core:runtime`: Android/Compose 앱에서만 의미 있는 runtime renderer,
  permission, WebView holder, lifecycle collector, notice host, resource resolver.

### Router Contract

Notmid는 Compose-first이므로 외부 코드베이스의 Activity/Fragment 중심 router
이름이나 구조를 복사하지 않는다.

채택할 점:

- caller-facing route contract는 feature API가 소유한다.
- product shell route registration과 `:core:runtime` coordinator가 실제 실행을
  결정한다.
- ActivityRoute를 ComposeRoute와 같은 route plan 안에서 표현한다.
- assertions 모듈에서 router 호출을 기록한다.

### Network Error Boundary

- raw transport/serialization exception은 impl/data boundary에 가둔다.
- server error envelope를 안전한 typed failure로 변환한다.
- retryable/recoverable/presentation hint를 공통 계약으로 만든다.
- ViewModel은 typed failure를 UI state/effect로 매핑한다.

### Notice Assertions

- `:core:notice:api` test source에서 contract tests를 제공한다.
- runtime assertions는 재사용 압력이 생기면 `:core:runtime` test source 또는
  별도 assertions 모듈로 분리한다.
- `Toast`, `Snackbar`, `Alert`, `Inline`, `FullPage`는 같은 notice contract의
  presentation variant로 두되 렌더러는 분리한다.
- feature 테스트는 Android `Toast`나 Compose `AlertDialog` 구현에 의존하지
  않는다.

### WebView Holder

- 현재 `feature:webview` ActivityRoute는 유지한다.
- WebView가 auth, file chooser, permission, fullscreen media, JavaScript bridge를
  확장하는 순간 reusable holder contract를 검토한다.
- URL allowlist와 JavaScript bridge policy는 API 계약보다 먼저 문서화한다.

## 제외할 것

Notmid에는 다음을 들여오지 않는다.

- DI/KSP 생성기를 구조 참고만으로 추가하는 것.
- Firebase, FCM, Crashlytics, ads, billing, domain-specific runtime dependency.
- 대규모 holder/repository hierarchy.
- Activity/Fragment/Service 중심 router API.
- `assertion` 단수 명명. Notmid는 `assertions`를 사용한다.
- 외부 프로젝트 package/id/name.

## Decision

다음 구현은 이미 도입된 router/network assertions와 notice/base/runtime 분리를
기준 패턴으로 삼는다. 새 공통화는 가장 낮은 위험의 assertions 또는 pure
contract boundary부터 시작하되, 구현 전에는 matching example packet이 있어야
한다.

이유:

- `:core:router:api`/`:core:router:impl`/`:core:router:assertions`가 기준
  선례다.
- assertions 모듈은 product behavior를 바꾸지 않고 테스트 관찰 지점을 만든다.
- AppRouter/AppDeepLinkResolver 테스트와 `:core:runtime` router runtime 테스트는
  새 runtime boundary 검증의 기준이다.
- 이후 WebView, notice, protected action navigation에도 같은 테스트 패턴을
  재사용할 수 있다.
