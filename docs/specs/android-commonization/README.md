---
title: Android Commonization Spec Index
audience: Android/product engineers and AI agents
purpose: Android 공통화 패턴을 Notmid에 맞게 분해해 이후 구현 작업 단위로 전환한다.
status: draft
owner: notmid Android architecture
source_of_truth: docs/specs/android-commonization
last_verified: 2026-06-28
applies_to: Android Gradle modules, Compose app shell, router, network, notice, WebView, tests
related_pages:
  - llm-wiki/module-map.md
  - llm-wiki/routing-deeplinks.md
  - docs/specs/notmid-router-architecture.md
  - docs/specs/notmid-monorepo-platform.md
  - docs/specs/android-commonization/09-agent-example-packets.md
---

# Android Commonization Spec Index

이 문서 묶음은 Android 공통화 패턴을 Notmid의 최신 Compose 구조로 다시 설계한 로컬 한국어 스펙이다. 목표는 외부 코드를 옮기는 것이 아니라, 이후 구현을 작은 PR 또는 커밋 단위로 나눌 수 있게 `core`, `core/runtime`, feature, `api`/`impl`/`assertions` 경계를 먼저 확정하는 것이다.

## 결론

Notmid에는 외부 Android 코드베이스 구조를 그대로 적용하지 않는다.

채택할 것은 다음 세 가지다.

- 호출자는 안정 계약만 보고 구현은 모르게 하는 `api`/`impl` 경계.
- 테스트가 구현 모듈이나 앱 쉘을 끌어오지 않게 하는 `assertions` 경계.
- 재사용 Compose Activity base는 `:core:base`로 분리하고, router/notice 같은 Android/Compose 앱 런타임 실행 부품은 `:core:runtime`에 두며, 순수 계약과 도메인은 `core`에 남기는 경계.

버릴 것은 다음이다.

- 참고 프로젝트를 그대로 따라 넣는 광고, 은행/카드/결제, Firebase 런타임 의존성, 대규모 holder 계층.
- DI는 현재 Hilt/KSP를 Notmid 기준으로 채택하되, Gradle 설정은 `build-logic` 컨벤션으로만 적용하고 product binding은 app/runtime/feature impl에 둔다.
- `Success`/`Failure` 래퍼를 모든 suspend 네트워크 호출에 씌우는 방식.
- 한 번에 `core:designsystem` 전체를 옮기는 큰 rename.
- feature마다 무조건 `assertions`를 만드는 모듈 복제.

## 문서 분리

| 문서 | 역할 | 다음 구현 단위 |
| --- | --- | --- |
| [01-reference-inventory.md](01-reference-inventory.md) | Notmid/Tao Agent OS 기준으로 정리한 Android 공통화 패턴과 제외 기준 | 적용 대상과 제외 대상 확정 |
| [02-target-module-taxonomy.md](02-target-module-taxonomy.md) | `core`, `core/runtime`, feature, data, assertions 모듈 분류 | `settings.gradle.kts` 목표 모듈 목록 설계 |
| [03-build-logic-module-templates.md](03-build-logic-module-templates.md) | Gradle convention과 모듈 템플릿 | `assertions` convention 추가 여부 결정 |
| [04-router-webview-contract.md](04-router-webview-contract.md) | 라우터, 딥링크, ActivityRoute, WebView 계약 | 라우터 assertions와 WebView hardening |
| [05-network-error-contract.md](05-network-error-contract.md) | 네트워크 클라이언트, 서버 오류, 공통 오류 모델 | network API/impl/assertions 개편 |
| [06-notice-alert-toast-contract.md](06-notice-alert-toast-contract.md) | toast/snackbar/alert/full-page notice 공통화 | `:core:notice:api` 계약과 `:core:runtime` host 패키지 |
| [07-state-assertions-testing.md](07-state-assertions-testing.md) | ViewModel/state/reducer 테스트와 assertions 모듈 규칙 | 테스트 대역/검증 DSL 도입 |
| [08-implementation-work-breakdown.md](08-implementation-work-breakdown.md) | 실제 적용 순서, acceptance, 검증 명령 | 작업 티켓/커밋 분리 기준 |
| [09-agent-example-packets.md](09-agent-example-packets.md) | AI agent가 참고 코드 없이 구현 가능한 수준의 예제 packet과 중단 기준 | 구현 전 예제/검증 입력으로 사용 |

## 문서 중복 관리

공통 Android 원칙은 Tao Agent OS Android 문서가 가진다. Notmid 문서는 그
원칙을 다시 길게 복사하지 않고, Notmid에만 필요한 다음 정보를 남긴다.

- 실제 Notmid Gradle module 이름.
- Notmid에서 이미 구현된 기준 파일 또는 아직 없는 target file sketch.
- allowed imports / forbidden imports.
- 첫 caller 또는 test.
- nearest verification command.
- 구현하지 말고 접어야 하는 collapse rule.

어떤 문단이 Notmid module, file sketch, acceptance, verification 중 하나도
말하지 못하면 Tao Agent OS 공통 규칙으로 옮기거나 삭제한다. 반대로 AI agent가
문단만 읽고 파일을 만들 수 없다면 [09-agent-example-packets.md](09-agent-example-packets.md)에
예제 packet을 먼저 추가한다.

## 현재 Tao Agent OS/문서 반영 상태

이미 반영된 내용:

- Tao Agent OS Android cards hold shared module, ViewModel, router/runtime,
  WebView, security, and testing guidance.
- `llm-wiki/module-map.md`는 현재 구현된 `:core:base`, `:core:runtime`, `:core:notice:api`, router/network `assertions` 모듈을 repo inventory로 기록한다.
- Notmid는 repo-local skill docs를 두지 않는다. Notmid-specific facts are kept
  in `llm-wiki` and `docs/specs`; reusable rules stay in Tao Agent OS.

아직 부족한 내용:

- permission runtime과 WebView holder는 reusable pressure가 생기는 시점에 더 구체적인 runtime package 계약이 필요하다.
- server error envelope/presentation hint는 network/data/domain/UI 경계별 책임을 더 구현으로 검증해야 한다.
- 한국어 planning spec은 로컬 planning history로 유지할 수 있다. 외부 공유가
  필요하면 Tao Agent OS 또는 공개 문서 기준으로 영어 canonical 문서를 만든다.

따라서 이 스펙은 구현과 문서화를 맞추는 작업 기준이다. 구현이 바뀌면
`llm-wiki/module-map.md`와 관련 `docs/specs`를 갱신하고, reusable guidance가
바뀐 경우 Tao Agent OS shared cards도 함께 갱신한다.

## 대상 구조 원칙

```text
:core:*             순수 Kotlin 계약, 도메인, 라우터/네트워크 핵심 계약, 테스트 가능한 fake/assertion
:core:base          Compose 전용 BaseActivity, AppRoot, edge-to-edge, pending deep-link handoff
:core:runtime       Android/Compose 실행 런타임, router bundle/runtime, notice, permission, WebView runtime
:feature:*:api      feature route/event/entry contract
:feature:*:impl     Compose screen, ViewModel/state owner, feature-local UI
:feature:*:assertions
                    feature 계약을 여러 테스트가 공유할 때만 추가
:app                MainActivity screen host, Hilt runtime config/repository/auth wiring, product content binding, concrete platform launch binding
```

## 적용 순서 요약

1. 문서 기준 확정: 이 디렉터리의 스펙을 구현 기준으로 삼는다.
2. `assertions` convention과 naming을 먼저 정한다.
3. 가장 위험이 낮은 `core:router:assertions`부터 만든다.
4. network error 모델과 network assertions를 추가한다.
5. notice/toast/alert 렌더러를 `:core:runtime` 내부 notice 패키지로 분리한다.
6. WebView는 현재 `feature:webview`를 유지하되, reusable holder가 필요해지는 시점에 `:core:runtime` 내부 webview 패키지로 뺀다.
7. 앱 shell/base는 `:core:base`의 좁은 Compose `BaseActivity` 템플릿으로 분리하고, router/notice 실행 부품은 `:core:runtime`의 `AppRoot`/`RouteCoordinator`/`NoticeHost` 조합으로 연결한다.

## 중단 조건

다음 상황이면 구현을 시작하지 않고 스펙을 다시 조정한다.

- 새 모듈이 단 하나의 호출자만 가지고 안정 계약도 없다.
- `assertions` 모듈이 구현 모듈에 의존해야만 동작한다.
- `core` 모듈이 Android `Context`, `Activity`, Compose UI 타입, OkHttp 구현 타입, Firebase 타입을 안정 계약으로 노출한다.
- notice 공통화가 feature별 copy, analytics, route decision을 삼킨다.
- WebView 공통화가 URL allowlist, 파일 chooser, permission, JavaScript bridge 보안을 문서화하지 않는다.
- 구현하려는 agent가 target boundary, file sketch, allowed imports, forbidden
  imports, first caller/test, verification, collapse rule을 한 번에 말하지 못한다.

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
