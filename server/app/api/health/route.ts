import { databaseSource, hasDatabase } from "@/lib/db";

export const dynamic = "force-dynamic";

/** Lets the app's Settings screen tell "wrong URL" from "database not attached". */
export async function GET(): Promise<Response> {
  return Response.json({
    ok: true,
    service: "am-system",
    databaseAttached: hasDatabase(),
    databaseSource: databaseSource(),
    time: new Date().toISOString(),
  });
}
