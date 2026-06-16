---
title: Android Commonization Spec Index
audience: Android/product engineers and AI agents
purpose: Reference Android project structure를 Notmid에 맞게 분해해 이후 구현 작업 단위로 전환한다.
status: draft
owner: notmid Android architecture
source_of_truth: docs/specs/android-commonization
last_verified: 2026-06-16
applies_to: Android Gradle modules, Compose app shell, router, network, feedback, WebView, tests
related_pages:
  - llm-wiki/module-map.md
  - llm-wiki/routing-deeplinks.md
  - docs/specs/notmid-router-architecture.md
  - docs/specs/notmid-monorepo-platform.md
---

# Android Commonization Spec Index

이 문서 묶음은 참조 Android 프로젝트 구조에서 배울 수 있는 경계를 Notmid의 최신 Compose 구조로 다시 설계한 로컬 한국어 스펙이다. 목표는 코드를 바로 옮기는 것이 아니라, 이후 구현을 작은 PR 또는 커밋 단위로 나눌 수 있게 `core`, `core-app`, feature, `api`/`impl`/`assertions` 경계를 먼저 확정하는 것이다.

## 결론

Notmid에는 참조 프로젝트 구조를 그대로 적용하지 않는다.

채택할 것은 다음 세 가지다.

- 호출자는 안정 계약만 보고 구현은 모르게 하는 `api`/`impl` 경계.
- 테스트가 구현 모듈이나 앱 쉘을 끌어오지 않게 하는 `assertions` 경계.
- Android/Compose 앱 런타임 공통 요소를 단일 `core-app` 모듈로 빼고, 순수 계약과 도메인은 `core`에 남기는 경계.

버릴 것은 다음이다.

- Hilt, KSP 생성기, 광고, 은행/카드/결제, Firebase 런타임 의존성, 대규모 holder 계층.
- `Success`/`Failure` 래퍼를 모든 suspend 네트워크 호출에 씌우는 방식.
- 한 번에 `core:designsystem` 전체를 옮기는 큰 rename.
- feature마다 무조건 `assertions`를 만드는 모듈 복제.

## 문서 분리

| 문서 | 역할 | 다음 구현 단위 |
| --- | --- | --- |
| [01-reference-inventory.md](01-reference-inventory.md) | 참조 프로젝트/Notmid/스킬 문서에서 확인한 구조와 차용 기준 | 적용 대상과 제외 대상 확정 |
| [02-target-module-taxonomy.md](02-target-module-taxonomy.md) | `core`, `core-app`, feature, data, assertions 모듈 분류 | `settings.gradle.kts` 목표 모듈 목록 설계 |
| [03-build-logic-module-templates.md](03-build-logic-module-templates.md) | Gradle convention과 모듈 템플릿 | `assertions` convention 추가 여부 결정 |
| [04-router-webview-contract.md](04-router-webview-contract.md) | 라우터, 딥링크, ActivityRoute, WebView 계약 | 라우터 assertions와 WebView hardening |
| [05-network-error-contract.md](05-network-error-contract.md) | 네트워크 클라이언트, 서버 오류, 공통 오류 모델 | network API/impl/assertions 개편 |
| [06-feedback-alert-toast-contract.md](06-feedback-alert-toast-contract.md) | toast/snackbar/alert/full-page feedback 공통화 | `core-app` 내부 feedback 패키지 |
| [07-state-assertions-testing.md](07-state-assertions-testing.md) | ViewModel/state/reducer 테스트와 assertions 모듈 규칙 | 테스트 대역/검증 DSL 도입 |
| [08-implementation-work-breakdown.md](08-implementation-work-breakdown.md) | 실제 적용 순서, acceptance, 검증 명령 | 작업 티켓/커밋 분리 기준 |

## 현재 스킬 문서 반영 상태

이미 반영된 내용:

- `.agents/skills/glassnavlab-android-stewardship/SKILL.md`는 특정 로컬 참조 프로젝트를 구조 참고용으로만 보고 Firebase/Hilt/광고/뱅킹 의존성은 가져오지 말라고 명시한다.
- `.agents/skills/android-mixed-activity-compose-router/SKILL.md`는 feature API route/event, app-level route graph, ActivityRoute, WebView Activity 경계를 설명한다.
- `.agents/skills/notmid-product-engineering/SKILL.md`는 Notmid feature API/impl, route event, WebView ActivityRoute, Firebase-safe open-source 방향을 설명한다.

부족한 내용:

- `core`와 `core-app`을 Notmid의 장기 목표 모듈군으로 나누는 기준.
- `assertions` 모듈을 언제 만들고 무엇을 담는지에 대한 공식 규칙.
- network error, feedback, permission, WebView holder 같은 앱 런타임 공통화를 최신 Compose 방식으로 나누는 순서.
- 기존 `:core:designsystem`, `:core:model`에 들어간 feedback/effect 계약을 어떻게 `core-app`으로 점진 이전할지.

따라서 이 스펙은 구현 전 임시 기준이다. 구현이 시작되면 `llm-wiki/module-map.md`, `llm-wiki/implementation-checklist.md`, 관련 skill 문서를 영어 canonical 문서로 갱신해야 한다.

## 대상 구조 원칙

```text
:core:*             순수 Kotlin 계약, 도메인, 라우터/네트워크 핵심 계약, 테스트 가능한 fake/assertion
:core-app           Android/Compose 앱 런타임 공통 요소 단일 모듈, router bundle/runtime, feedback, permission, WebView runtime, app shell helper
:feature:*:api      feature route/event/entry contract
:feature:*:impl     Compose screen, ViewModel/state owner, feature-local UI
:feature:*:assertions
                    feature 계약을 여러 테스트가 공유할 때만 추가
:app                MainActivity, runtime config injection, pending deep-link handoff, concrete platform launch binding
```

## 적용 순서 요약

1. 문서 기준 확정: 이 디렉터리의 스펙을 구현 기준으로 삼는다.
2. `assertions` convention과 naming을 먼저 정한다.
3. 가장 위험이 낮은 `core:router:assertions`부터 만든다.
4. network error 모델과 network assertions를 추가한다.
5. feedback/toast/alert 렌더러를 `core-app` 내부 feedback 패키지로 분리한다.
6. WebView는 현재 `feature:webview`를 유지하되, reusable holder가 필요해지는 시점에 `core-app` 내부 webview 패키지로 뺀다.
7. 앱 shell/base는 BaseActivity 상속 구조가 아니라 Compose `AppRoot`/`AppEnvironment`/`RouteCoordinator`/`FeedbackHost` 조합으로 분리한다.

## 중단 조건

다음 상황이면 구현을 시작하지 않고 스펙을 다시 조정한다.

- 새 모듈이 단 하나의 호출자만 가지고 안정 계약도 없다.
- `assertions` 모듈이 구현 모듈에 의존해야만 동작한다.
- `core` 모듈이 Android `Context`, `Activity`, Compose UI 타입, OkHttp 구현 타입, Firebase 타입을 안정 계약으로 노출한다.
- feedback 공통화가 feature별 copy, analytics, route decision을 삼킨다.
- WebView 공통화가 URL allowlist, 파일 chooser, permission, JavaScript bridge 보안을 문서화하지 않는다.

## 검증 기준

문서-only 단계:

```bash
git diff --check
vibeguard audit .
agent-finish-check.py
```

구현 단계:

```bash
./gradlew help
./gradlew :app:compileDebugKotlin
./gradlew test
```

영역별 구현 문서는 각 파일의 `Verification` 섹션을 따른다.
