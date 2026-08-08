import postgres from "postgres";

/**
 * Postgres access, deliberately provider-agnostic.
 *
 * An earlier version used Neon's HTTP driver, which only speaks to Neon
 * endpoints — that quietly made the choice of database vendor a code decision.
 * postgres.js speaks the ordinary wire protocol, so this service runs against
 * Neon, Supabase, Railway, Render, a Vercel marketplace store, or a Postgres
 * you host yourself. Switching provider is now a change of environment
 * variable, not a change of code.
 *
 * Any of the usual variable names is accepted, because each provider sets a
 * different one and having to rename it is a pointless failure mode.
 */
const URL_VARS = [
  "DATABASE_URL",
  "POSTGRES_URL",
  "POSTGRES_PRISMA_URL",
  "POSTGRES_URL_NON_POOLING",
  "SUPABASE_DB_URL",
  "PG_URL",
] as const;

function connectionString(): string {
  for (const name of URL_VARS) {
    const value = process.env[name];
    if (value && value.trim()) return value.trim();
  }
  throw new Error(
    "No database configured. Set DATABASE_URL to any Postgres connection " +
      "string in this project's environment variables.",
  );
}

export function hasDatabase(): boolean {
  return URL_VARS.some((name) => Boolean(process.env[name]?.trim()));
}

/** Which variable supplied the URL — surfaced by /api/health for debugging. */
export function databaseSource(): string | null {
  return URL_VARS.find((name) => Boolean(process.env[name]?.trim())) ?? null;
}

/**
 * One client per warm instance.
 *
 * `max: 1` because a serverless invocation handles a single request: a larger
 * pool would multiply idle connections by the number of warm instances and
 * exhaust the server's connection limit, which is the classic way a working
 * deployment falls over the first busy day. `idle_timeout` lets those close
 * rather than lingering after an instance goes cold.
 */
let client: ReturnType<typeof postgres> | null = null;

export function sql() {
  if (!client) {
    const url = connectionString();
    client = postgres(url, {
      max: 1,
      idle_timeout: 20,
      connect_timeout: 15,
      // Managed Postgres almost always requires TLS but frequently presents a
      // certificate the default verifier rejects. Providers document this as
      // expected; the alternative is bundling a CA per vendor, which would
      // reintroduce exactly the coupling this file removes.
      ssl: url.includes("sslmode=disable") ? false : "require",
      // Serverless has no stable console; a thrown error carries the detail.
      onnotice: () => {},
    });
  }
  return client;
}

/**
 * Creates the schema if it is missing.
 *
 * Run on demand rather than as a deploy step: this is a single-user service
 * with no migration pipeline, and `IF NOT EXISTS` makes it safe to call on
 * every request. Cached per warm instance, so it costs one round trip per
 * cold start rather than one per call.
 */
let ensured = false;
export async function ensureSchema(): Promise<void> {
  if (ensured) return;
  const db = sql();

  await db`
    CREATE TABLE IF NOT EXISTS hunters (
      hunter_id    TEXT PRIMARY KEY,
      created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
      last_seen_at TIMESTAMPTZ NOT NULL DEFAULT now(),
      hunter_name  TEXT
    )
  `;

  // Snapshots kept as history so a bad restore isn't a one-way door.
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
      hunter_id     TEXT NOT NULL REFERENCES hunters(hunter_id) ON DELETE CASCADE,
      date          DATE NOT NULL,
      nonneg_total  INT NOT NULL DEFAULT 0,
      nonneg_done   INT NOT NULL DEFAULT 0,
      optional_done INT NOT NULL DEFAULT 0,
      xp_earned     INT NOT NULL DEFAULT 0,
      gold_earned   INT NOT NULL DEFAULT 0,
      focus_minutes INT NOT NULL DEFAULT 0,
      perfect       BOOLEAN NOT NULL DEFAULT false,
      PRIMARY KEY (hunter_id, date)
    )
  `;

  await db`
    CREATE TABLE IF NOT EXISTS habit_facts (
      hunter_id      TEXT NOT NULL REFERENCES hunters(hunter_id) ON DELETE CASCADE,
      date           DATE NOT NULL,
      habit_name     TEXT NOT NULL,
      stat           TEXT NOT NULL,
      non_negotiable BOOLEAN NOT NULL DEFAULT false,
      completed      BOOLEAN NOT NULL DEFAULT false,
      penalty_quest  BOOLEAN NOT NULL DEFAULT false,
      streak         INT NOT NULL DEFAULT 0,
      PRIMARY KEY (hunter_id, date, habit_name)
    )
  `;
  await db`
    CREATE INDEX IF NOT EXISTS habit_facts_hunter_habit
      ON habit_facts (hunter_id, habit_name, date DESC)
  `;

  ensured = true;
}
