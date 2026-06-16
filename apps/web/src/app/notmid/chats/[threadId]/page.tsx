import { isNotmidApiRequestError } from "@notmid/api-client";
import { getNotmidThreadDetail, notmidRoutes, type NotmidMessageAttachment } from "@notmid/contracts";
import { revalidatePath } from "next/cache";
import Link from "next/link";
import { notFound, redirect } from "next/navigation";
import { getNotmidAuthStatus } from "../../../../lib/notmidAuth";
import { createNotmidWebApiClient } from "../../../../lib/notmidRuntime";
import { runNotmidAuthenticatedApiAction } from "../../../../lib/notmidServerActions";

export const dynamic = "force-dynamic";

type ChatPageProps = {
  params: Promise<{
    threadId: string;
  }>;
  searchParams?: Promise<Record<string, string | string[] | undefined>>;
};

export default async function ChatPage({ params, searchParams }: ChatPageProps) {
  const { threadId } = await params;
  const api = createNotmidWebApiClient();
  const [auth, detail] = await Promise.all([
    getNotmidAuthStatus(),
    api.getThreadDetail(threadId).catch(() => getNotmidThreadDetail(threadId)),
  ]);

  if (!detail) {
    notFound();
  }

  const { thread } = detail;
  const chatAccess = thread.chatAccess;
  const chatLocked = chatAccess ? !chatAccess.canSendMessage : false;
  const status = chatStatusForQuery(await searchParams);

  return (
    <main className="chat-shell">
      <Link className="detail-back" href={notmidRoutes.inbox}>
        Inbox
      </Link>

      <section className="chat-workspace">
        <div className="chat-heading-panel">
          <p>{thread.participantHandles.join(" / ")}</p>
          <h1>{thread.title}</h1>
          <span>{thread.unreadCount > 0 ? `${thread.unreadCount} unread` : "caught up"}</span>
        </div>

        <aside className="chat-context-card">
          <p>Attached context</p>
          <strong>{detail.attachedClip?.title ?? "No clip attached"}</strong>
          <span>{detail.attachedPlace?.name ?? "No place attached"}</span>
          {chatAccess ? <small>{chatAccess.reasonLabel}</small> : null}
        </aside>

        <div className="chat-message-stack">
          {detail.messages.map((message) => (
            <article className={message.mine ? "message-bubble mine" : "message-bubble"} key={message.id}>
              <span>{message.senderHandle}</span>
              <p>{message.body}</p>
              {message.attachment ? <small>{attachmentLabel(message.attachment)}</small> : null}
            </article>
          ))}
        </div>

        <div className="chat-composer-panel">
          {status ? (
            <p className="chat-status" role="status">
              {status}
            </p>
          ) : (
            <span>Ask for timing, route, or another receipt</span>
          )}
          {auth.authenticated ? (
            <>
              {chatAccess?.canAcceptInvite || chatAccess?.canRejectInvite ? (
                <div className="chat-composer-form">
                  {chatAccess.canAcceptInvite ? (
                    <form action={acceptThreadInviteFromWeb}>
                      <input name="threadId" type="hidden" value={threadId} />
                      <button type="submit">Accept</button>
                    </form>
                  ) : null}
                  {chatAccess.canRejectInvite ? (
                    <form action={rejectThreadInviteFromWeb}>
                      <input name="threadId" type="hidden" value={threadId} />
                      <button type="submit">Reject</button>
                    </form>
                  ) : null}
                </div>
              ) : null}
              <form action={sendThreadMessageFromWeb} className="chat-composer-form">
                <input name="threadId" type="hidden" value={threadId} />
                <textarea
                  aria-label="Message"
                  disabled={chatLocked}
                  name="body"
                  placeholder="Message"
                  rows={2}
                />
                <button disabled={chatLocked} type="submit">
                  Send
                </button>
              </form>
            </>
          ) : (
            <Link href={notmidRoutes.login(notmidRoutes.chat(threadId))}>Continue</Link>
          )}
        </div>
      </section>
    </main>
  );
}

async function sendThreadMessageFromWeb(formData: FormData) {
  "use server";

  const threadId = readFormString(formData, "threadId");
  const body = readFormString(formData, "body");

  if (!threadId) {
    redirect(notmidRoutes.inbox);
  }

  if (!body) {
    redirect(`${notmidRoutes.chat(threadId)}?message=empty`);
  }

  try {
    await runNotmidAuthenticatedApiAction(notmidRoutes.chat(threadId), (accessToken) =>
      createNotmidWebApiClient().sendThreadMessage(threadId, { body }, accessToken),
    );
  } catch (error) {
    if (isNotmidApiRequestError(error, 403)) {
      redirect(`${notmidRoutes.chat(threadId)}?message=invite`);
    }

    if (isNotmidApiRequestError(error, 400) || isNotmidApiRequestError(error, 404)) {
      redirect(`${notmidRoutes.chat(threadId)}?message=failed`);
    }

    throw error;
  }

  revalidatePath(notmidRoutes.chat(threadId));
  revalidatePath(notmidRoutes.inboxChat(threadId));
  revalidatePath(notmidRoutes.inbox);
  redirect(`${notmidRoutes.chat(threadId)}?sent=1`);
}

async function acceptThreadInviteFromWeb(formData: FormData) {
  "use server";

  await respondThreadInviteFromWeb(formData, "accept");
}

async function rejectThreadInviteFromWeb(formData: FormData) {
  "use server";

  await respondThreadInviteFromWeb(formData, "reject");
}

async function respondThreadInviteFromWeb(formData: FormData, decision: "accept" | "reject") {
  const threadId = readFormString(formData, "threadId");

  if (!threadId) {
    redirect(notmidRoutes.inbox);
  }

  try {
    await runNotmidAuthenticatedApiAction(notmidRoutes.chat(threadId), (accessToken) => {
      const client = createNotmidWebApiClient();
      return decision === "accept"
        ? client.acceptThreadInvite(threadId, accessToken)
        : client.rejectThreadInvite(threadId, accessToken);
    });
  } catch (error) {
    if (isNotmidApiRequestError(error, 400) || isNotmidApiRequestError(error, 404)) {
      redirect(`${notmidRoutes.chat(threadId)}?message=failed`);
    }

    throw error;
  }

  revalidatePath(notmidRoutes.chat(threadId));
  revalidatePath(notmidRoutes.inboxChat(threadId));
  revalidatePath(notmidRoutes.inbox);
  redirect(`${notmidRoutes.chat(threadId)}?invite=${decision}`);
}

function attachmentLabel(attachment: NotmidMessageAttachment) {
  if (attachment.type === "clip") {
    return `clip: ${attachment.clipId}`;
  }
  if (attachment.type === "place") {
    return `place: ${attachment.placeId}`;
  }
  return `route: ${attachment.title}`;
}

function chatStatusForQuery(query: Record<string, string | string[] | undefined> | undefined) {
  if (singleQueryValue(query?.sent) === "1") {
    return "Message sent.";
  }

  if (singleQueryValue(query?.started) === "1") {
    return "Chat started.";
  }

  const messageStatus = singleQueryValue(query?.message);

  if (messageStatus === "empty") {
    return "Message body is required.";
  }

  if (messageStatus === "failed") {
    return "Message could not be sent.";
  }

  if (messageStatus === "invite") {
    return "Accept the chat request before sending a message.";
  }

  const inviteStatus = singleQueryValue(query?.invite);

  if (inviteStatus === "accept") {
    return "Chat request accepted.";
  }

  if (inviteStatus === "reject") {
    return "Chat request rejected.";
  }

  return undefined;
}

function readFormString(formData: FormData, key: string): string {
  const value = formData.get(key);
  return typeof value === "string" ? value.trim() : "";
}

function singleQueryValue(value: string | string[] | undefined): string | undefined {
  return Array.isArray(value) ? value[0] : value;
}
