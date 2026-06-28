# notmid Subagent TODO

This document is the working queue for building notmid with multiple agents without stepping on the same files.

## Operating Model

One coordinator owns sequencing, branch hygiene, and merge order. Subagents work from small handoff packets and report a patch summary plus verification results. A subagent should not edit files outside its assigned lane unless the coordinator expands the scope.

Default rule:

- Parallelize discovery and isolated implementation.
- Merge sequentially at explicit gates.
- Keep current dirty worktree changes intact until they are verified or intentionally replaced.
- Use deterministic fake data before network, Firebase, persistence, or new heavy dependencies.
- Keep Android, web, and API contracts aligned through URL/API shapes, not cross-language source imports.

## Agent Lanes

### A0 Coordinator

Owns:

- This TODO.
- Worktree status, conflict prevention, and final merge order.
- Verification gates.
- Cross-lane decisions about route, model, auth, and fake data contracts.

Reads:

- `llm-wiki/notmid-overview.md`
- `llm-wiki/module-map.md`
- AgentPlayBook Android/product guidance selected by the route manifest
- `docs/specs/notmid-screen-detail-plan.md`
- Current `git status --short`

Must not:

- Rewrite subagent patches during parallel work unless unblocking a merge conflict.

### A1 Android Shell And Design System

Owns:

- `core/designsystem/**`
- `feature/notmid/impl/**`
- `feature/notmid/common/**`

Focus:

- Liquid Glass bottom navigation, capture trailing action, responsive insets.
- Shared Notmid UI primitives: auth gate, capture composer shell, map pin, place preview, chat composer.
- Visual polish and stable dimensions.

Verification:

```bash
./gradlew :app:compileDebugKotlin
git diff --check
```

### A2 Android Feature Screens

Owns:

- `feature/feed/**`
- `feature/map/**`
- `feature/capture/**`
- `feature/inbox/**`
- `feature/profile/**`

Focus:

- Turn generic card lists into product-shaped screens.
- Keep feature impl modules emitting route events only.
- Avoid feature impl to feature impl dependencies.

Verification:

```bash
./gradlew :app:compileDebugKotlin
./gradlew test
```

### A3 Android Models, Data, Domain, Routing

Owns:

- `core/model/**`
- `core/domain/**`
- `core/data/**`
- `core/router/**`
- `app/src/main/java/app/thdev/glassnavlab/navigation/**`
- `app/src/test/java/app/thdev/glassnavlab/navigation/**`

Focus:

- Product-shaped fake data.
- Route stacks, deep links, and route event contracts.
- Auth state boundaries before real network/Firebase work.

Verification:

```bash
./gradlew test
./gradlew :app:compileDebugKotlin
git diff --check
```

### A4 Web And API

Owns:

- `apps/api/**`
- `apps/web/**`
- `packages/contracts/**`
- `packages/api-client/**`
- `scripts/smoke-web-api.sh`

Focus:

- Keep web routes canonical with Android deep links.
- Improve `/notmid/capture`, `/notmid/inbox`, `/notmid/profile/settings`.
- Add API/client contracts only after fake DTOs are stable.

Verification:

```bash
pnpm typecheck
bash scripts/smoke-web-api.sh
```

### A5 QA And Release Gate

Owns:

- Verification notes.
- Smoke plans.
- Screenshot/video refresh only when visual contract changes.

Focus:

- Run the narrowest useful gate after each merge.
- Run full local verification before a milestone close.
- Record blockers and pre-existing failures separately from new regressions.

Verification:

```bash
bash scripts/verify-local.sh
```

## Sequential Queue

### Phase 0A - Screen Detail Plan

Goal: fill product and screen detail gaps before implementing the next feature slices.

Owner: A0.

Tasks:

- [x] Define detailed Feed, Clip Detail, Map, Place Detail, Capture, Inbox, Chat Thread, Profile, and Profile Settings requirements.
- [x] Define required states and data needs per screen.
- [x] Define Android and web parity expectations.
- [x] Link implementation slices back to subagent lanes.

Exit gate:

- `docs/specs/notmid-screen-detail-plan.md` exists and is treated as the source for the next MVP slices.

### Phase 0 - Freeze And Verify Current Work

Goal: finish the current dirty Android changes before starting new product work.

Owner: A0 with A1/A3 support.

Tasks:

- [x] Identify whether `.antigravitycli/` should be ignored, committed, or left local.
- [x] Verify code-level Live Clip Card, capture trailing action, Liquid Glass tone, and AGP bump.
- [ ] Refresh unlocked Android visual capture for Live Clip Card, capture trailing action, and Liquid Glass tone.
- [x] Run `git diff --check`.
- [x] Run `./gradlew :app:compileDebugKotlin`.
- [x] Run `./gradlew test`.
- [x] Fix compile/test failures inside the current changed files only.
- [x] Decide whether current Android visual changes are ready to commit.

Exit gate:

- Current worktree is either clean after commit or intentionally documented as an active patch.

Current blocker:

- 2026-05-30: Debug APK installed and launched on the connected Android device;
  `app.thdev.glassnavlab` is running and focused behind keyguard. Actual app UI
  screenshot refresh remains blocked until the device is unlocked. Current
  decision: keep the patch active and do not treat it as visual-release-ready
  until `bash scripts/verify-android-smoke.sh` passes on an unlocked device or
  emulator.

### Phase 1 - Capture MVP

Goal: make Capture feel like a real receipt composer.

Primary owner: A2.

Support:

- A1 for shared composer/auth-gate components.
- A3 for fake draft model only if needed.

Tasks:

- [x] Replace generic `CaptureScreen` content with camera/upload surface.
- [x] Add caption field, mood tags, place attach, visibility, and publish state.
- [x] Keep signed-out capture behind existing auth flow.
- [x] Add deterministic fake draft data if UI needs initial state.
- [x] Verify `CaptureRoute` auth behavior still works.

Exit gate:

- `./gradlew :app:compileDebugKotlin`
- `git diff --check`

### Phase 2 - Map MVP

Goal: make Map a place-first discovery surface, not a card list.

Primary owner: A2.

Support:

- A1 for `MapPin` and `PlacePreviewSheet`.
- A3 for category/filter fake data if required.

Tasks:

- [x] Create full-screen map-like canvas with stable fake pins.
- [x] Add category rail: Cafe, Work, Night, Exhibit, Walk.
- [x] Add selected pin state and place preview sheet.
- [x] Ensure place clicks emit `MapRouteEvent.PlaceRequested`.
- [x] Keep `/notmid/map/places/{placeId}` deep link behavior unchanged.

Exit gate:

- `./gradlew :app:compileDebugKotlin`
- `./gradlew test`

### Phase 3 - Inbox And Chat MVP

Goal: make chat place-aware and clip-aware.

Primary owner: A2.

Support:

- A1 for `RichMessageBubble` and `ChatComposer`.
- A3 for fake thread/message models.

Tasks:

- [x] Replace generic Inbox card list with thread rows.
- [x] Add attached clip/place preview in thread rows.
- [x] Build chat thread screen with message groups.
- [x] Add composer with clip/place/route attachment actions.
- [x] Ensure thread clicks emit `InboxRouteEvent.ChatThreadRequested`.

Exit gate:

- `./gradlew :app:compileDebugKotlin`
- `./gradlew test`

### Phase 4 - Profile And Settings MVP

Goal: replace route-demo settings with account/profile surfaces.

Primary owner: A2.

Support:

- A3 for auth user fake data.
- A1 for reusable settings rows or profile header if needed.

Tasks:

- [x] Add profile header: avatar, handle, display name, neighborhood, roles.
- [x] Add tabs: Clips, Saved, Places, Routes.
- [x] Convert settings route into account/privacy/open-source config sections.
- [x] Keep `/notmid/profile/settings` resolving to `[Profile, ProfileSettings]`.

Exit gate:

- `./gradlew :app:compileDebugKotlin`
- `./gradlew test`

### Phase 5 - Android Data Contract Cleanup

Goal: make fake data represent the real product loop.

Primary owner: A3.

Support:

- A2 for screen data needs.
- A4 for web fixture parity.

Tasks:

- [x] Add explicit clip-place-thread relationships where screens need them.
- [x] Move quality/play-progress placeholders into model data or remove them.
- [x] Add category/filter data for map.
- [x] Add draft/capture state if Phase 1 needs it.
- [x] Keep `core:model` free of Compose, Android, `Color`, and `Dp`.

Exit gate:

- `./gradlew test`
- `./gradlew :app:compileDebugKotlin`
- `git diff --check`

### Phase 6 - Web Parity Pass

Goal: make web routes match the Android product shape.

Primary owner: A4.

Support:

- A0 for route parity decisions.
- A5 for smoke coverage.

Tasks:

- [x] Upgrade `/notmid/capture` from simple panel to composer shell.
- [x] Upgrade `/notmid/inbox` and chat pages with attached clip/place context.
- [x] Upgrade `/notmid/profile/settings` with real account/privacy sections.
- [x] Keep `/notmid` as app shell, not landing page.
- [x] Add smoke assertions for profile/settings primary surfaces.

Exit gate:

- `pnpm typecheck`
- `bash scripts/smoke-web-api.sh`

### Phase 7 - Network/Auth Boundary

Goal: prepare for server-backed Android without Firebase coupling.

Primary owner: A3.

Support:

- A4 for API shape.

Tasks:

- [x] Add `:core:network:api` and `:core:network:impl` only after API DTOs stabilize.
- [x] Add `:core:auth:api` and `:core:auth:impl` for anonymous/Google-ready contracts.
- [x] Keep Firebase optional and behind API verification.
- [x] Do not commit real Firebase config, API secrets, or service accounts.

Exit gate:

- `./gradlew help`
- `./gradlew :app:compileDebugKotlin`
- `./gradlew test`

### Phase 8 - Full Local Gate

Goal: close the milestone with confidence.

Owner: A5.

Tasks:

- [x] Run `bash scripts/verify-local.sh`.
- [x] Run `bash scripts/smoke-web-api.sh`.
- [x] Add reproducible Android visual smoke script.
- [ ] Refresh screenshots or captures only if visual contract changed.
- [x] Record known residual risks in the final handoff.

Current visual note:

- 2026-05-30: Android visual smoke captured only the locked device screen. Keep
  screenshot refresh open until an unlocked device or emulator is available.
  Run `bash scripts/verify-android-smoke.sh` after unlocking to install, launch,
  foreground-check, and capture the app UI.

Exit gate:

- Full local verification passes or failures are clearly categorized as pre-existing/blocking.

## Parallelization Rules

Safe to run in parallel:

- A4 web/API work while A1/A2 Android UI work is happening, if no shared route/DTO changes are needed.
- A1 shared component exploration while A2 writes feature screen structure, if A1 does not change feature files.
- A5 verification in a separate worktree or after a subagent exports a patch.

Do not run in parallel:

- A1 and A2 editing `feature/notmid/common/**`.
- A2 and A3 editing the same feature API route contracts.
- A3 and A4 changing canonical route shapes without coordinator approval.
- Any agent changing `gradle/libs.versions.toml` while another build-logic task is active.

## Handoff Template

Each subagent should report:

```text
Lane:
Scope:
Files changed:
Behavior changed:
Verification run:
Failures/blockers:
Next recommended task:
```

## Current Starting State

As of this TODO creation, the repo has active Android changes in:

- `core/data/src/main/kotlin/app/thdev/glassnavlab/core/data/notmid/StaticNotmidContentRepository.kt`
- `core/designsystem/src/main/java/app/thdev/glassnavlab/core/designsystem/component/NotmidBottomNavigation.kt`
- `core/designsystem/src/main/java/app/thdev/glassnavlab/core/designsystem/component/liquidglass/LiquidGlassAgslOverlay.kt`
- `core/designsystem/src/main/java/app/thdev/glassnavlab/core/designsystem/component/liquidglass/LiquidGlassBottomNavigation.kt`
- `core/model/src/main/kotlin/app/thdev/glassnavlab/core/model/notmid/NotmidModels.kt`
- `feature/feed/impl/src/main/java/app/thdev/glassnavlab/feature/feed/FeedScreen.kt`
- `feature/inbox/impl/src/main/java/app/thdev/glassnavlab/feature/inbox/InboxScreen.kt`
- `feature/map/impl/src/main/java/app/thdev/glassnavlab/feature/map/MapScreen.kt`
- `feature/notmid/common/src/main/java/app/thdev/glassnavlab/feature/notmid/common/components/NotmidClipCard.kt`
- `feature/notmid/common/src/main/java/app/thdev/glassnavlab/feature/notmid/common/model/NotmidUiModels.kt`
- `feature/notmid/impl/src/main/java/app/thdev/glassnavlab/feature/notmid/NotmidShellScreen.kt`
- `feature/profile/impl/src/main/java/app/thdev/glassnavlab/feature/profile/ProfileScreen.kt`
- `gradle/libs.versions.toml`

There is also an untracked `.antigravitycli/` directory. Treat it as user/local state until the coordinator decides otherwise.
