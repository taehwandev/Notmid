# Notmid Android

Notmid is a short-video place discovery and place-aware chat product.
This repository contains its Android app and Android modules.

## Repositories

| Repository | Owner |
| --- | --- |
| `Notmid` | Android Compose app, modules, SDK adapters and client tests |
| `Notmid-web` | Next.js web UI, browser/session adapters and typed API client |
| `Notmid-server` | HTTP API, authorization, persistence, SQL migrations and canonical contracts |

The repositories build independently. Android consumes a
[pinned OpenAPI contract](docs/contracts/README.md); it does not import
TypeScript or run Node/pnpm during verification. Web consumes a versioned
contract snapshot owned by the server. No remote repository or deployment is
required for the local split.

## Android layout

```text
app/           Application/Activity entry, app coordination and DI assembly
core/          Android capabilities, domain ports and client adapters
feature/       Feature route contracts and Compose surfaces
build-logic/   Gradle convention plugins
docs/          Android architecture, product contracts and release guidance
llm-wiki/      Project-specific module and integration notes
```

The accepted module policy and remaining migration stages are in
[the target ARD](docs/specs/notmid-target-boundary-ard.md).
Service extraction does not mean the unfinished feature state decomposition is
complete: `NotmidAppViewModel` still coordinates feature content and writes.

## Run and verify

Start from the empty local configuration template:

```sh
cp local.properties.example local.properties
./gradlew :app:installDebug
bash scripts/verify-local.sh
```

Local verification runs secret/patch checks, all Android unit tests, and the
Debug APK build. For cached dependencies use:

```sh
bash scripts/verify-android.sh --offline
```

Debug uses the separately started Notmid-server API at
`http://10.0.2.2:8787` on an Android emulator by default. Set
`NOTMID_DEBUG_CONTENT_SOURCE=static` only when intentionally using the preserved
offline fixture mode. Server and web setup commands belong in their respective
repositories. Android release builds reject static content and fake auth.

Device and release checks:

```sh
bash scripts/verify-android-smoke.sh
bash scripts/verify-release-config.sh
bash scripts/verify-release-readiness.sh
```

Release readiness requires signing, version and environment inputs; see
[the Android release contract](docs/release/android-release-contract.md).
CI verifies Android and release configuration, without installing Node/pnpm.

## Integration and secrets

App DI selects repository and platform adapters. Product authorization and
database access belong to Notmid-server. Android retains its HTTP client,
response mapping, Credential Manager adapter and local presentation state.

Canonical product links remain `https://thdev.app/notmid/...`; Android currently
resolves them locally. See [routing](llm-wiki/routing-deeplinks.md).

Keep tokens, Firebase private credentials, signing keys and real environment
values in ignored local files or CI secret storage. Public client identifiers
still need provider restrictions. Commit only value-free configuration
templates. Run `bash scripts/verify-secret-hygiene.sh` before sharing changes.
