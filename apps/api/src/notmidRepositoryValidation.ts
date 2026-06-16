import {
  notmidFakeAuthUser,
  type NotmidCapturePublishRequest,
  type NotmidChatRelationship,
  type NotmidErrorResponse,
  type NotmidProfileSettingsResponse,
  type NotmidProfileSettingsUpdateRequest,
  type NotmidStartThreadRequest,
} from "@notmid/contracts";
import type { NotmidRepositoryActor, NotmidRepositoryResult } from "./notmidRepository";

export function validateCapturePublishRequest(
  request: Partial<NotmidCapturePublishRequest>,
): NotmidErrorResponse["error"] | null {
  if (!request.draftId?.trim()) {
    return { code: "missing_draft", message: "draftId is required." };
  }

  if (!request.caption?.trim()) {
    return { code: "missing_caption", message: "caption is required." };
  }

  if (!request.placeId?.trim()) {
    return { code: "missing_place", message: "placeId is required." };
  }

  if (!Array.isArray(request.moodTags) || request.moodTags.length === 0) {
    return { code: "missing_tags", message: "At least one mood tag is required." };
  }

  if (
    request.visibility !== "public" &&
    request.visibility !== "friends" &&
    request.visibility !== "private"
  ) {
    return { code: "invalid_visibility", message: "visibility is invalid." };
  }

  return null;
}

export function validateProfileSettingsUpdateRequest(
  request: Partial<NotmidProfileSettingsUpdateRequest>,
): NotmidErrorResponse["error"] | null {
  const displayName = request.displayName?.trim();
  const homeNeighborhood = request.homeNeighborhood?.trim();

  if (!displayName) {
    return { code: "missing_display_name", message: "displayName is required." };
  }

  if (displayName.length > 80) {
    return { code: "display_name_too_long", message: "displayName must be 80 characters or fewer." };
  }

  if (!homeNeighborhood) {
    return { code: "missing_neighborhood", message: "homeNeighborhood is required." };
  }

  if (homeNeighborhood.length > 80) {
    return {
      code: "neighborhood_too_long",
      message: "homeNeighborhood must be 80 characters or fewer.",
    };
  }

  return null;
}

export function validateStartThreadRequest(
  request: Partial<NotmidStartThreadRequest>,
): NotmidErrorResponse["error"] | null {
  if (!request.participantHandle?.trim()) {
    return { code: "missing_participant", message: "participantHandle is required." };
  }

  if (!request.body?.trim()) {
    return { code: "empty_message", message: "Message body must not be empty." };
  }

  if (request.body.trim().length > 800) {
    return { code: "message_too_long", message: "Message body must be 800 characters or fewer." };
  }

  if (request.attachedClipId?.trim() === "") {
    return { code: "invalid_clip", message: "attachedClipId must not be empty." };
  }

  if (request.attachedPlaceId?.trim() === "") {
    return { code: "invalid_place", message: "attachedPlaceId must not be empty." };
  }

  return null;
}

export function startThreadId(
  actorHandle: string,
  participantHandle: string,
  request: Pick<NotmidStartThreadRequest, "attachedClipId" | "attachedPlaceId">,
): string {
  const handles = [actorHandle, participantHandle].map(slugPart).sort();
  const context =
    request.attachedClipId?.trim() ??
    request.attachedPlaceId?.trim() ??
    "direct";

  return `thread-${handles.join("-")}-${slugPart(context)}`;
}

export function localFixtureActor(): NotmidRepositoryActor {
  return {
    avatarImageUrl: notmidFakeAuthUser.avatarImageUrl,
    displayName: notmidFakeAuthUser.displayName,
    handle: notmidFakeAuthUser.handle,
    homeNeighborhood: notmidFakeAuthUser.homeNeighborhood,
    roles: notmidFakeAuthUser.roles,
    userId: notmidFakeAuthUser.id,
  };
}

export function profileSettingsResponseForActor(
  actor: NotmidRepositoryActor,
  generatedAt: string,
): NotmidProfileSettingsResponse {
  return {
    source: "api",
    generatedAt,
    settings: {
      user: {
        id: actor.userId,
        handle: actor.handle,
        displayName: actor.displayName,
        homeNeighborhood: actor.homeNeighborhood,
        avatarImageUrl: actor.avatarImageUrl ?? "",
        roles: actor.roles,
      },
      privacy: {
        savedPlacesVisibility: "private",
        chatInvites: "shared-clips-and-places",
        defaultReceiptVisibility: "public",
      },
    },
  };
}

export function repositorySuccess<T>(value: T): NotmidRepositoryResult<T> {
  return {
    ok: true,
    value,
  };
}

export function repositoryFailure(
  status: 400 | 403 | 404,
  error: NotmidErrorResponse["error"],
): NotmidRepositoryResult<never> {
  return {
    error,
    ok: false,
    status,
  };
}

type FixtureChatParticipant = {
  handle: string;
  relationship: NotmidChatRelationship;
};

export function fixtureChatParticipant(handle: string): FixtureChatParticipant | undefined {
  const normalizedHandle = handle.trim();
  const participants: FixtureChatParticipant[] = [
    { handle: "min.zip", relationship: "friend" },
    { handle: "yapmap.ji", relationship: "friend" },
    { handle: "receipt.han", relationship: "non-friend" },
  ];

  return participants.find((participant) => participant.handle === normalizedHandle);
}

function slugPart(value: string): string {
  const slug = value
    .trim()
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-+|-+$/g, "");

  return slug || "item";
}
