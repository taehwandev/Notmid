import { notmidRoutes } from "@notmid/contracts";
import Link from "next/link";

export default function ProfilePage() {
  return (
    <main className="simple-shell">
      <Link className="detail-back" href={notmidRoutes.home}>
        notmid
      </Link>
      <section className="simple-panel profile-panel">
        <p>@you</p>
        <h1>receipts saved</h1>
        <div className="profile-stats">
          <span>12 clips</span>
          <span>31 saves</span>
          <Link href={notmidRoutes.profileSettings}>settings</Link>
        </div>
      </section>
    </main>
  );
}
