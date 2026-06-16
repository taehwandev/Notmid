import type {
  NotmidChatInviteStatus,
  NotmidChatRelationship,
  NotmidStartThreadRequest,
  NotmidThread,
  NotmidThreadMessage,
} from "@notmid/contracts";
import { startThreadId, type NotmidRepositoryActor } from "./notmidRepository";
import type { PostgresQueryClient } from "./postgresNotmidRepository";

export type PostgresChatUserRow = {
  avatar_image_url: string | null;
  display_name: string;
  handle: string;
  home_neighborhood: string | null;
  id: string;
  roles: string[] | null;
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

type FriendRelationshipRow = {
  status: "friend";
};

export async function selectPostgresChatUserByHandle(
  client: PostgresQueryClient,
  handle: string,
): Promise<PostgresChatUserRow | undefined> {
  const result = await client.query<PostgresChatUserRow>(
    `
      SELECT
        id,
        handle,
        display_name,
        home_neighborhood,
        avatar_image_url,
        roles
      FROM notmid_users
      WHERE handle = $1
    `,
    [handle],
  );

  return result.rows[0];
}

export async function selectPostgresUsersAreFriends(
  client: PostgresQueryClient,
  actorUserId: string,
  targetUserId: string,
): Promise<boolean> {
  const result = await client.query<FriendRelationshipRow>(
    `
      SELECT status
      FROM notmid_user_relationships
      WHERE user_id = $1
        AND related_user_id = $2
        AND status = 'friend'
      LIMIT 1
    `,
    [actorUserId, targetUserId],
  );

  return result.rows.length > 0;
}

export async function insertPostgresStartedThread(
  client: PostgresQueryClient,
  input: {
    actor: NotmidRepositoryActor;
    actorInviteStatus: NotmidChatInviteStatus;
    body: string;
    messageId: string;
    relationship: NotmidChatRelationship;
    request: NotmidStartThreadRequest;
    target: PostgresChatUserRow;
    targetInviteStatus: NotmidChatInviteStatus;
    threadId: string;
  },
): Promise<NotmidThreadMessage | undefined> {
  await client.query(
    `
      INSERT INTO notmid_chat_threads (
        id,
        place_id,
        clip_id,
        title,
        last_message
      )
      VALUES ($1, $2, $3, $4, $5)
      ON CONFLICT (id) DO NOTHING
    `,
    [
      input.threadId,
      input.request.attachedPlaceId ?? null,
      input.request.attachedClipId ?? null,
      input.relationship === "friend"
        ? `chat with ${input.target.handle}`
        : `request to ${input.target.handle}`,
      input.body,
    ],
  );

  await client.query(
    `
      INSERT INTO notmid_chat_thread_participants (thread_id, user_id)
      VALUES ($1, $2), ($1, $3)
      ON CONFLICT DO NOTHING
    `,
    [input.threadId, input.actor.userId, input.target.id],
  );

  await client.query(
    `
      INSERT INTO notmid_chat_thread_access (
        thread_id,
        user_id,
        relationship,
        invite_status
      )
      VALUES
        ($1, $2, $4, $5),
        ($1, $3, $4, $6)
      ON CONFLICT DO NOTHING
    `,
    [
      input.threadId,
      input.actor.userId,
      input.target.id,
      input.relationship,
      input.actorInviteStatus,
      input.targetInviteStatus,
    ],
  );

  const messageResult = await client.query<MessageRow>(
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
      ON CONFLICT (id) DO NOTHING
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
      input.messageId,
      input.threadId,
      input.actor.userId,
      input.body,
      input.request.attachedClipId ?? null,
      input.request.attachedPlaceId ?? null,
      input.actor.handle,
    ],
  );

  return messageResult.rows[0] ? mapMessageRow(messageResult.rows[0]) : undefined;
}

export function fallbackPostgresStartedThread(
  threadId: string,
  actor: NotmidRepositoryActor,
  target: PostgresChatUserRow,
  request: NotmidStartThreadRequest,
  relationship: NotmidChatRelationship,
  inviteStatus: NotmidChatInviteStatus,
): NotmidThread {
  return {
    id: threadId,
    title: relationship === "friend" ? `chat with ${target.handle}` : `request to ${target.handle}`,
    preview: request.body.trim(),
    updatedAtLabel: "now",
    participantHandles: [actor.handle, target.handle],
    attachedPlaceId: request.attachedPlaceId,
    attachedClipId: request.attachedClipId,
    unreadCount: 0,
    chatAccess: chatAccessFor(relationship, inviteStatus),
  };
}

export function postgresStartThreadId(
  actor: NotmidRepositoryActor,
  target: PostgresChatUserRow,
  request: Pick<NotmidStartThreadRequest, "attachedClipId" | "attachedPlaceId">,
): string {
  return startThreadId(actor.userId, target.id, request);
}

export function chatAccessFor(
  relationship: NotmidChatRelationship,
  inviteStatus: NotmidChatInviteStatus,
) {
  const canSendMessage = inviteStatus === "accepted";
  return {
    relationship,
    inviteStatus,
    canSendMessage,
    canAcceptInvite: inviteStatus === "pending-inbound",
    canRejectInvite: inviteStatus === "pending-inbound",
    reasonLabel: chatAccessReasonLabel(relationship, inviteStatus),
  };
}

function chatAccessReasonLabel(
  relationship: NotmidChatRelationship,
  inviteStatus: NotmidChatInviteStatus,
): string {
  if (relationship === "friend" && inviteStatus === "accepted") {
    return "Friends can chat immediately.";
  }

  switch (inviteStatus) {
    case "accepted":
      return "Chat request accepted.";
    case "pending-inbound":
      return "Accept or reject this chat request before messaging.";
    case "pending-outbound":
      return "Waiting for the other person to accept this chat request.";
    case "rejected":
      return "This chat request was rejected.";
  }
}

function mapMessageRow(row: MessageRow): NotmidThreadMessage {
  return {
    id: row.id,
    threadId: row.thread_id,
    senderHandle: row.sender_handle,
    body: row.body,
    createdAtLabel: timestampLabel(row.created_at),
    mine: row.mine ?? false,
    attachment: row.attachment_clip_id
      ? { type: "clip", clipId: row.attachment_clip_id }
      : row.attachment_place_id
        ? { type: "place", placeId: row.attachment_place_id }
        : undefined,
  };
}

function timestampLabel(value: Date | string | null): string {
  if (!value) {
    return "";
  }

  return value instanceof Date ? value.toISOString() : value;
}
