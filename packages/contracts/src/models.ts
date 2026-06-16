export type NotmidSource = "fixture" | "api" | "cache";

export type NotmidMetricSet = {
  likes: number;
  saves: number;
  comments: number;
  distanceLabel: string;
};

export type NotmidPlace = {
  id: string;
  name: string;
  neighborhood: string;
  category: string;
  address: string;
  lat: number;
  lng: number;
  openNow: boolean;
  score: number;
  receiptCount: number;
  coverImageUrl: string;
};

export type NotmidClip = {
  id: string;
  title: string;
  caption: string;
  creatorHandle: string;
  placeId: string;
  moodTags: string[];
  capturedAtLabel: string;
  coverImageUrl: string;
  videoUrl?: string;
  metrics: NotmidMetricSet;
};

export type NotmidClipSaveResponse = {
  source: NotmidSource;
  generatedAt: string;
  clip: NotmidClip;
  saved: true;
};

export type NotmidThread = {
  id: string;
  title: string;
  preview: string;
  updatedAtLabel: string;
  participantHandles: string[];
  attachedPlaceId?: string;
  attachedClipId?: string;
  unreadCount: number;
  chatAccess?: NotmidChatAccess;
};

export type NotmidChatRelationship = "friend" | "non-friend";

export type NotmidChatInviteStatus =
  | "accepted"
  | "pending-inbound"
  | "pending-outbound"
  | "rejected";

export type NotmidChatAccess = {
  relationship: NotmidChatRelationship;
  inviteStatus: NotmidChatInviteStatus;
  canSendMessage: boolean;
  canAcceptInvite: boolean;
  canRejectInvite: boolean;
  reasonLabel: string;
};

export type NotmidChatInviteDecision = "accept" | "reject";

export type NotmidStartThreadRequest = {
  participantHandle: string;
  body: string;
  attachedClipId?: string;
  attachedPlaceId?: string;
};

export type NotmidMessageAttachment =
  | {
      type: "clip";
      clipId: string;
    }
  | {
      type: "place";
      placeId: string;
    }
  | {
      type: "route";
      title: string;
      placeIds: string[];
    };

export type NotmidThreadMessage = {
  id: string;
  threadId: string;
  senderHandle: string;
  body: string;
  createdAtLabel: string;
  mine: boolean;
  attachment?: NotmidMessageAttachment;
};

export type NotmidThreadDetailResponse = {
  source: NotmidSource;
  generatedAt: string;
  thread: NotmidThread;
  messages: NotmidThreadMessage[];
  attachedClip?: NotmidClip;
  attachedPlace?: NotmidPlace;
};

export type NotmidSendThreadMessageRequest = {
  body: string;
  attachment?: NotmidMessageAttachment;
};

export type NotmidSendThreadMessageResponse = {
  source: NotmidSource;
  generatedAt: string;
  message: NotmidThreadMessage;
};

export type NotmidChatInviteResponse = {
  source: NotmidSource;
  generatedAt: string;
  thread: NotmidThread;
};

export type NotmidStartThreadResponse = {
  source: NotmidSource;
  generatedAt: string;
  thread: NotmidThread;
  message?: NotmidThreadMessage;
};

export type NotmidCaptureVisibility = "public" | "friends" | "private";

export type NotmidCaptureDraft = {
  id: string;
  caption: string;
  placeId?: string;
  moodTags: string[];
  visibility: NotmidCaptureVisibility;
  mediaState: "empty" | "local-preview" | "uploaded";
};

export type NotmidCaptureDraftResponse = {
  source: NotmidSource;
  generatedAt: string;
  draft: NotmidCaptureDraft;
  candidatePlaces: NotmidPlace[];
};

export type NotmidCapturePublishRequest = {
  draftId: string;
  caption: string;
  placeId: string;
  moodTags: string[];
  visibility: NotmidCaptureVisibility;
};

export type NotmidCapturePublishResponse = {
  source: NotmidSource;
  generatedAt: string;
  clip: NotmidClip;
  moderationStatus: "queued" | "published";
};

export type NotmidFeedResponse = {
  source: NotmidSource;
  generatedAt: string;
  clips: NotmidClip[];
  places: NotmidPlace[];
};

export type NotmidMapResponse = {
  source: NotmidSource;
  generatedAt: string;
  places: NotmidPlace[];
  highlightedClipIds: string[];
};

export type NotmidInboxResponse = {
  source: NotmidSource;
  generatedAt: string;
  threads: NotmidThread[];
};

export type NotmidAuthMode = "fake" | "firebase" | "disabled";

export type NotmidAuthProvider = "fake" | "anonymous" | "google";

export type NotmidAuthIntent = "browse" | "capture" | "chat" | "profile";

export type NotmidAuthRequirement = "capture" | "save" | "chat" | "profile-edit" | "moderation";

export type NotmidAuthUser = {
  id: string;
  handle: string;
  displayName: string;
  homeNeighborhood: string;
  avatarImageUrl: string;
  roles: string[];
};

export type NotmidProfilePrivacySettings = {
  savedPlacesVisibility: "private";
  chatInvites: "shared-clips-and-places";
  defaultReceiptVisibility: NotmidCaptureVisibility;
};

export type NotmidProfileSettings = {
  user: NotmidAuthUser;
  privacy: NotmidProfilePrivacySettings;
};

export type NotmidProfileSettingsResponse = {
  source: NotmidSource;
  generatedAt: string;
  settings: NotmidProfileSettings;
};

export type NotmidProfileSettingsUpdateRequest = {
  displayName: string;
  homeNeighborhood: string;
};

export type NotmidProfileSettingsUpdateResponse = {
  source: NotmidSource;
  generatedAt: string;
  settings: NotmidProfileSettings;
  updated: true;
};

export type NotmidAuthSession = {
  accessToken: string;
  provider: NotmidAuthProvider;
  expiresAt: string;
  user: NotmidAuthUser;
};

export type NotmidAuthStatusResponse = {
  source: NotmidSource;
  generatedAt: string;
  mode: NotmidAuthMode;
  authenticated: boolean;
  user?: NotmidAuthUser;
  sessionExpiresAt?: string;
  requiredFor: NotmidAuthRequirement[];
};

export type NotmidSignInRequest = {
  provider: NotmidAuthProvider;
  intent?: NotmidAuthIntent;
  returnTo?: string;
};

export type NotmidSignInResponse = {
  mode: NotmidAuthMode;
  session: NotmidAuthSession;
  nextPath: string;
};

export type NotmidErrorResponse = {
  error: {
    code: string;
    message: string;
    requestId?: string;
  };
};
