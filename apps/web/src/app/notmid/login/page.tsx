import { createNotmidApiClient } from "@notmid/api-client";
import { notmidFakeAuthSession, notmidRoutes, type NotmidAuthProvider } from "@notmid/contracts";
import { cookies } from "next/headers";
import Link from "next/link";
import { redirect } from "next/navigation";
import {
  normalizeNotmidReturnTo,
  noStoreFetch,
  notmidAuthCookieName,
} from "../../../lib/notmidAuth";

type LoginPageProps = {
  searchParams?: Promise<Record<string, string | string[] | undefined>>;
};

export default async function LoginPage({ searchParams }: LoginPageProps) {
  const params = await searchParams;
  const rawNext = Array.isArray(params?.next) ? params?.next[0] : params?.next;
  const nextPath = normalizeNotmidReturnTo(rawNext) ?? notmidRoutes.capture;

  return (
    <main className="login-web-shell">
      <section className="login-web-hero" aria-label="notmid brand">
        <div className="login-web-glow" aria-hidden="true" />
        <Link className="login-web-logo" href={notmidRoutes.home}>
          notmid
        </Link>

        <div className="login-web-hero-copy">
          <div className="login-web-verified">
            <span aria-hidden="true">V</span>
            <strong>GENUINE REVIEWS ONLY</strong>
          </div>
          <h1>
            not mid.
            <br />
            <span>show receipts.</span>
          </h1>
          <p>
            The local discovery platform where proof is the only currency. Stop guessing and start
            seeing what&apos;s actually worth your time.
          </p>
        </div>

        <div className="login-web-receipts" aria-hidden="true">
          <article className="login-web-receipt login-web-receipt-a">
            <div className="login-web-receipt-image">
              <img
                alt=""
                src="https://lh3.googleusercontent.com/aida-public/AB6AXuABe_VFSduAyhiRAaaRGbFkOGrE9n0YiPVGOdHkKgCvCktZY_1enXR5N-KfQK9KOh-2uWwBi1Yq9oZoiIAcGySbsIDJGM0yLKQx6MOV2YZGu4cj5vzya7SVviVHU0tD_owGyTXWOZd2K1F0WNIUHv2aG4b3yPJzHVM9uW0kt1JppfWpIgjnhVkONShuSzc9bRo9XqYEoVOqGj2JkUm6OLY3A7W9PqMgJQdUBUMt28wjo2BKsLlD2XJOU4R61EEri7iHnvukKJPg7wM"
              />
            </div>
            <strong>Supernova Coffee</strong>
            <span>4.9 - 2h ago</span>
          </article>
          <article className="login-web-receipt login-web-receipt-b">
            <div className="login-web-receipt-image">
              <img
                alt=""
                src="https://lh3.googleusercontent.com/aida-public/AB6AXuD61zVDVneZuK-xnEXzCkgWcgJ_XkLbl4ijq5RD_dtIwg5sNrBWrV5Ls3ieOR2AISPwcXNpi_YBHh8o-WOajGs6WDpcHt2qS4y7o9SwA6Dy-z1IauO4l4ANbRioij5WQNi2JYV-5Lp3ivR_DTWcwPXz-PCsRafh8MOPb9jC7n0eO5ErdPzVGNpmK5yvXjbnqKFKkteOaJedM0SYZWGLcvtDlNdKrTnzKBAzpM8czKZRpgNxzusAaaiaW3wXWjk2mnmSl-xOED-_DUk"
              />
            </div>
            <strong>The Low Key</strong>
            <span>5.0 - Just now</span>
          </article>
        </div>
      </section>

      <section className="login-web-form-panel" aria-label="notmid login">
        <div className="login-web-form-wrap">
          <Link className="login-web-mobile-logo" href={notmidRoutes.home}>
            notmid
          </Link>

          <div className="login-web-heading">
            <h2>Welcome back</h2>
            <p>Enter your credentials to access the feed.</p>
          </div>

          <div className="login-web-social-stack">
            <form action={continueWithProvider}>
              <input name="provider" type="hidden" value="google" />
              <input name="returnTo" type="hidden" value={nextPath} />
              <button className="login-web-social-button" type="submit">
                <span>G</span>
                Continue with Google
              </button>
            </form>
            <form action={continueWithProvider}>
              <input name="provider" type="hidden" value="anonymous" />
              <input name="returnTo" type="hidden" value={nextPath} />
              <button className="login-web-social-button" type="submit">
                <span>A</span>
                Continue with Apple
              </button>
            </form>
          </div>

          <div className="login-web-divider">
            <span />
            <p>or</p>
          </div>

          <form action={continueWithProvider} className="login-web-form">
            <input name="provider" type="hidden" value="fake" />
            <input name="returnTo" type="hidden" value={nextPath} />

            <label className="login-web-field" htmlFor="notmid-login-identity">
              <span>Username or Email</span>
              <input
                autoComplete="username"
                id="notmid-login-identity"
                name="identity"
                placeholder="name@example.com"
                type="text"
              />
            </label>

            <label className="login-web-field" htmlFor="notmid-login-password">
              <span>
                Password
                <a href="#forgot">Forgot?</a>
              </span>
              <input
                autoComplete="current-password"
                id="notmid-login-password"
                name="password"
                placeholder="********"
                type="password"
              />
            </label>

            <label className="login-web-remember" htmlFor="notmid-login-remember">
              <input id="notmid-login-remember" name="remember" type="checkbox" />
              <span>Keep me logged in</span>
            </label>

            <button className="login-web-submit" type="submit">
              Login to Dashboard
            </button>
          </form>

          <footer className="login-web-footer">
            <p>New here?</p>
            <form action={continueWithProvider}>
              <input name="provider" type="hidden" value="fake" />
              <input name="returnTo" type="hidden" value={nextPath} />
              <button type="submit">Join the movement</button>
            </form>
          </footer>
        </div>
      </section>
    </main>
  );
}

async function continueWithProvider(formData: FormData) {
  "use server";

  const provider = parseProvider(formData.get("provider"));
  const returnTo = normalizeNotmidReturnTo(formData.get("returnTo")) ?? notmidRoutes.capture;
  const api = createNotmidApiClient({
    baseUrl: process.env.NOTMID_API_BASE_URL,
    fetcher: noStoreFetch,
  });

  const response = await api
    .signIn({ provider, intent: provider === "google" ? "profile" : "capture", returnTo })
    .catch(() => ({
      mode: "fake" as const,
      session: {
        ...notmidFakeAuthSession,
        provider,
      },
      nextPath: returnTo,
    }));

  const cookieStore = await cookies();
  cookieStore.set(notmidAuthCookieName, response.session.accessToken, {
    httpOnly: true,
    maxAge: 60 * 60 * 24 * 7,
    path: "/notmid",
    sameSite: "lax",
    secure: process.env.NODE_ENV === "production",
  });

  redirect(response.nextPath);
}

function parseProvider(value: FormDataEntryValue | null): NotmidAuthProvider {
  return value === "anonymous" || value === "google" ? value : "fake";
}
