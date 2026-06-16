import type {
  NotmidClip,
  NotmidAuthRequirement,
  NotmidAuthSession,
  NotmidAuthStatusResponse,
  NotmidAuthUser,
  NotmidCaptureDraftResponse,
  NotmidChatAccess,
  NotmidFeedResponse,
  NotmidInboxResponse,
  NotmidMapResponse,
  NotmidPlace,
  NotmidThreadDetailResponse,
  NotmidThreadMessage,
  NotmidThread,
} from "./models";

const generatedAt = "2026-05-17T00:00:00.000Z";

export const notmidAuthRequiredActions: NotmidAuthRequirement[] = [
  "capture",
  "save",
  "chat",
  "profile-edit",
  "moderation",
];

export const notmidPlaces: NotmidPlace[] = [
  {
    id: "neon-yard",
    name: "Neon Yard",
    neighborhood: "Seongsu",
    category: "night coffee",
    address: "Seongsu-dong, Seoul",
    lat: 37.5446,
    lng: 127.0557,
    openNow: true,
    score: 92,
    receiptCount: 184,
    coverImageUrl:
      "https://images.unsplash.com/photo-1514933651103-005eec06c04b?auto=format&fit=crop&w=1200&q=80",
  },
  {
    id: "thin-air-records",
    name: "Thin Air Records",
    neighborhood: "Hapjeong",
    category: "record bar",
    address: "Hapjeong-dong, Seoul",
    lat: 37.5494,
    lng: 126.9138,
    openNow: true,
    score: 88,
    receiptCount: 97,
    coverImageUrl:
      "https://images.unsplash.com/photo-1516280440614-37939bbacd81?auto=format&fit=crop&w=1200&q=80",
  },
  {
    id: "underpass-gallery",
    name: "Underpass Gallery",
    neighborhood: "Euljiro",
    category: "exhibit",
    address: "Euljiro, Seoul",
    lat: 37.5662,
    lng: 126.9919,
    openNow: false,
    score: 84,
    receiptCount: 62,
    coverImageUrl:
      "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?auto=format&fit=crop&w=1200&q=80",
  },
];

export const notmidClips: NotmidClip[] = [
  {
    id: "latte-line-was-worth-it",
    title: "latte line was worth it",
    caption: "Foam art, fast seats, and the alley playlist is doing work.",
    creatorHandle: "min.zip",
    placeId: "neon-yard",
    moodTags: ["live rn", "date safe", "good light"],
    capturedAtLabel: "12m ago",
    coverImageUrl:
      "https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?auto=format&fit=crop&w=1200&q=80",
    metrics: {
      likes: 1280,
      saves: 420,
      comments: 38,
      distanceLabel: "1.2 km",
    },
  },
  {
    id: "bring-the-friends-who-dig-vinyl",
    title: "bring the friends who dig vinyl",
    caption: "Tiny room, no menu anxiety, jazz set starts before nine.",
    creatorHandle: "yapmap.ji",
    placeId: "thin-air-records",
    moodTags: ["pull up?", "lowkey", "group"],
    capturedAtLabel: "31m ago",
    coverImageUrl:
      "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?auto=format&fit=crop&w=1200&q=80",
    metrics: {
      likes: 932,
      saves: 318,
      comments: 44,
      distanceLabel: "3.8 km",
    },
  },
  {
    id: "rain-made-this-gallery-better",
    title: "rain made this gallery better",
    caption: "Concrete, projections, and a quiet corner near the south exit.",
    creatorHandle: "receipt.han",
    placeId: "underpass-gallery",
    moodTags: ["rain route", "solo", "worth it"],
    capturedAtLabel: "1h ago",
    coverImageUrl:
      "https://images.unsplash.com/photo-1545987796-200677ee1011?auto=format&fit=crop&w=1200&q=80",
    metrics: {
      likes: 604,
      saves: 219,
      comments: 17,
      distanceLabel: "4.4 km",
    },
  },
];

export const notmidThreads: NotmidThread[] = [
  {
    id: "tonight-seongsu",
    title: "tonight in seongsu?",
    preview: "Neon Yard looks not mid. meet after 8?",
    updatedAtLabel: "now",
    participantHandles: ["min.zip", "yapmap.ji", "you"],
    attachedPlaceId: "neon-yard",
    attachedClipId: "latte-line-was-worth-it",
    unreadCount: 3,
    chatAccess: acceptedFriendChatAccess(),
  },
  {
    id: "rain-route",
    title: "rain route",
    preview: "receipt.han wants to start a chat from the rain route",
    updatedAtLabel: "18m",
    participantHandles: ["receipt.han", "you"],
    attachedPlaceId: "underpass-gallery",
    attachedClipId: "rain-made-this-gallery-better",
    unreadCount: 0,
    chatAccess: pendingInboundChatAccess(),
  },
];

export const notmidThreadMessages: NotmidThreadMessage[] = [
  {
    id: "msg-tonight-seongsu-1",
    threadId: "tonight-seongsu",
    senderHandle: "min.zip",
    body: "Neon Yard looks current. Is the line still moving?",
    createdAtLabel: "12:08",
    mine: false,
    attachment: {
      type: "clip",
      clipId: "latte-line-was-worth-it",
    },
  },
  {
    id: "msg-tonight-seongsu-2",
    threadId: "tonight-seongsu",
    senderHandle: "you",
    body: "Yes. Seats opened near the window and the clip was from this hour.",
    createdAtLabel: "12:11",
    mine: true,
    attachment: {
      type: "place",
      placeId: "neon-yard",
    },
  },
  {
    id: "msg-rain-route-1",
    threadId: "rain-route",
    senderHandle: "receipt.han",
    body: "Underpass first, then food nearby if the rain keeps up.",
    createdAtLabel: "18m",
    mine: false,
    attachment: {
      type: "route",
      title: "rain route",
      placeIds: ["underpass-gallery", "thin-air-records"],
    },
  },
];

export const notmidFixtureCaptureDraft: NotmidCaptureDraftResponse = {
  source: "fixture",
  generatedAt,
  draft: {
    id: "draft-local-receipt",
    caption: "Steam on the counter, line moving fast, seats opening near the window.",
    placeId: "neon-yard",
    moodTags: ["line proof", "worth it"],
    visibility: "public",
    mediaState: "local-preview",
  },
  candidatePlaces: notmidPlaces,
};

const runtimeEnv = globalThis as typeof globalThis & {
  process?: {
    env?: Record<string, string | undefined>;
  };
};

export const notmidFakeAccessToken =
  runtimeEnv.process?.env?.NOTMID_FAKE_ACCESS_TOKEN ?? "notmid-fake-local-dev-token";

export const notmidFakeAuthUser: NotmidAuthUser = {
  id: "local-you",
  handle: "you.local",
  displayName: "Local You",
  homeNeighborhood: "Seongsu",
  avatarImageUrl:
    "https://images.unsplash.com/photo-1508214751196-bcfd4ca60f91?auto=format&fit=crop&w=400&q=80",
  roles: ["creator", "local-dev"],
};

export const notmidFakeAuthSession: NotmidAuthSession = {
  accessToken: notmidFakeAccessToken,
  provider: "fake",
  expiresAt: "2026-05-24T00:00:00.000Z",
  user: notmidFakeAuthUser,
};

export const notmidSignedOutAuthStatus: NotmidAuthStatusResponse = {
  source: "fixture",
  generatedAt,
  mode: "fake",
  authenticated: false,
  requiredFor: notmidAuthRequiredActions,
};

export const notmidFakeAuthStatus: NotmidAuthStatusResponse = {
  source: "fixture",
  generatedAt,
  mode: "fake",
  authenticated: true,
  user: notmidFakeAuthUser,
  sessionExpiresAt: notmidFakeAuthSession.expiresAt,
  requiredFor: notmidAuthRequiredActions,
};

export const notmidFixtureFeed: NotmidFeedResponse = {
  source: "fixture",
  generatedAt,
  clips: notmidClips,
  places: notmidPlaces,
};

export const notmidFixtureMap: NotmidMapResponse = {
  source: "fixture",
  generatedAt,
  places: notmidPlaces,
  highlightedClipIds: notmidClips.map((clip) => clip.id),
};

export const notmidFixtureInbox: NotmidInboxResponse = {
  source: "fixture",
  generatedAt,
  threads: notmidThreads,
};

export function findNotmidClip(clipId: string): NotmidClip | undefined {
  return notmidClips.find((clip) => clip.id === clipId);
}

export function findNotmidPlace(placeId: string): NotmidPlace | undefined {
  return notmidPlaces.find((place) => place.id === placeId);
}

export function findNotmidThread(threadId: string): NotmidThread | undefined {
  return notmidThreads.find((thread) => thread.id === threadId);
}

export function getNotmidThreadDetail(threadId: string): NotmidThreadDetailResponse | undefined {
  const thread = findNotmidThread(threadId);

  if (!thread) {
    return undefined;
  }

  return {
    source: "fixture",
    generatedAt,
    thread,
    messages: notmidThreadMessages.filter((message) => message.threadId === thread.id),
    attachedClip: thread.attachedClipId ? findNotmidClip(thread.attachedClipId) : undefined,
    attachedPlace: thread.attachedPlaceId ? findNotmidPlace(thread.attachedPlaceId) : undefined,
  };
}

export function acceptedFriendChatAccess(): NotmidChatAccess {
  return {
    relationship: "friend",
    inviteStatus: "accepted",
    canSendMessage: true,
    canAcceptInvite: false,
    canRejectInvite: false,
    reasonLabel: "Friends can chat immediately.",
  };
}

export function acceptedNonFriendChatAccess(): NotmidChatAccess {
  return {
    relationship: "non-friend",
    inviteStatus: "accepted",
    canSendMessage: true,
    canAcceptInvite: false,
    canRejectInvite: false,
    reasonLabel: "Chat request accepted.",
  };
}

export function pendingInboundChatAccess(): NotmidChatAccess {
  return {
    relationship: "non-friend",
    inviteStatus: "pending-inbound",
    canSendMessage: false,
    canAcceptInvite: true,
    canRejectInvite: true,
    reasonLabel: "Accept or reject this chat request before messaging.",
  };
}

export function pendingOutboundChatAccess(): NotmidChatAccess {
  return {
    relationship: "non-friend",
    inviteStatus: "pending-outbound",
    canSendMessage: false,
    canAcceptInvite: false,
    canRejectInvite: false,
    reasonLabel: "Waiting for the other person to accept this chat request.",
  };
}

export function rejectedChatAccess(): NotmidChatAccess {
  return {
    relationship: "non-friend",
    inviteStatus: "rejected",
    canSendMessage: false,
    canAcceptInvite: false,
    canRejectInvite: false,
    reasonLabel: "This chat request was rejected.",
  };
}
