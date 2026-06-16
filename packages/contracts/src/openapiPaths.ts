import {
  jsonRequest,
  jsonResponse,
  optionalBearerSecurity,
  pathParameter,
  requiredBearerSecurity,
} from "./openapiHelpers";

export const notmidOpenApiPaths = {
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
} as const;
