import { findNotmidPlace, notmidRoutes } from "@notmid/contracts";
import Link from "next/link";
import { notFound } from "next/navigation";
import { createNotmidWebApiClient } from "../../../../lib/notmidRuntime";

export const dynamic = "force-dynamic";

type PlacePageProps = {
  params: Promise<{
    placeId: string;
  }>;
};

export default async function PlacePage({ params }: PlacePageProps) {
  const { placeId } = await params;
  const api = createNotmidWebApiClient();

  const place = await api.getPlace(placeId).catch(() => findNotmidPlace(placeId));

  if (!place) {
    notFound();
  }

  return (
    <main className="detail-shell">
      <Link className="detail-back" href={notmidRoutes.home}>
        notmid
      </Link>
      <section
        className="detail-visual"
        style={{
          backgroundImage: `linear-gradient(180deg, rgba(11, 12, 10, 0.06), rgba(11, 12, 10, 0.72)), url(${place.coverImageUrl})`,
        }}
      >
        <div className="detail-copy">
          <p>
            {place.neighborhood} / {place.category}
          </p>
          <h1>{place.name}</h1>
          <span>{place.openNow ? "open rn" : "closed rn"} / {place.receiptCount} receipts</span>
        </div>
      </section>
    </main>
  );
}
