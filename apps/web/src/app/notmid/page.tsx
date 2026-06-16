import { notmidFixtureFeed, notmidFixtureInbox, notmidFixtureMap } from "@notmid/contracts";
import { NotmidProductShell } from "../../components/NotmidProductShell";
import { getNotmidAuthStatus } from "../../lib/notmidAuth";
import { createNotmidWebApiClient } from "../../lib/notmidRuntime";

export const dynamic = "force-dynamic";

type NotmidPageProps = {
  searchParams?: Promise<Record<string, string | string[] | undefined>>;
};

export default async function NotmidPage({ searchParams }: NotmidPageProps) {
  const api = createNotmidWebApiClient();

  const [feed, map, inbox, auth] = await Promise.all([
    api.getFeed().catch(() => notmidFixtureFeed),
    api.getMap().catch(() => notmidFixtureMap),
    api.getInbox().catch(() => notmidFixtureInbox),
    getNotmidAuthStatus(),
  ]);

  return (
    <NotmidProductShell
      feed={feed}
      map={map}
      inbox={inbox}
      auth={auth}
      saveStatus={saveStatusForQuery(await searchParams)}
    />
  );
}

function saveStatusForQuery(query: Record<string, string | string[] | undefined> | undefined) {
  if (singleQueryValue(query?.saved) === "1") {
    return "Clip saved.";
  }

  if (singleQueryValue(query?.save) === "missing") {
    return "Clip could not be saved.";
  }

  return undefined;
}

function singleQueryValue(value: string | string[] | undefined): string | undefined {
  return Array.isArray(value) ? value[0] : value;
}
