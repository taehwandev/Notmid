---
title: Android authentication boundary
status: in-progress
owner: notmid Android
---

# Android authentication boundary

The app composes authentication through Hilt. Token-provider contracts belong
to `core:auth:api`, not the transport implementation. Android credential SDK
adapters and Firebase HTTP implementations consume those contracts independently.

## First migration unit

- Owner: `core:auth:api`, package `core.auth.token`.
- Exports: `GoogleIdTokenProvider`, `GoogleIdTokenResult`,
  `FirebaseIdTokenProvider`, and `FirebaseIdTokenResult`, one family per file.
- Allowed imports: Kotlin and the existing `core:model` authentication values.
- Forbidden imports: Android, credentials SDK, Hilt, HTTP, serialization, `app`,
  and `core:auth:impl`.
- Consumers: Firebase REST provider, API-verified gateway, unavailable providers,
  app credential adapter, app DI, and their existing tests.
- Verification: compile `core:auth:api` alone; run authentication and app unit
  tests; assemble the app to exercise Hilt integration.
- Compatibility: relocate Kotlin imports only. Preserve token result shapes,
  sign-in/sign-out behavior, HTTP requests, and failure codes.
- Collapse rule: reuse the existing auth API module; no additional contract
  module, facade, or compatibility alias is needed.

This first unit is serial because all token consumers must adopt the same
contract relocation. The pre-existing navigation/module edits are outside it.
Its review scope is the four contracts, their import consumers, and this note;
each new runtime file has one contract owner and fewer than 120 lines.

Verified on 2026-09-19: `:core:auth:api:compileKotlin`,
`:core:auth:impl:test`, `:app:testDebugUnitTest`, and `:app:assembleDebug`
passed with the existing dependency versions. The SDK 36.1 platform required by
the project was installed locally before the app checks. No runtime behavior,
SDK API usage, dependency version, or server contract changed in this unit.

## Remaining migration

The Android Credential Manager adapter still lives in `app/auth`. Move it and
its tests to an Android auth implementation boundary after the pure contracts
compile. Keep BuildConfig selection and app DI assembly in `app`.

The broader feature-state, data-fixture, app-shell, and web/API extraction work
remains governed by `notmid-target-boundary-ard.md`; this note does not declare
that migration complete. Web/API destination and history policy remain pending.
