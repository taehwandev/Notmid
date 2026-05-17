import { notmidRoutes } from "@notmid/contracts";
import Link from "next/link";

export default function ProfileSettingsPage() {
  return (
    <main className="simple-shell">
      <Link className="detail-back" href={notmidRoutes.profile}>
        Profile
      </Link>
      <section className="simple-panel settings-panel">
        <p>Settings</p>
        <h1>open-source mode</h1>
        <div className="settings-list">
          <span>API fixture mode</span>
          <span>Firebase optional</span>
          <span>no committed secrets</span>
        </div>
      </section>
    </main>
  );
}
