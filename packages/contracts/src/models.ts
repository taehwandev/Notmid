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

export type NotmidThread = {
  id: string;
  title: string;
  preview: string;
  updatedAtLabel: string;
  participantHandles: string[];
  attachedPlaceId?: string;
  attachedClipId?: string;
  unreadCount: number;
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

export type NotmidErrorResponse = {
  error: {
    code: string;
    message: string;
  };
};
