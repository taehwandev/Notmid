# Firebase And Open Source

## Direction

notmid is server-first. Firebase is useful auxiliary infrastructure, but it should not be the long-term domain contract by default.

Target shape:

```text
Android / Web
  -> apps/api
      -> Postgres / Redis / Object Storage later
      -> Firebase Admin / FCM / App Check when useful
```

Firebase can support:

```text
Authentication
FCM
App Check
Crashlytics / Analytics
Emulator Suite
Hosting or App Hosting for web if useful
```

## Auth Rules

- Signed-out users can browse public feed/map.
- Sign-in is required for capture, upload, save, chat, profile editing, and moderation actions.
- Anonymous browsing should not require production Firebase config.
- Fake/local mode should remain usable without Firebase credentials.
- If Firebase Auth is used, clients send ID tokens to `apps/api`; the API verifies and maps them to notmid users.
- Web Firebase Auth uses public `NEXT_PUBLIC_NOTMID_FIREBASE_*` values only.
  The client obtains an anonymous ID token through Firebase Auth REST, sends it
  to the Next.js session bridge, and the bridge sets an HTTP-only cookie only
  after API verification succeeds. The refresh token is kept only in an
  HTTP-only cookie and exchanged server-side through the refresh route. The
  web Google path uses public `NEXT_PUBLIC_NOTMID_GOOGLE_CLIENT_ID`, receives a
  Google Identity Services ID token, exchanges it for a Firebase session through
  Next.js, and links to the current anonymous Firebase session when possible.
  The `/notmid` middleware may refresh expired or near-expired ID tokens before
  server-rendered product routes continue, after verifying the refreshed ID
  token with `apps/api`. Web sign-out must clear both Firebase cookies and the
  legacy fake session cookie server-side. Protected web write actions should
  retry once through the same server-side refresh path after API 401, then
  redirect to login if no verified session remains.

## Secret Rules

Never commit:

```text
API keys intended to remain private
service account JSON
production google-services.json
production GoogleService-Info.plist
private Firebase project IDs
MCP API keys
keystores
release signing passwords
```

Allowed:

```text
placeholder config files
documented local config names
emulator config examples
public-safe sample values clearly marked as examples
```

If a real key appears in chat or local files, treat it as exposed and recommend rotating it.

## Suggested Local Files

Use ignored local files for real config:

```text
local.properties
app/google-services.json
apps/api/.env.local
apps/web/.env.local
firebase.local.json
```

Keep examples explicit:

```text
google-services.example.json
.env.example
```

## Implementation Order

1. Keep deterministic fake repositories.
2. Keep API fixture endpoints and web fixture fallback working.
3. Add server auth/session contracts before client SDK wiring.
4. Add Android `:core:network:*` and `:core:auth:*` boundaries.
5. Add web Firebase Auth client wiring only after the API verification boundary
   exists.
6. Add Firebase Admin/FCM/App Check only behind `apps/api`.
7. Add production config loading only through ignored local files or secret stores.
8. Add App Check and server write policy before exposing public writes.

## Documentation

Update `docs/specs/firebase-auth-open-source-security.md` when changing auth, Firebase config, emulator setup, or secret-handling policy.
