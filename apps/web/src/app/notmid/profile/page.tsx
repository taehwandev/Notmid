import { notmidFixtureFeed, notmidRoutes } from "@notmid/contracts";
import Link from "next/link";
import { getNotmidAuthStatus } from "../../../lib/notmidAuth";

export const dynamic = "force-dynamic";

export default async function ProfilePage() {
  const auth = await getNotmidAuthStatus();
  const clips = notmidFixtureFeed.clips.slice(0, 2);
  const places = notmidFixtureFeed.places.slice(0, 2);
  const user = auth.user;

  return (
    <main className="profile-shell">
      <Link className="detail-back" href={notmidRoutes.home}>
        notmid
      </Link>

      <section className="profile-workspace">
        <div className="profile-hero-panel">
          <div className="profile-avatar">{user?.handle.slice(0, 1).toUpperCase() ?? "N"}</div>
          <div className="profile-copy">
            <p>{auth.authenticated ? `@${user?.handle}` : "signed out"}</p>
            <h1>{auth.authenticated ? (user?.displayName ?? "Local You") : "profile starts after sign in"}</h1>
            <span>
              {auth.authenticated
                ? (user?.homeNeighborhood ?? "Local mode")
                : "Browse works now. Capture, saves, chat, and edits need auth."}
            </span>
          </div>
          <Link
            className="profile-action"
            href={auth.authenticated ? notmidRoutes.profileSettings : notmidRoutes.login(notmidRoutes.profile)}
          >
            {auth.authenticated ? "Settings" : "Continue"}
          </Link>
        </div>

        <div className="profile-stat-row">
          <span>
            <strong>{clips.length}</strong>
            clips
          </span>
          <span>
            <strong>{places.length}</strong>
            saved
          </span>
          <span>
            <strong>2</strong>
            routes
          </span>
          <span>
            <strong>{auth.mode}</strong>
            auth
          </span>
        </div>

        <nav className="profile-tabs" aria-label="Profile sections">
          <span aria-current="page">Clips</span>
          <span>Saved</span>
          <span>Places</span>
          <span>Routes</span>
        </nav>

        <section className="profile-grid" aria-label="Profile receipts and saved places">
          {clips.map((clip) => (
            <Link className="profile-card receipt-card" href={notmidRoutes.clip(clip.id)} key={clip.id}>
              <small>{clip.capturedAtLabel}</small>
              <strong>{clip.title}</strong>
              <p>{clip.caption}</p>
            </Link>
          ))}
          {places.map((place) => (
            <Link className="profile-card place-card" href={notmidRoutes.place(place.id)} key={place.id}>
              <small>{place.neighborhood}</small>
              <strong>{place.name}</strong>
              <p>
                {place.receiptCount} receipts · {place.openNow ? "open now" : "closed"}
              </p>
            </Link>
          ))}
        </section>

        <section className="profile-control-panel">
          <div>
            <p>Account controls</p>
            <h2>Privacy, provider, and local config live in settings.</h2>
          </div>
          <Link href={auth.authenticated ? notmidRoutes.profileSettings : notmidRoutes.login(notmidRoutes.profileSettings)}>
            Open settings
          </Link>
        </section>
      </section>
    </main>
  );
}
