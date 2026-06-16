import assert from "node:assert/strict";
import { createPostgresNotmidRepository } from "./postgresNotmidRepository";
import { createSmokePostgresClient } from "./postgresNotmidRepositorySmokeFixtures";
import type { NotmidRepositoryResult } from "./notmidRepository";

type RepositorySuccess<T> = Extract<NotmidRepositoryResult<T>, { ok: true }>;
type RepositoryFailure<T> = Extract<NotmidRepositoryResult<T>, { ok: false }>;

const { actor, calls, client } = createSmokePostgresClient();
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
assertFailure(missingPlace);
assert.equal(missingPlace.status, 404);

const missingClip = await repository.getClip("missing-clip");
assertFailure(missingClip);
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
assertSuccess(published);
assert.equal(published.value.clip.id, "receipt-draft-001");
assert.equal(published.value.clip.creatorHandle, "verified");

const saved = await repository.saveClip("latte-line-was-worth-it", actor);
assertSuccess(saved);
assert.equal(saved.value.saved, true);
assert.equal(saved.value.clip.metrics.saves, 6);

const unknownSave = await repository.saveClip("missing-clip", actor);
assertFailure(unknownSave);
assert.equal(unknownSave.error.code, "clip_not_found");

const profileSettings = await repository.getProfileSettings(actor);
assert.equal(profileSettings.source, "api");
assert.equal(profileSettings.settings.user.homeNeighborhood, "Seongsu");
assert.equal(profileSettings.settings.privacy.savedPlacesVisibility, "private");

const updatedProfile = await repository.updateProfileSettings(
  { displayName: "Updated User", homeNeighborhood: "Hapjeong" },
  actor,
);
assertSuccess(updatedProfile);
assert.equal(updatedProfile.value.updated, true);
assert.equal(updatedProfile.value.settings.user.displayName, "Updated User");
assert.equal(updatedProfile.value.settings.user.homeNeighborhood, "Hapjeong");

const invalidProfile = await repository.updateProfileSettings(
  { displayName: "", homeNeighborhood: "Hapjeong" },
  actor,
);
assertFailure(invalidProfile);
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
assertFailure(invalidPublish);
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
assertFailure(unknownPlacePublish);
assert.equal(unknownPlacePublish.error.code, "place_not_found");

const startedFriendThread = await repository.startThread(
  {
    participantHandle: "min.zip",
    body: "Postgres friend start",
    attachedClipId: "latte-line-was-worth-it",
  },
  actor,
);
assertSuccess(startedFriendThread);
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
assertSuccess(duplicateFriendThread);
assert.equal(duplicateFriendThread.value.thread.id, startedFriendThread.value.thread.id);

const startedNonFriendThread = await repository.startThread(
  {
    participantHandle: "receipt.han",
    body: "Postgres non-friend start",
    attachedPlaceId: "neon-yard",
  },
  actor,
);
assertSuccess(startedNonFriendThread);
assert.equal(startedNonFriendThread.value.thread.chatAccess?.relationship, "non-friend");
assert.equal(startedNonFriendThread.value.thread.chatAccess?.inviteStatus, "pending-outbound");
assert.equal(startedNonFriendThread.value.thread.chatAccess?.canSendMessage, false);

const blockedStartedThreadReply = await repository.sendThreadMessage(
  startedNonFriendThread.value.thread.id,
  { body: "Postgres non-friend follow-up before acceptance" },
  actor,
);
assertFailure(blockedStartedThreadReply);
assert.equal(blockedStartedThreadReply.status, 403);
assert.equal(blockedStartedThreadReply.error.code, "chat_invite_required");

const unknownStartedThread = await repository.startThread(
  {
    participantHandle: "missing.handle",
    body: "Postgres unknown participant",
  },
  actor,
);
assertFailure(unknownStartedThread);
assert.equal(unknownStartedThread.error.code, "chat_participant_not_found");

const detail = await repository.getThreadDetail("tonight-seongsu");
assertSuccess(detail);
assert.equal(detail.value.messages[0].attachment?.type, "clip");
assert.equal(detail.value.attachedPlace?.id, "neon-yard");
assert.equal(detail.value.thread.chatAccess?.inviteStatus, "accepted");
assert.equal(detail.value.thread.chatAccess?.canSendMessage, true);

const pendingDetail = await repository.getThreadDetail("rain-route", actor);
assertSuccess(pendingDetail);
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
assertSuccess(reply);
assert.equal(reply.value.message.id, "msg-tonight-seongsu-verified");
assert.equal(reply.value.message.senderHandle, "verified");
assert.equal(reply.value.message.attachment?.type, "place");

const blockedReply = await repository.sendThreadMessage(
  "rain-route",
  { body: "Postgres adapter reply before acceptance" },
  actor,
);
assertFailure(blockedReply);
assert.equal(blockedReply.status, 403);
assert.equal(blockedReply.error.code, "chat_invite_required");

const acceptedInvite = await repository.respondThreadInvite("rain-route", "accept", actor);
assertSuccess(acceptedInvite);
assert.equal(acceptedInvite.value.thread.chatAccess?.inviteStatus, "accepted");
assert.equal(acceptedInvite.value.thread.chatAccess?.canSendMessage, true);

const replyAfterAccept = await repository.sendThreadMessage(
  "rain-route",
  { body: "Postgres adapter reply after acceptance" },
  actor,
);
assertSuccess(replyAfterAccept);
assert.equal(replyAfterAccept.value.message.threadId, "rain-route");

const duplicateInviteResponse = await repository.respondThreadInvite("rain-route", "reject", actor);
assertFailure(duplicateInviteResponse);
assert.equal(duplicateInviteResponse.error.code, "chat_invite_not_actionable");

const emptyReply = await repository.sendThreadMessage("tonight-seongsu", { body: "   " }, actor);
assertFailure(emptyReply);
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
assert.equal(
  threadInsertCall.values[0],
  "thread-firebase-friend-user-firebase-test-user-latte-line-was-worth-it",
);

const accessInsertCall = calls.find(
  (call) =>
    call.sql.includes("INSERT INTO notmid_chat_thread_access") &&
    call.values[0] === "thread-firebase-friend-user-firebase-test-user-latte-line-was-worth-it",
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
  threadListSql.indexOf("GROUP BY thread.id") <
    threadListSql.indexOf("ORDER BY thread.updated_at DESC"),
  "Thread list SQL must group before ordering.",
);
assert.ok(
  threadListSql.includes("ARRAY_REMOVE(ARRAY_AGG(participant.handle"),
  "Thread participant aggregation must remove null participant handles.",
);

console.log("Postgres repository adapter checks passed");

function assertSuccess<T>(result: NotmidRepositoryResult<T>): asserts result is RepositorySuccess<T> {
  assert.equal(result.ok, true);
}

function assertFailure<T>(result: NotmidRepositoryResult<T>): asserts result is RepositoryFailure<T> {
  assert.equal(result.ok, false);
}
