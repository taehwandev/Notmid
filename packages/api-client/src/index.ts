import type {
  NotmidClip,
  NotmidClipSaveResponse,
  NotmidAuthStatusResponse,
  NotmidCaptureDraftResponse,
  NotmidCapturePublishRequest,
  NotmidCapturePublishResponse,
  NotmidFeedResponse,
  NotmidInboxResponse,
  NotmidMapResponse,
  NotmidErrorResponse,
  NotmidPlace,
  NotmidProfileSettingsResponse,
  NotmidProfileSettingsUpdateRequest,
  NotmidProfileSettingsUpdateResponse,
  NotmidResolvedRouteStack,
  NotmidChatInviteResponse,
  NotmidSendThreadMessageRequest,
  NotmidSendThreadMessageResponse,
  NotmidSignInRequest,
  NotmidSignInResponse,
  NotmidStartThreadRequest,
  NotmidStartThreadResponse,
  NotmidThread,
  NotmidThreadDetailResponse,
} from "@notmid/contracts";

export type NotmidApiClientOptions = {
  baseUrl?: string;
  fetcher?: typeof fetch;
};

export class NotmidApiRequestError extends Error {
  constructor(
    message: string,
    readonly status: number,
    readonly statusText: string,
    readonly apiError?: NotmidErrorResponse["error"],
  ) {
    super(message);
    this.name = "NotmidApiRequestError";
  }
}

export type NotmidApiClient = {
  getHealth: () => Promise<{ ok: boolean; service: string }>;
  getAuthStatus: (accessToken?: string) => Promise<NotmidAuthStatusResponse>;
  signIn: (request: NotmidSignInRequest) => Promise<NotmidSignInResponse>;
  getFeed: () => Promise<NotmidFeedResponse>;
  getMap: () => Promise<NotmidMapResponse>;
  getInbox: () => Promise<NotmidInboxResponse>;
  getClip: (clipId: string) => Promise<NotmidClip>;
  saveClip: (clipId: string, accessToken?: string) => Promise<NotmidClipSaveResponse>;
  getProfileSettings: (accessToken?: string) => Promise<NotmidProfileSettingsResponse>;
  updateProfileSettings: (
    request: NotmidProfileSettingsUpdateRequest,
    accessToken?: string,
  ) => Promise<NotmidProfileSettingsUpdateResponse>;
  getPlace: (placeId: string) => Promise<NotmidPlace>;
  getThread: (threadId: string) => Promise<NotmidThread>;
  getThreadDetail: (threadId: string) => Promise<NotmidThreadDetailResponse>;
  acceptThreadInvite: (
    threadId: string,
    accessToken?: string,
  ) => Promise<NotmidChatInviteResponse>;
  rejectThreadInvite: (
    threadId: string,
    accessToken?: string,
  ) => Promise<NotmidChatInviteResponse>;
  startThread: (
    request: NotmidStartThreadRequest,
    accessToken?: string,
  ) => Promise<NotmidStartThreadResponse>;
  sendThreadMessage: (
    threadId: string,
    request: NotmidSendThreadMessageRequest,
    accessToken?: string,
  ) => Promise<NotmidSendThreadMessageResponse>;
  getCaptureDraft: () => Promise<NotmidCaptureDraftResponse>;
  publishCapture: (
    request: NotmidCapturePublishRequest,
    accessToken?: string,
  ) => Promise<NotmidCapturePublishResponse>;
  resolveDeepLink: (url: string) => Promise<NotmidResolvedRouteStack>;
};

export function createNotmidApiClient(options: NotmidApiClientOptions = {}): NotmidApiClient {
  const baseUrl = normalizeBaseUrl(options.baseUrl ?? "http://localhost:8787");
  const fetcher = options.fetcher ?? fetch;

  return {
    getHealth: () => getJson(fetcher, `${baseUrl}/health`),
    getAuthStatus: (accessToken) =>
      getJson(fetcher, `${baseUrl}/v1/auth/status`, accessToken ? authHeaders(accessToken) : {}),
    signIn: (request) => postJson(fetcher, `${baseUrl}/v1/auth/fake-sign-in`, request),
    getFeed: () => getJson(fetcher, `${baseUrl}/v1/feed`),
    getMap: () => getJson(fetcher, `${baseUrl}/v1/map`),
    getInbox: () => getJson(fetcher, `${baseUrl}/v1/inbox/threads`),
    getClip: (clipId) => getJson(fetcher, `${baseUrl}/v1/clips/${encodeURIComponent(clipId)}`),
    saveClip: (clipId, accessToken) =>
      postJson(
        fetcher,
        `${baseUrl}/v1/clips/${encodeURIComponent(clipId)}/save`,
        {},
        accessToken ? authHeaders(accessToken) : {},
      ),
    getProfileSettings: (accessToken) =>
      getJson(
        fetcher,
        `${baseUrl}/v1/profile/settings`,
        accessToken ? authHeaders(accessToken) : {},
      ),
    updateProfileSettings: (request, accessToken) =>
      patchJson(
        fetcher,
        `${baseUrl}/v1/profile/settings`,
        request,
        accessToken ? authHeaders(accessToken) : {},
      ),
    getPlace: (placeId) => getJson(fetcher, `${baseUrl}/v1/places/${encodeURIComponent(placeId)}`),
    getThread: (threadId) =>
      getJson(fetcher, `${baseUrl}/v1/inbox/threads/${encodeURIComponent(threadId)}`),
    getThreadDetail: (threadId) =>
      getJson(fetcher, `${baseUrl}/v1/inbox/threads/${encodeURIComponent(threadId)}/detail`),
    acceptThreadInvite: (threadId, accessToken) =>
      postJson(
        fetcher,
        `${baseUrl}/v1/inbox/threads/${encodeURIComponent(threadId)}/invite/accept`,
        {},
        accessToken ? authHeaders(accessToken) : {},
      ),
    rejectThreadInvite: (threadId, accessToken) =>
      postJson(
        fetcher,
        `${baseUrl}/v1/inbox/threads/${encodeURIComponent(threadId)}/invite/reject`,
        {},
        accessToken ? authHeaders(accessToken) : {},
      ),
    startThread: (request, accessToken) =>
      postJson(
        fetcher,
        `${baseUrl}/v1/inbox/threads`,
        request,
        accessToken ? authHeaders(accessToken) : {},
      ),
    sendThreadMessage: (threadId, request, accessToken) =>
      postJson(
        fetcher,
        `${baseUrl}/v1/inbox/threads/${encodeURIComponent(threadId)}/messages`,
        request,
        accessToken ? authHeaders(accessToken) : {},
      ),
    getCaptureDraft: () => getJson(fetcher, `${baseUrl}/v1/capture/draft`),
    publishCapture: (request, accessToken) =>
      postJson(
        fetcher,
        `${baseUrl}/v1/capture/publish`,
        request,
        accessToken ? authHeaders(accessToken) : {},
      ),
    resolveDeepLink: (url) =>
      getJson(fetcher, `${baseUrl}/v1/deeplinks/resolve?url=${encodeURIComponent(url)}`),
  };
}

export function isNotmidApiRequestError(
  error: unknown,
  status?: number,
): error is NotmidApiRequestError {
  return error instanceof NotmidApiRequestError && (status === undefined || error.status === status);
}

async function getJson<T>(
  fetcher: typeof fetch,
  url: string,
  headers: Record<string, string> = {},
): Promise<T> {
  const response = await fetcher(url, {
    headers: {
      accept: "application/json",
      ...headers,
    },
  });

  if (!response.ok) {
    throw await notmidApiRequestError(response);
  }

  return (await response.json()) as T;
}

async function postJson<TRequest, TResponse>(
  fetcher: typeof fetch,
  url: string,
  body: TRequest,
  headers: Record<string, string> = {},
): Promise<TResponse> {
  const response = await fetcher(url, {
    method: "POST",
    headers: {
      accept: "application/json",
      "content-type": "application/json",
      ...headers,
    },
    body: JSON.stringify(body),
  });

  if (!response.ok) {
    throw await notmidApiRequestError(response);
  }

  return (await response.json()) as TResponse;
}

async function patchJson<TRequest, TResponse>(
  fetcher: typeof fetch,
  url: string,
  body: TRequest,
  headers: Record<string, string> = {},
): Promise<TResponse> {
  const response = await fetcher(url, {
    method: "PATCH",
    headers: {
      accept: "application/json",
      "content-type": "application/json",
      ...headers,
    },
    body: JSON.stringify(body),
  });

  if (!response.ok) {
    throw await notmidApiRequestError(response);
  }

  return (await response.json()) as TResponse;
}

function authHeaders(accessToken: string): Record<string, string> {
  return {
    authorization: `Bearer ${accessToken}`,
  };
}

async function notmidApiRequestError(response: Response): Promise<NotmidApiRequestError> {
  const apiError = await readApiError(response);
  return new NotmidApiRequestError(
    `notmid API request failed: ${response.status} ${response.statusText}`,
    response.status,
    response.statusText,
    apiError,
  );
}

async function readApiError(response: Response): Promise<NotmidErrorResponse["error"] | undefined> {
  try {
    return parseApiError(await response.clone().json());
  } catch {
    return undefined;
  }
}

function parseApiError(value: unknown): NotmidErrorResponse["error"] | undefined {
  if (!value || typeof value !== "object" || !("error" in value)) {
    return undefined;
  }

  const error = (value as { error?: unknown }).error;
  if (!error || typeof error !== "object") {
    return undefined;
  }

  const code = (error as { code?: unknown }).code;
  const message = (error as { message?: unknown }).message;
  const requestId = (error as { requestId?: unknown }).requestId;

  if (typeof code !== "string" || typeof message !== "string") {
    return undefined;
  }

  return typeof requestId === "string" ? { code, message, requestId } : { code, message };
}

function normalizeBaseUrl(baseUrl: string): string {
  return baseUrl.endsWith("/") ? baseUrl.slice(0, -1) : baseUrl;
}
