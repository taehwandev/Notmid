import type {
  NotmidChatAccess,
  NotmidChatInviteStatus,
  NotmidChatRelationship,
  NotmidClip,
  NotmidMessageAttachment,
  NotmidPlace,
  NotmidThread,
  NotmidThreadMessage,
} from "@notmid/contracts";
import { chatAccessFor } from "./postgresChatRepository";
import type { NotmidRepositoryActor } from "./notmidRepository";

export type PlaceRow = {
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

export type ClipRow = {
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

export type ThreadRow = {
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

export type MessageRow = {
  attachment_clip_id: string | null;
  attachment_place_id: string | null;
  body: string;
  created_at: Date | string | null;
  id: string;
  mine: boolean | null;
  sender_handle: string;
  thread_id: string;
};

export type UserRow = {
  avatar_image_url: string | null;
  display_name: string;
  handle: string;
  home_neighborhood: string | null;
  id: string;
  roles: string[] | null;
};

export type ThreadAccessRow = {
  invite_status: NotmidChatInviteStatus;
  relationship: NotmidChatRelationship;
};

export function mapPlaceRow(row: PlaceRow): NotmidPlace {
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

export function mapClipRow(row: ClipRow): NotmidClip {
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

export function mapThreadRow(row: ThreadRow): NotmidThread {
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

export function mapMessageRow(row: MessageRow): NotmidThreadMessage {
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

export function actorForUserRow(row: UserRow): NotmidRepositoryActor {
  return {
    avatarImageUrl: row.avatar_image_url ?? "",
    displayName: row.display_name,
    handle: row.handle,
    homeNeighborhood: row.home_neighborhood ?? "",
    roles: row.roles ?? ["creator"],
    userId: row.id,
  };
}

function chatAccessForRow(row: ThreadRow): NotmidChatAccess {
  if (row.chat_relationship && row.chat_invite_status) {
    return chatAccessFor(row.chat_relationship, row.chat_invite_status);
  }

  return chatAccessFor("friend", "accepted");
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
