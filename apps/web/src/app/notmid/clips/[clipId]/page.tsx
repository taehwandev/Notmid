import { createNotmidApiClient } from "@notmid/api-client";
import { findNotmidClip, findNotmidPlace, notmidRoutes } from "@notmid/contracts";
import Link from "next/link";
import { notFound } from "next/navigation";

type ClipPageProps = {
  params: Promise<{
    clipId: string;
  }>;
};

export default async function ClipPage({ params }: ClipPageProps) {
  const { clipId } = await params;
  const api = createNotmidApiClient({
    baseUrl: process.env.NOTMID_API_BASE_URL,
    fetcher: noStoreFetch,
  });

  const clip = await api.getClip(clipId).catch(() => findNotmidClip(clipId));

  if (!clip) {
    notFound();
  }

  const place = findNotmidPlace(clip.placeId);

  return (
    <main className="detail-shell">
      <Link className="detail-back" href={notmidRoutes.home}>
        notmid
      </Link>
      <section
        className="detail-visual"
        style={{
          backgroundImage: `linear-gradient(180deg, rgba(11, 12, 10, 0.08), rgba(11, 12, 10, 0.78)), url(${clip.coverImageUrl})`,
        }}
      >
        <div className="detail-copy">
          <p>{clip.creatorHandle}</p>
          <h1>{clip.title}</h1>
          <span>{place?.name ?? "place pending"}</span>
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
