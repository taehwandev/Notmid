---
title: Notice Alert Toast Contract
audience: Android engineers and AI agents
purpose: toast, snackbar, alert, inline, full-page notice를 API 계약과 core/runtime 런타임으로 분리한다.
status: draft
owner: notmid Android architecture
source_of_truth: docs/specs/android-commonization
last_verified: 2026-06-16
applies_to: UI effects, design system, app shell, ViewModel notice
related_pages:
  - 05-network-error-contract.md
  - 07-state-assertions-testing.md
  - llm-wiki/design-system.md
---

# Notice Alert Toast Contract

## Current State

Notmid notice is split across a pure API contract, a runtime host, and
design-system visual primitives.

```text
:core:notice:api
  model/ NoticeRequest
  model/ NoticePresentation
  model/ NoticeTone
  model/ NoticeAction
  effect/ NoticeEffect
  effect/ NoticeEffectDelegate

:core:runtime
  notice/host/ NoticeHost
  notice/host/ NoticeEffectLifecycleCollector
  notice/host/ NoticeAlertDialog
  Android Toast/Snackbar/Alert dispatch

:core:designsystem
  NotmidSnackbarHost visual style
  tokens and reusable visual primitives
```

`core:model` does not own UI effect contracts. `core:designsystem` does not
collect flows, show Toasts, or render runtime alerts.

## Decision

Use a dedicated `:core:notice:api` module before adding more notice behavior.
Render and collect those effects from `:core:runtime`, not from feature screens or
the design system.

Target:

```text
:core:notice:api
  model/
  effect/

:core:runtime
  notice/host/

:core:designsystem
  visual notice primitives only
```

Do not leave duplicate notice contracts in `:core:model` once callers migrate.
Do not put lifecycle collection, Toast rendering, or app effects back into
`:core:designsystem`.

## Notice Contract

The API should express a notice request, not a concrete UI widget.

Target concepts:

```text
NoticeRequest
  id
  presentation
  tone
  message
  action

NoticePresentation
  Toast
  Snackbar
  Alert
  Inline
  FullPage

NoticeAction
  sealed interface for closed shared UI triggers
  current action: OpenDeepLink(label, deepLink)
  future actions: Retry, Dismiss, SignIn after a real caller exists
```

Do not make a reusable notice action carry feature-specific sealed actions. Feature actions remain feature-owned.

## Presentation Rules

### Toast

Use only for transient status.

Good:

- saved.
- copied.
- local best-effort confirmation.

Bad:

- retry surface.
- destructive confirmation.
- auth required.
- important error that needs action.

### Snackbar

Use for recoverable transient errors or undo/retry when the screen can continue.

Example:

```text
save failed -> snackbar with retry
```

### Alert

Use for blocking or explicit decision.

Example:

```text
permission denied explanation
conflict that needs confirm/cancel
server policy rejection with detail
```

### Inline

Use for form fields, composer errors, and local screen sections.

Inline notice is usually feature state, not a global UI effect.

### FullPage

Use for blocking content load failures, not one-off effects.

Full-page notice should normally be represented in `UiState`, not as a transient event.

## Ownership

### Feature/ViewModel

Feature owns:

- user action.
- product copy key.
- whether retry/dismiss/sign-in is appropriate.
- local inline/full-page state.

Feature emits:

- `NoticeRequest` or `UiState` failure.
- route/action event for follow-up, not direct UI code.

### Core Notice API

API owns:

- stable notice value types.
- notice sink/source interfaces.
- one-shot `NoticeEffect` contracts and delegate interfaces.

### Core Runtime Notice Host

`:core:runtime` owns:

- Toast rendering.
- Snackbar host adapter.
- AlertDialog renderer.
- lifecycle-aware collection.
- Android resource/message resolving.

### Core Notice Assertions

Assertions owns:

- `RecordingNoticeSink`.
- `FakeNoticeMessageResolver`.
- `NoticeRequestAssertions`.
- helper fixtures for common error presentations.

## Message Policy

Long term, notice messages should be message keys or resource ids where feasible.

Rules:

- ViewModel should not call `Context.getString`.
- low-level network/domain errors should not own final UI copy.
- safe server fallback messages may be carried, but feature/app decides whether to show them.
- `assertions` should compare message key or stable safe text, not localized platform string output.

## Action Policy

Notice action is a UI trigger, not business logic.

Allowed:

```text
NoticeAction.Retry
NoticeAction.Dismiss
NoticeAction.OpenDeepLink(uri)
NoticeAction.SignIn
```

Feature/app maps that trigger to an owned action:

```text
NoticeAction.Retry -> NotmidAppAction.ReloadContent
NoticeAction.SignIn -> NotmidAppAction.ContinueAuth(...)
NoticeAction.OpenDeepLink -> AppRouterRuntime.navigateDeepLink(...)
```

Forbidden:

```text
NoticeAction(payload = InboxAction.SendMessage(...))
Notice renderer calls repository
Toast click performs protected write
```

## Relationship To Design System

Design system owns visual primitives.

Notice module owns runtime orchestration.

Target split:

```text
:core:runtime notice/host package
  NoticeHost
  NoticeEffectLifecycleCollector
  NoticeAlertDialog
  NoticeActionHandler

:core:designsystem
  NotmidSnackbarHost visual style
  NotmidButton
  NotmidSurface
  tokens
```

The host split is:

```text
NoticeHost
  effect collection and dispatch

AlertRenderer
  composable AlertDialog wrapper

ToastRenderer
  Android Toast boundary
```

## Assertions Examples

Recording sink:

```kotlin
val sink = RecordingNoticeSink()
sink.show(NoticeRequest.toast("Saved"))

sink.assertLast {
    hasPresentation(NoticePresentation.Toast)
    hasTone(NoticeTone.Success)
}
```

Alert assertion:

```kotlin
sink.assertLastAlert {
    hasAction(NoticeAction.SignIn)
    isCancellable()
}
```

The assertion helper should not require Compose runtime.

## Migration Steps

1. Keep pure value/effect contracts in `:core:notice:api`.
2. Keep Android/Compose collection and rendering in `:core:runtime` `notice/host`.
3. Keep design-system visual components reusable and product-free.
4. Update Activity shells to install `NoticeHost` once around app content.
5. Add assertions modules only when multiple external test boundaries need the helpers.

## Stop Conditions

Stop and redesign if:

- notice API needs feature-specific action types.
- notice renderer needs repository or router implementation dependency.
- server error text is shown directly without a safe fallback policy.
- Toast becomes the retry/action surface for important errors.
- assertions need Android UI to verify basic notice emission.

## Verification

Pure notice API/assertions:

```bash
./gradlew :core:notice:api:test
```

Runtime notice host:

```bash
./gradlew :core:runtime:compileDebugKotlin
./gradlew :app:compileDebugKotlin
```

App state:

```bash
./gradlew :app:test --tests '*NotmidAppViewModelTest'
```
