import type {
  NotmidCaptureDraftResponse,
  NotmidCapturePublishRequest,
  NotmidCapturePublishResponse,
  NotmidChatAccess,
  NotmidChatInviteDecision,
  NotmidChatInviteStatus,
  NotmidChatRelationship,
  NotmidClip,
  NotmidFeedResponse,
  NotmidInboxResponse,
  NotmidMapResponse,
  NotmidMessageAttachment,
  NotmidPlace,
  NotmidProfileSettingsUpdateRequest,
  NotmidSendThreadMessageRequest,
  NotmidSendThreadMessageResponse,
  NotmidStartThreadRequest,
  NotmidThread,
  NotmidThreadDetailResponse,
  NotmidThreadMessage,
} from "@notmid/contracts";
import {
  chatAccessFor,
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
  type NotmidRepositoryActor,
  type NotmidRepositoryResult,
} from "./notmidRepository";

export type PostgresQueryResult<Row> = {
  rows: Row[];
};

export type PostgresQueryClient = {
  query: <Row>(sql: string, values?: readonly unknown[]) => Promise<PostgresQueryResult<Row>>;
};

export type PostgresNotmidRepositoryOptions = {
  generatedAt?: () => string;
  messageId?: (threadId: string) => string;
  receiptClipId?: (draftId: string) => string;
};

type PlaceRow = {
  address: string | null;
  category: string;
  cover_image_url: string | null;
  id: string;
  lat: number | null;
  lng: number | null;
  name: string;
  neighborhood: string;
  open_now: boolean | null;
  receipt_count: number | string | null;
  score: number | string | null;
};

type ClipRow = {
  caption: string;
  comment_count: number | string | null;
  cover_image_url: string | null;
  created_at: Date | string | null;
  creator_handle: string;
  id: string;
  like_count: number | string | null;
  mood_tags: string[] | null;
  place_id: string;
  save_count: number | string | null;
  title: string;
  video_object_key: string | null;
};

type ThreadRow = {
  attached_clip_id: string | null;
  attached_place_id: string | null;
  chat_invite_status: NotmidChatInviteStatus | null;
  chat_relationship: NotmidChatRelationship | null;
  id: string;
  participant_handles: string[] | null;
  preview: string | null;
  title: string;
  updated_at: Date | string | null;
};

type MessageRow = {
  attachment_clip_id: string | null;
  attachment_place_id: string | null;
  body: string;
  created_at: Date | string | null;
  id: string;
  mine: boolean | null;
  sender_handle: string;
  thread_id: string;
};

type UserRow = {
  avatar_image_url: string | null;
  display_name: string;
  handle: string;
  home_neighborhood: string | null;
  id: string;
  roles: string[] | null;
};

type ThreadAccessRow = {
  invite_status: NotmidChatInviteStatus;
  relationship: NotmidChatRelationship;
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

async function selectPlaces(client: PostgresQueryClient): Promise<NotmidPlace[]> {
  const result = await client.query<PlaceRow>(
    `
      SELECT
        place.id,
        place.name,
        place.region AS neighborhood,
        place.category,
        place.address,
        place.lat,
        place.lng,
        place.open_now,
        place.score,
        place.cover_image_url,
        COUNT(clip.id)::INT AS receipt_count
      FROM notmid_places place
      LEFT JOIN notmid_clips clip ON clip.place_id = place.id
      GROUP BY place.id
      ORDER BY place.updated_at DESC, place.name ASC
    `,
  );

  return result.rows.map(mapPlaceRow);
}

async function selectPlace(
  client: PostgresQueryClient,
  placeId: string,
): Promise<NotmidPlace | undefined> {
  const result = await client.query<PlaceRow>(
    `
      SELECT
        place.id,
        place.name,
        place.region AS neighborhood,
        place.category,
        place.address,
        place.lat,
        place.lng,
        place.open_now,
        place.score,
        place.cover_image_url,
        COUNT(clip.id)::INT AS receipt_count
      FROM notmid_places place
      LEFT JOIN notmid_clips clip ON clip.place_id = place.id
      WHERE place.id = $1
      GROUP BY place.id
    `,
    [placeId],
  );

  return result.rows[0] ? mapPlaceRow(result.rows[0]) : undefined;
}

async function selectClips(client: PostgresQueryClient): Promise<NotmidClip[]> {
  const result = await client.query<ClipRow>(clipSelectSql("ORDER BY clip.created_at DESC LIMIT 50"));

  return result.rows.map(mapClipRow);
}

async function selectClip(
  client: PostgresQueryClient,
  clipId: string,
): Promise<NotmidClip | undefined> {
  const result = await client.query<ClipRow>(clipSelectSql("WHERE clip.id = $1"), [clipId]);

  return result.rows[0] ? mapClipRow(result.rows[0]) : undefined;
}

async function selectHighlightedClipIds(client: PostgresQueryClient): Promise<string[]> {
  const result = await client.query<{ id: string }>(
    `
      SELECT id
      FROM notmid_clips
      WHERE visibility = 'public'
      ORDER BY created_at DESC
      LIMIT 24
    `,
  );

  return result.rows.map((row) => row.id);
}

async function selectThreads(
  client: PostgresQueryClient,
  actor?: NotmidRepositoryActor,
): Promise<NotmidThread[]> {
  const tail = actor
    ? "GROUP BY thread.id, access.relationship, access.invite_status ORDER BY thread.updated_at DESC LIMIT 50"
    : "GROUP BY thread.id ORDER BY thread.updated_at DESC LIMIT 50";
  const result = await client.query<ThreadRow>(
    threadSelectSql(tail, actor ? 1 : undefined),
    actor ? [actor.userId] : [],
  );

  return result.rows.map(mapThreadRow);
}

async function selectThread(
  client: PostgresQueryClient,
  threadId: string,
  actor?: NotmidRepositoryActor,
): Promise<NotmidThread | undefined> {
  const tail = actor
    ? "WHERE thread.id = $1 GROUP BY thread.id, access.relationship, access.invite_status"
    : "WHERE thread.id = $1 GROUP BY thread.id";
  const result = await client.query<ThreadRow>(
    threadSelectSql(tail, actor ? 2 : undefined),
    actor ? [threadId, actor.userId] : [threadId],
  );

  return result.rows[0] ? mapThreadRow(result.rows[0]) : undefined;
}

async function selectMessages(
  client: PostgresQueryClient,
  threadId: string,
): Promise<NotmidThreadMessage[]> {
  const result = await client.query<MessageRow>(
    `
      SELECT
        message.id,
        message.thread_id,
        sender.handle AS sender_handle,
        message.body,
        message.created_at,
        message.clip_id AS attachment_clip_id,
        message.place_id AS attachment_place_id,
        FALSE AS mine
      FROM notmid_chat_messages message
      JOIN notmid_users sender ON sender.id = message.sender_user_id
      WHERE message.thread_id = $1
      ORDER BY message.created_at ASC
    `,
    [threadId],
  );

  return result.rows.map(mapMessageRow);
}

async function updateThreadAccess(
  client: PostgresQueryClient,
  threadId: string,
  actorUserId: string,
  decision: NotmidChatInviteDecision,
): Promise<ThreadAccessRow | undefined> {
  const nextStatus: NotmidChatInviteStatus = decision === "accept" ? "accepted" : "rejected";
  const result = await client.query<ThreadAccessRow>(
    `
      UPDATE notmid_chat_thread_access
      SET
        invite_status = $3,
        responded_at = NOW(),
        updated_at = NOW()
      WHERE thread_id = $1
        AND user_id = $2
        AND invite_status = 'pending-inbound'
      RETURNING
        relationship,
        invite_status
    `,
    [threadId, actorUserId, nextStatus],
  );

  return result.rows[0];
}

async function selectUser(
  client: PostgresQueryClient,
  userId: string,
): Promise<UserRow | undefined> {
  const result = await client.query<UserRow>(
    `
      SELECT
        id,
        handle,
        display_name,
        home_neighborhood,
        avatar_image_url,
        roles
      FROM notmid_users
      WHERE id = $1
    `,
    [userId],
  );

  return result.rows[0];
}

async function upsertActor(client: PostgresQueryClient, actor: NotmidRepositoryActor): Promise<void> {
  await client.query(
    `
      INSERT INTO notmid_users (
        id,
        handle,
        display_name,
        home_neighborhood,
        avatar_image_url,
        roles
      )
      VALUES ($1, $2, $3, $4, $5, $6)
      ON CONFLICT (id) DO UPDATE SET
        handle = EXCLUDED.handle,
        display_name = EXCLUDED.display_name,
        home_neighborhood = EXCLUDED.home_neighborhood,
        avatar_image_url = EXCLUDED.avatar_image_url,
        roles = EXCLUDED.roles,
        updated_at = NOW()
    `,
    [
      actor.userId,
      actor.handle,
      actor.displayName,
      actor.homeNeighborhood,
      actor.avatarImageUrl ?? "",
      actor.roles.length > 0 ? actor.roles : ["creator"],
    ],
  );
}

function clipSelectSql(tail: string): string {
  return `
    SELECT
      clip.id,
      clip.title,
      clip.caption,
      creator.handle AS creator_handle,
      clip.place_id,
      clip.mood_tags,
      clip.cover_image_url,
      clip.video_object_key,
      clip.like_count,
      clip.save_count,
      clip.comment_count,
      clip.created_at
    FROM notmid_clips clip
    JOIN notmid_users creator ON creator.id = clip.owner_user_id
    ${tail}
  `;
}

function threadSelectSql(tail: string, actorParamIndex?: number): string {
  const accessSelect = actorParamIndex
    ? `
      access.relationship AS chat_relationship,
      access.invite_status AS chat_invite_status,
    `
    : `
      NULL::TEXT AS chat_relationship,
      NULL::TEXT AS chat_invite_status,
    `;
  const accessJoin = actorParamIndex
    ? `
    LEFT JOIN notmid_chat_thread_access access
      ON access.thread_id = thread.id
      AND access.user_id = $${actorParamIndex}
    `
    : "";

  return `
    SELECT
      thread.id,
      thread.title,
      thread.last_message AS preview,
      thread.updated_at,
      thread.place_id AS attached_place_id,
      thread.clip_id AS attached_clip_id,
      ${accessSelect}
      COALESCE(
        ARRAY_REMOVE(ARRAY_AGG(participant.handle ORDER BY participant.handle), NULL),
        ARRAY[]::TEXT[]
      )
        AS participant_handles
    FROM notmid_chat_threads thread
    LEFT JOIN notmid_chat_thread_participants thread_participant
      ON thread_participant.thread_id = thread.id
    LEFT JOIN notmid_users participant
      ON participant.id = thread_participant.user_id
    ${accessJoin}
    ${tail}
  `;
}

function mapPlaceRow(row: PlaceRow): NotmidPlace {
  return {
    id: row.id,
    name: row.name,
    neighborhood: row.neighborhood,
    category: row.category,
    address: row.address ?? "",
    lat: toNumber(row.lat),
    lng: toNumber(row.lng),
    openNow: row.open_now ?? false,
    score: toNumber(row.score),
    receiptCount: toNumber(row.receipt_count),
    coverImageUrl: row.cover_image_url ?? "",
  };
}

function mapClipRow(row: ClipRow): NotmidClip {
  return {
    id: row.id,
    title: row.title,
    caption: row.caption,
    creatorHandle: row.creator_handle,
    placeId: row.place_id,
    moodTags: row.mood_tags ?? [],
    capturedAtLabel: timestampLabel(row.created_at),
    coverImageUrl: row.cover_image_url ?? "",
    videoUrl: row.video_object_key ?? undefined,
    metrics: {
      likes: toNumber(row.like_count),
      saves: toNumber(row.save_count),
      comments: toNumber(row.comment_count),
      distanceLabel: "",
    },
  };
}

function mapThreadRow(row: ThreadRow): NotmidThread {
  return {
    id: row.id,
    title: row.title,
    preview: row.preview ?? "",
    updatedAtLabel: timestampLabel(row.updated_at),
    participantHandles: row.participant_handles ?? [],
    attachedPlaceId: row.attached_place_id ?? undefined,
    attachedClipId: row.attached_clip_id ?? undefined,
    unreadCount: 0,
    chatAccess: chatAccessForRow(row),
  };
}

function chatAccessForRow(row: ThreadRow): NotmidChatAccess {
  if (row.chat_relationship && row.chat_invite_status) {
    return chatAccessFor(row.chat_relationship, row.chat_invite_status);
  }

  return chatAccessFor("friend", "accepted");
}

function mapMessageRow(row: MessageRow): NotmidThreadMessage {
  return {
    id: row.id,
    threadId: row.thread_id,
    senderHandle: row.sender_handle,
    body: row.body,
    createdAtLabel: timestampLabel(row.created_at),
    mine: row.mine ?? false,
    attachment: messageAttachment(row),
  };
}

function actorForUserRow(row: UserRow): NotmidRepositoryActor {
  return {
    avatarImageUrl: row.avatar_image_url ?? "",
    displayName: row.display_name,
    handle: row.handle,
    homeNeighborhood: row.home_neighborhood ?? "",
    roles: row.roles ?? ["creator"],
    userId: row.id,
  };
}

function messageAttachment(row: MessageRow): NotmidMessageAttachment | undefined {
  if (row.attachment_clip_id) {
    return { type: "clip", clipId: row.attachment_clip_id };
  }

  if (row.attachment_place_id) {
    return { type: "place", placeId: row.attachment_place_id };
  }

  return undefined;
}

function timestampLabel(value: Date | string | null): string {
  if (!value) {
    return "";
  }

  return value instanceof Date ? value.toISOString() : value;
}

function toNumber(value: number | string | null): number {
  if (typeof value === "number") {
    return value;
  }

  if (typeof value === "string") {
    const parsed = Number.parseFloat(value);
    return Number.isFinite(parsed) ? parsed : 0;
  }

  return 0;
}
