import { randomUUID } from "node:crypto";
import type { NotmidErrorResponse } from "@notmid/contracts";

const requestIdPattern = /^[A-Za-z0-9._:-]{8,80}$/;

export type NotmidRequestLog = {
  durationMs: number;
  method: string;
  path: string;
  requestId: string;
  status: number;
};

export type NotmidAuditLog = {
  action: string;
  actorId?: string;
  authMode: string;
  method: string;
  outcome: "success" | "denied" | "failed";
  path: string;
  requestId: string;
  status: number;
};

export function resolveRequestId(incomingRequestId: string | undefined): string {
  if (incomingRequestId && requestIdPattern.test(incomingRequestId)) {
    return incomingRequestId;
  }

  return randomUUID();
}

export function pathFromUrl(url: string): string {
  try {
    return new URL(url).pathname;
  } catch {
    return "/unknown";
  }
}

export function errorResponse(
  code: string,
  message: string,
  requestId: string,
): NotmidErrorResponse {
  return {
    error: {
      code,
      message,
      requestId,
    },
  };
}

export function logRequest(details: NotmidRequestLog): void {
  const event = {
    durationMs: details.durationMs,
    event: "notmid_api_request",
    method: details.method,
    path: details.path,
    requestId: details.requestId,
    status: details.status,
  };
  const line = JSON.stringify(event);

  if (details.status >= 500) {
    console.error(line);
    return;
  }

  if (details.status >= 400) {
    console.warn(line);
    return;
  }

  console.log(line);
}

export function logAuditEvent(details: NotmidAuditLog): void {
  const line = JSON.stringify({
    action: details.action,
    actorId: details.actorId ?? "anonymous",
    authMode: details.authMode,
    event: "notmid_api_audit",
    method: details.method,
    outcome: details.outcome,
    path: details.path,
    requestId: details.requestId,
    status: details.status,
  });

  if (details.outcome === "success") {
    console.log(line);
    return;
  }

  console.warn(line);
}

export function logUnhandledError(error: Error, details: Omit<NotmidRequestLog, "durationMs">): void {
  console.error(
    JSON.stringify({
      errorName: error.name,
      event: "notmid_api_unhandled_error",
      method: details.method,
      path: details.path,
      requestId: details.requestId,
      status: details.status,
    }),
  );
}
