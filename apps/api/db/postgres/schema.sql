-- notmid API Postgres schema source artifact.
-- This file is reviewed by scripts/verify-api-persistence-config.sh.
-- Local verification may plan this schema, but it must not apply it to a real
-- database. Real apply requires scripts/migrate-api-postgres.sh --apply with
-- DATABASE_URL and NOTMID_MIGRATION_CONFIRM=apply.

CREATE TABLE IF NOT EXISTS notmid_users (
  id TEXT PRIMARY KEY,
  firebase_uid TEXT UNIQUE,
  handle TEXT NOT NULL UNIQUE,
  display_name TEXT NOT NULL,
  home_neighborhood TEXT NOT NULL DEFAULT '',
  avatar_image_url TEXT NOT NULL DEFAULT '',
  roles TEXT[] NOT NULL DEFAULT ARRAY['creator']::TEXT[],
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS notmid_places (
  id TEXT PRIMARY KEY,
  name TEXT NOT NULL,
  region TEXT NOT NULL,
  category TEXT NOT NULL,
  address TEXT NOT NULL DEFAULT '',
  lat DOUBLE PRECISION,
  lng DOUBLE PRECISION,
  open_now BOOLEAN NOT NULL DEFAULT FALSE,
  score INTEGER NOT NULL DEFAULT 0 CHECK (score >= 0),
  cover_image_url TEXT NOT NULL DEFAULT '',
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS notmid_clips (
  id TEXT PRIMARY KEY,
  owner_user_id TEXT NOT NULL REFERENCES notmid_users(id) ON DELETE RESTRICT,
  place_id TEXT NOT NULL REFERENCES notmid_places(id) ON DELETE RESTRICT,
  title TEXT NOT NULL,
  caption TEXT NOT NULL,
  visibility TEXT NOT NULL CHECK (visibility IN ('public', 'friends', 'private')),
  mood_tags TEXT[] NOT NULL DEFAULT ARRAY[]::TEXT[],
  cover_image_url TEXT NOT NULL DEFAULT '',
  video_object_key TEXT,
  thumbnail_object_key TEXT,
  moderation_status TEXT NOT NULL DEFAULT 'queued',
  like_count INTEGER NOT NULL DEFAULT 0 CHECK (like_count >= 0),
  save_count INTEGER NOT NULL DEFAULT 0 CHECK (save_count >= 0),
  comment_count INTEGER NOT NULL DEFAULT 0 CHECK (comment_count >= 0),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS notmid_saved_clips (
  user_id TEXT NOT NULL REFERENCES notmid_users(id) ON DELETE CASCADE,
  clip_id TEXT NOT NULL REFERENCES notmid_clips(id) ON DELETE CASCADE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  PRIMARY KEY (user_id, clip_id)
);

CREATE TABLE IF NOT EXISTS notmid_saved_places (
  user_id TEXT NOT NULL REFERENCES notmid_users(id) ON DELETE CASCADE,
  place_id TEXT NOT NULL REFERENCES notmid_places(id) ON DELETE CASCADE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  PRIMARY KEY (user_id, place_id)
);

CREATE TABLE IF NOT EXISTS notmid_chat_threads (
  id TEXT PRIMARY KEY,
  place_id TEXT REFERENCES notmid_places(id) ON DELETE SET NULL,
  clip_id TEXT REFERENCES notmid_clips(id) ON DELETE SET NULL,
  title TEXT NOT NULL,
  last_message TEXT NOT NULL DEFAULT '',
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS notmid_chat_thread_participants (
  thread_id TEXT NOT NULL REFERENCES notmid_chat_threads(id) ON DELETE CASCADE,
  user_id TEXT NOT NULL REFERENCES notmid_users(id) ON DELETE CASCADE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  PRIMARY KEY (thread_id, user_id)
);

CREATE TABLE IF NOT EXISTS notmid_chat_messages (
  id TEXT PRIMARY KEY,
  thread_id TEXT NOT NULL REFERENCES notmid_chat_threads(id) ON DELETE CASCADE,
  sender_user_id TEXT NOT NULL REFERENCES notmid_users(id) ON DELETE RESTRICT,
  body TEXT NOT NULL,
  clip_id TEXT REFERENCES notmid_clips(id) ON DELETE SET NULL,
  place_id TEXT REFERENCES notmid_places(id) ON DELETE SET NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS notmid_clips_place_created_idx
  ON notmid_clips (place_id, created_at DESC);

CREATE INDEX IF NOT EXISTS notmid_clips_owner_created_idx
  ON notmid_clips (owner_user_id, created_at DESC);

CREATE INDEX IF NOT EXISTS notmid_chat_messages_thread_created_idx
  ON notmid_chat_messages (thread_id, created_at ASC);
