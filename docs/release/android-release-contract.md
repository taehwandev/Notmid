# notmid Android Release Contract

This is the repo-local contract for Android release candidates and production
artifacts. It defines the values that must be injected from ignored local config
or CI secret storage before a build can be treated as distributable.

## Artifact

- Platform: Android
- Application ID: `app.thdev.glassnavlab`
- Source module: `:app`
- Output checked by CI-safe gate: APK from `./gradlew :app:assembleRelease`
- Distribution-ready gate: `bash scripts/verify-release-readiness.sh`

The release APK can assemble unsigned for open-source validation. An unsigned
artifact is never distributable.

## Versioning

User-facing Android `versionName` uses monthly CalVer:

```text
scheme: calver-monthly
pattern: YY.MM.N
timezone: UTC
release unit: signed Android artifact intended for an internal, staging, or production track
example: 26.05.1
tag prefix: android-v
tag example: android-v26.05.1
prerelease example: 26.05.1-rc.1
```

`N` starts at `1` each UTC month and increments only for signed artifacts that
are intentionally promoted to an Android distribution track. Failed local builds
and unsigned CI artifacts do not consume `N`.

Android `versionCode` must be a monotonically increasing integer. Use:

```text
YYMMNNBB
```

- `YYMMNN` mirrors the `versionName` release line.
- `BB` is a two-digit uploaded build number for that release line.
- Example: `26.05.1` first signed candidate uses `26050101`.
- A later candidate or forward-fix for the same release line increments `BB`.

Before distribution, override the local baseline values:

```text
NOTMID_VERSION_NAME=26.05.1
NOTMID_VERSION_CODE=26050101
```

The checked-in fallback `1 / 1.0` is local-only. `bash scripts/verify-release-readiness.sh`
must fail while the artifact still uses that fallback.

## Environment Inputs

Release builds must use release-specific environment names:

```text
NOTMID_RELEASE_API_BASE_URL=https://thdev.app
NOTMID_RELEASE_AUTH_MODE=disabled
NOTMID_RELEASE_FIREBASE_API_KEY=
NOTMID_RELEASE_FIREBASE_AUTH_REQUEST_URI=https://thdev.app/notmid/firebase-auth/android
NOTMID_RELEASE_GOOGLE_SERVER_CLIENT_ID=
NOTMID_RELEASE_CONTENT_SOURCE=api
NOTMID_RELEASE_MAP_PROVIDER=fake
NOTMID_VERSION_CODE=26050101
NOTMID_VERSION_NAME=26.05.1
```

`NOTMID_RELEASE_API_BASE_URL` must be HTTPS and must not point at the Android
emulator host. Release builds must not inherit `NOTMID_DEBUG_API_BASE_URL` or
`NOTMID_API_BASE_URL`.

`NOTMID_RELEASE_AUTH_MODE` must be `firebase` or `disabled`. The local fake auth
mode is allowed for debug builds only and must not be packaged into a release
candidate. When `NOTMID_RELEASE_AUTH_MODE=firebase`, the release build must
inject a public, provider-restricted `NOTMID_RELEASE_FIREBASE_API_KEY` and a
public `NOTMID_RELEASE_GOOGLE_SERVER_CLIENT_ID` for Android Credential Manager
Sign in with Google. These values are client-visible identifiers, not OAuth
client secrets, and must be restricted by package name, signing certificate
fingerprint, API scope, and quota.

`NOTMID_RELEASE_CONTENT_SOURCE` must be `api`. Debug builds default to
`NOTMID_DEBUG_CONTENT_SOURCE=api` against the local fixture API server and may
override to `static` only for intentional offline development, but a release
candidate must not package static fixture content as the primary content source.

Map provider values are client-visible. If `google` or `mapbox` is used, the
corresponding key or token must be provider-restricted by package name, signing
certificate fingerprint, API scope, and quota.

## Signing Inputs

Signing values must come from ignored `local.properties`, an OS secret store, or
the CI provider secret store:

```text
NOTMID_RELEASE_STORE_FILE=/path/to/upload-keystore.jks
NOTMID_RELEASE_STORE_PASSWORD=
NOTMID_RELEASE_KEY_ALIAS=
NOTMID_RELEASE_KEY_PASSWORD=
```

Do not commit keystores, passwords, upload keys, Play Console credentials, or
generated secret files. If CI stores a base64-encoded keystore, decode it into a
temporary workspace path during the job and set `NOTMID_RELEASE_STORE_FILE` to
that path before running Gradle.

## Required Gates

Before publishing or uploading an Android release artifact:

1. Run `bash scripts/verify-secret-hygiene.sh`.
2. Run `bash scripts/verify-local.sh`.
3. Run `bash scripts/smoke-web-api.sh`.
4. Run `bash scripts/verify-release-config.sh`.
5. Run `bash scripts/verify-android-smoke.sh` on an unlocked device or emulator.
6. Run VibeGuard audit.
7. Run `bash scripts/verify-release-readiness.sh` with signing, version, and
   release environment inputs set.

`verify-release-config` is CI-safe and may pass with an unsigned artifact.
`verify-release-readiness` is the distribution gate and must fail if the APK is
unsigned, still uses `1 / 1.0`, points at the emulator API host, or packages
static fixture content as the release content source. It also fails when
Firebase release auth is enabled without the public Firebase API key or Google
server client id required by the Android login path.

## Tagging And Promotion

Create the release tag only after the intended source revision and signed
artifact pass the required gates:

```text
android-v26.05.1
android-v26.05.1-rc.1
```

Do not move an existing published tag to a later commit. If a post-release fix
changes app behavior or the artifact, publish a new version or release candidate.

## Rollback And Forward Fix

Android distribution tracks require monotonically increasing `versionCode`, so
rollback normally means promoting a previous artifact that is still accepted by
the track or shipping a forward-fix with a higher `versionCode`.

Server and API changes must remain backward compatible with at least the current
Android release candidate. If a production API or auth change cannot be backward
compatible, it needs a separate migration and rollout plan before the Android
artifact is promoted.
