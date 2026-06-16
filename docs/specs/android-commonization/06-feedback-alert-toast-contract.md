---
title: Feedback Alert Toast Contract
audience: Android engineers and AI agents
purpose: toast, snackbar, alert, inline, full-page feedback을 core-app 계약으로 분리한다.
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

Notmid currently has feedback-related contracts in two places.

```text
:core:model
  NotmidUiFeedback
  NotmidFeedbackPresentation
  NotmidFeedbackTone
  NotmidFeedbackAction
  NotmidUiEffect
  NotmidUiEffectDelegate

:core:designsystem
  NotmidFeedbackEffectHandler
  NotmidUiEffectLifecycleCollector
  NotmidSnackbarHost
  AlertDialog/Toast rendering
```

This works, but `core:model` now carries UI effect concepts and `core:designsystem` owns app-runtime feedback collection. In the target structure, feedback belongs to `core-app`.

## Decision

Create a dedicated feedback package family inside `:core-app` before adding more feedback behavior.

Target:

```text
:core-app
  feedback API package
  feedback implementation package
  feedback assertions in test source until external reuse proves a module boundary
```

Short-term compatibility:

- Existing `NotmidUiFeedback` can remain in `:core:model` until the new API exists.
- `NotmidFeedbackEffectHandler` can move later, not in the first module addition.
- Avoid broad renames in the same change that introduces new behavior.

## Feedback Contract

The API should express a feedback request, not a concrete UI widget.

Target concepts:

```text
FeedbackMessage
  message key or safe fallback text

FeedbackRequest
  id
  presentation
  tone
  message
  action
  dismiss policy

FeedbackPresentation
  Toast
  Snackbar
  Alert
  Inline
  FullPage

FeedbackAction
  label/message key
  semantic action: Retry, Dismiss, OpenDeepLink, SignIn
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

### Core-App Feedback API

API owns:

- stable feedback value types.
- feedback sink/source interfaces.
- lifecycle collection contract if pure enough.

### Core-App Feedback Impl

Impl owns:

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
:core-app feedback implementation package
  NotmidFeedbackHost
  NotmidFeedbackEffectCollector
  NotmidToastRenderer
  NotmidAlertRenderer

:core:designsystem
  NotmidSnackbarHost visual style
  NotmidButton
  NotmidSurface
  tokens
```

`NotmidFeedbackEffectHandler` can be split into:

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

1. Add a `:core-app` feedback API package with types equivalent to current `NotmidUiFeedback`.
2. Add `:core-app` feedback assertions in test source with recording sink.
3. Add adapters from old `NotmidUiFeedback` to new `FeedbackRequest` if needed.
4. Move `NotmidFeedbackEffectHandler` from `:core:designsystem` to the `:core-app` feedback implementation package.
5. Keep design-system visual components reusable and product-free.
6. Update app to use `FeedbackHost`.
7. Remove old duplicates from `:core:model` only after callers migrate.

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
./gradlew :core-app:testDebugUnitTest
```

Feedback impl:

```bash
./gradlew :core-app:compileDebugKotlin
./gradlew :app:compileDebugKotlin
```

App state:

```bash
./gradlew :app:test --tests '*NotmidAppViewModelTest'
```
