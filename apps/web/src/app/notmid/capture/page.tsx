import { notmidRoutes } from "@notmid/contracts";
import Link from "next/link";

export default function CapturePage() {
  return (
    <main className="simple-shell">
      <Link className="detail-back" href={notmidRoutes.home}>
        notmid
      </Link>
      <section className="simple-panel capture-panel">
        <p>Capture</p>
        <h1>show the receipt</h1>
        <div className="capture-grid">
          <span>clip</span>
          <span>place</span>
          <span>caption</span>
          <span>publish</span>
        </div>
      </section>
    </main>
  );
}
