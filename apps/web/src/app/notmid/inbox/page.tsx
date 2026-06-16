import {
  findNotmidClip,
  findNotmidPlace,
  notmidFixtureInbox,
  notmidRoutes,
} from "@notmid/contracts";
import Link from "next/link";
import { createNotmidWebApiClient } from "../../../lib/notmidRuntime";

export const dynamic = "force-dynamic";

export default async function InboxPage() {
  const api = createNotmidWebApiClient();
  const inbox = await api.getInbox().catch(() => notmidFixtureInbox);
  const unreadCount = inbox.threads.reduce((count, thread) => count + thread.unreadCount, 0);

  return (
    <main className="inbox-shell">
      <Link className="detail-back" href={notmidRoutes.home}>
        notmid
      </Link>

      <section className="inbox-workspace">
        <div className="inbox-heading-panel">
          <p>Inbox</p>
          <h1>pull up plans</h1>
          <span>{unreadCount > 0 ? `${unreadCount} unread around clips and places` : "caught up"}</span>
        </div>

        <div className="inbox-thread-stack">
          {inbox.threads.map((thread) => {
            const clip = thread.attachedClipId ? findNotmidClip(thread.attachedClipId) : undefined;
            const place = thread.attachedPlaceId ? findNotmidPlace(thread.attachedPlaceId) : undefined;

            return (
              <Link className="inbox-thread-card" href={notmidRoutes.chat(thread.id)} key={thread.id}>
                <div>
                  <span>{thread.updatedAtLabel}</span>
                  <strong>{thread.title}</strong>
                  <p>{thread.preview}</p>
                </div>
                <div className="thread-attachment">
                  <small>{clip?.title ?? "no clip attached"}</small>
                  <b>{place?.name ?? "no place attached"}</b>
                  <em>{thread.participantHandles.join(" / ")}</em>
                </div>
                {thread.unreadCount > 0 ? <i>{thread.unreadCount}</i> : null}
              </Link>
            );
          })}
        </div>

        <div className="inbox-empty-panel">
          <p>Start from proof</p>
          <h2>Share a clip or place to keep chat tied to a real decision.</h2>
          <Link href={notmidRoutes.feed}>Open feed</Link>
        </div>
      </section>
    </main>
  );
}
