import type { NotmidFeedResponse, NotmidInboxResponse, NotmidMapResponse } from "@notmid/contracts";
import { notmidRoutes } from "@notmid/contracts";
import Link from "next/link";
import { MapBoard } from "./MapBoard";

type NotmidProductShellProps = {
  feed: NotmidFeedResponse;
  map: NotmidMapResponse;
  inbox: NotmidInboxResponse;
};

export function NotmidProductShell({ feed, map, inbox }: NotmidProductShellProps) {
  const activeClip = feed.clips[0];
  const activePlace = feed.places.find((place) => place.id === activeClip.placeId) ?? feed.places[0];

  return (
    <main className="product-shell">
      <aside className="side-rail">
        <Link className="brand-lockup" href={notmidRoutes.home}>
          <span>notmid</span>
          <strong>receipts live</strong>
        </Link>
        <nav className="rail-nav" aria-label="notmid navigation">
          <Link href={notmidRoutes.feed}>Feed</Link>
          <Link href={notmidRoutes.map}>Map</Link>
          <Link href={notmidRoutes.capture}>Capture</Link>
          <Link href={notmidRoutes.inbox}>Inbox</Link>
          <Link href={notmidRoutes.profile}>Profile</Link>
        </nav>
        <div className="rail-status">
          <span>{feed.source}</span>
          <strong>{feed.clips.length} clips nearby</strong>
        </div>
      </aside>

      <section
        className="clip-stage"
        style={{
          backgroundImage: `linear-gradient(180deg, rgba(10, 11, 9, 0.12), rgba(10, 11, 9, 0.84)), url(${activeClip.coverImageUrl})`,
        }}
      >
        <div className="clip-actions" aria-label="clip actions">
          <span>{activeClip.metrics.likes}</span>
          <span>{activeClip.metrics.saves}</span>
          <span>{activeClip.metrics.comments}</span>
        </div>

        <div className="clip-copy">
          <Link href={notmidRoutes.clip(activeClip.id)}>{activeClip.creatorHandle}</Link>
          <h1>{activeClip.title}</h1>
          <p>{activeClip.caption}</p>
          <div className="tag-row">
            {activeClip.moodTags.map((tag) => (
              <span key={tag}>{tag}</span>
            ))}
          </div>
        </div>

        <Link className="place-strip" href={notmidRoutes.place(activePlace.id)}>
          <span>{activePlace.name}</span>
          <strong>{activePlace.neighborhood}</strong>
          <small>{activeClip.metrics.distanceLabel}</small>
        </Link>
      </section>

      <section className="right-panel">
        <MapBoard places={map.places} compact />

        <div className="thread-list">
          <div className="panel-heading">
            <span>Inbox</span>
            <strong>{inbox.threads.reduce((count, thread) => count + thread.unreadCount, 0)}</strong>
          </div>
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
