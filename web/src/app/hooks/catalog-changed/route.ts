import { timingSafeEqual } from "node:crypto";
import { revalidateTag } from "next/cache";
import { CATALOG_TAG } from "@/api/server";

/**
 * The API calls this after a publish, so pages built from the old data are
 * rebuilt on their next visit instead of after their usual 10 minutes. It
 * needs the shared secret, and is off when no secret is set.
 */
export async function POST(request: Request): Promise<Response> {
  const secret = process.env.REFRESH_SECRET;
  if (!secret || !matches(request.headers.get("x-refresh-secret"), secret)) {
    return new Response(null, { status: 404 });
  }
  // expire 0: the next visitor gets the new data, never the old page.
  revalidateTag(CATALOG_TAG, { expire: 0 });
  return Response.json({ refreshed: true });
}

function matches(given: string | null, secret: string): boolean {
  if (given === null) {
    return false;
  }
  const a = Buffer.from(given);
  const b = Buffer.from(secret);
  return a.length === b.length && timingSafeEqual(a, b);
}
