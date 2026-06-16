-- Persist server-owned user relationship state used by chat-start policy.
-- This is additive and safe to apply after chat thread access state exists.

CREATE TABLE IF NOT EXISTS notmid_user_relationships (
  user_id TEXT NOT NULL REFERENCES notmid_users(id) ON DELETE CASCADE,
  related_user_id TEXT NOT NULL REFERENCES notmid_users(id) ON DELETE CASCADE,
  status TEXT NOT NULL CHECK (status IN ('friend', 'blocked')),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  PRIMARY KEY (user_id, related_user_id),
  CHECK (user_id <> related_user_id)
);

CREATE INDEX IF NOT EXISTS notmid_user_relationships_related_status_idx
  ON notmid_user_relationships (related_user_id, status, updated_at DESC);
