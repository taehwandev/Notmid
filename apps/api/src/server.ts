import { serve } from "@hono/node-server";
import { Hono } from "hono";
import { cors } from "hono/cors";
import {
  findNotmidClip,
  findNotmidPlace,
  findNotmidThread,
  notmidFixtureFeed,
  notmidFixtureInbox,
  notmidFixtureMap,
  resolveNotmidPathStack,
} from "@notmid/contracts";

const app = new Hono();

app.use(
  "*",
  cors({
    origin: process.env.NOTMID_WEB_ORIGIN ?? "http://localhost:3000",
    allowMethods: ["GET", "POST", "OPTIONS"],
    allowHeaders: ["content-type", "authorization"],
  }),
);

app.get("/health", (context) =>
  context.json({
    ok: true,
    service: "notmid-api",
    mode: process.env.NOTMID_AUTH_MODE ?? "fake",
  }),
);

app.get("/v1/feed", (context) => context.json(notmidFixtureFeed));

app.get("/v1/map", (context) => context.json(notmidFixtureMap));

app.get("/v1/clips/:clipId", (context) => {
  const clip = findNotmidClip(context.req.param("clipId"));

  if (!clip) {
    return context.json({ error: { code: "clip_not_found", message: "Clip not found." } }, 404);
  }

  return context.json(clip);
});

app.get("/v1/places/:placeId", (context) => {
  const place = findNotmidPlace(context.req.param("placeId"));

  if (!place) {
    return context.json({ error: { code: "place_not_found", message: "Place not found." } }, 404);
  }

  return context.json(place);
});

app.get("/v1/inbox/threads", (context) => context.json(notmidFixtureInbox));

app.get("/v1/inbox/threads/:threadId", (context) => {
  const thread = findNotmidThread(context.req.param("threadId"));

  if (!thread) {
    return context.json({ error: { code: "thread_not_found", message: "Thread not found." } }, 404);
  }

  return context.json(thread);
});

app.get("/v1/deeplinks/resolve", (context) => {
  const url = context.req.query("url") ?? "/notmid";
  return context.json(resolveNotmidPathStack(url));
});

const port = Number.parseInt(process.env.NOTMID_API_PORT ?? "8787", 10);

serve(
  {
    fetch: app.fetch,
    port,
  },
  (info) => {
    console.log(`notmid API listening on http://localhost:${info.port}`);
  },
);
