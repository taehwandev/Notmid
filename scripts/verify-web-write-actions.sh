#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

pnpm_cmd() {
  if command -v pnpm >/dev/null 2>&1; then
    pnpm "$@"
  else
    npm exec --yes pnpm@10.12.1 -- "$@"
  fi
}

ensure_js_deps() {
  if [[ -x apps/api/node_modules/.bin/tsx && -x apps/web/node_modules/.bin/next ]]; then
    echo "Web/API dependencies already installed"
  else
    pnpm_cmd install --frozen-lockfile
  fi
}

echo "== Web write action dependencies =="
ensure_js_deps

echo "== Web protected write action static checks =="
rg -q "class NotmidApiRequestError" packages/api-client/src/index.ts
rg -q "apiError\\?: NotmidErrorResponse" packages/api-client/src/index.ts
rg -q "parseApiError" packages/api-client/src/index.ts
rg -q "isNotmidApiRequestError\\(error, 401\\)" apps/web/src/lib/notmidServerActions.ts
rg -q "refreshNotmidFirebaseIdToken" apps/web/src/lib/notmidServerActions.ts
rg -q "getAuthStatus\\(refreshedSession.idToken\\)" apps/web/src/lib/notmidServerActions.ts
rg -q "notmidRoutes.login\\(returnTo\\)" apps/web/src/lib/notmidServerActions.ts

rg -q "action=\\{publishCaptureFromWeb\\}" apps/web/src/app/notmid/capture/page.tsx
rg -q "publishCapture\\(request, accessToken\\)" apps/web/src/app/notmid/capture/page.tsx
rg -q "revalidatePath\\(notmidRoutes.feed\\)" apps/web/src/app/notmid/capture/page.tsx

rg -q "action=\\{sendThreadMessageFromWeb\\}" 'apps/web/src/app/notmid/chats/[threadId]/page.tsx'
rg -q "sendThreadMessage\\(threadId, \\{ body \\}, accessToken\\)" \
  'apps/web/src/app/notmid/chats/[threadId]/page.tsx'
rg -q "action=\\{acceptThreadInviteFromWeb\\}" 'apps/web/src/app/notmid/chats/[threadId]/page.tsx'
rg -q "acceptThreadInvite\\(threadId, accessToken\\)" \
  'apps/web/src/app/notmid/chats/[threadId]/page.tsx'
rg -q "action=\\{rejectThreadInviteFromWeb\\}" 'apps/web/src/app/notmid/chats/[threadId]/page.tsx'
rg -q "rejectThreadInvite\\(threadId, accessToken\\)" \
  'apps/web/src/app/notmid/chats/[threadId]/page.tsx'
rg -q "revalidatePath\\(notmidRoutes.inbox\\)" 'apps/web/src/app/notmid/chats/[threadId]/page.tsx'

rg -q "export async function saveClipFromWeb" apps/web/src/lib/notmidClipActions.ts
rg -q "runNotmidAuthenticatedApiAction\\(returnTo" apps/web/src/lib/notmidClipActions.ts
rg -q "saveClip\\(clipId, accessToken\\)" apps/web/src/lib/notmidClipActions.ts
rg -q "revalidatePath\\(notmidRoutes.feed\\)" apps/web/src/lib/notmidClipActions.ts
rg -q "action=\\{saveClipFromWeb\\}" apps/web/src/components/NotmidProductShell.tsx
rg -q "action=\\{saveClipFromWeb\\}" 'apps/web/src/app/notmid/clips/[clipId]/page.tsx'
rg -q "saveClip: \\(clipId: string, accessToken\\?: string\\)" packages/api-client/src/index.ts
rg -q "app.post\\(\"/v1/clips/:clipId/save\"" apps/api/src/server.ts
rg -q "action=\\{startThreadFromClipWeb\\}" 'apps/web/src/app/notmid/clips/[clipId]/page.tsx'
rg -q "startThread\\(" 'apps/web/src/app/notmid/clips/[clipId]/page.tsx'
rg -q "startThread: \\(" packages/api-client/src/index.ts
rg -q "app.post\\(\"/v1/inbox/threads\"" apps/api/src/server.ts

rg -q "action=\\{updateProfileSettingsFromWeb\\}" apps/web/src/app/notmid/profile/settings/page.tsx
rg -q "updateProfileSettings\\(request, accessToken\\)" apps/web/src/app/notmid/profile/settings/page.tsx
rg -q "revalidatePath\\(notmidRoutes.profile\\)" apps/web/src/app/notmid/profile/settings/page.tsx
rg -q "getProfileSettings\\(accessToken\\)" apps/web/src/app/notmid/profile/settings/page.tsx
rg -q "updateProfileSettings: \\(" packages/api-client/src/index.ts
rg -q "app.patch\\(\"/v1/profile/settings\"" apps/api/src/server.ts

if rg -q "localStorage|sessionStorage|browserLocalPersistence" \
  apps/web/src/lib/notmidServerActions.ts \
  apps/web/src/lib/notmidClipActions.ts \
  apps/web/src/app/notmid/capture/page.tsx \
  'apps/web/src/app/notmid/chats/[threadId]/page.tsx' \
  'apps/web/src/app/notmid/clips/[clipId]/page.tsx' \
  apps/web/src/app/notmid/profile/settings/page.tsx \
  apps/web/src/components/NotmidProductShell.tsx; then
  echo "Web write actions must not store auth state in browser storage." >&2
  exit 1
fi

echo "verify-web-write-actions passed"
