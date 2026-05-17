import { createNotmidApiClient } from "@notmid/api-client";
import { findNotmidPlace, notmidRoutes } from "@notmid/contracts";
import Link from "next/link";
import { notFound } from "next/navigation";

type PlacePageProps = {
  params: Promise<{
    placeId: string;
  }>;
};

export default async function PlacePage({ params }: PlacePageProps) {
  const { placeId } = await params;
  const api = createNotmidApiClient({
    baseUrl: process.env.NOTMID_API_BASE_URL,
    fetcher: noStoreFetch,
  });

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

const noStoreFetch: typeof fetch = (input, init) =>
  fetch(input, {
    ...init,
    cache: "no-store",
  });
