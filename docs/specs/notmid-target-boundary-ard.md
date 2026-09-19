---
title: notmid Target Boundary ARD
audience: Android engineers, web/API engineers, and AI agents
purpose: Notmid의 목표 repository 경계와 Android 모듈 taxonomy(api/ui/impl), 라우트·상태 소유권을 확정한다.
status: accepted
owner: notmid architecture
source_of_truth: docs/specs/notmid-target-boundary-ard.md
last_verified: 2026-09-12
applies_to: settings.gradle.kts, Gradle modules, package ownership, pnpm workspace, CI
supersedes:
  - docs/specs/android-commonization/02-target-module-taxonomy.md (Feature Impl, core/runtime, Base/App Shell Rule, Import Direction)
  - docs/specs/notmid-monorepo-platform.md (Repository Layout, Ownership)
  - docs/specs/notmid-router-architecture.md (Modules, Current Route Event Wiring)
related_pages:
  - docs/specs/android-commonization/README.md
  - docs/specs/android-commonization/02-target-module-taxonomy.md
  - docs/specs/notmid-router-architecture.md
  - docs/specs/notmid-monorepo-platform.md
  - llm-wiki/module-map.md
  - llm-wiki/routing-deeplinks.md
---

# notmid Target Boundary ARD

## Status

Accepted (2026-09-12). 이 문서가 목표 경계의 source of truth다.

진행 상황은 [Migration Order](#migration-order)의 Status 열이 기록한다. 단계 0은
Open Decision 3(새 repository 이름과 히스토리 처리)이 답해질 때까지 시작하지 않는다.

## Context

현재 구조는 두 가지 이유로 목표와 어긋나 있다.

1. **최신 Tao Agent OS Android 가이드가 바뀌었다.** `platforms/android/skills/android-module-structure`는
   이제 Compose feature를 `ui`가 소유하고 `impl`은 Activity/Intent/manifest 같은
   플랫폼 진입 전용이라고 규정한다. Notmid는 `impl`을 Compose 모듈로 쓰고 있다.
2. **코드가 자기 스펙도 만족하지 못한다.** `02-target-module-taxonomy.md`의 Feature Impl
   규칙은 이미 "direct dependency on another feature impl" 금지와
   "ViewModel/state/effects/actions 소유"를 요구하는데, 현재 코드는 둘 다 위반한다.

즉 이번 변경은 새 유행을 좇는 것이 아니라, 한 번도 도달하지 못한 목표 경계를
최신 가이드에 맞춰 다시 확정하는 작업이다.

## Evidence

측정 기준일: 2026-09-12, 브랜치 `refactor/core-module-structure`.

### E1. 라우트 엔진이 세 벌이고 하나는 죽어 있다

| 구현 | 위치 | 상태 |
| --- | --- | --- |
| Android | `core/router/impl` + `feature/*/api/deeplink/*DeepLinkSpec.kt` + `NotmidRouteGraph` | 사용 중 |
| Web | `packages/contracts/src/routes.ts` `resolveNotmidPathStack()` | 사용 중 |
| Server | `apps/api/src/server.ts:387` `GET /v1/deeplinks/resolve` | web만 사용 |

Android은 `core/network/api/.../NotmidApiPaths.kt:13`에 `DEEPLINK_RESOLVE` 상수를
선언하지만 호출부가 없다. 세 구현의 일치를 검증하는 테스트도 없다.

### E2. 앱 셸이 feature 모듈 안에 있다

`feature/notmid/impl/build.gradle.kts`는 `capture/feed/inbox/map/profile`의
`impl` 5개에 모두 의존한다. `NotmidRouteGraph.kt`는 전 feature의 Route와
DeepLinkSpec을 직접 import하고, 딥링크 호스트 상수(`thdev.app`, `notmid`)까지
이 모듈에 하드코딩되어 있다. `:app`이 이 feature에 의존하므로 의존 방향이 역전이다.

`notmid-router-architecture.md`의 Direction은 이미 "app-level router gathers those
contracts"라고 적고 있다. 코드가 문서에서 이탈한 것이다.

### E3. feature API가 sibling feature API에 의존한다

`feature/{capture,feed,inbox,map,profile}/api/build.gradle.kts`가 모두
`api(project(":feature:notmid:api"))`를 선언한다. 그런데 `feature:notmid:api`가
가진 것은 `NotmidRoute`, `NotmidTopLevelRoute`, `NotmidDestinationIds`,
`NotmidStaticDeepLinkSpec` — feature 계약이 아니라 **앱 라우트 계약**이다.

### E4. ViewModel이 앱 전체에 하나뿐이다

`grep -rl ViewModel core feature app` 결과 프로덕션 ViewModel은
`app/.../NotmidAppViewModel.kt`(387줄) 하나다. 이 하나가 5개 탭 콘텐츠 로딩,
인증 플로우, 보호된 쓰기(capture/clip/chat/profile), notice 효과, 딥링크 문자열
생성을 모두 소유한다. feature 모듈의 ViewModel과 UiState 소유자는 0개다.

결과적으로 `feature/notmid/impl/.../NotmidRouteContent.kt`는 파라미터 25개짜리
단일 dispatcher가 되었고, 모든 화면이 셸 개념인 `destination`과 **공유
`listState`**를 주입받는다. 화면 하나를 추가하려면 `NotmidAppViewModel`,
`NotmidContentUiState`, `NotmidRouteContent`, `NotmidRouteGraph` 네 파일을 동시에
고쳐야 한다.

### E5. 도메인 모델이 두 벌이다

> **정정 (2026-09-12).** 최초 작성 시 `core/model/notmid/`와
> `feature/notmid/common/model/`에 "같은 개념이 12쌍 중복"이라고 적었는데, 이는
> **파일 이름만 대조한 결과**였다. 3단계 착수 전에 내용을 대조해보니 12쌍 중
> **10쌍만 진짜 중복**이고 나머지는 정당한 도메인→UI 경계였다. 아래가 정확한 분류다.

**진짜 중복 10 — 필드까지 동일하고 존재 이유가 없다.**
`NotmidThread`, `NotmidThreadMessage`, `NotmidThreadMessageAttachment`(core의
`NotmidMessageAttachment`와 구조 동일, 이름만 다름), `NotmidCaptureDraft`,
`NotmidChatAccess`, `NotmidGeoPoint`, `NotmidCaptureVisibility`,
`NotmidCaptureMediaState`, `NotmidChatInviteStatus`, `NotmidChatRelationship`.

**정당한 UI 모델 3 — Compose 타입을 쓰므로 도메인 모델이 될 수 없다.**

| 타입 | core:model | common (UI) |
| --- | --- | --- |
| `NotmidClip` | `badge: String`, `palette: List<NotmidColor>` | `badge: NotmidBadge`, `palette: List<Color>` |
| `NotmidPlace` | `heightDp: Int`, `contentColor: NotmidColor` | `height: Dp`, `contentColor: Color` |
| `NotmidDestination` | 도메인 clip/place를 담음 | UI clip/place를 담음 |

`NotmidUiModelMappers.kt`가 둘을 잇는데, `import ... as NotmidClipModel`처럼
**별칭 import로 이름 충돌을 피하고 있다는 점 자체가** 이 구조의 냄새였다. 매퍼
168줄 중 약 110줄이 동일 타입 간 항등 변환이었다.

`feature:notmid:common`은 전 feature가 의존하는 UI+모델 공용 버킷이다.

### E6. core 네임스페이스가 모호하고 방향이 역전되어 있다

- `core:base`, `core:runtime`은 최신 가이드가 명시적으로 중단 대상으로 지목한 이름이다.
- `core/runtime/router/config/`에 `AppRouterBundle`, `AppDeepLinkUrlConfig`,
  `AppRoutePlanner` 등 **앱 정책**이 들어 있다. core가 app을 알고 있다.
- `core:notice:api`에 `NoticeEffectViewModel.kt`가 있다(api 모듈의 ViewModel).
- `core:model`에 `ChannelNotmidActionDelegate.kt`가 있다(코루틴 채널 런타임, 모델 아님).
- `core:data`에 `StaticNotmidContentRepository.kt`(451줄),
  `StaticNotmidProtectedWriteRepository.kt`(315줄) 등 fixture가 프로덕션 모듈에 동거한다.

### E7. 웹/서버는 같은 레포에 있지만 빌드 결합은 없다

| | Android | Web/API |
| --- | --- | --- |
| 추적 파일 | 437 | 81 |
| 해당 영역을 건드린 커밋 | 35 | 8 (전체 57) |

Gradle이 `packages/contracts`를 읽지 않고 pnpm이 Kotlin을 읽지 않는다. 실제 결합은
네 곳뿐이다: `.github/workflows/ci.yml`(한 파이프라인에서 JDK+Android SDK+Node+pnpm
설치), `scripts/verify-local.sh`(`./gradlew test` 다음 줄에 `pnpm install`),
`docs`/`scripts`/`llm-wiki` 공유, 그리고 E1의 수기 복제된 계약.

---

## Decision 1 — 웹/서버는 별도 repository로 추출한다

`apps/web`, `apps/api`, `packages/contracts`, `packages/api-client`와 이들에만
해당하는 스크립트·CI를 새 repository로 옮긴다. 이 repository는 순수 Android
Gradle 프로젝트가 된다.

**근거.** 빌드 결합이 없고(E7), 웹/서버 커밋이 8개뿐이라 히스토리 보존 비용이
낮으며, 현재는 Android CI가 Node/pnpm 설치와 API smoke를 기다린다. 두 제품의
릴리스 주기도 다르다.

**두 레포의 유일한 계약 seam은 HTTP다.**

- `apps/api`가 OpenAPI 스펙을 산출물로 발행한다.
- Android은 그 스펙의 고정 사본을 저장하고, `NotmidApiPaths`와 DTO가 사본과
  일치하는지 테스트로 검증한다.
- 딥링크 URL 문법(`https://thdev.app/notmid/...`)은 양쪽이 공유하는 계약이며,
  스펙 문서에 단일 정의를 둔다.

**Android은 TypeScript 소스를 소비하지 않는다.** `llm-wiki/module-map.md`가 이미
세운 규칙을 유지한다.

## Decision 2 — Compose feature는 `ui`가 소유하고 `impl`은 플랫폼 진입 전용이다

`feature:<name>:impl`(Compose) → `feature:<name>:ui`로 재분류한다.

현재 9개 `impl` 중 플랫폼 진입 자격을 갖춘 것은 `feature:webview:impl`
(`NotmidWebViewActivity` 보유) 하나다. 나머지는 모두 `ui`가 된다.

`ui` 모듈은 holder `Route`, `ViewModel`, `UiState`/`Action`/`Effect`, stateless
`Screen`, UI mapper, feature-local component, preview, UI/ViewModel 테스트를
소유한다. `impl` 없이 컴파일·렌더·테스트가 가능해야 한다.

## Decision 3 — 라우트 계약은 core가, 라우트 그래프는 app이 소유한다

- `feature:notmid:api` → `core:navigation:api`로 이동한다. `NotmidRoute`,
  `NotmidTopLevelRoute`, `NotmidDestinationIds`, `NotmidStaticDeepLinkSpec`은
  앱 라우트 계약이지 feature 계약이 아니다. 이것으로 E3의 feature→feature 엣지가
  사라진다.
- `core:router:{api,impl,assertions}` → `core:navigation:{api,impl,assertions}`로
  개명해 `core:navigation:api`와 한 owner 아래 모은다.
- `NotmidRouteGraph`, 라우트 이벤트 핸들러, 딥링크 호스트 정책, `NotmidRouteContent`
  dispatcher는 `:app`으로 옮긴다. `core/runtime/router/config`의 `App*` 정책 타입도
  함께 `:app`으로 올린다(E6의 방향 역전 해소).
- `feature:notmid:impl`은 소멸한다. 로그인 화면은 `feature:auth:ui`로, 셸 화면과
  네비게이션은 `:app`으로 간다.

**딥링크 해석 중복(E1)은 이 ARD의 범위가 아니다.** Android이 자체 해석을 유지할지
서버 `/v1/deeplinks/resolve`로 수렴할지는 Open Decision 1로 남긴다. 다만 죽은 상수
`NotmidApiPaths.DEEPLINK_RESOLVE`는 결정 전까지 제거한다.

## Decision 4 — 화면 상태는 feature `ui`가 소유한다

`NotmidAppViewModel`을 feature별 ViewModel로 분해한다: `FeedViewModel`,
`MapViewModel`, `CaptureViewModel`, `InboxViewModel`, `ProfileViewModel`,
`ChatThreadViewModel`, `ClipDetailViewModel`, `PlaceDetailViewModel`.

`:app`에는 앱 수준 관심사만 남는다: 인증/세션 상태, 라우트 조정, notice 호스트.

각 `ui` 모듈은 자기 `UiState`/`Action`/`Effect`를 소유하고 안정적인 repository
포트에만 의존한다. 셸 개념(`destination`, 공유 `listState`)은 화면 파라미터에서
제거한다.

## Decision 5 — core 네임스페이스를 capability 이름으로 정리한다

- `core:base` 해체 → `core:activity`(BaseActivity, EdgeToEdge, AppRoot).
- `core:runtime` 해체 → 라우트 정책은 `:app`, `NoticeHost` 렌더링은 `core:notice:ui`,
  ActivityRoute launcher는 `core:activity`.
- `core:notice:api`의 `NoticeEffectViewModel` → `core:notice:ui`.
- `core:model`의 `ChannelNotmidActionDelegate` → `core:activity` 또는 `:app`.
- `core:data` → `core:data:{api,impl,assertions}`. `Static*` fixture repository는
  `assertions`로 이동한다.
- `feature:notmid:common`은 **해체하지 않고 정리한다**(아래 정정 참조).

> **정정 (2026-09-12).** 최초 작성 시 "`feature:notmid:common` 해체 → 도메인 무관
> 컴포넌트는 `core:designsystem`, 제품 특화 컴포넌트와 매퍼는 각 feature `ui`로
> 분산"이라고 적었다. 3단계에서 실제 사용처를 재보니 **이 계획은 실행 불가능하고
> 스킬에도 어긋난다**:
>
> - 살아 있는 컴포넌트는 여러 feature가 공유한다. `NotmidRouteDetailContent`는
>   feed/ui와 map/ui가, `NotmidGlassIcon`은 feed/ui와 notmid/impl이 쓴다. 이를 한
>   feature `ui`로 옮기면 금지 엣지인 `feature:*:ui -> feature:*:ui`가 생긴다.
> - 이들은 도메인 무관 프리미티브가 아니라 제품 카드/헤더다. `android-module-structure`의
>   `module-layout.md`는 "feature 제품 카드, 화면 헤더, 도메인→UI 매핑은 design
>   system이 아니라 feature 또는 feature-common 모듈에 둔다"고 명시하고, 동시에
>   "여러 feature owner가 공유하는 제품 UI 패턴에는 feature-common 모듈을 쓴다"고
>   허용한다.
>
> 따라서 이 모듈은 남되, 중복 모델(E5의 10개)과 죽은 코드를 덜어내고 소유 범위를
> 좁힌다. 정리 후 소유물은 UI 모델 3종(`NotmidClip`, `NotmidPlace`,
> `NotmidDestination`), 그 매퍼, 그리고 공유 제품 컴포넌트 4종이다.
>
> 남은 문제는 이름이다. `common`은 스킬이 지목한 모호한 버킷 이름이고, 무엇보다
> UI 모델 3종이 `core:model`과 **이름이 같아서** 매퍼가 별칭 import를 써야 한다.
> 모듈명과 타입명 정리는 Open Decision 5로 남긴다.

---

## Target Tree

```text
app/                            셸, 라우트 그래프, 딥링크 정책, DI 조립, 인증/세션
core/
  activity/                     BaseActivity, EdgeToEdge, AppRoot, ActivityRoute launcher
  designsystem/
  model/                        도메인 모델 단일 소스
  domain/
  data/{api,impl,assertions}
  network/{api,impl,assertions}
  navigation/{api,impl,assertions}
  notice/{api,ui}
  auth/{api,impl}
feature/
  auth/{api,ui}                 로그인 화면
  feed/{api,ui}
  map/{api,ui}
  capture/{api,ui}
  inbox/{api,ui}
  profile/{api,ui}
  webview/{api,impl}            유일하게 정당한 impl
```

## Import Direction

```text
app            -> feature *:api + feature *:ui + feature webview:impl + core *
feature *:ui   -> 자기 api + core:navigation:api + core:designsystem
                  + core:model + core:domain + core:data:api + core:notice:api
feature *:api  -> core:navigation:api + core:model
webview:impl   -> 자기 api + 자기 ui + core:activity
core:*:impl    -> 같은 owner의 api
core:*:assertions -> 같은 owner의 api
```

금지 엣지:

- `feature:*:api -> feature:*:ui`
- `feature:*:api -> 다른 feature:*:api`
- `feature:*:ui -> 다른 feature:*:ui`
- `feature:*:ui -> feature:*:impl`
- `core:* -> feature:*`
- `core:* -> app 정책 타입`
- `app -> core:data:impl` 내부 (DI 바인딩 제외)

## Current → Target Mapping

| 현재 | 목표 | 비고 |
| --- | --- | --- |
| ~~`:feature:notmid:api`~~ | `:core:navigation:api` | 완료, E3 해소 |
| ~~`:core:router:{api,impl,assertions}`~~ | `:core:navigation:{api,impl,assertions}` | 완료 |
| `:feature:notmid:impl` 셸·라우터 | `:app` | 모듈 소멸 |
| `:feature:notmid:impl` 로그인 | `:feature:auth:ui` | |
| ~~`:feature:notmid:common` 중복 모델 10종~~ | `:core:model` | 완료; UI 모델 3종은 의도적으로 잔류 |
| `:feature:notmid:common` 컴포넌트 | 모듈에 잔류 | 여러 feature가 공유하므로 이동 불가, Decision 5 정정 참조 |
| ~~`:feature:{feed,map,capture,inbox,profile}:impl`~~ | `…:ui` | 개명 완료, VM 추가는 4단계 |
| `:feature:webview:impl` | 유지 | 유일한 정당 impl |
| `:core:base` | `:core:activity` | |
| `:core:runtime` | `:app` / `:core:notice:ui` / `:core:activity` | 모듈 소멸 |
| `:core:data` | `:core:data:{api,impl,assertions}` | `Static*` → assertions |
| `NotmidAppViewModel` | feature별 ViewModel + app 인증/세션 | |

## Migration Order

각 단계는 독립 PR이며, 다음 단계로 넘어가기 전에 명시된 검증을 통과해야 한다.

| # | 작업 | 검증 | 위험 | Status |
| --- | --- | --- | --- | --- |
| 0 | 웹/서버 별도 레포 추출, CI·스크립트 분리 | 새 레포 typecheck/smoke 통과, 이 레포 `./gradlew test :app:assembleDebug` 통과, CI에서 Node 단계 제거 | 낮음 | blocked on Open Decision 3 |
| 1 | `feature:notmid:api` → `core:navigation:api`, router 개명 | 전 모듈 컴파일, feature→feature api 엣지 0 | 낮음 | done 2026-09-12 |
| 2 | `impl` → `ui` 개명 | 컴파일, `settings.gradle.kts`와 namespace 일치 | 낮음(기계적) | done 2026-09-12 |
| 3 | 모델 이중화 제거, `feature:notmid:common` 정리 | 컴파일, core:model과 이름이 겹치는 타입이 3종 이하 | 중간 | done 2026-09-12 |
| 4 | ViewModel 분해 — feature 하나씩 1 PR | feature별 ViewModel 테스트, 화면 파라미터에서 `destination`/`listState` 제거 | 높음 | next |
| 5 | 셸을 `:app`으로 이동, `feature:notmid` 삭제 | 컴파일, 딥링크 수동 스모크, `:app`만 라우트 그래프 소유 | 높음 | |
| 6 | `core:base`/`core:runtime` 해체, `core:data` 분할 | 컴파일, `Static*`가 프로덕션 의존성에 없음 | 중간 | |

단계 0은 1~6과 독립이므로 먼저 끝내면 이후 모든 단계의 CI 시간이 줄어든다.

### 단계 3 실행 기록 (2026-09-12)

착수 전에 12쌍이라 적어둔 중복을 내용까지 대조했고, 그 결과 E5와 Decision 5를
정정했다. 실제로 한 일은 다음과 같다.

- 진짜 중복 10종을 `feature:notmid:common`에서 삭제하고 모든 참조를
  `core:model`로 돌렸다. `NotmidThreadMessageAttachment`는 구조가 같은
  `NotmidMessageAttachment`로 흡수했다.
- `NotmidUiModelMappers.kt`가 168줄에서 66줄이 됐다. 남은 것은 `NotmidClip`과
  `NotmidPlace`의 실제 도메인→UI 변환과 destination 조립뿐이고, 사라진 약 110줄은
  전부 동일 타입 간 항등 변환이었다.
- 죽은 컴포저블 `NotmidDestinationContent`와 그것만 쓰던 `NotmidHeader`를 삭제했다.
- `core:model` 의존이 빠져 있던 `feature:{capture,inbox,map}:ui`에 추가했다.

합계 359줄 삭제, 54줄 추가. 테스트는 133개 그대로이고 `./gradlew test`와
`:app:assembleDebug` 모두 BUILD SUCCESSFUL이다.

측정 과정에서 한 번 틀렸던 점을 남겨둔다: 파일명으로 참조를 세면
`NotmidUiModelMappers`, `NotmidUiPalette`, `NotmidDestinationLookup`,
`NotmidBadgeLabel`이 참조 0으로 나와 죽은 코드처럼 보인다. 실제로는 파일명과 다른
top-level 함수(`toNotmidDestinations`, `notmidPalette`, `destinationFor`,
`labelText`)를 담고 있어 모두 살아 있다. 이 저장소에서 Kotlin 죽은 코드를 찾을 때는
파일명이 아니라 선언명으로 세야 한다.

### 단계 1·2 실행 기록 (2026-09-12)

기준선은 변경 전 `./gradlew test --offline` **133 tests BUILD SUCCESSFUL** 과
`./gradlew :app:assembleDebug --offline` BUILD SUCCESSFUL 이었고, 두 단계 후에도
동일하다. 테스트는 추가·삭제·skip 없이 133개 그대로다.

단계 1에서 실제로 바뀐 것:

- `core/router/{api,impl,assertions}` → `core/navigation/{api,impl,assertions}`,
  패키지 `core.router.*` → `core.navigation.*`.
- `feature/notmid/api`의 5개 계약(`NotmidRoute`, `NotmidTopLevelRoute`,
  `NotmidRouteEvent`, `NotmidDestinationIds`, `NotmidStaticDeepLinkSpec`)이
  `core:navigation:api`의 `core.navigation.notmid` 패키지로 이동하고 모듈은 삭제됐다.
- `feature/{capture,feed,inbox,map,profile}/api`의
  `api(project(":feature:notmid:api"))`가 `":core:navigation:api"`로 바뀌어
  **feature-api → sibling feature-api 엣지가 0이 됐다**(E3 해소).

단계 2에서 실제로 바뀐 것:

- `feature/{capture,feed,inbox,map,profile}/impl` → `.../ui`, Gradle 좌표도 동일하게 개명.
- Kotlin 패키지는 원래 `feature.<name>`이라 `impl` 세그먼트가 없었으므로 패키지
  변경과 `namespace` 변경은 필요 없었다.
- `feature:webview:impl`은 Activity 진입을 소유하므로 이름을 유지한다.

## Stop Conditions

- 단계 4에서 어떤 feature의 ViewModel 분해가 `:app`의 인증/보호된 쓰기 경로를
  건드려야 한다면 멈추고, 그 경로를 먼저 포트로 추출한다.
- 새 모듈을 만들 때 owner, 허용 import, 금지 import, 첫 호출자, 검증 경로,
  collapse 규칙을 적을 수 없으면 만들지 않는다.
- `ui` 모듈 하나가 다른 `ui` 모듈을 필요로 하면, 공유 대상을
  `core:designsystem`으로 올릴지 결정하기 전에는 진행하지 않는다.

## Open Decisions

1. **딥링크 해석의 단일 소스.** Android이 자체 `DeepLinkSpec` 해석을 유지할 것인가,
   서버 `/v1/deeplinks/resolve`로 수렴할 것인가. 별도 레포 분리 후 결정한다.
2. **`feature:auth:ui` 신설 여부.** 로그인 화면을 feature로 둘지 `:app` 셸에 둘지.
   현재 로그인은 셸 상태(`shouldShowLogin`)에 강하게 묶여 있다.
3. **새 repository 이름과 히스토리 보존 방식.** `git filter-repo`로 8개 커밋을
   옮길지, 단일 초기 커밋으로 시작할지.
4. **`core:designsystem` 개명.** 기존 Transition Policy는 보류를 권고했다. 이
   ARD도 보류를 유지한다.
5. **`feature:notmid:common` 모듈명과 UI 모델 3종의 타입명.** 3단계가 드러낸
   문제다. 정리 후에도 `NotmidClip`, `NotmidPlace`, `NotmidDestination`이
   `core:model`의 동명 타입과 충돌해서 매퍼가 `as NotmidClipModel` 별칭 import를
   써야 한다. 두 축을 같이 정해야 한다.

   - 타입명: UI 쪽을 `ClipUiModel`/`PlaceUiModel`/`DestinationUiModel`처럼 접미사로
     구분할 것인가, 아니면 core 쪽 이름을 바꿀 것인가. 별칭 import를 계속 쓰는 것은
     선택지가 아니다.
   - 모듈명: `common`은 스킬이 지목한 모호한 버킷 이름이다. 정리 후 이 모듈이 실제로
     소유하는 것은 "여러 feature가 공유하는 제품 UI 모델과 컴포넌트"이므로, 그것을
     말해주는 이름으로 바꾼다.

   타입명 변경은 5개 feature `ui` 전체를 건드리므로 4단계(ViewModel 분해)와 같은
   파일들을 만진다. 4단계와 묶어서 하는 편이 충돌이 적다.

## Verification

- 각 단계 후 `./gradlew test`와 `./gradlew :app:assembleDebug`가 통과한다.
- 금지 엣지는 `settings.gradle.kts`와 각 `build.gradle.kts` 검사로 확인한다.
- 단계 0 후 `.github/workflows/ci.yml`에 Node/pnpm 단계가 남아 있지 않다.
- 단계 4 후 `grep -rl ViewModel feature`가 각 `ui` 모듈을 나열한다.
- 단계 5 후 `feature/notmid` 경로가 존재하지 않는다.
- 이 문서의 Current → Target Mapping 표는 각 단계 PR에서 갱신한다.
