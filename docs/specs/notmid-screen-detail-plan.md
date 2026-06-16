# notmid Screen Detail Plan

This plan fills the product-detail gap before the next implementation passes. It defines what each screen should show, what states it needs, and which route/data contracts must stay stable across Android and web.

## Product Loop

The app should make this loop obvious without explaining it in UI copy:

1. Watch a fresh short clip tied to a place.
2. Check proof: freshness, distance, vibe, crowd, price, and credibility.
3. Save, open on map, or share into chat.
4. Visit the place and post a receipt clip.

Primary screen priority:

```text
Feed -> Place/Map -> Chat planning -> Capture receipt -> Profile credibility
```

## Shared Detail Rules

- Use `Notmid*` design-system wrappers in Android feature/app code.
- Use glass only for controls, nav, composers, overlays, preview sheets, and other functional surfaces.
- Keep cards stable in size; dynamic labels must not resize the surrounding layout.
- Signed-out users can browse Feed, Clip Detail, Map, and Place Detail.
- Capture, save, chat, and profile edit require auth.
- Fake data must be deterministic and shaped like real product data.
- Web routes and Android deep links must stay canonical:

```text
/notmid
/notmid/feed
/notmid/clips/{clipId}
/notmid/feed/clips/{clipId}
/notmid/map
/notmid/places/{placeId}
/notmid/map/places/{placeId}
/notmid/capture
/notmid/inbox
/notmid/chats/{threadId}
/notmid/inbox/chats/{threadId}
/notmid/profile
/notmid/profile/settings
```

## Feed

### Purpose

Feed is the proof stream. It should feel like short video first, with place context always attached.

### Primary Layout

- Full-height vertical clip stage.
- Media-derived background or simulated video surface.
- Floating place strip near lower third.
- Right-side action rail: like, save, chat/share, map/route.
- Bottom Liquid Glass navigation.
- Secondary list/stack may show more clips, but the first viewport should be video-dominant.

### Required States

- `SignedOutBrowsing`: actions that require auth route to login.
- `SignedInBrowsing`: save/chat/share actions are enabled.
- `MissingLocation`: feed falls back to trending proof nearby.
- `EmptyFeed`: explain no fresh receipts without becoming a marketing state.
- `ClipLoading`: stable skeleton dimensions.

### Data Needed

- clip id, title, caption/description, creator handle
- place id and place summary
- captured time/freshness label
- distance label
- mood tags
- metrics: likes, saves, comments
- video/cover asset or deterministic simulated media palette
- auth requirements for save/chat

### Android Work

- Keep `FeedScreen` as holder that receives `NotmidDestination`.
- Add a feed-specific UI path instead of only `NotmidDestinationContent`.
- Keep click output as `FeedRouteEvent.ClipRequested(clipId)`.
- Reuse `NotmidActionRail` and `NotmidClipCard` only where the feed surface is not the full-screen stage.

### Web Work

- Keep `/notmid` and `/notmid/feed` as product shell views, not landing pages.
- Add signed-out action routing to `/notmid/login?next=...`.
- Ensure clip links use canonical short and nested forms consistently.

### Acceptance

- First viewport communicates clip + place + action loop.
- Opening a clip detail preserves `[Feed, ClipDetail]` on Android.
- Save/chat interactions do not silently no-op when signed out.

## Clip Detail

### Purpose

Clip Detail is the receipt page. It should let a user inspect proof and jump to place/map/chat.

### Primary Layout

- Large media block.
- Creator row with credibility signal.
- Proof facts: captured time, freshness, distance, wait/crowd, price/context.
- Attached place card.
- Action row: save, open map, share to chat.
- Related clips from the same place.

### Required States

- `KnownClip`: hydrated clip and attached place.
- `UnknownClip`: valid route but no local fake match.
- `SignedOutAction`: save/chat route to login.

### Data Needed

- clip detail fields
- attached place summary
- proof metadata: quality, duration/progress, freshness, source
- related clip ids

### Android Work

- Replace current generic `NotmidRouteDetailContent` usage when product data is ready.
- Keep fallback for unknown local fake content.

### Web Work

- `/notmid/clips/{clipId}` and `/notmid/feed/clips/{clipId}` should render the same canonical content.

### Acceptance

- Detail page is useful even from a copied link.
- Unknown clip state is clear and non-crashing.

## Map

### Purpose

Map is the spatial view of fresh receipts. It should show where proof is happening now.

### Primary Layout

- Full-screen map-like canvas.
- Fake but stable map grid/streets until a real map SDK is introduced.
- Circular clip pins with thumbnail/palette and freshness ring.
- Category rail: Cafe, Work, Night, Exhibit, Walk.
- Selected place preview sheet over the map.
- Bottom Liquid Glass navigation remains legible over the canvas.

### Required States

- `NoLocationPermission`: default city/trending map.
- `CategorySelected`: pins/filter update without route change.
- `PinSelected`: preview sheet opens.
- `Clustered`: low-zoom grouped pins in fake mode.
- `PlaceDeepLinked`: route opens with selected preview/detail context.

### Data Needed

- place id, name, neighborhood, category
- lat/lng or fake x/y canvas position
- open state and score
- receipt count and highlighted clip id
- category filters
- freshness/crowd signal

### Android Work

- Replace generic `MapScreen` list with a map-specific UI.
- Add local selected pin state.
- Emit `MapRouteEvent.PlaceRequested(place.id)` from preview sheet/detail action.
- Keep `PlaceDetailRoute(placeId)` fallback for unmatched fake content.

### Web Work

- `MapBoard` should support selected pins and category filters.
- `/notmid/map/places/{placeId}` and `/notmid/places/{placeId}` should share content.

### Acceptance

- User can see multiple places spatially without reading a list.
- Selecting a pin gives enough context to decide whether to open detail.
- Existing map deep-link tests keep passing.

## Place Detail

### Purpose

Place Detail proves whether a place is worth visiting right now.

### Primary Layout

- Place hero with cover/media palette.
- Freshness summary: latest receipt time, receipt count, open state.
- Proof clips from that place.
- Map context: neighborhood, address, distance.
- Actions: save, route, share to chat.

### Required States

- `KnownPlace`
- `UnknownPlace`
- `SignedOutSaveOrChat`

### Data Needed

- place summary and address
- attached clip ids
- score, receipt count, open state
- vibe/category tags
- route/share metadata

### Acceptance

- A linked place is inspectable without navigating back to map.
- Route/share actions have signed-out handling.

## Capture

### Purpose

Capture lets a signed-in user record or upload a receipt tied to a place.

### Primary Layout

- Camera/upload preview surface.
- Composer panel:
  - caption
  - mood tags
  - place attach/search stub
  - freshness facts: wait, price, crowd, noise
  - visibility
  - draft status
- Primary publish action.
- Secondary save draft action.

### Required States

- `SignedOutGate`: login required.
- `EmptyDraft`: no media selected.
- `DraftReady`: media + required place attached.
- `Publishing`: progress and disabled inputs.
- `PublishFailed`: retry state.
- `Published`: route to clip detail or feed.

### Data Needed

- draft id
- local media placeholder
- caption
- selected mood tags
- attached place id
- visibility
- upload/publish progress
- validation errors

### Android Work

- Build a capture-specific screen in `feature/capture/impl`.
- Use shared design-system wrappers for inputs/buttons.
- Keep auth gate in shell or add a reusable `AuthGate` component only if multiple screens need it.
- Keep Android camera work inside `feature/capture/impl`: CameraX preview, runtime
  CAMERA permission, lifecycle resume checks, and local still capture are allowed
  there without leaking CameraX types into feature API, domain, or app shell
  contracts.
- Avoid Firebase upload/storage work in this phase.

### Web Work

- Upgrade `/notmid/capture` from simple panel to composer shell.
- Preserve login redirect for signed-out users.

### Acceptance

- Signed-in local fake user can fill a believable draft.
- Publish can be a deterministic fake completion, not a backend write yet.
- No real secrets or Firebase config are introduced.

## Inbox

### Purpose

Inbox is for place planning, not generic messaging.

### Primary Layout

- Thread list with attached clip/place preview.
- Unread count and participant handles/avatars.
- Filter/search row for people, clips, and places.
- Empty state focused on sharing a place or clip.

### Required States

- `SignedOutGate`
- `ThreadList`
- `EmptyInbox`
- `SearchFiltering`

### Data Needed

- thread id, title, preview
- participants
- unread count
- updated label
- attached place id
- attached clip id

### Android Work

- Replace `InboxScreen` generic destination content.
- Emit `InboxRouteEvent.ChatThreadRequested(threadId)`.
- Do not use clip/place id as thread id unless fake data explicitly maps it.

### Web Work

- Upgrade `/notmid/inbox` to show same thread context.

### Acceptance

- Thread rows explain what place/clip the conversation is about.
- Android and web use the same thread ids.

## Chat Thread

### Purpose

Chat Thread lets users plan around a clip, place, or route.

### Primary Layout

- Header with thread title and attached context.
- Messages grouped by time.
- Rich bubbles for attached clip/place/route.
- Floating composer:
  - text input
  - attach clip
  - attach place
  - attach route
  - send

### Required States

- `KnownThread`
- `UnknownThread`
- `ComposerEmpty`
- `ComposerHasText`
- `SendFailed`

### Data Needed

- messages with sender, timestamp, body
- attachment type and id
- participant handles
- moderation/report placeholder

### Android Work

- Add fake message data after thread model is ready.
- Keep composer local-only until write API exists.

### Web Work

- `/notmid/chats/{threadId}` and `/notmid/inbox/chats/{threadId}` should share rendering.

### Acceptance

- A chat can be understood from a deep link.
- Attachments are visibly tied to product objects.

## Profile

### Purpose

Profile shows creator credibility, saved receipts, places, routes, and account entry points.

### Primary Layout

- Header: avatar, handle, display name, home neighborhood, credibility signals.
- Stats: receipts, saves, places, routes.
- Tabs: Clips, Saved, Places, Routes.
- Settings entry.

### Required States

- `SignedOutGate`
- `OwnProfile`
- `EmptyTab`
- `SettingsEntry`

### Data Needed

- auth user
- posted clip ids
- saved place ids
- saved route ids
- roles/credibility signals

### Android Work

- Replace generic `ProfileScreen` destination content.
- Use fake auth user from `NotmidAuthState`.
- Keep profile edit behind settings/auth.

### Web Work

- Upgrade `/notmid/profile` to reflect signed-in fake user and saved content.

### Acceptance

- Signed-in profile feels owned by the current user.
- Signed-out state routes to login.

## Profile Settings

### Purpose

Settings owns account, privacy, auth provider, and open-source safety affordances.

### Primary Layout

- Account section: handle, display name, neighborhood.
- Privacy section: visibility, chat invites, saved places visibility.
- Auth section: provider, local fake mode, future Google/Firebase note.
- Open-source config section: no secrets committed, local config status.
- Danger/reporting section placeholder.

### Required States

- `SignedOutGate`
- `SignedInSettings`
- `FakeMode`
- `FirebaseUnavailable`

### Data Needed

- auth mode
- auth provider
- current user
- required auth actions
- local config flags later

### Android Work

- Replace route-stack demo cards with settings sections.
- Preserve deep link stack `[Profile, ProfileSettings]`.

### Web Work

- Upgrade `/notmid/profile/settings` to match Android section model.

### Acceptance

- It no longer reads like a routing demo.
- Open-source safety is visible but not part of the primary consumer loop.

## Cross-Screen Component Backlog

Build only when at least one screen needs the component immediately:

- `AuthGate`: reusable signed-out gate for protected routes/actions.
- `CaptureComposer`: camera/upload + caption + place attach + publish controls.
- `MapPin`: stable fake pin with freshness state.
- `PlacePreviewSheet`: selected place summary over map.
- `ThreadRow`: inbox item with attached object preview.
- `RichMessageBubble`: chat message with clip/place/route attachment.
- `ChatComposer`: text + attach actions.
- `ProfileHeader`: auth user and credibility summary.
- `SettingsSection`: compact grouped controls.

## API And Web Contract Backlog

Implement web/API only after the Android fake data shape for the same slice is stable.

### Route Baseline

- Add a contract helper for `/notmid/web?url={encodedUrl}` with `http`/`https` validation to match Android `WebViewRoute`.
- Decide explicitly whether unknown `/notmid/*` web paths should mirror Android null resolution or keep the current web fallback.
- Centralize `returnTo` normalization so API and web login do not drift.
- Add smoke coverage for:
  - `/notmid/capture`
  - `/notmid/inbox`
  - `/notmid/chats/{threadId}`
  - `/notmid/profile/settings`
  - `/notmid/feed/clips/{clipId}`
  - `/notmid/map/places/{placeId}`
  - `/notmid/web?url=https%3A%2F%2Fthdev.app%2Fhelp`

### Capture API

Candidate endpoints:

```text
GET /v1/capture/draft
POST /v1/capture/publish
```

DTOs needed:

- draft media placeholder
- caption
- mood tags
- attached place id
- visibility
- publish validation errors
- fake publish result with next clip path

### Map API

Candidate endpoint changes:

```text
GET /v1/map?category={category}
```

DTOs needed:

- stable category ids
- pin position or map-preview coordinates
- selected place preview data
- highlighted clip summary

### Inbox And Chat API

Candidate endpoints:

```text
GET /v1/inbox/threads
GET /v1/inbox/threads/{threadId}
POST /v1/inbox/threads/{threadId}/messages
```

DTOs needed:

- participant summaries
- messages
- attachment summaries for clip/place/route
- fake send-message response

### Profile And Settings API

Candidate endpoints:

```text
GET /v1/profile/me
GET /v1/profile/settings
PATCH /v1/profile/settings
```

DTOs needed:

- profile stats
- saved clips/places/routes
- privacy preferences
- auth provider and mode
- open-source/Firebase config status

## First Implementation Slices

Use these slices to keep agents parallel without conflicts:

1. A1: shared `AuthGate` and lightweight composer/sheet primitives in design/common modules.
2. A2: `CaptureScreen` product UI using existing fake data and local state.
3. A3: fake data model cleanup for clip-place-thread relationships.
4. A2: `MapScreen` product UI using the new fake data.
5. A2: `InboxScreen` and `ChatThreadScreen`.
6. A2: `ProfileScreen` and `ProfileSettingsScreen`.
7. A4: web parity after Android screen data contracts stabilize.

## Verification Matrix

Android UI slice:

```bash
./gradlew :app:compileDebugKotlin
git diff --check
```

Route/data slice:

```bash
./gradlew test
./gradlew :app:compileDebugKotlin
git diff --check
```

Web/API slice:

```bash
pnpm typecheck
bash scripts/smoke-web-api.sh
```

Milestone:

```bash
bash scripts/verify-local.sh
bash scripts/smoke-web-api.sh
```
