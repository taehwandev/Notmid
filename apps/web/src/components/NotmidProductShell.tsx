import type {
  NotmidAuthStatusResponse,
  NotmidFeedResponse,
  NotmidInboxResponse,
  NotmidMapResponse,
} from "@notmid/contracts";
import { notmidRoutes } from "@notmid/contracts";
import Link from "next/link";
import { saveClipFromWeb } from "../lib/notmidClipActions";
import { MapBoard } from "./MapBoard";

type NotmidProductShellProps = {
  feed: NotmidFeedResponse;
  map: NotmidMapResponse;
  inbox: NotmidInboxResponse;
  auth: NotmidAuthStatusResponse;
  saveStatus?: string;
};

export function NotmidProductShell({ feed, map, inbox, auth, saveStatus }: NotmidProductShellProps) {
  const activeClip = feed.clips[0];
  const activePlace = feed.places.find((place) => place.id === activeClip.placeId) ?? feed.places[0];
  const captureHref = auth.authenticated ? notmidRoutes.capture : notmidRoutes.login(notmidRoutes.capture);
  const inboxHref = auth.authenticated ? notmidRoutes.inbox : notmidRoutes.login(notmidRoutes.inbox);
  const profileHref = auth.authenticated ? notmidRoutes.profile : notmidRoutes.login(notmidRoutes.profile);

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
          <Link href={captureHref}>Capture</Link>
          <Link href={inboxHref}>Inbox</Link>
          <Link href={profileHref}>Profile</Link>
        </nav>
        <div className="rail-status">
          <span>{auth.authenticated ? `@${auth.user?.handle}` : "signed out"}</span>
          <strong>{feed.clips.length} clips nearby</strong>
          <Link className="rail-auth-link" href={auth.authenticated ? notmidRoutes.profile : notmidRoutes.login()}>
            {auth.authenticated ? "View profile" : "Sign in"}
          </Link>
        </div>
      </aside>

      <section
        className="clip-stage"
        style={{
          backgroundImage: `linear-gradient(180deg, rgba(10, 11, 9, 0.12), rgba(10, 11, 9, 0.84)), url(${activeClip.coverImageUrl})`,
        }}
      >
        <div className="clip-actions" aria-label="clip actions">
          <span>
            <strong>{activeClip.metrics.likes}</strong>
            <small>Likes</small>
          </span>
          {auth.authenticated ? (
            <form action={saveClipFromWeb}>
              <input name="clipId" type="hidden" value={activeClip.id} />
              <input name="returnTo" type="hidden" value={notmidRoutes.home} />
              <button className="clip-action-button" type="submit">
                <strong>{activeClip.metrics.saves}</strong>
                <small>Save</small>
              </button>
            </form>
          ) : (
            <Link className="clip-action-button" href={notmidRoutes.login(notmidRoutes.home)}>
              <strong>{activeClip.metrics.saves}</strong>
              <small>Save</small>
            </Link>
          )}
          <span>
            <strong>{activeClip.metrics.comments}</strong>
            <small>Chat</small>
          </span>
        </div>

        <div className="clip-copy">
          <Link href={notmidRoutes.clip(activeClip.id)}>{activeClip.creatorHandle}</Link>
          <h1>{activeClip.title}</h1>
          {saveStatus ? (
            <p className="clip-save-status" role="status">
              {saveStatus}
            </p>
          ) : null}
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
