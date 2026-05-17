import { createNotmidApiClient } from "@notmid/api-client";
import { findNotmidThread, notmidRoutes } from "@notmid/contracts";
import Link from "next/link";
import { notFound } from "next/navigation";

type ChatPageProps = {
  params: Promise<{
    threadId: string;
  }>;
};

export default async function ChatPage({ params }: ChatPageProps) {
  const { threadId } = await params;
  const api = createNotmidApiClient({
    baseUrl: process.env.NOTMID_API_BASE_URL,
    fetcher: noStoreFetch,
  });
  const thread = await api.getThread(threadId).catch(() => findNotmidThread(threadId));

  if (!thread) {
    notFound();
  }

  return (
    <main className="simple-shell">
      <Link className="detail-back" href={notmidRoutes.inbox}>
        Inbox
      </Link>
      <section className="simple-panel chat-panel">
        <p>{thread.participantHandles.join(" / ")}</p>
        <h1>{thread.title}</h1>
        <div className="message-stack">
          <span>{thread.preview}</span>
          <strong>{thread.unreadCount > 0 ? `${thread.unreadCount} unread` : "caught up"}</strong>
        </div>
      </section>
    </main>
  );
}

const noStoreFetch: typeof fetch = (input, init) =>
  fetch(input, {
    ...init,
    cache: "no-store",
  });
