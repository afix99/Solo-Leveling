import { authenticate, unauthorized } from "@/lib/auth";
import { ensureSchema, hasDatabase, sql } from "@/lib/db";

export const dynamic = "force-dynamic";
export const maxDuration = 30;

/**
 * Server-computed analytics over the synced history.
 *
 * This exists so the AI coach receives *findings* rather than a data dump.
 * The phone already knows its own numbers; what it cannot cheaply do is ask
 * "which weekday do I actually fail on", "which habit is quietly decaying",
 * or "am I trending up or down against my own baseline" across months of
 * history. Those are SQL's job, and the answers are what make coaching
 * advice specific instead of generic.
 *
 * Everything here is descriptive. No thresholds are dressed up as verdicts —
 * the model is given the shape of the data and left to interpret it.
 */
type Row = Record<string, unknown>;

function num(value: unknown): number {
  const n = Number(value);
  return Number.isFinite(n) ? n : 0;
}

function pct(part: number, whole: number): number | null {
  if (whole <= 0) return null;
  return Math.round((part / whole) * 100);
}

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
  const id = hunter.hunterId;

  // ---- Coverage: how much history is actually here --------------------
  const coverage = (await db`
    SELECT
      -- Cast to text in SQL: the driver hydrates DATE into a JS Date, which
      -- serialises as a full ISO timestamp. These end up in an AI prompt, so
      -- "2026-04-11" is both correct and cheaper than the midnight-UTC form.
      MIN(date)::text AS first_day,
      MAX(date)::text AS last_day,
      COUNT(*)  AS days_recorded
    FROM daily_facts WHERE hunter_id = ${id}
  `) as unknown as Row[];

  if (num(coverage[0]?.days_recorded) === 0) {
    return Response.json({
      hasData: false,
      note: "Nothing synced yet. Back up from the app first.",
    });
  }

  // ---- Adherence across windows ---------------------------------------
  const windows = (await db`
    SELECT
      w.label,
      COUNT(d.date)                                   AS days,
      COALESCE(SUM(d.nonneg_done), 0)                 AS nonneg_done,
      COALESCE(SUM(d.nonneg_total), 0)                AS nonneg_total,
      COALESCE(SUM(CASE WHEN d.perfect THEN 1 ELSE 0 END), 0) AS perfect_days,
      COALESCE(SUM(d.focus_minutes), 0)               AS focus_minutes,
      COALESCE(SUM(d.xp_earned), 0)                   AS xp,
      COALESCE(SUM(d.gold_earned), 0)                 AS gold
    FROM (VALUES (7, '7d'), (30, '30d'), (90, '90d')) AS w(span, label)
    LEFT JOIN daily_facts d
      ON d.hunter_id = ${id}
     AND d.date > CURRENT_DATE - w.span
    GROUP BY w.label, w.span
    ORDER BY w.span
  `) as unknown as Row[];

  // ---- Which weekday actually breaks the streak ------------------------
  const weekdays = (await db`
    SELECT
      TO_CHAR(date, 'Dy')                    AS weekday,
      EXTRACT(DOW FROM date)::int            AS dow,
      COUNT(*)                               AS days,
      COALESCE(SUM(nonneg_done), 0)          AS done,
      COALESCE(SUM(nonneg_total), 0)         AS total
    FROM daily_facts
    WHERE hunter_id = ${id} AND date > CURRENT_DATE - 90
    GROUP BY 1, 2
    ORDER BY 2
  `) as unknown as Row[];

  // ---- Per-habit reliability, and whether it is decaying ---------------
  // Recent vs prior window on the same habit is the cheapest honest signal
  // that something is slipping before the streak visibly breaks.
  const habits = (await db`
    SELECT
      habit_name,
      MAX(stat)                                                          AS stat,
      BOOL_OR(non_negotiable)                                            AS non_negotiable,
      COUNT(*) FILTER (WHERE date > CURRENT_DATE - 30)                   AS days_30,
      COUNT(*) FILTER (WHERE date > CURRENT_DATE - 30 AND completed)     AS done_30,
      COUNT(*) FILTER (WHERE date > CURRENT_DATE - 14)                   AS days_recent,
      COUNT(*) FILTER (WHERE date > CURRENT_DATE - 14 AND completed)     AS done_recent,
      COUNT(*) FILTER (WHERE date <= CURRENT_DATE - 14
                         AND date > CURRENT_DATE - 28)                   AS days_prior,
      COUNT(*) FILTER (WHERE date <= CURRENT_DATE - 14
                         AND date > CURRENT_DATE - 28 AND completed)     AS done_prior,
      MAX(streak)                                                        AS best_streak,
      COUNT(*) FILTER (WHERE penalty_quest)                              AS penalty_quests
    FROM habit_facts
    WHERE hunter_id = ${id}
    GROUP BY habit_name
    ORDER BY habit_name
  `) as unknown as Row[];

  const habitReport = habits.map((h) => {
    const recent = pct(num(h.done_recent), num(h.days_recent));
    const prior = pct(num(h.done_prior), num(h.days_prior));
    return {
      habit: h.habit_name,
      stat: h.stat,
      nonNegotiable: Boolean(h.non_negotiable),
      completionRate30: pct(num(h.done_30), num(h.days_30)),
      recentRate14: recent,
      priorRate14: prior,
      // Positive means improving, negative means slipping. Null when there
      // isn't enough history on both sides to compare honestly.
      trendPoints: recent !== null && prior !== null ? recent - prior : null,
      bestStreak: num(h.best_streak),
      penaltyQuestsTriggered: num(h.penalty_quests),
    };
  });

  const slipping = habitReport
    .filter((h) => h.trendPoints !== null && h.trendPoints <= -15)
    .sort((a, b) => (a.trendPoints ?? 0) - (b.trendPoints ?? 0));

  const strongest = [...habitReport]
    .filter((h) => h.completionRate30 !== null)
    .sort((a, b) => (b.completionRate30 ?? 0) - (a.completionRate30 ?? 0))
    .slice(0, 3);

  const weakest = [...habitReport]
    .filter((h) => h.completionRate30 !== null)
    .sort((a, b) => (a.completionRate30 ?? 0) - (b.completionRate30 ?? 0))
    .slice(0, 3);

  // ---- Momentum: this fortnight against the last -----------------------
  const momentum = (await db`
    SELECT
      COALESCE(SUM(nonneg_done) FILTER (WHERE date > CURRENT_DATE - 14), 0)  AS recent_done,
      COALESCE(SUM(nonneg_total) FILTER (WHERE date > CURRENT_DATE - 14), 0) AS recent_total,
      COALESCE(SUM(nonneg_done) FILTER (WHERE date <= CURRENT_DATE - 14
                                          AND date > CURRENT_DATE - 28), 0)  AS prior_done,
      COALESCE(SUM(nonneg_total) FILTER (WHERE date <= CURRENT_DATE - 14
                                           AND date > CURRENT_DATE - 28), 0) AS prior_total,
      COALESCE(SUM(focus_minutes) FILTER (WHERE date > CURRENT_DATE - 14), 0) AS recent_focus,
      COALESCE(SUM(focus_minutes) FILTER (WHERE date <= CURRENT_DATE - 14
                                            AND date > CURRENT_DATE - 28), 0) AS prior_focus
    FROM daily_facts WHERE hunter_id = ${id}
  `) as unknown as Row[];

  const m = momentum[0] ?? {};
  const recentRate = pct(num(m.recent_done), num(m.recent_total));
  const priorRate = pct(num(m.prior_done), num(m.prior_total));

  // ---- Longest run of perfect days, ever -------------------------------
  // Gaps-and-islands: consecutive dates share (date - row_number).
  const streaks = (await db`
    WITH perfect AS (
      SELECT date,
             date - (ROW_NUMBER() OVER (ORDER BY date))::int AS grp
      FROM daily_facts
      WHERE hunter_id = ${id} AND perfect
    )
    SELECT COUNT(*) AS length, MIN(date)::text AS started, MAX(date)::text AS ended
    FROM perfect GROUP BY grp ORDER BY length DESC LIMIT 3
  `) as unknown as Row[];

  return Response.json({
    hasData: true,
    generatedAt: new Date().toISOString(),
    coverage: {
      firstDay: coverage[0]?.first_day,
      lastDay: coverage[0]?.last_day,
      daysRecorded: num(coverage[0]?.days_recorded),
    },
    windows: windows.map((w) => ({
      window: w.label,
      daysRecorded: num(w.days),
      nonNegotiableRate: pct(num(w.nonneg_done), num(w.nonneg_total)),
      perfectDays: num(w.perfect_days),
      focusMinutes: num(w.focus_minutes),
      xpEarned: num(w.xp),
      goldEarned: num(w.gold),
    })),
    weekdayPattern: weekdays.map((d) => ({
      weekday: String(d.weekday).trim(),
      daysRecorded: num(d.days),
      nonNegotiableRate: pct(num(d.done), num(d.total)),
    })),
    momentum: {
      recentRate14: recentRate,
      priorRate14: priorRate,
      changePoints: recentRate !== null && priorRate !== null ? recentRate - priorRate : null,
      recentFocusMinutes: num(m.recent_focus),
      priorFocusMinutes: num(m.prior_focus),
    },
    habits: habitReport,
    slippingHabits: slipping,
    strongestHabits: strongest,
    weakestHabits: weakest,
    longestPerfectRuns: streaks.map((s) => ({
      lengthDays: num(s.length),
      started: s.started,
      ended: s.ended,
    })),
  });
}
