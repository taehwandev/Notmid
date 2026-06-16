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
  if [[ -x apps/api/node_modules/.bin/tsx ]]; then
    echo "API dependencies already installed"
  else
    pnpm_cmd install --frozen-lockfile
  fi
}

echo "== API Postgres repository dependencies =="
ensure_js_deps

echo "== API Postgres repository adapter =="
(
  cd apps/api
  node --import tsx --input-type=module <<'NODE'
import assert from "node:assert/strict";
import { createPostgresNotmidRepository } from "./src/postgresNotmidRepository.ts";

const actor = {
  userId: "firebase:test-user",
  handle: "verified",
  displayName: "Verified User",
  homeNeighborhood: "Seongsu",
  avatarImageUrl: "https://example.test/avatar.png",
  roles: ["creator"],
};

const user = {
  id: "firebase:test-user",
  handle: "verified",
  display_name: "Verified User",
  home_neighborhood: "Seongsu",
  avatar_image_url: "https://example.test/avatar.png",
  roles: ["creator"],
};

const friendUser = {
  id: "firebase:friend-user",
  handle: "min.zip",
  display_name: "Min Zip",
  home_neighborhood: "Seongsu",
  avatar_image_url: "https://example.test/min.png",
  roles: ["creator"],
};

const nonFriendUser = {
  id: "firebase:non-friend-user",
  handle: "receipt.han",
  display_name: "Receipt Han",
  home_neighborhood: "Euljiro",
  avatar_image_url: "https://example.test/receipt.png",
  roles: ["creator"],
};

const places = [
  {
    id: "neon-yard",
    name: "Neon Yard",
    neighborhood: "Seongsu",
    category: "night coffee",
    address: "Seongsu-dong",
    lat: 37.5446,
    lng: 127.0557,
    open_now: true,
    score: 92,
    cover_image_url: "https://example.test/neon-yard.jpg",
    receipt_count: 2,
  },
];

const clips = [
  {
    id: "latte-line-was-worth-it",
    title: "latte line was worth it",
    caption: "Foam art and fast seats.",
    creator_handle: "min.zip",
    place_id: "neon-yard",
    mood_tags: ["live rn"],
    cover_image_url: "https://example.test/clip.jpg",
    video_object_key: null,
    like_count: 12,
    save_count: 5,
    comment_count: 2,
    created_at: "2026-05-17T00:00:00.000Z",
  },
];

const threads = [
  {
    id: "tonight-seongsu",
    title: "tonight in seongsu?",
    preview: "Neon Yard?",
    updated_at: "2026-05-17T00:10:00.000Z",
    attached_place_id: "neon-yard",
    attached_clip_id: "latte-line-was-worth-it",
    participant_handles: ["min.zip", "verified"],
  },
  {
    id: "rain-route",
    title: "rain route",
    preview: "receipt.han wants to start a chat from the rain route",
    updated_at: "2026-05-17T00:20:00.000Z",
    attached_place_id: "neon-yard",
    attached_clip_id: "latte-line-was-worth-it",
    participant_handles: ["receipt.han", "verified"],
  },
];

const chatAccessByThreadId = new Map([
  [
    "tonight-seongsu",
    {
      chat_relationship: "friend",
      chat_invite_status: "accepted",
    },
  ],
  [
    "rain-route",
    {
      chat_relationship: "non-friend",
      chat_invite_status: "pending-inbound",
    },
  ],
]);

const messages = [
  {
    id: "msg-1",
    thread_id: "tonight-seongsu",
    sender_handle: "min.zip",
    body: "meet after 8?",
    created_at: "2026-05-17T00:11:00.000Z",
    attachment_clip_id: "latte-line-was-worth-it",
    attachment_place_id: null,
    mine: false,
  },
];

const calls = [];
function threadRowsFor(threadId) {
  return threads
    .filter((thread) => !threadId || thread.id === threadId)
    .map((thread) => ({
      ...thread,
      ...chatAccessByThreadId.get(thread.id),
    }));
}

const client = {
  async query(sql, values = []) {
    calls.push({ sql, values });
    const normalized = sql.replace(/\s+/g, " ").trim();

    if (normalized === "BEGIN" || normalized === "COMMIT" || normalized === "ROLLBACK") {
      return { rows: [] };
    }

    if (normalized.startsWith("INSERT INTO notmid_users")) {
      return { rows: [] };
    }

    if (normalized.startsWith("UPDATE notmid_users")) {
      return {
        rows: [
          {
            ...user,
            display_name: values[1],
            home_neighborhood: values[2],
          },
        ],
      };
    }

    if (normalized.includes("FROM notmid_users") && normalized.includes("WHERE id = $1")) {
      return { rows: values[0] === "firebase:test-user" ? [user] : [] };
    }

    if (normalized.includes("FROM notmid_users") && normalized.includes("WHERE handle = $1")) {
      if (values[0] === "min.zip") {
        return { rows: [friendUser] };
      }

      if (values[0] === "receipt.han") {
        return { rows: [nonFriendUser] };
      }

      return { rows: [] };
    }

    if (normalized.includes("FROM notmid_user_relationships")) {
      return {
        rows:
          values[0] === "firebase:test-user" && values[1] === "firebase:friend-user"
            ? [{ status: "friend" }]
            : [],
      };
    }

    if (normalized.startsWith("INSERT INTO notmid_clips")) {
      return {
        rows: [
          {
            ...clips[0],
            id: values[0],
            creator_handle: values[8],
            title: values[3],
            caption: values[4],
            place_id: values[2],
            mood_tags: values[6],
            cover_image_url: values[7],
          },
        ],
      };
    }

    if (normalized.startsWith("INSERT INTO notmid_chat_messages")) {
      return {
        rows: [
          {
            id: values[0],
            thread_id: values[1],
            sender_handle: values[6],
            body: values[3],
            created_at: "2026-05-17T00:12:00.000Z",
            attachment_clip_id: values[4],
            attachment_place_id: values[5],
            mine: true,
          },
        ],
      };
    }

    if (normalized.startsWith("INSERT INTO notmid_chat_threads")) {
      const [threadId, placeId, clipId, title, body] = values;
      if (!threads.some((thread) => thread.id === threadId)) {
        threads.push({
          id: threadId,
          title,
          preview: body,
          updated_at: "2026-05-17T00:30:00.000Z",
          attached_place_id: placeId,
          attached_clip_id: clipId,
          participant_handles: [],
        });
      }

      return { rows: [] };
    }

    if (normalized.startsWith("INSERT INTO notmid_chat_thread_participants")) {
      const [threadId, actorUserId, targetUserId] = values;
      const thread = threads.find((item) => item.id === threadId);
      if (thread) {
        const handles = [
          actorUserId === "firebase:test-user" ? user.handle : undefined,
          targetUserId === "firebase:friend-user" ? friendUser.handle : nonFriendUser.handle,
        ].filter(Boolean);
        thread.participant_handles = handles;
      }

      return { rows: [] };
    }

    if (normalized.startsWith("INSERT INTO notmid_chat_thread_access")) {
      const [threadId, actorUserId, , relationship, actorInviteStatus] = values;

      if (actorUserId === "firebase:test-user") {
        chatAccessByThreadId.set(threadId, {
          chat_relationship: relationship,
          chat_invite_status: actorInviteStatus,
        });
      }

      return { rows: [] };
    }

    if (normalized.startsWith("UPDATE notmid_chat_thread_access")) {
      const [threadId, userId, inviteStatus] = values;
      const current = chatAccessByThreadId.get(threadId);

      if (
        userId === "firebase:test-user" &&
        current?.chat_invite_status === "pending-inbound"
      ) {
        const nextAccess = {
          chat_relationship: current.chat_relationship,
          chat_invite_status: inviteStatus,
        };
        chatAccessByThreadId.set(threadId, nextAccess);
        return {
          rows: [
            {
              relationship: nextAccess.chat_relationship,
              invite_status: nextAccess.chat_invite_status,
            },
          ],
        };
      }

      return { rows: [] };
    }

    if (normalized.startsWith("WITH saved AS")) {
      return {
        rows: [
          {
            ...clips[0],
            save_count: Number(clips[0].save_count) + 1,
          },
        ],
      };
    }

    if (normalized.includes("FROM notmid_places place") && normalized.includes("WHERE place.id = $1")) {
      return { rows: values[0] === "neon-yard" ? places : [] };
    }

    if (normalized.includes("FROM notmid_places place")) {
      return { rows: places };
    }

    if (normalized.includes("FROM notmid_clips clip") && normalized.includes("WHERE clip.id = $1")) {
      return { rows: values[0] === "latte-line-was-worth-it" ? clips : [] };
    }

    if (normalized.includes("FROM notmid_clips clip")) {
      return { rows: clips };
    }

    if (normalized.includes("FROM notmid_clips") && normalized.includes("WHERE visibility = 'public'")) {
      return { rows: clips.map((clip) => ({ id: clip.id })) };
    }

    if (
      normalized.includes("FROM notmid_chat_threads thread") &&
      normalized.includes("WHERE thread.id = $1")
    ) {
      return { rows: threadRowsFor(values[0]) };
    }

    if (normalized.includes("FROM notmid_chat_threads thread")) {
      return { rows: threadRowsFor() };
    }

    if (normalized.includes("FROM notmid_chat_messages message")) {
      return { rows: values[0] === "tonight-seongsu" ? messages : [] };
    }

    throw new Error(`Unexpected SQL: ${normalized}`);
  },
};

const repository = createPostgresNotmidRepository(client, {
  generatedAt: () => "2026-05-17T00:00:00.000Z",
  messageId: (threadId) => `msg-${threadId}-verified`,
  receiptClipId: (draftId) => `receipt-${draftId}`,
});

const feed = await repository.getFeed();
assert.equal(feed.source, "api");
assert.equal(feed.clips[0].creatorHandle, "min.zip");
assert.equal(feed.places[0].receiptCount, 2);

const map = await repository.getMap();
assert.deepEqual(map.highlightedClipIds, ["latte-line-was-worth-it"]);

const missingPlace = await repository.getPlace("missing-place");
assert.equal(missingPlace.ok, false);
assert.equal(missingPlace.status, 404);

const missingClip = await repository.getClip("missing-clip");
assert.equal(missingClip.ok, false);
assert.equal(missingClip.error.code, "clip_not_found");

const published = await repository.publishCapture(
  {
    draftId: "draft-001",
    caption: "Postgres adapter smoke",
    placeId: "neon-yard",
    moodTags: ["adapter"],
    visibility: "public",
  },
  actor,
);
assert.equal(published.ok, true);
assert.equal(published.value.clip.id, "receipt-draft-001");
assert.equal(published.value.clip.creatorHandle, "verified");

const saved = await repository.saveClip("latte-line-was-worth-it", actor);
assert.equal(saved.ok, true);
assert.equal(saved.value.saved, true);
assert.equal(saved.value.clip.metrics.saves, 6);

const unknownSave = await repository.saveClip("missing-clip", actor);
assert.equal(unknownSave.ok, false);
assert.equal(unknownSave.error.code, "clip_not_found");

const profileSettings = await repository.getProfileSettings(actor);
assert.equal(profileSettings.source, "api");
assert.equal(profileSettings.settings.user.homeNeighborhood, "Seongsu");
assert.equal(profileSettings.settings.privacy.savedPlacesVisibility, "private");

const updatedProfile = await repository.updateProfileSettings(
  { displayName: "Updated User", homeNeighborhood: "Hapjeong" },
  actor,
);
assert.equal(updatedProfile.ok, true);
assert.equal(updatedProfile.value.updated, true);
assert.equal(updatedProfile.value.settings.user.displayName, "Updated User");
assert.equal(updatedProfile.value.settings.user.homeNeighborhood, "Hapjeong");

const invalidProfile = await repository.updateProfileSettings(
  { displayName: "", homeNeighborhood: "Hapjeong" },
  actor,
);
assert.equal(invalidProfile.ok, false);
assert.equal(invalidProfile.error.code, "missing_display_name");

const invalidPublish = await repository.publishCapture(
  {
    draftId: "draft-001",
    caption: "",
    placeId: "neon-yard",
    moodTags: ["adapter"],
    visibility: "public",
  },
  actor,
);
assert.equal(invalidPublish.ok, false);
assert.equal(invalidPublish.error.code, "missing_caption");

const unknownPlacePublish = await repository.publishCapture(
  {
    draftId: "draft-002",
    caption: "Unknown place should fail",
    placeId: "missing-place",
    moodTags: ["adapter"],
    visibility: "public",
  },
  actor,
);
assert.equal(unknownPlacePublish.ok, false);
assert.equal(unknownPlacePublish.error.code, "place_not_found");

const startedFriendThread = await repository.startThread(
  {
    participantHandle: "min.zip",
    body: "Postgres friend start",
    attachedClipId: "latte-line-was-worth-it",
  },
  actor,
);
assert.equal(startedFriendThread.ok, true);
assert.equal(startedFriendThread.value.thread.chatAccess?.relationship, "friend");
assert.equal(startedFriendThread.value.thread.chatAccess?.inviteStatus, "accepted");
assert.equal(startedFriendThread.value.thread.chatAccess?.canSendMessage, true);
assert.equal(startedFriendThread.value.message?.senderHandle, "verified");

const duplicateFriendThread = await repository.startThread(
  {
    participantHandle: "min.zip",
    body: "Postgres friend start duplicate",
    attachedClipId: "latte-line-was-worth-it",
  },
  actor,
);
assert.equal(duplicateFriendThread.ok, true);
assert.equal(duplicateFriendThread.value.thread.id, startedFriendThread.value.thread.id);

const startedNonFriendThread = await repository.startThread(
  {
    participantHandle: "receipt.han",
    body: "Postgres non-friend start",
    attachedPlaceId: "neon-yard",
  },
  actor,
);
assert.equal(startedNonFriendThread.ok, true);
assert.equal(startedNonFriendThread.value.thread.chatAccess?.relationship, "non-friend");
assert.equal(startedNonFriendThread.value.thread.chatAccess?.inviteStatus, "pending-outbound");
assert.equal(startedNonFriendThread.value.thread.chatAccess?.canSendMessage, false);

const blockedStartedThreadReply = await repository.sendThreadMessage(
  startedNonFriendThread.value.thread.id,
  { body: "Postgres non-friend follow-up before acceptance" },
  actor,
);
assert.equal(blockedStartedThreadReply.ok, false);
assert.equal(blockedStartedThreadReply.status, 403);
assert.equal(blockedStartedThreadReply.error.code, "chat_invite_required");

const unknownStartedThread = await repository.startThread(
  {
    participantHandle: "missing.handle",
    body: "Postgres unknown participant",
  },
  actor,
);
assert.equal(unknownStartedThread.ok, false);
assert.equal(unknownStartedThread.error.code, "chat_participant_not_found");

const detail = await repository.getThreadDetail("tonight-seongsu");
assert.equal(detail.ok, true);
assert.equal(detail.value.messages[0].attachment?.type, "clip");
assert.equal(detail.value.attachedPlace?.id, "neon-yard");
assert.equal(detail.value.thread.chatAccess?.inviteStatus, "accepted");
assert.equal(detail.value.thread.chatAccess?.canSendMessage, true);

const pendingDetail = await repository.getThreadDetail("rain-route", actor);
assert.equal(pendingDetail.ok, true);
assert.equal(pendingDetail.value.thread.chatAccess?.inviteStatus, "pending-inbound");
assert.equal(pendingDetail.value.thread.chatAccess?.canSendMessage, false);
assert.equal(pendingDetail.value.thread.chatAccess?.canAcceptInvite, true);

const inbox = await repository.getInbox();
assert.equal(inbox.source, "api");
assert.equal(inbox.threads[0].id, "tonight-seongsu");

const reply = await repository.sendThreadMessage(
  "tonight-seongsu",
  { body: "Postgres adapter reply", attachment: { type: "place", placeId: "neon-yard" } },
  actor,
);
assert.equal(reply.ok, true);
assert.equal(reply.value.message.id, "msg-tonight-seongsu-verified");
assert.equal(reply.value.message.senderHandle, "verified");
assert.equal(reply.value.message.attachment?.type, "place");

const blockedReply = await repository.sendThreadMessage(
  "rain-route",
  { body: "Postgres adapter reply before acceptance" },
  actor,
);
assert.equal(blockedReply.ok, false);
assert.equal(blockedReply.status, 403);
assert.equal(blockedReply.error.code, "chat_invite_required");

const acceptedInvite = await repository.respondThreadInvite("rain-route", "accept", actor);
assert.equal(acceptedInvite.ok, true);
assert.equal(acceptedInvite.value.thread.chatAccess?.inviteStatus, "accepted");
assert.equal(acceptedInvite.value.thread.chatAccess?.canSendMessage, true);

const replyAfterAccept = await repository.sendThreadMessage(
  "rain-route",
  { body: "Postgres adapter reply after acceptance" },
  actor,
);
assert.equal(replyAfterAccept.ok, true);
assert.equal(replyAfterAccept.value.message.threadId, "rain-route");

const duplicateInviteResponse = await repository.respondThreadInvite("rain-route", "reject", actor);
assert.equal(duplicateInviteResponse.ok, false);
assert.equal(duplicateInviteResponse.error.code, "chat_invite_not_actionable");

const emptyReply = await repository.sendThreadMessage("tonight-seongsu", { body: "   " }, actor);
assert.equal(emptyReply.ok, false);
assert.equal(emptyReply.error.code, "empty_message");

const upsertCall = calls.find((call) => call.sql.includes("INSERT INTO notmid_users"));
assert.ok(upsertCall, "Postgres writes must upsert the verified actor before mutations.");
assert.deepEqual(upsertCall.values.slice(0, 3), ["firebase:test-user", "verified", "Verified User"]);

const saveCall = calls.find((call) => call.sql.includes("INSERT INTO notmid_saved_clips"));
assert.ok(saveCall, "Postgres save must persist through notmid_saved_clips.");
assert.deepEqual(saveCall.values, ["firebase:test-user", "latte-line-was-worth-it"]);

const profileUpdateCall = calls.find((call) => call.sql.includes("UPDATE notmid_users"));
assert.ok(profileUpdateCall, "Postgres profile edit must update notmid_users.");
assert.deepEqual(profileUpdateCall.values, ["firebase:test-user", "Updated User", "Hapjeong"]);

const chatAccessUpdateCall = calls.find((call) =>
  call.sql.includes("UPDATE notmid_chat_thread_access"),
);
assert.ok(chatAccessUpdateCall, "Postgres chat invite response must update access state.");
assert.deepEqual(chatAccessUpdateCall.values, ["rain-route", "firebase:test-user", "accepted"]);

const relationshipSelectCall = calls.find((call) =>
  call.sql.includes("FROM notmid_user_relationships"),
);
assert.ok(relationshipSelectCall, "Postgres chat start must read server-owned relationship state.");
assert.deepEqual(relationshipSelectCall.values, ["firebase:test-user", "firebase:friend-user"]);

const threadInsertCall = calls.find((call) => call.sql.includes("INSERT INTO notmid_chat_threads"));
assert.ok(threadInsertCall, "Postgres chat start must create a chat thread.");
assert.equal(threadInsertCall.values[0], "thread-firebase-friend-user-firebase-test-user-latte-line-was-worth-it");

const accessInsertCall = calls.find((call) =>
  call.sql.includes("INSERT INTO notmid_chat_thread_access") &&
  call.values[0] === "thread-firebase-friend-user-firebase-test-user-latte-line-was-worth-it"
);
assert.ok(accessInsertCall, "Postgres chat start must create actor and target access rows.");
assert.deepEqual(accessInsertCall.values.slice(3), ["friend", "accepted", "accepted"]);

for (const call of calls) {
  assert.equal(
    call.sql.includes("firebase:test-user"),
    false,
    "SQL text must not interpolate actor ids directly.",
  );
  assert.equal(
    call.sql.includes("Postgres adapter smoke"),
    false,
    "SQL text must not interpolate request bodies directly.",
  );
}

const threadListCall = calls.find(
  (call) =>
    call.sql.includes("FROM notmid_chat_threads thread") &&
    call.sql.includes("ORDER BY thread.updated_at DESC"),
);
assert.ok(threadListCall, "Thread list SQL must be exercised.");
const threadListSql = threadListCall.sql.replace(/\s+/g, " ").trim();
assert.ok(
  threadListSql.indexOf("GROUP BY thread.id") < threadListSql.indexOf("ORDER BY thread.updated_at DESC"),
  "Thread list SQL must group before ordering.",
);
assert.ok(
  threadListSql.includes("ARRAY_REMOVE(ARRAY_AGG(participant.handle"),
  "Thread participant aggregation must remove null participant handles.",
);

console.log("Postgres repository adapter checks passed");
NODE
)

echo "verify-api-postgres-repository passed"
