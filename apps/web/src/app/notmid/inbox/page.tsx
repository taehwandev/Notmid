import { createNotmidApiClient } from "@notmid/api-client";
import { notmidFixtureInbox, notmidRoutes } from "@notmid/contracts";
import Link from "next/link";

export default async function InboxPage() {
  const api = createNotmidApiClient({
    baseUrl: process.env.NOTMID_API_BASE_URL,
    fetcher: noStoreFetch,
  });
  const inbox = await api.getInbox().catch(() => notmidFixtureInbox);

  return (
    <main className="simple-shell">
      <Link className="detail-back" href={notmidRoutes.home}>
        notmid
      </Link>
      <section className="simple-panel">
        <p>Inbox</p>
        <h1>pull up plans</h1>
        <div className="thread-list embedded">
          {inbox.threads.map((thread) => (
            <Link className="thread-row" href={notmidRoutes.chat(thread.id)} key={thread.id}>
              <span>{thread.title}</span>
              <p>{thread.preview}</p>
              <small>{thread.updatedAtLabel}</small>
            </Link>
          ))}
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
