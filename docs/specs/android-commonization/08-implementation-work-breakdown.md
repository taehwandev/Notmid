---
title: Implementation Work Breakdown
audience: Android engineers and AI agents
purpose: 분리 스펙을 실제 구현 작업 단위, 순서, acceptance, 검증 명령으로 나눈다.
status: draft
owner: notmid Android architecture
source_of_truth: docs/specs/android-commonization
last_verified: 2026-06-16
applies_to: future Android implementation tasks
related_pages:
  - README.md
  - 02-target-module-taxonomy.md
  - 04-router-webview-contract.md
  - 05-network-error-contract.md
  - 06-feedback-alert-toast-contract.md
  - 07-state-assertions-testing.md
---

# Implementation Work Breakdown

## Strategy

Do not migrate everything at once.

Order the work by risk:

1. Add test-only assertion boundaries where production behavior does not change.
2. Strengthen typed contracts.
3. Move app-runtime rendering into `:core:app`.
4. Extract app shell/base helpers.
5. Harden WebView.
6. Update skills/wiki after code proves the shape.

## Phase 0: Planning Docs

Scope:

- Add this spec directory.
- No production code changes.
- No new modules.

Acceptance:

- Spec files are split by implementation boundary.
- Reference project borrow/reject rules are explicit.
- Current skill doc gap is recorded.
- Future implementation order is clear.

Verification:

```bash
git diff --check
vibeguard audit .
agent-finish-check.py
```

## Phase 1: Router Assertions

Goal:

Add the first `assertions` module with minimal risk.

Proposed changes:

```text
settings.gradle.kts
  include ":core:router:assertions"

core/router/assertions/build.gradle.kts
core/router/assertions/src/main/kotlin/.../RecordingRouter.kt
core/router/assertions/src/main/kotlin/.../RouteStackAssertions.kt
```

Dependencies:

```text
:core:router:assertions -> :core:router:api
app tests -> :core:router:assertions
```

Acceptance:

- App router tests can use `RecordingRouter` or route stack assertions.
- Assertions module does not depend on `:app`, feature impl, or Android runtime.
- `RoutePlan.fromStack` mixed Compose/Activity behavior has focused tests.

Verification:

```bash
./gradlew :core:router:assertions:test
./gradlew :app:test --tests '*AppRouterTest'
./gradlew :app:test --tests '*AppDeepLinkResolverTest'
./gradlew :app:compileDebugKotlin
```

## Phase 2: Network Assertions And Failure Shape

Goal:

Make network tests reusable before changing failure behavior.

Proposed changes:

```text
:core:network:assertions
  FakeNotmidNetworkClient
  RecordingNotmidNetworkClient
  QueuedNetworkResponse
  NetworkRequestAssertions
```

Then extend failure metadata in `:core:network:api` only as needed.

Acceptance:

- Repository tests can queue success, HTTP status, malformed body, timeout, and transport failures.
- Request assertions can validate path/body/header presence without printing token values.
- Existing `OkHttpNotmidNetworkClientTest` still passes.
- No generic `Success`/`Failure` wrapper is introduced for all suspend calls.

Verification:

```bash
./gradlew :core:network:assertions:test
./gradlew :core:network:api:test
./gradlew :core:network:impl:test
./gradlew :core:data:test
```

## Phase 3: Server Error Envelope And Domain Mapping

Goal:

Centralize common API error parsing and map it into domain failures.

Proposed changes:

```text
core/network/api
  safe server envelope value type or parser contract

core/data
  common mapper for API error envelope -> domain failure

core/domain
  richer protected write/content failure when needed
```

Acceptance:

- Protected write failure mapping no longer duplicates body parsing per repository method.
- Server presentation hints are parsed as safe metadata only.
- Domain failures do not expose raw response body by default.
- Cancellation is not converted to retryable network error.

Verification:

```bash
./gradlew :core:data:test
./gradlew :app:test --tests '*NotmidAppViewModelTest'
```

## Phase 4: Feedback API And Assertions

Goal:

Move feedback contracts into a pure `:core:feedback:api` module without a broad design-system move.

Proposed changes:

```text
:core:feedback:api
  model/
  FeedbackRequest
  FeedbackPresentation
  FeedbackTone
  FeedbackAction
  effect/
  FeedbackEffect
  FeedbackEffectDelegate

:core:feedback:api test source
  FeedbackEffectDelegate tests
  feedback contract fixtures
```

Acceptance:

- ViewModel tests can assert feedback emission without Compose renderer.
- Old `NotmidUiFeedback`/`NotmidUiEffect` duplicates are removed after callers migrate.
- Feature/app code does not call Android Toast/Alert directly.
- Feature-specific actions remain feature-owned.

Verification:

```bash
./gradlew :core:feedback:api:test
./gradlew :app:test --tests '*NotmidAppViewModelTest'
```

## Phase 5: Core-App Feedback Impl

Goal:

Move runtime rendering out of `:core:designsystem` into `:core:app`.

Proposed changes:

```text
:core:app feedback/host package
  FeedbackHost
  FeedbackEffectLifecycleCollector
  FeedbackAlertDialog
  FeedbackActionHandler
```

Migration:

- Keep `NotmidSnackbarHost` visual style in design system.
- Move effect collection/rendering responsibility to feedback impl.
- Update `MainActivity`/app root to install `FeedbackHost`.

Acceptance:

- `:core:designsystem` no longer owns global feedback effect lifecycle.
- Feedback renderer depends on design-system primitives where needed.
- App state and feature tests use `:core:feedback:api` contracts.

Verification:

```bash
./gradlew :core:app:compileDebugKotlin
./gradlew :app:compileDebugKotlin
./gradlew :app:test
```

## Phase 6: App Shell/Base Commonization

Goal:

Extract compositional app shell helpers without BaseActivity inheritance.

Proposed candidates:

```text
AppEnvironment
NotmidAppRoot
RouteCoordinator
AuthGateCoordinator
ProtectedActionCoordinator
```

Acceptance:

- `MainActivity` is thinner but still owns Android entry setup.
- Product state remains in `NotmidAppViewModel` or explicit app coordinator, not in design-system/core helpers.
- No broad behavior change is mixed with the extraction.

Verification:

```bash
./gradlew :app:test --tests '*NotmidAppViewModelTest'
./gradlew :app:test --tests '*AppRouterTest'
./gradlew :app:compileDebugKotlin
```

## Phase 7: WebView Hardening

Goal:

Improve current WebView ActivityRoute before extracting reusable runtime modules.

Proposed changes:

- URL scheme validation at route/intent boundary.
- optional allowlist policy.
- mode-based JavaScript default.
- explicit file chooser cleanup if file chooser support is added.
- fullscreen media and new-window policy if needed.

Keep module placement:

```text
:feature:webview:api
:feature:webview:impl
```

Only add a `:core:app` webview package after a second caller or reusable holder pressure appears.

Acceptance:

- invalid URL finishes safely or refuses route before Activity launch.
- JS bridge remains absent unless a dedicated contract is written.
- WebView lifecycle cleanup remains explicit.

Verification:

```bash
./gradlew :feature:webview:impl:compileDebugKotlin
./gradlew :app:compileDebugKotlin
```

## Phase 8: Skill/Wiki Update

Goal:

Make the proven structure discoverable for future agents.

Proposed docs:

```text
llm-wiki/module-map.md
llm-wiki/implementation-checklist.md
llm-wiki/routing-deeplinks.md
.agents/skills/glassnavlab-android-stewardship/SKILL.md
.agents/skills/notmid-product-engineering/SKILL.md
```

Acceptance:

- English canonical docs match implemented modules.
- Korean planning docs can stay as local planning history or be translated before commit if requested.
- Skills route agents to the new `:core:app`/`assertions` specs.

Verification:

```bash
git diff --check
vibeguard audit .
```

## Commit Split Recommendation

Use separate commits.

```text
docs(android): plan core-app commonization
test(android): add router assertions module
test(android): add network assertions module
feat(android): add feedback api and assertions
refactor(android): move feedback host to core-app
refactor(android): extract app shell coordinators
fix(android): harden webview route policy
docs(android): update module map and agent skills
```

Do not combine docs, build-logic, network behavior, feedback UI, and WebView policy in one commit.

## Global Done Criteria

The commonization work is done only when:

- each new module has a clear owner and allowed import direction.
- `assertions` modules are used by at least one real test.
- app compile and focused tests pass.
- VibeGuard audit has no new secret/cost/data blockers.
- skill/wiki docs match implemented structure.
