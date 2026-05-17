import { createNotmidApiClient } from "@notmid/api-client";
import { notmidFixtureFeed, notmidFixtureInbox, notmidFixtureMap } from "@notmid/contracts";
import { NotmidProductShell } from "../../components/NotmidProductShell";

export default async function NotmidPage() {
  const api = createNotmidApiClient({
    baseUrl: process.env.NOTMID_API_BASE_URL,
    fetcher: noStoreFetch,
  });

  const [feed, map, inbox] = await Promise.all([
    api.getFeed().catch(() => notmidFixtureFeed),
    api.getMap().catch(() => notmidFixtureMap),
    api.getInbox().catch(() => notmidFixtureInbox),
  ]);

  return <NotmidProductShell feed={feed} map={map} inbox={inbox} />;
}

const noStoreFetch: typeof fetch = (input, init) =>
  fetch(input, {
    ...init,
    cache: "no-store",
  });
