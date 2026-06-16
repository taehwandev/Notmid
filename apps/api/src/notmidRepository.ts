import {
  acceptedNonFriendChatAccess,
  acceptedFriendChatAccess,
  findNotmidClip,
  findNotmidPlace,
  findNotmidThread,
  getNotmidThreadDetail,
  notmidFakeAuthUser,
  notmidFixtureCaptureDraft,
  notmidFixtureFeed,
  notmidFixtureInbox,
  notmidFixtureMap,
  pendingOutboundChatAccess,
  rejectedChatAccess,
  type NotmidCaptureDraftResponse,
  type NotmidCapturePublishRequest,
  type NotmidCapturePublishResponse,
  type NotmidChatInviteDecision,
  type NotmidChatInviteResponse,
  type NotmidChatRelationship,
  type NotmidClip,
  type NotmidClipSaveResponse,
  type NotmidErrorResponse,
  type NotmidFeedResponse,
  type NotmidInboxResponse,
  type NotmidMapResponse,
  type NotmidPlace,
  type NotmidProfileSettingsResponse,
  type NotmidProfileSettingsUpdateRequest,
  type NotmidProfileSettingsUpdateResponse,
  type NotmidSendThreadMessageRequest,
  type NotmidSendThreadMessageResponse,
  type NotmidStartThreadRequest,
  type NotmidStartThreadResponse,
  type NotmidThread,
  type NotmidThreadDetailResponse,
  type NotmidThreadMessage,
} from "@notmid/contracts";

export type NotmidRepositoryActor = {
  avatarImageUrl?: string;
  displayName: string;
  handle: string;
  homeNeighborhood: string;
  roles: string[];
  userId: string;
};

export type NotmidRepositoryResult<T> =
  | {
      ok: true;
      value: T;
    }
  | {
      error: NotmidErrorResponse["error"];
      ok: false;
      status: 400 | 403 | 404;
    };

export type NotmidRepository = {
  getCaptureDraft: () => Promise<NotmidCaptureDraftResponse>;
  publishCapture: (
    request: Partial<NotmidCapturePublishRequest>,
    actor?: NotmidRepositoryActor,
  ) => Promise<NotmidRepositoryResult<NotmidCapturePublishResponse>>;
  getFeed: () => Promise<NotmidFeedResponse>;
  getMap: () => Promise<NotmidMapResponse>;
  getClip: (clipId: string) => Promise<NotmidRepositoryResult<NotmidClip>>;
  saveClip: (
    clipId: string,
    actor?: NotmidRepositoryActor,
  ) => Promise<NotmidRepositoryResult<NotmidClipSaveResponse>>;
  getProfileSettings: (actor?: NotmidRepositoryActor) => Promise<NotmidProfileSettingsResponse>;
  updateProfileSettings: (
    request: Partial<NotmidProfileSettingsUpdateRequest>,
    actor?: NotmidRepositoryActor,
  ) => Promise<NotmidRepositoryResult<NotmidProfileSettingsUpdateResponse>>;
  getPlace: (placeId: string) => Promise<NotmidRepositoryResult<NotmidPlace>>;
  getInbox: (actor?: NotmidRepositoryActor) => Promise<NotmidInboxResponse>;
  getThread: (
    threadId: string,
    actor?: NotmidRepositoryActor,
  ) => Promise<NotmidRepositoryResult<NotmidThread>>;
  getThreadDetail: (
    threadId: string,
    actor?: NotmidRepositoryActor,
  ) => Promise<NotmidRepositoryResult<NotmidThreadDetailResponse>>;
  sendThreadMessage: (
    threadId: string,
    request: Partial<NotmidSendThreadMessageRequest>,
    actor?: NotmidRepositoryActor,
  ) => Promise<NotmidRepositoryResult<NotmidSendThreadMessageResponse>>;
  startThread: (
    request: Partial<NotmidStartThreadRequest>,
    actor?: NotmidRepositoryActor,
  ) => Promise<NotmidRepositoryResult<NotmidStartThreadResponse>>;
  respondThreadInvite: (
    threadId: string,
    decision: NotmidChatInviteDecision,
    actor?: NotmidRepositoryActor,
  ) => Promise<NotmidRepositoryResult<NotmidChatInviteResponse>>;
};

export function createFixtureNotmidRepository(
  generatedAt: () => string = () => new Date().toISOString(),
): NotmidRepository {
  const chatAccessByThreadId = new Map<string, NotmidThread["chatAccess"]>();
  const startedThreadsById = new Map<string, NotmidThread>();
  const startedMessagesByThreadId = new Map<string, NotmidThreadMessage>();
  const threadWithCurrentChatAccess = (thread: NotmidThread): NotmidThread => ({
    ...thread,
    chatAccess: chatAccessByThreadId.get(thread.id) ?? thread.chatAccess,
  });
  const findThread = (threadId: string): NotmidThread | undefined =>
    findNotmidThread(threadId) ?? startedThreadsById.get(threadId);

  return {
    async getCaptureDraft() {
      return {
        ...notmidFixtureCaptureDraft,
        source: "api",
      };
    },
    async publishCapture(request, actor = localFixtureActor()) {
      const validationError = validateCapturePublishRequest(request);

      if (validationError) {
        return failure(400, validationError);
      }

      const publishRequest = request as NotmidCapturePublishRequest;
      const place = findNotmidPlace(publishRequest.placeId);

      if (!place) {
        return failure(404, { code: "place_not_found", message: "Place not found." });
      }

      return success({
        source: "api",
        generatedAt: generatedAt(),
        clip: {
          id: `receipt-${publishRequest.draftId}`,
          title: publishRequest.caption.slice(0, 48),
          caption: publishRequest.caption,
          creatorHandle: actor.handle,
          placeId: publishRequest.placeId,
          moodTags: publishRequest.moodTags,
          capturedAtLabel: "now",
          coverImageUrl: place.coverImageUrl,
          metrics: {
            likes: 0,
            saves: 0,
            comments: 0,
            distanceLabel: "new",
          },
        },
        moderationStatus: "queued",
      });
    },
    async getFeed() {
      return notmidFixtureFeed;
    },
    async getMap() {
      return notmidFixtureMap;
    },
    async getClip(clipId) {
      const clip = findNotmidClip(clipId);

      if (!clip) {
        return failure(404, { code: "clip_not_found", message: "Clip not found." });
      }

      return success(clip);
    },
    async saveClip(clipId) {
      const clip = findNotmidClip(clipId);

      if (!clip) {
        return failure(404, { code: "clip_not_found", message: "Clip not found." });
      }

      return success({
        source: "api",
        generatedAt: generatedAt(),
        clip: {
          ...clip,
          metrics: {
            ...clip.metrics,
            saves: clip.metrics.saves + 1,
          },
        },
        saved: true,
      });
    },
    async getProfileSettings(actor = localFixtureActor()) {
      return profileSettingsResponseForActor(actor, generatedAt());
    },
    async updateProfileSettings(request, actor = localFixtureActor()) {
      const validationError = validateProfileSettingsUpdateRequest(request);

      if (validationError) {
        return failure(400, validationError);
      }

      return success({
        ...profileSettingsResponseForActor(
          {
            ...actor,
            displayName: request.displayName?.trim() ?? actor.displayName,
            homeNeighborhood: request.homeNeighborhood?.trim() ?? actor.homeNeighborhood,
          },
          generatedAt(),
        ),
        updated: true,
      });
    },
    async getPlace(placeId) {
      const place = findNotmidPlace(placeId);

      if (!place) {
        return failure(404, { code: "place_not_found", message: "Place not found." });
      }

      return success(place);
    },
    async getInbox() {
      return {
        ...notmidFixtureInbox,
        threads: [
          ...Array.from(startedThreadsById.values()),
          ...notmidFixtureInbox.threads,
        ].map(threadWithCurrentChatAccess),
      };
    },
    async getThread(threadId) {
      const thread = findThread(threadId);

      if (!thread) {
        return failure(404, { code: "thread_not_found", message: "Thread not found." });
      }

      return success(threadWithCurrentChatAccess(thread));
    },
    async getThreadDetail(threadId) {
      const detail = getNotmidThreadDetail(threadId);
      const startedThread = startedThreadsById.get(threadId);

      if (!detail && !startedThread) {
        return failure(404, { code: "thread_not_found", message: "Thread not found." });
      }

      if (startedThread) {
        const message = startedMessagesByThreadId.get(threadId);
        return success({
          source: "api",
          generatedAt: generatedAt(),
          thread: threadWithCurrentChatAccess(startedThread),
          messages: message ? [message] : [],
          attachedClip: startedThread.attachedClipId
            ? findNotmidClip(startedThread.attachedClipId)
            : undefined,
          attachedPlace: startedThread.attachedPlaceId
            ? findNotmidPlace(startedThread.attachedPlaceId)
            : undefined,
        });
      }

      return success({
        ...detail!,
        source: "api",
        thread: threadWithCurrentChatAccess(detail!.thread),
      });
    },
    async sendThreadMessage(threadId, request, actor = localFixtureActor()) {
      const thread = findThread(threadId);

      if (!thread) {
        return failure(404, { code: "thread_not_found", message: "Thread not found." });
      }

      const currentThread = threadWithCurrentChatAccess(thread);
      if (!currentThread.chatAccess?.canSendMessage) {
        return failure(403, {
          code: "chat_invite_required",
          message:
            currentThread.chatAccess?.reasonLabel ??
            "Accept the chat request before sending a message.",
        });
      }

      const body = request.body?.trim();

      if (!body) {
        return failure(400, {
          code: "empty_message",
          message: "Message body must not be empty.",
        });
      }

      return success({
        source: "api",
        generatedAt: generatedAt(),
        message: {
          id: `msg-${threadId}-local-reply`,
          threadId,
          senderHandle: actor.handle,
          body,
          createdAtLabel: "now",
          mine: true,
          attachment: request.attachment,
        },
      });
    },
    async startThread(request, actor = localFixtureActor()) {
      const validationError = validateStartThreadRequest(request);

      if (validationError) {
        return failure(400, validationError);
      }

      const startRequest = request as NotmidStartThreadRequest;
      const participant = fixtureChatParticipant(startRequest.participantHandle);

      if (!participant) {
        return failure(404, {
          code: "chat_participant_not_found",
          message: "Chat participant not found.",
        });
      }

      if (participant.handle === actor.handle) {
        return failure(400, {
          code: "chat_self_not_allowed",
          message: "You cannot start a chat with yourself.",
        });
      }

      if (startRequest.attachedClipId && !findNotmidClip(startRequest.attachedClipId)) {
        return failure(404, { code: "clip_not_found", message: "Clip not found." });
      }

      if (startRequest.attachedPlaceId && !findNotmidPlace(startRequest.attachedPlaceId)) {
        return failure(404, { code: "place_not_found", message: "Place not found." });
      }

      const threadId = startThreadId(actor.handle, participant.handle, startRequest);
      const existingThread = findThread(threadId);

      if (existingThread) {
        return success({
          source: "api",
          generatedAt: generatedAt(),
          thread: threadWithCurrentChatAccess(existingThread),
        });
      }

      const chatAccess =
        participant.relationship === "friend"
          ? acceptedFriendChatAccess()
          : pendingOutboundChatAccess();
      const thread: NotmidThread = {
        id: threadId,
        title:
          participant.relationship === "friend"
            ? `chat with ${participant.handle}`
            : `request to ${participant.handle}`,
        preview: startRequest.body.trim(),
        updatedAtLabel: "now",
        participantHandles: [participant.handle, actor.handle],
        attachedPlaceId: startRequest.attachedPlaceId,
        attachedClipId: startRequest.attachedClipId,
        unreadCount: 0,
        chatAccess,
      };
      const message: NotmidThreadMessage = {
        id: `msg-${threadId}-start`,
        threadId,
        senderHandle: actor.handle,
        body: startRequest.body.trim(),
        createdAtLabel: "now",
        mine: true,
        attachment: startRequest.attachedClipId
          ? { type: "clip", clipId: startRequest.attachedClipId }
          : startRequest.attachedPlaceId
            ? { type: "place", placeId: startRequest.attachedPlaceId }
            : undefined,
      };

      startedThreadsById.set(threadId, thread);
      startedMessagesByThreadId.set(threadId, message);
      chatAccessByThreadId.set(threadId, chatAccess);

      return success({
        source: "api",
        generatedAt: generatedAt(),
        thread,
        message,
      });
    },
    async respondThreadInvite(threadId, decision) {
      const thread = findThread(threadId);

      if (!thread) {
        return failure(404, { code: "thread_not_found", message: "Thread not found." });
      }

      const currentThread = threadWithCurrentChatAccess(thread);
      const currentAccess = currentThread.chatAccess;
      const canRespond = currentAccess?.canAcceptInvite || currentAccess?.canRejectInvite;

      if (!canRespond || currentAccess?.inviteStatus !== "pending-inbound") {
        return failure(400, {
          code: "chat_invite_not_actionable",
          message: "This chat request cannot be accepted or rejected.",
        });
      }

      const nextAccess =
        decision === "accept" ? acceptedNonFriendChatAccess() : rejectedChatAccess();
      chatAccessByThreadId.set(threadId, nextAccess);

      return success({
        source: "api",
        generatedAt: generatedAt(),
        thread: {
          ...thread,
          preview:
            decision === "accept"
              ? "Chat request accepted. You can message now."
              : "Chat request rejected.",
          chatAccess: nextAccess,
        },
      });
    },
  };
}

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

const success = repositorySuccess;
const failure = repositoryFailure;

type FixtureChatParticipant = {
  handle: string;
  relationship: NotmidChatRelationship;
};

function fixtureChatParticipant(handle: string): FixtureChatParticipant | undefined {
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
