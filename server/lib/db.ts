import { neon } from "@neondatabase/serverless";

/**
 * Postgres over HTTP. The serverless driver is used rather than a pooled TCP
 * client because every request here is a short burst of queries from a phone,
 * and a Vercel function has no connection to keep warm between them.
 */
function connectionString(): string {
  const url =
    process.env.DATABASE_URL ??
    process.env.POSTGRES_URL ??
    process.env.POSTGRES_PRISMA_URL;
  if (!url) {
    throw new Error(
      "No database configured. Attach a Postgres store to this Vercel project " +
        "(Storage tab), which sets DATABASE_URL automatically.",
    );
  }
  return url;
}

export function sql() {
  return neon(connectionString());
}

export function hasDatabase(): boolean {
  return Boolean(
    process.env.DATABASE_URL ??
      process.env.POSTGRES_URL ??
      process.env.POSTGRES_PRISMA_URL,
  );
}

/**
 * Creates the schema if it is missing.
 *
 * Run on demand rather than as a migration step: this is a single-user service
 * with no deploy pipeline of its own, and `IF NOT EXISTS` makes it safe to
 * call on every request. Cached per warm instance so it costs one round trip
 * per cold start rather than one per call.
 */
let ensured = false;
export async function ensureSchema(): Promise<void> {
  if (ensured) return;
  const db = sql();

  await db`
    CREATE TABLE IF NOT EXISTS hunters (
      hunter_id   TEXT PRIMARY KEY,
      created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
      last_seen_at TIMESTAMPTZ NOT NULL DEFAULT now(),
      hunter_name TEXT
    )
  `;

  // Full snapshots, kept as history so a bad restore isn't a one-way door.
  await db`
    CREATE TABLE IF NOT EXISTS snapshots (
      id             BIGSERIAL PRIMARY KEY,
      hunter_id      TEXT NOT NULL REFERENCES hunters(hunter_id) ON DELETE CASCADE,
      taken_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
      schema_version INT NOT NULL,
      app_version    TEXT,
      payload        JSONB NOT NULL
    )
  `;
  await db`
    CREATE INDEX IF NOT EXISTS snapshots_hunter_taken
      ON snapshots (hunter_id, taken_at DESC)
  `;

  // Flattened per-day facts. The phone already knows these; storing them
  // relationally is what lets the server answer analytical questions without
  // unpacking a JSON blob on every read.
  await db`
    CREATE TABLE IF NOT EXISTS daily_facts (
      hunter_id       TEXT NOT NULL REFERENCES hunters(hunter_id) ON DELETE CASCADE,
      date            DATE NOT NULL,
      nonneg_total    INT NOT NULL DEFAULT 0,
      nonneg_done     INT NOT NULL DEFAULT 0,
      optional_done   INT NOT NULL DEFAULT 0,
      xp_earned       INT NOT NULL DEFAULT 0,
      gold_earned     INT NOT NULL DEFAULT 0,
      focus_minutes   INT NOT NULL DEFAULT 0,
      perfect         BOOLEAN NOT NULL DEFAULT false,
      PRIMARY KEY (hunter_id, date)
    )
  `;

  await db`
    CREATE TABLE IF NOT EXISTS habit_facts (
      hunter_id  TEXT NOT NULL REFERENCES hunters(hunter_id) ON DELETE CASCADE,
      date       DATE NOT NULL,
      habit_name TEXT NOT NULL,
      stat       TEXT NOT NULL,
      non_negotiable BOOLEAN NOT NULL DEFAULT false,
      completed  BOOLEAN NOT NULL DEFAULT false,
      penalty_quest BOOLEAN NOT NULL DEFAULT false,
      streak     INT NOT NULL DEFAULT 0,
      PRIMARY KEY (hunter_id, date, habit_name)
    )
  `;
  await db`
    CREATE INDEX IF NOT EXISTS habit_facts_hunter_habit
      ON habit_facts (hunter_id, habit_name, date DESC)
  `;

  ensured = true;
}
