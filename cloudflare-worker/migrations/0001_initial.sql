PRAGMA foreign_keys = ON;

CREATE TABLE users (
  id TEXT PRIMARY KEY,
  created_at INTEGER NOT NULL
);

CREATE TABLE install_tokens (
  token_hash TEXT PRIMARY KEY,
  user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  created_at INTEGER NOT NULL,
  last_used_at INTEGER NOT NULL
);

CREATE TABLE link_codes (
  code_hash TEXT PRIMARY KEY,
  user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  expires_at INTEGER NOT NULL,
  claimed_at INTEGER
);

CREATE TABLE telegram_links (
  telegram_user_id TEXT PRIMARY KEY,
  chat_id TEXT NOT NULL,
  user_id TEXT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
  username TEXT,
  first_name TEXT,
  leaderboard_opt_in INTEGER NOT NULL DEFAULT 0,
  linked_at INTEGER NOT NULL
);

CREATE TABLE records (
  user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  client_record_id TEXT NOT NULL,
  session_id TEXT NOT NULL,
  attempt_number INTEGER NOT NULL,
  hold_duration_millis INTEGER NOT NULL,
  recovery_duration_millis INTEGER NOT NULL,
  recorded_at INTEGER NOT NULL,
  comfort_rating INTEGER,
  synced_at INTEGER NOT NULL,
  PRIMARY KEY (user_id, client_record_id)
);

CREATE INDEX records_user_time ON records(user_id, recorded_at DESC);

CREATE TABLE stats (
  user_id TEXT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
  attempts INTEGER NOT NULL DEFAULT 0,
  total_hold_millis INTEGER NOT NULL DEFAULT 0,
  max_hold_millis INTEGER NOT NULL DEFAULT 0,
  latest_hold_millis INTEGER NOT NULL DEFAULT 0,
  latest_timestamp INTEGER NOT NULL DEFAULT 0,
  updated_at INTEGER NOT NULL
);

CREATE TABLE monthly_stats (
  user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  period TEXT NOT NULL,
  attempts INTEGER NOT NULL DEFAULT 0,
  total_hold_millis INTEGER NOT NULL DEFAULT 0,
  max_hold_millis INTEGER NOT NULL DEFAULT 0,
  latest_hold_millis INTEGER NOT NULL DEFAULT 0,
  latest_timestamp INTEGER NOT NULL DEFAULT 0,
  updated_at INTEGER NOT NULL,
  PRIMARY KEY (user_id, period)
);

CREATE TABLE leaderboard_entries (
  user_id TEXT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
  alias TEXT NOT NULL,
  max_hold_millis INTEGER NOT NULL DEFAULT 0,
  attempts INTEGER NOT NULL DEFAULT 0,
  updated_at INTEGER NOT NULL
);

CREATE INDEX leaderboard_max ON leaderboard_entries(max_hold_millis DESC);
