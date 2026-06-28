---
title: Agent Example Packets
audience: Android engineers and AI agents
purpose: AI agent가 외부 참고 코드 없이 Notmid Android 공통화 구조를 구현할 수 있게 최소 예제와 중단 기준을 제공한다.
status: draft
owner: notmid Android architecture
source_of_truth: docs/specs/android-commonization
last_verified: 2026-06-28
applies_to: Android module planning, runtime contracts, assertions, build logic
related_pages:
  - README.md
  - 01-reference-inventory.md
  - 03-build-logic-module-templates.md
  - 04-router-webview-contract.md
  - 05-network-error-contract.md
  - 06-notice-alert-toast-contract.md
  - 07-state-assertions-testing.md
---

# Agent Example Packets

## 목적

이 문서는 외부 Android 코드베이스에서 확인한 구조를 Notmid에 적용할 때,
다음 agent가 실제 파일을 만들 수 있는 수준의 예제 packet만 남긴다.

공통 원칙은 AgentPlayBook Android 문서가 가진다. 이 문서는 Notmid module,
file sketch, import boundary, first caller/test, verification만 가진다. 외부
프로젝트 이름, 로컬 경로, package id, class name, signing/config/credential,
도메인 의존성은 기록하지 않는다.

## Packet 형식

구현 전에는 아래 항목을 모두 채운다.

```text
transferable lesson:
target boundary:
lowest acceptable ownership level:
minimal file/module sketch:
allowed imports:
forbidden imports:
first caller or test:
nearest verification:
collapse rule:
```

하나라도 비면 구현하지 않는다. 구현이 이미 진행 중이면 가장 낮은 owner로
축소하고, 빈 항목을 TODO로 남긴 뒤 스펙을 먼저 보강한다.

## 중복 제거 기준

Notmid 문서에 같은 규칙이 반복되면 다음 기준으로 정리한다.

- AgentPlayBook에 있는 공통 기준: 삭제하거나 링크만 남긴다.
- Notmid module/file 이름이 있는 기준: Notmid 문서에 남긴다.
- 구현 예제가 없는 기준: 이 문서의 packet으로 보강한다.
- 특정 외부 앱의 module tree를 설명하는 기준: 제거하고 transferable lesson만
  남긴다.

## Packet 1: Feature Route API / Impl / Assertions

```text
transferable lesson: caller는 route/event 계약만 보고, 화면 구현과 test helper는 분리한다.
target boundary: :feature:<name>:api + :feature:<name>:impl + optional :feature:<name>:assertions
lowest acceptable ownership level: feature impl 내부 파일 split; 두 번째 caller/test가 생기기 전에는 assertions module 금지
minimal file/module sketch:
  feature/<name>/api/route/<Name>Route.kt
  feature/<name>/api/event/<Name>RouteEvent.kt
  feature/<name>/impl/<Name>RouteHolder.kt
  feature/<name>/impl/<Name>Screen.kt
  feature/<name>/impl/<Name>ViewModel.kt
  feature/<name>/assertions/<Name>RouteFixtures.kt only after reuse is real
allowed imports:
  api -> :core:router:api, :core:model, Kotlin value types
  impl -> own api, :core:designsystem, :core:notice:api, :core:domain
  assertions -> own api and test libraries
forbidden imports:
  api -> Activity, Context, Intent, NavController, Compose UI, feature impl
  assertions -> production impl, app, runtime singletons
first caller or test:
  app/product router imports <Name>Route or <Name>RouteEvent without depending on impl
nearest verification:
  ./gradlew :feature:<name>:api:test
  ./gradlew :feature:<name>:impl:compileDebugKotlin
collapse rule:
  if app does not need the route/event without implementation, keep one feature impl module and split files only
```

Minimal contract example:

```kotlin
data class ClipDetailRoute(
    val clipId: NotmidClipId,
) : ComposeRoute

sealed interface FeedRouteEvent : RouteEvent {
    data class ClipRequested(val clipId: NotmidClipId) : FeedRouteEvent
}
```

Minimal assertion helper only after reuse:

```kotlin
object FeedRouteFixtures {
    fun clipDetail(id: String = "clip-1") = ClipDetailRoute(NotmidClipId(id))
}
```

## Packet 2: Router Runtime And ActivityRoute Launch

```text
transferable lesson: pure route plan과 Android Activity 실행을 분리한다.
target boundary: :core:router:api + :core:router:impl + :core:runtime/router/activity
lowest acceptable ownership level: current :core:runtime test source until a second external test boundary needs runtime assertions
minimal file/module sketch:
  core/router/api/.../ActivityRoute.kt
  core/router/api/.../RoutePlan.kt
  core/runtime/.../router/activity/ActivityRouteLaunchHandler.kt
  core/runtime/.../router/activity/DefaultActivityRouteLauncher.kt
  core/runtime/.../router/activity/ActivityRouteLauncherEffect.kt
allowed imports:
  router api -> Kotlin contracts only
  runtime activity -> Android Context/Intent/Activity result APIs only where execution happens
forbidden imports:
  router api -> Context, Intent, Activity, NavController, Compose runtime
  runtime activity -> feature impl screens, repositories, auth policy
first caller or test:
  WebViewRoute launch handler and AppRouterRuntime pending ActivityRoute test
nearest verification:
  ./gradlew :core:router:api:test
  ./gradlew :core:runtime:test
  ./gradlew :feature:webview:impl:compileDebugKotlin
collapse rule:
  if only one feature starts one Activity and no RoutePlan test needs it, keep launch local to that feature
```

Minimal API shape:

```kotlin
interface ActivityRoute : Route {
    val activityKey: String
}

data class RoutePlan(
    val composeStack: RouteStack? = null,
    val activityRoutes: List<ActivityRoute> = emptyList(),
)
```

Runtime 실행은 `ActivityRouteLaunchHandler`가 `Intent` 생성을 알고,
`DefaultActivityRouteLauncher`가 처리 가능한 handler를 찾아 실행한다. route
API는 어떤 Activity가 열리는지 모른다.

## Packet 3: Notice Contract / Runtime Host / Recording Fake

```text
transferable lesson: ViewModel은 notice 요청만 emit하고, runtime host가 toast/snackbar/alert를 표시한다.
target boundary: :core:notice:api + :core:runtime/notice/host + local or future notice assertions
lowest acceptable ownership level: :core:notice:api test source; external reuse가 생기기 전에는 notice assertions module 금지
minimal file/module sketch:
  core/notice/api/.../model/NoticeRequest.kt
  core/notice/api/.../model/NoticePresentation.kt
  core/notice/api/.../effect/NoticeEffect.kt
  core/runtime/.../notice/host/NoticeHost.kt
  core/runtime/src/test/.../RecordingNoticeSink.kt
allowed imports:
  notice api -> Kotlin value types and immutable models
  runtime host -> Compose Material/Android Toast only in rendering boundary
forbidden imports:
  notice api -> Toast, SnackbarHostState, AlertDialog, Context, Compose UI
  runtime host -> feature copy, repositories, network error parsing, route policy
first caller or test:
  NotmidAppViewModel emits NoticeEffect.ShowNotice and runtime host renders it
nearest verification:
  ./gradlew :core:notice:api:test
  ./gradlew :core:runtime:test
  ./gradlew :app:test --tests '*NotmidAppViewModelTest'
collapse rule:
  if notice is used by one ViewModel and no runtime renderer exists, keep a local side-effect test first
```

Minimal contract example:

```kotlin
data class NoticeRequest(
    val message: String,
    val presentation: NoticePresentation,
    val tone: NoticeTone = NoticeTone.Neutral,
)

sealed interface NoticeEffect {
    data class ShowNotice(val request: NoticeRequest) : NoticeEffect
}
```

Minimal recording fake:

```kotlin
class RecordingNoticeSink : NoticeSink {
    val requests = mutableListOf<NoticeRequest>()

    override fun show(request: NoticeRequest) {
        requests += request
    }
}
```

## Packet 4: Network API / Impl / Assertions

```text
transferable lesson: repository tests need request/response fakes without importing network impl.
target boundary: :core:network:api + :core:network:impl + :core:network:assertions
lowest acceptable ownership level: repository-local fake until two repository/auth/data tests share it
minimal file/module sketch:
  core/network/api/.../NotmidNetworkClient.kt
  core/network/api/.../NotmidApiErrorEnvelope.kt
  core/network/impl/.../OkHttpNotmidNetworkClient.kt
  core/network/assertions/.../RecordingNotmidNetworkClient.kt
  core/network/assertions/.../QueuedNetworkResponse.kt
  core/network/assertions/.../NetworkRequestSubject.kt
allowed imports:
  assertions -> :core:network:api and test libraries
forbidden imports:
  assertions -> :core:network:impl, OkHttp internals, app, feature impl, real network/database/Firebase
first caller or test:
  core:data repository test queues success/failure and asserts path/header/body safely
nearest verification:
  ./gradlew :core:network:assertions:test
  ./gradlew :core:data:test
collapse rule:
  if only one repository test needs the fake, keep it in that test source
```

Minimum fake behavior:

- fail when response queue is empty.
- fail when queued responses remain after test.
- redact authorization/token-like headers in assertion messages.
- never perform real IO.

## Packet 5: Build Logic Convention

```text
transferable lesson: convention plugins remove repeated Gradle setup; they do not encode product behavior.
target boundary: build-logic included build
lowest acceptable ownership level: module-local build.gradle.kts until two modules repeat the same setup
minimal file/module sketch:
  build-logic/settings.gradle.kts
  build-logic/convention/src/main/kotlin/NotmidAndroidLibraryConventionPlugin.kt
  build-logic/convention/src/main/kotlin/NotmidAndroidLibraryComposeConventionPlugin.kt
  build-logic/convention/src/main/kotlin/NotmidKotlinLibraryConventionPlugin.kt
allowed imports:
  convention -> Android/Kotlin/Compose Gradle APIs, version catalog aliases
forbidden imports:
  convention -> product route registration, DI bindings, feature source paths, signing secrets, environment URLs
first caller or test:
  two or more Notmid modules use the same Android library or Compose library setup
nearest verification:
  ./gradlew help
  ./gradlew :app:compileDebugKotlin
collapse rule:
  if only one module needs the dependency setup, keep it in that module build file
```

Do not import a reference app's plugin list wholesale. Start with Notmid's
minimum plugin set and add specialized convention plugins only after repeated
Notmid module setup proves the need.

## Packet 6: WebView Compose Base And Activity Shell

```text
transferable lesson: WebView starts as a feature ActivityRoute, but the WebView body is Compose content reused by both Activity and future Compose mappings.
target boundary: :feature:webview:api + :feature:webview:impl, future :core:runtime/webview only after reuse
lowest acceptable ownership level: feature:webview implementation
minimal file/module sketch:
  feature/webview/api/route/WebViewRoute.kt
  feature/webview/api/deeplink/WebViewDeepLinkSpec.kt
  feature/webview/impl/NotmidWebViewContent.kt
  feature/webview/impl/NotmidWebViewActivity.kt
  feature/webview/impl/WebViewActivityRouteLaunchHandler.kt
  future core/runtime/webview/WebViewSecurityPolicy.kt only after second caller
allowed imports:
  feature api -> router api, Kotlin value types
  feature impl -> Android WebView, Compose AndroidView, ActivityRoute handler, own api
forbidden imports:
  feature api -> WebView, Context, Intent, JavaScript bridge implementation
  core runtime webview -> Notmid product copy, auth policy, feature impl screens
first caller or test:
  ActivityRoute path launches NotmidWebViewActivity, and Activity shell delegates rendering to NotmidWebViewRouteContent
nearest verification:
  ./gradlew :feature:webview:impl:compileDebugKotlin
  ./gradlew :app:compileDebugKotlin
collapse rule:
  if there is no second WebView caller or no repeated policy, do not create core runtime webview package
```

Implementation sketch:

```kotlin
@Composable
fun NotmidWebViewRouteContent(route: WebViewRoute) {
    NotmidWebViewContent(
        url = route.url,
        mode = route.mode,
        javaScriptEnabled = route.javaScriptEnabled,
    )
}

class NotmidWebViewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val route = intent.toWebViewRoute() ?: return finish()
        title = route.title.orEmpty()
        setContent {
            NotmidWebViewRouteContent(route)
        }
    }
}
```

Before extracting reusable WebView runtime, write explicit policy for:

- allowed URL schemes and host/base-path policy.
- JavaScript default by mode.
- file chooser lifecycle cleanup.
- fullscreen media and new-window handling.
- whether JavaScript bridge is forbidden or contract-bound.
- whether Compose navigation should embed `NotmidWebViewRouteContent` or keep
  the route Activity-backed for lifecycle, permission, and fullscreen reasons.

## Packet 7: BaseActivity / AppRoot / Activity 없는 Host

```text
transferable lesson: Activity lifecycle template과 Activity 없이 설치 가능한 runtime host를 분리한다.
target boundary: :core:base + :core:runtime + pure router/notice API
lowest acceptable ownership level: MainActivity-local helper until a second Activity or Compose host needs it
minimal file/module sketch:
  core/base/.../activity/BaseActivity.kt
  core/base/.../root/AppRoot.kt
  core/base/.../deeplink/PendingDeepLink.kt
  core/runtime/.../notice/host/NoticeHost.kt
  core/runtime/.../router/activity/ActivityRouteLauncherEffect.kt
  app/.../MainActivity.kt
allowed imports:
  core/base -> ComponentActivity, Intent, enableEdgeToEdge, Compose setContent
  core/runtime -> Compose runtime, lifecycle collection, Android adapters only in execution packages
  pure api -> Kotlin value types and route/notice contracts
forbidden imports:
  core/base -> product route registration, repositories, feature screen mapping, ViewModel creation, network error copy
  core/runtime host -> product feature copy, repository calls, auth policy, app-only URL policy
  pure api -> Activity, Context, Intent, NavController, SnackbarHostState
first caller or test:
  MainActivity extends BaseActivity and installs BaseAppRoot with caller-provided router, notice effects, and ActivityRoute launcher
nearest verification:
  ./gradlew :core:base:compileDebugKotlin
  ./gradlew :core:runtime:test
  ./gradlew :app:compileDebugKotlin
collapse rule:
  if only MainActivity uses the helper and no Activity-absent host needs runtime contracts, keep helper in app and document missing caller
```

BaseActivity는 Activity entrypoint를 template화한다. Activity가 없어도 필요한
`NoticeHost`, router runtime state, pending ActivityRoute effect, permission/result
adapter contract는 `:core:runtime` 또는 pure API로 남겨 caller가 조합한다.

## Packet 8: ViewModel Action / UiState / SideEffect Streams

```text
transferable lesson: View 입력, durable state, runtime side-effect, repository one-shot work를 같은 Flow로 섞지 않는다.
target boundary: feature/app ViewModel + core/model action delegate + notice/router runtime delegates
lowest acceptable ownership level: ViewModel-local direct onAction until ordering/backpressure reuse proves a delegate
minimal file/module sketch:
  app/.../NotmidAppAction.kt
  app/.../NotmidAppViewModel.kt
  app/.../NotmidAppUiState.kt
  core/model/.../NotmidActionDelegate.kt
  core/notice/api/.../NoticeEffectDelegate.kt
allowed imports:
  ViewModel -> own Action/UiState, domain repository contract, notice/router delegate contract
  action delegate -> Kotlin coroutines Channel/Flow only
  notice delegate -> Kotlin coroutines SharedFlow only
forbidden imports:
  ViewModel -> Toast, SnackbarHostState, AlertDialog, Context, Activity, Intent, NavController
  UiState -> Flow, Channel, MutableList, callbacks, repositories, runtime launchers
  Action -> Android UI/runtime objects or raw generic event maps
first caller or test:
  NotmidAppViewModel receives NotmidAppAction, calls suspend repository work, updates StateFlow, emits NoticeEffect
nearest verification:
  ./gradlew :app:test --tests '*NotmidAppViewModelTest'
  ./gradlew :core:notice:api:test
  git diff --check
collapse rule:
  if a screen has only two simple callbacks and no queue/backpressure need, keep explicit methods or direct onAction without adding a Channel delegate
```

Primitive 선택 기준은 AgentPlayBook
`platforms/android/android-viewmodel-state.md`의 `Stream Primitive Selection`을
따른다. 이 packet은 Notmid의 현재 매핑만 고정한다:
`NotmidActionDelegate`는 Action queue, `NoticeEffectDelegate`는 one-shot notice
effect, app/feature ViewModel은 `StateFlow<UiState>`, repository/use case 호출은
`suspend`.

## Packet 9: Capability Extension Modules

```text
transferable lesson: core module 이름은 plugin type이 아니라 caller가 import하는 capability를 설명해야 한다.
target boundary: :core:<capability>, :core:<capability>:api, :core:runtime/<capability>, optional extension modules
lowest acceptable ownership level: package-level extension file until two modules import the same helper
minimal file/module sketch:
  core/kotlin-extensions/.../<Capability>KotlinExtensions.kt only for pure Kotlin helpers
  core/compose-extensions/.../<Capability>ComposeExtensions.kt only for Compose runtime helpers
  core/runtime/<capability>/... when helper executes Android/Compose runtime behavior
allowed imports:
  kotlin-extensions -> Kotlin stdlib/coroutines only when platform-free
  compose-extensions -> Compose runtime/ui contracts and design-system-free helpers
  runtime capability -> Android/Compose execution APIs required by that adapter
forbidden imports:
  kotlin-extensions -> Android, Compose, resources, app/feature impl
  compose-extensions -> repositories, product route policy, app-only runtime config, feature copy
  runtime capability -> broad product policy or unrelated helper families
first caller or test:
  two Notmid modules import the helper without needing each other's implementation
nearest verification:
  ./gradlew :<core-module>:test
  ./gradlew :<android-or-compose-module>:compileDebugKotlin
collapse rule:
  if the helper has one caller, a vague name, or mixed Android/Compose/Kotlin imports, keep it local or move it under a named capability package
```

`core` module type 판단 기준은 AgentPlayBook
`platforms/android/android-module-structure.md`의 `Core Is A Capability Namespace`를
따른다. 이 packet은 Notmid에 새 extension module을 만들 때 필요한 target boundary,
first caller/test, collapse rule만 고정한다.
