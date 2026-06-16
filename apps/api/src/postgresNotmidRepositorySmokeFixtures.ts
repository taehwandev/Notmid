import type { NotmidRepositoryActor } from "./notmidRepository";
import type { PostgresQueryClient, PostgresQueryResult } from "./postgresNotmidQueryTypes";
import type { ClipRow, MessageRow, PlaceRow, ThreadRow, UserRow } from "./postgresNotmidRows";

export type SmokeSqlCall = {
  sql: string;
  values: readonly unknown[];
};

type SmokeThreadBase = Omit<ThreadRow, "chat_invite_status" | "chat_relationship">;

export type SmokePostgresClientFixture = {
  actor: NotmidRepositoryActor;
  calls: SmokeSqlCall[];
  client: PostgresQueryClient;
};

const actor: NotmidRepositoryActor = {
  userId: "firebase:test-user",
  handle: "verified",
  displayName: "Verified User",
  homeNeighborhood: "Seongsu",
  avatarImageUrl: "https://example.test/avatar.png",
  roles: ["creator"],
};

const user: UserRow = {
  id: "firebase:test-user",
  handle: "verified",
  display_name: "Verified User",
  home_neighborhood: "Seongsu",
  avatar_image_url: "https://example.test/avatar.png",
  roles: ["creator"],
};

const friendUser: UserRow = {
  id: "firebase:friend-user",
  handle: "min.zip",
  display_name: "Min Zip",
  home_neighborhood: "Seongsu",
  avatar_image_url: "https://example.test/min.png",
  roles: ["creator"],
};

const nonFriendUser: UserRow = {
  id: "firebase:non-friend-user",
  handle: "receipt.han",
  display_name: "Receipt Han",
  home_neighborhood: "Euljiro",
  avatar_image_url: "https://example.test/receipt.png",
  roles: ["creator"],
};

const places: PlaceRow[] = [
  {
    id: "neon-yard",
    name: "Neon Yard",
    neighborhood: "Seongsu",
    category: "night coffee",
    address: "Seongsu-dong",
    lat: 37.5446,
    lng: 127.0557,
    open_now: true,
    score: 92,
    cover_image_url: "https://example.test/neon-yard.jpg",
    receipt_count: 2,
  },
];

const clips: ClipRow[] = [
  {
    id: "latte-line-was-worth-it",
    title: "latte line was worth it",
    caption: "Foam art and fast seats.",
    creator_handle: "min.zip",
    place_id: "neon-yard",
    mood_tags: ["live rn"],
    cover_image_url: "https://example.test/clip.jpg",
    video_object_key: null,
    like_count: 12,
    save_count: 5,
    comment_count: 2,
    created_at: "2026-05-17T00:00:00.000Z",
  },
];

export function createSmokePostgresClient(): SmokePostgresClientFixture {
  const threads: SmokeThreadBase[] = [
    {
      id: "tonight-seongsu",
      title: "tonight in seongsu?",
      preview: "Neon Yard?",
      updated_at: "2026-05-17T00:10:00.000Z",
      attached_place_id: "neon-yard",
      attached_clip_id: "latte-line-was-worth-it",
      participant_handles: ["min.zip", "verified"],
    },
    {
      id: "rain-route",
      title: "rain route",
      preview: "receipt.han wants to start a chat from the rain route",
      updated_at: "2026-05-17T00:20:00.000Z",
      attached_place_id: "neon-yard",
      attached_clip_id: "latte-line-was-worth-it",
      participant_handles: ["receipt.han", "verified"],
    },
  ];

  const chatAccessByThreadId = new Map<
    string,
    Pick<ThreadRow, "chat_invite_status" | "chat_relationship">
  >([
    [
      "tonight-seongsu",
      {
        chat_relationship: "friend",
        chat_invite_status: "accepted",
      },
    ],
    [
      "rain-route",
      {
        chat_relationship: "non-friend",
        chat_invite_status: "pending-inbound",
      },
    ],
  ]);

  const messages: MessageRow[] = [
    {
      id: "msg-1",
      thread_id: "tonight-seongsu",
      sender_handle: "min.zip",
      body: "meet after 8?",
      created_at: "2026-05-17T00:11:00.000Z",
      attachment_clip_id: "latte-line-was-worth-it",
      attachment_place_id: null,
      mine: false,
    },
  ];

  const calls: SmokeSqlCall[] = [];

  function threadRowsFor(threadId?: unknown): ThreadRow[] {
    const requestedThreadId = typeof threadId === "string" ? threadId : undefined;

    return threads
      .filter((thread) => !requestedThreadId || thread.id === requestedThreadId)
      .map((thread) => ({
        ...thread,
        ...(chatAccessByThreadId.get(thread.id) ?? {
          chat_relationship: null,
          chat_invite_status: null,
        }),
      }));
  }

  const client: PostgresQueryClient = {
    async query<Row>(
      sql: string,
      values: readonly unknown[] = [],
    ): Promise<PostgresQueryResult<Row>> {
      calls.push({ sql, values });
      const normalized = sql.replace(/\s+/g, " ").trim();

      if (normalized === "BEGIN" || normalized === "COMMIT" || normalized === "ROLLBACK") {
        return result();
      }

      if (normalized.startsWith("INSERT INTO notmid_users")) {
        return result();
      }

      if (normalized.startsWith("UPDATE notmid_users")) {
        return result([
          {
            ...user,
            display_name: stringValue(values[1]),
            home_neighborhood: stringValue(values[2]),
          },
        ]);
      }

      if (normalized.includes("FROM notmid_users") && normalized.includes("WHERE id = $1")) {
        return result(values[0] === "firebase:test-user" ? [user] : []);
      }

      if (normalized.includes("FROM notmid_users") && normalized.includes("WHERE handle = $1")) {
        if (values[0] === "min.zip") {
          return result([friendUser]);
        }

        if (values[0] === "receipt.han") {
          return result([nonFriendUser]);
        }

        return result();
      }

      if (normalized.includes("FROM notmid_user_relationships")) {
        return result(
          values[0] === "firebase:test-user" && values[1] === "firebase:friend-user"
            ? [{ status: "friend" }]
            : [],
        );
      }

      if (normalized.startsWith("INSERT INTO notmid_clips")) {
        return result([
          {
            ...clips[0],
            id: stringValue(values[0]),
            creator_handle: stringValue(values[8]),
            title: stringValue(values[3]),
            caption: stringValue(values[4]),
            place_id: stringValue(values[2]),
            mood_tags: stringArrayValue(values[6]),
            cover_image_url: nullableStringValue(values[7]),
          },
        ]);
      }

      if (normalized.startsWith("INSERT INTO notmid_chat_messages")) {
        return result([
          {
            id: stringValue(values[0]),
            thread_id: stringValue(values[1]),
            sender_handle: stringValue(values[6]),
            body: stringValue(values[3]),
            created_at: "2026-05-17T00:12:00.000Z",
            attachment_clip_id: nullableStringValue(values[4]),
            attachment_place_id: nullableStringValue(values[5]),
            mine: true,
          },
        ]);
      }

      if (normalized.startsWith("INSERT INTO notmid_chat_threads")) {
        const [threadId, placeId, clipId, title, body] = values;
        const insertedThreadId = stringValue(threadId);

        if (!threads.some((thread) => thread.id === insertedThreadId)) {
          threads.push({
            id: insertedThreadId,
            title: stringValue(title),
            preview: stringValue(body),
            updated_at: "2026-05-17T00:30:00.000Z",
            attached_place_id: nullableStringValue(placeId),
            attached_clip_id: nullableStringValue(clipId),
            participant_handles: [],
          });
        }

        return result();
      }

      if (normalized.startsWith("INSERT INTO notmid_chat_thread_participants")) {
        const [threadId, actorUserId, targetUserId] = values;
        const thread = threads.find((item) => item.id === threadId);
        if (thread) {
          thread.participant_handles = [
            actorUserId === "firebase:test-user" ? user.handle : undefined,
            targetUserId === "firebase:friend-user" ? friendUser.handle : nonFriendUser.handle,
          ].filter(isString);
        }

        return result();
      }

      if (normalized.startsWith("INSERT INTO notmid_chat_thread_access")) {
        const [threadId, actorUserId, , relationship, actorInviteStatus] = values;

        if (actorUserId === "firebase:test-user") {
          chatAccessByThreadId.set(stringValue(threadId), {
            chat_relationship:
              relationship === "friend" || relationship === "non-friend" ? relationship : null,
            chat_invite_status: inviteStatusValue(actorInviteStatus),
          });
        }

        return result();
      }

      if (normalized.startsWith("UPDATE notmid_chat_thread_access")) {
        const [threadId, userId, inviteStatus] = values;
        const current = chatAccessByThreadId.get(stringValue(threadId));

        if (
          userId === "firebase:test-user" &&
          current?.chat_invite_status === "pending-inbound"
        ) {
          const nextAccess = {
            chat_relationship: current.chat_relationship,
            chat_invite_status: inviteStatusValue(inviteStatus),
          };
          chatAccessByThreadId.set(stringValue(threadId), nextAccess);
          return result([
            {
              relationship: nextAccess.chat_relationship,
              invite_status: nextAccess.chat_invite_status,
            },
          ]);
        }

        return result();
      }

      if (normalized.startsWith("WITH saved AS")) {
        return result([
          {
            ...clips[0],
            save_count: Number(clips[0].save_count) + 1,
          },
        ]);
      }

      if (normalized.includes("FROM notmid_places place") && normalized.includes("WHERE place.id = $1")) {
        return result(values[0] === "neon-yard" ? places : []);
      }

      if (normalized.includes("FROM notmid_places place")) {
        return result(places);
      }

      if (normalized.includes("FROM notmid_clips clip") && normalized.includes("WHERE clip.id = $1")) {
        return result(values[0] === "latte-line-was-worth-it" ? clips : []);
      }

      if (normalized.includes("FROM notmid_clips clip")) {
        return result(clips);
      }

      if (normalized.includes("FROM notmid_clips") && normalized.includes("WHERE visibility = 'public'")) {
        return result(clips.map((clip) => ({ id: clip.id })));
      }

      if (
        normalized.includes("FROM notmid_chat_threads thread") &&
        normalized.includes("WHERE thread.id = $1")
      ) {
        return result(threadRowsFor(values[0]));
      }

      if (normalized.includes("FROM notmid_chat_threads thread")) {
        return result(threadRowsFor());
      }

      if (normalized.includes("FROM notmid_chat_messages message")) {
        return result(values[0] === "tonight-seongsu" ? messages : []);
      }

      throw new Error(`Unexpected SQL: ${normalized}`);
    },
  };

  return { actor, calls, client };
}

function result<Row>(rows: readonly unknown[] = []): PostgresQueryResult<Row> {
  return { rows: rows as Row[] };
}

function stringValue(value: unknown): string {
  return typeof value === "string" ? value : String(value ?? "");
}

function nullableStringValue(value: unknown): string | null {
  return typeof value === "string" ? value : null;
}

function stringArrayValue(value: unknown): string[] {
  return Array.isArray(value) && value.every((item) => typeof item === "string") ? value : [];
}

function inviteStatusValue(value: unknown): ThreadRow["chat_invite_status"] {
  return value === "accepted" ||
    value === "rejected" ||
    value === "pending-inbound" ||
    value === "pending-outbound"
    ? value
    : null;
}

function isString(value: unknown): value is string {
  return typeof value === "string";
}
