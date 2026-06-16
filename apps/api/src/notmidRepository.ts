import {
  acceptedNonFriendChatAccess,
  acceptedFriendChatAccess,
  findNotmidClip,
  findNotmidPlace,
  findNotmidThread,
  getNotmidThreadDetail,
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
import {
  fixtureChatParticipant,
  localFixtureActor,
  profileSettingsResponseForActor,
  repositoryFailure,
  repositorySuccess,
  startThreadId,
  validateCapturePublishRequest,
  validateProfileSettingsUpdateRequest,
  validateStartThreadRequest,
} from "./notmidRepositoryValidation";

export {
  localFixtureActor,
  profileSettingsResponseForActor,
  repositoryFailure,
  repositorySuccess,
  startThreadId,
  validateCapturePublishRequest,
  validateProfileSettingsUpdateRequest,
  validateStartThreadRequest,
} from "./notmidRepositoryValidation";

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

const success = repositorySuccess;
const failure = repositoryFailure;
