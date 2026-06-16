import type {
  NotmidChatInviteDecision,
  NotmidChatInviteStatus,
  NotmidClip,
  NotmidPlace,
  NotmidThread,
  NotmidThreadMessage,
} from "@notmid/contracts";
import type { NotmidRepositoryActor } from "./notmidRepository";
import type { PostgresQueryClient } from "./postgresNotmidQueryTypes";
import {
  mapClipRow,
  mapMessageRow,
  mapPlaceRow,
  mapThreadRow,
  type ClipRow,
  type MessageRow,
  type PlaceRow,
  type ThreadAccessRow,
  type ThreadRow,
  type UserRow,
} from "./postgresNotmidRows";

export async function selectPlaces(client: PostgresQueryClient): Promise<NotmidPlace[]> {
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

export async function selectPlace(
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

export async function selectClips(client: PostgresQueryClient): Promise<NotmidClip[]> {
  const result = await client.query<ClipRow>(clipSelectSql("ORDER BY clip.created_at DESC LIMIT 50"));

  return result.rows.map(mapClipRow);
}

export async function selectClip(
  client: PostgresQueryClient,
  clipId: string,
): Promise<NotmidClip | undefined> {
  const result = await client.query<ClipRow>(clipSelectSql("WHERE clip.id = $1"), [clipId]);

  return result.rows[0] ? mapClipRow(result.rows[0]) : undefined;
}

export async function selectHighlightedClipIds(client: PostgresQueryClient): Promise<string[]> {
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

export async function selectThreads(
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

export async function selectThread(
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

export async function selectMessages(
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

export async function updateThreadAccess(
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

export async function selectUser(
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

export async function upsertActor(
  client: PostgresQueryClient,
  actor: NotmidRepositoryActor,
): Promise<void> {
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
