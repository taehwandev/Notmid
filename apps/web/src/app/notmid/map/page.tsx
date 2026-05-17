import { createNotmidApiClient } from "@notmid/api-client";
import { notmidFixtureMap } from "@notmid/contracts";
import { MapBoard } from "../../../components/MapBoard";

export default async function MapPage() {
  const api = createNotmidApiClient({
    baseUrl: process.env.NOTMID_API_BASE_URL,
    fetcher: noStoreFetch,
  });
  const map = await api.getMap().catch(() => notmidFixtureMap);

  return (
    <main className="map-page-shell">
      <MapBoard places={map.places} />
    </main>
  );
}

const noStoreFetch: typeof fetch = (input, init) =>
  fetch(input, {
    ...init,
    cache: "no-store",
  });
