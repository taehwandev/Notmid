---
title: Reference Android Project Inventory
audience: Android/product engineers and AI agents
purpose: 참조 Android 프로젝트 구조와 Notmid 현재 구조를 대조해 무엇을 차용하고 버릴지 정한다.
status: draft
owner: notmid Android architecture
source_of_truth: local Android reference plus Notmid repo docs
last_verified: 2026-06-16
applies_to: Android modularization planning
related_pages:
  - README.md
  - 02-target-module-taxonomy.md
---

# Reference Android Project Inventory

## Source Boundary

참조 Android 프로젝트는 로컬 참조 프로젝트로만 읽었다. Notmid 저장소 안으로 코드를 복사하거나, 새로 다운로드하거나, 벤더링하지 않는다.

스펙에서 참조 프로젝트 루트가 필요하면 절대 경로를 쓰지 말고 다음 placeholder를 사용한다.

```text
${REFERENCE_ANDROID_PROJECT_ROOT}
```

## 확인한 Notmid 상태

Notmid는 이미 단일 샘플 앱이 아니라 Android, Web, API, shared contracts가 있는 monorepo다.

현재 Android 쪽 핵심 모듈:

```text
:app
:core:auth:api
:core:auth:impl
:core:data
:core:designsystem
:core:domain
:core:model
:core:network:api
:core:network:impl
:core:router:api
:core:router:impl
:feature:*:api
:feature:*:impl
:feature:notmid:common
:feature:webview:api
:feature:webview:impl
```

이미 있는 좋은 경계:

- feature API가 route/deep-link/event 계약을 소유한다.
- app이 route graph, deep-link resolver, ActivityRoute launcher를 소유한다.
- network는 `api`/`impl`로 분리되어 있고 OkHttp는 impl에 있다.
- feedback effect는 ViewModel에서 `Flow<NotmidUiEffect>`로 나가고 design system handler에서 렌더링한다.
- WebView는 ActivityRoute로 분리되어 있다.

현재 부족한 경계:

- `core-app` 모듈군이 아직 없다.
- `assertions` 모듈군이 없다.
- `:core:designsystem`이 Android/Compose 앱 UI 런타임인데 `core` 아래 있다.
- `:core:model`이 순수 모델과 UI feedback/effect 계약을 함께 갖고 있다.
- network error는 transport 수준의 `NotmidNetworkException`과 repository별 domain failure로 나뉘지만, 공통 server error envelope/presentation hint 계약은 아직 약하다.
- feedback 렌더링은 `:core:designsystem` 안에 있고 toast/alert/snackbar/full-page를 독립적으로 테스트할 assertion boundary가 없다.

## 확인한 참조 Android 프로젝트 구조

참조 프로젝트는 매우 넓은 Android multi-module 구조다. Notmid가 그대로 가져오면 과하다.

핵심 계층:

```text
:core:*                       저수준 계약, lifecycle, router, secure, resource-provider, collector
:core-app:*                   Android app runtime, Compose, network, dialog, toast, permission, resources
:core-data:domain:*           domain/use case 계열
:core-data:repository:*:*     repository api/impl 계열
:feature:*:*                  화면/기능 구현
:feature:*:*:*-api            feature route/contract
:*-assertion                  테스트용 mock, recording fake, assertion helper
```

참조 프로젝트에서 확인한 대표 패턴:

- `core/router/router-api`, `router`, `router-assertion`.
- `core-app/network/network-api`, `network`, `network-assertion`.
- `core-app/network/exception/network-exception-api`, `network-exception`, `network-exception-assertion`.
- `core-app/toast/toast-api`, `toast`, `toast-assertion`.
- `core-app/dialog/dialog-api`, `dialog`, `dialog-assertion`.
- `feature/web-view/common/web-view-api`, `web-view`, `feature/web-view/holder/web-view-holder`.
- feature마다 `*-api`와 구현 모듈을 나누고, 앱 또는 router가 계약을 통해 이동한다.

## 차용할 것

### API / Impl / Assertions

참조 프로젝트의 가장 중요한 장점은 구현 의존성을 숨기는 것보다 테스트 경계를 같이 둔다는 점이다.

Notmid 적용:

```text
:core:router:api
:core:router:impl
:core:router:assertions

:core:network:api
:core:network:impl
:core:network:assertions

:core-app:feedback:api
:core-app:feedback:impl
:core-app:feedback:assertions
```

`assertions`는 production 구현이 아니라 테스트용 관찰 지점이다. 예를 들면:

- 마지막 route plan.
- emit된 feedback 목록.
- network request history.
- fake response queue.
- 호출 횟수와 마지막 builder state.

### Core / Core-App 구분

참조 프로젝트는 `core`와 `core-app`을 분리한다. Notmid도 이 원칙을 채택한다.

Notmid 해석:

- `core`: Android/Compose가 없어도 설명되는 계약, domain, model, route, network transport contract.
- `core-app`: Android/Compose 앱에서만 의미 있는 runtime renderer, permission, WebView holder, lifecycle collector, feedback host, resource resolver.

### Router Contract

참조 프로젝트의 `JourneyGuidance`는 Activity/Fragment/Service 시대의 계약이다. Notmid는 Compose-first이므로 그 이름과 구조를 복사하지 않는다.

차용할 점:

- caller-facing route contract는 feature API가 소유한다.
- app-level registry/coordinator가 실제 실행을 결정한다.
- ActivityRoute를 ComposeRoute와 같은 route plan 안에 둔다.
- assertions 모듈에서 router 호출을 기록한다.

### Network Error Boundary

참조 프로젝트는 network exception handler와 server error model을 공통화한다.

Notmid 적용:

- raw OkHttp/serialization exception은 impl/data boundary에 가둔다.
- server error envelope를 안전한 typed failure로 변환한다.
- retryable/recoverable/presentation hint를 공통 계약으로 만든다.
- ViewModel은 typed failure를 UI state/effect로 매핑한다.

### Toast/Dialog Assertions

참조 프로젝트의 toast/dialog assertion은 실제 Android UI를 띄우지 않고 builder 호출과 payload를 검증한다.

Notmid 적용:

- `core-app:feedback:assertions`에서 `RecordingFeedbackSink`, `FeedbackAssertions`를 제공한다.
- `Toast`와 `Alert`는 같은 feedback contract의 presentation variant로 두되 렌더러는 분리한다.
- feature 테스트는 Android `Toast`나 Compose `AlertDialog` 구현에 의존하지 않는다.

### WebView Holder Lessons

참조 프로젝트 WebView holder는 WebView state, navigator, file chooser, SSL error, new tab, JavaScript bridge를 따로 관리한다.

Notmid 적용:

- 현재 `feature:webview` ActivityRoute는 유지한다.
- WebView가 auth, file chooser, permission, fullscreen media, JS bridge를 확장하는 순간 reusable holder contract를 검토한다.
- URL allowlist와 JavaScript bridge policy는 API 계약보다 먼저 문서화한다.

## 버릴 것

Notmid에는 다음을 들여오지 않는다.

- Hilt/Dagger 모듈 생성기.
- KSP lifecycle/router generator.
- Firebase, FCM, Crashlytics, ads, billing, banking, secure keypad, app suit 의존성.
- 대규모 holder/repository hierarchy.
- Fragment/Service 중심 router API.
- `assertion` 단수 명명. Notmid는 `assertions`를 사용한다.
- reference project package/id/name.

## Notmid 변형 규칙

참조 프로젝트 패턴을 그대로 대입하지 말고 다음 변환을 적용한다.

| 참조 프로젝트 | Notmid 변형 |
| --- | --- |
| `JourneyGuidance` | route keys, route events, route plans |
| `ActivityRouter` | `AppRouteCoordinator` + `ActivityRouteLauncher`, 이후 `core-app:router` 후보 |
| `toast-api/toast/toast-assertion` | `core-app:feedback:api/impl/assertions` |
| `dialog-api/dialog/dialog-assertion` | `core-app:feedback`의 Alert renderer/assertions |
| `network-exception-*` | `core:network` typed failure + `core-app:feedback` presentation mapping |
| `web-view-holder` | 필요한 시점에 `core-app:webview` reusable holder |
| `feature-common/holder` | Notmid에서는 `feature:notmid:common` 또는 feature-local로 축소 |

## 스킬 문서와의 차이

현재 repo skill은 “특정 로컬 참조 프로젝트를 그대로 복사하지 말라”는 안전장치는 충분하다. 하지만 앞으로 구현할 `core-app`과 `assertions` 목표 구조는 아직 skill에 없다.

구현 후 업데이트해야 할 문서:

- `llm-wiki/module-map.md`: 새 모듈 ownership 반영.
- `llm-wiki/implementation-checklist.md`: assertions와 core-app 체크 추가.
- `.agents/skills/glassnavlab-android-stewardship/SKILL.md`: `core-app`/`assertions` route 추가.
- `.agents/skills/notmid-product-engineering/SKILL.md`: feedback/network/WebView event 처리 기준 반영.

## Decision

첫 구현은 router 또는 feedback 중 하나로 시작한다.

추천은 router다.

이유:

- 이미 `:core:router:api`/`:core:router:impl`이 있다.
- `assertions` 모듈을 추가해도 product behavior가 바뀌지 않는다.
- AppRouter/AppDeepLinkResolver 테스트가 이미 있어 검증 경로가 명확하다.
- 이후 WebView, feedback, protected action navigation에도 같은 테스트 패턴을 재사용할 수 있다.
