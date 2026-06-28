---
title: State Assertions And Testing
audience: Android engineers and AI agents
purpose: ViewModel/state/action/effect 테스트와 assertions 모듈 설계 기준을 정의한다.
status: draft
owner: notmid Android architecture
source_of_truth: docs/specs/android-commonization
last_verified: 2026-06-28
applies_to: ViewModel tests, fake repositories, route tests, notice tests
related_pages:
  - 02-target-module-taxonomy.md
  - 03-build-logic-module-templates.md
---

# State Assertions And Testing

## Decision

Assertions modules are first-class test support modules, not production modules.

They exist to make this possible:

```text
test -> assertions -> api
```

instead of this:

```text
test -> app shell -> impl -> api
```

Do not add `assertions` to every module by default. Add it when at least one of these is true:

- multiple tests need the same fake/recording helper.
- a feature/app test needs to verify an API contract without implementation dependency.
- a common state/effect/action contract needs reusable fixtures.
- contract testing is blocked because fake code is trapped inside one `src/test`.

## Assertions Module Contents

Allowed:

```text
Fake*
Recording*
Stub*
Fixture*
*Assertions
*Subject
TestClock/TestDispatcher adapter when contract requires it
```

Examples:

```text
RecordingRouter
FakeRouteRegistry
RecordingNoticeSink
FakeNotmidNetworkClient
NetworkRequestSubject
NotmidUiStateFixtures
```

Not allowed:

```text
production implementation
Android Activity launch side effects
real network calls
real Firebase/auth credentials
feature implementation imports
large app shell factory
```

## State Ownership Baseline

Use this flow unless a feature is intentionally simpler:

```text
Route holder -> ViewModel -> UseCase -> Repository -> Network/DataSource
```

Composable rules:

- stateless screen renders state and emits actions.
- route holder collects state lifecycle-aware.
- ViewModel owns durable screen state and one-off effects.
- user actions are typed sealed values for non-trivial screens.
- one-off effects do not replay after rotation unless explicitly required.

## UiState Contract

State should represent reachable states explicitly.

Required for async screens:

```text
Loading
Content
Empty
Error
PermissionDenied when applicable
SignedOut/RequiresAuth when applicable
Refreshing/stale content when applicable
```

Avoid:

- nullable payload plus unrelated flags.
- raw exceptions in state.
- callbacks inside state.
- repository or router objects inside state.
- Android `Context`, `Activity`, `NavController` in state.

Compose-observed state should be immutable and stable by structure. Add Compose stability annotations only where the module is Compose-aware and the promise is true.

## Action And Effect Contract

Feature action:

```kotlin
sealed interface FeedAction {
    data object Refresh : FeedAction
    data class ClipClick(val clipId: String) : FeedAction
    data object DismissNotice : FeedAction
}
```

Feature effect:

```kotlin
sealed interface FeedEffect {
    data class OpenClip(val clipId: String) : FeedEffect
    data class ShowNotice(val request: NoticeRequest) : FeedEffect
}
```

If the feature already emits route events through `RouteEventSink`, do not duplicate route effects unless the local state owner needs an intermediate effect for testing.

## Channel / SharedFlow / StateFlow / suspend 기준

공통 primitive 선택 기준은 AgentPlayBook
`platforms/android/android-viewmodel-state.md`의 `Stream Primitive Selection`을
따른다. Notmid tests는 그 기준을 반복하지 않고 현재 구현 매핑을 검증한다.

- `NotmidActionDelegate`: Action 순서, backpressure, 중복 submit/cancellation.
- `StateFlow<UiState>`: 초기값, loading/content/error 전이, stale result 억제.
- `NoticeEffectDelegate`와 router/runtime effects: 한 번만 emit되고 replay되지 않는지.
- `suspend` repository/use case 호출: 성공/실패/cancellation mapping과 dispatcher 제어.
- router pending state: 놓치면 안 되는 route/effect를 blind replay가 아니라 id/consume
  contract로 검증.

## Router Assertions

Test target:

```text
feature emits route event -> app maps to RoutePlan -> router records expected stack/activity route
```

Assertions should make these easy:

- route stack includes expected entries.
- dynamic argument preserved.
- ActivityRoute is separated from Compose stack.
- deep link resolves to ordered stack.
- unknown deep link returns null.

## Network Assertions

Test target:

```text
repository sends expected request -> fake client returns body/failure -> repository maps entity/failure
```

Assertions should make these easy:

- method/path/body match.
- auth header is present or absent without printing token value.
- HTTP status maps to domain failure.
- malformed JSON maps to malformed response.
- timeout/transport failure preserves retryability.

## Notice Assertions

Test target:

```text
ViewModel catches domain failure -> emits notice/state -> renderer is not required
```

Assertions should make these easy:

- presentation equals Toast/Snackbar/Alert/Inline/FullPage.
- tone equals Info/Success/Warning/Error.
- action semantic is expected.
- deep link is allowlisted or blank.
- notice is emitted once and not replayed by the fake sink.

## WebView Assertions

Create only after WebView state expands.

Possible helpers:

```text
FakeWebViewNavigator
RecordingWebViewEventSink
WebViewSecurityPolicySubject
FakeFileChooserLauncher
```

Test target:

- http/https validation.
- blocked host.
- JavaScript disabled for generic mode unless explicitly enabled.
- file chooser request and cleanup.
- back/reload events recorded without real WebView.

## Feature Assertions

Only add `:feature:<name>:assertions` when:

- other modules test against that feature API contract.
- the feature exports reusable route fixtures.
- the feature has public display model fixtures used by app shell tests.

Do not add feature assertions just to hold previews. Previews can stay in feature impl until shared.

## Test Data Policy

Deterministic fixtures belong in the lowest stable owner.

```text
core:model fixture -> if model is pure and reused broadly
feature assertions fixture -> if model is feature-specific route/display contract
core:network assertions fixture -> if it describes network envelope/request
:core:runtime notice assertions fixture -> if it describes notice runtime
```

Avoid fake data that encodes production credentials, private URLs, or user-specific values.

## Migration Steps

1. Add router assertions.
2. Move repeated app router test setup into router assertions.
3. Add network assertions.
4. Move repeated fake network client behavior into network assertions.
5. Add notice assertions.
6. Refactor ViewModel tests to assert notice without needing design-system renderer.
7. Add feature assertions only where shared test pressure appears.

## Review Checklist

- Does the assertion helper depend only on the stable API?
- Does it expose enough observation for tests without real side effects?
- Could it accidentally be used in production?
- Does the fake preserve cancellation and failure semantics when relevant?
- Does the fixture hide product policy that should live in a feature/domain test?

## Verification

For each new assertions module:

```bash
./gradlew :<module>:test
./gradlew :app:test
./gradlew test
git diff --check
```

If a module has no tests yet, compile it and add at least one smoke test for the assertion helper itself.
