import { isNotmidApiRequestError } from "@notmid/api-client";
import { findNotmidClip, findNotmidPlace, notmidRoutes } from "@notmid/contracts";
import { revalidatePath } from "next/cache";
import Link from "next/link";
import { notFound, redirect } from "next/navigation";
import { getNotmidAuthStatus } from "../../../../lib/notmidAuth";
import { saveClipFromWeb } from "../../../../lib/notmidClipActions";
import { createNotmidWebApiClient } from "../../../../lib/notmidRuntime";
import { runNotmidAuthenticatedApiAction } from "../../../../lib/notmidServerActions";

export const dynamic = "force-dynamic";

type ClipPageProps = {
  params: Promise<{
    clipId: string;
  }>;
  searchParams?: Promise<Record<string, string | string[] | undefined>>;
};

export default async function ClipPage({ params, searchParams }: ClipPageProps) {
  const { clipId } = await params;
  const api = createNotmidWebApiClient();

  const [auth, clip] = await Promise.all([
    getNotmidAuthStatus(),
    api.getClip(clipId).catch(() => findNotmidClip(clipId)),
  ]);

  if (!clip) {
    notFound();
  }

  const place = findNotmidPlace(clip.placeId);
  const saveStatus = saveStatusForQuery(await searchParams);

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
          {saveStatus ? (
            <p className="detail-status" role="status">
              {saveStatus}
            </p>
          ) : null}
          <div className="detail-actions">
            {auth.authenticated ? (
              <form action={saveClipFromWeb}>
                <input name="clipId" type="hidden" value={clip.id} />
                <input name="returnTo" type="hidden" value={notmidRoutes.clip(clip.id)} />
                <button type="submit">Save {clip.metrics.saves}</button>
              </form>
            ) : (
              <Link href={notmidRoutes.login(notmidRoutes.clip(clip.id))}>
                Sign in to save {clip.metrics.saves}
              </Link>
            )}
            {place ? <Link href={notmidRoutes.place(place.id)}>{place.neighborhood}</Link> : null}
            {auth.authenticated ? (
              <form action={startThreadFromClipWeb}>
                <input name="clipId" type="hidden" value={clip.id} />
                <input name="creatorHandle" type="hidden" value={clip.creatorHandle} />
                <input name="clipTitle" type="hidden" value={clip.title} />
                <button type="submit">Chat</button>
              </form>
            ) : (
              <Link href={notmidRoutes.login(notmidRoutes.clip(clip.id))}>Sign in to chat</Link>
            )}
          </div>
        </div>
      </section>
    </main>
  );
}

async function startThreadFromClipWeb(formData: FormData) {
  "use server";

  const clipId = readFormString(formData, "clipId");
  const creatorHandle = readFormString(formData, "creatorHandle");
  const clipTitle = readFormString(formData, "clipTitle");
  const returnTo = clipId ? notmidRoutes.clip(clipId) : notmidRoutes.inbox;

  if (!clipId || !creatorHandle) {
    redirect(notmidRoutes.inbox);
  }

  let threadId: string;

  try {
    const response = await runNotmidAuthenticatedApiAction(returnTo, (accessToken) =>
      createNotmidWebApiClient().startThread(
        {
          participantHandle: creatorHandle,
          body: clipTitle ? `Can we chat about ${clipTitle}?` : "Can we chat about this receipt?",
          attachedClipId: clipId,
        },
        accessToken,
      ),
    );
    threadId = response.thread.id;
  } catch (error) {
    if (isNotmidApiRequestError(error, 400) || isNotmidApiRequestError(error, 404)) {
      redirect(`${returnTo}?chat=failed`);
    }

    throw error;
  }

  revalidatePath(notmidRoutes.inbox);
  revalidatePath(notmidRoutes.chat(threadId));
  redirect(`${notmidRoutes.chat(threadId)}?started=1`);
}

function saveStatusForQuery(query: Record<string, string | string[] | undefined> | undefined) {
  if (singleQueryValue(query?.saved) === "1") {
    return "Clip saved.";
  }

  if (singleQueryValue(query?.chat) === "failed") {
    return "Chat could not be started.";
  }

  if (singleQueryValue(query?.save) === "missing") {
    return "Clip could not be saved.";
  }

  return undefined;
}

function readFormString(formData: FormData, key: string): string {
  const value = formData.get(key);
  return typeof value === "string" ? value.trim() : "";
}

function singleQueryValue(value: string | string[] | undefined): string | undefined {
  return Array.isArray(value) ? value[0] : value;
}
