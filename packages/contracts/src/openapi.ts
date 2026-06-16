export const notmidOpenApiDocument = {
  openapi: "3.1.0",
  info: {
    title: "notmid API",
    version: "0.1.0",
    description:
      "Contract surface for the notmid server-first product API. Local fixture mode is supported, but production deployments must provide real auth and environment configuration.",
  },
  servers: [
    {
      url: "http://localhost:8787",
      description: "Local development",
    },
    {
      url: "https://thdev.app",
      description: "Production host placeholder",
    },
  ],
  tags: [
    { name: "health" },
    { name: "auth" },
    { name: "feed" },
    { name: "map" },
    { name: "capture" },
    { name: "inbox" },
    { name: "profile" },
    { name: "deeplinks" },
  ],
  paths: {
    "/health": {
      get: {
        tags: ["health"],
        operationId: "getHealth",
        responses: {
          "200": jsonResponse("API health", "HealthResponse"),
        },
      },
    },
    "/openapi.json": {
      get: {
        tags: ["health"],
        operationId: "getOpenApiDocument",
        responses: {
          "200": {
            description: "OpenAPI contract document",
          },
        },
      },
    },
    "/v1/auth/status": {
      get: {
        tags: ["auth"],
        operationId: "getAuthStatus",
        security: optionalBearerSecurity(),
        responses: {
          "200": jsonResponse("Current auth status", "NotmidAuthStatusResponse"),
        },
      },
    },
    "/v1/auth/fake-sign-in": {
      post: {
        tags: ["auth"],
        operationId: "fakeSignIn",
        requestBody: jsonRequest("Sign-in request", "NotmidSignInRequest"),
        responses: {
          "200": jsonResponse("Local fake session", "NotmidSignInResponse"),
          "400": jsonResponse("Invalid JSON request", "NotmidErrorResponse"),
          "409": jsonResponse("Fake auth disabled", "NotmidErrorResponse"),
          "429": jsonResponse("Rate limited", "NotmidErrorResponse"),
        },
      },
    },
    "/v1/profile/settings": {
      get: {
        tags: ["profile"],
        operationId: "getProfileSettings",
        security: requiredBearerSecurity(),
        responses: {
          "200": jsonResponse("Profile settings", "NotmidProfileSettingsResponse"),
          "401": jsonResponse("Auth required", "NotmidErrorResponse"),
        },
      },
      patch: {
        tags: ["profile"],
        operationId: "updateProfileSettings",
        security: requiredBearerSecurity(),
        requestBody: jsonRequest(
          "Profile settings update request",
          "NotmidProfileSettingsUpdateRequest",
        ),
        responses: {
          "200": jsonResponse(
            "Profile settings update response",
            "NotmidProfileSettingsUpdateResponse",
          ),
          "400": jsonResponse("Invalid profile settings request", "NotmidErrorResponse"),
          "401": jsonResponse("Auth required", "NotmidErrorResponse"),
          "429": jsonResponse("Rate limited", "NotmidErrorResponse"),
        },
      },
    },
    "/v1/capture/draft": {
      get: {
        tags: ["capture"],
        operationId: "getCaptureDraft",
        responses: {
          "200": jsonResponse("Capture draft", "NotmidCaptureDraftResponse"),
        },
      },
    },
    "/v1/capture/publish": {
      post: {
        tags: ["capture"],
        operationId: "publishCapture",
        security: requiredBearerSecurity(),
        requestBody: jsonRequest("Capture publish request", "NotmidCapturePublishRequest"),
        responses: {
          "200": jsonResponse("Capture publish response", "NotmidCapturePublishResponse"),
          "400": jsonResponse("Invalid capture request", "NotmidErrorResponse"),
          "401": jsonResponse("Auth required", "NotmidErrorResponse"),
          "404": jsonResponse("Place not found", "NotmidErrorResponse"),
          "429": jsonResponse("Rate limited", "NotmidErrorResponse"),
        },
      },
    },
    "/v1/feed": {
      get: {
        tags: ["feed"],
        operationId: "getFeed",
        responses: {
          "200": jsonResponse("Feed response", "NotmidFeedResponse"),
        },
      },
    },
    "/v1/map": {
      get: {
        tags: ["map"],
        operationId: "getMap",
        responses: {
          "200": jsonResponse("Map response", "NotmidMapResponse"),
        },
      },
    },
    "/v1/clips/{clipId}": {
      get: {
        tags: ["feed"],
        operationId: "getClip",
        parameters: [pathParameter("clipId", "Clip id")],
        responses: {
          "200": jsonResponse("Clip detail", "NotmidClip"),
          "404": jsonResponse("Clip not found", "NotmidErrorResponse"),
        },
      },
    },
    "/v1/clips/{clipId}/save": {
      post: {
        tags: ["feed"],
        operationId: "saveClip",
        security: requiredBearerSecurity(),
        parameters: [pathParameter("clipId", "Clip id")],
        responses: {
          "200": jsonResponse("Clip save response", "NotmidClipSaveResponse"),
          "401": jsonResponse("Auth required", "NotmidErrorResponse"),
          "404": jsonResponse("Clip not found", "NotmidErrorResponse"),
          "429": jsonResponse("Rate limited", "NotmidErrorResponse"),
        },
      },
    },
    "/v1/places/{placeId}": {
      get: {
        tags: ["map"],
        operationId: "getPlace",
        parameters: [pathParameter("placeId", "Place id")],
        responses: {
          "200": jsonResponse("Place detail", "NotmidPlace"),
          "404": jsonResponse("Place not found", "NotmidErrorResponse"),
        },
      },
    },
    "/v1/inbox/threads": {
      get: {
        tags: ["inbox"],
        operationId: "getInboxThreads",
        responses: {
          "200": jsonResponse("Inbox threads", "NotmidInboxResponse"),
        },
      },
      post: {
        tags: ["inbox"],
        operationId: "startInboxThread",
        security: requiredBearerSecurity(),
        requestBody: jsonRequest("Start thread request", "NotmidStartThreadRequest"),
        responses: {
          "200": jsonResponse("Start thread response", "NotmidStartThreadResponse"),
          "400": jsonResponse("Invalid start thread request", "NotmidErrorResponse"),
          "401": jsonResponse("Auth required", "NotmidErrorResponse"),
          "404": jsonResponse("Participant or attachment not found", "NotmidErrorResponse"),
          "429": jsonResponse("Rate limited", "NotmidErrorResponse"),
        },
      },
    },
    "/v1/inbox/threads/{threadId}": {
      get: {
        tags: ["inbox"],
        operationId: "getInboxThread",
        parameters: [pathParameter("threadId", "Thread id")],
        responses: {
          "200": jsonResponse("Thread summary", "NotmidThread"),
          "404": jsonResponse("Thread not found", "NotmidErrorResponse"),
        },
      },
    },
    "/v1/inbox/threads/{threadId}/detail": {
      get: {
        tags: ["inbox"],
        operationId: "getInboxThreadDetail",
        parameters: [pathParameter("threadId", "Thread id")],
        responses: {
          "200": jsonResponse("Thread detail", "NotmidThreadDetailResponse"),
          "404": jsonResponse("Thread not found", "NotmidErrorResponse"),
        },
      },
    },
    "/v1/inbox/threads/{threadId}/invite/accept": {
      post: {
        tags: ["inbox"],
        operationId: "acceptInboxThreadInvite",
        security: requiredBearerSecurity(),
        parameters: [pathParameter("threadId", "Thread id")],
        responses: {
          "200": jsonResponse("Chat invite response", "NotmidChatInviteResponse"),
          "400": jsonResponse("Invite not actionable", "NotmidErrorResponse"),
          "401": jsonResponse("Auth required", "NotmidErrorResponse"),
          "404": jsonResponse("Thread not found", "NotmidErrorResponse"),
          "429": jsonResponse("Rate limited", "NotmidErrorResponse"),
        },
      },
    },
    "/v1/inbox/threads/{threadId}/invite/reject": {
      post: {
        tags: ["inbox"],
        operationId: "rejectInboxThreadInvite",
        security: requiredBearerSecurity(),
        parameters: [pathParameter("threadId", "Thread id")],
        responses: {
          "200": jsonResponse("Chat invite response", "NotmidChatInviteResponse"),
          "400": jsonResponse("Invite not actionable", "NotmidErrorResponse"),
          "401": jsonResponse("Auth required", "NotmidErrorResponse"),
          "404": jsonResponse("Thread not found", "NotmidErrorResponse"),
          "429": jsonResponse("Rate limited", "NotmidErrorResponse"),
        },
      },
    },
    "/v1/inbox/threads/{threadId}/messages": {
      post: {
        tags: ["inbox"],
        operationId: "sendInboxThreadMessage",
        security: requiredBearerSecurity(),
        parameters: [pathParameter("threadId", "Thread id")],
        requestBody: jsonRequest("Send message request", "NotmidSendThreadMessageRequest"),
        responses: {
          "200": jsonResponse("Send message response", "NotmidSendThreadMessageResponse"),
          "400": jsonResponse("Invalid message request", "NotmidErrorResponse"),
          "401": jsonResponse("Auth required", "NotmidErrorResponse"),
          "403": jsonResponse("Chat invite required", "NotmidErrorResponse"),
          "404": jsonResponse("Thread not found", "NotmidErrorResponse"),
          "429": jsonResponse("Rate limited", "NotmidErrorResponse"),
        },
      },
    },
    "/v1/deeplinks/resolve": {
      get: {
        tags: ["deeplinks"],
        operationId: "resolveDeepLink",
        parameters: [
          {
            name: "url",
            in: "query",
            required: false,
            schema: { type: "string" },
          },
        ],
        responses: {
          "200": jsonResponse("Resolved route stack", "NotmidResolvedRouteStack"),
        },
      },
    },
  },
  components: {
    securitySchemes: {
      bearerAuth: {
        type: "http",
        scheme: "bearer",
      },
    },
    schemas: {
      HealthResponse: objectSchema({
        ok: { type: "boolean" },
        requestId: { type: "string" },
        service: { type: "string" },
        mode: { $ref: "#/components/schemas/NotmidAuthMode" },
      }),
      NotmidSource: { type: "string", enum: ["fixture", "api", "cache"] },
      NotmidAuthMode: { type: "string", enum: ["fake", "firebase", "disabled"] },
      NotmidAuthProvider: { type: "string", enum: ["fake", "anonymous", "google"] },
      NotmidAuthIntent: { type: "string", enum: ["browse", "capture", "chat", "profile"] },
      NotmidAuthRequirement: {
        type: "string",
        enum: ["capture", "save", "chat", "profile-edit", "moderation"],
      },
      NotmidCaptureVisibility: { type: "string", enum: ["public", "friends", "private"] },
      NotmidMetricSet: objectSchema({
        likes: { type: "number" },
        saves: { type: "number" },
        comments: { type: "number" },
        distanceLabel: { type: "string" },
      }),
      NotmidPlace: objectSchema({
        id: { type: "string" },
        name: { type: "string" },
        neighborhood: { type: "string" },
        category: { type: "string" },
        address: { type: "string" },
        lat: { type: "number" },
        lng: { type: "number" },
        openNow: { type: "boolean" },
        score: { type: "number" },
        receiptCount: { type: "number" },
        coverImageUrl: { type: "string" },
      }),
      NotmidClip: objectSchema({
        id: { type: "string" },
        title: { type: "string" },
        caption: { type: "string" },
        creatorHandle: { type: "string" },
        placeId: { type: "string" },
        moodTags: arrayOf({ type: "string" }),
        capturedAtLabel: { type: "string" },
        coverImageUrl: { type: "string" },
        videoUrl: { type: "string" },
        metrics: { $ref: "#/components/schemas/NotmidMetricSet" },
      }),
      NotmidClipSaveResponse: objectSchema({
        source: { $ref: "#/components/schemas/NotmidSource" },
        generatedAt: { type: "string" },
        clip: { $ref: "#/components/schemas/NotmidClip" },
        saved: { type: "boolean" },
      }),
      NotmidChatRelationship: { type: "string", enum: ["friend", "non-friend"] },
      NotmidChatInviteStatus: {
        type: "string",
        enum: ["accepted", "pending-inbound", "pending-outbound", "rejected"],
      },
      NotmidChatAccess: objectSchema({
        relationship: { $ref: "#/components/schemas/NotmidChatRelationship" },
        inviteStatus: { $ref: "#/components/schemas/NotmidChatInviteStatus" },
        canSendMessage: { type: "boolean" },
        canAcceptInvite: { type: "boolean" },
        canRejectInvite: { type: "boolean" },
        reasonLabel: { type: "string" },
      }),
      NotmidThread: objectSchema({
        id: { type: "string" },
        title: { type: "string" },
        preview: { type: "string" },
        updatedAtLabel: { type: "string" },
        participantHandles: arrayOf({ type: "string" }),
        attachedPlaceId: { type: "string" },
        attachedClipId: { type: "string" },
        unreadCount: { type: "number" },
        chatAccess: { $ref: "#/components/schemas/NotmidChatAccess" },
      }),
      NotmidMessageAttachment: {
        oneOf: [
          objectSchema({ type: { const: "clip" }, clipId: { type: "string" } }),
          objectSchema({ type: { const: "place" }, placeId: { type: "string" } }),
          objectSchema({ type: { const: "route" }, title: { type: "string" }, placeIds: arrayOf({ type: "string" }) }),
        ],
      },
      NotmidThreadMessage: objectSchema({
        id: { type: "string" },
        threadId: { type: "string" },
        senderHandle: { type: "string" },
        body: { type: "string" },
        createdAtLabel: { type: "string" },
        mine: { type: "boolean" },
        attachment: { $ref: "#/components/schemas/NotmidMessageAttachment" },
      }),
      NotmidThreadDetailResponse: objectSchema({
        source: { $ref: "#/components/schemas/NotmidSource" },
        generatedAt: { type: "string" },
        thread: { $ref: "#/components/schemas/NotmidThread" },
        messages: arrayOf({ $ref: "#/components/schemas/NotmidThreadMessage" }),
        attachedClip: { $ref: "#/components/schemas/NotmidClip" },
        attachedPlace: { $ref: "#/components/schemas/NotmidPlace" },
      }),
      NotmidSendThreadMessageRequest: objectSchema({
        body: { type: "string" },
        attachment: { $ref: "#/components/schemas/NotmidMessageAttachment" },
      }),
      NotmidStartThreadRequest: objectSchema({
        participantHandle: { type: "string" },
        body: { type: "string" },
        attachedClipId: { type: "string" },
        attachedPlaceId: { type: "string" },
      }),
      NotmidSendThreadMessageResponse: objectSchema({
        source: { $ref: "#/components/schemas/NotmidSource" },
        generatedAt: { type: "string" },
        message: { $ref: "#/components/schemas/NotmidThreadMessage" },
      }),
      NotmidChatInviteResponse: objectSchema({
        source: { $ref: "#/components/schemas/NotmidSource" },
        generatedAt: { type: "string" },
        thread: { $ref: "#/components/schemas/NotmidThread" },
      }),
      NotmidStartThreadResponse: objectSchema({
        source: { $ref: "#/components/schemas/NotmidSource" },
        generatedAt: { type: "string" },
        thread: { $ref: "#/components/schemas/NotmidThread" },
        message: { $ref: "#/components/schemas/NotmidThreadMessage" },
      }),
      NotmidCaptureDraft: objectSchema({
        id: { type: "string" },
        caption: { type: "string" },
        placeId: { type: "string" },
        moodTags: arrayOf({ type: "string" }),
        visibility: { $ref: "#/components/schemas/NotmidCaptureVisibility" },
        mediaState: { type: "string", enum: ["empty", "local-preview", "uploaded"] },
      }),
      NotmidCaptureDraftResponse: objectSchema({
        source: { $ref: "#/components/schemas/NotmidSource" },
        generatedAt: { type: "string" },
        draft: { $ref: "#/components/schemas/NotmidCaptureDraft" },
        candidatePlaces: arrayOf({ $ref: "#/components/schemas/NotmidPlace" }),
      }),
      NotmidCapturePublishRequest: objectSchema({
        draftId: { type: "string" },
        caption: { type: "string" },
        placeId: { type: "string" },
        moodTags: arrayOf({ type: "string" }),
        visibility: { $ref: "#/components/schemas/NotmidCaptureVisibility" },
      }),
      NotmidCapturePublishResponse: objectSchema({
        source: { $ref: "#/components/schemas/NotmidSource" },
        generatedAt: { type: "string" },
        clip: { $ref: "#/components/schemas/NotmidClip" },
        moderationStatus: { type: "string", enum: ["queued", "published"] },
      }),
      NotmidFeedResponse: objectSchema({
        source: { $ref: "#/components/schemas/NotmidSource" },
        generatedAt: { type: "string" },
        clips: arrayOf({ $ref: "#/components/schemas/NotmidClip" }),
        places: arrayOf({ $ref: "#/components/schemas/NotmidPlace" }),
      }),
      NotmidMapResponse: objectSchema({
        source: { $ref: "#/components/schemas/NotmidSource" },
        generatedAt: { type: "string" },
        places: arrayOf({ $ref: "#/components/schemas/NotmidPlace" }),
        highlightedClipIds: arrayOf({ type: "string" }),
      }),
      NotmidInboxResponse: objectSchema({
        source: { $ref: "#/components/schemas/NotmidSource" },
        generatedAt: { type: "string" },
        threads: arrayOf({ $ref: "#/components/schemas/NotmidThread" }),
      }),
      NotmidAuthUser: objectSchema({
        id: { type: "string" },
        handle: { type: "string" },
        displayName: { type: "string" },
        homeNeighborhood: { type: "string" },
        avatarImageUrl: { type: "string" },
        roles: arrayOf({ type: "string" }),
      }),
      NotmidProfilePrivacySettings: objectSchema({
        savedPlacesVisibility: { type: "string", enum: ["private"] },
        chatInvites: { type: "string", enum: ["shared-clips-and-places"] },
        defaultReceiptVisibility: { $ref: "#/components/schemas/NotmidCaptureVisibility" },
      }),
      NotmidProfileSettings: objectSchema({
        user: { $ref: "#/components/schemas/NotmidAuthUser" },
        privacy: { $ref: "#/components/schemas/NotmidProfilePrivacySettings" },
      }),
      NotmidProfileSettingsResponse: objectSchema({
        source: { $ref: "#/components/schemas/NotmidSource" },
        generatedAt: { type: "string" },
        settings: { $ref: "#/components/schemas/NotmidProfileSettings" },
      }),
      NotmidProfileSettingsUpdateRequest: objectSchema({
        displayName: { type: "string" },
        homeNeighborhood: { type: "string" },
      }),
      NotmidProfileSettingsUpdateResponse: objectSchema({
        source: { $ref: "#/components/schemas/NotmidSource" },
        generatedAt: { type: "string" },
        settings: { $ref: "#/components/schemas/NotmidProfileSettings" },
        updated: { type: "boolean" },
      }),
      NotmidAuthSession: objectSchema({
        accessToken: { type: "string" },
        provider: { $ref: "#/components/schemas/NotmidAuthProvider" },
        expiresAt: { type: "string" },
        user: { $ref: "#/components/schemas/NotmidAuthUser" },
      }),
      NotmidAuthStatusResponse: objectSchema({
        source: { $ref: "#/components/schemas/NotmidSource" },
        generatedAt: { type: "string" },
        mode: { $ref: "#/components/schemas/NotmidAuthMode" },
        authenticated: { type: "boolean" },
        user: { $ref: "#/components/schemas/NotmidAuthUser" },
        sessionExpiresAt: { type: "string" },
        requiredFor: arrayOf({ $ref: "#/components/schemas/NotmidAuthRequirement" }),
      }),
      NotmidSignInRequest: objectSchema({
        provider: { $ref: "#/components/schemas/NotmidAuthProvider" },
        intent: { $ref: "#/components/schemas/NotmidAuthIntent" },
        returnTo: { type: "string" },
      }),
      NotmidSignInResponse: objectSchema({
        mode: { $ref: "#/components/schemas/NotmidAuthMode" },
        session: { $ref: "#/components/schemas/NotmidAuthSession" },
        nextPath: { type: "string" },
      }),
      NotmidResolvedRoute: objectSchema({
        kind: { type: "string" },
        params: {
          type: "object",
          additionalProperties: { type: "string" },
        },
      }),
      NotmidResolvedRouteStack: objectSchema({
        canonicalPath: { type: "string" },
        stack: arrayOf({ $ref: "#/components/schemas/NotmidResolvedRoute" }),
      }),
      NotmidErrorResponse: objectSchema({
        error: objectSchema({
          code: { type: "string" },
          message: { type: "string" },
          requestId: { type: "string" },
        }),
      }),
    },
  },
} as const;

export type NotmidOpenApiDocument = typeof notmidOpenApiDocument;

function jsonResponse(description: string, schemaName: string) {
  return {
    description,
    content: {
      "application/json": {
        schema: { $ref: `#/components/schemas/${schemaName}` },
      },
    },
  };
}

function jsonRequest(description: string, schemaName: string) {
  return {
    required: true,
    description,
    content: {
      "application/json": {
        schema: { $ref: `#/components/schemas/${schemaName}` },
      },
    },
  };
}

function pathParameter(name: string, description: string) {
  return {
    name,
    in: "path",
    required: true,
    description,
    schema: { type: "string" },
  };
}

function optionalBearerSecurity() {
  return [{}, { bearerAuth: [] }];
}

function requiredBearerSecurity() {
  return [{ bearerAuth: [] }];
}

function objectSchema(properties: Record<string, unknown>) {
  return {
    type: "object",
    properties,
  };
}

function arrayOf(items: unknown) {
  return {
    type: "array",
    items,
  };
}
