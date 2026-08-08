import { authenticate, unauthorized } from "@/lib/auth";
import { ensureSchema, hasDatabase, sql } from "@/lib/db";

export const dynamic = "force-dynamic";
export const maxDuration = 30;

/** Upper bound on a snapshot body, so one bad client can't fill the store. */
const MAX_PAYLOAD_BYTES = 4 * 1024 * 1024;

/** Snapshots retained per hunter. Enough to undo a bad restore, not a archive. */
const SNAPSHOTS_KEPT = 10;

type DayFact = {
  date: string;
  nonNegTotal?: number;
  nonNegDone?: number;
  optionalDone?: number;
  xpEarned?: number;
  goldEarned?: number;
  focusMinutes?: number;
  perfect?: boolean;
};

type HabitFact = {
  date: string;
  habitName: string;
  stat: string;
  nonNegotiable?: boolean;
  completed?: boolean;
  penaltyQuest?: boolean;
  streak?: number;
};

type SyncBody = {
  schemaVersion?: number;
  appVersion?: string;
  hunterName?: string;
  payload?: unknown;
  days?: DayFact[];
  habits?: HabitFact[];
};

function isIsoDate(value: unknown): value is string {
  return typeof value === "string" && /^\d{4}-\d{2}-\d{2}$/.test(value);
}

function int(value: unknown): number {
  const n = Number(value);
  return Number.isFinite(n) ? Math.trunc(n) : 0;
}

/**
 * Accepts a full backup from the phone and flattens it into the fact tables.
 *
 * The phone stays the source of truth: this overwrites the server's view of
 * any day it sends, and never writes back. That one-way rule is what keeps
 * this safe to run without conflict resolution — there is no second writer.
 */
export async function POST(request: Request): Promise<Response> {
  if (!hasDatabase()) {
    return Response.json(
      { error: "No database attached to this deployment yet." },
      { status: 503 },
    );
  }

  const hunter = authenticate(request);
  if (!hunter) return unauthorized();

  const raw = await request.text();
  if (raw.length > MAX_PAYLOAD_BYTES) {
    return Response.json({ error: "Snapshot too large." }, { status: 413 });
  }

  let body: SyncBody;
  try {
    body = JSON.parse(raw) as SyncBody;
  } catch {
    return Response.json({ error: "Body is not valid JSON." }, { status: 400 });
  }

  await ensureSchema();
  const db = sql();

  await db`
    INSERT INTO hunters (hunter_id, hunter_name)
    VALUES (${hunter.hunterId}, ${body.hunterName ?? null})
    ON CONFLICT (hunter_id) DO UPDATE
      SET last_seen_at = now(),
          hunter_name  = COALESCE(EXCLUDED.hunter_name, hunters.hunter_name)
  `;

  if (body.payload !== undefined) {
    await db`
      INSERT INTO snapshots (hunter_id, schema_version, app_version, payload)
      VALUES (
        ${hunter.hunterId},
        ${int(body.schemaVersion)},
        ${body.appVersion ?? null},
        ${JSON.stringify(body.payload)}
      )
    `;
    // Trim history rather than letting daily backups grow without bound.
    await db`
      DELETE FROM snapshots
      WHERE hunter_id = ${hunter.hunterId}
        AND id NOT IN (
          SELECT id FROM snapshots
          WHERE hunter_id = ${hunter.hunterId}
          ORDER BY taken_at DESC
          LIMIT ${SNAPSHOTS_KEPT}
        )
    `;
  }

  let daysWritten = 0;
  for (const day of body.days ?? []) {
    if (!isIsoDate(day?.date)) continue;
    await db`
      INSERT INTO daily_facts (
        hunter_id, date, nonneg_total, nonneg_done, optional_done,
        xp_earned, gold_earned, focus_minutes, perfect
      ) VALUES (
        ${hunter.hunterId}, ${day.date}, ${int(day.nonNegTotal)},
        ${int(day.nonNegDone)}, ${int(day.optionalDone)}, ${int(day.xpEarned)},
        ${int(day.goldEarned)}, ${int(day.focusMinutes)}, ${Boolean(day.perfect)}
      )
      ON CONFLICT (hunter_id, date) DO UPDATE SET
        nonneg_total  = EXCLUDED.nonneg_total,
        nonneg_done   = EXCLUDED.nonneg_done,
        optional_done = EXCLUDED.optional_done,
        xp_earned     = EXCLUDED.xp_earned,
        gold_earned   = EXCLUDED.gold_earned,
        focus_minutes = EXCLUDED.focus_minutes,
        perfect       = EXCLUDED.perfect
    `;
    daysWritten += 1;
  }

  let habitsWritten = 0;
  for (const habit of body.habits ?? []) {
    if (!isIsoDate(habit?.date) || typeof habit?.habitName !== "string") continue;
    if (!habit.habitName.trim()) continue;
    await db`
      INSERT INTO habit_facts (
        hunter_id, date, habit_name, stat, non_negotiable,
        completed, penalty_quest, streak
      ) VALUES (
        ${hunter.hunterId}, ${habit.date}, ${habit.habitName.slice(0, 200)},
        ${String(habit.stat ?? "").slice(0, 20)}, ${Boolean(habit.nonNegotiable)},
        ${Boolean(habit.completed)}, ${Boolean(habit.penaltyQuest)}, ${int(habit.streak)}
      )
      ON CONFLICT (hunter_id, date, habit_name) DO UPDATE SET
        stat           = EXCLUDED.stat,
        non_negotiable = EXCLUDED.non_negotiable,
        completed      = EXCLUDED.completed,
        penalty_quest  = EXCLUDED.penalty_quest,
        streak         = EXCLUDED.streak
    `;
    habitsWritten += 1;
  }

  return Response.json({
    ok: true,
    daysWritten,
    habitsWritten,
    snapshotStored: body.payload !== undefined,
  });
}

/** Returns the most recent snapshot, for restore onto a new or wiped device. */
export async function GET(request: Request): Promise<Response> {
  if (!hasDatabase()) {
    return Response.json(
      { error: "No database attached to this deployment yet." },
      { status: 503 },
    );
  }

  const hunter = authenticate(request);
  if (!hunter) return unauthorized();

  await ensureSchema();
  const db = sql();

  const rows = (await db`
    SELECT taken_at, schema_version, app_version, payload
    FROM snapshots
    WHERE hunter_id = ${hunter.hunterId}
    ORDER BY taken_at DESC
    LIMIT 1
  `) as Array<{
    taken_at: string;
    schema_version: number;
    app_version: string | null;
    payload: unknown;
  }>;

  if (rows.length === 0) {
    return Response.json({ error: "No backup found for this key." }, { status: 404 });
  }

  const row = rows[0];
  return Response.json({
    takenAt: row.taken_at,
    schemaVersion: row.schema_version,
    appVersion: row.app_version,
    payload: row.payload,
  });
}
