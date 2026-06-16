import type {
  NotmidCapturePublishRequest,
  NotmidChatInviteStatus,
  NotmidChatRelationship,
  NotmidProfileSettingsUpdateRequest,
  NotmidStartThreadRequest,
} from "@notmid/contracts";
import {
  fallbackPostgresStartedThread,
  insertPostgresStartedThread,
  postgresStartThreadId,
  selectPostgresChatUserByHandle,
  selectPostgresUsersAreFriends,
} from "./postgresChatRepository";
import {
  localFixtureActor,
  profileSettingsResponseForActor,
  repositoryFailure as failure,
  repositorySuccess as success,
  validateCapturePublishRequest,
  validateProfileSettingsUpdateRequest,
  validateStartThreadRequest,
  type NotmidRepository,
} from "./notmidRepository";
import {
  selectClip,
  selectClips,
  selectHighlightedClipIds,
  selectMessages,
  selectPlace,
  selectPlaces,
  selectThread,
  selectThreads,
  selectUser,
  updateThreadAccess,
  upsertActor,
} from "./postgresNotmidQueries";
import {
  actorForUserRow,
  mapClipRow,
  mapMessageRow,
  type ClipRow,
  type MessageRow,
  type UserRow,
} from "./postgresNotmidRows";
import type { PostgresQueryClient } from "./postgresNotmidQueryTypes";

export type { PostgresQueryClient, PostgresQueryResult } from "./postgresNotmidQueryTypes";

export type PostgresNotmidRepositoryOptions = {
  generatedAt?: () => string;
  messageId?: (threadId: string) => string;
  receiptClipId?: (draftId: string) => string;
};

export function createPostgresNotmidRepository(
  client: PostgresQueryClient,
  options: PostgresNotmidRepositoryOptions = {},
): NotmidRepository {
  const generatedAt = options.generatedAt ?? (() => new Date().toISOString());
  const messageId = options.messageId ?? ((threadId) => `msg-${threadId}-${Date.now()}`);
  const receiptClipId = options.receiptClipId ?? ((draftId) => `receipt-${draftId}`);

  return {
    async getCaptureDraft() {
      return {
        source: "api",
        generatedAt: generatedAt(),
        draft: {
          id: "draft-server-receipt",
          caption: "",
          moodTags: [],
          visibility: "public",
          mediaState: "empty",
        },
        candidatePlaces: await selectPlaces(client),
      };
    },
    async publishCapture(request, actor = localFixtureActor()) {
      const validationError = validateCapturePublishRequest(request);

      if (validationError) {
        return failure(400, validationError);
      }

      const publishRequest = request as NotmidCapturePublishRequest;
      const place = await selectPlace(client, publishRequest.placeId);

      if (!place) {
        return failure(404, { code: "place_not_found", message: "Place not found." });
      }

      await upsertActor(client, actor);

      const clipId = receiptClipId(publishRequest.draftId);
      const title = publishRequest.caption.slice(0, 48);
      const result = await client.query<ClipRow>(
        `
          INSERT INTO notmid_clips (
            id,
            owner_user_id,
            place_id,
            title,
            caption,
            visibility,
            mood_tags,
            cover_image_url,
            moderation_status
          )
          VALUES ($1, $2, $3, $4, $5, $6, $7, $8, 'queued')
          RETURNING
            id,
            title,
            caption,
            $9::TEXT AS creator_handle,
            place_id,
            mood_tags,
            cover_image_url,
            video_object_key,
            like_count,
            save_count,
            comment_count,
            created_at
        `,
        [
          clipId,
          actor.userId,
          publishRequest.placeId,
          title,
          publishRequest.caption,
          publishRequest.visibility,
          publishRequest.moodTags,
          place.coverImageUrl,
          actor.handle,
        ],
      );

      return success({
        source: "api",
        generatedAt: generatedAt(),
        clip: mapClipRow(result.rows[0]),
        moderationStatus: "queued",
      });
    },
    async getFeed() {
      const [clips, places] = await Promise.all([selectClips(client), selectPlaces(client)]);

      return {
        source: "api",
        generatedAt: generatedAt(),
        clips,
        places,
      };
    },
    async getMap() {
      const [places, highlightedClipIds] = await Promise.all([
        selectPlaces(client),
        selectHighlightedClipIds(client),
      ]);

      return {
        source: "api",
        generatedAt: generatedAt(),
        places,
        highlightedClipIds,
      };
    },
    async getClip(clipId) {
      const clip = await selectClip(client, clipId);

      return clip ? success(clip) : failure(404, { code: "clip_not_found", message: "Clip not found." });
    },
    async saveClip(clipId, actor = localFixtureActor()) {
      const existingClip = await selectClip(client, clipId);

      if (!existingClip) {
        return failure(404, { code: "clip_not_found", message: "Clip not found." });
      }

      await upsertActor(client, actor);

      const result = await client.query<ClipRow>(
        `
          WITH saved AS (
            INSERT INTO notmid_saved_clips (user_id, clip_id)
            VALUES ($1, $2)
            ON CONFLICT DO NOTHING
            RETURNING clip_id
          ),
          bumped AS (
            UPDATE notmid_clips clip
            SET
              save_count = clip.save_count + CASE WHEN EXISTS (SELECT 1 FROM saved) THEN 1 ELSE 0 END,
              updated_at = NOW()
            WHERE clip.id = $2
            RETURNING clip.*
          )
          SELECT
            bumped.id,
            bumped.title,
            bumped.caption,
            creator.handle AS creator_handle,
            bumped.place_id,
            bumped.mood_tags,
            bumped.cover_image_url,
            bumped.video_object_key,
            bumped.like_count,
            bumped.save_count,
            bumped.comment_count,
            bumped.created_at
          FROM bumped
          JOIN notmid_users creator ON creator.id = bumped.owner_user_id
        `,
        [actor.userId, clipId],
      );

      return success({
        source: "api",
        generatedAt: generatedAt(),
        clip: result.rows[0] ? mapClipRow(result.rows[0]) : existingClip,
        saved: true,
      });
    },
    async getProfileSettings(actor = localFixtureActor()) {
      const user = await selectUser(client, actor.userId);
      return profileSettingsResponseForActor(user ? actorForUserRow(user) : actor, generatedAt());
    },
    async updateProfileSettings(request, actor = localFixtureActor()) {
      const validationError = validateProfileSettingsUpdateRequest(request);

      if (validationError) {
        return failure(400, validationError);
      }

      await upsertActor(client, actor);

      const updateRequest = request as NotmidProfileSettingsUpdateRequest;
      const result = await client.query<UserRow>(
        `
          UPDATE notmid_users
          SET
            display_name = $2,
            home_neighborhood = $3,
            updated_at = NOW()
          WHERE id = $1
          RETURNING
            id,
            handle,
            display_name,
            home_neighborhood,
            avatar_image_url,
            roles
        `,
        [
          actor.userId,
          updateRequest.displayName.trim(),
          updateRequest.homeNeighborhood.trim(),
        ],
      );

      return success({
        ...profileSettingsResponseForActor(actorForUserRow(result.rows[0]), generatedAt()),
        updated: true,
      });
    },
    async getPlace(placeId) {
      const place = await selectPlace(client, placeId);

      return place
        ? success(place)
        : failure(404, { code: "place_not_found", message: "Place not found." });
    },
    async getInbox(actor) {
      return {
        source: "api",
        generatedAt: generatedAt(),
        threads: await selectThreads(client, actor),
      };
    },
    async getThread(threadId, actor) {
      const thread = await selectThread(client, threadId, actor);

      return thread
        ? success(thread)
        : failure(404, { code: "thread_not_found", message: "Thread not found." });
    },
    async getThreadDetail(threadId, actor) {
      const thread = await selectThread(client, threadId, actor);

      if (!thread) {
        return failure(404, { code: "thread_not_found", message: "Thread not found." });
      }

      const [messages, attachedClip, attachedPlace] = await Promise.all([
        selectMessages(client, threadId),
        thread.attachedClipId ? selectClip(client, thread.attachedClipId) : Promise.resolve(undefined),
        thread.attachedPlaceId ? selectPlace(client, thread.attachedPlaceId) : Promise.resolve(undefined),
      ]);

      return success({
        source: "api",
        generatedAt: generatedAt(),
        thread,
        messages,
        attachedClip,
        attachedPlace,
      });
    },
    async sendThreadMessage(threadId, request, actor = localFixtureActor()) {
      const thread = await selectThread(client, threadId, actor);

      if (!thread) {
        return failure(404, { code: "thread_not_found", message: "Thread not found." });
      }

      if (!thread.chatAccess?.canSendMessage) {
        return failure(403, {
          code: "chat_invite_required",
          message: thread.chatAccess?.reasonLabel ?? "Accept the chat request before sending a message.",
        });
      }

      const body = request.body?.trim();

      if (!body) {
        return failure(400, {
          code: "empty_message",
          message: "Message body must not be empty.",
        });
      }

      await upsertActor(client, actor);

      const result = await client.query<MessageRow>(
        `
          INSERT INTO notmid_chat_messages (
            id,
            thread_id,
            sender_user_id,
            body,
            clip_id,
            place_id
          )
          VALUES ($1, $2, $3, $4, $5, $6)
          RETURNING
            id,
            thread_id,
            $7::TEXT AS sender_handle,
            body,
            created_at,
            clip_id AS attachment_clip_id,
            place_id AS attachment_place_id,
            TRUE AS mine
        `,
        [
          messageId(threadId),
          threadId,
          actor.userId,
          body,
          request.attachment?.type === "clip" ? request.attachment.clipId : null,
          request.attachment?.type === "place" ? request.attachment.placeId : null,
          actor.handle,
        ],
      );

      return success({
        source: "api",
        generatedAt: generatedAt(),
        message: mapMessageRow(result.rows[0]),
      });
    },
    async startThread(request, actor = localFixtureActor()) {
      const validationError = validateStartThreadRequest(request);

      if (validationError) {
        return failure(400, validationError);
      }

      const startRequest = request as NotmidStartThreadRequest;
      const participantHandle = startRequest.participantHandle.trim();

      if (participantHandle === actor.handle) {
        return failure(400, {
          code: "chat_self_not_allowed",
          message: "You cannot start a chat with yourself.",
        });
      }

      const target = await selectPostgresChatUserByHandle(client, participantHandle);

      if (!target) {
        return failure(404, {
          code: "chat_participant_not_found",
          message: "Chat participant not found.",
        });
      }

      if (startRequest.attachedClipId && !(await selectClip(client, startRequest.attachedClipId))) {
        return failure(404, { code: "clip_not_found", message: "Clip not found." });
      }

      if (startRequest.attachedPlaceId && !(await selectPlace(client, startRequest.attachedPlaceId))) {
        return failure(404, { code: "place_not_found", message: "Place not found." });
      }

      const threadId = postgresStartThreadId(actor, target, startRequest);
      const existingThread = await selectThread(client, threadId, actor);

      if (existingThread) {
        return success({
          source: "api",
          generatedAt: generatedAt(),
          thread: existingThread,
        });
      }

      const areFriends = await selectPostgresUsersAreFriends(client, actor.userId, target.id);
      const relationship: NotmidChatRelationship = areFriends ? "friend" : "non-friend";
      const actorInviteStatus: NotmidChatInviteStatus = areFriends ? "accepted" : "pending-outbound";
      const targetInviteStatus: NotmidChatInviteStatus = areFriends ? "accepted" : "pending-inbound";

      await client.query("BEGIN");
      try {
        await upsertActor(client, actor);
        const message = await insertPostgresStartedThread(client, {
          actor,
          actorInviteStatus,
          body: startRequest.body.trim(),
          messageId: messageId(threadId),
          relationship,
          request: startRequest,
          target,
          targetInviteStatus,
          threadId,
        });
        await client.query("COMMIT");

        const thread = await selectThread(client, threadId, actor);

        return success({
          source: "api",
          generatedAt: generatedAt(),
          thread:
            thread ??
            fallbackPostgresStartedThread(
              threadId,
              actor,
              target,
              startRequest,
              relationship,
              actorInviteStatus,
            ),
          message,
        });
      } catch (error) {
        await client.query("ROLLBACK");
        throw error;
      }
    },
    async respondThreadInvite(threadId, decision, actor = localFixtureActor()) {
      const thread = await selectThread(client, threadId, actor);

      if (!thread) {
        return failure(404, { code: "thread_not_found", message: "Thread not found." });
      }

      if (
        thread.chatAccess?.inviteStatus !== "pending-inbound" ||
        (!thread.chatAccess.canAcceptInvite && !thread.chatAccess.canRejectInvite)
      ) {
        return failure(400, {
          code: "chat_invite_not_actionable",
          message: "This chat request cannot be accepted or rejected.",
        });
      }

      await upsertActor(client, actor);
      const response = await updateThreadAccess(client, threadId, actor.userId, decision);

      if (!response) {
        return failure(400, {
          code: "chat_invite_not_actionable",
          message: "This chat request cannot be accepted or rejected.",
        });
      }

      const updatedThread = await selectThread(client, threadId, actor);

      return success({
        source: "api",
        generatedAt: generatedAt(),
        thread: updatedThread ?? thread,
      });
    },
  };
}
