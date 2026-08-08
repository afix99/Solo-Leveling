import { createHash } from "crypto";

/**
 * Identity model, deliberately minimal.
 *
 * There are no accounts, no email, no password. The phone generates a random
 * Hunter Key once and sends it as a bearer token; the server stores only its
 * SHA-256 hash and uses that as the row key. So the database can associate
 * days and habits with *a* hunter, but holds nothing that identifies a person,
 * and a dump of it cannot be replayed against the API.
 *
 * The trade this makes: losing the key means losing access to the backup.
 * That is stated plainly in the app, and is the right side of the trade for a
 * private habit journal.
 */
export type Hunter = { hunterId: string };

const MIN_KEY_LENGTH = 20;

export function hunterIdFromKey(key: string): string {
  return createHash("sha256").update(key.trim()).digest("hex");
}

/**
 * Extracts and validates the Hunter Key. Returns null when absent or too weak
 * to be a real key — a short key would let anyone brute-force their way into
 * someone else's row.
 */
export function authenticate(request: Request): Hunter | null {
  const header = request.headers.get("authorization") ?? "";
  const match = /^Bearer\s+(.+)$/i.exec(header.trim());
  const key = match?.[1]?.trim();
  if (!key || key.length < MIN_KEY_LENGTH) return null;
  return { hunterId: hunterIdFromKey(key) };
}

export function unauthorized(): Response {
  return Response.json(
    {
      error:
        "Missing or invalid Hunter Key. Send it as 'Authorization: Bearer <key>'. " +
        `Keys must be at least ${MIN_KEY_LENGTH} characters.`,
    },
    { status: 401 },
  );
}
