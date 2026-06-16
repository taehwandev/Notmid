import { notmidFixtureMap } from "@notmid/contracts";
import { MapBoard } from "../../../components/MapBoard";
import { createNotmidWebApiClient } from "../../../lib/notmidRuntime";

export const dynamic = "force-dynamic";

export default async function MapPage() {
  const api = createNotmidWebApiClient();
  const map = await api.getMap().catch(() => notmidFixtureMap);

  return (
    <main className="map-page-shell">
      <MapBoard places={map.places} />
    </main>
  );
}
