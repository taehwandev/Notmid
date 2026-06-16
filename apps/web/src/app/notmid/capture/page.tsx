import { isNotmidApiRequestError } from "@notmid/api-client";
import {
  notmidFixtureCaptureDraft,
  notmidRoutes,
  type NotmidCapturePublishRequest,
  type NotmidCaptureVisibility,
} from "@notmid/contracts";
import { revalidatePath } from "next/cache";
import Link from "next/link";
import { redirect } from "next/navigation";
import { getNotmidAuthStatus } from "../../../lib/notmidAuth";
import { createNotmidWebApiClient } from "../../../lib/notmidRuntime";
import { runNotmidAuthenticatedApiAction } from "../../../lib/notmidServerActions";

export const dynamic = "force-dynamic";

type CapturePageProps = {
  searchParams?: Promise<Record<string, string | string[] | undefined>>;
};

export default async function CapturePage({ searchParams }: CapturePageProps) {
  const auth = await getNotmidAuthStatus();
  const api = createNotmidWebApiClient();
  const draft = await api.getCaptureDraft().catch(() => notmidFixtureCaptureDraft);
  const attachedPlace = draft.candidatePlaces.find((place) => place.id === draft.draft.placeId);
  const status = captureStatusForQuery(await searchParams);

  return (
    <main className="capture-shell">
      <Link className="detail-back" href={notmidRoutes.home}>
        notmid
      </Link>

      <section className="capture-workspace">
        <div className="capture-media-panel">
          <span>{draft.draft.mediaState}</span>
          <h1>{auth.authenticated ? "show the receipt" : "sign in to publish receipts"}</h1>
          <p>No real media leaves local fake mode. The publish path stays server-gated.</p>
        </div>

        <form action={publishCaptureFromWeb} className="capture-composer-panel">
          <input name="draftId" type="hidden" value={draft.draft.id} />
          <input name="placeId" type="hidden" value={draft.draft.placeId ?? ""} />
          <input name="visibility" type="hidden" value={draft.draft.visibility} />
          {draft.draft.moodTags.map((tag) => (
            <input key={tag} name="moodTags" type="hidden" value={tag} />
          ))}
          <p>{auth.authenticated ? `@${auth.user?.handle}` : "auth required"}</p>
          {status ? (
            <div className="capture-status" role="status">
              {status}
            </div>
          ) : null}
          <label className="capture-field">
            <span>Caption</span>
            <textarea
              defaultValue={draft.draft.caption}
              name="caption"
              readOnly={!auth.authenticated}
              rows={4}
            />
          </label>

          <div className="capture-tags" aria-label="Mood tags">
            {draft.draft.moodTags.map((tag) => (
              <span key={tag}>{tag}</span>
            ))}
          </div>

          <div className="capture-place-row">
            <span>{attachedPlace?.name ?? "Attach place"}</span>
            <strong>{attachedPlace?.neighborhood ?? "required"}</strong>
            <small>{attachedPlace ? `${attachedPlace.receiptCount} receipts` : "no place attached"}</small>
          </div>

          <div className="capture-actions">
            {auth.authenticated ? (
              <button type="submit">Publish receipt</button>
            ) : (
              <Link href={notmidRoutes.login(notmidRoutes.capture)}>Continue</Link>
            )}
          </div>
        </form>
      </section>
    </main>
  );
}

async function publishCaptureFromWeb(formData: FormData) {
  "use server";

  const request: NotmidCapturePublishRequest = {
    draftId: readFormString(formData, "draftId"),
    caption: readFormString(formData, "caption"),
    placeId: readFormString(formData, "placeId"),
    moodTags: readMoodTags(formData),
    visibility: readVisibility(formData.get("visibility")),
  };

  let publishResult;

  try {
    publishResult = await runNotmidAuthenticatedApiAction(notmidRoutes.capture, (accessToken) =>
      createNotmidWebApiClient().publishCapture(request, accessToken),
    );
  } catch (error) {
    if (isNotmidApiRequestError(error, 400) || isNotmidApiRequestError(error, 404)) {
      redirect(`${notmidRoutes.capture}?publish=invalid`);
    }

    throw error;
  }

  revalidatePath(notmidRoutes.capture);
  revalidatePath(notmidRoutes.feed);
  redirect(`${notmidRoutes.capture}?published=${encodeURIComponent(publishResult.moderationStatus)}`);
}

function captureStatusForQuery(query: Record<string, string | string[] | undefined> | undefined) {
  if (singleQueryValue(query?.published) === "queued") {
    return "Receipt queued for moderation.";
  }

  if (singleQueryValue(query?.publish) === "invalid") {
    return "Receipt could not be published.";
  }

  return undefined;
}

function readFormString(formData: FormData, key: string): string {
  const value = formData.get(key);
  return typeof value === "string" ? value.trim() : "";
}

function readMoodTags(formData: FormData): string[] {
  return formData
    .getAll("moodTags")
    .filter((value): value is string => typeof value === "string")
    .map((value) => value.trim())
    .filter(Boolean);
}

function readVisibility(value: FormDataEntryValue | null): NotmidCaptureVisibility {
  return value === "friends" || value === "private" ? value : "public";
}

function singleQueryValue(value: string | string[] | undefined): string | undefined {
  return Array.isArray(value) ? value[0] : value;
}
