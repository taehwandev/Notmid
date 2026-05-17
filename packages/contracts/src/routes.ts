export const notmidWebBasePath = "/notmid";

export const notmidRoutes = {
  home: notmidWebBasePath,
  feed: `${notmidWebBasePath}/feed`,
  map: `${notmidWebBasePath}/map`,
  capture: `${notmidWebBasePath}/capture`,
  inbox: `${notmidWebBasePath}/inbox`,
  profile: `${notmidWebBasePath}/profile`,
  profileSettings: `${notmidWebBasePath}/profile/settings`,
  clip: (clipId: string) => `${notmidWebBasePath}/clips/${encodeURIComponent(clipId)}`,
  feedClip: (clipId: string) => `${notmidWebBasePath}/feed/clips/${encodeURIComponent(clipId)}`,
  place: (placeId: string) => `${notmidWebBasePath}/places/${encodeURIComponent(placeId)}`,
  mapPlace: (placeId: string) => `${notmidWebBasePath}/map/places/${encodeURIComponent(placeId)}`,
  chat: (threadId: string) => `${notmidWebBasePath}/chats/${encodeURIComponent(threadId)}`,
  inboxChat: (threadId: string) => `${notmidWebBasePath}/inbox/chats/${encodeURIComponent(threadId)}`,
};

export type NotmidRouteKind =
  | "feed"
  | "map"
  | "capture"
  | "inbox"
  | "profile"
  | "profile-settings"
  | "clip-detail"
  | "place-detail"
  | "chat-thread"
  | "web-fallback";

export type NotmidResolvedRoute = {
  kind: NotmidRouteKind;
  params?: Record<string, string>;
};

export type NotmidResolvedRouteStack = {
  canonicalPath: string;
  stack: NotmidResolvedRoute[];
};

export function resolveNotmidPathStack(input: string): NotmidResolvedRouteStack {
  const url = toUrl(input);
  const segments = url.pathname.split("/").filter(Boolean);

  if (segments[0] !== "notmid") {
    return {
      canonicalPath: notmidRoutes.home,
      stack: [{ kind: "web-fallback", params: { path: url.pathname } }],
    };
  }

  const section = segments[1];
  const id = segments[2] ? decodeURIComponent(segments[2]) : undefined;
  const nestedKind = segments[2];
  const nestedId = segments[3] ? decodeURIComponent(segments[3]) : undefined;

  if (section === "feed" && nestedKind === "clips" && nestedId) {
    return {
      canonicalPath: notmidRoutes.clip(nestedId),
      stack: [{ kind: "feed" }, { kind: "clip-detail", params: { clipId: nestedId } }],
    };
  }

  if (!section || section === "feed" || section === "home") {
    return { canonicalPath: notmidRoutes.feed, stack: [{ kind: "feed" }] };
  }

  if (section === "clips" && id) {
    return {
      canonicalPath: notmidRoutes.clip(id),
      stack: [{ kind: "feed" }, { kind: "clip-detail", params: { clipId: id } }],
    };
  }

  if (section === "places" && id) {
    return {
      canonicalPath: notmidRoutes.place(id),
      stack: [{ kind: "map" }, { kind: "place-detail", params: { placeId: id } }],
    };
  }

  if (section === "map" && nestedKind === "places" && nestedId) {
    return {
      canonicalPath: notmidRoutes.place(nestedId),
      stack: [{ kind: "map" }, { kind: "place-detail", params: { placeId: nestedId } }],
    };
  }

  if (section === "map") {
    return { canonicalPath: notmidRoutes.map, stack: [{ kind: "map" }] };
  }

  if (section === "capture") {
    return { canonicalPath: notmidRoutes.capture, stack: [{ kind: "capture" }] };
  }

  if (section === "chats" && id) {
    return {
      canonicalPath: notmidRoutes.chat(id),
      stack: [{ kind: "inbox" }, { kind: "chat-thread", params: { threadId: id } }],
    };
  }

  if (section === "inbox" && nestedKind === "chats" && nestedId) {
    return {
      canonicalPath: notmidRoutes.chat(nestedId),
      stack: [{ kind: "inbox" }, { kind: "chat-thread", params: { threadId: nestedId } }],
    };
  }

  if (section === "inbox") {
    return { canonicalPath: notmidRoutes.inbox, stack: [{ kind: "inbox" }] };
  }

  if (section === "profile" && segments[2] === "settings") {
    return {
      canonicalPath: notmidRoutes.profileSettings,
      stack: [{ kind: "profile" }, { kind: "profile-settings" }],
    };
  }

  if (section === "profile") {
    return { canonicalPath: notmidRoutes.profile, stack: [{ kind: "profile" }] };
  }

  return {
    canonicalPath: notmidRoutes.home,
    stack: [{ kind: "web-fallback", params: { path: url.pathname } }],
  };
}

function toUrl(input: string): URL {
  try {
    return new URL(input);
  } catch {
    return new URL(input.startsWith("/") ? input : `/${input}`, "https://thdev.app");
  }
}
