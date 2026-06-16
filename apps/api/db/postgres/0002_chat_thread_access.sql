-- Persist actor-specific chat access for friend and non-friend thread gates.
-- This is additive and safe to apply after the initial schema.

CREATE TABLE IF NOT EXISTS notmid_chat_thread_access (
  thread_id TEXT NOT NULL REFERENCES notmid_chat_threads(id) ON DELETE CASCADE,
  user_id TEXT NOT NULL REFERENCES notmid_users(id) ON DELETE CASCADE,
  relationship TEXT NOT NULL CHECK (relationship IN ('friend', 'non-friend')),
  invite_status TEXT NOT NULL CHECK (
    invite_status IN ('accepted', 'pending-inbound', 'pending-outbound', 'rejected')
  ),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  responded_at TIMESTAMPTZ,
  PRIMARY KEY (thread_id, user_id)
);

CREATE INDEX IF NOT EXISTS notmid_chat_thread_access_user_status_idx
  ON notmid_chat_thread_access (user_id, invite_status, updated_at DESC);
