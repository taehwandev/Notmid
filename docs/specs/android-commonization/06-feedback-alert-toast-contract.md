---
title: Feedback Alert Toast Contract
audience: Android engineers and AI agents
purpose: toast, snackbar, alert, inline, full-page feedback을 API 계약과 core/app 런타임으로 분리한다.
status: draft
owner: notmid Android architecture
source_of_truth: docs/specs/android-commonization
last_verified: 2026-06-16
applies_to: UI effects, design system, app shell, ViewModel feedback
related_pages:
  - 05-network-error-contract.md
  - 07-state-assertions-testing.md
  - llm-wiki/design-system.md
---

# Feedback Alert Toast Contract

## Current State

Notmid feedback is split across a pure API contract, an app-runtime host, and
design-system visual primitives.

```text
:core:feedback:api
  model/ FeedbackRequest
  model/ FeedbackPresentation
  model/ FeedbackTone
  model/ FeedbackAction
  effect/ FeedbackEffect
  effect/ FeedbackEffectDelegate

:core:app
  feedback/host/ FeedbackHost
  feedback/host/ FeedbackEffectLifecycleCollector
  feedback/host/ FeedbackAlertDialog
  Android Toast/Snackbar/Alert dispatch

:core:designsystem
  NotmidSnackbarHost visual style
  tokens and reusable visual primitives
```

`core:model` does not own UI effect contracts. `core:designsystem` does not
collect flows, show Toasts, or render app-runtime alerts.

## Decision

Use a dedicated `:core:feedback:api` module before adding more feedback behavior.
Render and collect those effects from `:core:app`, not from feature screens or
the design system.

Target:

```text
:core:feedback:api
  model/
  effect/

:core:app
  feedback/host/

:core:designsystem
  visual feedback primitives only
```

Do not leave duplicate feedback contracts in `:core:model` once callers migrate.
Do not put lifecycle collection, Toast rendering, or app effects back into
`:core:designsystem`.

## Feedback Contract

The API should express a feedback request, not a concrete UI widget.

Target concepts:

```text
FeedbackRequest
  id
  presentation
  tone
  message
  action

FeedbackPresentation
  Toast
  Snackbar
  Alert
  Inline
  FullPage

FeedbackAction
  sealed interface for closed shared UI triggers
  current action: OpenDeepLink(label, deepLink)
  future actions: Retry, Dismiss, SignIn after a real caller exists
```

Do not make a reusable feedback action carry feature-specific sealed actions. Feature actions remain feature-owned.

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

Inline feedback is usually feature state, not a global UI effect.

### FullPage

Use for blocking content load failures, not one-off effects.

Full-page feedback should normally be represented in `UiState`, not as a transient event.

## Ownership

### Feature/ViewModel

Feature owns:

- user action.
- product copy key.
- whether retry/dismiss/sign-in is appropriate.
- local inline/full-page state.

Feature emits:

- `FeedbackRequest` or `UiState` failure.
- route/action event for follow-up, not direct UI code.

### Core Feedback API

API owns:

- stable feedback value types.
- feedback sink/source interfaces.
- one-shot `FeedbackEffect` contracts and delegate interfaces.

### Core-App Feedback Host

`:core:app` owns:

- Toast rendering.
- Snackbar host adapter.
- AlertDialog renderer.
- lifecycle-aware collection.
- Android resource/message resolving.

### Core-App Feedback Assertions

Assertions owns:

- `RecordingFeedbackSink`.
- `FakeFeedbackMessageResolver`.
- `FeedbackRequestAssertions`.
- helper fixtures for common error presentations.

## Message Policy

Long term, feedback messages should be message keys or resource ids where feasible.

Rules:

- ViewModel should not call `Context.getString`.
- low-level network/domain errors should not own final UI copy.
- safe server fallback messages may be carried, but feature/app decides whether to show them.
- `assertions` should compare message key or stable safe text, not localized platform string output.

## Action Policy

Feedback action is a UI trigger, not business logic.

Allowed:

```text
FeedbackAction.Retry
FeedbackAction.Dismiss
FeedbackAction.OpenDeepLink(uri)
FeedbackAction.SignIn
```

Feature/app maps that trigger to an owned action:

```text
FeedbackAction.Retry -> NotmidAppAction.ReloadContent
FeedbackAction.SignIn -> NotmidAppAction.ContinueAuth(...)
FeedbackAction.OpenDeepLink -> AppRouterRuntime.navigateDeepLink(...)
```

Forbidden:

```text
FeedbackAction(payload = InboxAction.SendMessage(...))
Feedback renderer calls repository
Toast click performs protected write
```

## Relationship To Design System

Design system owns visual primitives.

Feedback module owns runtime orchestration.

Target split:

```text
:core:app feedback/host package
  FeedbackHost
  FeedbackEffectLifecycleCollector
  FeedbackAlertDialog
  FeedbackActionHandler

:core:designsystem
  NotmidSnackbarHost visual style
  NotmidButton
  NotmidSurface
  tokens
```

The host split is:

```text
FeedbackHost
  effect collection and dispatch

AlertRenderer
  composable AlertDialog wrapper

ToastRenderer
  Android Toast boundary
```

## Assertions Examples

Recording sink:

```kotlin
val sink = RecordingFeedbackSink()
sink.show(FeedbackRequest.toast("Saved"))

sink.assertLast {
    hasPresentation(FeedbackPresentation.Toast)
    hasTone(FeedbackTone.Success)
}
```

Alert assertion:

```kotlin
sink.assertLastAlert {
    hasAction(FeedbackAction.SignIn)
    isCancellable()
}
```

The assertion helper should not require Compose runtime.

## Migration Steps

1. Keep pure value/effect contracts in `:core:feedback:api`.
2. Keep Android/Compose collection and rendering in `:core:app` `feedback/host`.
3. Keep design-system visual components reusable and product-free.
4. Update Activity shells to install `FeedbackHost` once around app content.
5. Add assertions modules only when multiple external test boundaries need the helpers.

## Stop Conditions

Stop and redesign if:

- feedback API needs feature-specific action types.
- feedback renderer needs repository or router implementation dependency.
- server error text is shown directly without a safe fallback policy.
- Toast becomes the retry/action surface for important errors.
- assertions need Android UI to verify basic feedback emission.

## Verification

Pure feedback API/assertions:

```bash
./gradlew :core:feedback:api:test
```

Feedback impl:

```bash
./gradlew :core:app:compileDebugKotlin
./gradlew :app:compileDebugKotlin
```

App state:

```bash
./gradlew :app:test --tests '*NotmidAppViewModelTest'
```
