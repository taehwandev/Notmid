import { isNotmidApiRequestError } from "@notmid/api-client";
import { notmidRoutes } from "@notmid/contracts";
import { revalidatePath } from "next/cache";
import { cookies } from "next/headers";
import Link from "next/link";
import { redirect } from "next/navigation";
import {
  getNotmidAuthStatus,
  notmidAuthCookieName,
  notmidAuthCookiePath,
  notmidAuthRefreshCookieName,
} from "../../../../lib/notmidAuth";
import { notmidLegacyFakeAuthCookieName } from "../../../../lib/notmidAuthCookies";
import { createNotmidWebApiClient } from "../../../../lib/notmidRuntime";
import { runNotmidAuthenticatedApiAction } from "../../../../lib/notmidServerActions";

export const dynamic = "force-dynamic";

type ProfileSettingsPageProps = {
  searchParams?: Promise<Record<string, string | string[] | undefined>>;
};

export default async function ProfileSettingsPage({ searchParams }: ProfileSettingsPageProps) {
  const auth = await getNotmidAuthStatus();
  const settings = auth.authenticated
    ? await runNotmidAuthenticatedApiAction(notmidRoutes.profileSettings, (accessToken) =>
        createNotmidWebApiClient().getProfileSettings(accessToken),
      )
    : undefined;
  const user = settings?.settings.user ?? auth.user;
  const status = settingsStatusForQuery(await searchParams);

  return (
    <main className="profile-shell settings-shell">
      <Link className="detail-back" href={notmidRoutes.profile}>
        Profile
      </Link>

      <section className="profile-workspace settings-workspace">
        <div className="settings-heading-panel">
          <p>{auth.authenticated ? `@${user?.handle}` : "settings"}</p>
          <h1>{auth.authenticated ? "account settings" : "open-source mode"}</h1>
          <span>Account, privacy, auth provider, and Firebase-safe local configuration.</span>
          {status ? (
            <div className="settings-status" role="status">
              {status}
            </div>
          ) : null}
        </div>

        <div className="settings-grid">
          {auth.authenticated ? (
            <form action={updateProfileSettingsFromWeb} className="settings-card settings-profile-form">
              <p>Profile edit</p>
              <label>
                <span>Display name</span>
                <input
                  autoComplete="name"
                  defaultValue={user?.displayName ?? ""}
                  maxLength={80}
                  name="displayName"
                  required
                />
              </label>
              <label>
                <span>Neighborhood</span>
                <input
                  autoComplete="address-level2"
                  defaultValue={user?.homeNeighborhood ?? ""}
                  maxLength={80}
                  name="homeNeighborhood"
                  required
                />
              </label>
              <button className="profile-action" type="submit">
                Save profile
              </button>
            </form>
          ) : (
            <section className="settings-card">
              <p>Profile edit</p>
              <dl>
                <div>
                  <dt>Status</dt>
                  <dd>auth required</dd>
                </div>
                <div>
                  <dt>Action</dt>
                  <dd>sign in before editing</dd>
                </div>
              </dl>
            </section>
          )}

          <section className="settings-card">
            <p>Account</p>
            <dl>
              <div>
                <dt>Handle</dt>
                <dd>{user?.handle ?? "signed out"}</dd>
              </div>
              <div>
                <dt>Display name</dt>
                <dd>{user?.displayName ?? "Local browse"}</dd>
              </div>
              <div>
                <dt>Neighborhood</dt>
                <dd>{user?.homeNeighborhood ?? "not set"}</dd>
              </div>
            </dl>
          </section>

          <section className="settings-card">
            <p>Privacy</p>
            <dl>
              <div>
                <dt>Saved places</dt>
                <dd>{settings?.settings.privacy.savedPlacesVisibility ?? "private"}</dd>
              </div>
              <div>
                <dt>Chat invites</dt>
                <dd>
                  {settings?.settings.privacy.chatInvites.replace(/-/g, " ") ??
                    "shared clips and places"}
                </dd>
              </div>
              <div>
                <dt>Receipt visibility</dt>
                <dd>{settings?.settings.privacy.defaultReceiptVisibility ?? "public fake mode"}</dd>
              </div>
            </dl>
          </section>

          <section className="settings-card">
            <p>Auth</p>
            <dl>
              <div>
                <dt>Mode</dt>
                <dd>{auth.mode}</dd>
              </div>
              <div>
                <dt>Provider</dt>
                <dd>{auth.authenticated ? auth.mode : "none"}</dd>
              </div>
              <div>
                <dt>Protected</dt>
                <dd>{auth.requiredFor.join(", ")}</dd>
              </div>
            </dl>
          </section>

          <section className="settings-card">
            <p>Open source</p>
            <dl>
              <div>
                <dt>Firebase</dt>
                <dd>bring your own project</dd>
              </div>
              <div>
                <dt>Runtime config</dt>
                <dd>.env.local / local.properties</dd>
              </div>
              <div>
                <dt>Secrets</dt>
                <dd>not committed</dd>
              </div>
            </dl>
          </section>
        </div>

        {auth.authenticated ? (
          <form action={signOutFromNotmid} className="settings-session-form">
            <button className="profile-action settings-signout" type="submit">
              Sign out
            </button>
          </form>
        ) : (
          <Link className="profile-action settings-signin" href={notmidRoutes.login(notmidRoutes.profileSettings)}>
            Sign in to edit settings
          </Link>
        )}
      </section>
    </main>
  );
}

async function updateProfileSettingsFromWeb(formData: FormData) {
  "use server";

  const request = {
    displayName: readFormString(formData, "displayName"),
    homeNeighborhood: readFormString(formData, "homeNeighborhood"),
  };

  try {
    await runNotmidAuthenticatedApiAction(notmidRoutes.profileSettings, (accessToken) =>
      createNotmidWebApiClient().updateProfileSettings(request, accessToken),
    );
  } catch (error) {
    if (isNotmidApiRequestError(error, 400)) {
      redirect(`${notmidRoutes.profileSettings}?profile=invalid`);
    }

    throw error;
  }

  revalidatePath(notmidRoutes.profile);
  revalidatePath(notmidRoutes.profileSettings);
  redirect(`${notmidRoutes.profileSettings}?updated=1`);
}

async function signOutFromNotmid() {
  "use server";

  const cookieStore = await cookies();

  for (const cookieName of [
    notmidAuthCookieName,
    notmidAuthRefreshCookieName,
    notmidLegacyFakeAuthCookieName,
  ]) {
    cookieStore.set(cookieName, "", {
      httpOnly: true,
      maxAge: 0,
      path: notmidAuthCookiePath,
      sameSite: "lax",
      secure: process.env.NODE_ENV === "production",
    });
  }

  redirect(notmidRoutes.home);
}

function settingsStatusForQuery(query: Record<string, string | string[] | undefined> | undefined) {
  if (singleQueryValue(query?.updated) === "1") {
    return "Profile settings updated.";
  }

  if (singleQueryValue(query?.profile) === "invalid") {
    return "Profile settings could not be saved.";
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
